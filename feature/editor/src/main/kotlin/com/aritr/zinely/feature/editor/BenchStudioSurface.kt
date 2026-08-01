package com.aritr.zinely.feature.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.aritr.zinely.core.editor.Interaction
import com.aritr.zinely.core.editor.LiveSnap
import com.aritr.zinely.core.editor.LiveTransform
import com.aritr.zinely.core.imposition.Imposer
import com.aritr.zinely.core.model.AffineTransform2D
import com.aritr.zinely.core.model.Page
import com.aritr.zinely.core.model.PtPoint
import com.aritr.zinely.core.model.PtRect
import com.aritr.zinely.core.model.PtSize
import com.aritr.zinely.core.model.Transform
import com.aritr.zinely.ui.theme.ZinelyTheme
import com.aritr.zinely.ui.theme.ZinelyV2Colors
import com.aritr.zinely.ui.theme.ZinelyV2Dimens
import com.aritr.zinely.ui.theme.ZinelyV2Grain
import com.aritr.zinely.ui.theme.ZinelyV2ShadowLayer
import com.aritr.zinely.ui.theme.rememberZinelyV2GrainBrush
import com.aritr.zinely.ui.theme.zinelyV2Grain
import com.aritr.zinely.ui.components.zinelyV2Shadow

/**
 * **C1 — the studio surface.** The Bench's ground, the sheet on it, and the three marks the sheet
 * carries: the keep-clear boundary, the centre guide's inset, and the page number.
 *
 * Frozen source: [`v2-bench.html`](../../../../../../../../../docs/design/mockups/v2-bench.html)
 * `:107-108` (`.phone::after`), `:114` (`.canvasArea`), `:117-118` (`.page`), `:119-120`
 * (`.page::after`), `:121-123` (`.keepclear` / `.keepclear.warn`), `:127` (`.pagenum`). Planned and
 * pinned row by row in [ADR-089 §4](../../../../../../../../../docs/DECISIONS.md#adr-089), rows
 * 1.1–1.3 and 1.5–1.11.
 *
 * ### The page box is geometry, not decoration
 *
 * Everything here is positioned against **one** rectangle — the page — and that rectangle is the
 * document's real printable panel scaled by the single `screenPxPerPt` that [PagePreview],
 * [SnapGuides] and [SelectionChrome] already share. It is deliberately **not** a fixed dp size.
 *
 * That is the substance of [D-033](../../../../../../../../../docs/design/V2-SPEC-DEFECTS.md#d-033),
 * which blocked this package until the owner amended the frozen Bench on 2026-08-01. The prototype
 * had drawn a stylised `212×326` page (ratio 0.6503) against a real panel of `210.4725×297.638` pt
 * (0.70714), and a uniform `16px` keep-clear that depicted no real boundary at all. Both were
 * amended — `229×324` and `18.5px` — and the ruling made the page box canonical for `.keepclear`,
 * `.guide`, `.pagenum`, [D-032](../../../../../../../../../docs/design/V2-SPEC-DEFECTS.md#d-032)'s
 * intersection test and the Compose viewport.
 *
 * The practical consequence is [keepClearInset]: this file derives the cue from
 * [Imposer.DEFAULT_SAFE_AREA_INSET_PT] rather than transcribing the frozen `18.5.dp`. The frozen
 * number is a *depiction* of the engine's boundary at one page size; hard-coding it would draw the
 * right line only on a page that happens to be 229dp wide, and would keep drawing the old boundary
 * the day the engine's changed. Transcribe appearance; derive geometry.
 */
internal object BenchStudio {

    /** `.page{border-radius:5px}` — shared by the sheet, its grain clip and its hairline edge. */
    val PageRadius: Dp = 5.dp

    /** `.keepclear{border-radius:3px}` — deliberately tighter than the sheet's own corner. */
    val KeepClearRadius: Dp = 3.dp

    val PageShape: RoundedCornerShape = RoundedCornerShape(PageRadius)

    /** `.page::after{background-size:120px}` — the sheet's own grain tile. */
    val PageGrainTile: Dp = 120.dp

