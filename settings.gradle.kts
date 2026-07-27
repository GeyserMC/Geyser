@file:Suppress("UnstableApiUsage")

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        gradlePluginPortal()

        maven("https://repo.opencollab.dev/maven-snapshots")
        maven("https://maven.fabricmc.net")
        maven("https://maven.architectury.dev")
        maven("https://maven.neoforged.net/releases")
    }
    includeBuild("build-logic")
}

rootProject.name = "geyser-parent"

include(":ap")
include(":api")
include(":isolation")
include(":gametest")
include(":core")

include(":standalone")
project(":standalone").projectDir = file("bootstrap/standalone")

include(":mod")
project(":mod").projectDir = file("bootstrap/mod")
project(":gametest").projectDir = file("bootstrap/mod/gametest")

//todo see what's possible with fabric
include(":fabric")
project(":fabric").projectDir = file("bootstrap/mod/fabric")
include(":neoforge")
project(":neoforge").projectDir = file("bootstrap/mod/neoforge")

arrayOf("bungeecord", "spigot", "velocity", "viaproxy").forEach { platform ->
    arrayOf("base", "isolated").forEach {
        var id = ":$platform-$it"
        // isolated is the new default
        if (id.endsWith("-isolated")) id = ":$platform"

        include(id)
        project(id).projectDir = file("bootstrap/$platform/$it")
    }
}

// Allow to download JVMs for toolchains
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version ("1.0.0")
}
