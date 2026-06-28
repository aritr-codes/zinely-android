package com.aritr.zinely.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// The fixed "workbench" identity (docs/design/editor-visual-direction.md). M3 roles are mapped to
// the zine palette so the existing Material surfaces (context bar, text-edit sheet, FAB) pick up the
// identity without per-call recolouring: surface = paper, onSurface = ink, primary = coral tape.
private val ZineLightScheme = lightColorScheme(
    primary = TapeCoral,
    onPrimary = Color.White,
    secondary = TapeTeal,
    onSecondary = Color.White,
    tertiary = TapeYellow,
    onTertiary = ZineInk,
    background = ZineDesk,
    onBackground = ZinePaper,
    surface = ZinePaper,
    onSurface = ZineInk,
    surfaceVariant = ZinePaperEdge,
    onSurfaceVariant = ZineInkSoft,
    outline = ZineInkSoft,
)

// Dark theme keeps the same identity but leans the worktable darker; the paper sheet stays warm so
// the printed artifact still previews true-to-paper.
private val ZineDarkScheme = darkColorScheme(
    primary = TapeCoral,
    onPrimary = Color.White,
    secondary = TapeTeal,
    onSecondary = Color.White,
    tertiary = TapeYellow,
    onTertiary = ZineInk,
    background = Color(0xFF1F1F21),
    onBackground = ZinePaper,
    surface = ZinePaper,
    onSurface = ZineInk,
    surfaceVariant = ZinePaperEdge,
    onSurfaceVariant = ZineInkSoft,
    outline = ZineInkSoft,
)

@Composable
fun ZinelyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is OFF by default: a print-brand app needs one consistent identity, not the
    // user's wallpaper palette (design §3). Callers may still opt in on Android 12+.
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> ZineDarkScheme
        else -> ZineLightScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
