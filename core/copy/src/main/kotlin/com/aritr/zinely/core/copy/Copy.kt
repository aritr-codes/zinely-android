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
        /** The product wordmark as rendered in the UI (distinct from the launcher `app_name`, CI-86). */
        public const val BRAND: String = "Zinely"
    }

    /** Nav-host / share plumbing strings (`ZinelyNavHost.kt`, CI-82/CI-83). */
    public object Nav {
        public const val SHARE_CHOOSER_TITLE: String = "Share your zine"
        public const val NO_APP_TO_OPEN: String = "No app on your phone can open that yet."

        /**
         * Neutral fallback for the Proof top-bar title. CI-82 relocates it here **character-identical**;
         * threading the real Room/ADR-042 project title through is a behavioural follow-up, not this move.
         */
        public const val ZINE_NAME_FALLBACK: String = "Your zine"
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
        public const val REPLACE: String = "Replace"
        public const val DELETE: String = "Delete"
    }

    /** Editor canvas surface — reframe announcements, the whole-photo inert line, the Preview action. */
    public object Editor {
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

    /** First-run / empty-page invitation (`EditorEmptyState.kt`). */
    public object EmptyState {
        public const val ADD_A_PHOTO: String = "Add a photo"
        public const val ADD_WORDS: String = "Add words"
        public const val FIRST_PAGE_HEADLINE: String = "Let's make something cute ✨"
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

    /** Page-strip thumbnails (`EditorPageStrip.kt`). */
    public object PageStrip {
        public fun pageNumber(number: Int): String = "Page $number"
        public const val CURRENT_PAGE: String = "Current page"
        public const val NOT_SELECTED: String = "Not selected"
    }

    /** Editor effect outcomes surfaced to the user (`EditorEffects.kt`). */
    public object Effects {
        public const val ADDING_IMAGES_UNAVAILABLE: String = "Adding images isn’t available yet"
    }

    /** Inline text-editor (`EditTextSession.kt`). */
    public object EditText {
        public const val ZINE_TEXT: String = "Zine text"
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
        public const val FILL: String = "Fill"
        public const val CROPS_EDGES: String = "crops edges"
        public const val WHOLE_PHOTO: String = "Whole photo"
        public const val MAY_ADD_MARGINS: String = "may add margins"
        public const val CANCEL_REFRAMING: String = "Cancel reframing"
        public const val DONE_REFRAMING: String = "Done reframing"
        public const val REFRAME_THIS_PHOTO: String = "Reframe this photo"
        public const val CANCEL: String = "Cancel"
        public const val DONE: String = "Done"
        public const val REFRAME: String = "Reframe"
        public fun zoomPercentText(percent: Int): String = "$percent%"
        public fun zoomPercentAnnouncement(percent: Int): String = "Zoom $percent percent"
    }

    /** The shelf / home library (`ShelfStates.kt`, `ShelfSheets.kt`, `ShelfCard.kt`, `HomeScreen.kt`). */
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
        public const val LETTER: String = "Letter"
        public const val A4_DIMENSIONS: String = "210 × 297 mm"
        public const val LETTER_DIMENSIONS: String = "8.5 × 11 in"
        public const val A4_DIMENSIONS_LONG: String = "210 × 297 mm — most of the world"
        public const val LETTER_DIMENSIONS_LONG: String = "8.5 × 11 in — US & Canada"
    }

    /** The Proof surface top bar + action bar (`ProofScreen.kt`). "Try again" is [Common.TRY_AGAIN]. */
    public object Proof {
        public const val ACT_READ: String = "Read · swipe to turn the page"
        public const val ACT_SHEET: String = "Step 1 of 3 · The sheet"
        public const val ACT_PRINT: String = "Step 2 of 3 · Print"
        public const val ACT_FOLD: String = "Step 3 of 3 · Fold"
        public const val DONE_READY: String = "Done · Your zine is ready"
        public const val FOLD_NOW: String = "Fold now"
        public const val COULDNT_MAKE_PDF: String = "Couldn’t make the PDF"
        public const val ERROR_BODY: String =
            "Your zine is safe on this device — the export just didn’t finish. Try once more."
        public const val BACK_TO_YOUR_ZINE: String = "Back to your zine"
        public const val BACK_TO_BENCH_SAVED: String = "Back to the bench (your work is saved)"
        public const val PRINT_AND_FOLD: String = "Print & fold"
        public const val PRINT_SETUP: String = "Print setup"
        public const val BACK: String = "Back"
        public const val NOW_FOLD_IT: String = "Now fold it"
        public const val ITS_FOLDED: String = "It’s folded — show me"
        public const val BACK_TO_BENCH: String = "Back to bench"
        public const val MAKE_ANOTHER: String = "Make another"
        public fun savedToDownloads(name: String): String = "Saved “$name” to Downloads"
    }

    /** The imposed sheet act (`ProofSheet.kt`). */
    public object ProofSheet {
        public const val TITLE: String = "This is your sheet"
        public const val BODY: String =
            "One page, printed on one side. It looks scrambled on purpose — " +
                "the fold puts every page in order."
        public const val CONTENT_DESCRIPTION: String =
            "Your zine imposed on one landscape sheet: eight panels, " +
                "the top row upside-down, with one cut line across the centre."
        public const val ONE_CUT: String = "ONE CUT"
        public const val LEGEND_FOLD_LINES: String = "fold lines"
        public const val LEGEND_ONE_CUT: String = "the one cut"
        public const val LEGEND_PRINTER_REACH: String = "printer can’t reach here"
        public const val FRONT_COVER: String = "Front cover"
        public const val BACK_COVER: String = "Back cover"
    }

    /** The print recipe act (`ProofPrint.kt`). Share sheet title is [Nav.SHARE_CHOOSER_TITLE]. */
    public object ProofPrint {
        public const val TITLE: String = "Print it just like this"
        public const val BODY: String =
            "These four settings keep your zine the right size and in the right order. " +
                "Most printers already default to them."
        public const val SCALE_LABEL: String = "Scale"
        public const val SCALE_VALUE: String = "100% · Actual size"
        public const val SCALE_EMPHASIS: String = " — not “Fit to page”"
        public const val ORIENTATION_LABEL: String = "Orientation"
        public const val LANDSCAPE: String = "Landscape"
        public const val ORIENTATION_EMPHASIS: String = " — a portrait default breaks the fold"
        public const val PAPER_LABEL: String = "Paper"
        public const val SIDES_LABEL: String = "Sides"
        public const val SIDES_VALUE: String = "Single-sided — one side only"
        public const val SAVE_PDF: String = "Save PDF"
        public const val SHARE: String = "Share"
        public const val PAPER_SIZE_TITLE: String = "Paper size"
        public const val PAPER_SIZE_SUB: String =
            "Match this to the paper in your printer, so nothing gets clipped or shrunk."
        public const val SHARE_SUB: String =
            "The PDF stays on your device — you choose where it goes. Nothing is uploaded by Zinely."
        public const val SAVE_TO_FILES: String = "Save to Files"
        public const val SEND_TO_AN_APP: String = "Send to an app"
        public const val CHANGE: String = "Change"
        // The double-sided help line, built in three spans (the middle span is bold "single-sided").
        public const val SIDES_HELP_PREFIX: String = "If your printer asks about double-sided, choose "
        public const val SIDES_HELP_BOLD: String = "single-sided"
        public const val SIDES_HELP_SUFFIX: String = " (or “off”). A mini-zine prints on one side, then folds."
    }

    /** The fold guide act + climax (`ProofFold.kt`). */
    public object ProofFold {
        public const val INTRO_TITLE: String = "Fold it into a book"
        public const val INTRO_BODY: String = "Five steps. Take them one at a time — tap the arrow when a step is done."
        public const val PREVIOUS_STEP: String = "Previous step"
        public const val NEXT_STEP: String = "Next step"
        public const val DONE_HEADING: String = "Your zine is a book."
        public const val DONE_BODY: String =
            "Eight pages, made by hand, kept on this device. It’s on your shelf whenever you want it."

        // The five steps — title · body.
        public const val STEP1_TITLE: String = "Crease into eight"
        public const val STEP1_BODY: String =
            "Fold the sheet in half three times, then open it flat. You’ll see eight panels. " +
                "All folds are valleys."
        public const val STEP2_TITLE: String = "One cut — the only cut"
        public const val STEP2_BODY: String =
            "Fold in half short-end to short-end. Cut the slit across the two middle panels, " +
                "and stop at the quarter lines."
        public const val STEP3_TITLE: String = "Open it back up"
        public const val STEP3_BODY: String =
            "Lay the sheet flat again. The cut has become a small slot right through the middle."
        public const val STEP4_TITLE: String = "Fold the long way"
        public const val STEP4_BODY: String =
            "Fold the sheet in half so the long edges meet — one wide strip. The cut opens into a " +
                "diamond right on the fold."
        public const val STEP5_TITLE: String = "Push in and wrap"
        public const val STEP5_BODY: String =
            "Push the two ends toward the middle so the panels pop into a plus, then wrap them flat — " +
                "front cover facing out."

        // Diagram captions.
        public const val DIAGRAM_EIGHT_PANELS: String = "eight panels"
        public const val DIAGRAM_CUT_HERE: String = "CUT HERE"
        public const val DIAGRAM_ONE_WIDE_STRIP: String = "one wide strip"
        public const val DIAGRAM_WRAP_TO_A_BOOK: String = "wrap to a book"

        /** Numbered step heading, e.g. `1. Crease into eight`. */
        public fun stepHeading(number: Int, title: String): String = "$number. $title"
    }

    /** The Read act — the finished zine, page by page (`ProofRead.kt`). */
    public object ProofRead {
        public const val CONTENT_DESCRIPTION: String = "Your zine, page by page. Swipe to turn the page."
        public fun pageOf(current: Int, total: Int): String = "Page $current of $total"
    }
}
