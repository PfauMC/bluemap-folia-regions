package io.pfaumc.bluemapfoliaregions.command;

import io.pfaumc.bluemapfoliaregions.BlueMapFoliaRegionsPlugin;
import io.pfaumc.bluemapfoliaregions.config.PluginConfiguration;
import io.pfaumc.bluemapfoliaregions.service.BlueMapRegionMarkerService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Locale;

public class BlueMapFoliaRegionsCommand implements CommandExecutor, TabCompleter {
    private static final String RELOAD_PERMISSION = "bluemapfoliaregions.reload";

    private final BlueMapFoliaRegionsPlugin plugin;
    private final BlueMapRegionMarkerService regionMarkerService;

    public BlueMapFoliaRegionsCommand(BlueMapFoliaRegionsPlugin plugin, BlueMapRegionMarkerService regionMarkerService) {
        this.plugin = plugin;
        this.regionMarkerService = regionMarkerService;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission(RELOAD_PERMISSION)) {
                sender.sendMessage(Component.text("You do not have permission to reload this plugin.", NamedTextColor.RED));
                return true;
            }

            this.plugin.reloadConfig();
            PluginConfiguration configuration = PluginConfiguration.from(this.plugin.getConfig());
            boolean blueMapEnabled = this.regionMarkerService.reload(configuration);
            sender.sendMessage(Component.text("BlueMap Folia Regions configuration reloaded.", NamedTextColor.GREEN));
            if (!blueMapEnabled) {
                sender.sendMessage(Component.text("BlueMap is not enabled right now, so the new settings will apply when it comes online.", NamedTextColor.YELLOW));
            }
            return true;
        }

        sender.sendMessage(Component.text("Usage: /" + label + " reload", NamedTextColor.RED));
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1 && "reload".startsWith(args[0].toLowerCase(Locale.ROOT))) {
            return List.of("reload");
        }

        return List.of();
    }
}
