import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("geyser.base-conventions")
    id("com.gradleup.shadow")
}

shadow {
    addShadowVariantIntoJavaComponent = false
}

tasks {
    named<Jar>("jar") {
        from(project.rootProject.file("LICENSE"))
    }

    val shadowJar = named<ShadowJar>("shadowJar") {
        archiveBaseName.set(project.name)
        archiveVersion.set("")
        archiveClassifier.set("")
        
        val currentProjectName = project.name // Capture project name at config time

        dependencies {
            providedDependencies[currentProjectName]?.forEach { string ->
                println("Excluding $string from $currentProjectName") 
                exclude(dependency(string))
            }

            exclude(dependency("org.checkerframework:checker-qual:.*"))
            exclude(dependency("org.jetbrains:annotations:.*"))
        }
    }
    
    named("build") {
        dependsOn(shadowJar)
    }
}
