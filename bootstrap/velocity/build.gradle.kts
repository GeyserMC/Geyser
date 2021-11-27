import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("com.github.johnrengelman.shadow")
}

dependencies {
    api(project(":core"))
    compileOnly("com.velocitypowered:velocity-api:3.0.0")
}

tasks.withType<ShadowJar> {
    dependencies {
        exclude(dependency("com.google.*:*"))
        // Needed because Velocity provides every dependency except netty-resolver-dns
        exclude(dependency("io.netty:netty-transport-native-epoll:*"))
        exclude(dependency("io.netty:netty-transport-native-unix-common:*"))
        exclude(dependency("io.netty:netty-transport-native-kqueue:*"))
        exclude(dependency("io.netty:netty-handler:*"))
        exclude(dependency("io.netty:netty-common:*"))
        exclude(dependency("io.netty:netty-buffer:*"))
        exclude(dependency("io.netty:netty-resolver:*"))
        exclude(dependency("io.netty:netty-transport:*"))
        exclude(dependency("io.netty:netty-codec:*"))
        exclude(dependency("io.netty:netty-codec-haproxy:*"))
        exclude(dependency("org.slf4j:*"))
        exclude(dependency("org.ow2.asm:*"))
        // Exclude all Kyori dependencies except the legacy NBT serializer
        exclude(dependency("net.kyori:adventure-api:*"))
        exclude(dependency("net.kyori:examination-api:*"))
        exclude(dependency("net.kyori:examination-string:*"))
        exclude(dependency("net.kyori:adventure-text-serializer-gson:*"))
        exclude(dependency("net.kyori:adventure-text-serializer-legacy:*"))
        exclude(dependency("net.kyori:adventure-nbt:*"))
    }

    println(destinationDirectory.get())
    archiveFileName.set("Geyser-Velocity.jar")
    println(archiveFileName.get())
}

description = "bootstrap-velocity"