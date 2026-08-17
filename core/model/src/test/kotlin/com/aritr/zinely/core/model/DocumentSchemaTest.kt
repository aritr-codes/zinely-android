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
                    // The third primitive (ADR-105 / SUPPLIES-SPEC §2), pinned into the same hand-picked
                    // wire-contract sample as image and text — every non-default field set, so a dropped
                    // or renamed field fails the round-trip rather than silently defaulting back.
                    DecorElement(
                        id = "dec-1",
                        transform = Transform(xPt = 30.0, yPt = 40.0, widthPt = 60.0, heightPt = 20.0, rotationDegrees = 12.0),
                        zIndex = 2,
                        supplyId = "tape.torn",
                        ink = ColorRgba(200, 40, 90, 255),
                        mirrored = true,
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
        assertTrue(encoded.contains("\"type\":\"decor\""), encoded)
    }

    /**
     * Given a page carrying a supply, when the document is written and read back, then every decor
     * field survives byte-for-byte and the wire keys are the frozen names.
     *
     * The keys are asserted literally, not just structurally: `supplyId`, `ink` and `mirrored` are the
     * on-disk contract for every zine anyone saves from here on, and a rename is a silent data loss for
     * documents already written (SUPPLIES-SPEC §2).
     */
    @Test
    fun `a decor element round-trips with its supplyId, ink and mirror flag intact`() {
        val doc = sampleDocument()
        val encoded = json.encodeToString(ZineDocument.serializer(), doc)
        assertTrue(encoded.contains("\"supplyId\":\"tape.torn\""), encoded)
        assertTrue(encoded.contains("\"mirrored\":true"), encoded)

        val decoded = json.decodeFromString(ZineDocument.serializer(), encoded)
        val decor = decoded.pages[0].elements.filterIsInstance<DecorElement>().single()
        assertEquals("tape.torn", decor.supplyId)
        assertEquals(ColorRgba(200, 40, 90, 255), decor.ink)
        assertEquals(true, decor.mirrored)
        assertEquals(2, decor.zIndex)
        assertEquals(12.0, decor.transform.rotationDegrees)
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
