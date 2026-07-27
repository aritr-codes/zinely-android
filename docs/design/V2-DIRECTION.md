# V2-DIRECTION.md — the strategic direction decision (recommendation + open owner calls)

> **Status:** Pre-Phase-4 direction synthesis. Distils a four-lens brainstorm panel (steelman-elevation ·
> steelman-radical · hybrid-architect · adversarial-risk) grounded in [V2-RESEARCH.md](V2-RESEARCH.md) and
> [V2-CRITIQUE.md](V2-CRITIQUE.md). **This is a recommendation, not a ratified decision.** The two owner
> decisions in §4 are load-bearing and gate Phase 4 (principles) / Phase 5 (IA); the design decisions this
> motivates are recorded as ADRs and independently reviewed when made.

---

## 1. What the panel agreed on

Four independent lenses converged on the same shape:

- **A full radical reskin is not recommended as "a redesign."** It reopens ~6 owner ADRs that are *three
  days old* (ADR-061…068, 2026-07-24), invalidates the 61-golden Roborazzi safety net, and forces rework
  of most of the in-flight 99-item C0–C10 conformance programme — while re-opening the exact beta trust
  wounds (page-drift, "it lost my work," fake affordances) with **CI-68 (does the page hold size through
  the keyboard?) still unverified even on the current build.** It is only coherent as an *explicit,
  owner-sanctioned programme reset*, and nothing in the research justifies one (the research says
  calm/warm is an **unclaimed winning** position, not a failing one).
