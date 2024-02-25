plugins {
    id("geyser.base-conventions")
}

dependencies {
    api(libs.floodgate.isolation)
    api(projects.api)
}