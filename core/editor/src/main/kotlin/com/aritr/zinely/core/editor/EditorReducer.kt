package com.aritr.zinely.core.editor

import com.aritr.zinely.core.model.Crop
import com.aritr.zinely.core.model.DecorElement
import com.aritr.zinely.core.model.Fit
import com.aritr.zinely.core.model.ImageElement
import com.aritr.zinely.core.model.Page
import com.aritr.zinely.core.model.PageRole
import com.aritr.zinely.core.model.PtPoint
import com.aritr.zinely.core.model.TextElement
import com.aritr.zinely.core.model.Transform

/**
 * The pure MVI reducer (ADR-029 §2). `reduce(model, intent)` is total, synchronous, and side-effect-free:
 * I/O is **returned** as [Effect]s (autosave, image decode, a11y announce), never performed. Determinism is
 * preserved without a clock/RNG — element ids and session tokens both draw from [EditorModel.nextToken].
 *
 * ### How [com.aritr.zinely.core.model.DecorElement] routes through here (ADR-105 / SUPPLIES-SPEC §2)
 *
 * This file carries **ten** type-switch sites — the largest concentration in the codebase, and the one
 * SUPPLIES-SPEC §10 omits from both S2′ and S7′ (corrected by
 * [D-029's 2026-08-16 ruling](../../../../../../../docs/design/V2-SPEC-DEFECTS.md#d-029-ruling-2026-08-16)).
 * Only one of them is an exhaustive `when` (the `DoubleTapAt` seam); the other nine are `as?` casts, so
 * **the compiler would not have found them.** They fall into two groups, and the split is the invariant:
 *
 * - **Type-agnostic verbs — decor is a first-class citizen.** `Nudge`, `ScaleBy`, `RotateBy`,
 *   `BeginTransform`/`CommitTransform`, `Reorder`, `Delete` and undo/redo never name an element type;
 *   they go through `Element.withTransform` / `Element.withZIndex` ([Elements.kt]), whose exhaustive
 *   `when`s gained real decor arms. A supply moves, resizes, rotates, restacks, deletes and undoes
 *   exactly as a photo does.
 * - **Type-specific verbs — decor is a silent no-op, and that is correct.** `StyleText`,
 *   `BeginEditText`/`CommitText`, `ReplaceImage`, `ResetFraming`, `ToggleCopier`, `BeginReframe` each
 *   resolve their target with `as? TextElement` / `as? ImageElement`, so a decor id yields `null` and
 *   the existing "absent ⇒ no-op" branch runs. These are left as casts on purpose: adding a decor arm
 *   would mean inventing a behaviour the spec does not give them.
 *
 * **Placement landed with S7** ([Intent.PlaceSupply]) — a supply now enters the document the same way a
 * text box does, and **both of decor's *editing* verbs (SUPPLIES-SPEC §8) have now landed** —
 * [Intent.InkSupply] and [Intent.ReplaceSupply], each reducing to exactly one [EditDecorCommand]. Decor is
 * no longer a second-class element in this file: it has placement, both type-specific verbs, and every
 * shared verb.
 *
 * ⚠ Note the asymmetry with the paragraph above: `StyleText` keeps its `as? TextElement` cast *without* a
 * decor arm, because a supply has no size/align/bold/italic. Change ink is a **separate intent**, not a
 * widening of that one — see [Intent.InkSupply] for why that is the cheaper of the two shapes.
 */
public object EditorReducer {

