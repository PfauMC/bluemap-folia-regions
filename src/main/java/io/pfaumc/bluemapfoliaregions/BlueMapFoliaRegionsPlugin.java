package io.pfaumc.bluemapfoliaregions;

import ca.spottedleaf.moonrise.common.time.TickData;
import ca.spottedleaf.moonrise.common.util.CoordinateUtils;
import com.flowpowered.math.vector.Vector2d;
import de.bluecolored.bluemap.api.BlueMapAPI;
import de.bluecolored.bluemap.api.BlueMapMap;
import de.bluecolored.bluemap.api.markers.MarkerSet;
import de.bluecolored.bluemap.api.markers.ShapeMarker;
import de.bluecolored.bluemap.api.math.Shape;
import io.papermc.paper.threadedregions.ThreadedRegionizer;
import io.papermc.paper.threadedregions.ThreadedRegionizer.ThreadedRegion;
import io.papermc.paper.threadedregions.TickRegions;
import io.papermc.paper.threadedregions.TickRegions.TickRegionData;
import io.papermc.paper.threadedregions.TickRegions.TickRegionSectionData;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import net.minecraft.world.level.ChunkPos;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class BlueMapFoliaRegionsPlugin extends JavaPlugin {
    private final int sectionSize = 1 << TickRegions.getRegionChunkShift();
    private final Map<String, ScheduledTask> tasks = new HashMap<>();

    @Override
    public void onEnable() {
        BlueMapAPI.onEnable(this::onBlueMapEnable);
        BlueMapAPI.onDisable(this::onBlueMapDisable);
    }

    private void onBlueMapEnable(BlueMapAPI api) {
        for (BlueMapMap map : api.getMaps()) {
            ScheduledTask task = Bukkit.getGlobalRegionScheduler().runAtFixedRate(this, (t) -> updateRegionMarkets(api, map), 20, 20 * 5);
            this.tasks.put(map.getId(), task);
        }
    }

    private void onBlueMapDisable(BlueMapAPI api) {
        for (ScheduledTask value : this.tasks.values()) {
            value.cancel();
        }
        this.tasks.clear();
        for (BlueMapMap map : api.getMaps()) {
            map.getMarkerSets().remove("folia-regions");
        }
    }

    private void updateRegionMarkets(BlueMapAPI api, BlueMapMap map) {
        MarkerSet markerSet = MarkerSet.builder().label("Folia Regions").defaultHidden(true).toggleable(true).build();
        String id = map.getWorld().getId();
        String worldName = id.split("#")[0];
        World world = Bukkit.getWorld(worldName);

        if (world == null) {
            return;
        }
        ThreadedRegionizer<TickRegionData, TickRegionSectionData> regioniser = ((CraftWorld) world).getHandle().regioniser;
        Map<String, ShapeMarker> markers = createMarkers(regioniser);
        markerSet.getMarkers().putAll(markers);
        map.getMarkerSets().put("folia-regions", markerSet);
    }

    public Map<String, ShapeMarker> createMarkers(ThreadedRegionizer<TickRegionData, TickRegionSectionData> regioniser) {
        Map<ThreadedRegion<TickRegionData, TickRegionSectionData>, List<Long>> regionSections = new HashMap<>();
        regioniser.computeForAllRegions((region) -> regionSections.put(region, region.getOwnedSections()));

        Map<String, ShapeMarker> markers = new HashMap<>();
        for (Map.Entry<ThreadedRegion<TickRegionData, TickRegionSectionData>, List<Long>> entry : regionSections.entrySet()) {
            ThreadedRegion<TickRegionData, TickRegionSectionData> region = entry.getKey();
            List<Long> sections = entry.getValue();

            ChunkPos centerChunk = region.getCenterChunk();
            if (centerChunk == null) {
                continue; // dead region, with an empty chunk list
            }
            String label = "Region@" + region.getData().world.getTypeKey().identifier().getPath() + "[" + centerChunk.x + "," + centerChunk.z + "]";

            List<Shape> shapes = computePerimeters(sections);
            if (shapes.isEmpty()) continue;

            TickRegions.RegionStats stats = region.getData().getRegionStats();
            TickData.TickReportData reportData = region.getData().getRegionSchedulingHandle().getTickReport5s(System.nanoTime());
            String detail = getDetail(reportData, sections, stats);

            for (int i = 0; i < shapes.size(); i++) {
                String markerKey = shapes.size() > 1 ? label + "#" + i : label;
                ShapeMarker marker = ShapeMarker.builder()
                    .shape(shapes.get(i), 80)
                    .label(label)
                    .depthTestEnabled(false)
                    .build();
                marker.setDetail(detail);
                markers.put(markerKey, marker);
            }
        }
        return markers;
    }

    private static @NotNull String getDetail(TickData.TickReportData reportData, List<Long> sections, TickRegions.RegionStats stats) {
        final TickData.SegmentData tpsData = reportData.tpsData().segmentAll();
        final double mspt = reportData.timePerTickData().segmentAll().average() / 1.0E6;

        return "Sections: " + sections.size() + "\n" +
            "Chunks: " + stats.getChunkCount() + "\n" +
            "Entities: " + stats.getEntityCount() + "\n" +
            "Players: " + stats.getPlayerCount() + "\n" +
            "TPS: " + String.format("%.2f", tpsData.average()) + "\n" +
            "MSPT: " + String.format("%.2f", mspt) + "\n";
    }

    /**
     * Computes perimeter polygons for a set of sections.
     * Returns one Shape per connected boundary loop.
     *
     * Algorithm:
     * 1. For each section, emit directed boundary edges (clockwise) where no neighbor exists
     * 2. Chain edges into closed loops using right-turn-first rule at junctions
     * 3. Remove collinear intermediate points to simplify the polygon
     */
    private List<Shape> computePerimeters(List<Long> sections) {
        if (sections.isEmpty()) return Collections.emptyList();

        // Section size in blocks: sectionSize chunks * 16 blocks/chunk
        int sectionBlockSize = sectionSize * 16;

        Set<Long> sectionSet = new HashSet<>(sections.size() * 2);
        sectionSet.addAll(sections);

        // Collect boundary edges — directed clockwise around each section
        // Direction convention: 0=East(+x), 1=South(+z), 2=West(-x), 3=North(-z)
        int maxEdges = sections.size() * 4;
        long[] edgeFrom = new long[maxEdges];
        long[] edgeTo = new long[maxEdges];
        int[] edgeDir = new int[maxEdges];
        int edgeCount = 0;

        for (long section : sections) {
            int sx = CoordinateUtils.getChunkX(section);
            int sz = CoordinateUtils.getChunkZ(section);
            int x0 = sx * sectionBlockSize;
            int z0 = sz * sectionBlockSize;
            int x1 = x0 + sectionBlockSize;
            int z1 = z0 + sectionBlockSize;

            // North face → edge goes east
            if (!sectionSet.contains(ChunkPos.asLong(sx, sz - 1))) {
                edgeFrom[edgeCount] = packPoint(x0, z0);
                edgeTo[edgeCount] = packPoint(x1, z0);
                edgeDir[edgeCount++] = 0;
            }
            // East face → edge goes south
            if (!sectionSet.contains(ChunkPos.asLong(sx + 1, sz))) {
                edgeFrom[edgeCount] = packPoint(x1, z0);
                edgeTo[edgeCount] = packPoint(x1, z1);
                edgeDir[edgeCount++] = 1;
            }
            // South face → edge goes west
            if (!sectionSet.contains(ChunkPos.asLong(sx, sz + 1))) {
                edgeFrom[edgeCount] = packPoint(x1, z1);
                edgeTo[edgeCount] = packPoint(x0, z1);
                edgeDir[edgeCount++] = 2;
            }
            // West face → edge goes north
            if (!sectionSet.contains(ChunkPos.asLong(sx - 1, sz))) {
                edgeFrom[edgeCount] = packPoint(x0, z1);
                edgeTo[edgeCount] = packPoint(x0, z0);
                edgeDir[edgeCount++] = 3;
            }
        }

        if (edgeCount == 0) return Collections.emptyList();

        // Adjacency: from-point → list of edge indices
        Map<Long, List<Integer>> adj = new HashMap<>(edgeCount);
        for (int i = 0; i < edgeCount; i++) {
            adj.computeIfAbsent(edgeFrom[i], k -> new ArrayList<>()).add(i);
        }

        // Trace closed loops using right-turn-first rule
        boolean[] used = new boolean[edgeCount];
        List<Shape> shapes = new ArrayList<>();

        for (int startIdx = 0; startIdx < edgeCount; startIdx++) {
            if (used[startIdx]) continue;
            used[startIdx] = true;

            List<Long> loop = new ArrayList<>();
            loop.add(edgeFrom[startIdx]);
            int curIdx = startIdx;

            while (true) {
                long end = edgeTo[curIdx];
                if (end == edgeFrom[startIdx]) break; // closed loop
                loop.add(end);

                List<Integer> candidates = adj.get(end);
                if (candidates == null) break;

                // Right-turn priority: turn right, straight, turn left, U-turn
                int arrived = edgeDir[curIdx];
                int[] priority = {(arrived + 1) & 3, arrived, (arrived + 3) & 3, (arrived + 2) & 3};

                int nextIdx = -1;
                for (int p : priority) {
                    for (int ci : candidates) {
                        if (!used[ci] && edgeDir[ci] == p) {
                            nextIdx = ci;
                            break;
                        }
                    }
                    if (nextIdx >= 0) break;
                }
                if (nextIdx < 0) {
                    for (int ci : candidates) {
                        if (!used[ci]) {
                            nextIdx = ci;
                            break;
                        }
                    }
                }
                if (nextIdx < 0) break;

                used[nextIdx] = true;
                curIdx = nextIdx;
            }

            if (loop.size() < 3) continue;

            // Remove collinear points (all coordinates are multiples of sectionBlockSize, so exact math)
            int n = loop.size();
            List<Vector2d> simplified = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                long prev = loop.get((i - 1 + n) % n);
                long curr = loop.get(i);
                long next = loop.get((i + 1) % n);
                long cross = (long) (unpackX(curr) - unpackX(prev)) * (unpackZ(next) - unpackZ(curr)) -
                             (long) (unpackZ(curr) - unpackZ(prev)) * (unpackX(next) - unpackX(curr));
                if (cross != 0) {
                    simplified.add(Vector2d.from(unpackX(curr), unpackZ(curr)));
                }
            }

            if (simplified.size() >= 3) {
                shapes.add(new Shape(simplified));
            }
        }

        return shapes;
    }

    private static long packPoint(int x, int z) {
        return (long) x << 32 | (z & 0xFFFFFFFFL);
    }

    private static int unpackX(long packed) {
        return (int) (packed >> 32);
    }

    private static int unpackZ(long packed) {
        return (int) packed;
    }
}
