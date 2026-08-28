package com.aritr.zinely.render.android

import android.graphics.Path
import com.aritr.zinely.core.render.SupplyOutline
import java.util.IdentityHashMap

/**
 * Reuses the immutable Android path for a catalogue outline across draw frames.
 *
 * Supply outlines are canonical catalogue objects. Identity keys avoid repeatedly walking every segment
 * for structural hashing, while still rebuilding safely if a caller supplies a different outline object.
 * The owning painter/replayer is already single-surface and mutable, so this cache shares that lifetime
 * and thread-safety contract rather than introducing global retained state.
 */
internal class SupplyPathCache {
    private val paths = IdentityHashMap<SupplyOutline, Path>()

    @Synchronized
    fun pathFor(outline: SupplyOutline): Path =
        paths[outline] ?: outline.toPath().also { paths[outline] = it }

    @Synchronized
    fun prewarm(outlines: Iterable<SupplyOutline>) {
        outlines.forEach(::pathFor)
    }
}
