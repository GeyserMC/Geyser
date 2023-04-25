plugins {
    id("geyser.publish-conventions")
}

dependencies {
    api(libs.base.api)
    // https://mvnrepository.com/artifact/com.fasterxml.jackson.core/jackson-annotations
    implementation("com.fasterxml.jackson.core:jackson-annotations:2.14.0")

}