architectury {
    common("neoforge", "fabric")
}

dependencies {
    api(projects.core)

    compileOnly(libs.mixin)
}