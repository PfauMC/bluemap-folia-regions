package io.pfaumc.bluemapfoliaregions;

import de.bluecolored.bluemap.api.BlueMapAPI;
import io.pfaumc.bluemapfoliaregions.config.PluginConfiguration;
import io.pfaumc.bluemapfoliaregions.command.BlueMapFoliaRegionsCommand;
import io.pfaumc.bluemapfoliaregions.marker.RegionMarkerFactory;
import io.pfaumc.bluemapfoliaregions.service.BlueMapRegionMarkerService;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public class BlueMapFoliaRegionsPlugin extends JavaPlugin {
    private BlueMapRegionMarkerService regionMarkerService;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadConfig();

        PluginConfiguration configuration = PluginConfiguration.from(getConfig());
        RegionMarkerFactory markerFactory = new RegionMarkerFactory();
        this.regionMarkerService = new BlueMapRegionMarkerService(this, configuration, markerFactory);
        BlueMapFoliaRegionsCommand command = new BlueMapFoliaRegionsCommand(this, this.regionMarkerService);
        PluginCommand pluginCommand = Objects.requireNonNull(getCommand("bluemapfoliaregions"), "bluemapfoliaregions command missing from plugin.yml");
        pluginCommand.setExecutor(command);
        pluginCommand.setTabCompleter(command);

        BlueMapAPI.onEnable(this.regionMarkerService::enable);
        BlueMapAPI.onDisable(this.regionMarkerService::disable);
    }

    @Override
    public void onDisable() {
        if (this.regionMarkerService != null) {
            this.regionMarkerService.shutdown();
        }
    }
}
