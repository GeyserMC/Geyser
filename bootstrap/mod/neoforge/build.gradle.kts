plugins {
    application
}

architectury {
    platformSetupLoomIde()
    neoForge()
}

val common: Configuration by configurations.creating
val shadowCommon: Configuration by configurations.creating // Don't use shadow from the shadow plugin because we don't want IDEA to index this.
val developmentForge: Configuration = configurations.getByName("developmentNeoForge")
configurations {
    compileClasspath.get().extendsFrom(configurations["common"])
    runtimeClasspath.get().extendsFrom(configurations["common"])
    developmentForge.extendsFrom(configurations["common"])
}

dependencies {
    // See https://github.com/google/guava/issues/6618
    modules {
        module("com.google.guava:listenablefuture") {
          replacedBy("com.google.guava:guava", "listenablefuture is part of guava")
        }
    }

    neoForge(libs.neoforge.minecraft)

    //api(projects.mod)
    shadow(project(path = ":mod", configuration = "transformProductionNeoForge")) {
        isTransitive = false
    }
    shadow(projects.core) {
        exclude(group = "com.google.guava", module = "guava")
        exclude(group = "com.google.code.gson", module = "gson")
        exclude(group = "org.slf4j")
        exclude(group = "io.netty.incubator")
    }
    common(project(":mod", configuration = "namedElements")) { isTransitive = true }
    //shadowCommon(project(":mod", configuration = "transformProductionNeoForge")) { isTransitive = false }
}

application {
    mainClass.set("org.geysermc.geyser.platform.forge.GeyserNeoforgeMain")
}

tasks {
    shadowJar {
        relocate("it.unimi.dsi.fastutil", "org.geysermc.relocate.fastutil")
    }

    remapJar {
        archiveBaseName.set("Geyser-NeoForge")
    }

    remapModrinthJar {
        archiveBaseName.set("geyser-neoforge")
    }
}

modrinth {
    loaders.add("neoforge")
}