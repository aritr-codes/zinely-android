plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    `java-library`
}

// Pure-Kotlin, Android-independent geometry + shared enums + the @Serializable zine document
// schema. Consumed by :core:imposition and :core:data (serializer/migration/validation).
// kotlinx.serialization is multiplatform/pure-JVM — it does NOT violate the "zero Android deps"
// invariant. The document tree lives here (per the S2 spike) so the schema types stay framework-pure.
kotlin {
    jvmToolchain(21)
    explicitApi()
}

dependencies {
    // The document schema is @Serializable; its generated serializers are part of this module's
    // public API surface, so kotlinx.serialization is exposed as `api`.
    api(libs.kotlinx.serialization.json)

    testImplementation(libs.junit.jupiter)
    testImplementation(libs.jqwik)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.test {
    useJUnitPlatform()
}
