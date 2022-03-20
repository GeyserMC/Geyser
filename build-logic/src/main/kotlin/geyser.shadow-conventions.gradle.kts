import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("geyser.base-conventions")
    id("com.github.johnrengelman.shadow")
    id("com.jfrog.artifactory")
}

tasks {
    named<Jar>("jar") {
        archiveClassifier.set("unshaded")
        from(project.rootProject.file("LICENSE"))
    }
    val shadowJar = named<ShadowJar>("shadowJar") {
        archiveBaseName.set(project.name)
        archiveVersion.set("")
        archiveClassifier.set("")

        val sJar: ShadowJar = this

        doFirst {
            providedDependencies[project.name]?.forEach { string ->
                sJar.dependencies {
                    println("Excluding $string from ${project.name}")
                    exclude(dependency(string))
                }
            }
        }
    }
    named("build") {
        dependsOn(shadowJar)
    }
}

publishing {
    publications.named<MavenPublication>("mavenJava") {
        artifact(tasks["shadowJar"])
        artifact(tasks["sourcesJar"])
    }
}

artifactory {
    publish {
        repository {
            setRepoKey("maven-snapshots")
            setMavenCompatible(true)
        }
        defaults {
            publishConfigs("archives")
            setPublishArtifacts(true)
            setPublishPom(true)
            setPublishIvy(false)
        }
    }
}