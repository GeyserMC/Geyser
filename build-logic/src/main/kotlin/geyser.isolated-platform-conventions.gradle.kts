import org.gradle.api.attributes.Bundling
import org.gradle.api.attributes.Usage

plugins {
    id("geyser.base-conventions")
    application
}

// Resolves to the shaded runtime jar of the base platform implementation
val platformBundle: Configuration = configurations.create("platformBundle") {
    isCanBeConsumed = false
    isCanBeResolved = true

    attributes {
        attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
        attribute(Bundling.BUNDLING_ATTRIBUTE, objects.named(Bundling.SHADOWED))
    }
}

dependencies {
    api(project(":isolation"))
    platformBundle(project(":${project.name}-base"))
}

val runtimeClasspath = configurations.named("runtimeClasspath")

tasks.named<Jar>("jar") {
    archiveBaseName = "Geyser-${project.name}"
    archiveVersion = ""
    archiveClassifier = ""

    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(runtimeClasspath.map { classpath -> classpath.map { if (it.isDirectory) it else zipTree(it) } })
    dependsOn(runtimeClasspath)

    from(platformBundle) {
        rename { "platform-base.jar" }
        into("bundled/")
    }
}
