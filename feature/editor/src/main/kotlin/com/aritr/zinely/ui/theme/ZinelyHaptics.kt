package com.aritr.zinely.ui.theme

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * The four haptic verbs of the frozen spec — the complete vocabulary. There is no fifth verb, and a
 * surface never invents a pattern inline.
 *
 * ```js
 * const HAPTIC = { tick:[8], snap:[6,20,10], boundary:[24], success:[12,30,12,30] };
 * ```
 *
 * @property onOffMillis the pattern exactly as the spec writes it: **ON-first**, alternating
 *   on/off durations — the `navigator.vibrate()` convention.
 */
public enum class ZinelyHaptic(public val onOffMillis: LongArray) {
    /** A value ticked past a detent. */
    Tick(longArrayOf(8)),

    /** Something snapped into place. */
    Snap(longArrayOf(6, 20, 10)),

    /** A limit was reached; the gesture cannot go further. */
    Boundary(longArrayOf(24)),

    /** The work landed (exported, saved, folded). */
    Success(longArrayOf(12, 30, 12, 30)),
}

/**
 * Translate a spec pattern into the array Android's vibrator APIs expect.
 *
 * **This is not a copy.** `navigator.vibrate([8])` means *vibrate for 8ms*, but both
 * [VibrationEffect.createWaveform] and the legacy [Vibrator.vibrate] read `timings[0]` as *how long
 * to wait before turning the vibrator on*. Handing the spec array over verbatim inverts every
 * pattern — `Tick` would become an 8ms silence. Prepending a zero-length wait realigns the phases
 * and preserves the spec's default amplitude.
 */
internal fun androidTimings(haptic: ZinelyHaptic): LongArray =
    LongArray(haptic.onOffMillis.size + 1) { i ->
        if (i == 0) 0L else haptic.onOffMillis[i - 1]
    }

/**
 * Plays the four verbs. Obtain it from [LocalZinelyHaptics] — never construct one at a call site.
 *
 * Mirrors the spec's `buzz()` guard exactly: `if (canVibe && !reduced()) navigator.vibrate(...)`.
 * Reduced motion therefore silences haptics too, which is deliberate — the vibration is part of the
 * same motion budget.
 */
@Stable
public class ZinelyHaptics internal constructor(
    private val vibrator: Vibrator?,
    private val enabled: Boolean,
) {
    /** `canVibe && !reduced()` */
    public val isAvailable: Boolean get() = enabled && vibrator?.hasVibrator() == true

    public fun perform(haptic: ZinelyHaptic) {
        if (!enabled) return
        val vibrator = vibrator?.takeIf { it.hasVibrator() } ?: return
        val timings = androidTimings(haptic)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(timings, NO_REPEAT))
        } else {
            @Suppress("DEPRECATION") // the only vibrate() that exists below API 26 (minSdk 24)
            vibrator.vibrate(timings, NO_REPEAT)
        }
    }

    private companion object {
        const val NO_REPEAT = -1
    }
}

/** Builds the haptics for the current context, silenced when the user asked for reduced motion. */
@Composable
internal fun rememberZinelyHaptics(reduceMotion: Boolean): ZinelyHaptics {
    val context = LocalContext.current
    return remember(context, reduceMotion) {
        ZinelyHaptics(vibrator = systemVibrator(context), enabled = !reduceMotion)
    }
}

private fun systemVibrator(context: Context): Vibrator? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION") // VibratorManager is API 31+
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }
