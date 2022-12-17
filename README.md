<img src="https://geysermc.org/img/geyser-1760-860.png" alt="Geyser" width="600"/>

[![forthebadge made-with-java](https://forthebadge.com/images/badges/made-with-java.svg)](https://java.com/)

[![License: MIT](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
[![Build Status](https://ci.opencollab.dev/job/Geyser/job/master/badge/icon)](https://ci.opencollab.dev/job/GeyserMC/job/Geyser/job/master/)
[![Discord](https://img.shields.io/discord/613163671870242838.svg?color=%237289da&label=discord)](https://discord.gg/geysermc)
[![Crowdin](https://badges.crowdin.net/geyser/localized.svg)](https://translate.geysermc.org/)

Geyser is an open collaboration project by [CubeCraft Games](https://cubecraft.net) that serves as a bridge between Minecraft: Bedrock Edition and Minecraft: Java Edition, allowing players to play true cross-platform.

## What is Geyser?
Geyser is a proxy that connects Minecraft: Bedrock Edition and Minecraft: Java Edition servers. While the ultimate goal of this project is to enable Minecraft: Bedrock Edition users to join Minecraft: Java Edition servers with minimal issues, some limitations and bugs may occur due to the nature of Geyser translating packets between the two different games.

We would like to extend our thanks to the DragonProxy project for pioneering protocol translation and to all the team members who have joined us on this project.

### Geyser currently supports Minecraft Bedrock 1.19.20 - 1.19.51 and Minecraft Java 1.19.3.

## Setting Up

To set up Geyser, follow the instructions [here](https://wiki.geysermc.org/geyser/setup/).

[![YouTube Video](https://img.youtube.com/vi/U7dZZ8w7Gi4/0.jpg)](https://www.youtube.com/watch?v=U7dZZ8w7Gi4)

## Links:

- Website: https://geysermc.org
- Documentation: https://wiki.geysermc.org/geyser/
- Download: https://ci.geysermc.org
- Discord: https://discord.gg/geysermc
- Donate: https://opencollective.com/geysermc
- Test Server: `test.geysermc.org` port `25565` for Java and `19132` for Bedrock

## In Progress

- Near-perfect movement (to the point where anticheat on large servers is unlikely to ban you)
- Some Entity Flags
- Structure block UI

## What can't be fixed
There are a few things Geyser is unable to support due to various differences between Minecraft Bedrock and Java. For a list of these limitations, see the [Current Limitations](https://wiki.geysermc.org/geyser/current-limitations/) page.

## Compiling
To compile Geyser:

1. Clone the repository to your computer.
2. Navigate to the Geyser root directory and run `git submodule update --init --recursive`. This command downloads all the needed submodules for Geyser.
3. Run `gradlew build` and locate the `bootstrap/build` folder.

## Contributing
Any contributions are appreciated. Please feel free to reach out to us on [Discord](http://discord.geysermc.org/) if
you're interested in helping out with Geyser.

## Libraries Used:
- [Adventure Text Library](https://github.com/KyoriPowered/adventure)
- [NukkitX Bedrock Protocol Library](https://github.com/NukkitX/Protocol)
- [Steveice10's Java Protocol Library](https://github.com/Steveice10/MCProtocolLib)
- [TerminalConsoleAppender](https://github.com/Minecrell/TerminalConsoleAppender)
- [Simple Logging Facade for Java (slf4j)](https://github.com/qos-ch/slf4j)
