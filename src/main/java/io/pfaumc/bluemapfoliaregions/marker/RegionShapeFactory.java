package io.pfaumc.bluemapfoliaregions.marker;

import ca.spottedleaf.moonrise.common.util.CoordinateUtils;
import com.flowpowered.math.vector.Vector2d;
import de.bluecolored.bluemap.api.math.Shape;
import net.minecraft.world.level.ChunkPos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class RegionShapeFactory {
    private final int sectionBlockSize;

    public RegionShapeFactory(int regionChunkShift) {
        this.sectionBlockSize = (1 << regionChunkShift) * 16;
    }

    public List<Shape> computePerimeters(List<Long> sections) {
        if (sections.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Long> sectionSet = new HashSet<>(sections.size() * 2);
        sectionSet.addAll(sections);

        int maxEdges = sections.size() * 4;
        long[] edgeFrom = new long[maxEdges];
        long[] edgeTo = new long[maxEdges];
        int[] edgeDir = new int[maxEdges];
        int edgeCount = 0;

        for (long section : sections) {
            int sx = CoordinateUtils.getChunkX(section);
            int sz = CoordinateUtils.getChunkZ(section);
            int x0 = sx * this.sectionBlockSize;
            int z0 = sz * this.sectionBlockSize;
            int x1 = x0 + this.sectionBlockSize;
            int z1 = z0 + this.sectionBlockSize;

            if (!sectionSet.contains(ChunkPos.asLong(sx, sz - 1))) {
                edgeFrom[edgeCount] = packPoint(x0, z0);
                edgeTo[edgeCount] = packPoint(x1, z0);
                edgeDir[edgeCount++] = 0;
            }
            if (!sectionSet.contains(ChunkPos.asLong(sx + 1, sz))) {
                edgeFrom[edgeCount] = packPoint(x1, z0);
                edgeTo[edgeCount] = packPoint(x1, z1);
                edgeDir[edgeCount++] = 1;
            }
            if (!sectionSet.contains(ChunkPos.asLong(sx, sz + 1))) {
                edgeFrom[edgeCount] = packPoint(x1, z1);
                edgeTo[edgeCount] = packPoint(x0, z1);
                edgeDir[edgeCount++] = 2;
            }
            if (!sectionSet.contains(ChunkPos.asLong(sx - 1, sz))) {
                edgeFrom[edgeCount] = packPoint(x0, z1);
                edgeTo[edgeCount] = packPoint(x0, z0);
                edgeDir[edgeCount++] = 3;
            }
        }

        if (edgeCount == 0) {
            return Collections.emptyList();
        }

        Map<Long, List<Integer>> adjacency = new HashMap<>(edgeCount);
        for (int i = 0; i < edgeCount; i++) {
            adjacency.computeIfAbsent(edgeFrom[i], ignored -> new ArrayList<>()).add(i);
        }

        boolean[] used = new boolean[edgeCount];
        List<Shape> shapes = new ArrayList<>();

        for (int startIndex = 0; startIndex < edgeCount; startIndex++) {
            if (used[startIndex]) {
                continue;
            }

            used[startIndex] = true;
            List<Long> loop = new ArrayList<>();
            loop.add(edgeFrom[startIndex]);
            int currentIndex = startIndex;

            while (true) {
                long end = edgeTo[currentIndex];
                if (end == edgeFrom[startIndex]) {
                    break;
                }

                loop.add(end);
                List<Integer> candidates = adjacency.get(end);
                if (candidates == null) {
                    break;
                }

                int arrived = edgeDir[currentIndex];
                int[] priority = {(arrived + 1) & 3, arrived, (arrived + 3) & 3, (arrived + 2) & 3};
                int nextIndex = findNextEdge(candidates, used, edgeDir, priority);
                if (nextIndex < 0) {
                    break;
                }

                used[nextIndex] = true;
                currentIndex = nextIndex;
            }

            if (loop.size() < 3) {
                continue;
            }

            List<Vector2d> simplified = simplifyLoop(loop);
            if (simplified.size() >= 3) {
                shapes.add(new Shape(simplified));
            }
        }

        return shapes;
    }

    private static int findNextEdge(List<Integer> candidates, boolean[] used, int[] edgeDir, int[] priority) {
        for (int direction : priority) {
            for (int candidate : candidates) {
                if (!used[candidate] && edgeDir[candidate] == direction) {
                    return candidate;
                }
            }
        }

        for (int candidate : candidates) {
            if (!used[candidate]) {
                return candidate;
            }
        }

        return -1;
    }

    private static List<Vector2d> simplifyLoop(List<Long> loop) {
        int size = loop.size();
        List<Vector2d> simplified = new ArrayList<>();

        for (int i = 0; i < size; i++) {
            long previous = loop.get((i - 1 + size) % size);
            long current = loop.get(i);
            long next = loop.get((i + 1) % size);
            long cross = (long) (unpackX(current) - unpackX(previous)) * (unpackZ(next) - unpackZ(current)) -
                (long) (unpackZ(current) - unpackZ(previous)) * (unpackX(next) - unpackX(current));

            if (cross != 0) {
                simplified.add(Vector2d.from(unpackX(current), unpackZ(current)));
            }
        }

        return simplified;
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
