# Zinely Beta UX Review — 0.9.0-beta.1

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
