import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
}

dependencies {
    implementation("net.kyori", "indra-common", "2.0.6")
    implementation("org.jfrog.buildinfo", "build-info-extractor-gradle", "4.26.1")
    implementation("gradle.plugin.com.github.johnrengelman", "shadow", "7.1.2") {
        exclude("org.ow2.asm", "*")
    }

    // Within the gradle plugin classpath, there is a version conflict between loom and some other
    // plugin for databind. This fixes it: minimum 2.13.2 is required by loom.
    implementation("com.fasterxml.jackson.core:jackson-databind:2.13.3")

    // Use a newer version of ObjectWeb ASM than the one provided by loom.
    implementation("org.ow2.asm:asm-commons:9.4")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = "16"
    }
}