# Zinely 1.x — owner decision brief

Status: **for the owner. Nothing here is decided, and no option is recommended or ranked.** Prepared 2026-09-25,
read-only, against `origin/main` @ `5f7707a` (beta.5 released: tag `v0.9.0-beta.5` = `32da280`, an ancestor); rebased
2026-09-26 onto `eb75cf7`, where **Step 0 is complete** (PR #75). Step 0 answered none of these questions.

**What this is.** A neutral, one-sitting sheet listing only the decisions that implementation is waiting on.
- It restates each question from the [decision gate](ZINELY-1X-DECISION-GATE.md) without the research's
  framing.
- The gate **owns the question text and evidence**, and remains the full register, with the research's 🟦
  framing and the deferrable O-items. This brief mirrors it.
- If the two disagree on a fact, the cited code or ADR decides, and both are corrected.

**Where rulings go.** Not here, and not in the gate. A ruling counts only once it is recorded as a dated line
in the record its [OWNER-CHECKLIST](../OWNER-CHECKLIST.md#15-product--design-authorship) row links to, or as an
ADR. Implementation sessions read only recorded rulings
([handoff template](IMPLEMENTATION-HANDOFF-TEMPLATE.md)).

**Labels used below.**
- ✅ **Fact:** verified in the repository or a primary source, with a citation.
- ⚠️ **Unverified:** a claim that still needs measuring.
- ⚖️ **Judgment:** a product or design value call. Evidence can inform it but cannot settle it.

---

## 1. The decisions, grouped by what they need

| Needs | Decisions |
|---|---|
| **Can be made immediately** (desk decisions) | Q1 · Q3 · Q4 · Q5 (whether to run the fold study) · Q8 (a), (b), (c) |
| **A physical or device study first** | Q2 (one printed page of Averia, plus sample maker text) · Q7 (the print study) · Q5's *outcome* (the fold study) · PR #70 acceptance (hands-on TalkBack; not a decision, [§4](#4-already-decided--acceptance-work-only)) |
| **An HTML amendment the owner approves** (ruling first, then amendment) | Q3 (a `v21-bench.html` behaviour note) · Q4 (which prototype route D2 amends) · Q6 (the `v21-bench.html` Art set) · Q7 (`v21-proof.html` Print step) · plus the amendments in [§5](#5-html-amendments-that-need-owner-approval) that follow from no Q at all |
| **Can safely be deferred** | Q2 until before step 8 · Q6 until before step 9 · Q7 until the print study reports (step 5 is optional for wave 1) · every row of the gate's [can-wait table](ZINELY-1X-DECISION-GATE.md#decisions-that-can-wait) (O1, O2, O3, O4, O5, O6 alongside Q1, O11, O13, D4 wording, and the optional F6 guard, which step 0 shipped without) |

✅ Step 0 is complete. No question blocks plan steps 1, 2 or 3 ([sequence](ZINELY-1X-IMPLEMENTATION-PLAN.md#5-sequencing));
step 1b waits for Q8 and a spec, and step 3 follows PR #70's acceptance. Steps 1 and 3
still need their HTML amendments approved ([§5](#5-html-amendments-that-need-owner-approval)).

---

## 2. Q1–Q8

### Q1 — Android developer verification (O15)

- **Question:** register as a verified Android developer, and if so when, and under which account (personal
  or organisation)?
- **Evidence:**
  - ✅ Google requires side-loaded APKs to come from a verified developer. The requirement starts
    **30 Sep 2026** in Brazil, Indonesia, Singapore and Thailand, and goes global in 2027
    ([developer.android.com/developer-verification](https://developer.android.com/developer-verification)).
  - ✅ Zinely ships only as a GitHub APK today (ROADMAP; beta.5 pre-release).
  - ⚠️ Whether the timeline or the requirements have changed since the research: re-read the primary page.
  - ⚠️ Whether the package name or signing key must be registered. If so, it touches O6, the Play signing
    key, which is irreversible.
- **Blocked by it:** installing any public APK in affected regions after the cut-over, including the
  wave-1 release.
- **Not blocked:** any code. Steps 0–9 can all be built.
- **Options:**
  - (a) Register now, personal account.
  - (b) Register now, organisation account.
  - (c) Register later, before a named release.
  - (d) Don't register, and accept that affected regions can't install.
- **⚖️ Judgment:** which account, and whether reach in the affected regions matters before 2027.

### Q2 — Typefaces inside a zine (O12 + O8)

- **Question:** which typefaces may set text inside a maker's zine, and under which rule? It has four
  sub-questions, in this order:
  1. **Scope.** Does V2-CONSTITUTION §III govern zine text, or only Zinely's interface?
  2. **Rule.** If §III governs zine text, which rule applies?
  3. **Coverage.** Is a voice acceptable when it can't set common European letters?
  4. **Older builds.** Which failure do older builds meet?
- **Evidence:**
  - ✅ **§III points both ways.** It names "zine body, captions" and "the maker's own short strings", and also
    "No fourth UI typeface". It rules that "the imperfect face never sets running text"
    (`docs/design/V2-CONSTITUTION.md` ~:138-153). Ruling "interface only" is a §VI amendment.
  - ✅ **Every zine today is set in Inter.** `DocumentFontRegistry` registers one family, and the editing
    surface draws Inter whatever the zine's font, faking italic (`BenchEditingSurface.kt:213-224`).
  - ⚠️ **Averia is Latin-only.** About 115 of 127 Latin Extended-A letters are missing; re-count from the
    font file. Missing letters fall back to a device font silently.
  - ✅ **Older builds render an unknown font as Inter.** For a narrower face this clips text. The ROADMAP
    says *"Unknown-font fallback must not silently become layout loss"*
    ([ROADMAP](../ROADMAP.md#creative-tools-assessment-2026-09-12)).
  - ✅ **A schema bump has a cost.** Once a library holds one newer-version zine, the whole backup refuses to
    restore on an older build (`ZineLibraryBackupStager.kt:367-376`).
  - ✅ **The release boundary matters.** A version number guards only the fields released with it
    (`JsonDocumentSerializer.kt:45-48`). A D2 that lands after v4 is released needs v5
    ([plan F1b](ZINELY-1X-IMPLEMENTATION-PLAN.md#4-foundations--only-what-the-repository-actually-needs)).
  - ⚠️ **Print behaviour is unmeasured:** printed x-height and legibility at 10–14 pt on a home inkjet.
- **Blocked by it:** all of D2 (plan step 8): its ADR superseding ADR-055's exclusion, its HTML amendment,
  its schema plan, and the rewrite of [Brief 02](#3-briefs-02-and-03).
- **Not blocked:** steps 0–7, D5, and PR #70's acceptance.
- **Options, per sub-question:**
  1. **Scope:**
     - (a) §III governs zine text.
     - (b) Interface only, by an explicit §VI amendment.
  2. **Rule (if 1a):**
     - (a) Hand for short text only. Define the unit, and what happens past it: block, warn or keep.
     - (b) Amend §III.
     - (c) Two voices: Book + Plain.
     - (d) Two voices: Hand + Plain (`ZINE-DIRECTION.md` ~:974).
     - (e) A different imperfect face that covers Latin Extended-A.
  3. **Coverage:**
     - (a) Accept Latin-only voices.
     - (b) Disable a voice for text it can't set.
     - (c) Require full Latin Extended-A coverage.
  4. **Older builds:**
     - (a) No bump: text clips. This amends the ROADMAP rule above on purpose.
     - (b) A schema bump: older builds refuse the whole backup. This means v4 if D2 lands before v4 is
       released, v5 after.
  - Also: "named voices" or "a real picker" (the ROADMAP wording).
- **⚖️ Judgment:** all four sub-questions. The facts bound them; they don't decide them.

### Q3 — Tap-through hit-testing: how far it reaches

- **Question:** when a maker taps the empty part of an Art piece, should the tap reach what is beneath it?
  If so, for which pieces?
- **Evidence:**
  - ✅ **Every element is hit by its bounding box** (`HitTest.kt:21-36`).
  - ✅ **Six holed pieces ship:** `paper.window`, `paper.hole`, `shape.ring`, `fix.grommet`, `fix.corner` and
    `mark.registration` (`SupplyCatalog.kt`). A tap inside any of them selects the piece, never the photo
    beneath. Double-tap to Reframe is blocked the same way (`EditorReducer.kt:137-145`). The workarounds are
    *Send backward* and TalkBack.
  - ✅ **The box hit test is inherited behaviour,** not a rule the frozen Bench spec states. So changing it is
    an interaction change, and the HTML is updated first (CLAUDE.md, DESIGN FREEZE).
  - ✅ **32 supplies ship.** Outline hit-testing for all of them changes taps on thin and open pieces too:
    rule, crop marks, halftone, staple, clip.
  - ⚠️ **Thin pieces may become hard to grab.** A tolerance is designed in Brief 05, and a Pass 2 device
    session would show it.
- **Blocked by it:**
  - plan step 4, and its `v21-bench.html` behaviour note;
  - D5 frames (step 9), which require step 4 merged.
- **Not blocked:** steps 0–3, step 5, and the release (step 4 is part of the wave-1 set only if Q3 is
  answered in time).
- **Options:**
  - (a) Outline hit-testing for the six holed pieces only.
  - (b) Outline hit-testing for all Art, with a touch tolerance.
  - (c) No change.
- **⚖️ Judgment:** how much existing tap behaviour may change to fix the holed pieces.

### Q4 — Are the typebar and reframe prototypes frozen? (O10)

- **Question:** are `docs/design/mockups/v21-typebar.html` and `v21-reframe.html` frozen specifications, or
  proposals?
- **Evidence:**
  - ✅ Their headers say "PROPOSAL, NOT FROZEN".
  - ✅ ZINE-DIRECTION N2 treats them as frozen.
- **Blocked by it:** D2's design route: amend and re-freeze, or finish and freeze first.
- **Not blocked:** everything except D2. Reframe is shipped behaviour (ADR-053), and this question changes
  no code.
- **Options:**
  - (a) Both are frozen: correct the headers.
  - (b) Both are proposals: correct N2.
  - (c) One of each.
- **⚖️ Judgment:** what you intended when N2 was written. No evidence settles it.

### Q5 — The fold study before creative work (O14)

- **Question:** run the fold-clarity study (6–8 first-time makers, about 30 minutes each) before D2 or D5
  starts, or allow either to start first?
- **Evidence:**
  - ✅ The ROADMAP lists fold clarity first among "Exploring" items.
  - ✅ The ROADMAP says the creative slice "does not reorder the … fold-study … gates without owner approval".
  - ✅ The study costs people and paper, not code
    ([audit §4](ZINELY-1X-READINESS-AUDIT.md#4-wave-0-audit)).
  - The outcome is unknown until the study runs.
- **Blocked by it:**
  - when D2 (step 8) and D5 (step 9) may start;
  - any Proof fold-caption fixes that the study's findings lead to.
- **Not blocked:** steps 0–7, the wave-1 release and v4.
- **Options:**
  - (a) Run it; D2 and D5 wait for its report.
  - (b) Run it alongside, and allow D2 and/or D5 to start before it reports.
  - (c) Don't run it, and approve the reorder explicitly.
- **⚖️ Judgment:** whether fold clarity outranks creative tools.

### Q6 — Frames (O9 + stretch)

- **Question:**
  - which frames and backlog supplies ship, and who draws their outlines;
  - whether Art keeps its proportions when resized.
- **Evidence:**
  - ✅ **Resize is free-aspect** (`TransformMath.kt:77-78`). The uniform-scale lock in SUPPLIES-SPEC §3.4.1 was
    never built, and that is the open defect [D-100](../design/V2-SPEC-DEFECTS.md).
  - ✅ **Even window borders are out of reach today.** Neither free stretch nor a uniform lock keeps a
    window frame's borders even on a non-square photo. Even borders need nine-slice-style scaling, a new
    render concept that would need its own ADR.
  - ✅ **ADR-107 constrains frames.** R1 leaves about 19 researched supplies in the backlog; R1a makes set
    membership the owner's call; R3 withdrew composite frames.
  - ✅ **A per-supply landing size breaks the `{shape.rule}`-only placement override.** That needs a
    SUPPLIES-SPEC §5.2 ruling (`SupplyPlacement.kt:133-139`).
- **Blocked by it:** D5 (step 9), its `v21-bench.html` Art-set amendment, and any resize-lock session.
- **Not blocked:** steps 0–8, including the hit test.
- **Options:**
  - **The set:** any subset of the backlog, plus drawn frames.
  - **The author:** you, a named illustrator, or the implementer under review.
  - **Stretch:**
    - (a) Free stretch, as today.
    - (b) A uniform lock per listed supply.
    - (c) A lock for the ring-like pieces only.
    - (d) Border-preserving scaling, which needs a new ADR.
- **⚖️ Judgment:** the set, the look, and who draws it.

### Q7 — What print guidance may promise (O13 + D3 stage 2)

- **Question, after the print study:**
  - (a) is the stage-1 guidance (a fold-line check in Proof's Print step) enough?
  - (b) should a static test page also ship?
  - (c) if some printers can't reach the 17 pt inset: a per-device reach profile, or one wider margin for
    everyone?
  - (d) what may the app say about scale?
- **Evidence:**
  - ✅ **Shipped copy already promises "100% · Actual size"** (`Copy.kt` ~:1289-1297).
  - ✅ **The app can't print.** Android's system print dialog has no scaling control (ADR-052).
  - ✅ **A documented worst-case margin exceeds the inset:** Canon PIXMA on Letter needs 6.4 mm, and the
    inset is 6.0 mm (audit §7 citation).
  - ⚠️ **Some Android print paths may offer no 100 %.** Reports are weak and must be measured.
  - ✅ **Each follow-on has its own ADR route.** A test page amends ADR-039 (the ruler deferral). A wider
    inset amends ADR-012 and the frozen `v21-bench.html` keep-clear band.
- **Blocked by it:**
  - D3 stage 1's wording and stage 2;
  - any inset change;
  - the rewrite of [Brief 03](#3-briefs-02-and-03).
- **Not blocked:** steps 0–4, the wave-1 release (step 5 is optional for it), and everything after.
- **Options:**
  - (a): yes or no.
  - (b): yes or no.
  - (c): a reach profile, one global margin, or no change.
  - (d): keep the current copy, reword it to what the study shows is reachable, or remove the scale claim.
- **⚖️ Judgment:** how much guidance is enough. The study supplies the facts for (c) and (d).

### Q8 — The next release, and the backup/restore defects

- **Question, in three parts:**
  - **(a) Release shape.** Cut a wave-1 release on today's document format before the v4 schema bump? The
    ROADMAP schedules no next build today.
  - **(b) Restore-message defects.** Schedule the shipped restore-message defects, and when?
  - **(c) Skip-and-list backups.** Should a backup skip a zine it can't open, and say how many it left out?
- **Evidence** ([audit §5](ZINELY-1X-READINESS-AUDIT.md#5-d1-audit--last-backed-up--changing-phones)):
  - ✅ **Shipped defects** (constitution Article 5):
    - "Restore cancelled." shown although zines were added;
    - "Couldn't read that file" after a restore that succeeded;
    - a full disk reported as "This backup looks damaged";
    - "Backup cancelled." after a complete file was saved;
    - a poisoned local photo blamed on the backup file.
  - ✅ **A v4 zine anywhere in a library makes the whole backup unrestorable on older builds.** v4 is planned
    to be held unreleased from step 6 until step 7 lands, and no release can be cut in between.
  - ✅ **One unreadable zine blocks every backup** (`RoomProjectRepository.kt:431-433`). If it was unreadable
    when indexed, it has no shelf row (:604-605, :674-676); if it was damaged later, it keeps its row (:300,
    :687).
  - ✅ **Skip-and-list would change what a backup contains**, so it needs an ADR amending ADR-110.
  - ✅ **Partial-file cleanup is already in D1 part 1** ([Brief 01](BRIEF-01-VISIBLE-OWNERSHIP.md)). It is not
    part of this question.
- **Blocked by it:**
  - (a) steps 6–8 and Brief 04 part B;
  - (b) step 1b, which has no slot until scheduled, and no spec yet;
  - (c) any backup-content change.
- **Not blocked:** steps 0–5, including D1 part 1, which only makes the failure honest.
- **Options:**
  - **(a):**
    - a wave-1 release of steps 0–4, plus step 5 if the print study has reported;
    - a different release set;
    - no release before v4.
  - **(b):**
    - a separate step 1b in wave 1;
    - folded into step 1;
    - after the wave-1 release;
    - unscheduled.
  - **(c):**
    - yes, via an ADR-110 amendment;
    - no;
    - later.
- **⚖️ Judgment:** release cadence, and how much backup behaviour may change.

---

## 3. Briefs 02 and 03

Neither brief was rewritten: the session's permission classifier denied both rewrite agents, and no other
route was tried. **Neither is ready.** Each carries a "Corrections pending" banner, and the handoff template
stops a session that meets one.

| | **Brief 02 — Three voices (D2)** | **Brief 03 — Printer test page (D3)** |
|---|---|---|
| **Stale or incomplete** | Base `5c40e7b`, placed in "wave 1" (now step 8, after v4). O7 framed as an open choice (it is approved). No readiness line, authority header or product audit. Missing: the editing surface draws Inter and fakes italic; Averia's Latin Extended-A gap and its silent fallback; the older-build choice and the v4/v5 release boundary; §III read as settled when it points both ways; the two-voice fallback conflict (Book+Plain vs ZINE-DIRECTION's Hand+Plain); no Fraunces optical size; printed x-height ([audit §6](ZINELY-1X-READINESS-AUDIT.md#6-d2-audit--typefaces--voices)) | Base `5c40e7b`. Band test flawed: a shrunk print shows every band, the bands straddle the threshold, and a 50 mm ruler barely detects a 2–4 % shrink. Promises "Actual size" advice that may be unreachable on Android. Verification points at a print procedure that doesn't exist. Not staged (stage 1 guidance → stage 2 test page). Inset change and reach profile are bundled rather than separated. No readiness line, authority header or product audit ([audit §7](ZINELY-1X-READINESS-AUDIT.md#7-d3-audit--printer-test-page)) |
| **Needed to rewrite** | Q2's four rulings. Q4 (the design route). Q5 (the start condition). PR #70's outcome (merged or closed, ADR-115). A re-counted Averia coverage table from the font file. One printed Averia page at 10/12/14 pt. The next free ADR number | The print study's results: scale per print path, whether 100 % is reachable, printer reach on at least 3 brands on A4 and Letter, and 2–3 first-time makers following the guidance. Q7's rulings. The `v21-proof.html` amendment scope, shared with any fold-study fixes. A "Print pass" section for DEVICE-VERIFICATION |
| **Immediate blocker if deferred?** | **No.** Nothing before step 8 reads it | **No.** Step 5 is optional for wave 1, and nothing else reads it. ⚠️ The print study itself must *start* early, or step 5 misses wave 1 |
| **Must be resolved by** | Before step 8 (D2) opens, after Q2/Q4/Q5 are recorded | Before step 5 (D3 stage 1) opens, after the print study reports and Q7 is recorded |

---

## 4. Already decided — acceptance work only

- **PR #70** (remove the dead Font control): **owner-approved**
  ([ROADMAP](../ROADMAP.md#in-development--built-or-being-tested-not-in-the-public-download)).
- **Remaining work:** hands-on TalkBack and rendered HTML parity.
- **Sequencing evidence:**
  - ✅ The local branch `editor/remove-unavailable-font` forks from `ad86586`, before beta.5.
  - ✅ It changes `v21-bench.html`, `EditorScreen.kt`, `DECISIONS.md` (ADR-115), `ROADMAP.md` and
    `OWNER-CHECKLIST.md`. Steps 3 and 4 also amend `v21-bench.html`, and step 3 changes `EditorScreen.kt`.
  - See [§6](#6-sequence-check-after-beta5).

## 5. HTML amendments that need owner approval

None of these has been made; the HTML is untouched. Each is its own reviewed change, **before** any Compose
work. An amendment that changes copy or interaction inside a frozen state needs owner approval.

**Hard prerequisites for the next steps**

- [ ] **`docs/design/mockups/backup-restore.html`** — plan step 1 (D1 part 1)
  ([Brief 01](BRIEF-01-VISIBLE-OWNERSHIP.md#uiux-proposal)):
  - the last-backup line in two states, with the file name when reported;
  - the title decision;
  - the "what this file holds" sentence;
  - backup-failure states in backup wording: *can't be opened (may not be on the shelf)*, *made by a newer
    Zinely*, and confirmation of the existing neutral no-space / save-failed / busy copy;
  - every changed string at 360 dp and 200 % text;
  - **if Q8 schedules step 1b:** its restore-message states too, so the frozen file is amended once.
- [ ] **`docs/design/mockups/v21-bench.html` — undo snack**, plan step 3 (D4-A3)
  ([Brief 04](BRIEF-04-READING-ORDER-AND-ALT-TEXT.md#uiux-proposal)):
  - extend the post-undo snack to every undo, replacing "Put back" with named copy in physical words;
  - add a page-changing variant;
  - draw it at 360 dp and 200 % text.
- [ ] **`docs/design/mockups/v21-bench.html` — tap-through behaviour note**, plan step 4, scoped as Q3 rules
  ([Brief 05](BRIEF-05-MATERIALS-FRAMES.md#uiux-proposal)).
- [ ] **`docs/design/mockups/v21-proof.html` — Print-step guidance**, plan step 5 (D3 stage 1), after the print
  study. It may share one amendment with fold-study caption fixes.

**D4 reading order (step 2) needs no HTML amendment.** Its rule is one clause in
[ZINELY-DESIGN-SYSTEM §4.5](../ZINELY-DESIGN-SYSTEM.md), which that document's amendment rule requires to be
**recorded as an ADR** (next free number on `main`: 119), reviewed inside the change. That is a spec amendment,
not an owner question; it is listed here so it isn't mistaken for "no approval needed". D4-A2 needs a wording
spec and your TalkBack listen, not HTML.

**Later, not yet prerequisites**

- [ ] `v21-bench.html` — the Describe verb in `toolsFor()`, for step 7 (D4-B).
- [ ] `v21-proof.html` — Read semantics, for step 7. It starts from the frozen `aria-live` at :634 that the
  build doesn't honour.
- [ ] `v21-typebar.html` / `v21-reframe.html` — step 8 (D2), by the route Q4 sets.
- [ ] `v21-bench.html` — the Art set, for step 9 (D5; ADR-107 R1a).
- [ ] `backup-restore.html` — the "Changing phones?" line, for D1 part 2, after the two-phone restore pass.
- [ ] `v21-bench.html` — the keep-clear band, only if Q7 widens the inset.

## 6. Sequence check after beta.5

The order below was checked against `5f7707a` and re-checked on 2026-09-26 at `eb75cf7`, after Step 0 merged:

> Step 0 → D1 part 1 → restore-message work (1b) → D4-A1 reading order → named undo → hit test → print guidance →
> wave-1 release → v4 → alt text → typefaces → frames

✅ **It still holds.** Nothing shipped in beta.5 removes a step or reverses a dependency:
- The `ZSheet` scrim fix shipped. `ZineActionScrim` (`ZineActionSheet.kt:265-278`) still lacks it, so step 2
  still has it to do.
- The beta.5 TalkBack-focus attempt was reverted (`d957f1f`) and is a known limitation. Brief 04 keeps it out
  of scope.

**Evidence-based adjustments. The order is unchanged; these are insertions and clarifications:**
1. **Slot PR #70's acceptance (merge or close) before step 3.** Its branch predates beta.5 and changes the
   same `v21-bench.html` and `EditorScreen.kt` that steps 3–4 amend. Settling it first avoids two open
   amendments to one frozen file and keeps ADR-115's reserved number from colliding.
   It is recorded in [plan §5](ZINELY-1X-IMPLEMENTATION-PLAN.md#5-sequencing) as a proposed insertion.
2. **Step 2 may run before step 1** if the `backup-restore.html` amendment isn't approved yet. The plan already
   allows this.
3. **Q1 has a date (30 Sep 2026) before any wave-1 APK.** It gates publishing, not building.
4. **Start the print and fold studies now.** They gate step 5 (optional for wave 1) and steps 8–9, and they
   cost weeks of calendar, not code.
5. **No release is cut between steps 6 and 7** while v4 is held.

## Change log

| Date | Change |
|---|---|
| 2026-09-26 | Re-based onto `eb75cf7` (Step 0 merged, PR #75); §1 and §6 re-checked; the steps-not-blocked line names 1b and PR #70. No question changed, no ruling made. |
| 2026-09-25 | Created: a neutral decision sheet for Q1–Q8, with a Briefs 02/03 status, an HTML-amendment checklist and a post-beta.5 sequence check. No ruling made, no option recommended. |
