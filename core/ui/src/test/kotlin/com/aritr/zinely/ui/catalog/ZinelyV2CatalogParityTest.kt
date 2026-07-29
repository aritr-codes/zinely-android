package com.aritr.zinely.ui.catalog

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.aritr.zinely.ui.golden.cropToBounds
import com.aritr.zinely.ui.golden.rasterizeToBitmap
import com.aritr.zinely.ui.theme.ZinelyTheme
import com.aritr.zinely.ui.theme.ZinelyV2Icons
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import kotlin.math.roundToInt

/**
 * **A9's parity proof: the frozen corpus is the oracle, not this implementation.**
 *
 * A Roborazzi golden can only ever say *today's output equals yesterday's output*. Record a golden of a
 * catalog with the wrong cream in it and the gate stays green for the life of the project. That is the
 * difference between reproducing the design and merely resembling it, and it is the difference this file
 * exists to close: the expected values are **parsed out of
 * [V2-TOKENS.md](docs/design/V2-TOKENS.md) at run time** and compared against the pixels the renderer
 * actually produced. Nothing here is written down twice.
 *
 * That is a deliberate sharpening of the V1 convention. `ZComponentGoldenTest`'s behavioural proof asserts
 * `countColour(Color(0xFFC64E34)) > 200` — a hex transcribed into the test, so the test and the token can
 * be wrong together and agree. Here they cannot: if someone edits `ZinelyV2Colors.kt`, this fails; if
 * someone edits the design document, this fails; only changing both, deliberately, keeps it green.
 *
 * ## Exactness is the point
 *
 * The comparison is `==` on a packed ARGB int, not a tolerance. The catalog's swatches are shaped to make
 * that possible — flat, square, un-bordered, no grain — because the moment a comparison needs a tolerance
 * it stops answering "is this the frozen value?" and starts answering "is this close enough?", which is
 * the question A9 was told not to answer.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
// A window wide and tall enough that no section is clipped by the host's default 320x480. A clipped
// section would not fail — the crop would simply be short — so the value is read off pixels that were
// never drawn, and the suite goes green on a partial render. Density is left at the host default (1.0).
@Config(qualifiers = "w480dp-h1600dp")
class ZinelyV2CatalogParityTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    // ----- the corpus ---------------------------------------------------------------------------

    /** One chrome role as [V2-TOKENS.md](docs/design/V2-TOKENS.md) states it. */
    private data class CorpusRole(val name: String, val lightArgb: Int, val darkArgb: Int)

    /**
     * Parse the "Semantic roles" table out of the design document.
     *
     * Reads cells positionally (role | light | dark) rather than by scanning the whole line for hex, so a
     * hex that appears in a *description* cannot be mistaken for a value. A row counts only when both
     * colour cells hold a `#rrggbb`, which is what skips the header and separator rows without a
     * special case for either.
     */
    private fun corpusRoles(): List<CorpusRole> {
        val doc = File(repoRoot(), "docs/design/V2-TOKENS.md")
        assertTrue("the design document must exist at ${doc.absolutePath}", doc.isFile)
        val backticked = Regex("`([^`]+)`")
        val hex = Regex("^#[0-9A-Fa-f]{6}$")
        return doc.readLines().mapNotNull { line ->
            val cells = line.split('|')
            if (cells.size < 5) return@mapNotNull null
            val role = backticked.find(cells[1])?.groupValues?.get(1) ?: return@mapNotNull null
            val light = backticked.find(cells[2])?.groupValues?.get(1) ?: return@mapNotNull null
            val dark = backticked.find(cells[3])?.groupValues?.get(1) ?: return@mapNotNull null
            if (!hex.matches(light) || !hex.matches(dark)) return@mapNotNull null
            CorpusRole(role, argbOf(light), argbOf(dark))
        }
    }

    private fun argbOf(hex: String): Int = (0xFF000000L or hex.removePrefix("#").toLong(16)).toInt()

    private fun repoRoot(): File {
        var dir: File? = File(System.getProperty("user.dir") ?: ".").absoluteFile
        while (dir != null) {
            if (File(dir, "settings.gradle.kts").isFile) return dir
            dir = dir.parentFile
        }
        error("Could not locate the repository root (no settings.gradle.kts above the working directory)")
    }

    // ----- rendering ----------------------------------------------------------------------------

    private fun render(darkTheme: Boolean, content: @Composable () -> Unit): Bitmap {
        composeRule.setContent { ZinelyTheme(darkTheme = darkTheme) { content() } }
        composeRule.waitForIdle()
        return composeRule.activity.window.decorView.rasterizeToBitmap()
    }

    /** The colour actually painted at the centre of the tagged node, read off the rasterised window. */
    private fun Bitmap.centreOf(tag: String): Int {
        val b = composeRule.onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot
        val x = (b.left + b.width / 2f).roundToInt()
        val y = (b.top + b.height / 2f).roundToInt()
        // Deliberately NOT clamped into range. A swatch laid out beyond the window would clamp to an edge
        // pixel and be compared against whatever happens to be painted there — a colour assertion reading
        // a pixel the swatch never drew. Off-window is a defect in the fixture, so it fails as one.
        assertTrue(
            "$tag's centre ($x, $y) is outside the ${width}x$height window — it was never rendered",
            x in 0 until width && y in 0 until height,
        )
        return getPixel(x, y)
    }

    private fun crop(full: Bitmap, tag: String): Bitmap =
        cropToBounds(full, composeRule.onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot)

    /** Every swatch role the composed palette actually published, read back off its tags. */
    private fun renderedSwatchRoles(): Set<String> {
        val prefix = ZinelyV2CatalogTags.swatch("")
        return composeRule.onAllNodes(
            SemanticsMatcher("test tag starts with '$prefix'") {
                it.config.getOrNull(SemanticsProperties.TestTag)?.startsWith(prefix) == true
            },
            useUnmergedTree = true,
        ).fetchSemanticsNodes()
            .mapNotNull { it.config.getOrNull(SemanticsProperties.TestTag) }
            .map { it.removePrefix(prefix) }
            .toSet()
    }

    private fun Bitmap.pixels(): List<Int> =
        IntArray(width * height).also { getPixels(it, 0, width, 0, 0, width, height) }.toList()

    /**
     * `paper` as the corpus states it — the ground every icon cell and the plain material panel is drawn on.
     *
     * Read from the document, never transcribed. A literal here is worse than merely redundant: the icon
     * test compares each mark against this value to decide whether it drew anything, so a stale copy makes
     * *every* pixel differ from the ground and the "this mark drew nothing" assertion can never fire again.
     * It would not go red on a palette re-freeze — it would go quietly, permanently vacuous.
     */
    private fun paperArgb(darkTheme: Boolean): Int =
        corpusRoles().first { it.name == "paper" }.let { if (darkTheme) it.darkArgb else it.lightArgb }

    // ----- the parse itself, before anything relies on it ----------------------------------------

    @Test
    fun `the design document parses into the fourteen chrome roles it declares`() {
        // Guards the oracle. A regex that silently matched nothing would make every colour assertion
        // below vacuously green — the failure mode that turned three earlier packages red on review.
        //
        // Every assertion here is STRUCTURAL, and that is deliberate. An earlier version ended with
        // `assertEquals(0xFFF7F2E7, paper.lightArgb)` — a design value transcribed into the very test
        // whose thesis is "nothing is written down twice", which a legitimate re-freeze of the palette
        // would turn red for no reason. Checking the shape of the parse catches "matched nothing" and
        // "matched the wrong cells" without the test holding an opinion about what the design says.
        val roles = corpusRoles()
        assertEquals(
            "V2-TOKENS.md's semantic-role table should yield 14 rows; got ${roles.map { it.name }}",
            14,
            roles.size,
        )
        assertTrue("paper must be among them", roles.any { it.name == "paper" })
        assertEquals("every role must be distinct", 14, roles.map { it.name }.toSet().size)
        assertTrue(
            "every parsed colour must be fully opaque — a partial parse yields zero in the alpha byte",
            roles.all { (it.lightArgb ushr 24) == 0xFF && (it.darkArgb ushr 24) == 0xFF },
        )
        // Reading one colour cell twice — the classic wrong-cell parse — gives light == dark. The frozen
        // palette has a genuinely different value in each theme for all fourteen roles.
        assertTrue(
            "these roles parsed the same value for both themes, which means one column was read twice: " +
                roles.filter { it.lightArgb == it.darkArgb }.map { it.name },
            roles.none { it.lightArgb == it.darkArgb },
        )
    }

    @Test
    fun `every role the corpus declares has a swatch, and the catalog invents none`() {
        render(darkTheme = false) { ZinelyV2CatalogPalette() }
        val corpus = corpusRoles().map { it.name }.toSet()
        val rendered = renderedSwatchRoles()
        assertTrue(
            "the catalog is missing swatches for ${corpus - rendered}",
            (corpus - rendered).isEmpty(),
        )
        assertEquals(
            "the catalog shows swatches the corpus does not declare; only onMatcha is permitted, " +
                "because V2-TOKENS.md states it as a pairing rather than as a table row",
            setOf("onMatcha"),
            rendered - corpus,
        )
    }

    // ----- colour parity ------------------------------------------------------------------------

    @Test
    fun `every chrome role paints the exact colour the corpus states, in the light theme`() {
        assertPaletteMatchesCorpus(darkTheme = false)
    }

    @Test
    fun `every chrome role paints the exact colour the corpus states, in the warm-charcoal dark theme`() {
        assertPaletteMatchesCorpus(darkTheme = true)
    }

    private fun assertPaletteMatchesCorpus(darkTheme: Boolean) {
        val roles = corpusRoles()
        val bmp = render(darkTheme) { ZinelyV2CatalogPalette() }
        val theme = if (darkTheme) "dark" else "light"
        val wrong = roles.mapNotNull { role ->
            val expected = if (darkTheme) role.darkArgb else role.lightArgb
            val actual = bmp.centreOf(ZinelyV2CatalogTags.swatch(role.name))
            if (actual == expected) null else "${role.name}: expected ${hex(expected)}, painted ${hex(actual)}"
        }
        assertTrue(
            "$theme theme — ${wrong.size} of ${roles.size} chrome roles did not paint the value " +
                "V2-TOKENS.md states:\n  " + wrong.joinToString("\n  "),
            wrong.isEmpty(),
        )
    }

    private fun hex(argb: Int): String = "#%06X".format(argb and 0xFFFFFF)

    // ----- type parity --------------------------------------------------------------------------

    @Test
    fun `the bundled families actually reach the renderer, rather than falling back to the platform face`() {
        // You cannot assert "this is Fraunces" from a bitmap. You can assert it is not the same shape as
        // Inter — and a missing `res/font` resource, or a family that silently resolved to the system
        // default, collapses exactly that difference while still producing plausible-looking text.
        val full = render(darkTheme = false) { ZinelyV2CatalogType() }
        val voice = crop(full, ZinelyV2CatalogTags.specimen("voice-400")).pixels()
        val work = crop(full, ZinelyV2CatalogTags.specimen("work-400")).pixels()
        assertTrue(
            "Fraunces and Inter rendered the same word identically at the same size and weight — one of " +
                "the two bundled families is not being applied",
            voice != work,
        )
    }

    @Test
    fun `every weight and every declared style renders distinctly from every other`() {
        // The variable-font trap: a family can load while its weight axis never applies, giving one
        // weight for every FontWeight asked for.
        //
        // Asserted PAIRWISE across all seven specimens, not just 400-vs-600, because an earlier version
        // checked only the extremes — and `voice-500` could be silently downgraded to 400 while both the
        // parity suite and the goldens stayed green. (The goldens miss it because a single specimen row
        // is well under Roborazzi's 2% change threshold on the whole type crop, so a golden cannot be
        // relied on to catch one changed row.) The two styles the foundation actually DECLARES — `base`
        // and `sectionLabel` — were unasserted for the same reason and are now in the same net.
        val full = render(darkTheme = false) { ZinelyV2CatalogType() }
        val names = listOf("voice-400", "voice-500", "voice-600", "work-400", "work-600", "base", "sectionLabel")
        val rendered = names.associateWith { crop(full, ZinelyV2CatalogTags.specimen(it)).pixels() }
        val collisions = names.flatMapIndexed { i, a ->
            names.drop(i + 1).filter { b -> rendered[a] == rendered[b] }.map { b -> "$a == $b" }
        }
        assertTrue(
            "these specimens rendered pixel-identically, so a weight or a style is not reaching the " +
                "renderer: $collisions",
            collisions.isEmpty(),
        )
    }

    // ----- icon parity --------------------------------------------------------------------------

    @Test
    fun `every one of the frozen marks draws ink, and no two marks draw the same`() {
        val full = render(darkTheme = false) { ZinelyV2CatalogIcons() }
        val ground = paperArgb(darkTheme = false) // paper, the cell's own background, per the corpus
        val blank = mutableListOf<String>()
        val byPixels = mutableMapOf<List<Int>, String>()
        val identical = mutableListOf<String>()
        ZinelyV2Icons.All.forEach { icon ->
            val px = crop(full, ZinelyV2CatalogTags.icon(icon.name)).pixels()
            if (px.none { it != ground }) blank += icon.name
            val seen = byPixels.put(px, icon.name)
            if (seen != null) identical += "${seen} == ${icon.name}"
        }
        assertTrue("these marks drew nothing at all: $blank", blank.isEmpty())
        assertTrue(
            "these marks rendered pixel-identically, so at least one is drawing the wrong geometry: " +
                "$identical",
            identical.isEmpty(),
        )
        assertEquals("the frozen set is 36 marks", 36, ZinelyV2Icons.All.size)
    }

    // ----- material parity ----------------------------------------------------------------------

    @Test
    fun `the grain overlay actually changes the paper surface`() {
        // ADR-076's whole claim is that the tile is drawn at soft-light over the sheet. If the brush were
        // never applied — a missing resource, a no-op modifier — the sheet would still be the right cream
        // and every colour assertion above would stay green. Only a difference catches it.
        val full = render(darkTheme = false) { ZinelyV2CatalogMaterial() }
        val plain = crop(full, ZinelyV2CatalogTags.PaperPlain).pixels()
        val grained = crop(full, ZinelyV2CatalogTags.PaperGrained).pixels()
        assertTrue(
            "the grained panel is pixel-identical to the plain one — the grain is not being drawn " +
                "(on API 24-28 this is correct and expected, per D-014, but this host runs well above that)",
            plain != grained,
        )
        assertTrue(
            "the plain panel must be flat paper, exactly as the corpus states it",
            plain.all { it == paperArgb(darkTheme = false) },
        )
    }
}
