package com.aritr.zinely.data.android

import java.util.concurrent.atomic.AtomicInteger

/**
 * One operation's race between the maker's Cancel and the work being done (Brief 01 "the late Cancel",
 * ADR-120). Whichever claims the latch first decides the outcome, once:
 *
 * - [markDone] wins → the work is complete. A later Cancel is a no-op and the real result is reported.
 * - [requestCancel] wins → the work is abandoned. The operation must not report success, even if its
 *   last byte has already been written; it cleans up instead.
 */
public class OutcomeLatch {
    private val state = AtomicInteger(OPEN)

    /** Claims the outcome for the finished work; `false` when a Cancel got there first. */
    public fun markDone(): Boolean = state.compareAndSet(OPEN, DONE)

    /** Claims the outcome for Cancel; `false` when the work was already done. */
    public fun requestCancel(): Boolean = state.compareAndSet(OPEN, CANCELLED)

    /** `true` once a Cancel has won: nothing the work does afterwards may be reported as its failure. */
    public val isCancelled: Boolean get() = state.get() == CANCELLED

    private companion object {
        const val OPEN = 0
        const val DONE = 1
        const val CANCELLED = 2
    }
}
