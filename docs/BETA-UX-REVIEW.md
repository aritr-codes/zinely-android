# Zinely UX Review

> This file collects **first-time-user (Pass 2) reviews**, each stamped with the build it ran against.
> The first covers `0.9.0-beta.1`; later sections cover later builds and say so in their own headers.

## 0.9.0-beta.1

Reviewed 2026-07-21 against the signed `0.9.0-beta.1` build on a physical Galaxy A17 5G (Android 16),
from the position of a first-time user making their first zine. Evidence is seven device screenshots
plus the repository.

> **Status of this document.** A time-stamped review, deliberately *not* a source of truth. Approved
> findings move into [PRD](PRD.md) (scope), [ROADMAP](ROADMAP.md) (phasing) and ADRs (decisions) per the
> [Documentation Rule](../CLAUDE.md#documentation-rule-mandatory); this file then just records what was
> seen and when.

---

## 0. The one-sentence finding

**You cannot see your zine.** You can see one page at a time while editing, and a schematic of where the
folds go — but at no point does Zinely show you the thing you are making. Everything else in this review
is smaller than that.

---

## 1. Three defects, root-caused

The brief reported five P0s. Three of them are code defects with identified causes; the diagnoses differ
from the symptoms in ways that change the fix.

### D1 — The empty page draws its invitation off the paper · **BLOCKER** · cost **S**

**Evidence:** screenshot #3. After undo, "A fresh page. What goes he[re]?" and "…from the supplies below"
run off the right edge of the sheet and are clipped by the screen.

**Cause.** The canvas is a `Box(Modifier.fillMaxSize())`. The paper is drawn inside it *top-left
anchored* at the fitted scale, so on a portrait page it occupies only the left ~80% of the canvas. The
empty state is placed with `Modifier.align(Alignment.Center)` — centred on the **canvas**, not on the
**paper** ([EditorScreen.kt:687](../feature/editor/src/main/kotlin/com/aritr/zinely/feature/editor/EditorScreen.kt#L687)).
So it is centred on a box the paper does not fill, and overflows.

**Broken mental model.** The paper is the object; the dark surround is "the desk". Copy that straddles
the edge of the paper says *the app has lost track of where the page is* — which is precisely the
inference a first-time user draws, and why it reads as corruption rather than as a misplaced label.

**Smallest fix.** Place the empty state inside the paper box, or align it to the paper's bounds instead
of the canvas'. One-line change.

### D2 — Paper and content can be drawn at two different scales · **BLOCKER** · cost **S**

**Evidence:** screenshots #2, #4 and #6 show the sheet at visibly different widths under the same
toolbar, and in #6 the photo and the text box extend past the paper's right edge.

**Cause.** Two scales exist and only one of them is live.
- The **paper backing** is sized from `scale`, recomputed every measure from `BoxWithConstraints`.
- **All content** — `EditorPagePreview`, the gesture surface, the semantics layer — reads
  `uiState.view.screenPxPerPt`, which is only refreshed by `dispatch(Intent.SetViewport(...))` **when
  the interaction is `Idle`** ([EditorScreen.kt:553-557](../feature/editor/src/main/kotlin/com/aritr/zinely/feature/editor/EditorScreen.kt#L553)).

That deferral is deliberate and correct — updating the viewport mid-gesture re-keys
`pointerInput(screenPxPerPt, …)` and strands the session. The defect is that the **paper did not join the
deferral**. Any canvas resize while a session is open — most obviously the soft keyboard opening during
an inline text edit — moves the paper immediately and leaves the content behind, and it stays wrong until
the session ends.

Contrast [EditorPageStrip.kt:226-240](../feature/editor/src/main/kotlin/com/aritr/zinely/feature/editor/EditorPageStrip.kt#L226):
the thumbnails compute one local scale and use it for *both* the output box and the render, so they
cannot disagree. The strip has the right shape; the main canvas doesn't.

**Smallest fix.** Size the paper from `uiState.view.screenPxPerPt` too. Then the two agree by
construction — both lag together during a session, which is invisible, instead of diverging.

**Related smell (not the defect).** Because the fit is recomputed against the free canvas height, the
page also visibly resizes whenever the toolbar changes height (an element gets selected, the transform
row appears). The page "breathes" as you work. Worth fixing after the beta by fitting to a stable
container rather than the residual space.

### D3 — The zoom stepper is inert but never looks it · **HIGH** · cost **S**

**Evidence:** screenshot #7 — "Whole photo" is selected, zoom reads 100%, and the reported symptom is
that zoom does nothing "after reopening".

**Cause.** Not a reopen bug and not persistence. Two rules make zoom a no-op, and neither is visible:
1. `MIN_ZOOM = 1.0` ([FramingDraft.kt:48](../feature/editor/src/main/kotlin/com/aritr/zinely/feature/editor/FramingDraft.kt#L48)),
   so in **Fill** you can never go below 100% — by design, since a smaller crop would gap the frame.
2. In **Whole photo**, zoom is ignored entirely (`FrameFit.WHOLE → zoom = MIN_ZOOM`).

`ZoomButton` takes no `enabled` parameter, so **−** and **+** are always painted as live controls
([ReframeControls.kt:217](../feature/editor/src/main/kotlin/com/aritr/zinely/feature/editor/ReframeControls.kt#L217)).

The "after reopen" framing is a red herring with a real cause underneath: a newly placed photo is
`Fit.FILL`, where zooming *up* works. A photo you previously set to "Whole photo" reopens in `WHOLE`,
where nothing works. Same code, different persisted state — so it looks like reopening broke it.

**Broken mental model.** A button that is lit, tappable and haptic must do something. When it doesn't,
the user concludes the *app* has frozen — not that the control is unavailable. This is the single
cheapest confidence loss in the build.

**Smallest fix.** Give `ZoomButton` an `enabled` flag; disable **−** at `MIN_ZOOM`, **+** at `MAX_ZOOM`,
and both in Whole-photo mode — exactly the pattern the Type bar's size stepper already uses. The Type bar
proves the codebase already knows how to do this.

**Research note, in Zinely's favour.** ✅ A zoom floor at exact coverage is the correct and universal
behaviour for a fill-style cropper (Instagram, Canva) — the constraint is right, only its silence is
wrong. ✅ Figma's vocabulary is the clearest in the industry (**Fill** *"may clip the image"* vs **Fit**
*"ensures the entire image is visible"*), and Zinely's *Fill / Whole photo* labels are a good plain-English
rendering of it. ✅ Zinely is also right to avoid the word "crop": Figma has to explicitly reassure users
that *"the cropped area does not get deleted"*, whereas a verb about the **frame** — reframe, reposition —
reads as safe. **Keep the vocabulary; fix the button.**

---

## 2. Preview is not a preview · **BLOCKER (naming) / V1 (feature)** · cost **S** then **M**

**Evidence:** screenshot #5. "Preview" leads to *Step 1 of 3 · The sheet*: an imposition diagram with
grey page numbers, fold lines, one cut line, and Front/Back cover cards that are **blank**.

**This is not a rendering bug.** It is a documented deferral —
[ProofSheet.kt:96](../feature/editor/src/main/kotlin/com/aritr/zinely/feature/editor/ProofSheet.kt#L96):
*"cells carry the engine-derived page number, a schematic stand-in — real per-panel artwork needs the
document tree threaded through the Proof VM seam (deferred)."* The build is behaving as designed.

**Why it feels wrong.** The user has just placed a photo and typed a caption. They tap the only thing
labelled "Preview". They are shown eight empty rectangles with numbers, four of them upside-down. The
honest reading from the user's chair is *"it lost my work."* The screen is a genuinely good explanation
of **how a folded sheet works** — it is simply answering a question nobody asked yet.

**The industry has a name for the distinction Zinely is missing.** ✅ The print world separates **reader
spreads** from **printer spreads**, and the rule is stated as a slogan: *"Always design in reader spreads.
Always print from printer spreads."* ([PDF Press](https://pdfpress.app/blog/how-to-create-zine)). InDesign
keeps the same split — you author in spreads, and imposition is a separate *Print Booklet* step
([Adobe](https://helpx.adobe.com/indesign/desktop/print/print-booklets/impose-documents-for-booklet-printing.html)).
Zinely currently offers only the printer spread.

✅ **Blurb's triad is the cleanest model to copy**: **Edit** shows production furniture (trim areas on),
**Preview** strips it — *"there are no trim areas visible in Preview mode"* — and **Review** lists
pre-print warnings that *"you can choose to ignore"*
([Blurb](https://support.blurb.com/hc/en-us/articles/360015822551-What-s-the-difference-between-BookWright-s-Preview-feature-and-my-book-s-online-preview-on-Blurb-s-website)).
The advisory-not-blocking framing of Review is worth stealing wholesale.

**The three things these are, which the app currently conflates into one word:**

| Mode | Question it answers | Zinely today |
|---|---|---|
| **Read** | "What did I make?" | ❌ does not exist |
| **Print sheet** | "What comes out of the printer?" | ⚠️ exists as a schematic, no artwork |
| **Fold** | "How do I turn that into a book?" | ✅ exists, and is good |

**Read mode is a differentiator, not catch-up.** ✅ Zeenster — the closest comparable — has **no reading
mode at all**: a canvas editor plus an "Export Zine for Printing" modal, and the imposed sheet is shown
only at the moment of committing to print
([source](https://github.com/virgilvox/zine-maker/blob/main/docs/03-user-guide.md)). Page-by-page Preview
is standard in *photo-book* tools and absent from *zine* tools. On a phone, a swipeable 8-page reader is
the natural idiom and nobody in this category has it.

**Smallest fix for the beta (S).** Do not build a reading view this week. **Rename.** The entry point
becomes *Print & fold* (or *Get it printed*), and the step-1 heading keeps explaining the sheet. That
alone removes the false promise. Add one line to the tester note: *"there is no way to page through your
finished zine yet — that's the top of the V1 list."*

**The real fix (M, V1).** A **Read** mode: a horizontal pager, one page per screen, in reading order
1→8. Cost is lower than it looks — `SceneRenderer.render(page, …)` + `PagePreview` already render any
page at any scale, and `EditorPageStrip` already walks all pages in order. Read mode is mostly a
`HorizontalPager` over components that exist.

Then: **Preview → Read · Print sheet · Fold**, with Read as the default landing tab.

---

## 3. Library — it doesn't look like my work · **HIGH** · cost **M**

**Evidence:** screenshot #1. Wordmark "Zinely.", an "On this device" pill, "Your zines 1", and a card
showing a *generated* coral shape — while the zine's actual first page is a photo of fruit.

**Cause.** The cover is synthesised from a recipe (archetype + riso inks,
[ShelfCoverRecipe.kt](../feature/editor/src/main/kotlin/com/aritr/zinely/feature/editor/ShelfCoverRecipe.kt)),
because the shelf only has `ProjectSummary` — id, title, format, paper, timestamps. **No document, no
thumbnail.**

**Broken mental model.** In every creative gallery the thumbnail *is* the project. The generated cover
answers "what template was this?" when the only question a returning user has is "which one is mine?".
With one project it is merely odd; with six it becomes unusable, because all six will be abstract shapes
and the only discriminator is a title the user probably never changed from "My zine".

**On the two smaller questions:**
- **The wordmark should go.** A user inside your app knows they are in your app. It costs a full row of
  the most valuable screen space. Procreate, Figma and Photos all show the *content* first.
- **"On this device" should go from here** — but the *promise* must not. It is one of Zinely's few real
  differentiators, and it already appears where it earns attention: the empty state says *"Kept on this
  device — no account, nothing uploaded"*, and the share sheet says *"Nothing is uploaded by Zinely."*
  Both are at moments of doubt. A permanent chip is decoration; those two are reassurance.
- **Hierarchy.** Today: app name → "On this device" → "Your zines 1" → card. It should be: card → card →
  card. "Your zines" with a count of 1 is a label for a list that has no ambiguity.

**What the field does.** ✅ Figma derives the thumbnail from *"the contents of the first page of the
file"* by default, with a **"Set as thumbnail"** override for anyone who wants to curate
([Figma](https://help.figma.com/hc/en-us/articles/360038511413-Set-custom-thumbnails-for-files)).
✅ Procreate shows a thumbnail grid with the **title underneath and no metadata at all** — no dimensions,
no dates ([Procreate](https://help.procreate.com/procreate/handbook/gallery/gallery-organize)).
✅ Canva calls it the *"preview thumbnail… so you can quickly spot the right file without opening each
one"* ([Canva](https://www.canva.com/help/create-design-preview-thumbnail/)).

⚠️ **One caution worth heeding**: Canva maintains a support article titled *"Design preview not
updating"* ([Canva](https://www.canva.com/help/preview-not-updating/)). **A live cover that goes stale is
worse than a generated one**, because it silently misrepresents state. Whatever writes the thumbnail must
be on the same path as the save that changed the page, not a best-effort side job.

**Smallest fix.** Cache a cover thumbnail on save: render page 1 with the existing `SceneRenderer` +
`PagePreview` path, write a small PNG beside `document.json`, show it on the card, fall back to today's
generated recipe when absent (new/empty zines still look intentional). Drop the wordmark row; keep title,
"Edited …", and — unlike Procreate — **keep format and page count** ("8 pages · A4"), because for a zine
the format *is* what the physical object is, not a spec.

---

## 4. Text editing — two objects that should be one · **HIGH** · cost **S** now, **M** to do properly

**Evidence:** screenshot #6. The word "TEST" is being typed into a strip near the top of the screen while
the coral rectangle that *is* the text box sits in the middle of the photo. The keyboard covers the
lower half.

**Broken mental model.** There are two things on screen claiming to be the same text. The user's eye
binds "my text" to the orange box, because that is where it will print. The caret is somewhere else
entirely. Every keystroke updates a thing that isn't the thing they are looking at.

Note this is *aggravated by D2*: in #6 the photo and the box also overflow the paper, because the
keyboard resized the canvas mid-session and only the paper moved. Fix D2 first and this screen gets
materially less confusing before any redesign.

**What the field does.** ✅ Procreate layers this by frequency: typing happens **in place** on the canvas
("the Text box will expand to fit new content"), common commands live in a thin floating **"Text Entry
Companion"**, and rare styling escalates to a panel — reached via **an "Aa" button in the top right of the
keyboard itself**
([Procreate](https://help.procreate.com/procreate/handbook/text/text-interface)).
✅ Canva mobile **auto-dismisses the keyboard** when you pick a styling tool, treating typing and styling
as mutually exclusive modes ([Canva](https://www.canva.com/help/add-and-edit-text/)) — though users also
report its floating toolbar *"is constantly in the way."*
⚠️ A full-screen text sheet is the **worst** option for Zinely specifically: on a fixed-size zine page,
whether the text *fits* is the only question that matters, and a sheet hides exactly that. ✅ General
guidance agrees inline editing is wrong when *"editing is the primary function of the screen"*
([UI Patterns](https://ui-patterns.com/patterns/InplaceEditor)) — zine text is short, so the balance
tips toward in-place.

**Smallest fix (beta, S).** Keep the docked editor — an in-place editor under a soft keyboard is a real
project — but make the two read as one object: while the session is open, keep the target box visually
active (its selection chrome lit) and put the block's own text in the strip with a clear tie, e.g. a
caption *"Editing this text box"*. The goal for the beta is only to stop the user wondering *which* text
they are editing.

**The real fix (V1, M).** Edit in place: when a session opens, pan/scale the canvas so the target box
sits above the keyboard, and draw the caret in the box.

**Discoverability of typography — revised after research.** The Type bar is behind an `Aa` button that
sits **off-screen** in a horizontally scrolling toolbar; I had to scroll the row to find it during device
testing, *knowing it existed*. **The best feature in this release is invisible.**

My first instinct was to reorder the toolbar. Procreate's answer is better and Zinely is already most of
the way to it: put **`Aa` in the keyboard accessory row** during a text session. That places the control
at the thumb, adjacent to the keyboard that caused the occlusion, at the exact moment the user is thinking
about text — instead of in a toolbar the keyboard is covering. Do both: `Aa` first in the row *and* on the
keyboard accessory.

---

## 5. Recommendations — typography, graphics, spanning

Deliberately conservative; each is a V1 item, not beta.

### 5.1 Fonts — six, as pairs
`fontFamily` already round-trips and is honoured by the renderer since F3, so the model work is done; the
cost is bundling and a picker. Six faces, presented as **three "voices"** rather than a font list:

| Voice | Display | Body |
|---|---|---|
| **Classic** | a warm serif | its own text weight |
| **Plain** | Inter (already bundled) | Inter |
| **Marker** | a handwriting/marker face | Inter |

Beginners choose a *mood*, not a typeface. A picker showing "Fraunces / Inter / Caveat" asks a question
they cannot answer; "Classic / Plain / Marker" asks one they can. Keep per-attribute overrides in the
Type bar for anyone who wants them.

✅ **This is what the field actually does.** Canva's Text tab has a **"Font combinations"** section of
presets, and **Text Styles** let you *"set your heading font, body font, and accent font once."* Its own
stated heuristic: *"one font for headings and another for subheading text… then a simple sans serif for
body"* ([Canva](https://www.canva.com/help/color-and-font-combinations/)). Adobe Express shows a
**Recommended** set first with the full library only after scrolling *"all the way right"*
([Adobe](https://helpx.adobe.com/express/web/create-and-customize-text/add-text.html)). The unit of choice
is a **relationship**, and the big picker is a fallback rather than the entry point.

🟨 Research suggests 6–8 families across ~5 categories is the comfortable band — below ~6 reads as a
limitation, above ~10 reinstates the paralysis. Three voices at six faces sits at the bottom of that band,
which is the right end to start from for a beta.

⚠️ Watch the charset: the bundle is Latin-first and non-Latin already renders blank. Adding faces
multiplies that surface — the coverage warning (`analyzeTextCoverage`, which exists but has no caller)
should ship *with* font choice, not after it.

### 5.2 Graphics — three themed packs of ten, all recolourable
Arrows · stars · hearts · circles · flowers · tape · torn paper · scribbles · check marks. Monochrome
vectors that take the five text inks, so they cannot clash with the palette and need no new colour model.

✅ **Canva's own *Sticker Starter Pack* is ten designs, themed** — a company with unlimited assets sizes a
starter at ten ([source](https://jennifermaker.com/make-stickers-in-canva/)). ✅ GoodNotes' asset class is
literally *"editable stickers"* grouped into **collections**
([GoodNotes](https://www.goodnotes.com/blog/editable-stickers-goodnotes-marketplace)). ✅ Canva's stated
reason for categories: *"browsing by category turns an overwhelming library into a manageable search."*

Three lessons that change my recommendation:
1. **Ship themed packs, not a flat row.** Three coherent packs of ten read richer than thirty loose items
   — the user perceives worlds to choose between rather than an inventory to exhaust.
2. **Never show an exhaustible grid.** A single screen containing visibly *all* the assets invites
   counting. A pack the user taps into never reveals its ceiling.
3. **Recolourable is the multiplier.** One tintable star is five stickers in Zinely's ink palette.

✅ Worth noting the opposite bet: **Zeenster ships no stickers at all** — just shapes (rectangle, circle,
triangle, line) and a freehand draw tool
([source](https://github.com/virgilvox/zine-maker/blob/main/docs/03-user-guide.md)). For zine culture,
which is photocopy-DIY rather than polished, primitives plus a real brush may serve better than clip art
at a fraction of the asset cost. **A shapes-and-brush tool is a legitimate cheaper alternative to a
sticker library, and arguably more on-brand.**

### 5.3 Photo spanning — the signature feature
Agreed that this is the strongest of the P1 ideas, and it should be designed against the fold rather than
bolted on. Sketch:

**The geometry works out, and I verified it against the engine rather than assuming.** From
[`Convention.TOP_ROW_ROTATED`](../core/imposition/src/main/kotlin/com/aritr/zinely/core/imposition/Convention.kt),
`cellOf` is `1→(1,3) 2→(0,3) 3→(0,2) 4→(0,1) 5→(0,0) 6→(1,0) 7→(1,1) 8→(1,2)`, with pages 2–5 rotated
`HALF` and 1, 6–8 `NONE`. Therefore:

| Span | Sheet cells | Contiguous? | Same rotation? |
|---|---|---|---|
| **2–3** | row 0, cols 3→2 | ✅ | ✅ both HALF |
| **4–5** | row 0, cols 1→0 | ✅ | ✅ both HALF |
| **6–7** | row 1, cols 0→1 | ✅ | ✅ both NONE |
| **2–3–4–5** | row 0, all four cols | ✅ **the entire top row** | ✅ all HALF |
| 8–1 | row 1, cols 2→3 | ✅ | ✅ | *but not a reading pair — that's the outside wrap* |

So **every facing pair is a contiguous, uniformly-rotated strip on the print sheet**, and a four-page
panorama across 2–5 is exactly the top row. This is a much better position than photo-book tools are in,
and it means spanning needs no special imposition work — only a restriction to the pairs above.

- **Non-destructive by construction.** Spanning is not a new element type: it is one `ImageElement`
  reference appearing on *n* consecutive pages, each with its own `crop` window computed from the span.
  The existing per-element crop is exactly the right primitive. ✅ This matches how Blurb behaves — an
  image in a spread layout *"can be moved around within the two-page layout so that it spans the two pages
  automatically"* ([Blurb](https://support.blurb.com/hc/en-us/articles/207795326-Use-a-photo-spread-across-two-pages)) —
  and how Figma frames cropping: *"a non-destructive action that works similarly to using a mask… the
  cropped area does not get deleted"* ([Figma](https://help.figma.com/hc/en-us/articles/360040675194-Crop-an-image)).
- **Interaction: explicit command, then free repositioning.** Select a photo → *Span across…* → 2 pages /
  4 pages. ✅ This is the settled industry model (Blurb, Apple Photos both make "spread" a named layout
  you choose, after which the image behaves normally in a wider frame). Not drag-across-the-gutter: on a
  phone the two pages are never on screen together, so InDesign's direct-manipulation gesture has no
  equivalent. ⚠️ And not auto-split-on-import, which reads as destructive.
- **Why this matters: Lulu is the cautionary tale.** ✅ Lulu *"does not accept two-page spreads"* — a
  landscape image *"must be split accordingly"* by the user, in an external editor, with the seam exact
  ([Lulu](https://help.lulu.com/en/support/solutions/articles/64000255583-tips-for-formatting-documents)).
  That is precisely the labour a print-first app should absorb.
- **The fold, not the gutter.** ✅ Photo-book tools warn that content in the **gutter** is *"lost in the
  binding."* A mini-zine has **no binding** — pages meet at folds — so that specific hazard doesn't apply.
  The real one is the **crease** running through the image, plus the printer's unreachable margin (already
  drawn in the proof legend as *"printer can't reach here"*). Show the crease line live while spanning.
- **Override.** Each page's crop stays individually adjustable afterwards via Reframe; "re-span" resets
  them. No schema change, which is why this is a strong V1 candidate.

---

## 6. Priorities

### Release blockers — fix before the beta ships
| | Item | Cost |
|---|---|---|
| **D1** | Empty state drawn off the paper | S |
| **D2** | Paper and content at two different scales | S |
| **D3** | Zoom stepper inert but enabled-looking | S |
| **P0-N** | Rename "Preview" → "Print & fold"; say in the tester note that a reading view doesn't exist yet | S |

All four are small. None is a redesign. Together they remove every "the app is broken" reading in the
seven screenshots.

### Beta improvements — during the beta, on feedback
- Live cover thumbnails on the library card; drop the wordmark row and the "On this device" chip (**M**)
- Put `Aa` first in the toolbar so typography is discoverable (**S**)
- Tie the inline text editor visually to its box (**S**)
- Stop the page resizing when the toolbar changes height (**M**)

### Version 1.0
- **Read mode** — the missing third of Preview, and the highest-value single item in this review (**M**)
- In-place text editing (**M**)
- Font voices + coverage warning (**M**)
- Starter graphics set (**M**)
- Photo spanning across 2/4 pages (**L**)

---

## 7. What is genuinely good, and should not be touched

Worth recording, because a review that only lists faults will get the wrong things "fixed":

- **The fold guide** (step 3) is the clearest explanation of an 8-page imposition I have seen in a phone
  app — five steps, one at a time, with "the one cut" called out. It is the app's teaching moment.
- **The print-setup screen** pre-empts the four settings that actually ruin a home print (100% scale,
  landscape, paper, single-sided) with a reason attached to each. Most tools leave users to discover
  "Fit to page" ruined their zine after printing it. ✅ Zeenster documents *"Print at 100% (Actual Size);
  no scaling"* as essential guidance
  ([source](https://github.com/virgilvox/zine-maker/blob/main/docs/05-templates-and-export.md)) — Zinely
  surfaces it as UI rather than help text, which is better.
- **Imposition is never exposed as editable.** ✅ Every tool surveyed reduces page-order arithmetic to a
  template or format choice; it is the thing beginners most reliably get wrong. Zinely deriving panel
  order from the engine and never offering to let users rearrange it is correct and should stay that way.
- **"Reframe" as the verb**, for the reasons in D3.
- **The privacy copy** is placed where doubt occurs rather than in a settings page.
- **The empty-page copy** — "A fresh page. What goes here?" — is the right tone. It just needs to be on
  the paper.

---

## 8. Sources

Competitive research, gathered 2026-07-21. Claims above are labelled ✅ VERIFIED (sourced) or
🟨 ASSUMPTION (inference) per the [Research standards](../CLAUDE.md#research-standards).

**Zine tools.** [Zeenster / zine-maker](https://github.com/virgilvox/zine-maker) — the site itself could
not be fetched, so its own source repository and shipped docs were read instead, which is authoritative
for behaviour rather than marketing · [PDF Press](https://pdfpress.app/blog/how-to-create-zine) ·
[snipzine](https://snipzine.com/)

**Photo books & print.** [Blurb BookWright — Preview vs online preview](https://support.blurb.com/hc/en-us/articles/360015822551-What-s-the-difference-between-BookWright-s-Preview-feature-and-my-book-s-online-preview-on-Blurb-s-website) ·
[Blurb — photo across two pages](https://support.blurb.com/hc/en-us/articles/207795326-Use-a-photo-spread-across-two-pages) ·
[Lulu — formatting tips](https://help.lulu.com/en/support/solutions/articles/64000255583-tips-for-formatting-documents) ·
[Adobe InDesign — impose for booklet printing](https://helpx.adobe.com/indesign/desktop/print/print-booklets/impose-documents-for-booklet-printing.html)

**Creative apps.** [Procreate — Gallery](https://help.procreate.com/procreate/handbook/gallery/gallery-organize) ·
[Procreate — Text interface](https://help.procreate.com/procreate/handbook/text/text-interface) ·
[Figma — Crop an image](https://help.figma.com/hc/en-us/articles/360040675194-Crop-an-image) ·
[Figma — custom thumbnails](https://help.figma.com/hc/en-us/articles/360038511413-Set-custom-thumbnails-for-files) ·
[Canva — font & colour combinations](https://www.canva.com/help/color-and-font-combinations/) ·
[Canva — add & edit text](https://www.canva.com/help/add-and-edit-text/) ·
[Canva — preview not updating](https://www.canva.com/help/preview-not-updating/) ·
[Canva — add elements](https://www.canva.com/help/add-elements/) ·
[Adobe Express — add text](https://helpx.adobe.com/express/web/create-and-customize-text/add-text.html) ·
[GoodNotes — editable stickers](https://www.goodnotes.com/blog/editable-stickers-goodnotes-marketplace) ·
[Keynote iPhone — text boxes](https://support.apple.com/en-gb/guide/keynote-iphone/tan4fd6ee725/ios)

**Patterns.** [UI Patterns — In-place editor](https://ui-patterns.com/patterns/InplaceEditor)

Two caveats on provenance: `zeenster.com`, `snipzine.com` and `support.blurb.com` refused direct fetches,
so Zeenster is cited from its repository and Blurb/Canva partly from search-surfaced help-centre text;
and Apple Photos' crop specifics could not be verified and were therefore not relied on.

---

# Pass 2 — V2.1 Bench, Type bar, Reframe (2026-08-15)

> **Different build, same method.** Everything above reviews `0.9.0-beta.1`. This section reviews the
> `feat/v21-freeze-and-tokens` branch at **`a80cc07`**, installed as `zinely-0.9.0-beta.1-debug.apk` on
> the same physical SM-A176B (Android 16, dark theme, density 420, font scale 0.9). It lives here rather
> than in a new file because this document owns first-time-user findings
> ([Documentation Rule](../CLAUDE.md#documentation-rule-mandatory)); the build under test is stated in
> every claim so the two reviews are never conflated.
>
> Evidence: fourteen device screenshots and eleven `uiautomator` dumps, captured while making a zine from
> an empty shelf. The test zine was deleted and the pushed test image removed at the end of the pass.

## The one-sentence finding

**The Bench keeps advertising capabilities it does not have.** A first-time user reads every instance of it
as *the app is broken*, never as *that part is not built yet*.

This pass first counted three instances. Source review found **five dead controls**, not three: `Font` and
`Replace` on the context bar (`BenchContextBar.kt:99, 119`) and `Font`, `Size` and `Ink` in the style row,
whose own KDoc says the quiet part out loud — *"Why three of its four controls are inert"*
(`BenchStyleRow.kt:153-160, 240-242`). Add the pruned z-order button (F-2) and copy pointing at an
unimplemented Supplies sheet (F-3), and the pattern is the dominant defect of this build. **The device pass
understated its own headline finding**; `Replace` never appeared in it at all.

## Findings

Classified per the [review principles](../CLAUDE.md#review-principles-review-agent). A **disagreement**
between the two device passes is itself the finding.

### F-1 · `Font` ships permanently disabled on text — Required Fix

`uiautomator` on a selected text element: `Font | enabled=false`, while `Edit`, `Size`, `Ink` and `Delete`
are all `enabled=true`. A control that exists and is dead advertises a capability the app does not have.
**Pass 1 would pass this; Pass 2 fails it** — the disagreement is the finding, and per the handbook Pass 1
does not overrule Pass 2 on the grounds that the behaviour is correct.

**Corrected cause.** The first draft guessed *"the fonts work is queued and one face ships, so disabled is
correct"*. Source says otherwise: `BenchContextBar.kt:78-83` disables `Font` under **OD-9** — the freeze
draws a control the application flow cannot honour. `TextStyle.fontFamily: String = "sans-serif"` **already
exists in the model** (`Document.kt:115`) and nothing in the editor or renderer ever reads it. What is
missing is a picker and an intent, **not a second typeface** — which makes this materially cheaper than the
first draft implied.

**Corrected scope.** The draft also claimed the two surfaces disagree. They do not: `BenchStyleRow.kt:240`
ships `InertChip(Copy.BenchVerbs.FONT)` — announced disabled, no `clickable`, asserted by
`BenchStyleRowPlatformA11yTest` and `BenchC3Test:629`. Both surfaces are consistently dead. The
inconsistency noted at F-13·2 (*"`Font` and `Size` are bare words"*) is not a labelling nit; those chips are
**inert**, and that bullet rolls into this finding.

**Corrected again: half of what this finding asked for is already ruled against.** The draft above offered
*"hide the dead `Font` affordance, or give it a reason"* as a Release Blocker. **Hiding it is not available.**
[D-031 / OD-9](design/V2-SPEC-DEFECTS.md#d-031) ruled on 2026-08-01 that **"Font and Size stay *drawn* with
no invented capability"**, and [D-044 / OD-17](design/V2-SPEC-DEFECTS.md#d-044) settled the style row's
chips as verb-labelled for the same reason. Proposing removal here would re-litigate two owner rulings —
the same error this document already made once at F-8.

**What the ruling does not decide, and what this pass therefore actually asks for.** OD-9 says the control
stays *drawn* and invents *nothing*. It does not say the control must be **silent**. There is real room
between "drawn" and "unexplained", and explaining an absence invents no capability — it is the opposite of
inventing one.

This is the same defect and the same fix as **F-4** on Reframe, where five controls were correctly dead and
the surface said nothing about why. The remedy there is one line of outcome language — the `.padhint` block
in `v21-reframe.html`, implemented and device-verified the same day — and the remedy here is the same shape. **A house rule is emerging from two independent findings and is worth stating once:**

> **A control that is drawn and disabled says why.** Not an error, not a warning — an instruction in the
> same voice as `crops edges`. Silence is what a first-time user reads as breakage.

| | Category | |
|---|---|---|
| Give the drawn-and-disabled controls a reason | **Release Blocker** for this branch | the actual ask, and it is compatible with OD-9 |
| ~~Hide the dead affordance~~ | **ruled against** | OD-9 — not available without an owner reopening it |
| Ship selectable faces | **Future Enhancement** | already queued; not this branch |
| Drawn, inert and unexplained | **Known Limitation** | what ships today; must appear in release notes if it ships again |

**Implemented (selection bar half), 2026-08-15.** Two reasons were added to `Copy.BenchVerbs` and wired
through `BenchVerb.unavailableBecause` into the `clearAndSetSemantics` block of
[`BenchContextBar.kt`](../feature/editor/src/main/kotlin/com/aritr/zinely/feature/editor/BenchContextBar.kt):
`Font` and `Replace` report **"Not available yet"**, and `Size` / `Ink` on a still-blank box report
**"Type something first"**. They are deliberately two strings, not one "unavailable" — the second names a
move the user can make *now*, and collapsing them would throw away the actionable half.

The reason rides `stateDescription`, **not** `contentDescription`: the control keeps its name (`Font` is
still announced as `Font`), so nothing about the frozen bar's vocabulary changes and OD-9's *invents
nothing* holds — an absence is explained, no capability is claimed. Covered by
`BenchContextBarTest."a drawn but disabled verb announces why, and an enabled one announces no state"`,
which was proved to fail (`expected:<Not available yet> but was:<null>`) with the line removed. Suite after:
**965 tests, 0 failures, 1 skipped** across `:feature:editor`, `:core:ui`, `:core:copy`.

**Implemented (editing row), same day.** [`BenchStyleRow.kt`](../feature/editor/src/main/kotlin/com/aritr/zinely/feature/editor/BenchStyleRow.kt)'s
three inert chips now carry a reason too, and `InertChip` takes it as a **required** parameter — a
drawn-and-disabled chip that cannot say why no longer compiles. `Font` reports **"Not available yet"**;
`Size` and `Ink` report a third string, **"Finish typing to change this"** (`Copy.BenchVerbs.FINISH_TYPING`),
because their absence here is not a missing capability at all: both verbs are live on the selection bar the
moment this row stands down, and [D-042](design/V2-SPEC-DEFECTS.md#d-042) records that the two surfaces are
mutually exclusive by construction. Reusing `NOT_YET` there would have been false. `Done`, the one live
control, carries no state. Asserted by `BenchC3Test.three_of_the_four_style_controls_are_inert_and_say_so`.

That string is the **second** attempt, and the first one is worth keeping on record because both obvious
answers are wrong. It read *"Tap Done, then change it"*, and independent review caught it as a lie in the
state a new user meets first: `Add → Text` opens this row on a blank box, and `Done` on a blank box does
not hand back something to style — the reducer removes the element. The reviewer's proposed repair, choosing
between this string and `TYPE_FIRST` from `editingElement.text`, is wrong for a subtler reason: the draft is
feature-ephemeral ([ADR-029](DECISIONS.md#adr-029) §5.6) and does not reach the store until commit, so that
text reads blank for the whole of the typing and would announce *"Type something first"* to someone who has
just written a paragraph. Naming the **condition** rather than the button or the content is true in every
state and needs no new plumbing.

**Scope, stated exactly.** These two surfaces are what F-1 covers; the **Reframe pad is F-4's**, and it was
implemented later the same day (see the F-4 section at the end of this file). All three surfaces that carry
drawn-and-dead controls now explain themselves. `Undo`, `Redo` and `Done` on
[`BenchBottomBar.kt`](../feature/editor/src/main/kotlin/com/aritr/zinely/feature/editor/BenchBottomBar.kt)
are deliberately **not** given a reason: they are disabled by ordinary transient state, they re-enable in
the same session as a consequence of what the user just did, and "Nothing to undo" is the one sentence a
disabled `Undo` already says by being disabled. The house rule is about absences the user cannot account
for, not about every dim pixel.

### F-2 · `Send backward` is pruned from the tree; `Bring forward` is 8dp wide — Required Fix

The transform row declares more controls than fit a 403dp container. (Count varies by selection: **eleven**
for a single non-blank text box — 8 transform + 2 reorder + Style, with `Delete` withheld under OD-14 — and
ten for a photo or a blank box. The observed case was ten.) Nine lay out;
`Send backward` is absent from the platform tree entirely, and `Bring forward` measures
`[1059,2077][1080,2203]` — **21px, 8dp of a 48dp target**. The row *is* horizontally scrollable, but
carries no fade edge and no deliberate half-button peek, so the 8dp sliver reads as a rendering glitch
rather than as "there is more". Pass 1 measured this; Pass 2 supplies why it matters: **as a user I never
learned those controls existed.**

**The row is specified nowhere.** `v21-bench.html` — DESIGN-FROZEN — contains no `bring forward`, no
`send backward`, no `z-order`, no nudge control; grep returns nothing for any of them. So this is not a
deviation from the freeze but an **omission in it**, which
[V2-SPEC-DEFECTS.md](design/V2-SPEC-DEFECTS.md) explicitly owns (*"contradictions, stale text, omissions"*).
The clipping is an implementation bug and is fixed as one; the missing specification is a separate entry and
must be filed there, because until it exists there is nothing to verify parity against.

### F-3 · The empty state points at supplies that do not exist — Required Fix (parity)

A new page reads *"Grab a photo or a few words from the supplies below."* Below are page thumbnails, undo,
redo, Add and a checkmark. The `Add` chooser offers exactly two rows — `Text` and `Photo`. The illustration
is three sticker tiles, promising material the app does not have.

**Corrected root cause.** The first draft of this finding called it *"copy shipped ahead of its surface"*.
That is wrong. The **frozen** Bench specifies the surface: `v21-bench.html:848-860` builds an `Adding · Art`
sheet with four families of authored supplies, `Recent · ⭐ favourites`, and a `Supplies` grid. The copy is
faithful to the freeze; **Compose does not implement the sheet.** This is therefore a *pixel-parity failure
against a frozen artifact*, not premature copy — which makes it an allowed post-freeze fix
(*implementation parity*) rather than a design question. Route via
[V2-SPEC-DEFECTS.md](design/V2-SPEC-DEFECTS.md) only if the freeze itself is found wanting; otherwise it is
ordinary parity work.

### F-4 · Reframe opens with five of seven controls dead and no explanation — Required Fix

On entry at 100% with `Fill`, `Move photo up/left/right/down` and `Zoom out` are all `enabled=false`; only
`Zoom in` is live. One zoom step to 115% and **all seven become `enabled=true`**. `Framing.abilities()`
(`FramingDraft.kt:182-192`) is genuinely overflow-gated, so the logic is correct. This is the pass's cleanest
disagreement: correct logic, misleading entry state. Per CLAUDE.md, *"correct but misleading is a defect with
a known cause, which makes it cheaper to fix than most, not safer to ship."*

**One correction, so nobody "fixes" correct code.** The draft generalised this to *"at 100%-Fill the photo
has no overflow"*. That is not true in general — `coverExtent(pratio, bratio)` (`FramingDraft.kt:90-91`)
leaves exactly one axis `< 1` **unless the photo's aspect equals its frame's**, which would light two nudges.
All four are dead at entry only because a newly placed photo's frame is seeded to the photo's own aspect. The
finding is about the *entry state a user actually meets*; the gating itself must not be touched.

*Method note:* the observation used a synthetic 600×600 gradient rather than a real photo, precisely so the
frame geometry was known. That is also why the aspect-equality case was the one observed.

### F-5 · The ink panel covers the text it is colouring — Required Fix

With the panel open, the selected text sits pinned at the top edge, half under the panel's border. You
choose ink for type you cannot see. "Chrome over the artifact" is the symptom; source gives a sharper cause.

**Root cause: a clearance term that exists next door and is missing here.** `BenchStyleRow` already solves
this exact problem — `onDockedTopChanged` / `benchEditPanMagnitudeDp` (D-043 / OD-16,
`BenchStyleRow.kt:174-207`) pans the page clear of the docked panel. `BenchInkPopover` is placed with the
same bottom-anchored `ctxModifier` (`EditorScreen.kt:1364`) and has **no equivalent**. The fix is to extend
an existing mechanism, not to invent one.

### F-6 · Two identical green `Done` pills on screen at once — Required Fix

The ink panel's `Done` and the page-level commit `✓` are both `colors.leaf` pills
(`BenchInkPopover.kt:441`, `BenchBottomBar.kt:356`), visible simultaneously. A first-time user cannot tell
which one ends what. *This settles the open freeze question "how does the panel close?"* — it closes via a
control that collides with the global commit.

**This class was already solved once, and the popover was not covered by the rule.**
`doneEnabled = editingElement == null` (`EditorScreen.kt:1485`) withholds the bottom bar's `Done` during a
text session for exactly this reason — so two `Done`s never coexist. The ink popover simply sits outside
that condition. That is the fix's shape, and the device pass missed it.

**Surface re-attributed — this is a Bench defect, not a Type bar one.** The first draft treated F-6 as an
open *Type bar* interaction question, and used it to argue the Type bar spec could not be frozen. Wrong on
both counts. The colliding controls are `BenchInkPopover` and `BenchBottomBar`, both on the **frozen Bench**.
The Type bar's own spec settles its side already, in its header: *"this panel is **non-modal, has no commit
and no primary** (cancel is undo), and `.add` never leaves."* `TypeBar.kt` ships no `Done` at all, matching
it. The mis-attribution came from the context bar's `Ink` routing to the **Bench popover**, a different
surface from the Type bar's inline Colour row. F-5 re-attributes the same way, for the same reason.

### F-7 · No swatch matches the text's actual ink — Required Fix (palette split, **not** accessibility)

**The first draft of this finding was false and is retracted.** It read *"the current ink is visible to the
eye and invisible to TalkBack"* and filed the defect as *"same family as the three a11y fixes in
`a80cc07`"*. Source refutes both halves. `BenchInkPopover.kt:521-528` already publishes state:

```kotlin
.selectable(
    selected = selected,
    role = Role.RadioButton,
    onClick = pick,
)
```

Acting on the retracted version would have added semantics that are already there.

**The real defect.** `selected` is `inkTarget?.style?.color?.toComposeColor()`
(`EditorScreen.kt:1349`) — the element's own ink. The observed text carried the `TextStyle.color` default,
which appears in **no** offered band, so nothing was ringed **on either channel**. The ring was absent
visually too; the device pass mistook "nothing selected" for "selection not announced". The call site's KDoc
had already predicted it: *"an ink applied from the Type bar (Coral, Teal, Blue — in no frozen band)
correctly rings nothing rather than ringing something stale."*

So this is **the colour-vocabulary split, observed in the wild** — `TypeBar.kt:104-109` offers five
`TextInk` values (`Ink`, `Coral`, `Teal`, `Blue`, `Ochre`) chosen for legibility contrast, and most do not
exist in `benchInkBands`. The two vocabularies were deliberately kept separate (ADR-055 Decision 6), but
nothing reconciles what a user sees when an element styled by one is inspected by the other. **This finding
collapses into the existing "reconcile the three colour vocabularies" work rather than standing alone.**

Terminology note that survives: this panel shows **two** bands, `INKS` (10) and `NEUTRALS` (4) — no paper
tints, correctly, because type cannot be set in a paper tint. The nineteen-swatch figure describes the
*Bench* popover, not this one.

### F-8 · The shelf shows a placeholder instead of your work — Known Limitation of this build; open finding elsewhere

**This finding is not new and is not owned here.**
[§3 above](#3--library--it-doesnt-look-like-my-work--high--cost-m) already owns it, and
[V1-DESIGN-ELEVATION.md](V1-DESIGN-ELEVATION.md) ranks it **Top-20 change #1**.

**§3's stated cause is now stale — do not act on it.** It attributes the cover to `ShelfCoverRecipe.kt`.
That file has **zero call sites in main source**; it and `ShelfCover.kt` are reachable only from their own
tests, left behind by the V1-shelf deletion (`2b6a71b`). What actually renders is a persisted
`ZineCoverRecipe`, *"assigned **once**, at creation, and stored"* (`ZineLibraryScreen.kt:58-67`) — which is a
stronger statement of the defect than "no thumbnail available": the cover is **fixed at creation by design**
and cannot reflect content that arrives later. §3 should be updated when this is fixed.

Two corrections to this section's own first draft, which were wrong:

- It is **not a "recurrence"** — it was never fixed. This pass is a *third witness* to a still-open finding.
- It is **not** "on a surface that was not part of that fix". The Library was ranked **first**, above
  everything.

What this pass adds is one new fact, and it is **not** the one an earlier draft of this section claimed. The
draft proposed filing the generic cover as a defect in a frozen artifact. It is not a defect — **it is an
owner ruling.** [D-017](design/V2-SPEC-DEFECTS.md#d-017) was raised and resolved on 2026-07-30: *"assign at
creation and persist; **do not** derive from the title"*, with [D-026](design/V2-SPEC-DEFECTS.md#d-026)
extending it to zines that predate the field. `ZineLibraryScreen.kt:58-67` implements that ruling exactly.

So the shelf is not unfinished and not drifting — **it is faithfully executing a decision that was made
deliberately.** Filing a new register entry would re-litigate a settled ruling, which the register exists to
prevent. The live question is a *product* one and it is already owned upstream as
[V1-DESIGN-ELEVATION.md](V1-DESIGN-ELEVATION.md) Top-20 #1: does D-017's ruling still serve the Library's
own question once a zine has content? That is the owner's to reopen, on this pass's evidence, or to leave.

New device evidence for the file: two zines both named `My zine`, separable only by cover colour, a paper
badge (`A4` / `US LETTER`) and a timestamp. `Rename` exists in the card's overflow menu; nothing invites it.

Against **this branch**, whose scope is Bench / Type bar / Reframe, it is a **Known Limitation**, not a
release blocker — classifying it Required Fix here would be a scope expansion onto a frozen surface.

### F-9 · `Reset framing` is the only unworded control among three — Recommended Improvement

`Cancel` and `Done` carry words; between them sits a bare circular arrow. Its a11y label is correct
(`Reset framing`), so this is **visual only**. It matters because the circular arrow is *also* the rotate
glyph, and the Bench's transform row uses circular arrows for `Rotate clockwise` /
`Rotate counterclockwise` one surface away. *This settles the open freeze question "does Reset need a
word?" — yes, on glyph-collision evidence rather than on taste.*

### F-10 · `Move photo up` (crop) collides with `Move up` (element) — Recommended Improvement

Inside Reframe the nudges are `Move photo up/left/right/down`; on the Bench the transform row is
`Move up/left/right/down`. One moves the image inside its frame, the other moves the frame on the page.
For TalkBack the two are near-indistinguishable. Related: **the verb "Reframe" itself survives Pass 2** —
the panel's own `Fill / crops edges` vs `Whole photo / may add margins` explains the screen on arrival, so
the evidence does **not** support renaming it. *That settles the third open freeze question.*

### F-11 · Three privacy assertions in four screens — Required Fix (parity)

`works offline · stays on your phone` (empty state), `From your phone — it never leaves the device` (Add
chooser), plus the offline badge. The app already *earns* the claim silently and better: picking a photo
goes through the system photo picker and **raises no permission prompt at all**. The mechanism is more
convincing than any of the three sentences.

**This is not a new opinion and does not re-decide anything.** The frozen Bench already ruled it, in the
spec's own words at `v21-bench.html:857-859`:

> *The label reads "Supplies", not "Bundled with Zinely · always offline". Offline is an invisible strength,
> not a slogan (ADR-104): naming it here would advertise the absence of the thing we just removed, which is
> exactly the dead-feature smell the amendment forbids.*

**Corrected 2026-08-16, after auditing the frozen corpus rather than one file of it.** Three details above
are wrong, and the last one changes what the fix may legally be.

1. The citation is `857-859`, not `860-862`.
2. That comment governs the **Supplies sheet label**, not the empty state. It is evidence for the rule; it
   is not evidence that this surface's freeze encodes it.
3. **The count is five, across four screens, and only one of them is a parity failure.** The `works offline
   · stays on your phone` pill has *no frozen source on its own surface at all* — `v21-bench.html` draws no
   empty state, and the CSS behind it was transcribed from `v21-library.html:306-307`, another screen's
   spec. The other four are drawn by the freeze itself (`v21-bench.html:825-829`, `v21-library.html:466`,
   `v21-proof.html:593`), plus one that exists **only in the accessibility tree** (`Saved on this device`,
   which the freeze renders as `Saved` alone) — so a screen-reader user hears the promise once more than a
   sighted user sees it. The freeze also specifies a sixth instance Compose never built.

So the sentence *"the freeze encodes the rule and the implementation drifted from it"* is true of exactly
one instance and false of the rest: for those, **the freeze itself breaks R12**, and striking them is an
owner amendment, not a parity fix. Filed as [D-079](design/V2-SPEC-DEFECTS.md#d-079) with the options.

What remains straightforwardly repairable here is instance #1 alone. Before touching it, read
[V1-DESIGN-ELEVATION.md](V1-DESIGN-ELEVATION.md) §18.1–18.2: §18.2 already ruled the duplication *"a rule
violation, not a rule to change"*, and §18.1 says the [ADR-033](DECISIONS.md#adr-033) collision **must not
be resolved by the implementer alone**. ADR-033 asks for the editor's privacy line; ADR-104 Consequence 3,
adopted later and by the owner, forbids replacing removed online UI with offline messaging. That is the one
question this finding cannot answer for itself.

### F-12 · ADR-090 does not distinguish transient scrim from sustained dimming — Observation

The `Add` sheet dims the page, exactly as Reframe used to before `a80cc07`. A modal bottom sheet dimming
its background for a moment is ordinary convention; Reframe's was a *sustained working state*. **ADR-090 as
written — "the artifact does not dim; the room around it may" — forbids both**, and a fix was just spent
enforcing it literally. The rule needs the distinction stated before it is enforced again.

**Routing:** this is an **ADR amendment request against ADR-090**, not a review observation to be left as
prose. It is filed as one, or it does not exist.

**Filed 2026-08-16 — ⏳ amendment drafted, awaiting owner adoption.**
[ADR-090 · Amendment request (2026-08-16)](DECISIONS.md#adr-090-amendment-scrim). It surveys every surface
that dims — the `ZSheet` scrim, the page grid, Reframe before and after `a80cc07`, and the selection wash —
and puts three options and a recommendation to the owner. **Three of this finding's premises did not survive
that survey**, and the corrections are kept here rather than folded away, because each would have aimed a fix
at the wrong surface:

1. **The axis proposed above is the wrong one.** Two of the four permitted mechanisms are *sustained* —
   Reframe's overflow dim and the selection wash — so *transient vs. sustained* cannot be the test that
   separates them. The test the code already obeys is **what is dimmed**: the artifact, versus the room,
   versus a part of the user's content the artifact will not carry.
2. **`a80cc07` did not enforce the rule literally.** It did not remove Reframe's dim; it re-aimed it to
   `dst - frame`, clamped inside the photo's own destination rect. A sustained dim of the user's own pixels
   survives that commit on purpose.
3. **The Add sheet's scrim and Reframe's old one are not the same thing** beyond sharing a literal.
   `ReframeOverlay.kt:84-87` already distinguishes them — *"a modal backdrop … a permanent crop dimmer …
   same value, different job"* — which is the distinction this finding asks for, written in Kotlin by an
   implementer who needed it and had no rule to cite.

**And the request is filed against the wrong document, which the amendment says of itself.** ADR-090 *records*
the rule; the rule is owned by [OD-12 / D-035](design/V2-SPEC-DEFECTS.md#d-035-ruling), extended universally
by **OD-31 / D-071** — so adoption lands as a new owner ruling in
[V2-SPEC-DEFECTS.md](design/V2-SPEC-DEFECTS.md), not as an edit to the ADR.

### F-13 · Smaller notes — Observation

- The new text box has no placeholder; its caret is a hairline against the dashed border. Nothing inside
  signals that typing lands there.
- ~~`Ink` shows its current swatch; `Font` and `Size` are bare words.~~ **Rolled into F-1** — those chips are
  not under-labelled, they are inert.
- First text renders in the platform sans — the least zine-like thing on screen, on the first words a user
  ever writes. Note this is `TextStyle.fontFamily`'s `"sans-serif"` default being honoured by nothing; see
  F-1.
- ~~The box lands low-left rather than centred.~~ **Retracted — observer error.** `centeredTextBox`
  (`EditorScreen.kt:1671-1680`) computes `x = (page.width - w) / 2` and `y = (page.height - h) / 2`, dead
  centre in points. Re-measuring the keyboard-down screenshot confirms it: page spans y 240–1318 (centre
  779), box centre 775. The misread came from judging position while the **keyboard cropped the page**, which
  moved the apparent centre. *Recorded rather than deleted: "I judged a layout against a viewport that was
  not showing the whole page" is a mistake worth not repeating.*

## What went right, and should not be lost in a fix

- **The ink panel's costing sentence** — *"Zines look best — and print cheapest — with 1–3 inks. This one
  uses 1."* The small-press identity arriving as material guidance rather than as branding. It is the best
  sentence in the app.
- **`Fill / crops edges` vs `Whole photo / may add margins`** — plain language that states the consequence
  instead of naming the mechanism.
- **The paper-choice sheet** — real dimensions (210 × 297 mm / 8.5 × 11 in) and *"Eight pages from one
  folded sheet."*
- **Deleting a zine** is immediate with an `Undo` snackbar and no confirmation dialog — the right pattern
  for a reversible destructive act.
- **The scrim fix holds in the real flow.** The page stays at full brightness throughout reframing.
- **No data loss.** Leaving to the photo picker and relaunching cold returned the work intact
  (*"Edited 5 minutes ago"*).

## Recommendation: freeze Reframe after this pass's revisions; the Type bar is not ready

The plan entering Pass 2 was *fix what is measured → Pass 2 → decide the rest → freeze*. Pass 2 changes the
last step, but **not for the reason this section first gave**, which was wrong and is corrected here because
leaving it on the record would teach a false lesson.

**The withdrawn argument.** The first draft argued that freezing would put F-4, F-5 and F-6 *"permanently
out of reach of the ordinary fix path"* because a [DESIGN FREEZE](../CLAUDE.md#design-freeze) forbids
interaction redesign. That is false, and the handbook says so: the pipeline places **Device verification
after DESIGN FREEZE**, and supplies the post-freeze route in the next sentence — *"Any UX change after
freeze must first update the HTML specification, then be implemented in Compose."* Amendment is the
**ordinary** path, not an exile from it, and [V2-SPEC-DEFECTS.md](design/V2-SPEC-DEFECTS.md) is its standing
queue with a history of same-day rulings. Two of the cited findings would not even need it: F-5 is a layout
defect of the class already fixed under freeze at `a80cc07`, and F-7 is expressly *"accessibility
improvements"*. A freeze does not trap defects.

**The actual reason, and how far it reaches.** A freeze follows *Design refinement*, and **you cannot freeze
a specification whose interaction questions are still open** — the freeze would ratify undecided design.
Working through them one surface at a time narrowed this considerably:

| Question | Surface | Status |
|---|---|---|
| F-4 · dead-on-arrival entry state | **Reframe** | genuinely unspecified — the spec never described it |
| F-9 · `Reset` has no word | **Reframe** | genuinely unspecified |
| F-10 · is "Reframe" the right verb? | **Reframe** | the spec *asks* it explicitly; evidence now answers it |
| F-6 · two identical `Done` pills | **Bench**, not Type bar | the *observed instance* is `BenchInkPopover` + `BenchBottomBar` |
| F-5 · panel occludes the artifact | **Bench** instance; **class is not retired** | the Type bar has no clearance term either |
| "How the panel closes" | **Type bar** | **still open — the file asks it explicitly** |
| The word "Coral" | **Type bar** | **still open — this pass never saw the Colour row** |

**Two corrections, one of them to a correction.** The first draft said F-6 proved the panel's dismissal was
never designed. The second draft over-corrected, concluding the Type bar therefore had *no* open interaction
question and could freeze as-is. Both are wrong, and the file itself says so — `v21-typebar.html:739-748`
carries *"Two questions this file does not answer, because they are the owner's,"* and **question 1 is "How
the panel closes."** Re-attributing the *two-pills* defect to the Bench settles how the **ink popover**
closes; it says nothing about the Type bar panel, whose premise is different (`Size` opens it, and the
context bar is withheld while it is open, so the control that opened it is not on screen to close it).

The file even predicted this pass: *"it is the first thing a first-time pass will stop on."* It was.

**What actually closes it today, undocumented.** `doneEnabled = editingElement == null`
(`EditorScreen.kt:1485`) is *true* throughout a Type bar session, so `BenchBottomBar`'s green `Done` is
visible and enabled, and tapping it dispatches `ClearSelection` (`:1497`) → `styleTarget` nulls → the panel
closes. So the panel *does* coexist with a Done-like control — one, not two, which is why F-6 proper is
Bench-only — and that control is the de-facto answer to the file's own question, reached through something
that reads as a global commit. No spec says this. `BackHandler` is registered for `pageGridOpen` and
`inkPopoverOpen` but **not** for the Type bar, so Back does not dismiss it either.

**The cost of waiting, stated honestly.** The Type bar and Reframe Compose is already built and on a device.
Every day a spec stays `PROPOSAL`, the shipped Compose is the de-facto specification — precisely the
*"never the reverse"* the handbook forbids. That cost is why this recommendation freezes what is ready
rather than holding both back for tidiness.

**Recommendation, for the owner's ruling — freeze one, not both.**

- **`v21-reframe.html` — ready to freeze**, after the three revisions made in this pass and recorded in the
  file: the `.padhint` entry-state rule with its accessibility exposure (F-4), `Reset` carrying a word (F-9),
  and the verb question answered with evidence and a recommendation to **keep "Reframe"** (F-10). One
  pre-existing caveat the owner should see: `.frame{overflow:hidden}` is flagged in the file itself as *"a
  PROTOTYPE CONVENIENCE, not the specified behaviour"*, so the prototype cannot be pixel-diffed against the
  build on the one behaviour this surface is about — the dimmed overflow spill.

- **`v21-typebar.html` — NOT ready**, and this pass did not move it. Its own closing captions name four
  outstanding items and this pass answered none of them:

  | | Outstanding | Kind |
  |---|---|---|
  | 1 | Swatch pitch: `.pot` at 38px invalidates `Swatch`'s measured 40×48 TalkBack bounds — *"must be re-dumped, not re-reasoned"* | device measurement |
  | 2 | Which row is widest, and does it fit 360dp at the largest font scale | device measurement |
  | 3 | **How the panel closes** | owner question |
  | 4 | The word **"Coral"** — a swatch named for a colour the design language abolished | owner question |

  Items 1 and 2 are **dischargeable now** and should be, before the owner is asked anything: the shipped
  `SwatchGap` was already raised to a 48dp pitch in this branch, which *diverges from the 38px the spec
  draws* — so the spec needs revising to match the implementation, and the platform-tree dump then becomes
  the evidence rather than an open question. Item 4 is a naming question this pass **could not** answer: the
  device route from the context bar's `Ink` goes to the Bench popover, so the Type bar's own Colour row was
  never on screen.

Both files carry
`STATUS. PROPOSAL. The owner freezes; nothing here is authoritative until…`
(`v21-typebar.html:50`, `v21-reframe.html:56`), so declaring a freeze is not this session's act to make —
this section recommends and the owner rules. Editing those two files is **revision**, not *amendment*:
[V2-CONSTITUTION.md §VI](design/V2-CONSTITUTION.md) reserves "amendment" for owner acts against **frozen**
surfaces, and neither of these is frozen. `V21-SPEC.md` §3.4 confirms the frozen corpus is Library / Bench /
Proof only.

The three open questions are now answered on evidence — F-6, F-9, and F-10 (the verb survives).

**Correction on "Coral".** This section first said *"Coral turned out to name a colour the app does not
ship."* That is wrong. `TypeBar.kt:106` ships `Coral(Copy.Type.INK_CORAL, ColorRgba(0xA6, 0x3C, 0x22))`. What
is true is narrower and more useful: Coral exists in the **Type bar's** five-value `TextInk` set and in
**no** band of the Bench popover — which is not a naming question at all, but F-7's palette split seen from
the other side.

> **Where the decision lives.** This file declares itself *not a source of truth*. When the owner rules, the
> freeze state belongs in [V21-SPEC.md](design/V21-SPEC.md) and an ADR; this section is the evidence behind
> it, not the record of it.

---

## F-14 · Answering the Type bar's "Coral" question — and a defect found while answering it

*Added 2026-08-15. This was not a device finding; it came from measuring the two palettes against each
other after Pass 2 raised F-7. It answers one of the two owner questions `v21-typebar.html:746-748` leaves
open, and it supersedes this document's earlier confusion about the word.*

### The measurement

Every value below is the WCAG contrast ratio of the swatch **used as text**, against Zinely's two papers
(`--paper #FFF6E8` and the Cream stock `#F1E9D6`). AA for normal text is **4.5:1**.

| Bench maker ink | as text | Type bar ink | as text | |
|---|---|---|---|---|
| `Ink` `#2A251E` | 14.18 / 12.57 | `Ink` `#23201C` | 15.14 / 13.41 | same name ✓ |
| `Ochre` `#D19A3C` | **2.33 / 2.07** | `Ochre` `#7A5E12` | 5.70 / 5.05 | same name ✓ |
| `Brick` `#B0503F` | 4.82 / **4.27** | `Coral` `#A63C22` | 5.96 / 5.28 | **names differ** |
| `Aqua` `#57B0A9` | **2.39 / 2.12** | `Teal` `#2A9D8F` | **3.10 / 2.75** | **names differ** |
| `Cornflower` `#6E86C9` | **3.31 / 2.94** | `Blue` `#264653` | 9.41 / 8.34 | **names differ** |

### What this shows

**The two palettes are not rivals — one is the other, corrected.** Not a single bench maker ink meets AA as
normal text; every Type bar value is a darkened form of a bench ink that does. `TypeBar.kt:102` already says
the sets *"must not be conflated"* (ADR-055 Decision 6), and that is right about their **values**. What it
never says is that they describe **the same five materials**, which is why they drifted apart in name.

Two of the five already keep the bench name — `Ink` and `Ochre` — so the convention exists. It was simply
applied to two swatches and not the other three.

### The answer to "Coral"

**Rename `Coral` → `Brick`.** The file asks whether a swatch should be named for a colour the design language
abolished. The measurement gives a better reason than taste to change it: **`#A63C22` already *is* the
contrast-corrected `Brick`.** Bench `Brick` is the only maker ink it could be a correction of, and `Brick`
fails AA on Cream by a hair (4.27) — exactly the kind of near-miss that gets a darkened text twin. Renaming
it is not inventing a name; it is **restoring the one the convention already implies**, and it retires the
abolished word as a side effect rather than as the goal.

**The stated rule, which is what actually needs freezing:**

> A text ink carries the **name** of the bench maker ink it corrects, and a **darker value** chosen to clear
> AA as normal text on both papers. The name names the *material*; the value serves *legibility*. They are
> the same ink in two jobs.

With that rule stated, `Coral → Brick`, `Teal → Aqua` and `Blue → Cornflower` all follow, and the vocabulary
question F-7 raised is closed by a rule rather than by a table of exceptions.

### The defect found on the way — Required Fix

**`Teal #2A9D8F` fails AA as text on both papers: 3.10 and 2.75.** It is the only member of a set whose
entire justification is contrast correction that was never actually corrected — it clears AA-Large (3:1) on
`--paper` by 0.10 and fails even that on Cream. A user setting body text in Teal gets text below AA, offered
by a control whose KDoc promises the opposite.

This is **not** a naming question and must not be bundled with one. It needs a corrected value; a darkened
Aqua in the same family as the other four corrections. Suggested starting point for measurement, not a
ruling: around `#1F7268` measures ≈5.0 on `--paper` and ≈4.5 on Cream — but the value is the owner's, and it
must be re-measured, not accepted from this note.

### Cost, unchanged from what the spec already states

Renaming touches `Copy.Type.INK_CORAL`, the announcement it composes, the test tag derived from it, and the
copy tests that assert it — the same cost `v21-typebar.html:746-748` records. Renaming three instead of one
multiplies that cost but not its kind. **Recommendation: take `Coral → Brick` now** (it answers the asked
question and carries the measurement behind it), fix Teal's **value** as a separate accessibility change, and
hold `Teal → Aqua` / `Blue → Cornflower` until the rule above is ruled on — so the renames land once, under a
stated convention, rather than twice.

---

## Device session 2026-08-15 (late) — the Type bar measurements the spec demanded

*SM-A176B, Android 16, density 420 (2.625 px/dp), build `a80cc07`. The Type bar was reached the way a user
reaches it: select a text element → `Size` on the selection context bar. A test zine was created for this and
deleted afterwards; `font_scale` was temporarily set to 2.0 and **restored to the owner's 0.9**.*

### Blocker 1 — DISCHARGED. All five swatches report a full 48×48 dp

`v21-typebar.html` demanded this be *"re-dumped, not re-reasoned."* Dumped:

| Swatch | `uiautomator` bounds | px | dp |
|---|---|---|---|
| `Ink` | `[307,1574][433,1700]` | 126 × 126 | **48.0 × 48.0** |
| `Coral` | `[433,1574][559,1700]` | 126 × 126 | **48.0 × 48.0** |
| `Teal` | `[559,1574][685,1700]` | 126 × 126 | **48.0 × 48.0** |
| `Blue` | `[685,1574][811,1700]` | 126 × 126 | **48.0 × 48.0** |
| `Ochre` | `[811,1574][937,1700]` | 126 × 126 | **48.0 × 48.0** |

The bounds are **edge-to-edge** (`433` → `433`, `559` → `559` …): the 48dp expansions abut exactly and never
overlap, so Compose has nothing to prune.

**Correction to this section's first draft.** It said `Swatch`'s KDoc *"must be corrected"* because it claims
four of five swatches report 40×48. Reading the KDoc rather than trusting the caption's summary of it shows
that is wrong: `TypeBar.kt:823-832` cites 40×48 as the measurement **at V1's 40dp pitch**, and 38.1×48.0 as
the measurement at V2.1's **first** 38dp pitch, then states *"At 48dp pitch the expansions abut exactly and
nothing is pruned."* That is exactly what the device now reports. **The KDoc was already correct; the spec
caption warning that it had gone stale is what was stale.** The dump confirms the KDoc rather than
correcting it, and is worth adding to it as device evidence for a claim that previously rested on a
Robolectric assertion.

### Blocker 2 — ANSWERED, and the answer is **no**

The question was *"which row is now widest, and whether the widest still fits a 360 dp screen at the largest
font scale."* Measured at `font_scale 2.0` on a 411dp-wide screen:

| Row | Extent | Width |
|---|---|---|
| Size | 74 → 1017 | 359 dp |
| Align | 74 → 1006 | 355 dp |
| Style | 74 → 1009 | 356 dp |
| **Colour** | **74 → 1030** | **364 dp** ← widest |

**`Colour` is the widest row**, and it is the *worst-behaved* one for a reason worth stating: its five 48dp
swatches are **fixed** — they do not scale with font — while its `COLOUR` label does. So the row grows with
font scale without any of its growth being absorbable.

- On this **411 dp** screen it fits, with about 19 dp to spare.
- On a **360 dp** screen the card's own `max-width:calc(100% - 24px)` leaves **336 dp**, and the row needs
  **364 dp** — it **overflows by roughly 28 dp**.

The spec's own worry was well-founded; its wording was *"the last time it was guessed, five swatches blew the
card to exactly 360 dp."* Measured rather than guessed, five swatches now blow it past 360.

**A second, visible defect at the same scale:** the `Align` row's `Right` button **wraps mid-word** — it
renders as `Righ` / `t` across two lines (bounds `[798,1170][1006,1412]`, 242px tall against its siblings'
142px). That is visible on the 411dp screen, i.e. it does not need the 360dp case to appear.

**Blocker 2 therefore remains open, with its answer now known.** It is no longer a measurement question; it
is a layout decision — the Colour row needs to wrap, scroll, or drop its fixed swatch size at large font
scales, and that choice is design.

## F-15 · Back does not dismiss the Type bar — it exits the editor · Required Fix

**Reproduced twice.** With the Type bar open, one press of **Back** returns to `Your shelf`. Not "Back fails
to close the panel" — Back discards the entire editing context in a single press, from a surface the user
opened to change a font size.

This sharpens the 2026-08-15 research recommendation rather than replacing it. That research recommended
registering `BackHandler` on the Type bar, on the grounds that the corpus's other overlays
(`pageGridOpen`, `inkPopoverOpen`) already do and this one does not. The device shows the consequence is
worse than an inconsistency: **the unhandled Back falls through to the editor's own exit.**

The accessibility consequence is the sharp end. TalkBack maps **swipe-down-then-left to Back** — so a
screen-reader user who opens the Type bar and performs the one canonical "get me out of here" gesture is
returned to the shelf. There is no announcement that this is what will happen.

**Fix:** `BackHandler` on the Type bar that closes the panel **and preserves the selection**, matching what
the research recommends for tap-outside. It is one line and it is the highest-value item this device session
produced.

### Two smaller findings from the same session

- **The bottom bar's `Done` reports `enabled=true clickable=false` while the Type bar is open.** An earlier
  review claimed tapping it is what closes the panel; the platform tree does not support that — it is
  enabled-looking and not clickable, which is the *same* semantics shape as the defect fixed in `a80cc07`.
  Whether it is deliberately withheld or accidentally unclickable needs one look at the call site; either
  way, a control that paints as available and reports `clickable=false` is the pattern this branch has
  already been burned by once.
- **During a text *editing session*, `Font` **and** `Size` are both disabled** — only `Ink` is live. F-1
  counted `Font` dead on the *selection* bar; on the editing bar `Size` joins it. You can recolour text while
  typing but not resize it, and nothing says why.

---

## Device verification 2026-08-15 (late) — the two fixes this session shipped

**Device** SM-A176B (`RZCYA1VBQ2H`), Android 16, `com.aritr.zinely` debug, `zinely-0.9.0-beta.1-debug.apk`
· override density **420** (2.625 px/dp) · `font_scale` **0.9** (unchanged by this pass, so every dp below is
measured, not converted from a different scale). Instrument: `adb exec-out uiautomator dump`, i.e. the real
`AccessibilityNodeInfo` tree TalkBack consumes.

### F-15 — PASS

With `Text: hello` selected, `Size` opened the Type bar: 18 nodes, the five swatches and both checkboxes
present. **One press of Back:**

| | before Back | after Back |
|---|---|---|
| Type bar | present (`Bold`, `Italic`, `Coral`, `Larger`) | **absent** |
| window focus | `com.aritr.zinely/.MainActivity` | **unchanged** |
| the element | `Text: hello` `checked=true` | **`Text: hello` `checked=true`** |
| context bar | withheld | **`Edit · Font · Size · Ink · Delete` back** |

That is exactly the three-part assertion `BenchC6Test` makes, now measured on the platform tree rather than
Robolectric's. The reproduced defect — one press returning to `Your shelf` — is gone.

A **second** press does exit the editor to the shelf, with the element still selected. That is the correct
next stop for Back and no work is lost, but it is worth noting that selection is *not* a step on the way
out: `Back` never means "deselect". OD-13 made tap-outside the only deselect path, so this is consistent
rather than a defect — recorded here so the next reader does not re-discover it as one.

### F-1 — PASS on what the instrument can read; the announcement itself is **limited by the instrument**

| control | surface | `enabled` | `clickable` | `content-desc` |
|---|---|---|---|---|
| `Font` | selection bar, text | `false` | `false` | **`Font`** |
| `Size`, `Ink` | selection bar, non-blank text | `true` | `true` | `Size`, `Ink` |
| `Replace` | selection bar, photo | `false` | `false` | **`Replace`** |
| `Font`, `Size`, `Ink` | editing row | `false` | `false` | **unchanged** |
| `Done` | editing row | `true` | `true` | `Done` |

The name half of the fix is verified here: no reason has leaked into `content-desc`, so TalkBack still calls
the control `Font`. **The reason itself cannot be read on device** — `stateDescription` is not in
`uiautomator dump`'s schema at all
([DEVICE-VERIFICATION.md §2](DEVICE-VERIFICATION.md)) — so per that document's own instruction this item is
recorded as *limited by the instrument*, and its machine evidence is the CI-26 platform-tier tests
(`BenchStyleRowPlatformA11yTest`, `BenchInkPresetPlatformA11yTest`), which read the same
`AccessibilityNodeInfo` from Robolectric and assert the exact strings. What still needs a human ear is
whether the sentences *sound* right when spoken — a Pass 2 item, not a machine result.

### The copy fix, tested against the device rather than against the reducer

Both halves of the `FINISH_TYPING` argument were checked on the phone rather than inferred:

- **`Add → Text`, then `Done` without typing** — the element is **gone**: no `Empty text` node, no context
  bar, no selection. So the rejected string *"Tap Done, then change it"* would have promised a thing that
  deletes itself, in the state a new user meets first. Confirmed, not merely reviewed.
- **Type, then `Done`** — `Font` stays `false`, and `Size` and `Ink` come back `enabled=true clickable=true`.
  So *"Finish typing to change this"* is a true statement about what happens next.

### One correction to F-15's own follow-up note

The note above claims that during a text editing session *"`Font` **and** `Size` are both disabled — only
`Ink` is live"*. The dump says otherwise: on the editing row **all three** of `Font`, `Size` and `Ink` report
`enabled=false clickable=false`, and `Done` is the row's only live control. That is what `BenchStyleRow`'s
own KDoc has always said (three inert chips, OD-9), and what the F-1 fix has now given three reasons to. The
"only Ink is live" reading was wrong.

### F-4 — implemented and device-verified, same session

`.padhint` now exists in Compose (`ReframeControls.kt`, `Copy.Reframe.ZOOM_IN_TO_MOVE`). The card became a
column exactly as the revised spec draws it: the control row keeps its geometry, the hint takes its own line
below at the card's own `--gap-sm`, in `--ink-soft` at 12.5sp — quiet ink, no warning colour, because it is
an instruction and an instruction in jam reads as a failure.

It is **composed conditionally, not hidden**. The moment either pan axis goes live the `Text` leaves the
tree, so TalkBack can never read stale advice on a pad whose arrows already work. That is the assertion with
teeth in `ReframePadHintTest`, and it was proved by mutation: forcing the condition true failed
`the_hint_leaves_the_tree_the_moment_one_axis_goes_live` and `the_hint_is_absent_on_a_fully_live_pad`.

**On device** (same SM-A176B session), reached through `Reset framing`, which returns a photo to the exact
entry state this finding is about:

| | reported |
|---|---|
| `Move photo up/left/right/down` | `enabled=false` |
| `Zoom out` | `enabled=false` |
| `Zoom in` | `enabled=true` — the act the hint names, never dim here |
| readout | `Zoom 100 percent` |
| the hint | **`Zoom in to move the photo`**, `[345,1782][736,1828]`, inside the card, centred under the controls |

One tap on `Zoom in` → 115%, all four nudges and `Zoom out` come live, and the hint is **gone from the tree**
(not merely faded). Both directions verified with the instrument rather than argued.

**Pass 2 reading.** The pad now answers the question a first-time user actually holds there — *"why won't it
move?"* — with the one sentence that gets them out of it. What it does not do is explain the *cause* (the
photo exactly fills its frame, so there is nothing to slide), and that is the right trade: the cause is a
fact about crop geometry the maker never needs to learn, and BP-4 forbids teaching it.

**Observation, not a finding.** The dead nudges report `enabled=false clickable=true` to the platform, where
the Bench's dim verbs report `clickable=false`. Both correctly refuse activation and both are deliberate —
`NudgeCell`'s KDoc records that `clickable(enabled = false)` is what carries the disabled bit there — but the
corpus now has two conventions for the same idea. Worth one owner call some day; it is not a defect today.

**Two defects independent review found in the first cut, both fixed before this section was final.**

1. **The hint advised a control the maker could not press.** The spec's own comment asserts it "can never
   contradict itself by advising an unavailable control", reasoning that `Zoom in` is live at the entry
   state. True of Fill — and false one chip away: `Framing.abilities()` returns `ReframeAbilities.NONE` for
   `FrameFit.WHOLE`, and `EditorScreen` uses the same `NONE` for the inert state before a photo's aspect
   resolves. Every nudge dead, `Zoom in` dead, and the pad telling the maker to zoom in. Tapping
   **`Whole photo`** is the whole repro. The condition now carries a `zoomIn` term the spec's sentence does
   not, guarded by `the_hint_never_advises_a_zoom_the_maker_cannot_reach`. **The spec is wrong here too, and
   its sentence should be amended when it is next opened** — the implementation is deliberately stricter
   than the frozen text, which is recorded rather than silently absorbed.
2. **The accessibility half was missing.** The spec binds the hint with `aria-describedby` so a screen
   reader states it on *entering* the group; the first cut left it a bare trailing node, which TalkBack
   reaches **after** the five dead controls it explains — the exact opposite. Compose has no `describedby`,
   and the nearest wrong answer (a `contentDescription` on the card) would merge the seven controls away.

   **My first repair for this was wrong, and my own test is what caught it.** I reproduced `describedby`
   with an `isTraversalGroup` plus `traversalIndex = -1f` on the hint, and wrote a test that asserted the
   index value — which passes whether or not the index does anything. Rewritten to assert the *property*
   through `platformTraversalStops`, it failed: the platform order was
   `[Move photo up, …, Zoom in, Zoom in to move the photo]`, byte-identical with the index set and with it
   removed. The harness's own note says why — Compose expresses re-sorting through
   `setTraversalBefore/After`, `UNDEFINED` on every node this repo has probed — and a device
   `uiautomator dump` returned the same order. **A test that asserts the mechanism cannot tell you the
   mechanism is inert.**

   What ships instead needs no ordering at all: the reason rides the four dead arrows as a
   `stateDescription`, which is the remedy F-1 already uses on the Bench and which is proven to reach the
   platform. Whichever arrow the maker reaches first tells them what to do, and it clears the moment the
   arrows come live, so the visible hint and the spoken one cannot disagree — one condition, two channels.
   `Zoom out` is deliberately excluded: it is dead because 100% is the floor, a different sentence, and
   "Zoom in to move the photo" on the zoom-*out* button would be nonsense.

Both fixes were proved by mutation: reverting either fails exactly its own test and nothing else.

**Two follow-ups this pass opens and does not close.**

- **No golden covers the entry state.** `ReframeControlsGoldenTest` renders an all-abilities-true band, so
  the new visual has zero pixel coverage — and the Row→Column change is pixel-identical *there*, which is
  why that suite stayed green and proves nothing about this. A `reframe_controls_entry_light/dark` pair is
  owed. Deliberately not added in the same change: a golden recorded alongside the code it guards asserts
  only that the code matches itself.
- **F-9 is now a live parity gap.** The same spec revision made `Reset` a `.text-btn` carrying a word;
  Compose still draws `ReframeIconButton(Icons.Filled.Refresh)`. HTML and Compose diverge on that point
  until it is implemented.

**Device re-check of both fixes** (SM-A176B, same session, rebuilt and reinstalled):

| state | reached by | hint |
|---|---|---|
| entry (Fill, 100%) | `Reset framing` | **present**, four arrows + `Zoom out` `enabled=false`, `Zoom in` live |
| `Whole photo` | one tap on the chip | **absent** — and `Zoom in` reports `enabled=false` there, which is exactly the contradiction the fix prevents |
| back to `Fill` | one tap | **present** again |

The spoken half cannot be read on device — `stateDescription` is not in `uiautomator dump`'s schema — so its
evidence is the platform-tier test (`every_dead_arrow_says_what_would_revive_it`, reading the real
`AccessibilityNodeInfo`) plus a human ear on TalkBack, which is still owed. **971 tests, 0 failures,
1 skipped.**

**Owed to the spec, not to the code.** `v21-reframe.html`'s comment claims the hint "can never contradict
itself by advising an unavailable control". That is false for `Whole photo`, and the implementation is
deliberately stricter than the frozen text until the sentence is amended.

### F-9 — implemented and device-verified

Both secondary session actions now draw a word. `ReframeIconButton` is gone (it had one caller) and
`CancelButton` generalised into `ReframeTextButton(word, spokenLabel, onClick)`, so the rule the spec states
— *both secondary actions keep their words* — is applied by construction rather than to one of the two.
`Copy.Reframe.RESET` is the drawn word; the spoken label stays the long `Copy.A11y.RESET_FRAMING`, which was
correct throughout and is exactly why no accessibility assertion in this repo could ever have caught this.

**Device**, SM-A176B, Reframe session open (a11y bounds, so ÷2.625 for dp):

| control | bounds | width |
|---|---|---|
| `Cancel reframing` | `[42,2046][215,2172]` | 65.9dp |
| `Reset framing` | `[236,2046][390,2172]` | **58.7dp** — sized to its word, not a square |
| `Done reframing` | `[411,2046][1038,2172]` | the primary, unchanged |

Screenshot reads `Cancel · Reset · Done`, matching `v21-reframe.html`'s markup exactly. No rotate glyph.

**The golden could not see this, and said so itself.** `ReframeControlsGoldenTest` went green straight
across the change — its own KDoc records that `captureRoboImage` is a no-op under plain
`testDebugUnitTest`. A visual defect passed the visual test. `ReframeBandWordsTest` now asserts the nearest
verifiable consequence — the two buttons share one height and differ in width, which a pair of icon squares
cannot do — and says in its KDoc that this is a proxy, because the drawn glyphs are only provable by a
golden that really compares or by the device.

⚠ **A correction to this document's own earlier measurement.** The Type-bar swatch entry above reads the
five swatches as "126×126px = 48.0×48.0dp" and treats that as their drawn size. It is not: 126px is what
**every** control in these dumps measures at its minimum, because Compose expands the *touch target* to
48dp and reports the expanded rect in the accessibility node — the 34dp nudge cells measure 126px too. The
blocker that measurement discharged was a **touch-target** blocker, which is precisely what the expansion
governs, so that conclusion stands unchanged. What it never showed is how large the swatch is *painted*.

---

### F-5 — implemented; the fix is the missing term, not a new mechanism

`BenchInkPopover` now reports its docked top edge (`onDockedTopChanged`, the same parameter and the same
`positionInWindow()` reading `BenchStyleRow` already had), and `EditorScreen`'s pan feeds
`benchEditPanMagnitudeDp` from **whichever panel is currently docked** over **whichever element that panel is
about** — the edited one, or, with the popover up, the selected text it is recolouring. The rule is
untouched: still `min(96dp, slack + clearance)`, still measured at rest, still D-043 / OD-16. What changed is
that a second occluder now pays it. The editing row wins when both could apply, which is a state the freeze
never produces (`ctxVisible` carries `!inkPopoverOpen`).

**This closes a hole this repo had written down and could not previously reach.** `BenchC3Test` records that
the clearance term has *no screen-level test anywhere*, because on a device its occluder is the IME and
Robolectric has no IME — so it survived on unit arithmetic plus a device-checklist item. The popover is not
the IME; it is an ordinary composable that measures and docks under Robolectric. `BenchInkClearanceTest`
(3 tests) is therefore the first place in the repo where `slack + clearance` is observed end to end, from a
real occluder's measured edge to a real page's displacement.

**Proven by mutation, not by green — and the first form of the proof was not good enough.** An independent
review killed the opening assertion as written (`panned < rest`): on that host the *slack* term alone
satisfies it, so a docked edge reported at the canvas bottom — the degenerate value the popover's own KDoc
warns about — passed while observing nothing about clearance. Two further defects surfaced fixing it:

- `boundsInRoot` is **clipped by parents**, so a page lifted to the 96dp ceiling saturates at the canvas's
  top edge and reads as a 43px lift. The measurement is now `positionInRoot`.
- the element had to be re-placed at 35pt rather than the page bottom, so that `slack + clearance` stays
  *below* the ceiling — otherwise the assertion measures the clamp, not the measurement.

Both mutations now kill it: reporting the docked edge 1000px low, and removing the popover term from
`occludedElement`. The third test — *closing returns the page to rest* — stays green under both by
construction, and is recorded as a guard against a stale docked edge rather than as evidence of the fix.

**Also fixed from that review:** a dead `edited == null` branch (smart-cast, permanently false), and
`BenchBottomBar`'s `@param doneEnabled`, which still said "a text session" after F-6 gave it a second state.

**Device — SM-A176B, Android 16, debug build of this working tree, page 1, one selected text element.**
Same element, measured from the platform tree on both sides of the tap:

| state | `Text: s` bounds | page displacement |
|---|---|---|
| selected, popover closed | `[253,811][827,1014]` | rest |
| `Ink` tapped, popover docked | `[253,650][827,853]` | **−161px = −61.3dp** |
| popover's `Done` tapped | `[253,811][827,1014]` | back to rest, to the pixel |

61dp is neither the frozen 96 nor zero — it is `slack + clearance` on a device whose slack is ~0, which is
the amended rule doing exactly what it does for the editing row. The screenshot shows the selected box and
both its handles standing clear above the card; before this change the same element sat behind its border.

⚠ **The ceiling still binds, and it is inherited rather than introduced.** A box deep at the page bottom can
need more than 96dp and will keep part of its box behind the card — the priced cost already recorded on
D-043, now owed by a second surface. Not re-decided here.

### F-6 — implemented, with the finding's stated cause corrected on device evidence

The fix is the one the finding proposed — `doneEnabled = editingElement == null && !inkPopoverOpen`, the
existing OD-14 condition gaining its missing term — but ⚠ **the reason given for it was wrong, and the device
says so.**

| the finding claimed | the device shows |
|---|---|
| "two identical green `Done` pills" | the popover's `Done` is a `--leaf` pill; the bottom bar's is a **dark stroked ✓** |
| `BenchBottomBar.kt:356` is the colliding pill | that line is inside `BenchAddButton`'s KDoc — the second green pill on screen is **`+ Add`** |

So the *visual* collision is `Done`-vs-`Add`, not `Done`-vs-`Done`. What the accessibility tree shows is the
collision the finding meant, in the one channel that cannot dress its way out of it —
`uiautomator dump` with the popover open returns exactly two nodes named `Done`:

```
'Done' | ''     | TextView | clickable=false | [905,927][984,973]     ← the popover's chip
''     | 'Done' | Button   | clickable=true  | [917,1898][1043,2024]  ← the bottom bar's ✓
```

Two controls, one name, simultaneously reachable. That *is* OD-14's defect, which is why the fix stands
while its stated cause does not. The rationale in `EditorScreen.kt` carries this correction at the code.

**Device confirmation, same session and same build.** With the popover docked, the bottom bar's `Done`
reports `enabled=false` to the platform, and it returns to `enabled=true` the instant the popover's own
`Done` closes it. One live `Done` at a time, in the tree TalkBack actually reads.

**F-1's rule followed F-6 to this control.** Withholding `Done` gave it a second reason to be dim and no way
to say either, so a screen-reader user heard *"Done, disabled"* and stopped — the same defect F-1 fixed one
file over. `Copy.BenchBar.DONE_AFTER_TEXT` / `DONE_AFTER_INK` now ride `stateDescription`; two strings, not
one, because a text session is finished by the row's `Done` and an ink session by the card's, and a shared
sentence would be true of both and useful for neither. `Undo` and `Redo` deliberately stay silent: nothing
revives them but doing something, which is not an instruction anyone needs. Pinned by two platform-tier
tests in `BenchBottomBarPlatformA11yTest` — one that a withheld `Done` says why and keeps its name, one that
a live `Done` says nothing at all.

**A first-time user still meets two green pills** (`Done` in the card, `+ Add` below it) and that is a
separate question — one about whether the panel should suppress `Add`, which OD-14 never ruled on. Not fixed
here, because narrowing it to the `Done` rule would be inventing a ruling; recorded as owed to the owner.


---

### F-2 — fixed by wrapping, on the owner's ruling; the first fix is kept on record as wrong

The transform row **wraps** instead of scrolling. Eleven ≥48dp controls cannot share one line on a 360dp
phone, every target must stay ≥48dp, so the only axis left is vertical.

**Device, SM-A176B, photo selected (ten controls), platform tree:**

| before | after |
|---|---|
| nine laid out; `Send backward` **absent from the platform tree** | **ten** laid out |
| `Bring forward` `[1059,2077][1080,2203]` — 21px | `[412,2077][538,2203]` — **126px = 48dp** |
| — | all ten measure 126px; two on a centred second line |

**⚠ The first fix was a scroll hint, and the device disproved it.** A gradient fade at each scrollable edge:
tagged, three tests, all three killed by mutation — and still wrong, because **the clipped control is a 21px
slice of an *empty* pill**, its glyph centred 40px past the screen edge. The fade faded nothing, and a fade
over the desk is the desk. There was no half-seen button to make legible, which was the assumption the whole
fix rested on. It also shipped a layout regression the entire suite passed: `fillMaxHeight()` on an overlay
resolves against the incoming **maximum** height, so the bar grew to fill the column and squeezed the page
canvas to nothing — 980 green tests against an editor with no page in it.

**The price, stated plainly.** Wrapping costs a second 48dp row of chrome. The owner ruled to pay it
(2026-08-16) on the ground that two controls unreachable by touch and one absent from the accessibility tree
is the worse trade.

**What that price exposed was a real bug, and the first explanation of it was wrong.** On the 300×400dp host
`EditorScreenTest` uses, the move/resize hint's `Got it` stopped working: the hint vanished, but because the
**selection** had been cleared, not because it had been dismissed — so it returned on the next selection and
the persistence flag was never written. I first wrote this up as a host-realism artifact and raised the two
tests to a phone-height host. A Review Agent refused that explanation on the grounds that it was asserted and
never isolated, and it was right.

Isolated, by probe, at the original host:

| stimulus | `onMoveResizeHintSeen` | selection after |
|---|---|---|
| injected tap on `Got it` | **false** | **`[]`** — cleared |
| the same button's semantics action | true | intact |

So the button and its handler were correct and the *tap* never reached them. The page gesture surface is
`fillMaxSize()` over the whole canvas (`EditorScreen.kt:1118`) and its miss branch deselects (D-037);
composition order alone did not keep the notice's one control above it. `Modifier.zIndex(1f)` on the hint
fixes it, and with the fix in place the two tests pass at the **original 400dp host** — so the host change is
reverted and unnecessary.

**This was reachable on a phone**, not an artifact of a small harness: any layout where the hint overlaps the
gesture surface has the same race, and the hint is drawn over the canvas by design. It is now pinned by
`the_hints_own_button_wins_the_tap_against_the_full_screen_gesture_surface`, which asserts the two facts that
tell a dismissal from a deselection — the callback fires **and** the selection survives — and which no
existing test checked. Removing the `zIndex` turns it red.

**Correcting this document's own arithmetic:** an earlier draft of this section said chrome "became 156dp".
The wrapped row is `48·2 + 2 + 4·2 = 106dp`, a **~50dp delta** from wrapping. 156dp was the whole bottom
region including the navigation-bar inset, not the row.

**Still true and still owed:** this row is **specified nowhere** — `v21-bench.html` has no `bring forward`,
no `send backward`, no nudge control. Wrapping is a bug fix against an omission, and until the row exists in
the freeze there is nothing to verify either layout against.

### F-3 — re-scoped, not implemented: this is a postponed decision, not missing parity

The finding says Compose failed to implement a frozen surface. That is wrong in its operative half, and
building it now would be a false promise. The frozen `Art` sheet (`v21-bench.html:840-861` — F-3's own
citation `:848-860` is short at both ends) needs, before a single composable:

| requirement | repo state |
|---|---|
| `DecorElement(supplyId, ink, mirrored)` | `Element` is `ImageElement \| TextElement` (`core/model/.../Document.kt:66`) |
| `DrawShape` render command | `DrawCommand` has `FillRect`, `DrawImage`, `DrawTextBox` only |
| `AffineTransform2D.scale` | absent — `localToPage` has no scale term, so a 0..1 outline renders 1pt square |
| the sixteen authored outlines | **named in prose only** (`SUPPLIES-SPEC.md:388-391`, whose scope line reads *"documentation only. No code has been written."*) |

And three owner rulings already sequence it away: **OD-2** re-seated `DecorElement` beyond Phase C, **OD-21**
released only Text and Photo, and **ADR-105 D-4** puts the photocopier filter *ahead* of the supplies —
*"decorating a product before it has its voice is the wrong order."* The one fence that has lifted is the
legal one (ADR-104 is Accepted); the model and sequencing fences have not.

**What would be a false promise, and is therefore not done:** drawing the `Art` row inert (`BenchAddChooser`
already argues from C3's Pass 2 that a truthful-but-dead control invites the press harder than a blank one);
shipping the prototype's four placeholder glyphs in place of the sixteen; a ⭐ favourites row with no
persistence; or rewriting `SUPPLY_CUE` on my own authority, which is exactly what **D-050** is open about.

**The honest sentence for the owner:** *the empty state is not promising a surface we forgot to build — it is
promising a product decision we made and then postponed twice.* Either the copy retreats to what exists
today, or the sequencing changes. Both are owner calls; neither is parity work. The user-visible half merges
into **D-050**; the capability half waits on **D-029**.
