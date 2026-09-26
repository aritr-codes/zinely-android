# Zinely 1.x — decision gate

Status: **RULED — the owner decided Q1–Q8 on 2026-09-26.** This file is the **ruling record** the
[OWNER-CHECKLIST](../OWNER-CHECKLIST.md#15-product--design-authorship) rows link to: each question below carries
its dated ruling, what the ruling settles, what is delegated to implementation sessions, and what is still the
owner's. Prepared 2026-09-25 from the [readiness audit](ZINELY-1X-READINESS-AUDIT.md)
([§11](ZINELY-1X-READINESS-AUDIT.md#11-owner-decisions) groups all fifteen O-items); rulings recorded on
`origin/main` @ `0aa7a7d`. The one-page summary is the [owner decision brief](ZINELY-1X-OWNER-DECISION-BRIEF.md);
the resulting sequence and per-step readiness are in the [plan §5 and §11](ZINELY-1X-IMPLEMENTATION-PLAN.md#11-implementation-readiness-after-owner-decisions).

**How to read this.**
- **Ruling** lines are the owner's decisions, dated. Implementation sessions quote them verbatim in the
  [handoff template](IMPLEMENTATION-HANDOFF-TEMPLATE.md)'s owner-rulings table.
- **Delegated** means Claude's implementation/research sessions decide it under the ruling, with review.
- **Still the owner's** names what the ruling deliberately left open. A 🟦 recommendation is never a ruling.
- ADR text is **not** written here. Where a ruling needs an ADR, its draft lives in the brief that owns the
  work, and it lands in `DECISIONS.md` in that implementation session.

| # | Question | Ruling (2026-09-26) | Gates |
|---|---|---|---|
| [Q1](#q1-developer-verification-o15) | Developer verification and distribution | Individual **Play Console** developer; Play Console is the verification home for GitHub now and Play later | GitHub APKs from the 2027 global phase; Play publication |
| [Q2](#q2-typefaces-o12--o8) | Typefaces inside a zine | **Book = Fraunces, Plain = Inter**; Hand deferred; scope ruling; no schema bump | Step 8 (D2) |
| [Q3](#q3-tap-through-hit-testing-how-far-it-reaches) | Tap-through reach | **Six holed pieces**, outline hit test; crop marks excluded | Step 4; D5 |
| [Q4](#q4-typebar--reframe-specs-o10) | Typebar / Reframe status | **Freeze both** (TypeBar after its correction; Reframe as shipped) | Step Q4-F; D2's route |
| [Q5](#q5-fold-study-before-creative-work-o14) | Fold study order | **Run now**, with the print study; **not a gate** on D2/D5 | Proof fold-caption fixes |
| [Q6](#q6-frames-o9--stretch) | Frames | **Two** hand-cut frames, no nine-slice, per-piece resize; names/visuals await owner | Step 9 (D5) |
| [Q7](#q7-print-o13--d3-stage-2) | Print guidance | **Design now, ship after the print study**; "100 %" only when the print app offers scale | Step 5 (D3) |
| [Q8](#q8-the-next-release-and-the-backuprestore-defects) | Next release; backup/restore defects | **Wave 1 on v3 before v4**; 1b restore honesty + **skip-and-list** in wave 1 (a poisoned or unreadable photo too — supplementary ruling) | Steps 1, 1b, release, 6 |

---

## Q1. Developer verification (O15)

- **Question:** register for Android developer verification, how, and under which account?
- **Evidence (corrected 2026-09-26, ✅ [developer.android.com/developer-verification](https://developer.android.com/developer-verification) and its FAQ, updated 15 Jul 2026):**
  - The phase starting **30 Sep 2026** covers installs from named app stores (Google Play, Galaxy Store and
    other listed stores) in Brazil, Indonesia, Singapore and Thailand. The FAQ: apps users **side-load
    directly "won't" need verification yet**. So it does **not** gate Zinely's GitHub APKs today.
  - The **global phase is 2027** (month unpublished) and covers all apps on certified devices, including
    side-loaded ones. That is Zinely's real deadline for GitHub distribution.
  - Registration proves **package name + signing-key ownership** (the SHA-256 of the signing certificate,
    proven with an APK signed by that key). A package may carry several keys. A Play Console account handles
    verification for outside-Play apps too.
  - ADB installs are exempt; the "advanced flow" lets a determined user allow unverified installs.
- **Ruling (owner, 2026-09-26):**
  - The owner **is registered as an individual Google Play Console developer**.
  - **GitHub** remains the current distribution channel; **Google Play** is the planned future channel.
  - **No separate Android Developer Console account.** Play Console is the developer-verification and
    package-registration home for both Play and outside-Play distribution.
  - The **existing Zinely signing identity must remain compatible** with future Play distribution. Do not modify
    signing configuration.
  - **Play App Signing / key migration is a future release-planning task**, not something to change now.
  - The future launch plan includes the current personal-account rule: a new personal account needs a **closed
    test with at least 12 testers continuously opted in for 14 days** before production access.
  - The current **address-verification problem is administrative** and does not block GitHub distribution.
- **Delegated:** keeping the release checklist's verification line current; re-reading Google's page for the
  2027 date before each release.
- **Still the owner's:** the Play launch date; Play App Signing execution (O6, irreversible); running the
  closed test; completing the address verification.

## Q2. Typefaces (O12 + O8)

- **Question:** which faces may set text **inside a maker's zine**, and under which rule.
- **Evidence (✅ measured 2026-09-26 from the bundled font files):**
  - Every zine today is set in Inter: `DocumentFontRegistry.Bundled` registers one family, and unknown names
    fall back to Inter. The editing surface draws chrome Inter and fakes italic (`BenchEditingSurface.kt:213-224`).
  - Latin Extended-A coverage: **Averia 12 of 128**, Fraunces 126, Inter 127. Averia lacks ą ę ć ś ź ż, č ě ř
    ů, ğ ş İ, ő ű, ă ţ.
  - Width vs Inter on a pangram: Fraunces −2 %, Averia −8 %. x-height at 10 pt: Inter 1.93 mm, Fraunces 1.66 mm.
  - V2-CONSTITUTION §III (~:147-153) already assigns Fraunces "zine body, captions, pull quotes", and ends
    "No fourth **UI** typeface". Precedent for reading a clause as written: the 2026-08-10 scope ruling (~:159).
  - Bundled Fraunces = three **static 9 pt roman** chrome cuts (400/500/600) in `:core:ui`; no Bold, no Italic
    ([Brief 02 pre-rewrite record](BRIEF-02-THREE-VOICES.md)).
- **Ruling (owner, 2026-09-26):**
  - **Book = Fraunces. Plain = Inter. Hand = deferred.** Averia remains the interface voice for now.
  - **Scope ruling (dated; not a constitutional rewrite):** *text inside a zine may use a document voice; a
    document voice is not a UI typeface under the existing constitutional wording.*
  - **No schema bump for document voices.** Compatibility rule: older builds must continue to restore and
    preserve content; they may fall back to Inter for a voice they don't understand; **that is not data
    loss**; the layout implications are documented and tested honestly.
  - Caveat: inspect the exact bundled assets before Brief 02 is rewritten (done 2026-09-26; see Brief 02).
- **Delegated:** coverage handling for scripts Fraunces lacks; the four document-face instances and their
  measurement; the editing-surface change; D2's ADR (supersedes ADR-055's exclusion); the "named voices, not a
  picker" control shape within the frozen type bar.
- **Still the owner's:** the **minimum Fraunces print size**, after the physical printed page (10/12/14 pt);
  approving the type-bar amendment drawing.
- **Note:** for document voices this ruling qualifies the ROADMAP constraint "unknown-font fallback must not
  silently become layout loss"; the ROADMAP carries a dated note pointing here. D2 must still document and test
  the fallback layout honestly (re-wrap, possible overflow of a fixed box).

## Q3. Tap-through hit-testing: how far it reaches

- **Question:** when a maker taps the empty part of an Art piece, does the tap reach what is beneath?
- **Evidence:** every tap path uses one rotated-box test (`HitTest.kt:21-36`); vector outlines already exist per
  supply (`SupplyOutline.kt`); `v21-bench.html` binds taps to the element box and never shows a holed piece
  over a photo, so box hitting is inherited behaviour, not a design rule.
- **Ruling (owner, 2026-09-26):**
  - Outline hit testing for the **six genuinely holed / negative-space pieces**: `paper.window`, `paper.hole`,
    `shape.ring`, `fix.grommet`, `fix.corner`, `mark.registration`.
  - The **drawn region wins**; the **empty region passes through** to the object underneath; a **small touch
    tolerance**; if **nothing is underneath, the piece remains selectable**.
  - **Crop marks are not in the first scope.**
  - **TalkBack semantics unchanged** unless implementation evidence requires a separate accessibility change.
- **Delegated:** the ADR (draft in [Brief 05](BRIEF-05-MATERIALS-FRAMES.md), including the `core:editor`
  module seam), the tolerance value, containment maths and tests, and the `v21-bench.html` behaviour-note
  specification.
- **Still the owner's:** approving the drawn `v21-bench.html` behaviour note.

## Q4. Typebar / reframe specs (O10)

- **Question:** are `docs/design/mockups/v21-typebar.html` and `v21-reframe.html` frozen?
- **Evidence:** both headers say "PROPOSAL, NOT FROZEN"; ADR-102 §12.16–12.17 makes freezing the owner's call;
  both are shipped (`TypeBar.kt`, `ReframeControls.kt`). ZINE-DIRECTION N2 reads "Freeze …" with its ✅ in the
  Evidence column: a **to-do**, not a claim that they were frozen (corrected 2026-09-26; there was no
  conflict).
- **Ruling (owner, 2026-09-26):** correct the known TypeBar issue first; **freeze TypeBar; freeze Reframe as
  shipped**; future behavioural or design changes require an amendment.
- **Delegated:** the freeze session, [plan step Q4-F](ZINELY-1X-IMPLEMENTATION-PLAN.md#5-sequencing) (the
  `TypeBar.kt:600-603` correction, divergence check, header flips, N2 marked done).
- **Still the owner's:** any TypeBar divergence **beyond the recorded correction** comes back to the owner rather
  than being resolved silently ("as shipped" was ruled for Reframe only); and the questions `v21-typebar.html`
  itself records as the owner's (how the panel closes, the "Coral" name) plus its Teal contrast failure, which
  stay open under the frozen header (scope line in [plan step Q4-F](ZINELY-1X-IMPLEMENTATION-PLAN.md#5-sequencing)).

## Q5. Fold study before creative work (O14)

- **Question:** must the fold-clarity study report before D2 or D5 starts?
- **Evidence:** no first-time maker has been observed folding a Zinely sheet (reviews 2026-08-27, 2026-09-09);
  the captions come from desk critique; fold guidance shares no code or spec with document fonts or supplies.
  The ROADMAP requires owner approval to reorder this gate.
- **Ruling (owner, 2026-09-26):** **run the fold study now**; combine it with the print study where practical;
  **remove it as a hard dependency for D2 (typefaces) and D5 (frames)**; keep it as evidence for Proof fold
  instructions; **if it uncovers a fundamental product failure, the owner may reprioritise** on that evidence.
- **Delegated:** the [study protocol](STUDY-PRINT-AND-FOLD-PROTOCOL.md) (written 2026-09-26), the write-up, and
  turning findings into caption fixes.
- **Still the owner's:** running the sessions (people, paper); any reprioritisation after a fundamental failure.

## Q6. Frames (O9 + stretch)

- **Question:** which frames ship, who draws them, and do they keep their proportions when resized?
- **Evidence:** resize is free-aspect (`TransformMath.kt:77-78`); SUPPLIES-SPEC §3.4.1's uniform lock was never
  built ([D-100](../design/V2-SPEC-DEFECTS.md)) and its `mark.*` scope is stale (it now covers 10 pieces,
  including strips); even window borders would need nine-slice, a new render concept; ADR-107 R1 ("attach,
  point, tear or cut?"), R1a (the owner rules set, names and order), R3 (no composites); ADR-105 D-2 (§IV
  governs the studio, not what it is stocked with).
- **Ruling (owner, 2026-09-26):** start with **two** frames, not a catalogue; **hand-cut** visual character;
  **no nine-slice**; resize behaviour **decided per piece**; frames come **after** the hit-testing work.
  **Final names and visuals are not decided** — they require the owner's visual approval.
- **Delegated:** outline drawing under R1a with attestation; the per-piece resize list closing D-100
  ([Brief 05](BRIEF-05-MATERIALS-FRAMES.md), 🟦 list); landing at the family default. A per-supply landing
  override is an owner ruling on SUPPLIES-SPEC §5.2, not delegated.
- **Still the owner's:** the two frames' **names, visuals, set membership and order**, via the `v21-bench.html`
  Art-set amendment; and a per-supply landing override, only if a frame needs one.

## Q7. Print (O13 + D3 stage 2)

- **Question:** what may print guidance promise, and when does it ship?
- **Evidence:** shipped copy promises "100% · Actual size — not 'Fit to page'" (`Copy.kt` ~:1289-1297) and
  "Print it at 100%" (~:1178), yet ADR-052 records that the system dialog exposes no scale control. 🟨 AOSP's
  default print service appears to print at exact size only when the printer's unprintable margin is blank;
  Zinely's guides reach the edge (`SheetGuides.kt`), so the default path may shrink the sheet — unmeasured.
  Among documented printers, only Canon PIXMA on Letter (6.4 mm) exceeds the 6.0 mm inset.
- **Ruling (owner, 2026-09-26):** design **truthful, result-based** print guidance now; **do not ship it until
  the physical / default-path print study**; keep any "100 %" advice **conditional** on the print app actually
  exposing a scale control; keep Landscape / Single-sided guidance where already verified; **never claim Android
  preserves exact size** until the study establishes it. The study tests the real Zinely print path and
  establishes: actual scaling; reachable scale controls; printable margins; whether one global inset works;
  whether a render change is required.
- **Delegated:** the guidance copy and the `v21-proof.html` amendment specification; the
  [study protocol](STUDY-PRINT-AND-FOLD-PROTOCOL.md); after the study, the recommendation on inset vs render
  change.
- **Still the owner's (after the study):** approving the Print-step amendment; a static test page (ADR-039);
  any inset change (ADR-012); any render change.

## Q8. The next release, and the backup/restore defects

- **Evidence (✅ code paths, [audit §5](ZINELY-1X-READINESS-AUDIT.md#5-d1-audit--last-backed-up--changing-phones)):**
  - Shipped messages that contradict what happened (constitution Article 5):
    1. a **full disk during restore** reported as "This backup looks damaged"
       (`ZineLibraryBackupStager.kt:127-133`: any `IOException` → malformed archive);
    2. **"Restore cancelled."** although the zines were committed (cancel during the non-cancellable commit,
       `RoomProjectRepository.kt:361-391`, `HomeViewModel.kt:448-460`);
    3. **"Couldn't read that file"** after a restore that committed (post-commit reconcile failure,
       `RoomProjectRepository.kt:366-389`);
    4. **"Backup cancelled."** when Cancel lands after a complete file was saved (`LibrarySafTransport.kt:124-130` and its return path);
    5. a **poisoned local photo** (same hash, bad bytes): on **restore** it shows "Couldn't read that file";
       on **backup**, a missing or unreadable local asset shows "This backup looks damaged". Either way the
       backup file is blamed for a local problem;
    6. an empty or partial file left after a failed backup (already in D1 part 1);
    7. no janitor for staging residue after a process death (engineering follow-up).
  - **One unreadable zine blocks every backup, permanently** (`RoomProjectRepository.kt:419-485`); if it has no
    shelf row, the maker has no remedy short of a code change.
  - ADR-110 paraphrases V1 product law as "all zines in one user-owned file" and specifies fail-closed. The
    law itself is the "(one file, user destinations only)" row at [`zinely-v1.md:68`](../zinely-v1.md) (with
    `:55`); `zinely-v1.md` outranks ADRs, so its amendment lands with the ADR.
  - The plan forbids any release between steps 6 and 7 while v4 is held.
- **Ruling (owner, 2026-09-26):**
  - **(a)** Cut a **wave-1 release on v3 before the v4 schema work**.
  - **(b)** Include the **restore-honesty fixes** — defects 1, 2 and 3 above — in wave 1 (step 1b). Include the
    **late "Backup cancelled." correction** (defect 4) in **step 1**. Defect 5's **restore** half (the
    poisoned local photo met during restore) stays out of wave 1; its **backup** half is covered by the
    supplementary ruling below.
  - **(c)** Include **skip-and-list for unreadable zines** in wave 1.
  - **New product rule:** *"A backup may be complete or explicitly partial, but it must never be silently
    partial."* The partial state must stay discoverable beyond the immediate backup screen; investigate a
    manifest/metadata representation so a later restore can also tell the maker the backup was partial.
- **Supplementary ruling (owner, 2026-09-26, after the final planning audit):** *a single poisoned or unreadable
  photo MUST be handled by skip-and-list rather than invalidating the whole backup.* Its zines are left out and
  named, with an honest photo reason, in the same `omitted` record (`packageVersion` stays 2). Spec:
  [Brief 01, "A photo that fails its check"](BRIEF-01-VISIBLE-OWNERSHIP.md#a-photo-that-fails-its-check).
- **Delegated:** the step 1b written spec, the ADR-110 amendment draft, the cited V1 product-law amendment
  draft and the `backup-restore.html` amendment specification — all in
  [Brief 01](BRIEF-01-VISIBLE-OWNERSHIP.md); the manifest representation (recommended there); how the photo
  ruling is implemented (pre-hash, and a rebuild when the writer's backstop trips).
- **Still the owner's:** approving the drawn `backup-restore.html` amendment; approving the wave-1 release;
  **acknowledging one limit of the new rule:** a partial archive restored on a build older than step 1b
  (beta.4-r3, beta.5, wave-1 builds before 1b) restores without a partial notice, because older builds ignore
  the new manifest field ([Brief 01](BRIEF-01-VISIBLE-OWNERSHIP.md) ADR draft). The release notes say so.

---

## Already recorded — not questions

- **O7, PR #70 (remove the dead Font control):** the removal is **owner-approved** as a design judgment
  ([ROADMAP](../ROADMAP.md#in-development--built-or-being-tested-not-in-the-public-download)).
  - The PR stays draft pending **acceptance work**: a rebase, a hands-on TalkBack check, and rendered HTML
    parity. It remains a separate prerequisite before steps 3 and 4 (owner, 2026-09-26), and before step 8 (the
    planning review's sequencing).
  - D2 later re-adds a voice control; its removal now does not reject font choice.
  - ADR-115 lands with the PR.

## Decisions that can wait

These are still **owner** decisions. They are just not needed yet.

| # | Question | Why it can wait | Revisit when |
|---|---|---|---|
| O1 | Enable Android phone-to-phone transfer? | D1 part 1 is buildable under every option. On Android 12+, `allowBackup="false"` does **not** stop device transfer; the `<device-transfer>` exclusions do. Google's "Copy apps & data" skips apps not installed from Play ([D1 audit](ZINELY-1X-READINESS-AUDIT.md#5-d1-audit--last-backed-up--changing-phones)). No option includes a reminder (constitution Article 4) | When Zinely ships on Play |
| O6 | Play App Signing / key migration (**irreversible**) | A future release-planning task (Q1). Constraint already set: the existing signing identity stays compatible with Play, so GitHub users can update in place | Before the first Play upload |
| — | Play launch date and the 12-tester, 14-day closed test | Future release planning (Q1) | When a Play launch is planned |
| O5 | Paid-pack clock | Nothing in 1.x depends on it | ~Aug–Sep 2027 |
| O2 | Starters (Tribunal KEEP vs DO NOT BUILD) | Gates only D8, which is unscheduled | If D8 is proposed |
| O3 | Ink lift | Gates only D6-(4), after an asset-model ADR | After that ADR |
| O4 | Tray / X2 (D-029) | Gates only the Clippings tray | If the tray is proposed |
| O11 | Page count (D-030) | Gates 16-page zines only | Not in 1.x |
| — | **Accessibility product calls in D4** — whether *Redo* is spoken as well as *Undo*, and whether decorative Art is skipped in Read | Needed during D4 part A3 and part B; a TalkBack listen informs them | Before those sessions |
| — | D4 wording: "Photo: <description>" or the description alone; replacing the deprecated announce API app-wide | Copy and scope choices | During D4 |
| — | Named-undo wording for *duplicate*, *reset framing* and restack direction | Copy choices; see [Brief 04](BRIEF-04-READING-ORDER-AND-ALT-TEXT.md) | During that session |
| — | F6 portability denylist: now (its own small session), or later | Optional guard; step 0 shipped without it (PR #75) ([plan §4](ZINELY-1X-IMPLEMENTATION-PLAN.md#4-foundations--only-what-the-repository-actually-needs)) | Any time |
| — | Ratify "imperfect surface, perfect mechanics" (research proposal D13) as a product principle | Already used as a lens; ratifying changes no work | Any time |

## Change log

| Date | Change |
|---|---|
| 2026-09-26 | **Supplementary Q8 ruling recorded** (after the final planning audit): a single poisoned or unreadable photo is skip-and-list, never a whole-backup failure. Replaces the 🟦 owner-to-confirm bullet; defect 5's restore half stays out. |
| 2026-09-26 | **Owner rulings Q1–Q8 recorded**; the file becomes the ruling record. Corrections: Q1's 30 Sep 2026 framing (named stores in four countries, not GitHub APKs; global phase 2027); registration = package name + signing-key ownership; Averia measured at 12 of 128 Latin Extended-A letters; the N2 "conflict" was a misreading (N2 is a to-do); Q8's defect list now names the three restore-honesty defects instead of "the first three", and the poisoned-photo wording distinguishes restore ("Couldn't read that file") from backup ("This backup looks damaged"). Base `0aa7a7d`. |
| 2026-09-26 | Base moved to `eb75cf7` (Step 0 merged). F6 row: step 0 shipped without it. Steps-not-blocked line names 1b (Q8 + spec) and PR #70 before step 3. Questions unchanged. |
| 2026-09-25 | Created from readiness-audit §11/§13 (seven questions). |
| 2026-09-25 | After the product review: O7 moved to "already recorded" (ROADMAP records the approval). Q3 became the tap-through scope question and Q8 the release-shape and restore-honesty question. Q2 now flags the ROADMAP layout-loss rule and "§VI amendment". The O1 reminder was removed and O6 cross-linked with Q1. Q7 notes the shipped "Actual size" copy. |
