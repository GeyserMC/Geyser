plugins {
    application
}

architectury {
    platformSetupLoomIde()
    forge()
}

loom {
    forge {
        mixinConfig("geyser.mixins.json")
    }
}

dependencies {
    forge(libs.forge.minecraft)

    api(projects.mod)
    shadow(project(path = ":mod", configuration = "transformProductionForge")) {
        isTransitive = false
    }
    shadow(projects.core) {
        exclude(group = "com.google.guava", module = "guava")
        //exclude(group = "com.google.code.gson", module = "gson")
        exclude(group = "org.slf4j")
        exclude(group = "io.netty.incubator")
    }
    implementation(libs.gson)
}

application {
    mainClass.set("org.geysermc.geyser.platform.forge.GeyserForgeMain")
}

tasks {
    shadowJar {
        relocate("it.unimi.dsi.fastutil", "org.geysermc.relocate.fastutil")
        relocate("com.google.gson", "org.geysermc.relocate.gson")
    }

    remapJar {
        archiveBaseName.set("Geyser-Forge")
    }

    remapModrinthJar {
        archiveBaseName.set("geyser-forge")
    }
}

modrinth {
    loaders.add("forge")
}