    public fun reduce(model: EditorModel, intent: Intent): Reduction = when (intent) {
        // — selection (no document mutation ⇒ no autosave) —
        is Intent.Select -> Reduction(model.copy(selection = setOfNotNull(intent.id)))
        is Intent.SelectAt -> {
            val hit = HitTest.topmostAt(currentPage(model), intent.pagePoint)
            Reduction(model.copy(selection = setOfNotNull(hit)))
        }
        Intent.ClearSelection -> Reduction(model.copy(selection = emptySet()))

        // — placement / content —
        is Intent.PlaceText -> {
            val id = "el-${model.nextToken}"
            val el = TextElement(id = id, transform = intent.transform, zIndex = nextZ(model), text = intent.text)
            committing(model.copy(nextToken = model.nextToken + 1, selection = setOf(id)),
                PlaceCommand(model.currentPageIndex, el))
        }
        is Intent.PlaceTextAndEdit -> {
            val id = "el-${model.nextToken}"
            val element = TextElement(
                id = id,
                transform = intent.transform,
                zIndex = nextZ(model),
                text = "",
            )
            val placed = committing(
                model.copy(nextToken = model.nextToken + 1, selection = setOf(id)),
                PlaceCommand(model.currentPageIndex, element),
            )
            val editing = openTextSession(placed.model, id)
            Reduction(editing.model, placed.effects + editing.effects)
        }
        Intent.RequestAddImage -> Reduction(model, listOf(Effect.PickAndDecodeImage))
        is Intent.CommitAddImage -> {
            // Mint the id reducer-side (single source of id allocation) so it can never collide with an
            // existing element — a duplicate id would make PlaceCommand.invertOn delete BOTH matches.
            // That holds only because `nextToken` starts past every id the document already carries
            // (EditorModel.firstFreeToken); a constant seed silently broke it on every reopen.
            val id = "el-${model.nextToken}"
            // Placement-default policy (ADR-053 §2/§3): a newly placed photo covers its frame — Fit.FILL,
            // full crop — applied HERE at placement time, not as a render-time default flip. The model's
            // fallback stays Fit.FIT (Document.kt) so every already-saved zine renders byte-identically.
            val placed = intent.element.copy(id = id, zIndex = nextZ(model), fit = Fit.FILL)
            committing(model.copy(nextToken = model.nextToken + 1, selection = setOf(id)),
                PlaceCommand(model.currentPageIndex, placed))
        }
        is Intent.PlaceSupply -> {
            val id = "el-${model.nextToken}"
            val el = DecorElement(
                id = id,
                transform = intent.transform,
                zIndex = nextZ(model),
                supplyId = intent.supplyId,
                ink = intent.ink,
            )
            committing(
                model.copy(nextToken = model.nextToken + 1, selection = setOf(id)),
                PlaceCommand(model.currentPageIndex, el),
            )
        }
        is Intent.BeginEditText -> openTextSession(model, intent.id)
        is Intent.BeginEditTextAt -> openTextSession(model, HitTest.topmostAt(currentPage(model), intent.pagePoint))
        is Intent.CommitText -> {
            val tx = model.interaction as? Interaction.EditingText
            // Stale / mismatched commit (nav, cancel, or a newer session replaced ours) ⇒ no-op (D5).
            if (tx == null || tx.token != intent.token || tx.id != intent.id) Reduction(model)
            else endTextSession(model, intent.id, intent.after)
        }
        is Intent.CancelText -> {
            val tx = model.interaction as? Interaction.EditingText
            if (tx == null || tx.token != intent.token || tx.id != intent.id) Reduction(model)
            else endTextSession(model, intent.id, after = null)
        }
        is Intent.StyleText -> styleText(model, intent)
        is Intent.InkSupply -> inkSupply(model, intent)
        is Intent.ReplaceSupply -> replaceSupply(model, intent)

        // — double-tap seam: retarget by topmost element type (ADR-053 §4) —
        is Intent.DoubleTapAt -> when (val hit = HitTest.topmostAt(currentPage(model), intent.pagePoint)
            ?.let { id -> currentPage(model).elements.firstOrNull { it.id == id } }) {
            is TextElement -> openTextSession(model, hit.id)
            is ImageElement -> openReframeSession(model, hit.id)
            // Named, not left to the `else`: a supply has nothing inside it to open — no text to edit and
            // no framing to adjust — so double-tap on decor is a **deliberate** no-op, not an unhandled
            // case. (`Replace supply` is a sheet, not an in-place session; SUPPLIES-SPEC §7/S7.) The arm
            // exists so the next reader sees a decision rather than a fallthrough.
            is DecorElement -> Reduction(model)
            else -> Reduction(model) // empty space (null hit) ⇒ no-op
        }

        // — image reframe: begin/commit/cancel session + replace/reset one-shots (ADR-053) —
        is Intent.BeginReframe -> openReframeSession(model, intent.id)
        is Intent.CommitReframe -> {
            val rx = model.interaction as? Interaction.Reframing
            // Stale / mismatched commit (nav, cancel, or a newer session replaced ours) ⇒ no-op.
            if (rx == null || rx.token != intent.token || rx.id != intent.id || rx.pageIndex != model.currentPageIndex) {
                Reduction(model)
            } else if (currentPage(model).elements.none { it.id == rx.id }) {
                // The element was deleted mid-session (Delete doesn't close the interaction) ⇒ just close,
                // never push a command that matches no id (mirrors endTextSession's vanished-element bail).
                Reduction(model.copy(interaction = Interaction.Idle))
            } else {
                // Take ONLY crop/fit from the draft (clamped valid); keep before's assetId/geometry/zIndex so a
                // malformed draft can neither swap the photo nor move the element (mirrors EditTextCommand).
                val committed = rx.before.copy(crop = FramingMath.clampCrop(intent.after.crop), fit = intent.after.fit)
                val idle = model.copy(interaction = Interaction.Idle)
                // No change ⇒ close, no command/autosave. Compared through `sameFraming` and NOT with `==`:
                // the crop arriving here has been round-tripped through a zoom (`seedDraft`→`resolveCrop`)
                // and lands 1–2 ULP from the one on disk, so `==` recorded an undo step and an autosave for
                // 56 % of sessions in which the maker touched nothing (D-097). `committed` differs from
                // `rx.before` only in crop/fit by construction, so comparing those two is the whole test.
                if (FramingMath.sameFraming(committed.crop, committed.fit, rx.before.crop, rx.before.fit)) {
                    Reduction(idle)
                } else {
                    committing(idle, EditImageCommand(rx.pageIndex, rx.id, rx.before, committed))
                }
            }
        }
        is Intent.CancelReframe -> {
            val rx = model.interaction as? Interaction.Reframing
            if (rx == null || rx.token != intent.token) Reduction(model)
            else Reduction(model.copy(interaction = Interaction.Idle))
        }
        is Intent.ReplaceImage -> {
            val el = currentPage(model).elements.firstOrNull { it.id == intent.id } as? ImageElement
            // Preserve framing (crop/fit/transform/zIndex) — only the bytes change (ADR-053 §6).
            if (el == null || el.assetId == intent.assetId) Reduction(model)
            else committing(model, EditImageCommand(model.currentPageIndex, el.id, el, el.copy(assetId = intent.assetId)))
        }
        is Intent.ResetFraming -> {
            val el = currentPage(model).elements.firstOrNull { it.id == intent.id } as? ImageElement
            val reset = el?.copy(crop = Crop.FULL, fit = Fit.FILL)
            if (el == null || reset == el) Reduction(model) // absent or already at default ⇒ no-op
            else committing(model, EditImageCommand(model.currentPageIndex, el.id, el, reset!!))
        }
        is Intent.ToggleCopier -> {
            val el = currentPage(model).elements.firstOrNull { it.id == intent.id } as? ImageElement
            // One undoable command, and its own inverse — the filter is a flag, so "turn it off" is the
            // same edit run again (ADR-106). A missing id or a text element is a no-op, never a throw.
            if (el == null) Reduction(model)
            else committing(model, EditImageCommand(model.currentPageIndex, el.id, el, el.copy(copier = !el.copier)))
        }
        is Intent.MakeImageSpread -> {
            val sourcePage = currentPage(model)
            val source = sourcePage.elements.firstOrNull { it.id == intent.id } as? ImageElement
            val pair = imageSpreadPair(model.currentPageIndex)
            val pageAspect = intent.pageSizePt.width / intent.pageSizePt.height
            val crops = imageSpreadCrops(intent.photoAspect, pageAspect)
            val partnerPage = pair?.partnerPageIndex?.let(model.document.pages::getOrNull)
            if (source == null || pair == null || crops == null || partnerPage == null) {
                Reduction(model)
            } else {
                val (leftCrop, rightCrop) = crops
                val fullPage = Transform(
                    xPt = 0.0,
                    yPt = 0.0,
                    widthPt = intent.pageSizePt.width,
                    heightPt = intent.pageSizePt.height,
                )
                val sourceBack = (sourcePage.elements.minOfOrNull { it.zIndex } ?: 0).let {
                    if (it == Int.MIN_VALUE) it else it - 1
                }
                val partnerBack = (partnerPage.elements.minOfOrNull { it.zIndex } ?: 0).let {
                    if (it == Int.MIN_VALUE) it else it - 1
                }
                val sourceCrop = if (pair.sourceIsLeft) leftCrop else rightCrop
                val partnerCrop = if (pair.sourceIsLeft) rightCrop else leftCrop
                val sourceAfter = source.copy(
                    transform = fullPage,
                    zIndex = sourceBack,
                    crop = sourceCrop,
                    fit = Fit.FIT,
                )
                val partner = source.copy(
                    id = "el-${model.nextToken}",
                    transform = fullPage,
                    zIndex = partnerBack,
                    crop = partnerCrop,
                    fit = Fit.FIT,
                )
                committing(
                    model.copy(nextToken = model.nextToken + 1, selection = setOf(source.id)),
                    MakeImageSpreadCommand(
                        sourcePageIndex = model.currentPageIndex,
                        sourceId = source.id,
                        beforeSource = source,
                        afterSource = sourceAfter,
                        partnerPageIndex = pair.partnerPageIndex,
                        partner = partner,
                    ),
                )
            }
        }

        // — transform: begin/commit/cancel + a11y twins —
        is Intent.BeginTransform -> {
            val before = transformsOf(model, intent.ids)
            if (before.isEmpty()) Reduction(model) else Reduction(
                model.copy(
                    selection = intent.ids,
                    nextToken = model.nextToken + 1,
                    interaction = Interaction.Transforming(model.currentPageIndex, intent.ids, before, model.nextToken),
                ),
            )
        }
        is Intent.CommitTransform -> {
            val tx = model.interaction as? Interaction.Transforming
            if (tx == null || tx.token != intent.token || tx.pageIndex != model.currentPageIndex) {
                Reduction(model) // stale / mismatched commit ⇒ no-op (required-fix #1)
            } else {
                // Keep only ids the session actually snapshotted, so the command stays fully invertible
                // (a foreign id in `after` would have no `before` entry to restore).
                val after = intent.after.filterKeys { it in tx.before }
                committing(model.copy(interaction = Interaction.Idle),
                    TransformCommand(tx.pageIndex, tx.before, after))
            }
        }
        is Intent.CancelTransform -> {
            val tx = model.interaction as? Interaction.Transforming
            // Stale / mismatched cancel (a newer session replaced ours) ⇒ no-op, so it can't wipe a live one.
            if (tx == null || tx.token != intent.token) Reduction(model)
            else Reduction(model.copy(interaction = Interaction.Idle))
        }

        // Display-only viewport update: no autosave, no history, and selection/interaction untouched so a
        // resize/rotation mid-session can't disturb an open gesture. Idempotent — equal view ⇒ no-op.
        is Intent.SetViewport -> {
            val next = ViewState(intent.screenPxPerPt, intent.pageOffset)
            if (next == model.view) Reduction(model) else Reduction(model.copy(view = next))
        }
        is Intent.Nudge -> bakeSelection(model) { it.copy(xPt = it.xPt + intent.deltaPt.x, yPt = it.yPt + intent.deltaPt.y) }
        is Intent.ScaleBy -> bakeSelection(model) { TransformMath.bakeCentreAnchored(it, PtPoint(0.0, 0.0), intent.factor, 0.0) }
        is Intent.RotateBy -> bakeSelection(model) { it.copy(rotationDegrees = it.rotationDegrees + intent.degrees) }

        // — structure —
        is Intent.Reorder -> {
            val page = currentPage(model)
            val beforeZ = page.elements.associate { it.id to it.zIndex }
            val afterZ = ZOrder.reorder(page, intent.id, intent.op).elements.associate { it.id to it.zIndex }
            if (beforeZ == afterZ) Reduction(model)
            else committing(model, ReorderCommand(model.currentPageIndex, beforeZ, afterZ))
        }
        is Intent.Delete -> {
            val page = currentPage(model)
            val removed = page.elements.withIndex().filter { it.value.id in intent.ids }.map { it.index to it.value }
            if (removed.isEmpty()) Reduction(model)
            else committing(model.copy(selection = model.selection - intent.ids),
                DeleteCommand(model.currentPageIndex, removed))
        }

        // — pages — selection/interaction are per-page, so a page switch clears them and ends any
        // open transform session (else a stale same-index/same-token commit could hit the wrong page).
        is Intent.GoToPage ->
            leavePage(model, intent.index.coerceIn(0, model.document.pages.lastIndex))
        Intent.AddPage -> {
            val at = model.document.pages.size
            committing(leavePage(model, at).model, AddPageCommand(Page(index = at, role = PageRole.INTERIOR), at))
        }
        is Intent.DeletePage -> {
            if (model.document.pages.size <= 1) Reduction(model) else {
                val at = intent.index.coerceIn(0, model.document.pages.lastIndex)
                // If the deleted page is at/ before current, current shifts down one to follow its page.
                val shifted = if (at <= model.currentPageIndex) model.currentPageIndex - 1 else model.currentPageIndex
                val newCurrent = shifted.coerceIn(0, model.document.pages.size - 2)
                val left = leavePage(model, newCurrent).model
                val liveIds = left.document.pages.asSequence()
                    .flatMap { it.elements.asSequence() }
                    .map { it.id }
                    .toSet()
                committing(
                    left,
                    DeletePageCommand(left.document.pages[at], at, model.selection.intersect(liveIds)),
                )
            }
        }

        // — history —
        Intent.Undo -> stepHistory(model, redo = false)
        Intent.Redo -> stepHistory(model, redo = true)
    }

