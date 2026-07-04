package com.aritr.zinely.feature.editor

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Secondary text drawn directly on the desk (`background`). The workbench theme keeps the desk dark
 * in BOTH modes and binds `onSurfaceVariant` to *paper* ink (soft brown), so desk-level copy must
 * pair with `onBackground` — softened here to the mockups' quieter desk tone (e.g. `#d8d1c4`),
 * since M3 has no on-background variant role. Primary desk text uses `onBackground` directly.
 */
internal val ColorScheme.deskTextSoft: Color
    get() = onBackground.copy(alpha = 0.8f)
