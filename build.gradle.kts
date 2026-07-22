// Top-level build configuration common to all sub-projects and modules.
plugins {
    // Android Application Plugin (defines base AGP configuration)
    alias(libs.plugins.android.application) apply false

    // Kotlin Android Plugin (compiles Kotlin code targeting Android)
    alias(libs.plugins.kotlin.android) apply false

    // Kotlin Compose Compiler Plugin (manages Jetpack Compose code generation)
    alias(libs.plugins.kotlin.compose) apply false
}