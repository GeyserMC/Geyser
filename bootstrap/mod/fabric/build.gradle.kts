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
    shadow(libs.netty.codec.haproxy) { isTransitive = false }
    shadow("org.cloudburstmc:nbt:3.0.2.Final") { isTransitive = false }
    shadow("io.netty:netty-codec-dns:4.1.103.Final") { isTransitive = false }

    modImplementation(libs.fabric.permissions)
    include(libs.fabric.permissions)
}

application {
    mainClass.set("org.geysermc.geyser.platform.fabric.GeyserFabricMain")
}

tasks {
    shadowJar {
        relocate("org.cloudburstmc", "org.geysermc.relocate.cloudburst")
        relocate("com.github.steveice10.mc.auth", "org.geysermc.relocate.mcauth")
    }
    remapJar {
        archiveBaseName.set("Geyser-Fabric")
    }

    remapModrinthJar {
        archiveBaseName.set("geyser-fabric")
    }
}

modrinth {
    loaders.add("fabric")
    dependencies {
        required.project("fabric-api")
    }
}