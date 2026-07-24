plugins {
    id("geyser.publish-conventions")
    id("io.freefair.lombok")
}

// Major.minor + the next minor, for the mod metadata version ranges.
// Strips any patch/pre-release suffix (26.2.1 / 26.2-rc-2 -> 26.2).
val minecraftVersion = libs.minecraft.get().version as String
val minecraftLine = Regex("^\\d+\\.\\d+").find(minecraftVersion)?.value ?: minecraftVersion
val (minecraftMajor, minecraftMinor) = minecraftLine.split(".")
val minecraftNext = "$minecraftMajor.${minecraftMinor.toInt() + 1}"

tasks {
    processResources {
        // Spigot, BungeeCord, Velocity, Fabric, ViaProxy, NeoForge
        filesMatching(listOf("plugin.yml", "bungee.yml", "velocity-plugin.json", "fabric.mod.json", "viaproxy.yml", "META-INF/neoforge.mods.toml")) {
            expand(
                "id" to "geyser",
                "name" to "Geyser",
                "version" to project.version,
                // Must be this for gradle :neoforge:runServer task to work
                "file" to mapOf(
                    "jarVersion" to project.version
                ),
                "description" to project.description as String,
                "url" to "https://geysermc.org",
                "author" to "GeyserMC",
                "minecraft" to minecraftLine,
                "minecraftNext" to minecraftNext
            )
        }
    }
}