    // — helpers —

    private fun currentPage(model: EditorModel): Page = model.document.pages[model.currentPageIndex]

    /**
     * Switch to [pageIndex], dropping per-page state after closing an open text session through the same
     * cleanup seam as Done/Back. This matters for a freshly placed blank box: simply setting the interaction
     * to Idle leaves an invisible element in the authoritative document (D-041). Add/Delete page callers use
     * the cleaned model and let their structural commit emit the final autosave; plain navigation returns the
     * cleanup autosave itself when the document changed.
     */
    private fun leavePage(model: EditorModel, pageIndex: Int): Reduction {
        val closed = (model.interaction as? Interaction.EditingText)
            ?.let { endTextSession(model, it.id, after = null) }
            ?: Reduction(model)
        return closed.copy(
            model = closed.model.copy(
                currentPageIndex = pageIndex,
                selection = emptySet(),
                interaction = Interaction.Idle,
            ),
        )
    }

    private fun nextZ(model: EditorModel): Int = (currentPage(model).elements.maxOfOrNull { it.zIndex } ?: -1) + 1

    private fun transformsOf(model: EditorModel, ids: Set<String>): Map<String, Transform> =
        currentPage(model).elements.filter { it.id in ids }.associate { it.id to it.transform }

    /** Apply [cmd], push to undo, clear redo, and request an autosave (the only place autosave is emitted). */
    private fun committing(model: EditorModel, cmd: Command): Reduction {
        val doc = cmd.applyTo(model.document)
        val next = model.copy(
            document = doc,
            history = History(undo = model.history.undo + cmd, redo = emptyList()),
        )
        return Reduction(next, listOf(Effect.Autosave(doc)))
    }

