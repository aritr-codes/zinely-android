package com.aritr.zinely.core.copy

/**
 * The single source of truth for every user-facing string in Zinely — the machine embodiment of
 * [VOICE](../../../../../../../docs/design/VOICE.md) (the human wording authority). Extracted here by C9
 * ([ADR-060](../../../../../../../docs/DECISIONS.md)) so "copy comes from VOICE" is mechanically checkable
 * (see `CopyNoProseLiteralTest`) rather than hundreds of human comparisons per review.
 *
 * **Invariants.**
 * - Fixed copy is a `const val`; copy with runtime values is a function returning the same shape the call
 *   site used to build inline. The rendered characters are **identical** to what shipped — relocating a
 *   string must never change a golden ([ADR-060], C9's "zero intended visual change").
 * - Punctuation is currently *as-shipped* (some straight quotes, some doubled spaces). The "set, don't
 *   type" typographic sweep (CI-84) is a separate, golden-affecting change layered on top of this object,
 *   never folded into the relocation.
 * - This module is pure Kotlin and Android-free. No `Context`, no resources — a `Copy` string is available
 *   anywhere, including pure-JVM tests and non-Composable code.
 * - Screen-reader strings (content descriptions, state descriptions, custom-action labels, announcements)
 *   are copy too, and live here; their **semantics must not change** when relocated.
 */
public object Copy {

    /** Strings shared verbatim across more than one surface. */
    public object Common {
        public const val TRY_AGAIN: String = "Try again"
        public const val GOT_IT: String = "Got it"
        public const val START_A_ZINE: String = "Start a zine"

        /**
         * The spoken name of a **scrim** — the dimmed ground behind an overlay, which is also a tap target
         * that closes it.
         *
         * An audit found every scrim in the app publishing an unnamed clickable node the size of the whole
         * screen. On the page grid that node is in the **main window** (the grid is not a Dialog), so it
         * sits ahead of the panel in traversal order: a TalkBack user meets an unlabelled full-screen
         * button before they reach the thing it is covering. Naming it matches Material's own modal
         * treatment and keeps tap-outside-to-close reachable without sight.
         */
        public const val CLOSE_OVERLAY: String = "Close"
        /** The product wordmark as rendered in the UI (distinct from the launcher `app_name`, CI-86). */
        public const val BRAND: String = "Zinely"
    }

    /**
     * **Share-in** — what Zinely says when another app hands it photos (`ShareInbox.kt`, `MainActivity.kt`,
     * `EditorViewModel.kt`; SUPPLIES-SPEC §6, re-sequenced by
     * [ADR-105](../../../../../../../docs/DECISIONS.md#adr-105)).
     *
     * Three sentences, each answering a question the maker is holding at that exact moment:
     *
     *  - [CHOOSE_ZINE] — *"where did my photo go?"*. Shown when the share lands on the shelf, which is
     *    where a cold start lands. Without it the app opens on a grid of covers and says nothing about
     *    the photo the maker just sent it. It says **which action** completes the transfer ("open a
     *    zine") rather than merely reporting that something was received, because a report the maker
     *    cannot act on is not an answer.
     *  - [ADDING_TO_OPEN_ZINE] — the same question, answered for the maker who already has a zine open.
     *    ⚠ It is **not** redundant with the photos simply appearing: the editor's spoken
     *    [importSummary] can be dropped (the announcement channel is replay-free, so an emission with no
     *    collector — the Proof screen is showing, the editor is mid-boot — is discarded), and an import
     *    that reports nothing at all is indistinguishable from a share the app ignored.
     *  - [ONLY_PHOTOS] — the honest refusal for a non-image share. Names what Zinely *does* take, not
     *    what the file was: the maker knows what they shared, and a refusal that only says "no" makes
     *    them guess at the rule.
     *  - [importSummary] — the count, once the import has actually run. Spoken through the editor's
     *    existing live region, the same channel every other import outcome uses.
     *
     * ⚠ **The failure half is spoken, not shown** — [importSummary] rides `announceForAccessibility`,
     * so a maker without TalkBack sees photos appear and is told nothing about the ones that did not.
     * That is the *existing* import pipeline's behaviour (`ImagePickResult.Failure` → `Announcer`), and
     * this path deliberately inherits it rather than inventing a second, better-informed failure surface
     * for one entry point. Recorded as a known gap in **D-081**, not as a claim that it is right.
     */
    public object ShareIn {
        public const val CHOOSE_ZINE: String = "Open a zine and your photos will be added to it."
        public const val ADDING_TO_OPEN_ZINE: String = "Adding your photos to this zine."
        public const val ONLY_PHOTOS: String = "Zinely can only add photos."

        /**
         * What the editor announces once a share-in batch has finished importing.
         *
         * Both halves are reported, in that order, because they are two different facts and collapsing
         * them would let a partial import read as a whole one. A batch where nothing at all succeeded is
         * still an announcement — silence after a share is indistinguishable from the app ignoring it.
         *
         * @param added photos that reached the page.
         * @param failed photos that could not be read or decoded (revoked grant, corrupt, unsupported).
         */
        public fun importSummary(added: Int, failed: Int): String {
            val addedPart = when (added) {
                0 -> null
                1 -> "Photo added."
                else -> "$added photos added."
            }
            val failedPart = when (failed) {
                0 -> null
                1 -> "One photo couldn’t be added."
                else -> "$failed photos couldn’t be added."
            }
            return listOfNotNull(addedPart, failedPart).joinToString(" ")
        }
    }

    /** Nav-host / share plumbing strings (`ZinelyNavHost.kt`, CI-82/CI-83). */
    public object Nav {
        public const val SHARE_CHOOSER_TITLE: String = "Share your zine"
        public const val NO_APP_TO_OPEN: String = "No app on your phone can open that yet."

        // `ZINE_NAME_FALLBACK` ("Your zine") lived here as the Proof top bar's placeholder title, against
        // the day the real ADR-042 project title threaded through. ADR-101 P6 retired the title itself —
        // the frozen top bar carries the page ticket where the name used to sit — so the follow-up it was
        // waiting for will never be owed, and a string with no surface is a string that misleads the next
        // reader into building one.
        public const val BACK_TO_EDITING: String = "‹  Back to editing"
        public const val BACK_TO_SHELF: String = "‹  Back to your shelf"
    }

    /**
     * The editor's accessibility vocabulary (`EditorA11y.kt`, CI-85) — the discrete single-pointer twins
     * of every gesture transform. Shared verbatim by the visible context bar (`EditorContextBar.kt`), which
     * dispatches the same reducer intents. Changing any of these changes the accessible product; verify on
     * the platform tree, not merged semantics (CI-26).
     */
    public object A11y {
        public const val EDIT_TEXT: String = "Edit text"
        public const val REFRAME_PHOTO: String = "Reframe photo"
        public const val RESET_FRAMING: String = "Reset framing"
        public const val MOVE_LEFT: String = "Move left"
        public const val MOVE_RIGHT: String = "Move right"
        public const val MOVE_UP: String = "Move up"
        public const val MOVE_DOWN: String = "Move down"
        public const val MAKE_LARGER: String = "Make larger"
        public const val MAKE_SMALLER: String = "Make smaller"
        public const val ROTATE_CLOCKWISE: String = "Rotate clockwise"
        public const val ROTATE_COUNTERCLOCKWISE: String = "Rotate counterclockwise"
        public const val BRING_FORWARD: String = "Bring forward"
        public const val SEND_BACKWARD: String = "Send backward"
        public const val DELETE: String = "Delete"

        /** Spoken label for an empty text element. */
        public const val EMPTY_TEXT: String = "Empty text"
        /** Spoken label for a photo element. */
        public const val PHOTO: String = "Photo"
        /** Spoken label for a non-empty text element. */
        public fun textLabel(text: String): String = "Text: $text"

        /**
         * Spoken **state** for an element whose drawn extent leaves the printer's reach — the non-visual
         * twin of the keep-clear warning ([OD-49](../../../../../../../../docs/DECISIONS.md#adr-102-p2c)).
         *
         * Said as a state rather than an alert because the platform speaks a state change on the focused
         * node *and* re-reads it on every later focus: a maker who nudges past the edge hears it when it
         * happens, and a maker who arrives afterwards can still find out. A one-shot announcement would
         * only ever serve the first of those.
         *
         * ⚠ **No jargon, by [BP-4](../../../../../../../../docs/design/V2-BENCH-PRINCIPLES.md)** — *"the
         * maker never learns the word 'bleed'"*. It names the consequence (*may be cut off*) rather than
         * the boundary, because the boundary has no name this product is allowed to teach.
         */
        public const val OUTSIDE_PRINT_REACH: String = "Too close to the edge — may be cut off when printed"

