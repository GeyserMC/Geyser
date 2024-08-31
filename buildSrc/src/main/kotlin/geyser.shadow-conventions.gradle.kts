import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("geyser.base-conventions")
    id("com.github.johnrengelman.shadow")
}

tasks {
    named<Jar>("jar") {
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

            sJar.dependencies {
                exclude(dependency("org.checkerframework:checker-qual:.*"))
                exclude(dependency("org.jetbrains:annotations:.*"))
            }
        }
    }
    named("build") {
        dependsOn(shadowJar)
    }
}