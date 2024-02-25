plugins {
    java
}

dependencies {
    api(projects.isolation)
    compileOnlyApi(libs.velocity.api)
}

application {
    mainClass.set("org.geysermc.geyser.platform.bungeecord.BungeeMain")
}

tasks {
    jar {
        dependsOn(":bungeecord-base:build", configurations.runtimeClasspath)

        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })

        archiveBaseName = "geyser-${project.name}"
        archiveVersion = ""
        archiveClassifier = ""

        val libsDir = project.projects
            .bungeecordBase.dependencyProject
            .layout.buildDirectory.dir("libs")

        from(libsDir) {
            include("Geyser-BungeeCord.jar")
            rename("Geyser-BungeeCord.jar", "platform-base.jar")
            into("bundled/")
        }
    }
}