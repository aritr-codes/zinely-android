package com.aritr.zinely.core.data.repository

import com.aritr.zinely.core.model.ColorRgba

/**
 * The paper a zine is meant to be printed on ([ADR-150](docs/DECISIONS.md#adr-150) decisions 3 to 5; design AW6.5 in
 * `v21-library.html`). A shelf detail kept in `meta.json`, never in `document.json`.
 *
 * **Invariant: nothing that writes a PDF or PNG receives this type.** It lives here, in the shelf layer, and not in
 * `core:render` or the export path; the Compose host multiplies [srgb] over the page on the surfaces AW6.7 names.
 *
 * The set is closed. [id] is permanent once shipped and is what is stored ([WHITE] is stored as no key at all);
 * [srgb] may be retuned (R41's values are estimates), never renamed. Adding a paper is add-only and needs no
 * schema bump. Brown was cut by AW6.6's gate. Pastel orange, Pastel purple and Light brown were added by A56.8 (ADR-152).
 *
 * The declaration order IS the chooser's order (A56.3: light and warm, then cool and neutral). Nothing stores or compares an
 * ordinal, so inserting a paper mid-list is safe; a test pins the order.
 */
public enum class PaperColour(public val id: String, public val srgb: ColorRgba) {
    WHITE("white", ColorRgba(0xFF, 0xFF, 0xFF, 0xFF)),
    IVORY("ivory", ColorRgba(0xF4, 0xEE, 0xDC, 0xFF)),
    PASTEL_YELLOW("pastel-yellow", ColorRgba(0xF7, 0xEC, 0x9B, 0xFF)),
    PASTEL_ORANGE("pastel-orange", ColorRgba(0xF5, 0xCF, 0xA8, 0xFF)),
    PASTEL_PINK("pastel-pink", ColorRgba(0xF4, 0xC6, 0xD0, 0xFF)),
    PASTEL_PURPLE("pastel-purple", ColorRgba(0xDC, 0xD0, 0xEC, 0xFF)),
    PASTEL_BLUE("pastel-blue", ColorRgba(0xBC, 0xD6, 0xEA, 0xFF)),
    PASTEL_GREEN("pastel-green", ColorRgba(0xCD, 0xE6, 0xC8, 0xFF)),
    LIGHT_BROWN("light-brown", ColorRgba(0xE8, 0xD3, 0xA9, 0xFF)),
    GREY("grey", ColorRgba(0xCF, 0xCD, 0xC6, 0xFF)),
    ;

    /** What `meta.json` stores: `null` for [WHITE] (absent key), else [id]. */
    public val storedId: String? get() = if (this == WHITE) null else id

    /**
     * The colour the host multiplies over a page for this paper, or `null` for [WHITE]: White draws no layer at all,
     * so every White zine's screens stay byte-identical (AW6.7, ADR-150 decision 5).
     */
    public val overlay: ColorRgba? get() = if (this == WHITE) null else srgb

    public companion object {
        /**
         * The colour a stored id names. Absent, unknown (a later build's paper) or malformed reads as [WHITE]; the raw
         * string itself stays on disk untouched, so this is only for drawing and naming.
         */
        public fun fromId(raw: String?): PaperColour = entries.firstOrNull { it.id == raw } ?: WHITE
    }
}
