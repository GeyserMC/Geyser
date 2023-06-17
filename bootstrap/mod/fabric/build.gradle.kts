plugins {
    application
}

architectury {
    platformSetupLoomIde()
    fabric()
}

dependencies {
    modImplementation(libs.fabric.loader)
    modApi(libs.fabric.api)

    api(projects.mod)
    shadow(project(path = ":mod", configuration = "transformProductionFabric")) {
        isTransitive = false
    }
    shadow(projects.core) {
        exclude(group = "com.google.guava", module = "guava")
        exclude(group = "com.google.code.gson", module = "gson")
        exclude(group = "org.slf4j")
        exclude(group = "com.nukkitx.fastutil")
        exclude(group = "io.netty.incubator")
    }

    // This should be in the libs TOML, but something about modImplementation AND include just doesn't work
    include(modImplementation("me.lucko", "fabric-permissions-api", "0.2-SNAPSHOT"))
}

repositories {
    mavenLocal()

    maven("https://repo.opencollab.dev/maven-releases/")
    maven("https://repo.opencollab.dev/maven-snapshots/")
    maven("https://jitpack.io")
    maven("https://oss.sonatype.org/content/repositories/snapshots/")
    maven("https://s01.oss.sonatype.org/content/repositories/snapshots/")
}

application {
    mainClass.set("org.geysermc.geyser.platform.fabric.GeyserFabricMain")
}

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