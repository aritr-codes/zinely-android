# Implementation handoff — template

Use this to open **every** fresh implementation session in Zinely 1.x. Copy the block below, fill in every
`<…>`, and delete nothing: if a field does not apply, write "none" and why. A field you cannot fill is a
reason not to start yet.

Where the answers come from:
- The **direction's brief** in [docs/planning/](.) supplies the spec, files, tests and out-of-scope list.
- The [decision gate](ZINELY-1X-DECISION-GATE.md) and the records it links supply the owner rulings.
- The [readiness audit](ZINELY-1X-READINESS-AUDIT.md) is the evidence behind the brief. The session reads the
  brief, not the audit.
- **Foundation steps have no brief.** Step 0 (F1a, F3, F4) is complete (PR #75). Step 6 (F1b), and F6 if the owner
  ever opts in, take their spec from the
  F-row in [plan §4](ZINELY-1X-IMPLEMENTATION-PLAN.md#4-foundations--only-what-the-repository-actually-needs);
  for those steps only, [audit §4](ZINELY-1X-READINESS-AUDIT.md#4-wave-0-audit) is also read, as its evidence.
- **A brief split into parts** (Brief 01 parts 1–3, Brief 04 A1/A2/A3/B, Brief 05 hit test / frames) is ready
  only for the part whose readiness line says so.

The general rules live in [CLAUDE.md](../../CLAUDE.md). This template only makes them concrete per session.

---

```markdown
# Implement <direction ID and name> — <part, if the brief splits it>

## Role
You are the Implementer Agent for exactly ONE direction: <ID — name — part>.
Do not start, prepare or "while I'm here" any other direction, foundation or follow-up.
You never self-approve: the session ends with a review handoff, not a merge.

## Base
- `git fetch origin`, then branch `<feat/…>` from `origin/main` @ `<full sha>`. Never branch from local `main`.
- Work in a new worktree: `.claude/worktrees/<name>`.
- The stash stack is shared across sessions. Never run a bare `git stash`; set work aside with a WIP commit.
- Do not merge, push, tag, release, sign, touch Play, or touch any `release/*` branch.

## Authoritative specification
- Brief: `docs/planning/<BRIEF-NN-….md>` at commit `<sha>`, section(s) <…>.
  The brief already includes the readiness-audit corrections. It is the spec.
  For a foundation step: `docs/planning/ZINELY-1X-IMPLEMENTATION-PLAN.md` §4 row <F…> at commit `<sha>`.
- If the brief is still marked "Corrections pending", **stop**: it is not ready.

## Owner rulings
| Decision | Ruling (verbatim) | Date | Source (ADR / record link) |
|---|---|---|---|
| <O-id> | <…> | <YYYY-MM-DD> | <link> |
Every **owner** gate listed in the brief's "Gates" section must appear here (work gates belong under Base or Out of scope). A missing row means stop.
A 🟦 recommendation in a brief or the audit is **not** a ruling.

## Design source
- Frozen HTML: `docs/design/<file>.html`, section/state `<…>`. Amendment status: <not needed | amended and
  re-frozen in <sha> | to be made first in this session, as its own reviewed commit>.
- Implementation follows the HTML; never the reverse (CLAUDE.md "HTML-first UI workflow").

## ADR
- <ADR-NNN — title — Proposed/Accepted>, or "new ADR-<next free number>: <title>, supersedes <…>", or
  "none, because <…>" (e.g. a defect fix inside an existing ADR's contract). Never leave it blank.
  Check the next free number in `docs/DECISIONS.md` first. ADR-115 is reserved on PR #70's branch.

## Files and seams
- Modules and files from the brief's touchpoint table: <list>.
- Coupling rules ([audit §10](ZINELY-1X-READINESS-AUDIT.md#10-architecture--ios-implications)):
  - new pure logic goes in `core:*`;
  - no `java.*`/`android.*` in model, copy, editor, render or imposition;
  - Room, DataStore and SAF stay in `data-android`;
  - Hilt stays in `app` and `data-android`.
- Schema: <no schema change | the v4 bump, authorised by <ruling>>.
  An unplanned schema change means stop: v4 makes whole library backups unrestorable on older builds.

## Tests
| Level | Test (class / name) | Proves |
|---|---|---|
| JVM (pure core) | <…> | <…> |
| Robolectric / Compose semantics | <…> | <…> |
| Roborazzi golden | <…> | <…> |
| Device | see Device validation | |
- Step 0's guards stay green: `PrivacyManifestTest`, `:app:checkDependencyAllowlist`, `DocumentSchemaShapeTest`,
  `DocumentFixtureCorpusTest`, `LibraryBackupFixtureTest`. Never regenerate or edit a fixture or shape file to
  make a test pass (the one exception is the planned v4 bump, plan step 6), and never widen the allow-list
  without review. No INTERNET permission, and no network or analytics library.
- Fixtures, screenshots and archives use **fictional content only**, never the owner's device library.

## Golden rules
- Run `bash tools/grun.sh gold` before claiming anything is visually neutral. It compares with
  `--rerun-tasks`, because a green `testDebugUnitTest` does not compare goldens.
- Open and read every `*_compare.png` diff before deciding anything.
- Never re-record to make a failure go away. Re-record only for an intended change, only via
  `record-goldens.yml` on the pinned CI image, and name every re-recorded golden in the handoff.

## Device validation
- Device: <Samsung SM-A176B, Android 16 (API 36), build <…> | Pixel <…> | emulator API <…>>.
- Pass 1 (did we build it right): <items>. Read the platform tree (`uiautomator dump`) where semantics
  matter; remember that the dump shows child order, not TalkBack traversal.
- Pass 2 (is it right, first-time user): <the screen's question; the ordinary goals to attempt>.
- TalkBack: <not required | required: version, gestures, what must be heard>.
- **Compose focus tests do not prove TalkBack behaviour.** Only a listen on a real device does.
- Do not wipe, fill or restore the owner's device library.

## Documentation (same change)
- ROADMAP: <row/section to update, plus a change-log row>
- CHANGELOG `[Unreleased]`: <plain-language entry; no overpromising>
- ARCHITECTURE: <section, or "none">
- DECISIONS: <ADR to add or amend, with the review outcome noted>
- OWNER-CHECKLIST: <row to strike or add>
- The brief: set its status to "implemented in <sha>" and add a change-log row.

## Website
Do not modify public website copy until the feature is part of a tagged release, per
[ADR-118](../DECISIONS.md#adr-118).

## Out of scope
<the brief's Out of scope list, verbatim>, plus
[readiness audit §15](ZINELY-1X-READINESS-AUDIT.md#15-what-not-to-change-yet).

## Stop conditions — stop, report, and ask; do not improvise
- An owner ruling this work needs is missing or ambiguous.
- A frozen spec would need to change beyond the named amendment.
- An Accepted ADR, the V2 constitution or the brief conflict.
- An unexpected schema, backup-format or permission change appears necessary.
- The code contradicts an architecture assumption in the brief (file moved, behaviour differs).
- Tests or goldens fail for reasons unrelated to this change.
- Anything touches release, signing, versioning or Play.
- The work would take more than the named direction/part.
- <direction-specific stop conditions from the brief>

## Evidence (paste into the handoff, verbatim)
- `git status --porcelain` before the final test run **and** before the commit. A run proves the tree it
  ran in.
- Test results (command, pass/fail counts, any failure output verbatim).
- `bash tools/grun.sh gold` result, and the list of goldens changed or re-recorded.
- Device results for Pass 1 and Pass 2 (device, OS, build, TalkBack version; what was seen and heard).
- The final diff summary: `git diff --stat origin/main...HEAD`.

## Review
End with the CLAUDE.md "Implementer → Review Agent" package: Session Summary, Review Package and a
ready-to-paste Reviewer Prompt. Request **two** independent reviewers with different lenses:
- **code evidence:** correctness, tests, ADR and spec conformance;
- **product and accessibility:** Pass 2, copy, "every screen answers the user's current question".

Both reviewers must treat KDoc, docs and this summary as untrusted and validate the actual repository
state. Reconcile every finding as ACCEPT / PARTIAL / REJECT before asking the owner to merge.
```

---

## Change log

| Date | Change |
|---|---|
| 2026-09-26 | Step 0 merged (PR #75): foundation-step note updated; Tests section names Step 0's guards, which every session keeps green. |
| 2026-09-25 | Created from the readiness audit's appendix, which it replaces. |
