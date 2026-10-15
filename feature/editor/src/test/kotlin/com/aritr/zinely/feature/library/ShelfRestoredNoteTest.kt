package com.aritr.zinely.feature.library

import android.accessibilityservice.AccessibilityServiceInfo
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityManager
import android.view.accessibility.AccessibilityNodeInfo
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import com.aritr.zinely.core.model.ZineCoverRecipe
import com.aritr.zinely.core.model.ZineCoverStamp
import com.aritr.zinely.core.model.ZineCoverSurface
import com.aritr.zinely.ui.a11y.platformNode
import com.aritr.zinely.ui.a11y.platformTraversalStops
import com.aritr.zinely.ui.theme.ZinelyTheme
import kotlinx.coroutines.flow.emptyFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The restored-details note on My Shelf — `v21-library.html` A37, [ADR-134](docs/DECISIONS.md#adr-134). The words
 * are written out here as literals from A37.3, not read back from `Copy`.
 *
 * TalkBack's side is checked on the **platform** node (what TalkBack reads), not only Compose's merged tree:
 * one node for the title and body, a polite live region only when the note arrives on a Shelf already drawn, and
 * none on re-entry or once the words change. What TalkBack then says is the device pass's.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w392dp-h812dp", sdk = [33])
class ShelfRestoredNoteTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private var state by mutableStateOf<LibraryShelfState>(LibraryShelfState.Loading)
    private val dismissed = mutableListOf<List<String>>()
    private lateinit var inputMode: InputModeManager

    private val oneBack = listOf(RestoredZine("Letters home", "Letters home", "Letters to Gran"))
    private val spokenOneBack = "“Letters to Gran” is called “Letters home” again. A small file that holds its name, " +
        "cover and folder was damaged, so Zinely used the copy from before the last change to them. " +
        "You can rename it again."

    @Test
    fun `the note sits under the heading, says A37's words as one node, and Got it is the next stop`() {
        show(LibraryShelfState.Content(ZINES, oneBack))

        assertEquals(spokenOneBack, said())
        val head = composeRule.onNodeWithTag(ZineShelfHeadTestTag).getBoundsInRoot()
        val note = composeRule.onNodeWithTag(ShelfRestoredNoteTestTag).getBoundsInRoot()
        val cover = composeRule.onNodeWithTag(zineShelfCoverTestTag(0)).getBoundsInRoot()
        assertTrue("under the heading", note.top >= head.bottom)
        assertTrue("above the first zine", note.bottom <= cover.top)

        // Platform order: My Shelf, its count, the note, Got it, then the zines.
        val stops = platformTraversalStops(composeRule.activity).map { it.label }
        val at = stops.indexOf(spokenOneBack)
        assertTrue("the note is one stop: $stops", at > 0)
        assertEquals(listOf("My Shelf", "6 zines"), stops.take(at).takeLast(2))
        assertEquals("Got it", stops[at + 1])
        val ok = composeRule.onNodeWithTag(ShelfRestoredNoteOkTestTag).platformNode(composeRule.activity)
        assertEquals("android.widget.Button", ok.className)
        assertTrue("48dp tall at least", ok.boundsInScreen.height() >= px(48))
    }

    @Test
    fun `a note there when the Shelf first draws is read in order, not announced`() {
        show(LibraryShelfState.Content(ZINES, oneBack))

        assertEquals(View.ACCESSIBILITY_LIVE_REGION_NONE, platformText().liveRegion)
        assertNull(textConfig(SemanticsProperties.LiveRegion))
    }

    @Test
    fun `a note arriving on a Shelf already drawn is announced once, with its words put in after it`() {
        turnAccessibilityOn()
        show(LibraryShelfState.Content(ZINES))
        composeRule.onNodeWithTag(ShelfRestoredNoteTestTag).assertDoesNotExist()

        composeRule.mainClock.autoAdvance = false
        update(LibraryShelfState.Content(ZINES, oneBack))
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.mainClock.advanceTimeByFrame()

        // Placed, a live region, and not yet saying anything.
        assertEquals(View.ACCESSIBILITY_LIVE_REGION_POLITE, platformText().liveRegion)
        assertNull(said())

        composeRule.mainClock.advanceTimeBy(ArrivalSettleMillis + 50)
        assertEquals(spokenOneBack, said())
        val node = platformText()
        assertEquals(View.ACCESSIBILITY_LIVE_REGION_POLITE, node.liveRegion)
        assertEquals(spokenOneBack, node.contentDescription?.toString())
    }

    @Test
    fun `once the words change while the note is up, the change is silent`() {
        show(LibraryShelfState.Content(ZINES))
        update(LibraryShelfState.Content(ZINES, oneBack))
        composeRule.mainClock.advanceTimeBy(ArrivalSettleMillis + 50)
        composeRule.waitForIdle()
        assertTrue(textConfig(SemanticsProperties.LiveRegion) != null)
        assertEquals("the words are in once the arrival has settled", spokenOneBack, said())

        // A second zine joins (or the first is renamed): the words change, and the live region is gone with it.
        update(LibraryShelfState.Content(ZINES, oneBack + RestoredZine("Riso tests", "Riso tests", null)))
        composeRule.waitForIdle()

        assertEquals(
            "Zinely repaired 2 zines. “Letters home” (was “Letters to Gran”) and “Riso tests”. Small files that hold " +
                "their names, covers and folders were damaged, so Zinely used the copies from before the last changes " +
                "to them.",
            said(),
        )
        assertNull(textConfig(SemanticsProperties.LiveRegion))
        assertEquals(View.ACCESSIBILITY_LIVE_REGION_NONE, platformText().liveRegion)
    }

    @Test
    fun `the note waits on My Shelf, is not in a folder, and is not announced coming back out`() {
        val inFolder = ZINES.mapIndexed { i, z -> if (i < 2) z.copy(folder = "Family") else z }
        show(LibraryShelfState.Content(inFolder))
        update(LibraryShelfState.Content(inFolder, oneBack))
        composeRule.waitForIdle()

        composeRule.onNodeWithContentDescription("Family, folder, 2 zines").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(ShelfRestoredNoteTestTag).assertDoesNotExist()

        composeRule.onNodeWithContentDescription("Back to My Shelf").performClick()
        composeRule.waitForIdle()
        assertEquals(spokenOneBack, said())
        assertNull("re-entry announces nothing", textConfig(SemanticsProperties.LiveRegion))
    }

    @Test
    fun `Got it dismisses every zine the note names and puts focus on the My Shelf heading`() {
        val two = oneBack + RestoredZine("Riso tests", "Riso tests", null)
        show(LibraryShelfState.Content(ZINES, two))
        composeRule.runOnUiThread { assertTrue(inputMode.requestInputMode(InputMode.Keyboard)) }

        composeRule.onNodeWithTag(ShelfRestoredNoteOkTestTag).performClick()
        update(LibraryShelfState.Content(ZINES)) // the store lists the Shelf without them
        composeRule.waitForIdle()

        assertEquals(listOf(listOf("Letters home", "Riso tests")), dismissed)
        composeRule.onNode(isHeading()).assertIsFocused()
        composeRule.onNodeWithTag(ShelfRestoredNoteTestTag).assertDoesNotExist()
    }

    @Test
    fun `no note in the loading, empty or error states`() {
        show(LibraryShelfState.Loading)
        for (s in listOf(LibraryShelfState.Loading, LibraryShelfState.Empty, LibraryShelfState.Error)) {
            update(s)
            composeRule.waitForIdle()
            composeRule.onNodeWithTag(ShelfRestoredNoteTestTag).assertDoesNotExist()
        }
    }

    @Test
    @Config(qualifiers = "w360dp-h800dp")
    fun `at text size 2 on a 360dp phone every word shows and Got it stays under them, 48dp tall`() {
        val five = listOf("Letters home", LONG_NAME, "Mum’s garden", "Tiny poems", "Coffee log")
            .map { RestoredZine(it, it, if (it == "Letters home") "Letters to Gran" else null) }
        show(LibraryShelfState.Content(ZINES, five), fontScale = 2f)

        val root = composeRule.onRoot().getBoundsInRoot()
        val note = composeRule.onNodeWithTag(ShelfRestoredNoteTestTag).getBoundsInRoot()
        val text = composeRule.onNodeWithTag(ShelfRestoredNoteTextTestTag).getBoundsInRoot()
        val ok = composeRule.onNodeWithTag(ShelfRestoredNoteOkTestTag).getBoundsInRoot()
        assertTrue("no sideways overflow", note.left >= root.left && note.right <= root.right)
        assertTrue("Got it is under the words, never beside them", ok.top >= text.bottom)
        assertTrue("Got it is inside the note", ok.bottom <= note.bottom)
        assertTrue("48dp tall", ok.height >= 48.dp)
        // Every word: the note's one node carries all five, three named and "and 2 more" (A37.3), and the text has
        // no line limit, so it is as tall as its words need.
        assertTrue(said()!!.contains("“Letters home” (was “Letters to Gran”), “$LONG_NAME”, “Mum’s garden” and 2 more."))
        // A37.8: a name with no space in it breaks anywhere; the words stay inside the note.
        assertTrue("the words stay inside the note", text.left >= note.left && text.right <= note.right)
    }

    // ---------------------------------------------------------------------------------------------

    private fun show(initial: LibraryShelfState, fontScale: Float = 1f) {
        state = initial
        composeRule.setContent { Screen(fontScale) }
        composeRule.waitForIdle()
    }

    /**
     * The store lists the Shelf again. The apply notification is sent here, on the test's own clock, rather than
     * left to the main looper, so a test that holds the clock still sees the change in the frame it advances.
     */
    private fun update(next: LibraryShelfState) {
        state = next
        Snapshot.sendApplyNotifications()
    }

    @Composable
    private fun Screen(fontScale: Float) {
        val base = LocalDensity.current
        CompositionLocalProvider(LocalDensity provides Density(base.density, fontScale)) {
            ZinelyTheme {
                inputMode = LocalInputModeManager.current
                Box(Modifier.fillMaxSize()) {
                    ZineLibraryScreen(
                        state = state,
                        events = emptyFlow(),
                        backupRestoreState = null,
                        lastBackup = null,
                        onOpenZine = {},
                        onShareExport = {},
                        onStartZine = { _, _ -> },
                        onRenameZine = { _, _ -> },
                        onDuplicateZine = {},
                        onDeleteZine = {},
                        onDeleteUndo = {},
                        onDeleteCommit = {},
                        onRetry = {},
                        onStartBackup = {},
                        onStartRestore = {},
                        onDismissBackupRestore = {},
                        onCancelBackupRestore = {},
                        onRetryBackupRestore = {},
                        isDeleteWaiting = { true },
                        checkFolderName = ::plainFolderNameVerdict,
                        modifier = Modifier.fillMaxSize(),
                        onDismissRestored = { dismissed += it },
                    )
                }
            }
        }
    }

    private fun textNode() = composeRule.onNodeWithTag(ShelfRestoredNoteTextTestTag).fetchSemanticsNode()

    private fun <T> textConfig(key: androidx.compose.ui.semantics.SemanticsPropertyKey<T>): T? =
        textNode().config.getOrNull(key)

    /** What the note's one node says; null while its words are held back. */
    private fun said(): String? = textConfig(SemanticsProperties.ContentDescription)?.single()

    /** The platform's own node for the note's words: what TalkBack reads. */
    private fun platformText(): AccessibilityNodeInfo {
        val content = composeRule.activity.findViewById<ViewGroup>(android.R.id.content)
        val composeView = (content.getChildAt(0) as ViewGroup).getChildAt(0)
        return checkNotNull(composeView.accessibilityNodeProvider.createAccessibilityNodeInfo(textNode().id))
    }

    /** Report accessibility as on, as with TalkBack running: Compose sends its events only then. */
    private fun turnAccessibilityOn() {
        val manager = composeRule.activity.getSystemService(AccessibilityManager::class.java)
        shadowOf(manager).setEnabled(true)
        shadowOf(manager).setEnabledAccessibilityServiceList(listOf(AccessibilityServiceInfo()))
        shadowOf(manager).setTouchExplorationEnabled(true)
    }

    private fun px(dp: Int): Int = (dp * composeRule.activity.resources.displayMetrics.density).toInt()

    private companion object {
        private fun zine(title: String) = LibraryZine(
            id = title,
            title = title,
            subtitle = "A4 · today",
            cover = ZineCoverRecipe(ZineCoverSurface.MatchaInk, ZineCoverStamp.Sun),
        )

        /** 40 characters, the longest a title can be, with nowhere to break but anywhere. */
        const val LONG_NAME = "Risotestsfromthelibrarybasementzineszz12"

        val ZINES = listOf("Letters home", "Riso tests", "Mum’s garden", "Tiny poems", "Coffee log", "Sunday market")
            .map(::zine)
    }
}
