plugins {
    id("geyser.modded-conventions")
}

architectury {
    platformSetupLoomIde()
    neoForge()
}

loom {
    mods {
        create("geyser-neoforge") {
            sourceSet(sourceSets.main.get())
            sourceSet("main", projects.mod)
            sourceSet("main", projects.core)
        }
    }
}

// This is provided by "org.cloudburstmc.math.mutable" too, so yeet.
// NeoForge's class loader is *really* annoying.
provided("org.cloudburstmc.math", "api")
provided("com.google.errorprone", "error_prone_annotations")

dependencies {
    // See https://github.com/google/guava/issues/6618
    modules {
        module("com.google.guava:listenablefuture") {
          replacedBy("com.google.guava:guava", "listenablefuture is part of guava")
        }
    }

    neoForge(libs.neoforge.minecraft)

    api(project(":mod"))
    shadowBundle(project(path = ":mod", configuration = "transformProductionNeoForge"))
    shadowBundle(projects.core)

    // Let's shade in our own api
    shadowBundle(projects.api)

    // shade + relocate these to avoid conflicts
    shadowBundle(libs.configurate.`interface`)
    shadowBundle(libs.configurate.yaml)
    shadowBundle(libs.configurate.core)

    // cannot be shaded, since neoforge will complain if floodgate-neoforge tries to provide this
    include(projects.common)

    // Include all transitive deps of core via JiJ
    includeTransitive(projects.core)

    // The webrtc natives are classifier artifacts of the same module as the
    // webrtc-java api jar; the JiJ include logic keys on module id and drops
    // classifiers, so neither the natives nor (since shading them) the api
    // jar arrive on their own. Both must be explicit: the api jar shaded
    // flat, the natives shaded flat, or NetherNet dies at class load
    // (NoClassDefFoundError: dev/kastle/webrtc/...) or native load.
    shadowBundle("dev.kastle.webrtc:webrtc-java:${libs.versions.webrtc.java.get()}")
    val webrtcNativePlatforms = (findProperty("webrtcNatives") as? String)
        ?.split(',')?.map { it.trim() }?.filter { it.isNotEmpty() }
        ?: listOf("windows-x86_64", "windows-aarch64", "linux-x86_64", "linux-aarch64", "macos-x86_64", "macos-aarch64")
    webrtcNativePlatforms.forEach { platform ->
        shadowBundle("dev.kastle.webrtc:webrtc-java:${libs.versions.webrtc.java.get()}:$platform")
    }

    implementation(libs.cloud.neoforge)
    include(libs.cloud.neoforge)
}

relocate("org.spongepowered.configurate")

tasks.withType<Jar> {
    manifest.attributes["Main-Class"] = "org.geysermc.geyser.platform.neoforge.GeyserNeoForgeMain"
}

tasks {
    named<Jar>("mergeShadowAndJarJar") {
        from (
            zipTree( shadowJar.map { it.outputs.files.singleFile } ).matching {
                exclude("LICENSE")
            },
            zipTree( jar.map { it.outputs.files.singleFile } ).matching {
                include("META-INF/jars/**")
                include("META-INF/jarjar/**")
                include("LICENSE")
            }
        )
        archiveBaseName.set("EduGeyser-NeoForge")
    }
}

modrinth {
    loaders.add("neoforge")
}
