# ADR-102 P2b / OD-48 — device verification (the warn-only keep-clear boundary)

**Device** Samsung **SM-A176B** `RZCYA1VBQ2H` · 1080×2340 · override density 420 (2.625 px/dp) · **Android 16**
**Build** `:app:installDebug`, `zinely-0.9.0-beta.1-debug.apk`, installed 2026-08-13 17:26, first measurement 17:29.
**Scope** [ADR-102 §12.11 / OD-48](../DECISIONS.md#adr-102-p2b) — the boundary is drawn **only** while a gesture
carries content across the printer's reach.

⚠ **The device identity does not match the record.** [P2's pass](2026-08-13-adr-102-p2-device-verification.md)
records this same serial as *"SM-A536E … Android 14"*. `adb` reports `SM-A176B` / release 16 today, and the
installer agrees (*"Installed on 'SM-A176B - 16'"*). One of the two records is wrong about the hardware its
measurements were taken on; this one states what the tool printed.

Both passes were run. They **disagree**, and per [CLAUDE.md](../../CLAUDE.md#acceptance) the disagreement is
the finding.

---

## Pass 1 — Developer Verification

### The states, in order, on glass

| state | expected after OD-48 | measured |
|---|---|---|
| empty page, at rest | nothing | nothing |
| element selected, verb bar up | **nothing** | **nothing** — the change, visible |
| in a text-edit session | nothing | nothing |
| **drag held, box across the inset** | `jam` at `.90` | **drawn**, α **.886** |
| drag released, content left outside | nothing | **nothing** |

The middle row is the one worth stating plainly: before OD-48 a dashed rectangle appeared here, inset in the
sheet, on every selection. It does not any more, and the page reads as paper.

The last row is [D-032](../design/V2-SPEC-DEFECTS.md#d-032-ruling) holding on hardware — *"the warning cannot
outlive the gesture that raised it"* — with content **actually left** in the margin, which is the case the
ruling is about and the one a reducer-backed implementation would have got wrong.

### The warning, measured

Held mid-gesture via `adb shell input motionevent DOWN/MOVE` (no `UP`), so the raster is taken with the
finger down:

```
light room   cue (213, 93, 59)  on paper (253,243,230)   α .870 / .888 / .900   mean .886
dark  room   cue (213, 93, 59)  on paper (253,243,230)   α .870 / .888 / .900   mean .886
model        (212, 91, 59) predicted at .90 over that paper
```

Nominal is `.90`; the screen gave `.886`. **The island is exact**: the cue pixel and the paper under it are
*byte-identical* between the two rooms — not close, identical — which is [OD-31](../DECISIONS.md#adr-098-od31)
and the seven-token island doing their job where the whole `jamText`/`jam` argument said they would.

**Paint order is verified by the alpha itself.** A cue painted under `BenchFocusScrim` reads ≈ `.445`; this
reads `.886`. The P2 defect has not returned, and it could not have been caught any other way — the
selection state that used to expose it no longer draws anything.

### ⚠ The contrast figure is 3.51:1, not 3.66:1

| | ratio |
|---|---|
| §12.9 / §12.11 as published | **3.66:1** |
| measured, this device, both rooms | **3.51:1** |

Neither number is wrong; they measure different things. **3.66 is the flat-token model** — `jam` at `.90` over
`paper` `#FFF6E8`. **3.51 is the sheet as it actually renders**, where the grain darkens the paper reference
to `(253,243,230)`. The mark still clears WCAG 1.4.11's 3:1 with room, in both themes, which is what the
ruling rests on. But §12.9's *"3.66:1"* has now been quoted through three documents as though it were an
observation, and it is a computation; P2's own Pass 1 hit the identical gap on the cue (2.07 computed, 2.01
measured) and the lesson did not travel. **The published figure should say which kind of number it is.**

### Accessibility: the boundary is not in the platform tree at all

`uiautomator dump` on the selected state — 133 nodes, and **no node mentions the boundary**. A search across
every `text` and `content-desc` for *margin / printer / reach / keep / bleed / safe* returns nothing.

This is not a regression — the mark was never announced, and as a decorative stroke it arguably should not
be. It is recorded because **OD-48 changed what that silence costs**: the visual mark is now the *only* signal
that content is leaving the printer's reach, and it is transient. A TalkBack user is not told, and there is no
longer even a resting boundary they might encounter by another route.

### ⚠ Required Fix — the nudge path warns about nothing

The platform tree exposes `Move left / right / up / down`, `Make larger / smaller`, `Rotate`. These are how the
element moves without a drag. **Nudging content across the printer's reach draws nothing at all** — measured,
twice, with the element already outside: **zero** cue pixels on the sheet.

The cause is not a bug in the fix, it is the fix's blind spot. `BenchStudio.keepClearWarn` derives the warning
from `Interaction.Transforming` + `live`, or from `resizeOverride` — *in-flight gestures*. A nudge is a
discrete intent; it is never "in flight", so it can never warn. Before OD-48 that path was covered by
accident: selecting the element drew the resting boundary, so a user nudging content out could **see** the
limit they were approaching. OD-48 removed the accident and left nothing behind it.

So the defect class is: **the accessible way to move an element is the one way you are never warned.** That
reads worse the more carefully it is stated, because the nudge buttons exist precisely for users who cannot
drag.

---

## Pass 2 — First-Time User Verification

Reset assumptions. A warm sheet, my word on it, eight handles, a row of verbs.

**U4 — the warning going away looks exactly like the problem going away.** I dragged my text toward the corner
and a red dotted rectangle appeared. I understood it instantly: *too far, that's the edge.* Then I let go, and
it vanished. *Written down before I let myself remember the implementation:* **"good — I must have got it back
inside."** I had not. My text is still hanging off the edge of the printable area, and the screen looks
exactly as calm as it did before I ever touched it.

This is the mirror image of the finding OD-48 was ruled on. U2 said a mark that appears with selection reads
as *caused by* the selection. U4 says a mark that disappears on release reads as *resolved by* the release.
Both are the same underlying problem — the mark's timing is telling a story about my content that isn't true.

**What genuinely improved, and it is not small.** At rest and on selection the page is now quiet. There is no
dotted rectangle sitting on my work, no faint frame implying a form I have to fill correctly. The sheet looks
like paper. And the alarm colour is no longer arguing with the moment — when I see red, something *is* wrong.
BP-4's *"not a visible pro grid"* is satisfied on the screen and not only in the ADR.

**U1′ — still unlabelled.** Nothing names the rectangle. This bothered me less than in P2, because it now
arrives at the moment it is about, which is its own explanation. It is narrower, not closed.

**Against the screen's question.** The Editor answers *"How do I change this page?"* The boundary now only
speaks while I am changing something, which is the right moment. It just stops speaking a beat too early.

---

## Acceptance

| | verdict |
|---|---|
| Pass 1 | **FAIL — one Required Fix.** Everything specified is on the glass and measured (`.886` both themes, island exact, D-032 honoured, paint order intact). But the nudge path cannot warn, and OD-48 removed the resting cue that used to cover it |
| Pass 2 | **FAIL — U4.** The warning's disappearance reads as the problem being solved |

**The two passes disagree, and this time they disagree about the same event.** Pass 1 says the transient
warning is exactly the ruling, implemented exactly. Pass 2 says transience is what makes it lie. Both are
true. *"Correct but misleading"* is a defect with a known cause, which makes it cheaper to fix than most —
not safer to ship.

⚠ **Neither failure argues for reverting OD-48.** The resting cue was worse on its own terms and the room got
quieter without it. What both findings say is that **the release is unhandled**: the product has no way to
tell a maker *"this is still outside"* once the gesture ends, and never had — the resting cue only ever
implied it. That is the shape of the next package, and it is a design question, not a code fix:

- something that persists at the moment content is *committed* outside the reach, rather than while it moves;
- or a check the maker meets later, at the point the question is actually theirs — Preview or Print, where
  *"how do I print this correctly?"* is the question the screen already answers;
- **and** a non-gesture path to the same information, because the nudge buttons and TalkBack are the same gap
  seen twice.

**Device left in light theme** (its pre-pass state), Do Not Disturb off, the test element deleted, the zine as
found.
