package com.aritr.zinely.core.editor.ink

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

private fun h(i: Int) = InkHex("%06X".format(java.util.Locale.ROOT, i))
private fun draftOf(vararg i: Int, id: Long? = 1, name: String = "P", orig: List<InkHex> = i.map(::h)) =
    PaletteDraft(id, name, name, i.map(::h), orig)

class PaletteDraftTest {
    @Test fun `new draft holds the current ink under a unique default name`() {
        val lib = (InkLibrary.EMPTY.addPalette("My palette", listOf(h(1), h(2))) as PaletteResult.Done).library
        val d = PaletteDraft.newDraft(lib, h(9), "My palette")
        assertNull(d.id)
        assertEquals("My palette 2", d.name)
        assertEquals("My palette 2", d.startName)
        assertEquals(listOf(h(9)), d.inks)
        assertEquals(emptyList<InkHex>(), d.orig)
    }

    @Test fun `edit draft copies the palette`() {
        val d = PaletteDraft.editDraft(InkPalette(4, "Warm", listOf(h(1), h(2))))
        assertEquals(4L, d.id)
        assertEquals("Warm", d.name)
        assertEquals("Warm", d.startName)
        assertEquals(d.inks, d.orig)
    }

    @Test fun `toggle adds removes and refuses a seventh`() {
        val d = draftOf(1)
        val added = (d.toggle(h(2)) as ToggleResult.Added).draft
        assertEquals(listOf(h(1), h(2)), added.inks)
        val removed = (added.toggle(h(1)) as ToggleResult.Removed).draft
        assertEquals(listOf(h(2)), removed.inks)
        val six = draftOf(1, 2, 3, 4, 5, 6)
        assertEquals(ToggleResult.Full, six.toggle(h(7)))
        assertTrue(six.toggle(h(3)) is ToggleResult.Removed)
    }

    @Test fun `removeAt drops one and ignores a bad index`() {
        val d = draftOf(1, 2, 3)
        assertEquals(listOf(h(1), h(3)), d.removeAt(1).inks)
        assertEquals(d, d.removeAt(7))
        assertEquals(d, d.removeAt(-1))
    }

    @Test fun `addMixed has three outcomes and already in beats full`() {
        assertTrue(draftOf(1, 2).addMixed(h(3)) is MixIntoPalette.Added)
        assertEquals(listOf(h(1), h(2), h(3)), (draftOf(1, 2).addMixed(h(3)) as MixIntoPalette.Added).draft.inks)
        assertEquals(MixIntoPalette.AlreadyIn, draftOf(1, 2).addMixed(h(2)))
        val six = draftOf(1, 2, 3, 4, 5, 6)
        assertEquals(MixIntoPalette.PaletteFull, six.addMixed(h(7)))
        assertEquals(MixIntoPalette.AlreadyIn, six.addMixed(h(6)))
    }

    @Test fun `lost excludes kept and built in inks and those still picked`() {
        val lib = InkLibrary.EMPTY.use(h(2)).library
        val d = PaletteDraft(1, "P", "P", inks = listOf(h(4)), orig = listOf(h(1), h(2), h(3), h(4)))
        assertEquals(listOf(h(1)), d.lost(lib, builtIns = listOf(h(3))))
    }

    @Test fun `canSave needs a name and two inks`() {
        assertTrue(draftOf(1, 2).canSave())
        assertFalse(draftOf(1).canSave())
        assertFalse(draftOf(1, 2).copy(name = "  ").canSave())
    }

    @Test fun `changed is false for a fresh new draft and true after renaming it`() {
        val lib = InkLibrary.EMPTY
        val fresh = PaletteDraft.newDraft(lib, h(9), "My palette")
        assertFalse(fresh.changed(h(9)))
        assertTrue(fresh.copy(name = "Night").changed(h(9)))
        assertTrue(fresh.copy(name = "My palette ").changed(h(9)).not()) // trailing space is not a change
        assertTrue(fresh.copy(inks = listOf(h(9), h(1))).changed(h(9)))
    }

    @Test fun `changed for an edit draft tracks name and picks`() {
        val d = PaletteDraft.editDraft(InkPalette(4, "Warm", listOf(h(1), h(2))))
        assertFalse(d.changed(h(9)))
        assertTrue(d.copy(name = "Cool").changed(h(9)))
        assertTrue(d.copy(inks = listOf(h(1), h(3))).changed(h(9)))
        assertTrue(d.copy(inks = listOf(h(2), h(1))).changed(h(9))) // order counts
    }

    @Test fun `isDuplicateName ignores case and itself`() {
        val lib = (InkLibrary.EMPTY.addPalette("Night bus", listOf(h(1), h(2))) as PaletteResult.Done).library
        val own = PaletteDraft.editDraft(lib.palettes.single())
        assertFalse(own.isDuplicateName(lib))
        assertTrue(PaletteDraft.newDraft(lib, h(1), "x").copy(name = " night BUS ").isDuplicateName(lib))
        assertFalse(PaletteDraft.newDraft(lib, h(1), "x").copy(name = " ").isDuplicateName(lib))
    }
}
