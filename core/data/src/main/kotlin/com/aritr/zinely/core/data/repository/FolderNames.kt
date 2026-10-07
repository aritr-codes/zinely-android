package com.aritr.zinely.core.data.repository

import java.text.Normalizer
import java.util.Locale
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonPrimitive

/**
 * What a Shelf folder's name is ([ADR-125](docs/DECISIONS.md#adr-125) rule 13). A folder is not a thing on
 * disk: it is the name each of its zines carries, so this object is the only definition of a valid name and
 * of two names being the same folder.
 *
 * Invariant: **every name read from `meta.json` or from a backup goes through [clean] before it is compared,
 * shown or written**, so a hand-edited or hostile value cannot produce a name the Shelf cannot show.
 *
 * Nothing here depends on the device's language: case is folded with [Locale.ROOT], so a phone set to Turkish
 * groups the same zines as any other.
 */
public object FolderNames {
    /** The longest a name may be, counted in characters as a reader sees them. */
    public const val MAX_LENGTH: Int = 40

    /**
     * The longest a name may be in UTF-16 units, whichever limit is met first. One character a reader sees can
     * be made of any number of code points (a letter under a thousand marks), so [MAX_LENGTH] alone bounds
     * nothing. 160 is forty four-unit emoji, the longest ordinary name.
     */
    public const val MAX_UNITS: Int = 160

    /** The Shelf's own name. It is never a folder (rule 9), typed or read from a file. */
    public const val MY_SHELF: String = "My Shelf"

    /**
     * The name [raw] stands for, or `null` when it stands for no folder (My Shelf).
     *
     * Removes line breaks, control characters and invisible formatting characters, trims, normalises to NFC,
     * and cuts to [MAX_LENGTH] and [MAX_UNITS] at a character boundary ([cut]). A result with nothing to see in
     * it (blank, or only marks and blank-looking characters), or *My Shelf* in any capitals, is no folder.
     * Cleaning a cleaned name returns it unchanged.
     *
     * Only the first [MAX_READ] units of [raw] are looked at. A value from a backup or a hand-edited file can
     * be megabytes long, and normalising a long run of marks takes time that grows with its square.
     */
    public fun clean(raw: String?): String? {
        if (raw == null) return null
        val end = if (raw.length <= MAX_READ) raw.length else MAX_READ - (if (raw[MAX_READ - 1].isHighSurrogate()) 1 else 0)
        val visible = buildString(end) {
            var i = 0
            while (i < end) {
                val cp = raw.codePointAt(i)
                if (!isRemoved(cp)) appendCodePoint(cp)
                i += Character.charCount(cp)
            }
        }
        val name = cut(Normalizer.normalize(visible.trim(), Normalizer.Form.NFC)).trim()
        return name.takeUnless { !hasSomethingToSee(it) || looksLikeMyShelf(it) }
    }

    /** The value two names share exactly when they are the same folder: NFC, then lower case by [Locale.ROOT]. */
    public fun key(name: String): String = Normalizer.normalize(name, Normalizer.Form.NFC).lowercase(Locale.ROOT)

    /** Whether [a] and [b] name the same place. `null` is My Shelf. */
    public fun same(a: String?, b: String?): Boolean = a?.let(::key) == b?.let(::key)

    /**
     * The one spelling shown for a folder whose zines disagree, which only files changed outside the app can
     * cause: the spelling that sorts first by code point. `null` when [spellings] is empty.
     */
    public fun display(spellings: Iterable<String>): String? = spellings.minWithOrNull(BY_CODE_POINT)

    private val MY_SHELF_KEY = key(MY_SHELF)

    private val BY_CODE_POINT = Comparator<String> { a, b ->
        var i = 0
        var j = 0
        while (i < a.length && j < b.length) {
            val x = a.codePointAt(i)
            val y = b.codePointAt(j)
            if (x != y) return@Comparator x.compareTo(y)
            i += Character.charCount(x)
            j += Character.charCount(y)
        }
        (a.length - i).compareTo(b.length - j)
    }

    /** Whether [name] has one character that draws ink: not a space, a mark on its own, or a blank-looking one. */
    private fun hasSomethingToSee(name: String): Boolean {
        var i = 0
        while (i < name.length) {
            val cp = name.codePointAt(i)
            val draws = !Character.isWhitespace(cp) && !Character.isSpaceChar(cp) && cp !in BLANK_LOOKING && !isHidden(cp)
            if (draws) return true
            i += Character.charCount(cp)
        }
        return false
    }

