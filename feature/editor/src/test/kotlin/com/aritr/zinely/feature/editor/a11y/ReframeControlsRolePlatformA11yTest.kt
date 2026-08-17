package com.aritr.zinely.feature.editor.a11y

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import com.aritr.zinely.feature.editor.FrameFit
import com.aritr.zinely.feature.editor.ReframeAbilities
import com.aritr.zinely.feature.editor.ReframeControls
import com.aritr.zinely.ui.a11y.platformNode
import com.aritr.zinely.ui.theme.ZinelyTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

/**
 * CI-30 (`Role`) for the [ReframeControls] surface on the **platform** accessibility tree (CI-26
 * [platformNode]). Every Reframe *command* sets `role = Role.Button`; the sibling demo
 * [ReframeControlsPlatformA11yTest] pins the one "Zoom in" stepper (the `f4faaa4` control). This suite
 * extends that to the rest of the band — nudge pad, both zoom steps, the fit chips, Reset, Cancel and Done
 * — so every control TalkBack announces as a Button is proven to carry `android.widget.Button` to the
 * platform, not just to Compose's merged tree. The fit chips carry a second contract on top of that one,
 * asserted in [the_platform_is_told_which_fit_is_chosen].
 *
 * `ReframeControls` is rendered standalone (no photo), so it does **not** touch the Robolectric NATIVE
 * image decoder that flakes in the full-screen [com.aritr.zinely.feature.editor.ReframeA11yTest].
 *
 * **CI-93 (now assertable — the gap the stale base reported has closed):** since `f4faaa4` "steppers admit
 * when they can't act", `ReframeControls` takes a [com.aritr.zinely.feature.editor.ReframeAbilities] and each
 * zoom stepper's `clickable(enabled = abilities.zoomIn / .zoomOut)` carries a real disabled path — the exact
 * control whose platform ENABLED bit lied. [the_disabled_zoom_steppers_report_disabled_on_the_platform_tree]
 * renders the band with `zoomIn = false, zoomOut = false` and asserts BOTH steppers report `isEnabled = false`
 * to the platform tree TalkBack reads, not merely to Compose's merged tree (`assertIsNotEnabled` passed against
 * this very control while it told the platform otherwise). This is the CI-93 disabled clause the earlier note
 * said was absent; against the current tree it is present, so it is asserted rather than deferred.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ReframeControlsRolePlatformA11yTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    /** All verbs available — the fully-lit band the button-role sweep needs (every control enabled). */
    private val allAbilities = ReframeAbilities(
        zoomIn = true, zoomOut = true, panHorizontally = true, panVertically = true,
    )

    private fun render(abilities: ReframeAbilities = allAbilities) {
        composeRule.setContent {
            ZinelyTheme {
                ReframeControls(
                    fit = FrameFit.FILL,
                    zoomPercent = 100,
                    abilities = abilities,
                    onFit = {},
                    onNudge = { _, _ -> },
                    onZoom = {},
                    onReset = {},
                    onCancel = {},
                    onDone = {},
                )
            }
        }
    }

    private fun assertPlatformButton(spokenLabel: String) {
        val node = composeRule.onNodeWithContentDescription(spokenLabel).platformNode(composeRule.activity)
        assertEquals("$spokenLabel must carry Role.Button to the platform tree", "android.widget.Button", node.className)
        assertTrue("$spokenLabel must be enabled to the platform", node.isEnabled)
    }

    @Test
    fun every_reframe_control_reports_button_role_on_the_platform_tree() {
        render() // all abilities available — every control lit.

        // Nudge pad (4 cross cells).
        assertPlatformButton("Move photo up")
        assertPlatformButton("Move photo left")
        assertPlatformButton("Move photo right")
        assertPlatformButton("Move photo down")

        // Zoom steppers.
        assertPlatformButton("Zoom in")
        assertPlatformButton("Zoom out")

        // Fit chips, Reset, Cancel, Done. The chips stay in this sweep deliberately: a `selectable` would
        // have delivered their state at the cost of the click action asserted here, and
        // [the_platform_is_told_which_fit_is_chosen] is what makes that trade unnecessary. Both properties
        // must hold at once, so both are asserted.
        assertPlatformButton("Fill")
        assertPlatformButton("Whole photo")
        assertPlatformButton("Reset framing")
        assertPlatformButton("Cancel reframing")
        assertPlatformButton("Done reframing")
    }

    /**
     * **The fit chips tell the platform which one is on** — and this test exists because nothing asserted
     * that, not because it was broken.
     *
     * The V2.1 re-skin's Pass 1 read `uiautomator dump`, saw `selected="false"` on *both* chips, and
     * concluded the active fit was announced no differently from the inactive one. That conclusion was
     * wrong, and how it was disproved is why the test is worth keeping: the explicit `stateDescription`
     * written to "fix" it was deleted again, and this assertion stayed green. Compose supplies
     * *"Selected"* / *"Not selected"* for any node carrying `SemanticsProperties.Selected`. The dump's
     * `selected="false"` is a true fact about a **different** attribute — `isSelected`, which Compose maps
     * `Selected` onto for `Role.Tab` alone — and the XML never prints `stateDescription` at all. A dump
     * that does not show a thing is not evidence the thing is absent.
     *
     * What it guards is real and was undefended: the platform's fallback holds **only while no explicit
     * `stateDescription` is set**, because one replaces the platform's rather than adding to it. The day a
     * chip wants to say anything else about itself — the way an element outside the printer's reach does
     * ([com.aritr.zinely.core.copy.Copy.A11y.outsidePrintReachState], which has to carry *"Selected"*
     * along with its own text for exactly this reason) — this test is what fails.
     *
     * `isSelected` staying false is pinned deliberately too, so the day Compose starts honouring it for
     * this role the test says so rather than quietly agreeing with a comment.
     */
    @Test
    fun the_platform_is_told_which_fit_is_chosen() {
        render() // rendered at FrameFit.FILL

        val fill = composeRule.onNodeWithContentDescription("Fill").platformNode(composeRule.activity)
        val whole = composeRule.onNodeWithContentDescription("Whole photo").platformNode(composeRule.activity)

        assertEquals("the chosen fit's state never reaches a service", "Selected", fill.stateDescription)
        assertEquals("the other fit's state never reaches a service", "Not selected", whole.stateDescription)
        assertTrue(
            "Compose now maps Selected → isSelected for this role; prefer that and simplify this test",
            !fill.isSelected && !whole.isSelected,
        )
    }

    /**
     * CI-93 disabled clause on the real `f4faaa4` control. Rendered with `zoomIn = false, zoomOut = false`
     * (the `ReframeAbilities` state a Whole-photo / clamped fit produces), both zoom steppers must report
     * `isEnabled = false` to the PLATFORM tree — the bit an accessibility service consumes and the bit that
     * lied in `f4faaa4` (green under `assertIsNotEnabled` on the merged tree, `enabled = true` to TalkBack).
     * The `ZoomButton`'s `clickable(enabled = abilities.zoomOut/.zoomIn, role = Role.Button)` collapses to a
     * single `android.widget.Button` carrying label, role and the disabled state together, so the class is
     * still `android.widget.Button` here even while disabled (unlike a merged-content ZToolButton).
     */
    @Test
    fun the_disabled_zoom_steppers_report_disabled_on_the_platform_tree() {
        render(
            ReframeAbilities(
                zoomIn = false,
                zoomOut = false,
                // Pan left available so the band still has a lit control — the assertion is about the two
                // zoom steppers being OFF, not the whole bar being dead.
                panHorizontally = true,
                panVertically = true,
            ),
        )

        val zoomIn = composeRule.onNodeWithContentDescription("Zoom in").platformNode(composeRule.activity)
        assertEquals("android.widget.Button", zoomIn.className)
        assertFalse(
            "Zoom in must be DISABLED to the platform tree when abilities.zoomIn = false (the f4faaa4 bit)",
            zoomIn.isEnabled,
        )

        val zoomOut = composeRule.onNodeWithContentDescription("Zoom out").platformNode(composeRule.activity)
        assertEquals("android.widget.Button", zoomOut.className)
        assertFalse(
            "Zoom out must be DISABLED to the platform tree when abilities.zoomOut = false (the f4faaa4 bit)",
            zoomOut.isEnabled,
        )
    }
}
