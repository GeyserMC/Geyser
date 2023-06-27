plugins {
    `java-library`
    id("geyser.build-logic")
    id("io.freefair.lombok") version "6.3.0" apply false
}

allprojects {
    group = "org.geysermc.geyser"
    version = "2.1.1-SNAPSHOT"
    description = "Allows for players from Minecraft: Bedrock Edition to join Minecraft: Java Edition servers."

    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
    }
}

val platforms = setOf(
    projects.fabric,
    projects.bungeecord,
    projects.spigot,
    projects.sponge,
    projects.standalone,
    projects.velocity
).map { it.dependencyProject }

subprojects {
    apply {
        plugin("java-library")
        plugin("io.freefair.lombok")
        plugin("geyser.build-logic")
    }

    when (this) {
        in platforms -> plugins.apply("geyser.platform-conventions")
        else -> plugins.apply("geyser.base-conventions")
    }
}