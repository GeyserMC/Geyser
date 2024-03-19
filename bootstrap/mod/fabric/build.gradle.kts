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
    // TODO: re-evaluate after loom 1.6 (https://github.com/FabricMC/fabric-loom/pull/1075)
    shadow(libs.protocol.connection) { isTransitive = false }
    shadow(libs.protocol.common) { isTransitive = false }
    shadow(libs.protocol.codec) { isTransitive = false }
    shadow(libs.mcauthlib) { isTransitive = false }
    shadow(libs.raknet) { isTransitive = false }
    shadow(libs.netty.codec.haproxy) { isTransitive = false }
    shadow("org.cloudburstmc:nbt:3.0.2.Final") { isTransitive = false }
    shadow("io.netty:netty-codec-dns:4.1.103.Final") { isTransitive = false }
    shadow("io.netty:netty-resolver-dns-classes-macos:4.1.103.Final") { isTransitive = false }

    // Consequences of shading + relocating mcauthlib: shadow/relocate mcpl!
    shadow(libs.mcprotocollib) { isTransitive = false }

    // Since we also relocate cloudburst protocol: shade erosion common
    shadow(libs.erosion.common) { isTransitive = false }

    // Permissions
    modImplementation(libs.fabric.permissions)
    include(libs.fabric.permissions)
}

application {
    mainClass.set("org.geysermc.geyser.platform.fabric.GeyserFabricMain")
}

tasks {
    shadowJar {
        relocate("org.cloudburstmc.nbt", "org.geysermc.relocate.cloudburst.nbt")
        relocate("org.cloudburstmc.netty", "org.geysermc.relocate.cloudburst.netty")
        relocate("org.cloudburstmc.protocol", "org.geysermc.relocate.cloudburst.protocol")
        relocate("io.netty.handler.codec.dns", "org.geysermc.relocate.netty.codec-dns")
        relocate("io.netty.handler.codec.haproxy", "org.geysermc.relocate.netty.codec-haproxy")
        relocate("io.netty.resolver.dns.macos", "org.geysermc.relocate.netty.dns-macos")
        relocate("com.github.steveice10.mc.protocol", "org.geysermc.relocate.mcpl")
        relocate("com.github.steveice10.mc.auth", "org.geysermc.relocate.authlib")
        relocate("com.github.steveice10.packetlib", "org.geysermc.relocate.packetlib")
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