    /** Open a text-edit session on [id] iff it names a [TextElement] on the current page; else a no-op. */
    private fun openTextSession(model: EditorModel, id: String?): Reduction {
        val el = id?.let { currentPage(model).elements.firstOrNull { e -> e.id == it } } as? TextElement
        return if (el == null) Reduction(model) else Reduction(
            model.copy(
                selection = setOf(el.id),
                nextToken = model.nextToken + 1,
                interaction = Interaction.EditingText(el.id, model.nextToken),
            ),
        )
    }

    /**
     * Immediate style commit (FR-3, ADR-055). Patches only the supplied fields onto the element's current
     * [com.aritr.zinely.core.model.TextStyle] via copy-on-copy, so every untouched field — including
     * `fontFamily`, which has no patch — plus the element's text/geometry/id/zIndex are preserved. One
     * committed change ⇒ one undoable [EditTextCommand]. Absent / non-text id or an unchanged style ⇒ no-op.
     */
    /**
     * SUPPLIES-SPEC §8 *Change ink*. Total by construction: anything that is not a [DecorElement] on the
     * **current** page resolves to `null` and reduces to a no-op, which is the same shape every other
     * type-specific verb here already has.
     *
     * The `after == el` short-circuit is not an optimisation — it is what stops re-picking the ink a supply
     * already carries from pushing an empty entry onto the undo stack. [styleText] guards the same way.
     */
    private fun inkSupply(model: EditorModel, intent: Intent.InkSupply): Reduction {
        val el = currentPage(model).elements.firstOrNull { it.id == intent.id } as? DecorElement
            ?: return Reduction(model)
        val after = el.copy(ink = intent.ink)
        return if (after == el) Reduction(model)
        else committing(model, EditDecorCommand(model.currentPageIndex, el.id, el, after))
    }