        /**
         * The element's spoken state when it is **both** outside the printer's reach and selected — or not.
         *
         * ⚠ **A state description does not add to the platform's; it replaces it.** Compose supplies
         * *"Selected"* / *"Not selected"* for a node carrying `Selected` **only when no explicit
         * `stateDescription` is set**, and for `Role.Button` that fallback is the only channel selection
         * reaches the platform on at all — the defect `BenchPageGrid.kt:302-307` records from a device pass,
         * where every grid cell announced itself unselected. So setting the reach text alone would have
         * silenced *"selected"* on exactly the elements a maker is nudging, which is where selection is the
         * precondition for every verb. A review caught it; the merged-semantics test could not, because
         * `SemanticsProperties.Selected` is still perfectly present in the tree it reads.
         *
         * Only crossing elements get an explicit string; everything else keeps the platform's own wording.
         */
        public fun outsidePrintReachState(selected: Boolean): String =
            if (selected) "Selected, $OUTSIDE_PRINT_REACH" else "Not selected, $OUTSIDE_PRINT_REACH"
    }

    /**
     * The frozen contextual verb bar (`v2-bench.html` `toolsFor()`, ADR-092 row 2.13). These are the
     * verbs the freeze names for the selected element, verbatim; each doubles as the control's spoken
     * label, since the icon above it is decorative.
     */
    public object BenchVerbs {
        public const val EDIT: String = "Edit"
        public const val FONT: String = "Font"
        public const val SIZE: String = "Size"
        public const val INK: String = "Ink"
        public const val REFRAME: String = "Reframe"

        /** The photocopier filter's toggle (`v21-bench.html:678`, ADR-106). It names the machine, not
         *  the algorithm: nobody asks a copier for Floyd–Steinberg. */
        public const val COPIER: String = "Copier"

        /** [COPIER]'s spoken state — it is the one verb on this bar that is a toggle, and a toggle that
         *  does not say which way it is set is a button that appears not to work. No frozen file draws a
         *  visual on-state; that half is [D-082](../../../../../../../docs/design/V2-SPEC-DEFECTS.md#d-082) Q4.
         *
         *  Shares the `stateDescription` channel with [NOT_YET] and friends below, and cannot collide with
         *  them: a disabled verb has no setting to report, a toggle is live by construction. */
        public const val COPIER_ON: String = "On"
        public const val COPIER_OFF: String = "Off"
        public const val REPLACE: String = "Replace"
        public const val DELETE: String = "Delete"

        /**
         * Why a **drawn but disabled** verb is disabled, announced as its state.
         *
         * [OD-9](../../../../../../../../docs/design/V2-SPEC-DEFECTS.md#d-031-ruling) ruled that a control
         * the freeze draws stays drawn and **invents no capability**. It did not rule that the control
         * stays *silent*, and a device pass found silence is what a first-time user reads as breakage —
         * the same defect, and the same remedy, as the Reframe pad's dead entry state
         * (`docs/BETA-UX-REVIEW.md` F-1, F-4). Explaining an absence invents nothing; it is the opposite.
         *
         * The two reasons are kept apart because they are answerable by the user in opposite ways: one is
         * a capability the product does not have yet, the other is a thing the user can fix in one move.
         * Collapsing them into a single "unavailable" would throw away the half that is actionable.
         */
        public const val NOT_YET: String = "Not available yet"

        /** Size and Ink on a still-blank box — the reducer refuses to style one (ADR-055). */
        public const val TYPE_FIRST: String = "Type something first"

        /**
         * `Size` and `Ink` on the **editing** row (`BenchStyleRow`), which the freeze draws with no handler.
         *
         * Distinct from [NOT_YET] because it is not a missing capability: both verbs are live on the
         * selection bar, and D-042 records that the two surfaces are mutually exclusive by construction
         * (`styleTarget` is gated on `interaction !is Interaction.EditingText`). So the honest sentence is
         * the route, not an apology — the user is a step from the control they are reaching for.
         *
         * ### Why it says *finish typing* and not *tap Done*
         *
         * It named the button first, and independent review caught that as a **lie in the state a new user
         * meets first**: `Add → Text` opens this row on a box that is still blank, and `Done` on a blank box
         * does not hand back a stylable element — the reducer removes it (`EditorReducer` on
         * `resultText.isBlank()`). Naming the button promised a thing that deletes itself.
         *
         * The obvious repair — choose between this and [TYPE_FIRST] from `editingElement.text` — is wrong
         * for a subtler reason: the draft is feature-ephemeral (ADR-029 §5.6) and does not reach the store
         * until commit, so that text reads *blank* for the whole of the typing. It would announce "Type
         * something first" to a user who has just written a paragraph.
         *
         * One sentence covers both without either fault: finishing the typing is exactly the condition —
         * type, then leave the session — and it is true whether the box currently holds words or not.
         */
        public const val FINISH_TYPING: String = "Finish typing to change this"
    }

    /**
     * The frozen bottom bar (`BenchBottomBar.kt`, `v2-bench.html:464-468`; ADR-094 rows 4.1–4.8b). `Done`
     * is [EditText.DONE] — the same word the editing row already ships, deliberately not re-typed.
     */
    public object BenchBar {
        public const val UNDO: String = "Undo"
        public const val REDO: String = "Redo"

        /**
         * The `.add` label names no medium: choosing the medium is what the chooser it opens is for
         * ([OD-21](../../../../../../../docs/design/V2-SPEC-DEFECTS.md#d-047-ruling)).
         */
        public const val ADD: String = "Add"

        /**
         * Why the page-level `Done` is dim, in the two states OD-14 withholds it — *"a control that is
         * drawn and disabled says why"*, the rule F-1 established for the style row and F-6 inherited here.
         *
         * Two strings rather than one because the two states end differently: a text session is finished
         * by the row's own `Done`, an ink session by the card's. A single "Finish what you started" would
         * be true of both and useful for neither. Both ride `stateDescription`, never `contentDescription`
         * — the control's name is `Done` in every state, and only its availability changes.
         */
        public const val DONE_AFTER_TEXT: String = "Finish your text first"
        public const val DONE_AFTER_INK: String = "Close the ink panel first"
    }

    /**
     * The frozen ink popover — H4, the maker palette (`BenchInkPopover.kt`, `v2-bench.html:679-704`;
     * [ADR-096](../../../../../../../docs/DECISIONS.md#adr-096)).
     *
     * The swatch **names** live here rather than beside their colours because
     * [ZinelyContentInks][com.aritr.zinely.ui.theme.ZinelyContentInks] models identity as an enum and
     * leaves the labels to this layer ([ADR-060](../../../../../../../docs/DECISIONS.md#adr-060)) — they
     * are user-facing copy, not colour tokens. They are transcribed verbatim from the frozen arrays.
     *
     * [PAPER_TINTS] is here and is **not offered to a text element**
     * ([OD-24](../../../../../../../docs/design/V2-SPEC-DEFECTS.md#d-028-ruling)): the band is fenced, not
     * deleted, and it returns the day a paper target exists. Deleting the string would have made the fence
     * look like an omission.
     */
    public object BenchInk {
        /** The popover's own title — the same word the verb that opens it carries. */
        public const val TITLE: String = BenchVerbs.INK

        /** The popover's dismiss. The same word the editing row and the bar already ship. */
        public const val DONE: String = EditText.DONE

        // — the four band labels, verbatim (`v2-bench.html:690` Inks/Neutrals, `:688` Paper tints, `:682` the presets) —
        public const val INKS: String = "Inks"
        public const val PAPER_TINTS: String = "Paper tints"
        public const val NEUTRALS: String = "Neutrals"
        public const val PRESETS: String = "Ready-made palettes"

        // — band 1, the ten riso spot inks, in frozen order —
        public const val MATCHA: String = "Matcha"
        public const val FOREST: String = "Forest"
        public const val STRAWBERRY: String = "Strawberry"
        public const val BRICK: String = "Brick"
        public const val SUNFLOWER: String = "Sunflower"
        public const val OCHRE: String = "Ochre"
        public const val AQUA: String = "Aqua"
        public const val CORNFLOWER: String = "Cornflower"
        public const val PLUM: String = "Plum"
        public const val INK: String = "Ink"

        // — band 2, the paper tints (fenced for a text target, see above) —
        public const val CREAM: String = "Cream"
        public const val BLUSH: String = "Blush"
        public const val SKY: String = "Sky"
        public const val SAGE: String = "Sage"
        public const val KRAFT: String = "Kraft"

        // — band 3, the neutrals. `Ink` repeats band 1's, verbatim from the frozen source —
        public const val SLATE: String = "Slate"
        public const val STONE: String = "Stone"
        public const val FOG: String = "Fog"

        // — the three preset recipes —
        public const val PRESET_TWO_COLOUR: String = "Two-colour"
        public const val PRESET_WARM: String = "Warm zine"
        public const val PRESET_COOL: String = "Cool zine"

        /**
         * The `.inkuse` note (`v2-bench.html:691`). The count is **live** — the prototype hard-codes 2
         * because it has no document to count. It counts the distinct inks in the whole **zine**, not the
         * open page: "print cheapest" is a per-zine cost, since a riso or a copy shop charges by the ink
         * on the job.
         */
        public fun useNote(inks: Int): String =
            "Zines look best — and print cheapest — with 1–3 inks. This one uses $inks."

