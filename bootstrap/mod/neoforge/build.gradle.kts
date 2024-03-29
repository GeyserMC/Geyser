plugins {
    application
}

// This is provided by "org.cloudburstmc.math.mutable" too, so yeet.
// NeoForge's class loader is *really* annoying.
provided("org.cloudburstmc.math", "api")

architectury {
    platformSetupLoomIde()
    neoForge()
}

val includeTransitive: Configuration = configurations.getByName("includeTransitive")

dependencies {
    // See https://github.com/google/guava/issues/6618
    modules {
        module("com.google.guava:listenablefuture") {
          replacedBy("com.google.guava:guava", "listenablefuture is part of guava")
        }
    }

    neoForge(libs.neoforge.minecraft)

    api(project(":mod", configuration = "namedElements"))
    shadow(project(path = ":mod", configuration = "transformProductionNeoForge")) {
        isTransitive = false
    }
    shadow(projects.core) { isTransitive = false }

    // Let's shade in our own api
    shadow(projects.api) { isTransitive = false }

    // Include all transitive deps of core via JiJ
    includeTransitive(projects.core)
}

application {
    mainClass.set("org.geysermc.geyser.platform.forge.GeyserNeoForgeMain")
}

tasks {
    remapJar {
        archiveBaseName.set("Geyser-NeoForge")
    }

    remapModrinthJar {
        archiveBaseName.set("geyser-neoforge")
    }
}

modrinth {
    loaders.add("neoforge")
}