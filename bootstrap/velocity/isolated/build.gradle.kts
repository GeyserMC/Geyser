plugins {
    java
}

dependencies {
    api(projects.isolation)
    compileOnlyApi(libs.velocity.api)
}

application {
    mainClass.set("org.geysermc.geyser.platform.velocity.VelocityMain")
}

tasks {
    jar {
        dependsOn(":velocity-base:build", configurations.runtimeClasspath)

        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })

        archiveBaseName = "geyser-${project.name}"
        archiveVersion = ""
        archiveClassifier = ""

        val libsDir = project.projects
            .velocityBase.dependencyProject
            .layout.buildDirectory.dir("libs")

        from(libsDir) {
            include("Geyser-Velocity.jar")
            rename("Geyser-Velocity.jar", "platform-base.jar")
            into("bundled/")
        }
    }
}