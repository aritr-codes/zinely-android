package com.aritr.zinely.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// ---------------------------------------------------------------------------------------------
// The token layer.
//
// Every surface reads its design values from these CompositionLocals, never from a literal at the
// call site and never from `MaterialTheme.colorScheme` (whose roles are a different vocabulary).
// `staticCompositionLocalOf` is right for all of them: the values change only when the theme flips,
// so a whole-subtree recomposition beats the read-tracking overhead of a dynamic local.
//
// Each errors rather than defaults when unprovided — an accidental read outside `ZinelyTheme` is a
// bug, not something to paper over with a silently wrong palette.
// ---------------------------------------------------------------------------------------------

public val LocalZinelyColors: ProvidableCompositionLocal<ZinelyColors> =
    staticCompositionLocalOf { error("No ZinelyColors provided — wrap the tree in ZinelyTheme.") }

public val LocalZinelyElevation: ProvidableCompositionLocal<ZinelyElevation> =
    staticCompositionLocalOf { error("No ZinelyElevation provided — wrap the tree in ZinelyTheme.") }

public val LocalZinelyMotion: ProvidableCompositionLocal<ZinelyMotion> =
    staticCompositionLocalOf { error("No ZinelyMotion provided — wrap the tree in ZinelyTheme.") }

public val LocalZinelyHaptics: ProvidableCompositionLocal<ZinelyHaptics> =
    staticCompositionLocalOf { error("No ZinelyHaptics provided — wrap the tree in ZinelyTheme.") }

public val LocalZinelyTypography: ProvidableCompositionLocal<ZinelyTypography> =
    staticCompositionLocalOf { error("No ZinelyTypography provided — wrap the tree in ZinelyTheme.") }

/**
 * Convenience accessors, mirroring the shape of `MaterialTheme.colorScheme`.
 *
 * ```kotlin
 * Box(Modifier.background(ZinelyTheme.colors.desk))
 * ```
 */
public object ZinelyTheme {
    public val colors: ZinelyColors
        @Composable get() = LocalZinelyColors.current

    public val elevation: ZinelyElevation
        @Composable get() = LocalZinelyElevation.current

    public val motion: ZinelyMotion
        @Composable get() = LocalZinelyMotion.current

    public val haptics: ZinelyHaptics
        @Composable get() = LocalZinelyHaptics.current

    public val typography: ZinelyTypography
        @Composable get() = LocalZinelyTypography.current
}

// ---------------------------------------------------------------------------------------------
// The Material 3 scheme.
//
// These are the PRE-RESKIN values, preserved byte-for-byte from the theme this milestone replaced
// (they used to live in a since-deleted Color.kt). They are not the frozen riso tokens, and are not
// meant to be: screens that have not been reskinned yet still read `MaterialTheme.colorScheme`, and
// M0's brief is "no visual regressions". Note the role abuse the reskin will unwind — `background`
// is a slate grey that does not exist in the frozen palette, and `surface` is paper.
//
// Each screen drops its MaterialTheme reads as it migrates onto the token layer (M2–M5); when the
// last one has, this scheme goes away with it.
// ---------------------------------------------------------------------------------------------

private val LegacyPaper = Color(0xFFF4EFE6)
private val LegacyPaperEdge = Color(0xFFE7DFD0)
private val LegacyDesk = Color(0xFF3A3A3C)
private val LegacyInk = Color(0xFF23201C)
private val LegacyInkSoft = Color(0xFF6B6358)
private val LegacyCoral = Color(0xFFE76F51)
private val LegacyTeal = Color(0xFF2A9D8F)
private val LegacyYellow = Color(0xFFE9C46A)

private val LegacyLightScheme = lightColorScheme(
    primary = LegacyCoral,
    onPrimary = Color.White,
    secondary = LegacyTeal,
    onSecondary = Color.White,
    tertiary = LegacyYellow,
    onTertiary = LegacyInk,
    background = LegacyDesk,
    onBackground = LegacyPaper,
    surface = LegacyPaper,
    onSurface = LegacyInk,
    surfaceVariant = LegacyPaperEdge,
    onSurfaceVariant = LegacyInkSoft,
    outline = LegacyInkSoft,
)

private val LegacyDarkScheme = darkColorScheme(
    primary = LegacyCoral,
    onPrimary = Color.White,
    secondary = LegacyTeal,
    onSecondary = Color.White,
    tertiary = LegacyYellow,
    onTertiary = LegacyInk,
    background = Color(0xFF1F1F21),
    onBackground = LegacyPaper,
    surface = LegacyPaper,
    onSurface = LegacyInk,
    surfaceVariant = LegacyPaperEdge,
    onSurfaceVariant = LegacyInkSoft,
    outline = LegacyInkSoft,
)

/**
 * The one theme. Wrap the whole application in it.
 *
 * There is no `dynamicColor` parameter. Zinely is a print-brand app with a fixed riso identity — the
 * frozen spec states "dynamic color OFF" — and a knob that can repaint that identity from the user's
 * wallpaper is a knob that eventually gets turned. It is deleted, not defaulted off.
 *
 * Reduced motion resolves once, here, and threads down: it silences animation ([ZinelyMotion]) *and*
 * haptics ([ZinelyHaptics]), exactly as the spec's `buzz()` guard does.
 */
@Composable
public fun ZinelyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val reduceMotion = rememberReduceMotion()
    val colors = remember(darkTheme) { if (darkTheme) zinelyDarkColors() else zinelyLightColors() }
    val elevation = remember(darkTheme) { if (darkTheme) zinelyDarkElevation() else zinelyLightElevation() }
    val motion = remember(reduceMotion) { ZinelyMotion(reduceMotion = reduceMotion) }
    val typography = remember { ZinelyTypography() }
    val haptics = rememberZinelyHaptics(reduceMotion)

    CompositionLocalProvider(
        LocalZinelyColors provides colors,
        LocalZinelyElevation provides elevation,
        LocalZinelyMotion provides motion,
        LocalZinelyHaptics provides haptics,
        LocalZinelyTypography provides typography,
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) LegacyDarkScheme else LegacyLightScheme,
            typography = Typography,
            content = content,
        )
    }
}
