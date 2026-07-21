package com.aritr.zinely.render.android

/**
 * One bundled family's four static faces, as asset paths under the render module's `assets/`.
 *
 * Four real TTFs, never synthesised bold/italic and never a variable font whose weight axis needs
 * API 26+ (`minSdk` is 24, [ADR-024](../../../../../../../../docs/DECISIONS.md#adr-024)). [name] is the
 * **wire name**: the exact string a document's `TextStyle.fontFamily` carries on disk, so a family
 * rename is a document-migration concern, not a rendering one.
 */
public data class DocumentFontFamily(
    val name: String,
    val regularAsset: String,
    val boldAsset: String,
    val italicAsset: String,
    val boldItalicAsset: String,
)

/**
 * **The single source of truth for document typography (F3).**
 *
 * Every surface that draws document text — editor preview, raster export, PDF export, thumbnails —
 * reaches its typeface through this registry, because all of them share one `CanvasReplayer` and one
 * [SharedTextLayout], which resolve through a [FontResolver] built over it. A family therefore renders
 * with the same glyphs and metrics everywhere by construction rather than by discipline (ADR-006,
 * ADR-028 §4.2).
 *
 * **What changed, and why it matters.** Before F3 the resolver ignored `fontFamily` entirely: every
 * family — known, unknown, or nonsense — resolved to Inter. A document authored in one family rendered
 * in another with nothing recording the substitution, so "the document says serif" and "the page shows
 * Inter" could both be true at once. The registry makes family resolution *explicit*: a requested family
 * either matches a registered one or is deliberately substituted by [defaultFamilyName], and
 * [isRegistered] lets a caller tell those apart instead of guessing.
 *
 * **Adding a family is data, not code**: bundle its four TTFs and add a [DocumentFontFamily] row. That
 * is the property F3 exists to establish — *which* families ship is a curation decision owned by the
 * designer's font/preset freeze, not by this file, which is why [Bundled] registers only the family the
 * render module actually carries today.
 *
 * **Document typography is deliberately separate from UI typography.** This registry governs what is
 * drawn *inside a zine*. Application chrome (the shelf, the editor's own controls) loads its fonts
 * independently through Compose resources in `:feature:editor`, and is out of scope here — see
 * [ARCHITECTURE.md](../../../../../../../../docs/ARCHITECTURE.md). The two are not merged because they
 * answer different questions: chrome typography is a product-design choice about the app, document
 * typography is user content that must survive export to paper.
 *
 * @param families the registered families, in declaration order.
 * @param defaultFamilyName the family an unregistered request resolves to; must be registered.
 */
public class DocumentFontRegistry(
    families: List<DocumentFontFamily>,
    public val defaultFamilyName: String,
) {
    private val byName: Map<String, DocumentFontFamily> = families.associateBy { it.name.canonical() }

    init {
        require(families.isNotEmpty()) { "a font registry needs at least one family" }
        require(byName.size == families.size) {
            "duplicate family names: ${families.map { it.name }}"
        }
        require(defaultFamilyName.canonical() in byName) {
            "default family '$defaultFamilyName' is not registered (have: ${families.map { it.name }})"
        }
    }

    /** Registered families in declaration order — the document fonts available to a document. */
    public val families: List<DocumentFontFamily> = families.toList()

    /** The family an unregistered request falls back to. */
    public val defaultFamily: DocumentFontFamily = byName.getValue(defaultFamilyName.canonical())

    /**
     * Whether [fontFamily] names a registered family. A caller that needs to be *honest* about
     * substitution — telling the user their font is unavailable rather than quietly swapping it — asks
     * this first; [resolve] alone cannot distinguish "matched" from "fell back".
     */
    public fun isRegistered(fontFamily: String): Boolean = fontFamily.canonical() in byName

    /**
     * The family to draw [fontFamily] with. Never null: an unregistered family resolves to
     * [defaultFamily], so a document authored against a font this build does not carry still renders
     * readable text rather than failing or reaching a device font whose metrics vary per phone.
     *
     * Matching is case-insensitive and whitespace-trimmed so a hand-edited document or an older
     * capitalisation still lands on the intended family; it is deliberately *not* fuzzy beyond that,
     * because a near-miss silently resolving to the wrong family is the failure this registry removes.
     */
    public fun resolve(fontFamily: String): DocumentFontFamily =
        byName[fontFamily.canonical()] ?: defaultFamily

    public companion object {
        /** The wire name of the family the render module bundles today. */
        public const val INTER: String = "Inter"

        /**
         * The families this build actually carries in `assets/fonts/`.
         *
         * One family today. That is a statement about what is bundled, not about what the registry
         * supports — expanding the set is the designer's font/preset curation, gated on its freeze, and
         * lands here as additional rows plus their TTFs.
         */
        public val Bundled: DocumentFontRegistry = DocumentFontRegistry(
            families = listOf(
                DocumentFontFamily(
                    name = INTER,
                    regularAsset = "fonts/Inter-Regular.ttf",
                    boldAsset = "fonts/Inter-Bold.ttf",
                    italicAsset = "fonts/Inter-Italic.ttf",
                    boldItalicAsset = "fonts/Inter-BoldItalic.ttf",
                ),
            ),
            defaultFamilyName = INTER,
        )
    }
}

/** Case- and surrounding-whitespace-insensitive key for family lookup. */
private fun String.canonical(): String = trim().lowercase()
