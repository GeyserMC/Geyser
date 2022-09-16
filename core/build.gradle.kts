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

    // Network libraries
    implementation("org.java-websocket", "Java-WebSocket", Versions.websocketVersion)

    api("com.github.CloudburstMC.Protocol", "bedrock-v553", Versions.protocolVersion) {
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

tasks.processResources {
    // This is solely for backwards compatibility for other programs that used this file before the switch to gradle.
    // It used to be generated by the maven Git-Commit-Id-Plugin
    filesMatching("git.properties") {
        val info = GitInfo()
        expand(
            "branch" to info.branch,
            "buildNumber" to info.buildNumber,
            "projectVersion" to project.version,
            "commit" to info.commit,
            "commitAbbrev" to info.commitAbbrev,
            "commitMessage" to info.commitMessage,
            "repository" to info.repository
        )
    }
}

configure<BlossomExtension> {
    val mainFile = "src/main/java/org/geysermc/geyser/GeyserImpl.java"
    val info = GitInfo()

    replaceToken("\${version}", "${project.version} (${info.gitVersion})", mainFile)
    replaceToken("\${gitVersion}", info.gitVersion, mainFile)
    replaceToken("\${buildNumber}", info.buildNumber, mainFile)
    replaceToken("\${branch}", info.branch, mainFile)
    replaceToken("\${commit}", info.commit, mainFile)
    replaceToken("\${repository}", info.repository, mainFile)
}

fun Project.buildNumber(): Int =
    Integer.parseInt(System.getenv("BUILD_NUMBER") ?: "-1")

inner class GitInfo {
    val branch: String
    val commit: String
    val commitAbbrev: String

    val gitVersion: String
    val version: String
    val buildNumber: Int

    val commitMessage: String
    val repository: String

    init {
        // On Jenkins, a detached head is checked out, so indra cannot determine the branch.
        // Fortunately, this environment variable is available.
        branch = indraGit.branchName() ?: System.getenv("GIT_BRANCH") ?: "DEV"

        val commit = indraGit.commit()
        this.commit = commit?.name ?: "0".repeat(40)
        commitAbbrev = commit?.name?.substring(0, 7) ?: "0".repeat(7)

        gitVersion = "git-${branch}-${commitAbbrev}"
        version = "${project.version} ($gitVersion)"
        buildNumber = buildNumber()

        val git = indraGit.git()
        commitMessage = git?.commit()?.message ?: ""
        repository = git?.repository?.config?.getString("remote", "origin", "url") ?: ""
    }
}
