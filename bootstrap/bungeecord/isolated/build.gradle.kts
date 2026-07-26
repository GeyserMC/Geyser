plugins {
    id("geyser.isolated-platform-conventions")
}

application {
    mainClass = "org.geysermc.geyser.platform.bungeecord.BungeeMain"
}

tasks {
    jar {
        archiveBaseName.set("Geyser-BungeeCord")
    }
}
