plugins {
    alias(libs.plugins.kotlin.jvm)
    `java-library`
}

// Pure-Kotlin, Android-independent render core (S3, ADR-027): turns a document Page into an
// ordered, flat list of self-contained draw commands in page-local points, consumed by both the
// editor preview (S4) and PDF/image export (S5) to guarantee preview==export parity (ADR-006).
// Production code depends ONLY on :core:model (the tests add :core:copy — see below). No Android
// types, no I/O, no bytes. See docs/spikes/core-render.md.
kotlin {
    jvmToolchain(21)
    explicitApi()
}

dependencies {
    // The document model (Page, Element, Transform, geometry, AffineTransform2D) is part of this
    // module's public API surface.
    api(project(":core:model"))

    // TEST ONLY, and deliberately not `api`/`implementation`: `Copy.Supplies` is the single source of
    // truth for supply ids (ADR-105 S6), so `SupplyCatalogTest` cross-checks the catalogue's keys
    // against it. Production render code must stay ignorant of the copy layer — an outline has no
    // words in it, and the reverse edge would put user-facing strings in the draw tape.
    testImplementation(project(":core:copy"))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.jqwik)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.test {
    useJUnitPlatform()
}
