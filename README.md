<img src="https://geysermc.org/img/geyser-1760-860.png" alt="Geyser" width="600"/>

[![forthebadge made-with-java](http://ForTheBadge.com/images/badges/made-with-java.svg)](https://java.com/)

[![License: MIT](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
[![Build Status](https://ci.opencollab.dev/job/Geyser/job/master/badge/icon)](https://ci.opencollab.dev/job/GeyserMC/job/Geyser/job/master/)
[![Discord](https://img.shields.io/discord/613163671870242838.svg?color=%237289da&label=discord)](http://discord.geysermc.org/)
[![HitCount](http://hits.dwyl.com/Geyser/GeyserMC.svg)](http://hits.dwyl.com/Geyser/GeyserMC)
[![Crowdin](https://badges.crowdin.net/geyser/localized.svg)](https://translate.geysermc.org/)

Geyser is a bridge between Minecraft: Bedrock Edition and Minecraft: Java Edition, closing the gap from those wanting to play true cross-platform.

Geyser is an open collaboration project by [CubeCraft Games](https://cubecraft.net).

## What is Geyser?
Geyser is a proxy, bridging the gap between Minecraft: Bedrock Edition and Minecraft: Java Edition servers.
The ultimate goal of this project is to allow Minecraft: Bedrock Edition users to join Minecraft: Java Edition servers as seamlessly as possible. **Please note, this project is still a work in progress and should not be used on production. Expect bugs!**

Special thanks to the DragonProxy project for being a trailblazer in protocol translation and for all the team members who have now joined us here!

### Currently supporting Minecraft Bedrock v1.16.100 - v1.16.220 and Minecraft Java v1.16.4 - v1.16.5.

## Setting Up
Take a look [here](https://github.com/GeyserMC/Geyser/wiki#Setup) for how to set up Geyser.

[![YouTube Video](https://img.youtube.com/vi/U7dZZ8w7Gi4/0.jpg)](https://www.youtube.com/watch?v=U7dZZ8w7Gi4)

## Links:
- Website: https://geysermc.org
- Docs: https://github.com/GeyserMC/Geyser/wiki
- Download: http://ci.geysermc.org
- Discord: http://discord.geysermc.org/
- ~~Donate: https://patreon.com/GeyserMC~~ Currently disabled.
- Test Server: `test.geysermc.org` port `25565` for Java and `19132` for Bedrock

## What's Left to be Added/Fixed
- Near-perfect movement (to the point where anticheat on large servers is unlikely to ban you)
- Resource pack conversion/CustomModelData
- Some Entity Flags
- Structure block UI

## What can't be fixed
The following things can't be fixed because of Bedrock limitations. They might be fixable in the future, but not as of now.

- Custom heads in inventories
- Clickable links in chat
- Glowing effect
- Custom armor stand poses

## Compiling
1. Clone the repo to your computer
2. [Install Maven](https://maven.apache.org/install.html)
3. Navigate to the Geyser root directory and run `git submodule update --init --recursive`. This downloads all the needed submodules for Geyser and is a crucial step in this process.
4. Run `mvn clean install` and locate to the `target` folder.

## Contributing
Any contributions are appreciated. Please feel free to reach out to us on [Discord](http://discord.geysermc.org/) if
you're interested in helping out with Geyser.

## Libraries Used:
- [NukkitX Bedrock Protocol Library](https://github.com/NukkitX/Protocol)
- [Steveice10's Java Protocol Library](https://github.com/Steveice10/MCProtocolLib)
- [TerminalConsoleAppender](https://github.com/Minecrell/TerminalConsoleAppender)
- [Simple Logging Facade for Java (slf4j)](https://github.com/qos-ch/slf4j)
