plugins {
    alias(libs.plugins.kotlin.jvm)
    `java-library`
}

// Pure-Kotlin, Android-independent geometry + shared enums for the zine document model.
// Consumed by :core:imposition (and later by the render/export layers). No Android deps.
kotlin {
    jvmToolchain(21)
    explicitApi()
}

dependencies {
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.jqwik)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.test {
    useJUnitPlatform()
}
