# V2.1 Library — device verification (both passes)

**Date** 2026-08-10 · **Branch** `feat/v21-freeze-and-tokens` · **Build** `zinely-0.9.0-beta.1-debug.apk`,
assembled from `38d22a6` (Pass 1 re-run from `38d22a6` + the sheet inset fix below)
**Device** Samsung `SM-A176B` (`a17x`), **Android 16 / SDK 36**, 1080×2340, 450dpi physical / **420dpi
override** (2.625×), **three-button navigation**
**TalkBack** not run; the accessibility evidence here is the platform `AccessibilityNodeInfo` tree read via
`uiautomator dump`, not the Compose semantics tree. See [Not covered](#not-covered).

Screens exercised: the shelf with **9 real zines** (content state), both themes, and the action sheet.
Screenshots and tree dumps are in the session scratchpad; the numbers quoted below are from the dumps.

> **A dump trap, for the next person.** `adb shell uiautomator dump /sdcard/ui.xml` writes to
> `/Files/Git/sdcard/ui.xml` under Git Bash — MSYS rewrites the leading `/`. Use
> `adb exec-out uiautomator dump /dev/tty > ui.xml` instead. Added to
> [DEVICE-VERIFICATION.md](../DEVICE-VERIFICATION.md).

---

## Pass 1 — Developer verification

### ✗ → ✓ P1-1 · The action sheet put **Delete** under the navigation bar — **fixed and re-verified**

The frozen file writes `.sheet{padding:0 0 var(--gap-xl)}` and no `env(safe-area-inset-bottom)`, because a
browser has no gesture bar. Transcribed literally, a sheet pinned to `bottom:0` ends *behind* the system
navigation bar.

| | `Delete` row bounds | navigation bar top |
|---|---|---|
| before | `[0,2114]–[1080,2277]` | ~2235 |
| after | `[0,1988]–[1080,2151]` | ~2235 |

Roughly a quarter of the row — **the one row whose misfires are unrecoverable** — was under the system's own
targets. Fixed by consuming `navigationBars ∪ displayCutout` on the sheet before applying the frozen 24dp,
so the padding is measured from the top of the bar rather than from the screen edge beneath it.

This is the third instance of one defect class in this screen (the dock's own pad and
`zineDockClearance` are the other two), and it is **pre-existing** — V2's sheet had no inset either. It is
recorded as a Pass 1 failure rather than as inherited debt because the surface was re-skinned in this
package and the row is destructive.

### ✓ P1-2 · The shelf's dock clearance holds on hardware

The claim `zineDockClearance` was written for, and the one no unit test in this repo can make (see
[`ZineLibraryInsetTest`](../../feature/editor/src/test/kotlin/com/aritr/zinely/feature/library/ZineLibraryInsetTest.kt)
— the harness's decor view swallows a dispatched inset, so the assertion could only ever have been `0 == 0`).
Scrolled to the end of a 9-zine shelf on three-button navigation, the last caption (`Edited 9 days ago`)
clears the dock band with room to spare. **Verified.**

### ✓ P1-3 · The sheet's `border-top:2px solid var(--ink)` follows its rounded corners

Read off the raster rather than the source, because this was a real defect a week of code review did not
see: a straight `drawLine` under a `clip(SheetShape)` loses `--br-xl` (36dp) of rule at *each* top corner,
which is precisely where the sheet's edge meets the scrim. First ink pixel per column, light theme:

```
x=   2 → y 1210    x=  20 → y 1171    x=  60 → y 1142    x= 540 → y 1135
x=1078 → y 1215    x=1065 → y 1180    x=1030 → y 1147    x=1000 → y 1137
```

A continuous arc–line–arc, symmetric to ~5px. Run thickness on the flat top is **5px = 1.9dp** against a
frozen 2dp. Paper reads `#FFF6E8` exactly — the frozen `--paper`.

### ✓ P1-4 · The platform tree says what the design says

Every tile is a real `android.widget.Button`, `clickable` **and** `long-clickable`, named by its title; each
`⋯` is a separate `Actions for <title>` button. Touch targets are expanded to 126px = **48dp** (the `⋯` is
34dp of ink), so both routes to the action sheet meet the minimum.

```
Garden copy         | Button | click=true | long=true  | [42,192][519,935]
Actions for Garden… | Button | click=true | long=false | [413,829][539,955]
Make a zine         | Button | click=true | long=false | [340,1879][741,2025]
```

No node exists for the caption's date — the tile's seam ends in `clearAndSetSemantics`, which is the
deliberate narrowing recorded in `ZineOnShelf`. See [P2-4](#p2-4--the-shelf-says-less-by-ear-than-by-eye).

### ✓ P1-5 · Both themes render, and the material is the material

Light: desk cream, ink `#33261C`, grain visible on desk *and* covers, hard shadows offset with no blur, tape
translucent over the cover's own edge, spine crease on the binding side, postmark seated on the corner.
Dark: the same, inverted per the corpus's own dark block.

---

## Pass 2 — First-time user verification

*The screen's question is **"Which zine do I want?"*** It answers it: covers are the screen, and the name and
recency sit under each one. Nothing competes with that. Below are the places the reading breaks.

### P2-1 · A dark-theme "paper stock" cover reads as a hole, not as a zine

`.paper-s .fill{background:var(--paper)}`, and in the corpus's dark block `--paper:#332B22` sits on
`--desk:#241E18`. On the device that cover is very nearly the desk: what you see is an outline, a tape strip
and a mark floating in the background, and the first reading is **"that one failed to load"** — the same
sentence a broken thumbnail earns.

**This is faithful to the freeze**, so it is not an implementation defect and I have not touched it. It is a
finding *against the design*, which under the HTML-first workflow means the fix begins in
`v21-library.html`, not in Compose. **Owner ruling requested.**

### P2-2 · A long title wraps without limit and pushes its date under the dock

`.name` declares `line-height:1.2` and no `white-space`/`text-overflow`, and the corpus's longest title is
two words. A real shelf has `Garden copy copy copy copy copy`, which wraps to three lines, shoves
`Edited 6 days ago` into the dock's band, and makes the row look broken. Also faithful to the freeze, and
also a design question rather than a transcription one — a clamp (2 lines + ellipsis) is a spec change.
**Owner ruling requested.**

### P2-3 · The `⋯` does not read as a control

At rest it is 50% of `ink-soft` on cream, unlabelled, floating to the right of the caption with no chip, no
border and no hover state a finger can produce. Written down before knowing why: *"why is there an ellipsis
after my zine's name?"* It reads as punctuation or as a loading state. Long-press does the same job and is
undiscoverable by different means, so this is the only *visible* route to the sheet.

Kept by owner ruling (removing it leaves long-press as the sole route); recorded because the ruling was
about **whether it exists**, and this finding is about **whether it looks like anything**.

### P2-4 · The shelf says less by ear than by eye

A sighted user reads `A4` on the cover and `Edited 9 days ago` beneath it. TalkBack hears `Garden copy,
button` and nothing else — the caption and the postmark have no nodes. The sheet's header does state
`A4 · Edited 9 days ago`, so the information is reachable, but it costs an extra gesture and only if you
already suspect it is there. The alternative (announcing all three on every tile) makes a nine-item shelf
unskimmable by ear, which is why it was written this way. Recorded as the trade it is.

---

## Acceptance

| Pass | Outcome |
|---|---|
| Pass 1 | **Accepted** — one failure found (P1-1), fixed, and re-verified on the same device. |
| Pass 2 | **Accepted with three findings open**, none of them implementation defects: P2-1 and P2-2 are faithful transcriptions whose fix must start in the frozen HTML; P2-3 is a known addition whose *appearance* has never been designed. |

The two passes **do not disagree** on any screen: nothing here is "correct but misleading" in the
`0.9.0-beta.1` Preview sense. P2-1 and P2-2 are cases where correct transcription of a prototype meets data a
prototype never had.

## Not covered

- **TalkBack itself** was not run; only the platform node tree was read. Focus order, the long-press action
  label (`Actions`), and the sheet's `paneTitle` announcement are unverified.
- **Empty, loading and error states** — reaching them means clearing the app's data, which on this device
  means deleting the owner's nine real zines. Not done without the owner asking.
- **Reduced motion on device.** The Library's only continuous animation is the loading shimmer, which is
  gated by test (`ZineShelfMotionTest`) but was never observed on hardware, because the state is transient
  and the store answers immediately.
- **Pixel parity against the frozen HTML** is step **4d**, not this pass. What is verified here is that the
  values the code carries are the values the device draws (P1-3, P1-5), not that every one of them matches
  the prototype.
