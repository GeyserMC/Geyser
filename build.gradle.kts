plugins {
    `java-library`
    // Ensure AP works in eclipse (no effect on other IDEs)
    eclipse
    id("geyser.build-logic")
    alias(libs.plugins.lombok) apply false
}

allprojects {
    group = properties["group"] as String + "." + properties["id"] as String
    version = properties["version"] as String
    description = properties["description"] as String
}

val basePlatforms = setOf(
    projects.bungeecord,
    projects.spigot,
    projects.standalone,
    projects.velocity,
    projects.viaproxy
).map { it.dependencyProject }

val moddedPlatforms = setOf(
    projects.fabric,
    projects.neoforge,
    projects.mod
).map { it.dependencyProject }

val modrinthPlatforms = setOf(
    projects.bungeecord,
    projects.fabric,
    projects.neoforge,
    projects.spigot,
    projects.velocity
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

    // Not combined with platform-conventions as that also contains
    // platforms which we cant publish to modrinth
    if (modrinthPlatforms.contains(this)) {
        plugins.apply("geyser.modrinth-uploading-conventions")
    }
}