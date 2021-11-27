import java.io.ByteArrayOutputStream

plugins {
    id("net.kyori.blossom")
}

dependencies {
    api(project(":geyser-api"))
    api(project(":common"))
    compileOnly(project(":ap"))
    annotationProcessor(project(":ap"))
    api("com.fasterxml.jackson.core:jackson-annotations:2.12.4")
    api("com.fasterxml.jackson.core:jackson-core:2.12.4")
    api("com.fasterxml.jackson.core:jackson-databind:2.12.4")
    api("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.12.4")
    api("com.google.guava:guava:29.0-jre")
    api("com.nukkitx:nbt:2.1.0")
    api("com.nukkitx.fastutil:fastutil-int-int-maps:8.5.2")
    api("com.nukkitx.fastutil:fastutil-long-long-maps:8.5.2")
    api("com.nukkitx.fastutil:fastutil-int-byte-maps:8.5.2")
    api("com.nukkitx.fastutil:fastutil-int-boolean-maps:8.5.2")
    api("com.nukkitx.fastutil:fastutil-object-int-maps:8.5.2")
    api("com.nukkitx.fastutil:fastutil-object-object-maps:8.5.2")
    api("org.java-websocket:Java-WebSocket:1.5.1")
    api("com.github.CloudburstMC.Protocol:bedrock-v475:c22aa595")
    api("com.nukkitx.network:raknet:1.6.27-20210506.111625-1")
    api("com.github.RednedEpic:MCAuthLib:6c99331")
    api("com.github.GeyserMC:MCProtocolLib:f7f84e7")
    api("com.github.steveice10:packetlib:2.1-SNAPSHOT")
    api("io.netty:netty-resolver-dns:4.1.66.Final")
    api("io.netty:netty-resolver-dns-native-macos:4.1.66.Final")
    api("io.netty:netty-codec-haproxy:4.1.66.Final")
    api("io.netty:netty-handler:4.1.66.Final")
    api("io.netty:netty-transport-native-epoll:4.1.66.Final")
    api("io.netty:netty-transport-native-epoll:4.1.66.Final")
    api("io.netty:netty-transport-native-kqueue:4.1.66.Final")
    api("net.kyori:adventure-text-serializer-legacy:4.9.3")
    api("net.kyori:adventure-text-serializer-plain:4.9.3")
    testImplementation("junit:junit:4.13.1")
}

blossom {
    replaceToken("\$VERSION", project.version)
    replaceToken("\$GIT_VERSION", "git-Geyser-${project.version}:${gitCommitHash()}")
}

fun gitCommitHash(): String {
    val stdout = ByteArrayOutputStream()
    rootProject.exec {
        commandLine("git", "rev-parse", "--short", "HEAD")
        standardOutput = stdout
    }
    return stdout.toString(Charsets.UTF_8.name()).trim()
}

tasks.processResources {
    expand("name" to project.name)
    expand("version" to project.version)
}

description = "core"
