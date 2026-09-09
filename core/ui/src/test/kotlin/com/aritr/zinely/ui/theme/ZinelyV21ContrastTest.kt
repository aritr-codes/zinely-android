package com.aritr.zinely.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Contrast and identity gate for the owner-frozen `37596.jpg` studio palette. */
class ZinelyV21ContrastTest {

    private fun assertClears(name: String, foreground: Color, background: Color, floor: Float) {
        val ratio = WcagContrast.contrastRatio(foreground, background)
        assertTrue("$name ${"%.2f".format(ratio)}:1 is below $floor:1", ratio >= floor)
    }

    @Test
    fun `the six source swatches are exact and remain visible in both themes`() {
        val light = zinelyV21LightColors()
        val dark = zinelyV21DarkColors()
        assertEquals(Color(0xFF8E9546), light.leaf)
        assertEquals(Color(0xFFBBCA6F), light.deskEdge)
        assertEquals(Color(0xFFE9E29B), light.desk)
        assertEquals(Color(0xFFF2CFBB), light.surface)
        assertEquals(Color(0xFFF1B4AF), light.surfaceSoft)
        assertEquals(Color(0xFFF28892), light.berry)
        assertEquals(Color(0xFFBBCA6F), dark.leaf)
        assertEquals(Color(0xFF8E9546), dark.leafTint)
        assertEquals(Color(0xFFE9E29B), dark.butter)
        assertEquals(Color(0xFFF1B4AF), light.berryTint)
        assertEquals(Color(0xFFF28892), dark.berry)
        assertEquals(Color(0xFF8E9546), light.inkFaint)
        assertEquals(light.inkFaint, dark.inkFaint)
        assertEquals(Color(0x7027270F), ZinelyV21Scrim)
    }

    @Test
    fun `physical paper is theme invariant`() {
        val light = zinelyV21LightColors()
        val dark = zinelyV21DarkColors()
        assertEquals(Color(0xFFFFF6E8), light.paper)
        assertEquals(light.paper, dark.paper)
        assertEquals(light.paperEdge, dark.paperEdge)
    }

    @Test
    fun `room copy clears AA on the surfaces where it is authored`() {
        for ((label, colors) in listOf("day" to zinelyV21LightColors(), "night" to zinelyV21DarkColors())) {
            for ((groundName, ground) in listOf(
                "room" to colors.desk,
                "room edge" to colors.deskEdge,
                "surface" to colors.surface,
                "surfaceSoft" to colors.surfaceSoft,
            )) {
                assertClears("$label ink / $groundName", colors.ink, ground, WcagContrast.AA_NORMAL)
                assertClears("$label inkSoft / $groundName", colors.inkSoft, ground, WcagContrast.AA_NORMAL)
            }
            assertClears("$label inkSoft / butterTint", colors.inkSoft, colors.butterTint, WcagContrast.AA_NORMAL)
        }
    }

    @Test
    fun `physical paper uses the lit ink palette`() {
        val lit = zinelyV21LightColors()
        assertClears("lit ink / paper", lit.ink, lit.paper, WcagContrast.AA_NORMAL)
        assertClears("lit inkSoft / paper", lit.inkSoft, lit.paper, WcagContrast.AA_NORMAL)
        assertClears("lit leafText / paper", lit.leafText, lit.paper, WcagContrast.AA_NORMAL)
        assertClears("lit consequence / paper", lit.jamText, lit.paper, WcagContrast.AA_NORMAL)
        assertClears("lit cut / paper", lit.jam, lit.paper, WcagContrast.AA_LARGE)
        assertClears("lit ink / paper edge", lit.ink, lit.paperEdge, WcagContrast.AA_NORMAL)
        assertClears("lit inkSoft / paper edge", lit.inkSoft, lit.paperEdge, WcagContrast.AA_NORMAL)
        assertClears("lit current page / quiet action ground", lit.onLeaf, lit.leafTint, WcagContrast.AA_NORMAL)
    }

    @Test
    fun `primary buttons use their frozen dark on-bright ink`() {
        for ((label, colors) in listOf("day" to zinelyV21LightColors(), "night" to zinelyV21DarkColors())) {
            assertClears("$label onPrimary / primary", colors.onLeaf, colors.leaf, WcagContrast.AA_NORMAL)
            assertClears("$label onPrimary / quiet action", colors.onLeaf, colors.leafTint, WcagContrast.AA_NORMAL)
            assertClears("$label onButter / butter", colors.onButter, colors.butter, WcagContrast.AA_NORMAL)
            assertClears("$label onButter / paper", colors.onButter, colors.paper, WcagContrast.AA_NORMAL)
        }
    }

    @Test
    fun `bright icon grounds use ground-aware dark ink`() {
        for ((label, colors) in listOf("day" to zinelyV21LightColors(), "night" to zinelyV21DarkColors())) {
            assertClears("$label onLeaf / berryTint", colors.onLeaf, colors.berryTint, WcagContrast.AA_LARGE)
            assertClears("$label ink / butterTint", colors.ink, colors.butterTint, WcagContrast.AA_LARGE)
            assertClears("$label onLeaf / paper", colors.onLeaf, colors.paper, WcagContrast.AA_LARGE)
            assertClears("$label jam / surface", colors.jam, colors.surface, WcagContrast.AA_LARGE)
        }
    }

    @Test
    fun `consequence fills carry a readable label in both themes`() {
        for ((label, colors) in listOf("day" to zinelyV21LightColors(), "night" to zinelyV21DarkColors())) {
            assertClears("$label onConsequence / consequence", colors.onJam, colors.jam, WcagContrast.AA_NORMAL)
        }
    }

    @Test
    fun `semantic colour used as chrome text clears AA on its actual grounds`() {
        for ((label, colors) in listOf("day" to zinelyV21LightColors(), "night" to zinelyV21DarkColors())) {
            for ((groundName, ground) in listOf("room" to colors.desk, "surface" to colors.surface)) {
                assertClears("$label leafText / $groundName", colors.leafText, ground, WcagContrast.AA_NORMAL)
                assertClears("$label jamText / $groundName", colors.jamText, ground, WcagContrast.AA_NORMAL)
            }
        }
    }

    @Test
    fun `meaning bearing room outlines clear non-text contrast`() {
        for ((label, colors) in listOf("day" to zinelyV21LightColors(), "night" to zinelyV21DarkColors())) {
            assertClears("$label ink / room", colors.ink, colors.desk, WcagContrast.AA_LARGE)
            assertClears("$label ink / room edge", colors.ink, colors.deskEdge, WcagContrast.AA_LARGE)
            // Floating editor cards use this pair for 9.6sp captions and 12.48sp controls, so the normal-
            // text threshold is the relevant contract; large-text AA would allow the muddy TypeBar state
            // caught on the Samsung in dark mode.
            assertClears("$label ink / surface", colors.ink, colors.surface, WcagContrast.AA_NORMAL)
            assertClears("$label ink / surfaceSoft", colors.ink, colors.surfaceSoft, WcagContrast.AA_NORMAL)
        }
    }
}
