# Zinely 1.x — decision gate

Status: **for the owner to resolve. Nothing here is decided.** Prepared 2026-09-25 from the
[readiness audit](ZINELY-1X-READINESS-AUDIT.md), whose [§11](ZINELY-1X-READINESS-AUDIT.md#11-owner-decisions)
groups all fifteen O-items. Base: `origin/main` @ `eb75cf7` (Step 0 merged, PR #75); evidence gathered at `5f7707a`,
beta.5 released. Step 0 decided none of Q1–Q8.

The plan's fifteen owner items, plus the questions the audit and reviews surfaced, come down to **eight questions**. This gate owns the question text and its evidence; the [owner decision brief](ZINELY-1X-OWNER-DECISION-BRIEF.md) mirrors it without framing.
Every other item can wait, and the [last section](#decisions-that-can-wait) says why.

The count was seven in the audit. It changed for three reasons:
- **O7 (PR #70) left the list.** The owner already approved removing the dead Font control
  ([ROADMAP "In development"](../ROADMAP.md#in-development--built-or-being-tested-not-in-the-public-download)).
  What remains is acceptance work, not a decision ([below](#already-recorded--not-questions)).
- **Two questions joined:**
  - how far the tap-through fix reaches ([Q3](#q3-tap-through-hit-testing-how-far-it-reaches));
  - the shape of the next release, including the backup and restore honesty defects ([Q8](#q8-the-next-release-and-the-backuprestore-defects)).

A neutral version without the research's framing, for the sitting itself, is the
[owner decision brief](ZINELY-1X-OWNER-DECISION-BRIEF.md).

**How to read this.**
- 🟦 marks the research's **recommended framing**. It is not a decision.
- A decision exists only once you record it, as a dated line in the record the row links to, or as an ADR.
  The [OWNER-CHECKLIST](../OWNER-CHECKLIST.md) indexes those records.
- Implementation sessions read only recorded rulings, so an answer given in chat does not count until it is
  written down.

**What can be decided in one sitting today:** Q1, Q3, Q4, Q5 and Q8.
- **Q2** needs one printed page of Averia first.
- **Q6** needs the frame shortlist.
- **Q7** needs the print study.

Step 0 is complete (PR #75). None of the eight blocks plan steps 1, 2 or 3 ([sequence](ZINELY-1X-IMPLEMENTATION-PLAN.md#5-sequencing));
step 1b waits for Q8 and a spec, and step 3 follows PR #70's acceptance. Steps 1
and 3 still need you to approve their HTML amendments.

| # | Question | Gates | Decide |
|---|---|---|---|
| [Q1](#q1-developer-verification-o15) | Register for Android developer verification? | Every public APK after the cut-over | **Now** (calendar) |
| [Q2](#q2-typefaces-o12--o8) | Which typefaces may set text inside a zine? | D2 | After one printed Averia page |
| [Q3](#q3-tap-through-hit-testing-how-far-it-reaches) | Tap-through: all Art, holed pieces only, or not at all? | Plan step 4; D5 | Today |
| [Q4](#q4-typebar--reframe-specs-o10) | Are `v21-typebar` / `v21-reframe` frozen? | D2 design route | Today (a minute) |
| [Q5](#q5-fold-study-before-creative-work-o14) | Run the fold study before creative work? | D2 and D5 timing | Today |
| [Q6](#q6-frames-o9--stretch) | Which frames, drawn by whom, and do they keep their shape? | D5 | After a shortlist |
| [Q7](#q7-print-o13--d3-stage-2) | What does print guidance promise? | D3 | After the print study |
| [Q8](#q8-the-next-release-and-the-backuprestore-defects) | Approve a release before the v4 bump; schedule the backup/restore defects? | Plan steps 6–8; D1 follow-ups | Today |

---

## Q1. Developer verification (O15)

- **Decision:** register as a verified Android developer now, later, or not at all, and under which account.
- **Why it matters:**
  - Google will require side-loaded APKs to come from a verified developer, starting **30 Sep 2026** in
    Brazil, Indonesia, Singapore and Thailand, and **globally in 2027**.
  - Zinely ships only as a GitHub APK today, so this affects its only channel, not just Play.
- **What it gates:** installation of every public APK in affected regions after the cut-over. It gates no code.
- **Evidence available:**
  - ✅ [developer.android.com/developer-verification](https://developer.android.com/developer-verification);
  - [research summary item 4 and §23](../research/ZINELY-FUTURE-PRODUCT-RESEARCH.md#23-website-strategy);
  - the OWNER-CHECKLIST §3 row.
- **Unknown:**
  - ⚠️ Whether Google's timeline and requirements have changed since the research; re-read the primary page.
  - Which account, personal or organisation.
  - Whether a signing key or package name must be registered. That also touches **O6**, the Play signing key,
    which is irreversible ([below](#decisions-that-can-wait)).
- **🟦 Recommended framing:** an admin task with a date, not a product choice. Decide the account with O6 in
  view, so the same key story serves GitHub and Play.
- **Decide now?** **Yes.** It is the only item with a date inside the next week.

## Q2. Typefaces (O12 + O8)

- **Decision:** which faces may set text **inside a maker's zine**, and under which rule.
- **Why it matters:**
  - Every zine today is set in Inter, and the Font control is dead. Its removal is already approved; see
    [below](#already-recorded--not-questions).
  - The ZINE-DIRECTION names three voices: Averia (Hand), Fraunces (Book) and Inter (Plain).
  - V2-CONSTITUTION §III (`docs/design/V2-CONSTITUTION.md` ~:146-153) points both ways:
    - *"Averia carries headings, screen titles and **the maker's own short strings** … Fraunces carries
      long-form editorial: **zine body, captions**, pull quotes, guide prose."*
    - *"**No fourth UI typeface.**"*
    - Binding either way: *"the imperfect face never sets running text … A violation of that sentence is a
      violation of this constitution."*
- **What it gates:** all of D2: its ADR (which supersedes ADR-055's exclusion), its HTML amendment and its
  schema plan.
- **Sub-decisions, in order:**
  1. **Scope.** Does §III govern zine text, or only Zinely's interface? The clause names "zine body", so
     ruling "interface only" is a **§VI amendment**, not a clarification. After it, the running-text
     constraint no longer applies to zines.
  2. **If it governs zine text,** choose one:
     - **(a)** Hand for short text only. Define the unit (characters, lines or box size) and what happens
       when a maker types past it: block, warn or keep. Silently switching the face is not an option.
     - **(b)** Amend §III.
     - **(c)** Two voices. Say which two: Book + Plain (the old Brief 02) or Hand + Plain
       (`ZINE-DIRECTION.md` ~:974).
     - **(d)** A different imperfect face that covers Latin Extended-A. This would also touch the interface's
       Averia under §III.
  3. **Coverage.** Averia covers Latin only. ⚠️ An audit count finds about 115 of 127 Latin Extended-A letters
     missing; re-count from the font file. Those letters fall back to a device font silently.
     - Zinely has no localisation, and makers type in whatever language they write. Article 3 rules out
       measuring what they type. So this is decided **on principle**: is a voice acceptable if it can't set
       Polish, Czech, Turkish, Romanian or Hungarian?
  4. **Older builds.** A beta.5 reader only ever gets a newer zine by restoring a library backup, or by
     downgrading. Pick which failure they meet:
     - **No schema bump:** every zine opens, but text in a narrower voice renders in Inter and its last lines
       are clipped. ⚠️ This **overrides a ROADMAP rule**: *"Unknown-font fallback must not silently become
       layout loss"* ([ROADMAP](../ROADMAP.md#creative-tools-assessment-2026-09-12)). Choosing it means
       amending that rule on purpose.
     - **Ride the v4 bump:** the **whole backup** refuses to restore on the older build once it holds one v4
       zine (`ZineLibraryBackupStager.kt` ~:367-376). That happens with any bump, not only D2's.
     - **The release boundary matters.** A version number guards only the fields released with it. Once v4 has
       shipped (planned together with alt text, [plan §4 F1b](ZINELY-1X-IMPLEMENTATION-PLAN.md#4-foundations--only-what-the-repository-actually-needs)),
       "riding the bump" for a later D2 means **v5**, not v4. Otherwise v4 builds without voices clip D2 zines.
- **Evidence available:**
  - ✅ the clause text and the Averia/Fraunces subset lists;
  - ✅ the code: the editing surface draws Inter whatever the zine's font, and italic is faked while editing;
  - the audit's [D2 section](ZINELY-1X-READINESS-AUDIT.md#6-d2-audit--typefaces--voices).
- **Unknown:**
  - how long real makers' text actually is (tester zines you have permission to look at);
  - how Averia looks printed at 10–14 pt on a home inkjet;
  - ⚠️ re-measured widths and x-heights.
- **Note:** the plan says "a small set of named voices, not a font picker"; the ROADMAP's creative-tools note
  says "with a real picker". Your ruling settles which.
- **🟦 Recommended framing:**
  - Answer scope (1) first; it may dissolve the rest.
  - Look at one printed page of Averia at 10, 12 and 14 pt before ruling on 2–3.
  - For 4, the research leans to riding the bump: refusal is honest where clipping is silent, and it keeps
    the ROADMAP rule intact.
- **Decide now?** No. D2 is not scheduled before the wave-1 release, but it cannot start without this.

## Q3. Tap-through hit-testing: how far it reaches

- **Decision:** when a maker taps the empty part of an Art piece, should the tap reach what is beneath?
- **Why it matters:**
  - Today every element is hit by its bounding box (`HitTest.kt:21-36`).
  - Six holed pieces ship: `paper.window`, `paper.hole`, `shape.ring`, `fix.grommet`, `fix.corner` and
    `mark.registration`. Tapping inside any of them selects the piece, never the photo beneath, so the photo
    can't be selected or reframed without *Send backward*.
  - SUPPLIES-SPEC records that supplies "inherited" the box hit test for free. It is inherited behaviour, not a
    rule the frozen Bench spec states, so changing it is an **interaction change**, not only a bug fix.
- **Options:**
  - **(a) Holed pieces only.** Outline hit-testing for the six holed pieces; everything else keeps its box.
    It fixes the reported problem with the least behaviour change.
  - **(b) All Art.** Outline hit-testing for every decor piece.
    - This changes taps on all 32 supplies (halftone, crop marks, rule, perforation…).
    - Thin pieces get harder to grab, so it needs a touch tolerance ([Brief 05](BRIEF-05-MATERIALS-FRAMES.md)).
  - **(c) No change.** Keep the box, and rely on *Send backward* and TalkBack.
- **What it gates:** plan step 4 (the hit-test session), and therefore D5.
- **Evidence available:**
  - the audit's [D5 section](ZINELY-1X-READINESS-AUDIT.md#9-d5-audit--frames);
  - Brief 05's design;
  - SVG's precedent that hit-testing follows the painted interior
    ([SVG 2](https://svgwg.org/svg2-draft/interact.html)).
- **Unknown:** how makers actually tap thin pieces. The device Pass 2 in step 4 would show it.
- **🟦 Recommended framing:**
  - (a) is the smallest change that fixes the defect.
  - (b) is more consistent, but moves every piece.
  - Either way, a short `v21-bench.html` behaviour note comes first (DESIGN FREEZE).
- **Decide now?** Yes, if step 4 is to run in wave 1.

## Q4. Typebar / reframe specs (O10)

- **Decision:** are `docs/design/mockups/v21-typebar.html` and `v21-reframe.html` frozen?
  - Their headers say "PROPOSAL, NOT FROZEN".
  - ZINE-DIRECTION N2 treats them as frozen.
- **Why it matters:** the HTML-first rule decides the route.
  - If frozen, D2 must amend them and re-freeze.
  - If proposals, they must be finished and frozen before any Compose work.
- **What it gates:** D2's design route only.
- **Evidence available:** the two headers, ZINE-DIRECTION N2, and V21-SPEC.
- **Unknown:** your intent when N2 was written.
- **🟦 Recommended framing:** one line, recorded in V21-SPEC or the ZINE-DIRECTION, that makes the headers and
  N2 agree.
- **Decide now?** Yes. It takes a minute.

## Q5. Fold study before creative work (O14)

- **Decision:** run the fold-clarity study before any creative-tool direction starts, or explicitly allow D2 or
  D5 first.
- **Why it matters:**
  - The ROADMAP puts fold clarity first among "Exploring" items.
  - It says its creative slice (fonts, the Art pack, decorative frames) *"does not reorder the … fold-study …
    gates without owner approval"*.
  - A maker who can't fold the booklet never sees the typography.
- **What it gates:** when D2 and D5 may start. It does not gate D1, D3, D4, the hit test or the guards.
- **Evidence available:** the study design in [audit §4](ZINELY-1X-READINESS-AUDIT.md#4-wave-0-audit):
  - 6–8 first-time makers, about 30 minutes each, with a printed sheet;
  - a finding means the same failure at the same step for two or more people;
  - it costs people and paper, not code.
- **Unknown:** the result, which is the point of running it.
- **🟦 Recommended framing:**
  - Run it; it is cheap and runs alongside wave 1.
  - Its results get their own step in the plan: Proof fold-caption fixes, sharing one `v21-proof.html`
    amendment with D3 stage 1.
- **Decide now?** Deciding *to run it* costs nothing and removes the question.

## Q6. Frames (O9 + stretch)

- **Decision:**
  - which decorative frames ship;
  - who draws their outlines;
  - whether frames keep their proportions when resized.
- **Why it matters:**
  - Frames are where a hand-drawn surface meets exact mechanics.
  - Resizing is free-aspect today, so a window frame gets uneven borders and a ring becomes an ellipse.
- **What it gates:** D5, and the `v21-bench.html` set amendment it requires
  ([ADR-107](../DECISIONS.md#adr-107) R1a).
- **Evidence available:**
  - the audit's [D5 section](ZINELY-1X-READINESS-AUDIT.md#9-d5-audit--frames) and
    [Brief 05](BRIEF-05-MATERIALS-FRAMES.md);
  - ADR-107's backlog of about 19 researched supplies;
  - the stretch question is already open as [D-100](../design/V2-SPEC-DEFECTS.md), because SUPPLIES-SPEC
    §3.4.1's uniform-scale rule was never implemented. Answering Q6 answers D-100;
  - ⚠️ neither free stretch nor a uniform lock keeps a window frame's borders even on a 4:3 photo. Even
    borders would need nine-slice-style scaling, a new render concept.
- **Unknown:** which frames fit the house style, and whether anyone is available to draw them.
- **🟦 Recommended framing:**
  - Decide the stretch policy first: free stretch, a uniform lock per supply, or a lock only for the ring
    family. It applies to the six shipping holed pieces too.
  - Because no option keeps window borders even, favour frames that **tolerate** stretching: photo corners,
    a torn hole, taped edges. Pick three or four.
  - Keep to the house test of scissors, torn paper and photocopier. Avoid instant-photo borders: they
    imitate an object rather than cut one (ADR-107 R1), and V2-CONSTITUTION §IV names tilted "polaroid" frames
    as costume.
- **Decide now?** No. D5 is last in the sequence.

## Q7. Print (O13 + D3 stage 2)

- **Decision, after the physical print study:**
  - **(a)** Is stage-1 guidance enough? That is a fold-line check in Proof's Print step.
  - **(b)** Should a static test page ship as well?
  - **(c)** If some printers can't reach the inset: a per-device "reach profile", or one wider margin for
    everyone?
  - **(d)** What may the app say about scale?
- **Why it matters:**
  - A shrunk or clipped first print is the most common way a home zine fails.
  - The app can't print by itself (ADR-052), only advise, so its advice must be true on Android.
  - **The shipped copy already promises "100% · Actual size"** (`Copy.kt` ~:1289-1297). If the study finds
    print paths with no reachable 100 %, today's app is already overpromising.
- **What it gates:**
  - D3 stage 2;
  - the wording of stage 1;
  - a test page (b), which needs an ADR amending ADR-039, because ADR-039 deferred the on-sheet ruler;
  - any change to the 17 pt inset, which is its own session with an HTML amendment and an ADR amending
    ADR-012, which set the inset.
- **Evidence available:**
  - the audit's [D3 section](ZINELY-1X-READINESS-AUDIT.md#7-d3-audit--printer-test-page);
  - documented worst-case printer margins: Canon PIXMA on Letter is 6.4 mm, above the 6 mm inset;
  - ⚠️ weak reports that some Android print paths offer no scaling control.
- **Unknown:** everything the study measures:
  - the real scale of each print path;
  - whether a 100 % option exists;
  - real printer reach on three or more brands, on A4 and Letter.
- **🟦 Recommended framing:**
  - Answer only after the study.
  - The research leans to no reach profile, and one global margin if needed: a remembered per-printer
    profile is state that goes stale.
  - For (d), promise only what the study shows is reachable, and correct the shipped copy if it isn't.
- **Decide now?** No. The print study runs alongside wave 1.

## Q8. The next release, and the backup/restore defects

- **Decision:**
  - **(a) Release shape.** Approve the plan's shape: a **wave-1 release** (steps 0–4, plus step 5 if the print study has
    reported) on today's document
    format, **before** the v4 schema bump. The ROADMAP says no next build is scheduled; steps 6–8 and
    Brief 04 part B depend on this.
  - **(b) Honesty defects.** Schedule the defects the D1 audit found in what the app tells makers after a
    backup or restore ([audit §5](ZINELY-1X-READINESS-AUDIT.md#5-d1-audit--last-backed-up--changing-phones)).
    They ship today, and they sit under constitution Article 5 ("Honest, all the way down"):
    - "Restore cancelled." shown although the zines were added;
    - "Couldn't read that file" shown after a restore that actually succeeded;
    - a full disk reported as "This backup looks damaged";
    - an empty or partial file left behind after a failed backup. *Already scheduled:* it is in D1 part 1
      (Brief 01), since it lives in the transport layer; listed here only for completeness;
    - "Backup cancelled." when Cancel lands after a complete file was saved;
    - a poisoned local photo (same hash, bad bytes) reported as "Couldn't read that file", blaming the backup;
    - no janitor for staging residue or orphaned photos after a process death (engineering follow-up).
  - **(c) Skip-and-list.** Should a backup skip a zine it can't open, and say how many it left out? Today one
    unreadable zine blocks every backup, and the maker often can't find it: if it was unreadable when indexed,
    it has no shelf row. D1 part 1 only
    makes the failure honest. Skip-and-list changes what a backup contains, so it needs an ADR amending
    [ADR-110](../DECISIONS.md#adr-110).
- **Why it matters:**
  - (a) The v4 bump makes whole backups unrestorable on older builds, so a release on today's format first
    keeps the format change isolated.
  - (b) These defects mislead makers about their own work, which Article 5 treats as bugs rather than polish.
- **What it gates:** (a) plan steps 6–8; (b) a slot in the sequence, which today has none.
- **Evidence available:** plan [§5](ZINELY-1X-IMPLEMENTATION-PLAN.md#5-sequencing); the D1 audit's
  file:line evidence.
- **Unknown:** none technical. It is a scheduling call.
- **🟦 Recommended framing:**
  - (a) Yes.
  - (b) Treat the first three defects as one small restore-honesty session in wave 1, after D1 part 1. It
    touches the same sheet and the same error mapping.
- **Decide now?** Yes. It shapes wave 1.

---

## Already recorded — not questions

- **O7, PR #70 (remove the dead Font control):** the removal is **owner-approved** as a design judgment
  ([ROADMAP](../ROADMAP.md#in-development--built-or-being-tested-not-in-the-public-download)).
  - The PR stays draft pending **acceptance work**: a hands-on TalkBack check, and rendered HTML parity.
  - That work is for a session to do, not a decision to make.
  - D2 later re-adds a font control; its removal now does not reject font choice (ROADMAP).
  - ADR-115 lands with the PR.

## Decisions that can wait

These are still **owner** decisions. They are just not needed yet.

| # | Question | Why it can wait | Revisit when |
|---|---|---|---|
| O1 | Enable Android phone-to-phone transfer? | D1 part 1 is buildable under every option. Research: on Android 12+, `allowBackup="false"` does **not** stop device transfer; the `<device-transfer>` exclusions do. Google's "Copy apps & data" skips apps not installed from Play, so enabling transfer buys a side-loaded APK little ([D1 audit](ZINELY-1X-READINESS-AUDIT.md#5-d1-audit--last-backed-up--changing-phones)). No option includes a reminder: that is a guilt mechanic (constitution Article 4) | If Zinely ships on Play |
| O6 | Play signing key (**irreversible**) | No Play upload is imminent, but Google Play publication is the ROADMAP's only *Planned* item. Decide it with Q1 in view | Before the first Play upload, and alongside Q1 |
| O13 | Printer reach profile | Folded into Q7 | After the print study |
| O5 | Paid-pack clock | Nothing in 1.x depends on it | ~Aug–Sep 2027 |
| O2 | Starters (Tribunal KEEP vs DO NOT BUILD) | Gates only D8, which is unscheduled | If D8 is proposed |
| O3 | Ink lift | Gates only D6-(4), after an asset-model ADR | After that ADR |
| O4 | Tray / X2 (D-029) | Gates only the Clippings tray | If the tray is proposed |
| O11 | Page count (D-030) | Gates 16-page zines only | Not in 1.x |
| — | **Accessibility product calls in D4** — whether *Redo* is spoken as well as *Undo* (parity), and whether decorative Art is skipped in Read (what a blind reader gets) | Needed during D4 part A3 and part B respectively; a TalkBack listen informs them | Before those sessions |
| — | D4 wording: "Photo: <description>" or the description alone; replacing the deprecated announce API app-wide | Copy and scope choices | During D4 |
| — | Named-undo wording for *duplicate*, *reset framing* and restack direction | Copy choices; see [Brief 04](BRIEF-04-READING-ORDER-AND-ALT-TEXT.md) | During that session |
| — | F6 portability denylist: now (its own small session), or later | Optional guard; step 0 shipped without it (PR #75) ([plan §4](ZINELY-1X-IMPLEMENTATION-PLAN.md#4-foundations--only-what-the-repository-actually-needs)) | Any time; its own small session |
| — | Ratify "imperfect surface, perfect mechanics" (research proposal D13) as a product principle | It is already used as an evaluation lens; ratifying changes no work | Any time |

## Change log

| Date | Change |
|---|---|
| 2026-09-26 | Base moved to `eb75cf7` (Step 0 merged). F6 row: step 0 shipped without it. Steps-not-blocked line names 1b (Q8 + spec) and PR #70 before step 3. Questions unchanged. |
| 2026-09-25 | Created from readiness-audit §11/§13 (seven questions). |
| 2026-09-25 | After the product review: O7 moved to "already recorded" (ROADMAP records the approval). Q3 became the tap-through scope question and Q8 the release-shape and restore-honesty question. Q2 now flags the ROADMAP layout-loss rule and "§VI amendment". The O1 reminder was removed and O6 cross-linked with Q1. Q7 notes the shipped "Actual size" copy. |
