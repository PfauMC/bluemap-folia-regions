package io.pfaumc.bluemapfoliaregions;

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
import io.papermc.paper.util.CoordinateUtils;
import net.minecraft.world.level.ChunkPos;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.plugin.java.JavaPlugin;

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
            ScheduledTask task = Bukkit.getGlobalRegionScheduler().runAtFixedRate(this, (t) -> updateRegionMarkets(map), 20, 20 * 5);
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

    private void updateRegionMarkets(BlueMapMap map) {
        MarkerSet markerSet = MarkerSet.builder().label("Folia Regions").defaultHidden(true).toggleable(true).build();
        String id = map.getWorld().getId();
        World world = Bukkit.getWorld(UUID.fromString(id));
        if (world == null) {
            getLogger().warning("World not found: " + id);
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
            String label = "Region@" + region.getData().world.getTypeKey().location().getPath() + "[" + centerChunk.x + "," + centerChunk.z + "]";

            List<Vector2d> points = getSectionPoints(sections);
            Shape shape = new Shape(points);

            TickRegions.RegionStats stats = region.getData().getRegionStats();

            String detail = "Sections: " + sections.size() + "\n" +
                "Chunks: " + stats.getChunkCount() + "\n" +
                "Entities: " + stats.getEntityCount() + "\n" +
                "Players: " + stats.getPlayerCount() + "\n";

            ShapeMarker marker = ShapeMarker.builder()
                .shape(shape, 80)
                .label(label)
                .depthTestEnabled(false)
                .build();
            marker.setDetail(detail);

            markers.put(label, marker);
        }
        return markers;
    }

    private List<Vector2d> getSectionPoints(List<Long> sections) {
        List<Vector2d> points = getPolygonPoints(sections);
        for (int i = 0; i < points.size(); i++) {
            Vector2d point = points.get(i);
            Vector2d newPoint = Vector2d.from(point.getX() * sectionSize, point.getY() * sectionSize);
            points.set(i, newPoint);
        }
        return points;
    }

    private static List<Vector2d> getPolygonPoints(List<Long> chunkPositions) {
        List<Vector2d> points = new ArrayList<>();
        for (long chunkPos : chunkPositions) {
            int x = CoordinateUtils.getChunkX(chunkPos);
            int z = CoordinateUtils.getChunkZ(chunkPos);
            points.add(Vector2d.from(x * 16, z * 16));
            points.add(Vector2d.from(x * 16 + 15, z * 16));
            points.add(Vector2d.from(x * 16, z * 16 + 15));
            points.add(Vector2d.from(x * 16 + 15, z * 16 + 15));
        }
        return convexHull(points);
    }

    private static List<Vector2d> convexHull(List<Vector2d> points) {
        if (points.size() < 3) {
            return points;
        }

        HashSet<Vector2d> uniquePoints = new HashSet<>(points);
        points = uniquePoints.stream().sorted((p1, p2) -> {
            if (p1.getFloorX() != p2.getFloorX()) {
                return p1.getFloorX() - p2.getFloorX();
            }
            return p1.getFloorY() - p2.getFloorY();
        }).toList();

        List<Vector2d> upperHull = new ArrayList<>();
        List<Vector2d> lowerHull = new ArrayList<>();

        for (Vector2d p : points) {
            while (upperHull.size() >= 2 &&
                crossProduct(upperHull.get(upperHull.size() - 2), upperHull.get(upperHull.size() - 1), p) >= 0) {
                upperHull.remove(upperHull.size() - 1);
            }
            upperHull.add(p);
        }

        for (int i = points.size() - 1; i >= 0; i--) {
            Vector2d p = points.get(i);
            while (lowerHull.size() >= 2 &&
                crossProduct(lowerHull.get(lowerHull.size() - 2), lowerHull.get(lowerHull.size() - 1), p) >= 0) {
                lowerHull.remove(lowerHull.size() - 1);
            }
            lowerHull.add(p);
        }

        upperHull.remove(upperHull.size() - 1);
        lowerHull.remove(lowerHull.size() - 1);

        upperHull.addAll(lowerHull);

        Vector2d lastPoint = null;
        upperHull.add(upperHull.get(0));

        List<Vector2d> result = new ArrayList<>();
        for (Vector2d point : upperHull) {
            if (lastPoint != null && lastPoint.getFloorX() != point.getFloorX() && lastPoint.getFloorY() != point.getFloorY()) {
                Vector2d candidate1 = Vector2d.from(lastPoint.getFloorX(), point.getFloorY());
                if (uniquePoints.contains(candidate1)) {
                    result.add(candidate1);
                } else {
                    Vector2d candidate2 = Vector2d.from(point.getFloorX(), lastPoint.getFloorY());
                    if (uniquePoints.contains(candidate2)) {
                        result.add(candidate2);
                    }
                }
            }

            lastPoint = point;
            result.add(point);
        }
        result.remove(result.size() - 1);
        return result;
    }

    private static double crossProduct(Vector2d a, Vector2d b, Vector2d c) {
        return (b.getX() - a.getX()) * (c.getY() - a.getY()) - (b.getY() - a.getY()) * (c.getX() - a.getX());
    }
}
