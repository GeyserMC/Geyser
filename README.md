<img src="https://geysermc.org/img/oss_logo.png" alt="Geyser" width="600"  height="170"/>

[![forthebadge made-with-java](http://ForTheBadge.com/images/badges/made-with-java.svg)](https://java.com/)

[![License: MIT](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
[![Build Status](https://ci.nukkitx.com/job/Geyser/job/master/badge/icon)](https://ci.nukkitx.com/job/Geyser/job/master/)
[![Discord](https://img.shields.io/discord/597838753859633172.svg?color=%237289da&label=discord)](https://discord.gg/GPMF8E2)
[![HitCount](http://hits.dwyl.io/Geyser/GeyserMC.svg)](http://hits.dwyl.io/Geyser/GeyserMC)

A bridge between Minecraft: Bedrock Edition and Minecraft: Java Edition, closing the gap from those wanting to play together.

## What is Geyser?
Geyser is a proxy, bridging the gap between Minecraft: Bedrock Edition and Minecraft: Java Edition servers.
The ultimate goal for this project is to allow those to join Minecraft: Java Edition servers using the Bedrock Edition.

### Please note, this project is still a work in progress and should not be used on production yet.

Links:
- Website: https://geysermc.org
- Docs: https://geysermc.org/docs
- Download: http://ci.geysermc.org
- Discord: https://discord.gg/mRjbCsS

## What's Completed
- [x] Server recognized in server list 
- [x] Join detection from remote
- [x] Online mode/auth support
- [x] Chat/command support
- [ ] Scoreboard
  - [x] Objective-based scoreboards
  - [ ] Team-based scoreboards
- [ ] Inventory support
  - [x] Inventory viewing
  - [x] NBT data
  - [ ] Inventory movement (transactions)
- [x] Player movement support
- [x] Entity support
- [ ] Chunks (Experimental)
  - [x] Block translation (may be missing mappings)
  - [x] Block updates
  - [ ] Block entities
  - [ ] Extra data
  - [ ] Biome colors
- [ ] Block break/place support

## Compiling
Clone the repo to your computer, and run `mvn clean install`.

## Contributing
Any contributions are appreciated. Please feel free to reach out to us on Discord if
you're interested in helping this project come along.

## Libraries Used:
- [NukkitX Bedrock Protocol Library](https://github.com/NukkitX/Protocol)
- [Steveice10's Java Protocol Library](https://github.com/Steveice10/MCProtocolLib)
- [TerminalConsoleAppender](https://github.com/Minecrell/TerminalConsoleAppender)
- [Simple Logging Facade for Java (slf4j)](https://github.com/qos-ch/slf4j)
