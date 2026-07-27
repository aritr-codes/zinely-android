# V2-LIBRARY-CRITIQUE-2.md — flagship-level review of the revised Library

> **Status:** Second pre-freeze design review of the Library
> ([mockups/v2-library.html](mockups/v2-library.html)), against the standard of a modern flagship creative
> app. Reviewed from first principles as a Senior Product Designer / UX Researcher; no cosmetic-only notes.
> **No redesign here** — this sets the pre-freeze action plan. Every recommendation carries a UX reason and
> the four judgements the owner asked for: **why it's a problem · severity · worth changing before V1 ·
> would it actually improve UX.** Research corroboration for the newest claims (metadata disclosure;
> distinctiveness; empty-state comprehension; action-sheet norms) is being folded in from two cited passes.

---

## 1. Overall assessment

**The Library has crossed from "competent" into "genuinely good," and it is now ~80% of the way to
flagship. It reads warm, calm, and coherent, and the object metaphor is the right thesis. What holds it at
80% is not colour and not layout — it is that the covers are still *flat digital blocks* rather than
*printed paper objects*, the metadata still makes the shelf read a little like a file manager, and one real
layout defect (the create dock overlapping the last row) undercuts the perceived quality the rest earns.**
The gap to "flagship" is now concentrated in **material fidelity** and **subtraction**, not in more design.

Answering the owner's blunt question (§8 of the brief) — *warm/handcrafted/calm/creative/premium/memorable,
or "an app with nice colours"?* — honestly: it is **warm, calm, creative, and on its way to premium**, but
**not yet handcrafted or memorable**. The single thing preventing distinctiveness: **the covers assert
"handmade paper" through shape (a fold line, a fore-edge) but never *deliver* it through material** — there
is no riso grain, no paper fibre, no ink texture, no depth or weight. A user's eye still reads "nicely
coloured rectangles," not "a shelf of little printed books." That is the crux, and it is fixable.

---

## 2. What has improved (vs. the first prototype)

Real, and worth banking:
- **Chrome removed** — no wordmark, no count, no sort, no "8 pages," no duplicated title. The covers now
  lead. This was the highest-leverage change and it landed.
- **The object metaphor is present** — clean alignment (the fake tilt is gone), a press state, a fold
  spine, a fore-edge, and shelves. The *intent* is right even where the *execution* is still soft.
