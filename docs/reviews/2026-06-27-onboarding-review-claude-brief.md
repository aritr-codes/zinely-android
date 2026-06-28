# Reviewer -> Implementer Handoff

Date: 2026-06-27
Reviewer: Codex
Implementer: Claude Code
Scope: onboarding review of the current `main` checkout for architecture truthfulness, invariant compliance, and doc/code drift

## Findings

### Required Fix 1

Top-level docs misstate the current repository shape and will mis-onboard implementers.

Evidence:
- `README.md:38-39` says the repo is still "pure-Kotlin core, no app UI yet" and that S2B/S3 are next.
- `README.md:63-66` says there is no app module yet and only `core:model`, `core:imposition`, and `core:data` build.
- `docs/ARCHITECTURE.md:5` says no Android-backed modules or app UI exist yet.
- `docs/ROADMAP.md:39` says S2B Android-backed data sources are the next build step.
- `settings.gradle.kts:28-64` includes `:app`, `:data-android`, `:render-android`, `:feature:editor`, `:core:render`, and `:core:editor`.
- `app/src/main/java/com/aritr/zinely/editor/EditorViewModel.kt` and `app/src/main/java/com/aritr/zinely/editor/ZinelyNavHost.kt` show the editor is mounted in the real app.

Required outcome:
- Update `README.md`, `docs/ARCHITECTURE.md`, and `docs/ROADMAP.md` so they describe the current checkout truthfully.
- Do not leave contradictory "implemented so far" or "next build step" language in those files.

### Required Fix 2

`docs/ARCHITECTURE.md` contains an internal contradiction about `:feature:editor` and the realized/planned split.

Evidence:
- `docs/ARCHITECTURE.md:91` says `:feature:editor` is "preview host only" and that MVI store, gestures, and undo are still planned.
- `docs/ARCHITECTURE.md:390-392` says `:core:render` and `:feature:editor` interaction surface are already on `main`.
- Code matches the later claim, not the earlier one:
  - `core/editor/src/main/kotlin/com/aritr/zinely/core/editor/EditorReducer.kt`
  - `feature/editor/src/main/kotlin/com/aritr/zinely/feature/editor/EditorStore.kt`
  - `feature/editor/src/main/kotlin/com/aritr/zinely/feature/editor/EditorGestures.kt`
  - `feature/editor/src/main/kotlin/com/aritr/zinely/feature/editor/EditorScreen.kt`

Required outcome:
- Rewrite the module-status section to distinguish what is actually shipped versus what remains deferred.
- Keep the claim scope honest: the editor surface exists, but other product features are still absent.

### Required Fix 3

The documented persistence architecture still reads as if Room metadata and `ProjectRepository` are present in production, but the app is currently file-only and single-project.

Evidence:
- `docs/ARCHITECTURE.md:25-30`, `docs/ARCHITECTURE.md:114`, and ADR-003 language imply Room metadata plus JSON document storage.
- `core/data/src/main/kotlin/com/aritr/zinely/core/data/repository/ProjectRepository.kt` defines the metadata contract.
- No production Room/DAO/Database implementation is present in the repo.
- `data-android/src/main/kotlin/com/aritr/zinely/data/android/di/DataModule.kt:43-64` binds only `DocumentRepository`.
- `data-android/src/main/kotlin/com/aritr/zinely/data/android/DocumentRepositoryImpl.kt` persists `projects/<id>/document.json`.
- `app/src/main/java/com/aritr/zinely/editor/ZinelyNavHost.kt:37-45` hardcodes `EditorRoute(projectId = "default")`.
- `app/src/main/java/com/aritr/zinely/editor/EditorBootstrap.kt:20-27` seeds a single blank document for first run.
- `docs/DECISIONS.md:470-485` already acknowledges that there is no production `ProjectRepository` yet.

Required outcome:
- Update architecture and roadmap docs so they state explicitly that current production persistence is:
  - file-backed `DocumentRepository`
  - single fixed `"default"` project
  - `ProjectRepository` deferred
  - Room metadata not yet implemented

### Required Fix 4

Docs overstate export availability. Shared render/export backends exist, but there is no user-facing export flow yet.

Evidence:
- `docs/ROADMAP.md:50-55` presents PDF/image export plus share as MVP expectations.
- Render/export primitives exist:
  - `render-android/src/main/kotlin/com/aritr/zinely/render/android/RasterPageRenderer.kt`
  - `render-android/src/main/kotlin/com/aritr/zinely/render/android/PdfPageRenderer.kt`
  - `render-android/src/main/kotlin/com/aritr/zinely/render/android/CanvasReplayer.kt`
- No production `:feature:export`, `FileProvider`, `MediaStore`, `ACTION_CREATE_DOCUMENT`, `ACTION_OPEN_DOCUMENT`, or `PrintManager` wiring exists in current code.

Required outcome:
- Document the current state accurately:
  - shared render/export primitives are implemented
  - export UI/workflow/share wiring is still pending

### Recommended Improvement 1

The `AssetStore` contract and durability comments lag the accepted ADR-022 amendment and the shipped implementation.

