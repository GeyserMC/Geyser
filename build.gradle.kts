plugins {
    `java-library`
    id("geyser.build-logic")
    id("io.freefair.lombok") version "6.3.0" apply false
}

allprojects {
    group = "org.geysermc"
    version = "2.1.0-SNAPSHOT"
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

val api: Project = projects.api.dependencyProject

subprojects {
    apply {
        plugin("java-library")
        plugin("io.freefair.lombok")
        plugin("geyser.build-logic")
    }

    val relativePath = projectDir.relativeTo(rootProject.projectDir).path

    if (relativePath.contains("api")) {
        plugins.apply("geyser.api-conventions")
    } else {
        group = rootProject.group as String + ".geyser"
        when (this) {
            in platforms -> plugins.apply("geyser.platform-conventions")
            api -> plugins.apply("geyser.publish-conventions")
            else -> plugins.apply("geyser.base-conventions")
        }
    }
}