plugins {
    id("geyser.platform-base-conventions")
}

dependencies {
    implementation(libs.floodgate.velocity)
    implementation(projects.core)

    compileOnly(libs.velocity.proxy)
    compileOnly(libs.netty.transport.native.io.uring)
    compileOnly(libs.netty.transport.native.kqueue)

    compileOnlyApi(libs.velocity.api)
    api(libs.cloud.velocity)
}
        
// These dependencies are already present on the platform
provided(libs.velocity.api)
provided(libs.geantyref)

tasks {
    shadowJar {
        archiveBaseName.set("Geyser-Velocity-Base")

        // TODO look over these
        dependencies {
            exclude(dependency("com.google.*:.*"))
            exclude(dependency("io.netty:.*"))
            exclude(dependency("org.slf4j:.*"))
            exclude(dependency("org.ow2.asm:.*"))
            // Exclude all Kyori dependencies
            exclude(dependency("net.kyori:.*:.*"))
        }
    }
}
