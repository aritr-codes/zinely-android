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
// Built incrementally across gates G1–G6 (spike §9): G1 scaffold; G2 CanvasReplayer + FillRect; G3
// SharedTextLayout; G4 ImageBlitter; G5 (current) the two export canvas providers + the PdfRenderer
// rasterise-back harness; G6 the Roborazzi parity goldens. Robolectric graphicsMode=NATIVE rasterises
// real Skia pixels in CI for the raster path (spike §7.1); the PDF write path is instrumented (below).
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
        // The raster provider is proven headless (Robolectric NATIVE rasterises real Skia). The PDF
        // provider's write+rasterise-back proof needs the real android.graphics.pdf stack (PdfDocument
        // generation is unsupported under Robolectric NATIVE — "document is closed!" on a fresh page),
        // so it lives in src/androidTest and runs on a device/emulator — same split as :data-android's
        // Os.fsync checks (ADR-028 risk R1/Q3). Plain runner; no Hilt graph in this module.
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
            // Robolectric needs merged Android resources/assets on the unit-test classpath for
            // graphicsMode=NATIVE. (The G6a image-master fixture lives in androidTest assets — the
            // headless raster suite renders only Canvas-primitive cases, no PNG decode.)
            isIncludeAndroidResources = true

            // G6a Roborazzi golden stability (ADR-028 §7.3/§7.5). The golden FILE LOCATION is the
            // explicit module-relative path each test passes to captureRoboImage (".../src/test/
            // roborazzi/<case>.png"), resolved against the unit-test task's working dir (the module
            // root) — the SAME path in record and verify mode, so a golden recorded on the pinned CI
            // image gates every later run. captureRoboImage is a no-op under a plain testDebugUnitTest
            // (neither record nor verify property set), so the headless job stays green until the
            // goldens are committed — exactly the G1 task-wiring intent. maxParallelForks=1 keeps the
            // raster single-fork deterministic.
            all { test ->
                test.maxParallelForks = 1
            }
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

    // Instrumented tests (real device/emulator): the PDF provider's exit-bar proof — write a page with
    // the real PdfDocument and rasterise it back with the real PdfRenderer (ADR-028 §7.3 layer 1).
    // Robolectric NATIVE rasterises Bitmap/Canvas but cannot generate a PdfDocument, so this proof
    // cannot run in the current no-emulator CI (it is compile-checked there; run on a device). Same
    // authored-not-CI-run split as :data-android's Os.fsync durability tests.
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.core) // ApplicationProvider (explicit, not via transitivity)
}
