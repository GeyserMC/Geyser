plugins {
    id("geyser.isolated-platform-conventions")
}

dependencies {
    compileOnlyApi(libs.velocity.api)
}

application {
    mainClass = "org.geysermc.geyser.platform.velocity.VelocityMain"
}

tasks {
    jar {
        archiveBaseName.set("Geyser-Velocity")
    }
}
