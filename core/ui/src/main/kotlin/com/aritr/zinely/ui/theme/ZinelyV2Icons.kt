package com.aritr.zinely.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aritr.zinely.ui.theme.ZinelyV2IconShape.Circle
import com.aritr.zinely.ui.theme.ZinelyV2IconShape.Path
import com.aritr.zinely.ui.theme.ZinelyV2IconShape.Rect

/**
 * The **V2 icon set** — 36 marks drawn on a 24-unit grid, transcribed from the frozen trilogy.
 *
 * ### The one fact that shapes this whole API
 *
 * **An icon in V2 does not own its stroke weight. The control containing it does.**
 *
 * Not one icon in the frozen HTML carries its own `stroke-width` except the seven pieces of artwork;
 * every UI icon inherits it from the CSS on its container. Across the 21 containers that state a
 * weight it takes **eight** values — 1.0, 1.7, 1.8, 1.9, 2.0, 2.2, 2.4, 2.6 — and the rendered size
 * takes **eleven** — 11, 12, 13, 15, 16, 17, 18, 19, 20, 22 and 26px. The same [Tick] is drawn at 2.4
 * in the Proof’s READY band and at 2.6 in its checklist. Weight tracks the control, not the symbol.
 *
 * That rules out the obvious implementation. An Android `VectorDrawable`, and equally an `ImageVector`
 * declared as a constant, **bakes `strokeLineWidth` into the asset** — so shipping these as static
 * vectors would mean either freezing every icon at one arbitrary weight or emitting roughly 90 assets
 * for the observed weight/size pairs. An icon here is therefore **geometry plus, at most, the paint the
 * design actually states**, and [toImageVector] builds the vector for one call site.
 *
 * ### Not everything is a stroke, which the first draft of this file got wrong
 *
 * Most V2 icons are stroked in `currentColor` over `fill:none`. Three are not, and each was found only
 * by reading the container CSS rather than the markup:
 *
 * | Mark | Container | Paint |
 * |---|---|---|
 * | [Favourite] | `.rail .rl svg` (`v2-bench.html:211`) | **fill only** — ochre, no stroke of any kind |
 * | [Play] | `.animtoggle svg` (`v2-proof.html:220`) | **fill *and* stroke** at weight 1 |
 * | [Pause] | the same container | as [Play] |
 *
 * A model with one `filled` boolean cannot say "fill and stroke", and a model that requires a stroke
 * cannot say "fill only" — so [ZinelyV2IconPaint] has all three cases. Getting this wrong is invisible
 * in a diff and obvious on screen: a hollow star where a solid ochre one belongs.
 *
 * ### [Pause] exists only at run time
 *
 * `startAnim()` at `v2-proof.html:558` replaces `#animIcon`’s contents with two bars, and `stopAnim()`
 * at `:569` puts [Play] back. It is a real part of the frozen design that no static `<svg>` contains,
 * and it is transcribed here — a set built only from the static markup would be missing an icon Phase B
 * needs the moment the fold animation runs.
 *
 * ### Fidelity
 *
 * Path data is **verbatim** — the `d` attributes are copied out of the frozen files unaltered, and
 * `ZinelyV2IconsTest` re-extracts all 42 placements from the HTML at run time and compares **geometry
 * and paint together**, so the two cannot drift. `<circle>` and `<rect>` keep their own parameters
 * rather than being hand-converted to path data; that conversion happens in tested code
 * ([ZinelyV2IconShape.pathData]), not in a transcription.
 *
 * `currentColor` maps to Compose’s tint: paths are built opaque black and `Icon` recolours them, which
 * is the same contract Material’s own icons use.
 *
 * ### Names
 *
 * Seven names are the design’s own — `v2-bench.html:381` declares `var ICON={edit, font, size, colour,
 * reframe, replace, del}`, and those are used ([Edit], [Font], [TextSize], [Colour], [Reframe],
 * [Replace], [Delete]). The rest are taken from the call site’s `aria-label` or its class. Invented
 * names describe the **shape** rather than the role, because four marks are already reused across roles
 * and a role name would be wrong at the second call site.
 *
 * ### Two pairs that ought to be one icon each — see D-015
 *
 * The trilogy draws **chevron-right twice**, with different geometry, in the same file: [ChevronRight]
 * (`M9 5l7 7-7 7`, the fold navigator) and [ChevronRightBand] (`M9 6l6 6-6 6`, the READY band). Only the
 * first is the mirror of [ChevronLeft]. It draws **a check twice** as well: [Tick] (`M4 12l5 5 11-12`,
 * the Proof, four placements) and [Done] (`M20 6 9 17l-5-5`, the Bench). Both pairs are transcribed as
 * found — collapsing them would be a redesign, and choosing which survives is an owner call.
 */
