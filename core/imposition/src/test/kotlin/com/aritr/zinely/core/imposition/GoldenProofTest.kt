package com.aritr.zinely.core.imposition

import com.aritr.zinely.core.model.PaperSize
import com.aritr.zinely.core.model.ZineFormat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Test
import java.io.File

/**
 * Golden-file tests for the SVG proof sheet. The committed golden under src/test/resources/golden
 * is the visual contract; any renderer change must update it deliberately.
 *
 * A missing golden is a hard failure (bad checkout / packaging / wrong working dir), NOT a silent
 * pass. Regenerate intentionally by running the tests with `-DupdateGolden=true`.
 */
class GoldenProofTest {
    private val updateGolden: Boolean = System.getProperty("updateGolden") == "true"

    private fun goldenCheck(name: String, paper: PaperSize) {
        val actual = SvgProofSheetRenderer()
            .render(SingleSheet8Imposer().layout(ZineFormat.SINGLE_SHEET_8, paper))
            .svg
            .replace("\r\n", "\n")

        val file = File("src/test/resources/golden/$name.svg")
        if (updateGolden) {
            file.parentFile.mkdirs()
            file.writeText(actual)
        }
        if (!file.exists()) {
            fail<Unit>("Golden $name.svg is missing. Regenerate with -DupdateGolden=true, then review and commit it.")
        }
        val expected = file.readText().replace("\r\n", "\n")
        assertEquals(expected, actual, "Proof SVG drifted from golden $name.svg; review and update with -DupdateGolden=true if intended.")
    }

    @Test
    fun `letter proof matches its golden`() = goldenCheck("single-sheet-8-letter", PaperSize.LETTER)

    @Test
    fun `a4 proof matches its golden`() = goldenCheck("single-sheet-8-a4", PaperSize.A4)
}
