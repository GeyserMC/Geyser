architectury {
    platformSetupLoomIde()
    forge()
}

loom {
    forge {
        mixinConfig("geyser_forge.mixins.json")
    }
}

dependencies {
    forge("net.minecraftforge:forge:1.19.4-45.0.63")

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
        dependsOn(shadowJar)
        inputFile.set(shadowJar.get().archiveFile)
        archiveBaseName.set("Geyser-Forge")
        archiveClassifier.set("")
        archiveVersion.set("")
    }
}