    /**
     * A character that takes no room of its own: a formatting character, a mark that sits on another
     * character, or a code point Unicode reserves as ignorable. An unassigned code point outside those reserved
     * ranges is NOT hidden: an emoji newer than this device's tables is still a character someone typed.
     */
    private fun isHidden(cp: Int): Boolean = when (Character.getType(cp).toByte()) {
        Character.FORMAT, Character.NON_SPACING_MARK, Character.ENCLOSING_MARK -> true
        Character.UNASSIGNED -> cp == 0x2065 || cp in 0xFFF0..0xFFF8 || cp in 0xE0000..0xE0FFF
        else -> false
    }

    /**
     * Whether [name] reads as *My Shelf* (rule 9), judged more widely than [key]: hidden characters
     * ([isHidden]) are ignored, a run of spaces and blank-looking characters of any kind is one space, and
     * compatibility forms (fullwidth letters) are folded, so a stray joiner or a no-break space cannot smuggle
     * the Shelf's own name in as a folder. It errs toward refusing: a mark that cannot combine with its letter
     * is ignored too, and *My Shelf* typed with two spaces is refused.
     */
    private fun looksLikeMyShelf(name: String): Boolean {
        val skeleton = buildString(name.length) {
            var i = 0
            while (i < name.length) {
                val cp = name.codePointAt(i)
                when {
                    Character.isWhitespace(cp) || Character.isSpaceChar(cp) || cp in BLANK_LOOKING ->
                        if (!endsWith(' ')) append(' ')
                    isHidden(cp) -> Unit
                    else -> appendCodePoint(cp)
                }
                i += Character.charCount(cp)
            }
        }
        return key(Normalizer.normalize(skeleton.trim(), Normalizer.Form.NFKC)) == MY_SHELF_KEY
    }

    /**
     * Control characters, the line and paragraph separators, an unpaired surrogate, and the formatting
     * characters that change nothing a reader sees ([INVISIBLE]), so the commonest hidden characters cannot
     * make a second folder that looks like the first. The two joiners, variation selectors and tag characters
     * stay, because emoji and several scripts are spelled with them; a stray one still makes a different name.
     */
    private fun isRemoved(cp: Int): Boolean = cp in INVISIBLE || when (Character.getType(cp).toByte()) {
        Character.CONTROL, Character.LINE_SEPARATOR, Character.PARAGRAPH_SEPARATOR, Character.SURROGATE -> true
        else -> false
    }

    /**
     * Cuts [text] to [MAX_LENGTH] characters as a reader sees them, and to [MAX_UNITS].
     *
     * The rule is written out here, not taken from the platform's own text-breaking, so that it does not
     * change when that does. (The character tables it reads are still the device's.) It keeps together: a
     * letter and its combining marks; an emoji and its variation selector, skin tone, tag sequence or keycap;
     * emoji joined by a zero-width joiner; the two regional indicators of a flag; and, in the six scripts of
     * [VIRAMAS], a consonant joined to the next by a virama.
     *
     * It is an approximation of Unicode's grapheme clusters, not an implementation of them. It can still cut
     * inside what Unicode keeps as one in cases it does not list: old Hangul written as separate jamo, the
     * prepended signs of Arabic and some Indic scripts, and viramas outside [VIRAMAS].
     *
     * A character that would pass [MAX_UNITS] is left out whole, never cut through, so the result may be
     * shorter than [MAX_LENGTH] characters and is empty when the first character alone is too long.
     */
    private fun cut(text: String): String {
        var seen = 0
        var i = 0
        var start = 0 // where the character being read began
        var joined = false // the previous code point was a zero-width joiner
        var flagHalf = false // the previous code point opened a flag that this one may close
        var linked = false // a virama, or a virama and joiners, came just before: a letter now continues
        while (i < text.length) {
            val cp = text.codePointAt(i)
            val continues = i > 0 && (
                isExtender(cp) ||
                    // After a joiner only something that is not a letter or digit continues: emoji are joined
                    // this way, and a run of joined letters must not count as one endless character.
                    (joined && !Character.isLetterOrDigit(cp)) ||
                    (flagHalf && isRegionalIndicator(cp)) ||
                    (linked && Character.isLetter(cp))
                )
            if (!continues) {
                if (seen == MAX_LENGTH) return text.substring(0, i)
                seen++
                start = i
            }
            val next = i + Character.charCount(cp)
            if (next > MAX_UNITS) return text.substring(0, start)
            linked = cp in VIRAMAS || (linked && (cp == ZERO_WIDTH_JOINER || cp == ZERO_WIDTH_NON_JOINER))
            flagHalf = isRegionalIndicator(cp) && !(continues && flagHalf)
            joined = cp == ZERO_WIDTH_JOINER
            i = next
        }
        return text
    }

