import java.util.Properties

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
// "0.9.0-beta.1" = the first build put in front of the named beta cohort.
val zinelyVersionName = "0.9.0-beta.1"

// Release signing (beta). Credentials live in an untracked `keystore.properties` at the repo root,
// or in ZINELY_KEYSTORE_* environment variables — never in git. See docs/RELEASING.md.
//
// Why this exists: through 0.8.0 the release build was signed with the machine-local *debug*
// keystore, which cannot be reproduced on another machine. A build signed by a different key
// cannot be installed over the previous one, so a tester would have to uninstall to take an
// update — and uninstalling destroys their zines, because backup/restore does not exist yet.
// A stable release key is what makes a beta patchable.
// Read through a *tracked* provider, not File.exists(): an untracked configuration-time file read is
// invisible to the configuration cache, so creating keystore.properties would not invalidate a cached
// configuration and the build would go on believing no key exists.
val keystorePropertiesText: String? =
    providers.fileContents(rootProject.layout.projectDirectory.file("keystore.properties"))
        .asText
        .orNull

val keystoreProperties = Properties().apply {
    keystorePropertiesText?.let { load(it.reader()) }
}

/** Reads a signing credential from `keystore.properties` first, then the environment (CI). */
fun signingCredential(propertyKey: String, environmentKey: String): String? =
    keystoreProperties.getProperty(propertyKey)?.takeIf(String::isNotBlank)
        ?: System.getenv(environmentKey)?.takeIf(String::isNotBlank)

/**
 * All four credentials, or null. Partial configuration counts as *unconfigured* rather than
 * half-signing: supplying only `storeFile` used to activate the signing config and then fail deep
 * inside AGP's signing task with a message that named neither the missing key nor this file.
 */
val releaseSigning: Map<String, String>? = run {
    val credentials = listOf(
        "storeFile" to signingCredential("storeFile", "ZINELY_KEYSTORE_FILE"),
        "storePassword" to signingCredential("storePassword", "ZINELY_KEYSTORE_PASSWORD"),
        "keyAlias" to signingCredential("keyAlias", "ZINELY_KEY_ALIAS"),
        "keyPassword" to signingCredential("keyPassword", "ZINELY_KEY_PASSWORD"),
    )
    val missing = credentials.filter { it.second == null }.map { it.first }
    when {
        missing.size == credentials.size -> null // nothing configured at all — the ordinary case
        missing.isNotEmpty() -> error(
            "zinely: release signing is partially configured — missing ${missing.joinToString()}. " +
                "Supply all four (storeFile, storePassword, keyAlias, keyPassword) in " +
                "keystore.properties / ZINELY_KEYSTORE_*, or none. See docs/RELEASING.md."
        )
        else -> credentials.associate { it.first to it.second!! }
    }
}

// The deliberate escape hatch for a debug-signed release build (CI smoke, local perf profiling).
// Read as a Gradle property so it is a tracked configuration input.
val allowDebugSignedRelease: Boolean =
    providers.gradleProperty("allowDebugSignedRelease").isPresent

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
        // Monotonic, and independent of versionName: Android compares only this when deciding
        // whether an APK is an upgrade. It stayed at 1 through 0.6.0-alpha.1 .. 0.8.0 (no build
        // was ever distributed with an upgrade path); 2 is the first beta.
        versionCode = 2
        versionName = zinelyVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        // Registered unconditionally so the DSL stays valid on a fresh clone; only populated when
        // all four credentials are present. `releaseSigning == null` is the "this machine has no
        // release key" case.
        create("release") {
            releaseSigning?.let { credentials ->
                // Resolved against the repo root, so a relative storeFile in keystore.properties
                // means what it looks like it means (`file(...)` alone would resolve against app/).
                storeFile = rootProject.file(credentials.getValue("storeFile"))
                storePassword = credentials.getValue("storePassword")
                keyAlias = credentials.getValue("keyAlias")
                keyPassword = credentials.getValue("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            // Falls back to debug signing when no release key is configured, so `assembleRelease`
            // still works on a fresh clone or a contributor's machine. Such a build is runnable but
            // NOT distributable — the packageRelease gate below is what stops it leaving the machine.
            signingConfig =
                if (releaseSigning != null) {
                    signingConfigs.getByName("release")
                } else {
                    signingConfigs.getByName("debug")
                }
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

// The gate that makes the debug-signing fallback safe.
//
// This MUST fail at execution time, not configuration time. The first version of this guard was a
// configuration-phase `logger.warn`, and with `org.gradle.configuration-cache=true` a cached
// configuration is not re-run — so the warning silently never printed and `assembleRelease` produced
// a debug-signed APK carrying the release filename, with a clean build log. A `doFirst` capturing
// only a Boolean and a String is configuration-cache safe and runs on every build.
//
// Escape hatch for a deliberately debug-signed release build (CI smoke, perf profiling):
//   ./gradlew :app:assembleRelease -PallowDebugSignedRelease
if (releaseSigning == null) {
    val version = zinelyVersionName
    val optedOut = allowDebugSignedRelease
    tasks.matching { it.name == "packageRelease" }.configureEach {
        doFirst {
            if (optedOut) {
                logger.warn(
                    "zinely: building $version release with the DEBUG key (-PallowDebugSignedRelease). " +
                        "Do not distribute this APK."
                )
            } else {
                error(
                    "zinely: refusing to package a $version release APK signed with the debug key.\n" +
                        "No release keystore is configured (keystore.properties / ZINELY_KEYSTORE_*).\n" +
                        "A debug-signed build cannot be installed as an update over a properly signed " +
                        "one, so shipping it would force testers to uninstall — which destroys their " +
                        "zines, since backup/restore does not exist yet.\n" +
                        "Configure the key (docs/RELEASING.md), or pass -PallowDebugSignedRelease if " +
                        "you deliberately want an undistributable build."
                )
            }
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