package com.aritr.zinely.core.data.validation

import com.aritr.zinely.core.model.Background
import com.aritr.zinely.core.model.ColorRgba
import com.aritr.zinely.core.model.Crop
import com.aritr.zinely.core.model.CURRENT_SCHEMA_VERSION
import com.aritr.zinely.core.model.Element
import com.aritr.zinely.core.model.ImageElement
import com.aritr.zinely.core.model.Page
import com.aritr.zinely.core.model.PageRole
import com.aritr.zinely.core.model.PaperSize
import com.aritr.zinely.core.model.TextElement
import com.aritr.zinely.core.model.TextStyle
import com.aritr.zinely.core.model.Transform
import com.aritr.zinely.core.model.ZineDocument
import com.aritr.zinely.core.model.ZineFormat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * The document validator enforces the structural invariants the schema can't express in types
 * (page counts, unique ids, finite/positive geometry, normalised crops, in-range colours). It
 * returns a structured [ValidationResult] with ERROR/WARNING severities and stable, coded issues —
 * the concrete shape ADR-015 contemplates. Errors block; warnings inform.
 */
class DefaultDocumentValidatorTest {

    private val validator: DocumentValidator = DefaultDocumentValidator()

    private fun unitTransform() = Transform(xPt = 0.0, yPt = 0.0, widthPt = 10.0, heightPt = 10.0)

    private fun image(id: String) = ImageElement(id = id, transform = unitTransform(), assetId = "a".repeat(64))
    private fun text(id: String, body: String = "hi") =
        TextElement(id = id, transform = unitTransform(), text = body)

    /** A structurally sound 8-page single-sheet zine. */
    private fun validDocument(pageZeroElements: List<Element> = listOf(image("img-0"), text("txt-0"))): ZineDocument {
        val pages = (0 until 8).map { i ->
            val role = when (i) {
                0 -> PageRole.FRONT_COVER
                7 -> PageRole.BACK_COVER
                else -> PageRole.INTERIOR
            }
            Page(index = i, role = role, elements = if (i == 0) pageZeroElements else emptyList())
        }
        return ZineDocument(format = ZineFormat.SINGLE_SHEET_8, paperSize = PaperSize.LETTER, pages = pages)
    }

    private fun codes(doc: ZineDocument): List<String> = validator.validate(doc).issues.map { it.code }

    @Test
    fun `a structurally sound document is valid with no issues`() {
        val result = validator.validate(validDocument())
        assertTrue(result.isValid, result.issues.toString())
        assertTrue(result.issues.isEmpty(), result.issues.toString())
    }

    @Test
    fun `a wrong page count is an error`() {
        val doc = validDocument().let { it.copy(pages = it.pages.drop(1)) }
        val result = validator.validate(doc)
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.code == "pages.count.mismatch" }, result.issues.toString())
    }

    @Test
    fun `duplicate page indices are an error`() {
        val doc = validDocument().let { it.copy(pages = it.pages.mapIndexed { i, p -> if (i == 7) p.copy(index = 6) else p }) }
        assertTrue(codes(doc).contains("pages.index.duplicate"), codes(doc).toString())
    }

    @Test
    fun `a page index out of range is an error`() {
        val doc = validDocument().let { it.copy(pages = it.pages.mapIndexed { i, p -> if (i == 0) p.copy(index = 99) else p }) }
        assertTrue(codes(doc).contains("pages.index.outOfRange"), codes(doc).toString())
    }

    @Test
    fun `duplicate element ids across the document are an error`() {
        val doc = validDocument(listOf(image("dup"), text("dup")))
        assertTrue(codes(doc).contains("element.id.duplicate"), codes(doc).toString())
    }

    @Test
    fun `a blank element id is an error`() {
        val doc = validDocument(listOf(image("  ")))
        assertTrue(codes(doc).contains("element.id.blank"), codes(doc).toString())
    }

    @Test
    fun `a non-positive element size is an error`() {
        val doc = validDocument(listOf(image("img-0").copy(transform = unitTransform().copy(widthPt = 0.0))))
        assertTrue(codes(doc).contains("transform.size.nonPositive"), codes(doc).toString())
    }

    @Test
    fun `a non-finite transform value is an error`() {
        val doc = validDocument(listOf(image("img-0").copy(transform = unitTransform().copy(xPt = Double.NaN))))
        assertTrue(codes(doc).contains("transform.value.nonFinite"), codes(doc).toString())
    }

    @Test
    fun `an image asset id that is not a sha256 hash is an error`() {
        val doc = validDocument(listOf(ImageElement(id = "img-0", transform = unitTransform(), assetId = "not-a-hash")))
        assertTrue(codes(doc).contains("image.assetId.invalid"), codes(doc).toString())
    }

    @Test
    fun `an inverted crop rectangle is an error`() {
        val bad = image("img-0").copy(crop = Crop(left = 0.9, top = 0.0, right = 0.1, bottom = 1.0))
        val doc = validDocument(listOf(bad))
        assertTrue(codes(doc).contains("image.crop.invalid"), codes(doc).toString())
    }

    @Test
    fun `a non-positive text size is an error`() {
        val doc = validDocument(listOf(text("txt-0").copy(style = TextStyle(sizePt = 0.0))))
        assertTrue(codes(doc).contains("text.style.size.nonPositive"), codes(doc).toString())
    }

    @Test
    fun `a colour channel out of range is an error`() {
        val styled = text("txt-0").copy(style = TextStyle(color = ColorRgba(r = 999, g = 0, b = 0)))
        val doc = validDocument(listOf(styled))
        assertTrue(codes(doc).contains("color.channel.outOfRange"), codes(doc).toString())
    }

    @Test
    fun `an unsupported schema version is an error`() {
        val doc = validDocument().copy(schemaVersion = CURRENT_SCHEMA_VERSION + 1)
        assertTrue(codes(doc).contains("schema.version.unsupported"), codes(doc).toString())
    }

    @Test
    fun `empty text is a non-blocking warning`() {
        val doc = validDocument(listOf(text("txt-0", body = "")))
        val result = validator.validate(doc)
        assertTrue(result.isValid, "empty text must not block: ${result.issues}")
        assertTrue(result.warnings.any { it.code == "text.empty" }, result.issues.toString())
    }

    @Test
    fun `issues carry a path locating the offending node`() {
        val doc = validDocument(listOf(image("img-0").copy(transform = unitTransform().copy(heightPt = -1.0))))
        val issue = validator.validate(doc).errors.first { it.code == "transform.size.nonPositive" }
        assertEquals("pages[0].elements[0].transform", issue.path)
    }
}
