plugins {
    id("geyser.modded-conventions")
    id("geyser.modrinth-uploading-conventions")
}

architectury {
    platformSetupLoomIde()
    fabric()
}

dependencies {
    modImplementation(libs.fabric.loader)
    modApi(libs.fabric.api)

    api(project(":mod", configuration = "namedElements"))
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

    modImplementation(libs.cloud.fabric)
    include(libs.cloud.fabric)
    include(libs.fabric.permissions.api)
}

tasks.withType<Jar> {
    manifest.attributes["Main-Class"] = "org.geysermc.geyser.platform.fabric.GeyserFabricMain"
}

relocate("org.cloudburstmc.netty")
relocate("org.cloudburstmc.protocol")
relocate("org.spongepowered.configurate")

tasks {
    remapJar {
        archiveBaseName.set("Geyser-Fabric")
    }

    remapModrinthJar {
        archiveBaseName.set("geyser-fabric")
    }

    shadowJar {
        mergeServiceFiles()
    }
}

modrinth {
    loaders.add("fabric")
    uploadFile.set(tasks.getByPath("remapModrinthJar"))
    dependencies {
        required.project("fabric-api")
    }
}
