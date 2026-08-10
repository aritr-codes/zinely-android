# V2.1 Library — device verification (both passes)

**Date** 2026-08-10 · **Branch** `feat/v21-freeze-and-tokens` · **Build** `zinely-0.9.0-beta.1-debug.apk`,
assembled from `38d22a6` (Pass 1 re-run from `38d22a6` + the sheet inset fix below)
**Device** Samsung `SM-A176B` (`a17x`), **Android 16 / SDK 36**, 1080×2340, 450dpi physical / **420dpi
override** (2.625×), **three-button navigation**
**TalkBack** Samsung TalkBack, bound and running for the accessibility readings (§P1-6).

Screens exercised: the shelf with real zines (content state) in **both themes**, the action sheet, and — on
the owner's authorisation to delete the device's data — the **loading**, **empty** and **error** states.
Screenshots and tree dumps are in the session scratchpad; the numbers quoted below are from the dumps.

> **Reading the screenshots:** with TalkBack running, its accessibility-focus indicator paints as a **filled
> blue box** over the focused text on this device. It appears over `ONE SHEET` in the mid-session empty-state
> captures and it is not a defect — the same frame with TalkBack unbound is clean. It cost twenty minutes to
> rule out, which is the reason it is written down.

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

### ✗ → ✓ P1-6 · The empty state's `→` was drawn by an **emoji font** — fixed, twice over

`.tf .arrow` sets U+2192 in the voice face. On the device it rendered as a **blue rounded square with an
orange arrow** (`#1A5CE5` measured in the raster), ignoring `color:var(--jam)`, sitting between two hand-drawn
illustrations. It reads as a broken image. Nothing in the repository could have caught it: Robolectric's font
fallback chain is not the device's.

Two fixes were tried on hardware and **both failed**, which is what makes this worth recording:

| attempt | reasoning | result |
|---|---|---|
| set it in the sans | Averia carries no U+2192 (cmap read out of `averia_sans_libre_{regular,bold}.ttf`); all four bundled Inter weights do | still `#1A5CE5` |
| append **U+FE0E** | the text-presentation variation selector — the documented "this is text" signal | still `#1A5CE5` |

U+2192 is `Emoji=Yes` (text-default) in UTR #51 and the substitution happens **above the font layer**, so
neither a `fontFamily` nor a variation selector reaches it. The arrow is now a **path** — 24dp box, 2dp
round-capped strokes, `jam` — verified on device as `#D04D2B` with no blue pixel in the region.

D-021's three other orphans (`⋯` U+22EF, `✎` U+270E, `⧉` U+29C9) are absent from Inter too and still fall
back, but none is emoji-eligible and all three drew as plain monochrome text glyphs here. Left alone.

### ✓ P1-7 · Loading, empty and error all render, and the hidden shelf head holds its place

- **Loading** — four `.ph` placeholders, 2×2, dashed border on paper with the hard shadow and a visible
  diagonal sweep. The shelf head's space is **blank but reserved**, exactly as the reconciliation intended:
  the first placeholder row starts where it would with the heading drawn. This is the direct device evidence
  for the fix both 4c reviewers demanded.
- **Empty** — vertically centred as `.empty{justify-content:center}` requires, with the 150dp + safe-area
  clearance below it.
- **Error** — the `!` disc, *"Your shelf didn't open."*, the two-line reassurance, and `Try again` as a quiet
  control with the dock still offering `Make a zine`.

### ✓ P1-8 · Zero NAF nodes

