import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
    maven("https://repo.opencollab.dev/maven-snapshots")
}

dependencies {
    implementation("net.kyori", "indra-common", "2.0.6")
    implementation("org.jfrog.buildinfo", "build-info-extractor-gradle", "4.26.1")
    implementation("com.github.johnrengelman", "shadow", "7.1.3-SNAPSHOT")

    // Within the gradle plugin classpath, there is a version conflict between loom and some other
    // plugin for databind. This fixes it: minimum 2.13.2 is required by loom.
    implementation("com.fasterxml.jackson.core:jackson-databind:2.13.3")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = "16"
    }
}