        /**
         * The confirmation the frozen `applyInk` raises (`v2-bench.html:702`) — the ink's own name, so the
         * message says which one landed rather than that something did.
         */
        public fun applied(name: String): String = "Ink · $name"

        /** Spoken name for one swatch: the colour's name is the control's whole meaning. */
        public fun swatchLabel(name: String): String = name

        /**
         * Spoken name for one preset. The dots are decorative — a reader cannot hear three overlapping
         * circles — so the recipe's name carries it, with its primary ink named because that is the one
         * the tap actually applies (OD-24).
         */
        public fun presetLabel(name: String, primary: String): String = "$name. Primary ink $primary"
    }

    /**
     * **The sixteen supplies** — the Zinely cabinet's whole vocabulary
     * ([SUPPLIES-SPEC §4](../../../../../../../docs/design/SUPPLIES-SPEC.md),
     * [ADR-105](../../../../../../../docs/DECISIONS.md#adr-105) step S6). *Art* is the verb; Supplies is
     * the drawer (§1).
     *
     * ### Why the id lives next to the word
     *
     * `DecorElement.supplyId` is the durable half — it is written into every saved document and must match
     * `^[a-z]+\.[a-z]+$` (§2.2, enforced by the document validator). The display name is the disposable
     * half: it can be reworded without touching a single zine. Keeping them in **one** map rather than two
     * parallel lists is the whole point — a `supplyId` with no name, or a name with no supply, is then not
     * expressible rather than merely tested for.
     *
     * ⚠ **The id prefix is not the family.** Five prefixes (`tape`, `fix`, `mark`, `paper`, `shape`) carry
     * four families, because *Tape & fixings* holds one tape and three fixings. Anything that needs the
     * family must read [BY_FAMILY], never `supplyId.substringBefore('.')`.
     *
     * ### The names, and where they depart from §4's prose
     *
     * §4 restores `ZINE-DIRECTION.md` §9.2's sixteen verbatim, and that prose is a *specification*, not a
     * set of labels: two entries are slash-pairs (*star/asterisk*, *cut label/speech tag*) and a screen
     * reader cannot say a slash. So the naming here picks one word per supply, and departs in exactly five
     * places, each for a reason that shows up in speech:
     *
     *  - `mark.asterisk` → **Star**, not *"star/asterisk"*. §8's own worked example of a decor content
     *    description is *"Star, medium, berry"* — the spec had already chosen.
     *  - `paper.tag` → **Speech tag**, not *"cut label/speech tag"*. *Label* is what every other control on
     *    the Bench already is; *speech tag* names the thing on the page.
     *  - `tape.torn` → **Torn tape**, not *"torn tape strip"*. §4 flags this and `paper.strip` (*torn
     *    strip*) as near-neighbours and keeps both deliberately — one is tape, one is paper. But *"Torn
     *    strip"* is a **prefix of** *"Torn tape strip"*, and a listener who hears the shorter one cannot
     *    know they did not simply miss a word. Dropping *strip* from the tape makes the two differ on their
     *    second syllable instead of their fourth, which is the only difference speech can carry.
     *  - `mark.halftone` → **Halftone dots**, not *"halftone dot cluster"*. *Cluster* is print-shop
     *    vocabulary describing the drawing, and [BP-4] forbids teaching the maker a word they did not ask
     *    for.
     *  - `paper.window` → **Window frame**, not *"cut-out window frame"*. The supply is spoken under its
     *    family heading, and that heading is already the word *Cut* — "Cut paper · Cut-out window frame"
     *    spends a listener's attention saying *cut* twice before it says what the thing is. This departure
     *    went unlisted in the first draft of this KDoc, which claimed four; the reconciliation against
     *    `v21-bench.html` found the fifth, which is the argument for reconciling the two files at all.
     *
     * `mark.registration` keeps its trade word because there is no plainer name for the thing, and it is
     * one of the two **process** marks that carry ADR-104's thesis — renaming it to *"cross"* would file it
     * with the geometry it is deliberately not.
     *
     * ### The `Ink` collision, checked and clear
     *
     * §8 warns that *"`Ink` is ambiguous"*. **No supply is named `Ink`** — the collision §8 names is
     * between two *swatches*: [BenchInk.INK] is the single string serving both `ZinelyMakerInkId.Ink` and
     * `ZinelyNeutralId.Ink` (`BenchInkPopover.kt:188` and `:202`), and it is also the word on the context
     * bar's verb ([BenchVerbs.INK]). That defect is real and is **not this object's to fix**: band-
     * qualifying a swatch changes a drawn label in a frozen popover. Recorded, not repaired here.
     *
     * What this object owes is the promise that S6 does not make it worse — so `SuppliesCopyTest` pins that
     * no supply name equals any shipped swatch name or bench verb. The day someone renames `mark.asterisk`
     * to *"Ink star"* or the popover renames a swatch to *"Staple"*, that assertion breaks first.
     */
    public object Supplies {

        // — the four family headings (§4's table, and the picker's own headings per §0 O-C) —
        public const val TAPE_AND_FIXINGS: String = "Tape & fixings"
        public const val STAMPS_AND_MARKS: String = "Stamps & marks"
        public const val CUT_PAPER: String = "Cut paper"
        public const val CUT_SHAPES: String = "Cut shapes"

        /**
         * Family heading → its supplies, `supplyId` → spoken and drawn name, both in §4's frozen order.
         *
         * Ordered maps throughout: the drawer is *"the same every time you open it"* is the one claim §9
         * withdrew, but the **order** is still frozen design — sixteen items on one screen have no sort
         * control and no search, so position is the only way a maker finds a supply twice.
         */
        public val BY_FAMILY: Map<String, Map<String, String>> = linkedMapOf(
            TAPE_AND_FIXINGS to linkedMapOf(
                "tape.torn" to "Torn tape",
                "fix.corner" to "Photo corner",
                "fix.staple" to "Staple",
                "fix.clip" to "Paper clip",
            ),
            STAMPS_AND_MARKS to linkedMapOf(
                "mark.asterisk" to "Star",
                "mark.arrow" to "Arrow",
                "mark.halftone" to "Halftone dots",
                "mark.registration" to "Registration cross",
            ),
            CUT_PAPER to linkedMapOf(
                "paper.strip" to "Torn strip",
                "paper.window" to "Window frame",
                "paper.tag" to "Speech tag",
                "paper.underline" to "Marker underline",
            ),
            CUT_SHAPES to linkedMapOf(
                "shape.rect" to "Rectangle",
                "shape.circle" to "Circle",
                "shape.triangle" to "Triangle",
                "shape.rule" to "Straight rule",
            ),
        )

        /** Every supply, flattened — derived from [BY_FAMILY] so the two can never disagree. */
        public val NAMES: Map<String, String> =
            BY_FAMILY.values.flatMap { it.entries }.associate { it.key to it.value }
    }

    /**
     * The bar's Add chooser (`BenchAddChooser.kt`, `v21-bench.html:826-829`; ADR-094). **All three rows**
     * as of ADR-105 step S7 — the freeze's own narration is *"Add stays three verbs — Text · Photo ·
     * Art"*, and the fence that held `Art` back was that nothing could be taken out of the cabinet. Each
     * row's spoken label is [optionLabel], one target rather than three fragments.
     */
    public object AddChooser {
        public const val TITLE: String = "Add to your page"
        public const val TEXT_TITLE: String = "Text"
        public const val TEXT_SUBTITLE: String = "Type words onto the page"
        public const val PHOTO_TITLE: String = "Photo"
        public const val PHOTO_SUBTITLE: String = "From your phone — it never leaves the device"

        /** The frozen `Art` row (`v21-bench.html:829`), title and subtitle verbatim. */
        public const val ART_TITLE: String = "Art"
        public const val ART_SUBTITLE: String = "Tape, stamps and cut paper"

        public fun optionLabel(title: String, subtitle: String): String = "$title. $subtitle"
    }

    /**
     * The top status strip's autosave chip (`BenchStatusStrip.kt`, `v2-bench.html:390`; ADR-094 row 4.10).
     * The flower is decoration and is never required to parse the meaning (VOICE rule 7), so the live
     * region announces [SAVED_SPOKEN] without it.
     */
    public object Status {
        public const val SAVED_MARK: String = "✿"
        public const val SAVED_WORD: String = "Saved"
        public const val SAVED_QUALIFIER: String = " · on this device"
        public const val SAVED_SPOKEN: String = "Saved on this device"
    }

    /**
     * The delete/undo snackbar (`BenchSnack.kt`, `v2-bench.html:626`; ADR-094 row 4.13). The full stop is
     * the freeze's and it is kept: the line is a sentence about something that happened, not a label.
     */
    public object Snack {
        public fun deleted(label: String): String = "$label deleted."

        /**
         * The frozen `toast('Placed on the page',true)` an Art tile raises (`v21-bench.html:862`).
         *
         * No full stop, unlike [deleted]: the freeze writes neither, and this one is a label on a thing
         * that just happened rather than a sentence about it. `undoable=true` is why the snack carries
         * the `Undo` action — the placement is one command, so one press takes it back.
         */
        public const val PLACED: String = "Placed on the page"
    }

