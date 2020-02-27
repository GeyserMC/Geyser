<img src="https://geysermc.org/img/geyserlogo.png" alt="Geyser" width="600"/>

[![forthebadge made-with-java](http://ForTheBadge.com/images/badges/made-with-java.svg)](https://java.com/)

[![License: MIT](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
[![Build on Push](https://github.com/LoyaltyMC/Geyser/workflows/Build%20on%20Push/badge.svg)](https://github.com/LoyaltyMC/Geyser/actions?query=workflow%3A%22Build+on+Push%22)
[![Discord](https://img.shields.io/discord/613163671870242838.svg?color=%237289da&label=discord)](http://discord.geysermc.org/)
[![HitCount](http://hits.dwyl.io/Geyser/GeyserMC.svg)](http://hits.dwyl.io/Geyser/GeyserMC)

## Don't post bug reports in the Geyser discord or the Geyser repo. Use this at your own risk
### If you want block entities support, then use the Legacy branch, although there won't be any major updates on that branch.

## What is Geyser?
Geyser is a proxy, bridging the gap between Minecraft: Bedrock Edition and Minecraft: Java Edition servers.
The ultimate goal of this project is to allow Minecraft: Bedrock Edition users to join Minecraft: Java Edition servers as seamlessly as possible.

## Download
[Download Latest Build](https://github.com/LoyaltyMC/Geyser/actions) (GitHub account required)

You can also download the latest release [here](https://github.com/LoyaltyMC/Geyser/releases) (No account required) although it might not have the latest changes.

## Compiling
1. Clone the repo to your computer
2. [Install Maven](https://maven.apache.org/install.html)
3. Navigate to the Geyser root directory and run `git submodule update --init --recursive`. This downloads all the needed submodules for Geyser and is a crucial step in this process.
4. Run `mvn clean install` and locate to the `target` folder.

## What's Left to be Added/Fixed
- Sounds
- Block Particles
- Block Entities ([`block-entities`](https://github.com/GeyserMC/Geyser/tree/block-entities))
- Some Entity Flags

## Libraries Used:
- [NukkitX Bedrock Protocol Library](https://github.com/NukkitX/Protocol)
- [Steveice10's Java Protocol Library](https://github.com/Steveice10/MCProtocolLib)
- [TerminalConsoleAppender](https://github.com/Minecrell/TerminalConsoleAppender)
- [Simple Logging Facade for Java (slf4j)](https://github.com/qos-ch/slf4j)
