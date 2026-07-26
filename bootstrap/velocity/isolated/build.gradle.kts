plugins {
    id("geyser.isolated-platform-conventions")
    id("geyser.modrinth-uploading-conventions")
    alias(libs.plugins.runvelocity)
}

// todo is this also provided through platformBundle?
dependencies {
    compileOnly(libs.velocity.api)
}

tasks {
    jar {
        manifest.attributes["Main-Class"] = "org.geysermc.geyser.platform.velocity.VelocityMain"
    }

    shadowJar {
        archiveBaseName.set("Geyser-Velocity")
        dependencies {
            exclude(dependency(libs.geantyref))
        }
    }

    runVelocity {
        version(libs.versions.runvelocityversion.get())
    }
}

modrinth {
    uploadFile.set(tasks.getByPath("shadowJar"))
    loaders.addAll("velocity")
}
