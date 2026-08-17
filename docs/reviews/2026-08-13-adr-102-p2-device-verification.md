# ADR-102 P2 — device verification (keep-clear cue + snap guides)

**Device** Samsung SM-A536E `RZCYA1VBQ2H` · 1080×2340 · override density 420 (2.625 px/dp) · Android 14
**Build** `:app:installDebug`, installed 2026-08-13 13:17:54, first measurement 13:18:06 — twelve seconds, so
the screen under the probe is the tree under the probe.
**Scope** [ADR-102 §12.9](../DECISIONS.md#129--p2-the-printers-reach-and-the-guides-that-find-it) — `BenchKeepClear`
and `SnapGuides`.

Both passes were run. They **disagree**, and per [CLAUDE.md](../../CLAUDE.md#acceptance) the disagreement is
recorded rather than averaged.

---

## Pass 1 — Developer Verification

### The defect this pass exists to have caught

§12.9 accepts `berry` below the 3:1 floor **as decorative**, and it rests that acceptance on `jam` being the one
mark that clears the floor. Both readings are arithmetic over a stated alpha. On glass, neither held:

| mark | §12.9 says | measured, pre-fix | why |
|---|---|---|---|
| keep-clear cue (`berry`, α .85) | 2.07:1 | **1.42:1** at an effective α of **.42** | painted under the focus wash |
| warn (`jam`, α .90) | 3.66:1 | **1.82:1** at an effective α of **.445** | painted under the focus wash |
| snap guide (`butter`, α .85) | 1.60:1 | 1.60:1 at α **.85** | drawn *above* the wash — correct |

`BenchKeepClear` was nested inside the sheet box, which composes **before** `EditorPagePreview` — and that
composable draws `BenchFocusScrim`, one composite bounded to the page rect. The freeze dims
`.el:not(.selected)` (`v21-bench.html:207`): it dims **elements**. `.keepclear` is a *sibling* of `.content`
(`v21-bench.html:503-505`) and is dimmed by nothing. So the Compose composite washed a mark the frozen design
never washes.

**The review improved this argument, and the improvement is worth more than the finding.** I justified the
fix by document order and by what the dim selector scopes to — both true, both readings. The reveal rule is
`.content.focusing~.keepclear,.content.focusing~.guideV{opacity:.85}` (`v21-bench.html:190`), and `~` is the
general-sibling combinator: it matches **only** elements that follow `.content`. The freeze does not merely
permit the cue above the content — its own selector cannot be satisfied otherwise. The pre-fix order was
un-implementable, not merely divergent. Note also that `.pagenum` is deliberately *absent* from that rule, so
lifting the folio out of the wash moves it toward the freeze too.

The sharp part: **the cue's trigger is the wash's trigger.** Both fire on `.content.focusing`. The cue was
therefore never once seen at its own alpha — there was no state of the app in which the defect was absent, and
so nothing looked like a regression.

Measurement, dark theme, on the sheet's own paper `(253,243,230)`:

```
cue core (242,199,200)  → implied α per channel .440 / .407 / .423   (nominal .85)
warn core (232,169,147) → implied α per channel .457 / .438 / .440   (nominal .90)
```

The warn's three channels agree tightly against `jam` `#CF4A28` and not at all against `berry`
(`.84 / .685 / 1.167`), which independently confirms the review-caught `jamText`→`jam` island fix is live in
**dark theme** — the theme the defect lived in.

`0.85 × (1 − BenchFocusDimAlpha)` = `0.425`. Measured `0.42`. The model is exact.

### The fix, and its check

`BenchKeepClear` + `BenchPageNumber` moved into a second Box, identically positioned from the same
`paperX/paperY/paperWidth/paperHeight`, composed **after** `EditorPagePreview`. Geometry is unchanged, so
[D-033](../DECISIONS.md) still holds; only paint order moved. This also matches the freeze's own document
order, where `.keepclear`, `.guideV` and `.pagenum` all follow `.content` and therefore paint over elements.

No existing check could see this. The unit tests over `BenchStudio`'s colour maths read the *nominal* alpha and
remain correct. The `SelectionChromeGoldenTest` scenes hand-assemble their own z-order, so asserting against
them would assert only what the test itself wrote. And no golden covered it at all: the `EditorScreen` golden
page carries **no elements**, so nothing on it could ever be selected.

`EditorScreenGoldenTest."the keep-clear cue is not dimmed by the focus wash it is triggered with"` therefore
renders the **real** `EditorScreen`, places and selects a real element, and probes the raster — asserting
implied alpha against paper measured in the same image, so the sheet's grain divides out.

**Mutation-checked.** Reverting the paint order fails it at implied **0.404** (per-channel `.36 / .42 / .43`) —
the same figure the device gave, from Robolectric.

### Post-fix measurement, light theme

```
cue core (232,153,170)  predicted (232,152,170)
implied α per channel   .840 / .835 / .845          (nominal .85)
contrast on its paper   2.01:1                      (§12.9's 2.07:1, on grained paper)
```

The island holds across themes by construction and by measurement: pre-fix, the cue read `(242,199,200)` in
dark and `(242,199,201)` in light — one count apart on one channel.

### Geometry (unchanged by the fix, measured pre-fix)

| property | frozen | measured |
|---|---|---|
| cue stroke | 1.5 dp | 3.5–3.9 px ≈ 1.4 dp |
| guide stroke | 1.5 dp | 3.9 px ≈ 1.49 dp |
| dash | equal on/off | cue 5 on / 6 off; guide 68 runs across the sheet |
| guide colour | `butter` α .85 | `(247,189,74)` vs predicted `(247,188,72)` |
| trigger | `opacity:0 → .85` | 0 non-paper px at rest; 158 on selection, same row |

### What this pass did **not** establish

**The warn state was not re-measured on the fixed build.** It needs a gesture held in flight across an adb
round trip, and the edit-pan moves the sheet mid-drag, which repeatedly carried the mark out of the probe
window. It *was* measured pre-fix (above), and the fix changes paint order only — the warn is the same
`drawRoundRect` with a different colour and alpha constant, both already unit-tested — so the post-fix `berry`
measurement of .840 covers the mechanism. But I did not observe the warn at .90 on glass, and §12.9's 3.66:1
is therefore still a computed figure rather than a measured one.

---

## Pass 2 — First-Time User Verification

Reset assumptions. A warm sheet, a pink dotted rectangle inset from its edge, my text in a black dashed box
with eight handles, a floating bar of actions.

**U1 — the boundary is unlabelled, and it is the colour of a mistake.** A pink-red dotted rectangle appears the
instant I touch my text. Nothing on screen names it. *Written down before I let myself remember what it is:*
"have I put something somewhere I shouldn't have?" It is in fact a helpful margin guide, but it announces
itself in the vocabulary of an error, and it arrives at the same moment as my selection — so it reads as
**caused by** what I just did.

**U2 — it comes and goes with selection, which is backwards for what it means.** The printer's reach belongs to
the *page*. It is true whether or not I am holding anything. Showing it only while I hold something told me it
was a limit on *this element* — nearly right, for the wrong reason, and a reading that would break the first
time I saw an older element already sitting outside it.

**U3 — the action bar covers the bottom of the boundary.** Edit / Font / Size / Ink / Delete sits across the
sheet's lower portion and cuts the pink rectangle's bottom edge. The one thing the boundary exists to tell me —
*where the bottom limit is* — is the part I cannot see. Both the bar and the cue are summoned by the same tap,
so they arrive together and fight over the same moment. In the text-editing state the style row does the same
thing higher up, cutting the rectangle in two.

**U4 — the page number is behind that bar too.** Same cause as U3.

**U5 — "Font" is greyed out on a text element** while Size and Ink are live, with nothing saying why. Recorded
as first-read confusion; it is not P2's to fix.

**Against the screen's question.** The Editor answers *"How do I change this page?"* The cue answers *"where
will the printer cut?"* — a print question arriving during an edit. That is the [ADR-058](../DECISIONS.md#adr-058)
"Preview" shape in miniature: a good answer, at a moment the user did not ask it. [D-032](../DECISIONS.md)
deliberately makes this transient guidance tied to the interaction, and the owner has ruled on keeping it, so
this is logged as an observation and not a demand.

---

---

## Disposition of the Pass 2 findings (added 2026-08-13, after the owner's ruling)

Ruled in [ADR-102 §12.10](../DECISIONS.md#adr-102-p2-pass2); research carried in
[the P2b brief](../proposals/2026-08-13-p2b-warn-only-boundary.md).

**U3 — fixed for the verb bar and re-verified on device.** `EditorScreen` reserves the bar's band before
fitting the page, so the sheet is sized and centred above it. Re-measured on the corrected build: the
boundary's **complete rectangle** and the folio `1 / 8` are visible, the sheet's foot sits flush with the
bar's top (which is the reserve's definition, not a margin) and the boundary clears the card by **21 dp**.
Held at rest as well as on selection, because the band is reserved unconditionally.

⚠ **Scope:** the Type bar and the `.inkpop` popover occupy the same bottom inset and the popover is taller
than the reserve, so this defect class is **not** closed for Size and Ink. Named in §12.10 so it is not
mistaken for done.

⚠ **The reserve took two wrong answers.** 80dp first (counting the transparent padding above the card), then
a review's proposal of Material's 48dp row floor (real, but it grows the touch target, not the layout box).
The suite now asserts the constant against a measurement of the composed bar — **68dp** — so neither error
can recur silently.

**U1 — cause withdrawn, finding carried.** I wrote that the cue is *"the colour of a mistake"*. The hue half
looks wrong: print convention puts **magenta/pink on the safe-zone guide** and **red on the bleed/trim line**,
and Zinely draws no bleed line. But a second review was right that this is vendor documentation rather than a
standard, and that it cannot retract a *reading* — only a *diagnosis*. The confusion recorded on the device
stands and travels to P2b. *Pass 2 asks what felt wrong; I answered with why, which is the one thing that pass
disqualifies you from knowing.*

**U1′ (unlabelled) — carried, because the obvious fix is forbidden.** [BP-4](../design/V2-BENCH-PRINCIPLES.md)
says *"the maker never learns the word 'bleed'"*, and the industry has no standard name to borrow anyway
("live area" / "safe zone" / "safety margin" all mean the same thing and vary by vendor). Labelling it is not
available.

**U2 — deferred to its own package, and then ruled the same day.** It is a freeze amendment and it deletes
the mark §12.9 had just accepted as decorative, so it was briefed separately at
[P2b](../proposals/2026-08-13-p2b-warn-only-boundary.md) rather than settled inside this fix. The owner ruled
**option (a), warn-only** — [ADR-102 §12.11 / OD-48](../DECISIONS.md#adr-102-p2b). The resting cue is gone:
the boundary is drawn only while a gesture carries content across it.

That also answers **U1** without arguing about hue. A mark that appears only when something is wrong is
entitled to look like a warning; the reading recorded on this device was that an alarm colour arrived at a
moment nothing was wrong, and there is no longer such a moment. **U1′ narrows rather than closes** — the mark
is still unlabelled, but it now appears at the instant it is about, which is the only teaching BP-4 allows.

⚠ **The passes below were run against the pre-OD-48 build and do not verify it.** Everything measured here —
paint order, alphas, geometry, the reserve — still holds, because OD-48 changed the cue's *trigger* and
*colour*, not its z-order or its box. But the questions Pass 2 would now ask are new ones (*is a maker
pushing content off the edge still told, when nothing announced the boundary beforehand?*) and no device has
been asked them. Both passes are owed on the amended build before merge.

## Acceptance

| | verdict |
|---|---|
| Pass 1 *(pre-OD-48 build)* | **PASS**, after the paint-order fix and with the warn's post-fix reading stated as unmeasured |
| Pass 2 *(pre-OD-48 build)* | **PASS with one finding carried** — U3 fixed and re-verified, U1 withdrawn, U1′ carried, U2 briefed separately |

⚠ Both rows are qualified in the table itself and not only in the paragraph above it, because a reader
skimming to a verdict finds the table. An unmarked **PASS** against a build that no longer exists is the
failure mode commit `0d7cc57` was written about — *three freeze banners that lied* — and it is cheap to
avoid twice.

**The two passes disagree, and the disagreement is the finding.** Pass 1 says the cue is now exactly what the
freeze specifies, at the alpha the freeze names, in both themes. Pass 2 says a mark that is correct, unlabelled,
the colour of an error, and half-hidden by the toolbar that appears with it is not yet doing its job. Both
readings are true. "Correct but misleading" is a defect with a known cause, which makes it cheaper to fix than
most — not safer to ship.

U1/U2 are design questions and would need the HTML specification updated first. U3 is an occlusion between two
surfaces that fire on the same predicate; it is the same cause as the P1-booked finding that the wash covered
the page border and page number, and it is now the second finding pointing at that seam. Neither is a P2 code
change; both are the owner's to rule on.

**Device left in light theme** (its pre-P2 state). The test element placed for these measurements was deleted
from the zine.
