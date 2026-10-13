package com.aritr.zinely.feature.editor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** [foldStepFit]: how a fold step shares its room between the diagram and the words. */
class FoldStepFitTest {

    private fun fit(viewport: Int?, sentence: Int = 60) =
        foldStepFit(viewport = viewport, fixed = 100, sentence = sentence, tallestSentence = 90, leastDiagram = 120)

    @Test
    fun `with room to spare the diagram takes what the words leave, and the tallest sentence is reserved`() {
        val fit = fit(viewport = 500)

        assertTrue(fit.steady)
        assertEquals(190, fit.fixedAndSentence)
        assertEquals(310, fit.diagramMax)
    }

    @Test
    fun `a step that exactly fits with the least diagram is still steady`() {
        val fit = fit(viewport = 310)

        assertTrue(fit.steady)
        assertEquals(120, fit.diagramMax)
    }

    @Test
    fun `one pixel short, the reserve is dropped and the diagram keeps its least height`() {
        val fit = fit(viewport = 309)

        assertFalse(fit.steady)
        assertEquals("only this step's own sentence is counted", 160, fit.fixedAndSentence)
        // 309 - 160 = 149 is left for the diagram once the reserve is gone.
        assertEquals(149, fit.diagramMax)
    }

    @Test
    fun `with no room at all the diagram is never given less than its least height`() {
        assertEquals(120, fit(viewport = 0).diagramMax)
        assertEquals(120, fit(viewport = 200).diagramMax)
        assertFalse(fit(viewport = 0).steady)
    }

    @Test
    fun `whether a step is steady does not depend on which step it is`() {
        // The controls under the step would jump if two steps disagreed.
        for (viewport in listOf(0, 250, 309, 310, 311, 600)) {
            assertEquals(fit(viewport, sentence = 30).steady, fit(viewport, sentence = 90).steady)
        }
    }

    @Test
    fun `unbounded room puts no cap on the diagram`() {
        val fit = fit(viewport = null)

        assertTrue(fit.steady)
        assertNull(fit.diagramMax)
        assertEquals(190, fit.fixedAndSentence)
    }
}
