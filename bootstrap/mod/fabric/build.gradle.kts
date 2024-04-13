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

    // Let's shade in our own api
    shadow(projects.api) { isTransitive = false }

    // Permissions
    modImplementation(libs.fabric.permissions)
    include(libs.fabric.permissions)
}

application {
    mainClass.set("org.geysermc.geyser.platform.fabric.GeyserFabricMain")
}

relocate("org.cloudburstmc.nbt")
relocate("org.cloudburstmc.netty")
relocate("org.cloudburstmc.protocol")
relocate("io.netty.handler.codec.dns")
relocate("io.netty.handler.codec.haproxy")
relocate("io.netty.resolver.dns.macos")
relocate("com.github.steveice10.mc.protocol")
relocate("com.github.steveice10.mc.auth")
relocate("com.github.steveice10.packetlib")

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
    dependencies {
        required.project("fabric-api")
    }
}