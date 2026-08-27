# Release-gap roadmap — 2026-08-27

## Scope and evidence boundary

This is a live-checkout audit of `feat/zine-backup-v2` after the reported physical printer run. It separates
confirmed defects from release evidence still required and from scale-up work. Source documents and tests remain
authoritative; this file does not close a device or Play Console gate by itself.

## P0 — fix before the next release candidate

| Gap | Evidence | Disposition |
|---|---|---|
| Page navigation could discard an in-progress inline text draft. | `EditTextSession` keeps the draft feature-local until focus loss, while `GoToPage` closes the reducer session. The filmstrip and all-pages sheet previously dispatched page navigation first. | Fixed in the current focused batch: both routes clear focus and defer navigation until the text session has committed. Regression coverage exercises both routes. |

## P1 — release gates and trust work

| Gap | Why it matters | Required next action |
|---|---|---|
| Flip has no committed Samsung release report. | The change crosses schema, reducer, renderer, PDF and Compose layers. Automation is strong but does not replace the required device gate. | Run and commit the focused Flip device pass: Photo and Art H/V, undo/redo, reopen, Reframe, Proof/PDF parity, large font and TalkBack. |
| Public Play privacy policy and declarations are not complete. | Google Play requires the Data safety form and a privacy-policy link, including for apps that collect no user data. | Owner supplies the public URL, support contact and final data-use confirmation; then publish the policy and complete Data safety, content rating and target-audience declarations. |
| Disaster-recovery evidence is incomplete. | Backup/restore is the user's recovery promise. Wipe/reinstall restore, another device/API, provider interruption and offline journey remain unproven. | Prioritize a Samsung wipe/reinstall restore from a real `.zine` archive; record the result in the torture matrix. |
| Already-indexed corrupt/newer document files can remain visible in the Room index. | Files are authoritative and Room is rebuildable; a stale row can present an apparently healthy zine that later fails to open. | Design a per-project revalidation/health-state seam. Do not add shelf-side cleanup. |
| Reframe refusal is silent for an unreadable photo. | A failed user action currently has no spoken or visible outcome. | Obtain the founder-owned concise copy, add it to `:core:copy`, then test the announcement. |

## P2 — post-gate quality and scale

| Candidate | Boundary to preserve |
|---|---|
| Art drawer first-cold-open latency | Measure post-install first open on Samsung; optimize only the cold path, not the art interaction design. |
| Orphaned image assets after delete/import churn | Correct the optimistic contract wording now; implement a transactional sweeper only with import pinning and recovery tests. |
| SAF out-of-space classification | Improve error mapping only after a realistic low-storage experiment. |
| Library scale features (search, sort, archive/status) | Add a query boundary to `ProjectRepository`; do not bolt filtering onto the current whole-list stream. |
| More document fonts/presets | Do not expose a font picker until a curated, licensed document font set has four-surface parity, script coverage and physical-print evidence. |

## Deliberate non-actions

- No broad editor, Library or Art redesign is included in the P0 batch.
- The physical printer run is accepted as evidence for the already-tested print path; it does not substitute for
  device verification of newly-added Flip behavior.
- A local policy draft cannot satisfy Play's public-URL requirement, so publishing it waits for owner-controlled
  contact and hosting details rather than inventing legal claims.
