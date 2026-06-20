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
}
// Pure-Kotlin, Android-independent core (imposition engine spike).
include(":core:model")
include(":core:imposition")
// Pure-Kotlin data core (S2A): document schema, serializer, migration, validation,
// repository + asset-manifest contracts. No Android deps yet (Room/WorkManager land in S2B).
include(":core:data")
 