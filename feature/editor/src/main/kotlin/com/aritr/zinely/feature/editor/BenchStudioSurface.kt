package com.aritr.zinely.feature.editor

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.TextUnit
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
import com.aritr.zinely.ui.theme.LocalZinelyV2Colors
import com.aritr.zinely.ui.theme.LocalZinelyV21Colors
import com.aritr.zinely.ui.theme.ZinelyV2Colors
import com.aritr.zinely.ui.theme.ZinelyV2Dimens
import com.aritr.zinely.ui.theme.zinelyV2LightColors
import com.aritr.zinely.ui.theme.ZinelyV21Colors
import com.aritr.zinely.ui.theme.ZinelyV21Dimens
import com.aritr.zinely.ui.theme.ZinelyV21Grain
import com.aritr.zinely.ui.theme.zinelyV21LightColors
import com.aritr.zinely.ui.theme.rememberZinelyV21GrainBrush
import com.aritr.zinely.ui.theme.zinelyV21Grain
import com.aritr.zinely.ui.components.zinelyV21HardShadow

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

    /**
     * `.page{border-radius:var(--br-xs) var(--br-md) var(--br-md) var(--br-xs)}` — **the spine.**
     *
     * V2.1's sheet is not a uniform rounded rectangle. Its two **leading** corners are tight
     * ([ZinelyV21Dimens.radiusXs]) and its two **trailing** ones are generous
     * ([ZinelyV21Dimens.radiusMd]): the page is bound on the left, so that edge is cut square where a
     * spine would hold it and rounded where the paper is free. V2 drew a single `5px` on all four.
     *
     * Written as start/end rather than left/right so the spine follows the layout direction, which is
     * what "bound edge" means and what `RoundedCornerShape`'s start/end parameters already express.
     */
    val PageRadiusSpine: Dp = ZinelyV21Dimens.radiusXs
    val PageRadiusFree: Dp = ZinelyV21Dimens.radiusMd

    /** `.keepclear{border-radius:var(--br-xs)}` — converted with the colour and the trigger in P2. */
    val KeepClearRadius: Dp = ZinelyV21Dimens.radiusXs

    val PageShape: RoundedCornerShape = RoundedCornerShape(
        topStart = PageRadiusSpine,
        topEnd = PageRadiusFree,
        bottomEnd = PageRadiusFree,
        bottomStart = PageRadiusSpine,
    )

    /**
     * `.page{border:1.5px solid var(--ink)}` — and it is **`ink`, not `paperEdge`**.
     *
     * V2 drew a 1px `paperEdge` hairline: the sheet's own cut edge, barely there. V2.1 draws a real
     * 1.5px line in the same ink the content is set in, because the whole language is printed marks
     * rather than surfaces. `paperEdge` survives in the palette and has **no use on this page**, which
     * is why it left the island ([ADR-102 §12.1](../../../../../../../../../docs/DECISIONS.md#adr-102-island-v21)).
     *
     * A literal rather than a published token: V2.1 writes `1.5px` at 37 Bench use sites and publishes
     * no border scale, so this transcribes the frozen value under OD-29's traceability rule.
     */
    val PageBorder: Dp = 1.5.dp

    /** `.page::after{background-size:130px}` — the sheet's own grain tile. */
    val PageGrainTile: Dp = 130.dp

    /**
     * `.page::after` at the **effective** strength: the tile's baked alpha times the rule's own
     * `opacity:.35`.
     *
     * Expressed as that product against [ZinelyV21Grain.BakedAlpha] rather than as a literal. The
     * first cut of this file wrote the literal under a comment claiming it was "taken from the grain
     * table … so the two cannot drift", which was untrue — nothing referenced that file (review RI3).
     * Only the **CSS** half is transcribed here; the baked half is a property of the tile.
     *
     * ⚠ **The page's and the screen's alphas swapped between V2 and V2.1**, and the tiles changed with
     * them: V2 was page `120px/.45`, screen `150px/.35`; V2.1 is page `130px/.35`, screen `160px/.45`.
     * A reader reconciling this file against V2 will find both numbers on the wrong line.
     */
    val PAGE_GRAIN_ALPHA: Float = ZinelyV21Grain.BakedAlpha * 0.35f

    /** `.phone::after{background-size:160px}` — the grain over the whole screen. */
    val ScreenGrainTile: Dp = 160.dp

    /** `.phone::after`: the baked alpha × its `opacity:.45`. Same binding as [PAGE_GRAIN_ALPHA]. */
    val SCREEN_GRAIN_ALPHA: Float = ZinelyV21Grain.BakedAlpha * 0.45f

    /** `.pagenum{font-size:.6rem}` — `rem` against the prototype's 16px root, so 9.6px. */
    val PageNumberSize: TextUnit = 9.6.sp

    /** `.pagenum{right:9px;bottom:6px}` — V2.1 seats the number at the sheet's **foot**, not its head. */
    val PageNumberBottomInset: Dp = 6.dp
    val PageNumberEndInset: Dp = 9.dp

    /**
     * `.keepclear.warn{opacity:.9}` (`v21-bench.html:190`) — the cue's **only** alpha, because after
     * [OD-48](../../../../../../../../../docs/DECISIONS.md#adr-102-p2b) it has only one state.
     *
     * Three specifications have now passed through this constant, and the direction is one way:
     * V2 drew the boundary permanently at `.32` with a `.9` warning; V2.1's first freeze rested it at
     * `opacity:0` and revealed it at `.85` on *selection*; OD-48 removed the selection reveal too. The
     * mark now appears only while a gesture is carrying content across the printer's reach — which is
     * [BP-4](../../../../../../../../../docs/design/V2-BENCH-PRINCIPLES.md)'s *"a gentle nudge only when
     * text or faces cross it"* and [D-032](../../../../../../../../../docs/design/V2-SPEC-DEFECTS.md#d-032)'s
     * *"transient guidance, not document state"* applied without a residue.
     *
     * ⚠ **The accessibility argument inverted, and it is worth reading twice.** §12.9 accepted the
     * frozen `berry` cue at **2.07:1** *as decorative*, resting that acceptance on the warning (`jam` at
     * this alpha, **3.66:1** in both themes — that figure is *computed* on flat tokens; the device
     * measured **3.51:1** through the sheet's grain, and both clear the floor) being the one mark on the
     * surface that clears WCAG 1.4.11's 3:1 floor. OD-48 deletes the decorative mark rather than the accessible one, so the boundary no
     * longer draws anything below the floor at all. The sub-3:1 marks left on this surface are the
     * `butter` snap guide (**1.20:1** at its frozen .85 alpha) and nothing else.
     *
     * Keeping a warning at all remains a departure — **the freeze had none** — ruled by the owner on
     * 2026-08-13 ([ADR-102 §12.9](../../../../../../../../../docs/DECISIONS.md#adr-102-p2-marks)) under
     * the [OD-11](../../../../../../../../../docs/DECISIONS.md#adr-098-od11) capability rule, in the same
     * family as the handle halo the freeze also lacks (§12.6 row 5). OD-48 promoted that departure from
     * *an extra state* to *the whole mark*, and the freeze was amended to match before this file was.
     */
    const val KEEP_CLEAR_WARN_ALPHA: Float = 0.90f

    /** `.guide.v{top:8px;bottom:8px}` — the centre guide stops short of the page's corners. */
    val GuideEndInset: Dp = 8.dp

    /** `.content.focusing~.guideV{opacity:.85}` — a guide is only ever drawn in its fired state. */
    const val GUIDE_ALPHA: Float = 0.85f

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
     * The sheet's palette inside [BenchSheetIsland] — [room] with **exactly the eight tokens `.page`
     * re-declares** taken from the light theme, and nothing else touched.
     *
     * Eight, not the whole scheme. The first cut of the island provided `zinelyV2LightColors()` wholesale,
     * which lightened all twenty-six — including `pageShadow` / `pageContact`, the sheet's own shadow. In
     * dark that drew a warm-brown shadow on a dark desk: precisely the defect
     * [D-010](../../../../../../../../../docs/design/V2-SPEC-DEFECTS.md#d-010) was raised for and OD-3
     * amended both frozen files to remove, silently reinstated by the fix for a different one — and
     * certified by a re-recorded golden, the corpus's own *goldens-in-record-mode-aren't-assertions*, for
     * the second time in this package. Review caught it; the pixels below the sheet were *brighter* than
     * the ground, a glow where a contact shadow belongs.
     *
     * So this mirrors the CSS cascade instead of replacing the scheme: `.page` declares eight custom
     * properties and inherits the rest, and so does this. The shadow is the room's, because the shadow
     * *is* the room — it is the sheet's mark on the desk, not part of the sheet.
     *
     * Pure, so the set is asserted against the frozen `.page` block without a composition.
     */
    fun sheetIsland(room: ZinelyV2Colors): ZinelyV2Colors {
        val light = zinelyV2LightColors()
        return room.copy(
            paper = light.paper,
            paperEdge = light.paperEdge,
            ink = light.ink,
            inkSoft = light.inkSoft,
            inkFaint = light.inkFaint,
            matcha = light.matcha,
            matchaText = light.matchaText,
            strawberryText = light.strawberryText,
        )
    }

    /**
     * The V2.1 sheet island — **seven tokens**, and none of V2's reasoning
     * ([ADR-102 §12.1](../../../../../../../../../docs/DECISIONS.md#adr-102-island-v21)).
     *
     * It was eight until [OD-48](../../../../../../../../../docs/DECISIONS.md#adr-102-p2b) deleted the
     * resting keep-clear cue, which was the only mark on the sheet painted in `berry`. The count is not
     * the invariant — *what the page paints* is — and this doc says seven only so a reader comparing it
     * against V2's eight does not read the difference as drift.
     *
     * ### It is derived from the frozen file, not carried over from V2
     *
     * ADR-102 §3 published a destination table mapping V2's eight to V2.1 — and it was written without
     * opening `v21-bench.html`. Four of its rows have no V2.1 use at all: the page's border is `ink`
     * rather than `paperEdge`, the keep-clear is `jam` (`berry` until OD-48) rather than `inkFaint`, and `matcha`'s mark
     * (`matchaText` with it) no longer exists on this surface. Two reviews returned NO-GO on it before
     * any code was written. The set below is what the frozen file's own `the canvas` section
     * (`v21-bench.html:174–219`) actually paints, and `BenchStudioSurfaceTest` re-derives it from that
     * file rather than trusting this list.
     *
     * `leaf` and `berryTint` reach the island through the prototype's **content stand-ins** (`.sticker`,
     * `.photo`) rather than through a live Compose surface today. They are islanded anyway, because the
     * island is a property of the *subtree*: a mark that reaches for `leaf` on paper tomorrow gets the
     * lit value without anyone having to remember this list.
     *
     * ### `inkLine` is the ninth token, and it is excluded
     *
     * The V2.1 page's shadow is `box-shadow:var(--hard) var(--hard) 0 var(--ink-line)` — a hard offset
     * shadow that falls on the **bench**, not on the paper. `inkLine` is `#27270F` light and `#120E0A`
     * dark; lighting it here would put a `#27270F` shadow on a `#242312` worktop — **D-010**, *a glow
     * where a contact shadow belongs*, for the third time in this file's history and inside the fix for
     * D-035 for the second.
     *
     * ADR-102 §3 warned about exactly this and named the wrong tokens: it forbids `softShadow` and
     * `contact`, which the V2.1 page does not use at all. The warning was right and its cause was stale.
     * Because this is a `copy` of the room rather than a wholesale light palette, `inkLine` — and every
     * other token not named below — keeps the room's value **by construction**. That is the entire
     * reason the wholesale form is forbidden here while the Proof may keep it: the Proof's lift comes
     * from `ZinelyTheme.elevation`, a separate CompositionLocal, and the Bench's comes from the scheme.
     *
     * Pure, so the set is asserted against the frozen file without a composition.
     */
    fun sheetIslandV21(room: ZinelyV21Colors): ZinelyV21Colors {
        val light = zinelyV21LightColors()
        return room.copy(
            paper = light.paper,
            ink = light.ink,
            inkSoft = light.inkSoft,
            // ⚠ `berry` is **not** here, and its absence is a consequence rather than an oversight. It was
            // on this list for exactly one mark — the resting keep-clear cue — and OD-48 deleted that mark,
            // so the frozen page region no longer paints the token anywhere. The island's rule is that it
            // names what the sheet paints and nothing else, and `the Compose sheet island lights exactly
            // the tokens the frozen page paints` fails in *both* directions, so it caught this the moment
            // the CSS changed. `berryTint` stays: the `.photo` stand-in still fills with it
            // (`v21-bench.html:210-211`) — a review corrected this comment, which said `.el`, a selector
            // that carries no background at all.
            jam = light.jam,
            leaf = light.leaf,
        )
    }

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

    // `pageShadowLayers` is deleted, not ported. V2 drew the sheet with a two-layer SOFT shadow
    // (`0 14px 30px -14px var(--page-shadow), 0 2px 5px var(--page-contact)`); V2.1 draws a single
    // HARD offset shadow in `inkLine` — see [benchPageSurface]. `pageShadow` / `pageContact` have no
    // V2.1 counterpart at all, so keeping the function would leave two tokens alive whose only job was
    // to be excluded from the island. The exclusion survives as `inkLine`'s — see [sheetIslandV21].

    /**
     * Row 1.9's trigger — whether `.keepclear.warn` shows this frame.
     *
     * The [D-032](../../../../../../../../../docs/design/V2-SPEC-DEFECTS.md#d-032-ruling) ruling
     * (OD-10, 2026-08-01) settled what the frozen file never said: the warning is **transient guidance,
     * not document state**. Idle never warns — not even with content already sitting in the margin — and
     * the warning cannot outlive the gesture that raised it.
     *
     * ### ⚠ [OD-49](../../../../../../../../../docs/DECISIONS.md#adr-102-p2c) narrows that, and the device
     * is why
     *
     * Reading *only* the in-flight gesture left two holes that
     * [P2b's device pass](../../../../../../../../../docs/reviews/2026-08-13-adr-102-p2b-device-verification.md)
     * walked straight into. **The nudge path never warns at all** — `Move left`, `Make smaller`, `Rotate`
     * are discrete intents, never "in flight" — so the *accessible* way to move an element was the one way
     * you were never told. And **release read as resolution**: let go with content outside and the mark
     * vanishes, which a first-time reader reports as *"good, I must have got it back inside."*
     *
     * Before [OD-48](../../../../../../../../../docs/DECISIONS.md#adr-102-p2b) both were covered by
     * accident, because selecting anything drew the resting boundary. OD-48 removed the accident. So the
     * predicate is now **"the selection is crossing"** rather than **"a gesture is crossing"**: an
     * in-flight gesture still answers with its live, snapped geometry, and every other frame answers with
     * the selection's **committed** geometry.
     *
     * What survives of D-032 is the part that was actually load-bearing: **an unheld page never warns.**
     * Nothing is drawn at idle, nothing on an unselected page, nothing about content the maker is not
     * currently working on — and still no persisted flag, nothing in the reducer, nothing to clear. What is
     * given up is the phrase *"cannot outlive the gesture"*, which turned out to describe a mark that
     * stopped speaking while the thing it warned about was still true.
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
     * @param selection the ids the maker is holding. What the warning answers for in every frame with no
     *   gesture in flight — and the reason an empty set is silent.
     * @return `true` when the geometry the maker is currently answerable for leaves the keep-clear rect:
     *   an in-flight gesture's **live** frame, or otherwise the selection's **committed** one. `false` for
     *   an unheld page and for an in-place session, always. ⚠ This tag said *"only while an interaction is
     *   in flight"* until OD-49 and was left behind when the body was rewritten — a review caught it, and
     *   it is the sentence a reader checks the behaviour against.
     */
    fun keepClearWarn(
        page: Page,
        interaction: Interaction,
        live: LiveTransform?,
        resizeOverride: Map<String, Transform>?,
        screenPxPerPt: Float,
        pageSizePt: PtSize,
        selection: Set<String> = emptySet(),
        insetPt: Double = Imposer.DEFAULT_SAFE_AREA_INSET_PT,
    ): Boolean {
        val answering: Collection<Transform> = when {
            // **An in-place session answers its own question, and this is not it.** Typing into a text box
            // asks *"what do these words say?"*; reframing asks *"which part of the photo shows?"*. Neither
            // asks where the printer stops, and neither can act on the answer — the box cannot be moved
            // from inside either session. A print alarm raised there is [ADR-058](../../../../../../../../../docs/DECISIONS.md#adr-058)'s
            // "Preview" failure in miniature: a good answer at a moment the maker did not ask it. The
            // session ends with the element still selected, so the warning arrives one beat later, when it
            // is actionable. ⚠ This is not the golden's convenience: `BenchStyleRowGoldenTest`'s editing
            // scene places a box that genuinely overhangs the foot (y 76+18 against an 83pt limit), and it
            // failing is what raised the question.
            //
            // ⚠ **First, so the ruling's table is what the code enforces.** It sat below the resize branch,
            // where a non-null `resizeOverride` during a session would have warned anyway — unreachable
            // today only because the handles are gated on `!editing && reframing == null` at the call site.
            // Ordering the branch correctly is cheaper than depending on that gate never leaking, and no
            // test pinned the old order.
            interaction is Interaction.EditingText || interaction is Interaction.Reframing -> return false
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
            // **No gesture: the selection's committed geometry answers instead** (OD-49). This branch used
            // to `return false`, and the device pass is what proved the cost — see the KDoc above. An empty
            // selection still returns false here, because `none {}` is false: an unheld page never warns,
            // which is the half of D-032 that survives intact.
            else -> page.elements.filter { it.id in selection }.map { it.transform }
        }
        if (insetPt <= 0.0) return false
        return answering.any { crossesKeepClear(it, pageSizePt, insetPt) }
    }

    /**
     * Whether one element's **drawn extent** leaves the keep-clear rect — the geometry half of
     * [keepClearWarn], extracted because [ElementSemanticsLayer] needs the same answer per element to
     * speak it, and two implementations of *"is this outside the printer's reach"* would eventually
     * disagree about a rotated photo's corner.
     *
     * Rotation is expanded rather than ignored, for the reason [keepClearWarn] records: the claim is about
     * what is *drawn*, and a turned photo's corner enters the margin while its unrotated box still clears.
     */
    fun crossesKeepClear(
        transform: Transform,
        pageSizePt: PtSize,
        insetPt: Double = Imposer.DEFAULT_SAFE_AREA_INSET_PT,
    ): Boolean {
        if (insetPt <= 0.0) return false
        val box = rotatedBoundsPt(transform)
        return box.x < insetPt ||
            box.y < insetPt ||
            box.x + box.width > pageSizePt.width - insetPt ||
            box.y + box.height > pageSizePt.height - insetPt
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
 * The studio ground and the grain over the whole screen.
 *
 * `.phone{background:var(--bench)}` (`v21-bench.html:158`), with `.phone::after` laying the chrome grain
 * over everything at 160px / soft-light / `.45`.
 *
 * ### The ground is `bench`. It was `paper`, and the change is not cosmetic
 *
 * V2 had no worktop token: `--phone` *was* `--paper`, and this file's previous comment argued at length
 * that *"the Bench's ground is **paper**, not the Library's desk … reading one for the other is the
 * mistake this comment exists to prevent."* That was true of V2 and is false of V2.1, which publishes
 * `--bench` — *"a darker desk, so the page reads as the hero"* ([ZinelyV21Colors.bench]).
 *
 * **This is the token that makes the island visible for the first time.** While ground and sheet were
 * both `paper`, the boundary between the room and the artifact was nearly invisible and a mistake on
 * either side of it cost little. Under `bench` the room genuinely darkens around a lit sheet, so every
 * chrome-versus-artifact decision on this screen becomes something the user can see. The opt-outs
 * themselves are unchanged; what changes is the cost of getting one wrong.
 *
 * Those opt-outs are `EditorScreen`'s `roomColors` / `roomColors21` pair and the three
 * `CompositionLocalProvider` blocks that restore them — and note that the **third reads the theme
 * afresh rather than the hoisted pair**, so it is three opt-out *sites* and not one value flowing to
 * three places. Deliberately described rather than cited by line: ADR-102 §12.6 row 7 records that
 * every previous attempt to cite them by number drifted, and a rename sweep that trusts a stale number
 * misses the odd one out — which is the whole defect that row exists to name.
 *
 * Applied to the screen root so the grain sits over chrome and canvas alike, exactly as
 * `z-index:90` does in the prototype.
 */
@Composable
internal fun Modifier.benchStudioGround(): Modifier {
    val colors = ZinelyTheme.v21Colors
    val grain = rememberZinelyV21GrainBrush(BenchStudio.ScreenGrainTile)
    return this
        .background(colors.bench)
        .zinelyV21Grain(grain, alpha = BenchStudio.SCREEN_GRAIN_ALPHA, blend = ZinelyV21Grain.ChromeBlend)
}

/**
 * **The sheet is a light-theme island** — the [D-035](../../../../../../../../../docs/design/V2-SPEC-DEFECTS.md#d-035)
 * amendment, and the one thing about C1 that is not a transcription.
 *
 * The dark theme dims the *room*. It must not dim the *artifact*, because the artifact's ink is print
 * colour: the document's content inks are theme-independent by design — a PDF that changed with the
 * phone's night setting would be the worse defect by far — so a dimmed sheet left the user's own black
 * text at **1.60:1** on device, while the Read/Proof screen showed the same page at 16.92:1. The owner
 * ruled that the artifact does not dim, and the frozen Bench was amended to match (`.page` re-declares
 * the on-paper tokens with their light values).
 *
 * This is that amendment: everything composed inside — the sheet, the keep-clear cue, the centre guide,
 * the page number, the render tape, the selection chrome and handles, and the blank-page invitation —
 * resolves the on-paper tokens to their light values in both themes. No new colour is introduced and
 * none is remapped: it is the light palette, applied where the paper is. Everything else, the sheet's
 * own shadow included, stays the room's — see [BenchStudio.sheetIslandV21] for why that distinction is
 * load-bearing rather than tidy.
 *
 * **The rule generalised on 2026-08-12.** [OD-31](../../../../../../../../../docs/DECISIONS.md#adr-098-od31)
 * closed *yes, and universally: the artifact does not dim, on any surface.* D-035 was the Bench's
 * instance of it. That ruling licenses the **rule** everywhere and **not** the mechanism: the Proof may
 * provide `zinelyV21LightColors()` wholesale because its lift comes from `ZinelyTheme.elevation`, a
 * separate CompositionLocal, and the Bench may not, because its shadow ink is in the scheme.
 *
 * ### Why this provides two palettes
 *
 * P1 converted the ground, the sheet, the selection chrome, the handles and the caret to V2.1, and **P2
 * converted the keep-clear cue and the snap guide on 2026-08-13**
 * ([§12.9](../../../../../../../../../docs/DECISIONS.md#adr-102-p2-marks)) — they moved whole, colour and
 * trigger together, because the freeze gives them no resting state to paint and that was P2's ruling
 * ([ADR-102 §12.3](../../../../../../../../../docs/DECISIONS.md#adr-102-p1-recut)) — and it does not
 * convert the Type bar, the text-edit session sheet, the save-failure banner, the "Saved" chip, the
 * coverage notice, the move/resize hint or the Reframe chrome, which are P6's.
 *
 * So both islands are provided for the interval, and the V2 one is **transitional**: it exists to keep
 * the unconverted children lit while they wait their turn, and it is deleted when P6 lands. Stated
 * rather than left to be inferred, because an island that outlives its last reader is exactly the kind
 * of thing that gets read as design.
 *
 * The bar, the tray, the page strip and the context bar are already outside this scope, which is where
 * room chrome belongs.
 */
@Composable
internal fun BenchSheetIsland(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val room = ZinelyTheme.v2Colors
    val room21 = ZinelyTheme.v21Colors
    val onPaper = remember(room) { BenchStudio.sheetIsland(room) }
    val onPaper21 = remember(room21) { BenchStudio.sheetIslandV21(room21) }
    CompositionLocalProvider(
        LocalZinelyV2Colors provides onPaper,
        LocalZinelyV21Colors provides onPaper21,
    ) {
        Box(modifier = modifier, content = content)
    }
}

/**
 * The sheet: `--paper`, a **1.5px `--ink` border**, the spine radius, the **hard offset shadow**, and
 * the page's own grain clipped to that shape (`v21-bench.html:177-182`).
 *
 * ### Three of the four layers changed material, not just value
 *
 *  - **The shadow.** V2 cast two soft layers in `pageShadow` / `pageContact`; V2.1 casts one *printed*
 *    shadow — `var(--hard) var(--hard) 0 var(--ink-line)`, zero blur, down-right, flat ink. They are two
 *    materials, which is why [zinelyV21HardShadow] is a separate primitive rather than the V2 layer with
 *    an `x` added.
 *  - **The edge.** `1px paperEdge` → `1.5px ink`. See [BenchStudio.PageBorder].
 *  - **The corner.** A uniform `5px` → a spine. See [BenchStudio.PageShape].
 *
 * **The shadow's ink comes from the room, and it must.** `inkLine` is deliberately *not* in
 * [BenchStudio.sheetIslandV21], so reading it here — inside the island — still yields the room's value
 * by construction. That is not a happy accident of the `copy`; it is the reason the island is a `copy`.
 * A wholesale light palette would light `#120E0A` to `#27270F` and put a glow under the sheet on a
 * `#242312` worktop: **D-010**, in the package whose purpose is not to re-break D-035.
 *
 * Sized by the caller. Draws no content of its own; the render tape, the chrome and the furniture all
 * stack over it, so this is the *material* and nothing else.
 */
@Composable
internal fun Modifier.benchPageSurface(): Modifier {
    val colors = ZinelyTheme.v21Colors
    val grain = rememberZinelyV21GrainBrush(BenchStudio.PageGrainTile)
    return this
        .zinelyV21HardShadow(ZinelyV21Dimens.hardShadow, colors.inkLine, BenchStudio.PageShape)
        .background(colors.paper, BenchStudio.PageShape)
        .border(BenchStudio.PageBorder, colors.ink, BenchStudio.PageShape)
        // The grain is `.page::after{inset:0;border-radius:inherit}` — it takes the sheet's corners, so
        // it is clipped before it is drawn rather than squared off over a rounded sheet. `inherit` is
        // why the spine radius could not be hard-coded here a second time.
        //
        // The clip sits to the RIGHT of the hard shadow deliberately: a clip to its left would cut the
        // 4dp the shadow paints outside the node, which is the chain contract [zinelyV21Pressable]
        // records and the failure mode that reads on a device as "the shadow is missing".
        .clip(BenchStudio.PageShape)
        // `mix-blend-mode:multiply` — paper grain darkens where the noise is dark, like ink. The screen's
        // grain is soft-light; the two are different materials and [ZinelyV21Grain] names both.
        .zinelyV21Grain(grain, alpha = BenchStudio.PAGE_GRAIN_ALPHA, blend = ZinelyV21Grain.PaperBlend)
    // Deliberately untagged: the sheet is identified by the caller's own
    // `EditorPaperSurfaceTestTag`, and a second `testTag` in the same chain shadows it.
}

/**
 * Rows 1.8 and 1.9 — the keep-clear cue: the print boundary, drawn.
 *
 * V2.1 (P2b): a **1.5dp dashed `jam`** rectangle at the document's safe area, radius `--br-xs`, drawn at
 * `.9` **only while a gesture is carrying content across it** and `opacity:0` in every other frame.
 *
 * ### The mark has one state, and that is [OD-48](../../../../../../../../../docs/DECISIONS.md#adr-102-p2b)
 *
 * V2 drew this permanently at `.32`. V2.1's first freeze rested it at `opacity:0` and revealed it at `.85`
 * on `.content.focusing` — a live selection. P2's device pass then found that reading confusing in a way
 * no arithmetic could answer: an unlabelled dotted rectangle in an alarm colour arriving at the instant of
 * selection reads as *caused by* the selection, and a boundary that comes and goes with what you are
 * holding claims to be a limit on the **element** when it is a property of the **page**. The owner ruled
 * warn-only on 2026-08-13. `v21-bench.html:190` was amended first — the reveal rule now names `.guideV`
 * alone, and the cue is raised by `.keepclear.warn` — and this file follows the amended freeze.
 *
 * The gain is not only the confusion. A mark that appears *only* when something is wrong is a mark whose
 * alarm colour is finally telling the truth, and it is the reading [BP-4] asked for in the first place:
 * *"a gentle nudge only when text or faces cross it"*, explicitly *"not a visible pro grid"*.
 *
 * ### The warning is kept, and the freeze did not have one
 *
 * Ruled by the owner on 2026-08-13 — see [BenchStudio.KEEP_CLEAR_WARN_ALPHA], which also records how
 * OD-48 inverted §12.9's accessibility argument: the mark accepted **as decorative** below WCAG 1.4.11's
 * 3:1 floor is the one that was deleted, so this boundary now draws only its 3.66:1 state or nothing.
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
 * @param warn whether the geometry the maker is answerable for crosses this boundary — an in-flight
 *   gesture's live frame, or otherwise the selection's committed one ([BenchStudio.keepClearWarn], and
 *   OD-49 for why it is no longer *"an interaction in flight"*). The cue's only trigger since OD-48; false
 *   draws nothing at all.
 * @param panelWidthPt the document panel's width in points, so the inset is the engine's real safe
 *   area rather than a transcribed pixel count — see [BenchStudio]. **Kept over the freeze's flat
 *   `inset:14px`**: the prototype has one page size and the product has several, so a transcribed
 *   pixel count would draw a boundary that is not the printer's.
 */
@Composable
internal fun BenchKeepClear(
    warn: Boolean,
    panelWidthPt: Double,
    modifier: Modifier = Modifier,
) {
    val colors = ZinelyTheme.v21Colors
    val density = LocalDensity.current
    // `jam` is V2.1's answer to V2's `strawberryText`, and since OD-48 it is the cue's only colour —
    // `berry` drew the resting boundary that ruling deleted, and nothing here selects between them now.
    //
    // **`jam`, not `jamText`, and the reason is the island.** `sheetIslandV21` lights `jam` and does not
    // light `jamText` — so inside the sheet `jam` is the light `#CF4A28` in both themes, while `jamText`
    // would resolve to the *room's* value and paint dark-theme `#E4856D` on light paper: **2.26:1**, and
    // this whole ruling rests on the warning being the one mark that clears 3:1. A review caught it.
    // `jam` measures **3.66:1** at this alpha, in both themes, with no island change and no new
    // departure — and it is the correct token regardless, because this is a 1.5dp stroke and `jamText`
    // exists for text.
    val color = colors.jam
    // The frozen `opacity:0 → .9` fade, over the frozen `.18s`. ⚠ It no longer shares a trigger with
    // `EditorPagePreview`'s focus wash — before OD-48 both fired on the live selection, and the note here
    // was about the two arriving a frame apart. They are now unrelated events: the wash follows selection,
    // this follows a gesture crossing the boundary. The *duration* still matches, which is the freeze's,
    // and the easing still differs (the wash uses a bare `tween`) — a divergence a review flagged and one
    // that belongs to whoever owns that file.
    val target = if (warn) BenchStudio.KEEP_CLEAR_WARN_ALPHA else 0f
    // Through the motion seam, not around it: `standard` — the freeze names a bare `.18s` with no easing,
    // and this fade informs rather than settles — and it collapses to 0ms under reduced motion. The first
    // cut of this called `tween()` directly and C9's policy scan caught it, which is exactly what that
    // scan is for: a new animation is the easiest place in this codebase to silently drop the a11y
    // contract, because nothing about `animateFloatAsState` looks like a policy decision.
    val alpha by animateFloatAsState(
        targetValue = target,
        animationSpec = ZinelyTheme.v2Motion.standard(BenchFocusDimMillis),
        label = "keep-clear-reveal",
    )
    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag(BenchKeepClearTestTag)
            .drawBehind {
                if (alpha <= 0f) return@drawBehind
                val inset = BenchStudio.keepClearInsetPx(size.width, panelWidthPt)
                if (inset <= 0f || inset * 2f >= size.width || inset * 2f >= size.height) return@drawBehind
                val stroke = with(density) { BenchStudio.PageBorder.toPx() }
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
 * `.pagenum`: `"3 / 12"` at `.6rem` **`--ink-soft`**, bold, tabular figures, 9px from the right and
 * **6px from the bottom** of the sheet (`v21-bench.html:183-184`).
 *
 * ### Three changes, and one of them is a rule
 *
 *  - **`inkFaint` → `inkSoft`.** Not a preference. [ZinelyV21Colors.inkFaint] measures 3.04:1 on paper
 *    and **sets no text at all** in V2.1 — the palette's KDoc records that all 26 prototype uses of it
 *    as text, page numbers included, moved to `inkSoft`. The text ramp is two tiers, not three.
 *  - **Head → foot.** V2 sat this at `top:7px;right:10px`; V2.1 seats it at the sheet's foot, where a
 *    printed folio belongs.
 *  - **Tracking is gone, weight arrives.** V2 tracked `.04em`; V2.1 sets `font-weight:700` and
 *    `font-variant-numeric:tabular-nums` instead. Tabular figures are what stop the number shifting
 *    under the reader as the page count crosses a digit — the same job the tracking was doing badly.
 *
 * The **format** is frozen; the numbers are the document's. The frozen `12` belongs to
 * [D-030](../../../../../../../../../docs/design/V2-SPEC-DEFECTS.md#d-030) — a page count the product
 * cannot have — and OD-2 re-seated that capability beyond Phase C, so [pageCount] is read from the
 * document and never written as a constant.
 *
 * **Silent to assistive technology, deliberately.** [BenchPageNav] already announces the page
 * position ([benchPageLabel]) and is the control that can *change* it; this is the sheet's
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
    val colors = ZinelyTheme.v21Colors
    Text(
        text = "$pageNumber / $pageCount",
        color = colors.inkSoft,
        fontSize = BenchStudio.PageNumberSize,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.End,
        // `font-variant-numeric:tabular-nums`. Compose exposes this only as an OpenType feature tag;
        // there is no typed parameter for it.
        //
        // **`.copy` of the ambient style, NOT a fresh `TextStyle`.** `Text`'s `style` is the *base* that
        // the explicit parameters merge onto, so a bare `TextStyle(...)` silently discards everything
        // `LocalTextStyle.current` carries — here M3 `bodyLarge`'s `lineHeight` and `letterSpacing`,
        // which this composable inherited before. That is an undocumented layout change hiding inside a
        // one-word font feature, and it would have been baked into the re-recorded goldens. [TypeBar]
        // records the same hazard at its own `tnum` call site.
        style = LocalTextStyle.current.copy(fontFeatureSettings = "tnum"),
        modifier = modifier
            .padding(bottom = BenchStudio.PageNumberBottomInset, end = BenchStudio.PageNumberEndInset)
            .testTag(BenchPageNumberTestTag)
            .clearAndSetSemantics { },
    )
}
