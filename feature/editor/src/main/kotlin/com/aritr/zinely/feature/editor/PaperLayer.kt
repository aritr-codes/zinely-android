package com.aritr.zinely.feature.editor

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.aritr.zinely.core.copy.Copy
import com.aritr.zinely.core.data.repository.PaperColour
import java.util.Locale

/**
 * The paper the open zine is loaded on (AW6, [ADR-150](../../../../../../docs/DECISIONS.md#adr-150) decision 5).
 *
 * Provided by the host around the Bench and inside [ProofScreen]; read only by the surfaces AW6.7 names: the Bench
 * canvas ([EditorPagePreview]), the Bench page strip and grid ([BenchPageMiniature]), the Proof stage's leaf and Print
 * details' sheet picture. **Not** the fold guide (AW6-P.6) and not a Shelf cover (AW6.7). Nothing that writes a PDF or
 * PNG reads it: the export path lives in `:render-android`, which cannot see [PaperColour] at all.
 */
public val LocalPaperColour: ProvidableCompositionLocal<PaperColour> = compositionLocalOf { PaperColour.WHITE }

/**
 * The host's paper layer: ONE rectangle of [paper] over [page], multiplied, drawn **after** the replayed page and
 * **before** any chrome. Multiply commutes, so this is AW6.8's "page multiplied onto the paper", and it needs no
 * offscreen layer (R41.5). White draws nothing, so a White zine's pixels are untouched.
 *
 * Invariant: the destination under [page] must already be the opaque page on a WHITE ground (the export's ground).
 * The replayer draws no ground of its own, so each host lays white under a coloured page first ([PagePreview],
 * [ImposedSheet]); over the app's warm page paper the result would be paper over cream, and over a transparent
 * destination multiply would punch through rather than tint.
 */
internal fun DrawScope.drawPaperLayer(paper: PaperColour, page: Rect) {
    val colour = paper.overlay ?: return
    drawRect(colour.toComposeColor(), topLeft = Offset(page.left, page.top), size = page.size, blendMode = BlendMode.Multiply)
}

/** The paper's name as a list shows it ("Pastel yellow"). Exhaustive, so a new paper cannot ship unnamed. */
internal val PaperColour.displayName: String
    get() = when (this) {
        PaperColour.WHITE -> Copy.PaperColour.WHITE
        PaperColour.IVORY -> Copy.PaperColour.IVORY
        PaperColour.PASTEL_YELLOW -> Copy.PaperColour.PASTEL_YELLOW
        PaperColour.PASTEL_PINK -> Copy.PaperColour.PASTEL_PINK
        PaperColour.PASTEL_BLUE -> Copy.PaperColour.PASTEL_BLUE
        PaperColour.PASTEL_GREEN -> Copy.PaperColour.PASTEL_GREEN
        PaperColour.GREY -> Copy.PaperColour.GREY
    }

/** Inside a sentence the name is lower-case ("Load pastel yellow paper", AW6.13). */
internal val PaperColour.sentenceName: String get() = displayName.lowercase(Locale.ROOT)

/** The note a paper carries in the chooser, or `null` (AW6.5: only White has one). */
internal val PaperColour.note: String?
    get() = if (this == PaperColour.WHITE) Copy.PaperColour.WHITE_NOTE else null
