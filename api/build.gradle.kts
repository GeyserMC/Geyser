plugins {
    id("geyser.publish-conventions")
    alias(libs.plugins.blossom) apply true
}

dependencies {
    api(libs.base.api)
    api(libs.math)
}

version = property("version")!!
val apiVersion = (version as String).removeSuffix("-SNAPSHOT")

sourceSets {
    main {
        blossom {
            javaSources {
                property("version", apiVersion)
            }
        }
    }
}
