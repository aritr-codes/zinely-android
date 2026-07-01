package com.aritr.zinely.editor

import kotlinx.serialization.Serializable

/**
 * The type-safe navigation route for the editor (ADR-030 §1). [projectId] is the document this host
 * opens; the MVP seeds a single fixed `"default"` project (no ProjectRepository yet, ADR-030 §4), but
 * the id is a real route argument — threaded into [EditorViewModel] via `SavedStateHandle.toRoute` —
 * so multi-project navigation is an additive change, not a rewrite.
 */
@Serializable
internal data class EditorRoute(val projectId: String)

/**
 * The type-safe route for the reader's-booklet [PreviewScreen] (S5 step 1). Carries the same [projectId]
 * as the [EditorRoute] it was opened from, so the preview host can re-fetch the *editor's* back-stack
 * entry and share its already-constructed `EditorViewModel` — never a second one. That sharing is not a
 * nicety: the VM eagerly registers its `projectId` with the single-writer autosave factory (ADR-026), so
 * a second VM for the same project would be rejected. The preview therefore renders the live editor store.
 */
@Serializable
internal data class PreviewRoute(val projectId: String)

/**
 * The type-safe route for the [ExportScreen][com.aritr.zinely.feature.editor.ExportScreen] · "Print & fold"
 * (S5 step 2, [ADR-039](../../../../../../docs/DECISIONS.md#adr-039)). Carries the same [projectId] so the
 * export host re-fetches the *editor's* back-stack entry and reads its live `EditorViewModel` document —
 * the same shared-VM seam [PreviewRoute] uses, and for the same reason (the single-writer autosave factory,
 * ADR-026, forbids a second VM for the project). The export therefore renders exactly what Preview showed.
 */
@Serializable
internal data class ExportRoute(val projectId: String)

/**
 * The type-safe route for the [CompletionScreen][com.aritr.zinely.feature.editor.CompletionScreen] ·
 * fold steps (S5 step 3, [ADR-040](../../../../../../docs/DECISIONS.md#adr-040)) — the payoff peak. Carries
 * the same [projectId] so the completion host re-fetches the *editor's* back-stack entry and reuses the
 * shipped export seam (same shared-VM reason as [ExportRoute] / [PreviewRoute]: the single-writer autosave
 * factory, ADR-026, forbids a second VM). Reached today from Export's "How do I fold it?"; the auto
 * post-export landing is deferred (ADR-040).
 */
@Serializable
internal data class CompletionRoute(val projectId: String)
