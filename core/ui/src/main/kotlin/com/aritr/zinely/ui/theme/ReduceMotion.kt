package com.aritr.zinely.ui.theme

import android.content.Context
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode

/**
 * Android's equivalent of the web's `prefers-reduced-motion: reduce`.
 *
 * Settings > Accessibility > "Remove animations" writes `ANIMATOR_DURATION_SCALE = 0`. That is the
 * signal the platform's own `MotionSpec`/`accessibility` guidance keys off, and the one Compose's
 * animation clock does *not* honour automatically — so every animated surface must consult this.
 *
 * ponytail: read once per composition. A [android.database.ContentObserver] would react to a live
 * toggle mid-session; wire one only if a surface is ever observed animating after the user flips the
 * switch without leaving the screen.
 */
@Composable
public fun rememberReduceMotion(): Boolean {
    if (LocalInspectionMode.current) return false // @Preview has no real Settings provider
    val context = LocalContext.current
    return remember(context) { isReduceMotionEnabled(context) }
}

/** The non-composable seam, so the rule is unit-testable and callable outside composition. */
public fun isReduceMotionEnabled(context: Context): Boolean =
    Settings.Global.getFloat(
        context.contentResolver,
        Settings.Global.ANIMATOR_DURATION_SCALE,
        1.0f,
    ) == 0.0f
