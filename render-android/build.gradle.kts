plugins {
    // AGP 9 supplies Kotlin built-in (see :app / :data-android, which apply no kotlin-android plugin);
    // the `kotlin { }` block below configures that built-in extension.
    alias(libs.plugins.android.library)
    // S3 visual-fidelity tier: registers recordRoborazzi*/verifyRoborazzi* tasks for the
    // preview==export golden diffs (wired into the Android-SDK CI job). G1 applies the plugin so the
    // tasks exist and the toolchain resolves; the goldens themselves arrive at G6.
    alias(libs.plugins.roborazzi)
}

// :render-android (ADR-028, spike docs/spikes/core-render-android-backend.md) — the Android replay
// layer for the pure :core:render DrawCommand tape. ONE CanvasReplayer draws onto a raw
// android.graphics.Canvas behind two providers (export PDF in PostScript points + export raster
// ×300/72); the Compose preview host is a thin bridge that lands in :feature:editor (S4).
//
// G1 (this change) is SCAFFOLD ONLY — no replay logic. It proves the gated module builds, that the
// api(:core:render) edge resolves, and that Robolectric graphicsMode=NATIVE rasterises real pixels in
// CI (the load-bearing prerequisite for every later golden, spike §7.1). G2–G6 add the logic + goldens.
//
// Dependency direction (ADR-025/028): :render-android -> :core:render -> :core:model, never the
// reverse — no Android type leaks into a pure core. No Compose / Coil / Room / :data-android: image
// bytes arrive later through an injected AssetBytesSource seam (G4), not a persistence dependency.
// Gated behind ZINELY_CORE_ONLY in settings.gradle.kts alongside :app / :data-android.
android {
    namespace = "com.aritr.zinely.render.android"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        minSdk = 24
    }

    compileOptions {
        // Core library desugaring backports desugar-eligible java.* LIBRARY APIs (java.time / java.nio)
        // to minSdk 24 (ADR-024) — it does NOT change bytecode target; :core:render is consumed as
        // ordinary classes. The current scaffold needs no backport; kept for parity with :data-android
        // so the Android tier configures consistently and G4's decode/blit work has it available.
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    testOptions {
        unitTests {
            // Robolectric needs merged Android resources/assets on the unit-test classpath; required
            // for graphicsMode=NATIVE and (at G6) for loading the bundled font + image fixtures.
            isIncludeAndroidResources = true
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
    }
    // Library module: keep the same explicit-API discipline as the core it replays.
    explicitApi()
}

dependencies {
    // The ONLY production edge: the pure render tape + computeImageBlit (transitively :core:model).
    api(project(":core:render"))

    // Desugar runtime for isCoreLibraryDesugaringEnabled above (ADR-024). Convention parity with
    // :data-android; no java.* library backport is required by the current G1 scaffold.
    coreLibraryDesugaring(libs.android.desugar.jdk.libs.nio)

    // Unit tests run on the JVM via Robolectric NATIVE (real Skia) — no emulator, fits the existing
    // Android-SDK CI job. Roborazzi drives the golden diffs (goldens land at G6).
    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.junit.rule)
}