    /**
     * SUPPLIES-SPEC §8 *Replace supply*. Same shape as [inkSupply]: not a `DecorElement` on the current
     * page ⇒ no-op; no observable change ⇒ no undo entry.
     *
     * The `copy` names **only** `supplyId` and `transform`, which is the enforcement of the rule stated in
     * [Intent.ReplaceSupply]: id, ink, mirror and zIndex survive a swap because they are not mentioned here.
     * Adding a field to this `copy` is changing what a replacement means.
     */
    private fun replaceSupply(model: EditorModel, intent: Intent.ReplaceSupply): Reduction {
        val el = currentPage(model).elements.firstOrNull { it.id == intent.id } as? DecorElement
            ?: return Reduction(model)
        val after = el.copy(supplyId = intent.supplyId, transform = intent.transform)
        return if (after == el) Reduction(model)
        else committing(model, EditDecorCommand(model.currentPageIndex, el.id, el, after))
    }

    private fun styleText(model: EditorModel, intent: Intent.StyleText): Reduction {
        val el = currentPage(model).elements.firstOrNull { it.id == intent.id } as? TextElement
            ?: return Reduction(model)
        // No style on a blank box: a still-blank freshly-placed box keeps its PlaceCommand as the last undo
        // entry, which endTextSession's fresh-blank-place coalescing relies on (ADR-055 §3). Styling it would
        // push an EditTextCommand and break "add text, type nothing, dismiss ⇒ no undo cruft".
        if (el.text.isBlank()) return Reduction(model)
        val after = el.copy(
            style = el.style.copy(
                sizePt = intent.sizePt ?: el.style.sizePt,
                color = intent.color ?: el.style.color,
                align = intent.align ?: el.style.align,
                bold = intent.bold ?: el.style.bold,
                italic = intent.italic ?: el.style.italic,
            ),
        )
        return if (after == el) Reduction(model)
        else committing(model, EditTextCommand(model.currentPageIndex, el.id, el, after))
    }

