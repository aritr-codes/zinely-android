package com.aritr.zinely.core.editor

import com.aritr.zinely.core.model.ColorRgba
import com.aritr.zinely.core.model.DecorElement
import com.aritr.zinely.core.model.Element
import com.aritr.zinely.core.model.ImageElement
import com.aritr.zinely.core.model.Page
import com.aritr.zinely.core.model.PageRole
import com.aritr.zinely.core.model.PaperSize
import com.aritr.zinely.core.model.PtPoint
import com.aritr.zinely.core.model.TextElement
import com.aritr.zinely.core.model.Transform
import com.aritr.zinely.core.model.ZineDocument
import com.aritr.zinely.core.model.ZineFormat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Taps pass through the empty part of six holed pieces (ADR-124): drawn, then near, then box.
 *
 * Every distance here is in **page points** — `HitTest` knows nothing of dp. Unless a test says
 * otherwise a piece is 100×100 pt at (50, 50), so unit point (u, v) is page point (50 + 100u, 50 + 100v).
 */
class HitTestInkTest {

    private val tol = 8.0

    private fun piece(
        id: String,
        supplyId: String = id,
        x: Double = 50.0,
        y: Double = 50.0,
        w: Double = 100.0,
        h: Double = 100.0,
        z: Int = 1,
        rot: Double = 0.0,
        mirrored: Boolean = false,
        flipped: Boolean = false,
    ) = DecorElement(id, Transform(x, y, w, h, rot), z, supplyId, ColorRgba(0, 0, 0), mirrored, flipped)

    private fun photo(id: String = "photo", z: Int = 0) =
        ImageElement(id = id, transform = Transform(0.0, 0.0, 200.0, 200.0), zIndex = z, assetId = "sha-$id")

    private fun words(id: String = "words", z: Int = 0) =
        TextElement(id = id, transform = Transform(0.0, 0.0, 200.0, 200.0), zIndex = z, text = "x")

    private fun page(vararg els: Element) = Page(index = 0, role = PageRole.INTERIOR, elements = els.toList())

    /** Unit point of the default 100×100 piece at (50, 50), as a page point. */
    private fun unit(u: Double, v: Double) = PtPoint(50.0 + 100.0 * u, 50.0 + 100.0 * v)

    /** Supply id → a unit point on its ink, and unit points in its hole or bare paper. */
    private val six: Map<String, Pair<PtPoint, List<PtPoint>>> = mapOf(
        "paper.window" to (unit(0.05, 0.5) to listOf(unit(0.5, 0.5))),
        "paper.hole" to (unit(0.125, 0.5) to listOf(unit(0.5, 0.5))),
        "shape.ring" to (unit(0.1, 0.5) to listOf(unit(0.5, 0.5))),
        "fix.grommet" to (unit(0.1, 0.5) to listOf(unit(0.5, 0.5))),
        "fix.corner" to (unit(0.06, 0.9) to listOf(unit(0.25, 0.7), unit(0.9, 0.2))),
        "mark.registration" to (unit(0.05, 0.5) to listOf(unit(0.5, 0.5), unit(0.15, 0.15))),
    )

    // — 7, 8: each of the six —

    @Test
    fun `each of the six over a photo - the ink is the piece, the hole is the photo`() {
        for ((id, probes) in six) {
            val p = page(photo(), piece(id))
            for (t in listOf(0.0, tol)) {
                assertEquals(id, HitTest.topmostAt(p, probes.first, t), "$id ink, tolerance $t")
                for (bare in probes.second) {
                    assertEquals("photo", HitTest.topmostAt(p, bare, t), "$id bare paper at $bare, tolerance $t")
                }
            }
        }
    }

    @Test
    fun `each of the six over words - the hole reaches the words`() {
        for ((id, probes) in six) {
            val p = page(words(), piece(id))
            assertEquals(id, HitTest.topmostAt(p, probes.first, tol), "$id ink")
            for (bare in probes.second) assertEquals("words", HitTest.topmostAt(p, bare, tol), "$id at $bare")
        }
    }

