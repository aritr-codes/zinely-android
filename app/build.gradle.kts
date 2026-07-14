plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    // PR-A Step 7: Hilt root (@HiltAndroidApp lives here); KSP runs the Hilt processor and the
    // cross-module SingletonComponent aggregation — this is where the whole graph is validated.
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    // S4 app shell (ADR-030): @Serializable type-safe navigation routes need the kotlinx-serialization
    // compiler plugin to generate the route serializers navigation-compose's toRoute()/composable<T> read.
    alias(libs.plugins.kotlin.serialization)
}

// The single version truth: versionName below and the artifact name both read this, so a
// version bump is one line and the APK renames itself. "0.6.0-alpha.1" = the first installable
// alpha (Step 4, 2026-07-07): the physical print/fold verification cleared the ADR-047 gate and
// the preview-text report was triaged to the ADR-028 Latin-first charset limitation.
val zinelyVersionName = "0.7.0"

// Artifact naming: every APK/bundle is "zinely-<versionName>-<variant>.apk" (e.g.
// zinely-1.0-release.apk) so testers always see the app name + version in the file, never
// an anonymous "app-release.apk".
base.archivesName.set("zinely-$zinelyVersionName")

android {
    namespace = "com.aritr.zinely"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.aritr.zinely"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = zinelyVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            // Alpha side-load signing only: the machine-local debug keystore keeps the release
            // build installable for testers (and signature-stable across rebuilds from this
            // machine). A real release keystore must replace this before any store distribution.
            signingConfig = signingConfigs.getByName("debug")
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        // java.nio.file (used by the durability core, ADR-025) is API 26; minSdk is 24 (ADR-024).
        // Core library desugaring backports it rather than raising minSdk — ADR-024's pre-blessed path.
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }

    testOptions {
        unitTests {
            // S6.5 (ADR-046): the ZinelyNavHost back-stack-policy tests run the real graph under
            // Robolectric — merged resources/manifest give the test the debug-only HiltTestActivity.
            isIncludeAndroidResources = true
        }
    }
}

dependencies {
    // S2B Android adapters (ADR-026 / ADR-025): the app consumes the data layer through this module.
    // Completes the intended one-way graph :app -> :data-android -> :core:* (core never depends back).
    implementation(project(":data-android"))

    // S4 editor host (ADR-030): the app mounts :feature:editor's EditorScreen + EditorStore, constructs
    // the store's effect seams, and derives pageSizePt from :core:imposition. :core:editor types
    // (EditorModel/Intent) appear in the wiring; :core:imposition supplies the panel size.
    implementation(project(":feature:editor"))
    implementation(project(":core:editor"))
    implementation(project(":core:imposition"))
    // S5 export (ADR-039): the app owns the export product path — it runs the Imposer + SceneRenderer and
    // hands per-panel tapes to :render-android's SheetComposer (the multi-panel PDF/PNG composer over the
    // shared CanvasReplayer). :core:render supplies SceneRenderer + the DrawCommand tape.
    implementation(project(":render-android"))
    implementation(project(":core:render"))
    // The editor host references these contracts directly (DocumentRepository, DataResult, the autosave
    // DocumentSnapshotProvider); :data-android pulls them as `implementation`, so they are not on :app's
    // classpath transitively — declare them explicitly.
    implementation(project(":core:data"))          // DocumentRepository + DataResult/DataError
    implementation(project(":core:data-storage"))  // DocumentSnapshotProvider (autosave pull seam)

    // Single-Activity navigation graph + type-safe routes, and the hiltViewModel() bridge (ADR-030 §1).
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.navigation.compose)
    // Route serializers for the @Serializable navigation routes (kotlin-serialization plugin output).
    implementation(libs.kotlinx.serialization.json)
    // S4 Inc 2b: EXIF orientation normalisation on image import (ADR-031 §4).
    implementation(libs.androidx.exifinterface)
    // collectAsStateWithLifecycle in the nav host / boot state (CLAUDE.md). Same 2.6.1 as -ktx.
    implementation(libs.androidx.lifecycle.runtime.compose)

    // PR-A Step 7: Hilt root. The @HiltAndroidApp Application is here; KSP aggregates and validates
    // the SingletonComponent graph contributed by :data-android at `:app:compileDebugKotlin`.
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    // Backport java.nio.file (durability core) to minSdk 24 (ADR-024 / ADR-025).
    coreLibraryDesugaring(libs.android.desugar.jdk.libs.nio)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    testImplementation(libs.junit)
    // EditorBootstrap unit tests (ADR-030): load over a fake DocumentRepository (runTest) and
    // editedPageSize over the real SingleSheet8Imposer — pure JVM, no Robolectric/Hilt.
    testImplementation(libs.kotlinx.coroutines.test)
    // S6.5 nav re-root (ADR-046): the graph's first host-level tests — the REAL ZinelyNavHost +
    // Hilt graph under Robolectric, driven by a TestNavHostController (start destination, card-tap
    // push, singleTop dedupe, error back, Keep-editing pop-to-existing).
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.navigation.testing)
    testImplementation(libs.hilt.android.testing)
    kspTest(libs.hilt.compiler)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}