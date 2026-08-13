# P2b — should the keep-clear boundary appear on selection, or only on crossing?

**Status** 🟦 proposal, awaiting an owner ruling. Booked by [ADR-102 §12.10](../DECISIONS.md#adr-102-p2-pass2)
out of P2's device Pass 2 (finding U2). Nothing here is implemented.

**Why it is a separate package.** It is a **freeze amendment** — the HTML specification changes first — and it
would delete the mark [§12.9](../DECISIONS.md#adr-102-p2-marks) has just spent a ruling accepting as
decorative. Deciding it inside a device-verification fix would be deciding it quietly.

> ⚠ **Research note.** Durable findings normally land in [RESEARCH.md](../RESEARCH.md); that file is under a
> standing do-not-modify instruction for this work, so the evidence is carried here instead. It should be
> folded in by whoever takes this package.

---

## The finding

The boundary appears the instant you select anything and disappears when you deselect. Written down during
Pass 2, before letting the implementation explain it:

> *The printer's reach belongs to the page. It is true whether or not I am holding something. Showing it only
> while I hold something told me it was a limit on this element — nearly right, for the wrong reason, and a
> reading that breaks the first time I see an older element already sitting outside it.*

## What the product's own principles say

[BP-4](../design/V2-BENCH-PRINCIPLES.md) — *Make print-correctness felt, not taught*:

> *"The maker never learns the word 'bleed.' The three pro print boundaries collapse into one soft keep-clear
> inset whose meaning is **behavioural** — a gentle nudge only when text or faces cross it… Calm comes from
> alignment the maker didn't have to think about: **an invisible snapping grid, not a visible pro grid.**"*

[OD-10](../design/V2-SPEC-DEFECTS.md#d-032-ruling) already held the **warn** state to that standard —
*"transient guidance, not document state"*, derived per frame from the in-flight gesture, no reducer state.

**The resting cue is the one part never held to it.** It comes from the freeze's
`.content.focusing~.keepclear{opacity:.85}` (`v21-bench.html:190`) and §12.9 ratified it as *"Trigger:
frozen"* without asking whether the freeze and BP-4 agree. They may not: a boundary drawn on every selection
is nearer the visible pro grid BP-4 rejects than the felt nudge it asks for.

## What comparable products do — ✅ VERIFIED

| Product | Margin / safe-area guide | Trigger |
|---|---|---|
| **Canva** | dotted lines for bleed and margin; fixed 0.125 in, not adjustable | a **view setting** (`File → View settings → Show print bleed`) — persistent, user-toggled, never tied to selection ([Canva Help](https://www.canva.com/help/margins-bleed-crop-marks/)) |
| **InDesign** | **magenta/violet = margin (safe zone)**; **red = bleed/trim** — two colours for two meanings | persistent document guides ([OER Commons](https://oercommons.org/authoring/27469-indesign-cc-guides-for-bleeds-and-margins/view)) |
| **Figma** | no bleed concept at all; you size a larger frame by hand ([8designers](https://www.8designers.com/blog/what-is-the-bleed-margin-in-canva)) | n/a |
| **Printful, label printers** | dotted line, often black | persistent on the proof ([Plum Grove](https://plumgroveinc.com/safe-zone-for-print/)) |

Two findings that bear directly on the ruling:

- ✅ **The industry answer is "persistent, opt-in", not "on selection".** No surveyed tool ties the guide to
  selection. But every surveyed tool is a **pro** tool that teaches the vocabulary — which is the thing BP-4
  refuses. *The convention is evidence about what is legible, not about what this product should do.*
- ✅ **`berry` is the conventional hue, and Pass 2's U1 was wrong to call it alarming.** Magenta/pink is the
  safe-zone colour; red is the bleed line, which Zinely does not draw. This withdrew U1 — recorded in §12.10.
- ✅ **There is no standard name to borrow.** "Live area", "safe zone", "safety margin", "safe area" all mean
  the same thing and vary by vendor ([Plum Grove](https://plumgroveinc.com/safe-zone-for-print/)). So a label
  would have to be invented in the app's own voice — and BP-4 forbids teaching the vocabulary at all, which
  is why "just label it" is not the available fix for U1′.

## Options

| | | Cost |
|---|---|---|
| **(a) Warn only** | delete the resting cue; the boundary appears **only** while the in-flight gesture crosses it | BP-4 and OD-10 applied literally, and the mark's alarm colour becomes *correct* because it then only ever means alarm. Removes U1′ (nothing unlabelled is ever shown without cause) and U2 (it is no longer a claim about the page). **But** the maker gets no ambient sense of the margin before crossing it, and it deletes a frozen mark |
| **(b) Ratify as frozen** | change nothing; carry U2 as an observation | consistent with OD-26/27/28's handling of C9's Pass 2. Leaves the freeze and BP-4 in unresolved disagreement, in writing |
| **(c) Persistent, page-level, fainter** | draw it always, never tied to selection, at lower alpha | fixes the mental model; matches every surveyed tool; is the *visible pro grid* BP-4 names as the failure. Also puts a permanent mark on the artifact, which invites an [OD-31](../DECISIONS.md#adr-098-od31) reading |
| **(d) Persistent but opt-in** | a view toggle, off by default | the literal Canva answer. Adds a setting to a product whose whole posture is *no settings the maker has to find* |

**Recommendation: (a), with (b) as the honest fallback** if the loss of the ambient margin reads worse on
glass than the argument predicts. Whichever is taken, the HTML specification changes **first**.

## What a ruling needs to settle

1. Does the resting cue survive at all?
2. If it does, does BP-4 get amended to admit it — because as written, BP-4 argues against it?
3. Does [D-064](../design/V2-SPEC-DEFECTS.md#d-064) still have two decorative marks to ask about, or one?
4. §12.9's acceptance rests on *redundancy* — the resting cue says *about here*, the warning says *you are
   crossing it*. Option (a) removes the first half, so the acceptance would need restating rather than
   inheriting.