24 nodes on the content shelf under a running TalkBack, **none** flagged `NAF` ("not accessibility
friendly"). Traversal order is heading → count → tile → that tile's `⋯` → next tile → … → `Make a zine`,
which matches the visual order. Two limits of this instrument: `uiautomator` exposes neither the `heading()`
flag nor custom action labels, so *"Actions"* on long-press remains unverified.

---

## Pass 2 — First-time user verification

*The screen's question is **"Which zine do I want?"*** It answers it: covers are the screen, and the name and
recency sit under each one. Nothing competes with that. Below are the places the reading breaks.

### 🔴 P2-0 · A corrupt database destroys the user's zines and then shows them the **invitation**

**The most serious finding in this pass, and it is not a Library defect — the Library is the screen where it
becomes visible.** Sequence, measured end to end on the device:

1. `databases/zinely.db` was overwritten with 33 bytes of text (`run-as`, contents verified before launch).
2. On launch Room threw
   `SQLiteDatabaseCorruptException: file is not a database (code 26 SQLITE_NOTADB)` on `PRAGMA journal_mode`.
3. The file came back as a fresh 4096-byte `SQLite format 3` — **Room deleted the database and recreated it
   empty.** Everything the user had made is gone, with no prompt, no backup and no notice.
4. The screen that greeted this: **the empty state.** `Make your first little zine.` Pixel-identical to a
   genuine first run (55 differing pixels out of 157,950 sampled — the clock).

The `Error` state does exist and is reachable — `chmod 500 databases` produces
`SQLiteCantOpenDatabaseException` and the shelf renders *"Your shelf didn't open. Your zines are still on
your phone…"* (P1-7). So the app has **two** read failures and tells the truth in only one of them:

| failure | data | what the screen says |
|---|---|---|
| cannot open | intact | *"Your zines are still on your phone"* — true |
| corrupt | **destroyed by the recovery** | *"Make your first little zine."* — the user is told they have never made anything |

Against [the product principle](../../CLAUDE.md#product-principle-every-screen-answers-the-users-current-question),
the user arrives holding *"which zine do I want?"* and is answered *"you have no zines."* The honest reading
from their chair is **"it deleted my work"** — and unlike the `0.9.0-beta.1` Preview case, that reading is
literally correct.

**Not fixed here, deliberately.** The fix is a data-layer decision, not a re-skin: whether Room may destroy on
corruption at all, whether a corrupt file is quarantined rather than deleted, and which state the Library
shows when it happens. That is ADR-shaped and it belongs to the owner. Two facts to decide against: nothing
in the app writes a backup, and the empty state is the only screen a user will ever see for this.

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
| Pass 1 | **Accepted** — two failures found (P1-1 the sheet's Delete row, P1-6 the emoji arrow), both fixed and re-verified on the same device. |
| Pass 2 | **NOT accepted for the product** on **P2-0**, which is a data-loss finding and an owner decision. **Accepted for the re-skin**: P2-0 is not a Library defect, and the three remaining findings are not implementation defects — P2-1 and P2-2 are faithful transcriptions whose fix must start in the frozen HTML, and P2-3 is a known addition whose *appearance* has never been designed. |

**The two passes disagree once, and the disagreement is P2-0.** Pass 1 says the error state is correct, the
copy is honest, and the recovery does not crash — all true. Pass 2 says the user is shown a screen stating
they have never made a zine, moments after the app deleted every zine they made. Per
[CLAUDE.md](../../CLAUDE.md#acceptance) that disagreement is the finding and must be resolved rather than
averaged; it is recorded here and referred to the owner, and Pass 1's correctness does not overrule it.

The other three are cases where correct transcription of a prototype meets data a prototype never had.

## Not covered

- **TalkBack's spoken output.** The service ran, and what it consumes — the platform node tree, traversal
  order, NAF flags — was read directly. The utterances themselves were not captured: Samsung TalkBack does
  not log them, and injected `input tap` events bypass explore-by-touch, so focus could not be driven from
  the shell. The `heading()` flag, the long-press action label (*Actions*) and the sheet's `paneTitle` are
  therefore unverified — `uiautomator` does not carry any of the three.
- **Reduced motion on device.** The Library's only continuous animation is the loading shimmer, which is
  gated by test (`ZineShelfMotionTest`) but was never observed on hardware, because the state is transient
  and the store answers immediately.
- **Whether P2-0 loses data on a *real* corruption** (a partial write, a bad sector) rather than on a
  synthetic one. The recovery path is the same either way; the trigger was manufactured.
- **Pixel parity against the frozen HTML** is step **4d**, not this pass. What is verified here is that the
  values the code carries are the values the device draws (P1-3, P1-5), not that every one of them matches
  the prototype.
