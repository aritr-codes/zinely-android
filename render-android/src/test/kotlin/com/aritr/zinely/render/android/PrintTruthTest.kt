package com.aritr.zinely.render.android

import com.aritr.zinely.core.model.AffineTransform2D
import com.aritr.zinely.core.model.ColorRgba
import com.aritr.zinely.core.model.PtRect
import com.aritr.zinely.core.model.PtSize
import com.aritr.zinely.core.render.FillRect
import java.io.ByteArrayOutputStream
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Print truth ([ADR-150](docs/DECISIONS.md#adr-150) decision 5): **no PDF or PNG byte depends on the paper colour.**
 *
 * The proof is structural. `PaperColour` lives in `:core:data` (the shelf layer); this module, which writes every
 * PDF and PNG ([SheetComposer]), depends on `:core:render` and `:core:model` only, so the type is not on its
 * classpath and nothing here can receive it. If someone adds that dependency, the first test fails and the
 * decision must be re-argued. The second only shows the PNG writer is deterministic (one sheet written twice is
 * the same bytes); it does not by itself prove colour-independence, it backs the first test's premise that the
 * document (which holds no colour; see `schema-shape-v5.txt`) is the writer's only input. The PDF path cannot run under Robolectric; it is the same composer and replayer
 * (`PdfExportInstrumentedTest`), and receives no more inputs than the PNG path.
 */
@RunWith(RobolectricTestRunner::class)
class PrintTruthTest {

    @Test
    fun `the export renderer cannot see the paper colour type`() {
        try {
            Class.forName("com.aritr.zinely.core.data.repository.PaperColour")
            fail("PaperColour is on the export renderer's classpath: the PDF/PNG path could receive the paper colour")
        } catch (_: ClassNotFoundException) {
            // expected: the export path cannot depend on it
        }
    }

    @Test
    fun `one sheet written twice is the same PNG bytes`() {
        val composer = SheetComposer(CanvasReplayer())
        val sheet = PtSize(144.0, 72.0)
        val panels = listOf(
            SheetPanel(
                AffineTransform2D.identity(),
                PtRect(0.0, 0.0, 72.0, 72.0),
                listOf(FillRect(rect = PtRect(10.0, 10.0, 40.0, 40.0), color = ColorRgba(200, 40, 90, 255))),
            ),
        )
        fun png() = ByteArrayOutputStream().also { composer.writePng(sheet, panels, emptyList(), it) }.toByteArray()
        assertArrayEquals(png(), png())
    }
}
