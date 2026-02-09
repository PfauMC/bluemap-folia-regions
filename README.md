# BlueMap Folia Regions

A Paper plugin that visualizes [Folia](https://github.com/PaperMC/Folia)'s threaded tick regions on [BlueMap](https://bluemap.bluecolored.de/)'s web map in real time.

<img width="862" alt="image" src="https://github.com/PfauMC/bluemap-folia-regions/assets/97797573/e384c881-ad78-441b-9106-fc25eb620182">

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

## Building

```bash
./gradlew build
```

The compiled jar will be in `build/libs/`.

## License

[MIT](LICENSE)
