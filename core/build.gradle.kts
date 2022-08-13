import net.kyori.blossom.BlossomExtension
import net.kyori.indra.git.IndraGitExtension

plugins {
    id("net.kyori.blossom")
    id("net.kyori.indra.git")
    id("geyser.publish-conventions")
}

dependencies {
    api(projects.geyserApi)
    api(projects.common)

    // Jackson JSON and YAML serialization
    api("com.fasterxml.jackson.core", "jackson-annotations", Versions.jacksonVersion)
    api("com.fasterxml.jackson.core", "jackson-databind", Versions.jacksonVersion + ".1") // Extra .1 as databind is a slightly different version
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
    implementation("com.nukkitx.fastutil", "fastutil-object-reference-maps", Versions.fastutilVersion)

    // Network libraries
    implementation("org.java-websocket", "Java-WebSocket", Versions.websocketVersion)

    api("com.github.CloudburstMC.Protocol", "bedrock-v544", Versions.protocolVersion) {
        exclude("com.nukkitx.network", "raknet")
        exclude("com.nukkitx", "nbt")
    }

    api("com.github.GeyserMC", "MCAuthLib", Versions.mcauthlibVersion)
    api("com.github.GeyserMC", "MCProtocolLib", Versions.mcprotocollibversion) {
        exclude("com.github.GeyserMC", "packetlib")
        exclude("com.github.GeyserMC", "mcauthlib")
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

    // Test
    testImplementation("junit", "junit", Versions.junitVersion)

    // Annotation Processors
    compileOnly(projects.ap)

    annotationProcessor(projects.ap)
}

configure<BlossomExtension> {
    val indra = the<IndraGitExtension>()

    val mainFile = "src/main/java/org/geysermc/geyser/GeyserImpl.java"
    val gitVersion = "git-${branchName()}-${indra.commit()?.name?.substring(0, 7) ?: "0000000"}"

    replaceToken("\${version}", "${project.version} ($gitVersion)", mainFile)
    replaceToken("\${gitVersion}", gitVersion, mainFile)
    replaceToken("\${buildNumber}", buildNumber(), mainFile)
    replaceToken("\${branch}", branchName(), mainFile)
}

fun Project.branchName(): String =
        System.getenv("GIT_BRANCH") ?: "local/dev"

fun Project.buildNumber(): Int =
    Integer.parseInt(System.getenv("BUILD_NUMBER") ?: "-1")