plugins {
    `java-library`
    `maven-publish`
}

dependencies {
    compileOnly("org.checkerframework", "checker-qual", "3.19.0")
}

tasks {
    processResources {
        // Spigot, BungeeCord, Velocity, Sponge, Fabric
        filesMatching(listOf("plugin.yml", "bungee.yml", "velocity-plugin.json", "META-INF/sponge_plugins.json", "fabric.mod.json")) {
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
    compileJava {
        options.encoding = Charsets.UTF_8.name()
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_16
    targetCompatibility = JavaVersion.VERSION_16

    withSourcesJar()
}
