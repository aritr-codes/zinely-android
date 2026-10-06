plugins {
    alias(libs.plugins.kotlin.jvm)
    `java-library`
}

// Pure-Kotlin, Android-independent editor core (S4, ADR-029): EditorModel, the intent set, the pure
// reducer (Intent -> Reduction(model, effects)), command/field-memento undo, and the pure hit-test /
// snap / transform-bake geometry. Mutates the existing :core:model ZineDocument tree (decomposed
// Transform, render-derived matrix); the Android store/gestures/contextbar live in :feature:editor.
// Depends on :core:model, and on :core:render for ONE function (ADR-124): SupplyInk.distancePt, so the
// hit test reads the outline the renderer draws. Nothing else from :core:render is called from here,
// and :core:render must never depend on this module. No Android types, no I/O.
// See docs/spikes/s4-editor-mvi.md.
kotlin {
    jvmToolchain(21)
    explicitApi()
}

dependencies {
    // The document tree (ZineDocument, Page, Element, Transform, geometry) this core mutates.
    api(project(":core:model"))
    // ADR-124: the drawn-ink hit test. `implementation`, not `api` — see the header.
    implementation(project(":core:render"))

    testImplementation(libs.junit.jupiter)
    testImplementation(libs.jqwik)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.test {
    useJUnitPlatform()
}
