plugins {
    id("geyser.shadow-conventions")
}

tasks {
    shadowJar {
        archiveBaseName.set(archiveBaseName.get() + "-api")
    }
}