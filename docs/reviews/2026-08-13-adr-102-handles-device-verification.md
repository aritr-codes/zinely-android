# Device verification — the selection ring, the handle marks, and the grid's page number

**Date** 2026-08-13 · **Device** RZCYA1VBQ2H (SM-A176B, Android 16) · **Display** 1080×2340, override
density 420 ⇒ **2.625 px/dp** · **Build** `versionName=0.9.0-beta.1`, installed 10:48:31, measured 10:49–11:0x
· **Both passes run.**

Subject: the ruling recorded at [ADR-102 §12.8](../DECISIONS.md#adr-102-p1-handles) (handle marks move onto
the ring; hit targets stay on the geometric corner; ring inset corrected 6dp → 5.2dp) and the WCAG 1.4.3
fix booked in [§12.5](../DECISIONS.md#adr-102-od47) (grid page number `inkFaint` → `inkSoft`).

Every number below is measured from a device raster or read from the platform
`AccessibilityNodeInfo` tree. Nothing is taken from the unit suite, which cannot see any of it.

---

## Pass 1 — Developer Verification

### 1.1 The eight marks, and where they actually sit

Element under test, from the platform tree: `content-desc="Empty text"`, `bounds="[235,860][846,1076]"`.

Eight mark blobs found by connected-component search, each **24×24 px** (9dp mark + 1.6dp border ⇒ 24.1 px
predicted):

| | x = 220.5 | x = 539.5 | x = 858.5 |
|---|---|---|---|
| **y = 845.5** | TL | **T** | TR |
| **y = 967.5** | **L** | — | **R** |
| **y = 1089.5** | BL | **B** | BR |

The four edge marks land on the **exact** midpoints of the corner marks — `(220.5+858.5)/2 = 539.5`,
`(845.5+1089.5)/2 = 967.5` — which is the frozen `calc(50% - 4.5px)` reproduced to the pixel.

**Standoff, by intensity-weighted centroid** (a hard threshold cannot resolve this; the stroke is 4.2 px
wide and antialiased):

| | measured | ruled |
|---|---|---|
| mark, left edge | 14.49 px = **5.52 dp** | 5.5 dp |
| mark, top edge | 14.50 px = **5.52 dp** | 5.5 dp |

✅ **The marks are where the ruling puts them**, to 0.02 dp.

### 1.2 The ring — confirmed present and placed, *not* separated from the marks

| | measured |
|---|---|
| ring stroke centre, left | 5.49 dp outside the box |
| ring stroke centre, top | 5.33 dp |
| ring stroke width | 4 px ≈ 1.52 dp (frozen 1.6 dp) |

⚠ **Bound on this pass.** The ruling separates the ring (5.2 dp) from the marks (5.5 dp) by **0.79 px** at
this density. Measured, the two differ by **0.16 px** — well inside the error of a centroid taken over a
4.2 px antialiased *dashed* stroke. **This pass certifies that the ring is present, dashed, `ink`, and
stands off by ~5.3–5.5 dp. It does NOT certify 5.2 dp as distinct from 5.5 dp.** That distinction is
asserted where it can actually fail — `BenchStudioSurfaceTest`, which derives both constants from the
frozen CSS. The device is the wrong instrument for it, and saying so is cheaper than implying otherwise.

What the device *does* settle, and the unit suite could not: **the marks read as lying on the ring.** One
figure, not two concentric ones. That was the ruling's entire purpose.

### 1.3 The hit target did not move — tested by dragging, not by inspection

The handles expose **no platform node** (resize is pointer-only; the a11y route is the nudge pad, §1.5), so
the seed was tested by driving it.

| | before | after | Δ |
|---|---|---|---|
| bounds | `[235,860][846,1076]` | `[235,860][978,1208]` | — |
| width | 611 px | 743 px | **+132** |
| height | 216 px | 348 px | **+132** |
| top-left | (235,860) | (235,860) | **0, 0** |

Drag was +150 px on both axes; 18 px went to touch slop. **A seed displaced onto the mark would have added
5.5 dp = 14.4 px to each axis and read ≈ 146.** It reads 132. The opposite corner held exactly, and both
axes moved identically.

✅ `centerPx` still seeds the accumulator from the geometric corner.

### 1.4 Rotation — the marks turn with the box, rigidly

Three taps of `Rotate clockwise` (15° each ⇒ 45°). Seven of eight marks recovered (the eighth sits outside
the scanned column range):

- Every edge mark is the **exact midpoint** of its two neighbouring corner marks, after rotation.
- Mark-rectangle side lengths **771.1 × 377.6 px** against **771.9 × 376.9 px** predicted (the element box
  inflated 5.5 dp per side and rotated). Error < 0.9 px on a 771 px span.

✅ The rotation is applied to the offset, at the right sign, and preserves length.

### 1.5 Accessibility — read from the platform tree, not from Compose semantics

- Style row: `Edit` and `Delete` report `clickable=true enabled=true`; `Font`, `Size`, `Ink` report
  **`clickable=false enabled=false`**, matching their dimmed appearance. **This is not the ADR-058
  `ZoomButton` defect** — the platform is being told the truth here.
- The handles contribute no node, so touch-resize is unavailable to TalkBack **by design**; the documented
  alternative (`Make larger` / `Make smaller`, plus the four nudges and two rotates) is present and enabled.
- ℹ️ Adjacent 48 dp targets **overlap by ~4 px** on an element this small (corner and edge marks 122 px
  apart, targets 126 px). Pre-existing — the targets did not move in this change — and it does **not**
  breach WCAG 2.5.8 AA, whose spacing exception is measured on 24 CSS px circles (63 px here, comfortably
  clear at 122 px separation).

### 1.6 The grid's page number — the AA fix, measured on both grounds

| Theme | card ground (device) | glyph core | contrast | previously (`inkFaint`) |
|---|---|---|---|---|
| Dark | `(50,45,36)` | `(182,173,154)` | **6.14:1** | 3.31:1 |
| Light | `(247,242,231)` | `(93,85,73)` | **6.57:1** | 3.41:1 |

✅ **AA (4.5:1 at 9 sp) now passes in both themes on the device.**

⚠ **This pass falsified a figure this package had already defended.** The light card measures
`(247,242,231)` — the `paper` token exactly. The **3.47:1** light figure that had spread to four files was
computed against `(253,243,231)`, which is not the light card; it was a *filmstrip* reading that migrated
into a grid row. The grain that lifts the dark card by 15 levels is worth **3 levels** on light paper, so
light has no separate grained figure at all: **3.41:1 by both instruments**. Corrected in
[§12.5](../DECISIONS.md#adr-102-od47), the freeze banner, and the brief. Recorded here because the wrong
number survived two review rounds and one recomputation, and was only killed by a screen.

### 1.7 Theme invariance of the chrome

Light theme, same element, same numbers: mark standoff **5.52 dp**, ring **5.49 dp**, mark fill
`(255,246,232)` = `paper #FFF6E8`, border `(55,42,31)` = `ink #33261C`. Bench ground `(235,215,182)`,
sheet `(249,240,227)`.

The dark-theme capture independently re-confirms the island: the grid card behind it is `(50,45,36)` while
the Bench sheet in the same theme is paper.

**Pass 1 verdict: PASS.** One stated bound (§1.2), one figure corrected (§1.6), no defects.

---

## Pass 2 — First-Time User Verification

*Approached as someone who has never seen this editor: I tapped the thing on the page and looked at what
happened.*

**The selection reads as one object.** Eight small squares threaded onto a dashed rectangle — the squares
sit *on* the line, so the whole thing reads as a single frame with grab points, and "I can pull any of
these" is immediate. This is the change's real payoff and it survives contact with a screen: nothing about
it says "two rings, slightly apart", which is what the pre-ruling build drew.

**The edge handles now look deliberate.** Before the ruling they sat inside the ring on their own inset;
on the ring they read as members of the same set as the corners. A first-timer would not notice them as a
separate feature, which is correct — they are not one.

⚠ **Finding — `Font`, `Size` and `Ink` are dimmed on a text element, and the reason is invisible.** I
selected an empty text box and three of the five actions offered are unavailable. My first thought, before
I knew why, was *"is this element broken?"* — not *"there is no text yet to style"*. The platform agrees
they are disabled (§1.5), so this is honest, not misleading; it is just unexplained. Setting a font
*before* typing is a perfectly ordinary intent and the screen refuses it silently.

**Pre-existing, out of this change's scope, and not a blocker** — recorded because Pass 2's instruction is
to write down what felt wrong *before* knowing the reason, and this did. It belongs with the empty-state
work, not here.

**Nothing else on the Bench misread.** The keep-clear dashes and the focus wash over the page border are
both visible and both already booked to **P2** ([§12.7](../DECISIONS.md#adr-102-p1-review)); this pass adds
no new opinion about them.

**Pass 2 verdict: PASS**, with one Observation routed out of scope.

---

## Acceptance

Both passes pass, and they do not disagree. The ruling of §12.8 is **accepted on the device**: the marks
are on the ring at 5.52 dp, the targets are unmoved and drag unbiased, rotation is rigid, and the AA fix
holds in both themes.

Carried forward, not fixed here:

| Item | Category | Owner |
|---|---|---|
| Ring's 5.2 dp cannot be distinguished from 5.5 dp on a 2.625 px/dp raster (§1.2) | Stated bound of this pass | the CSS-derived unit assertion owns it |
| `Font`/`Size`/`Ink` dimmed without explanation on an empty text element (Pass 2) | Observation | empty-state work |
| Adjacent handle targets overlap ~4 px on small elements (§1.5) | Observation, pre-existing, not a 2.5.8 breach | — |
