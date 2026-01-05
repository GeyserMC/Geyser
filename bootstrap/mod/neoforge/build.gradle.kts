plugins {
    id("geyser.modded-conventions")
    id("geyser.modrinth-uploading-conventions")
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

    api(project(":mod", configuration = "namedElements"))
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

    modImplementation(libs.cloud.neoforge)
    include(libs.cloud.neoforge)
}

relocate("org.spongepowered.configurate")

tasks.withType<Jar> {
    manifest.attributes["Main-Class"] = "org.geysermc.geyser.platform.neoforge.GeyserNeoForgeMain"
}

tasks {
    remapJar {
        archiveBaseName.set("Geyser-NeoForge")
    }

    remapModrinthJar {
        archiveBaseName.set("geyser-neoforge")
    }

    shadowJar {
        mergeServiceFiles()
    }
}

modrinth {
    loaders.add("neoforge")
    uploadFile.set(tasks.getByPath("remapModrinthJar"))
}