    /** Editor canvas surface — reframe announcements, the whole-photo inert line, the Preview action. */
    public object Editor {
        /**
         * The boot placeholder's spoken name (`ZinelyNavHost.BootLoading`).
         *
         * A `progressBarRangeInfo = Indeterminate` node with no `contentDescription` reaches TalkBack as
         * the bare words *"progress bar"* — which is a control, not a statement, and this screen is the
         * whole window. Naming it is what makes the announcement a sentence a person can act on (they
         * cannot: the right action is to wait, and that is exactly what the sentence says).
         */
        public const val OPENING_ZINE: String = "Opening your zine"

        public const val REFRAMING_PHOTO: String =
            "Reframing photo. Drag to reposition, pinch to zoom, or use the on-screen " +
                "move and zoom controls. Done saves, Cancel discards."
        public const val FRAMING_SAVED: String = "Framing saved."
        public const val FRAMING_UNCHANGED: String = "Framing unchanged."
        public const val MOVED_LEFT: String = "Moved left"
        public const val MOVED_RIGHT: String = "Moved right"
        public const val MOVED_UP: String = "Moved up"
        public const val MOVED_DOWN: String = "Moved down"
        public const val NO_ROOM_TO_MOVE: String = "No room to move that way."
        public const val ALREADY_LARGEST_ZOOM: String = "Already at the largest zoom."
        public const val ALREADY_SMALLEST_ZOOM: String = "Already at the smallest zoom."
        public const val FILLING_THE_FRAME: String = "Filling the frame. Edges may be cropped."
        public const val SHOWING_WHOLE_PHOTO: String = "Showing the whole photo. Margins may appear on paper."
        public const val FRAMING_RESET: String = "Framing reset. Cancel to undo."
        public const val REFRAMING_CANCELLED: String = "Reframing cancelled."
        public const val WHOLE_PHOTO_INERT: String =
            "Whole photo can’t be moved or zoomed. Choose Fill to adjust it."
        public const val PREVIEW: String = "Preview"
        public const val PREVIEW_LABEL: String = "Preview  ›"

        /** Context-bar-only strings (`EditorContextBar.kt`). Transform verbs come from [A11y]. */
        public const val TEXT_STYLE: String = "Text style"
        public const val SHOWING: String = "Showing"
        public const val HIDDEN: String = "Hidden"
    }

    /**
     * First-run / empty-page invitation (`EditorEmptyState.kt`).
     *
     * ⚠ [FIRST_PAGE_HEADLINE] lost its trailing `✨`. That is a **visual-language** fix, not a rewording:
     * the glyph renders through Noto Color Emoji, so it lands as the one full-colour object in a corpus
     * whose entire illustration vocabulary is ink on paper — and it lands inside an Averia headline, next
     * to the frozen `.empty h2` it is meant to rhyme with (`v21-library.html:464`, *"Make your first
     * little zine."*, which carries no emoji). The words are untouched; [D-050] still leaves the *wording*
     * to the owner.
     */
    public object EmptyState {
        public const val ADD_A_PHOTO: String = "Add a photo"
        public const val ADD_WORDS: String = "Add words"
        public const val FIRST_PAGE_HEADLINE: String = "Let's make something cute."
        public const val LATER_PAGE_HEADLINE: String = "A fresh page. What goes here?"
        public const val SUPPLY_CUE: String = "Grab a photo or a few words from the supplies below."
        public const val OFFLINE_NOTE: String = "works offline · stays on your phone"
    }

    /** The move/resize coach hint (`EditorMoveResizeHint.kt`). Dismiss label is [Common.GOT_IT]. */
    public object MoveResizeHint {
        public const val TEXT: String = "Drag to move it. Pinch to resize."
    }

    /** Save-failure banner copy (`EditorSaveFailure.kt`). */
    public object SaveFailure {
        public const val RETRY_LABEL: String = "Try now"
        public const val DISMISS_LABEL: String = "Got it"
        public const val GENERIC: String =
            "Couldn’t save your latest change just now. Tap Try now, or keep editing — it’ll try again."
        public const val OUT_OF_SPACE: String =
            "Your phone is low on storage. Free up a little space, then tap Try now — or keep editing to retry."
    }

    /**
     * The page picker's per-thumb **state**. C5 replaced the V1 strip's `"Page N"` label with [PageNav]'s
     * `"Page N of M"`, so only these two state lines remain here — CI-29's `stateDescription`, which the
     * frozen Bench has no equivalent of and which the conformance path still asserts.
     */
    public object PageStrip {
        public const val CURRENT_PAGE: String = "Current page"
        public const val NOT_SELECTED: String = "Not selected"
    }

    /**
     * The Bench's page navigation — the filmstrip row and the summoned page grid
     * (`BenchPageNav.kt`, `BenchPageGrid.kt`; [ADR-095](../../../../../../../docs/DECISIONS.md#adr-095)
     * rows 5.9, 5.10, 5.15).
     *
     * Roles are passed as *which function you call*, not as a string: `:core:copy` has no dependencies, so
     * it cannot see `PageRole`, and inventing a parallel string enum here would be a second source of truth
     * for the same three cases. The composable maps the role; this object owns only the wording.
     */
    public object PageNav {
        /** The grid button's label — it summons the grid, so it says what it shows. */
        public const val ALL_PAGES: String = "All pages"

        /** An interior page. `N` is always the document's real page count, never a constant. */
        public fun pageLabel(number: Int, count: Int): String = "Page $number of $count"

        /** The first page. TalkBack says *which* page as well as *what* it is. */
        public fun frontCoverLabel(number: Int, count: Int): String =
            "${pageLabel(number, count)} (front cover)"

        /** The last page. */
        public fun backCoverLabel(number: Int, count: Int): String =
            "${pageLabel(number, count)} (back)"

        /** The grid's header. Possessive, not a noun-phrase title: it is the user's zine, not a document. */
        public fun gridTitle(count: Int): String = "Your zine · $count pages"

        /** The front cover's cell badge. */
        public const val COVER: String = "Cover"

        /** The back cover's cell badge. */
        public const val BACK: String = "Back"
    }

    /** Editor effect outcomes surfaced to the user (`EditorEffects.kt`). */
    public object Effects {
        public const val ADDING_IMAGES_UNAVAILABLE: String = "Adding images isn’t available yet"
    }

    /** Inline text-editor (`EditTextSession.kt`). */
    public object EditText {
        public const val ZINE_TEXT: String = "Zine text"

        /**
         * The frozen editing row's `#doneEdit` button (`v2-bench.html:410`) — the one live control in
         * `BenchStyleRow`. Ends the session and returns the element to Selected (ADR-093 row 3.10).
         */
        public const val DONE: String = "Done"
    }

    /**
     * The live unsupported-character notice (`EditorCoverageNotice.kt`,
     * [ADR-070](../../../../../../../docs/DECISIONS.md#adr-070); VOICE §Errors). Permanent product
     * behaviour: when the user types a character the document renderer cannot print, this names the
     * script(s) — so the refusal is *specific* — attributes the limit to printing rather than to the
     * user, and reassures the text is kept, so nothing is ever silently lost.
     *
     * The message is keyed off the human [com.aritr.zinely.core.model.Script] names, not off a hardcoded
     * list, so it **auto-narrows** the day a script is added to the bundled set: that script stops being
     * reported and the sentence simply stops naming it — no copy change required.
     */
    public object Coverage {
        /**
         * The notice line for [scripts] — the distinct human script names present that cannot print,
         * in first-appearance order (e.g. `["Bengali"]`, `["Bengali", "Tamil"]`). The caller passes a
         * de-duplicated list; [joinScripts] only handles the grammar of stitching them together.
         */
        public fun unsupported(scripts: List<String>): String =
            "${joinScripts(scripts)} characters can’t print yet — " +
                "but they’re saved with your zine, so nothing’s lost."

        /** English list grammar: `A` · `A and B` · `A, B and C`. */
        private fun joinScripts(scripts: List<String>): String = when (scripts.size) {
            // Defensive only — the notice is hidden whenever the text is fully covered, so an empty
            // list never reaches paint. A neutral word keeps the sentence grammatical if it ever does.
            0 -> "Some"
            1 -> scripts[0]
            2 -> "${scripts[0]} and ${scripts[1]}"
            else -> scripts.dropLast(1).joinToString(", ") + " and " + scripts.last()
        }
    }

