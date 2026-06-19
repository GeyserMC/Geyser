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
}
