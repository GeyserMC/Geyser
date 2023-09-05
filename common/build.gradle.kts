plugins {
    id("geyser.publish-conventions")
}

dependencies {
    api(libs.cumulus)
    api(libs.gson)
}

indra {
    javaVersions {
        target(8)
    }
}
