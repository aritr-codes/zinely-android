package com.aritr.zinely.feature.editor

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.platform.testTag
import com.aritr.zinely.core.model.PtPoint
import com.aritr.zinely.core.model.PtSize
import com.aritr.zinely.core.model.Transform
import com.aritr.zinely.render.android.ExportScale

/** Test tag on the focus-scrim Canvas. */
public const val BenchFocusScrimTestTag: String = "bench-focus-scrim"

/**
 * The frozen selection **dim** and **materialise**, drawn as one paper wash over the sheet
 * ([ADR-091 §2](../../../../../../../../docs/DECISIONS.md#adr-091)).
 *
 * The Bench gives every element its own DOM node, so `.content.focusing .el:not(.sel-focus){opacity:.4}`
 * and `@keyframes mat{opacity:0→1}` are one-line declarations there. Zinely renders a page as **one tape
 * into one canvas**, and there is no per-element alpha in that tape — nor may there be one in the model,
 * because an `opacity` on `Element` would serialise into the document and reach the exported PDF. A dim
 * that prints is a data-loss bug wearing a UI feature's clothes.
 *
 * So the dim is composited **after** the render instead of inside it, and the arithmetic is exact rather
 * than approximate: CSS `opacity:.4` over paper resolves to `0.4·element + 0.6·paper`, which is precisely
 * what a `--paper` wash at **α 0.6** produces over that same element. One render pass, and — unlike
 * rendering the page twice at two alphas — **z-order is untouched**, so selecting a background photo does
 * not lift it over the caption sitting on top of it.
 *
 * [holes] are punched out with [PathFillType.EvenOdd]: the selected element keeps its full strength,
 * exactly as `:not(.sel-focus)` excludes it. [covers] are the opposite — regions washed at *full* opacity,
 * which is how the materialise fade runs (α 1 → 0 over the arriving element, [BenchMaterialise]).
 *
 * **Two recorded deviations** ([ADR-091](../../../../../../../../docs/DECISIONS.md#adr-091) rows 2.8a and
 * 2.9a), both consequences of compositing by region rather than by element:
 *
 *  1. **The dim's:** an unselected element lying *on top of* the selected one is dimmed by the freeze and
 *     is not dimmed here, because it falls inside the hole. Invisible in practice — the wash is paper over
 *     paper — and it errs toward showing the user's own content at full strength, the direction owner
 *     ruling OD-12 chose for the artifact.
 *  2. **The materialise's:** a [covers] quad is opaque, so anything drawn *above* the arriving element
 *     within its rect is covered for the animation's 300 ms too. The common case is safe because an
 *     insertion appends on top of the page, but inserting *beneath* an existing caption blanks that
 *     caption while the new element arrives. This one is visible, which is why it is stated rather than
 *     folded into the first.
 *
 * Both are asserted as deviations, so they stay recorded readings rather than undiscovered bugs.
 *
 * **The wash is clipped to the sheet, and that is load-bearing.** This composable fills the whole canvas
 * (its siblings do, so their device-px coordinates agree), but the sheet is a smaller centred child of it.
 * Washing the full canvas paints *paper* over the **desk** — in the dark theme that composites `#2F2A22`
 * to roughly `#A7A298`, so the entire room bleaches to light grey on every selection and un-bleaches on
 * every deselection. That inverts [ADR-090](../../../../../../../../docs/DECISIONS.md#adr-090) exactly:
 * the room is what may dim and the sheet is what may not. The freeze scopes the dim to
 * `.content{inset:22px}` *inside* `.page`, and it never reaches the desk either. So [pageRect] bounds the
 * wash, and this parameter exists to make that impossible to forget.
 *
 * @param paper the sheet colour to wash with — the island `--paper`, so the arithmetic holds in both themes.
 * @param pageRect the sheet's device-px rectangle; the wash and the covers are confined to it.
 * @param dimAlpha the wash's alpha; `0f` ⇒ nothing is drawn at all.
 * @param holes device-px quads to exclude from the wash (the live selection).
 * @param covers device-px quads washed at full opacity regardless of [dimAlpha] (the arriving element).
 */
@Composable
internal fun BenchFocusScrim(
    paper: Color,
    pageRect: Rect,
    dimAlpha: Float,
    holes: List<List<PtPoint>>,
    covers: List<Pair<List<PtPoint>, Float>>,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.testTag(BenchFocusScrimTestTag)) {
        if (dimAlpha > 0f) {
            // EvenOdd over [the sheet] + [each hole] leaves the holes unpainted — the `:not(.sel-focus)`
            // exclusion, expressed as geometry because it cannot be expressed as an element property.
            val wash = Path().apply {
                fillType = PathFillType.EvenOdd
                addRect(pageRect)
                for (quad in holes) appendQuad(quad)
            }
            drawPath(path = wash, color = paper, alpha = dimAlpha)
        }
        clipRect(pageRect.left, pageRect.top, pageRect.right, pageRect.bottom) {
            for ((quad, alpha) in covers) {
                if (alpha <= 0f) continue
                drawPath(path = Path().apply { appendQuad(quad) }, color = paper, alpha = alpha)
            }
        }
    }
}

