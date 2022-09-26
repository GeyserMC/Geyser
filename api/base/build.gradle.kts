dependencies {
    api(libs.cumulus)
    api(libs.events) {
        exclude(group = "com.google.guava", module = "guava")
        exclude(group = "org.lanternpowered", module = "lmbda")
    }
}