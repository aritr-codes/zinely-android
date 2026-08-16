package com.aritr.zinely.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The schema version the current app **writes**. Bumped whenever the persisted shape changes; the
 * migration framework (`:core:data`) chains `vN → vN+1` migrators up to this value on open
 * (S2 spike §6, [docs/DECISIONS.md ADR-020]).
 *
 * **v2 (2026-08-16) — `DecorElement`.** Bumped deliberately, *against* SUPPLIES-SPEC §2.1, on the
 * authority of [D-029's 2026-08-16 ruling](../../../../../../../docs/design/V2-SPEC-DEFECTS.md#d-029-ruling-2026-08-16)
 * Q5. The spec is right that no **migrator** is needed (nothing structural changes for existing
 * documents) and wrong about the **risk**, and the asymmetry is the whole point: ADR-106's `copier` is a
 * defaulted *field*, so a build predating it drops it silently and the zine still opens plainer. A new
 * sealed *discriminator* is not survivable — `0.9.0-beta.1` fails to parse the whole document, and
 * `ignoreUnknownKeys` does not rescue an unknown polymorphic type. The bump routes that into
 * `NewerSchemaVersionException` ([ADR-021]), which is the difference between "this zine needs a newer
 * Zinely" and "this zine is broken". The v1→v2 step is therefore an **identity** migrator
 * (`DocumentMigrations` requires a contiguous chain; see `JsonDocumentSerializer.MIGRATORS`).
 */
public const val CURRENT_SCHEMA_VERSION: Int = 2

/**
 * The root of a zine document — a `@Serializable` tree persisted as JSON (S2 spike §3,
 * [docs/DECISIONS.md ADR-020]). This is the **single source of truth** for project content; Room
 * metadata is a derived cache.
 *
 * Invariants the schema encodes (not enforced here — see `DocumentValidator` in `:core:data`):
 * - all geometry is in **points** (1/72"), never pixels — the renderer maps points→pixels at draw;
 * - `schemaVersion` is the first field so a migrator can peek it without decoding the whole tree;
 * - every non-essential field has a default, so an older reader tolerates a newer document.
 */
@Serializable
public data class ZineDocument(
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
    val format: ZineFormat,
    val paperSize: PaperSize,
    val defaults: DocumentDefaults = DocumentDefaults(),
    val pages: List<Page> = emptyList(),
)

/** Document-wide defaults applied to new elements/pages unless overridden. */
@Serializable
public data class DocumentDefaults(
    val textStyle: TextStyle = TextStyle(),
    val background: Background = Background.None,
)

/** One logical booklet page. `index` is the reading order (0-based); `role` is its cover/interior part. */
@Serializable
public data class Page(
    val index: Int,
    val role: PageRole,
    val background: Background = Background.None,
    val elements: List<Element> = emptyList(),
)

/**
 * Placement of an element on a page, in **points**. `x/y` is the top-left of the element box,
 * `width/height` its size, `rotationDegrees` a clockwise rotation about the box centre.
 */
@Serializable
public data class Transform(
    val xPt: Double,
    val yPt: Double,
    val widthPt: Double,
    val heightPt: Double,
    val rotationDegrees: Double = 0.0,
)

/**
 * A placed element. Sealed so new kinds (shapes, V2) are additive; the JSON discriminator is the
 * stable string `type` (configured on the `Json` instance in `:core:data`).
 */
@Serializable
public sealed interface Element {
    public val id: String
    public val transform: Transform
    public val zIndex: Int
}

/** An image placed by content hash. The bytes live in the content-addressed asset store (ADR-022/023). */
@Serializable
@SerialName("image")
public data class ImageElement(
    override val id: String,
    override val transform: Transform,
    override val zIndex: Int = 0,
    /** `sha256` of the import-master bytes; resolves to `assets/<assetId>` (ADR-022). */
    val assetId: String,
    val crop: Crop = Crop.FULL,
    val fit: Fit = Fit.FIT,
    /**
     * The photocopier filter (X3b, [ADR-106](../../../../../../../docs/DECISIONS.md#adr-106)): when
     * `true` the photo prints 1-bit dithered. A **presentation** flag, not a new asset — the
     * content-addressed master bytes are never rewritten, so the filter is reversible by unsetting it
     * and the same `assetId` can be filtered on one page and not on another. Defaulted, so an older
     * document reads unfiltered and no schema bump or migrator is needed.
     */
    val copier: Boolean = false,
) : Element

/** A run of text with a style. */
@Serializable
@SerialName("text")
public data class TextElement(
    override val id: String,
    override val transform: Transform,
    override val zIndex: Int = 0,
    val text: String,
    val style: TextStyle = TextStyle(),
) : Element

/**
 * A placed supply — one authored mark from the Zinely cabinet, laid down in one colour
 * (SUPPLIES-SPEC §2, [ADR-105]). The **third and final** member of [Element] (§7).
 *
 * Carries an *identifier*, never geometry: the outline lives in `SupplyCatalog` (`:core:render`) as
 * code, so a document stays small, a supply cannot be hand-edited into something unauthored, and
 * redrawing a supply improves every zine that used it. Same intent-not-pixels seam that lets
 * [ImageElement] carry an `assetId` instead of bytes (ADR-022/027).
 *
 * Deliberately absent, per §2's rejection table: `opacity` (riso lays ink down or it doesn't),
 * `strokeWidth`/`strokeColor` (a mark is ink, not a stroked outline), `path` (that would put an
 * authoring tool in the document), `secondaryInk` (a pack feature, additive later).
 *
 * **Where the spec was silent, this follows [ImageElement]'s existing shape:** `zIndex` is defaulted
 * to `0` exactly as its two siblings are — SUPPLIES-SPEC §2's sketch omits the default, and matching
 * the siblings costs nothing and keeps the wire tolerant of a hand-written element.
 */
@Serializable
@SerialName("decor")
public data class DecorElement(
    override val id: String,
    override val transform: Transform,
    override val zIndex: Int = 0,
    /**
     * Stable catalogue key, `family.name` — e.g. `tape.torn`. Never a content hash: supplies are
     * authored, not imported. Shape is enforced by `DefaultDocumentValidator` (`^[a-z]+\.[a-z]+$`);
     * catalogue **membership** is deliberately *not* checked there — `:core:data` is Android-free and
     * `api(project(":core:model"))`-only, so it cannot see the catalogue (SUPPLIES-SPEC §2.2 ruling).
     */
    val supplyId: String,
    /**
     * The single colour the mark is laid down in. Supplies are outlines rather than images precisely
     * so this is possible; single-coverage, so tinting is exact. Not validated against any palette —
     * an off-palette colour renders correctly, and refusing to open the zine would be the worse harm.
     */
    val ink: ColorRgba,
    /**
     * Reflects the outline across its own vertical centre line (nine of the sixteen supplies are
     * asymmetric). This genuinely cannot ride on [Transform]: it has no scale term, and the validator
     * errors on non-positive width/height, so negative-scale is doubly unavailable.
     */
    val mirrored: Boolean = false,
) : Element

/**
 * A crop rectangle in **normalised** image coordinates (0..1), where `(left, top)` is the top-left
 * corner kept and `(right, bottom)` the bottom-right. `FULL` keeps the whole image.
 */
@Serializable
public data class Crop(
    val left: Double = 0.0,
    val top: Double = 0.0,
    val right: Double = 1.0,
    val bottom: Double = 1.0,
) {
    public companion object {
        public val FULL: Crop = Crop(0.0, 0.0, 1.0, 1.0)
    }
}

/** Text styling. Font size is in **points**; colour is RGBA. */
@Serializable
public data class TextStyle(
    val fontFamily: String = "sans-serif",
    val sizePt: Double = 12.0,
    val color: ColorRgba = ColorRgba.BLACK,
    val align: TextAlign = TextAlign.START,
    val bold: Boolean = false,
    val italic: Boolean = false,
)

/** A straight RGBA colour, 8 bits per channel (0..255). */
@Serializable
public data class ColorRgba(
    val r: Int,
    val g: Int,
    val b: Int,
    val a: Int = 255,
) {
    public companion object {
        public val BLACK: ColorRgba = ColorRgba(0, 0, 0, 255)
        public val WHITE: ColorRgba = ColorRgba(255, 255, 255, 255)
    }
}

/** A page or element background. Sealed; the JSON discriminator is `type`. */
@Serializable
public sealed interface Background {
    /** No fill — transparent / paper-white. */
    @Serializable
    @SerialName("none")
    public data object None : Background

    /** A solid colour fill. */
    @Serializable
    @SerialName("solid")
    public data class Solid(val color: ColorRgba) : Background
}
