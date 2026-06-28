plugins {
    alias(libs.plugins.kotlin.jvm)
    `java-library`
}

// S2B durability core (ADR-025). Pure-JVM (java.nio only) implementations behind the S2A
// `:core:data` contracts: the atomic document file source (ADR-021), the autosave coordinator,
// and the content-addressed asset byte-store. The mark-and-sweep GC and its ADR-022 pin-file +
// generation-counter race closure are **deferred — not yet implemented here** (no sweeper ships
// until import is pin-safe, ADR-031 §2). Deliberately Android-free so the highest-risk durability
// logic runs under fast pure-JVM unit tests in CI (like the imposition core). The Android adapters
// (Bitmap/EXIF import master, SAF; Room/WorkManager planned) live in the separate `:data-android`
// module (ADR-025) and are gated behind ZINELY_CORE_ONLY.
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
