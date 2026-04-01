package io.pfaumc.bluemapfoliaregions.service;

import de.bluecolored.bluemap.api.BlueMapAPI;
import de.bluecolored.bluemap.api.BlueMapMap;
import de.bluecolored.bluemap.api.markers.MarkerSet;
import de.bluecolored.bluemap.api.markers.ShapeMarker;
import io.pfaumc.bluemapfoliaregions.config.PluginConfiguration;
import io.pfaumc.bluemapfoliaregions.marker.RegionMarkerFactory;
import io.papermc.paper.threadedregions.TickRegions.TickRegionData;
import io.papermc.paper.threadedregions.TickRegions.TickRegionSectionData;
import io.papermc.paper.threadedregions.ThreadedRegionizer;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;

public class BlueMapRegionMarkerService {
    private final JavaPlugin plugin;
    private final PluginConfiguration configuration;
    private final RegionMarkerFactory markerFactory;
    private final Map<String, ScheduledTask> tasks = new HashMap<>();
    private final Map<String, MarkerSet> markerSets = new HashMap<>();

    public BlueMapRegionMarkerService(JavaPlugin plugin, PluginConfiguration configuration, RegionMarkerFactory markerFactory) {
        this.plugin = plugin;
        this.configuration = configuration;
        this.markerFactory = markerFactory;
    }

    public void enable(BlueMapAPI api) {
        for (BlueMapMap map : api.getMaps()) {
            ScheduledTask task = Bukkit.getGlobalRegionScheduler().runAtFixedRate(
                this.plugin,
                scheduledTask -> updateMarkers(map),
                20,
                this.configuration.updateIntervalTicks()
            );
            this.tasks.put(map.getId(), task);
        }
    }

    public void disable(BlueMapAPI api) {
        for (ScheduledTask task : this.tasks.values()) {
            task.cancel();
        }
        this.tasks.clear();
        this.markerSets.clear();

        for (BlueMapMap map : api.getMaps()) {
            map.getMarkerSets().remove(this.configuration.markerSetKey());
        }
    }

    private void updateMarkers(BlueMapMap map) {
        World world = Bukkit.getWorld(resolveWorldName(map));
        if (world == null) {
            return;
        }

        MarkerSet markerSet = this.markerSets.computeIfAbsent(map.getId(), ignored -> MarkerSet.builder()
            .label(this.configuration.markerSetLabel())
            .defaultHidden(this.configuration.defaultHidden())
            .toggleable(this.configuration.toggleable())
            .build());

        ThreadedRegionizer<TickRegionData, TickRegionSectionData> regioniser = ((CraftWorld) world).getHandle().regioniser;
        Map<String, ShapeMarker> markers = this.markerFactory.createMarkers(regioniser, this.configuration);
        markerSet.getMarkers().clear();
        markerSet.getMarkers().putAll(markers);
        map.getMarkerSets().put(this.configuration.markerSetKey(), markerSet);
    }

    private static String resolveWorldName(BlueMapMap map) {
        return map.getWorld().getId().split("#")[0];
    }
}
