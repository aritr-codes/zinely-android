plugins {
    alias(libs.plugins.kotlin.jvm)
    `java-library`
}

// Pure-Kotlin, Android-independent imposition engine: maps logical zine pages onto a
// physical sheet (panels + rotations), emits fold/cut guides, and renders an SVG proof
// sheet for printer-free validation. See docs/spikes/imposition-engine.md. No Android deps.
kotlin {
    jvmToolchain(21)
    explicitApi()
}

dependencies {
    // Geometry + shared enums are part of this engine's public API surface.
    api(project(":core:model"))

    testImplementation(libs.junit.jupiter)
    testImplementation(libs.jqwik)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.test {
    useJUnitPlatform()
}
