package com.aritr.zinely.core.editor.ink

import net.jqwik.api.Arbitraries
import net.jqwik.api.Arbitrary
import net.jqwik.api.ForAll
import net.jqwik.api.Property
import net.jqwik.api.Provide
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

private fun hx(i: Int) = InkHex("%06X".format(java.util.Locale.ROOT, i))
private fun fullInks(): InkLibrary = (1..9).fold(InkLibrary.EMPTY) { l, i -> l.use(hx(i)).library }
private fun fullPalettes(): InkLibrary = (1..9).fold(InkLibrary.EMPTY) { l, i ->
    (l.addPalette("P$i", listOf(hx(1), hx(2))) as PaletteResult.Done).library
}

class InkLibraryTest {
    @Test fun `given nine kept inks when a new hex is used then the least recent is evicted`() {
        val lib = fullInks()
        assertEquals(hx(1), lib.wouldEvict(hx(100)))
        val r = lib.use(hx(100))
        assertEquals(hx(1), r.evicted)
        assertEquals(9, r.library.myInks.size)
        assertEquals(hx(100), r.library.myInks.first().hex)
        assertTrue(r.library.myInks.none { it.hex == hx(1) })
    }

    @Test fun `given a kept hex when used or touched then position is stable and used is the new max`() {
        val lib = fullInks() // newest first: 9,8,7,6,5,...
        val target = lib.myInks[4].hex
        for (after in listOf(lib.use(target).library, lib.touch(target))) {
            assertEquals(target, after.myInks[4].hex)
            assertEquals(after.clock, after.myInks[4].used)
            assertTrue(after.myInks.all { it.used <= after.myInks[4].used })
        }
        assertNull(lib.use(target).evicted)
        assertNull(lib.wouldEvict(target))
    }

    @Test fun `given a hex not kept when touched then the receiver is returned`() {
        val lib = fullInks()
        assertSame(lib, lib.touch(hx(500)))
        assertNull(InkLibrary.EMPTY.wouldEvict(hx(1)))
    }

    @Test fun `given a tie on used then the first in the list is least recently used`() {
        val lib = InkLibrary(myInks = listOf(MyInk(hx(1), 5), MyInk(hx(2), 5)), clock = 5)
        assertEquals(hx(1), lib.leastRecentlyUsed()?.hex)
    }

    @Test fun `given nine palettes when add duplicate or starter copy then Full and unchanged`() {
        val lib = fullPalettes()
        assertEquals(PaletteResult.Full, lib.addPalette("X", listOf(hx(1), hx(2))))
        assertEquals(PaletteResult.Full, lib.duplicate(lib.palettes.first().id, " copy"))
        assertEquals(PaletteResult.Full, lib.saveStarterCopy("Two-colour", listOf(hx(1), hx(2))))
        val id = lib.palettes.last().id
        val up = lib.updatePalette(id, "Renamed", listOf(hx(3), hx(4)))
        assertTrue(up is PaletteResult.Done)
        assertEquals("Renamed", (up as PaletteResult.Done).library.palettes.first { it.id == id }.name)
    }

    @Test fun `given an unknown id when update or duplicate then Missing`() {
        assertEquals(PaletteResult.Missing, InkLibrary.EMPTY.updatePalette(7, "A", listOf(hx(1), hx(2))))
        assertEquals(PaletteResult.Missing, InkLibrary.EMPTY.duplicate(7, " copy"))
    }

    @Test fun `given names Night bus and night BUS 2 when uniqueName then Night bus 3`() {
        var lib = InkLibrary.EMPTY
        lib = (lib.addPalette("Night bus", listOf(hx(1), hx(2))) as PaletteResult.Done).library
        lib = (lib.addPalette("night BUS 2", listOf(hx(1), hx(2))) as PaletteResult.Done).library
        assertEquals("Night bus 3", lib.uniqueName("Night bus"))
        assertEquals("Free", lib.uniqueName("Free"))
    }

    @Test fun `duplicate names the copy and counts up`() {
        var lib = (InkLibrary.EMPTY.addPalette("Warm copy", listOf(hx(1), hx(2))) as PaletteResult.Done).library
        lib = (lib.addPalette("Warm", listOf(hx(1), hx(2))) as PaletteResult.Done).library
        val warm = lib.palettes.first { it.name == "Warm" }.id
        val warmCopy = lib.palettes.first { it.name == "Warm copy" }.id
        assertEquals("Warm copy 2", ((lib.duplicate(warmCopy, " copy") as PaletteResult.Done).library.palettes.first().name))
        assertEquals("Warm copy 2", ((lib.duplicate(warm, " copy") as PaletteResult.Done).library.palettes.first().name))
    }

