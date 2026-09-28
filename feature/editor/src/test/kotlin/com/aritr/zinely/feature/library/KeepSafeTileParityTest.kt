package com.aritr.zinely.feature.library

import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aritr.zinely.ui.golden.rasterizeToBitmap
import com.aritr.zinely.ui.theme.LocalZinelyMotion
import com.aritr.zinely.ui.theme.ZinelyMotion
import com.aritr.zinely.ui.theme.ZinelyTheme
import com.aritr.zinely.ui.theme.ZinelyV21Dimens
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowDialog
import kotlin.math.roundToInt

/**
 * Backup-sheet polish (backup-restore.html, frozen 2026-09-27): Backup and Restore draw one tile treatment, and
 * it shows against the sheet — the old Backup tile was the sheet's own colour, so Restore read as primary.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w392dp-h812dp")
class KeepSafeTileParityTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test fun `light - both action tiles match and stand out from the sheet`() = assertPeers(dark = false)

    @Test fun `dark - both action tiles match and stand out from the sheet`() = assertPeers(dark = true)

    private fun assertPeers(dark: Boolean) {
        var leafTint = 0
        composeRule.setContent {
            ZinelyTheme(darkTheme = dark) {
                leafTint = ZinelyTheme.v21Colors.leafTint.toArgb()
                CompositionLocalProvider(LocalZinelyMotion provides ZinelyMotion(reduceMotion = true)) {
                    KeepSafeSheet(
                        visible = true,
                        canBackup = true,
                        lastBackup = null,
                        onDismiss = {},
                        onHidden = {},
                        onSaveBackup = {},
                        onRestoreBackup = {},
                    )
                }
            }
        }
        composeRule.waitForIdle()

        // Inside the tile's left edge, clear of its rounded corners and of the centred glyph.
        val tileX = ZinelyV21Dimens.gapXl + 3.dp
        val tileY = ZinelyV21Dimens.gapLg + 16.dp
        val save = pixel(KeepSafeSaveActionTestTag, tileX, tileY)
        val restore = pixel(KeepSafeRestoreActionTestTag, tileX, tileY)
        val sheet = pixel(KeepSafeSaveActionTestTag, 4.dp, 4.dp)

        assertEquals("Backup draws the leaf-tint tile", leafTint, save)
        assertEquals("Backup and Restore share one tile", restore, save)
        assertNotEquals("the tile shows against the sheet", sheet, save)
    }

    // The sheet is its own dialog window, so rasterize that window, not the activity's.
    private fun pixel(tag: String, x: Dp, y: Dp): Int {
        val window = ShadowDialog.getLatestDialog().window!!.decorView.rasterizeToBitmap()
        val bounds = composeRule.onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot
        return with(composeRule.density) {
            window.getPixel(bounds.left.roundToInt() + x.roundToPx(), bounds.top.roundToInt() + y.roundToPx())
        }
    }
}
