plugins {
    `kotlin-dsl`
}

repositories {
    maven("https://repo.stellardrift.ca/repository/snapshots/")
    gradlePluginPortal()
    maven("https://repo.opencollab.dev/maven-snapshots")
}

dependencies {
    implementation("net.kyori", "indra-common", "3.1.0-SNAPSHOT")
    implementation("com.github.johnrengelman", "shadow", "7.1.3-SNAPSHOT")

    // Within the gradle plugin classpath, there is a version conflict between loom and some other
    // plugin for databind. This fixes it: minimum 2.13.2 is required by loom.
    implementation("com.fasterxml.jackson.core:jackson-databind:2.14.0")
}
