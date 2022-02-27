plugins {
    `java-library`
    `maven-publish`
}

dependencies {
    compileOnly("org.checkerframework", "checker-qual", Versions.checkerQualVersion)
}

tasks {
    processResources {
        filesMatching(listOf("plugin.yml", "bungee.yml", "velocity-plugin.json")) {
            expand(
                "id" to "Geyser",
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

publishing {
    publications.create<MavenPublication>("mavenJava") {
        groupId = project.group as String
        artifactId = project.name
        version = project.version as String

        artifact(tasks["jar"])
    }
}