    /**
     * `.page::after` at the **effective** strength Phase A published for it: the tile's baked alpha
     * times the rule's own `opacity:.45`.
     *
     * Expressed as that product against [ZinelyV2Grain.BakedAlpha] rather than as the literal
     * `0.225f`. The first cut of this file wrote the literal under a comment claiming it was "taken
     * from `ZinelyV2Grain`'s table … so the two cannot drift", which was simply untrue — nothing
     * referenced that file (review RI3). Only the **CSS** half is transcribed here; the baked half is
     * a property of the tile and belongs to whoever owns the tile.
     */
    val PAGE_GRAIN_ALPHA: Float = ZinelyV2Grain.BakedAlpha * 0.45f

    /** `.phone::after{background-size:150px}` — the grain over the whole screen. */
    val ScreenGrainTile: Dp = 150.dp

    /** `.phone::after`: the baked alpha × its `opacity:.35`. Same binding as [PAGE_GRAIN_ALPHA]. */
    val SCREEN_GRAIN_ALPHA: Float = ZinelyV2Grain.BakedAlpha * 0.35f

    /** `.pagenum{font-size:9px}` — extracted so row 1.11's `9px→11px` mutation has something to kill. */
    val PageNumberSize: TextUnit = 9.sp

    /** `.pagenum{letter-spacing:.04em}` — `em`, not sp: the tracking follows the size, as the CSS unit means. */
    val PageNumberTracking: TextUnit = 0.04.em

    /** `.keepclear{opacity:.32}` at rest. */
    const val KEEP_CLEAR_REST_ALPHA: Float = 0.32f

    /** `.keepclear.warn{opacity:.9}`. */
    const val KEEP_CLEAR_WARN_ALPHA: Float = 0.90f

    /** `.guide.v{top:8px;bottom:8px}` — the centre guide stops short of the page's corners. */
    val GuideEndInset: Dp = 8.dp

    /** `.guide.show{opacity:.6}` — a guide is only ever drawn in its fired state. */
    const val GUIDE_ALPHA: Float = 0.6f

    /** `.pagenum{top:7px;right:10px;font-size:9px;letter-spacing:.04em}`. */
    val PageNumberTopInset: Dp = 7.dp
    val PageNumberEndInset: Dp = 10.dp

    /**
     * CSS names `dashed` and specifies **no** dash length; every browser picks its own, so the frozen
     * file has no number here to transcribe. Chrome — the renderer the prototype is read in — draws a
     * 1px dashed border at roughly 2px on, 2px off, and that is what this reproduces.
     *
     * Recorded rather than silently chosen: this is the one value in C1 that the frozen file does not
     * state, and a future reader comparing Compose against the CSS will find no `2px` in it.
     */
    val KeepClearDash: Dp = 2.dp

    /**
     * The keep-clear inset for a page drawn at [pageWidthPx] px wide, from a panel [panelWidthPt] pt
     * wide — [Imposer.DEFAULT_SAFE_AREA_INSET_PT] in the page's own scale.
     *
     * Pure, so the derivation is testable without a composition. The frozen `18.5px` is what this
     * returns for the amended `229×324` page (`17 × 229/210.4725 = 18.496`), which is the check that
     * ties the transcription and the derivation together — see `BenchStudioSurfaceTest`.
     */
    fun keepClearInsetPx(pageWidthPx: Float, panelWidthPt: Double): Float =
        if (panelWidthPt <= 0.0 || pageWidthPx <= 0f) {
            0f
        } else {
            (Imposer.DEFAULT_SAFE_AREA_INSET_PT * pageWidthPx / panelWidthPt).toFloat()
        }

    /**
     * `.page{box-shadow:0 14px 30px -14px var(--page-shadow), 0 2px 5px var(--page-contact)}`.
     *
     * Two layers, two tokens, deliberately — see [ZinelyV2Colors.pageShadow]. Collapsing them onto one
     * renders *almost* right in light and wrong in dark, which is the bug the D-010 amendment exists
     * to prevent and the planned mutation for row 1.6.
     */
    fun pageShadowLayers(colors: ZinelyV2Colors): List<ZinelyV2ShadowLayer> = listOf(
        ZinelyV2ShadowLayer(dy = 14.dp, blur = 30.dp, spread = (-14).dp, color = colors.pageShadow),
        ZinelyV2ShadowLayer(dy = 2.dp, blur = 5.dp, color = colors.pageContact),
    )

