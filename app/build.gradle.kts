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
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
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
    // EditorBootstrap unit tests (ADR-030): seed-on-miss over a fake DocumentRepository (runTest) and
    // editedPageSize over the real SingleSheet8Imposer — pure JVM, no Robolectric/Hilt.
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}