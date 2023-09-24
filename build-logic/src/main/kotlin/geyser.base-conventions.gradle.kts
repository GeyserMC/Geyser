plugins {
    `java-library`
    id("net.kyori.indra")
}

dependencies {
    compileOnly("org.checkerframework", "checker-qual", "3.19.0")
}

indra {
    github("GeyserMC", "Geyser") {
        ci(true)
        issues(true)
        scm(true)
    }
    mitLicense()

    javaVersions {
        target(17)
    }
}

tasks {
    processResources {
        // Spigot, BungeeCord, Velocity, Fabric
        filesMatching(listOf("plugin.yml", "bungee.yml", "velocity-plugin.json", "fabric.mod.json")) {
            expand(
                "id" to "geyser",
                "name" to "Geyser",
                "version" to project.version,
                "description" to project.description,
                "url" to "https://geysermc.org",
                "author" to "GeyserMC"
            )
        }
    }
}