dependencies {
    annotationProcessor(libs.velocity.api)
    api(projects.core)
}

platformRelocate("com.fasterxml.jackson")
platformRelocate("it.unimi.dsi.fastutil")
platformRelocate("net.kyori.adventure.text.serializer.gson.legacyimpl")

exclude("com.google.*:*")

// Needed because Velocity provides every dependency except netty-resolver-dns 
exclude("io.netty:netty-transport-native-epoll:*")
exclude("io.netty:netty-transport-native-unix-common:*")
exclude("io.netty:netty-transport-native-kqueue:*")
exclude("io.netty:netty-handler:*")
exclude("io.netty:netty-common:*")
exclude("io.netty:netty-buffer:*")
exclude("io.netty:netty-resolver:*")
exclude("io.netty:netty-transport:*")
exclude("io.netty:netty-codec:*")
exclude("io.netty:netty-codec-haproxy:*")
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

application {
    mainClass.set("org.geysermc.geyser.platform.velocity.GeyserVelocityMain")
}

tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
    archiveBaseName.set("Geyser-Velocity")

    dependencies {
        exclude(dependency("com.google.*:.*"))
        // Needed because Velocity provides every dependency except netty-resolver-dns
        exclude(dependency("io.netty:netty-transport-native-epoll:.*"))
        exclude(dependency("io.netty:netty-transport-native-unix-common:.*"))
        exclude(dependency("io.netty:netty-transport-native-kqueue:.*"))
        exclude(dependency("io.netty:netty-handler:.*"))
        exclude(dependency("io.netty:netty-common:.*"))
        exclude(dependency("io.netty:netty-buffer:.*"))
        exclude(dependency("io.netty:netty-resolver:.*"))
        exclude(dependency("io.netty:netty-transport:.*"))
        exclude(dependency("io.netty:netty-codec:.*"))
        exclude(dependency("io.netty:netty-codec-haproxy:.*"))
        exclude(dependency("org.slf4j:.*"))
        exclude(dependency("org.ow2.asm:.*"))
        // Exclude all Kyori dependencies except the legacy NBT serializer
        exclude(dependency("net.kyori:adventure-api:.*"))
        exclude(dependency("net.kyori:examination-api:.*"))
        exclude(dependency("net.kyori:examination-string:.*"))
        exclude(dependency("net.kyori:adventure-text-serializer-gson:.*"))
        exclude(dependency("net.kyori:adventure-text-serializer-legacy:.*"))
        exclude(dependency("net.kyori:adventure-nbt:.*"))
    }
}