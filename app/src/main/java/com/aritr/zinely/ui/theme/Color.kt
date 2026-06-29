package com.aritr.zinely.ui.theme

import androidx.compose.ui.graphics.Color

// Zinely "workbench" palette (docs/design/editor-visual-direction.md §3) — paper & ink with a
// small set of craft-accent tape/sticker colors. Replaces the default Android-template Purple/Pink
// so the print-brand identity is consistent (dynamic color is off — see Theme.kt).

/** Warm off-white of the page sheet and chrome cards. */
val ZinePaper = Color(0xFFF4EFE6)
/** Slightly darker paper for card edges / inner shadow. */
val ZinePaperEdge = Color(0xFFE7DFD0)
/** Muted slate of the worktable behind the bright sheet. */
val ZineDesk = Color(0xFF3A3A3C)
/** Near-black ink for primary text. */
val ZineInk = Color(0xFF23201C)
/** Soft brown-grey for secondary text. */
val ZineInkSoft = Color(0xFF6B6358)

// Craft accents (tape / sticker / stamp).
val TapeYellow = Color(0xFFE9C46A)
val TapeCoral = Color(0xFFE76F51)
val TapeTeal = Color(0xFF2A9D8F)
val StampBlue = Color(0xFF264653)