    /** Open a Reframe session on [id] iff it names an [ImageElement] on the current page; else a no-op. */
    private fun openReframeSession(model: EditorModel, id: String?): Reduction {
        val el = id?.let { currentPage(model).elements.firstOrNull { e -> e.id == it } } as? ImageElement
        return if (el == null) Reduction(model) else Reduction(
            model.copy(
                selection = setOf(el.id),
                nextToken = model.nextToken + 1,
                interaction = Interaction.Reframing(model.currentPageIndex, el.id, el, model.nextToken),
            ),
        )
    }

    /**
     * End a text-edit session (§5.6). [after] == null means **discard** (cancel); a non-null [after] is the
     * committed draft. Either way the session closes to [Interaction.Idle]. No empty `TextElement` ever
     * leaks (matches the `text.empty` warning):
     *  - **Blank result** ⇒ the box is removed. If the box was a still-blank **freshly-placed** one (its
     *    placement is the last undo step), the placement is **coalesced away** — undone and popped — so
     *    "add text, type nothing, dismiss" leaves no undo cruft. Otherwise an existing box is removed via a
     *    [DeleteCommand] (one undo restores it).
     *  - **Non-blank** ⇒ only the `text`/`style` are taken from [after]; geometry/zIndex are kept from
     *    `before` (a malformed commit can't move the element). Equal to `before` ⇒ no command/autosave.
     */
    private fun endTextSession(model: EditorModel, id: String, after: TextElement?): Reduction {
        val idle = model.copy(interaction = Interaction.Idle)
        val before = currentPage(model).elements.firstOrNull { it.id == id } as? TextElement
            ?: return Reduction(idle) // element vanished mid-session ⇒ just close
        // Cancel (after == null) keeps `before`'s text; commit takes the draft's. The box is "blank" only if
        // its RESULTING text is blank — so cancelling a box that already has text is never a delete.
        val resultText = after?.text ?: before.text
        if (resultText.isBlank()) {
            val lastPlace = model.history.undo.lastOrNull() as? PlaceCommand
            val freshBlankPlace = lastPlace != null && lastPlace.element.id == id &&
                (lastPlace.element as? TextElement)?.text?.isBlank() == true
            return if (freshBlankPlace) {
                // The box was just placed empty and never gained content ⇒ undo the placement entirely.
                val doc = lastPlace!!.invertOn(model.document)
                Reduction(
                    idle.copy(
                        document = doc,
                        selection = idle.selection - id,
                        history = model.history.copy(undo = model.history.undo.dropLast(1)),
                    ),
                    listOf(Effect.Autosave(doc)),
                )
            } else {
                val removed = currentPage(model).elements.withIndex()
                    .filter { it.value.id == id }.map { it.index to it.value }
                committing(idle.copy(selection = idle.selection - id), DeleteCommand(model.currentPageIndex, removed))
            }
        }
        // Non-blank: change only text/style; keep before's geometry/zIndex. Cancel (after == null) keeps
        // `before` verbatim ⇒ no-op. No command/autosave if nothing actually changed.
        val committed = if (after == null) before else before.copy(text = after.text, style = after.style)
        return if (committed == before) Reduction(idle)
        else committing(idle, EditTextCommand(model.currentPageIndex, id, before, committed))
    }

