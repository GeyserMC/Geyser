<img src="https://geysermc.org/img/geyser-1760-860.png" alt="Geyser" width="600"/>

[![License: MIT](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
[![Discord](https://img.shields.io/discord/613163671870242838.svg?color=%237289da&label=discord)](https://discord.gg/geysermc)
[![Crowdin](https://badges.crowdin.net/e/51361b7f8a01644a238d0fe8f3bddc62/localized.svg)](https://translate.geysermc.org/)

Geyser is a bridge between Minecraft: Bedrock Edition and Minecraft: Java Edition, closing the gap from those wanting to play true cross-platform.

Geyser is an [Open Collaboration](https://opencollaboration.dev/) project.

## MCXboxBroadcast NetherNet ingress

This fork includes a portal-style NetherNet ingress designed to pair with the
[MCXboxBroadcast standalone publisher](https://github.com/arti-inc/Broadcaster).
Geyser owns the Bedrock/NetherNet gameplay connection; MCXboxBroadcast only
publishes the Xbox session. Do not run a second Bedrock relay for the same
session.

### Minimal paired configuration

Install this fork as `Geyser-Spigot.jar` alongside Floodgate and ViaVersion on
Paper. In `config.yml`, set the portal bridge under
`advanced.bedrock.portal-bridge`:

```yaml
advanced:
  bedrock:
    portal-bridge:
      enabled: true
      xbox-auth-header-file: /absolute/path/to/mcxbox-standalone/cache/cache.json
      nether-net-network-id: ''
      shard-count: 1
      debug-logging: false
```

The auth-file must be the MCXboxBroadcast cache on the same trusted machine.
It is read without logging the token. Leave `nether-net-network-id` empty;
Geyser generates/persists the active ID and writes an atomic
`portal-session-status.json`. MCXboxBroadcast discovers that file and verifies
the ID before publishing a session.

Use `external-hosted: true` and an empty `external-network-id` in the
MCXboxBroadcast config. Start Paper/Geyser before the publisher, or use the
paired local launcher. The publisher waits for Geyser readiness, so manual
NetherNet ID copying is unnecessary.

For complete directory layout, friend-safety defaults, joining instructions,
and stage-by-stage troubleshooting, see the companion
[setup guide](https://github.com/arti-inc/Broadcaster#reliable-geyser--mcxboxbroadcast-setup).

The tested companion artifact is available from the
[NetherNet ingress release](https://github.com/arti-inc/Geyser-Nethernet-for-mcxb/releases/tag/nethernet-ingress-2).

## What is Geyser?
Geyser is a proxy, bridging the gap between Minecraft: Bedrock Edition and Minecraft: Java Edition servers.
The ultimate goal of this project is to allow Minecraft: Bedrock Edition users to join Minecraft: Java Edition servers as seamlessly as possible. However, due to the nature of Geyser translating packets over the network of two different games, *do not expect everything to work perfectly!*

Special thanks to the DragonProxy project for being a trailblazer in protocol translation and for all the team members who have joined us here!

## Supported Versions

| Edition | Supported Versions                                                                                  |
|---------|-----------------------------------------------------------------------------------------------------|
| Bedrock | 26.0, 26.1, 26.2, 26.3, 26.10, 26.20, 26.21, 26.22, 26.23, 26.30, 26.31, 26.32, 26.33, 26.34, 26.40 |
| Java    | 26.2 (For older versions, [see this guide](https://geysermc.org/wiki/geyser/supported-versions/))   |

## Setting Up
Take a look [here](https://geysermc.org/wiki/geyser/setup/) for how to set up Geyser.

## Links:
- Website: https://geysermc.org
- Docs: https://geysermc.org/wiki/geyser/
- Download: https://geysermc.org/download
- Discord: https://discord.gg/geysermc
- Donate: https://opencollective.com/geysermc
- Test Server: `test.geysermc.org` port `25565` for Java and `19132` for Bedrock

## What's Left to be Added/Fixed
- Near-perfect movement (to the point where anticheat on large servers is unlikely to ban you)
- Some Entity Flags

## What can't be fixed
There are a few things Geyser is unable to support due to various differences between Minecraft Bedrock and Java. For a list of these limitations, see the [Current Limitations](https://geysermc.org/wiki/geyser/current-limitations/) page.

## Compiling
1. Clone the repo to your computer
2. Navigate to the Geyser root directory and run `git submodule update --init --recursive`. This command downloads all the needed submodules for Geyser and is a crucial step in this process.
3. Run `gradlew build` and locate to `bootstrap/build` folder.


## Libraries Used:
- [Adventure Text Library](https://github.com/KyoriPowered/adventure)
- [CloudburstMC Bedrock Protocol Library](https://github.com/CloudburstMC/Protocol)
- [GeyserMC's Java Protocol Library](https://github.com/GeyserMC/MCProtocolLib)
- [TerminalConsoleAppender](https://github.com/Minecrell/TerminalConsoleAppender)
- [Simple Logging Facade for Java (slf4j)](https://github.com/qos-ch/slf4j)
