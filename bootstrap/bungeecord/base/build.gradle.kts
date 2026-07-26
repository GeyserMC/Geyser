plugins {
    id("geyser.platform-base-conventions")
}

dependencies {
    implementation(projects.core)
    implementation(libs.floodgate.bungee)
    implementation(libs.cloud.bungee)
    compileOnlyApi(libs.bungeecord.proxy) {
        isTransitive = false
    }
    compileOnlyApi(libs.bungeecord.api)
}

// TODO: figure out if yaml works correctly without falling back to bungee's version
// platformRelocate("org.yaml") // Broken as of 1.20

// These dependencies are already present on the platform
provided(libs.bungeecord.proxy)

tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
    archiveBaseName.set("Geyser-BungeeCord-Base")

    dependencies {
        exclude(dependency("com.google.*:.*"))
        exclude(dependency("io.netty.*:.*"))
    }
}
