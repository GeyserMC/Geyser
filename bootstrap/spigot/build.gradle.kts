val paperVersion = "1.19-R0.1-SNAPSHOT"
val viaVersion = "4.0.0"
val adaptersVersion = "1.5-SNAPSHOT"
val commodoreVersion = "2.2"

dependencies {
    api(projects.core)

    implementation("org.geysermc.geyser.adapters", "spigot-all", adaptersVersion)

    implementation("me.lucko", "commodore", commodoreVersion)

    implementation("net.kyori", "adventure-text-serializer-bungeecord", Versions.adventurePlatformVersion)
    
    // Both paper-api and paper-mojangapi only provide Java 17 versions for 1.19
    compileOnly("io.papermc.paper", "paper-api", paperVersion) {
        attributes {
            attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, 17)
        }
    }
    compileOnly("io.papermc.paper", "paper-mojangapi", paperVersion) {
        attributes {
            attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, 17)
        }
    }
}

platformRelocate("it.unimi.dsi.fastutil")
platformRelocate("com.fasterxml.jackson")
// Relocate net.kyori but exclude the component logger
platformRelocate("net.kyori", "net.kyori.adventure.text.logger.slf4j.ComponentLogger")
platformRelocate("org.objectweb.asm")
platformRelocate("me.lucko.commodore")
platformRelocate("io.netty.channel.kqueue")

// These dependencies are already present on the platform
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