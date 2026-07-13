package com.aritr.zinely.data.android.prefs

import kotlinx.coroutines.flow.Flow

/**
 * The local "seen this one-time teaching hint" store for the editor (ADR-032). A tiny key-value seam,
 * deliberately separate from the `.zine` document store: a "seen hint X" flag is app/install state, not
 * document content (ARCHITECTURE §15.6 item 4). Local-only — no network, no account (privacy invariant).
 *
 * This is the first slice of the broader Settings/onboarding preferences store; it intentionally exposes
 * only the one flag the move/resize hint needs. Reduced-motion/haptic/theme/paper-size choices land here
 * later behind the same DataStore (folded into a Settings ADR), not a new mechanism.
 *
 * Reads are a cold [Flow] so a consumer can gate UI on the *loaded* value and never flash a hint before
 * the persisted state arrives; the write is idempotent (set-to-true), so calling it repeatedly is safe.
 */
public interface EditorOnboardingStore {
    /**
     * Whether the one-time move/resize hint has already been shown-and-dismissed on this install.
     * Emits `false` until the flag is first marked (and on a corrupt/missing store — degrade by showing
     * the hint, never by crashing). Re-emits on every change.
     */
    public val moveResizeHintSeen: Flow<Boolean>

    /** Persist that the move/resize hint has been seen. Idempotent (set-to-true); safe to call again. */
    public suspend fun markMoveResizeHintSeen()

    /**
     * Whether the first-run Reframe coach-mark (the pulsing "Reframe" affordance, ADR-053 RF2) has already
     * taught on this install. Emits `false` until first marked (and on a corrupt/missing store — degrade by
     * teaching, never by crashing). Re-emits on every change. The teach fires once so a returning user isn't
     * re-pulsed at every photo selection.
     */
    public val reframeCoachSeen: Flow<Boolean>

    /** Persist that the Reframe coach-mark has taught. Idempotent (set-to-true); safe to call again. */
    public suspend fun markReframeCoachSeen()
}
