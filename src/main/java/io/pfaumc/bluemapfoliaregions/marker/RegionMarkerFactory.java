package io.pfaumc.bluemapfoliaregions.marker;

import ca.spottedleaf.moonrise.common.time.TickData;
import de.bluecolored.bluemap.api.markers.ShapeMarker;
import de.bluecolored.bluemap.api.math.Shape;
import io.pfaumc.bluemapfoliaregions.config.PluginConfiguration;
import io.papermc.paper.threadedregions.ThreadedRegionizer;
import io.papermc.paper.threadedregions.ThreadedRegionizer.ThreadedRegion;
import io.papermc.paper.threadedregions.TickRegions;
import io.papermc.paper.threadedregions.TickRegions.TickRegionData;
import io.papermc.paper.threadedregions.TickRegions.TickRegionSectionData;
import net.minecraft.world.level.ChunkPos;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RegionMarkerFactory {
    private final RegionShapeFactory shapeFactory = new RegionShapeFactory(TickRegions.getRegionChunkShift());
    private final RegionLabelFormatter labelFormatter = new RegionLabelFormatter();

    public Map<String, ShapeMarker> createMarkers(ThreadedRegionizer<TickRegionData, TickRegionSectionData> regioniser, PluginConfiguration configuration) {
        Map<ThreadedRegion<TickRegionData, TickRegionSectionData>, List<Long>> regionSections = new HashMap<>();
        regioniser.computeForAllRegions(region -> regionSections.put(region, region.getOwnedSections()));

        Map<String, ShapeMarker> markers = new HashMap<>();
        for (Map.Entry<ThreadedRegion<TickRegionData, TickRegionSectionData>, List<Long>> entry : regionSections.entrySet()) {
            ThreadedRegion<TickRegionData, TickRegionSectionData> region = entry.getKey();
            List<Long> sections = entry.getValue();

            ChunkPos centerChunk = region.getCenterChunk();
            if (centerChunk == null) {
                continue;
            }

            List<Shape> shapes = this.shapeFactory.computePerimeters(sections);
            if (shapes.isEmpty()) {
                continue;
            }

            String label = this.labelFormatter.format(configuration.regionLabelFormat(), region, centerChunk, sections, shapes.size());
            TickRegions.RegionStats stats = region.getData().getRegionStats();
            TickData.TickReportData reportData = region.getData().getRegionSchedulingHandle().getTickReport5s(System.nanoTime());
            String detail = getDetail(reportData, sections, stats);

            for (int i = 0; i < shapes.size(); i++) {
                String markerKey = shapes.size() > 1 ? label + "#" + (i + 1) : label;
                ShapeMarker marker = ShapeMarker.builder()
                    .shape(shapes.get(i), configuration.markerHeight())
                    .label(label)
                    .depthTestEnabled(false)
                    .build();
                marker.setFillColor(configuration.markerFillColor());
                marker.setLineColor(configuration.markerLineColor());
                marker.setLineWidth(configuration.markerLineWidth());
                marker.setDetail(detail);
                markers.put(markerKey, marker);
            }
        }

        return markers;
    }

    private static @NotNull String getDetail(TickData.TickReportData reportData, List<Long> sections, TickRegions.RegionStats stats) {
        TickData.SegmentData tpsData = reportData.tpsData().segmentAll();
        double mspt = reportData.timePerTickData().segmentAll().average() / 1.0E6;

        return "Sections: " + sections.size() + "\n" +
            "Chunks: " + stats.getChunkCount() + "\n" +
            "Entities: " + stats.getEntityCount() + "\n" +
            "Players: " + stats.getPlayerCount() + "\n" +
            "TPS: " + String.format("%.2f", tpsData.average()) + "\n" +
            "MSPT: " + String.format("%.2f", mspt) + "\n";
    }
}
