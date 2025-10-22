plugins {
    id("geyser.modded-conventions")
    id("geyser.modrinth-uploading-conventions")
}

//architectury {
//    platformSetupLoomIde()
//    fabric()
//}

loom {
    mods {
        create("geyser-fabric") {
            sourceSet(sourceSets.main.get())
            sourceSet("main", projects.mod)
            sourceSet("main", projects.core)
        }
    }
}

fabricApi {
    configureTests {
        createSourceSet = true
        modId = "geyser-gametest"
        enableClientGameTests = false
        eula = true
    }
}

dependencies {
    modImplementation(libs.fabric.loader)
    modApi(libs.fabric.api)

    api(project(":mod", configuration = "namedElements"))
    //shadowBundle(project(path = ":mod", configuration = "transformProductionFabric"))
    shadowBundle(projects.core)
    includeTransitive(projects.core)

    // These are NOT transitively included, and instead shadowed + relocated.
    // Avoids fabric complaining about non-SemVer versioning
    shadowBundle(libs.protocol.connection)
    shadowBundle(libs.protocol.common)
    shadowBundle(libs.protocol.codec)
    shadowBundle(libs.raknet)
    shadowBundle(libs.mcprotocollib)

    // Since we also relocate cloudburst protocol: shade erosion common
    shadowBundle(libs.erosion.common)

    // Let's shade in our own api/common module
    shadowBundle(projects.api)
    shadowBundle(projects.common)

    modImplementation(libs.cloud.fabric)
    include(libs.cloud.fabric)
    include(libs.fabric.permissions.api)
}

relocate("org.cloudburstmc.netty")
relocate("org.cloudburstmc.protocol")

tasks {
    jar {
        manifest.attributes["Main-Class"] = "org.geysermc.geyser.platform.fabric.GeyserFabricMain"
    }

    remapJar {
        archiveBaseName.set("Geyser-Fabric")
    }

    remapModrinthJar {
        archiveBaseName.set("geyser-fabric")
    }

    getByName("processGametestResources", ProcessResources::class) {
        filesMatching("fabric.mod.json") {
            expand(
                "id" to "geyser",
                "name" to "Geyser",
                "version" to project.version,
                "description" to project.description,
                "url" to "https://geysermc.org",
                "author" to "GeyserMC"
            )
        }
    }
}

modrinth {
    loaders.add("fabric")
    uploadFile.set(tasks.getByPath("remapModrinthJar"))
    dependencies {
        required.project("fabric-api")
    }
}
