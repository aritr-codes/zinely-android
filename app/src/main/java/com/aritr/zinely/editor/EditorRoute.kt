package com.aritr.zinely.editor

import kotlinx.serialization.Serializable

/**
 * The type-safe route for the Home · "My zines" shelf — the app's start destination and single
 * back-stack root (S6.5, [ADR-046](../../../../../../docs/DECISIONS.md#adr-046) §1). Every editor
 * chain is pushed above it and popping always lands back here; no code path *navigates* to Home, so
 * two [EditorRoute] entries can never coexist.
 */
@Serializable
internal data object HomeRoute

/**
 * The type-safe navigation route for the editor (ADR-030 §1). [projectId] is the project a shelf
 * card tap or a fresh create handed over (ADR-046 §1/§5 — the ADR-030 §4 fixed-`"default"` seed is
 * retired); it is a real route argument — threaded into [EditorViewModel] via
 * `SavedStateHandle.toRoute` — so every open is the same code path.
 */
@Serializable
internal data class EditorRoute(val projectId: String)

/**
 * The type-safe route for the single **Proof** surface (M5, [ADR-051](../../../../../../docs/DECISIONS.md#adr-051)).
 * One route, three internal acts (Sheet → Print → Fold) — the collapse of the former Preview + Export +
 * Completion triad into one screen (ADR-051 Decision A). Carries the same [projectId] so the Proof host
 * re-fetches the *editor's* back-stack entry and shares its already-constructed `EditorViewModel` (the
 * ADR-026 single-writer seam), exactly as the retired triad did.
 */
@Serializable
internal data class ProofRoute(val projectId: String)
