package com.aritr.zinely.feature.editor

import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * Retry a test **once** when the Robolectric image decoder fails under it.
 *
 * The Reframe surface tests need real decoded pixels: since M7-01 the frame is adjustable only while
 * the photo is genuinely on screen, so with no bitmap the controls never mount and the assertions have
 * nothing to stand on. On the Linux CI image `BitmapFactory` intermittently refuses — the runtime prints
 * *"Failed to create image decoder with message 'unimplemented'"* — and whichever test was running dies.
 *
 * **The evidence says intermittent, not cumulative, and that correction matters.** The first diagnosis
 * was decoder *exhaustion*: a resource consumed past a threshold, which the rotating failure set seemed
 * to support ({106,120,188}, then {106,131}, then {165,264}). It predicted that once the decoder died
 * everything after it would die too — and that is not what the runs show. Every red run fails **one or
 * two** tests and the remaining three hundred pass, including tests that decode. A resource that ran out
 * does not come back; this one does, within the same JVM. So the rotation is just randomness landing
 * somewhere different each time, and `forkEvery` — which lowers decodes *per process* but not the total
 * — was treating the wrong mechanism. It went 2 failures → 1, which is what noise looks like either way.
 *
 * A retry is the honest remedy for a genuinely random environmental failure, and it is bounded so it
 * cannot hide a real one: a regression fails **deterministically**, so it fails the retry too and the
 * run still goes red. Only a failure that does not reproduce on immediate re-execution is absorbed, and
 * it is announced on stderr rather than swallowed, so a test that starts needing its retry every run is
 * visible instead of quietly load-bearing.
 *
 * Applied only to the Reframe suites, which are the only ones that decode a full bitmap. Delete this
 * when Robolectric's decoder stops failing on the CI image; nothing here is wrong on a real device,
 * where the whole surface was exercised by hand for the ADR-053/ADR-055 device passes.
 */
class RetryFlakyDecode : TestRule {

    override fun apply(base: Statement, description: Description): Statement = object : Statement() {
        override fun evaluate() {
            try {
                base.evaluate()
            } catch (first: Throwable) {
                System.err.println(
                    "RetryFlakyDecode: ${description.displayName} failed once (${first.javaClass.simpleName}: " +
                        "${first.message?.take(160)}). Re-running once — a real regression fails again.",
                )
                try {
                    base.evaluate()
                } catch (second: Throwable) {
                    // Keep the SECOND failure as the reported one but carry the first, so a genuinely
                    // deterministic break reads as itself rather than as "a flaky test finally gave up".
                    second.addSuppressed(first)
                    throw second
                }
                System.err.println(
                    "RetryFlakyDecode: ${description.displayName} passed on retry — treated as the known " +
                        "CI decoder flake. If this recurs every run it is no longer flake; investigate.",
                )
            }
        }
    }
}