/** Appends a closed device-px quad; a malformed quad is skipped rather than drawn as a stray triangle. */
private fun Path.appendQuad(quad: List<PtPoint>) {
    if (quad.size != 4) return
    moveTo(quad[0].x.toFloat(), quad[0].y.toFloat())
    for (i in 1 until quad.size) lineTo(quad[i].x.toFloat(), quad[i].y.toFloat())
    close()
}

/**
 * The frozen `.content.focusing` wash alpha. The freeze dims the element to `opacity:.4`; over paper that
 * composites to `0.4·element + 0.6·paper`, so the *wash* is the complement — **0.6**, not 0.4. Getting
 * this the obvious way round would be a visibly weaker dim, and the arithmetic is the whole reason the
 * composite is admissible in place of a per-element alpha.
 */
internal const val BenchFocusDimAlpha: Float = 0.6f

/** Frozen `.content.focusing .el{transition:opacity .18s}` (v2-bench.html `:152`). */
internal const val BenchFocusDimMillis: Int = 180

/** Frozen `.sel` / `.handle` `transition:opacity .12s` (v2-bench.html `:155`, `:157`). */
internal const val BenchChromeFadeMillis: Int = 120

/**
 * The sheet's device-px rectangle — the same seam every other layer reads, so the scrim's bound cannot
 * drift from the page the user sees. Pure, so the one number that stops the wash reaching the desk is
 * testable without a composition.
 */
internal fun benchPageRect(screenPxPerPt: Float, pageOffset: PtPoint, pageSizePt: PtSize): Rect {
    val toDevice = ExportScale.previewPageToDevice(screenPxPerPt.toDouble(), pageOffset)
    val topLeft = toDevice.map(PtPoint(0.0, 0.0))
    val bottomRight = toDevice.map(PtPoint(pageSizePt.width, pageSizePt.height))
    return Rect(
        left = topLeft.x.toFloat(),
        top = topLeft.y.toFloat(),
        right = bottomRight.x.toFloat(),
        bottom = bottomRight.y.toFloat(),
    )
}

/** Frozen `.el.materialize{animation:mat .3s var(--settle)}` (v2-bench.html `:160`). */
internal const val BenchMaterialiseMillis: Int = 300

/** Frozen `@keyframes mat{from{transform:scale(.92)}}` (v2-bench.html `:161`). */
internal const val BenchMaterialiseFromScale: Float = 0.92f

/**
 * The frozen materialise, as pure functions of a `0f..1f` progress — so both halves are unit-testable
 * without a frame clock, and neither needs reducer state, an `Intent`, or a model field.
 *
 * `@keyframes mat` runs `scale(.92)→scale(1)` and `opacity:0→1` together. The scale rides
 * `LivePreview.applyOverride` — the *existing* seam the live gesture preview and the commit already share
 * — and the fade is [BenchFocusScrim]'s cover run backwards, from a full paper wash to none. Nothing new
 * is introduced for either half.
 */
internal object BenchMaterialise {

    /** `scale(.92)` at `progress = 0`, `scale(1)` at `1`; the easing is applied by the caller's animation. */
    fun scaleAt(progress: Float): Float {
        val p = progress.coerceIn(0f, 1f)
        return BenchMaterialiseFromScale + (1f - BenchMaterialiseFromScale) * p
    }

    /** The paper cover's alpha: full at `progress = 0` (element invisible), gone at `1`. */
    fun coverAlphaAt(progress: Float): Float = 1f - progress.coerceIn(0f, 1f)

    /**
     * [t] scaled about its own **centre**, which is what `transform:scale()` does — a naive width/height
     * multiply would anchor the growth at the top-left and make the element crawl into place instead of
     * settling into it. Rotation is untouched.
     */
    fun scaledAboutCentre(t: Transform, scale: Float): Transform {
        val cx = t.xPt + t.widthPt / 2.0
        val cy = t.yPt + t.heightPt / 2.0
        val w = t.widthPt * scale
        val h = t.heightPt * scale
        return t.copy(xPt = cx - w / 2.0, yPt = cy - h / 2.0, widthPt = w, heightPt = h)
    }
}
