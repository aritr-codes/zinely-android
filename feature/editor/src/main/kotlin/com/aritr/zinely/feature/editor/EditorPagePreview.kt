package com.aritr.zinely.feature.editor

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.aritr.zinely.core.editor.EditorUiState
import com.aritr.zinely.core.editor.Interaction
import com.aritr.zinely.core.editor.LivePreview
import com.aritr.zinely.core.editor.LiveSnap
import com.aritr.zinely.core.editor.LiveTransform
import com.aritr.zinely.core.editor.SnapGuide
import com.aritr.zinely.core.model.DocumentDefaults
import com.aritr.zinely.core.model.Page
import com.aritr.zinely.core.model.PtPoint
import com.aritr.zinely.core.model.PtSize
import com.aritr.zinely.core.model.TextStyle
import com.aritr.zinely.core.model.Transform
import com.aritr.zinely.core.render.SceneRenderer
import com.aritr.zinely.render.android.AssetBytesSource
import com.aritr.zinely.render.android.SelectionChromeGeometry
import com.aritr.zinely.ui.theme.ZinelyTheme
import com.aritr.zinely.ui.theme.ZinelyV2Settle

/**
 * The S4 editor canvas (ADR-029 §5, selection-chrome increment): the stateless host that stacks the
 * page [PagePreview] under the [SelectionChrome] for the current page, applying the live gesture preview.
 *
 * **Live preview = document-order re-render** (Codex review, selection-chrome increment): during an open
 * [Interaction.Transforming] the selected transforms are baked through [LivePreview.apply] (the same
 * [LiveTransform.bake] the commit uses) and the whole page is re-rendered via the normal
 * [SceneRenderer] → [PagePreview] path — **not** a `graphicsLayer` over a cached layer. This makes the
 * live frame identical to the baked commit (preview == commit), keeps z-order correct, and applies the
 * per-element `MIN_SIZE_PT` clamp exactly. Per-frame re-render of a small zine page is pure point math +
 * tape replay; image bytes are cached in the replayer by asset key (the live transform never re-decodes).
 *
 * Stateless: all inputs are hoisted. The pointer-input gesture modifier ([editorTransformGestures]) and
 * the `live`/`onPreview` state it drives are owned by the editor screen that hosts this composable; this
 * host just renders a given [uiState] + [live] frame. Resize handles, snap guides, and the a11y
 * contextbar land in the following increments.
 *
 * @param uiState the history-free editor projection (document, selection, interaction, view).
 * @param defaults document defaults the renderer folds (background).
 * @param pageSizePt the edited page/panel size in points (also the page clip); hoisted (imposition owns
 *   the panel size — not derived here).
 * @param live the ephemeral pan/pinch/rotate accumulator for this frame, or `null` when inactive.
 * @param resizeOverride directly-baked transforms for an active **handle-resize** drag (opposite-anchor,
 *   §5.3), or `null`. When non-null it takes precedence over [live] (a handle drag is its own session).
 * @param styleOverride the in-flight text style of a settling Type-bar size burst (ADR-055), or `null`.
 *   Orthogonal to the transform overrides — composed on top of whichever of them is active, never
 *   competing with them.
 * @param modifier sized by the caller; both the preview and the chrome fill it so their device-px
 *   coordinates align.
 * @param imageBytes import-master byte source for image elements; defaults to the missing-asset placeholder.
 * @param hiddenElementId the element the host is drawing itself this frame — omitted from the tape so it
 *   is not painted twice. C3 uses it for the element under an open in-place text session (ADR-093 row
 *   3.11); `null` is the normal case. Affects the **render** only: selection chrome, snap guides and the
 *   focus scrim still read the full page, so a suppressed element keeps its outline and its hole.
 * @param deleting the element in C4's soft delete and how far through it is (`0f` .. `1f`), or `null`.
 *   Drives the frozen `scale(.9)` through the override seam and the fade through a paper cover — see
 *   [benchDeleteScaled] and [benchDeleteCovers] at the foot of this file for why the fade is a cover.
 */