    /**
     * Row 1.9's trigger — whether `.keepclear.warn` shows this frame.
     *
     * The [D-032](../../../../../../../../../docs/design/V2-SPEC-DEFECTS.md#d-032-ruling) ruling
     * (OD-10, 2026-08-01) settles what the frozen file never said: the warning is **transient
     * guidance, not document state**. Idle never warns — not even with content already sitting in the
     * margin — and the warning cannot outlive the gesture that raised it. So this reads *only* the
     * in-flight interaction and returns `false` for every other frame by construction: there is no
     * persisted flag, nothing in the reducer, and nothing to clear.
     *
     * The geometry is the manipulated element's **drawn extent** against the keep-clear rect. Two
     * consequences worth stating, because neither is arbitrary:
     *
     *  - **The snapped frame is tested, not the raw finger.** A drag resolves through the same
     *    [LiveSnap.resolve] call [EditorPagePreview] renders and [EditorGestures] commits, so the
     *    warning answers for the rectangle the user can actually see. Testing the pre-snap geometry
     *    would blink the cue on and off within the 8px snap threshold, against a page that never moved
     *    there.
     *  - **Rotation is expanded, not ignored.** [LiveSnap] and [Snap] deliberately bail on rotated
     *    elements — edge alignment stops meaning anything once the box is turned — but "content would
     *    enter the keep-clear area" is a claim about what is *drawn*, and a rotated photo's corner
     *    enters the margin while its unrotated box still clears it. Reporting "no warning" there would
     *    be visibly wrong, so the four corners are rotated about the centre and their bounding box is
     *    tested. This is the one reading of the ruling's words that matches the pixels.
     *
     * Note that nothing *enforces* this boundary: `LayoutValidator`'s `SAFE_NOT_IN_PANEL` checks the
     * imposer's own panel geometry, not user content. The cue is advice about the print trim, and the
     * warn state is the moment that advice becomes relevant.
     *
     * Pure, and deliberately not a composable, so every branch is testable without a composition.
     *
     * @param resizeOverride the directly-baked handle-resize frame; wins over [live], exactly as in
     *   [EditorPagePreview], so the cue and the render never disagree about which gesture is running.
     * @return `true` only while an interaction is in flight whose live geometry leaves the keep-clear
     *   rect.
     */
    fun keepClearWarn(
        page: Page,
        interaction: Interaction,
        live: LiveTransform?,
        resizeOverride: Map<String, Transform>?,
        screenPxPerPt: Float,
        pageSizePt: PtSize,
        insetPt: Double = Imposer.DEFAULT_SAFE_AREA_INSET_PT,
    ): Boolean {
        val moving: Collection<Transform> = when {
            resizeOverride != null -> resizeOverride.values
            interaction is Interaction.Transforming && live != null -> {
                val s = screenPxPerPt.toDouble()
                if (!s.isFinite() || s <= 0.0) return false
                LiveSnap.resolve(
                    page = page,
                    selection = interaction.ids,
                    before = interaction.before,
                    live = live,
                    screenPxPerPt = s,
                    pageSize = pageSizePt,
                    thresholdPt = LiveSnap.thresholdPt(s),
                ).transforms.filterKeys { it in interaction.ids }.values
            }
            // Idle, a text session, a reframe, a selection with no gesture — all "not interacting".
            else -> return false
        }
        if (insetPt <= 0.0) return false
        return moving.any { t ->
            val box = rotatedBoundsPt(t)
            box.x < insetPt ||
                box.y < insetPt ||
                box.x + box.width > pageSizePt.width - insetPt ||
                box.y + box.height > pageSizePt.height - insetPt
        }
    }

