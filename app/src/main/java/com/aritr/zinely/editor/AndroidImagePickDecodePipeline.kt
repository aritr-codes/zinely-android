package com.aritr.zinely.editor

import android.net.Uri
import com.aritr.zinely.core.data.asset.AssetStore
import com.aritr.zinely.core.data.repository.DataResult
import com.aritr.zinely.core.model.ImageElement
import com.aritr.zinely.core.model.PtSize
import com.aritr.zinely.core.model.Transform
import com.aritr.zinely.feature.editor.ImagePickDecodePipeline
import com.aritr.zinely.feature.editor.ImagePickResult
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/**
 * The production [ImagePickDecodePipeline] (ADR-031 §5) — replaces `UnavailableImagePipeline`. Honours
 * the editor's `Effect.PickAndDecodeImage → pickAndDecode()` seam: it **launches the system picker on
 * [main]** (Codex RF1 — `ActivityResultLauncher.launch` is main-thread-only), then decodes and stores
 * the master on [io]. A cancel returns [ImagePickResult.Cancelled] (the expected non-error exit); a
 * decode or store failure returns [ImagePickResult.Failure] (wired to the editor's announce path), so
 * the add-image flow never silently no-ops.
 *
 * The reducer mints the element id + z-index and keeps the supplied [Transform], so this pipeline
 * supplies only the placement (centered, aspect-correct — [defaultImagePlacement]).
 */
public class AndroidImagePickDecodePipeline(
    private val picker: PhotoPicker<Uri>,
    private val decoder: ImportMasterDecoder,
    private val assetStore: AssetStore,
    private val io: CoroutineDispatcher,
    private val main: CoroutineDispatcher,
    private val pageSizePt: PtSize,
) : ImagePickDecodePipeline {

    override suspend fun pickAndDecode(): ImagePickResult {
        val uri = withContext(main) { picker.await() } ?: return ImagePickResult.Cancelled
        val master = withContext(io) { decoder.decodeToMaster(uri) }
            ?: return ImagePickResult.Failure("That image couldn’t be added.")
        return when (val stored = withContext(io) { assetStore.store(master.bytes) }) {
            is DataResult.Success -> ImagePickResult.Success(
                ImageElement(
                    id = "", // placeholder — the reducer re-mints the id (EditorReducer.CommitAddImage)
                    transform = defaultImagePlacement(master.widthPx, master.heightPx, pageSizePt),
                    assetId = stored.value.hex,
                ),
            )
            is DataResult.Failure -> ImagePickResult.Failure("Couldn’t save that image.")
        }
    }
}

/**
 * A centered default placement for a newly-imported image (ADR-031 §5): an aspect-correct box bounded
 * to [FRACTION] of each page edge, centered on the page. Pure — unit-tested.
 */
internal fun defaultImagePlacement(masterWidthPx: Int, masterHeightPx: Int, pageSizePt: PtSize): Transform {
    val aspect = if (masterHeightPx > 0) masterWidthPx.toDouble() / masterHeightPx else 1.0
    val maxW = pageSizePt.width * FRACTION
    val maxH = pageSizePt.height * FRACTION
    // Fit the aspect box inside (maxW × maxH), preserving aspect (contain).
    var w = maxW
    var h = w / aspect
    if (h > maxH) {
        h = maxH
        w = h * aspect
    }
    val x = (pageSizePt.width - w) / 2.0
    val y = (pageSizePt.height - h) / 2.0
    return Transform(xPt = x, yPt = y, widthPt = w, heightPt = h)
}

/** New images occupy ≤ 60 % of each page edge, leaving room to reposition. */
private const val FRACTION = 0.6