@Composable
public fun EditorPagePreview(
    uiState: EditorUiState,
    defaults: DocumentDefaults,
    pageSizePt: PtSize,
    live: LiveTransform?,
    modifier: Modifier = Modifier,
    resizeOverride: Map<String, Transform>? = null,
    styleOverride: Map<String, TextStyle>? = null,
    imageBytes: AssetBytesSource = EmptyAssetBytes,
    hiddenElementId: String? = null,
    deleting: Pair<String, Float>? = null,
) {
    val page = uiState.document.pages[uiState.currentPageIndex]
    val interaction = uiState.interaction
    val screenPxPerPt = uiState.view.screenPxPerPt
    val pageOffset = uiState.view.pageOffset

    // Bake the active gesture into the selected transforms for an open session only. A handle-resize
    // override (directly-baked, opposite-anchor) wins over the pan/pinch LiveTransform path. The pan/pinch
    // path resolves through LiveSnap (§5.4) — the SAME call the gesture commit makes, so the snapped frame
    // shown here equals the committed transform (preview == commit), and its render-only guides are drawn.
    val effectivePage: Page
    val guides: List<SnapGuide>
    when {
        resizeOverride != null -> {
            effectivePage = LivePreview.applyOverride(page, resizeOverride)
            guides = emptyList()
        }
        interaction is Interaction.Transforming && live != null -> {
            val snap = LiveSnap.resolve(
                page = page,
                // The session's ids (== commit's `tx.ids`), NOT ambient selection, so preview == commit
                // even if selection churns mid-gesture (Codex rec #1).
                selection = interaction.ids,
                before = interaction.before,
                live = live,
                screenPxPerPt = screenPxPerPt.toDouble(),
                pageSize = pageSizePt,
                thresholdPt = LiveSnap.thresholdPt(screenPxPerPt.toDouble()),
            )
            effectivePage = LivePreview.applyOverride(page, snap.transforms)
            guides = snap.guides
        }
        else -> {
            effectivePage = page
            guides = emptyList()
        }
    }

    // A settling Type-bar size burst is orthogonal to the gesture overrides above — a burst can settle
    // while nothing is dragging, and a drag can run while one settles. So it composes on top of whichever
    // branch won rather than joining the `when`. This is the frozen `applyTextStyle` half of bench's
    // apply-now/commit-later split (ADR-055): the glyphs move with the readout, the undo entry still lands
    // once, on settle.
    val styledPage = styleOverride?.let { LivePreview.applyStyleOverride(effectivePage, it) } ?: effectivePage

    // C2a (ADR-091 §2.2): `@keyframes mat` fires on **insertion**. "Which element is new" is a remembered
    // diff of this page's element ids — no reducer state, no Intent, no model field, nothing to clear and
    // nothing that can reach a save. Same discipline D-032 forced on the keep-clear warn: transient
    // appearance is derived per frame. Reduced motion collapses it to 0ms (ADR-075) — it is a one-shot,
    // so it simply arrives.
    val reduceMotion = ZinelyTheme.motion.reduceMotion
    val pageKey = uiState.currentPageIndex
    val ids = page.elements.map { it.id }
    var seenIds by remember(pageKey) { mutableStateOf(ids.toSet()) }
    var arrivingId by remember(pageKey) { mutableStateOf<String?>(null) }
    val materialise = remember(pageKey) { Animatable(1f) }
    LaunchedEffect(pageKey, ids) {
        // An insertion is one new id AND nothing lost. Both clauses are load-bearing: a page whose whole
        // content is replaced can present one unfamiliar id while others vanish, and animating that would
        // say "this one is yours, just now" about a page the user did not build. The size check is what
        // rejects it — `fresh.singleOrNull()` alone cannot tell an arrival from a swap.
        //
        // What this deliberately cannot distinguish is an **undo that restores a single deleted element**,
        // or a redo of an insert: both present exactly one genuinely new id and both animate. Telling them
        // apart needs the reducer's intent, and reading that here would make this transient appearance
        // depend on history — the one thing D-032's discipline forbids. Recorded at ADR-091 row 2.9b.
        val fresh = ids.filterNot { it in seenIds }
        val grewByOne = ids.size == seenIds.size + 1
        seenIds = ids.toSet()
        val newId = fresh.singleOrNull()?.takeIf { grewByOne }
        if (newId != null && !reduceMotion) {
            arrivingId = newId
            materialise.snapTo(0f)
            materialise.animateTo(1f, tween(BenchMaterialiseMillis, easing = ZinelyV2Settle))
        }
        arrivingId = null
    }
    val arriving = arrivingId
    val materialiseProgress = if (arriving == null) 1f else materialise.value

    // The materialise scale rides applyOverride — the same seam the gesture preview and the commit use —
    // so the arriving element grows through the normal render path rather than through a graphicsLayer
    // that would have to be reconciled with it.
    val renderedPage = if (arriving == null || materialiseProgress >= 1f) {
        styledPage
    } else {
        val t = styledPage.elements.firstOrNull { it.id == arriving }?.transform
        if (t == null) {
            styledPage
        } else {
            LivePreview.applyOverride(
                styledPage,
                mapOf(arriving to BenchMaterialise.scaledAboutCentre(t, BenchMaterialise.scaleAt(materialiseProgress))),
            )
        }
    }

    // C3 (ADR-093 row 3.11): the element under an open text session is drawn by the in-place editing
    // field, so the tape must NOT also draw it — otherwise the same words appear twice, offset by
    // whatever the two text engines disagree about, which reads as the artifact duplicating itself.
    // Suppression happens here, at the tape, rather than by covering the box: a cover would have to be
    // opaque, and the page is not one flat colour behind it.
    val visiblePage0 = if (hiddenElementId == null) {
        renderedPage
    } else {
        renderedPage.copy(elements = renderedPage.elements.filterNot { it.id == hiddenElementId })
    }

    // C4 (ADR-094 row 4.13): the frozen soft delete shrinks the element to `scale(.9)` while it fades.
    // The scale rides `applyOverride` — the same seam the gesture preview, the commit and C2a's materialise
    // all use — rather than a graphicsLayer that would have to be reconciled with them. The fade is the
    // cover in [benchDeleteCovers]; see there for why it is a cover and not a per-element alpha.
    val visiblePage = benchDeleteScaled(deleting, visiblePage0)

    // Recomputed only when the effective page / defaults / size change — i.e. per frame during a drag
    // (effectivePage changes) or per step during a size burst, never on unrelated recompositions.
    val tape = remember(visiblePage, defaults, pageSizePt) {
        SceneRenderer.render(visiblePage, pageSizePt, defaults)
    }
    val selectedTransforms = remember(effectivePage, uiState.selection) {
        effectivePage.elements.filter { it.id in uiState.selection }.map { it.transform }
    }

    // C2a (ADR-091 §2.1): `.content.focusing` is on whenever a selection is live (v2-bench.html `:513`,
    // cleared at `:521`). The dim is a paper wash with the selection punched out — see [BenchFocusScrim]
    // for why it is a composite and not a per-element alpha.
    val selected = selectedTransforms.isNotEmpty()
    val dimAlpha by animateFloatAsState(
        targetValue = if (selected) BenchFocusDimAlpha else 0f,
        animationSpec = tween(if (reduceMotion) 0 else BenchFocusDimMillis),
        label = "bench-focus-dim",
    )
    // The frozen `.sel{transition:opacity .12s}` (row 2.3). A transition needs something to transition
    // *from*, so the last non-empty selection is retained and keeps being drawn while the fade runs down
    // — otherwise deselection has nothing left to fade and the outline snaps out instead.
    val chromeAlpha by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = tween(if (reduceMotion) 0 else BenchChromeFadeMillis),
        label = "bench-chrome-fade",
    )
    var lastSelected by remember { mutableStateOf(selectedTransforms) }
    if (selected) lastSelected = selectedTransforms
    val chromeTransforms = if (selected) selectedTransforms else lastSelected

    val holes = remember(selectedTransforms, screenPxPerPt, pageOffset) {
        selectedTransforms.map { SelectionChromeGeometry.outlineDevicePx(it, screenPxPerPt.toDouble(), pageOffset) }
    }
    val pageRect = remember(screenPxPerPt, pageOffset, pageSizePt) {
        benchPageRect(screenPxPerPt, pageOffset, pageSizePt)
    }
    val covers = if (arriving == null || materialiseProgress >= 1f) {
        emptyList()
    } else {
        val t = renderedPage.elements.firstOrNull { it.id == arriving }?.transform
        if (t == null) {
            emptyList()
        } else {
            listOf(
                SelectionChromeGeometry.outlineDevicePx(t, screenPxPerPt.toDouble(), pageOffset) to
                    BenchMaterialise.coverAlphaAt(materialiseProgress),
            )
        }
    } + benchDeleteCovers(deleting, renderedPage, screenPxPerPt, pageOffset)

    Box(modifier = modifier) {
        PagePreview(
            tape = tape,
            sheet = pageSizePt,
            screenPxPerPt = screenPxPerPt,
            pageOffset = pageOffset,
            modifier = Modifier.fillMaxSize(),
            imageBytes = imageBytes,
        )
        BenchFocusScrim(
            // The island `--paper` (ADR-090), so `0.4·element + 0.6·paper` is the sheet's own paper in
            // both themes — the arithmetic the dim rests on holds at night too.
            paper = ZinelyTheme.v2Colors.paper,
            // Bounded to the sheet: this composable fills the canvas so its coordinates agree with its
            // siblings', but a wash that filled the canvas would paint paper over the *desk* and bleach
            // the room on every selection — see [BenchFocusScrim].
            pageRect = pageRect,
            dimAlpha = dimAlpha,
            holes = holes,
            covers = covers,
            modifier = Modifier.fillMaxSize(),
        )
        SnapGuides(
            guides = guides,
            screenPxPerPt = screenPxPerPt,
            pageOffset = pageOffset,
            pageSizePt = pageSizePt,
            modifier = Modifier.fillMaxSize(),
        )
        SelectionChrome(
            transforms = chromeTransforms,
            screenPxPerPt = screenPxPerPt,
            pageOffset = pageOffset,
            modifier = Modifier.fillMaxSize(),
            alpha = chromeAlpha,
        )
    }
}

