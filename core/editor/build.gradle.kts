plugins {
    alias(libs.plugins.kotlin.jvm)
    `java-library`
}

// Pure-Kotlin, Android-independent editor core (S4, ADR-029): EditorModel, the intent set, the pure
// reducer (Intent -> Reduction(model, effects)), command/field-memento undo, and the pure hit-test /
// snap / transform-bake geometry. Mutates the existing :core:model ZineDocument tree (decomposed
// Transform, render-derived matrix); the Android store/gestures/contextbar live in :feature:editor.
// Depends ONLY on :core:model. No Android types, no I/O. See docs/spikes/s4-editor-mvi.md.
kotlin {
    jvmToolchain(21)
    explicitApi()
}

dependencies {
    // The document tree (ZineDocument, Page, Element, Transform, geometry) this core mutates.
    api(project(":core:model"))

    testImplementation(libs.junit.jupiter)
    testImplementation(libs.jqwik)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.test {
    useJUnitPlatform()
}
