pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "zinely"
// CI runs the pure-Kotlin core Android-free: set ZINELY_CORE_ONLY=true to drop the Android `:app`
// module (whose AGP/compileSdk config needs an Android SDK on the runner). Local builds include it.
if (providers.environmentVariable("ZINELY_CORE_ONLY").orNull != "true") {
    include(":app")
    // S2B Android adapters (ADR-026 / ADR-025): binds the pure-Kotlin durability core to the real
    // Android stack (Os.fsync FileSystemOps, file-only DocumentRepository, autosave coordinator
    // factory, lifecycle binder, Hilt graph). Needs the Android SDK, so it is gated out with `:app`.
    include(":data-android")
    // S3 Android render backend (ADR-028 / spike core-render-android-backend.md): replays the pure
    // :core:render DrawCommand tape onto a real android.graphics.Canvas via one CanvasReplayer + two
    // canvas providers (export PDF/raster; Compose preview host lands in S4). Needs the Android SDK
    // (Robolectric NATIVE + Roborazzi parity goldens), so it is gated out with :app / :data-android.
    // G1 = scaffold only; api(:core:render) is the only production edge (no Compose/Coil/Room).
    include(":render-android")
    // S4 Compose preview host (ADR-028 §2.4 / spike §2.4): a thin drawIntoCanvas bridge that replays
    // the pure :core:render tape onto the Compose nativeCanvas via the SAME :render-android
    // CanvasReplayer the export path uses, discharging the Compose-host==export parity obligation
    // (Codex Required-fix C) the S3 spike deferred. Needs the Android SDK (Compose + Robolectric
    // NATIVE + Roborazzi), so it is gated out with :app / :data-android / :render-android. Preview
    // host only this step — no MVI store / gestures / undo yet.
    include(":feature:editor")
}
// Pure-Kotlin, Android-independent core (imposition engine spike).
include(":core:model")
include(":core:imposition")
// Pure-Kotlin render core (S3, ADR-027): scene → ordered draw-command tape consumed by both
// the editor preview (S4) and PDF/image export (S5) for preview==export parity. Only dep
// :core:model; zero Android. Android backends live in a platform module, not here.
include(":core:render")
// Pure-Kotlin data core (S2A): document schema, serializer, migration, validation,
// repository + asset-manifest contracts. No Android deps yet (Room/WorkManager land in S2B).
include(":core:data")
// S2B durability/GC core (ADR-025): pure-JVM (java.nio) atomic file source, autosave
// coordinator, content-addressed asset store + mark-and-sweep GC. Android-free; CI-tested.
include(":core:data-storage")
// S4 editor state + interaction core (ADR-029): pure MVI reducer over the :core:model ZineDocument
// tree, command/field-memento undo, and pure hit-test/snap/transform-bake geometry. Only dep
// :core:model; zero Android. The Android store/gestures/contextbar live in :feature:editor.
include(":core:editor")
 