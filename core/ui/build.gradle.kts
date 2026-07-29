plugins {
    // AGP 9 supplies the Kotlin built-in (see :app / :feature:editor, which apply no kotlin-android
    // plugin); the `kotlin { }` block below configures that built-in extension.
    alias(libs.plugins.android.library)
    // Compose compiler plugin (Kotlin 2.x): required for @Composable in this module. Same plugin
    // :feature:editor applies; buildFeatures.compose = true alone is not enough on AGP 9 / Kotlin 2.2.
    alias(libs.plugins.kotlin.compose)
    // Roborazzi is applied for parity with :feature:editor's design-token/component test tier and so
    // the `verifyRoborazziDebug`/`recordRoborazziDebug` tasks exist for CI. The z_components_* goldens
    // stay in :feature:editor for now (they share its rasterizeToBitmap host helper), so this module
    // currently records no golden of its own — verifyRoborazziDebug runs testDebugUnitTest in verify
    // mode with nothing to diff (a green no-op) until any golden is co-located here in a later cleanup.
    alias(libs.plugins.roborazzi)
}

// :core:ui (C2, CI-34 / roadmap §C2) — the shared design system, extracted verbatim out of
// :feature:editor. It carries the frozen tokens (ZinelyColors/Dimens/Elevation/Motion/Haptics/Type/
// Theme) and the Z* component primitives, KEEPING the package `com.aritr.zinely.ui.*` unchanged so no
// consumer import moved. This is a pure relocation: behaviour, pixels and the public (explicitApi)
// surface are identical to the pre-move :feature:editor code. The bundled UI fonts (Inter/Fraunces,
// res/font/) moved with Type.kt — Type.kt's ONLY changed line is its R import (feature.editor.R ->
// this module's com.aritr.zinely.ui.R), the mechanical consequence of the resource move.
//
// Unlike the pure-Kotlin :core:model/imposition/render/data modules (Android-free, JVM-tested in the
// core-only CI job), :core:ui is a Compose/Android library — it needs the SDK — so it is gated behind
// ZINELY_CORE_ONLY in settings.gradle.kts alongside :app / :feature:editor / :render-android, and runs
// in the SDK-provisioned android-graph CI job, never the pure-JVM core-tests job.
android {
    namespace = "com.aritr.zinely.ui"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        minSdk = 24
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        // Convention parity with :feature:editor / :render-android / :data-android (ADR-024). The design
        // system needs no java.* backport, but the Android tier configures consistently.
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        compose = true
    }

    // A8 (accessibility infrastructure): the CI-26 platform-`AccessibilityNodeInfo` harness lives in
    // `src/testFixtures` rather than `src/test`, because it must be readable from BOTH this module's own
    // tests and :feature:editor's. It was written in :feature:editor's test source set, where a test
    // source set is not published — so :core:ui, which is UPSTREAM of :feature:editor and cannot depend on
    // it, had no way to assert the platform tree for its own components. That is why every existing
    // platform-tree test for a :core:ui component (ZButtonPlatformA11yTest) sits in :feature:editor today.
    // Test fixtures fix the direction of the dependency without duplicating the harness.
    testFixtures {
        enable = true
    }

    testOptions {
        unitTests {
            // Merged Android resources/assets on the unit-test classpath — parity with :feature:editor's
            // Roborazzi tier. The moved token/contrast tests are plain JVM (no Robolectric), so this is a
            // no-op for them today; it is kept so a Roborazzi golden co-located here later just works.
            isIncludeAndroidResources = true

            // Single-fork determinism, mirroring :feature:editor's convention.
            all { test ->
                test.maxParallelForks = 1

                // The frozen corpus is a real INPUT to this module's tests: ZinelyV2CatalogParityTest
                // (ADR-079) parses V2-TOKENS.md at run time and asserts rendered pixels equal the values
                // it states. Without declaring it, Gradle sees no input change when the design document
                // is edited and reports the parity gate FROM-CACHE — the one edit the gate exists to
                // catch is the one that would not re-run it.
                test.inputs.file(rootProject.file("docs/design/V2-TOKENS.md"))
                    .withPropertyName("v2TokensCorpus")
                    .withPathSensitivity(PathSensitivity.RELATIVE)
            }
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
    }
    // Library module: keep the same explicit-API discipline the code carried in :feature:editor, so the
    // public design-system surface is asserted identical to the pre-move API.
    explicitApi()
}

dependencies {
    // Desugar runtime for isCoreLibraryDesugaringEnabled above (ADR-024). Convention parity.
    coreLibraryDesugaring(libs.android.desugar.jdk.libs.nio)

    // kotlinx.coroutines.delay is used inside ZSnackbar/ZToast auto-dismiss (no coroutine type appears
    // in any public signature), so `implementation`, not `api`.
    implementation(libs.kotlinx.coroutines.core)

    // Compose. Material 3 api-exposes foundation, so the components' foundation.* imports resolve
    // transitively exactly as they did in :feature:editor (which also declares no explicit foundation).
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // Unit tests. The moved ZinelyTokensTest / ZinelyColorsContrastTest are plain JUnit on the JVM
    // (Color/dp are pure value classes); the Robolectric/Roborazzi stack is present for parity with
    // :feature:editor and for any golden co-located here later.
    // Test fixtures (A8): the CI-26 platform-a11y harness. It touches only framework `View` /
    // `AccessibilityNodeInfo` API plus compose-ui-test's `SemanticsNodeInteraction`, so those two are all
    // it compiles against — `api`, because both types appear in its public signatures.
    testFixturesApi(platform(libs.androidx.compose.bom))
    testFixturesApi(libs.androidx.compose.ui.test.junit4)

    testImplementation(libs.junit)
    testImplementation(testFixtures(project(":core:ui")))
    testImplementation(libs.robolectric)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.junit.rule)
    testImplementation(libs.roborazzi.compose)
    testImplementation(libs.androidx.test.core)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
    testImplementation(libs.androidx.compose.ui.test.manifest)
}
