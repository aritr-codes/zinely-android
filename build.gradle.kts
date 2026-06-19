// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    // Registered once at the root so the pure-Kotlin :core modules can apply it
    // without re-declaring a version (avoids "plugin already on the classpath" conflicts).
    alias(libs.plugins.kotlin.jvm) apply false
}