public object ZinelyV2Icons {

    /** `library:149` */
    public val StampSun: ZinelyV2Icon = icon(
        "StampSun",
        Circle(12f, 12f, 4.2f),
        Path("M12 2.5v3M12 18.5v3M2.5 12h3M18.5 12h3M5.2 5.2l2.1 2.1M16.7 16.7l2.1 2.1M18.8 5.2l-2.1 2.1M7.3 16.7l-2.1 2.1", cap = StrokeCap.Round),
        frozenPaint = ZinelyV2IconPaint.Stroke(1.6f),
    )

    /** `library:150` */
    public val StampLetter: ZinelyV2Icon = icon(
        "StampLetter",
        Path("M3.5 6.5h17v11h-17z"),
        Path("M3.5 7l8.5 6 8.5-6"),
        frozenPaint = ZinelyV2IconPaint.Stroke(1.6f, join = StrokeJoin.Round),
    )

    /** `library:151` */
    public val StampWaves: ZinelyV2Icon = icon(
        "StampWaves",
        Path("M2.5 15c3-4 5.5-4 8.5 0s5.5 4 8.5 0"),
        Path("M2.5 9.5c3-4 5.5-4 8.5 0s5.5 4 8.5 0"),
        frozenPaint = ZinelyV2IconPaint.Stroke(1.6f, cap = StrokeCap.Round),
    )

    /** `library:152` */
    public val StampSprig: ZinelyV2Icon = icon(
        "StampSprig",
        Path("M12 21c0-6 0-9 0-9M12 12c-4 0-6-2-6-5 3 0 6 1 6 5zM12 12c4 0 6-1.5 6-4.5-3 0-6 .5-6 4.5z"),
        frozenPaint = ZinelyV2IconPaint.Stroke(1.6f, cap = StrokeCap.Round, join = StrokeJoin.Round),
    )

    /** `library:153` */
    public val StampStar: ZinelyV2Icon = icon(
        "StampStar",
        Path("M12 3l2.2 6.2H20l-4.8 3.7 1.9 6.1L12 15.3 6.9 19l1.9-6.1L4 9.2h5.8z"),
        frozenPaint = ZinelyV2IconPaint.Stroke(1.6f, cap = StrokeCap.Round, join = StrokeJoin.Round),
    )

    /** `library:154` */
    public val StampFace: ZinelyV2Icon = icon(
        "StampFace",
        Circle(12f, 12f, 8.5f),
        Path("M8.5 13.5c1 1.4 2 2 3.5 2s2.5-.6 3.5-2", cap = StrokeCap.Round),
        Circle(9.3f, 10f, 0.7f, paint = ZinelyV2IconPaint.Fill),
        Circle(14.7f, 10f, 0.7f, paint = ZinelyV2IconPaint.Fill),
        frozenPaint = ZinelyV2IconPaint.Stroke(1.6f),
    )

    /** `bench:330` */
    public val Shelf: ZinelyV2Icon = icon(
        "Shelf",
        Path("M3 7h18v13H3z"),
        Path("M3 7l3-4h12l3 4"),
    )