Evidence:
- `core/data/src/main/kotlin/com/aritr/zinely/core/data/asset/AssetStore.kt:10-21` still describes the older in-flight-root and mtime race-closure model as if it were the active contract.
- `core/data-storage/build.gradle.kts:6-12` reads as if pin-file/generation-counter GC exists in the shipped core.
- `core/data-storage/src/main/kotlin/com/aritr/zinely/core/data/storage/FileAssetStore.kt:25-30` explicitly says GC is deferred and blocked until pin-safe import exists.

Suggested outcome:
- Align comments and KDoc with the real state:
  - content-addressed store exists
  - GC/pins/generation safety are not yet shipped
  - no sweeper may ship before the pin-safe import/GC work lands

### Recommended Improvement 2

The invariant "errors cross boundaries as a sealed Result type; no swallowed exceptions" needs either clarification or selective tightening.

Evidence:
- Repository/autosave boundaries are disciplined:
  - `core/data/.../DataResult.kt`
  - `core/data/.../DataError.kt`
  - `data-android/.../DocumentRepositoryImpl.kt`
  - `core/data-storage/.../AutosaveCoordinator.kt`
- Image/render seams intentionally degrade:
  - `app/.../ImportMasterDecoder.kt` returns `null` on decode/OOM/runtime failures
  - `app/.../FileAssetBytesSource.kt` returns `null` on unreadable blobs
  - `render-android/.../ImageBlitter.kt` uses placeholder fallback on decode failure

Suggested outcome:
- Decide whether the invariant should explicitly permit graceful UI/render degradation seams, or whether some of these paths should surface richer typed failure.

### Observation 1

The seed document uses `PageRole.INTERIOR` for every page.

Evidence:
- `app/src/main/java/com/aritr/zinely/editor/EditorBootstrap.kt:20-27`

Note:
- This may be intentional for the current validator and editor scope. It is not clearly wrong, but it is worth a product-model sanity check against cover semantics in the PRD.

## Review Decision

Decision: GO WITH FIXES

Rationale:
- The implementation architecture is more advanced than the top-level docs claim.
- The most urgent problems are documentation truthfulness and a few stale contract comments, not core architectural breakage.
- The privacy invariant and the `core:*` Android-free invariant appear to hold in the current checkout.

## Next Action

Patch the canonical docs first so they describe the current repository truthfully, then clean the stale storage-contract comments if that can be done without broadening scope.

## Implementation Brief

Current state:
- The real module graph includes `:app`, `:data-android`, `:render-android`, `:feature:editor`, `:core:render`, and `:core:editor`.
- The app mounts a working editor flow with MVI store, gestures, autosave binder, image import pipeline, and shared preview rendering.
- Production persistence is currently file-only and single-project; Room metadata and `ProjectRepository` are still deferred.
- Shared render/export backends exist, but no end-user export/share feature is wired yet.

Decision:
- GO WITH FIXES

Required fixes:
- Fix stale status text in `README.md`.
- Fix contradictory and stale status/module sections in `docs/ARCHITECTURE.md`.
- Fix stale phase/status text in `docs/ROADMAP.md`.
- Make persistence-state wording honest: single fixed `"default"` project, file-backed `DocumentRepository`, no production Room metadata or `ProjectRepository` yet.
- Make export-status wording honest: backends exist, user-facing export flow does not.

Known risks:
- Do not reintroduce blanket claims that the full product is already at the documented MVP state.
- Do not accidentally describe deferred Room/export/home flows as implemented.
- Keep wording tied to the actual checkout, not to intended architecture only.

Relevant ADRs:
- ADR-003
- ADR-005
- ADR-021
- ADR-022
- ADR-025
- ADR-026
- ADR-027
- ADR-028
- ADR-029
- ADR-030
- ADR-031

Affected modules:
- `:app`
- `:data-android`
- `:render-android`
- `:feature:editor`
- `:core:data`
- `:core:data-storage`
- `:core:render`
- `:core:editor`

Desired outcome:
- Canonical docs match the current codebase closely enough that a new implementer can trust them.
- Any remaining deferred work is clearly labeled as deferred.
- Storage/render contract comments stop overstating unshipped GC guarantees.

Ready-to-paste implementer prompt:

```text
Patch the canonical docs and any narrowly-related stale KDoc/comments so they match the current Zinely checkout exactly.

Ground truth to validate against the actual repo:
- settings.gradle.kts includes :app, :data-android, :render-android, :feature:editor, :core:render, :core:editor
- the app mounts the editor via app/src/main/java/com/aritr/zinely/editor/ZinelyNavHost.kt and EditorViewModel.kt
- production persistence is currently file-backed DocumentRepository only, with a fixed "default" project; no production Room metadata or ProjectRepository implementation exists yet
- render/export backends exist in render-android, but no user-facing export/share flow is wired yet

Patch targets:
1. README.md
2. docs/ARCHITECTURE.md
3. docs/ROADMAP.md
4. If scope stays tight, stale storage-contract comments in:
   - core/data/src/main/kotlin/com/aritr/zinely/core/data/asset/AssetStore.kt
   - core/data-storage/build.gradle.kts

Constraints:
- Keep wording checkout-accurate and scope-honest.
- Do not claim unimplemented Room/export/home/settings features are present.
- Preserve accepted architecture intent, but distinguish current implementation from deferred work.
- Do not broaden into product redesign or unrelated refactors.

After patching, provide:
- the exact files changed
- the exact doc claims corrected
- any deliberately deferred wording you left unchanged and why
```
