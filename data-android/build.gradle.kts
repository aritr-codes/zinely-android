plugins {
    // AGP 9 supplies Kotlin built-in (see :app, which applies no kotlin-android plugin); the
    // `kotlin { }` block below configures that built-in extension.
    alias(libs.plugins.android.library)
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
    }

    // Match :app's Java level so the Android tier links consistently (the core modules build at
    // their own jvmToolchain; this module only consumes their published classes).
    compileOptions {
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

dependencies {
    // ADR-025 dependency direction: this Android module depends on the pure-Kotlin core; the core
    // never depends back. These are the only edges the adapters in later steps will bind against.
    implementation(project(":core:data"))          // DocumentRepository + DataResult/DataError contracts
    implementation(project(":core:data-storage"))  // AtomicFileStore, FileSystemOps seam, AutosaveCoordinator
    implementation(project(":core:model"))          // ZineDocument schema

    testImplementation(libs.junit)
}
