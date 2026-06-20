plugins {
    alias(libs.plugins.kotlin.jvm)
    `java-library`
}

// S2B durability/GC core (ADR-025). Pure-JVM (java.nio only) implementations behind the S2A
// `:core:data` contracts: the atomic document file source (ADR-021), the autosave coordinator,
// the content-addressed asset byte-store, and the mark-and-sweep GC algorithm with the
// ADR-022 pin-file + generation-counter race closure. Deliberately Android-free so the
// highest-risk durability logic runs under fast pure-JVM unit tests in CI (like the imposition
// core). The Android adapters (Room, WorkManager scheduler, Bitmap/EXIF, SAF) live in the
// separate `:data-android` module (ADR-025) and are gated behind ZINELY_CORE_ONLY.
//
// Durability/atomicity guarantees are scoped to app-private internal storage behind the
// `FileSystemOps` capability seam; SAF/external is best-effort (ADR-022 amendment).
kotlin {
    jvmToolchain(21)
    explicitApi()
}

dependencies {
    // Implements the S2A persistence contracts (DocumentSerializer, AssetStore, DataResult, …).
    api(project(":core:data"))

    // Coroutines for the autosave coordinator (injected dispatchers; single-writer mutex).
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(libs.junit.jupiter)
    testImplementation(libs.jqwik)
    testImplementation(libs.kotlinx.coroutines.test)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.test {
    useJUnitPlatform()
}
