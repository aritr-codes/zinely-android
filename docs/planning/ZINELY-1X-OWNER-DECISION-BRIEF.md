# Zinely 1.x — owner decision brief

Status: **APPROVED — the owner ruled Q1–Q8 on 2026-09-26.** This is the one-page summary. The ruling text, its
evidence, and what each ruling delegates live in the [decision gate](ZINELY-1X-DECISION-GATE.md), which is the
record implementation sessions quote. If the two ever disagree, the gate wins and both are corrected.
Base: `origin/main` @ `0aa7a7d`.

(Until 2026-09-26 this file was a neutral option sheet with no recommendation; the owner then asked for
concrete recommendations and approved the decision set below.)

---

## 1. What was approved

| # | Approved direction (2026-09-26) | Where it lands |
|---|---|---|
| **Q1** Distribution | Individual **Google Play Console** developer (already registered). GitHub now, Google Play later. Play Console is the verification and package-registration home for both; no separate Android Developer Console account. Keep the existing signing identity compatible with Play; Play App Signing is future release planning. Future launch plan includes the personal-account closed test (≥ 12 testers opted in for 14 continuous days). The address-verification issue is administrative and does not block GitHub | [OWNER-CHECKLIST §3](../OWNER-CHECKLIST.md#3-release--credentials) |
| **Q2** Typefaces | **Book = Fraunces, Plain = Inter**; Hand deferred; Averia stays the interface voice; **no schema bump**. Scope ruling: *text inside a zine may use a document voice; a document voice is not a UI typeface* under V2-CONSTITUTION §III as written. Older builds keep and restore the content and may fall back to Inter — not data loss; layout documented and tested | Plan step 8 · [Brief 02](BRIEF-02-THREE-VOICES.md) (prepared, rewrite pending) |
| **Q3** Hit testing | Outline hit testing for the **six holed pieces**; drawn region wins; empty region passes through; small touch tolerance; nothing beneath → the piece stays selectable. **Crop marks excluded.** TalkBack unchanged | Plan step 4 · [Brief 05](BRIEF-05-MATERIALS-FRAMES.md) (ADR draft + HTML spec) |
| **Q4** Typebar / Reframe | Correct the TypeBar issue, then **freeze TypeBar**; **freeze Reframe as shipped**; later changes need an amendment | Plan step Q4-F |
| **Q5** Fold study | **Run now**, combined with the print study; **no longer a gate** on D2 or D5; evidence for Proof's fold instructions; the owner may reprioritise if it finds a fundamental failure | [Study protocol](STUDY-PRINT-AND-FOLD-PROTOCOL.md) |
| **Q6** Frames | **Two** frames, hand-cut character, no nine-slice, resize decided per piece, after the hit test. **Names and visuals not yet chosen** | Plan step 9 · Brief 05 |
| **Q7** Print | Design **truthful, result-based** guidance now; **ship only after the physical print study**; "100 %" only where the print app offers a scale control; never claim Android keeps exact size until measured | Plan step 5 · [Brief 03](BRIEF-03-PRINTER-TEST-PAGE.md) (blocked on the study) |
| **Q8** Backup / wave 1 | **Wave 1 on v3 before v4**; restore-honesty fixes (step 1b); late "Backup cancelled." (step 1); **skip-and-list** in wave 1 — a poisoned or unreadable photo too (supplementary ruling, after the final planning audit). New rule: *a backup may be complete or explicitly partial, but never silently partial*, discoverable at restore time too | Plan steps 1, 1b · [Brief 01](BRIEF-01-VISIBLE-OWNERSHIP.md) (1b spec, ADR and product-law drafts, HTML spec) |

## 2. What is still yours

These are the only owner decisions left in 1.x. Everything else is delegated to reviewed implementation
sessions under the rulings above.

1. **Approve each drawn HTML amendment** before its Compose work ([§4](#4-html-amendments-that-need-owner-approval)).
2. **The two frames' names, visuals, set membership and order** (Q6); a landing-size override only if a frame
   needs one.
3. **Fraunces minimum print size**, after a printed page at 10/12/14 pt (Q2).
4. **After the print study:** whether the guidance ships as designed, a static test page, any inset change,
   any render change (Q7). **After the fold study:** reprioritise only if it finds a fundamental failure (Q5).
5. **Approve the wave-1 release** in its release session, and acknowledge one limit of the backup rule: a
   partial archive restored on a build older than step 1b restores without a partial notice (older builds ignore
   the new manifest field).
6. **Play launch planning** (not now): the launch date; Play App Signing / key migration (O6, irreversible);
   running the 12-tester, 14-day closed test; finishing the address verification.
7. **Deferrable as before:** O1–O5, O11, the D4 accessibility calls, F6
   ([gate](ZINELY-1X-DECISION-GATE.md#decisions-that-can-wait)); and the questions `v21-typebar.html` records
   as yours (how the panel closes, the "Coral" name) plus its Teal contrast failure — they stay open under the
   Q4-F freeze.

Physical work only you (or a helper) can do: the print study, the fold study, the printed Fraunces page, and the
two-phone restore pass.

## 3. Briefs

| Brief | State after 2026-09-26 |
|---|---|
| [01 Visible ownership](BRIEF-01-VISIBLE-OWNERSHIP.md) | **Rewritten.** Part 1 (+ late "Backup cancelled."), part 1b spec (restore honesty + skip-and-list), ADR-110 amendment draft, product-law draft, one `backup-restore.html` amendment spec |
| [02 Voices](BRIEF-02-THREE-VOICES.md) | **Prepared, not rewritten.** Rulings and the measured Fraunces assets recorded at the top; "Corrections pending" stays until the rewrite |
| [03 Printer test page](BRIEF-03-PRINTER-TEST-PAGE.md) | **Blocked** on the physical print study; rewritten after it reports |
| [04 Reading order and alt text](BRIEF-04-READING-ORDER-AND-ALT-TEXT.md) | **Updated** for the current dependency structure |
| [05 Materials: frames](BRIEF-05-MATERIALS-FRAMES.md) | **Updated.** Six-piece hit test with its ADR draft and `v21-bench.html` spec; two-frame scope; fold study not a gate |

## 4. HTML amendments that need owner approval

None has been made; the frozen HTML is untouched. Each is its own reviewed change, **before** any Compose work.

**Next**
- [ ] **`backup-restore.html`** — steps 1 and 1b in **one** amendment: last-backup line (incl. partial), title,
  "what this file holds", backup-failure wording, the three restore-honesty states, partial-backup success, a
  partial-archive notice on restore; 360 dp and 200 % text ([Brief 01](BRIEF-01-VISIBLE-OWNERSHIP.md)).
- [ ] **`v21-typebar.html` / `v21-reframe.html`** — the freeze (step Q4-F): TypeBar's recorded correction, then
  both headers flipped. Approved in principle by Q4; any TypeBar divergence beyond the recorded correction
  comes back to you.
- [ ] **`v21-bench.html` — undo snack** (step 3): named undo in physical words, a page-changing variant.
- [ ] **`v21-bench.html` — tap-through note** (step 4): the six-piece rule; the stale three-piece text corrected
  ([Brief 05](BRIEF-05-MATERIALS-FRAMES.md)).

**Later**
- [ ] `v21-proof.html` — Print step guidance (step 5), **after the print study**; may share one amendment with
  fold-caption fixes.
- [ ] `v21-bench.html` — Describe verb (step 7); `v21-proof.html` — Read semantics (step 7).
- [ ] `v21-typebar.html` — the Voice control (step 8), after Q4-F.
- [ ] `v21-bench.html` — the Art-set amendment with the two frames (step 9) — **your visual approval**.
- [ ] `backup-restore.html` — "Changing phones?" (D1 part 2), after the two-phone pass.
- [ ] `v21-bench.html` keep-clear band — only if the print study calls for an inset change.

**Step 2 (reading order) needs no HTML amendment**; its rule is recorded as an ADR inside the session.

## 5. Sequence (unchanged order)

> Step 0 ✅ → 2 (READY) · 1a → 1 → 1b → PR #70 settled → 3 → 4 → (5 if the print study has reported) →
> **wave-1 release on v3** → 6 v4 → 7 alt text → v4 release → 8 voices (after Q4-F; no longer v4-bound) → 9 frames

In parallel now: the print and fold study, the Q4-F freeze, and Play Console admin. The printed Fraunces page
follows once its procedure is written ([plan §11](ZINELY-1X-IMPLEMENTATION-PLAN.md#11-implementation-readiness-after-owner-decisions)).
Readiness per step: [plan §11](ZINELY-1X-IMPLEMENTATION-PLAN.md#11-implementation-readiness-after-owner-decisions).

## Change log

| Date | Change |
|---|---|
| 2026-09-26 | Final planning audit: the supplementary Q8 ruling (a poisoned or unreadable photo is skip-and-list) recorded and removed from §2; frames decision names set membership and order; the TypeBar file's own open questions listed as deferrable. |
| 2026-09-26 | **Owner approved Q1–Q8.** Rewritten from a neutral option sheet into the approved-decision summary: what was approved, what is still the owner's, brief states, HTML amendments, sequence. Corrected facts carried from the gate (Q1 date framing, registration = package + signing key, Averia 12 of 128, N2 is a to-do, the three named restore defects, poisoned-photo wording). Base `0aa7a7d`. |
| 2026-09-26 | Re-based onto `eb75cf7` (Step 0 merged, PR #75); §1 and §6 re-checked; the steps-not-blocked line names 1b and PR #70. No question changed, no ruling made. |
| 2026-09-25 | Created: a neutral decision sheet for Q1–Q8, with a Briefs 02/03 status, an HTML-amendment checklist and a post-beta.5 sequence check. No ruling made, no option recommended. |