    /** `bench:338` */
    public val Grid: ZinelyV2Icon = icon(
        "Grid",
        Rect(3f, 3f, 7f, 8f, 1f),
        Rect(14f, 3f, 7f, 8f, 1f),
        Rect(3f, 14f, 7f, 7f, 1f),
        Rect(14f, 14f, 7f, 7f, 1f),
    )

    /** `bench:344` */
    public val Undo: ZinelyV2Icon = icon(
        "Undo",
        Path("M9 14 4 9l5-5"),
        Path("M4 9h11a5 5 0 0 1 0 10h-3"),
    )

    /**
     * `bench:466` — **added by [OD-21](../../../../../../../../docs/design/V2-SPEC-DEFECTS.md#d-047-ruling)**,
     * the amendment that gave the frozen `.bar` its fourth control. It is [Undo]'s geometry mirrored, which
     * is what the amended markup draws; it is transcribed here rather than produced by flipping [Undo] at the
     * call site, because this set's contract is that it *is* the frozen trilogy's marks — a mirror applied in
     * Compose would leave the catalogue one mark short of the spec it claims to transcribe, and
     * `ZinelyV2IconsTest` scrapes the frozen bodies precisely so that cannot happen quietly.
     *
     * The neighbouring `bench:NNN` references in this file are **stale** after the same amendment
     * (`+23`/`+24`); they belong to [D-046](../../../../../../../../docs/design/V2-SPEC-DEFECTS.md#d-046) and
     * are left for the package that owns this file, per
     * [OD-18](../../../../../../../../docs/design/V2-SPEC-DEFECTS.md#d-046-ruling).
     */
    public val Redo: ZinelyV2Icon = icon(
        "Redo",
        Path("M15 14 20 9l-5-5"),
        Path("M20 9H9a5 5 0 0 0 0 10h3"),
    )

    /** `bench:345` */
    public val Add: ZinelyV2Icon = icon(
        "Add",
        Path("M12 5v14M5 12h14"),
    )

    /** `bench:346` */
    public val Done: ZinelyV2Icon = icon(
        "Done",
        Path("M20 6 9 17l-5-5"),
    )

    /** `bench:382` */
    public val Edit: ZinelyV2Icon = icon(
        "Edit",
        Path("M12 20h9"),
        Path("M16.5 3.5a2.1 2.1 0 0 1 3 3L7 19l-4 1 1-4Z"),
    )

    /** `bench:383` */
    public val Font: ZinelyV2Icon = icon(
        "Font",
        Path("M4 20 10 4l6 16"),
        Path("M6.5 14h7"),
    )

    /** `bench:384` */
    public val TextSize: ZinelyV2Icon = icon(
        "TextSize",
        Path("M3 7V4h18v3"),
        Path("M12 4v16"),
        Path("M9 20h6"),
    )

    /** `bench:385` */
    public val Colour: ZinelyV2Icon = icon(
        "Colour",
        Circle(12f, 12f, 9f),
        Circle(9f, 9f, 1.4f),
        Circle(15f, 9f, 1.4f),
        Circle(16f, 14f, 1.4f),
    )

    /** `bench:386` */
    public val Reframe: ZinelyV2Icon = icon(
        "Reframe",
        Path("M4 8V4h4"),
        Path("M20 8V4h-4"),
        Path("M4 16v4h4"),
        Path("M20 16v4h-4"),
    )

    /** `bench:387` */
    public val Replace: ZinelyV2Icon = icon(
        "Replace",
        Path("M3 12a9 9 0 0 1 15-6.7L21 8"),
        Path("M21 3v5h-5"),
        Path("M21 12a9 9 0 0 1-15 6.7L3 16"),
        Path("M3 21v-5h5"),
    )

    /** `bench:388` */
    public val Delete: ZinelyV2Icon = icon(
        "Delete",
        Path("M4 7h16"),
        Path("M9 7V4h6v3"),
        Path("M6 7l1 13h10l1-13"),
    )

    /** `bench:461` */
    public val Shield: ZinelyV2Icon = icon(
        "Shield",
        Path("M12 3l7 3v6c0 4-3 7-7 9-4-2-7-5-7-9V6z"),
    )

