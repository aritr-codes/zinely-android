# Zinely — Design Rules

> **The non-negotiable checklist every screen must pass before it ships.** A short, practical
> gate derived from the design references. If a screen fails any rule, it is not done — fix the
> screen, not the rule. A companion design reference under the canonical doc system in
> [CLAUDE.md](../../CLAUDE.md), not a parallel source of truth. Rationale lives in
> [DESIGN-LANGUAGE.md](DESIGN-LANGUAGE.md); words in [VOICE.md](VOICE.md); the arc in
> [EXPERIENCE-MAP.md](EXPERIENCE-MAP.md). Status: design reference · 2026-06-28.

These rules encode the one goal — *"I want to make something cute,"* not *"I need to learn this
editor"* ([ADR-008](../DECISIONS.md#adr-008)). They are intentionally absolute so reviews are
quick: a rule is met or it isn't.

---

## The 12 rules

1. **Every primary action is visible.** No essential action lives only behind a gesture, a long-
   press, or a hidden menu. Gestures are a *shortcut*, never the *only* path (WCAG 2.5.1/2.5.7).
2. **Every screen answers "What can I do next?"** There is always one obvious, prominent next
   step. Look at any screen cold and the next move is unmistakable.
3. **One primary action per screen.** No two controls compete for "most important." Secondary
   actions are visibly quieter.
4. **Every blank state invites creation.** Emptiness is an invitation with a clear first action,
   never a void, never "No items" (see [VOICE empty states](VOICE.md#empty-states)).
5. **Copy is warm and human.** Plain, second-person, encouraging; never system-error grammar,
   never jargon. Strings come from [VOICE.md](VOICE.md).
6. **Nothing is unrecoverable without a gentle confirm.** Undo and autosave are real and *say so*.
   Destructive actions are undoable or confirmed kindly — never a scary "cannot be undone" dialog.
7. **Touch targets ≥48dp, primary actions in the thumb zone.** Reachable one-handed; nothing
   important in the top corners.
8. **Contrast meets AA, including over texture.** Decoration never costs legibility. Paper grain,
   tape, and shadows sit behind text, never under it at the expense of contrast.
9. **Screen-reader parity for everything.** Every visible control and on-canvas element has a
   meaningful semantic label; every gesture has a discrete-control twin already announced.
10. **Decoration must also do a job — or get out of the way.** A flourish is allowed only if it is
    also an affordance or a state (a tape strip *is* the current-page marker), or if it is purely
    background and reduces no usability. No decoration that competes with content.
11. **No jargon, no walls, no dead ends.** No "canvas / element / export settings," no settings
    gate before making, no screen a beginner can get stuck on with no way forward.
12. **Privacy is reassurance, never a wall.** Surface "works offline · stays on your phone" as a
    gift, once, warmly. Never an account, a permission up front, or a network call
    ([privacy invariant](../PRD.md#5-product-principles-non-negotiable)).

## Per-screen review checklist

Run this before calling any screen done. Every box must be ✅.

- [ ] **Next step obvious** — a stranger knows the primary action in <2 seconds (rules 1–3).
- [ ] **Primary action visible & thumb-reachable**, ≥48dp (rules 1, 7).
- [ ] **Empty/loading/error states designed** and inviting, with copy from [VOICE.md](VOICE.md)
      (rules 4, 5).
- [ ] **All copy pulled from VOICE.md**; no placeholder or system strings (rule 5).
- [ ] **Every gesture has a visible-control twin** (rules 1, 9).
- [ ] **Undo/autosave reachable or stated; no scary destructive dialog** (rule 6).
- [ ] **AA contrast verified, including text over texture** (rule 8).
- [ ] **Screen-reader pass**: labels meaningful, order logical, decoration not announced (rule 9).
- [ ] **No jargon, no settings wall, no dead end** (rule 11).
- [ ] **No network, no account, no up-front permission**; privacy line present where it belongs
      (rule 12).
- [ ] **Decoration audited** — each flourish is an affordance, a state, or harmless background
      (rule 10).
- [ ] **Reduced-motion path** degrades gracefully (rule 8 / [motion philosophy](DESIGN-LANGUAGE.md#motion)).

## How this gate is used

- **Design:** a screen's [HTML prototype](mockups/) must satisfy the rules before it becomes the
  working visual reference.
- **Implementation:** the Compose PR description links the screen's prototype and confirms the
  checklist; the [reviewer](../../CLAUDE.md#review-workflow) validates against it.
- **Conflict:** if a rule and a feature request collide, the rule wins or the feature is
  re-scoped — escalate to an [ADR](../DECISIONS.md), don't quietly break a rule.
