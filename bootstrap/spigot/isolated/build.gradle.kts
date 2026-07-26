plugins {
    id("geyser.isolated-platform-conventions")
}

application {
    mainClass = "org.geysermc.geyser.platform.spigot.SpigotMain"
}

tasks {
    jar {
        archiveBaseName.set("Geyser-Spigot")
    }
}
