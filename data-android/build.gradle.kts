plugins {
    // AGP 9 supplies Kotlin built-in (see :app, which applies no kotlin-android plugin); the
    // `kotlin { }` block below configures that built-in extension.
    alias(libs.plugins.android.library)
    // PR-A Step 7: Hilt graph + KSP annotation processing for the DI bindings in `di/`. The Hilt
    // plugin is applied here (not just ksp(hilt-compiler)) so cross-module component aggregation
    // works with `implementation` deps; `enableAggregatingTask` is set below.
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

// :data-android (ADR-026 / ADR-025) — the Android production adapters that bind the Android-free
// S2B durability core (:core:data-storage) to the real Android stack. Build Order Step 1 (PR-A)
// lands ONLY the gated module skeleton + dependency wiring; the real adapters — the Os.fsync-backed
// FileSystemOps, the file-only DocumentRepository, the autosave coordinator factory, the lifecycle
// binder, and the Hilt graph — arrive in later steps.
//
// Dependency direction is one-way (ADR-025): :data-android -> :core:*, never the reverse, so no
// Android type ever leaks into a pure-Kotlin core module. Gated behind ZINELY_CORE_ONLY in
// settings.gradle.kts alongside :app, so CI keeps building the core Android-free.
android {
    namespace = "com.aritr.zinely.data.android"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        minSdk = 24
        // Real-device durability checks (Os.fsync, atomic rename) run as instrumented tests; the
        // pure fail-closed logic is covered by JVM unit tests via the DirFsync seam. The runner is
        // the Hilt-aware one (PR-A Step 7) so the device-only graph smoke test gets a
        // HiltTestApplication; the existing non-Hilt instrumented tests run unchanged under it.
        testInstrumentationRunner = "com.aritr.zinely.data.android.HiltTestRunner"
    }

    // Match :app's Java level so the Android tier links consistently (the core modules build at
    // their own jvmToolchain; this module only consumes their published classes).
    compileOptions {
        // This module (and the :core:data-storage durability core it adapts) use java.nio.file,
        // which is API 26; minSdk is 24 (ADR-024). Core library desugaring backports it instead of
        // raising minSdk — ADR-024's pre-blessed resolution.
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
    }
    // Library module exposing API to :app; keep the same explicit-API discipline as the core.
    explicitApi()
}

// PR-A Step 7: aggregate Hilt components across modules even though :app consumes this via
// `implementation` (not `api`); recommended by the Hilt Gradle setup for multi-module graphs.
hilt {
    enableAggregatingTask = true
}

dependencies {
    // ADR-025 dependency direction: this Android module depends on the pure-Kotlin core; the core
    // never depends back. These are the only edges the adapters in later steps will bind against.
    implementation(project(":core:data"))          // DocumentRepository + DataResult/DataError contracts
    implementation(project(":core:data-storage"))  // AtomicFileStore, FileSystemOps seam, AutosaveCoordinator
    implementation(project(":core:model"))          // ZineDocument schema

    // The in-memory SaveFailureSink (ADR-026) exposes autosave failures as a StateFlow; the editor
    // collects it for a "couldn't save" cue. coroutines-core is the only production runtime dep here.
    implementation(libs.kotlinx.coroutines.core)

    // EditorAutosaveBinder (ADR-026 §1/§3) observes Android lifecycle (ON_PAUSE/ON_STOP) to drive
    // flush/teardown. Only the pure-JVM lifecycle-common types (Lifecycle, LifecycleEventObserver)
    // are touched; no LifecycleRegistry/main-thread machinery, so unit tests run on plain JVM.
    implementation(libs.androidx.lifecycle.runtime.ktx)

    // PR-A Step 7: Hilt graph for the autosave stack (modules + qualifiers + EntryPoint in `di/`).
    // DI/build-time only — no networking/account/cloud dep (privacy invariant intact).
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    // Backport java.nio.file (this module + the durability core) to minSdk 24 (ADR-024 / ADR-025).
    coreLibraryDesugaring(libs.android.desugar.jdk.libs.nio)

    // Unit tests (plain JVM): drive the fail-closed durability contract through the DirFsync seam,
    // so no device/emulator is needed for the logic. android.system.Os is never touched here.
    testImplementation(libs.junit)
    // The DocumentRepository contract is `suspend`; runTest drives it from JUnit4 without runBlocking
    // (android-tdd). Production code stays coroutine-free — `suspend` compiles on the stdlib alone.
    testImplementation(libs.kotlinx.coroutines.test)

    // Instrumented tests (real device/emulator): exercise the real android.system.Os directory
    // fsync + atomic rename on app-private storage. Cannot run in the current no-emulator CI.
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.test.runner)

    // PR-A Step 7 device-only graph smoke test (§13.3, SUPPLEMENTAL — not a CI gate, no emulator
    // here). Resolves AutosaveGraph from a HiltTestApplication and round-trips one save.
    androidTestImplementation(libs.hilt.android.testing)
    kspAndroidTest(libs.hilt.compiler)
}