    /** The text Type bar (`TypeBar.kt`). */
    public object Type {
        // Ink swatch names.
        public const val INK_INK: String = "Ink"
        public const val INK_CORAL: String = "Coral"
        public const val INK_TEAL: String = "Teal"
        public const val INK_BLUE: String = "Blue"
        public const val INK_OCHRE: String = "Ochre"
        // Announcements.
        public const val BOLD_ON: String = "Bold on"
        public const val BOLD_OFF: String = "Bold off"
        public const val ITALIC_ON: String = "Italic on"
        public const val ITALIC_OFF: String = "Italic off"
        // Row labels.
        public const val ROW_SIZE: String = "Size"
        public const val ROW_ALIGN: String = "Align"
        public const val ROW_STYLE: String = "Style"
        public const val ROW_COLOUR: String = "Colour"
        // Alignment (visible option labels + announcements).
        public const val ALIGN_LEFT: String = "Left"
        public const val ALIGN_CENTER: String = "Center"
        public const val ALIGN_RIGHT: String = "Right"
        public const val LEFT_ALIGNED: String = "Left aligned"
        public const val CENTERED: String = "Centered"
        public const val RIGHT_ALIGNED: String = "Right aligned"
        // Style toggles (a11y labels; the visible "B"/"I" faces stay glyphs).
        public const val STYLE_BOLD: String = "Bold"
        public const val STYLE_ITALIC: String = "Italic"
        // Size stepper (a11y labels; the "−"/"+" faces stay glyphs).
        public const val SMALLER: String = "Smaller"
        public const val LARGER: String = "Larger"
        public fun sizePtLabel(pt: Int): String = "$pt pt"
        public fun sizePointAnnouncement(pt: Int): String = "Size $pt point"
        public fun colourAnnouncement(label: String): String = "Colour $label"
    }

    /** Photo reframe controls (`ReframeControls.kt`). "Reset framing" comes from [A11y.RESET_FRAMING]. */
    public object Reframe {
        public const val MOVE_PHOTO_UP: String = "Move photo up"
        public const val MOVE_PHOTO_LEFT: String = "Move photo left"
        public const val MOVE_PHOTO_RIGHT: String = "Move photo right"
        public const val MOVE_PHOTO_DOWN: String = "Move photo down"
        public const val ZOOM_OUT: String = "Zoom out"
        public const val ZOOM_IN: String = "Zoom in"

        /**
         * `.padhint` (`v21-reframe.html`, revised 2026-08-15) — the pad's **entry state**, which the frozen
         * file never specified until a device pass found it (`docs/BETA-UX-REVIEW.md` F-4).
         *
         * A newly placed photo's frame is seeded to the photo's own aspect, so at 100% Fill there is no
         * overflow on either axis and the implementation correctly disables all four nudges *and* `Zoom
         * out`. Five dead controls, and nothing said why — the same defect as F-1 on the Bench, one surface
         * along, and the reason the house rule is written once for both.
         *
         * It is an **instruction, not an error**: no warning colour, no icon, the pad's own quiet ink. It
         * names the act that revives the pad rather than the state that killed it, and that act is `Zoom
         * in`, which is never disabled at the entry state — so the hint can never advise a control the user
         * cannot reach.
         */
        public const val ZOOM_IN_TO_MOVE: String = "Zoom in to move the photo"
        public const val FILL: String = "Fill"
        public const val CROPS_EDGES: String = "crops edges"
        public const val WHOLE_PHOTO: String = "Whole photo"
        public const val MAY_ADD_MARGINS: String = "may add margins"
        public const val CANCEL_REFRAMING: String = "Cancel reframing"

        /**
         * The **drawn** word on Reset — its spoken label stays the long [A11y.RESET_FRAMING].
         *
         * F-9: the control shipped as a bare circular arrow between two worded neighbours, and that glyph
         * is the *rotate* glyph on the Bench's own transform row, one surface away. Same glyph family,
         * different act, adjacent surfaces. The spoken label was always right, so this was a purely visual
         * defect and the fix is a word (`v21-reframe.html`, revised 2026-08-15: `Reset` is a `.text-btn`).
         */
        public const val RESET: String = "Reset"
        public const val DONE_REFRAMING: String = "Done reframing"
        public const val REFRAME_THIS_PHOTO: String = "Reframe this photo"
        public const val CANCEL: String = "Cancel"
        public const val DONE: String = "Done"
        public const val REFRAME: String = "Reframe"

        public fun zoomPercentText(percent: Int): String = "$percent%"
        public fun zoomPercentAnnouncement(percent: Int): String = "Zoom $percent percent"
    }

    /**
     * The Library — *"which zine do I want?"* (`feature/library/`, plus the still-shared
     * `ShelfSheets.kt`).
     *
     * ⚠ Some of these strings outlived the screen that introduced them: `ShelfStates.kt`,
     * `ShelfCard.kt` and `HomeScreen.kt` were the V1 shelf and are deleted. Anything here still in use is
     * read by the V2.1 Library; anything not is dead copy, and this object has not been swept for it.
     */
    public object Shelf {
        public const val ON_THIS_DEVICE: String = "On this device"
        public const val SEARCH_YOUR_ZINES: String = "Search your zines"
        public const val YOUR_ZINES: String = "Your zines"
        public const val LOADING_YOUR_ZINES: String = "Loading your zines"
        public const val EMPTY_BODY: String =
            "One sheet of paper, printed at home and folded by hand into a small book. " +
                "Start one and the bench will teach you the rest."
        public const val KEPT_ON_DEVICE: String = "Kept on this device — no account, nothing uploaded"
        public const val COULDNT_OPEN_SHELF: String = "Couldn't open your shelf"
        public const val ERROR_BODY: String =
            "Your zines are safe on this device — we just couldn't read them this time."
        public const val NOTHING_BY_THAT_NAME: String = "Nothing here by that name."
        public const val HOME_EMPTY_HEADLINE: String = "Make your first little zine."

        // Sort options (`ShelfSheets.kt` SortOption — long label · short chip label).
        public const val SORT_RECENT_LONG: String = "Recently opened"
        public const val SORT_RECENT_SHORT: String = "Recent"
        public const val SORT_NAME_LONG: String = "Name (A–Z)"
        public const val SORT_NAME_SHORT: String = "Name"
        public const val SORT_OLDEST_LONG: String = "Oldest first"
        public const val SORT_OLDEST_SHORT: String = "Oldest"

        // Create sheet.
        public const val CHOOSE_PAPER_SUB: String = "Choose your paper. You can print it at home on either."
        public const val EIGHT_PAGES_FROM_SHEET: String = "Eight pages from one folded sheet."

        // Card action sheet.
        public const val OPEN_ON_THE_BENCH: String = "Open on the bench"
        public const val RENAME: String = "Rename"

        /**
         * The rename field's spoken name.
         *
         * `RenameInput` is a `BasicTextField` with a `decorationBox` and no placeholder, so it publishes
         * **no accessible name at all** — a TalkBack user focusing it hears the zine's current title, or
         * silence when it is empty, and never learns what the box is for. The sheet's own heading says
         * *"Rename"*, but a heading is not a label: focus lands on the field, not on the words above it.
         */
        public const val RENAME_FIELD: String = "Zine name"
        public const val DUPLICATE: String = "Duplicate"
        public const val DELETE: String = "Delete"
        public const val SAVE: String = "Save"
        public const val SORT: String = "Sort"
        public const val UNDO: String = "Undo"

        public fun cardOpenLabel(title: String): String = "$title, finished zine. Open on the bench."
        public fun actionsFor(title: String): String = "Actions for $title"
        public fun deletedMessage(title: String): String = "Deleted “$title”"
    }

    /** Paper-size display names, shared by the shelf chooser and the print recipe. */
    public object Paper {
        public const val A4: String = "A4"
        // "US Letter", not "Letter": the frozen `.paperseg` says so, and beside "A4" a size called
        // "Letter" reads like a document type. ADR-101 P3's review caught the build and the spec
        // disagreeing here.
        public const val LETTER: String = "US Letter"
        public const val A4_DIMENSIONS: String = "210 × 297 mm"
        public const val LETTER_DIMENSIONS: String = "8.5 × 11 in"
        // `A4_DIMENSIONS_LONG` / `LETTER_DIMENSIONS_LONG` were deleted by ADR-101 P3: the paper chooser
        // sheet's sub-line was their only consumer, and the segmented control that replaced it names the
        // size and nothing else.
    }

    /** The Proof surface top bar + band (`ProofScreen.kt`). "Try again" is [Common.TRY_AGAIN]. */
    public object Proof {
        // `ACT_READ` — the top bar's status line, *"Read · tap the edges to turn"* — is retired with the
        // line itself (ADR-101 P6). P5 had just corrected its wording from "swipe" to "tap the edges",
        // and then replaced the job it was doing: the reader's chevrons teach the turn where the hand
        // already is, and they *disappear* at the ends, which a sentence at the top of the screen cannot.
        // The position it never carried is now the `.pcount` ticket's, in the frozen place.
        public const val COULDNT_MAKE_PDF: String = "Couldn’t make the PDF"
        public const val ERROR_BODY: String =
            "Your zine is safe on this device — the export just didn’t finish. Try once more."
        public const val BACK_TO_BENCH_SAVED: String = "Back to the bench (your work is saved)"

