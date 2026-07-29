package com.aritr.zinely.ui.a11y

import androidx.activity.ComponentActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

/**
 * The A8 acceptance test for [zinelyV2Control], asserted against the **platform**
 * `AccessibilityNodeInfo` tree via the CI-26 harness — the tree TalkBack reads, not Compose's merged
 * semantics tree.
 *
 * ## The fixture is the test
 *
 * The [ADR-059](docs/DECISIONS.md#adr-059) defect appears in exactly one shape: a **container** that
 * carries the click and wraps a child contributing semantics. A first version of this file put the
 * modifier directly on a `Text` — which makes the text node *itself* the control, with no child at all —
 * and every assertion below then passed against a bare `Modifier.clickable(role = …)`. It could not tell
 * the seam from nothing. Measured on this host:
 *
 * | fixture | class | spoken name |
 * |---|---|---|
 * | modifier on the `Text` itself (no child) | `android.widget.Button` | "Alpha" |
 * | container wrapping a `Text`, plain `clickable` + `semantics` | **`android.view.View`** | **`null`** |
 * | container wrapping a `Text`, through [zinelyV2Control] | `android.widget.Button` | "Gamma" |
 *
 * The middle row is the shipped V1 `ZButton` shape, and it loses **both** the role class and the label.
 * So every test here uses [wrapped] — a container round a text child — and one test pins the middle row
 * explicitly. If Compose ever stops collapsing merged roles, that control test goes red and says the seam
 * has become unnecessary; nothing else in the repository would notice.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ZinelyV2ControlPlatformA11yTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun node(tag: String): PlatformA11yNode =
        composeRule.onNodeWithTag(tag).platformNode(composeRule.activity)

    /**
     * A container carrying the control modifier and wrapping a text child — the V1 `ZButton` shape.
     *
     * The `testTag` goes **before** the control modifier, and that is not cosmetic: `zinelyV2Control`
     * ends in `clearAndSetSemantics`, which discards every semantic declared after it — a tag included.
     * A tag applied afterwards makes the node unfindable, which is how this was discovered.
     */
    @Composable
    private fun wrapped(tag: String, text: String, modifier: Modifier) {
        Box(Modifier.testTag(tag).then(modifier)) { BasicText(text) }
    }

    private fun setSeamControls() {
        composeRule.setContent {
            Column {
                wrapped("v2-on", "Start a zine", Modifier.zinelyV2Control("Start a zine", onClick = {}))
                wrapped("v2-off", "Print", Modifier.zinelyV2Control("Print", onClick = {}, enabled = false))
            }
        }
    }

    @Test
    fun `the V1 shape really does lose its role and its name - the defect this seam exists to prevent`() {
        // Not a test of our code. It pins the platform behaviour every assertion below is meaningful
        // against, so a seam that silently became a no-op cannot pass by accident.
        composeRule.setContent {
            wrapped(
                "defectControl",
                "Beta",
                Modifier
                    .clickable(enabled = false, role = Role.Button) {}
                    .semantics { contentDescription = "Beta" },
            )
        }
        val n = node("defectControl")
        assertEquals(
            "if this is no longer android.view.View, Compose has changed and the seam may be redundant",
            "android.view.View",
            n.className,
        )
        assertEquals("the merged container also swallows the spoken name", null, n.contentDescription)
    }

    @Test
    fun `a container wrapping a text child still reaches the platform as a Button`() {
        setSeamControls()
        assertEquals("android.widget.Button", node("v2-on").className)
        assertNotEquals("android.view.View", node("v2-on").className)
    }

    @Test
    fun `the disabled twin keeps its Button class - a disabled control is still announced as a button`() {
        setSeamControls()
        assertEquals("android.widget.Button", node("v2-off").className)
    }

    @Test
    fun `the enabled bit reaches the platform, not only the merged tree`() {
        setSeamControls()
        assertTrue("the enabled control must be enabled to the platform", node("v2-on").isEnabled)
        assertFalse(
            "the disabled control must be disabled to the platform — the f4faaa4 bit",
            node("v2-off").isEnabled,
        )
    }

    @Test
    fun `the click survives the clear, so an accessibility service can activate the control`() {
        setSeamControls()
        assertTrue(
            "the platform must advertise a click — if clickable sat INSIDE clearAndSetSemantics the " +
                "action would be discarded and assistive tech could not fire it (the IF5 correction)",
            node("v2-on").isClickable,
        )
    }

    @Test
    fun `the label reaches the platform as the spoken name, which the merged container loses`() {
        setSeamControls()
        assertEquals("Start a zine", node("v2-on").contentDescription)
        assertEquals("Print", node("v2-off").contentDescription)
    }

    @Test
    fun `a non-button role also survives, with its selected state`() {
        // ADR-059 records the same collapse for Role.RadioButton and Role.Checkbox; C6's TypeBar radios
        // are the call site that will need this.
        composeRule.setContent {
            wrapped(
                "v2-radio",
                "Left",
                Modifier.zinelyV2Control("Align left", onClick = {}, role = Role.RadioButton, selected = true),
            )
        }
        assertEquals("android.widget.RadioButton", node("v2-radio").className)
        assertEquals("Align left", node("v2-radio").contentDescription)
        // `selected` is the one thing in the cleared block that `clickable` cannot say, so it is the one
        // thing that must be asserted from the block. The CI-26 harness does not snapshot the platform's
        // selected bit, so this reads the semantics node — narrower evidence, stated as such.
        assertEquals(
            true,
            composeRule.onNodeWithTag("v2-radio").fetchSemanticsNode()
                .config[SemanticsProperties.Selected],
        )
    }

    @Test
    fun `a plain button declares nothing about selection, rather than declaring itself unselected`() {
        // The KDoc promises this explicitly, and only line-DELETION was caught before: replacing the
        // null-check with `this.selected = (selected == true)` left the suite green while every plain
        // button began telling a service it was an unselected something. Absence is the assertion.
        setSeamControls()
        assertFalse(
            "a button with no selection state must not publish Selected at all",
            composeRule.onNodeWithTag("v2-on").fetchSemanticsNode()
                .config.contains(SemanticsProperties.Selected),
        )
    }

    /**
     * The minimum touch target, read off the platform node — and a one-pixel rounding artefact worth
     * stating rather than tuning away.
     *
     * A one-glyph container lays out at 7x16 px at density 1.0. `minimumInteractiveComponentSize()` does
     * not resize that node; it widens the **touch bounds** around it, and those are what Compose reports
     * to the platform. So the node arrives near 48x48 rather than 7x16 — the enforcement is real, and it
     * is the whole difference between this and a bare `clickable`.
     *
     * The missing pixel is arithmetic, not a gap in the target. The touch bounds land on half-pixels
     * (`0.5 .. 48.5` for the earlier measured case), and `getBoundsInScreen`'s conversion rounds the two
     * edges in opposite directions — `0.5` up to 1, `48.5` down to 48 — so the reported span is 47 where
     * the real one is 48. At a device density of 2.625-3.5 that pixel is under 0.4dp.
     *
     * The allowance is exactly one pixel, and it is nowhere near vacuous: an unenforced target would
     * report the content's own 7px and miss by forty. Dropping `minimumInteractiveComponentSize()` from
     * the seam turns this test red.
     */
    @Test
    fun `the control presents at least the platform minimum touch target, to within a rounding pixel`() {
        composeRule.setContent {
            wrapped("v2-tiny", "x", Modifier.zinelyV2Control("Tiny", onClick = {}))
        }
        val bounds = node("v2-tiny").boundsInScreen
        // 48.dp directly, not ZinelyV2CanvasNodeMinSide: that constant is the *canvas* node floor and
        // only coincidentally the same number. A laid-out control's floor is the platform's own.
        val minPx = with(composeRule.density) { 48.dp.toPx() }.toInt()
        assertTrue(
            "a one-glyph control must still present a ~${minPx}px target to the platform " +
                "(got ${bounds.width()}x${bounds.height()} px)",
            bounds.width() >= minPx - 1 && bounds.height() >= minPx - 1,
        )
    }
}