    /** `bench:578` */
    public val Art: ZinelyV2Icon = icon(
        "Art",
        Path("M4 16l5-6 4 5 3-4 4 5"),
        Rect(3f, 4f, 18f, 16f, 2f),
    )

    /**
     * `v21-bench.html` A15/A16 — two overlapping paper scraps and one printed dot.
     *
     * The owner-approved amendment gives the Add chooser's `Art` row this collage mark so it no longer
     * repeats [Art], the older frame-and-horizon mark now used for `Photo` there. Paint remains a property
     * of the chooser's `.opt .ico` container, like the other Bench controls.
     */
    public val Collage: ZinelyV2Icon = icon(
        "Collage",
        Path("M4 7l8-3 3 9-8 3z"),
        Path("M11 12l8-2 1 8-8 2z"),
        Circle(17.5f, 6.5f, 2.5f),
    )

    /** `bench:598` */
    public val Search: ZinelyV2Icon = icon(
        "Search",
        Circle(11f, 11f, 7f),
        Path("M21 21l-4-4"),
    )

    /** `bench:599` */
    public val Favourite: ZinelyV2Icon = icon(
        "Favourite",
        Path("M12 3l2.5 6H21l-5 4 2 7-6-4-6 4 2-7-5-4h6.5z"),
        frozenPaint = ZinelyV2IconPaint.Fill,
    )

    /** `bench:602` */
    public val Globe: ZinelyV2Icon = icon(
        "Globe",
        Circle(12f, 12f, 9f),
        Path("M3 12h18M12 3a15 15 0 0 1 0 18M12 3a15 15 0 0 0 0 18"),
    )

    /** `bench:605` */
    public val ShieldCheck: ZinelyV2Icon = icon(
        "ShieldCheck",
        Path("M12 3l7 3v6c0 4-3 7-7 9-4-2-7-5-7-9V6z"),
        Path("M9 12l2 2 4-4"),
    )

    /** `proof:276 (also proof:374)` */
    public val ChevronLeft: ZinelyV2Icon = icon(
        "ChevronLeft",
        Path("M15 5l-7 7 7 7"),
    )

    /** `proof:280` */
    public val Booklet: ZinelyV2Icon = icon(
        "Booklet",
        Path("M4 7l8-3 8 3v10l-8 3-8-3z"),
        Path("M12 4v16"),
    )

    /** `proof:294 (also proof:311, proof:344, proof:485)` */
    public val Tick: ZinelyV2Icon = icon(
        "Tick",
        Path("M4 12l5 5 11-12"),
    )

    /** `proof:296` */
    public val ChevronRightBand: ZinelyV2Icon = icon(
        "ChevronRightBand",
        Path("M9 6l6 6-6 6"),
    )

    /** `proof:301` */
    public val Document: ZinelyV2Icon = icon(
        "Document",
        Path("M5 4h11l3 3v13H5z"),
        Path("M8 4v5h7"),
        Path("M8 20v-6h8v6"),
    )

    /** `proof:305` */
    public val Share: ZinelyV2Icon = icon(
        "Share",
        Path("M4 12v7a1 1 0 001 1h14a1 1 0 001-1v-7"),
        Path("M12 3v13"),
        Path("M8 7l4-4 4 4"),
    )

    /** `proof:328 (also proof:366)` */
    public val Close: ZinelyV2Icon = icon(
        "Close",
        Path("M6 6l12 12M18 6L6 18"),
    )

    /** `proof:348` */
    public val Bookmark: ZinelyV2Icon = icon(
        "Bookmark",
        Path("M6 3h12v18l-6-3-6 3z"),
    )

    /** `proof:376` */
    public val ChevronRight: ZinelyV2Icon = icon(
        "ChevronRight",
        Path("M9 5l7 7-7 7"),
    )

    /** `proof:379 (also proof:569)` */
    public val Play: ZinelyV2Icon = icon(
        "Play",
        Path("M7 4v16l13-8z"),
        frozenPaint = ZinelyV2IconPaint.FillAndStroke(1f),
    )

