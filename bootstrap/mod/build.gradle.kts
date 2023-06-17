architectury {
    common("forge", "fabric")
}

dependencies {
    api(projects.core)

    compileOnly(libs.mixin)
}