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

// Opt-in local development against the sibling NetworkCompatible checkout:
// pass -PlocalNethernet to substitute the JitPack-pinned nethernet transport
// with the local build. Normal and release builds keep using the pinned hash.
if (extra.has("localNethernet")) {
    val networkCompatible = file("../NetworkCompatible")
    require(networkCompatible.isDirectory) {
        "-PlocalNethernet was set but ${networkCompatible.absolutePath} does not exist"
    }
    includeBuild(networkCompatible) {
        dependencySubstitution {
            substitute(module("com.github.SendableMetatype.NetworkCompatible:netty-transport-nethernet"))
                .using(project(":transport-nethernet"))
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
