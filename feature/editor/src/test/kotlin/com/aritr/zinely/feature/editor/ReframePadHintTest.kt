package com.aritr.zinely.feature.editor

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import com.aritr.zinely.core.copy.Copy
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
 * **F-4 — the Reframe pad's entry state says what revives it.**
 *
 * A newly placed photo's frame is seeded to the photo's own aspect, so at 100% Fill it overflows on neither
 * axis: [ReframeAbilities] reports all four nudges and `Zoom out` unavailable, and the surface opens with
 * five of its seven controls correctly dead. A device pass read that as breakage rather than as "not yet"
 * (`docs/BETA-UX-REVIEW.md` F-4), which is the same finding as F-1 on the Bench and answered by the same
 * house rule: **a control that is drawn and disabled says why.**
 *
 * `.padhint` (`v21-reframe.html`, revised 2026-08-15) is the spec's answer. The two properties worth a test
 * are the ones a later refactor would break silently:
 *
 * 1. it is **there** in the state that motivated it, and
 * 2. it is **gone** — not hidden, not merely invisible — the moment a nudge goes live, because a stale
 *    instruction still in the tree is worse than none. TalkBack would happily read *"Zoom in to move the
 *    photo"* on a pad whose arrows already work.
 *
 * The second is the assertion with teeth: `assertDoesNotExist` fails if the hint is ever implemented as an
 * alpha or a visibility flag rather than as composition.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ReframePadHintTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    /**
     * The real entry state, not an invented one: a freshly placed photo can zoom **in** and do nothing
     * else. `Zoom out` is dead here for the same reason the nudges are, and the hint deliberately does not
     * name it — it names the one act that is always available, so it can never advise an unavailable
     * control.
     */
    private val entryState = ReframeAbilities(
        zoomIn = true, zoomOut = false, panHorizontally = false, panVertically = false,
    )

    private fun host(abilities: ReframeAbilities) {
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
        composeRule.waitForIdle()
    }

    @Test
    fun the_entry_state_names_the_act_that_revives_the_pad() {
        host(entryState)

        composeRule.onNodeWithTag(ReframePadHintTestTag)
            .assertIsDisplayed()
            .assertTextEquals(Copy.Reframe.ZOOM_IN_TO_MOVE)
    }

    @Test
    fun the_hint_leaves_the_tree_the_moment_one_axis_goes_live() {
        // Horizontal only: the pad is half alive, which is the case a "some nudge works" reading would get
        // wrong in the generous direction. The frozen rule is EVERY nudge dead, so one live axis removes it.
        host(entryState.copy(panHorizontally = true))

        composeRule.onNodeWithTag(ReframePadHintTestTag).assertDoesNotExist()
    }

    @Test
    fun the_hint_is_absent_on_a_fully_live_pad() {
        host(ReframeAbilities(zoomIn = true, zoomOut = true, panHorizontally = true, panVertically = true))

        composeRule.onNodeWithTag(ReframePadHintTestTag).assertDoesNotExist()
    }

    /**
     * The state the spec's own invariant misses, found by review rather than by the device.
     *
     * `v21-reframe.html` argues the hint "can never contradict itself by advising an unavailable control"
     * because `Zoom in` is live at the entry state. That is true of **Fill**, and false one chip away:
     * `Framing.abilities()` returns [ReframeAbilities.NONE] for `FrameFit.WHOLE`, and `EditorScreen` uses
     * the same `NONE` for the inert state before a photo's aspect resolves (M7-01). Every nudge is dead in
     * both — and so is the very control the sentence tells the maker to press.
     *
     * So the condition carries a `zoomIn` term the spec's sentence does not, and this is the test that
     * would fail if a later reader "simplified" it back to the spec's wording.
     */
    @Test
    fun the_hint_never_advises_a_zoom_the_maker_cannot_reach() {
        host(ReframeAbilities.NONE)

        composeRule.onNodeWithTag(ReframePadHintTestTag).assertDoesNotExist()
    }

    /**
     * The accessibility half of F-4, asserted where TalkBack reads: the **platform** node.
     *
     * The spec binds the hint with `aria-describedby`, so a screen reader states it on *entering* the group
     * rather than after the controls it explains. The first attempt reproduced that with a traversal group
     * and `traversalIndex = -1f` — and an honest test killed it: `platformTraversalStops` returned
     * `[Move photo up, …, Zoom in, Zoom in to move the photo]` with the index set, byte-identical to the
     * order without it, because Compose expresses re-sorting through `setTraversalBefore/After` and this
     * host leaves those `UNDEFINED`. A device `uiautomator dump` agreed. **The mechanism did nothing.**
     *
     * What works needs no ordering: the reason rides the dead controls themselves, the same remedy F-1 uses
     * on the Bench. Whichever arrow the maker reaches first tells them what to do.
     */
    @Test
    fun every_dead_arrow_says_what_would_revive_it() {
        host(entryState)

        for (arrow in listOf(
            Copy.Reframe.MOVE_PHOTO_UP,
            Copy.Reframe.MOVE_PHOTO_DOWN,
            Copy.Reframe.MOVE_PHOTO_LEFT,
            Copy.Reframe.MOVE_PHOTO_RIGHT,
        )) {
            val node = composeRule.onNodeWithContentDescription(arrow).platformNode(composeRule.activity)
            assertFalse("$arrow must be dead at the entry state", node.isEnabled)
            assertEquals(
                "$arrow must tell the platform what would revive it",
                Copy.Reframe.ZOOM_IN_TO_MOVE,
                node.stateDescription?.toString(),
            )
            assertEquals("the reason must not migrate into the name", arrow, node.contentDescription?.toString())
        }
    }

    /**
     * ...and it goes away with the hint, so the two channels can never disagree. A live arrow carrying
     * "Zoom in to move the photo" would be the audible twin of the stale-hint defect the tests above forbid.
     */
    @Test
    fun a_live_arrow_carries_no_reason() {
        host(entryState.copy(panHorizontally = true, panVertically = true))

        val node = composeRule.onNodeWithContentDescription(Copy.Reframe.MOVE_PHOTO_UP)
            .platformNode(composeRule.activity)
        assertTrue(node.isEnabled)
        assertEquals(null, node.stateDescription?.toString())
    }
}