    @Test
    fun `each of the six over blank paper - the hole still selects the piece, by its box`() {
        for ((id, probes) in six) {
            val p = page(piece(id))
            for (t in listOf(0.0, tol)) {
                assertEquals(id, HitTest.topmostAt(p, probes.first, t), "$id ink, tolerance $t")
                for (bare in probes.second) assertEquals(id, HitTest.topmostAt(p, bare, t), "$id at $bare, tolerance $t")
            }
        }
    }

    // — 9: tolerance —

    @Test
    fun `just off the ink over blank paper - inside the tolerance is the piece, beyond it nothing`() {
        val p = page(piece("paper.window"))
        val fiveOff = PtPoint(45.0, 100.0) // 5 pt left of the band, outside the box
        val nineOff = PtPoint(41.0, 100.0)
        assertEquals("paper.window", HitTest.topmostAt(p, fiveOff, tol))
        assertEquals("paper.window", HitTest.topmostAt(p, PtPoint(42.5, 100.0), tol), "7.5 pt is within")
        assertNull(HitTest.topmostAt(p, nineOff, tol))
        assertNull(HitTest.topmostAt(p, fiveOff), "no tolerance, no near pass")
        assertNull(HitTest.topmostAt(p, fiveOff, 0.0))
    }

    @Test
    fun `the thin registration arm is within reach from either side at its default size`() {
        // ~34 pt box: the arm is 2.4 pt wide. 5 pt above it, in the box, over blank paper.
        val reg = piece("mark.registration", x = 100.0, y = 100.0, w = 34.0, h = 34.0)
        val aboveTheLeftArm = PtPoint(102.0, 117.0 - 0.035 * 34.0 - 5.0)
        assertEquals("mark.registration", HitTest.topmostAt(page(reg), aboveTheLeftArm, tol))
        // Over a photo the same point is the photo's: exact beats near.
        assertEquals("photo", HitTest.topmostAt(page(photo(), reg), aboveTheLeftArm, tol))
    }

    @Test
    fun `a tolerance that is not finite is no tolerance`() {
        // What a zero preview scale would produce at the gesture layer (8 dp / 0). It must not make every
        // tap on blank paper select the topmost holed piece on the page.
        val p = page(piece("paper.window"))
        for (bad in listOf(Double.POSITIVE_INFINITY, Double.NaN, -8.0)) {
            assertNull(HitTest.topmostAt(p, PtPoint(1000.0, 1000.0), bad), "tolerance $bad, far from everything")
            assertNull(HitTest.topmostAt(p, PtPoint(45.0, 100.0), bad), "tolerance $bad, 5 pt off the ink")
            assertEquals("paper.window", HitTest.topmostAt(p, PtPoint(55.0, 100.0), bad), "tolerance $bad, on the ink")
        }
    }

    // — 9a: exact beats near —

    @Test
    fun `exact beats near - a photo, words or an ordinary piece containing the tap wins over a holed piece above it`() {
        val window = piece("paper.window", z = 5)
        val justOutsideTheBand = PtPoint(45.0, 100.0) // 5 pt off the ink, outside the window's box
        val justInsideTheHole = PtPoint(67.0, 100.0) // 3 pt off the ink, inside the window's box
        val under = listOf(
            photo(),
            words(),
            DecorElement("ordinary", Transform(0.0, 0.0, 200.0, 200.0), 0, "shape.rect", ColorRgba(0, 0, 0)),
        )
        for (el in under) {
            val p = page(el, window)
            assertEquals(el.id, HitTest.topmostAt(p, justOutsideTheBand, tol), "${el.id}, tap outside the window's box")
            assertEquals(el.id, HitTest.topmostAt(p, justInsideTheHole, tol), "${el.id}, tap in the window's hole")
            // The near candidate is real: without the containing element the same taps pick the window up.
            assertEquals("paper.window", HitTest.topmostAt(page(window), justOutsideTheBand, tol))
        }
    }

    // — 9b: stacked near —

