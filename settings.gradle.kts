@file:Suppress("UnstableApiUsage")

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

dependencyResolutionManagement {
    repositories {
        // mavenLocal()
        
        // Floodgate, Cumulus etc.
        maven("https://repo.opencollab.dev/main")

        // Paper, Velocity
        maven("https://repo.papermc.io/repository/maven-public")
        // Spigot
        maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots") {
            mavenContent { snapshotsOnly() }
        }

        // BungeeCord
        maven("https://oss.sonatype.org/content/repositories/snapshots") {
            mavenContent { snapshotsOnly() }
        }

        // NeoForge
        maven("https://maven.neoforged.net/releases") {
            mavenContent { releasesOnly() }
        }

        // Minecraft
        maven("https://libraries.minecraft.net") {
            name = "minecraft"
            mavenContent { releasesOnly() }
        }

        mavenCentral()

        // ViaVersion
        maven("https://repo.viaversion.com") {
            name = "viaversion"
        }

        maven("https://jitpack.io") {
            content { includeGroupByRegex("com\\.github\\..*") }
        }

        // For Adventure snapshots
        maven("https://s01.oss.sonatype.org/content/repositories/snapshots/")
    }
}

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

include(":ap")
include(":api")
include(":bungeecord")
include(":fabric")
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
project(":neoforge").projectDir = file("bootstrap/mod/neoforge")
project(":mod").projectDir = file("bootstrap/mod")
project(":spigot").projectDir = file("bootstrap/spigot")
project(":standalone").projectDir = file("bootstrap/standalone")
project(":velocity").projectDir = file("bootstrap/velocity")
project(":viaproxy").projectDir = file("bootstrap/viaproxy")
