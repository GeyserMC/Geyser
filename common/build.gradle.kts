plugins {
    id("geyser.publish-conventions")
    id("io.freefair.lombok")
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