        /**
         * The fold guide's last step hands off to this, and it **no longer promises a reveal**.
         *
         * It read *"It's folded — show me"*, and what it showed was the finished-book climax this ADR
         * retired ([ADR-101 §6.8](../../docs/DECISIONS.md#adr-101-p4-device)): a schematic drawing of a
         * booklet, offered to somebody holding the real one. The button now acknowledges and closes the
         * drawer, so the label stops making an offer the screen does not keep.
         *
         * Deleted with the climax: `DONE_READY` (a top-bar status line for a state that no longer
         * exists), `BACK_TO_BENCH` and `MAKE_ANOTHER` (its two exits — which were wired to the *same*
         * destination, the loud one promising a new zine and delivering the bench), and
         * `ProofFold.DONE_HEADING` / `DONE_BODY`.
         */
        public const val ITS_FOLDED: String = "It’s folded"

        // ── V2.1 (ADR-101 P1). Eight constants were **deleted** here, not deprecated: `ACT_SHEET`,
        // `ACT_PRINT`, `ACT_FOLD`, `PRINT_AND_FOLD`, `PRINT_SETUP`, `BACK`, `NOW_FOLD_IT` and
        // `BACK_TO_YOUR_ZINE` described the three-act climb the accepted design retires, and every one had
        // dropped to zero references. An earlier draft of this comment claimed they were "kept until P6
        // re-baselines the tests that assert them" — no test asserted them even then, which is the sort of
        // sentence that is checkable and was checked. Only strings with a live consumer survive below.

        /** The `drawer-details` title, and the band's opener into it. */
        public const val PRINT_DETAILS: String = "Print details"

        /** The `drawer-fold` title, and the top bar's right icon. */
        public const val HOW_TO_FOLD: String = "How to fold"

        /**
         * The frozen `.dclose` on both drawers — [ADR-101](../../../../../../../docs/DECISIONS.md#adr-101)
         * P1 booked this as owed, and P3 pays it. The word a screen reader says; the button itself is
         * `:core:ui`'s, which cannot own copy (ADR-060).
         */
        public const val CLOSE: String = "Close"

        // ── The band (ADR-101 P2) ────────────────────────────────────────────────────────────────
        //
        // The `.ready` row, the `.commit` pair and the `.done` block. The export strings moved here
        // from `ProofPrint` with the controls they label: the band owns the commit now, and the print
        // recipe is a drawer you consult, not the place you press Save.

        /** `.ready`'s heading before anything is saved. */
        public const val READY_WHEN_YOU_ARE: String = "Ready when you are"

        /**
         * `.ready`'s heading **after** a save.
         *
         * The frozen band hides this row on save, so it only ever needed one heading. P2 keeps the row —
         * the print recipe is most needed once the PDF exists — and that immediately made the heading
         * wrong: *"Ready when you are"* sitting above *"Saved to your phone"* means *ready for what, I
         * just did it*. Keeping the route without renaming the signpost buys reachability and loses
         * legibility, so the row says what it is for at the moment it is for it.
         */
        public const val BEFORE_YOU_PRINT: String = "Before you print"

        /** `.ready`'s sub-line — what will be printed, on what, and where it stays. */
        public fun readySummary(pages: Int, paper: String): String =
            "${pagesWord(pages)} · one sheet, one cut · $paper · stays on your phone"

        /**
         * The whole `.ready` row is **one** control, so it is announced as one string.
         *
         * The frozen row carries `aria-label="Print details"`, which on a real screen reader *replaces*
         * the heading and the summary rather than adding to them — a sighted user would get the page
         * count, the paper and the privacy promise, and a TalkBack user would get three words. So the
         * label is the row's own content and the destination rides on the click action instead
         * (`onClickLabel`), which is where Android puts "what happens if you activate this".
         */
        public fun readyLabel(pages: Int, paper: String, saved: Boolean = false): String =
            "${if (saved) BEFORE_YOU_PRINT else READY_WHEN_YOU_ARE}. ${readySummary(pages, paper)}"

        private fun pagesWord(pages: Int): String = if (pages == 1) "1 page" else "$pages pages"

        /**
         * `.commit` — the two honest export edges, moved out of the print recipe by P2.
         *
         * The frozen `#shareSheet` offered *"Save to Files"* and *"Send to an app"*; both were deleted
         * with the sheet that held them. On Android those are one surface — the OS chooser is where you
         * pick Files *or* an app — so the two rows called the same code, and a menu whose branches are
         * indistinguishable teaches the user that this app's choices are decorative. Worse, *"Save to
         * Files"* was a third save-flavoured phrase beside *"Save PDF"* and *"Saved to your phone"*.
         * Share now opens the chooser directly.
         */
        public const val SAVE_PDF: String = "Save PDF"
        public const val SHARE: String = "Share"

        /**
         * The in-flight labels — **the running button says which one is running**.
         *
         * Until ADR-102 the two commit buttons shared one `exportBusy` Boolean, so a render dimmed both
         * and named neither. A user tapping Share watched Save PDF react, which reads as *"I hit the
         * wrong thing"* — the report that opened this fix. Rendering an 8-page sheet is not instant; a
         * control that changes appearance and stays silent about why is the part that has to go, not the
         * disabling.
         *
         * Words rather than a spinner alone, because the label has to survive TalkBack, where a spinner is
         * nothing at all.
         *
         * ⚠ **Share says "Preparing…", not "Sharing…", and the constant is named for the word rather than
         * the button.** Nothing is shared until the chooser returns and the user picks something; the app's
         * job at this moment is to make the file. Claiming the share has begun would be the same class of
         * overstatement as the completion that named a file it had not written (§12.14 C).
         */
        public const val SAVING: String = "Saving…"
        public const val PREPARING_SHARE: String = "Preparing…"

        /** `stateDescription` for the running control — TalkBack's half of the same fact. */
        public const val EXPORT_WORKING: String = "Working"

        /** `.done` — the persistent completion the band raises in place of `.commit` after a save. */
        public const val SAVED_TO_YOUR_PHONE: String = "Saved to your phone"

        /**
         * `.done`'s body — and the **last** sentence a user reads before they walk to a printer.
         *
         * Two departures from the frozen *"In Downloads — print it whenever, then fold it up."*
         *
         * It **names the file**, because [ADR-054]'s promise is that a durable copy exists and a promise
         * you cannot check is not one. The name is the display name the exporter actually wrote,
         * extension included.
         *
         * And it carries the print recipe in three words. [ADR-052]'s four rows exist because a home
         * printer will silently ruin a zine — fit-to-page shrink, a portrait default, double-siding — and
         * until now every one of them lived inside a drawer nothing forces you through. *"Print it
         * whenever"* is a cheerful way to lose a sheet of paper. The drawer remains the full explanation;
         * this is the part that has to survive the walk.
         */
        public fun savedInDownloads(name: String): String =
            "In Downloads — “$name”. Print it at 100%, landscape, one side — then fold it up."

        /**
         * **`.done`'s second state — the band's answer to [ITS_FOLDED]** (ADR-101 §6.9).
         *
         * The fold guide's finish button asks the user to *declare* something. Before this pair existed,
         * declaring it changed nothing on screen: the band still headlined *"Saved to your phone"* over
         * *"…then fold it up"* under a leaf stamp primary reading **Fold it up**. Behaviourally correct —
         * nothing about the save had changed — and read cold as *the app wasn't listening*, which is
         * [ADR-058](../../../../../../../docs/DECISIONS.md#adr-058)'s failure one screen over.
         *
         * Asking for a report and then not reacting to it is worse than not asking. So the headline
         * becomes the reply, the body drops the instruction and keeps the file (the user may well want a
         * second copy), and [FOLD_IT_UP] demotes to a quiet [FoldAgain][HOW_TO_FOLD] link — reachable,
         * because forgetting step 8 five seconds later is ordinary, but no longer the loudest thing on a
         * screen telling someone to do what they just did.
         */
        public const val NICE_THATS_A_ZINE: String = "Nice — that’s a zine"

        /** @see NICE_THATS_A_ZINE */
        public fun foldedInDownloads(name: String): String =
            "Still in Downloads as “$name”, if you want to print another."

        /**
         * `.done`'s one action: the ADR-041 post-export → fold hand-off, now persistent — and the fold
         * drawer's own visible heading (`<h3>Fold it up</h3>`), which is deliberately the same words as
         * the button that opens it. [HOW_TO_FOLD] is that drawer's `aria-label` and the opener's label.
         */
        public const val FOLD_IT_UP: String = "Fold it up"

        /**
         * The band's stale-file notice — **the P3 design review's highest finding**.
         *
         * Changing the paper size drops `.done`, correctly: the file in Downloads was imposed for the old
         * size, and going on saying *"Saved to your phone"* about it would be a lie. But the change is made
         * inside a `Dialog`-backed drawer, so the band mutates behind a scrim: the user never witnesses the
         * cause, only discovers the effect on closing, and the honest reading from their chair is *"it
         * deleted my file"*. That is the `0.9.0-beta.1` Preview shape — correct behaviour, read as loss —
         * one tap deep inside the surface [ADR-058] exists to protect. The mechanism was never the defect;
         * the silence was. So the band says it out loud, and says the old file is still there, because it
         * is.
         */
        public fun paperChangedResave(newPaper: String, oldPaper: String): String =
            "Paper changed to $newPaper — save again to get ${article(newPaper)} $newPaper-sized PDF. " +
                "The $oldPaper one is still in Downloads."

