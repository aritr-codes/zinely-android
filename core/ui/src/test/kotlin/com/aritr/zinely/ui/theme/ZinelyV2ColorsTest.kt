package com.aritr.zinely.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pins every [ZinelyV2Colors] value to the `:root` blocks of the DESIGN-FROZEN V2 trilogy
 * (`docs/design/mockups/v2-{library,bench,proof}.html`).
 *
 * The literals below are **transcribed, not parsed** — deliberately. A test that re-parsed the HTML
 * would agree with the implementation for the same reason the implementation is wrong when it is
 * wrong (a shared misreading), and would silently follow the spec if someone edited it. Transcribing
 * means a spec change must be made in two places by a human who noticed it, which is the point.
 *
 * This mirrors `ZinelyTokensTest`, which does the same job for the V1 palette. Both run: V2 is
 * additive and V1 stays pinned until C0 retires it.
 */
class ZinelyV2ColorsTest {

    // ----- colors: `:root` (light) -----------------------------------------------------------

    @Test
    fun `light semantic roles match the frozen root block`() {
        val c = zinelyV2LightColors()
        assertEquals(Color(0xFFF7F2E7), c.paper)
        assertEquals(Color(0xFFEEE6D4), c.paperEdge)
        assertEquals(Color(0xFFECE3D1), c.desk)
        assertEquals(Color(0xFFE1D6BF), c.deskEdge)
        assertEquals(Color(0xFF2A251E), c.ink)
        assertEquals(Color(0xFF5B5347), c.inkSoft)
        assertEquals(Color(0xFF8C8269), c.inkFaint)
        assertEquals(Color(0xFF5E6B2F), c.matcha)
        assertEquals(Color(0xFF4C5826), c.matchaText)
        assertEquals(Color(0xFFDCE3C0), c.matchaTint)
        assertEquals(Color(0xFFE98F97), c.strawberry)
        assertEquals(Color(0xFFA6474F), c.strawberryText)
        assertEquals(Color(0xFFF6DAD3), c.strawberryTint)
        assertEquals(Color(0xFFA6382A), c.consequence)
    }

    @Test
    fun `light chrome group matches the frozen Bench block`() {
        val c = zinelyV2LightColors()
        assertEquals(Color(0xFFFFFFFF), c.onMatcha) // --on-matcha:#fff
        assertEquals(Color(0xFFB7C47C), c.accentOnInk)
        assertEquals(Color(0xFFFBF7EE), c.chrome)
        assertEquals(Color(0xFFE4DAC6), c.chromeLine)
        assertEquals(Color(0xFFFBF7EE), c.sheet)
    }

    @Test
    fun `light translucent tokens carry the frozen rgba alphas`() {
        val c = zinelyV2LightColors()
        // --scrim:rgba(42,37,30,.34) — Bench-canonical (the Proof's .42 is a per-screen override)
        assertEquals(Color(0xFF2A251E).copy(alpha = 0.34f), c.scrim)
        // --hair:rgba(42,37,30,.12)
        assertEquals(Color(0xFF2A251E).copy(alpha = 0.12f), c.hair)
        // --shadow:rgba(60,52,36,.34)
        assertEquals(Color(0xFF3C3424).copy(alpha = 0.34f), c.shadow)
        // --contact:rgba(60,52,36,.22)
        assertEquals(Color(0xFF3C3424).copy(alpha = 0.22f), c.contact)
        // --frame-shadow:rgba(58,48,32,.28)
        assertEquals(Color(0xFF3A3020).copy(alpha = 0.28f), c.frameShadow)
    }

    // ----- colors: `:root[data-theme="dark"]` -------------------------------------------------

    @Test
    fun `dark semantic roles match the frozen dark block`() {
        val c = zinelyV2DarkColors()
        assertEquals(Color(0xFF2F2A22), c.paper)
        assertEquals(Color(0xFF39322A), c.paperEdge)
        assertEquals(Color(0xFF201D18), c.desk)
        assertEquals(Color(0xFF2A261F), c.deskEdge)
        assertEquals(Color(0xFFECE4D3), c.ink)
        assertEquals(Color(0xFFB4AB97), c.inkSoft)
        assertEquals(Color(0xFF857C69), c.inkFaint)
        assertEquals(Color(0xFF93A257), c.matcha)
        assertEquals(Color(0xFFB7C47C), c.matchaText)
        assertEquals(Color(0xFF363826), c.matchaTint)
        assertEquals(Color(0xFFD98289), c.strawberry)
        assertEquals(Color(0xFFE8A6AB), c.strawberryText)
        assertEquals(Color(0xFF3C2C2A), c.strawberryTint)
        assertEquals(Color(0xFFE0857A), c.consequence)
    }

