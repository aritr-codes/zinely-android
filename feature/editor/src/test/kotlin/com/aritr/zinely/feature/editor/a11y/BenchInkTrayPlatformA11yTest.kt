package com.aritr.zinely.feature.editor.a11y

import android.view.View
import android.view.accessibility.AccessibilityNodeInfo
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import com.aritr.zinely.core.copy.Copy
import com.aritr.zinely.core.editor.ink.InkHex
import com.aritr.zinely.core.editor.ink.InkLibrary
import com.aritr.zinely.core.editor.ink.InkPalette
import com.aritr.zinely.core.editor.ink.MyInk
import com.aritr.zinely.core.editor.ink.PaletteDraft
import com.aritr.zinely.feature.editor.BenchInkCountTestTag
import com.aritr.zinely.feature.editor.BenchInkCurrentTestTag
import com.aritr.zinely.feature.editor.BenchInkKeptTestTag
import com.aritr.zinely.feature.editor.BenchInkMixTestTag
import com.aritr.zinely.feature.editor.BenchInkPalettePotTestTag
import com.aritr.zinely.feature.editor.BenchInkPopover
import com.aritr.zinely.feature.editor.BenchInkSavePaletteTestTag
import com.aritr.zinely.feature.editor.BenchInkStripTestTag
import com.aritr.zinely.feature.editor.BenchInkSwatchTestTag
import com.aritr.zinely.feature.editor.BenchInkToggleTestTag
import com.aritr.zinely.feature.editor.BenchVerbKind
import com.aritr.zinely.feature.editor.toComposeColor
import com.aritr.zinely.ui.a11y.PlatformA11yNode
import com.aritr.zinely.ui.a11y.platformNode
import com.aritr.zinely.ui.theme.ZinelyMakerInkId
import com.aritr.zinely.ui.theme.ZinelyTheme
import com.aritr.zinely.ui.theme.zinelyContentInks
import com.aritr.zinely.feature.editor.inkHex
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

