plugins {
    `java-library`
    // Ensure AP works in eclipse (no effect on other IDEs)
    `eclipse`
    id("geyser.build-logic")
    id("io.freefair.lombok") version "6.3.0" apply false
}

allprojects {
    group = properties["group"] as String + "." + properties["id"] as String
    version = properties["version"] as String
    description = properties["description"] as String
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

val platforms = setOf(
    projects.fabric,
    projects.bungeecord,
    projects.spigot,
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
