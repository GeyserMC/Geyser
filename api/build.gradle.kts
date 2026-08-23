plugins {
    // Allow blossom to mark sources root of templates
    idea
    id("geyser.publish-conventions")
    alias(libs.plugins.blossom)
}

dependencies {
    api(libs.base.api)
    api(libs.math)
    api(libs.jetbrains.annotations)
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
