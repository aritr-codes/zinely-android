package com.aritr.zinely.core.editor.ink

public sealed interface ToggleResult {
    public data class Added(val draft: PaletteDraft) : ToggleResult
    public data class Removed(val draft: PaletteDraft) : ToggleResult

    /** Six picked and the hex is absent: "A palette holds 6". */
    public data object Full : ToggleResult
}

/** Mixing into a palette (rev. 2): three messages, not one. */
public sealed interface MixIntoPalette {
    public data class Added(val draft: PaletteDraft) : MixIntoPalette
    public data object AlreadyIn : MixIntoPalette
    public data object PaletteFull : MixIntoPalette
}

/**
 * A palette being made or edited (prototype `edit`). [name] is the live field; [startName] is the name the
 * draft opened with and never changes (the prototype's `edit.name` / `edit.origName`, which typing never
 * updates). [orig] is the inks it opened with: empty for a new draft.
 */
public data class PaletteDraft(
    val id: Long?,
    val name: String,
    val startName: String,
    val inks: List<InkHex>,
    val orig: List<InkHex>,
) {
    public fun toggle(hex: InkHex): ToggleResult = when {
        hex in inks -> ToggleResult.Removed(copy(inks = inks - hex))
        inks.size >= PALETTE_MAX -> ToggleResult.Full
        else -> ToggleResult.Added(copy(inks = inks + hex))
    }

    public fun removeAt(index: Int): PaletteDraft =
        if (index in inks.indices) copy(inks = inks.filterIndexed { i, _ -> i != index }) else this

    /** Already in is checked before full (prototype `has` first): at six, a present hex says "already in". */
    public fun addMixed(hex: InkHex): MixIntoPalette = when {
        hex in inks -> MixIntoPalette.AlreadyIn
        inks.size >= PALETTE_MAX -> MixIntoPalette.PaletteFull
        else -> MixIntoPalette.Added(copy(inks = inks + hex))
    }

    /** "Was in this palette": inks the draft opened with, now removed, that nothing else still offers. */
    public fun lost(library: InkLibrary, builtIns: List<InkHex>): List<InkHex> =
        orig.filter { it !in inks && library.kept(it) == null && it !in builtIns }

    public fun canSave(): Boolean = name.trim().isNotEmpty() && inks.size >= PALETTE_MIN

    /** Drives "Palette not saved". A new draft is compared with `[current]`, which it opened holding. */
    public fun changed(current: InkHex): Boolean =
        name.trim() != startName || inks != (if (id != null) orig else listOf(current))

    /** The "That's fine" note: another palette (not this one) already has this name, ignoring case. */
    public fun isDuplicateName(library: InkLibrary): Boolean {
        val v = name.trim()
        return v.isNotEmpty() && library.palettes.any { it.id != id && it.name.equals(v, ignoreCase = true) }
    }

    public companion object {
        public fun newDraft(library: InkLibrary, current: InkHex, defaultName: String): PaletteDraft {
            val n = library.uniqueName(defaultName)
            return PaletteDraft(null, n, n, listOf(current), emptyList())
        }

        public fun editDraft(p: InkPalette): PaletteDraft = PaletteDraft(p.id, p.name, p.name, p.inks, p.inks)
    }
}
