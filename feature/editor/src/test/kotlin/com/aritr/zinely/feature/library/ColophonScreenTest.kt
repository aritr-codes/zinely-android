package com.aritr.zinely.feature.library

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.unit.Density
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.semantics.SemanticsProperties
import com.aritr.zinely.core.copy.Copy
import com.aritr.zinely.core.model.PaperSize
import com.aritr.zinely.ui.theme.ZinelyTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h640dp", sdk = [28])
class ColophonScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()
    private lateinit var inputMode: InputModeManager

    @Test
    fun `colophon opens, lists version, and keeps paper options discoverable`() {
        setContent(
            preferredPaper = PaperSize.A4,
            appVersion = "v9.9.9",
            onBackToShelf = {},
            onPreferredPaperChange = {},
        )

        composeRule.onNodeWithTag(ColophonScreenTestTag).assertIsDisplayed()
        scrollToTag(ColophonPaperGroupTestTag)
        val a4 = composeRule.onNodeWithTag(colophonPaperTestTag(PaperSize.A4)).fetchSemanticsNode().boundsInRoot
        val letter = composeRule.onNodeWithTag(colophonPaperTestTag(PaperSize.LETTER))
            .fetchSemanticsNode()
            .boundsInRoot
        assertTrue(a4.height >= 48f)
        assertTrue(letter.height >= 48f)
        assertTrue("A4 appears above Letter", a4.top < letter.top)
        composeRule.onNodeWithTag(colophonPaperTestTag(PaperSize.A4)).assertIsSelected()
        composeRule.onNodeWithTag(colophonPaperTestTag(PaperSize.LETTER)).assertIsNotSelectedNode()

        composeRule.onNodeWithTag(ColophonScreenTestTag)
            .performScrollToNode(hasTestTag(ColophonVersionTestTag))
        composeRule.onNodeWithTag(ColophonVersionTestTag).assertIsDisplayed()
        composeRule.onNodeWithText("v9.9.9").assertIsDisplayed()
    }

    @Test
    fun `about opens with the app specific makers note and retains its utilities`() {
        setContent(
            preferredPaper = PaperSize.A4,
            appVersion = "v1",
            onBackToShelf = {},
            onPreferredPaperChange = {},
        )

        composeRule.onNodeWithText("About Zinely").assertTextEquals(Copy.Colophon.TITLE)
        composeRule.onNodeWithText(Copy.Colophon.MAKER_TITLE).assertIsDisplayed()
        assertTrue(composeRule.onNodeWithText(Copy.Colophon.MAKER_TITLE)
            .fetchSemanticsNode().config.contains(SemanticsProperties.Heading))
        listOf(Copy.Colophon.MAKER_ORIGIN, Copy.Colophon.MAKER_PAGES, Copy.Colophon.MAKER_THANKS).forEach {
            composeRule.onNodeWithTag(ColophonScreenTestTag).performScrollToNode(hasText(it))
            composeRule.onNodeWithText(it).assertIsDisplayed()
        }
        composeRule.onNodeWithText("Some things deserve pages.").assertDoesNotExist()
        composeRule.onNodeWithText(
            "Zinely began with a simple wish: to make something for someone. " +
                "We hope it helps you make something worth keeping.",
        ).assertDoesNotExist()
        scrollToTag(ColophonPaperGroupTestTag)
        composeRule.onNodeWithTag(ColophonPaperGroupTestTag).assertIsDisplayed()
    }

    @Test
    fun `colophon paper selection reports a preference change`() {
        val changes = mutableListOf<PaperSize>()
        setContent(
            preferredPaper = PaperSize.A4,
            appVersion = "v1",
            onBackToShelf = {},
            onPreferredPaperChange = { changes += it },
        )

        scrollToTag(ColophonPaperGroupTestTag)
        composeRule.onNodeWithTag(colophonPaperTestTag(PaperSize.LETTER)).performClick()
        composeRule.onNodeWithTag(colophonPaperTestTag(PaperSize.LETTER)).assertIsSelected()
        composeRule.waitForIdle()
        assertEquals(listOf(PaperSize.LETTER), changes)
        scrollToTag(ColophonBackTestTag)
        composeRule.onNodeWithTag(ColophonBackTestTag).performClick()
    }

    @Test
    fun `colophon licence loads and renders text when provided by local copy`() {
        val licenseTypeface = ColophonTypeface.INTER
        setContent(
            preferredPaper = PaperSize.A4,
            appVersion = "v1",
            onBackToShelf = {},
            onPreferredPaperChange = {},
            loadLicence = {
                assertEquals(licenseTypeface, it)
                "Locally licensed text"
            },
        )

        composeRule.runOnUiThread { assertTrue(inputMode.requestInputMode(InputMode.Keyboard)) }
        composeRule.onNodeWithTag(ColophonScreenTestTag)
            .performScrollToNode(hasTestTag(colophonTypefaceTestTag(licenseTypeface)))
        composeRule.onNodeWithTag(colophonTypefaceTestTag(licenseTypeface)).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Locally licensed text").assertIsDisplayed()
        composeRule.onNodeWithTag(ColophonBackTestTag).performClick()
        composeRule.onNodeWithTag(colophonTypefaceTestTag(licenseTypeface)).assertIsDisplayed().assertIsFocused()
        scrollToTag(ColophonBackTestTag)
        composeRule.onNodeWithTag(ColophonBackTestTag)
            .assertContentDescriptionEquals(Copy.Colophon.BACK_TO_SHELF)
    }

    @Test
    fun `colophon licence shows unavailable on licence failure`() {
        setContent(
            preferredPaper = PaperSize.A4,
            appVersion = "v1",
            onBackToShelf = {},
            onPreferredPaperChange = {},
            loadLicence = { throw IllegalStateException("local failure") },
        )

        composeRule.onNodeWithTag(ColophonScreenTestTag)
            .performScrollToNode(hasTestTag(colophonTypefaceTestTag(ColophonTypeface.INTER)))
        composeRule.onNodeWithTag(colophonTypefaceTestTag(ColophonTypeface.INTER)).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText(Copy.Colophon.LICENCE_UNAVAILABLE).assertIsDisplayed()
    }

    @Test
    fun `colophon returns to shelf via back action`() {
        var backCalls = 0
        setContent(
            preferredPaper = PaperSize.A4,
            appVersion = "v1",
            onBackToShelf = { backCalls++ },
            onPreferredPaperChange = {},
        )

        composeRule.onNodeWithTag(ColophonBackTestTag).performClick()
        assertEquals(1, backCalls)
    }

    @Test
    fun `colophon back control is at least 48 dp tall`() {
        setContent(
            preferredPaper = PaperSize.A4,
            appVersion = "v1",
            onBackToShelf = {},
            onPreferredPaperChange = {},
        )
        val backBounds = composeRule.onNodeWithTag(ColophonBackTestTag).fetchSemanticsNode().boundsInRoot
        assertTrue(backBounds.height >= 48f)
    }

    @Test
    @Config(qualifiers = "w320dp-h640dp", sdk = [28])
    fun `large text note and utilities remain reachable and licence return preserves focus`() {
        setContent(PaperSize.A4, "v-large", {}, {}, { "Local licence" }, fontScale = 1.8f)
        composeRule.runOnUiThread { assertTrue(inputMode.requestInputMode(InputMode.Keyboard)) }
        listOf(Copy.Colophon.MAKER_TITLE, Copy.Colophon.MAKER_ORIGIN,
            Copy.Colophon.MAKER_PAGES, Copy.Colophon.MAKER_THANKS).forEach {
            composeRule.onNodeWithTag(ColophonScreenTestTag).performScrollToNode(hasText(it))
            composeRule.onNodeWithText(it).assertIsDisplayed()
        }
        scrollToTag(colophonPaperTestTag(PaperSize.LETTER))
        composeRule.onNodeWithTag(colophonPaperTestTag(PaperSize.LETTER)).performClick().assertIsSelected()
        val licence = colophonTypefaceTestTag(ColophonTypeface.INTER)
        scrollToTag(licence)
        composeRule.onNodeWithTag(licence).performClick()
        composeRule.onNodeWithText("Local licence").assertIsDisplayed()
        composeRule.onNodeWithTag(ColophonBackTestTag).performClick()
        composeRule.onNodeWithTag(licence).assertIsDisplayed().assertIsFocused()
        scrollToTag(ColophonVersionTestTag)
        composeRule.onNodeWithText("v-large").assertIsDisplayed()
    }

    private fun scrollToTag(tag: String) {
        composeRule.onNodeWithTag(ColophonScreenTestTag).performScrollToNode(hasTestTag(tag))
    }

    private fun setContent(
        preferredPaper: PaperSize,
        appVersion: String,
        onBackToShelf: () -> Unit,
        onPreferredPaperChange: (PaperSize) -> Unit,
        loadLicence: (suspend (ColophonTypeface) -> String)? = null,
        fontScale: Float = 1f,
    ) = composeRule.setContent {
        var selectedPaper by remember { mutableStateOf(preferredPaper) }
        inputMode = LocalInputModeManager.current

        CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, fontScale)) {
            ZinelyTheme {
                ColophonScreen(
                    preferredPaper = selectedPaper,
                    appVersion = appVersion,
                    onPreferredPaperChange = {
                        selectedPaper = it
                        onPreferredPaperChange(it)
                    },
                    onBackToShelf = onBackToShelf,
                    loadLicence = loadLicence,
                    modifier = androidx.compose.ui.Modifier,
                )
            }
        }
    }

    private fun androidx.compose.ui.test.SemanticsNodeInteraction.assertIsNotSelectedNode() = apply {
        assertEquals(false, fetchSemanticsNode().config[SemanticsProperties.Selected])
    }
}
