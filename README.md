<img src="https://res.cloudinary.com/dnqnmpfbt/image/upload/v1776060273/Edugeyser_logo_with_vibrant_geyser_burst_tem6rz.png" alt="EduGeyser" width="600"/>

[![License: MIT](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)

# EduGeyser

EduGeyser is a fork of [Geyser](https://github.com/GeyserMC/Geyser) that lets Minecraft: Education Edition players join Minecraft: Java Edition servers. Bedrock Edition players can join the same server at the same time.

Website and documentation: [edugeyser.org](https://edugeyser.org)

## Features

- Education Edition players from any school or tenant can join. The server needs no per-tenant configuration.
- Player identity is verified through Microsoft Education Services.
- Three ways for Education players to connect, provided by the [bundled education extension](#bundled-education-extension): connection ID, join codes, and the Education Edition server list. An optional tenant whitelist restricts which organizations may join.
- Full [NetherNet](#nethernet) support next to RakNet, on every platform.
- Bedrock players can join from their Xbox friends list through the [MCXboxBroadcast](https://github.com/EduGeyser/MCXboxBroadcast) extension, which advertises the server's NetherNet endpoint.
- Education player skins are shown to Java players through the [EduGeyser Signing Relay](https://github.com/EduGeyser/EduGeyser-Signing-Relay).
- [EduFloodgate](https://github.com/EduGeyser/EduFloodgate) gives Education players a Java identity on online-mode servers. [EduFloodgate-Modded](https://github.com/EduGeyser/EduFloodgate-Modded) covers Fabric and NeoForge servers.
- The UUID format for Education players is set in `uuid/scheme.yml`, which exists in the Geyser config folder and in the data folder of every EduFloodgate instance. The setting must be the same everywhere, or a player gets different UUIDs on different servers. `modern`, the default, derives the UUID from the identity verified by Microsoft. `legacy` derives it from the tenant and username only; it is insecure and should only be used if you have a specific reason to.

## NetherNet

NetherNet is Microsoft's successor to RakNet. It is the main transport of Education Edition, and Bedrock Edition has started using it for standard connections as well, so it is not an Education-only feature. EduGeyser supports NetherNet next to RakNet on every platform.

Connection ID, join code, and Xbox friends list joins run over Microsoft's signaling service and therefore always use NetherNet.

A server list entry only advertises an IP and port, which is the same as typing them into Bedrock's server list. For such direct connections the client tries three modes in order, and EduGeyser supports all three:

1. **NetherNet with HTTPS signaling.** EduGeyser obtains a certificate for the server's IP from Let's Encrypt automatically. You can also place your own `cert.pem` and `key.pem` in the `nethernet` folder next to the Geyser config; EduGeyser checks that they are valid.
2. **NetherNet with HTTP signaling**, used when no valid certificate is available or the client rejects the HTTPS connection. The server proves its identity with the key in `nethernet/identity-key.pem`. The client shows a trust prompt on the first connection and again whenever that key changes.
3. **RakNet**, if both NetherNet modes fail.

## Bundled Education Extension

The [Geyser Education Extension](https://github.com/EduGeyser/Geyser-Education-Extension) is embedded in EduGeyser itself, so there is nothing to download or install. On startup EduGeyser places the bundled copy into its extensions folder, replacing any older copy, so the extension always matches the EduGeyser build. It provides the Education-specific connection methods and their commands under `/edu`:

- **Connection ID**: a stable ID that players enter in Education Edition's join dialog. It works across all tenants and needs no account. EduGeyser prints it to the console on every startup.
- **Join codes**: codes or share links, each tied to one M365 Education tenant. Run `/edu joincode add` and sign in with an M365 Education account to create one.
- **Server list**: the server appears in Education Edition's own server browser. This requires Global Admin access to an M365 Education tenant.
- **Tenant whitelist**: optionally restricts which organizations may join. It is off by default.

The [Connection Methods](https://edugeyser.org/wiki/geyser/education/connection-methods) page on the website documents all of them.

## Supported Versions

| Edition   | Supported Versions                                                                                                                     |
|-----------|----------------------------------------------------------------------------------------------------------------------------------------|
| Education | 26.30 Preview, 26.32                                                                                                                   |
| Bedrock   | 26.0, 26.1, 26.2, 26.3, 26.10, 26.20, 26.21, 26.22, 26.23, 26.30, 26.31, 26.32, 26.33, 26.34, 26.40, 26.41, 26.42, 26.43, 26.44, 26.45 |
| Java      | 26.2 (For older versions, [see this guide](https://edugeyser.org/wiki/geyser/supported-versions/))                                     |

## Platforms

EduGeyser builds for Standalone, Velocity, BungeeCord, Spigot, ViaProxy, Fabric, and NeoForge. NetherNet is available on all of them.

## Downloads

Get the latest jars from the [download page](https://edugeyser.org/download) or from [GitHub Releases](https://github.com/EduGeyser/EduGeyser/releases).

## Setting Up

- [Education Setup](https://edugeyser.org/wiki/geyser/education/setup)
- [Connection Methods](https://edugeyser.org/wiki/geyser/education/connection-methods)
- [EduFloodgate](https://edugeyser.org/wiki/geyser/education/edufloodgate)
- [Troubleshooting and FAQ](https://edugeyser.org/wiki/geyser/education/troubleshooting)

The rest of the [wiki](https://edugeyser.org/wiki/) covers the Geyser features EduGeyser shares with upstream.

## Support

- Discord: https://edugeyser.org/discord
- Bug reports and feature requests: [GitHub Issues](https://github.com/EduGeyser/EduGeyser/issues)

## Related Repositories

| Repository | Purpose |
|------------|---------|
| [EduFloodgate](https://github.com/EduGeyser/EduFloodgate) | Floodgate fork for Education Edition support |
| [EduFloodgate-Modded](https://github.com/EduGeyser/EduFloodgate-Modded) | EduFloodgate for Fabric and NeoForge |
| [Geyser-Education-Extension](https://github.com/EduGeyser/Geyser-Education-Extension) | The education extension bundled in EduGeyser: connection IDs, join codes, server list, tenant whitelist |
| [MCXboxBroadcast](https://github.com/EduGeyser/MCXboxBroadcast) | Xbox friends list broadcasting |
| [EduGeyser-Signing-Relay](https://github.com/EduGeyser/EduGeyser-Signing-Relay) | Skin signing service for Education player skins |
| [NetworkM](https://github.com/EduGeyser/NetworkM) | RakNet and NetherNet transports |
| [webrtc-java](https://github.com/EduGeyser/webrtc-java) | WebRTC binding used by the NetherNet transport |
| [education-edition-support](https://github.com/EduGeyser/education-edition-support) | Bedrock protocol library with the Education Edition codecs |
| [EduGeyserWebsite](https://github.com/EduGeyser/EduGeyserWebsite) | Source of edugeyser.org |

## Compiling

1. Clone the repository.
2. Run `git submodule update --init --recursive` in the repository root. This downloads the submodules the build needs.
3. Build with JDK 25: `./gradlew build -PrequireBundledExtension` (on Windows `gradlew build -PrequireBundledExtension`).
4. The jars are in `bootstrap/<platform>/build/libs/EduGeyser-<Platform>.jar`. The Fabric and NeoForge jars are in `bootstrap/mod/<platform>/build/libs/`.

The education extension ships inside the jar: `bundled-extension/edu.jar` is embedded at build time and EduGeyser installs it into its extensions folder on startup. `-PrequireBundledExtension` fails the build if that jar is missing. Without the property the build still embeds the jar when it is present and only warns and continues when it is not.

To get a build of EduGeyser that does not install and start the extension, for example because you only want the Bedrock improvements and the NetherNet support, delete `bundled-extension/edu.jar` and build without `-PrequireBundledExtension`.

Two opt-in properties build against checkouts placed next to this one instead of the pinned versions:

- `-PlocalNetworkM` uses the sibling NetworkM checkout for both the RakNet and NetherNet transports. `-PlocalRaknet` and `-PlocalNethernet` are aliases for it. Set `-PnetworkMDir=/path/to/NetworkM` to use another checkout.
- `-PlocalProtocol` uses the sibling CloudburstProtocol checkout for the protocol modules.

They can be combined. Without them, builds use the pinned versions.

## Contributing

Contributions are welcome. Open an issue or a pull request on GitHub, or reach out on [Discord](https://edugeyser.org/discord).

## Credits

EduGeyser is a fork of [Geyser](https://github.com/GeyserMC/Geyser), an [Open Collaboration](https://opencollaboration.dev/) project.

Libraries used:

- [Adventure Text Library](https://github.com/KyoriPowered/adventure)
- [education-edition-support](https://github.com/EduGeyser/education-edition-support), a fork of the [CloudburstMC Bedrock Protocol Library](https://github.com/CloudburstMC/Protocol)
- [GeyserMC's Java Protocol Library](https://github.com/GeyserMC/MCProtocolLib)
- [NetworkM](https://github.com/EduGeyser/NetworkM), a fork of [CloudburstMC Network](https://github.com/CloudburstMC/Network)
- [webrtc-java](https://github.com/EduGeyser/webrtc-java), a fork of [devopvoid's webrtc-java](https://github.com/devopvoid/webrtc-java), optimized and slimmed down for data channel only usage
- [TerminalConsoleAppender](https://github.com/Minecrell/TerminalConsoleAppender)
- [Simple Logging Facade for Java (slf4j)](https://github.com/qos-ch/slf4j)

EduGeyser is licensed under the [MIT License](LICENSE).
