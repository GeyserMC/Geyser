dependencies {
    api(projects.api)
}

publishing {
    publications.named<MavenPublication>("mavenJava") {
        groupId = rootProject.group as String + ".geyser"
        artifactId = "api"
    }
}