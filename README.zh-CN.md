<img src="https://geysermc.org/img/geyser-1760-860.png" alt="Geyser" width="600"/>

[![License: MIT](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
[![Discord](https://img.shields.io/discord/613163671870242838.svg?color=%237289da&label=discord)](https://discord.gg/geysermc)
[![Crowdin](https://badges.crowdin.net/e/51361b7f8a01644a238d0fe8f3bddc62/localized.svg)](https://translate.geysermc.org/)
[![English](https://img.shields.io/badge/English-docs-white)](README.md)

Geyser 是 Minecraft Java 版和基岩版之间的桥梁，为追求**真正**跨平台体验的玩家消除障碍，是 [CubeCraft Games ](https://cubecraft.net)的开放协作项目。

## Geyser 是什么？
Geyser 是一个沟通 Minecraft Java 版和基岩版的代理。

本项目的终极目标是让 Minecraft 基岩版玩家尽可能无缝地加入 Java 版服务器。

然而，由于 Geyser 毕竟是在转换两个游戏版本之间的网络协议，**请不要指望一切都能完美地运行！**

特别感谢 DragonProxy 项目在协议转换上的开拓性贡献，以及我们的全体成员！

## 支持的版本
Geyser 目前支持 Minecraft 基岩版 1.21.70 - 1.21.93 ，Java版 1.21.7 - 1.21.8。详见 [此处](https://geysermc.org/wiki/geyser/supported-versions/) 。

## 如何安装？
点击 [此处](https://geysermc.org/wiki/geyser/setup/) 了解如何安装Geyser。

## 链接：
- 官网： https://geysermc.org
- 文档： https://geysermc.org/wiki/geyser/
- 下载： https://geysermc.org/download
- Discord: https://discord.gg/geysermc
- 捐赠： https://opencollective.com/geysermc
- 测试服务器 `test.geysermc.org` ，Java 版端口：`25565`，基岩版端口： `19132`

## 尚需补充/修改之处
- 近乎完美的移动（大型服务器的反作弊系统不大可能封禁你）
- 部分实体状态标记 (Entity Flags)

## 无法修复之处
由于 Minecraft Java 版和基岩版之间的一些不同之处，Geyser 无法支持某些功能。详见 [目前的限制](https://geysermc.org/wiki/geyser/current-limitations/) 页面（英文版）。

## 编译指南
1. 将仓库 clone 到本地
2. 进入 Geyser 根目录，运行`git submodule update --init --recursive`，这将下载 Geyser 所需的所有子模块。这是 Geyser 编译过程中的关键步骤。
3. 运行 `gradlew build` ，编译结果位于 `bootstrap/build`  

## 参与贡献
我们欢迎所有贡献者。如您有兴趣帮助改进 Geyser，欢迎在 [Discord](https://discord.gg/geysermc) 上联系我们。

## 使用的核心库
- [Adventure Text Library](https://github.com/KyoriPowered/adventure)
- [CloudburstMC Bedrock Protocol Library](https://github.com/CloudburstMC/Protocol)
- [GeyserMC's Java Protocol Library](https://github.com/GeyserMC/MCProtocolLib)
- [TerminalConsoleAppender](https://github.com/Minecrell/TerminalConsoleAppender)
- [Simple Logging Facade for Java (slf4j)](https://github.com/qos-ch/slf4j)
