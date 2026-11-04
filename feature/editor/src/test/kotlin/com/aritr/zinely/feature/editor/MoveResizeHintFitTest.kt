package com.aritr.zinely.feature.editor

import com.aritr.zinely.core.model.Transform
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Bench A54.1 / A54.2, the pure half: when the first-run hint fits above the selected piece. */
class MoveResizeHintFitTest {

    @Test
    fun the_hint_fits_when_it_ends_the_keep_above_the_piece() {
        // 8 down + 100 tall + 12 keep = 120: a piece whose top is at 120 leaves exactly room.
        assertTrue(moveResizeHintFits(hintHeightPx = 100, pieceTopPx = 120f, topGapPx = 8f, keepPx = 12f))
    }

    @Test
    fun one_pixel_short_and_the_hint_waits() {
        assertFalse(moveResizeHintFits(hintHeightPx = 101, pieceTopPx = 120f, topGapPx = 8f, keepPx = 12f))
    }

    @Test
    fun a_piece_lifted_by_a_tray_leaves_less_room() {
        // The host adds the page lift (negative) to the piece's top; the same hint then waits.
        val lifted = 120f + -40f
        assertFalse(moveResizeHintFits(hintHeightPx = 100, pieceTopPx = lifted, topGapPx = 8f, keepPx = 12f))
    }

    /** A54.4 on a 360dp phone: 100% and 130% keep the buttons beside the line; 150% and 200% stack them. */
    @Test
    fun the_save_notice_stacks_from_150_percent_on_a_360dp_phone() {
        val text = 12.64f // NoticeTextSize at 100%
        assertFalse(saveNoticeStacks(360f, text * 1.0f))
        assertFalse(saveNoticeStacks(360f, text * 1.3f))
        assertTrue(saveNoticeStacks(360f, text * 1.5f))
        assertTrue(saveNoticeStacks(360f, text * 2.0f))
        // The goldens' 430dp host at 100% never stacks.
        assertFalse(saveNoticeStacks(430f, text))
    }

    @Test
    fun an_upright_piece_starts_at_its_own_top() {
        assertEquals(50.0, turnedTopPt(Transform(10.0, 50.0, 100.0, 20.0)), 1e-9)
    }

    @Test
    fun a_turned_piece_starts_at_its_turned_box() {
        // A 100 x 20 box turned 90° stands 100 tall about the same centre (y 60): its top is 10.
        assertEquals(10.0, turnedTopPt(Transform(10.0, 50.0, 100.0, 20.0, rotationDegrees = 90.0)), 1e-9)
        // Turned 45°, either way: half-height (100 + 20) / 2 · sin 45° = 42.43 above the centre.
        val half = 120.0 / 2.0 * kotlin.math.sqrt(0.5)
        assertEquals(60.0 - half, turnedTopPt(Transform(10.0, 50.0, 100.0, 20.0, rotationDegrees = -45.0)), 1e-9)
    }
}
