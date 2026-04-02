package io.pfaumc.bluemapfoliaregions.config;

import de.bluecolored.bluemap.api.math.Color;
import org.bukkit.configuration.file.FileConfiguration;

public record PluginConfiguration(
    String markerSetKey,
    String markerSetLabel,
    boolean defaultHidden,
    boolean toggleable,
    String regionLabelFormat,
    int updateIntervalTicks,
    int markerHeight,
    Color markerFillColor,
    Color markerLineColor,
    int markerLineWidth
) {
    public static PluginConfiguration from(FileConfiguration config) {
        return new PluginConfiguration(
            config.getString("marker-set.key", "folia-regions"),
            config.getString("marker-set.label", "Folia Regions"),
            config.getBoolean("marker-set.default-hidden", true),
            config.getBoolean("marker-set.toggleable", true),
            config.getString("markers.label-format", "Region@{world}[{center_x},{center_z}]"),
            Math.max(20, config.getInt("update-interval-seconds", 5) * 20),
            config.getInt("markers.height", 80),
            parseColor(config.getString("markers.fill-color", "#3b82f618")),
            parseColor(config.getString("markers.line-color", "#1d4ed8ff")),
            Math.max(1, config.getInt("markers.line-width", 2))
        );
    }

    private static Color parseColor(String value) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.startsWith("#")) {
            normalized = normalized.substring(1);
        }

        return switch (normalized.length()) {
            case 6 -> new Color(Integer.parseInt(normalized.substring(0, 2), 16), Integer.parseInt(normalized.substring(2, 4), 16), Integer.parseInt(normalized.substring(4, 6), 16));
            case 8 -> new Color(
                Integer.parseInt(normalized.substring(0, 2), 16),
                Integer.parseInt(normalized.substring(2, 4), 16),
                Integer.parseInt(normalized.substring(4, 6), 16),
                (Integer.parseInt(normalized.substring(6, 8), 16) / 100f)
            );
            default -> throw new IllegalArgumentException("invalid color value: " + value + " expected #RRGGBB or #RRGGBBAA");
        };
    }
}
