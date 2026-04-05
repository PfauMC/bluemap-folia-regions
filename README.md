# BlueMap Folia Regions

A plugin that visualizes [Folia](https://github.com/PaperMC/Folia)'s threaded tick regions on [BlueMap](https://bluemap.bluecolored.de/)'s web map in real time.

<img width="862" alt="folia_regions" src="https://github.com/user-attachments/assets/0e6ae120-f303-40a2-b47a-77a4e8627c4f" />

## Overview

Folia splits the Minecraft world into independent regions that tick in parallel across multiple threads. This plugin renders those region boundaries as polygon overlays on BlueMap, giving server administrators a live view of how their world is partitioned.

Each region marker displays:
- Number of sections and chunks
- Entity and player count
- TPS (Ticks Per Second)
- MSPT (Milliseconds Per Tick)

Markers refresh every 5 seconds automatically.

## Requirements

- [Folia](https://github.com/PaperMC/Folia) or [Canvas](https://github.com/CraftCanvasMC/Canvas) server (1.20+)
- [BlueMap](https://bluemap.bluecolored.de/) plugin installed

## Installation

1. Download the latest release from the [Releases](https://github.com/PfauMC/bluemap-folia-regions/releases) page.
2. Place the `.jar` file into your server's `plugins` directory.
3. Restart the server.

The "Folia Regions" marker set will appear on your BlueMap (hidden by default — toggle it on from the map sidebar).

## Configuration

The plugin writes `plugins/BlueMap-Folia-Regions/config.yml` on first start.

```yaml
update-interval-seconds: 5

marker-set:
  key: folia-regions
  label: Folia Regions
  default-hidden: true
  toggleable: true

markers:
  label-format: "Region@{world}[{center_x},{center_z}]"
  height: 80
  fill-color: "#3b82f618"
  line-color: "#1d4ed8ff"
  line-width: 2
```

Set `marker-set.default-hidden` to control whether region markers are shown when BlueMap loads.
Use `markers.label-format` to control how region names are rendered.
Available placeholders: `{world}`, `{center_x}`, `{center_z}`, `{sections}`, `{shape_count}`.
Use `markers.fill-color` for the region box fill and `markers.line-color` / `markers.line-width` for the border.
Run `/bluemapfoliaregions reload` or `/bmfr reload` after editing the config to apply changes without restarting the server.

## Building

```bash
./gradlew build
```

The compiled jar will be in `build/libs/`.

## License

[MIT](LICENSE)
