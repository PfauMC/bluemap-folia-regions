package io.pfaumc.bluemapfoliaregions;

import de.bluecolored.bluemap.api.BlueMapAPI;
import io.pfaumc.bluemapfoliaregions.config.PluginConfiguration;
import io.pfaumc.bluemapfoliaregions.marker.RegionMarkerFactory;
import io.pfaumc.bluemapfoliaregions.service.BlueMapRegionMarkerService;
import org.bukkit.plugin.java.JavaPlugin;

public class BlueMapFoliaRegionsPlugin extends JavaPlugin {
    private BlueMapRegionMarkerService regionMarkerService;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadConfig();

        PluginConfiguration configuration = PluginConfiguration.from(getConfig());
        RegionMarkerFactory markerFactory = new RegionMarkerFactory();
        this.regionMarkerService = new BlueMapRegionMarkerService(this, configuration, markerFactory);

        BlueMapAPI.onEnable(this.regionMarkerService::enable);
        BlueMapAPI.onDisable(this.regionMarkerService::disable);
    }
}
