repositories {
    // mavenLocal()

    mavenCentral()

    // Floodgate, Cumulus etc.
    maven("https://repo.opencollab.dev/main")

    // Paper, Velocity
    maven("https://repo.papermc.io/repository/maven-public")

    // Spigot
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots") {
        mavenContent { snapshotsOnly() }
    }

    // BungeeCord
    maven("https://oss.sonatype.org/content/repositories/snapshots") {
        mavenContent { snapshotsOnly() }
    }

    // NeoForge
    maven("https://maven.neoforged.net/releases") {
        mavenContent { releasesOnly() }
    }

    // Minecraft
    maven("https://libraries.minecraft.net") {
        name = "minecraft"
        mavenContent { releasesOnly() }
    }

    // ViaVersion
    maven("https://repo.viaversion.com") {
        name = "viaversion"
    }

    // Jitpack for e.g. MCPL
    maven("https://jitpack.io") {
        content { includeGroupByRegex("com\\.github\\..*") }
    }

    // For Adventure snapshots
    maven("https://s01.oss.sonatype.org/content/repositories/snapshots/")
}
