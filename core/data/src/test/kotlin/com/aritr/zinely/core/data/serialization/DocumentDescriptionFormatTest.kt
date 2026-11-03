package com.aritr.zinely.core.data.serialization

import com.aritr.zinely.core.model.ColorRgba
import com.aritr.zinely.core.model.DecorElement
import com.aritr.zinely.core.model.Element
import com.aritr.zinely.core.model.ImageElement
import com.aritr.zinely.core.model.Page
import com.aritr.zinely.core.model.PageRole
import com.aritr.zinely.core.model.PaperSize
import com.aritr.zinely.core.model.TextElement
import com.aritr.zinely.core.model.Transform
import com.aritr.zinely.core.model.ZineDocument
import com.aritr.zinely.core.model.ZineFormat
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import net.jqwik.api.Arbitraries
import net.jqwik.api.Arbitrary
import net.jqwik.api.ForAll
import net.jqwik.api.Property
import net.jqwik.api.Provide
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/**
 * ADR-147: a description rides saved format v4, and a file says v4 **only** when it holds one. An
 * undescribed zine is written byte for byte as a v3 build writes it, so only a described piece needs the
 * newer version (and only a backup holding one is refused by older builds).
 */
class DocumentDescriptionFormatTest {

    private val serializer = JsonDocumentSerializer()

    private fun photo(id: String = "p", description: String? = null) = ImageElement(
        id = id,
        transform = Transform(0.0, 0.0, 100.0, 100.0),
        assetId = "a".repeat(64),
        description = description,
    )

    private fun art(id: String = "d", description: String? = null) = DecorElement(
        id = id,
        transform = Transform(0.0, 0.0, 20.0, 20.0),
        supplyId = "shape.star",
        ink = ColorRgba.BLACK,
        description = description,
    )

    private fun zine(vararg elements: Element) = ZineDocument(
        format = ZineFormat.SINGLE_SHEET_8,
        paperSize = PaperSize.A4,
        pages = listOf(Page(0, PageRole.INTERIOR, elements = elements.toList())),
    )

    private fun stamp(doc: ZineDocument): Int =
        Json.parseToJsonElement(serializer.serialize(doc)).let { (it as JsonObject)["schemaVersion"].toString().toInt() }

    @Test
    fun `a described photo and a described piece of Art round-trip equal`() {
        val doc = zine(photo(description = "Mum at the gate"), art(description = "this way to the flower end"))
        assertEquals(doc.copy(schemaVersion = 4), serializer.deserialize(serializer.serialize(doc)))
    }

    @Test
    fun `no description is written as an absent key, never as null`() {
        val text = serializer.serialize(zine(photo(), art()))
        assertFalse(text.contains("description"), text)
        assertFalse(text.contains("null"), text)
    }

    @Test
    fun `stamp is 3 with no description and 4 with any`() {
        assertEquals(3, stamp(zine(photo(), art())))
        assertEquals(3, stamp(zine()))
        assertEquals(4, stamp(zine(photo(description = "x"))))
        assertEquals(4, stamp(zine(art(description = "x"))))
    }

    @Test
    fun `a described piece wholly off its page still stamps 4`() {
        val off = photo(description = "x").copy(transform = Transform(-900.0, -900.0, 10.0, 10.0))
        assertEquals(4, stamp(zine(off)))
    }

    @Test
    fun `clearing the last description stamps 3 again`() {
        val described = zine(photo(description = "x"))
        val cleared = described.copy(
            pages = described.pages.map { p -> p.copy(elements = p.elements.map { (it as ImageElement).copy(description = null) }) },
        )
        assertEquals(4, stamp(described))
        assertEquals(3, stamp(cleared))
    }

    @Test
    fun `the stamp ignores the schemaVersion the document carries`() {
        assertEquals(3, stamp(zine(photo()).copy(schemaVersion = 4)))
        assertEquals(4, stamp(zine(photo(description = "x")).copy(schemaVersion = 1)))
    }

    @Test
    fun `decoding the frozen v3 fixture and writing it again gives its bytes exactly`() {
        val bytes = javaClass.getResourceAsStream("/fixtures/document-v3.json")!!.use { it.readBytes() }.decodeToString()
        assertEquals(bytes, serializer.serialize(serializer.deserialize(bytes)))
    }

    @Test
    fun `a v3 file decodes with a null description through the identity step`() {
        val bytes = javaClass.getResourceAsStream("/fixtures/document-v3.json")!!.use { it.readBytes() }.decodeToString()
        val doc = serializer.deserialize(bytes)
        val described = doc.pages.flatMap { it.elements }.filter { it is ImageElement || it is DecorElement }
        assertTrue(described.isNotEmpty())
        assertTrue(described.all { (it as? ImageElement)?.description == null && (it as? DecorElement)?.description == null })
    }

    @Test
    fun `every reader of the version reads up to 4 and refuses 5`() {
        // The readers all key off CURRENT_SCHEMA_VERSION (migrations, document validator, library and package
        // manifest validators, the stager, the shelf), so pinning the literal pins the six together.
        assertEquals(4, com.aritr.zinely.core.model.CURRENT_SCHEMA_VERSION)
        val validator = com.aritr.zinely.core.data.validation.DefaultDocumentValidator()
        val valid = serializer.deserialize(
            javaClass.getResourceAsStream("/fixtures/document-v4.json")!!.use { it.readBytes() }.decodeToString(),
        )
        assertTrue(validator.validate(valid.copy(schemaVersion = 4)).isValid)
        assertFalse(validator.validate(valid.copy(schemaVersion = 5)).isValid)
    }

    @Test
    fun `a v5 file is refused and a v4 file is accepted`() {
        val v4 = serializer.serialize(zine(photo(description = "x")))
        serializer.deserialize(v4)
        assertThrows<NewerSchemaVersionException> {
            serializer.deserialize(v4.replace("\"schemaVersion\":4", "\"schemaVersion\":5"))
        }
    }

    @Property
    fun `an undescribed document writes only keys the v3 fixture has`(@ForAll("undescribed") doc: ZineDocument) {
        val allowed = keyPaths(Json.parseToJsonElement(
            javaClass.getResourceAsStream("/fixtures/document-v3.json")!!.use { it.readBytes() }.decodeToString(),
        ))
        val written = keyPaths(Json.parseToJsonElement(serializer.serialize(doc)))
        assertTrue(allowed.containsAll(written), "keys outside v3: ${written - allowed}")
    }

    @Provide
    fun undescribed(): Arbitrary<ZineDocument> {
        val ids = Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(8)
        val element: Arbitrary<Element> = Arbitraries.oneOf(
            ids.map { photo(it) },
            ids.map { art(it) },
            ids.map {
                TextElement(id = it, transform = Transform(1.0, 2.0, 30.0, 40.0), text = "words")
            },
        )
        return element.list().ofMaxSize(5).map { zine(*it.toTypedArray()) }
    }

    /** Every key path with list indices dropped, so `pages[].elements[].type` is one path whatever the count. */
    private fun keyPaths(e: JsonElement, prefix: String = ""): Set<String> = when (e) {
        is JsonObject -> e.entries.flatMap { (k, v) -> setOf("$prefix/$k") + keyPaths(v, "$prefix/$k") }.toSet()
        is JsonArray -> e.flatMap { keyPaths(it, prefix) }.toSet()
        else -> emptySet()
    }
}
