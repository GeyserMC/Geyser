@file:Suppress("UnstableApiUsage")

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        gradlePluginPortal()

        maven("https://repo.opencollab.dev/maven-snapshots/")
        maven("https://maven.fabricmc.net/")
        maven("https://maven.architectury.dev/")
        maven("https://maven.neoforged.net/releases")
    }
    includeBuild("build-logic")
}

rootProject.name = "geyser-parent"

val localNetworkM = extra.has("localNetworkM") || extra.has("localRaknet") || extra.has("localNethernet")
if (localNetworkM) {
    val networkM = file(providers.gradleProperty("networkMDir").getOrElse("../NetworkM"))
    require(networkM.resolve("settings.gradle.kts").isFile) {
        "Local transport testing requires NetworkM at ${networkM.absolutePath}. " +
            "Set -PnetworkMDir=/path/to/NetworkM to use another checkout."
    }
    includeBuild(networkM) {
        dependencySubstitution {
            substitute(module("io.github.sendablemetatype.netty:netty-transport-raknet"))
                .using(project(":transport-raknet"))
            substitute(module("io.github.sendablemetatype.netty:netty-transport-nethernet"))
                .using(project(":transport-nethernet"))
        }
    }
}

// The renamed transport packages require a matching Protocol build.
if (extra.has("localProtocol") || localNetworkM) {
    val cloudburstProtocol = file("../CloudburstProtocol")
    require(cloudburstProtocol.isDirectory) {
        "Local Protocol sources are required at ${cloudburstProtocol.absolutePath}"
    }
    includeBuild(cloudburstProtocol) {
        dependencySubstitution {
            substitute(module("com.github.SendableMetatype.education-edition-support:common"))
                .using(project(":common"))
            substitute(module("com.github.SendableMetatype.education-edition-support:bedrock-codec"))
                .using(project(":bedrock-codec"))
            substitute(module("com.github.SendableMetatype.education-edition-support:bedrock-connection"))
                .using(project(":bedrock-connection"))
        }
    }
}

include(":ap")
include(":api")
include(":bungeecord")
include(":fabric")
include(":gametest")
include(":neoforge")
include(":mod")
include(":spigot")
include(":standalone")
include(":velocity")
include(":viaproxy")
include(":common")
include(":core")

// Specify project dirs
project(":bungeecord").projectDir = file("bootstrap/bungeecord")
project(":fabric").projectDir = file("bootstrap/mod/fabric")
project(":gametest").projectDir = file("bootstrap/mod/gametest")
project(":neoforge").projectDir = file("bootstrap/mod/neoforge")
project(":mod").projectDir = file("bootstrap/mod")
project(":spigot").projectDir = file("bootstrap/spigot")
project(":standalone").projectDir = file("bootstrap/standalone")
project(":velocity").projectDir = file("bootstrap/velocity")
project(":viaproxy").projectDir = file("bootstrap/viaproxy")

// Allow to download JVMs for toolchains
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version ("1.0.0")
}