    /** `proof:422` */
    public val Leaf: ZinelyV2Icon = icon(
        "Leaf",
        Path("M12 21c7-1 9-7 9-14-7 0-13 2-14 9"),
        Path("M12 21C8 16 8 10 8 6"),
        frozenPaint = ZinelyV2IconPaint.Stroke(1.6f, cap = StrokeCap.Round, join = StrokeJoin.Round),
    )

    /** `proof:558` */
    public val Pause: ZinelyV2Icon = icon(
        "Pause",
        Rect(7f, 5f, 3.5f, 14f, 0f),
        Rect(13.5f, 5f, 3.5f, 14f, 0f),
        frozenPaint = ZinelyV2IconPaint.FillAndStroke(1f),
    )

    private fun icon(
        name: String,
        vararg shapes: ZinelyV2IconShape,
        frozenPaint: ZinelyV2IconPaint? = null,
    ): ZinelyV2Icon = ZinelyV2Icon(name, frozenPaint, shapes.toList())

    /** Every icon in the set, in declaration order — the tests walk this so none can be forgotten. */
    public val All: List<ZinelyV2Icon> = listOf(
        StampSun,
        StampLetter,
        StampWaves,
        StampSprig,
        StampStar,
        StampFace,
        Shelf,
        Grid,
        Undo,
        Redo,
        Add,
        Done,
        Edit,
        Font,
        TextSize,
        Colour,
        Reframe,
        Replace,
        Delete,
        Shield,
        Art,
        Collage,
        Search,
        Favourite,
        Globe,
        ShieldCheck,
        ChevronLeft,
        Booklet,
        Tick,
        ChevronRightBand,
        Document,
        Share,
        Close,
        Bookmark,
        ChevronRight,
        Play,
        Leaf,
        Pause,
    )
}

/**
 * How a mark is painted: stroked, filled, or both.
 *
 * All three cases occur in the frozen trilogy, which is the only reason the type has three
 * (see the table on [ZinelyV2Icons]). Widths are in **24-unit viewport units**, exactly as the frozen
 * CSS writes them, so `stroke-width:1.7` on a 20px control is `Stroke(1.7f)` at `size = 20.dp`.
 *
 * Cap and join default to SVG’s own initial values rather than the ones most V2 controls happen to
 * use. That is deliberate: 13 of the frozen containers set `stroke-linecap:round` and 9 set nothing at
 * all, so a default of `Round` would silently round the nine the design leaves square. An unspecified
 * cap here renders what an unspecified cap renders in a browser.
 */
@Immutable
public sealed class ZinelyV2IconPaint {

    /** The ordinary case: `fill:none` with a stroke, which is 33 of the 36 marks. */
    @Immutable
    public data class Stroke(
        val width: Float,
        val cap: StrokeCap = StrokeCap.Butt,
        val join: StrokeJoin = StrokeJoin.Miter,
    ) : ZinelyV2IconPaint()

    /** Filled, with no stroke at all — [ZinelyV2Icons.Favourite], and two pupils inside `StampFace`. */
    @Immutable
    public data object Fill : ZinelyV2IconPaint()

    /** Filled *and* stroked — the Proof’s play/pause toggle, and nothing else in the trilogy. */
    @Immutable
    public data class FillAndStroke(
        val width: Float,
        val cap: StrokeCap = StrokeCap.Butt,
        val join: StrokeJoin = StrokeJoin.Miter,
    ) : ZinelyV2IconPaint()

    internal val strokeWidth: Float?
        get() = when (this) {
            is Stroke -> width
            is FillAndStroke -> width
            Fill -> null
        }

    internal val strokeCap: StrokeCap
        get() = when (this) {
            is Stroke -> cap
            is FillAndStroke -> cap
            Fill -> StrokeCap.Butt
        }