- **Printed ink stamps** replace glossy emoji — on-brand, coherent, and they read as *stamped on paper*.
- **Discoverable actions** — the quiet ⋯ → action sheet fills the management gap without a toolbar.
- **The palette works in both rooms** — day and the warm-charcoal night are both legible and warm; the
  covers correctly stay their inks across themes (the artifact doesn't invert).

---

## 3. What should remain exactly as it is (and why)

Defended, because a review that only lists faults gets the wrong things "fixed":

1. **No persistent title / no wordmark at the top (Challenge 1).** **Keep it gone.** The covers establish
   context — a returning user knows this is their shelf the instant they see their covers. A persistent
   "Library"/"Zinely" header would be recognition-free chrome (the 12-app survey in the prior critique:
   0/12 brand the steady-state library). *Removing it entirely already makes the screen calmer, and it
   should stay removed.* This is correct and flagship-standard.
2. **A single matcha "Start a zine" primary.** One unambiguous create action is exactly right for a
   creation-first app; don't split it. (Its *placement* has a defect — §4 — but the weighting is correct.)
3. **Covers-as-identity (title + ink + stamp).** The core answer to "which zine is mine?" — keep the
   concept; deepen the material (§4).
4. **Undoable delete, no confirmation dialog.** Once wired, keep it. For a reversible action, undo-first
   beats an "Are you sure?" that trains dismissal (NN/g). Don't add a confirm dialog.
5. **Clean grid alignment (no tilt).** The decision to earn "object" through material rather than a CSS
   tilt was right. Keep the alignment; invest the "handmade" budget in texture, not rotation.
6. **Warm palette + day/night.** Locked and working. Don't reopen.

---

## 4. What still feels weak (with severity + judgement)

Ordered by impact on the flagship goal.

### W1 — The covers are flat, not *printed* — the distinctiveness gap · **Severity: HIGH · before V1: YES · improves UX: YES (perceived quality)**
**Why it's a problem.** The whole identity is *home-printed riso zines*, yet the covers have none of the
material qualities of that medium — no riso grain, no limited-ink texture, no paper fibre, no slight ink
misregistration, no depth or weight beyond a drop shadow. Per the aesthetic-usability effect, perceived
craft *is* perceived quality and buys user trust and tolerance; a flat block forfeits that. This is the
difference between "an app with nice colours" and "a shelf of objects I made."
**Judgement.** The *most* important thing to change before freeze — but scoped: a subtle riso-grain/paper
texture token and a touch more real depth/weight, **not** skeuomorphic kitsch (no torn edges, no coffee
stains). It's the highest perceived-quality-per-effort move available. Grounded in the prior cited research
([V2-RESEARCH §3.11–3.12](V2-RESEARCH.md)): perceived craft *is* perceived quality (the aesthetic-usability
effect — [NN/g](https://www.nngroup.com/articles/aesthetic-usability-effect/)); warmth is manufactured by
*fidelity to a physical metaphor* (Goodnotes' real paper/ink), and 2026's disciplined skeuomorphism revival
is "texture where it communicates, not 2010 fake leather" — texture belongs to the *material layer*
(surfaces), never the chrome, at barely-perceptible intensity. (The dedicated fresh pass on riso-material
vs. kitsch failed on an API error; this rests on that earlier cited corpus.)

### W2 — Metadata is always-on, so the shelf reads like a file manager · **Severity: MED-HIGH · before V1: YES · improves UX: YES**
**Why it's a problem.** Every cover carries a grey "A4 · 2 days ago" line that competes with the artwork
for attention (the owner's own observation, and correct). The cover already encodes identity; the extra
text adds visual noise and nudges the mental model from *browsing objects* toward *managing files*. HCI:
recognition (the cover) should carry browsing; details are recall-support the user wants *on demand*, not
always.
**Judgement.** **Adopt the owner's recommendation: hide almost all metadata until interaction.** Research
(pass 1) confirms this is the flagship default, not a preference: it's **recognition, not readout** —
Nielsen heuristic #6, *recognition rather than recall* ([NN/g](https://www.nngroup.com/articles/recognition-and-recall/)),
and a visible cover **is** the recognition cue, so a text line beside it adds load without adding
recognition. **Apple Photos ships titles OFF by default** and hides date/EXIF behind swipe-up/ⓘ
([Apple Community](https://discussions.apple.com/thread/7760387)); **Lightroom's** grid metadata is an
off-by-default two-finger toggle its own expert users call clutter
([Lightroom Killer Tips](https://lightroomkillertips.com/tip-how-to-turn-off-the-annoying-photo-info-overlay/));
**Pinterest** is image-first with text deferred to detail. Since a Zinely cover already *renders the
title*, "A4 · 2 days ago" is a pure properties-row — the "A4 · 2 days ago" line is **the single
highest-value cut**, and it's the exact "Lightroom-overlay-left-ON" clutter state. Move it to detail /
long-press / the action sheet. Strong yes before freeze — it directly serves "more collection, less file
manager."

### W3 — The create dock overlaps the last row's content · **Severity: HIGH (defect) · before V1: YES · improves UX: YES**
**Why it's a problem.** In the populated shelf the floating "Start a zine" pill and its gradient **cover the
bottom row's metadata** ("A4 · 5 days ago" is clipped). Content hidden behind a floating control is a
concrete usability defect and reads as unfinished — it quietly contradicts the polish everything else
earns. (Screenshot 2.)
**Judgement.** Fix before freeze. Reserve bottom scroll padding so the last row always clears the dock; or
reconsider the create affordance entirely (§7 discusses create-as-first-"fresh-sheet"-tile vs. floating).
Non-negotiable — a flagship app never hides content behind its own button.

### W4 — The "shelf" metaphor is asserted but barely lands · **Severity: MED · before V1: DECIDE · improves UX: YES if committed**
**Why it's a problem.** The ledges are so subtle they don't read as shelves; the covers look like cards on
a flat surface, not objects resting on a ledge. A metaphor that's *almost* there is worse than one clearly
committed or clearly absent — the eye senses something is off without naming it.
**Judgement.** **Commit or drop.** Either make the shelf real (a readable ledge with the cover casting a
contact shadow onto it, a sense of the object's weight and thickness) or drop shelves for a clean,
well-spaced grid and let *material* (W1) carry "object." Half-measures cost calm. Decide before freeze.

### W5 — The ⋯ affordance — refined by research · **Severity: MED · before V1: YES · improves UX: YES**
**Why it's a problem.** The per-card ⋯ is chrome the direction wants to shed, and its ~30px target is
below the 48dp minimum (ergonomics/accessibility).
**Judgement — corrected from my first read.** I initially said "just replace ⋯ with long-press." Research
(pass 1) tempers that: a *visible* affordance genuinely **aids** discoverability in a single-user calm app
where a user may never guess a hidden gesture — NN/g: *"don't hide essential, high-frequency actions behind
an extra tap,"* and the ⋯ itself has low information scent
([NN/g](https://www.nngroup.com/articles/contextual-menus-guidelines/)); Apple's HIG is blunt that a
context menu *"is hidden by default… you cannot rely on a user realizing a context menu exists"*
([Apple HIG](https://developer.apple.com/design/human-interface-guidelines/components/menus-and-actions/context-menus/)).
So the honest call is **not ⋯-vs-long-press** but: **add long-press → context menu as an accelerator, and
keep a visible, discoverable fallback (a selection mode and/or a quiet control) — never make actions
gesture-only** (Procreate does exactly this: swipe/long-press *plus* a selection mode). Concretely: adopt
long-press on the whole cover (fixes the target size and feels modern), and either keep a *quiet* ⋯ or add
a selection mode as the visible path. **The bigger chrome win is cutting the metadata line (W2), not the
⋯** — so this is "add a fast path + keep it discoverable," not "strip the visible affordance."

### W6 — Empty-state concept comprehension · **Severity: MED · before V1: YES · improves UX: YES**
**Why it's a problem.** The empty state is *welcoming* and correctly single-CTA, but a first-time user may
not know what "a zine" *is* — a folded 8-page mini-book from one sheet is a novel physical concept, and the
abstract booklet illustration + a text sentence may not build the mental model. If the core artifact isn't
understood, the CTA is a leap of faith.
**Judgement.** Improve comprehension before freeze — most likely by **showing the transformation** (flat
sheet → folded book) or a tiny example, so the user *gets* the magic before committing, without a tutorial
wall. Keep the single primary CTA; comprehension support is not a second competing action. Grounded in the
prior cited research ([V2-RESEARCH §2.5](V2-RESEARCH.md); [NN/g empty states](https://www.nngroup.com/articles/empty-state-interface-design/)):
the empty state *is* the onboarding, and teaching-by-showing beats a text description for a novel concept —
"the container is the onboarding." (Fresh pass on concept-comprehension failed on an API error; rests on
the earlier corpus.)

### W7 — Action-sheet content · **Severity: LOW-MED · before V1: PARTLY · improves UX: modestly**
**Why it's a problem / judgement (per item):**
- **"Open on the bench" is somewhat redundant** — tapping the cover already opens it. Mature context menus
  sometimes keep "Open" for discoverability, so this is defensible; but the sheet's real job is *management*.
  Low severity; keep or drop by convention (research on HIG norms folding in).
- **A likely-missing action: Share / Export.** A user may want to send or re-export a finished zine from the
  shelf without opening it. Worth considering before freeze (medium value).
- **Destructive separation:** Delete sits directly under Duplicate; a small divider/gap before the red
  Delete reduces mis-tap risk. Keep it undoable (no confirm). Low-med; cheap.

### W8 — Accessibility specifics · **Severity: MED · before V1: YES · improves UX: YES (for some users, always)**
- The grey meta line (`ink-faint` on cream) is borderline for contrast — *mooted if metadata is hidden per
  W2*, otherwise must clear AA.
- ⋯ target below 48dp — *mooted by W5's long-press*.
- Cover-title contrast on each maker ink needs CI verification (carried from the prior critique).
- Long-press context menus must expose a visible/again-reachable path for screen-reader and motor users
  (the action sheet stays; long-press is an *additional* fast path, never the only one).

---

## 5. Answers to the eight specific challenges (consolidated)

1. **Persistent title?** No — and there already isn't one inside the app. Removing it *did* make it calmer;
   the covers establish context. **Keep removed.** (§3.1)
2. **Always-on metadata?** No — **progressively disclose** it; let covers dominate more. (W2)
3. **"Start a zine" weighting/placement?** Weighting correct (one primary); **placement is defective**
   (overlaps content). Fix the overlap; consider create-as-a-fresh-sheet-tile as an alternative that never
   occludes and reads as content not chrome (§7). (W3)
4. **Empty state?** Welcoming and well-pitched, but **concept comprehension is at risk** — show the
   sheet→book transformation so a first-timer *understands a zine*, keeping the single CTA. (W6)
5. **Action sheet?** "Open" is arguably redundant; **Share/Export is likely missing**; **separate the
   destructive Delete**; keep delete undoable (no confirm). (W7)
6. **Spacing/rhythm?** Establish one 8pt vertical rhythm; **increase breathing room around covers** once
   metadata is hidden (W2 frees vertical space); fix the dock overlap so the rhythm doesn't break at the
   bottom. (W3)
7. **Interaction model / fewer taps / gestures?** **Long-press context menu** (W5) removes a tap-target and
   chrome; **drag-to-reorder** is a plausible expected gesture (deferred — order is Recent by default);
   avoid inventing gestures without visible fallbacks. Common task (open) is already one tap — good.
8. **Distinctiveness?** Warm/calm/creative: yes. Handcrafted/memorable/premium: **not yet** — the covers'
   lack of *print material* is what's preventing it. (W1, W4)

---

## 6. Nice-to-have (V2, not before freeze)
- **A signature delight moment** on the shelf — e.g. a gentle, reduced-motion-safe settle when a new cover
  joins the shelf (the "your book comes home" beat). Motion numbers deferred to the CI-14 baseline.
- **Drag-to-reorder** covers (only if users express a felt need; Recent-first serves most).
- **Cover editing from the library** ("Change cover" — ink/stamp) if not better placed in the editor.
- **Richer cover recipes** — more than one layout, so a large shelf doesn't look templated.

---

## 7. Prioritized action plan (before design freeze)

**P0 — defects & subtraction (do first):**
1. **Fix the dock overlap** (reserve bottom padding, or move to create-as-first-tile). *(W3)*
2. **Hide metadata until interaction** — covers dominate; reveal format/date on tap/long-press/in the
   sheet. *(W2)*
3. **Add long-press → context menu on the whole cover** (fixes the sub-48dp target, feels modern) **while
   keeping a visible/selection fallback** — actions must never be gesture-only. The metadata cut (P0.2), not
   the ⋯, is the real chrome win. *(W5, per research)*

**P1 — the distinctiveness investment (the flagship gap):**
4. **Give covers real print material** — a subtle riso-grain/paper-fibre texture token + honest depth/weight,
   stopping short of kitsch. *(W1)*
5. **Resolve the shelf metaphor** — commit (readable ledge + contact shadow + weight) or drop for a clean
   spaced grid; don't leave it half-landed. *(W4)*

**P2 — flows & content:**
6. **Empty state:** show the sheet→book transformation so "zine" is understood; keep the single CTA. *(W6)*
7. **Action sheet:** consider dropping redundant "Open," add **Share/Export**, and visually separate the
   destructive **Delete**. *(W7)*

**P3 — verify before freeze:**
8. AA contrast (cover titles on each ink; any surviving metadata); long-title truncation; screen-reader
   path for long-press actions; 8pt vertical rhythm audit; both device-verification passes.

---

## Cross-references
[mockups/v2-library.html](mockups/v2-library.html) · [V2-LIBRARY-CRITIQUE.md](V2-LIBRARY-CRITIQUE.md) (first
pass) · [V2-TOKENS.md](V2-TOKENS.md) · [V2-PRINCIPLES.md](V2-PRINCIPLES.md) · [V2-RESEARCH.md](V2-RESEARCH.md)
(§2.5 empty states, §2.7 gestures/selection, §2.8 undo, §3.11–3.12 delight/material, §4.7 aesthetic-usability).

*Compiled 2026-07-27. Flagship-level review feeding a revised Library before freeze — not a redesign, not a
ratified decision. Research: metadata-disclosure & context-menu findings from a fresh cited pass (Apple
Photos/Lightroom/Pinterest/Procreate + NN/g + Apple HIG); distinctiveness/empty-state/undo findings from the
earlier cited corpus ([V2-RESEARCH.md](V2-RESEARCH.md) §2.5, §2.8, §3.11–3.12, §4.7) — the dedicated fresh
pass for those failed on an API error and is not blocking, as the corpus already carries the citations.*
