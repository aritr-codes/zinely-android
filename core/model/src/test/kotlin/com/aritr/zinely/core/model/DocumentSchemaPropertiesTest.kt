package com.aritr.zinely.core.model

import kotlinx.serialization.json.Json
import net.jqwik.api.Arbitraries
import net.jqwik.api.Arbitrary
import net.jqwik.api.Combinators
import net.jqwik.api.ForAll
import net.jqwik.api.Property
import net.jqwik.api.Provide
import org.junit.jupiter.api.Assertions.assertEquals

/**
 * Property-based invariants for the document wire contract (jqwik): an arbitrary document survives
 * serialize→deserialize unchanged, and re-encoding is stable (S2 spike §9). This guards the schema
 * far beyond the hand-picked example in [DocumentSchemaTest].
 */
class DocumentSchemaPropertiesTest {

    private val json = Json {
        classDiscriminator = "type"
        encodeDefaults = true
    }

    private fun colors(): Arbitrary<ColorRgba> =
        Combinators.combine(
            Arbitraries.integers().between(0, 255),
            Arbitraries.integers().between(0, 255),
            Arbitraries.integers().between(0, 255),
            Arbitraries.integers().between(0, 255),
        ).`as` { r, g, b, a -> ColorRgba(r, g, b, a) }

    private fun transforms(): Arbitrary<Transform> =
        Combinators.combine(
            Arbitraries.doubles().between(-1000.0, 1000.0),
            Arbitraries.doubles().between(-1000.0, 1000.0),
            Arbitraries.doubles().between(0.0, 2000.0),
            Arbitraries.doubles().between(0.0, 2000.0),
            Arbitraries.doubles().between(0.0, 360.0),
        ).`as` { x, y, w, h, rot -> Transform(x, y, w, h, rot) }

    private fun backgrounds(): Arbitrary<Background> =
        Arbitraries.oneOf(
            Arbitraries.just(Background.None),
            colors().map { Background.Solid(it) },
        )

    private fun textStyles(): Arbitrary<TextStyle> =
        Combinators.combine(
            Arbitraries.of("sans-serif", "serif", "monospace"),
            Arbitraries.doubles().between(1.0, 200.0),
            colors(),
            Arbitraries.of(*TextAlign.entries.toTypedArray()),
            Arbitraries.of(true, false),
            Arbitraries.of(true, false),
        ).`as` { family, size, color, align, bold, italic ->
            TextStyle(family, size, color, align, bold, italic)
        }

    private fun elements(): Arbitrary<Element> {
        val ids = Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(12)
        val zIndices = Arbitraries.integers().between(0, 100)
        val image = Combinators.combine(
            ids, transforms(), zIndices,
            Arbitraries.strings().withChars("0123456789abcdef").ofLength(64),
            Arbitraries.of(*Fit.entries.toTypedArray()),
        ).`as` { id, t, z, asset, fit -> ImageElement(id, t, z, asset, Crop.FULL, fit) }
        val text = Combinators.combine(
            ids, transforms(), zIndices,
            Arbitraries.strings().ofMaxLength(40),
            textStyles(),
        ).`as` { id, t, z, s, style -> TextElement(id, t, z, s, style) }
        return Arbitraries.oneOf(image, text)
    }

    private fun pages(): Arbitrary<Page> =
        Combinators.combine(
            Arbitraries.integers().between(0, 7),
            Arbitraries.of(*PageRole.entries.toTypedArray()),
            backgrounds(),
            elements().list().ofMaxSize(5),
        ).`as` { index, role, bg, els -> Page(index, role, bg, els) }

    @Provide
    fun documents(): Arbitrary<ZineDocument> =
        Combinators.combine(
            Arbitraries.of(*ZineFormat.entries.toTypedArray()),
            Arbitraries.of(*PaperSize.entries.toTypedArray()),
            backgrounds().map { DocumentDefaults(background = it) },
            pages().list().ofMaxSize(8),
        ).`as` { format, paper, defaults, pgs -> ZineDocument(CURRENT_SCHEMA_VERSION, format, paper, defaults, pgs) }

    @Property(tries = 300)
    fun `any document round-trips through json unchanged`(@ForAll("documents") doc: ZineDocument) {
        val decoded = json.decodeFromString(ZineDocument.serializer(), json.encodeToString(ZineDocument.serializer(), doc))
        assertEquals(doc, decoded)
    }

    @Property(tries = 300)
    fun `re-encoding a document is stable`(@ForAll("documents") doc: ZineDocument) {
        val once = json.encodeToString(ZineDocument.serializer(), doc)
        val twice = json.encodeToString(
            ZineDocument.serializer(),
            json.decodeFromString(ZineDocument.serializer(), once),
        )
        assertEquals(once, twice)
    }
}
