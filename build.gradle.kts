plugins {
    // Ensure AP works in eclipse (no effect on other IDEs)
    eclipse
    id("geyser.base-conventions")
    alias(libs.plugins.lombok) apply false
}
