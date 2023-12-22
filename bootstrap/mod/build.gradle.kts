architectury {
    common("neoforge", "fabric")
}

dependencies {
    api(projects.core)

    compileOnly(libs.mixin)
}

// Fails without due to shadow in neoforge somehow depending on this task
// Should see if there's a better way to workaround this
configurations.create("transformProductionForge")