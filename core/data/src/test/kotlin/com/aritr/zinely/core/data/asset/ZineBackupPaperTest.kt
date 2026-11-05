package com.aritr.zinely.core.data.asset

import com.aritr.zinely.core.model.CURRENT_SCHEMA_VERSION
import com.aritr.zinely.core.model.PaperSize
import com.aritr.zinely.core.model.ZineFormat
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** ADR-150 decisions 2 and 3: the manifest's paper (`paperSize` + `sheet`) and its paper colour. */
class ZineBackupPaperTest {

    private val writerJson = Json { encodeDefaults = true }
    private val readerJson = Json { ignoreUnknownKeys = true }
    private val validator = ZineLibraryBackupValidator()

    @Test
    fun `Legal and A5 are written as the family size older builds know, plus the sheet`() {
        assertEquals(PaperSize.LETTER to "legal", entryFor(PaperSize.LEGAL).let { it.paperSize to it.sheet })
        assertEquals(PaperSize.A4 to "a5", entryFor(PaperSize.A5).let { it.paperSize to it.sheet })
        assertEquals(PaperSize.LETTER to null, entryFor(PaperSize.LETTER).let { it.paperSize to it.sheet })
        assertEquals(PaperSize.A4 to null, entryFor(PaperSize.A4).let { it.paperSize to it.sheet })
    }

    @Test
    fun `the effective size is the sheet when set, else paperSize`() {
        PaperSize.entries.forEach { assertEquals(it, entryFor(it).effectivePaperSize(), "$it") }
    }

    @Test
    fun `every entry carries a sheet key, null for Letter and A4`() {
        val text = writerJson.encodeToString(ZineBackupProjectEntry.serializer(), entryFor(PaperSize.A4))
        assertTrue(text.contains("\"sheet\":null"), text)
        val legal = writerJson.encodeToString(ZineBackupProjectEntry.serializer(), entryFor(PaperSize.LEGAL))
        assertTrue(legal.contains("\"paperSize\":\"letter\"") && legal.contains("\"sheet\":\"legal\""), legal)
    }

    @Test
    fun `inconsistent pairs, a sheet of letter or a4, an unknown sheet and a bare legal paperSize are refused`() {
        val bad = listOf(
            entryFor(PaperSize.A4).copy(sheet = "legal"), // Legal's family is Letter
            entryFor(PaperSize.LETTER).copy(sheet = "a5"), // A5's family is A4
            entryFor(PaperSize.LETTER).copy(sheet = "letter"),
            entryFor(PaperSize.A4).copy(sheet = "a4"),
            entryFor(PaperSize.A4).copy(sheet = "tabloid"),
            entryFor(PaperSize.A4).copy(sheet = "LEGAL"), // the serial name, never the enum name
            entryFor(PaperSize.LETTER).copy(paperSize = PaperSize.LEGAL, sheet = null), // never written
        )
        bad.forEach { entry ->
            assertNull(entry.effectivePaperSize(), "$entry")
            val result = validator.validate(manifestOf(entry), entriesOf(entry))
            assertFalse(result.isValid, "$entry")
            assertTrue(result.issues.any { it.code == "project.sheet.invalid" }, result.issues.toString())
        }
        PaperSize.entries.map(::entryFor).forEach { entry ->
            assertTrue(validator.validate(manifestOf(entry), entriesOf(entry)).isValid, "$entry")
        }
    }

    @Test
    fun `paper colour round-trips, is absent as null, and is read leniently`() {
        val coloured = entryFor(PaperSize.A4).copy(paperColour = "pastel-blue")
        val text = writerJson.encodeToString(ZineBackupProjectEntry.serializer(), coloured)
        assertEquals(coloured, readerJson.decodeFromString(ZineBackupProjectEntry.serializer(), text))
        // A later build's unknown id is kept as a string (decision 3: interpreted only where drawn or named).
        val unknown = text.replace("\"pastel-blue\"", "\"kraft\"")
        assertEquals("kraft", readerJson.decodeFromString(ZineBackupProjectEntry.serializer(), unknown).paperColour)
        // A non-string is no colour, never a refusal.
        val number = text.replace("\"pastel-blue\"", "7")
        assertNull(readerJson.decodeFromString(ZineBackupProjectEntry.serializer(), number).paperColour)
        // An older-shaped entry without the keys is White, on its own paper.
        val older = text.replace(",\"sheet\":null", "").replace(",\"paperColour\":\"pastel-blue\"", "")
        val decoded = readerJson.decodeFromString(ZineBackupProjectEntry.serializer(), older)
        assertNull(decoded.paperColour)
        assertEquals(PaperSize.A4, decoded.effectivePaperSize())
    }

    private fun entryFor(paper: PaperSize) = ZineBackupProjectEntry(
        sourceProjectId = "p",
        title = "Pocket poems",
        format = ZineFormat.SINGLE_SHEET_8,
        paperSize = paper.backupPaperSize(),
        createdAtEpochMs = 1L,
        updatedAtEpochMs = 2L,
        documentSchemaVersion = CURRENT_SCHEMA_VERSION,
        documentPath = "projects/p/document.json",
        documentSha256 = "d".repeat(64),
        documentByteCount = 10L,
        assetHashes = emptyList(),
        coverSurface = null,
        coverStamp = null,
        sheet = paper.backupSheet(),
    )

    private fun manifestOf(entry: ZineBackupProjectEntry) = ZineLibraryBackupManifest(
        packageVersion = CURRENT_LIBRARY_BACKUP_VERSION,
        kind = LIBRARY_BACKUP_KIND,
        appVersion = "0.9.0",
        createdAtEpochMs = 1L,
        projects = listOf(entry),
    )

    private fun entriesOf(entry: ZineBackupProjectEntry) = listOf(
        ZineArchiveEntry(MANIFEST_PATH, 100L),
        ZineArchiveEntry(entry.documentPath, entry.documentByteCount),
    )
}
