plugins {
    id("geyser.api-conventions")
}

dependencies {
    api(projects.api)

    implementation("net.kyori", "adventure-text-serializer-legacy", Versions.adventureVersion)
}

publishing {
    publications.named<MavenPublication>("mavenJava") {
        groupId = rootProject.group as String + ".geyser"
        artifactId = "api"
    }
}