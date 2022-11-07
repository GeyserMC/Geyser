plugins {
    id("geyser.api-conventions")
}

dependencies {
    api(libs.baseApi)
}

publishing {
    publications.named<MavenPublication>("mavenJava") {
        groupId = rootProject.group as String + ".geyser"
        artifactId = "api"
    }
}