    private fun isExtender(cp: Int): Boolean = when {
        cp == ZERO_WIDTH_JOINER || cp == ZERO_WIDTH_NON_JOINER || cp == COMBINING_ENCLOSING_KEYCAP -> true
        cp == 0xFF9E || cp == 0xFF9F -> true // halfwidth katakana voicing marks
        cp in 0xFE00..0xFE0F || cp in 0xE0100..0xE01EF -> true // variation selectors
        cp in 0x1F3FB..0x1F3FF -> true // skin tones
        cp in 0xE0020..0xE007F -> true // tag sequences (subdivision flags)
        else -> when (Character.getType(cp).toByte()) {
            Character.NON_SPACING_MARK, Character.ENCLOSING_MARK, Character.COMBINING_SPACING_MARK -> true
            else -> false
        }
    }

    private fun isRegionalIndicator(cp: Int): Boolean = cp in 0x1F1E6..0x1F1FF

    private const val MAX_READ = 1024
    private const val ZERO_WIDTH_JOINER = 0x200D
    private const val ZERO_WIDTH_NON_JOINER = 0x200C
    private const val COMBINING_ENCLOSING_KEYCAP = 0x20E3

    /** Soft hyphen, zero-width space, word joiner and its kin, byte-order mark, and the direction marks. */
    private val INVISIBLE: Set<Int> = buildSet {
        addAll(listOf(0x00AD, 0x061C, 0x200B, 0x200E, 0x200F, 0xFEFF))
        addAll(0x202A..0x202E)
        addAll(0x2060..0x2064)
        addAll(0x2066..0x2069)
    }

    /** Characters that are not spaces to Unicode and still draw nothing: Hangul fillers and the blank braille cell. */
    private val BLANK_LOOKING: Set<Int> = setOf(0x115F, 0x1160, 0x3164, 0xFFA0, 0x2800)

    /**
     * Devanagari, Bengali, Gujarati, Oriya, Telugu, Malayalam: the scripts where Unicode joins consonants
     * across this mark (Unicode 15.1). Not Gurmukhi, Kannada, Tamil or Sinhala, which that rule leaves out.
     */
    private val VIRAMAS: Set<Int> = setOf(0x094D, 0x09CD, 0x0ACD, 0x0B4D, 0x0C4D, 0x0D4D)
}

/**
 * What a folder operation over several zines did ([ADR-125](docs/DECISIONS.md#adr-125) rule 18). Such an
 * operation is one file write per zine and is **not atomic across zines**: it can stop partway, and then
 * [failedIds] names the zines still where they were. Running the same operation again finishes it.
 */
public data class FolderChange(
    /** The folder the zines in [changedIds] are in now, in the spelling written; `null` is My Shelf. */
    val folder: String?,
    /** The zines whose folder was changed. */
    val changedIds: List<String>,
    /** The zines that were to change and could not be written. They are left exactly as they were. */
    val failedIds: List<String> = emptyList(),
    /** Why the first of [failedIds] could not be changed; `null` when none failed. */
    val cause: DataError? = null,
) {
    /** Whether every zine that was to change did. */
    val complete: Boolean get() = failedIds.isEmpty()
}

/**
 * Reads a folder name **leniently**: anything that is not a JSON string is no folder, and never an error.
 * A folder name is arrangement, not work, so a malformed one must not make a `meta.json` unreadable or a
 * backup unrestorable (rule 15, as `omitted` is read under ADR-122). The value is not cleaned here; callers
 * pass it through [FolderNames.clean]. Encoding is plain.
 */
public object LenientFolderNameSerializer : KSerializer<String?> {
    private val delegate = String.serializer().nullable
    override val descriptor: SerialDescriptor = delegate.descriptor

    override fun serialize(encoder: Encoder, value: String?): Unit = delegate.serialize(encoder, value)

    override fun deserialize(decoder: Decoder): String? {
        val element = (decoder as? JsonDecoder)?.decodeJsonElement() ?: return delegate.deserialize(decoder)
        return (element as? JsonPrimitive)?.takeIf { it.isString }?.content
    }
}
