package com.aritr.zinely.core.editor

import com.aritr.zinely.core.model.DecorElement
import com.aritr.zinely.core.model.Element
import com.aritr.zinely.core.model.ImageElement
import com.aritr.zinely.core.model.TextElement
import com.aritr.zinely.core.model.Transform
import com.aritr.zinely.core.model.ZineDocument
import kotlin.math.abs

/**
 * What one history step did, in the maker's terms rather than the command's
 * ([ADR-123](../../../../../../../../docs/DECISIONS.md#adr-123), frozen `v21-bench.html` A26).
 *
 * Derived, never stored: [History] and `committing()` are unchanged and no label is persisted. The words
 * live in `Copy.Undo`; this is only the meaning they are chosen from.
 *
 * @property kind the thing acted on, or `null` for a verb the copy names without one (stacking, words,
 *   text style, copier, flip, ink, across fold, framing).
 * @property count how many things the step held; the copy says "N things" above one.
 */
public data class EditLabel(val verb: EditVerb, val kind: EditKind?, val count: Int = 1)

/** One row of A26's table. A toggle carries the state the command set (`_ON` = the command turned it on). */
public enum class EditVerb {
    DELETE, PLACE, MOVE, RESIZE, TURN, RESTACK,
    WORDS,

    /** Typing into a box that had no words: Add Text's second step (A26 "Words taken off"). */
    WORDS_FROM_EMPTY,
    TEXT_STYLE, SWAP,
    COPIER_ON, COPIER_OFF,
    FLIP_LEFT_RIGHT_ON, FLIP_LEFT_RIGHT_OFF, FLIP_TOP_BOTTOM_ON, FLIP_TOP_BOTTOM_OFF,
    INK, SPREAD, FRAMING,
}

/** [EMPTY_BOX] is Add Text's first step, a text box placed with no words (A26 "Empty box taken off"). */
public enum class EditKind { TEXT, PHOTO, ART, EMPTY_BOX }

/**
 * A26 D4 = 4c: a size change at or below 2% and a turn at or below 2° are incidental slips (a handle resize
 * moves the centre; two fingers pick up stray zoom and turn). Comparisons are strict `>`.
 */
internal const val TRANSFORM_SIZE_SLIP: Double = 0.02
internal const val TRANSFORM_TURN_SLIP_DEGREES: Double = 2.0

/**
 * The label of this command, read from its own memento. [doc] is the document **before** the history step
 * (either direction); it is consulted only where the command carries ids and not elements (Transform).
 *
 * `null` only for the page commands, which no Bench control raises: A26 gives page undo no line, and before
 * A26 their undo was silent too.
 */
public fun Command.editLabel(doc: ZineDocument): EditLabel? = when (this) {
    is DeleteCommand -> removed.map { it.second }.let { EditLabel(EditVerb.DELETE, kindOf(it.first()), it.size) }
    is PlaceCommand -> EditLabel(
        EditVerb.PLACE,
        if (element is TextElement && element.text.isBlank()) EditKind.EMPTY_BOX else kindOf(element),
    )
    is TransformCommand -> EditLabel(
        transformVerb(before, after),
        doc.pages.getOrNull(pageIndex)?.elements?.firstOrNull { it.id in after }?.let(::kindOf)
            // ponytail: an id the page no longer holds; `benchDeleteLabel` keeps Photo for the same unknown.
            ?: EditKind.PHOTO,
        after.size,
    )
    is ReorderCommand -> EditLabel(EditVerb.RESTACK, null)
    is EditTextCommand -> when {
        before.text == after.text -> EditLabel(EditVerb.TEXT_STYLE, null)
        // A26: words and style together say the words.
        before.text.isBlank() -> EditLabel(EditVerb.WORDS_FROM_EMPTY, null)
        else -> EditLabel(EditVerb.WORDS, null)
    }
    is EditImageCommand -> when {
        before.assetId != after.assetId -> EditLabel(EditVerb.SWAP, EditKind.PHOTO)
        before.copier != after.copier ->
            EditLabel(if (after.copier) EditVerb.COPIER_ON else EditVerb.COPIER_OFF, null)
        before.flippedHorizontally != after.flippedHorizontally -> EditLabel(
            if (after.flippedHorizontally) EditVerb.FLIP_LEFT_RIGHT_ON else EditVerb.FLIP_LEFT_RIGHT_OFF, null,
        )
        before.flippedVertically != after.flippedVertically -> EditLabel(
            if (after.flippedVertically) EditVerb.FLIP_TOP_BOTTOM_ON else EditVerb.FLIP_TOP_BOTTOM_OFF, null,
        )
        // Crop/fit: a Reframe or a Reset. D2 = 2a: never "reset" because it looks like one.
        else -> EditLabel(EditVerb.FRAMING, null)
    }
    is EditDecorCommand -> when {
        // A26: Replace also refits the box; the swap outranks the refit.
        before.supplyId != after.supplyId -> EditLabel(EditVerb.SWAP, EditKind.ART)
        before.mirrored != after.mirrored ->
            EditLabel(if (after.mirrored) EditVerb.FLIP_LEFT_RIGHT_ON else EditVerb.FLIP_LEFT_RIGHT_OFF, null)
        before.flippedVertically != after.flippedVertically -> EditLabel(
            if (after.flippedVertically) EditVerb.FLIP_TOP_BOTTOM_ON else EditVerb.FLIP_TOP_BOTTOM_OFF, null,
        )
        // Ink is the only other field an EditDecorCommand is raised for (`inkSupply`).
        else -> EditLabel(EditVerb.INK, null)
    }
    is MakeImageSpreadCommand -> EditLabel(EditVerb.SPREAD, EditKind.PHOTO)
    is AddPageCommand, is DeletePageCommand -> null
}

private fun kindOf(element: Element): EditKind = when (element) {
    is TextElement -> EditKind.TEXT
    is ImageElement -> EditKind.PHOTO
    is DecorElement -> EditKind.ART
}

/**
 * A26 D4 = 4c, over every element the step moved: the largest size change past 2% resizes; else the
 * largest turn past 2° turns; else it moved. Size is the larger relative change of width or height against
 * the pre-action box; turn is the shorter way round. Both are magnitudes, so a shrink or a counter-clockwise
 * turn counts the same as its opposite.
 */
internal fun transformVerb(before: Map<String, Transform>, after: Map<String, Transform>): EditVerb {
    val pairs = after.mapNotNull { (id, a) -> before[id]?.let { it to a } }
    val size = pairs.maxOfOrNull { (b, a) -> maxOf(abs(a.widthPt - b.widthPt) / b.widthPt, abs(a.heightPt - b.heightPt) / b.heightPt) } ?: 0.0
    val turn = pairs.maxOfOrNull { (b, a) -> abs(a.rotationDegrees - b.rotationDegrees).mod(360.0).let { minOf(it, 360.0 - it) } } ?: 0.0
    return when {
        size > TRANSFORM_SIZE_SLIP -> EditVerb.RESIZE
        turn > TRANSFORM_TURN_SLIP_DEGREES -> EditVerb.TURN
        else -> EditVerb.MOVE
    }
}
