<img src="https://geysermc.org/img/geyserlogo.png" alt="Geyser" width="600"  height="170"/>

[![forthebadge made-with-java](http://ForTheBadge.com/images/badges/made-with-java.svg)](https://java.com/)

[![License: MIT](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
[![Build Status](https://ci.nukkitx.com/job/Geyser/job/master/badge/icon)](https://ci.nukkitx.com/job/Geyser/job/master/)
[![Discord](https://img.shields.io/discord/597838753859633172.svg?color=%237289da&label=discord)](http://discord.geysermc.org/)
[![HitCount](http://hits.dwyl.io/Geyser/GeyserMC.svg)](http://hits.dwyl.io/Geyser/GeyserMC)

Geyser is a bridge between Minecraft: Bedrock Edition and Minecraft: Java Edition, closing the gap from those wanting to play true cross-platform.

## What is Geyser?
Geyser is a proxy, bridging the gap between Minecraft: Bedrock Edition and Minecraft: Java Edition servers.
The ultimate goal of this project is to allow Minecraft: Bedrock Edition users to join Minecraft: Java Edition servers as seamlessly as possible.

### Please note, this project is still a work in progress and should not be used on production.

## Setting Up
Please note, Geyser is **not** a plugin. Watch the video below or take a look [here](https://github.com/GeyserMC/Geyser/wiki#Setup) for how to set it up.

[![YouTube Video](https://img.youtube.com/vi/OmLxwl7_UzQ/0.jpg)](https://www.youtube.com/watch?v=OmLxwl7_UzQ)

Links:
- Website: https://geysermc.org
- Docs: https://github.com/GeyserMC/Geyser/wiki
- Download: http://ci.geysermc.org
- Discord: http://discord.geysermc.org/

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
- [x] Block break/place support

## Compiling
1. Clone the repo to your computer
2. [Install Maven](https://maven.apache.org/install.html)
3. Navigate to the Geyser root directory and run `mvn clean install`

## Contributing
Any contributions are appreciated. Please feel free to reach out to us on [Discord](http://discord.geysermc.org/) if
you're interested in helping out with Geyser.

## Libraries Used:
- [NukkitX Bedrock Protocol Library](https://github.com/NukkitX/Protocol)
- [Steveice10's Java Protocol Library](https://github.com/Steveice10/MCProtocolLib)
- [TerminalConsoleAppender](https://github.com/Minecrell/TerminalConsoleAppender)
- [Simple Logging Facade for Java (slf4j)](https://github.com/qos-ch/slf4j)
