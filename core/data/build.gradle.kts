plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    `java-library`
}

// Pure-Kotlin data core (S2A). Holds the persistence-shaped contracts and pure logic:
// DocumentSerializer (kotlinx JSON), the migration framework, the validation framework,
// repository + Result contracts, and the .zine/asset manifest contracts.
//
// S2A is deliberately Android-free: NO Room, NO WorkManager, NO Context, NO file I/O impls.
// kotlinx.serialization + kotlinx.coroutines are multiplatform/pure-JVM and do NOT violate the
// "no Android" constraint. The Android-backed data sources (Room DAO, file data source, asset
// store, autosave coordinator, WorkManager GC) arrive in S2B as a separate Android module.
kotlin {
    jvmToolchain(21)
    explicitApi()
}

dependencies {
    // The document schema (@Serializable types) and enums live in :core:model.
    api(project(":core:model"))

    // JSON (de)serialization for the document and manifests.
    api(libs.kotlinx.serialization.json)
    // Flow-returning repository contracts (pure-Kotlin coroutines; no Android).
    api(libs.kotlinx.coroutines.core)

    testImplementation(libs.junit.jupiter)
    testImplementation(libs.jqwik)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.test {
    useJUnitPlatform()
}
