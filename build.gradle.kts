plugins {
    // Ensure AP works in eclipse (no effect on other IDEs)
    eclipse
    id("geyser.base-conventions")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

// Maintainer task: copy a freshly built EduGeyser-Extension jar into bundled-extension/edu.jar so
// it gets embedded in the EduGeyser platform jars. Run after rebuilding the extension, then commit
// the updated jar:
//   ./gradlew refreshBundledExtension -PextensionJar=/path/to/EduGeyser-Extension-x.y.z.jar
tasks.register<Copy>("refreshBundledExtension") {
    description = "Refresh bundled-extension/edu.jar from a built EduGeyser-Extension jar (-PextensionJar=...)."
    group = "build"
    into(rootProject.file("bundled-extension"))
    rename { "edu.jar" }
    if (project.hasProperty("extensionJar")) {
        from(project.property("extensionJar")!!)
    }
    doFirst {
        if (!project.hasProperty("extensionJar")) {
            throw GradleException("Pass -PextensionJar=/path/to/EduGeyser-Extension-x.y.z.jar")
        }
    }
}
