plugins {
    id("geyser.modded-conventions")
}

architectury {
    platformSetupLoomIde()
    fabric()
}

loom {
    mods {
        create("geyser-gametest") {
            sourceSet(sourceSets.main.get())
            sourceSet("main", projects.mod)
            sourceSet("main", projects.core)
        }
    }
}

dependencies {
    implementation(libs.fabric.loader)
    api(libs.fabric.api)
    api(project(":mod"))
    implementation(libs.cloud.fabric)
}

relocate("org.cloudburstmc.netty")
relocate("org.cloudburstmc.protocol")
relocate("org.spongepowered.configurate")

fabricApi {
    configureTests {
        modId = "geyser-gametest"
        enableClientGameTests = false
        eula = true
    }

    configureDataGeneration {
        client = true
    }
}

tasks {
    // We don't want to produce a jar!
    jar.get().enabled = false
    shadowJar.get().enabled = false
    mergeShadowAndJarJar.get().enabled = false

    withType(PublishToMavenRepository::class).configureEach {
        enabled = false
    }

    // it'd be processGametestResources if we had a separate source set for tests
    getByName("processResources", ProcessResources::class) {
        filesMatching("fabric.mod.json") {
            expand(
                "id" to "geyser",
                "name" to "Geyser",
                "version" to project.version,
                "description" to project.description!!,
                "url" to "https://geysermc.org",
                "author" to "GeyserMC"
            )
        }
    }
}