    @Test
    fun `dark chrome group matches the frozen Bench dark block`() {
        val c = zinelyV2DarkColors()
        assertEquals(Color(0xFF20240E), c.onMatcha)
        assertEquals(Color(0xFF4C5826), c.accentOnInk)
        assertEquals(Color(0xFF252017), c.chrome)
        assertEquals(Color(0xFF3A332A), c.chromeLine)
        assertEquals(Color(0xFF252017), c.sheet)
    }

    @Test
    fun `dark translucent tokens carry the frozen rgba alphas`() {
        val c = zinelyV2DarkColors()
        assertEquals(Color.Black.copy(alpha = 0.50f), c.scrim)
        assertEquals(Color(0xFFECE4D3).copy(alpha = 0.13f), c.hair)
        assertEquals(Color.Black.copy(alpha = 0.60f), c.shadow)
        assertEquals(Color.Black.copy(alpha = 0.50f), c.contact)
        assertEquals(Color.Black.copy(alpha = 0.50f), c.frameShadow)
    }

    // ----- the constitutional invariants this palette must carry ------------------------------

    @Test
    fun `dark is re-derived rather than inverted — no token reuses its light value`() {
        // V2-CONSTITUTION.md §III Colour: "Dark is re-derived, not inverted — a warm charcoal room."
        // Every one of the 24 tokens carries a distinct dark value. If a future edit makes one theme
        // inherit from the other, that is an inversion creeping in, and it fails here.
        val l = zinelyV2LightColors()
        val d = zinelyV2DarkColors()
        val pairs: List<Pair<String, Pair<Color, Color>>> = listOf(
            "paper" to (l.paper to d.paper),
            "paperEdge" to (l.paperEdge to d.paperEdge),
            "desk" to (l.desk to d.desk),
            "deskEdge" to (l.deskEdge to d.deskEdge),
            "ink" to (l.ink to d.ink),
            "inkSoft" to (l.inkSoft to d.inkSoft),
            "inkFaint" to (l.inkFaint to d.inkFaint),
            "matcha" to (l.matcha to d.matcha),
            "matchaText" to (l.matchaText to d.matchaText),
            "matchaTint" to (l.matchaTint to d.matchaTint),
            "onMatcha" to (l.onMatcha to d.onMatcha),
            "strawberry" to (l.strawberry to d.strawberry),
            "strawberryText" to (l.strawberryText to d.strawberryText),
            "strawberryTint" to (l.strawberryTint to d.strawberryTint),
            "consequence" to (l.consequence to d.consequence),
            "chrome" to (l.chrome to d.chrome),
            "chromeLine" to (l.chromeLine to d.chromeLine),
            "sheet" to (l.sheet to d.sheet),
            "scrim" to (l.scrim to d.scrim),
            "accentOnInk" to (l.accentOnInk to d.accentOnInk),
            "hair" to (l.hair to d.hair),
            "shadow" to (l.shadow to d.shadow),
            "contact" to (l.contact to d.contact),
            "frameShadow" to (l.frameShadow to d.frameShadow),
        )
        assertEquals("every V2 token must be pinned in both themes", 24, pairs.size)
        for ((name, values) in pairs) {
            assertNotEquals("$name — dark reuses its light value; dark must be re-derived", values.first, values.second)
        }
    }

    @Test
    fun `the dark room stays warm rather than going blue-black`() {
        // V2-TOKENS.md: the desk is "warm; charcoal at night, never blue-black". A warm neutral has
        // red >= green >= blue; a cool one does not. Pins the temperature ruling, not just the hex.
        val d = zinelyV2DarkColors()
        for ((name, c) in listOf("desk" to d.desk, "deskEdge" to d.deskEdge, "paper" to d.paper)) {
            assertTrue(
                "$name went cool (r=${c.red} g=${c.green} b=${c.blue}); the night room must stay warm",
                c.red >= c.green && c.green >= c.blue,
            )
        }
    }

    @Test
    fun `consequence stays distinct from strawberry in both themes`() {
        // V2-TOKENS.md: consequence is "kept distinct from strawberry so a warning never reads as
        // fruit". Both are warm reds; collapsing them would be an easy and invisible mistake.
        assertNotEquals(zinelyV2LightColors().strawberry, zinelyV2LightColors().consequence)
        assertNotEquals(zinelyV2DarkColors().strawberry, zinelyV2DarkColors().consequence)
    }
}
