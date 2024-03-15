plugins {
    application
}

architectury {
    platformSetupLoomIde()
    fabric()
}

val includeTransitive: Configuration = configurations.getByName("includeTransitive")

dependencies {
    modImplementation(libs.fabric.loader)
    modApi(libs.fabric.api)

    api(project(":mod", configuration = "namedElements"))
    shadow(project(path = ":mod", configuration = "transformProductionFabric")) {
        isTransitive = false
    }

    includeTransitive(projects.core)
    shadow(project(path = ":core")) { isTransitive = false }

    modImplementation(libs.fabric.permissions)
    include(libs.fabric.permissions)
}

application {
    mainClass.set("org.geysermc.geyser.platform.fabric.GeyserFabricMain")
}

tasks {
    remapJar {
        archiveBaseName.set("Geyser-Fabric")
    }

    remapModrinthJar {
        archiveBaseName.set("geyser-fabric")
    }
}

modrinth {
    loaders.add("fabric")
    dependencies {
        required.project("fabric-api")
    }
}