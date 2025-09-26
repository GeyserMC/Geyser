plugins {
    id("geyser.modded-conventions")
    id("geyser.modrinth-uploading-conventions")
}

architectury {
    platformSetupLoomIde()
    fabric()
}

val includeTransitive: Configuration = configurations.getByName("includeTransitive")

dependencies {
    modImplementation(libs.fabric.loader)
    modApi(libs.fabric.api)

    api(project(":mod", configuration = "namedElements"))
    shadowBundle(project(path = ":mod", configuration = "transformProductionFabric"))
    shadowBundle(projects.core)
    includeTransitive(projects.core)

    // These are NOT transitively included, and instead shadowed + relocated.
    // Avoids fabric complaining about non-SemVer versioning
    // shadowBundle(libs.protocol.connection) TODO remove me when protocol adventure stuff has been fixed
    shadowBundle(libs.protocol.common)
    // shadowBundle(libs.protocol.codec) TODO remove me same as above
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

tasks.withType<Jar> {
    manifest.attributes["Main-Class"] = "org.geysermc.geyser.platform.fabric.GeyserFabricMain"
}

relocate("org.cloudburstmc.netty")
relocate("org.cloudburstmc.protocol")

tasks {
    remapJar {
        archiveBaseName.set("Geyser-Fabric")
    }

    remapModrinthJar {
        archiveBaseName.set("geyser-fabric")
    }
}

modrinth {
    loaders.add("fabric")
    uploadFile.set(tasks.getByPath("remapModrinthJar"))
    dependencies {
        required.project("fabric-api")
    }
}
