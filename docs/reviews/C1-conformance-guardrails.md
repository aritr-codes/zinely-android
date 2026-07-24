# C1 — Conformance Guardrails · milestone record

> **Status: merged to `main` (`a139fac`, 2026-07-23).** The first engineering milestone of the
> [V1 conformance programme](../V1-IMPLEMENTATION-ROADMAP.md). Test-infrastructure only — **zero
> `src/main` change**. This is the regression net that the later token/surface milestones (C3+) walk on.
> Governing: [V1-CONFORMANCE-INVENTORY](../V1-CONFORMANCE-INVENTORY.md) CI-25…CI-33/CI-93 ·
> [V1-IMPLEMENTATION-ROADMAP §C1](../V1-IMPLEMENTATION-ROADMAP.md).

## What C1 did

Built the net before the migration: goldens for the Editor, a platform-accessibility-tree assertion
harness, a static token-discipline gate, CI wiring for two never-run suites, and a11y/contrast
assertions — so that when C3+ start changing drawn values, a changed pixel or a broken semantic is
**loud**. Delivered by a fleet of file-disjoint subagents (one owner per file, one item per branch),
each **independently review-agent verified** before integration.

## Definition-of-done ledger (honest)

| Item | Outcome |
|---|---|
| **CI-25** Editor golden net (11 composables × light/dark) | ✅ Closed — with per-component `assertExists` non-vacuity guards |
| **CI-26** platform `AccessibilityNodeInfo` harness | ✅ Closed — reliable for the `enabled` bit; className/Role only on leaf/`clearAndSetSemantics` nodes (documented bound, see ADR-059) |
| **CI-27** token-discipline static gate + enrolment list | ✅ Closed — enrolment empty by design (nothing migrated yet); runs fresh in CI |
| **CI-28** `:app` + `:feature:editor` named CI tasks | ✅ Closed |
| **CI-29** `stateDescription` assertions | ✅ Closed |
| **CI-32** WCAG AA contrast test | ✅ Closed |
| **CI-93** disabled/enabled platform-tree assertions | ✅ Closed — **stronger than planned**: covers the real f4faaa4 `ReframeControls` stepper on `main` |
| **CI-30** Role | ⚠️ **Partial** — platform-tree Role on leaf controls (Reframe); merged-tree Role elsewhere; the platform-tree remainder is **deferred to the on-device passes** (forced by ADR-059's Compose behaviour, disclosed in the tests) |
| **CI-31** keyboard/traversal-order test | ⛔ **C0-blocked** — prereq CI-13 (Premium-Checklist location) unresolved |
| **CI-33** large-text / smallest-width golden | ⛔ **C0-blocked** — prereq CI-12 (A-8's **modality** clause — an earlier draft of this line said *density*, which is not one of A-8's two clauses) — **resolved 2026-07-24, [ADR-066](../DECISIONS.md#adr-066); CI-33 is no longer blocked** |

**7 fully closed · 1 partial (device-deferred) · 2 C0-blocked.** C1 cannot be declared *fully* closed
until C0 resolves CI-12 and CI-13.

## Verification evidence

- Every item independently falsified by a Review Agent against actual repo state (not summaries),
  each returning **GO** (A6 GO-WITH-FIXES → reconciled: three test-KDoc accuracy corrections, no code fix).
- The three net-proof items carry a **durable inject→revert commit pair** in history (the failing proof
  is permanent; the branch tip is `src/main`-clean): CI-26, CI-27, and CI-93 (incl. the ReframeControls
  stepper).
- **Merged-tree milestone gate:** `:feature:editor:verifyRoborazziDebug` +
  `:render-android:verifyRoborazziDebug` + `:app:testDebugUnitTest --rerun-tasks` → **BUILD SUCCESSFUL**,
  independently re-run by the final Review Agent (the Windows Robolectric NATIVE image-decoder message
  appeared but did **not** fail the build; CI/Linux is authoritative).
- `git diff main -- '**/src/main/**'` across the whole milestone: **empty**.

## Rebase-onto-`main` incident (disclosed, not absorbed)

The subagent worktrees were created from **`ec2c58b`, 12 commits behind `main` (`57f1e8b`)** — a base that
predated substantive production changes (the `f4faaa4` TalkBack fix, Read mode / ADR-058, a required
`abilities` parameter on `ReframeControls`, EditorScreen/ProofScreen edits). The first consolidated gate
caught it as a **compile failure**, exactly what the merged-tree gate exists for. Remediation was
fix-forward: a repair pass rebased the diverged-surface tests onto `main`, added the now-real
ReframeControls CI-93 coverage, and re-recorded **only** the two `editor_screen` goldens that `main`'s
layout actually moved (all others byte-identical) — then re-ran the gate green and was independently
reviewed GO. **Process lesson:** verify agent worktree bases are on the intended baseline (`main@57f1e8b`)
at dispatch, not after.

## Carried-forward findings

- 🔴 **`Role.Button → android.view.View` TalkBack defect** — house buttons announce as generic "View" in
  `0.9.0-beta.1`. Genuine (independent platform-tree probe; identical on-device). Fix **scheduled to
  C4/C6/C7** — [ADR-059](../DECISIONS.md#adr-059).
- 🟡 **`onDeskFaint`/`field` dark contrast = 3.917:1 (sub-AA)** — `onDeskFaint` is used as small secondary
  text (ProofPrint recipe labels, Shelf "Search your zines" placeholder, menu sublabels) yet the C1
  contrast test only pins `onDeskFaint`/`desk`. A pre-existing production concern → **C6/C7** (a token
  value change would be a C3a design escalation).
- 🆕 **Read-mode net (ProofRead goldens + a11y)** — the new ADR-058 Read surface has no C1 coverage
  (didn't exist at the frozen CI-25 scope). **Deferred** (owner decision) to the Proof/Read follow-up track.
- 🔧 **Token-gate follow-ups** (fire when the first package enrols, C3/C6): document/handle the `${…}`
  string-template blind spot; declare `config/token-enrolment.txt` as a Gradle task input to close a local
  warm-cache skip. CI is unaffected either way.

## Blocks the rest of the programme (C0 — owner track, not started)

- The V1 design corpus is **still untracked in git** (dangling authority references from committed tests).
- No **motion-and-haptics baseline** recorded on device.
- The **A-5 ruling, the eight validation additions, the §15 open items, the shelf-cover wire-or-delete,
  and the HTML re-freeze** are unmade. Until C0 lands, CI-31/CI-33 stay parked and C3+ cannot begin.