        /**
         * *"a"* or *"an"* for a paper name interpolated into a sentence — the band read *"a A4-sized PDF"*
         * on device, because the article was a literal in a string that interpolates its noun.
         *
         * ⚠ **Enumerated, not inferred, and that is the honest form here.** English picks the article by
         * SOUND, and no rule over letters gets both shipped names right: "A4" is said *ay-four* and takes
         * *an*; "US Letter" is said *you-ess* and takes *a*, despite starting with the vowel letter U. A
         * vowel-letter test fails the second, and an initialism test that fixes it ("A, E, F, H…") would
         * then say *"an Legal"* the day a third size arrives. So the exceptions are listed, and a name that
         * is not listed gets the common case.
         *
         * Adding a paper size means adding it here if it is said with a leading vowel sound.
         * `PaperArticleTest` pins the rendered sentence for both shipped names.
         */
        internal fun article(paper: String): String = if (paper in TAKES_AN) "an" else "a"

        /** Paper names said with a leading vowel sound. See [article]. */
        internal val TAKES_AN: Set<String> = setOf("A4")
    }

    /**
     * The imposed sheet illustration (`ProofSheet.kt`).
     *
     * `TITLE` (*"This is your sheet"*) was deleted by ADR-101 P3 — the sheet stopped being an act with its
     * own lead, and [ProofPrint.SECT_BOOKLET] heads the section that places it. `BODY` went with it on the
     * claim that [ProofPrint.BOOKLET_HINT] said the same thing; the design review showed it did not, and
     * [SCRAMBLED_CAPTION] is that job restored in the one position where it works.
     */
    public object ProofSheet {
        public const val CONTENT_DESCRIPTION: String =
            "Your zine imposed on one landscape sheet: eight panels, " +
                "the top row upside-down, with one cut line across the centre."
        public const val ONE_CUT: String = "ONE CUT"

        /**
         * The caption **under** the sheet, restoring the one job the deleted `BODY` was doing.
         *
         * P3 dropped *"This is your sheet / it looks scrambled on purpose"* on the grounds that
         * [ProofPrint.SECT_BOOKLET] + [ProofPrint.BOOKLET_HINT] say the same thing. The design review
         * disagreed and was right: `BOOKLET_HINT` is abstract (*"already in the order that makes a
         * booklet"*) and is read **before** the picture. What the user then actually sees is a top row
         * printed upside-down and pages running 5·4·3·2 / 6·7·8·1, and their first thought is *the export
         * is broken*. Nothing else on this surface names the upside-down row. Pre-empting that alarm is a
         * different job from explaining imposition, and it has to be done where the alarm happens.
         */
        public const val SCRAMBLED_CAPTION: String =
            "Looks scrambled on purpose — the top row prints upside-down so it’s the right way up " +
                "once it’s folded."
        public const val LEGEND_FOLD_LINES: String = "fold lines"
        public const val LEGEND_ONE_CUT: String = "the one cut"
        public const val LEGEND_PRINTER_REACH: String = "printer can’t reach here"
        public const val FRONT_COVER: String = "Front cover"
        public const val BACK_COVER: String = "Back cover"
    }

    /**
     * The print recipe act (`ProofPrint.kt`) — the four settings and the paper chooser.
     *
     * `SAVE_PDF` and `SHARE` **moved** to [Proof] with the controls, when ADR-101 P2 moved the commit into
     * the band. `SHARE_SUB`, `SAVE_TO_FILES` and `SEND_TO_AN_APP` were **deleted** rather than moved, and
     * exist nowhere in the repository: they were the in-app share chooser's title and its two rows, and P3
     * removed that sheet because both rows opened the same OS chooser (see [Proof.SAVE_PDF]'s docs). The
     * chooser Share now opens directly is the system's, titled [Nav.SHARE_CHOOSER_TITLE].
     */
    public object ProofPrint {
        public const val SCALE_LABEL: String = "Scale"
        public const val SCALE_VALUE: String = "100% · Actual size"
        public const val SCALE_EMPHASIS: String = " — not “Fit to page”"
        public const val ORIENTATION_LABEL: String = "Orientation"
        public const val LANDSCAPE: String = "Landscape"
        public const val ORIENTATION_EMPHASIS: String = " — a portrait default breaks the fold"
        public const val PAPER_LABEL: String = "Paper"
        public const val SIDES_LABEL: String = "Sides"
        public const val SIDES_VALUE: String = "Single-sided — one side only"

        // ── The one `.dbody` panel (ADR-101 P3) ─────────────────────────────────────────────────
        //
        // P3 replaced the paper *chooser sheet* with the frozen segmented control, so `PAPER_SIZE_TITLE`,
        // `PAPER_SIZE_SUB` and `CHANGE` went with it — a Dialog raised over a Dialog to answer a
        // two-option question, when the two options fit on one row of the panel that asked it.

        /** `.sect` 1 — the paper question, answered inline by the segmented control. */
        public const val SECT_PAPER: String = "What paper is in your printer?"

        /**
         * `.sect` 2 — **the honest replacement for the frozen `ALL SET` checklist.**
         *
         * The checklist is not built, and the reason is in [SECT_TEST]'s neighbour docs and ADR-101 §6.6:
         * two of its three green ticks asserted checks that do not exist. But deleting the false claims
         * also deleted the section's *job*, and the design review named the cost exactly — the panel became
         * "entirely instructions to the user… it reads like homework". The frozen section was the one place
         * the app said *we've got this part*, and its third tick (*one cut, down the middle*) was true by
         * construction and went out as collateral.
         *
         * So the reassurance comes back, and only the tick grammar stays gone: these are facts about the
         * **artifact we produced**, in plain lines, not check marks implying a pass on the user's content.
         * [PAPER_HINT] moved here from under the segmented control for the same reason — it was already a
         * we-did-this statement, orphaned in a section about a question.
         */
        public const val SECT_ALREADY: String = "What we’ve already done"
        public const val PAPER_HINT: String =
            "We lay your zine out for this exact size, so it prints at 100% — nothing shrinks to fit."
        public const val ALREADY_CUT: String =
            "One cut, down the middle — the red line on the sheet below is the only place the blade goes."
        public const val ALREADY_MARGIN: String =
            "We keep a margin at the edge of the sheet, because no printer can reach all the way to the paper’s edge."

        /**
         * `.sect` 3 — [ADR-052]'s recipe, which the frozen `.dbody` does not contain at all.
         *
         * Searching the prototype for these phrases returns one hit, and it is a code comment about
         * sheet geometry. They are kept because a home printer will silently ruin a zine and this is
         * the only place the full reason lives — the band's `.done` carries the three-word version.
         */
        public const val SECT_DIALOG: String = "At the print dialog"

        /**
         * The line that gives *"At the print dialog"* a referent.
         *
         * Straight from the design review, and it is the kind of gap only a cold read finds: the section
         * names a dialog the user has not seen and cannot reach from this panel, then lists four settings
         * for it. Per [ADR-052] the app has no `PrintManager` path at all — the print dialog belongs to
         * whatever app opens the PDF — and until now the only place the product admitted that was
         * [Proof.savedInDownloads], which exists only *after* a save. A recipe with no verb is homework.
         */
        public const val DIALOG_HINT: String =
            "Zinely doesn’t print for you. Save the PDF, then open it from Downloads in your printer’s " +
                "app — these are the settings to look for there."

        /** `.sect` 4 — the frozen `.testcard`, the freeze's strongest wasted-sheet guard. */
        public const val SECT_TEST: String = "Before you print a stack"
        public const val TEST_SHEET_LEAD: String = "New printer?"
        public const val TEST_SHEET_BODY: String =
            " Print one test sheet first and fold it — check the cover’s on top and the text is upright. " +
                "Printers flip pages differently; one sheet saves a stack."

        /** `.sect` 5 — the imposition explainer, and the sheet itself as its illustration. */
        public const val SECT_BOOKLET: String = "How it becomes a booklet"
        /**
         * **Takes the count rather than hardcoding eight** (ADR-101 P6). It read *"your 8 pages"* while the
         * band two rows away computed the real number and, since P6, so does the top bar's ticket — so on
         * any other document the surface said *"PAGE 3 OF 12"*, *"12 pages · one sheet, one cut"* and
         * *"your 8 pages"* at once. [ProofRead.leafLabel] states the rule this broke: two readouts on one
         * screen disagreeing about how many pages the zine has is the one thing this screen exists to
         * settle.
         */
        public fun bookletHint(pageCount: Int): String =
            "We lay your $pageCount pages onto one sheet, already in the order that makes a booklet once " +
                "it’s folded — so you never rearrange anything yourself."

