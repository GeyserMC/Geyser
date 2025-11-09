import org.eclipse.jgit.api.Git
import net.kyori.indra.git.RepositoryValueSource
import org.eclipse.jgit.revwalk.RevWalk
import java.util.Properties

plugins {
    // Allow blossom to mark sources root of templates
    idea
    eclipse
    alias(libs.plugins.blossom)
    id("geyser.publish-conventions")
    id("io.freefair.lombok")
}

dependencies {
    constraints {
        implementation(libs.raknet) // Ensure protocol does not override the RakNet version
    }

    api(projects.common)
    api(projects.api)

    // Jackson JSON and YAML serialization
    api(libs.bundles.jackson)
    api(libs.guava)

    // Fastutil Maps
    implementation(libs.bundles.fastutil)

    // Network libraries
    implementation(libs.websocket)

    api(libs.bundles.protocol)

    api(libs.minecraftauth)
    api(libs.mcprotocollib) {
        exclude("io.netty", "netty-all")
        exclude("net.raphimc", "MinecraftAuth")
    }

    implementation(libs.raknet) {
        exclude("io.netty", "*")
    }


    // Network dependencies we are updating ourselves
    api(libs.netty.handler)
    implementation(libs.netty.codec.haproxy)

    api(libs.netty.transport.native.epoll) { artifact { classifier = "linux-x86_64" } }
    implementation(libs.netty.transport.native.epoll) { artifact { classifier = "linux-aarch_64" } }
    // kqueue is macos only
    implementation(libs.netty.transport.native.kqueue) { artifact { classifier = "osx-x86_64" } }
    api(libs.netty.transport.native.io.uring) { artifact { classifier = "linux-x86_64" } }
    implementation(libs.netty.transport.native.io.uring) { artifact { classifier = "linux-aarch_64" } }

    // Adventure text serialization
    api(libs.bundles.adventure)

    // command library
    api(libs.cloud.core)

    api(libs.erosion.common) {
        isTransitive = false
    }

    // Test
    testImplementation(platform("org.junit:junit-bom:6.0.0"))
    testImplementation(libs.junit)
    testImplementation(libs.mockito)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    // Annotation Processors
    compileOnly(projects.ap)

    annotationProcessor(projects.ap)

    api(libs.events)
}

abstract class CommitMessageValueSource : RepositoryValueSource.Parameterless<String>() {
    override fun obtain(repository: Git): String? {
        val headCommitId = repository.repository.resolve("HEAD")

        if (headCommitId == null) {
            return ""
        }

        RevWalk(repository.repository).use { walk ->
            val commit = walk.parseCommit(headCommitId)
            return commit.fullMessage
        }
    }
}

abstract class RepositoryUrlValueSource : RepositoryValueSource.Parameterless<String>() {
    override fun obtain(repository: Git): String? {
        return repository.repository.config.getString("remote", "origin", "url")
    }
}

val gitBranch = indraGit.branchName().orElse("DEV")
val gitCommit = indraGit.commit()

val gitCommitName = gitCommit.map { it?.name ?: "0".repeat(40) }
val gitCommitAbbrev = gitCommit.map { it?.name?.substring(0, 7) ?: "0".repeat(7) }

val gitCommitMessage = indraGit.repositoryValue(CommitMessageValueSource::class.java).orElse("")
val gitRepositoryUrl = indraGit.repositoryValue(RepositoryUrlValueSource::class.java).orElse("").map {
    it.replace("git@github.com:", "https://github.com/")
}

val gitRepositoryIsDev = gitBranch.zip(gitRepositoryUrl) { branch, repo ->
    isDevBuild(branch, repo)
}

val gitVersion = gitBranch.zip(gitCommitAbbrev) { branch, commit ->
    "git-${branch}-${commit}"
}

val projectVersionProvider = gitRepositoryIsDev.map { isDev ->
    if (isDev) project.version.toString() else projectVersion(project).toString()
}

val finalVersion = projectVersionProvider.zip(gitVersion) { projVer, gitVer ->
    "$projVer ($gitVer)"
}

val buildNumber = provider { buildNumber().toString() }

val gitPropertiesMap = mapOf(
    "git.branch" to gitBranch,
    "git.build.number" to buildNumber,
    "git.build.version" to finalVersion,
    "git.commit.id" to gitCommitName,
    "git.commit.id.abbrev" to gitCommitAbbrev,
    "git.commit.message.full" to gitCommitMessage,
    "git.remote.origin.url" to gitRepositoryUrl
)

// Configuration cache compliant
tasks.processResources {
    val generatedPropsFile = layout.buildDirectory.file("generated/git/git.properties")
    inputs.properties(gitPropertiesMap)
    outputs.file(generatedPropsFile)
    doLast {
        val props = Properties()
        gitPropertiesMap.forEach { (key, provider) ->
            props[key] = provider.get()
        }
        generatedPropsFile.get().asFile.apply {
            parentFile.mkdirs()
            writer().use { props.store(it, null) }
        }
    }
}

tasks.named("sourcesJar") {
    dependsOn(tasks.named("processResources"))
}

sourceSets {
    main {
        blossom {
            javaSources {
                property("version", finalVersion)
                property("gitVersion", gitVersion)
                property("buildNumber", buildNumber)
                property("branch", gitBranch)
                property("commit", gitCommitName)
                property("repository", gitRepositoryUrl)
                property("devVersion", gitRepositoryIsDev.map { it.toString() })
            }
        }
        resources {
            srcDir(layout.buildDirectory.dir("generated/git"))
        }
    }
}

fun isDevBuild(branch: String, repository: String): Boolean {
    return branch != "master" || repository.equals("https://github.com/GeyserMC/Geyser", ignoreCase = true).not()
}

// Manual task to download the bedrock data files from the CloudburstMC/Data repository
// Invoke with ./gradlew :core:downloadBedrockData --suffix=1_20_70
// Set suffix to the current Bedrock version
tasks.register<DownloadFilesTask>("downloadBedrockData") {
    urls = listOf(
        "https://raw.githubusercontent.com/CloudburstMC/Data/master/entity_identifiers.dat",
        "https://raw.githubusercontent.com/CloudburstMC/Data/master/block_palette.nbt",
        "https://raw.githubusercontent.com/CloudburstMC/Data/master/creative_items.json",
        "https://raw.githubusercontent.com/CloudburstMC/Data/master/runtime_item_states.json",
        "https://raw.githubusercontent.com/CloudburstMC/Data/master/stripped_biome_definitions.json"
    )
    suffixedFiles = listOf("block_palette.nbt", "creative_items.json", "runtime_item_states.json")

    destinationDir = "$projectDir/src/main/resources/bedrock"
}
