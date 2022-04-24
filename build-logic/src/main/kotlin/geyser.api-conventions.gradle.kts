plugins {
    id("geyser.publish-conventions")
}

tasks {
    shadowJar {
        archiveBaseName.set(archiveBaseName.get() + "-api")
    }
}