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
| ~~Flip's approved P1 verification is incomplete.~~ | **Closed by owner acceptance on 2026-08-28.** The cross-schema/reducer/renderer/PDF/Compose implementation and focused automation are complete; the named-toggle repair has direct platform-node coverage and a committed Samsung accessibility report. The owner separately completed the human TalkBack spoken-order check and declared the approved P1 work complete. | Keep the existing Flip behavior and regression coverage intact. Any broader matrix expansion is post-gate evidence, not a reason to reopen the accepted P1 slice. |
| Public Play privacy policy and declarations are not complete. | Google Play requires the Data safety form and a privacy-policy link, including for apps that collect no user data. | Owner supplies the public URL, support contact and final data-use confirmation; then publish the policy and complete Data safety, content rating and target-audience declarations. |
| ~~The approved clean-reinstall recovery pass is incomplete.~~ | **Closed for the approved P1 scope on 2026-08-28.** A production `.zine` archive survived uninstall/reinstall and restored an openable zine with committed text on Samsung; the bounded report and torture matrix retain the exact limits. | Keep another-device/API, provider interruption, offline, media/cover and print-parity expansion in the torture matrix as post-gate evidence rather than overstating this pass. |
| ~~Already-indexed corrupt/newer document files can remain falsely healthy in the Room index.~~ | **Closed 2026-08-28.** D-111 adds the shelf-only files-as-truth health projection: corrupt/newer projects remain visible and route to their existing action sheet, where Open, Share and Duplicate are disabled while Rename and Delete remain reachable. | Preserve the existing repository/UI focused coverage. The deliberately invalid physical-file scenario remains optional destructive QA, not an implementation blocker. |
| ~~Reframe refusal is silent for an unreadable photo.~~ | **Closed 2026-08-28.** The owner authorised autonomous implementation of the focused slice; A25 records the implementation-selected concise recovery line in the existing Bench snack. The same line is visible and announced once through the snack's polite live region, Reframe stays closed, the document is untouched, and the ordinary Add control is the truthful recovery path. | Implemented through `:core:copy` and the existing surface-owned readability gate; focused UI coverage pins refusal, no mutation, visible copy, announcement semantics, no false Undo, and the enabled Add exit. |

### 2026-08-28 checkpoint reconciliation

- The owner completed the human TalkBack spoken-order verification.
- The intentional action-sheet change was recorded by the pinned CI workflow, visually inspected, and
  adopted only as `v21_sheet_light.png` / `v21_sheet_dark.png` in `f43683c`.
- Reframe failure feedback is closed in `9d52335`; no document schema change was required.

## P2 — post-gate quality and scale

| Candidate | Boundary to preserve |
|---|---|
| Art drawer first-cold-open latency | The repo-side prewarm remains safe, but the 2026-08-28 Samsung release-parity rerun did **not** clear the gate: two post-install samples rendered 31 frames with 7 / 6 janky (22.58% / 19.35%) and 42 / 29 ms medians. Responsiveness stays **Yellow**; investigate the remaining UI-thread/draw cost before claiming the cold spike is fixed. |
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
