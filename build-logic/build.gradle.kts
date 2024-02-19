plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()

    maven("https://repo.opencollab.dev/maven-snapshots/")
    maven("https://maven.fabricmc.net/")
    maven("https://maven.neoforged.net/releases")
    maven("https://maven.architectury.dev/")
}

dependencies {
    implementation(libs.indra)
    implementation(libs.shadow)
    implementation(libs.architectury.plugin)
    implementation(libs.architectury.loom)
    implementation(libs.minotaur)

    // Within the gradle plugin classpath, there is a version conflict between loom and some other
    // plugin for databind. This fixes it: minimum 2.13.2 is required by loom.
    implementation("com.fasterxml.jackson.core:jackson-databind:2.14.0")
}
