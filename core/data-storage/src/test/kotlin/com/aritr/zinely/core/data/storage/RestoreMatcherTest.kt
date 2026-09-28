package com.aritr.zinely.core.data.storage

import com.aritr.zinely.core.data.asset.ZineBackupProjectEntry
import com.aritr.zinely.core.model.Page
import com.aritr.zinely.core.model.PageRole
import com.aritr.zinely.core.model.PaperSize
import com.aritr.zinely.core.model.TextElement
import com.aritr.zinely.core.model.Transform
import com.aritr.zinely.core.model.ZineDocument
import com.aritr.zinely.core.model.ZineFormat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.nio.file.Path

/** ADR-121's matching rule, case by case (Brief 01 Part N "Tests required later", the pure half). */
class RestoreMatcherTest {

    @Test
    fun `multiplicity counts one shelf zine for one backup zine`() {
        val poems = backup("a", "Poems")
        fun shelf(n: Int) = List(n) { shelfCopyOf(poems) }

        assertCounts(added = 0, here = 1, RestoreMatcher.match(listOf(poems), shelf(1)))
        assertCounts(added = 0, here = 2, RestoreMatcher.match(listOf(poems, poems.copyAs("b")), shelf(2)))
        assertCounts(added = 0, here = 1, RestoreMatcher.match(listOf(poems), shelf(2)))
        assertCounts(added = 1, here = 1, RestoreMatcher.match(listOf(poems, poems.copyAs("b")), shelf(1)))
    }

    @Test
    fun `an empty shelf or another phone adds everything`() {
        val zines = listOf(backup("a", "Poems"), backup("b", "Maps", text = "north"))

        assertCounts(added = 2, here = 0, RestoreMatcher.match(zines, emptyList()))
        assertCounts(added = 2, here = 0, RestoreMatcher.match(zines, listOf(shelf("Recipes", document("soup")))))
    }

    @Test
    fun `the same backup restored twice adds nothing new the second time`() {
        val zines = listOf(backup("a", "Poems"), backup("b", "Maps", text = "north"))
        val first = RestoreMatcher.match(zines, emptyList())
        val shelfAfterFirst = first.added.map(::shelfCopyOf)

        assertCounts(added = 0, here = 2, RestoreMatcher.match(zines, shelfAfterFirst))
    }

    @Test
    fun `a changed version is added beside the identical one`() {
        val original = backup("a", "Poems", text = "v1")
        val changed = backup("b", "Poems", text = "v2")

        val match = RestoreMatcher.match(listOf(original, changed), listOf(shelfCopyOf(original)))

        assertEquals(listOf(changed), match.added)
        assertEquals(1, match.alreadyHereCount)
    }

    @Test
    fun `a zine edited on the shelf since the backup is added`() {
        val poems = backup("a", "Poems", text = "before")

        assertCounts(added = 1, here = 0, RestoreMatcher.match(listOf(poems), listOf(shelf("Poems", document("after")))))
    }

    @Test
    fun `a renamed zine is added`() {
        val poems = backup("a", "Poems")

        assertCounts(added = 1, here = 0, RestoreMatcher.match(listOf(poems), listOf(shelf("Poems ", poems.document, poems.manifestEntry.documentSha256))))
    }

    @Test
    fun `equal content in older bytes is already here`() {
        val poems = backup("a", "Poems")
        val olderBytes = shelf("Poems", poems.document, sha = "f".repeat(64))

        assertCounts(added = 0, here = 1, RestoreMatcher.match(listOf(poems), listOf(olderBytes)))
    }

    /** `ZineFormat` has one value today, so only the paper half of the key can differ. */
    @Test
    fun `a different paper size is added`() {
        val poems = backup("a", "Poems")
        val a4 = poems.document.copy(paperSize = PaperSize.A4)

        assertCounts(added = 1, here = 0, RestoreMatcher.match(listOf(poems), listOf(shelf("Poems", a4))))
    }