    @Test
    fun `stacked near - the upper piece at 7 pt beats the lower one at 2 pt`() {
        val tap = PtPoint(100.0, 100.0)
        val lower = piece("lower", "paper.window", x = -2.0, z = 1) // right edge at x = 98: 2 pt away
        val upper7 = piece("upper", "paper.window", x = 107.0, z = 2) // left edge at x = 107: 7 pt away
        val upper9 = piece("upper", "paper.window", x = 109.0, z = 2)

        assertEquals("upper", HitTest.topmostAt(page(lower, upper7), tap, tol))
        // List order must not be what decided it.
        assertEquals("upper", HitTest.topmostAt(page(upper7, lower), tap, tol))
        // At 9 pt the upper piece is no longer a candidate, so the lower one is picked.
        assertEquals("lower", HitTest.topmostAt(page(lower, upper9), tap, tol))
        assertNull(HitTest.topmostAt(page(lower, upper7), tap, 0.0), "neither box holds the tap")
    }

    // — 10: order —

    @Test
    fun `two holed pieces over a photo - both holes reach the photo`() {
        val p = page(photo(), piece("big", "paper.window", x = 40.0, y = 40.0, w = 120.0, h = 120.0, z = 1), piece("paper.window", z = 2))
        assertEquals("photo", HitTest.topmostAt(p, PtPoint(100.0, 100.0), tol))
    }

    @Test
    fun `the lower piece's ink seen through the upper one's hole is the lower piece`() {
        val ring = piece("shape.ring", x = 80.0, y = 80.0, w = 40.0, h = 40.0, z = 1)
        val p = page(photo(), ring, piece("paper.window", z = 2))
        assertEquals("shape.ring", HitTest.topmostAt(p, PtPoint(84.0, 100.0), tol)) // the ring's left band
        assertEquals("photo", HitTest.topmostAt(p, PtPoint(100.0, 100.0), tol)) // both holes
    }

    @Test
    fun `a ring in a larger window's hole on blank paper - near picks the ring, the box picks the window`() {
        val ring = piece("shape.ring", x = 80.0, y = 80.0, w = 40.0, h = 40.0, z = 1)
        val p = page(ring, piece("paper.window", z = 2))
        val justOffTheRing = PtPoint(77.0, 100.0) // 3 pt left of the ring, outside its box, in the window's hole
        assertEquals("paper.window", HitTest.topmostAt(p, justOffTheRing, 0.0))
        assertEquals("shape.ring", HitTest.topmostAt(p, justOffTheRing, tol))
    }

    // — 11: rotation and the two flips, on the one asymmetric piece —

    @Test
    fun `a rotated photo corner - the ink turns with it`() {
        // 90° clockwise about the centre (100, 100): local (x, y) → (−y, x). The bottom band's (0.5, 0.95)
        // is local (0, 45) and lands at page (55, 100); where it used to be is now the empty half.
        val p = page(photo(), piece("fix.corner", rot = 90.0))
        assertEquals("fix.corner", HitTest.topmostAt(p, PtPoint(55.0, 100.0)))
        assertEquals("photo", HitTest.topmostAt(p, PtPoint(100.0, 145.0)))
        assertEquals("fix.corner", HitTest.topmostAt(page(photo(), piece("fix.corner")), PtPoint(100.0, 145.0)))
    }

    @Test
    fun `a mirrored photo corner - the ink swaps sides`() {
        val p = page(photo(), piece("fix.corner", mirrored = true))
        assertEquals("fix.corner", HitTest.topmostAt(p, unit(0.94, 0.9)))
        assertEquals("photo", HitTest.topmostAt(p, unit(0.06, 0.9)))
    }

    @Test
    fun `a vertically flipped photo corner - the ink swaps ends`() {
        val p = page(photo(), piece("fix.corner", flipped = true))
        assertEquals("fix.corner", HitTest.topmostAt(p, unit(0.5, 0.05)))
        assertEquals("photo", HitTest.topmostAt(p, unit(0.5, 0.95)))
    }

    // — 12: everything else keeps its box —

