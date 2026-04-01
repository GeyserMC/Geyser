plugins {
    // Allow blossom to mark sources root of templates
    idea
    id("geyser.publish-conventions")
    alias(libs.plugins.blossom)
    kotlin("jvm") version "2.3.20"
}

dependencies {
    api(libs.base.api)
    api(libs.math)
    api(libs.jetbrains.annotations)
    implementation(kotlin("stdlib-jdk8"))
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
repositories {
    mavenCentral()
}
kotlin {
    jvmToolchain(21)
}
