package com.aritr.zinely.core.data.serialization

import com.aritr.zinely.core.model.CURRENT_SCHEMA_VERSION
import com.aritr.zinely.core.model.ImageElement
import com.aritr.zinely.core.model.Page
import com.aritr.zinely.core.model.PageRole
import com.aritr.zinely.core.model.PaperSize
import com.aritr.zinely.core.model.Transform
import com.aritr.zinely.core.model.ZineDocument
import com.aritr.zinely.core.model.ZineFormat
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * ADR-150 decision 1: US Legal and A5 ride saved format v5, and a file says 5 **only** when its paper is one
 * of them. A Letter or A4 zine is stamped 3 or 4 exactly as ADR-147 says.
 */
class DocumentPaperSizeFormatTest {

    private val serializer = JsonDocumentSerializer()

    private fun zine(paper: PaperSize, description: String? = null) = ZineDocument(
        format = ZineFormat.SINGLE_SHEET_8,
        paperSize = paper,
        pages = listOf(
            Page(
                0,
                PageRole.INTERIOR,
                elements = listOf(
                    ImageElement(
                        id = "p",
                        transform = Transform(0.0, 0.0, 100.0, 100.0),
                        assetId = "a".repeat(64),
                        description = description,
                    ),
                ),
            ),
        ),
    )

    private fun stamp(doc: ZineDocument): Int =
        Json.parseToJsonElement(serializer.serialize(doc)).let { (it as JsonObject)["schemaVersion"].toString().toInt() }

    @Test
    fun `a Legal or A5 zine is stamped 5, with or without a description`() {
        for (paper in listOf(PaperSize.LEGAL, PaperSize.A5)) {
            assertEquals(5, stamp(zine(paper)), "$paper")
            assertEquals(5, stamp(zine(paper, description = "x")), "$paper described")
        }
    }

    @Test
    fun `a Letter or A4 zine is stamped 3, or 4 with a description, as before`() {
        for (paper in listOf(PaperSize.LETTER, PaperSize.A4)) {
            assertEquals(3, stamp(zine(paper)), "$paper")
            assertEquals(4, stamp(zine(paper, description = "x")), "$paper described")
        }
    }

    @Test
    fun `a Legal and an A5 zine round-trip on their own paper`() {
        for (paper in listOf(PaperSize.LEGAL, PaperSize.A5)) {
            val doc = zine(paper)
            val text = serializer.serialize(doc)
            assertEquals(true, text.contains("\"paperSize\":\"${if (paper == PaperSize.LEGAL) "legal" else "a5"}\""), text)
            assertEquals(doc.copy(schemaVersion = CURRENT_SCHEMA_VERSION), serializer.deserialize(text))
        }
    }

    @Test
    fun `the frozen v3 and v4 fixtures are still written byte for byte`() {
        for (version in 3..4) {
            val bytes = javaClass.getResourceAsStream("/fixtures/document-v$version.json")!!.use { it.readBytes() }.decodeToString()
            assertEquals(bytes, serializer.serialize(serializer.deserialize(bytes)), "v$version")
        }
    }
}
