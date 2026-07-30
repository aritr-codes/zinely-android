# Compose V2 Implementation — Handover

> **Paste-in brief for a fresh Compose implementation session with no prior context.** It summarises months of
> design work so a new session can begin immediately, faithfully, without reconstructing anything. Read this, then
> the five documents it points to, then open the frozen HTML for the screen you're building.

---

## 0. Where the work actually is (2026-07-30)

**Phase A — Foundation — is CLOSED. Its gate passed on 2026-07-30 by owner ruling.** Nine implementation
packages (A1–A9), nine ADRs ([ADR-071](DECISIONS.md#adr-071)…[ADR-079](DECISIONS.md#adr-079)), each
independently reviewed and approved, plus A10 (documentation, [ADR-080](DECISIONS.md#adr-080)). Do not
rebuild it. The full record of what was built, what was built
*differently* than this document plans, and why, is the
[Phase A completion record](COMPOSE-V2-ROADMAP.md#phase-a-completion-record-2026-07-29) — **read that
before §6 below**, because §6 describes the plan and the record describes the outcome.

**What exists now:** a V2 design foundation in `:core:ui` — colours, maker inks, typography, shape and
elevation, motion, the paper grain, 36 icons, an accessibility control seam and canvas node tree, and a
debug-only catalog verified pixel-by-pixel against [V2-TOKENS.md](design/V2-TOKENS.md). No product screen,
no route, nothing user-facing.

**Two things Phase A did not do, by design:** V2 tokens currently sit **beside** V1's rather than replacing
them, and `config/token-enrolment.txt` enrols zero packages. Convergence happens surface by surface across
Phases B–D, each package enrolling in the same commit that migrates it. This is **scheduled duplication**,
confirmed as the migration architecture by the D-016 ruling — not drift, and not a parallel design system.

**The gate, and how it closed.** Phase A's criterion *"everything routes through tokens"* requires editing
V1 product components, which Phase A forbids; it is therefore **re-seated to Phase D** by owner ruling
([**D-016**](design/V2-SPEC-DEFECTS.md#d-016--two-of-phase-as-acceptance-criteria-cannot-be-met-by-a-phase-forbidden-to-touch-product-surface),
2026-07-30). The companion criterion *"no duplicate design system"* is **met by confirmation** of the
migration architecture — [ADR-080](DECISIONS.md#adr-080), now `Accepted`. Two further rulings landed with
it: **D-002** fixes the cover-title contrast floor at **3.0:1** with no design change, and **D-006** deleted
the dead `--r:18px` token from the frozen Bench and Proof. **Nothing is awaiting an owner.**

**Next:** Phase B — Library. It has not started and begins on an explicit owner GO.

---

## 1. The product

**Zinely** is a **privacy-first, offline-first Android app for making small, printable zines** on your own phone.
Kotlin · Jetpack Compose · Material 3 · on-device PDF/image export. **No account, no cloud, no network, no
analytics.** You start from a blank sheet and end with a folded little book you can hold.

The feeling being built: **a quiet café where you make tiny books with your hands** — calm, warm, handmade, and
private. The measure of success (the "Handmade Test"): a user should describe Zinely as *"it feels like making
tiny books in a quiet café,"* not *"it has a nice UI." *

## 2. The design philosophy (one paragraph)

**The interface stays quiet; the creations carry the warmth.** Chrome is restrained (few colours, calm surfaces,
the page is always the hero); all colour, texture, and expressiveness live in the user's zines — paper, inks,
covers — not in the app. This split is the load-bearing architecture: the app can stay calm for years while every
user's shelf becomes more *theirs*. Everything is honest (what you preview is exactly what prints), physical
(paper/press/shelf metaphors, not files/dialogs), and collected (zines are objects on a shelf, not files in a
list). Full statement: **[V2-CONSTITUTION.md](design/V2-CONSTITUTION.md)** — read it first; it is the highest
authority and it outranks everything, including the code.

## 3. The frozen artefacts (the canonical spec)

The design phase is **over**. These are frozen and authoritative — reproduce them, don't reinterpret them:

| Surface | Frozen HTML (the spec) | Authoring intent |
|---|---|---|
| **Library** ("which zine?") | [`design/mockups/v2-library.html`](design/mockups/v2-library.html) | V2 design docs |
| **Bench / editor** ("how do I change this page?") | [`design/mockups/v2-bench.html`](design/mockups/v2-bench.html) | [V2-BENCH-PRINCIPLES.md](design/V2-BENCH-PRINCIPLES.md) · [V2-BENCH-IA-INTERACTION.md](design/V2-BENCH-IA-INTERACTION.md) · [V2-BENCH-REVIEW.md](design/V2-BENCH-REVIEW.md) §E |
| **Proof** ("how do I print it right?") | [`design/mockups/v2-proof.html`](design/mockups/v2-proof.html) | [V2-PROOF-IA-INTERACTION.md](design/V2-PROOF-IA-INTERACTION.md) Part E |
| **Identity** (colour/covers/materials) | [`design/mockups/v2-materials.html`](design/mockups/v2-materials.html) · [`v2-living-audit.html`](design/mockups/v2-living-audit.html) | [V2-IDENTITY.md](design/V2-IDENTITY.md) · [V2-IDENTITY-AUDIT.md](design/V2-IDENTITY-AUDIT.md) |
| **Tokens** (locked palette) | — | [V2-TOKENS.md](design/V2-TOKENS.md) |

**Palette (chrome):** warm `paper #F7F2E7` / `desk #ECE3D1`, `ink #2A251E`, **`matcha #5E6B2F`** (the one action
colour), `strawberry #E98F97` (sparing punctuation), `consequence #A6382A` (delete/error only). Dark = **warm
charcoal, re-derived not inverted.** Type = **Fraunces (voice) + Inter (work)**, permanent. **Maker inks** (on the
*artifact*, never chrome) = the frozen **Bench H4 10-ink set**: Matcha #7C8A3F · Forest #3E5E3A · Strawberry
#E27F89 · Brick #B0503F · Sunflower #E7B53C · Ochre #D19A3C · Aqua #57B0A9 · Cornflower #6E86C9 · Plum #8A5A9B ·
Ink #2A251E.

## 4. The methodology (how you work)

1. **HTML is canonical.** Match the frozen prototype's *result*, idiomatically in Compose (M3, hoisted state,
   `collectAsStateWithLifecycle`, stateless children) — you're reproducing the design, not porting `<div>`s.
2. **No redesign, no interpretation, no feature creep.** If the HTML is wrong, **fix the HTML first** (owner gate),
   then the code — never the reverse.
3. **Deviate only for** platform truth (real a11y/IME/back/haptics), a genuine HTML bug, or *more* accessibility —
   and log every deviation with its reason.
4. **Repository truth beats summaries.** Read the actual file/commit/test/HTML before relying on it.
5. **The Bench and Proof already exist in code** — a real engine you **preserve, not rewrite** (see §6).

Full guide: **[COMPOSE-IMPLEMENTATION-GUIDE.md](COMPOSE-IMPLEMENTATION-GUIDE.md)**. One-page opener you re-read
each session: **[COMPOSE-IMPLEMENTATION-RULES.md](COMPOSE-IMPLEMENTATION-RULES.md)**.

## 5. The review process

- **Per PR:** an **independent Review Agent** (never the implementer) validates actual repo state, classifies
  findings **Required Fix / Recommended Improvement / Observation**, and returns **GO / GO WITH FIXES / NO-GO**.
  The implementer reconciles every Required Fix or surfaces the disagreement explicitly.
- **Device verification is mandatory, two passes:** Pass 1 (Developer — is it built right? assert the *platform*
  a11y tree) and Pass 2 (First-time user — would a stranger understand it?). A feature is accepted only when both
  pass; if they disagree, the disagreement is the finding.
- **Per phase:** a review gate (parity screenshots + side-by-side + device passes + no feature creep).

## 6. The implementation phases

Sequence and gates: **[COMPOSE-V2-ROADMAP.md](COMPOSE-V2-ROADMAP.md)**. In brief:

- **A · Foundation** — **✅ CLOSED 2026-07-30 (gate passed; [D-016](design/V2-SPEC-DEFECTS.md#d-016--two-of-phase-as-acceptance-criteria-cannot-be-met-by-a-phase-forbidden-to-touch-product-surface) ruled)** — theme, tokens, typography, motion, elevation, icons,
  CompositionLocals, paper system, a11y infra. *No product screens.* The parenthetical this list used to carry —
  *"this foundation is the **same** migration as the V1 conformance token work — do it once, not twice"* — is the
  intent, but it is **not what Phase A could deliver**: converging the two systems means editing V1 components,
  which Phase A forbids. V2 landed additively; convergence is Phases B–D, package by package, and the
  token-routing requirement is now a **Phase D** exit criterion ([ADR-080](DECISIONS.md#adr-080),
  `Accepted`). Do not read this line as a statement about the code today.
- **B · Library** ◀ **next** — pixel parity to the frozen Library. The closest to a clean re-skin; sets the parity bar.
- **C · Bench** — pixel + interaction + animation + editing-behaviour parity, on top of the **existing** engine.
  No feature additions.
- **D · Proof** — pixel + print-flow + fold-guide + a11y parity, for the shipped single-sheet-8 stage.
- **E · Cross-product polish** — make the three feel like one product (motion, transitions, haptics, dark mode).
- **F · Reality validation** — physical devices; only now are tiny fidelity-serving adjustments allowed.

**The Bench/Proof are not greenfield — preserve these:** `CanvasReplayer` (one draw path), `ElementSemanticsLayer`
(canvas a11y), command-undo + `AutosaveCoordinator` + "Saved ✨", direct-manipulation resize handles (48dp), and
the anti-desync viewport defence. The one interaction that is **new-in-V2** (already frozen inside `v2-bench.html`, not a new decision here) is
**in-place text editing with a rigid whole-page pan** on IME insets (page moves as one body, returns pixel-identical) — **conditioned on a device Pass-2
pixel-identical proof; fall back to hardening the bottom-sheet editor if the proof fails.**

## 7. The non-negotiable rules

Constitutional invariants — breaking one is a NO-GO no matter how good the screen looks:

- **One engine, one draw path:** preview == export == read ([ADR-028](DECISIONS.md#adr-028)). No second render path.
- **No per-edit render** — recipes, not cached rasters ([ADR-069](DECISIONS.md#adr-069)).
- **The page never drifts/reflows/resizes while editing** — rigid pan, pixel-identical rest.
- **Never-silent failure + loss-safe back** ([ADR-051](DECISIONS.md#adr-051)).
- **Print honesty** — no fake "Print"; 100% actual size; Save PDF + Share ([ADR-052](DECISIONS.md#adr-052)).
- **READ-first** — the finished-book reveal is Read's, not the Bench's ([ADR-058](DECISIONS.md#adr-058)).
- **Chrome = matcha + strawberry + consequence only** — warmth comes from content, never new chrome colour.
- **Privacy** — no network/analytics/cloud; offline-first; optional asset search sends a keyword only, never content.
- **Accessibility is not optional** — platform-tree truth; every gesture has a named action twin + visible fallback;
  AA gated in CI.
- **Every screen answers its one user question**; a correct answer to the wrong question is a defect.
- **MVI for the Bench** ([ADR-005](DECISIONS.md#adr-005)); clean architecture; sealed `Result`; Hilt/KSP; `jvmToolchain(21)`.

## 8. Definition of done (per change)

Code + tests pass · docs updated in the same change (decisions → ADRs) · UI: pixel parity verified + both device
passes accepted · no new network/account/cloud dependency · privacy & offline invariants intact · reviewed by an
independent Review Agent.

---

## Start here

1. Read **[V2-CONSTITUTION.md](design/V2-CONSTITUTION.md)** (why) → **[COMPOSE-IMPLEMENTATION-GUIDE.md](COMPOSE-IMPLEMENTATION-GUIDE.md)** (how) → **[COMPOSE-V2-ROADMAP.md](COMPOSE-V2-ROADMAP.md)** (what, in order).
2. Read the **[Phase A completion record](COMPOSE-V2-ROADMAP.md#phase-a-completion-record-2026-07-29)** — what
   already exists, and the four things a new engineer gets wrong that are not visible in the code.
3. Keep **[COMPOSE-IMPLEMENTATION-RULES.md](COMPOSE-IMPLEMENTATION-RULES.md)** open as your checklist.
4. Begin **Phase B — Library** on an explicit owner GO. Phase A is closed; building any of it again is the
   mistake this section exists to prevent.
5. For any screen, open its **frozen HTML** first; it is the spec.

*The design is done. Your job is faithful execution. If something feels like it should change, it goes into the
frozen HTML first (owner gate) — not into the code.*

---

*Handover written 2026-07-28 by the Design Custodian at the close of the V2 Design Program. §0 and the Phase A
status added 2026-07-29 (package A10); updated 2026-07-30 at the **Phase A closeout**, when the D-002, D-006
and D-016 owner rulings were recorded and Phase A's gate passed.*