    @Test
    fun `crop marks, an unknown supply and an ordinary piece keep their box over a photo`() {
        // Each of these is bare paper at the probed point, and each still owns the tap.
        assertEquals("mark.crop", HitTest.topmostAt(page(photo(), piece("mark.crop")), unit(0.5, 0.5), tol))
        assertEquals("zzz.unknown", HitTest.topmostAt(page(photo(), piece("zzz.unknown")), unit(0.5, 0.5), tol))
        assertEquals("fix.staple", HitTest.topmostAt(page(photo(), piece("fix.staple")), unit(0.5, 0.8), tol))
        // …and none of them reaches for a tap outside its box.
        assertEquals("photo", HitTest.topmostAt(page(photo(), piece("mark.crop")), PtPoint(45.0, 100.0), tol))
        assertNull(HitTest.topmostAt(page(piece("mark.crop")), PtPoint(45.0, 100.0), tol))
    }

    @Test
    fun `a holed piece with a degenerate box keeps its box`() {
        val flat = piece("paper.window", h = 0.0)
        assertEquals("paper.window", HitTest.topmostAt(page(flat), PtPoint(100.0, 50.0), tol))
        assertNull(HitTest.topmostAt(page(flat), PtPoint(100.0, 53.0), tol), "a null distance is never a near candidate")
    }

    // — 15, 17: the reducer's tap paths —

    private fun model(vararg els: Element) = EditorModel(
        document = ZineDocument(
            format = ZineFormat.SINGLE_SHEET_8,
            paperSize = PaperSize.LETTER,
            pages = listOf(page(*els)),
        ),
    )

    private val hole = PtPoint(100.0, 100.0)
    private val band = PtPoint(55.0, 100.0)

    @Test
    fun `DoubleTapAt through a window's hole onto a photo opens Reframe`() {
        val r = EditorReducer.reduce(model(photo(), piece("paper.window")), Intent.DoubleTapAt(hole))
        assertEquals("photo", (r.model.interaction as Interaction.Reframing).id)
    }

    @Test
    fun `DoubleTapAt through a window's hole onto words opens the text session`() {
        val r = EditorReducer.reduce(model(words(), piece("paper.window")), Intent.DoubleTapAt(hole))
        assertEquals("words", (r.model.interaction as Interaction.EditingText).id)
    }

    @Test
    fun `DoubleTapAt on the window's band changes nothing`() {
        val start = model(photo(), piece("paper.window"))
        val r = EditorReducer.reduce(start, Intent.DoubleTapAt(band))
        assertEquals(start, r.model)
        assertTrue(r.effects.isEmpty())
    }

    @Test
    fun `SelectAt in the hole selects the photo, then on the band selects the window`() {
        val start = model(photo(), piece("paper.window"))
        val first = EditorReducer.reduce(start, Intent.SelectAt(hole, tol)).model
        assertEquals(setOf("photo"), first.selection)
        val second = EditorReducer.reduce(first, Intent.SelectAt(band, tol)).model
        assertEquals(setOf("paper.window"), second.selection)
    }

    @Test
    fun `SelectAt hands its tolerance to the hit test`() {
        val start = model(piece("paper.window"))
        val fiveOff = PtPoint(45.0, 100.0)
        assertEquals(setOf("paper.window"), EditorReducer.reduce(start, Intent.SelectAt(fiveOff, tol)).model.selection)
        assertEquals(emptySet<String>(), EditorReducer.reduce(start, Intent.SelectAt(fiveOff)).model.selection)
    }

    @Test
    fun `a tap the box rejects by one rounding step is not ink either`() {
        // The sample the property test shrank to in CI: at 180 degrees the un-rotated corner lands one
        // rounding step outside the box, while the unit-space ink test rounds it back onto the band.
        val window = DecorElement(
            "w", Transform(0.0, 0.0, 10.0, 10.0, 180.0), 0, "paper.window", ColorRgba(0, 0, 0),
            mirrored = true, flippedVertically = true,
        )
        val corner = PtPoint(0.0, 0.0)
        assertTrue(!HitTest.contains(window, corner), "the premise: the box rejects this corner")
        assertNull(HitTest.topmostAt(page(window), corner))
    }
}