/**
 * The Ink tray (A40 rev. 2, INK task 9) on the **platform** tree TalkBack reads, not Compose's merged
 * semantics: name, role class, checked state, actions and live regions per node.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class BenchInkTrayPlatformA11yTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val grey = InkHex("505955")
    private val red = InkHex("C0392B")
    private val blue = InkHex("2E6F95")

    private val library = InkLibrary(
        myInks = listOf(MyInk(grey, 3), MyInk(red, 2)),
        palettes = listOf(InkPalette(2, "Sea", listOf(blue, red)), InkPalette(1, "Warm", listOf(red, grey))),
        clock = 3,
        nextPaletteId = 3,
        openPaletteId = 2,
    )

    private fun tray(selected: InkHex? = grey, draft: PaletteDraft? = null) {
        composeRule.setContent {
            ZinelyTheme {
                Box(Modifier.size(411.dp, 891.dp)) {
                    BenchInkPopover(
                        visible = true,
                        kind = BenchVerbKind.TEXT,
                        selected = selected?.toColorRgba()?.toComposeColor(),
                        onPick = {},
                        onDone = {},
                        inkLibrary = library,
                        draft = draft,
                    )
                }
            }
        }
        composeRule.waitForIdle()
    }

    /**
     * Scrolled into view first: the platform publishes nothing for a node the tray's scroll has not placed.
     * The pinned edit bar sits outside the scroll and has nothing to scroll.
     */
    private fun tagged(tag: String, i: Int = 0): PlatformA11yNode =
        composeRule.onAllNodesWithTag(tag)[i].also { runCatching { it.performScrollTo() }; composeRule.waitForIdle() }
            .platformNode(composeRule.activity)

    private fun named(name: String): PlatformA11yNode =
        composeRule.onNodeWithContentDescription(name).also { it.performScrollTo(); composeRule.waitForIdle() }
            .platformNode(composeRule.activity)

    @Test
    fun `given a kept ink is current then its pot is a named, checked radio with Remove from My inks`() {
        // Given the tray over a piece inked dark grey, which is kept
        tray(selected = grey)

        // Then the kept pot speaks its colour words and its digits singly, is checked, and carries the action
        val kept = tagged(BenchInkKeptTestTag, 0)
        assertEquals("My ink, dark grey, 5 0 5 9 5 5", kept.contentDescription)
        assertEquals("android.widget.RadioButton", kept.className)
        assertTrue(kept.isEnabled)
        // A checked radio: the platform says "selected" as its state (the prototype's ", selected" suffix).
        assertTrue("the current ink is the checked radio", kept.isCheckable && kept.isChecked)
        assertTrue(Copy.BenchInk.REMOVE_FROM_MY_INKS in kept.actionLabels)
        assertTrue(kept.isLongClickable)
        // And the other kept pot is a clickable, unchecked radio with the same action
        val other = tagged(BenchInkKeptTestTag, 1)
        assertEquals("My ink, red, C 0 3 9 2 B", other.contentDescription)
        assertTrue(other.isClickable)
        assertFalse(other.isChecked)
        assertTrue(Copy.BenchInk.REMOVE_FROM_MY_INKS in other.actionLabels)
    }

    @Test
    fun `given the tray then a built-in pot is its name only, Ink is listed once, and the current one is checked`() {
        tray(selected = zinelyContentInks()[ZinelyMakerInkId.Ink].value.inkHex()) // a fresh piece: the default ink

        val swatches = composeRule.onAllNodesWithTag(BenchInkSwatchTestTag).fetchSemanticsNodes()
        val names = swatches.indices.map { tagged(BenchInkSwatchTestTag, it).contentDescription }
        assertEquals("Ink appears once among the built-ins", 1, names.count { it == Copy.BenchInk.INK })
        val ink = tagged(BenchInkSwatchTestTag, names.indexOf(Copy.BenchInk.INK))
        assertTrue(ink.isChecked)
        assertEquals("android.widget.RadioButton", ink.className)
        assertTrue(names.filter { it != Copy.BenchInk.INK }.none { tagged(BenchInkSwatchTestTag, names.indexOf(it)).isChecked })
    }

    @Test
    fun `given palettes then a pot says its palette, a strip is a checked radio with its count, and Mix is a button`() {
        tray(selected = red)

        // The open palette's pots: "‹name›, from ‹palette›", the current one checked
        val pot = tagged(BenchInkPalettePotTestTag, 1)
        assertEquals("red, from Sea", pot.contentDescription)
        assertTrue(pot.isChecked)
        assertFalse(tagged(BenchInkPalettePotTestTag, 0).isChecked)

        // The strips: radios, the open one checked, "‹palette›, N inks"
        val open = tagged(BenchInkStripTestTag, 0)
        assertEquals(Copy.BenchInk.paletteGroup("Sea", 2), open.contentDescription)
        assertTrue(open.isCheckable && open.isChecked)
        val other = tagged(BenchInkStripTestTag, 1)
        assertEquals(Copy.BenchInk.paletteGroup("Warm", 2), other.contentDescription)
        assertTrue(other.isCheckable)
        assertFalse(other.isChecked)
        assertEquals("android.widget.RadioButton", open.className)

        // Mix: a named button
        val mix = tagged(BenchInkMixTestTag)
        assertEquals(Copy.BenchInk.MIX_AN_INK, mix.contentDescription)
        assertEquals("android.widget.Button", mix.className)
        assertTrue(mix.isClickable)
    }

    @Test
    fun `given the tray then the current ink row is focusable and is not a live region`() {
        tray(selected = grey)

        val row = tagged(BenchInkCurrentTestTag)
        assertEquals(View.ACCESSIBILITY_LIVE_REGION_NONE, row.liveRegion)
        assertTrue(row.isFocusable)
    }

    @Test
    fun `given edit mode then toggles are checkable and follow the draft, kept ones without the My ink prefix`() {
        // Given an edit holding red and blue
        tray(draft = PaletteDraft(id = 2, name = "Sea", startName = "Sea", inks = listOf(blue, red), orig = listOf(blue, red)))

        // Then the kept grey toggle reads "‹name›, ‹digits›", unchecked; the kept red one is checked
        val greyToggle = named("dark grey, 5 0 5 9 5 5")
        assertTrue(greyToggle.isCheckable)
        assertFalse(greyToggle.isChecked)
        assertTrue(greyToggle.isClickable)
        assertEquals("android.widget.CheckBox", greyToggle.className)
        val toggles = composeRule.onAllNodesWithTag(BenchInkToggleTestTag).fetchSemanticsNodes().indices.map { tagged(BenchInkToggleTestTag, it) }
        assertTrue("no edit toggle says My ink", toggles.none { it.contentDescription!!.startsWith("My ink") })
        // Red is kept, so its toggle shows checked; blue is offered nowhere below (only in the strip above).
        assertEquals(1, toggles.count { it.isChecked })
        // And the count line is a polite status, not a toggle's echo
        assertEquals(View.ACCESSIBILITY_LIVE_REGION_POLITE, tagged(BenchInkCountTestTag).liveRegion)
    }

    @Test
    fun `given too few inks then the dim Save is still an enabled, focusable button that says why`() {
        // Given an edit with one ink: Save is drawn at 50%
        tray(draft = PaletteDraft(id = null, name = "My palette", startName = "My palette", inks = listOf(red), orig = emptyList()))

        // Then the platform does not call it disabled (the ZoomButton lesson), and it states the reason
        val save = tagged(BenchInkSavePaletteTestTag)
        assertTrue("a false isEnabled would hide the reason a tap gives", save.isEnabled)
        assertTrue(save.isClickable)
        assertTrue(save.isFocusable)
        assertTrue(AccessibilityNodeInfo.ACTION_CLICK in save.actionIds)
        assertEquals(Copy.BenchInk.PICK_AT_LEAST_TWO, save.stateDescription)
    }

    @Test
    fun `given two inks but no name then the dim Save says to name it, and a savable Save says nothing extra`() {
        tray(draft = PaletteDraft(id = null, name = "  ", startName = "My palette", inks = listOf(red, blue), orig = emptyList()))
        assertEquals(Copy.BenchInk.NAME_EMPTY_HELP, tagged(BenchInkSavePaletteTestTag).stateDescription)
    }

    @Test
    fun `given a savable draft then Save carries no reason`() {
        tray(draft = PaletteDraft(id = null, name = "Mine", startName = "My palette", inks = listOf(red, blue), orig = emptyList()))
        assertEquals(null, tagged(BenchInkSavePaletteTestTag).stateDescription)
    }
}
