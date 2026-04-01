package io.pfaumc.bluemapfoliaregions.marker;

import io.papermc.paper.threadedregions.ThreadedRegionizer.ThreadedRegion;
import io.papermc.paper.threadedregions.TickRegions.TickRegionData;
import io.papermc.paper.threadedregions.TickRegions.TickRegionSectionData;
import net.minecraft.world.level.ChunkPos;

import java.util.List;

public class RegionLabelFormatter {
    public String format(
        String pattern,
        ThreadedRegion<TickRegionData, TickRegionSectionData> region,
        ChunkPos centerChunk,
        List<Long> sections,
        int shapeCount
    ) {
        return pattern
            .replace("{world}", region.getData().world.getTypeKey().identifier().getPath())
            .replace("{center_x}", Integer.toString(centerChunk.x))
            .replace("{center_z}", Integer.toString(centerChunk.z))
            .replace("{sections}", Integer.toString(sections.size()))
            .replace("{shape_count}", Integer.toString(shapeCount));
    }
}
