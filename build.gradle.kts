plugins {
    `java-library`
    id("geyser.build-logic")
    id("io.freefair.lombok") version "6.3.0" apply false
}

allprojects {
    group = "org.geysermc.geyser"
    version = "2.1.0-SNAPSHOT"
    description = "Allows for players from Minecraft: Bedrock Edition to join Minecraft: Java Edition servers."

    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
    }
}

val basePlatforms = setOf(
    projects.bungeecord,
    projects.spigot,
    projects.sponge,
    projects.standalone,
    projects.velocity
).map { it.dependencyProject }

val moddedPlatforms = setOf(
    projects.fabric,
    projects.forge,
    projects.mod
).map { it.dependencyProject }

subprojects {
    apply {
        plugin("java-library")
        plugin("io.freefair.lombok")
        plugin("geyser.build-logic")
    }

    when (this) {
        in basePlatforms -> plugins.apply("geyser.platform-conventions")
        in moddedPlatforms -> plugins.apply("geyser.modded-conventions")
        else -> plugins.apply("geyser.base-conventions")
    }
}