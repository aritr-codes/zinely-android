package com.aritr.zinely.feature.editor

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.aritr.zinely.core.editor.EditorUiState
import com.aritr.zinely.core.editor.Interaction
import com.aritr.zinely.core.editor.LivePreview
import com.aritr.zinely.core.editor.LiveTransform
import com.aritr.zinely.core.model.DocumentDefaults
import com.aritr.zinely.core.model.PtSize
import com.aritr.zinely.core.render.SceneRenderer
import com.aritr.zinely.render.android.AssetBytesSource

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
 * @param live the ephemeral gesture accumulator for this frame, or `null` when no gesture is active.
 * @param modifier sized by the caller; both the preview and the chrome fill it so their device-px
 *   coordinates align.
 * @param imageBytes import-master byte source for image elements; defaults to the missing-asset placeholder.
 */
@Composable
public fun EditorPagePreview(
    uiState: EditorUiState,
    defaults: DocumentDefaults,
    pageSizePt: PtSize,
    live: LiveTransform?,
    modifier: Modifier = Modifier,
    imageBytes: AssetBytesSource = EmptyAssetBytes,
) {
    val page = uiState.document.pages[uiState.currentPageIndex]
    val interaction = uiState.interaction
    val screenPxPerPt = uiState.view.screenPxPerPt
    val pageOffset = uiState.view.pageOffset

    // Bake the live gesture into the selected transforms (same bake as commit) for an open session only.
    val effectivePage = if (interaction is Interaction.Transforming && live != null) {
        LivePreview.apply(page, interaction.before, live, screenPxPerPt.toDouble())
    } else {
        page
    }

    // Recomputed only when the effective page / defaults / size change — i.e. per frame during a drag
    // (effectivePage changes), never on unrelated recompositions.
    val tape = remember(effectivePage, defaults, pageSizePt) {
        SceneRenderer.render(effectivePage, pageSizePt, defaults)
    }
    val selectedTransforms = remember(effectivePage, uiState.selection) {
        effectivePage.elements.filter { it.id in uiState.selection }.map { it.transform }
    }

    Box(modifier = modifier) {
        PagePreview(
            tape = tape,
            sheet = pageSizePt,
            screenPxPerPt = screenPxPerPt,
            pageOffset = pageOffset,
            modifier = Modifier.fillMaxSize(),
            imageBytes = imageBytes,
        )
        SelectionChrome(
            transforms = selectedTransforms,
            screenPxPerPt = screenPxPerPt,
            pageOffset = pageOffset,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
