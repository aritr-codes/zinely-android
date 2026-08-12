# Device verification — ADR-102 package P1 (Bench ground + sheet, V2 → V2.1)

**Date** 2026-08-12 · **Branch** `feat/v21-freeze-and-tokens` (uncommitted) ·
**Package** [ADR-102 §8 P1 / §12](../DECISIONS.md#adr-102-p1-handoff)

| | |
|---|---|
| Device | Samsung SM-A176B (`RZCYA1VBQ2H`) |
| OS | Android 16 / API 36 |
| Screen | 1080×2340, override density 420 ⇒ **2.625 px/dp** |
| Build | `zinely-0.9.0-beta.1-debug.apk`, `:app:installDebug`, package `com.aritr.zinely` |
| TalkBack | Samsung TalkBack `16.2.00.13` (installed; not driven — no `stateDescription` claim is made) |

Both passes were run per [CLAUDE.md § Device Verification](../../CLAUDE.md#device-verification-mandatory).
Every number below is **measured from the raster**, not read off a constant: the screenshots are sampled
pixel-by-pixel and the dp figures are `px ÷ 2.625`.

---

## Pass 1 — Developer Verification

### 1.1 The ground and the sheet

**Which theme each row was measured in is stated, because it is not the same for every row.** Only one
light raster exists (`11-light.png`) and it has a live selection, so the *undimmed* border, the page
number, the radii, the ring and the caret were measured in **dark only**.

| What | Frozen / token | Measured | dp | Theme | ✓ |
|---|---|---|---|---|---|
| Bench ground | `bench` `#211B15` | `(35,29,23)` | — | dark | ✓ † |
| Bench ground | `bench` `#EBD6B4` | `(235,215,182)` | — | light | ✓ |
| Sheet fill | `paper` `#FFF6E8` | `(250,241,227)` | — | **dark** | ✓ multiply grain |
| Page border | `1.5px` `ink` `#33261C` | `(53,40,29)`, 4 px | ≈ **1.5 dp** ‡ | dark | ✓ |
| Hard shadow | `inkLine` `#120E0A`, 4 dp | `(20,16,11)`, 10 px right of the sheet | **3.8 dp** | dark | ✓ |
| Hard shadow | `inkLine` `#33261C`, 4 dp | `(53,40,29)`, 10 px | **3.8 dp** | light | ✓ |
| Spine radius (top-left) | `--br-xs` 4 dp **outer** | inner edge settles over ~6 px | 2.3 dp inner | dark | ✓ § |
| Free radius (top-right) | `--br-md` 14 dp **outer** | inner edge settles over ~30 px | 11.4 dp inner | dark | ✓ § |

† The ground reads **2/255 lighter** than the token, not darker. That is not noise and not a defect: the
screen grain is `ZinelyV21Grain.ChromeBlend` = **soft-light**, which can lighten, while the *page* grain is
`PaperBlend` = multiply, which cannot — and the sheet does indeed measure darker than `paper`. The two
blends behaving differently in the right directions is itself a check.

‡ **At 2.625 px/dp, 1.5 dp and 1.6 dp are not separable** — 3.94 px and 4.20 px both raster to 4. This row
certifies "≈4 px of ink where the freeze puts a hairline"; it cannot discriminate the two widths, and the
same caveat applies to the ring in §1.3.

§ The token is the **outer** radius; the measurement is the *inner* (paper) edge, which is smaller by the
1.5 dp border under `box-sizing:border-box`. `4 − 1.5 = 2.5 dp` (6.6 px) and `14 − 1.5 = 12.5 dp` (32.8 px)
are what these rows should be compared against, and are what earns the ✓. Compared naively against 4 and
14 they would read as ~20 % shortfalls.

**The island is doing its job, and this is the measurement that proves it.** In **dark** theme the sheet
is `#FFF6E8` paper bordered in `#33261C` — the *light* values — while the room around it is `#211B15`.
The artifact does not dim ([OD-31](../DECISIONS.md#adr-098-od31)); the room darkens around it. The shadow
is the one token that correctly follows the room: `#120E0A` in dark, `#33261C` in light — which is exactly
why `inkLine` is excluded from the island ([§12.1](../DECISIONS.md#adr-102-island-v21)) and why copying the
Proof's wholesale `ProofLitPaper` would have reinstated **D-010** here.

### 1.2 The page number (moved top-right → bottom-right)

Measured at the sheet's bottom-right (dark theme): darkest glyph pixel `(108,88,70)` against `inkSoft`
`#6E5947` = `(110,89,71)` — within 2/255. Right inset ≈ 20 px = **7.6 dp** against a 9 dp constant, the
balance being the glyph's side bearing; glyph height 7.2 dp for a 9.6 sp bold face. Bottom-right ✓,
`inkSoft` ✓. The *position* is the P1 change and it is unambiguous; the inset figure is a sanity check,
not a transcription test.

### 1.3 The selection ring and the handles

Dark theme; the light raster's ring is under the focus wash and cannot serve.

| What | Source | Measured | ✓ |
|---|---|---|---|
| Ring stroke | freeze `1.6px` | 4 px — see §1.1 ‡, ≈4 px is all this density resolves | ✓ |
| Ring colour | freeze `var(--ink)` | `(53,40,29)` | ✓ |
| Ring dash | **implementation**, not the freeze | on 8–9 px, period 17 px | ✓ |
| Handle size | freeze `9px` square | x 223→246 = 24 px = **9.1 dp** | ✓ |
| Handle border | freeze `1.6px solid var(--ink)` | **5 px = 1.90 dp**, `(54,41,30)` | ≈ |
| Handle fill | freeze `var(--paper)` | `(255,246,232)` — `paper` exactly | ✓ |
| Halo (**retained against the freeze**) | not in the freeze | 4 px of `(254,251,247)` outside the border | ✓ |

Three honest qualifications, because the rows above are the ones most tempting to over-read:

- **The dash period is not in the freeze.** `v21-bench.html:145` says only `dashed`; CSS leaves the period
  to the UA. `on = off = 2 × stroke` is `SelectionChrome.SelectionOutlineDashDp`'s own definition, so a row
  claiming the freeze as its source would be citing the implementation back as its specification — the
  exact failure this branch's head commit is named for. The measurement confirms the implementation
  matches itself, which is worth knowing and is not a parity claim.
- **The handle border measures 1.90 dp against a 1.6 dp constant.** 5 px is one pixel of antialiased edge
  wider than 1.6 dp predicts (4.2 px). Marked `≈` rather than ✓; the raster cannot distinguish a 1.6 dp
  stroke from a 1.9 dp one at this density, so this is *consistent with* the constant, not proof of it.
- **The ring standoff is deliberately not tabled.** Measured, the ring's stroke centre is ~15.5 px
  (5.9 dp) outside the handle centres. Under the freeze's `box-sizing:border-box`, `.ring{inset:-6px}`
  and `.hnd{left:-10px}` put the ring and the handle centres within ~0.3 px of each other; the
  implementation instead centres each handle on the element's corner. **That is a ~5.5 dp departure from
  the freeze, not a confirmation of it**, and an earlier draft of this section used the departure as its
  ruler and ticked both. It is a real parity question and it belongs to whoever next opens
  `ResizeHandles.kt` — recorded here rather than resolved.

The halo composites to `0.7·255 + 0.3·paper ≈ 253` — within 1/255 of measurement, so the retained
`rgba(255,255,255,.7)` ring is present exactly as [§12.6 row 5](../DECISIONS.md#adr-102-p1-corrections)
rules it should be. Eight handles, per OD-11.

### 1.4 The caret is `jam`

Six rasters were taken of an open text session, but they are **two distinct observations, not six**:
`caret-1/4/5/6` are byte-identical to each other, `caret-2/3` are byte-identical to each other, and the
*only* difference between the two groups is the 264 caret pixels. So the caret is on in one observation
and off in the other — enough to establish that it blinks, and not more.

Where it is on: **264 px** of `#CF4A28` ±30 (mean `(209,80,46)`) at x 239–242 — 4 px, ≈1.5 dp under
§1.1 ‡ — × y 954–1019. `jam` ✓, width consistent with the frozen `1.5px` ✓.

### 1.5 The unselected-element dim measures 0.49

Two text elements, one selected. Darkest pixel of the **unselected** element vs the composite the freeze
predicts (`opacity:.5` ⇒ `0.5·element + 0.5·paper`):

| dimmed | selected (element ink) | paper | `.5` predicts | `.4` would predict | Δ from `.5` |
|---|---|---|---|---|---|
| `(129,124,117)` | `(0,0,0)` | `(253,243,230)` | `(126,122,115)` | `(152,146,138)` | ≤ 3 |

Implied α = `1 − 129/253` = **0.490**, so the honest statement is *0.49, decisively not 0.4* rather than
"exactly 0.5". The freeze's `.4` → `.5` value change is on the screen. The sampled pixel was checked to be
a glyph stem interior (a flat ≥12 px run) and not an antialiased edge. The wash is a **paper** composite
rather than a per-element alpha, which is why the dimmed glyph lands on paper's chromaticity instead of
going grey.

**One measurement, not two — and the fact that it is one is the finding.** The dimmed-element region of
the dark raster and of the light raster are **pixel-identical (0 differing pixels)**. That is not a
redundant second sample; it is direct evidence that the sheet is a light-theme island, since a
theme-dependent wash could not produce two identical regions.

### 1.6 Platform accessibility tree — read, not assumed

`adb exec-out uiautomator dump /dev/tty` (46 836 bytes, cross-checked against the screenshot).
**P1 changed no semantics and none were found changed.** What the tree *does* show is four pre-existing
defects, recorded here because a pass that only looks for its own change is not a pass:

1. **`Preview` is `android.view.View`, `clickable=false`** — [ADR-059](../DECISIONS.md#adr-059)'s Role→View
   defect, still open and still unbooked. The label and the action are not on one node.
2. **The whole nudge/transform pad reports `clickable=false` while `enabled=true`** — `Move left` …
   `Bring forward`, **nine** controls, eight of them 48×48 dp. This is the same *shape* of defect as
   ADR-058's `ReframeControls.ZoomButton`: the platform is told something the Compose tree does not say.
3. **The filmstrip splits label from action.** An earlier draft of this section said the thumbnails were
   *"non-clickable Views at 26×34 dp, below WCAG 2.5.8's 48 dp"*. **Both halves of that were wrong**, and
   the correction is worth more than the claim was:
   - The tree does carry `clickable="true"` ancestors — e.g. `[204,1653][290,1779]` = **33×48 dp** for
     page 2, 48×48 dp for page 8. The 26×34 dp node I measured is the *labelled inner* node, not the
     target.
   - **WCAG 2.5.8 (AA) is 24×24 CSS px, not 48 dp** — 48 dp is Material guidance and 44×44 is the AAA
     criterion 2.5.5. At either measured size the target *passes* 2.5.8.

   The real defect, which the false one hid: the clickable ancestor carries **no label** (`content-desc`
   empty) while the labelled child carries **no action** — ADR-059's Role→View shape once more — and the
   *current* page has no clickable node at all.
4. **`Bring forward` is clipped to 12 dp of width** at the right screen edge (`[1048,1080]`).

None are P1 regressions; all four predate it. Items 2–4 are not booked anywhere I can find and should be.

**Pass 1 verdict for P1's own surface: PASS.** Every value P1 transcribes was found on the device at the
frozen number, in both themes.

---

## Pass 2 — First-Time User Verification

*Approached as someone opening a zine app for the first time: make a page, put words on it, find the
page I want.*

### 2.1 What worked, and why it reads correctly

The Bench answers *"How do I change this page?"*. The sheet now sits **in a room** rather than on a
surface of its own colour, and the effect is the one §12.6 row 1 predicted without being able to show:
the page is obviously **the thing**, and everything around it is obviously not. Selecting an element
dims its neighbours and the eye goes where the work is. The handles read as *grips* — small, square,
outlined — where the old circles read as dots.

### 2.2 Finding — the page's own edge fades when you touch something (Recommended, P2)

With a selection live, the sheet's 1.5 dp border washes from `(53,40,29)` to `(155,144,132)`, i.e. it is
dimmed to 0.5 along with the unselected elements; so is the page number. **Written down before knowing
why**: *"the page went soft at the edges when I picked something up — did I pick up the page?"*

The cause is not new: `BenchFocusScrim` is a paper wash over the whole page with the selection punched
out, and it has always covered the page's chrome. What is new is that **you can see it**, because P1
replaced V2's 1 px `paperEdge` hairline (invisible either way) with a real ink border. A pre-existing
mechanism became legible, which is P1 revealing a defect rather than introducing one.

Not fixed here: the wash is P2's (`BenchFocusScrim` geometry), and narrowing it to the element layer is
a behaviour change that needs a ruling, not a value edit.

### 2.3 Finding — **the page grid shows the artifact as dark slabs in dark theme** (this is OD-47, device-confirmed)

Opening *All pages* in **dark** theme gives eight dark-grey rectangles. My page — which has two text
elements on it — showed **nothing at all**. Forty pixels below, the filmstrip renders the same eight
pages as paper-white with their content visible.

| Surface | dark theme | light theme |
|---|---|---|
| Page grid card | `(50,45,37)` | `(253,243,231)` |
| Filmstrip thumbnail | `(247,242,231)` | `(247,242,231)` |

One screen, one artifact, two renderings, and the unreadable one is the larger. The grid's question is
*"which page do I want?"* and in dark theme it cannot be answered from the grid — you must use the strip
underneath it, which is doing the grid's job better than the grid.

**This is [OD-47](../DECISIONS.md#adr-102-od47) and it is now measured rather than inferred.** The
mechanism is §12.5's: `BenchPageGrid.kt:287` fills each card with `colors.paper` under
`EditorScreen.kt:1364`'s **room** provider, so in dark theme `paper` resolves to the dark `#332B22`
rather than to the sheet's light value. It is P3's file, not P1's.

**P1 neither caused it nor worsened it, and the proof is a golden P1 did not touch:**
`bench_page_grid_dark.png` is unmodified on this branch and already renders cards at `(47,42,34)` = `#2F2A22`
on a `(32,29,24)` ground — §12.5's stated value, before P1. (P1's delta on the *light* grid golden is
3.61 % of pixels, every large difference confined to rows 0–51, the status strip — but a light golden
cannot settle a dark-theme defect, and citing it here would have been the wrong evidence for the right
conclusion.)

It is recorded because a Pass 2 that notices this and files it under "not mine" would be the exact failure
this document exists to prevent.

### 2.4 Observation — the Add sheet dims everything, including the artifact

The modal supply sheet scrims the whole screen, page included. This is an ordinary M3 modal scrim
covering chrome and artifact alike, not the artifact-specific dim OD-31 names — but OD-31 is worded
universally (*"the artifact does not dim, on any surface"*), so the boundary is the owner's to draw. No
change proposed.

**Pass 2 verdict for P1's own surface: PASS**, with one Recommended finding (§2.2) routed to P2.
**Pass 2 for the page grid: FAIL** — filed as OD-47, blocking P3, not P1.

---

## Acceptance

**P1's scope passes both passes.** Nothing in it was accepted on the strength of a summary: every
transcribed constant was re-measured on glass, and the two claims most likely to be wrong — that the
island lights the sheet in dark theme, and that the dim moved to `.5` — were the two that were
arithmetically checked rather than eyeballed.

**Where that acceptance is bounded.** Only one light-theme raster exists and it carries a live selection,
so the light readings are the ground, the shadow and the dim; everything else was read in dark. At
2.625 px/dp the raster cannot separate 1.5 dp from 1.6 dp (§1.1 ‡), so those two rows certify presence
and colour, not width. And §1.3 surfaced a genuine parity question rather than closing one: the handles
are centred on the element's corner, ~5.5 dp away from where `box-sizing:border-box` puts the freeze's.

Four items leave this pass **open and owed to someone else**:

| Item | Category | Owner |
|---|---|---|
| Handle centring departs from the freeze by ~5.5 dp (§1.3) | Parity question, unresolved | P1 follow-up / P2 |
| Focus wash covers the page border and page number (§2.2) | Recommended Improvement | P2 |
| Page grid renders the artifact dark in dark theme (§2.3) | **Release Blocker for P3**, open decision | OD-47 / P3 |
| Nudge pad `clickable=false`; filmstrip label/action split; `Bring forward` clipped (§1.6 items 2–4) | Technical Debt / a11y, unbooked | needs booking |

Evidence (screenshots and the accessibility dumps) is in the session scratchpad; it is not committed —
the repository carries the conclusions, not 20 MB of PNGs.