    @Test
    fun `doubt adds - undecodable or unreadable shelf zines match nothing unless bytes are identical`() {
        val poems = backup("a", "Poems")

        val undecodable = ShelfZine("Poems", "e".repeat(64)) { null }
        val noTitle = ShelfZine(null, poems.manifestEntry.documentSha256) { poems.document }
        val noBytes = ShelfZine("Poems", null) { poems.document }
        listOf(undecodable, noTitle, noBytes).forEach { shelfZine ->
            assertCounts(added = 1, here = 0, RestoreMatcher.match(listOf(poems), listOf(shelfZine)))
        }

        val identicalButUndecodable = ShelfZine("Poems", poems.manifestEntry.documentSha256) { null }
        assertCounts(added = 0, here = 1, RestoreMatcher.match(listOf(poems), listOf(identicalButUndecodable)))
    }

    @Test
    fun `added zines keep manifest order`() {
        val zines = listOf(backup("c", "C", text = "3"), backup("a", "A", text = "1"), backup("b", "B", text = "2"))

        val match = RestoreMatcher.match(zines, listOf(shelfCopyOf(zines[1])))

        assertEquals(listOf("c", "b"), match.added.map { it.manifestEntry.sourceProjectId })
    }

    @Test
    fun `a shelf zine is decoded only when its title matches and its bytes don't`() {
        val poems = backup("a", "Poems")
        var decodes = 0
        fun counting(title: String, sha: String) = ShelfZine(title, sha) { decodes++; poems.document }

        RestoreMatcher.match(listOf(poems), listOf(counting("Poems", poems.manifestEntry.documentSha256)))
        RestoreMatcher.match(listOf(poems), listOf(counting("Other", "e".repeat(64))))
        assertEquals(0, decodes)

        val olderBytes = counting("Poems", "e".repeat(64))
        RestoreMatcher.match(listOf(poems, poems.copyAs("b")), listOf(olderBytes))
        assertEquals(1, decodes)
    }

    private fun assertCounts(added: Int, here: Int, match: RestoreMatch) {
        assertEquals(added to here, match.added.size to match.alreadyHereCount)
    }

    private fun backup(id: String, title: String, text: String = "hello"): StagedZineProject {
        val document = document(text)
        return StagedZineProject(
            manifestEntry = ZineBackupProjectEntry(
                sourceProjectId = id,
                title = title,
                format = document.format,
                paperSize = document.paperSize,
                createdAtEpochMs = 1L,
                updatedAtEpochMs = 2L,
                documentSchemaVersion = document.schemaVersion,
                documentPath = "projects/$id/document.json",
                documentSha256 = text.hashCode().toString(16).padStart(64, '0'),
                documentByteCount = 1L,
                assetHashes = emptyList(),
                coverSurface = null,
                coverStamp = null,
            ),
            document = document,
            documentPath = Path.of("unused"),
        )
    }

    private fun StagedZineProject.copyAs(id: String) =
        copy(manifestEntry = manifestEntry.copy(sourceProjectId = id, documentPath = "projects/$id/document.json"))

    private fun shelfCopyOf(zine: StagedZineProject) =
        shelf(zine.manifestEntry.title, zine.document, zine.manifestEntry.documentSha256)

    private fun shelf(title: String, document: ZineDocument, sha: String = "d".repeat(64)) =
        ShelfZine(title, sha) { document }

    private fun document(text: String): ZineDocument {
        val pages = (0 until 8).map { index ->
            val role = when (index) {
                0 -> PageRole.FRONT_COVER
                7 -> PageRole.BACK_COVER
                else -> PageRole.INTERIOR
            }
            val elements = if (index == 1) {
                listOf(TextElement(id = "t", transform = Transform(0.0, 0.0, 10.0, 10.0), text = text))
            } else {
                emptyList()
            }
            Page(index = index, role = role, elements = elements)
        }
        return ZineDocument(format = ZineFormat.SINGLE_SHEET_8, paperSize = PaperSize.LETTER, pages = pages)
    }
}
