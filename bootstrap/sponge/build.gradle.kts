val spongeVersion = "7.1.0"

dependencies {
    api(projects.core)
}

platformRelocate("com.fasterxml.jackson")
platformRelocate("io.netty")
platformRelocate("it.unimi.dsi.fastutil")
platformRelocate("com.google.common")
platformRelocate("com.google.guava")
platformRelocate("net.kyori")

// Exclude these dependencies
exclude("com.google.code.gson:*")
exclude("org.yaml:*")
exclude("org.slf4j:*")
exclude("org.ow2.asm:*")

// These dependencies are already present on the platform
provided("org.spongepowered", "spongeapi", spongeVersion)

application {
    mainClass.set("org.geysermc.geyser.platform.sponge.GeyserSpongeMain")
}

tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
    archiveBaseName.set("Geyser-Sponge")

    dependencies {
        exclude(dependency("com.google.code.gson:.*"))
        exclude(dependency("org.yaml:.*"))
        exclude(dependency("org.slf4j:.*"))
        exclude(dependency("org.ow2.asm:.*"))
    }
}