plugins {
    alias(libs.plugins.kotlin.jvm)
    `java-library`
}

// Pure-Kotlin, Android-independent render core (S3, ADR-027): turns a document Page into an
// ordered, flat list of self-contained draw commands in page-local points, consumed by both the
// editor preview (S4) and PDF/image export (S5) to guarantee preview==export parity (ADR-006).
// Depends ONLY on :core:model. No Android types, no I/O, no bytes. See docs/spikes/core-render.md.
kotlin {
    jvmToolchain(21)
    explicitApi()
}

dependencies {
    // The document model (Page, Element, Transform, geometry, AffineTransform2D) is part of this
    // module's public API surface.
    api(project(":core:model"))

    testImplementation(libs.junit.jupiter)
    testImplementation(libs.jqwik)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.test {
    useJUnitPlatform()
}
