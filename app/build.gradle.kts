plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    // PR-A Step 7: Hilt root (@HiltAndroidApp lives here); KSP runs the Hilt processor and the
    // cross-module SingletonComponent aggregation — this is where the whole graph is validated.
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
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
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}