dependencies {
    api(projects.core)
}

platformRelocate("com.fasterxml.jackson")
platformRelocate("io.netty")
platformRelocate("it.unimi.dsi.fastutil")
platformRelocate("com.google.common")
platformRelocate("com.google.guava")
platformRelocate("net.kyori.adventure.text.serializer.gson.legacyimpl")
platformRelocate("net.kyori.adventure.nbt")

// These dependencies are already present on the platform
provided(libs.sponge.api)

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

        // Exclude all Kyori dependencies except the legacy NBT serializer and NBT
        exclude(dependency("net.kyori:adventure-api:.*"))
        exclude(dependency("net.kyori:examination-api:.*"))
        exclude(dependency("net.kyori:examination-string:.*"))
        exclude(dependency("net.kyori:adventure-text-serializer-gson:.*"))
        exclude(dependency("net.kyori:adventure-text-serializer-legacy:.*"))
        exclude(dependency("net.kyori:adventure-text-serializer-plain:.*"))
        exclude(dependency("net.kyori:adventure-key:.*"))
    }
}