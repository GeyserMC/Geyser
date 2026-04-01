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
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "geyser-parent"

include(":ap")
include(":api")
include(":spigot")
include(":common")
include(":core")

// Specify project dirs
project(":spigot").projectDir = file("bootstrap/spigot")