    /**
     * The axis-aligned page-space box a [Transform] actually covers, its `rotationDegrees` included.
     *
     * Rotation is about the box centre ([com.aritr.zinely.core.model.Transform]), and the sign
     * convention is [AffineTransform2D.rotateDeg]'s — the same one `HitTest` inverts, so a corner this
     * reports as outside is a corner the user can tap out there.
     */
    private fun rotatedBoundsPt(t: Transform): PtRect {
        val halfW = t.widthPt / 2.0
        val halfH = t.heightPt / 2.0
        val rot = AffineTransform2D.rotateDeg(t.rotationDegrees)
        val corners = listOf(
            rot.map(PtPoint(-halfW, -halfH)),
            rot.map(PtPoint(halfW, -halfH)),
            rot.map(PtPoint(halfW, halfH)),
            rot.map(PtPoint(-halfW, halfH)),
        )
        val cx = t.xPt + halfW
        val cy = t.yPt + halfH
        val minX = corners.minOf { it.x } + cx
        val minY = corners.minOf { it.y } + cy
        return PtRect(minX, minY, corners.maxOf { it.x } + cx - minX, corners.maxOf { it.y } + cy - minY)
    }
}

/** Test tag on the keep-clear cue. */
public const val BenchKeepClearTestTag: String = "bench-keep-clear"

/** Test tag on the page number. */
public const val BenchPageNumberTestTag: String = "bench-page-number"

/**
 * Rows 1.1 and 1.2 — the studio ground and the grain over the whole screen.
 *
 * `.phone{background:var(--phone)}` where `--phone` *is* `--paper`, and `.phone::after` lays the grain
 * over everything at 150px / effective .175. The Bench's ground is **paper**, not the Library's desk:
 * different screens, different grounds, and reading one for the other is the mistake this comment
 * exists to prevent.
 *
 * Applied to the screen root so the grain sits over chrome and canvas alike, exactly as
 * `z-index:60` does in the prototype.
 */
@Composable
internal fun Modifier.benchStudioGround(): Modifier {
    val colors = ZinelyTheme.v2Colors
    val grain = rememberZinelyV2GrainBrush(BenchStudio.ScreenGrainTile)
    return this
        .background(colors.paper)
        .zinelyV2Grain(grain, alpha = BenchStudio.SCREEN_GRAIN_ALPHA)
}

/**
 * Rows 1.5–1.7 — the sheet: `--paper`, a 1px `--paper-edge` hairline, radius 5, the two-layer shadow,
 * and the page's own grain clipped to that radius.
 *
 * Sized by the caller. Draws no content of its own; the render tape, the chrome and the furniture all
 * stack over it, so this is the *material* and nothing else.
 */
@Composable
internal fun Modifier.benchPageSurface(): Modifier {
    val colors = ZinelyTheme.v2Colors
    val grain = rememberZinelyV2GrainBrush(BenchStudio.PageGrainTile)
    return this
        .zinelyV2Shadow(BenchStudio.pageShadowLayers(colors), BenchStudio.PageShape)
        .background(colors.paper, BenchStudio.PageShape)
        .border(ZinelyV2Dimens.Hairline, colors.paperEdge, BenchStudio.PageShape)
        // The grain is `.page::after{inset:0;border-radius:5px}` — it takes the sheet's corners, so it
        // is clipped before it is drawn rather than squared off over a rounded sheet.
        .clip(BenchStudio.PageShape)
        .zinelyV2Grain(grain, alpha = BenchStudio.PAGE_GRAIN_ALPHA)
    // Deliberately untagged: the sheet is identified by the caller's own
    // `EditorPaperSurfaceTestTag`, and a second `testTag` in the same chain shadows it.
}

