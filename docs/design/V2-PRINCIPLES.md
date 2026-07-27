# V2-PRINCIPLES.md — opportunities, design principles & product identity

> **Status:** Phases 3–4 deliverable of the V2 redesign. Builds directly on the owner-ruled
> [V2-DIRECTION.md](V2-DIRECTION.md) (disciplined hybrid; **full palette re-derive to matcha/strawberry/
> cream**; fused with conformance), the [research](V2-RESEARCH.md), and the [critique](V2-CRITIQUE.md).
> Still upstream of any screen: this defines *what V2 is and the rules it obeys*, feeding Phase 5 (IA) →
> 6 (journeys) → 7 (wireframes) → 8 (concepts) → 9 (HTML) → 10 (freeze). The concrete decisions here become
> ADRs and are independently reviewed as they are made; nothing is a ratified decision yet.

---

## Part A — Phase 3: the opportunity set (decided)

The panel and owner rulings collapsed a long candidate list into one ordered programme. Ordered
**floor-before-ceiling** — because [R§4.7](V2-RESEARCH.md) is blunt that polish over an unfixed gap (a
drifting page, an unidentifiable library) *masks* the defect and costs more trust than the polish earns.

| # | Opportunity | Kind | Why it's here |
|---|---|---|---|
| **F1** | Complete the type/spacing/motion **token migration**; retire the legacy Material scale; one 8pt `space.*` scale | Foundation | Warmth becomes *systemic*, not per-component ([R§3](V2-RESEARCH.md)). Delivered as the conformance programme's design front-end (fused). |
| **F2** | Author the **dual-theme token architecture** (light + warm-charcoal) + one `surface.texture` grain token | Foundation | Makes the dark mode + paper material cheap later; done once. |
| **P1** | Re-derive the palette to **matcha / strawberry / cream** (OKLCH, contrast-gated), governed by a colour ADR | Identity | The owner's Q-A ruling — the signature identity move. |
| **B1** | **Stop the editor page** breathing / drifting scale around the keyboard | Floor (trust) | The last "the app is confused" moment; the #1 physicality violation ([V2-CRITIQUE §2.2](V2-CRITIQUE.md)). |
| **B2** | **One-object, in-place text** (needs the deferred canvas pan) | Floor (trust) | Closes the beta's still-open "two objects" finding; the single riskiest move (ADR-escalated as a rigid-body scene offset). |
| **C1** | **The Maker's Cover** — the library says *"this one is mine"* (maker-chosen; on-demand render, visible-cards-only, honouring [ADR-069](../DECISIONS.md#adr-069)) | Signature | Closes Q-L; raises the lowest-finish, most-visited surface. |
| **C2** | **The Warm Night Desk** — first-class warm-charcoal dark mode, re-derived not inverted | Signature | *Cozy after dark* is unclaimed in the category. |
| **C3** | **The Paper-Motion Signature** — one restrained paper settle/turn at the two peaks (Read + fold) | Signature | Delight where the arc peaks ([R§3.11](V2-RESEARCH.md)); reduced-motion-safe; numbers deferred to the CI-14 baseline. |
| **C4** | **The Crafty-Friend Ending** — one reusable `feedback` primitive + the display voice at each arc beat | Signature | Makes warmth coherent, not sprinkled; extends the four-verb haptic model. |

Explicitly **de-scoped from V2** (kept as future/roadmap, not this redesign): sticker/graphics packs,
templates, photo-spanning, search-at-scale — all real, none load-bearing for "make it feel beautiful, calm,
delightful." They ride later.

---

## Part B — Phase 4: the V2 design principles

Ten governing principles. Each is a *rule a later screen can be checked against*, not a mood word. They sit
**under** the ratified design-system constitution ([ZINELY-DESIGN-SYSTEM §1.1/§1.7](../ZINELY-DESIGN-SYSTEM.md));
where V2 needs to change a ratified rule (the palette), it does so by governed ADR, never by drift.

1. **Conservative in the tool, bold in the artifact.** The tool is precise so the artifact can be personal
   (design-system §1.1). Spend restraint on chrome/interaction; spend boldness on *their* zine, *their*
   cover, *their* night desk, and the emotional peaks. The one owner-sanctioned exception is the palette,
   which is a chrome-level bold move *because the owner chose it*.

2. **Warmth is a system, not a coat of paint.** Every colour, space, type, motion, texture, and voice value
   is a **two-tier semantic token** (primitive → role), authored in OKLCH, consumed by roles-not-screens,
   guarded by CI contrast checks and reduced-motion/transparency fallbacks. No component re-pins a font or
   a colour by hand again. A redesign that lives in components rots; one that lives in tokens compounds.

3. **The room is a warm café, not a dark IDE.** The dominant surface is warm cream *paper* on a warm *room*
   (light) or a warm charcoal room (dark) — never blue-black, never pure white, never true grey. Matcha is
   the primary ("your move"), strawberry the punctuation; the two brand hues + the separate consequence
   colour are the whole chrome palette. Softness lives in surfaces and decoration; **text-to-paper contrast
   stays crisp** (AA: body ≥4.5:1) — calm is not low-contrast-everywhere.

4. **Calm is mostly spacing.** The primary calm lever is the 8pt scale and generous negative space, biased
   one step larger than feels necessary; space *groups* (internal ≤ external), it doesn't merely pad. Reach
   for spacing before reaching for colour when a screen feels busy.