/**
 * The soft-deleting element, shrunk about its own centre through the `applyOverride` seam.
 *
 * Pure, so the arithmetic is unit-testable without a composition — the house rule this module keeps for
 * exactly the geometry that is otherwise only checkable by eye.
 */
internal fun benchDeleteScaled(deleting: Pair<String, Float>?, page: Page): Page {
    if (deleting == null) return page
    val (id, progress) = deleting
    val t = page.elements.firstOrNull { it.id == id }?.transform ?: return page
    return LivePreview.applyOverride(
        page,
        mapOf(id to BenchMaterialise.scaledAboutCentre(t, BenchMaterialise.deleteScaleAt(progress))),
    )
}

/**
 * The paper cover that fades **in** over the soft-deleting element — C2a's materialise technique reversed.
 * Empty when nothing is being deleted, so the common path allocates nothing.
 */
internal fun benchDeleteCovers(
    deleting: Pair<String, Float>?,
    page: Page,
    screenPxPerPt: Float,
    pageOffset: PtPoint,
): List<Pair<List<PtPoint>, Float>> {
    if (deleting == null) return emptyList()
    val (id, progress) = deleting
    val t = page.elements.firstOrNull { it.id == id }?.transform ?: return emptyList()
    // The cover tracks the SHRINKING box, not the resting one: a cover at the original size would sit
    // proud of the element it is hiding and read as a growing white rectangle rather than a fading one.
    val scaled = BenchMaterialise.scaledAboutCentre(t, BenchMaterialise.deleteScaleAt(progress))
    return listOf(
        SelectionChromeGeometry.outlineDevicePx(scaled, screenPxPerPt.toDouble(), pageOffset) to
            BenchMaterialise.deleteCoverAlphaAt(progress),
    )
}
