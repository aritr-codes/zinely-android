package com.aritr.zinely.home

import com.aritr.zinely.core.data.repository.ProjectSummary
import com.aritr.zinely.core.model.PaperSize
import com.aritr.zinely.core.model.ZineCoverRecipe
import com.aritr.zinely.core.model.ZineCoverStamp
import com.aritr.zinely.core.model.ZineCoverSurface
import com.aritr.zinely.core.model.ZineFormat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

/**
 * `ProjectSummary` → the V2 Library's [com.aritr.zinely.feature.library.LibraryZine]
 * ([ADR-086](docs/DECISIONS.md#adr-086) rows 6–8, 9).
 *
 * Pure mapping, so pure tests: no Android, no Hilt, no clock seam — `now` is a parameter, which is why
 * [editedLabel] never needed one either.
 */
class LibraryZineMappingTest {

    private fun summary(
        title: String = "Sunday market",
        paperSize: PaperSize = PaperSize.A4,
        updatedAtEpochMs: Long = NOW - TWO_DAYS,
        cover: ZineCoverRecipe? = RECIPE,
    ) = ProjectSummary(
        id = "p1",
        title = title,
        format = ZineFormat.SINGLE_SHEET_8,
        paperSize = paperSize,
        createdAtEpochMs = 0L,
        updatedAtEpochMs = updatedAtEpochMs,
        documentSchemaVersion = 1,
        cover = cover,
    )

    @Test
    fun `the subtitle reads paper then relative date, in that order`() {
        // Given a zine last touched two days ago, on A4
        val project = summary()

        // When
        val zine = project.toLibraryZine(NOW)

        // Then `data-sub` — `"A4 · 2 days ago"` (`v2-library.html:149`). Paper first, separator, then
        // recency. The recency words are [editedLabel]'s, which is already unit-tested and is the
        // authority on thresholds the frozen file never states; what is asserted here is the WIRING.
        assertEquals("A4 · ${editedLabel(project.updatedAtEpochMs, NOW)}", zine.subtitle)
        assertEquals("A4 · Edited 2 days ago", zine.subtitle)
    }

    @Test
    fun `the subtitle names the paper the project is actually on`() {
        // A separator with a hard-coded stock either side reads perfectly and is wrong for half the
        // shelf, so the paper is asserted against a project that is not on the default.
        val zine = summary(paperSize = PaperSize.LETTER).toLibraryZine(NOW)
        // "US Letter", not "Letter": ADR-101 P3 renamed the size to match the frozen `.paperseg`, and
        // `shelfLabel()` now reads `Copy.Paper` instead of repeating the string. This assertion is what
        // was left saying the old name.
        assertEquals("US Letter · Edited 2 days ago", zine.subtitle)
    }

    @Test
    fun `the cover is the persisted recipe, read and not re-derived`() {
        val zine = summary().toLibraryZine(NOW)
        // D-017: assigned once, at creation, and stored. The mapper's only job is to carry it.
        assertEquals(RECIPE, zine.cover)
    }

    @Test
    fun `two projects with the same title map to whatever each one stores, not to the same cover`() {
        // The behavioural half of "never derived from the title": identical titles, different stored
        // recipes, and the mapper must not collapse them. A title-seeded implementation passes every
        // single-project assertion above and fails here.
        val a = summary(title = "Same name", cover = RECIPE).toLibraryZine(NOW)
        val b = summary(title = "Same name", cover = OTHER_RECIPE).toLibraryZine(NOW)
        assertEquals(RECIPE, a.cover)
        assertEquals(OTHER_RECIPE, b.cover)
    }

    @Test
    fun `a project with no stored cover still reaches the shelf as an object`() {
        // `null` means a legacy backfill could not be written — the repository returns the meta
        // unchanged rather than fabricating an identity it cannot store. A shelf cannot draw an object
        // with no cover, so one is drawn for this rendering only. It is NOT derived from the id or the
        // title: that would be exactly the inference D-017 forbids, and it would look correct forever.
        val zine = summary(cover = null).toLibraryZine(NOW)
        assertNotNull(zine.cover)
    }

    @Test
    fun `a fallback cover is drawn once per zine, not once per emission`() {
        // The defect the mid-package review found: `cover ?: newZineCoverRecipe()` inline re-draws on
        // **every** state emission — and the shelf re-emits on every store change and every return from
        // the editor. That is the identity flicker D-017 exists to prevent, arriving by the back door on
        // the one path a real user can reach, and `assertNotNull` above passes on it happily.
        // The assigner is scripted to hand out a *different* recipe each call, so a per-emission draw is
        // unmistakable. Over the real `newZineCoverRecipe` two draws collide once in thirty-six, and a
        // test that flakes one run in thirty-six is not a proof of anything.
        val fallbacks = FallbackCovers(distinctRecipes())
        val project = summary(cover = null)

        val first = project.toLibraryZine(NOW, fallbacks::get).cover
        val second = project.toLibraryZine(NOW + 1, fallbacks::get).cover
        val third = project.toLibraryZine(NOW + 2, fallbacks::get).cover

        assertEquals("the zine was repainted on the second emission", first, second)
        assertEquals("the zine was repainted on the third emission", first, third)
    }

    @Test
    fun `two coverless zines do not share one fallback`() {
        // The other half, and the one a naive memo gets wrong: a single remembered recipe for "the
        // fallback" would make every unassigned zine identical — two indistinguishable objects on a
        // covers-only shelf, which is the exact failure D-026 protects duplicates from.
        val fallbacks = FallbackCovers(distinctRecipes())
        val a = summary(cover = null).copy(id = "a").toLibraryZine(NOW, fallbacks::get)
        val b = summary(cover = null).copy(id = "b").toLibraryZine(NOW, fallbacks::get)
        assertNotEquals("two different zines were given the same fallback cover", a.cover, b.cover)
    }

    /** An assigner that never repeats itself, so "drawn again" and "drawn once" cannot be confused. */
    private fun distinctRecipes(): () -> ZineCoverRecipe {
        var next = 0
        return {
            val i = next++
            ZineCoverRecipe(
                ZineCoverSurface.entries[i % ZineCoverSurface.entries.size],
                ZineCoverStamp.entries[i % ZineCoverStamp.entries.size],
            )
        }
    }

    private companion object {
        const val NOW = 1_000_000_000_000L
        const val TWO_DAYS = 2 * 86_400_000L
        val RECIPE = ZineCoverRecipe(ZineCoverSurface.MatchaInk, ZineCoverStamp.Sun)
        val OTHER_RECIPE = ZineCoverRecipe(ZineCoverSurface.OchreInk, ZineCoverStamp.Waves)
    }
}
