plugins {
    id("geyser.platform-conventions")
    id("geyser.modrinth-uploading-conventions")
    alias(libs.plugins.runvelocity)
}

dependencies {
    annotationProcessor(libs.velocity.api)
    api(projects.core)

    compileOnlyApi(libs.velocity.api)
    api(libs.cloud.velocity)
}

platformRelocate("com.fasterxml.jackson")
platformRelocate("it.unimi.dsi.fastutil")
platformRelocate("net.kyori.adventure.text.serializer.gson.legacyimpl")
platformRelocate("org.yaml")
platformRelocate("org.incendo")
platformRelocate("io.leangen.geantyref") // provided by cloud, should also be relocated

exclude("com.google.*:*")
exclude("io.netty:*")
exclude("org.slf4j:*")
exclude("org.ow2.asm:*")

// Exclude all Kyori dependencies except the legacy NBT serializer
exclude("net.kyori:adventure-api:*")
exclude("net.kyori:examination-api:*")
exclude("net.kyori:examination-string:*")
exclude("net.kyori:adventure-text-serializer-gson:*")
exclude("net.kyori:adventure-text-serializer-legacy:*")
exclude("net.kyori:adventure-nbt:*")
        
// These dependencies are already present on the platform
provided(libs.velocity.api)

tasks.withType<Jar> {
    manifest.attributes["Main-Class"] = "org.geysermc.geyser.platform.velocity.GeyserVelocityMain"
}

tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
    archiveBaseName.set("Geyser-Velocity")
}

modrinth {
    uploadFile.set(tasks.getByPath("shadowJar"))
    loaders.addAll("velocity")
}

tasks {
    runVelocity {
        version(libs.versions.runvelocityversion.get())
    }
}