    internal val strokeJoin: StrokeJoin
        get() = when (this) {
            is Stroke -> join
            is FillAndStroke -> join
            Fill -> StrokeJoin.Miter
        }

    internal val fills: Boolean get() = this !is Stroke
}

/** One drawn element of an icon, holding the frozen source’s own primitive rather than a rewrite of it. */
@Immutable
public sealed class ZinelyV2IconShape {

    /**
     * A paint for this element alone, overriding the icon’s.
     *
     * Used where the frozen markup styles one child differently from its siblings — `StampFace`’s two
     * pupils carry `fill="currentColor" stroke="none"` inside an otherwise stroked mark.
     */
    public abstract val paint: ZinelyV2IconPaint?

    /** Partial overrides, applied on top of whichever paint wins. */
    public abstract val cap: StrokeCap?
    public abstract val join: StrokeJoin?

    /** A `<path>`, carrying its `d` attribute **verbatim** from the frozen file. */
    @Immutable
    public data class Path(
        val data: String,
        override val paint: ZinelyV2IconPaint? = null,
        override val cap: StrokeCap? = null,
        override val join: StrokeJoin? = null,
    ) : ZinelyV2IconShape()

    /** A `<circle>`. Kept as parameters so nobody hand-writes an arc and gets it subtly wrong. */
    @Immutable
    public data class Circle(
        val cx: Float,
        val cy: Float,
        val r: Float,
        override val paint: ZinelyV2IconPaint? = null,
        override val cap: StrokeCap? = null,
        override val join: StrokeJoin? = null,
    ) : ZinelyV2IconShape()

    /** A `<rect>`, with `rx` for the rounded corners [ZinelyV2Icons.Grid] uses. */
    @Immutable
    public data class Rect(
        val x: Float,
        val y: Float,
        val width: Float,
        val height: Float,
        val rx: Float = 0f,
        override val paint: ZinelyV2IconPaint? = null,
        override val cap: StrokeCap? = null,
        override val join: StrokeJoin? = null,
    ) : ZinelyV2IconShape()

    /**
     * SVG path data for this shape — identity for [Path], and the specification’s own equivalent
     * construction for [Circle] and [Rect].
     *
     * Both conversions follow SVG 1.1 §9.4 (shape equivalence): a circle is two half-arcs, a rounded rect
     * is four sides joined by four quarter-arcs, swept clockwise. They are code rather than transcription
     * precisely because they are the part a human gets wrong, and tests assert the resulting bounds.
     */
    public fun pathData(): String = when (this) {
        is Path -> data
        is Circle -> "M${cx - r},$cy a$r,$r 0 1,0 ${r * 2},0 a$r,$r 0 1,0 ${-r * 2},0 Z"
        is Rect -> if (rx <= 0f) {
            "M$x,$y h$width v$height h${-width} Z"
        } else {
            val ix = width - rx * 2
            val iy = height - rx * 2
            "M${x + rx},$y h$ix a$rx,$rx 0 0,1 $rx,$rx v$iy a$rx,$rx 0 0,1 ${-rx},$rx " +
                "h${-ix} a$rx,$rx 0 0,1 ${-rx},${-rx} v${-iy} a$rx,$rx 0 0,1 $rx,${-rx} Z"
        }
    }
}

/**
 * One V2 icon: a name, the geometry, and — where the frozen design states one unambiguously — its paint.
 *
 * Deliberately **not** an `ImageVector`. See the note on [ZinelyV2Icons]: the frozen design makes stroke
 * weight a property of the call site, and an `ImageVector` cannot express that without being rebuilt.
 */