5. **Every screen answers the user's current question — and only that one.** Library → "which zine is
   mine?"; Editor → "how do I change this page?"; Read → "what did I make?"; Print → "how do I print it
   right?"; Fold → "how do I fold it?". A screen that answers a *good* question at the *wrong* moment reads
   as a malfunction (the beta "Preview" lesson). Fixing the *order* of questions beats adding features.

6. **The page is the fixed point.** The artifact never moves, resizes, or drifts scale unless the user
   moves it. Any scene translation (e.g. lifting the edited box above the keyboard) is a *transient
   rigid-body offset that returns exactly on exit* — escalated to an ADR (§1.7), never a page resize.

7. **Trust is the feature.** Local autosave with a quiet glanceable cue; undo-first with no confirm dialogs
   for reversible actions; honest, recoverable, in-place errors that preserve the user's work; never a
   claim of success the app doesn't have. "Your work is safe" is said at the moments of doubt, on-device,
   always true. (The [ADR-070](../DECISIONS.md#adr-070) coverage notice is this principle in miniature.)

8. **Honesty over fake affordances.** No control that looks live but isn't; no cover/thumbnail that lies
   about state; no menu item that does nothing; no term a beginner must look up. Do the print/imposition
   math invisibly and name things in plain human words. An honest omission beats a hollow feature.

9. **Delight is earned by direct manipulation and spent at the peaks.** Motion has a job, stays gentle
   (standard scheme by default), and echoes *paper* — never a theatrical flourish on the critical path.
   One signature paper-settle primitive, reserved for the two pride moments (finishing the read, folding
   the book). Everything reduced-motion-safe by construction: the static state is always correct.

10. **On-device, offline, deterministic — proudly.** No account, no cloud, no network, no analytics; every
    "smart"/"delightful" thing ships bundled and runs locally. `preview == export` and reproducible-vector-
    PDF are inviolable — any new type/texture/colour must have a matching render face and must not break
    them. The privacy invariant is a stated identity asset, not an apology.

---

## Part C — Phase 4: the product identity

### What Zinely *is*
A warm, private little **paper studio** for making a real thing you can hold — a printable, foldable
mini-zine — that lives entirely on your phone. Not a design tool, not a social app, not a cloud service: a
**quiet craft table** that happens to be digital.

### The emotional experience (the felt promise)
*"Sitting in a quiet café, making something with your hands."* Calm, warm, unhurried, forgiving, a little
playful — and, at two moments, quietly proud: when you first *see* your finished zine, and when it folds
into a book in your hands. The user should want to **stay** in the app, not just complete a task; the
overall feeling is **smooth, lightweight, almost invisible** — the tool recedes so the zine is the loud
object in the room.

### The visual language (direction, not screens)
- **Palette (owner-ruled full re-derive):** a warm-**cream** paper and room, **matcha** as the single
  primary ("your next move"), **strawberry** as sparing warm punctuation, warm ink for text, a separate
  reserved consequence colour. Muted, not desaturated; far-apart hues so the triad stays clean; a warm-
  charcoal counterpart room for night. Derived from the reference's *feeling* (temperature, muted ceiling,
  soft-surface/crisp-ink), never copied from the image.
- **Type:** the existing editorial pairing kept — **Fraunces** (warm humanist serif) as the *voice* at
  display/subhead and the arc-beat lines; **Inter** (humanist sans) as the *work* face for everything
  read-to-act. Restraint (a small register scale) reads as premium; the type budget goes to completing the
  token migration *under* the faces, not to swapping them.
- **Material:** one barely-perceptible, recognizable **"Zinely paper"** grain (tint + soft shadow, not a
  bitmap; one light source; a dark-mode variant on charcoal), so the sheet feels like the same real
  material every time — warmth you can feel, kitsch you can't see.
- **Motion & voice:** gentle standard motion + one signature paper-settle at the peaks; a warm, plain,
  second-person voice (never cute-to-childish, never a code) tokenized alongside the four haptic verbs.

### Interaction philosophy
Direct manipulation as the primary verb (touch the thing, move the thing); selection *is* the disclosure
mechanism (a calm canvas by default, verbs only for what's selected); a visible **supply tray** over a
lone FAB; visible, labelled affordances over clever hidden gestures; undo everywhere; **the container is
the onboarding** (a warm empty state and a partly-started page, never a tutorial wall).

### What Zinely deliberately is **not** (anti-identity)
Not enterprise, not "pro," not a feature buffet, not cloud/AI/social, not fashionable-for-its-own-sake, not
loud. It declines the most-hyped 2026 features on purpose — that refusal *is* part of the identity.

### The positioning claim (the unclaimed square Zinely takes)
The zine-tool category is sterile-utility on one side and frenetic-whimsy on the other; **nobody owns
"calm, warm, cozy."** Zinely's claim: *the warmest, calmest, most private way to turn a photo and a few
words into a little book you folded yourself.* Category ownership of **cozy**, delivered through calm — not
chaos.

---

## Cross-references
[V2-DIRECTION.md](V2-DIRECTION.md) (owner rulings) · [V2-RESEARCH.md](V2-RESEARCH.md) ·
[V2-CRITIQUE.md](V2-CRITIQUE.md) · [ZINELY-DESIGN-SYSTEM.md](../ZINELY-DESIGN-SYSTEM.md) ·
[VOICE.md](VOICE.md) · [DECISIONS.md](../DECISIONS.md) ADR-061…070.

*Compiled 2026-07-27. Principles + identity feeding IA/journeys/wireframes/HTML and later
independently-reviewed ADRs — not itself a ratified decision.*
