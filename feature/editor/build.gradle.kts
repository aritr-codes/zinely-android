plugins {
    // AGP 9 supplies the Kotlin built-in (see :app / :render-android, which apply no kotlin-android
    // plugin); the `kotlin { }` block below configures that built-in extension.
    alias(libs.plugins.android.library)
    // Compose compiler plugin (Kotlin 2.x): required for @Composable in this module. Same plugin
    // :app applies; buildFeatures.compose = true alone is not enough on AGP 9 / Kotlin 2.2.
    alias(libs.plugins.kotlin.compose)
    // S4 preview-host parity tier (ADR-028 §2.4 / §7.3, spike §2.4 "Codex Required-fix C"): registers
    // recordRoborazzi*/verifyRoborazzi* so the Compose-host == export raster golden can land. The
    // behavioural pixel parity test is the red/green gate; the golden is committed on the pinned CI
    // image (never locally — local PNGs differ and break verify).
    alias(libs.plugins.roborazzi)
}

// :feature:editor (S4, ADR-028 §2.4 / spike §2.4) — the Compose **preview host**: a thin
// `drawIntoCanvas` bridge that replays the pure :core:render DrawCommand tape onto the Compose
// `nativeCanvas` through the SAME :render-android CanvasReplayer the export path uses. It adds NO
// geometry of its own (all of it lives in ExportScale.previewPageToDevice + CanvasReplayer), so
// `preview == export` stays structural, not disciplinary (ADR-006 / ADR-028).
//
// This step lands the preview host ONLY — no MVI store, no gestures, no undo (those follow in S4).
// It discharges the Codex Required-fix C obligation the S3 spike (§2.4) deferred to S4: prove the
// Compose host adds zero drift versus the raw replayer for the same tape.
//
// Dependency direction (ADR-025/028): :feature:editor -> :render-android -> :core:render ->
// :core:model, never the reverse. The host injects BundledFontResolver (ADR-028 §4.2, the §4.2
// wiring obligation), never FontResolver.Default. Needs the Android SDK (Compose + Robolectric
// NATIVE + Roborazzi), so it is gated behind ZINELY_CORE_ONLY in settings.gradle.kts alongside
// :app / :data-android / :render-android — core-only CI skips it.
android {
    namespace = "com.aritr.zinely.feature.editor"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        minSdk = 24
        // Preview-host parity runs headless on the JVM (Robolectric NATIVE rasterises real Skia for
        // both the Compose host and the raw replayer); no instrumented exit-bar in this step. Plain
        // runner, no Hilt graph.
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        // Convention parity with :render-android / :data-android (ADR-024). The current preview host
        // needs no java.* backport, but the Android tier configures consistently.
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        compose = true
    }

    testOptions {
        unitTests {
            // Robolectric needs merged Android resources/assets on the unit-test classpath for
            // graphicsMode=NATIVE. Critically, the bundled Inter TTFs live in :render-android's
            // main assets; isIncludeAndroidResources merges transitive library assets so
            // BundledFontResolver(assets) resolves `fonts/Inter-*.ttf` in the headless host test.
            isIncludeAndroidResources = true

            // S4 preview-host Roborazzi golden stability (ADR-028 §7.3/§7.5). Same convention as
            // :render-android's RasterGoldenTest: each test passes an explicit module-relative path
            // to captureRoboImage (".../src/test/roborazzi/<case>.png"), a no-op under a plain
            // testDebugUnitTest so the headless job stays green until the golden is recorded on the
            // pinned CI image. maxParallelForks=1 keeps the raster single-fork deterministic.
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
    // The render backend: ExportScale.previewPageToDevice + CanvasReplayer + BundledFontResolver +
    // AssetBytesSource/ImageBlitter. PagePreview's PUBLIC signature exposes DrawCommand / PtSize /
    // PtPoint (and AssetBytesSource), so this must be `api` — it re-exports api(:core:render) ->
    // :core:model, giving consumers those API types transitively (Codex fix #6).
    api(project(":render-android"))

    // The pure MVI core (ADR-029): EditorStore's PUBLIC surface exposes EditorModel/EditorUiState/Intent
    // (params + dispatch + uiState), so this is `api`, not implementation.
    api(project(":core:editor"))

    // The canonical imposition convention (pure Kotlin, zero Android): the Proof Act 1 imposed
    // sheet derives its panel order/rotation from SingleSheet8.TOP_ROW_ROTATED instead of keeping a
    // second hardcoded truth that can drift (it did — the checkpoint caught 5·4·3·6/8·1·2·7).
    // Internal-only use, so `implementation`.
    implementation(project(":core:imposition"))

    // Coroutines: StateFlow / CoroutineScope / CoroutineDispatcher appear in EditorStore's public API
    // (uiState, constructor), so `api`. Dispatchers.Main itself (coroutines-android) is supplied by the
    // app module that constructs the store — the store only takes an injected dispatcher.
    api(libs.kotlinx.coroutines.core)

    // Desugar runtime for isCoreLibraryDesugaringEnabled above (ADR-024). Convention parity.
    coreLibraryDesugaring(libs.android.desugar.jdk.libs.nio)

    // Compose preview host (Material 3 baseline kept for the editor screen that follows this step).
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.material3)
    // Real Material icons for the context-bar restyle (replaces the unicode-glyph placeholders). The
    // directional/transform/reorder/delete glyphs (KeyboardArrow*, Add/Remove, Rotate*, FlipTo*, Delete)
    // are sourced here so the bar reads as designed chrome, not productivity-template text. Version from
    // the BOM; R8 tree-shakes unused vectors out of the release app.
    implementation(libs.androidx.compose.material.icons.extended)
    // collectAsStateWithLifecycle in EditorScreen (CLAUDE.md). Same 2.6.1 as -ktx — no version bump.
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // Unit tests run on the JVM via Robolectric NATIVE (real Skia) — no emulator, fits the existing
    // Android-SDK CI job. compose-ui-test drives the host composable; Roborazzi captures/diffs.
    testImplementation(libs.junit)
    // Store wiring tests: pure-JVM coroutines test (TestScope/StandardTestDispatcher), no Robolectric.
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.junit.rule)
    testImplementation(libs.roborazzi.compose)
    testImplementation(libs.androidx.test.core) // ApplicationProvider for the parity test's reference replay
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
    testImplementation(libs.androidx.compose.ui.test.manifest)
}
