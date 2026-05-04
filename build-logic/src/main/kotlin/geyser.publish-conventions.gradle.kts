plugins {
    id("geyser.shadow-conventions")
    id("net.kyori.indra.publishing")
}

indra {
    configurePublications {
        if (shouldAddBranchName()) {
            version = versionWithBranchName()
        }
    }

    publishSnapshotsTo("geysermc", "https://repo.opencollab.dev/maven-snapshots")
    publishReleasesTo("geysermc", "https://repo.opencollab.dev/maven-releases")
}
