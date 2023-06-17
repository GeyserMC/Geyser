plugins {
    application
}

architectury {
    platformSetupLoomIde()
    forge()
}

loom {
    forge {
        mixinConfig("geyser.mixins.json")
    }
}

dependencies {
    forge(libs.forge.minecraft)

    api(projects.mod)
    shadow(project(path = ":mod", configuration = "transformProductionForge")) {
        isTransitive = false
    }
    shadow(projects.core) {
        exclude(group = "com.google.guava", module = "guava")
        exclude(group = "com.google.code.gson", module = "gson")
        exclude(group = "org.slf4j")
        exclude(group = "com.nukkitx.fastutil")
        exclude(group = "io.netty.incubator")
    }
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
    mainClass.set("org.geysermc.geyser.platform.forge.GeyserForgeMain")
}

tasks {
    remapJar {
        archiveBaseName.set("Geyser-Forge")
    }

    remapModrinthJar {
        archiveBaseName.set("geyser-forge")
    }
}

modrinth {
    loaders.add("forge")
}