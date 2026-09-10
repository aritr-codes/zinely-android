# Release-gap roadmap — 2026-08-27

<a id="2026-09-09-current-state-review"></a>
## 2026-09-09 current-state review

### Website follow-up — supersedes the initial audit's pending decisions

At the start of this follow-up, live Pages matched policy-only commit `a8b9ff6` (run `34382725165`).
The concrete Roadmap and website Changelog entry below were still local, not deployed. Owner then
authorized the website fold refinement, feedback link, Aastra attribution, and verified merge/deployment.

- Google Forms was selected and published by the owner: `https://forms.gle/7ejUVJBdUaYoDytu6`.
  Read-only unauthenticated fetch returns the three-question responder page. No test response was sent;
  private form settings and inbox delivery are not independently audited. Policy is canonical in
  [PRIVACY-POLICY.md](../PRIVACY-POLICY.md#feedback-and-email), including 90-day team-managed retention.
- Aastra is the owner-confirmed two-person team. Website/README/About/privacy wording is aligned;
  no app copy or APK was changed in this slice.
- The folding desk is an HTML/SVG/JavaScript prototype: intentional step selection and six controlled
  demonstrations, pause/resume/reset, static checkpoints, reduced motion, all-steps/print/no-JS fallback.
  Three instruction lines at standard desktop width wrap safely at narrow widths/enlarged text. The
  maximum card size is reserved to keep playback aligned. No novice physical-paper success claim is made.
  Research and rejected alternatives: [R18](../RESEARCH.md#r18-deliberate-folding-instructions--9-september).
- Issue #53 was closed with Replace Photo evidence; #54 narrowed to evaluating long-press usefulness;
  #55 narrowed to full Reframe photo-overlay goldens. #56 and #57 remain open pending merge of their
  measured implementation branch. These actions supersede the initial read-only audit's proposed dispositions
  below. No branches or worktrees were removed.
- PR #63 merged on 9 September at `7d0960b`; main-head CI run `34388262137` and Pages run `34388262130`
  both passed. Its pre-merge draft had been stale against Replace Photo and r3 hardware/release acceptance.
  The backup torture matrix's foundation NO-GO is explicitly historical; unresolved stress/device coverage remains.
- Local website checks use `tools/check-website.cjs` with isolated, externally installed Playwright and
  axe tooling (no app/runtime dependency). Coverage: ten steps, normal desktop three-line text, stable
  controls at 390/320 px and 200% text, keyboard play/pause/resume/reset, no autoplay, cut endpoint,
  live reduced-motion change, no-JS and print fallback, axe WCAG A/AA checks and JavaScript errors.
  Screenshots are inspected separately; automated checks are not a WCAG certification.

### 2026-09-10 Reframe loading follow-up

Issues #56 and #57 were investigated together on Samsung SM-A176B / Android 16 with synthetic, non-private
assets. Each comparison has five entry samples after resetting frame statistics immediately before Reframe;
“cold” means a fresh app process, while “warm” reuses the process. These are transition-local `gfxinfo`
measurements, not whole-app launch benchmarks or a universal device claim.

| Asset and condition | Existing r3 APK p95/p99 | Task branch p95/p99 | Result |
|---|---:|---:|---|
| 800×600 JPEG, warm | 34–61 ms | 38–69 ms | No meaningful change |
| 800×600 JPEG, cold process | 36–73 ms | 38–69 ms | No meaningful change |
| 4096×4096 JPEG, warm | 150 ms | 34–53 ms | Large-image tail materially lower |
| 4096×4096 JPEG, cold process | 150–200 ms | 73–77 ms | Large-image tail materially lower |

Raw five-sample p95/p99 sequences (milliseconds; p95 and p99 were identical within each short sample) were:
small warm r3 `61, 57, 57, 34, 40`, small warm branch `69, 48, 44, 57, 38`; small cold r3
`65, 36, 69, 73, 73`, small cold branch `61, 65, 38, 65, 69`; large warm r3
`150, 150, 150, 150, 150`, large warm branch `48, 53, 53, 34, 38`; large cold r3
`150, 200, 150, 150, 200`, and large cold branch `73, 73, 73, 77, 77`.

The large baseline trace showed a main-thread full decode, a 4096×4096 texture upload and approximately
64 MiB of bitmap growth. The task-branch trace places decode work on `DefaultDispatcher`, uploads a 2048×2048
preview and grows bitmap memory by approximately 16 MiB. Full intrinsic dimensions remain the geometry source;
the sampled bitmap is display-only. Small-image measurements justify leaving already-small inputs unsampled.
The local verification traces are `zinely-reframe-large-main.trace` (SHA-256
`0410d41074733f2f27df602d25b4163343e25f48e97d73f2e0ce59b76f5b3d7b`) and
`zinely-reframe-large-branch.trace` (SHA-256
`eb59aa3233916692f310aa1b6bf0456753d2fff4788488900f250dca38c2e5ab`); they contain no task photo payload
and are identified here rather than checked into the repository.

The same atomic loader result supplies intrinsic size plus optional display pixels. Tests can now inject the
specific “measurable but undisplayable” state without consuming a one-shot stream, so the ignored #57 case is
restored and scheduling cannot change its precondition. The public r3 APK and its digest remain untouched;
these are repository-branch findings until review, CI and merge complete.

The remaining paragraphs in this September section preserve the **initial audit snapshot**. Its
read-only status, Tally preference, missing form, and uncommitted/deployment statements are historical.
**Pre-merge verification:** independent review **GO**, including a separate browser regression run and
inspection of desktop/final, paused diamond, mobile/200%-text, Roadmap, and Changelog captures. Accepted
and fixed enlarged-text button word splitting and disabled-button hover movement. The apparent skip-link
overlay in full-element captures is a capture artifact: a direct viewport-bounds assertion confirms the
unfocused link is offscreen. `node --check` and `git diff --check` pass. Tooling versions: Playwright 1.63.0,
axe Playwright 4.13.0; browser is isolated Chromium, not the owner's signed-in profile. A physical novice
paper test, form submission/delivery, and native-app animation acceptance remain unclaimed.

The broader mobile pass found the existing privacy permissions table was 650 px wide, ending at x=699
on a 390 px viewport. Wrap it in the existing scroll-container style, with a named, keyboard-focusable
region. Keep readable column widths inside that container rather than squeezing words into slivers.
This is a focused existing reflow defect, not a policy change; verify Jekyll's rendered table before merge.

Deployment gate outcome: PR #63 merged at `7d0960b` after review; main-head Android/core CI and the main-only
Pages deployment both passed. This replaces the pre-merge instruction while preserving the audit history below.

This checkpoint supersedes the older next-action/status wording below. Planning authority remains
[ROADMAP.md](../ROADMAP.md#current-priorities); this section owns the evidence, not a second backlog.

### Scope and confidence

- Reviewed `730fe47` on `feat/zine-backup-v2`, current source/tests, stakeholder and performance reports,
  live GitHub issues/PRs/releases/CI, and the public website. Home, Roadmap, Changelog and Privacy returned HTTP 200.
- Samsung SM-A176B is connected; package read-back reports `0.9.0-beta.4-r3`, code 9. This was **not** a fresh
  device UX, performance, TalkBack or physical-paper test. No app data, settings or APKs were changed.
- The public APK is older than HEAD: the app About simplification at `4bcd0a9` is repository-implemented, not in
  the frozen r3 artifact. Website updates are deployed independently. A green HEAD is not a rebuilt public APK.
- Prior accepted Replace Photo, Flip, Reframe routing, Duplicate, deletion, Add/Art, backup and PDF work is not
  reopened. The app remains open to improvement; a missing feature is not automatically a defect.

### What is worth improving

| Candidate | Observed fact | Assessment and next evidence |
|---|---|---|
| Reframe entry loading | The 10 September follow-up above measured small/large bounded masters and traced the large-image decode. Issue #56 remains open pending branch merge. | **Measured branch improvement, not yet public.** Keep the 2048 px bitmap display-only, preserve full intrinsic geometry, and close #56/#57 only after independent review and latest-head CI. |
| Disabled Font verb | `benchContextVerbs(TEXT)` explicitly sets `Font` disabled with `Not yet`. This is distinct from the corrected inline typing row. | **High confidence presence; medium confidence benefit.** Prototype removing this unavailable action rather than building an entire font system to justify it. Check whether makers can still find Edit, Size, Ink, Duplicate and Delete. Requires a freeze amendment. |
| Feedback access | Website contact links work; there is no submission form. Current `ColophonScreen` contains paper preference, licences, privacy copy and version, but no feedback route. | **High confidence gap; recommendation, not a defect.** A linked external form can avoid requiring a configured email app or GitHub account. Keep email visible. If demand warrants an app entry later, add one quiet external link through the existing surface, not a new screen/SDK. |
| Fold understanding | Website has ten complete static steps with optional arrow tracing; native guide has not been replaced. No novice comparison test is recorded. | **High confidence evidence gap.** Test paper execution before porting motion. Success is fewer wrong cuts/folds or requests for help, not more animation. |
| Dense toolbar / finding old zines | Context verbs scroll at enlarged text; shelf search/sort were deliberately omitted, with newest-first repository order. Existing tests protect reachability. | **Hypotheses, not observed regressions.** In a small novice task session, ask for Delete after selecting a photo and for a named older zine in a realistic library. Only a repeated failure justifies another affordance or search UI. |
| Storage and recovery edges | Image sweeping is explicitly deferred until imports pin assets; private-stage low space is classified, but provider writes can still map to generic IO. Existing recovery reports are bounded. | **Known technical work, not a quick polish task.** Use disposable storage/provider fixtures and a second API/device. Do not ship a “clean storage” button ahead of liveness/undo/recovery safety. |

The 31 August Art follow-up measured about 89.6 ms in first `measureAndLayout`, 17.1 ms recompose and 7.6 ms
diagnostic body draw; the earlier “raster” label was too broad. It explicitly deferred production changes.
Preserve the shared host and path warmup. This is lower priority than #56 unless new tester evidence changes it.

**Recommended short usability round:** ask first-time makers to create a page, edit/align its text, distinguish
Resize from Reframe, find Duplicate/Delete, visit another page, save and locate the PDF, then fold it. Separately,
ask them to locate a backup and explain what it protects. Record the exact task and hesitation without coaching.
The earlier stakeholder findings were fixed; this round looks for remaining friction, not reasons to reopen them.

### GitHub hygiene — read-only findings

| Item | Current evidence | Recommended action, not performed |
|---|---|---|
| Branches | Task branch and local `main` each match their own remote. Task HEAD is 96 commits ahead of `origin/main`, with zero commits behind. Default branch is `main`. | Retire the old claim that local main is diverged. Review the feature branch for an explicitly approved merge; do not merge merely to tidy the graph. |
| PR #63 | Draft, mergeable; latest HEAD CI `34357389981` passed core and Android/lint/test jobs. | Review the full release/app change set and evidence before marking ready. A documentation audit is not merge approval. |
| PR #62 | Open; its remote head `28c28e3` is already an ancestor of task HEAD. Its local branch also has one unpushed commit. | After #63 disposition, reconcile as incorporated/superseded if appropriate. Preserve the local extra commit and worktree ownership. |
| Issue #53 | Replace Photo implementation, tests and r3 release evidence exist, but issue is open. | Close with implementation evidence after owner approval; do not schedule Replace again. |
| Issue #55 | Its “no goldens” premise is stale: `ReframeControlsGoldenTest` has light/dark captures. Full photo-overlay coverage is a separate question. | Narrow to uncovered overlay states; do not claim all Reframe visual debt is closed. |
| Issues #56 / #57 | Synchronous decode and the ignored accessibility test are both still in source. | Keep open; investigate together. |
| Issue #54 | A visual long-press sheet is still a future enhancement; selected-object actions already have a visible toolbar. | Reassess usefulness instead of adding a duplicate menu by convention. |
| Releases | One published prerelease, r3; asset size 16,052,043 bytes, digest `3522042340e85042b7e3e314ae07de49d85fe7b4da553462438cd9f1ab97c3a1`. Annotated tag peels to `534831a`. | Preserve the artifact/tag. Release API `targetCommitish=main` does not override the actual tag target. No release repair is indicated by that field alone. |
| Pages / public links | Pages run `34357153693` succeeded; both main and feature-branch pushes can deploy. Public engineering links pointed at older `main`. | Point the revised roadmap at the actual development plan meanwhile; after an approved merge, use main-only Pages and main links. Do not change deployment ownership now. |
| Old checklists | Owner checklist still lists missing policy hosting, missing Art outlines, unreachable Mirror and a diverged main; newer code/reports contradict these. | A bounded evidence-backed tracker reconciliation is useful. Do not bulk-close the entire historical checklist. |

No GitHub issue, PR, release, branch or worktree was mutated by this check. Existing protected untracked paths
remain untouched. No broad secret scan or security certification is claimed.

### Feedback recommendation

Prefer a **link to a short owner-controlled Tally form**, not an embedded widget or custom backend; keep the
current email fallback. Google Forms is a workable alternative if avoiding another owner account is more important.
GitHub Issues can remain an optional technical route, not the primary feedback UI for makers.

Suggested fields: one required message (“What would you like to tell me?”), optional category (idea/problem/general),
and optional email (“Only if you want a reply”). Ask for app version/device/reproduction steps as optional bug-report
guidance, not required fields for every idea. No name, account, photo upload, zine attachment or automatic diagnostics.
Confirmation: “Thanks for helping make Zinely better. I read every message, but can't promise every idea will be built.”
That first-person promise must be accepted by the owner before publication.

Before activation: owner selects the provider/account, approves response access and a retention/deletion practice,
and confirms who reads messages. Keep drafts/partial submissions and tracking integrations off; review provider
metadata rather than promise anonymity. Update the website privacy section (currently explicitly says no form),
including voluntary feedback processing separately from the offline app. Test keyboard, mobile, zoom, errors,
screen-reader confirmation, spam handling, delivery and deletion using non-personal test data. Do not embed provider
code in the app. [Sources and trade-offs](../RESEARCH.md#r17-feedback-and-quality-of-life-review).

### Handoff

Validation of this documentation-only change: `git diff --check` passed; W3C Nu returned no messages for the
revised public Roadmap and Changelog. Isolated Edge found no meaningful main-content overflow at 1200 px,
390 px, or 320 px with 200% root text; mobile renders were inspected. The normal browser integration was unavailable.
No Android build/test suite was rerun and no fresh device UX acceptance is claimed.

Independent review: **GO**, no required fixes remaining. Accepted the request to avoid implying a measured
Reframe pause and to label the one-message form as a recommendation. The reported heading-order issue was not
present on final read-back (one H1, first line). Reviewer independently confirmed the source/issue/release/branch
facts and the Planned/Exploring/Completed boundaries. Six scoped documentation/site files remain local and
uncommitted for owner review; no GitHub mutation or deployment was performed.

This change is roadmap/content reconciliation only: no app fix, new form, provider configuration, release or merge.
Next engineering brief: measure #56 with the existing pipeline, fix #57's test seam, propose a bounded loading
change only if measurements justify it, and carry any implementation through normal parity/device/review gates.
Next owner decision: whether to approve that slice and which hosted-feedback provider/retention practice to use.

## Scope and evidence boundary

**Historical 27 August checkpoint follows.** Read the September checkpoint above for current dispositions.

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
