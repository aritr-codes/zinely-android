package com.aritr.zinely.core.model

/**
 * The two document voices a text can be set in ([ADR-126](../../../../../../../../docs/DECISIONS.md#adr-126)).
 *
 * A voice is **not** a saved value: the document still carries only `TextStyle.fontFamily`, an existing
 * string, so the saved format does not change. This is the reading of that string.
 *
 * @property familyName what choosing the voice **writes**. Plain writes `"sans-serif"`, the value every
 *   text has always held, so a text returned to Plain is byte-identical to one never changed. Book writes
 *   the registered family name. Ids are family names, never the display words.
 * @property scripts the scripts the voice sets. This is the promise the typing-time check applies and the
 *   font-file guard in `:render-android` measures the real faces against, so the two cannot drift apart.
 */
public enum class DocumentVoice(public val familyName: String, public val scripts: Set<Script>) {
    /** Inter. Exactly today's promise ([SupportedScripts.BUNDLED_SCRIPTS]). */
    PLAIN("sans-serif", SupportedScripts.BUNDLED_SCRIPTS),

    /** Fraunces. Latin only: the four faces hold no Greek and no Cyrillic. Emoji have their own font. */
    BOOK("Fraunces", setOf(Script.LATIN, Script.EMOJI)),
    ;

    public companion object {
        /**
         * The smallest size Book may be used at, in points. **One named value**: the owner ruled 12 pt on
         * 2026-10-08 *to be confirmed on the printed page*, and that page may change it. At or below the
         * size ramp's own bottom (10 pt) the whole mechanism falls away by itself.
         */
        public const val BOOK_MIN_SIZE_PT: Double = 12.0

        /**
         * The voice [fontFamily] names, or `null` for a family this build does not know.
         *
         * `"sans-serif"` and `"Inter"` are both Plain. Matching is trimmed and case-insensitive, the same
         * rule the font registry resolves by, so the two agree on what counts as known.
         */
        public fun of(fontFamily: String): DocumentVoice? = when (fontFamily.trim().lowercase()) {
            "sans-serif", "inter" -> PLAIN
            "fraunces" -> BOOK
            else -> null
        }

        /** The voice [fontFamily] is **drawn** in: an unknown family is drawn in Inter, so it is checked as Plain. */
        public fun drawnAs(fontFamily: String): DocumentVoice = of(fontFamily) ?: PLAIN
    }
}
