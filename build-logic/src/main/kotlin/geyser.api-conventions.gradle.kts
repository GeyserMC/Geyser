plugins {
    id("geyser.api-conventions")
}

tasks {
    shadowJar {
        archiveBaseName.set(archiveBaseName.get() + "-api")
    }
}