# Two open decisions before P2 — OD-47 and handle placement

**Date** 2026-08-12 · **For** the owner · **Status** ✅ **RULED 2026-08-12 — all six rows of §3 accepted as
recommended.** The rulings live in [ADR-102 §12.5](../DECISIONS.md#adr-102-od47) (OD-47 closed as filed;
the consistency defect goes to P5) and [§12.8](../DECISIONS.md#adr-102-p1-handles) (handle marks to the
ring, hit targets unmoved, ring inset corrected to 5.2dp). This file is kept as the evidence behind them,
not as an open question.

Research brief on the two items [ADR-102 §12](../DECISIONS.md#adr-102-p1-handoff) left open. Each section
gives the verified facts first, then the options, then a recommendation. Evidence labels follow
[CLAUDE.md § Research standards](../../CLAUDE.md#research-standards).

**Both investigations changed their own question.** OD-47's stated premise turns out to be false; the
handle question rests on an ADR whose arithmetic is wrong; and checking a contrast figure I expected to be
a footnote turned up **an unrecorded WCAG AA failure in both themes** (§1.2). None of it is what I expected
to be writing.

---

## 1. OD-47 — the page grid's dark cards

### 1.1 What OD-47 says, and why its premise does not survive

[§12.5](../DECISIONS.md#adr-102-od47) files this under [OD-31](../DECISIONS.md#adr-098-od31) — *"the
artifact does not dim, on any surface"* — on the premise that the grid is *"a grid of the user's pages…
everything they have made at once"*.

✅ **VERIFIED — the grid draws no page content at all.** `BenchPageGrid.kt:263-349` paints a blank
rounded rectangle, an optional `COVER`/`BACK` label and a page number. No `EditorPagePreview`, no
`PagePreview`, no `SceneRenderer`. There is no artifact on that screen to dim. (§12.5's premise, quoted
exactly, is *"a grid of the user's pages **is** the artifact, and it is the one screen that shows the user
**every page** they have made at once."*)

✅ **VERIFIED — both frozen files say so explicitly, and one of them says it about this exact question.**
`v2-bench.html:138`, an OD-23 amendment banner:

> **THE PAGE GRID IS STILL NOT AMENDED. `.pgcell` draws no page content, so it has no artifact to dim.**

That banner was written when the *filmstrip* was given its light-theme island (OD-23, after its thumbnails
were measured at **1.21:1** in dark). The grid was considered at the same moment and deliberately left out,
for the reason OD-47 now overlooks.

✅ **VERIFIED — both freezes paint the card in the room's `--paper`.** `v2-bench.html:439`
(`.pgcell{background:var(--paper)}`) and `v21-bench.html:434` (`.pgc{background:var(--paper);
border:1.5px solid var(--ink);box-shadow:3px 3px 0 var(--ink-line)}`). Neither declares a light island.
**So making the cards light is a departure from the frozen specification, not a parity fix.**

⚠️ **Correction to my own device report.** [§2.3](../reviews/2026-08-12-adr-102-p1-device-verification.md)
named the mechanism as V2.1's `paper` `#332B22`. `BenchPageGrid.kt` contains **zero** `ZinelyV21`
references — it is still V2-era, and the value is V2 dark `paper` `#2F2A22` (`ZinelyV2Colors.kt:220`).
The measured `(50,45,37)` is that plus the soft-light chrome grain. ADR-102 §12.5 had it right; my report
did not.

### 1.2 So is there a defect at all?

**Yes, but not the one filed.** What the device Pass 2 actually recorded was not *"the artifact dims"* — it
was *"one screen shows the same eight pages two ways, and the smaller one is the legible one."* That
finding stands on its own:

| Surface | dark | light | draws page content? |
|---|---|---|---|
| Page grid card | `(47,42,34)` `#2F2A22` ᵍ | `(253,243,231)` ᵈ | **no** |
| Filmstrip thumb | `(247,242,231)` ᵈ | `(247,242,231)` ᵈ | **yes** — real `SceneRenderer` output |

ᵍ from the golden `bench_page_grid_dark.png`; ᵈ measured on device. The device read the dark card as
`(50,45,37)` — the same token plus the soft-light chrome grain, which lightens. Two provenances, labelled,
because an earlier draft of this table mixed them silently and copied the filmstrip's light value into the
grid's row.

The strip is lit because OD-23 ruled it must be; the grid is not because the same ruling deliberately
excluded it. On a device, forty pixels apart, that reads as a malfunction whatever the ledger says.

#### ⚠️ A separate, previously unrecorded defect, found while checking whether an accessibility argument existed

✅ **VERIFIED — the grid's page number fails WCAG 1.4.3 AA, in *both* themes.** `inkFaint` on the card:

| Theme | foreground | background | ratio | AA needs |
|---|---|---|---|---|
| Dark | `#857C69` (`ZinelyV2Colors.kt:227`) | `#2F2A22` (`:220`) | **3.45:1** (3.31:1 against the grained `(50,45,37)`) | 4.5:1 |
| Light | `#8C8269` | ~~`(253,243,231)`~~ → `(247,242,231)` = `#F7F2E7`, the token exactly (device, 2026-08-13) | ~~**3.47:1**~~ → **3.41:1** | 4.5:1 |

The text is `BenchCellNumberSize = 9.sp` (`BenchPageGrid.kt:91`), nowhere near 1.4.3's large-scale threshold
(≥18 pt, or ≥14 pt bold), so **4.5:1 applies and both themes miss it**. This is a real AA failure, it is
nobody's open decision, and it is not booked anywhere. **It should be filed on its own**, not carried as a
footnote inside an argument about theming.

🟦 **And it is exactly why the accessibility carve-out cannot license OD-47's fix** — but not for the reason
I first wrote. The reason is not that the contrast is "marginal": **lighting the card does not fix it.**
The light theme already renders the card at `(253,243,231)` and the number still fails at 3.47:1. A
post-freeze accessibility justification has to be *for the change being made*, and this change does not
make the accessibility number better. So OD-47 needs an owner ruling and an HTML amendment, exactly as
OD-23 did — and the contrast defect needs fixing whichever way OD-47 goes.

(For scale: the strip's defect that forced OD-23 was **1.21:1** — user content going invisible. This is a
different order of problem, and a different kind: legibility of a *label*, not of the artifact.)

### 1.3 What the industry does — and why it does not settle this

✅ **VERIFIED, and genuinely split** (Word, Apple and Adobe quoted verbatim; Figma paraphrased — its page
reads *"Use the theme settings you change the appearance of the Figma interface"*, typo and all).
Word darkens the document and then ships a ribbon-level escape,
documenting the invariant in its own words: *"Regardless of your Dark Mode settings, your document will
print with the light mode page color"*
([Microsoft](https://support.microsoft.com/en-us/office/dark-mode-in-word-e17b79a3-762f-4280-a81c-a15a859a693a)).
Google Docs mobile does the same with a per-document *"View in light theme"*
([Google](https://support.google.com/docs/answer/9955476?hl=en&co=GENIE.Platform%3DAndroid)). Figma goes
the other way: theme changes the appearance of the Figma *interface* and is device-local, while the
canvas colour is **document data** shared with everyone
([Figma](https://help.figma.com/hc/en-us/articles/5576781786647-Change-themes-in-Figma)). Acrobat's dark
themes are enumerated as chrome only, and replacing *page* colours lives under Accessibility preferences
([Adobe](https://helpx.adobe.com/acrobat/desktop/get-started/preferences-and-settings/change-display.html)).

✅ **VERIFIED — platform guidance leans toward not theming user content.** Apple's Dark Interface criteria
carve out user content in terms: *"dark by default for all common tasks, **excluding any third-party or
user-generated content**"*, and *"when Smart Invert is enabled, make sure the colors in the media don't
appear inverted"*
([Apple](https://developer.apple.com/help/app-store-connect/manage-app-accessibility/dark-interface-evaluation-criteria/)).
Android makes force-dark opt-in with a documented per-view exclusion
([Android](https://developer.android.com/develop/ui/views/theming/darktheme)).

**But none of it reaches this case**, and saying so is the point: every source above is about the
*document*. A blank numbered card is not the document — it is a card that *stands for* a page, the way a
Library cover stands for a zine. 🟨 **ASSUMPTION** — no vendor documents what a blank page-navigator
placeholder should do under a dark theme; I found nothing, and would rather report the gap than dress a
Figma quote up as an answer to a question Figma was not asked.

### 1.4 Options

| # | What | Cost | Risk |
|---|---|---|---|
The grid has **three** goldens — `bench_page_grid_dark`, `_light`, `_open_light` — though only the dark one
changes under a cell-scoped fix (light room == light island).

| # | What | Cost | Risk |
|---|---|---|---|
| **1** | Cell-scoped `BenchStudio.sheetIsland` (8 tokens) in `BenchPageGrid.kt`, shadow left on the room | ~4 lines, 1 golden re-recorded | low **as code** — but it *is* the freeze departure §1.1 identifies, so it needs the ruling first; and P5 discards it |
| **2** | Same island via `CompositionLocalProvider` around the cell body | ~6 lines, 1 golden | same freeze departure; extra scaffolding that only pays off under #4 |
| **3** | Delete the opt-out provider at `EditorScreen.kt:1387` | −4 lines | **wrong** — lights the grid's *chrome* too (`ink` header, `matchaText` `Done`) on a `desk` ground, and deletes one of the four opt-outs §9 names as the do-not-move list |
| **4** | Make the cell draw the real page, as the strip does | medium; new render path, call-site change, goldens, HTML amendment | perf (N live tapes); a visual redesign after freeze |

⚠️ **Sequencing fact that changes the answer.** `BenchPageGrid.kt` is still V2-era (zero `ZinelyV21`
references), and ADR-102 §8's table assigns `BenchPageGrid` to **P5**. 🟨 **Derived, not cited** — §8's P5
row says only *"Page grid — BenchPageGrid … ⚠️ and the grid dims the artifact today"*; the *extent* of the
rewrite is my reading of `v21-bench.html:434-437` and `:701` against the current file, and it is total:
3/4 aspect (not .66), `1.5px ink` border, a `3px 3px 0 inkLine` hard shadow, `leafTint`/`leafText` for the
current cell, and **no Cover/Back label** (the frozen grid button emits a bare number). On that reading,
any fix landed today is thrown away by P5 — and P5 needs the same ruling regardless.

⚠️ **This reverses two recommendations of record, and I should say so rather than let it pass.**
[ADR-102 §12.5](../DECISIONS.md#adr-102-od47) says verbatim *"**Recommendation: fix in P1 as a sibling**,
not in P5."* My own device report says *"It is P3's file"* and *"blocking P3, not P1."* **Both are wrong,
and the second is mine**: §8's table puts `BenchPageGrid` in **P5**, so "P3" is a plain error I introduced
on 2026-08-12 and am correcting here. §12.5's "P1 as a sibling" was written before P1 shipped and before
the premise in §1.1 was known; P1 is now committed, so "as a sibling" is no longer available on its own
terms.

### 1.5 🟦 RECOMMENDATION

**Close OD-47 on its stated grounds and re-open the real question, then answer it in P5 rather than now.**

1. **OD-47 as filed is a misdiagnosis** — it cites OD-31 against a surface that draws no artifact, and
   `v2-bench.html:138` already ruled that exact point. Closing it on those grounds is not a dismissal; it
   is what keeps OD-31 meaning something.
2. **The real finding is representational consistency**, and it is worth fixing: a card standing for a
   sheet of paper should look like paper on any desk, and the strip beside it already proves the app
   agrees. That is a **product** call and it is yours.
3. **Take it HTML-first, in P5.** If you rule for paper: amend `v21-bench.html`'s `.pgc` with a light-island
   banner (the OD-23 route, which is precedent for precisely this move), then implement in P5 with the
   V2.1 8-token island — cell-scoped, shadow on the room. No throwaway code, no freeze violated, one
   golden re-recorded once instead of twice.
4. If you rule the other way — cards are chrome and follow the room — that is defensible too, and then the
   honest fix is the *strip*, not the grid: it would be the outlier. I do not recommend this, because the
   strip's lit thumbnails were ruled on measured contrast evidence and the grid's cards were not.

**Do not take option 3 under any ruling.** Everything else waits for P5.

---

## 2. Handle placement

### 2.1 The arithmetic

Two separate facts, kept apart because an earlier draft ran them together: `.el` has no padding and no
border, so the absolutely-positioned containing block *is* the element box; and `*{box-sizing:border-box}`
(`v21-bench.html:137`) makes `.hnd`'s `width:9px` its **outer** size, which is what puts the centre at −5.5.
`box-sizing` is **irrelevant to `.ring`**, which sets all four insets with `width:auto` and stretches to
fit regardless.

```
.hnd{width:9px}  .hnd.tl{left:-10px;top:-10px}
   centre = −10 + 9/2 = −5.5px, diagonally outside the corner        (all four corners, mirrored)

.ring{inset:-6px;border:1.6px}   a CSS border paints INSIDE the border box
   stroke centre-line = −6 + 1.6/2 = −5.2px
```

✅ **VERIFIED — the freeze puts the handle centres and the ring's stroke line 0.3 px apart.** The handles
are *threaded on* the ring. One figure.

Compose:

```
handles  SelectionChromeGeometry.kt:64-65  →  (local.x·w/2, local.y·h/2)  = the box corner, offset 0
         `inflateDevicePx` exists, but only on the OUTLINE path — the handle path never calls it
ring     SelectionChrome.kt:86,112         →  6.dp outward, and Compose strokes are CENTRED on the path
```

| | handle centre | ring stroke centre | gap |
|---|---|---|---|
| Freeze | −5.5 px | −5.2 px | **0.3 px** |
| Compose | 0 dp | −6 dp | **6 dp** = 15.75 px @ 2.625 |

Device measured 15.5 px. Computed 15.75 px. ✅ **The defect is confirmed, and it is 5.5 dp.**

**It is not a rounding difference — it is a different mark.** The freeze draws one figure; the shipped
chrome draws two concentric ones. That is what my Pass 2 eye caught without being able to name it.

*(This section confirms the device report rather than adding to it — the 15.5 px, the 0.3 px and the
~5.5 dp are all already recorded there. The new material is §2.2.)*

### 2.2 Two further findings, neither of which the device pass could see

✅ **VERIFIED — the ring is 0.8 dp out as well.** The freeze's `inset:-6px` is the ring's *outer edge*
(stroke centre −5.2 px); Compose treats 6 dp as the stroke *centre*. And
`BenchStudioSurfaceTest.kt:294-315` — **a test I wrote three days ago** — enshrines the misreading by
asserting the constant equals the raw CSS number. It is the ADR-073 trap one level deeper: I compared
against the declared CSS and still read it wrong, because I read the number and not the box model.

✅ **VERIFIED — [ADR-091](../DECISIONS.md#adr-091) row 2.6's arithmetic is false**, and that is why no test
ever existed. It states verbatim that *"the ∓10px offset on a 13px circle centres it on the box corner;
`handleDevicePx` already returns that centre, so the frozen offset is **satisfied by construction**"*.
Against V2's own numbers: `−10 + 13/2 = −3.5px`, not 0. Centring on the corner needed `−6.5px`. **The
claim was wrong by 3.5 px in V2**; the V2.1 re-skin kept `-10px` and shrank the mark 13→9 px, growing the
error to 5.5 px — and P1 transcribed the new *size* without re-checking the *position* claim it inherited.
*"Satisfied by construction"* is a phrase that ends testing, so it had better be true.

### 2.3 What is and is not already ruled

✅ **OD-11 does not reach this.** It is a **capability-preservation** ruling (*"no existing editor
capability is removed"*) — the eight-vs-four handle count. Position is not a capability. `D-036` is
likewise count-only, and its owner disposition (2026-08-02) was *"documentation / spec alignment only…
fences nothing"*, recommending **the freeze be amended to draw eight**.

⚠️ **The four edge handles are entirely unspecified.** The freeze has no `.hnd.t/.r/.b/.l` rule at all;
Compose puts them at the edge midpoints, offset 0 (`SelectionChromeGeometry.kt:65`). If the corners move
outward, where the edges go is **an unruled product question**, not an implementer's choice.

### 2.4 The trap that makes this dangerous to fix carelessly

⚠️ `centerPx` does three jobs: it places the 48 dp hit box (`ResizeHandles.kt:191`), keys the
`pointerInput` (`:200`), **and seeds the drag accumulator** (`:202`/`:206`) that `TransformMath.resizeByHandle`
treats as *the corner's new position* (`TransformMath.kt:76-78`). **Move `centerPx` and every resize jumps
5.5 dp on frame one.** Nothing compensates, and `ResizeHandlesTest.kt:105-108` asserts only
`width/height > 30.0` — a loose inequality that cannot fail on a seed shift. So the regression would ship
green.

The safe seam: keep `centerPx` geometric (hit target and drag stay correct) and displace **only the drawn
mark**. Trade to state plainly: the 48 dp target then centres on the corner, not on the mark.

### 2.5 Accessibility — checked, and it turns out to be **neutral**

✅ **VERIFIED — the thresholds, stated exactly, because they are the ones everyone misquotes.** WCAG 2.2
**2.5.8 (AA) is 24×24 CSS px**, with a *Spacing* exception: undersized targets pass if a **24 px-diameter
circle centred on the bounding box of each does not intersect another target, or another undersized
target's circle**
([W3C](https://www.w3.org/WAI/WCAG22/Understanding/target-size-minimum.html)). **2.5.5 (AAA) is 44×44**
([W3C](https://www.w3.org/WAI/WCAG22/Understanding/target-size-enhanced.html)). Apple asks 44 pt
([Apple](https://developer.apple.com/design/tips/)); Android/Material 48 dp
([Google](https://support.google.com/accessibility/android/answer/7101858?hl=en)).

✅ **VERIFIED — drawing small and hitting large is explicitly blessed**, which is the permission the 9 dp
mark with a 48 dp box already relies on: *"It can be beneficial to provide an option to increase the active
target area without increasing the visible target size"* (W3C, above), and Google's own worked example of
a 24 dp icon in a 48 dp target. Compose expands sub-48 dp targets automatically and **warns they can
overlap neighbours** ([Android](https://developer.android.com/develop/ui/compose/accessibility/api-defaults)).

**The relevant point for us — and a first draft of this section got it backwards.** I wrote that ADR-091
row 2.7's measured overlap (corner and edge targets **35 dp apart while each claims 48 dp** on a 70 dp-tall
element) argues *for* moving the handles outward. It does not, and §2.4 is the reason: **the safe seam
moves only the drawn mark and deliberately leaves the 48 dp target on the corner.** Under Option A as
specified, the target separation does not change at all. The benefit I claimed exists only on the path
§2.4 warns against, which is a good sign that I reached for it.

Two further facts, both of which flatten the argument rather than sharpen it: 35 dp **already passes**
2.5.8's Spacing exception (24 CSS px), and **neither** the current nor the moved arrangement meets
Material's 48 dp. So accessibility does not distinguish the options. It licenses the small drawn mark, it
constrains the *stroke* (IA §C.4's 3:1 over any photo, which is why the halo is retained), and on placement
it is **neutral**. The case for Option A has to stand on design intent alone — §2.8.

### 2.6 Placement conventions — an honest gap

🟨 **ASSUMPTION / ⚠️ DISPUTED — no vendor documents where a corner handle sits relative to the bounding
box.** Not Figma, not Sketch, not Adobe, not Microsoft. Microsoft documents what handles *do*, never their
geometry ([Microsoft](https://support.microsoft.com/en-us/office/resize-a-picture-shape-text-box-or-other-object-f9d717a8-b0b2-41b4-85be-e34ba28a949a)).
The nearest thing to a cross-tool survey ([bjango](https://bjango.com/articles/designtoolcanvashandles/))
maps *interaction hit zones* and **never raises placement relative to the bounding box at all** — so it
cannot be cited as declining to resolve a question it does not ask. (An earlier draft did exactly that, and
also attributed a "worst outcome" verdict the article does not give.) **Any claim of an industry convention
on centred-vs-outside would be manufactured, so I am not making one.**

What tools *do* agree on is the adjacent problem: small selections. Sketch enlarges the bounding box and
hides handles that will not fit ([Sketch](https://www.sketch.com/docs/designing/layer-basics/resizing-and-rotating-layers/));
Figma hides them; Affinity Designer, Illustrator and Photoshop let them stack (bjango, above, which
observes this without ranking it).
🔭 **FUTURE** — whichever way this ruling goes, the small-selection degradation is unspecified in our freeze
too, and four 48 dp targets on a small element collide long before they collide with anything else.

### 2.7 Options

| # | What | Cost | Risk |
|---|---|---|---|
| **A** | Move Compose to the freeze: drawn mark out 5.5 dp, ring corrected 6→5.2 dp | mark offset + ring constant + 2 goldens + the missing resize test | needs a ruling on the four edge handles; §2.4 trap if done wrong |
| **B** | Amend the freeze to match the code (`-10px` → `-4.5px`), and draw eight handles while there | lowest — zero code | reverses HTML-first; discards a design that *improved* between freezes |
| **C** | Accept as a documented departure; fix only the ring's 0.8 dp | near zero | a departure row whose reason column reads *"we did it this way"* |

### 2.8 🟦 RECOMMENDATION

**Option A, and rule the edge handles onto the ring.**

The case rests on design intent, not on accessibility (§2.5 is neutral) and not on industry convention
(§2.6 is a gap). The suggestive fact is that the gap **tightened between freezes**: V2 put the handles
2.75 px *inside* the ring, V2.1 puts them 0.3 px *outside* it. 🟨 **Stated as suggestive, not proof** — a
first draft called it decisive, and it is not. The convergence falls out of two independent changes (the
mark shrank 13→9 px while `-10px` was kept; the ring moved −7→−6 px as its border went 1.5→1.6 px), the sign
of the offset flips, and keeping the mark at 13 px would have left a 1.7 px gap. What the numbers support
is *"the V2.1 figure is markedly more coherent than V2's"*, not *"somebody intended it."*

That is still enough, because the alternative is worse: **B amends a frozen design to ratify a
transcription error** — one this brief has now traced to a false line of arithmetic in ADR-091 (§2.2) —
and it reverses the handbook's HTML-first rule to do it. A is the only option under which the mark on the
screen and the specification agree.

The ruling I need from you is the one the freeze does not cover: **should the four edge handles move to the
ring too** — 5.5 dp along each edge's outward normal — so all eight sit on one figure? Corners-on-the-ring
with edges-on-the-box would look arbitrary. I am asking this on its own merits and **not** claiming D-036
supports it: D-036 is a *count* ruling, its recommendation (a) is about drawing eight handles, and it says
nothing about where any of them go.

**Owed regardless of which option you choose**, all implementation and none of it design:

1. **[ADR-091](../DECISIONS.md#adr-091) row 2.6's arithmetic is wrong** and needs a correction notice. Not
   superseded — *wrong*, and it is why no test exists.
2. The ring's 0.8 dp misreading, and `BenchStudioSurfaceTest.kt:294-315` which pins it.
3. A resize-regression test with a tight assertion; the current `> 30.0` cannot fail on a seed shift.
4. Stale citations, three documents' worth: ADR-091 row 2.6 cites `:159` for a rule now at `:297` (rows
   2.3/2.5 are off by ~98); `D-036` (`V2-SPEC-DEFECTS.md:4285`) carries the same stale `:159`; ADR-102 §9
   lists the grid's opt-out at `EditorScreen.kt:1364` when it is at `:1387`; and
   `BenchSelectionAppearanceTest.kt:107,133` still cite V2's `7 →  0` inset and 1.5 dp stroke.
5. **The page-number contrast failure from §1.2** — 3.45:1 dark / 3.47:1 light against 1.4.3's 4.5:1, in a
   file nobody has booked. It is unrelated to either decision here and should not wait on them.

---

## 3. What I need from you

| # | Decision | My recommendation |
|---|---|---|
| 1 | OD-47: close as filed (no artifact is drawn)? | **Yes** — the premise is refuted by `v2-bench.html:138` |
| 2 | Should a grid card look like paper on any desk? | **Yes**, on consistency grounds — but it is a product call, it departs from both freezes, and it has no accessibility licence |
| 3 | If yes: HTML amendment now, implementation in **P5**? | **Yes** — P5 rewrites the file anyway; fixing it today is throwaway code |
| 4 | Handles: move Compose to the freeze (A), amend the freeze (B), or document the departure (C)? | **A** — the gap tightened between freezes, so it is a design, not a drift |
| 5 | If A: do the four edge handles move to the ring as well? | **Yes** — otherwise the figure is half-drawn |
| 6 | The grid page number fails AA at 3.45:1 / 3.47:1 (§1.2). Fix independently of OD-47? | **Yes** — it is a real AA failure, it is in neither theme's favour, and it should not wait on a theming ruling |

Items 1–5 of §2.8 proceed under any answer, and I would take them first: they are corrections of record.
One is a test I wrote three days ago that is currently pinning the wrong number, and one is an ADR whose
arithmetic has been false since V2 — which is precisely why nothing ever failed.
