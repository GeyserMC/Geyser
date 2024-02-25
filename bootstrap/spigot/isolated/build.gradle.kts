plugins {
    java
}

dependencies {
    api(projects.isolation)
    compileOnlyApi(libs.velocity.api)
}

application {
    mainClass.set("org.geysermc.geyser.platform.spigot.SpigotMain")
}

tasks {
    jar {
        dependsOn(":spigot-base:build", configurations.runtimeClasspath)

        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })

        archiveBaseName = "geyser-${project.name}"
        archiveVersion = ""
        archiveClassifier = ""

        val libsDir = project.projects
            .spigotBase.dependencyProject
            .layout.buildDirectory.dir("libs")

        from(libsDir) {
            include("Geyser-Spigot.jar")
            rename("Geyser-Spigot.jar", "platform-base.jar")
            into("bundled/")
        }
    }
}