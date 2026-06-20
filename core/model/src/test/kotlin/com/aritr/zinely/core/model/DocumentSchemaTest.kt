package com.aritr.zinely.core.model

import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * The zine document is a `@Serializable` sealed tree (S2 spike §3). These tests pin the wire
 * contract: a document round-trips losslessly, the element discriminator is the stable string
 * `type`, enum wire names are explicit, and `schemaVersion` is present so migration (§6) can peek it.
 */
class DocumentSchemaTest {

    private val json = Json {
        classDiscriminator = "type"
        encodeDefaults = true
        prettyPrint = false
    }

    private fun sampleDocument(): ZineDocument = ZineDocument(
        format = ZineFormat.SINGLE_SHEET_8,
        paperSize = PaperSize.A4,
        defaults = DocumentDefaults(
            textStyle = TextStyle(fontFamily = "serif", sizePt = 14.0, align = TextAlign.CENTER),
            background = Background.Solid(ColorRgba.WHITE),
        ),
        pages = listOf(
            Page(
                index = 0,
                role = PageRole.FRONT_COVER,
                background = Background.Solid(ColorRgba(10, 20, 30)),
                elements = listOf(
                    ImageElement(
                        id = "img-1",
                        transform = Transform(xPt = 10.0, yPt = 20.0, widthPt = 100.0, heightPt = 80.0, rotationDegrees = 90.0),
                        zIndex = 0,
                        assetId = "a".repeat(64),
                        crop = Crop(0.1, 0.1, 0.9, 0.9),
                        fit = Fit.FILL,
                    ),
                    TextElement(
                        id = "txt-1",
                        transform = Transform(xPt = 5.0, yPt = 6.0, widthPt = 50.0, heightPt = 12.0),
                        zIndex = 1,
                        text = "hello",
                        style = TextStyle(sizePt = 18.0, color = ColorRgba(1, 2, 3, 200), bold = true),
                    ),
                ),
            ),
            Page(index = 7, role = PageRole.BACK_COVER),
        ),
    )

    @Test
    fun `document round-trips through json unchanged`() {
        val doc = sampleDocument()
        val encoded = json.encodeToString(ZineDocument.serializer(), doc)
        val decoded = json.decodeFromString(ZineDocument.serializer(), encoded)
        assertEquals(doc, decoded)
    }

    @Test
    fun `schemaVersion defaults to the current schema version and is emitted`() {
        val doc = sampleDocument()
        assertEquals(CURRENT_SCHEMA_VERSION, doc.schemaVersion)
        val encoded = json.encodeToString(ZineDocument.serializer(), doc)
        assertTrue(encoded.contains("\"schemaVersion\":$CURRENT_SCHEMA_VERSION"), encoded)
    }

    @Test
    fun `element discriminator is the stable string type with image and text values`() {
        val doc = sampleDocument()
        val encoded = json.encodeToString(ZineDocument.serializer(), doc)
        assertTrue(encoded.contains("\"type\":\"image\""), encoded)
        assertTrue(encoded.contains("\"type\":\"text\""), encoded)
    }

    @Test
    fun `enum wire names are explicit and stable`() {
        val doc = sampleDocument()
        val encoded = json.encodeToString(ZineDocument.serializer(), doc)
        assertTrue(encoded.contains("\"single_sheet_8\""), encoded)
        assertTrue(encoded.contains("\"a4\""), encoded)
        assertTrue(encoded.contains("\"front_cover\""), encoded)
        assertTrue(encoded.contains("\"back_cover\""), encoded)
    }

    @Test
    fun `background is a sealed tree with none and solid variants`() {
        val none = Json.encodeToString(Background.serializer(), Background.None)
        val solid = Json.encodeToString(Background.serializer(), Background.Solid(ColorRgba.BLACK))
        assertTrue(none.contains("\"none\""), none)
        assertTrue(solid.contains("\"solid\""), solid)
    }
}
