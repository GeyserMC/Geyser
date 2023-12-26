plugins {
    application
}

architectury {
    platformSetupLoomIde()
    neoForge()
}

loom {
    neoForge {
        mixinConfig("geyser.mixins.json")
    }
}

dependencies {
    // See https://github.com/google/guava/issues/6618
    modules {
        module("com.google.guava:listenablefuture") {
          replacedBy("com.google.guava:guava", "listenablefuture is part of guava")
        }
    }

    neoForge(libs.neoforge.minecraft)

    api(projects.mod)
    shadow(project(path = ":mod", configuration = "transformProductionNeoForge")) {
        isTransitive = false
    }
    shadow(projects.core) {
        exclude(group = "com.google.guava", module = "guava")
        exclude(group = "com.google.code.gson", module = "gson")
        exclude(group = "org.slf4j")
        exclude(group = "io.netty.incubator")
    }
    implementation(libs.gson)
}

application {
    mainClass.set("org.geysermc.geyser.platform.forge.GeyserNeoforgeMain")
}

tasks {
    shadowJar {
        relocate("it.unimi.dsi.fastutil", "org.geysermc.relocate.fastutil")
    }

    remapJar {
        archiveBaseName.set("Geyser-Neoforge")
    }

    remapModrinthJar {
        archiveBaseName.set("geyser-neoforge")
    }
}

modrinth {
    loaders.add("neoforge")
}