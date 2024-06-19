plugins {
    application
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
    shadow(project(path = ":mod", configuration = "transformProductionFabric")) {
        isTransitive = false
    }
    shadow(projects.core) { isTransitive = false }
    includeTransitive(projects.core)

    // These are NOT transitively included, and instead shadowed + relocated.
    // Avoids fabric complaining about non-SemVer versioning
    shadow(libs.protocol.connection) { isTransitive = false }
    shadow(libs.protocol.common) { isTransitive = false }
    shadow(libs.protocol.codec) { isTransitive = false }
    shadow(libs.mcauthlib) { isTransitive = false }
    shadow(libs.raknet) { isTransitive = false }

    // Consequences of shading + relocating mcauthlib: shadow/relocate mcpl!
    shadow(libs.mcprotocollib) { isTransitive = false }

    // Since we also relocate cloudburst protocol: shade erosion common
    shadow(libs.erosion.common) { isTransitive = false }

    // Let's shade in our own api/common module
    shadow(projects.api) { isTransitive = false }
    shadow(projects.common) { isTransitive = false }

    // Permissions
    modImplementation(libs.fabric.permissions)
    include(libs.fabric.permissions)
}

application {
    mainClass.set("org.geysermc.geyser.platform.fabric.GeyserFabricMain")
}

relocate("org.cloudburstmc.netty")
relocate("org.cloudburstmc.protocol")
relocate("com.github.steveice10.mc.auth")

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