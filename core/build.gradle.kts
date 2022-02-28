import net.kyori.indra.git.IndraGitExtension
import net.kyori.blossom.BlossomExtension

plugins {
    id("net.kyori.blossom")
    id("net.kyori.indra.git")
}

dependencies {
    api(projects.geyserApi)
    api(projects.common)

    // Jackson JSON and YAML serialization
    api("com.fasterxml.jackson.core", "jackson-annotations", Versions.jacksonVersion)
    api("com.fasterxml.jackson.core", "jackson-databind", Versions.jacksonVersion)
    api("com.fasterxml.jackson.dataformat", "jackson-dataformat-yaml", Versions.jacksonVersion)
    api("com.google.guava", "guava", Versions.guavaVersion)

    api("com.nukkitx", "nbt", Versions.nbtVersion)

    // Fastutil Maps
    implementation("com.nukkitx.fastutil", "fastutil-int-int-maps", Versions.fastutilVersion)
    implementation("com.nukkitx.fastutil", "fastutil-int-long-maps", Versions.fastutilVersion)
    implementation("com.nukkitx.fastutil", "fastutil-int-byte-maps", Versions.fastutilVersion)
    implementation("com.nukkitx.fastutil", "fastutil-int-boolean-maps", Versions.fastutilVersion)
    implementation("com.nukkitx.fastutil", "fastutil-object-int-maps", Versions.fastutilVersion)
    implementation("com.nukkitx.fastutil", "fastutil-object-object-maps", Versions.fastutilVersion)

    // Network libraries
    implementation("org.java-websocket", "Java-WebSocket", Versions.websocketVersion)

    api("com.github.CloudburstMC.Protocol", "bedrock-v486", Versions.protocolVersion) {
        exclude("com.nukkitx.network", "raknet")
        exclude("com.nukkitx", "nbt")
    }

    api("com.github.RednedEpic", "MCAuthLib", Versions.mcauthlibVersion)
    api("com.github.GeyserMC", "MCProtocolLib", Versions.mcprotocollibversion) {
        exclude("com.github.steveice10", "packetlib")
        exclude("com.github.steveice10", "mcauthlib")
    }

    api("com.github.steveice10", "packetlib", Versions.packetlibVersion) {
        exclude("io.netty", "netty-all")
        // This is still experimental - additionally, it could only really benefit standalone
        exclude("io.netty.incubator", "netty-incubator-transport-native-io_uring")
    }

    implementation("com.nukkitx.network", "raknet", Versions.raknetVersion) {
        exclude("io.netty", "*");
    }

    implementation("io.netty", "netty-resolver-dns", Versions.nettyVersion)
    implementation("io.netty", "netty-resolver-dns-native-macos", Versions.nettyVersion, null, "osx-x86_64")
    implementation("io.netty", "netty-codec-haproxy", Versions.nettyVersion)

    // Network dependencies we are updating ourselves
    api("io.netty", "netty-handler", Versions.nettyVersion)

    implementation("io.netty", "netty-transport-native-epoll", Versions.nettyVersion, null, "linux-x86_64")
    implementation("io.netty", "netty-transport-native-epoll", Versions.nettyVersion, null, "linux-aarch_64")
    implementation("io.netty", "netty-transport-native-kqueue", Versions.nettyVersion, null, "osx-x86_64")

    // Adventure text serialization
    implementation("net.kyori", "adventure-text-serializer-legacy", Versions.adventureVersion)
    implementation("net.kyori", "adventure-text-serializer-plain", Versions.adventureVersion)

    // Kyori Misc
    implementation("net.kyori", "event-api", Versions.eventVersion)

    // Test
    testImplementation("junit", "junit", Versions.junitVersion)

    // Annotation Processors
    api(projects.ap)

    annotationProcessor(projects.ap)
}

configure<BlossomExtension> {
    val indra = the<IndraGitExtension>()

    val mainFile = "src/main/java/org/geysermc/geyser/GeyserImpl.java"
    val gitVersion = "git-${indra.branch()?.name ?: "DEV"}-${indra.commit()?.name?.substring(0, 7) ?: "0000000"}"

    replaceToken("\${version}", "${project.version} ($gitVersion)", mainFile)
    replaceToken("\${gitVersion}", gitVersion, mainFile)
    replaceToken("\${buildNumber}", Integer.parseInt(System.getProperty("BUILD_NUMBER", "-1")), mainFile)
    replaceToken("\${branch}", indra.branch()?.name ?: "DEV", mainFile)
}