    @Test fun `stripCopySuffix removes copy and numbered copy endings`() {
        assertEquals("Warm", stripCopySuffix("Warm copy 12", " copy"))
        assertEquals("Warm", stripCopySuffix("Warm copy", " copy"))
        assertEquals("Warm copy x", stripCopySuffix("Warm copy x", " copy"))
    }

    @Test fun `given duplicate inks and seven inks when added then deduped and first six`() {
        val a = InkLibrary.EMPTY.addPalette("A", listOf(hx(1), hx(1), hx(2))) as PaletteResult.Done
        assertEquals(listOf(hx(1), hx(2)), a.library.palettes.single().inks)
        val b = InkLibrary.EMPTY.addPalette("B", (1..7).map(::hx)) as PaletteResult.Done
        assertEquals((1..6).map(::hx), b.library.palettes.single().inks)
        assertEquals(b.paletteId, b.library.openPaletteId)
    }

    @Test fun `add validation reports too few and no name and trims and caps the name`() {
        assertEquals(PaletteResult.TooFew, InkLibrary.EMPTY.addPalette("A", listOf(hx(1))))
        assertEquals(PaletteResult.TooFew, InkLibrary.EMPTY.addPalette("A", listOf(hx(1), hx(1))))
        assertEquals(PaletteResult.NoName, InkLibrary.EMPTY.addPalette("   ", listOf(hx(1), hx(2))))
        val d = InkLibrary.EMPTY.addPalette("  " + "x".repeat(40), listOf(hx(1), hx(2))) as PaletteResult.Done
        assertEquals(PALETTE_NAME_MAX, d.library.palettes.single().name.length)
    }

    @Test fun `given a removed kept ink then every palette still holds it`() {
        val l0 = InkLibrary.EMPTY.use(hx(1)).library
        val l1 = (l0.addPalette("P", listOf(hx(1), hx(2))) as PaletteResult.Done).library
        val l2 = l1.remove(hx(1))
        assertTrue(l2.myInks.isEmpty())
        assertEquals(listOf(hx(1), hx(2)), l2.palettes.single().inks)
    }

    @Test fun `given delete of the open palette then the first remaining opens`() {
        var lib = InkLibrary.EMPTY
        repeat(3) { lib = (lib.addPalette("P$it", listOf(hx(1), hx(2))) as PaletteResult.Done).library }
        val open = lib.openPaletteId!!
        val d = lib.deletePalette(open)
        assertEquals(lib.palettes.first { it.id != open }.id, d.openPalette()?.id)
        assertEquals(2, d.palettes.size)
        assertNull(InkLibrary.EMPTY.openPalette())
        assertEquals(lib.openPaletteId, lib.open(999).openPaletteId) // unknown id: unchanged
    }

    @Test fun `given a messy library when sanitized then caps and invariants hold`() {
        val messy = InkLibrary(
            myInks = (1..12).map { MyInk(hx(it), it.toLong()) } + MyInk(hx(1), 99),
            palettes = listOf(
                InkPalette(1, "One ink", listOf(hx(1))),
                InkPalette(2, "  ", listOf(hx(1), hx(2))),
                InkPalette(3, " Ok ", (1..8).map(::hx) + hx(1)),
            ),
            clock = 0, nextPaletteId = 1, openPaletteId = 42,
        )
        val s = messy.sanitized()
        assertEquals(9, s.myInks.size)
        assertEquals(1, s.myInks.count { it.hex == hx(1) })
        assertEquals(1, s.palettes.size)
        assertEquals("Ok", s.palettes.single().name)
        assertEquals(6, s.palettes.single().inks.size)
        assertNull(s.openPaletteId)
        assertEquals(9L, s.clock) // max used among the nine kept (the dropped duplicate's 99 does not count)
        assertTrue(s.nextPaletteId > 3)
        assertEquals(s, s.sanitized())
        assertNotNull(s.openPalette())
    }

    @Property(tries = 200)
    fun `any sequence of use keeps the cap uniqueness and a rising clock`(@ForAll("hexes") seq: List<Int>) {
        var lib = InkLibrary.EMPTY
        for (i in seq) {
            val before = lib.clock
            lib = lib.use(hx(i)).library
            assertTrue(lib.clock > before)
            assertTrue(lib.myInks.size <= MY_INKS_CAP)
            assertEquals(lib.myInks.size, lib.myInks.map { it.hex }.toSet().size)
            assertEquals(hx(i), lib.myInks.first { it.used == lib.clock }.hex)
        }
    }

    @Provide
    fun hexes(): Arbitrary<List<Int>> = Arbitraries.integers().between(0, 14).list().ofMaxSize(60)
}
