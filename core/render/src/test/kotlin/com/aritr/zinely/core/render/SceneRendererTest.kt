package com.aritr.zinely.core.render

import com.aritr.zinely.core.model.Background
import com.aritr.zinely.core.model.ColorRgba
import com.aritr.zinely.core.model.Crop
import com.aritr.zinely.core.model.DocumentDefaults
import com.aritr.zinely.core.model.Fit
import com.aritr.zinely.core.model.ImageElement
import com.aritr.zinely.core.model.Page
import com.aritr.zinely.core.model.PageRole
import com.aritr.zinely.core.model.PtPoint
import com.aritr.zinely.core.model.PtSize
import com.aritr.zinely.core.model.TextElement
import com.aritr.zinely.core.model.TextStyle
import com.aritr.zinely.core.model.Transform
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SceneRendererTest {

    private val pageSize = PtSize(width = 200.0, height = 300.0)
    private val noDefaults = DocumentDefaults()

    private fun page(
        background: Background = Background.None,
        elements: List<com.aritr.zinely.core.model.Element> = emptyList(),
    ) = Page(index = 0, role = PageRole.INTERIOR, background = background, elements = elements)

    private fun assertPoint(expX: Double, expY: Double, actual: PtPoint, eps: Double = 1e-9) {
        assertTrue(kotlin.math.abs(expX - actual.x) <= eps, "x: expected $expX got ${actual.x}")
        assertTrue(kotlin.math.abs(expY - actual.y) <= eps, "y: expected $expY got ${actual.y}")
    }

    @Test
    fun `empty page yields no commands`() {
        val cmds = SceneRenderer.render(page(), pageSize, noDefaults)
        assertTrue(cmds.isEmpty())
    }

    @Test
    fun `solid page background yields a full-page FillRect first`() {
        val red = ColorRgba(255, 0, 0)
        val cmds = SceneRenderer.render(page(background = Background.Solid(red)), pageSize, noDefaults)

        assertEquals(1, cmds.size)
        val fill = cmds[0] as FillRect
        assertEquals(red, fill.color)
        assertEquals(0.0, fill.rect.x); assertEquals(0.0, fill.rect.y)
        assertEquals(200.0, fill.rect.width); assertEquals(300.0, fill.rect.height)
        assertNull(fill.localClip)
        // identity transform: (0,0) maps to (0,0)
        assertPoint(0.0, 0.0, fill.localToPage.map(PtPoint(0.0, 0.0)))
    }

    @Test
    fun `page background None falls back to document default background`() {
        val blue = ColorRgba(0, 0, 255)
        val defaults = DocumentDefaults(background = Background.Solid(blue))
        val cmds = SceneRenderer.render(page(background = Background.None), pageSize, defaults)
        assertEquals(blue, (cmds.single() as FillRect).color)
    }

    @Test
    fun `no background when both page and default are None`() {
        val cmds = SceneRenderer.render(page(background = Background.None), pageSize, noDefaults)
        assertTrue(cmds.none { it is FillRect })
    }

    @Test
    fun `elements emit back-to-front by zIndex`() {
        val top = TextElement(id = "top", transform = box(), zIndex = 5, text = "T")
        val bottom = TextElement(id = "bottom", transform = box(), zIndex = 1, text = "B")
        // author order: top first, bottom second — but zIndex must drive paint order
        val cmds = SceneRenderer.render(page(elements = listOf(top, bottom)), pageSize, noDefaults)
        val texts = cmds.filterIsInstance<DrawTextBox>()
        assertEquals(listOf("B", "T"), texts.map { it.text })
    }

    @Test
    fun `equal zIndex keeps author order`() {
        val a = TextElement(id = "a", transform = box(), zIndex = 0, text = "A")
        val b = TextElement(id = "b", transform = box(), zIndex = 0, text = "B")
        val cmds = SceneRenderer.render(page(elements = listOf(a, b)), pageSize, noDefaults)
        assertEquals(listOf("A", "B"), cmds.filterIsInstance<DrawTextBox>().map { it.text })
    }

    @Test
    fun `text element becomes a DrawTextBox with verbatim style and box-clipped`() {
        val style = TextStyle(sizePt = 18.0, bold = true)
        val el = TextElement(id = "t", transform = Transform(10.0, 20.0, 100.0, 50.0), zIndex = 0, text = "hi", style = style)
        val cmd = SceneRenderer.render(page(elements = listOf(el)), pageSize, noDefaults).single() as DrawTextBox

        assertEquals("hi", cmd.text)
        assertEquals(style, cmd.style) // verbatim — no doc-default fold
        assertEquals(100.0, cmd.boxWidthPt); assertEquals(50.0, cmd.boxHeightPt)
        // clip is the element box in local space
        assertEquals(0.0, cmd.localClip!!.x); assertEquals(0.0, cmd.localClip!!.y)
        assertEquals(100.0, cmd.localClip!!.width); assertEquals(50.0, cmd.localClip!!.height)
        // localToPage = translate(10,20) for unrotated
        assertPoint(10.0, 20.0, cmd.localToPage.map(PtPoint(0.0, 0.0)))
        assertPoint(110.0, 70.0, cmd.localToPage.map(PtPoint(100.0, 50.0)))
    }

    @Test
    fun `image element becomes DrawImage intent carrying crop and fit, box-clipped`() {
        val el = ImageElement(
            id = "img", transform = Transform(0.0, 0.0, 80.0, 40.0), zIndex = 0,
            assetId = "sha256:abc", crop = Crop(0.1, 0.0, 0.9, 1.0), fit = Fit.FILL,
        )
        val cmd = SceneRenderer.render(page(elements = listOf(el)), pageSize, noDefaults).single() as DrawImage

        assertEquals("sha256:abc", cmd.assetId)
        assertEquals(Crop(0.1, 0.0, 0.9, 1.0), cmd.crop)
        assertEquals(Fit.FILL, cmd.fit)
        assertEquals(80.0, cmd.box.width); assertEquals(40.0, cmd.box.height)
        assertEquals(cmd.box, cmd.localClip) // FILL/cover overflow clipped to the box
    }

    @Test
    fun `rotation maps the element-local center to the page-space box center`() {
        val el = TextElement(id = "r", transform = Transform(10.0, 20.0, 100.0, 50.0, rotationDegrees = 90.0), zIndex = 0, text = "x")
        val cmd = SceneRenderer.render(page(elements = listOf(el)), pageSize, noDefaults).single() as DrawTextBox
        // local center (50,25) must land at page center (10+50, 20+25) regardless of rotation
        assertPoint(60.0, 45.0, cmd.localToPage.map(PtPoint(50.0, 25.0)), eps = 1e-9)
    }

    @Test
    fun `90 degree rotation maps corners with the right direction and order`() {
        val el = TextElement(id = "r", transform = Transform(10.0, 20.0, 100.0, 50.0, rotationDegrees = 90.0), zIndex = 0, text = "x")
        val cmd = SceneRenderer.render(page(elements = listOf(el)), pageSize, noDefaults).single() as DrawTextBox
        // local (0,0) and (100,0) corners, hand-derived through T(10,20)·T(c)·R(90)·T(-c)
        assertPoint(85.0, -5.0, cmd.localToPage.map(PtPoint(0.0, 0.0)), eps = 1e-6)
        assertPoint(85.0, 95.0, cmd.localToPage.map(PtPoint(100.0, 0.0)), eps = 1e-6)
    }

    @Test
    fun `background is painted before elements`() {
        val el = TextElement(id = "t", transform = box(), zIndex = 0, text = "x")
        val cmds = SceneRenderer.render(
            page(background = Background.Solid(ColorRgba.WHITE), elements = listOf(el)),
            pageSize, noDefaults,
        )
        assertTrue(cmds.first() is FillRect)
        assertTrue(cmds.last() is DrawTextBox)
    }

    private fun box() = Transform(0.0, 0.0, 10.0, 10.0)
}