- **Pure elevation is the safe path — but the way the critique framed it has two flaws** the panel caught:
  1. **It may score the incumbent against a brief the critique itself softened** ("it's already mostly
     there") — the precise failure mode recorded in this project's own memory (*"the review that caught me
     scoring the incumbent against its own critique"*). Held honestly, the owner asked for *memorable*, and
     pure elevation is **structurally incapable of a distinctive identity**, because everything that would
     make it distinctive sits behind a rule elevation may not touch.
  2. **Its top two levers are not V2 at all — they are the conformance programme.** "Complete the
     type/spacing/motion token migration; one 8pt scale" ≡ conformance items CI-39/40/41/43/60/61/64/65/
     74/75 (C3→C7). Framing them as "V2" creates a **programme-level two-hubs collision**: two initiatives
     both owning the token migration, double-touching `ui/theme/` and the 14 editor surfaces. This is the
     Documentation-Rule failure ADR-061 fixed at the *document* level, re-appearing one layer up.
- **The genuine crux is the palette.** The shipped identity is **warm-cream paper on a charcoal desk with
  a coral accent** — *not* the **matcha-green / strawberry-pink / cream** world the owner's reference image
  points at. Matcha exists today only as `tapeTeal` and is *forbidden from the chrome* by the design
  system's own colour-job rule. So "the warm palette already exists" conflated *warm neutrals exist* with
  *the owner's emotional identity exists* — and it does not yet. **This is the one place the owner's own
  brief proves the palette should change**, and it is genuinely the owner's call.

---

## 2. The recommendation: a disciplined hybrid — **"conservative in the tool, bold in the artifact"**

The design system already contains the risk map, in its own §1.1: *"The tool is precise so that the
artifact can be personal."* That sentence *is* the strategy:

> **Preserve everything that makes the *tool* trustworthy-by-construction. Spend the entire boldness budget
> on making the *artifact* more theirs, and its emotional peaks more felt.**

Boldness aimed at the chrome-grammar fights ratified rulings and buys little memorability (a reshuffled
toolbar is not what anyone remembers). Boldness aimed at the artifact — *your* cover, *your* night desk,
the paper settling as your book finishes — is where the corpus does *not* govern, so it is both **lower
risk and higher reward.** This is how "bold" stays distinct from "reskin."

**Sequencing (floor before ceiling — polish that hides a drifting page is worse than no polish):**

- **Phase A — the invisible foundation (fused with conformance, one owner).** The token/spacing/motion
  migration and the 8pt scale. *This is delivered as the design front-end of the conformance programme's
  C3/C6/C7, not a parallel V2 track* — resolving the two-hubs collision. Also author the dual-theme
  (light + warm-charcoal) token architecture and one `surface.texture` grain token now, so later moves are
  cheap.
- **Phase B — raise the floor (the trust fixes the mandate must not skip).** Stop the editor page
  breathing/drifting (fit to a stable container; the paper joins the viewport deferral). Make text one
  object, edited in place (needs the deferred canvas pan — **the single riskiest move**, escalated to an
  ADR per §1.7 as a *rigid-body scene offset, not a page resize*).
- **Phase C — the signature moves (the boldness budget, artifact-side):**
  1. **The Maker's Cover** — the library learns to say *"this one is mine."* A maker-chosen cover
     (ink/emoji/"set a page as cover"), deterministic by default, on-demand render only for visible cards
     in memory — honouring [ADR-069](../DECISIONS.md#adr-069) (no per-edit disk pipeline). Closes the Q-L
     wound and raises the lowest-finish, most-visited surface.
  2. **The Warm Night Desk** — a first-class warm-charcoal dark mode, re-derived not inverted; *cozy after
     dark* is unclaimed in the category.
  3. **The Paper-Motion Signature** — one restrained "paper settle/turn" primitive reserved for the two
     peaks (Read page-turn echo + the fold reveal), reduced-motion-safe, numbers deferred to the CI-14
     device baseline.
  4. **The Crafty-Friend Ending** — one reusable `feedback` primitive (visual + haptic + copy) and the
     Fraunces display voice at each arc beat; makes warmth coherent, not sprinkled.

This spends the mandate on the 3–4 things a user tells a friend about, on the side of the line the ratified
corpus leaves free — which is exactly why it is bold *and* safe.

---

## 3. Guardrails (adversarial panel — do these regardless of direction)

1. **Adjudicate ownership of the token migration before designing anything.** One programme owns C3/C6/C7;
   V2 is its design front-end, not a second hub. (Owner call — §4 Q-B.)
2. **Lock the regression net before any pixel moves.** Finish C1: goldens for the 11 currently-uncovered
   editor composables (light+dark), the platform-`AccessibilityNodeInfo` harness (CI-26), and close the
   still-unverified page-resize check (CI-68). Without this, both the token work *and* any bold move are
   uncontrolled, and the ADR-059-class defects "no reader or test can see" ship again.
3. **Keep every goldened-surface change on the HTML-first rails** — spec updated and re-frozen first, then
   Compose, then pixel-parity, then both device-verification passes.

---

## 4. The load-bearing owner decisions (these gate Phase 4/5)

Two decisions are genuinely the owner's, and the panel proved they change what the next phases build.

### ⬥ Q-A — The palette (the identity crux)
Does V2 move the palette toward the **matcha / strawberry / cream** reference image, or keep the shipped
**coral-on-charcoal** identity? Three coherent options:
- **Keep incumbent** — warm-cream paper on charcoal desk, coral accent. Most trust-safe; spends boldness
  on covers/dark-mode/motion instead. **Cost:** does not match the reference the owner supplied.
- **Re-derive to the reference** — matcha primary + strawberry punctuation on a warm-cream *room*
  (replacing charcoal). The biggest memorability lever and the honest reading of the brief. **Cost:** a
  governed goldened-surface change — needs an ADR, touches the palette rules, re-runs the HTML re-freeze.
- **Middle (recommended lean)** — warm the *room* toward cream and admit matcha + strawberry as
  *sanctioned brand hues* (derived in OKLCH, contrast-gated), sequenced **after** the Phase-B trust fixes,
  as one governed ADR'd token change. Honours the reference without a free-for-all reskin.

### ⬥ Q-B — Corpus reopenability & programme ownership
"Are ADR-061…068 reopenable for V2 — and is the token migration V2 work, conformance work, or one fused
programme?" A clean answer here prevents the two-hubs collision and tells every later phase whether it may
touch a ratified rule (a *palette* change under Q-A, for instance, needs a governed reopening of the
colour rules). **Recommended:** fuse V2's foundation with the conformance token migration under one owner;
keep the corpus closed *except* for the specific, ADR-governed changes Q-A requires.

The remaining open questions from [V2-CRITIQUE §4](V2-CRITIQUE.md) (keep-Fraunces, dark-mode-now, IA,
motion) the panel answered with enough confidence to proceed autonomously: **keep Fraunces** (changing the
face is high-cost, ~zero memorability; spend the type budget completing the migration *under* it);
**ship dark mode** as Phase-C move 2; **keep the lean leave-safe nav spine** (enrich only the Library↔Read
return loop, no bottom bar); **motion** = standard default + one paper-settle primitive at the two peaks.

---

## 5. Cross-references
[V2-RESEARCH.md](V2-RESEARCH.md) · [V2-CRITIQUE.md](V2-CRITIQUE.md) · [ZINELY-DESIGN-SYSTEM.md](../ZINELY-DESIGN-SYSTEM.md)
(§1.1 tool/artifact, §1.7 escalation, §2.6 two-rooms) · [DECISIONS.md](../DECISIONS.md) ADR-061…070 ·
[V1-CONFORMANCE-INVENTORY.md](../V1-CONFORMANCE-INVENTORY.md) (C0–C10; the token-migration items).

*Compiled 2026-07-27 from a four-lens brainstorm panel. Recommendation feeding an owner decision and later
independently-reviewed ADRs — not itself a ratified decision.*
