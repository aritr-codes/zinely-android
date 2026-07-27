# V2-CRITIQUE.md — a critique of the *current* Zinely experience

> **Status:** Phase 2 deliverable of the V2 UI/UX redesign. This is an evidence-based critique of the
> **current on-main** app, measured against the [Phase 1 research](V2-RESEARCH.md) and the house rule that
> [every screen answers the user's current question](../../CLAUDE.md). It is *analysis*, not a decision:
> the design decisions it motivates arrive in Phase 4+ and are independently reviewed then. It supersedes
> nothing; where it overlaps the device-tested [Beta UX Review](../BETA-UX-REVIEW.md) it says so and marks
> what has since changed.
>
> **Method.** Current state established from (a) a full code map of the live screens (nav shell, theming,
> and all six surfaces), (b) the [Experience Map](EXPERIENCE-MAP.md) and [Screen Inventory](SCREEN-INVENTORY.md),
> (c) the [Beta UX Review](../BETA-UX-REVIEW.md) (device-tested `0.9.0-beta.1`, 2026-07-21), reconciled
> against what has shipped since. Research principles are cited as [R§n](V2-RESEARCH.md).

---

## 0. The headline: **V2 is an elevation, not a teardown**

The single most important finding of this critique is a *correction* to the framing of the brief. The
brief says "the current application works, but the UI/UX can become significantly better… this is not a
reskin, this is a product-level UX redesign." That is true — but the current app is **not** a generic
Material shell waiting to be warmed up. It already carries most of the bones the research says a
calm/warm/paper-first tool needs:

- **The warm-paper palette already exists** — `paper #F4EFE6`, `desk #E7DECE`, a single coral action
  accent (`coralStrong`, AA 4.6:1), and a "the hand" set (teal/yellow/stamp) reserved for authorial ink.
  This is [R§3.1](V2-RESEARCH.md)/[R§3.4](V2-RESEARCH.md) *already done in V1*: warmth in the surfaces,
  crisp ink for text, one disciplined accent.
- **The serif+sans voice pairing already exists** — `voice` = **Fraunces** (a warm humanist serif),
  `shell` = **Inter** (a humanist sans). That is precisely the [R§3.7](V2-RESEARCH.md) editorial pairing,
  bundled offline. The research's "custom type over Roboto" lever is *half-pulled already*.
- **The structural discipline the research demands is present** — one reducer path drives touch, keyboard,
  and accessibility (parity by construction); four haptic verbs and no fifth; reduced-motion resolved once
  and threaded to motion *and* haptics; "honest omission over fake affordances" as a stated, repeated
  principle (no WIP covers, no dead menu items, no un-built back button shipped hollow).
- **The two protected peaks are built and good** — Read mode (the finished zine, swipeable) and the fold
  climax both exist and are strong (see §2).

So the V2 opportunity is **not** "make it warm" — it is **"finish making it warm and calm, systematically,
and close the specific gaps where the current app still answers the tool's question instead of the
user's."** The differentiation lever from [R§0.5](V2-RESEARCH.md) — warm neutrals + custom type +
restrained motion — is *already the house style*; V2's job is to complete it into a governed **token
system** and resolve a handful of real UX gaps, not to invent a new look.

**The one-sentence finding:** *Zinely is already a warm, principled, honest app whose remaining friction is
concentrated in a few places where structure leaks through the paper — the library that can't tell you
which zine is yours, the editing surface that still shows the page moving and the text in two places, and a
type/spacing/motion warmth that lives in components rather than in a system.* Those are the V2 targets.

Two caveats that keep this honest:
- **The Beta UX Review's biggest finding is already resolved.** *"You cannot see your zine"* → **Read mode
  now exists** (`ProofRead`, Proof Act 0), and "Preview" is now a coherent four-act Proof surface
  ([ADR-051](../DECISIONS.md#adr-051)). Do not re-raise it.
- **Do not re-litigate the accepted V1 corpus.** The registers, precedence order, square-artifact rule and
  type roles ([ADR-061…068](../DECISIONS.md)) are ratified. V2 elevates their *warmth and calm*; a genuine
  conflict gets escalated, never quietly overridden ([R§6 Q6](V2-RESEARCH.md)).

---

## 1. What is already right — the load-bearing strengths to protect

Recorded first, because a critique that only lists faults gets the wrong things "fixed"
([R§4.5](V2-RESEARCH.md), R§4.7 aesthetic-usability: beauty *masks* defects, so protect what works).

| Strength | Evidence | Why it must survive V2 |
|---|---|---|
| **The fold climax** (`ProofFold`, `FinishedBook`) | staged reveal — book becomes a book *before* words/exits arrive | The category's under-taught trust surface ([R§0.4](V2-RESEARCH.md)); Zinely already owns it. |
| **The honest print recipe** (`ProofPrint`) | four settings that ruin a home print, each with a reason; "Actual size — not Fit to page" | Exactly [R§2.8/§1.2](V2-RESEARCH.md) "do the math invisibly, warn in plain words"; better than InDesign's jargon. |
| **Read == preview == export** | one `SceneRenderer` path for canvas, strip, Read, PDF | The `preview == export` invariant is the whole trust story; never fork it. |
| **Imposition never editable** | panel order derived from the engine, never offered to rearrange | [R§1.2](V2-RESEARCH.md): the thing beginners most reliably get wrong is removed. Keep. |
| **Supply tray over a lone FAB** | four equal craft supplies, disabled states that read as disabled | [R§2.2](V2-RESEARCH.md): the Editor has many peer actions — a tray is correct, a FAB would harm. |
| **"Reframe" as the verb; no "crop"** | frame-language reads as safe (beta §D3, ✅) | Research-validated vocabulary; keep. |
| **Privacy copy placed at moments of doubt** | empty state + share sheet, not a settings page | [R§0.2](V2-RESEARCH.md): the invariant is an asset; surface it where doubt occurs. |
| **Undo-first, no confirm dialogs** | shelf delete = undoable snackbar; queued so a second never commits the first | Exactly [R§2.8](V2-RESEARCH.md) undo-first. Keep. |
| **Honest omission over fake affordances** | no WIP cover, no dead menu items, no hollow back button | The opposite of [R§4.5](V2-RESEARCH.md) "hidden affordances / fake controls." A cultural asset. |
| **Three-way library emptiness** | store-empty vs search-miss vs zero-by-pending-delete are distinct | [R§2.5](V2-RESEARCH.md): empty states that don't lie. Rare and correct. |
| **Nav shell is already lean** | single Activity, **no bottom bar/tabs**, 3 destinations (Home/Editor/Proof) | [R§2.1](V2-RESEARCH.md): the research's own recommendation (don't spend the nav budget) is *already the architecture*. |

---

## 2. Per-surface critique

Each surface: **the question it should answer → current state → keep → opportunity** (opportunities are for
Phase 3, not fixes-now), with the research principle and beta status noted.

### 2.1 Library / Shelf — *"which zine do I want?"*
**Current.** `desk`-grounded grid of tilted 3:4 cards; each cover is a **generated riso recipe derived from
the title** (archetype + ink band + fold), with format label + Fraunces title + edited label; a floating
coral "Start a zine" dock; long-press/⋯ → actions (open/rename/duplicate/undoable-delete); sort sheet
(Recent/Name/Oldest); distinct loading/empty/error/search-miss states.
**Keep.** The dock as the single primary ([R§2.2](V2-RESEARCH.md)); the undoable-delete; the three-way
emptiness; the calm uniform grid ([R§1.4](V2-RESEARCH.md) normalize-cover-height is *already* honored — all
covers are one 3:4 shape).
**Opportunity (the library's core failure — beta §3, still open).** *The card can't answer "which one is
mine?"* Covers are abstract, title-derived shapes; with one zine it's merely odd, with six it's unusable
([R§1.4](V2-RESEARCH.md): "the card must defeat memory by itself"; Kindle's *"I don't remember what it's
about"*). **This is now a genuinely harder problem than the beta thought:** the beta's fix — "cache a page-1
thumbnail on save" — is exactly the pipeline [ADR-069](../DECISIONS.md#adr-069) **deleted** (it rendered a
PNG per edit into a field nothing read). So V2 must answer "which one is mine?" a *different* way — a
**maker-chosen cover** (pick a page / a colour / an emoji as the cover), or a richer identity treatment —
without reviving a per-edit render pipeline or breaking determinism. **This is the library's single biggest
V2 question** and an owner-facing one (§4 Q-L). Secondary, cheaper: verify the beta's hierarchy note (drop
the wordmark row and the permanent "On this device" chip so the content leads; the *promise* stays at the
empty state + share sheet where it earns attention).

### 2.2 Editor / Bench — *"how do I change this page?"*
**Current.** Top "Preview ›" nav; `weight(1f)` paper canvas (pan pinned to zero, MVP) with page render +
gesture surface + resize handles; a horizontally-scrolling context bar on selection; supply tray + page
strip below; TypeBar as a BottomCenter overlay; inline text as a BottomCenter IME-padded surface; reframe
chrome swaps in; move/resize hint; save-failure + the new coverage notice ([ADR-070](../DECISIONS.md#adr-070))
+ "Saved ✨", with an explicit TopCenter precedence stack.
**Keep.** Supply tray; one-reducer parity across touch/keyboard/a11y; the disabled-supply treatment; the
precedence discipline; the coverage notice (just shipped, [R§2.8](V2-RESEARCH.md) trust).
**Opportunities.**
- **The page still "breathes" and can drift out of scale** (beta §D2, documented as a live known
  compromise: the paper backing and the content can be drawn at two scales around a soft-keyboard resize;
  and the fit recomputes against residual height so the page resizes when the toolbar changes height). *A
  calm surface must not move while you work* — this is the most visible violation of [R§2.9](V2-RESEARCH.md)
  (motion must have a job) and [R§4.7](V2-RESEARCH.md) (jank damages trust). **Fit to a stable container;
  make the paper join the viewport deferral.** Highest-value editor opportunity.
- **Text still reads as two objects** (beta §4, still open): the coral box lives on the canvas, the caret
  lives in a docked strip. [R§1.1](V2-RESEARCH.md) (Goodnotes) and beta research both say short zine text
  wants **in-place** editing. The V2 answer: on session open, pan/scale the canvas so the target box sits
  above the keyboard and draw the caret *in the box*. (The pan-pinned-to-zero MVP is what blocks this
  today.)
- **The best feature is still hard to find** (beta §4): typography lives behind an "Aa" disclosure in a
  *horizontally-scrolling* context bar — off-screen on a narrow phone. [R§2.7](V2-RESEARCH.md) visible
  affordances / [R§4.3](V2-RESEARCH.md) hidden-for-cleanliness trap. Put "Aa" first in the row and/or in the
  keyboard accessory during a text session.
- **Context bar scroll hides controls.** Up to eleven ≥48dp chips overflow and scroll — reachable but not
  *seen* ([R§2.3](V2-RESEARCH.md) progressive disclosure vs [R§4.3](V2-RESEARCH.md) hidden affordances). A
  Phase-5/8 layout question: group by frequency, disclose the rarer transforms.

### 2.3 Read — *"what did I make?"*
**Current.** `HorizontalPager` over pages in reading order, one lifted paper card per screen, neighbours
peeking (`contentPadding`); renders through the shared path (read == export); "page N of M" caption as a
polite live region; no printer furniture.
**Keep.** *Almost everything.* This is the beta's #1 win, and it matches [R§1.4/§7](V2-RESEARCH.md): a
paged model with spatial landmarks (the peek says "there's more this way" without a hint chip). The
Canvas-page-can't-be-read-aloud honesty offloaded to the caption is exactly right.
**Opportunity (small).** This is the "I made this" pride peak ([EXPERIENCE-MAP](EXPERIENCE-MAP.md) ★). It is
currently *correct but quiet.* [R§3.11](V2-RESEARCH.md) says spend delight at the arc's beats — a
gentle, reduced-motion-safe page-turn that *echoes paper* (not a theatrical 3D curl — [R§1.4](V2-RESEARCH.md)
warns the realistic flip distracts) would raise perceived quality here more than anywhere else in the app.

### 2.4 Print — *"how do I print it correctly?"*
**Current.** Four recipe rows (Scale/Orientation/Paper/Sides), each icon + label + value, warnings tinted;
paper "Change" chooser; Save PDF / Share; honest privacy line; busy disables both.
**Keep.** *All of it.* This is a research exemplar ([R§1.2](V2-RESEARCH.md): InDesign correctness without
its vocabulary; beta §7 "should not be touched"). The warn-tinting and the plain-language reasons are the
model, not the target.
**Opportunity (minor, cosmetic-systemic).** The warmth here is carried by ad-hoc token re-pinning like
everywhere else; when V2 systematizes tokens (§3.1) this screen benefits for free. No structural change.

### 2.5 Fold — *"how do I turn this into a booklet?"*
**Current.** Five frozen steps, one crease at a time, `FoldDiagram` schematics with a crease-in animation;
polite-live-region captions; staged `FinishedBook` climax (settle → shelf-line → words → exits);
reduced-motion jumps to the end; keyboard-drivable.
**Keep.** *All of it* — the app's teaching moment and signature climax (beta §7). The staged reveal is a
textbook [R§3.11](V2-RESEARCH.md) milestone-delight done with restraint.
**Opportunity (none structural).** Only the systematic-token/motion-token benefit of §3. Protect it.

### 2.6 Proof scaffold & the nav shell — *the journey's spine*
**Current.** One Proof surface, four internal acts (Read/Sheet/Print/Fold), a single reconfigured action
bar, act-aware back with a "you can always leave, nothing is lost" invariant ([ADR-051](../DECISIONS.md#adr-051)),
passive progress creases (not a tab switcher). Nav shell: single Activity, no bottom bar, three global
destinations, Proof shares the Editor's ViewModel.
**Keep.** The single-surface-with-acts model and the leave-safe invariant; the lean nav shell
([R§2.1](V2-RESEARCH.md) — the research's own recommendation is already the architecture).
**Opportunity (the one real IA question).** Three destinations is *lean*, but the transitions between the
five *conceptual* surfaces (Library, Editor, Read, Print, Fold) are currently linear/forward. [R§2.1 Q1](V2-RESEARCH.md)
asks whether Library/Read are global "browse homes" while Editor/Print/Fold are document-scoped modes —
which the current architecture already half-expresses (Proof groups Read/Print/Fold under one document).
The V2 IA question (Phase 5) is not "add a bottom bar" — it's whether the *return* paths and the
Library↔Read relationship want to be richer, without breaking the leave-safe spine.

---

## 3. Cross-cutting opportunities — the biggest V2 levers (ranked)

1. **Turn the warmth from components into a *system*.** Today warmth is real but *distributed* — components
   repeatedly re-pin `ZinelyTheme.typography.shell/voice` and desk/paper tokens to correct for a
   still-legacy Material scale (`bodyLarge` on `FontFamily.Default`, 16sp). That is warmth held together by
   hand. [R§3](V2-RESEARCH.md) (reference→tokens, roles-not-screens) and [R§4.4](V2-RESEARCH.md)
   (emphasized-type tokens) point to **completing the type/spacing/motion token migration** so the Material
   scale retires and no component re-pins a font again. This is the single highest-leverage, lowest-risk
   V2 move: it makes every screen calmer *at once* and is mostly invisible-until-summed.
2. **Systematize spacing on one 8pt scale.** [R§2.4/§3.9](V2-RESEARCH.md): the calm target is *primarily a
   spacing problem*. Audit for ad-hoc spacing; adopt one tokenized `space.*` scale; bias one step larger.
3. **Stop the page from moving** (Editor §2.2). A calm canvas that never breathes or drifts scale.
4. **Answer "which zine is mine?"** (Library §2.1) — the maker-chosen-cover direction, respecting the
   ADR-069 no-per-edit-render constraint.
5. **Make text one object, in place** (Editor §2.2) — unblocks on the pan-enable the MVP deferred.
6. **Elevate the two pride peaks with restrained motion** (Read page-turn §2.3; the fold climax is already
   there) — [R§3.10/§3.11](V2-RESEARCH.md), always reduced-motion-safe.
7. **Author a dark-mode warm-charcoal token set** ([R§3.5](V2-RESEARCH.md)) — re-derived, not inverted, so
   the paper metaphor survives at night. (Confirm whether dark mode currently exists; the color object
   ships a light transcription and legacy dark scheme.)
8. **Tokenize *voice* and *delight*** ([R§3.11](V2-RESEARCH.md)) — VOICE.md already governs words; add a
   single reusable `feedback` pattern (visual + optional haptic + copy) so delight is coherent, not
   sprinkled. The four-verb haptic system is the model to extend, not replace.

Note the ranking is deliberately **warmth-and-calm-system first (1–2), then the concrete UX gaps (3–5),
then delight (6–8)** — because per [R§0.1](V2-RESEARCH.md) the aesthetic *is* mostly the system, and per
[R§4.7](V2-RESEARCH.md) polish that hides an unfixed gap (a drifting page, an unidentifiable library) is
worse than no polish.

---

## 4. Open questions carried into Phase 3+

Flagged, not decided here. Genuine owner calls marked ⬥.

- **⬥ Q-L (Library identity).** How does a card answer "which one is mine?" without the deleted per-edit
  render pipeline? Candidates: maker-picks-a-cover (a page / colour / emoji), or a richer non-render
  identity. Highest-leverage library decision.
- **⬥ Q-IA (Phase 5).** Do Library/Read become explicit global "browse homes" with richer return paths, or
  does the current linear/leave-safe spine stay? Not "add a bottom bar."
- **Q-Editor.** Enable canvas pan (deferred in MVP) to unblock in-place text and true centring — scope and
  risk against the frozen bench golden set.
- **⬥ Q-Type (from [R§6 Q4](V2-RESEARCH.md)).** Fraunces is the current serif voice. Does V2 keep Fraunces
  or choose a different warm humanist serif as it completes the type-token migration? (Changing it is a
  goldened-surface change.)
- **Q-Dark.** Does V2 ship a first-class warm-charcoal dark mode, or defer? Affects the token architecture
  now even if the UI ships later.
- **Q-Motion.** Confirm the [R§4.4](V2-RESEARCH.md) stance: standard motion scheme by default, expressive
  spring reserved for the two peaks, all reduced-motion-gated.

These feed Phase 3 (opportunities), Phase 4 (principles), and Phase 5 (IA). None is decided in this
document; several will surface as owner decision packages before the HTML prototypes are drawn.

---

## Cross-references
[V2-RESEARCH.md](V2-RESEARCH.md) (Phase 1) · [BETA-UX-REVIEW.md](../BETA-UX-REVIEW.md) (device-tested prior
critique) · [EXPERIENCE-MAP.md](EXPERIENCE-MAP.md) · [SCREEN-INVENTORY.md](SCREEN-INVENTORY.md) ·
[DESIGN-LANGUAGE.md](DESIGN-LANGUAGE.md) · [ZINELY-DESIGN-SYSTEM.md](../ZINELY-DESIGN-SYSTEM.md) ·
[ADR-051 Proof surface](../DECISIONS.md#adr-051) · [ADR-069 thumbnail deletion](../DECISIONS.md#adr-069) ·
[ADR-070 coverage notice](../DECISIONS.md#adr-070).

*Compiled 2026-07-27. Analysis feeding later, independently-reviewed design decisions — not itself a
decision or a code change.*