    /** a11y single-pointer twin: commit a per-selected-element transform via one [TransformCommand]. */
    private fun bakeSelection(model: EditorModel, f: (Transform) -> Transform): Reduction {
        val before = transformsOf(model, model.selection)
        if (before.isEmpty()) return Reduction(model)
        val after = before.mapValues { f(it.value) }
        return committing(model, TransformCommand(model.currentPageIndex, before, after))
    }

    private fun stepHistory(model: EditorModel, redo: Boolean): Reduction {
        val stack = if (redo) model.history.redo else model.history.undo
        val cmd = stack.lastOrNull() ?: return Reduction(model)
        val doc = if (redo) cmd.applyTo(model.document) else cmd.invertOn(model.document)
        val history = if (redo) {
            History(undo = model.history.undo + cmd, redo = model.history.redo.dropLast(1))
        } else {
            History(undo = model.history.undo.dropLast(1), redo = model.history.redo + cmd)
        }
        // Document-global undo/redo: if the command touched another page, navigate there + announce (Codex obs).
        val target = cmd.touchedPageIndex()?.coerceIn(0, doc.pages.lastIndex)
        val nav = target != null && target != model.currentPageIndex
        // Undoing a page delete restores the selection that page carried (Codex required-fix #8).
        val carried = if (!redo && cmd is DeletePageCommand) cmd.priorSelection else model.selection
        // ...but a selection may not outlive the element it points at. Undoing a *placement* removes the
        // element the placement auto-selected, and carrying the id forward left the editor holding a
        // selection of nothing: the frozen bar hid (it resolves the element and finds none) while the
        // transform bar still offered Delete, and the model still read `Selected` where the freeze's own
        // `undo()` captions `Rest` (`v2-bench.html:721`). Found by C9's return-to-Rest invariant, which is
        // the cross-package kind of defect no single package's review was scoped to see.
        //
        // Filtered against the whole document rather than the current page: element ids are unique, page
        // changes clear the selection anyway (`leavePage`), and an undo that navigates has already moved
        // `currentPageIndex` by the time this is read.
        val liveIds = doc.pages.asSequence().flatMap { it.elements.asSequence() }.map { it.id }.toSet()
        val selection = carried.intersect(liveIds)
        val next = model.copy(
            document = doc,
            history = history,
            selection = selection,
            currentPageIndex = if (nav) target!! else model.currentPageIndex.coerceIn(0, doc.pages.lastIndex),
        )
        val effects = buildList {
            add(Effect.Autosave(doc))
            if (nav) add(Effect.Announce("Changed page ${target!! + 1}"))
        }
        return Reduction(next, effects)
    }

    /** The page a command edits, for document-global undo navigation; `null` = structural/whole-doc. */
    private fun Command.touchedPageIndex(): Int? = when (this) {
        is TransformCommand -> pageIndex
        is ReorderCommand -> pageIndex
        is DeleteCommand -> pageIndex
        is PlaceCommand -> pageIndex
        is EditTextCommand -> pageIndex
        is EditImageCommand -> pageIndex
        is MakeImageSpreadCommand -> sourcePageIndex
        is EditDecorCommand -> pageIndex
        is AddPageCommand, is DeletePageCommand -> null
    }
}
