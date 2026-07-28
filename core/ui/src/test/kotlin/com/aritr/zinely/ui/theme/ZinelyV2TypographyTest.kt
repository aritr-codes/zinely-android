package com.aritr.zinely.ui.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import java.io.File
import java.lang.reflect.Modifier
import java.security.MessageDigest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pins the V2 type foundation to the DESIGN-FROZEN V2 trilogy.
 *
 * Same rule as [ZinelyV2ColorsTest]: every expected value below is transcribed from the frozen HTML,
 * never read back out of the implementation. A test that asserts `x == x` proves only that Kotlin
 * works.
 *
 * The font-file assertions are the unusual part and the point of the class. A `FontFamily` records
 * which *weight* a resource claims to be, not what the resource actually contains — so a weight table
 * alone would pass just as happily if someone dropped a Bold face in at `fraunces_medium.ttf`. Two of
 * the three cuts are not what a reader would assume (`fraunces_medium` does not exist upstream at all;
 * it was instanced), so the bytes themselves are pinned.
 */
class ZinelyV2TypographyTest {

    // Compose's `FontListFontFamily` happens to delegate `List<Font>`, which is how the weights inside
    // a family are reachable at all. That type is `internal`, so this is an implementation detail
    // rather than a contract — acceptable only because it is a test and the failure mode is a loud
    // `ClassCastException` on the first run after Compose changes it, never a silently weakened
    // assertion.
    @Suppress("UNCHECKED_CAST")
    private val voice = ZinelyV2Fonts.Voice as List<Font>

    @Suppress("UNCHECKED_CAST")
    private val work = ZinelyV2Fonts.Work as List<Font>

    // -- families ---------------------------------------------------------------------------------

    @Test
    fun `the voice face carries exactly the three weights the frozen chrome sets Fraunces at`() {
        // 400: proof.html:210 `.foldcap` (serif, 14px, no weight).
        // 500: bench.html `.inkpop h4` / `.sheet h3` / `.pgrid .pgh h3`; proof `.dhead h3` / `.done h4`.
        // 600: library.html `.sh-ttl` / `.shelf-head h1` / `.empty h2`.
        // D-005 is closed: 500 is canonical for the shared serif role (owner ruling 2026-07-28), so
        // Phase B renders the Library's headings at 500 rather than the 600 its own frozen CSS states.
        // 600 stays bundled because V1's ZinelyFonts.Voice is built on it until C0.
        assertEquals(
            "Fraunces ships at 400/500/600 — 500 canonical (D-005), 600 held for V1 until C0",
            listOf(FontWeight.Normal, FontWeight.Medium, FontWeight.SemiBold),
            voice.map { it.weight },
        )
    }

    @Test
    fun `the work face carries exactly the four weights the frozen chrome sets Inter at`() {
        assertEquals(
            listOf(FontWeight.Normal, FontWeight.Medium, FontWeight.SemiBold, FontWeight.Bold),
            work.map { it.weight },
        )
    }

    @Test
    fun `no chrome face is bundled in italic`() {
        // Italic Fraunces appears only in mock ZINE CONTENT (proof.html cover subtitle, pull-quote,
        // back cover) — drawn by the render engine, not by Compose. See D-004. Bundling an italic cut
        // here would make it reachable from chrome, which the frozen specs never do.
        val all = voice + work
        assertTrue(all.all { it.style == FontStyle.Normal })
        assertFalse(all.isEmpty())
    }

    @Test
    fun `each cut is a distinct resource rather than one face relabelled at several weights`() {
        assertEquals("voice: 3 distinct resources", 3, voice.toSet().size)
        assertEquals("work: 4 distinct resources", 4, work.toSet().size)
    }

    // -- the bytes behind the weights -------------------------------------------------------------

