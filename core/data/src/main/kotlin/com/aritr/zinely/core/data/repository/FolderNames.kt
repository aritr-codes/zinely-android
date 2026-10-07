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

    /** The Shelf's own name. It is never a folder (rule 9), typed or read from a file. */
    public const val MY_SHELF: String = "My Shelf"

    /**
     * The name [raw] stands for, or `null` when it stands for no folder (My Shelf).
     *
     * Removes line breaks and control characters, trims, normalises to NFC, and cuts to [MAX_LENGTH] without
     * splitting a character a reader sees as one. A result with nothing to see in it (blank, or only invisible
     * formatting characters), or *My Shelf* in any capitals, is no folder.
     * Cleaning a cleaned name returns it unchanged.
     */
    public fun clean(raw: String?): String? {
        if (raw == null) return null
        val visible = buildString(raw.length) {
            var i = 0
            while (i < raw.length) {
                val cp = raw.codePointAt(i)
                if (!isRemoved(cp)) appendCodePoint(cp)
                i += Character.charCount(cp)
            }
        }
        val name = cut(Normalizer.normalize(visible.trim(), Normalizer.Form.NFC)).trim()
        return name.takeUnless { !hasSomethingToSee(it) || key(it) == MY_SHELF_KEY }
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

    private fun hasSomethingToSee(name: String): Boolean {
        var i = 0
        while (i < name.length) {
            val cp = name.codePointAt(i)
            if (!Character.isWhitespace(cp) && !Character.isSpaceChar(cp) && Character.getType(cp).toByte() != Character.FORMAT) {
                return true
            }
            i += Character.charCount(cp)
        }
        return false
    }

    /** Control characters, and the line and paragraph separators. An unpaired surrogate goes too. */
    private fun isRemoved(cp: Int): Boolean = when (Character.getType(cp).toByte()) {
        Character.CONTROL, Character.LINE_SEPARATOR, Character.PARAGRAPH_SEPARATOR, Character.SURROGATE -> true
        else -> false
    }

    /**
     * Cuts [text] to [MAX_LENGTH] characters as a reader sees them.
     *
     * The rule is written out here, not taken from the platform's own text-breaking, so the same name is cut
     * at the same place on every Android version. It keeps together: a letter and its combining marks; an
     * emoji and its variation selector, skin tone, tag sequence or keycap; emoji joined by a zero-width
     * joiner; and the two regional indicators of a flag. It is an approximation of Unicode's grapheme
     * clusters that errs toward keeping more together, never toward splitting one.
     */
    private fun cut(text: String): String {
        var seen = 0
        var i = 0
        var joined = false // the previous code point was a zero-width joiner
        var flagHalf = false // the previous code point opened a flag that this one may close
        while (i < text.length) {
            val cp = text.codePointAt(i)
            val continues = when {
                i == 0 -> false
                joined -> true
                isExtender(cp) -> true
                flagHalf && isRegionalIndicator(cp) -> true
                else -> false
            }
            if (!continues) {
                if (seen == MAX_LENGTH) return text.substring(0, i)
                seen++
            }
            flagHalf = isRegionalIndicator(cp) && !(continues && flagHalf)
            joined = cp == ZERO_WIDTH_JOINER
            i += Character.charCount(cp)
        }
        return text
    }

    private fun isExtender(cp: Int): Boolean = when {
        cp == ZERO_WIDTH_JOINER || cp == COMBINING_ENCLOSING_KEYCAP -> true
        cp in 0xFE00..0xFE0F || cp in 0xE0100..0xE01EF -> true // variation selectors
        cp in 0x1F3FB..0x1F3FF -> true // skin tones
        cp in 0xE0020..0xE007F -> true // tag sequences (subdivision flags)
        else -> when (Character.getType(cp).toByte()) {
            Character.NON_SPACING_MARK, Character.ENCLOSING_MARK, Character.COMBINING_SPACING_MARK -> true
            else -> false
        }
    }

    private fun isRegionalIndicator(cp: Int): Boolean = cp in 0x1F1E6..0x1F1FF

    private const val ZERO_WIDTH_JOINER = 0x200D
    private const val COMBINING_ENCLOSING_KEYCAP = 0x20E3
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
