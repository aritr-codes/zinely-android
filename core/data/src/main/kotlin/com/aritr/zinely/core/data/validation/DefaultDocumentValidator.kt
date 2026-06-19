package com.aritr.zinely.core.data.validation

import com.aritr.zinely.core.model.Background
import com.aritr.zinely.core.model.ColorRgba
import com.aritr.zinely.core.model.Crop
import com.aritr.zinely.core.model.CURRENT_SCHEMA_VERSION
import com.aritr.zinely.core.model.Element
import com.aritr.zinely.core.model.ImageElement
import com.aritr.zinely.core.model.TextElement
import com.aritr.zinely.core.model.TextStyle
import com.aritr.zinely.core.model.Transform
import com.aritr.zinely.core.model.ZineDocument

/**
 * Default [DocumentValidator] for the MVP single-sheet format. Emits coded ERROR/WARNING issues in
 * stable document order. Errors cover counts/ids/geometry/colour invariants; warnings flag soft
 * problems (e.g. empty text) that should inform but not block.
 */
public class DefaultDocumentValidator : DocumentValidator {

    override fun validate(document: ZineDocument): ValidationResult {
        val issues = mutableListOf<ValidationIssue>()

        if (document.schemaVersion < 1 || document.schemaVersion > CURRENT_SCHEMA_VERSION) {
            issues += error(
                "schema.version.unsupported",
                "schemaVersion ${document.schemaVersion} is outside the supported range 1..$CURRENT_SCHEMA_VERSION",
                "schemaVersion",
            )
        }

        val expectedPages = document.format.pageCount
        if (document.pages.size != expectedPages) {
            issues += error(
                "pages.count.mismatch",
                "expected $expectedPages pages for ${document.format}, found ${document.pages.size}",
                "pages",
            )
        }

        val seenIndices = HashSet<Int>()
        val seenElementIds = HashSet<String>()
        document.pages.forEachIndexed { pageIndex, page ->
            val pagePath = "pages[$pageIndex]"
            if (page.index < 0 || page.index >= expectedPages) {
                issues += error(
                    "pages.index.outOfRange",
                    "page index ${page.index} is outside 0..${expectedPages - 1}",
                    "$pagePath.index",
                )
            }
            if (!seenIndices.add(page.index)) {
                issues += error("pages.index.duplicate", "duplicate page index ${page.index}", "$pagePath.index")
            }
            validateBackground(page.background, "$pagePath.background", issues)
            page.elements.forEachIndexed { elementIndex, element ->
                validateElement(element, "$pagePath.elements[$elementIndex]", seenElementIds, issues)
            }
        }

        validateBackground(document.defaults.background, "defaults.background", issues)
        validateTextStyle(document.defaults.textStyle, "defaults.textStyle", issues)

        return ValidationResult(issues)
    }

    private fun validateElement(
        element: Element,
        path: String,
        seenElementIds: HashSet<String>,
        issues: MutableList<ValidationIssue>,
    ) {
        if (element.id.isBlank()) {
            issues += error("element.id.blank", "element id is blank", "$path.id")
        } else if (!seenElementIds.add(element.id)) {
            issues += error("element.id.duplicate", "duplicate element id '${element.id}'", "$path.id")
        }
        validateTransform(element.transform, "$path.transform", issues)
        when (element) {
            is ImageElement -> {
                if (!isSha256(element.assetId)) {
                    issues += error(
                        "image.assetId.invalid",
                        "assetId '${element.assetId}' is not a sha256 content hash",
                        "$path.assetId",
                    )
                }
                validateCrop(element.crop, "$path.crop", issues)
            }
            is TextElement -> {
                if (element.text.isEmpty()) {
                    issues += warning("text.empty", "text element has empty text", "$path.text")
                }
                validateTextStyle(element.style, "$path.style", issues)
            }
        }
    }

    private fun validateTransform(transform: Transform, path: String, issues: MutableList<ValidationIssue>) {
        val finite = transform.xPt.isFinite() && transform.yPt.isFinite() &&
            transform.widthPt.isFinite() && transform.heightPt.isFinite() &&
            transform.rotationDegrees.isFinite()
        if (!finite) {
            issues += error("transform.value.nonFinite", "transform has a non-finite (NaN/Inf) value", path)
        }
        if (transform.widthPt <= 0.0 || transform.heightPt <= 0.0) {
            issues += error("transform.size.nonPositive", "transform width and height must be positive", path)
        }
    }

    private fun validateCrop(crop: Crop, path: String, issues: MutableList<ValidationIssue>) {
        val inUnitRange = listOf(crop.left, crop.top, crop.right, crop.bottom).all { it in 0.0..1.0 }
        if (!inUnitRange || crop.left >= crop.right || crop.top >= crop.bottom) {
            issues += error(
                "image.crop.invalid",
                "crop must satisfy 0 <= left < right <= 1 and 0 <= top < bottom <= 1",
                path,
            )
        }
    }

    private fun validateTextStyle(style: TextStyle, path: String, issues: MutableList<ValidationIssue>) {
        if (style.sizePt <= 0.0 || !style.sizePt.isFinite()) {
            issues += error("text.style.size.nonPositive", "font size must be a positive number of points", "$path.sizePt")
        }
        validateColor(style.color, "$path.color", issues)
    }

    private fun validateBackground(background: Background, path: String, issues: MutableList<ValidationIssue>) {
        if (background is Background.Solid) validateColor(background.color, "$path.color", issues)
    }

    private fun validateColor(color: ColorRgba, path: String, issues: MutableList<ValidationIssue>) {
        val inRange = listOf(color.r, color.g, color.b, color.a).all { it in 0..255 }
        if (!inRange) {
            issues += error("color.channel.outOfRange", "colour channels must be in 0..255", path)
        }
    }

    private fun error(code: String, message: String, path: String?): ValidationIssue =
        ValidationIssue(code, message, Severity.ERROR, path)

    private fun warning(code: String, message: String, path: String?): ValidationIssue =
        ValidationIssue(code, message, Severity.WARNING, path)

    private companion object {
        private val SHA256 = Regex("^[0-9a-f]{64}$")
        fun isSha256(value: String): Boolean = SHA256.matches(value)
    }
}