/**
 * Rows 1.8 and 1.9 — the keep-clear cue: the print boundary, drawn.
 *
 * A 1px dashed `--ink-faint` rectangle at the document's safe area, radius 3, at `.32` opacity while
 * resting and `.9` in `--strawberry-text` while warning.
 *
 * ### The warn state is transient, and that is a ruling
 *
 * [D-032](../../../../../../../../../docs/design/V2-SPEC-DEFECTS.md#d-032) was ruled on 2026-08-01:
 * the warning *"is NOT a continuous editing indicator"*. It shows only while an interaction would move
 * content into the keep-clear area, and it disappears when the interaction ends — content already
 * inside after editing finishes draws **no** persistent warning. So [warn] is derived per frame from
 * the in-flight gesture and is never read from, or written to, the document. It holds no reducer
 * state and is not undoable, in the same family as the render-only snap guides
 * ([ADR-029 §5.4](../../../../../../../../../docs/DECISIONS.md#adr-029)).
 *
 * The ruling also settles what the trigger *tests*: the manipulated element's **bounds**. The written
 * *"text or a face"* of the IA is superseded for implementation — nothing here analyses the content of
 * a photo, no detection engine is added, and the privacy principle is not engaged.
 *
 * @param warn whether an interaction is currently in flight whose geometry crosses this boundary.
 * @param panelWidthPt the document panel's width in points, so the inset is the engine's real safe
 *   area rather than a transcribed pixel count — see [BenchStudio].
 */
@Composable
internal fun BenchKeepClear(
    warn: Boolean,
    panelWidthPt: Double,
    modifier: Modifier = Modifier,
) {
    val colors = ZinelyTheme.v2Colors
    val density = LocalDensity.current
    val color = if (warn) colors.strawberryText else colors.inkFaint
    val alpha = if (warn) BenchStudio.KEEP_CLEAR_WARN_ALPHA else BenchStudio.KEEP_CLEAR_REST_ALPHA
    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag(BenchKeepClearTestTag)
            .drawBehind {
                val inset = BenchStudio.keepClearInsetPx(size.width, panelWidthPt)
                if (inset <= 0f || inset * 2f >= size.width || inset * 2f >= size.height) return@drawBehind
                val stroke = with(density) { ZinelyV2Dimens.Hairline.toPx() }
                // CSS puts the border OUTSIDE the inset — `inset:18.5px` with a 1px border paints
                // 18.5→19.5 — while drawRoundRect centres its stroke on the path. Half a stroke of
                // outward offset makes the two describe the same pixels (review Obs 3).
                val edge = inset + stroke / 2f
                val dash = with(density) { BenchStudio.KeepClearDash.toPx() }
                val radius = with(density) { BenchStudio.KeepClearRadius.toPx() }
                drawRoundRect(
                    color = color.copy(alpha = alpha),
                    topLeft = Offset(edge, edge),
                    size = Size(size.width - 2 * edge, size.height - 2 * edge),
                    cornerRadius = CornerRadius(radius, radius),
                    style = Stroke(
                        width = stroke,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash, dash)),
                    ),
                )
            },
    )
}

/**
 * Row 1.11 — `.pagenum`: `"3 / 12"` at 9px `--ink-faint`, `letter-spacing:.04em`, 7px from the top and
 * 10px from the right of the sheet.
 *
 * The **format** is frozen; the numbers are the document's. The frozen `12` belongs to
 * [D-030](../../../../../../../../../docs/design/V2-SPEC-DEFECTS.md#d-030) — a page count the product
 * cannot have — and OD-2 re-seated that capability beyond Phase C, so [pageCount] is read from the
 * document and never written as a constant.
 *
 * **Silent to assistive technology, deliberately.** `EditorPageStrip` already announces the page
 * position (`Copy.PageStrip.pageNumber`) and is the control that can *change* it; this is the sheet's
 * visual echo of that same fact. Left announcing, it inserts a second "3 / 12" in the middle of the
 * canvas subtree — a duplicate reading, and a traversal stop between the work and the tray that the
 * design does not have. `SurfaceTraversalOrderTest` caught exactly that. Nothing is withheld: the
 * information is still spoken, by the control that owns it.
 */
@Composable
internal fun BenchPageNumber(
    pageNumber: Int,
    pageCount: Int,
    modifier: Modifier = Modifier,
) {
    val colors = ZinelyTheme.v2Colors
    Text(
        text = "$pageNumber / $pageCount",
        color = colors.inkFaint,
        fontSize = BenchStudio.PageNumberSize,
        letterSpacing = BenchStudio.PageNumberTracking,
        textAlign = TextAlign.End,
        modifier = modifier
            .padding(top = BenchStudio.PageNumberTopInset, end = BenchStudio.PageNumberEndInset)
            .testTag(BenchPageNumberTestTag)
            .clearAndSetSemantics { },
    )
}
