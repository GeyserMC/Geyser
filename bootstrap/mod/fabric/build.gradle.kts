plugins {
    id("geyser.modded-conventions")
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

    // The webrtc natives are classifier artifacts of the same module as the
    // webrtc-java api jar; the JiJ include logic keys on module id and drops
    // classifiers, so neither the natives nor (since shading them) the api
    // jar arrive on their own. Both must be explicit: the api jar shaded
    // flat, the natives shaded flat, or NetherNet dies at class load
    // (NoClassDefFoundError: io/github/sendablemetatype/webrtc/...) or native load.
    shadowBundle("io.github.sendablemetatype.webrtc:webrtc-java:${libs.versions.webrtc.java.get()}")
    val webrtcNativePlatforms = (findProperty("webrtcNatives") as? String)
        ?.split(',')?.map { it.trim() }?.filter { it.isNotEmpty() }
        ?: listOf("windows-x86_64", "windows-aarch64", "linux-x86_64", "linux-aarch64", "linux-aarch32", "macos-x86_64", "macos-aarch64")
    webrtcNativePlatforms.forEach { platform ->
        shadowBundle("io.github.sendablemetatype.webrtc:webrtc-java:${libs.versions.webrtc.java.get()}:$platform")
    }

    // These are NOT transitively included, and instead shadowed (+ relocated, if not under the org.geyser namespace).
    // Avoids fabric complaining about non-SemVer versioning
    shadowBundle(libs.protocol.connection)
    shadowBundle(libs.protocol.common)
    shadowBundle(libs.protocol.codec)
    shadowBundle(libs.raknet)
    shadowBundle(libs.nethernet)
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

relocate("io.github.sendablemetatype.netty")
relocate("org.cloudburstmc.protocol")
relocate("org.spongepowered.configurate")

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
        archiveBaseName.set("EduGeyser-Fabric")
    }
}

modrinth {
    loaders.add("fabric")
    dependencies {
        required.project("fabric-api")
    }
}
