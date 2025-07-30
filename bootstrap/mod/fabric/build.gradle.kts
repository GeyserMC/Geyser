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

    include("org.incendo:cloud-fabric:2.0.0-beta.11")
    include("org.incendo:cloud-fabric:2.0.0-beta.10")
    include("org.incendo:cloud-fabric:2.0.0-beta.9")
    include("org.incendo:cloud-fabric:2.0.0-beta.8")
    include("org.incendo:cloud-fabric:2.0.0-beta.7")
    include("org.incendo:cloud-fabric:2.0.0-beta.6")
    include("org.incendo:cloud-fabric:2.0.0-beta.5")
    include("org.incendo:cloud-fabric:2.0.0-beta.4")

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
