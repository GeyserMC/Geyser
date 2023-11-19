plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()

    maven("https://repo.opencollab.dev/maven-snapshots/")
    maven("https://maven.fabricmc.net/")
    maven("https://maven.minecraftforge.net/")
    maven("https://maven.architectury.dev/")
}

dependencies {
    implementation("net.kyori", "indra-common", "3.1.1")
    implementation("com.github.johnrengelman", "shadow", "7.1.3-SNAPSHOT")
    implementation("architectury-plugin", "architectury-plugin.gradle.plugin", "3.4-SNAPSHOT")
    implementation("dev.architectury.loom", "dev.architectury.loom.gradle.plugin", "1.4-SNAPSHOT")
    implementation("com.modrinth.minotaur:Minotaur:2.7.5")

    // Within the gradle plugin classpath, there is a version conflict between loom and some other
    // plugin for databind. This fixes it: minimum 2.13.2 is required by loom.
    implementation("com.fasterxml.jackson.core:jackson-databind:2.14.0")
}
