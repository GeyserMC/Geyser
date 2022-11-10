plugins {
    id("geyser.api-conventions")
}

dependencies {
    compileOnly(libs.baseApi)
}

publishing {
    publications.named<MavenPublication>("mavenJava") {
        groupId = rootProject.group as String + ".geyser"
        artifactId = "api"
    }
}