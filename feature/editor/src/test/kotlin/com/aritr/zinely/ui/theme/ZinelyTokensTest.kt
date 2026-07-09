package com.aritr.zinely.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pins every design token to the DESIGN-FROZEN `:root` block of `docs/design/v1/shelf.html`
 * (frozen 2026-07-08; `bench.html` and `proof.html` declare byte-identical blocks).
 *
 * The HTML is the specification. If one of these assertions fails, the Compose token drifted and the
 * fix is here — unless the HTML spec was amended first, in which case update both, in that order.
 *
 * The literals are transcribed rather than parsed out of the HTML at run time: Gradle runs unit tests
 * with the *module* directory as the working directory, so `docs/design/v1/...` does not resolve, and
 * a test that silently skips when it cannot find its input is worse than no test.
 */
class ZinelyTokensTest {

    // ----- colors: `:root` (light) ---------------------------------------------------------

    @Test
    fun `light colors match the frozen root block`() {
        val c = zinelyLightColors()
        assertEquals(Color(0xFFF4EFE6), c.paper)
        assertEquals(Color(0xFFEFE8DA), c.paper2)
        assertEquals(Color(0xFFE1D8C7), c.paperEdge)
        assertEquals(Color(0xFF23201C), c.ink)
        assertEquals(Color(0xFF6B6358), c.inkSoft)
        assertEquals(Color(0xFF9C9385), c.inkFaint)
        assertEquals(Color(0xFFE76F51), c.coral)
        assertEquals(Color(0xFFC64E34), c.coralStrong)
        assertEquals(Color(0xFFA63C22), c.coralText)
        assertEquals(Color(0xFF2A9D8F), c.teal)
        assertEquals(Color(0xFFE9C46A), c.yellow)
        assertEquals(Color(0xFF264653), c.stamp)
        assertEquals(Color(0xFFE7DECE), c.desk)
        assertEquals(Color(0xFFDBD1BF), c.deskEdge)
        assertEquals(Color(0xFFFBF8F1), c.field)
        assertEquals(Color(0xFFDED4C2), c.fieldEdge)
        assertEquals(Color(0xFFFBF8F1), c.menu)
        assertEquals(Color(0xFF5E574C), c.onDeskSoft)
        assertEquals(Color(0xFF726A5C), c.onDeskFaint)
    }

    @Test
    fun `light on-desk is ink, per --on-desk var(--ink)`() {
        val c = zinelyLightColors()
        assertEquals(c.ink, c.onDesk)
    }

    @Test
    fun `light translucent tokens carry the frozen rgba alphas`() {
        val c = zinelyLightColors()
        // --shelf-line: rgba(35,32,28,.10)  /  --scrim: rgba(35,32,28,.42)
        assertEquals(Color(0xFF23201C).copy(alpha = 0.10f), c.shelfLine)
        assertEquals(Color(0xFF23201C).copy(alpha = 0.42f), c.scrim)
    }

    // ----- colors: `:root[data-theme="dark"]` ----------------------------------------------

    @Test
    fun `dark colors match the frozen dark block`() {
        val c = zinelyDarkColors()
        assertEquals(Color(0xFFEDE6D9), c.paper)
        assertEquals(Color(0xFFE4DBCB), c.paper2)
        assertEquals(Color(0xFFC7BCA6), c.paperEdge)
        assertEquals(Color(0xFF201F1E), c.desk)
        assertEquals(Color(0xFF161514), c.deskEdge)
        assertEquals(Color(0xFFEFE9DD), c.onDesk)
        assertEquals(Color(0xFFC3BBAC), c.onDeskSoft)
        assertEquals(Color(0xFF8C8577), c.onDeskFaint)
        assertEquals(Color(0xFF2B2A28), c.field)
        assertEquals(Color(0xFF413E39), c.fieldEdge)
        assertEquals(Color(0xFF2B2A28), c.menu)
        assertEquals(Color(0xFFE76F51), c.coralText)
        assertEquals(Color.White.copy(alpha = 0.06f), c.shelfLine)
        assertEquals(Color.Black.copy(alpha = 0.58f), c.scrim)
    }

    @Test
    fun `dark inherits the ink and accent inks the spec does not override`() {
        val light = zinelyLightColors()
        val dark = zinelyDarkColors()
        // The sheet stays lit, so ink on it stays dark. These are absent from the dark block.
        assertEquals(light.ink, dark.ink)
        assertEquals(light.inkSoft, dark.inkSoft)
        assertEquals(light.inkFaint, dark.inkFaint)
        assertEquals(light.coral, dark.coral)
        assertEquals(light.coralStrong, dark.coralStrong)
        assertEquals(light.teal, dark.teal)
        assertEquals(light.yellow, dark.yellow)
        assertEquals(light.stamp, dark.stamp)
        // ...but --coral-text IS overridden, and --on-desk stops tracking --ink.
        assertNotEquals(light.coralText, dark.coralText)
        assertNotEquals(dark.ink, dark.onDesk)
    }

    // ----- elevation ------------------------------------------------------------------------

    @Test
    fun `light elevation matches the frozen box-shadow layers`() {
        val e = zinelyLightElevation()
        val ink = Color(0xFF23201C)
        assertEquals(listOf(ZinelyShadowLayer(1.dp, 2.dp, ink.copy(alpha = 0.10f))), e.shadow1)
        assertEquals(
            listOf(
                ZinelyShadowLayer(10.dp, 22.dp, ink.copy(alpha = 0.16f)),
                ZinelyShadowLayer(2.dp, 5.dp, ink.copy(alpha = 0.10f)),
            ),
            e.shadow2,
        )
        assertEquals(
            listOf(
                ZinelyShadowLayer(18.dp, 34.dp, ink.copy(alpha = 0.22f)),
                ZinelyShadowLayer(3.dp, 8.dp, ink.copy(alpha = 0.14f)),
            ),
            e.shadowLift,
        )
    }

