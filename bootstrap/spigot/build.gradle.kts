val paperVersion = "1.17.1-R0.1-SNAPSHOT" // Needed because we do not support Java 17 yet
val viaVersion = "4.0.0"
val adaptersVersion = "1.4-SNAPSHOT"
val commodoreVersion = "1.13"

dependencies {
    api(projects.core)

    implementation("org.geysermc.geyser.adapters", "spigot-all", adaptersVersion)

    implementation("me.lucko", "commodore", commodoreVersion)
}

platformRelocate("it.unimi.dsi.fastutil")
platformRelocate("com.fasterxml.jackson")
platformRelocate("net.kyori")
platformRelocate("org.objectweb.asm")
platformRelocate("me.lucko.commodore")

// These dependencies are already present on the platform
provided("io.papermc.paper", "paper-api", paperVersion)
provided("io.papermc.paper", "paper-mojangapi", paperVersion)
provided("com.viaversion", "viaversion", viaVersion)

application {
    mainClass.set("org.geysermc.geyser.platform.spigot.GeyserSpigotMain")
}

tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
    archiveBaseName.set("Geyser-Spigot")

    dependencies {
        exclude(dependency("com.google.*:.*"))
        exclude(dependency("org.yaml:.*"))

        // We cannot shade Netty, or else native libraries will not load
        // Needed because older Spigot builds do not provide the haproxy module
        exclude(dependency("io.netty:netty-transport-native-epoll:.*"))
        exclude(dependency("io.netty:netty-transport-native-unix-common:.*"))
        exclude(dependency("io.netty:netty-transport-native-kqueue:.*"))
        exclude(dependency("io.netty:netty-handler:.*"))
        exclude(dependency("io.netty:netty-common:.*"))
        exclude(dependency("io.netty:netty-buffer:.*"))
        exclude(dependency("io.netty:netty-resolver:.*"))
        exclude(dependency("io.netty:netty-transport:.*"))
        exclude(dependency("io.netty:netty-codec:.*"))
        exclude(dependency("io.netty:netty-codec-dns:.*"))
        exclude(dependency("io.netty:netty-resolver-dns:.*"))
        exclude(dependency("io.netty:netty-resolver-dns-native-macos:.*"))

        // Commodore includes Brigadier
        exclude(dependency("com.mojang:.*"))
    }
}