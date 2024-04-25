dependencies {
    api(projects.core)
    implementation(libs.adventure.text.serializer.bungeecord)
    compileOnlyApi(libs.bungeecord.proxy)
}

platformRelocate("net.md_5.bungee.jni")
platformRelocate("com.fasterxml.jackson")
platformRelocate("io.netty.channel.kqueue") // This is not used because relocating breaks natives, but we must include it or else we get ClassDefNotFound
platformRelocate("net.kyori")
platformRelocate("org.yaml") // Broken as of 1.20

// These dependencies are already present on the platform
provided(libs.bungeecord.proxy)

application {
    mainClass.set("org.geysermc.geyser.platform.bungeecord.GeyserBungeeMain")
}

tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
    archiveBaseName.set("Geyser-BungeeCord")

    dependencies {
        exclude(dependency("com.google.*:.*"))
        exclude(dependency("io.netty.incubator:.*"))
        exclude(dependency("io.netty:netty-transport-native-epoll:.*"))
        exclude(dependency("io.netty:netty-transport-native-unix-common:.*"))
        exclude(dependency("io.netty:netty-handler:.*"))
        exclude(dependency("io.netty:netty-common:.*"))
        exclude(dependency("io.netty:netty-buffer:.*"))
        exclude(dependency("io.netty:netty-resolver:.*"))
        exclude(dependency("io.netty:netty-transport:.*"))
        exclude(dependency("io.netty:netty-codec:.*"))
        exclude(dependency("io.netty:netty-resolver-dns:.*"))
    }
}