    @Test
    fun `dark elevation is deeper and pure black`() {
        val e = zinelyDarkElevation()
        assertEquals(listOf(ZinelyShadowLayer(1.dp, 2.dp, Color.Black.copy(alpha = 0.50f))), e.shadow1)
        assertEquals(
            listOf(
                ZinelyShadowLayer(12.dp, 26.dp, Color.Black.copy(alpha = 0.55f)),
                ZinelyShadowLayer(2.dp, 6.dp, Color.Black.copy(alpha = 0.40f)),
            ),
            e.shadow2,
        )
        assertEquals(
            listOf(
                ZinelyShadowLayer(20.dp, 40.dp, Color.Black.copy(alpha = 0.62f)),
                ZinelyShadowLayer(4.dp, 10.dp, Color.Black.copy(alpha = 0.50f)),
            ),
            e.shadowLift,
        )
    }

    // ----- motion ---------------------------------------------------------------------------

    @Test
    fun `motion durations match --fast and --base`() {
        val m = ZinelyMotion(reduceMotion = false)
        assertEquals(130, m.fastMillis)
        assertEquals(230, m.baseMillis)
        assertEquals(130, ZINELY_FAST_MILLIS)
        assertEquals(230, ZINELY_BASE_MILLIS)
    }

    @Test
    fun `reduced motion collapses every duration to zero`() {
        val m = ZinelyMotion(reduceMotion = true)
        assertEquals(0, m.fastMillis)
        assertEquals(0, m.baseMillis)
        assertEquals(0, m.fast<Float>().durationMillis)
        assertEquals(0, m.base<Float>().durationMillis)
    }

    @Test
    fun `tweens carry the frozen easing`() {
        val m = ZinelyMotion(reduceMotion = false)
        assertEquals(ZinelyEasing, m.fast<Float>().easing)
        assertEquals(ZinelyEasing, m.base<Float>().easing)
    }

    @Test
    fun `easing is cubic-bezier point-2 point-7 point-3 one`() {
        // Sampled, because CubicBezierEasing exposes no control points. Endpoints are exact; the
        // midpoint pins the curve's shape (a slow-out/fast-in ease would not pass through here).
        assertEquals(0f, ZinelyEasing.transform(0f), 1e-4f)
        assertEquals(1f, ZinelyEasing.transform(1f), 1e-4f)
        assertEquals(0.910f, ZinelyEasing.transform(0.5f), 1e-3f)
    }

    // ----- haptics --------------------------------------------------------------------------

    @Test
    fun `the four verbs carry the frozen HAPTIC patterns`() {
        assertArrayEquals(longArrayOf(8), ZinelyHaptic.Tick.onOffMillis)
        assertArrayEquals(longArrayOf(6, 20, 10), ZinelyHaptic.Snap.onOffMillis)
        assertArrayEquals(longArrayOf(24), ZinelyHaptic.Boundary.onOffMillis)
        assertArrayEquals(longArrayOf(12, 30, 12, 30), ZinelyHaptic.Success.onOffMillis)
        assertEquals(4, ZinelyHaptic.entries.size)
    }

    @Test
    fun `android timings prepend a zero wait so the on-phases survive the translation`() {
        // navigator.vibrate() is ON-first; VibrationEffect.createWaveform() is OFF-first. Handing the
        // spec array over verbatim would make Tick an 8ms silence.
        assertArrayEquals(longArrayOf(0, 8), androidTimings(ZinelyHaptic.Tick))
        assertArrayEquals(longArrayOf(0, 6, 20, 10), androidTimings(ZinelyHaptic.Snap))
        assertArrayEquals(longArrayOf(0, 24), androidTimings(ZinelyHaptic.Boundary))
        assertArrayEquals(longArrayOf(0, 12, 30, 12, 30), androidTimings(ZinelyHaptic.Success))
    }

    @Test
    fun `every verb vibrates for exactly as long as the spec says`() {
        for (haptic in ZinelyHaptic.entries) {
            val timings = androidTimings(haptic)
            // Odd indices are the ON phases once the leading zero-wait is in place.
            val onTotal = timings.filterIndexed { i, _ -> i % 2 == 1 }.sum()
            val specOnTotal = haptic.onOffMillis.filterIndexed { i, _ -> i % 2 == 0 }.sum()
            assertEquals("$haptic on-time", specOnTotal, onTotal)
            assertTrue("$haptic must not start with a wait", timings[0] == 0L)
        }
    }

    // ----- dimensions -----------------------------------------------------------------------

    @Test
    fun `accessibility and focus-ring dimensions match the frozen spec`() {
        assertEquals(48f, ZinelyDimens.MinTouchTarget.value, 0f)
        assertEquals(3f, ZinelyDimens.FocusRingWidth.value, 0f)
        assertEquals(2f, ZinelyDimens.FocusRingOffset.value, 0f)
        assertEquals(6f, ZinelyDimens.FocusRingRadius.value, 0f)
    }
}

private fun assertArrayEquals(expected: LongArray, actual: LongArray) =
    assertEquals(expected.toList(), actual.toList())
