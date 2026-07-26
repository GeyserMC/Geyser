import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.attributes.Bundling
import org.gradle.api.attributes.Usage

plugins {
    id("geyser.base-conventions")
    id("geyser.platform-conventions")
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

tasks.named<ShadowJar>("shadowJar") {
    archiveBaseName = "Geyser-${project.name}"

    dependencies {
        // Strip out gson, provided by Minecraft
        exclude(dependency("com.google.*:.*"))
        // TODO should we ship slf4j? Which platforms?
        exclude(dependency(libs.slf4j.api))
    }

    from(platformBundle) {
        rename { "platform-base.jar" }
        into("bundled/")
    }
}
