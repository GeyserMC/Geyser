dependencies {
    api("org.geysermc.cumulus", "cumulus", Versions.cumulusVersion)
    api("org.geysermc.event", "events", Versions.eventsVersion) {
        exclude(group = "com.google.guava", module = "guava")
        exclude(group = "org.lanternpowered", module = "lmbda")
    }
}