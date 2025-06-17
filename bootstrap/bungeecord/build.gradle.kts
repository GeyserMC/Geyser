plugins {
    id("geyser.platform-conventions")
    id("geyser.modrinth-uploading-conventions")
}

dependencies {
    api(projects.core)

    implementation(libs.cloud.bungee)
    implementation(libs.adventure.text.serializer.bungeecord)
    compileOnlyApi(libs.bungeecord.proxy)
}

platformRelocate("net.md_5.bungee.jni")
platformRelocate("com.fasterxml.jackson")
platformRelocate("net.kyori")
platformRelocate("org.incendo")
platformRelocate("io.leangen.geantyref") // provided by cloud, should also be relocated
platformRelocate("org.yaml") // Broken as of 1.20

// These dependencies are already present on the platform
provided(libs.bungeecord.proxy)

tasks.withType<Jar> {
    manifest.attributes["Main-Class"] = "org.geysermc.geyser.platform.bungeecord.GeyserBungeeMain"
}

tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
    archiveBaseName.set("Geyser-BungeeCord")

    dependencies {
        exclude(dependency("com.google.*:.*"))
        exclude(dependency("io.netty.*:.*"))
    }
}

modrinth {
    uploadFile.set(tasks.getByPath("shadowJar"))
    loaders.add("bungeecord")
}