@Immutable
public class ZinelyV2Icon internal constructor(
    public val name: String,
    /**
     * The paint the frozen corpus gives this mark, where it gives one — otherwise null.
     *
     * This records the paint that belongs to **the mark**, which is narrower than every paint the design
     * states — and the distinction is the whole model, so it is worth being exact about:
     *
     * - The **seven artwork marks** carry `stroke-width="1.6"` in their own markup. The mark owns it.
     * - [ZinelyV2Icons.Favourite], [ZinelyV2Icons.Play] and [ZinelyV2Icons.Pause] are recorded because
     *   their containers state a **fill**, and fill is a *kind* rather than a weight. No call site could
     *   sensibly stroke the favourites star instead; getting it wrong draws the wrong object.
     * - The remaining **26 are null**, and this is where care is needed: for 24 of them the frozen CSS
     *   does state a complete paint, because they appear in exactly one container today. That is a fact
     *   about the current corpus, not about the mark. The design's structure puts weight on the
     *   *container*, and [ZinelyV2Icons.Tick] and [ZinelyV2Icons.ChevronLeft] already prove it by
     *   appearing under two weights each. Recording a weight here for the other 24 would encode a
     *   coincidence as a property, and would be wrong the first time Phase B reuses one.
     *
     * So for those 26 the **frozen CSS is the source**, read at the container the icon is mounted in —
     * not this field. A default, not a constraint: a call site may pass its own paint for any icon.
     */
    public val frozenPaint: ZinelyV2IconPaint?,
    internal val shapes: List<ZinelyV2IconShape>,
) {
    override fun toString(): String = "ZinelyV2Icon($name)"
}

/**
 * Build the `ImageVector` for this icon at one call site’s size and paint.
 *
 * @param size the container’s frozen CSS `width`, in dp — CSS px and dp agree here.
 * @param paint the container’s frozen paint. Optional only where [ZinelyV2Icon.frozenPaint] is set;
 *   omitting it elsewhere is a programming error rather than a defaulted guess, because the design has
 *   no default to guess — it states a different weight per container.
 */
public fun ZinelyV2Icon.toImageVector(
    size: Dp = ZinelyV2IconDefaults.Size,
    paint: ZinelyV2IconPaint? = null,
): ImageVector {
    val resolved = paint ?: requireNotNull(frozenPaint) {
        "$name takes its paint from the call site — the frozen CSS sets it on the container, and the " +
            "design states a different weight per container, so there is no default to fall back on. " +
            "Pass the frozen value."
    }
    val builder = ImageVector.Builder(
        name = name,
        defaultWidth = size,
        defaultHeight = size,
        viewportWidth = ZinelyV2IconDefaults.Viewport,
        viewportHeight = ZinelyV2IconDefaults.Viewport,
    )
    shapes.forEach { shape ->
        val p = shape.paint ?: resolved
        builder.addPath(
            pathData = PathParser().parsePathString(shape.pathData()).toNodes(),
            // Opaque black is a placeholder that Icon()'s tint replaces, which is how `currentColor`
            // behaves and how Material's own icons are built.
            fill = if (p.fills) SolidColor(Color.Black) else null,
            stroke = if (p.strokeWidth != null) SolidColor(Color.Black) else null,
            strokeLineWidth = p.strokeWidth ?: 0f,
            strokeLineCap = shape.cap ?: p.strokeCap,
            strokeLineJoin = shape.join ?: p.strokeJoin,
        )
    }
    return builder.build()
}

/** Remembered [toImageVector] — building one allocates, and icons redraw on every recomposition. */
@Composable
public fun rememberZinelyV2Icon(
    icon: ZinelyV2Icon,
    size: Dp = ZinelyV2IconDefaults.Size,
    paint: ZinelyV2IconPaint? = null,
): ImageVector = remember(icon, size, paint) { icon.toImageVector(size, paint) }

/** The two constants the icon set genuinely has, as opposed to the many it deliberately does not. */
public object ZinelyV2IconDefaults {

    /** Every icon in the trilogy is authored on `viewBox="0 0 24 24"` — all 42 placements, no exceptions. */
    public const val Viewport: Float = 24f

    /**
     * The grid size, used when a call site does not state one.
     *
     * Note this is the *authoring* size, not a design default: the frozen CSS never renders an icon at
     * 24px. It renders them at eleven other sizes. This exists so a preview has something to draw, not
     * so production code can skip stating the size.
     */
    public val Size: Dp = 24.dp
}
