package com.aritr.zinely.ui.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pins the **three-role** V2.1 type foundation ([V21-SPEC §4.2](docs/design/V21-SPEC.md)).
 *
 * The last two assertions are the point of the class. V2.1's voice face required
 * [V2-CONSTITUTION Amendment 1](docs/design/V2-CONSTITUTION.md#amendment-log): §III used to say *"No
 * third UI typeface"*, and the amendment replaced that with *"No **fourth** UI typeface"* plus the
 * constraint that makes three safe. Three is now the ceiling, and a fourth family should fail a test
 * rather than pass a review.
 */
class ZinelyV21FontsTest {

    // Same implementation-detail cast as ZinelyV2TypographyTest, and acceptable for the same reason:
    // `FontListFontFamily` delegates `List<Font>`, is `internal`, and a Compose change surfaces here
    // as a loud ClassCastException rather than a quietly weakened assertion.
    @Suppress("UNCHECKED_CAST")
    private val voice = ZinelyV21Fonts.Voice as List<Font>

    @Suppress("UNCHECKED_CAST")
    private val editorial = ZinelyV21Fonts.Editorial as List<Font>

    @Suppress("UNCHECKED_CAST")
    private val work = ZinelyV21Fonts.Work as List<Font>

    @Test
    fun `Voice carries the two Averia weights the corpus sets`() {
        // v21-*.html @font-face declares 400 and 700 only.
        assertEquals(listOf(FontWeight.Normal, FontWeight.Bold), voice.map { it.weight })
    }

    @Test
    fun `Editorial carries Fraunces at 400 and nothing else`() {
        // All seven --serif rules in the corpus take the default weight. A draft bundled Medium/500
        // by analogy with V2; no V2.1 rule sets it, and a bundled-but-unused weight is the dead-token
        // failure D-006 deleted `--r` for. If a 500 is ever wanted, the HTML states it first.
        assertEquals(listOf(FontWeight.Normal), editorial.map { it.weight })
    }

    @Test
    fun `Work carries the four Inter weights, unchanged from V2`() {
        assertEquals(
            listOf(FontWeight.Normal, FontWeight.Medium, FontWeight.SemiBold, FontWeight.Bold),
            work.map { it.weight },
        )
    }

    @Test
    fun `no chrome face is bundled in italic`() {
        // Unchanged from V2's rule: italic Fraunces belongs to zine CONTENT, drawn by the render
        // engine, and bundling an italic cut here would make it reachable from chrome.
        val all = voice + editorial + work
        assertTrue("every bundled chrome cut is upright", all.all { it.style == FontStyle.Normal })
    }

    @Test
    fun `each cut is a distinct resource rather than one face relabelled`() {
        assertEquals("voice: 2 distinct resources", 2, voice.toSet().size)
        assertEquals("editorial: 1 distinct resource", 1, editorial.toSet().size)
        assertEquals("work: 4 distinct resources", 4, work.toSet().size)
    }

    @Test
    fun `Voice never collapses back onto Editorial`() {
        // Amendment 1 was argued on exactly one claim: Fraunces can be warm but it cannot be wonky.
        // If these two ever resolve to the same family, that argument has been abandoned and the
        // amendment bought a 123.6 KB APK increase for nothing.
        assertNotEquals(ZinelyV21Fonts.Voice, ZinelyV21Fonts.Editorial)
        assertTrue(
            "Voice and Editorial must not share a resource",
            voice.toSet().intersect(editorial.toSet()).isEmpty(),
        )
    }

    @Test
    fun `V2_1 has exactly three type roles`() {
        // The constitutional ceiling, as a test. A fourth role is an owner question.
        val families = setOf(ZinelyV21Fonts.Voice, ZinelyV21Fonts.Editorial, ZinelyV21Fonts.Work)
        assertEquals("three roles, three families — no fourth UI typeface", 3, families.size)
    }
}
