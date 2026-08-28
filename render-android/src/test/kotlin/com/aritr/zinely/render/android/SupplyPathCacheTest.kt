package com.aritr.zinely.render.android

import android.graphics.Path
import com.aritr.zinely.core.render.SupplyCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SupplyPathCacheTest {

    @Test
    fun `given the same catalogue outline across frames, then the Android path is built once`() {
        val cache = SupplyPathCache()
        val outline = requireNotNull(SupplyCatalog.outlineOf("mark.registration"))

        val firstFrame = cache.pathFor(outline)
        val nextFrame = cache.pathFor(outline)

        assertSame(firstFrame, nextFrame)
        assertEquals(Path.FillType.EVEN_ODD, nextFrame.fillType)
    }

    @Test
    fun `given a different outline object, then the cached path is not reused`() {
        val cache = SupplyPathCache()
        val rectangle = requireNotNull(SupplyCatalog.outlineOf("shape.rect"))
        val equalButDistinct = rectangle.copy()

        val canonicalPath = cache.pathFor(rectangle)
        val distinctPath = cache.pathFor(equalButDistinct)

        assertNotSame(canonicalPath, distinctPath)
    }

    @Test
    fun `prewarm builds the canonical paths before the first visible draw`() {
        val cache = SupplyPathCache()
        val first = requireNotNull(SupplyCatalog.outlineOf("shape.rect"))
        val second = requireNotNull(SupplyCatalog.outlineOf("mark.registration"))

        cache.prewarm(listOf(first, second))

        assertSame(cache.pathFor(first), cache.pathFor(first))
        assertSame(cache.pathFor(second), cache.pathFor(second))
    }
}
