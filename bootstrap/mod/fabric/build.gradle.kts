plugins {
    id("geyser.modded-conventions")
    id("geyser.modrinth-uploading-conventions")
}

architectury {
    platformSetupLoomIde()
    fabric()
}

loom {
    mods {
        create("geyser-fabric") {
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
    shadowBundle(project(path = ":mod", configuration = "transformProductionFabric"))
    shadowBundle(projects.core)
    includeTransitive(projects.core)

    // These are NOT transitively included, and instead shadowed (+ relocated, if not under the org.geyser namespace).
    // Avoids fabric complaining about non-SemVer versioning
    shadowBundle(libs.protocol.connection)
    shadowBundle(libs.protocol.common)
    shadowBundle(libs.protocol.codec)
    shadowBundle(libs.raknet)
    shadowBundle(libs.mcprotocollib)

    // Shade + relocate configurate as we're using a fork
    shadowBundle(libs.configurate.`interface`)
    shadowBundle(libs.configurate.yaml)
    shadowBundle(libs.configurate.core)

    // Since we also relocate cloudburst protocol: shade erosion common
    shadowBundle(libs.erosion.common)

    // Let's shade in our own api/common module
    shadowBundle(projects.api)
    shadowBundle(projects.common)

    implementation(libs.cloud.fabric)
    include(libs.cloud.fabric)
    include(libs.fabric.permissions.api)
}

tasks.withType<Jar> {
    manifest.attributes["Main-Class"] = "org.geysermc.geyser.platform.fabric.GeyserFabricMain"
}

relocate("org.cloudburstmc.netty")
relocate("org.cloudburstmc.protocol")
relocate("org.spongepowered.configurate")

fabricApi {
    configureTests {
        createSourceSet = true
        modId = "geyser-gametest"
        enableClientGameTests = false
        eula = true
    }
}

tasks {
    named<Jar>("mergeShadowAndJarJar") {
        from (
            zipTree( shadowJar.map { it.outputs.files.singleFile } ).matching {
                exclude("fabric.mod.json")
                exclude("LICENSE")
            },
            zipTree( jar.map { it.outputs.files.singleFile } ).matching {
                include("META-INF/jars/**")
                include("fabric.mod.json")
                include("LICENSE")
            }
        )
        archiveBaseName.set("Geyser-Fabric")
    }

    getByName("processGametestResources", ProcessResources::class) {
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

modrinth {
    loaders.add("fabric")
    uploadFile.set(tasks.getByName("renameModrinthJar"))
    dependencies {
        required.project("fabric-api")
    }
}
