plugins {
    alias(libs.plugins.kotlin.jvm)
    `java-library`
}

// Pure-Kotlin, Android-independent copy layer (C9, ADR-060): the single VOICE-traceable home for every
// user-facing string — the `Copy` object of String constants + parameterised functions consumed by
// `:app` and `:feature:editor`. No Android types, no I/O, no dependencies. Included UNCONDITIONALLY
// (outside the ZINELY_CORE_ONLY gate) so the no-prose-literal guard runs in the pure-JVM core-tests job.
// See docs/DECISIONS.md#adr-060 and docs/design/VOICE.md (the human wording authority).
kotlin {
    jvmToolchain(21)
    explicitApi()
}

dependencies {
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.test {
    useJUnitPlatform()
}
