plugins {
    id("geyser.publish-conventions")
    alias(libs.plugins.blossom) apply true
}

dependencies {
    api(libs.base.api)
    api(libs.math)
}

version = property("version")!!

sourceSets {
    main {
        blossom {
            javaSources {
                property("version", version as String)
            }
        }
    }
}