        /**
         * The panel's last line, and the only thing on it that points anywhere.
         *
         * A section titled *how it becomes a booklet* ended at a picture of a sheet and then declined to
         * say how — the fold guide was reachable only from a top-bar glyph or from `.done`, neither of
         * which is where a user finishes reading this. Closes the loop the section's own title opens.
         */
        public const val SEE_HOW_TO_FOLD: String = "See how to fold it"
        // The double-sided help line, built in three spans (the middle span is bold "single-sided").
        public const val SIDES_HELP_PREFIX: String = "If your printer asks about double-sided, choose "
        public const val SIDES_HELP_BOLD: String = "single-sided"
        public const val SIDES_HELP_SUFFIX: String = " (or “off”). A mini-zine prints on one side, then folds."
    }

    /** The fold guide drawer + climax (`ProofFold.kt`). */
    public object ProofFold {
        // "Got your printed sheet?" opens the guide, because it can be opened before anything is printed
        // and step 1 says "Fold the sheet in half" without ever saying which sheet. It is shown on step 1
        // only: a precondition stops being useful the moment you are past it, and this drawer has eight
        // steps to fit. `INTRO_TITLE` was deleted with the lead — the drawer's own title says it.
        /**
         * The precondition, step 1 only — and **the only place scissors are named**. They are first needed
         * at step 5, by which point the user is sitting down with paper in both hands; a tool you have to
         * get up for belongs beside the sheet, not three steps in.
         */
        public const val INTRO_BODY: String =
            "Got your printed sheet and some scissors? Take the steps one at a time — tap the arrow when " +
                "a step is done."
        public const val PREVIOUS_STEP: String = "Previous step"
        public const val NEXT_STEP: String = "Next step"

        /** `.stepno` — the frozen step counter, which is also the guide's only heading. */
        public fun stepOf(step: Int, total: Int): String = "Step $step of $total"

        /** `.stepdots` — one button per step, and the group they live in. */
        public const val FOLD_STEPS_GROUP: String = "Fold steps"
        public fun stepDot(step: Int): String = "Step $step"

        /**
         * `.legend` — five marks whose meaning **never changes between steps**, which is the property
         * that makes the guide readable at a glance ([V21-SPEC §5.2](design/V21-SPEC.md)). Grey dashed is
         * a crease you already made; green is the fold you are making now; red is the cut; an ink rule is
         * paper travelling; a hollow bar is force you apply.
         *
         * **The list grew twice, both times for the same reason: a key implies it is complete.** The
         * freeze had three, and every arrow was drawn in the cut's red — so red meant *cut here* on one
         * step and *this paper travels* on seven. Naming the arrow fixed that and left the
         * Yoshizawa–Randlett **action** arrow of steps 4 and 7 unnamed, which is the identical defect one
         * arrow over. [LEGEND_ACT] closes it.
         *
         * It could not be folded into [LEGEND_MOVE] instead — the cheaper option, and the wrong one. A
         * motion arrow has one tail and one head: *this flap lands there*. Step 4 opens the sheet out
         * (a double-header) and step 7 pushes both ends in (a facing pair); that is relative motion,
         * which the filled arrow cannot state. Drawing them as `move` would be wrong information, not
         * less of it.
         */
        public const val LEGEND_CREASE: String = "crease"
        public const val LEGEND_FOLD_NOW: String = "fold now"
        public const val LEGEND_CUT: String = "cut"
        public const val LEGEND_MOVE: String = "move"
        public const val LEGEND_ACT: String = "push or pull"

        /**
         * **The eight steps — `.foldcap`, one physical action each** (ADR-101 P4).
         *
         * This replaces five steps that were a *different instruction sequence*, not a shorter version of
         * this one. The old step 1 was *"Fold the sheet in half three times, then open it flat"* — three
         * physical actions in one instruction, told to someone holding a sheet of paper for the first
         * time, and the frozen rule is one action per step ([V21-SPEC §5.2](design/V21-SPEC.md)). Steps 1,
         * 2, 3 and 4 here are those three folds and the unfold, each on its own. The old sequence also
         * never said *"fold in half the first way again"* before the cut, so the slit's position was left
         * to the diagram to imply.
         *
         * Deleted with it: `STEP1..5_TITLE`. The frozen guide has no per-step titles — the counter is the
         * heading, and the caption is what the diagram is labelled by, which is more use to a screen
         * reader than *"Crease into eight"* was.
         */
        public val STEP_CAPTIONS: List<String> = listOf(
            // The first clause is not in the original freeze. The diagram is drawn landscape and printed
        // side up, and step 1 assumed you would infer both — every later step inherits whichever way
        // you started, so getting it wrong here is only discovered at step 8.
        "Printed side up, the wide way round. Fold the sheet in half, bringing the two short edges " +
            "together.",
            "Fold it in half again, bringing the bottom edge up to the top.",
            "And fold in half once more, the same way as the first fold.",
            "Open it all the way back out. Eight panels, eight pages.",
            // "through both layers" is the cold-read fix, and it is the only place in the guide where a
            // missing word costs a sheet of paper. Everything else here is recoverable by unfolding; the
            // cut is not. "One panel deep" is a distance across the paper and says nothing about depth,
            // so a careful first-timer cuts the top layer only, and finds out at step 7.
            "Fold in half the first way again, then cut in along the middle crease — from the folded " +
                "edge, through both layers, one panel deep.",
            "Open it flat and fold it the long way, so the slit sits in the middle of the folded edge.",
            "Hold both ends and push them towards each other. The slit opens up into a plus.",
            "Fold the four panels round into a book. Your cover lands on top.",
        )

        /**
         * `.foldnow` — **what you should be holding once the step is done**, one line per step.
         *
         * The freeze's own invention and the thing that makes the guide checkable: an instruction tells
         * you what to do, and only this tells you whether you did it. A user whose paper does not look
         * like the sentence knows to go back one step instead of pressing on and discovering it at the
         * cut.
         */
        public val STEP_HOLDING: List<String> = listOf(
            "You should be holding a tall half-sheet.",
            "Now a small square-ish quarter.",
            "A little eight-page bundle.",
            "Creases only — nothing is cut yet.",
            "One short slit. Nothing else gets cut.",
            "A long strip with a slot in the centre.",
            "Four panels standing in a cross.",
            "Done — an eight-page zine from one sheet.",
        )
    }

    /** The Read act — the finished zine, one leaf at a time (`ProofRead.kt`). */
    public object ProofRead {
        // `CONTENT_DESCRIPTION` was **deleted** by ADR-101 P5, and not because it went out of date. It
        // labelled the stage the reader sits on, which was right while the reader was a pager — the pager
        // *was* the control. With the booklet the controls are the two edges below, and with the stage
        // labelled, **both of them were absent from the platform accessibility tree**: the surface's only
        // way to turn a page had no `AccessibilityNodeInfo` at all, while every Compose-semantics
        // assertion passed. Deleting the label restored them.
        //
        // Stated as measured, not as a law. `ProofFold.StepDots` is a labelled non-merging container whose
        // clickable children *do* reach the platform (`ProofStepDotsA11yTest`), so "a labelled container
        // hides its children" is not a general rule and must not be repeated as one — what is general is
        // that only the platform tree can answer this, and only `SurfaceTraversalOrderTest` reads it.

        /** The two `.tapz` edges. Invisible by design, so the label is the whole control. */
        public const val PREVIOUS_PAGE: String = "Previous page"
        public const val NEXT_PAGE: String = "Next page"

        /**
         * **The readout speaks the booklet, and still counts** (ADR-101 P5).
         *
         * A one-sheet zine opens *cover · 2|3 · 4|5 · 6|7 · back*, and the two ends of that are not pages
         * you count to — they are the outside of the object. Naming them the way the frozen `.pcount` does
         * is what makes the reader feel like a booklet rather than a list with eight entries.
         *
         * **But the name alone cannot ship, and the review that caught it read the screen rather than the
         * spec.** Named only, the readout walks *Cover → 2, 3, 4, 5, 6, 7 → Back cover* directly above a
         * band that says **"8 pages · one sheet, one cut"**. There is no "Page 1" and no "Page 8", so the
         * honest cold reading is *"page 1 is missing"* — on the one screen whose whole job is to settle
         * whether the zine came out whole. The freeze does not have this problem because it stamps the
         * number on the leaf as well (`.pgn`), and Compose cannot: the leaf is the user's own artwork, and
         * [ADR-058](../../../../../../../docs/DECISIONS.md#adr-058) keeps the reader free of furniture
         * printed over it. So the number comes back here, where it costs nothing and contradicts nothing.
         *
         * One string for both the visible text and the announcement. An earlier build had them diverge —
         * the freeze's wording on screen, the denominator only for TalkBack — which was solving the wrong
         * half: the sighted reader is the one standing next to the contradicting band.
         */
        public fun leafLabel(pageNumber: Int, total: Int): String = when {
            pageNumber == 1 -> "Cover · 1 of $total"
            pageNumber == total -> "Back cover · $total of $total"
            else -> "Page $pageNumber of $total"
        }
    }
}
