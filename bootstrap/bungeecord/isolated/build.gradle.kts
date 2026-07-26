plugins {
    id("geyser.isolated-platform-conventions")
    id("geyser.modrinth-uploading-conventions")
}

dependencies {
    compileOnlyApi(libs.bungeecord.proxy)
    compileOnlyApi(libs.bungeecord.api)
}

tasks {
    jar {
        manifest.attributes["Main-Class"] = "org.geysermc.geyser.platform.bungeecord.BungeeMain"
    }

    shadowJar {
        archiveBaseName.set("Geyser-BungeeCord")
    }
}

modrinth {
    uploadFile.set(tasks.getByPath("shadowJar"))
    loaders.add("bungeecord")
}

