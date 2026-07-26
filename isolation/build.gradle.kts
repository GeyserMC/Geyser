plugins {
    id("geyser.base-conventions")
}

dependencies {
    // Also provides the base api, cumulus, events
    api(libs.floodgate.isolation)
    // Provides base api, math
    api(projects.api)
}
