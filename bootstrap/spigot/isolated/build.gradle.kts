plugins {
    id("geyser.isolated-platform-conventions")
    id("geyser.modrinth-uploading-conventions")
    alias(libs.plugins.runpaper)
}

tasks {
    jar {
        manifest.attributes["Main-Class"] = "org.geysermc.geyser.platform.spigot.SpigotMain"
    }

    shadowJar {
        archiveBaseName.set("Geyser-Spigot")
    }

    runServer {
        minecraftVersion(libs.versions.runpaperversion.get())
        jvmArgs("-Dcom.mojang.eula.agree=true")
    }
}

modrinth {
    uploadFile.set(tasks.getByPath("shadowJar"))
    gameVersions.addAll("1.20.5", "1.20.6", "1.21", "1.21.1", "1.21.2", "1.21.3", "1.21.4",
        "1.21.5", "1.21.6", "1.21.7", "1.21.8", "1.21.9", "1.21.10", "1.21.11", "26.1", "26.1.1", "26.1.2")
    loaders.addAll("spigot", "paper")
}