    /**
     * Provenance, pinned. Changing any of these three lines is a claim that the product's voice
     * changed, and should read like one in the diff.
     *
     * All three report **`Version 1.003`** in their own `name` table (ID 5), which is the only
     * provenance claim checkable from inside this repository — "byte-identical to upstream" below is
     * a statement about where the files came from, not something these hashes can prove.
     *
     * - `fraunces_regular` / `fraunces_semibold` — upstream `Fraunces9pt-Regular.ttf` /
     *   `Fraunces9pt-SemiBold.ttf`, byte-identical, SIL OFL 1.1 with no Reserved Font Name.
     * - `fraunces_medium` — **instanced by us** from the upstream variable font at
     *   `opsz=9 wght=500 SOFT=0 WONK=1` (`fonttools varLib.instancer`, overlaps removed), because
     *   upstream ships no Medium static. Its advances interpolate strictly between the two upstream
     *   cuts (`H`: 1661 < 1685 < 1709), which is the evidence it came off the same design.
     */
    private val frozenFonts = mapOf(
        "fraunces_regular.ttf" to
            "54f7ec6290e8ddb967e1ebddd2cadb706d6b448254e3489c04f2bcd265db5fa2",
        "fraunces_medium.ttf" to
            "55888b9b978eaacb7778c88e4ea729ae4575983a0063dcb2d05a0c83c17f35e2",
        "fraunces_semibold.ttf" to
            "b0959bfeb942f2a4dcd929a728f8010e4e5369b709c894a592a0bf5e454fc451",
    )

    @Test
    fun `the bundled Fraunces cuts are the exact files their provenance claims`() {
        val fontDir = File("src/main/res/font")
        assertTrue("expected to run with :core:ui as the working directory", fontDir.isDirectory)

        val actual = frozenFonts.keys.associateWith { name ->
            val file = File(fontDir, name)
            assertTrue("$name is missing from res/font", file.isFile)
            MessageDigest.getInstance("SHA-256")
                .digest(file.readBytes())
                .joinToString("") { "%02x".format(it) }
        }
        assertEquals(frozenFonts, actual)
    }

    // -- the foundation ---------------------------------------------------------------------------

    private val type = ZinelyV2Typography()

    @Test
    fun `the base style is the browser default the frozen body inherits`() {
        // No `body{font-size}` in any of the three files, so 16px governs; `body{font-family:var(--sans)}`.
        assertEquals(ZinelyV2Fonts.Work, type.base.fontFamily)
        assertEquals(FontWeight.Normal, type.base.fontWeight)
        assertEquals(16.sp, type.base.fontSize)
    }

    @Test
    fun `the base style leaves line height unspecified because the inheriting controls disagree on one`() {
        // The four chrome controls that render at the inherited 16px do not share a line-height:
        // bench `.supply .opt` inherits 1.5, library `.sheet .act` gets `normal`, proof `.btn` declares
        // 1, proof `.ready` gets `normal`. Pinning any single number here would be wrong for at least
        // two of them, so line-height stays a per-component value like size and tracking.
        assertEquals(TextUnit.Unspecified, type.base.lineHeight)
    }

    @Test
    fun `the recurring section label is small, semibold and widely tracked`() {
        assertEquals(ZinelyV2Fonts.Work, type.sectionLabel.fontFamily)
        assertEquals(FontWeight.SemiBold, type.sectionLabel.fontWeight)
        assertEquals(10.5.sp, type.sectionLabel.fontSize)
        assertEquals(0.13.em, type.sectionLabel.letterSpacing)
    }

    @Test
    fun `the foundation exposes both families and nothing that pretends to be a scale`() {
        assertEquals(ZinelyV2Fonts.Work, type.work)
        assertEquals(ZinelyV2Fonts.Voice, type.voice)
        // 46 distinct sans styles and 8 serif styles across the trilogy, on no ladder. Only genuinely
        // shared values live here; per-component values stay at their call sites (Phase B onward).
        // This is a deliberate tripwire: growing the foundation should be a decision, not a drift.
        //
        // Static fields are excluded wholesale rather than by name. The Compose compiler emits a
        // `$stable` marker that is static but *not* synthetic, so `isSynthetic` alone lets it through;
        // excluding it by name would work today and then fire on the day someone adds a companion
        // object, which is the same false positive one step later.
        val declared = ZinelyV2Typography::class.java.declaredFields
            .filter { !it.isSynthetic && !Modifier.isStatic(it.modifiers) }
            .map { it.name }
            .sorted() // the JVM does not promise declaration order; only the membership is the assertion
        assertEquals(
            "adding a named style to ZinelyV2Typography needs an ADR — it is not a type scale",
            listOf("base", "sectionLabel", "voice", "work"),
            declared,
        )
    }
}
