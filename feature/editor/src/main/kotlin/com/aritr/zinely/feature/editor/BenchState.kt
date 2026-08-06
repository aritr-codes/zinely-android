package com.aritr.zinely.feature.editor

import com.aritr.zinely.core.editor.Interaction

/**
 * The Bench's four states — C9 row 9.1 ([ADR-097](../../../../../../../../docs/DECISIONS.md#adr-097)).
 *
 * [IA §A.1](../../../../../../../../docs/design/V2-BENCH-IA-INTERACTION.md) states the model in one line:
 * *"The Bench has **one canvas and four states**; chrome is present only to the degree the maker's current
 * intent needs it."* The frozen prototype narrates the same four through its `cap()` calls, and C9 exists to
 * make that model explicit across the six packages that each built one corner of it.
 *
 * **This enum carries no user-facing text, deliberately** (row 9.1b). The frozen `cap()` writes into
 * `.caption`/`.state`/`.hint` (`v2-bench.html:555`, `:583`), which [ADR-089 row 1.18](../../../../../../../../docs/DECISIONS.md#adr-089)
 * classifies **PROTO** — the prototype explaining itself to a viewer. The shipped Bench models the states
 * and ships none of the narration.
 */
internal enum class BenchState {
    /** *"let me look at / think about my page"* — the page maximised, the page ribbon, one primary action. */
    Rest,

    /** *"change **this** thing"* — outline, handles, and that element's verbs. */
    Selected,

    /** *"write here"* — caret in the real text, the page panned up as one rigid body. */
    Editing,

    /** *"put something new on the page"* — the supply sheet. */
    Adding,
}

/**
 * Derive the current [BenchState] from the model, exactly as the frozen script does.
 *
 * **The precedence is the freeze's, not a preference**, and each step is anchored:
 *
 * | order | state | why it wins |
 * |---|---|---|
 * | 1 | [BenchState.Adding] | `showSheet` removes `.ctx` (`v2-bench.html:847`), so the supply sheet supersedes a live selection rather than coexisting with it |
 * | 2 | [BenchState.Editing] | `edit()` also removes `.ctx` (`:649`) and requires a selection, so an editing session outranks the selection it is editing |
 * | 3 | [BenchState.Selected] | `selectNode` adds `.ctx` (`:621`) |
 * | 4 | [BenchState.Rest] | `deselect` (`:626-629`) — and the destination of `del` (`:715`), `undo` (`:721`), `setPage` (`:791`) and `closeGrid` (`:802`) |
 *
 * **Rest is the default *and* the destination** ([EP-2](../../../../../../../../docs/design/V2-BENCH-PRINCIPLES.md)):
 * every action returns the maker to it. That is asserted as an invariant rather than encoded here — a
 * derivation cannot enforce where transitions lead, only report where they landed.
 *
 * **The page grid is not a fifth state** (row 9.1a). `openGrid` captions `All pages` (`:800`) — the one
 * caption in the frozen script naming something outside the four — but it is an *overlay within Rest*:
 * `closeGrid` restores `Rest` (`:802`), and the grid is reachable only from the Rest-state page row. It is
 * therefore not a parameter of this function at all, which is the strongest way to say it is not a state.
 *
 * @param selection the model's live selection.
 * @param interaction the model's interaction, which carries the text-editing session.
 * @param addChooserOpen whether the C4 supply sheet is open. Surface state, not model state — the reducer
 *   neither knows nor needs to know a sheet is open, exactly as for the type bar and the ink popover.
 */
internal fun benchStateOf(
    selection: Set<String>,
    interaction: Interaction,
    addChooserOpen: Boolean,
): BenchState = when {
    addChooserOpen -> BenchState.Adding
    interaction is Interaction.EditingText -> BenchState.Editing
    selection.isNotEmpty() -> BenchState.Selected
    else -> BenchState.Rest
}
