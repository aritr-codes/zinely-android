# Zinely — product direction

**Status:** Decided · 2026-08-15 · Implementer acting as Art Director / Senior Product Designer / Product Owner under owner delegation
**Ratified law:** [ADR-103](../DECISIONS.md#adr-103) + [Constitution Amendment 2](V2-CONSTITUTION.md#amendment-log) (the world) · [ADR-104](../DECISIONS.md#adr-104) + [Constitution Amendment 3](V2-CONSTITUTION.md#amendment-log) (the asset layer) — **both owner-adopted 2026-08-15 and landed.** `v21-bench.html` amended under ADR-104.
**Supersedes:** [BETA-DIRECTION.md](BETA-DIRECTION.md) — folded in whole, including its reconciled review corrections
**Research inputs (evidence, not authority):** [ZINE-WORLD.md](ZINE-WORLD.md) · [PRODUCT-DIRECTION.md](PRODUCT-DIRECTION.md)
**Law:** [V2-CONSTITUTION.md](V2-CONSTITUTION.md) · [V21-SPEC.md](V21-SPEC.md) · [DECISIONS.md](../DECISIONS.md). Where this document changes law it says so and drafts the amendment.

### Evidence labels used throughout

| Label | Meaning |
|---|---|
| ✅ **VERIFIED** | I read the file, ran the grep, or measured it on the device. Line references given |
| 🟨 **INFERRED** | Follows from verified facts but I have not executed the path |
| 🔍 **NEEDS INVESTIGATION** | Genuinely unknown. Named as work, never costed |

No estimate in this document rests on an unlabelled assumption. The last review found six of eight of my "already exists" claims misstated; the labels are the fix.

---

# 1. Executive product direction

**Zinely is a small press that fits in one hand.** You make an eight-page zine from photos, words and marks, and print it on one sheet of ordinary paper, folded by hand into a booklet. No account, no cloud, no network.

**What Zinely is uniquely good at — the north star:**

> **Turning what is already on your phone into a physical object you can hold, in one sitting, without knowing anything about printing.**

Every word carries weight. *Already on your phone* — the camera roll is the raw material, and the phone is the one device that has it. *Physical object* — the loop ends in paper, not a file. *One sitting* — eight pages is a form you can finish, which is why the constraint is a feature. *Without knowing anything about printing* — imposition, creep, gutters and bleed are the press's job, and the maker never meets those words.

Nothing else does this. Canva is a design tool that can output PDF; a photo book app is a print-order funnel; a scrapbook app never reaches paper. **Zinely's competitor is a photocopier and a glue stick**, and it wins by being in your pocket.

**What it is becoming:** the same product, finished. Three things stand between here and complete:

1. **A third creative primitive.** Text and image exist; **Decor** is half-built and structurally required — the app decorates itself with tape, stamps and cut paper while withholding all three from the maker.
2. **Finishing the loop's edges.** Camera in, page images out, duplicate, page reorder, spreads, and a colophon. Each closes a hole a real user hits in the first session.
3. **One visual grammar instead of two.** The product currently speaks Zinely on the Proof screen and Material everywhere a list appears.

**What it will never become:** a layout tool, a social product, an asset marketplace, or an app with a sign-in screen.

---

# 2. The world

## 2.1 The metaphor, decided

> # Zinely is a small press that fits in one hand.
> **The café is how it feels. The press is what it is.**

"Small press" is the actual publishing term for a one-person independent publisher, and it carries both meanings at once: **the machine** (something comes off it) and **the institution** (it is yours, it is tiny, it answers to nobody).

It is the only candidate that survives all five tests the corpus imposes:

| Test | Café | Studio | Desk | Scrapbook | **Small press** |
|---|---|---|---|---|---|
| Explains the vocabulary law — *"paper, ink, presses, shelves, folds"* (`V2-CONSTITUTION.md:77`) | ✗ not one café noun | partly | partly | ✗ | **✓ every noun** |
| Explains why the north star is a verb — *"FINISHING. One word"* | ✗ | ✗ | ✗ | ✗ | **✓ its whole purpose** |
| Explains ADR-090 — the artifact is lit, the room may darken | ✗ | partly | partly | ✗ | **✓ the sheet is under the lamp** |
| Survives one focal zone (a phone has no peripheral vision) | ✓ | **✗ fatal** | ✓ | ✓ | **✓ a bench, not a room** |
| Answers *"what is a Type bar?"* | **✗ unanswerable** | vague | ✓ | ✗ | **✓ a tool you picked up** |

The café was doing two jobs — supplying the *feeling* and supplying the *place* — and it is only good at one. It keeps the job it is good at: quiet, warm, unhurried, private, yours. What it never supplied was an answer to "what is this screen?", which is exactly why surfaces drifted apart.

"Studio" fails hardest and most usefully: a studio is a room you look *around*, and a phone has one focal zone. **A bench is a studio scaled to a phone.**

## 2.2 What each thing is

| Thing | In the world | Consequence for design |
|---|---|---|
| **The Shelf** (`HomeRoute`) | Where finished copies stand. Yours, physical, a little untidy | Covers are objects — tilted, taped, stamped. Never a data grid. Never a file manager |
| **The Bench** (`EditorRoute`) | The lit work surface. One sheet under a lamp, tools within reach | Light paper in both themes (ADR-090). The page is the hero; tools are guests that arrive and leave |
| **The Press run** (`ProofRoute`) | Where the sheet comes off and becomes an object | Shows the *sheet*, honestly, then hands you a fold. The one place print reality is visible |
| **A document** | **One zine** — a small edition of one title, of which you can print many copies | Never "project" or "file". Duplicating a zine is *a new title*, not a backup |
| **A page** | **One panel of the folded sheet.** Fixed at eight, because the fold says eight | The fixed count is the form, like a sonnet's fourteen lines. Page 9 is not a missing feature; it is a different fold |
| **A tool** | **Something you pick up, use, and put down.** Type bar, Ink pots, Reframe, Supplies | Tools appear at thumb reach in a fixed position, do one job, and leave. A tool that stays on screen is clutter, not a tool |
| **A material** | **Stock you place and cannot un-invent.** Photos, words, tape, stamps, cut paper | Materials are finite, named, and authored. Five inks, three voices, sixteen supplies. Never a picker over infinity |
| **Importing** | **Bringing stock to the bench** — from the drawer (gallery), from the camera, from another app | Import is not "upload". It never leaves the phone, so it is a *reach*, not a *transfer* |
| **Exporting** | **The press run.** Ink meets paper; a sheet exists that did not before | The primary output is a foldable sheet. Page images are offcuts you may also keep |
| **Settings** | **The colophon** — the printer's note at the back of a book saying how it was made | See §2.3. This is the single best fit in the whole metaphor and it solves a legal obligation |
| **A future feature** | **A new supply, a new mark, or a shorter path to the press run** | If it is none of those three, it belongs to a different product |

## 2.3 Settings is a colophon

✅ **VERIFIED: no settings surface exists anywhere in `src/main`.** No route, no screen, no file. For a complete product that is a gap — but "Settings" is a generic-Android answer, and the press already has the right object.

A **colophon** is the note at the back of a printed book stating how it was made: the press, the typefaces, the paper, the edition. It is not a preferences panel that apologises for itself; it is *a statement of craft*, and it is the one place a confident product is allowed to talk about itself.

**It carries exactly four things, and each earns its place:**

| Contents | Why it belongs |
|---|---|
| **Default paper** — US Letter / A4 | ⚠ **Weaker than I first claimed.** I said the default is *"hardcoded `LETTER`, wrong for most of the world"* (`EditorBootstrap.kt:25`). ✅ Falsified: that function's own KDoc says *"Kept for test fixtures; production creation is the store's `createProject`"*, every caller is a test, and `HomeViewModel.kt:196` states *"the shelf's paper chooser decides; **nothing is hardcoded here**"*. The chooser even lists A4 first, and Proof can re-pick per press run. So this is a genuine **new preference** — a remembered first-launch default so returning users stop re-choosing — not a fix to a wrong value. It stays, demoted |
| **Typefaces used, with their licences** | ✅ **Stronger than I claimed, and the plank the colophon actually rests on.** **Four** OFL licence files ship and are reachable from nowhere: `feature/editor/src/main/assets/fonts/OFL-{AveriaSansLibre,Fraunces,Inter}.txt` and `render-android/src/main/assets/fonts/OFL.txt`. The OFL requires the licence travel with the fonts. A colophon discharges a real obligation *and* reads as craft |
| **How Zinely works** — the offline statement, **stated once, here** | §13's subtraction moves four scattered privacy reassurances into one confident sentence in the one place it is not defensive |
| **Version, and nothing else** | No account, no sync, no analytics toggle, no notification settings. There is nothing to configure because there is nothing running |

**Reached from the Shelf**, not the Bench — you read a colophon when you are not working. **BUILD.**

## 2.4 Constitution: an amendment is required

The existing constitution is **not** sufficient. §I's felt promise names one image — *"a quiet café where you make tiny books with your hands"* — and it has been carrying two jobs, authoritative on the emotional one and silent on the spatial one. That silence is measurable: four documents named four different places, and no document could answer "what is this screen?"

Ready to paste into `V2-CONSTITUTION.md` §VI and to land as **ADR-103** (current max is ADR-102):

> ### Amendment 2 — the spatial metaphor (2026-08-15)
>
> §I's felt promise — *"a quiet café where you make tiny books with your hands"* — has been carrying both the
> product's **emotional register** and its **spatial metaphor**. It is authoritative on the first and silent on
> the second, which is why four documents named four different places and none could answer *"what is this screen?"*
>
> **The register is unchanged and remains binding: quiet, warm, unhurried, private, yours.**
>
> **The spatial metaphor is now named: Zinely is a small press that fits in one hand.** It has three places —
> the **Shelf** (finished copies), the **Bench** (the lit work surface), and the **press run** (where the sheet
> comes off and is folded) — plus one statement of craft, the **colophon**. Every surface belongs to one of them.
> A surface that belongs to none is mis-homed, and is redesigned or removed.
>
> This amendment reconciles rather than overrules. It is required because §I's image cannot answer the object
> question, while `zinely-constitution.md`'s north star — *"FINISHING. One word."* — and §II's vocabulary law —
> *"paper, ink, presses, shelves, and folds"* — already describe a press. The metaphor names what the corpus and
> the shipped code were already doing.
>
> **Consequent:** [`DESIGN-LANGUAGE.md`](DESIGN-LANGUAGE.md) is marked **superseded** in full. Its craft-table
> metaphor (`:64-65`), coral-on-charcoal palette (`:71-73`), tilt-and-tape licence (`:74-75`), marker-face
> typography (`:78-80`) and overshoot motion (`:208-213`) have each been overruled elsewhere without the document
> ever saying so.

---

# 3. Design principles

Ten principles. Each is a *test you can fail*, not an aspiration.

1. **The page is the hero; the tool is a guest.** Chrome arrives when needed and leaves. If two rows of controls are permanently visible below the page, the screen has stopped being a bench.
2. **The sheet is under the lamp.** Light means the thing you are making; dark means the room you stand in. There is no third case. Tools never become paper, however close they float. *(ADR-090)*
3. **Materials are finite, named, and authored.** Five inks, three voices, sixteen supplies. A picker over infinity is an abdication — curation *is* the design work.
4. **Imperfection is intentional and quantified.** Tape tilts 1.4° because a hand stuck it down. Randomness may set an initial value; it may **never** be a running behaviour. Nothing re-rolls.
5. **Every mark names its physical cause.** If you cannot say what object made it, it does not ship. This governs texture, shadow, motion and decor alike.
6. **Show, don't explain.** A confident product states a thing once, where it matters. Repeated reassurance reads as anxiety.
7. **Nothing is inert.** No permanently disabled control, no dead end, no button that announces itself as activatable and isn't. A drawn control either works or is not drawn.
8. **Accessibility is a primitive's definition, not a pass at the end.** A new element type is not done when it renders; it is done when TalkBack can move, scale, rotate, reorder and delete it.
9. **The user never learns print.** Imposition, creep, bleed, gutter, signature — the press does that work silently. Meeting those words is a design failure.
10. **Priority order, never reversed:** user value → usability → coherence → quality → distinctiveness → delight. A beautiful interaction that harms a core task is not a good interaction.

**The line that settles most arguments:**

> **Put the handmade quality in content, typography, copy, and imperfection you can quantify. Keep it out of controls, hit targets, focus indication, and iconography.**

---

# 4. Visual grammar

## 4.1 Global rules — true on every surface

| Element | Rule |
|---|---|
| **Paper** | `paper` is the sheet you make on. Light in both themes. Never used for chrome, tools, or cards that float on the bench |
| **Room** | `desk`/`bench` is the surface you stand at. Darkens with theme; the artifact does not |
| **Ink** | Every border, outline and stroke. One ink weight per role, not per component |
| **Colour** | Five named roles, no sixth. `leaf` = your next move (the one action colour) · `berry` = punctuation, current-page, the printer's-reach guide, **never an action** · `jam` = the only urgent colour · `butter` = **material only** (tape, stamps, rings) — never an action, never text · `inkFaint` = decorative fills and strokes only, **sets no text** |
| **Borders** | Solid `ink` = a thing. Dashed `ink` = a boundary. Dashed `jam` = a cut. Solid `leaf` = your next move. Solid cream = a thing that moves |
| **Shadows** | Offset, **zero blur**, always down-right, full `inkLine`. *A printed shadow, not elevation.* Four press tiers: Hero 4→2→1 · Raised 3→2→1 · Flat 2→2→0 · Inline 2→1→1 |
| **The `--frame` ring** | 5px flat ring outside the hard shadow. **One per screen**, on the primary action only. Justified as riso misregistration |
| **Typography** | Voice = Averia Sans Libre · Editorial = Fraunces · Work = Inter. **The imperfect face never sets running text** (constitutional, Amendment 1) |
| **Labels** | The one recurring chrome pattern: all-caps section labels, 10–11sp, weight 600–700, tracking .12–.13em — via the **shared `sectionLabel` token** (`ZinelyV2Typography.kt:154`), not seven drifted values |
| **Texture** | *"If a texture cannot name its physical cause, it does not ship"* (`V2-CONSTITUTION.md:104`). Decorative textures are banned outright (`:264`) |
| **Tilt** | Objects at rest sit at ±0.6–2°. **Objects being worked on never tilt** (`V21-SPEC.md:494`) |

## 4.2 Contextual rules — cohesion, not uniformity

A book is cohesive because its chapters belong together, not because every page is identical. Each place gets a distinct *density*, within one grammar:

| Place | Character | Local licence |
|---|---|---|
| **Shelf** | Objects at rest. Warm, sparse, slightly untidy | Tilt is at its most generous. Tape and stamps appear as *material*. Deep press tiers |
| **Bench** | The work surface. Quiet, dense at the edges, empty in the middle | **Tilt is forbidden** — you are working on it. Chrome is flat/inline tier. Ink is quieter so the page is loud |
| **Press run** | Ceremony. The one moment of arrival | The single `--frame` ring lives here. Hero tier. The fold diagram is the most literal drawing in the app |
| **Colophon** | A printed page, read not used | Editorial face allowed for body. No controls beyond one choice. Most text-dense surface in the product |

## 4.3 Icons — the one rule the product currently breaks

**Icons are drawn objects from the world, at one stroke weight, filled only where the object is filled.** ✅ Verified divergence: `ZineActionSheet` and `BenchAddChooser` use Material's rounded-square icon tile — Material's list grammar wearing Zinely's colours, sitting next to hand-drawn tape and fold diagrams. **The tile goes.** An icon sits on the row, not inside a container.

Same for the `⋮` kebab: it is Android furniture in a room with no other Android furniture. Long-press is the world's gesture; the affordance is a visible, drawn one.

---

# 5. Interaction grammar

## 5.1 The physical logic

Four causes. Nothing moves without one.

| Cause | Where | Behaviour |
|---|---|---|
| **Something was set down** | page settle, element drop, sheet close | Decelerate ~300ms, **no bounce** — paper is damped, not rubber |
| **Something was pressed** | every button | Hard-shadow press tier, ≤100ms, instant return |
| **A drawer was pulled** | sheets, popovers, tool panels | Translate from the edge it belongs to, 200–250ms |
| **A mark was made** | selection outline, guides, boundaries | Appears **drawn**, ≤100ms, **no opacity ramp** |

**Banned, by physical cause:** parallax (nothing in a press has depth of field) · idle or floating loops (nothing on a bench drifts) · spring overshoot (paper does not bounce — this resolves the unreconciled `DESIGN-LANGUAGE.md:208` vs `V2-BENCH-PRINCIPLES.md:143` conflict in favour of the latter) · cross-fades between screens (you do not dissolve between rooms).

The fourth cause is the cheapest win in the document: **a selection outline that fades in reads as software; one that appears at once reads as a pencil line.**

`prefers-reduced-motion` downgrades duration and distance; it never removes the state change.

## 5.2 Touch

- **Tools live in fixed positions at thumb reach.** A printmaker reaches without looking. Never move a tool for layout convenience.
- **Tap places. Two fingers compose.** Rotate + scale + drag simultaneously is the one operation a mouse physically cannot do, and collage consists of it.
- **Press-and-lift is the stamp.** The phone's native verb and the stamp's are the same gesture — the product's luckiest coincidence.
- **Long-press opens the object's own menu** (a page in the grid, a zine on the shelf). Never a kebab.
- **Snapping is a magnet, not a grid.** It resists, then yields, and it says so with a drawn guide.
- **48dp minimum, no exceptions.** ✅ Measured defect: ink pots sit at **38.0dp** pitch; `Bring forward` renders **10px wide**.
- **Nothing commits until the press run.** Placement stays reversible; undo is always there.

## 5.3 Selection is the spine

Selection is how every element is edited, so it carries the whole editing model: a dashed-ink outline with square handles, a context bar of verbs for that element kind, and — critically — **the same verb set is reachable by TalkBack custom actions** (§12). Get selection right once and each new primitive inherits it.

---

# 6. Product capability map

The complete creative loop, audited stage by stage. Verdicts are decisions, not observations.

## capture / import

| Capability | State | Verdict |
|---|---|---|
| Gallery import | ✅ ships | **Already complete** |
| **Take a photo** | ✅ verified absent — `ACTION_IMAGE_CAPTURE`, `TakePicture`, `CAMERA` permission: zero hits in all `.kt`/`.xml` | **Missing but important → BUILD.** The one input device the user is holding is unreachable |
| **Share-sheet receive** | absent | **Missing but important → BUILD.** Makes every app on the phone an input to Zinely |
| Clippings tray (a holding area per zine) | absent | **Valuable but later.** Document-model change with no prototype |

## create

| Capability | State | Verdict |
|---|---|---|
| New zine, name, paper size at creation | ✅ ships (`startZine(paperSize)`) | **Already complete** |
| Paper size — chosen at creation, re-pickable at Proof | ✅ ships. `ShelfSheets` chooser lists A4 first; `ProofPrint` re-picks per press run (session state, not a document write) | **Already complete.** A *remembered* default is the only gap, and it belongs to the colophon |
| Empty-state teaching | ✅ ships, and is good | **Already complete** |

## compose

| Capability | State | Verdict |
|---|---|---|
| Text: create, edit, size, bold/italic, align, ink | ✅ ships | **Already complete** |
| Text: **font choice** | ✅ drawn, permanently disabled (`BenchContextBar.kt:99`) | **Needs completing → BUILD** as three named voices |
| Image: import, reframe, resize, rotate, position, delete | ✅ ships | **Already complete** |
| Image: **replace** | ✅ drawn, disabled; `Intent.ReplaceImage` exists and is dispatched from nowhere | **Needs completing → BUILD.** Closes D-038 |
| **Decor / graphics** | ✅ half-built: `DecorElement` prior art (OD-2), decor verb set in the freeze at `v21-bench.html:625`, `BenchInkPopover.kt:140` handles `DECOR`, and `BenchContextBar.kt:125` **throws** | **Missing but structurally required → BUILD.** §9 |
| **Duplicate element** | ✅ verified absent from `Intent` | **Missing but important → BUILD.** Repeated marks are the medium |
| Layering | ✅ implemented 3× (`ZOrder.kt:37`, `EditorContextBar.kt:177`, `EditorA11y.kt:70`) but **both buttons clipped** | **Needs fixing.** Layout defect, not missing capability |
| Snap guides, nudge, scale, rotate | ✅ ships | **Already complete** |
| Alignment / distribute palettes | absent | **Not worth building.** Layout-tool thinking; snapping is the phone answer |
| Canvas zoom/pan | ✅ `SetViewport` plumbing exists; zoom is element-level only | **Valuable but later.** A second zoom makes pinch ambiguous on the most-used gesture |

## organize

| Capability | State | Verdict |
|---|---|---|
| Page add / delete / navigate | ✅ ships | **Already complete** |
| **Page reorder** | ✅ absent in code — **but specified in the freeze** (`v21-bench.html:766`: *"tap to jump, drag to reorder"*) | **Needs completing → BUILD.** This is parity, not a new feature |
| **Page duplicate** | absent | **Missing but important → BUILD.** With a fixed fold you repeat layouts rather than add pages |
| Page grid shows page **content** | ✅ draws eight blank numbered cards | **Needs fixing.** Reads as *"my pages are gone"* |
| Shelf: rename, duplicate, delete + undo | ✅ ships | **Already complete** |

## refine

| Capability | State | Verdict |
|---|---|---|
| Undo / redo | ✅ ships — unbounded `List<Command>` stacks (`EditorModel.kt:71-74`) | **Already complete.** 🔍 depth cap unexamined; commands are small mementos, so this is a question not a defect |
| Autosave / recovery | ✅ ships | **Already complete** |
| **Photocopier filter** (1-bit dither) | absent | **Missing but important → BUILD.** Highest identity-per-line in the product |
| **Actual-size preview** | absent | **Quality-of-life → BUILD.** *"Hold this against a sheet of paper"* — a phone can, a laptop cannot |

## output

| Capability | State | Verdict |
|---|---|---|
| PDF export, imposed sheet | ✅ ships | **Already complete** |
| Fold instructions | ✅ ships and is the best surface in the app | **Needs completing** — promote it out of a drawer (§7) |
| **Page images (PNG)** | ✅ **exporter ships end-to-end**, unreachable: `ZineExporter.kt:175` handles `ExportFormat.PNG`; `ZinelyNavHost.kt:290,310` hardcode PDF. But it writes the *imposed sheet* | **Needs completing → BUILD** a per-page reading-order mode (§11) |
| Share | ✅ ships | **Already complete** |
| **Direct print (`PrintManager`)** | absent | **Valuable but later.** Driver re-scaling silently mis-places every fold |

## the product around the loop

| Capability | State | Verdict |
|---|---|---|
| **Colophon / settings / licences** | ✅ verified absent entirely | **Missing but important → BUILD** (§2.3) |
| Empty / loading / error / success states | ✅ ship | **Already complete** |
| Haptics | ✅ ships | **Already complete** |
| Accessibility | ✅ **11 shared + up to 2 type-specific** custom actions (`EditorA11y.kt:51-77`), wired via `ElementSemanticsLayer.kt:93,120` | **Already strong.** Extend to Decor; fix the two defects (§12) |
| Onboarding tour | absent | **Not worth building.** Empty states already teach |
| Templates gallery | absent | **Not worth building.** *"Blank is a peer"*; a gallery makes blank the failure state |

---

# 7. Current-state audit

## What is already good — and should be the model

**The Proof screen is the most coherent surface in the product.** The page tilts because it is at rest. `COVER · 1 OF 8` is a stamped dashed tag. A butter tape strip physically attaches the commit band. *"8 pages · one sheet, one cut · US Letter"* is concrete and honest. `Save PDF` carries the screen's one ring; `Share` is correctly demoted.

**Judge every other screen against Proof, not against a mockup.**

Also genuinely working: the Shelf's tilted taped cover with the `US LETTER` stamp straddling its edge · the printed `1 / 8` on the page · the dashed-ink selection with square handles · the empty page's three stamp tiles · the fold act's line legend · the eleven shared TalkBack actions on every element.

## What is broken

| # | Defect | Evidence |
|---|---|---|
| 1 | **`Bring forward` is 10px wide** and reports `enabled=true, clickable=false` — TalkBack announces an activatable button that cannot be activated. On `EditorContextBar`; its sibling `BenchContextBar.kt:296-309` already landed the fix | ✅ device dump |
| 2 | **Ink pots sit at 38.0dp**, below the 48dp floor | ✅ measured |
| 3 | **`BenchContextBar.kt:125` throws** on `BenchVerbKind.DECOR` | ✅ read |
| 4 | **Two current-page colours** — `BenchPageNav`/`ProofFold` use `berry`, `BenchPageGrid` uses `leaf`, one inch apart | ✅ read |
| 5 | **PNG export is unreachable** because navigation hardcodes PDF | ✅ read |

## What is incomplete

Font choice (drawn, dead) · Replace (drawn, dead) · Decor (half-built) · page reorder (in the freeze, not in the code) · page grid draws no content · the fold act buried in a drawer · no colophon · gallery-only import · no duplicate.

## What should be removed

See §13.

## The structural finding

Step 14 of the walkthrough is *export/share*. **But the loop does not end there — it ends when the user is holding a folded booklet.** The app knows this (the fold act exists and is excellent) and then hides it behind a modal drawer at the end of a screen most users treat as the finish line.

**Decision: after a successful save, "Fold it up" is the primary continuation**, not a secondary icon in the top bar. One screen's change; makes the product's whole thesis legible.

---

# 8. Feature decisions

| Feature | Decision | Reasoning |
|---|---|---|
| World metaphor = small press | **BUILD** (ADR-103 + Amendment 2) | Only candidate explaining the vocabulary law, the FINISHING north star, ADR-090, one focal zone, and "what is a Type bar?" |
| Colophon (settings/licences/paper default) | **BUILD** | Verified absent; discharges the OFL obligation; the metaphor's best fit |
| Font as three named voices | **FINISH** | ⚠ needs 8 static TTFs sourced — *not* "asset cost already paid". Decision holds because a dead control is a launch blocker |
| Replace image | **FINISH** | Reducer intent exists; closes D-038 |
| Decor / DecorElement | **BUILD** | §9. Half-built, freeze-anticipated, and the product contradicts itself without it |
| Duplicate element | **BUILD** | Verified absent; `PlaceCommand` generalises to it |
| Page reorder | **FINISH** | Already in the frozen spec — parity work |
| Page duplicate | **BUILD** | Fixed fold makes layout repetition the real need |
| Page grid draws content | **MODIFY** | Reads as data loss |
| Take a photo | **BUILD** | Verified absent; `FileProvider` already declared; no `CAMERA` permission needed |
| Share-sheet receive | **BUILD** | Zero permissions; every app becomes an input |
| Photocopier filter | **BUILD** | Zine authenticity *is* the photocopier look |
| Actual-size preview | **BUILD** | Trivial-looking, answers a real first-timer anxiety, phone-exclusive |
| Page images (PNG) | **FINISH** | Exporter ships; add per-page reading-order mode |
| Spreads — all four | **BUILD** | §10. Fold geometry verified |
| Promote the fold act | **MODIFY** | The loop ends in an object |
| Layer-order buttons | **MODIFY** | Unclip both; capability already exists |
| Ink pots to 48dp | **MODIFY** | Below the accessibility floor |
| Terminology consolidation | **MODIFY** | One name per concept (§13) |
| Material icon tiles / kebab | **MODIFY** | Two icon grammars in one product |
| Clippings tray | **DEFER** | Premature: schema change, no prototype |
| Canvas zoom/pan | **DEFER** | Premature: would make pinch ambiguous |
| Direct print (`PrintManager`) | **DEFER** | Incorrect-as-built: silent driver re-scaling breaks every fold |
| Alignment/distribute | **DO NOT BUILD** | Layout-tool thinking |
| Free colour picker / font picker | **DO NOT BUILD** | Materials are finite and named — the whole strategy |
| Opacity & blend sliders | **DO NOT BUILD** | Translucency is a material property, not a control |
| Pattern fill / auto-tiling | **DO NOT BUILD** | Names no physical cause a person could produce |
| Emoji in text | **DO NOT BUILD** | Per-OEM rasters make the same zine export differently on two phones |
| Page background colour | **DO NOT BUILD** | Home printers cannot bleed — prints as a panel in a white margin |
| Templates gallery | **DO NOT BUILD** | *"Blank is a peer"* |
| Onboarding tour | **DO NOT BUILD** | Empty states already teach |
| Accounts · cloud · feed · collaboration · marketplace · AI layout | **DO NOT BUILD** | The product is that there isn't one |
| Online Art search (drawn in the freeze) | **DO NOT BUILD** — ruled by **ADR-104** (§16) | The freeze fenced it behind an asset-layer ADR + legal pass that were never written. §16 performs both and rules bundled-only. Openverse *"does not verify licensing information"*; Unsplash mandates hotlinking and telemetry; Pexels allows 200 req/hr on a key shared by every install |
| Art sourced from ShareAlike or CC-BY | **DO NOT BUILD** | ⚠ The obligation would follow the user's exported zine. Rejects **OpenMoji** and **The Noun Project** specifically (§16.4) |
| Any networking library | **DO NOT BUILD** — in anything this document plans | NFR-1; the zero-network claim must stay inspectable in the manifest. ⚠ *Permanence* is constitutional and is the owner decision in §16.9, not a call this document makes |

---

# 9. Decor / graphics strategy

**Decor is a first-class primitive and ships.** Not because composition tools have shapes, but because **the app decorates itself with tape, stamps and cut paper while withholding all three from the maker** — a product contradicting itself. Without it, Zinely is a text-and-photo editor; with it, it is a composition tool.

## 9.1 What a DecorElement is

> **A placed instance of an authored supply, tinted with a named ink.**

Not a vector-drawing surface — that is a different product. A `DecorElement` carries only:

| Field | Why |
|---|---|
| `supplyId` | Which of the sixteen authored primitives |
| `transform` | Position, size, rotation — same `Transform` every element already uses |
| `ink` | One of the five named colours. Single-coverage, so tinting is exact |
| `mirrored: Boolean` | Nine of sixteen are asymmetric, so mirror earns its place |

🟨 **INFERRED** (from `Document.kt` structure, not yet written): this is a schema v1→v2 addition with a migrator. Existing documents contain no decor, so migration is additive and total.

## 9.2 The vocabulary — sixteen primitives, four families

| Family | Primitives |
|---|---|
| **Tape & fixings** | torn tape strip · photo corner · staple · paper clip |
| **Stamps & marks** | star/asterisk · arrow · halftone dot cluster · registration cross |
| **Cut paper** | torn strip · cut-out window frame · cut label/speech tag · marker underline |
| **Cut shapes** | rectangle · circle · triangle · straight rule |

The fourth family is not a betrayal of the metaphor — these are **shapes cut from coloured paper with scissors**, exactly what a paste-up artist does, and they keep the cut edge of the family they sit in. A Zinely rectangle is a *cut* rectangle, not a `RoundedCornerShape`. Without them a composition tool has no colour block and no divider, which is a hole no amount of nice tape fills.

Sixteen is argued from shipped systems, not a study: this repo's own cover grammar works at 8; Truchet at 1; Recursive curates infinity to 64; NN/g measures a 100-item picker inflating time-on-task over 500%.

## 9.3 How it is added, edited, and behaves

**Added** — ✅ **the third add verb is already frozen as `Art`** (`v21-bench.html:14`, `:775`), so building it is parity. It opens a tray of the sixteen grouped by family. Tap places at the page centre at a default size; drag places where the finger goes.

⚠ Two things the freeze draws here do **not** ship: the online search and the four generic chips (Illustrations/Icons/Frames/Patterns). See §14 A0 — the chips become the four authored families, and the search goes because sixteen curated primitives are browsed, not searched.

**Naming:** **Art** is the verb (what you are adding, parallel with Text and Photo). **Supplies** is the drawer the material lives in — `Copy.kt:320` already says *"from the supplies below"* and `SCREEN-INVENTORY.md:112` specifies the tray. A verb and a container, not two names for one thing.

**Edited** — ✅ **the frozen spec already defines this.** `v21-bench.html:625` specifies the decor verb set as `Replace, Ink, Delete`, and `BenchInkPopover.kt:140` already handles `BenchVerbKind.DECOR`. **Decor editing is parity work, not new design.** Adding the shared `Duplicate` verb is the only amendment.

**Behaves** — identically to every other element: two-finger transform, snap guides, nudge, layering, undo. **Decor is not special**, which is the point: one selection model, three primitives.

**Tilt** — a small rotation derived deterministically from the element id, so it is stable forever and survives reload. **No shuffle, no re-roll, no random position.** Randomness may set an initial value; it may never be a running behaviour.

## 9.4 Accessibility of Decor — part of its definition

A decor element is not done when it renders. It is done when it has:
- a spoken label naming the object, size and ink — *"Star, medium, berry ink"* — never "decor element 3";
- ✅ **the eleven shared custom actions** (`EditorA11y.kt:61-77`), inherited by being an ordinary element — move ×4, larger, smaller, rotate ×2, forward, backward, delete. It needs no type-specific action;
- ⚠ an explicit branch in `EditorA11y.label()` — the `when` is exhaustive over sealed `Element` with no `else`, so this is a **compile error**, not a silent gap. The safe case.

## 9.5 The blast radius, corrected

| Site class | Count | Behaviour on a third type |
|---|---|---|
| **`error(...)` on an unhandled kind** | **1** | ⚠ **Throws at runtime.** `BenchContextBar.kt:125`, with a test asserting the throw (`BenchContextBarTest.kt:137`). The single most dangerous site |
| Exhaustive `when (element)` | **7** | Compile error — safe. `DefaultDocumentValidator.kt:79` · `EditorReducer.kt:66` · `Elements.kt:17,22` · `SceneRenderer.kt:56` · `BenchContextBar.kt:129` · `EditorA11y.kt:31` |
| `as?` casts | **13** repo-wide (6 in `EditorScreen.kt`: 367, 664, 1108, 1127, 1304, 1341) | Silent no-op |
| `is`-guards | **4** — `LivePreview.kt:78` · `EditorA11y.kt:51,57` · `EditorGestures.kt:52` | Silent skip |

✅ **PDF export is already vector-capable** (`PdfPageRenderer.kt:13-18`), so authored supplies print as vectors at any size — no raster stickers.

## 9.6 What ships now, what waits

**Now:** the sixteen primitives · placement · transform · named-ink tint · mirror · layering · bleeding off the trim edge · the Supplies tray · full a11y parity.

**Waits:** favourites and recents (earn them once there is usage) · a second themed pack · user-saved combinations.

**Never:** pattern fill / auto-tiling · randomised scatter of N copies · non-uniform stretch · procedural generation of new primitives · auto-composition · an opacity slider.

---

# 10. Multi-page composition strategy

**All four spreads ship.** ✅ The load-bearing geometry, re-derived from `Convention.kt:32-49` and `SingleSheet8Imposer.kt`:

```
row 0 (all Rotation.HALF):  col0=5  col1=4  col2=3  col3=2
row 1 (all Rotation.NONE):  col0=6  col1=7  col2=8  col3=1
folds: y=h/2, x=w/4, x=w/2, x=3w/4     cut: y=h/2, x ∈ [w/4, 3w/4] only
```

| Spread | Cells | Shared edge | Is | Rotation parity |
|---|---|---|---|---|
| 2\|3 | (0,3)+(0,2) | `x = 3w/4` | **fold** | both HALF ✓ |
| 4\|5 | (0,1)+(0,0) | `x = w/4` | **fold** | both HALF ✓ |
| 6\|7 | (1,0)+(1,1) | `x = w/4` | **fold** | both NONE ✓ |
| 8\|1 | (1,2)+(1,3) | `x = 3w/4` | **fold** | both NONE ✓ |

**Every reading spread meets at a fold.** The cut separates only `4|7` and `3|8` — neither is a spread — and both halves of every spread share a rotation, so no image is split across a 180° flip.

**Not the rejected "spread view".** `v21-proof.html:25-27,175-179` rules *"NO SPREAD VIEW… A tablet spread is a scope decision"* — that was a **side-by-side editing surface**, and it stays rejected. This is one image crossing a fold on the printed object, edited a page at a time on the existing bench. Same word, different feature.

## The interaction — one button, no new concepts

Select a photo on a page that is half of a spread → **"Run this photo across both pages."** (On the cover: *"Wrap this photo around the cover."*) The warning is the same everywhere: **"The middle of this photo lands on the fold — keep faces and words away from it."** No gutter, no spread, no bleed. The user never meets a print word.

## The internal model — a property realised as an action, never a relationship record

**It creates two ordinary `ImageElement`s with complementary crops. There is no `Spread(pageA, pageB)` record, and there must not be.**

✅ The reason is verified: the only page identity in the schema is `Page.index` (`Document.kt:42`), and `renumber()` (`Elements.kt:33`) rewrites it on every add and delete — `pages.mapIndexed { i, p -> p.copy(index = i) }`. A relationship record would silently re-point at unrelated pages the first time someone deletes a page.

**Interactions this model gets right for free:**
- **Page reorder** — the two halves are just images on their pages. Reorder them apart and you get two cropped photos, which is honest. A relationship record would claim a continuity that no longer exists.
- **Delete one half** — the survivor is an ordinary cropped photo. Degrades correctly.
- **Export** — nothing to special-case; the imposer already places panels.
- **Fold logic** — untouched, because the geometry above already guarantees a fold boundary.

⚠ **The one real engine change.** `safeAreaInsetPt` is a single scalar applied to all four sides of every panel, so suppressing the keep-clear cue on the **inner** edge needs a per-edge safe-area concept `ImpositionLayout` does not have today. Required, not optional: get it wrong and you paint a 12mm white stripe through the middle of a "continuous" image. Small, but it is the only imposition-side work.

---

# 11. Import / export strategy

## Import

| Path | Decision |
|---|---|
| Gallery picker | ✅ ships |
| **Camera** | **BUILD.** `ActivityResultContracts.TakePicture`; ✅ the `FileProvider` is already declared (`${applicationId}.fileprovider`, `@xml/file_paths`), so this needs a path entry, not a provider. **No `CAMERA` permission** when the system camera does the capture — which keeps the permission list honest |
| **Share-sheet receive** | **BUILD.** `ACTION_SEND`/`SEND_MULTIPLE` for images → pick a zine → lands on the current page |
| Network / online stock library | **Never.** NFR-1. ⚠ This directly overrules `v21-bench.html:15`, which draws an opt-in online Art search — see §14 A0 and ADR-104 in §16 |

## Export — the complete model

**Do not build new rendering infrastructure.** ✅ The architecture already covers every output required:

| Layer | What it does | Status |
|---|---|---|
| `SceneRenderer.render()` | `List<DrawCommand>`, pure Kotlin, rasterises nothing | ✅ ships |
| `PdfPageRenderer` | `DrawCommand` → PDF bytes, **vector-capable** | ✅ ships |
| `RasterPageRenderer` | `DrawCommand` → `Bitmap` at 300 px/pt | ✅ ships, called from **no `src/main` code** |
| `SheetComposer.writePng` | imposed sheet → PNG | ✅ ships |
| `ZineExporter:175` | `ExportFormat.PNG -> composer.writePng(...)` | ✅ ships, **unreachable** |
| `ZinelyNavHost:290,310` | hardcodes `ExportFormat.PDF` | ⚠ the blocker |

**The complete export experience:**

| Output | Verdict | Form |
|---|---|---|
| **Imposed PDF, foldable** | **Primary. Keeps the one `--frame` ring.** | *Print & fold* |
| **Page images, reading order** | **BUILD** — a per-page mode over `RasterPageRenderer` | One PNG per page at 300 px/pt, named `<zine>-p3.png` |
| Share (PDF) | ✅ ships | *Send a copy* — Android share sheet |
| Imposed-sheet PNG | **DO NOT SURFACE** | It is a printer's artifact and meaningless as an image. The code path stays; the UI does not offer it |
| Spread images | **DO NOT BUILD** | Derivable by the user from two page images; adds a concept for no gain |
| Transparency | **DO NOT BUILD** | A zine page is paper. Opaque white ground, always |

**Naming:** `<zine-title>.pdf` and `<zine-title>-p1.png`. Human, sortable, no timestamps or UUIDs.
**Destination:** ✅ Downloads via MediaStore (ADR-054), unchanged.
**Quality:** 300 px/pt for both paths — ✅ `ExportScale` already fixes this and is shared.

**Hierarchy is preserved by placement, not absence:** *Print & fold* carries the ring; *Save pages as images* is a quiet second item under *Send a copy*.

---

# 12. Accessibility strategy

✅ **Starting from a stronger position than the audit implied** — and the correct shape is **eleven shared actions plus up to two type-specific ones**, not a flat twelve:

| Layer | Count | Actions |
|---|---|---|
| **Shared base** (`EditorA11y.kt:61-77`) | **11** | move ×4 · larger · smaller · rotate ×2 · bring forward · send backward · delete |
| Text adds (`:51`) | +1 | edit text |
| Image adds (`:58-59`) | +2 | reframe photo · reset framing |

✅ Wired, not dead code: `ElementSemanticsLayer.kt:93` calls `elementCustomActions`, `:120` assigns `customActions`.

Every core creative operation is already reachable without a drag gesture — better than most commercial creative apps, and it should be protected, not rebuilt.

⚠ **This matters for Decor.** I wrote that Decor *"inherits all twelve"*. It inherits **eleven** — it has no text to edit and no framing to reset. Writing a new primitive's accessibility bar from a number that does not apply to it is how a11y gaps get shipped as "done".

## The standard

> Not "no crash". **Every core operation must remain understandable and operable through accessibility services.**

## Per primitive

| Primitive | Label | Actions | Target | Gesture alternative | Focus order |
|---|---|---|---|---|---|
| **Text** | content + style — *"Text: 'summer', large, leaf ink"* | ✅ **12** = 11 + edit | 48dp | ✅ nudge/scale/rotate row (OD-11, WCAG 2.5.7) | page → elements in z-order → chrome |
| **Image** | source + framing state | ✅ **13** = 11 + reframe + reset | 48dp | ✅ same | same |
| **Decor** | object + size + ink — *"Star, medium, berry ink"* | **11 inherited** — no type-specific action, and none is needed | 48dp | ✅ inherited | same |
| **Page** | *"Page 3 of 8, 4 items"* | go to · duplicate · **reorder** | 48dp | ⚠ **reorder needs a non-drag path** — "move earlier"/"move later" custom actions, not drag-only | grid order |
| **Controls** | verb, not icon name | activate | ⚠ **two defects** | — | after content |
| **Selection** | announced on change via `Announce` effect | — | — | ✅ `SelectAt` is tap-based | — |

## The two verified defects

1. **`Bring forward` renders 10px wide and reports `enabled=true, clickable=false`** — TalkBack announces an activatable button that cannot be activated. This is the worst kind of a11y defect: the tree *lies*. On `EditorContextBar`; `BenchContextBar.kt:296-309` has the pattern to copy.
2. **Ink pots at 38.0dp** — below the 48dp floor, measured on device.

## Rules that now bind

- **A new element type is not complete until it has a label, the twelve actions, and a device pass.** Part of the primitive's definition, not a cleanup phase.
- **Page reorder ships with custom actions from day one.** Drag-to-reorder without a non-drag alternative would introduce a WCAG 2.5.7 failure into a product that currently passes.
- **Read the platform tree, not the merged one.** ✅ A Compose semantics test asserts against the *merged* tree; TalkBack reads `AccessibilityNodeInfo`. Only `platformNode(activity)` and `adb exec-out uiautomator dump` agree with TalkBack — this exact gap shipped the `ReframeControls.ZoomButton` defect through a green suite.
- **The nudge/transform row stays.** It exists to satisfy WCAG 2.5.7 (OD-11), and *a parity phase does not remove a conformance path*.

🔍 **NEEDS INVESTIGATION:** focus order across the Bench's chrome rows; whether the `Announce` effect is used consistently on selection change; keyboard/D-pad behaviour (unexamined — likely a gap, but not a claim).

---

# 13. Product subtraction

Removals, ranked by how much they make the product feel like a prototype.

| # | Remove | Why |
|---|---|---|
| 1 | **Three of four "works offline · stays on your phone" repetitions** | ✅ `VOICE.md:35` already rules reassurance is stated once. Repeated on fresh pages, it reads as anxiety. The survivor moves to the colophon — the one place self-description is craft, not defensiveness |
| 2 | **Both permanently disabled controls** | Not by hiding them — by making them work (§8). *Nothing is inert* |
| 3 | **The Material icon tile** in `ZineActionSheet` / `BenchAddChooser` | Material's list grammar wearing our colours |
| 4 | **The `⋮` kebab** | Android furniture in a room with no other Android furniture |
| 5 | **One of the four chrome rows on the Bench** | *"The page is the hero; the tool is a guest."* 🟨 Likely mechanical: `BenchContextBar` (`EditorScreen.kt:1299`) and `EditorContextBar` (`:1519`) both render — a duplicated-component problem, not a layout one. 🔍 confirm before cutting |
| 6 | **Imposition explainers** | The user never learns print (principle 9) |
| 7 | **The grab handle that promises a gesture the `Dialog` cannot accept** | An affordance that lies |
| 8 | **Terminology drift → one name per concept** | Ink not Colour · Add words not Text · Proof not Preview · Print & fold not Export · Send a copy not Share & export · zine not project · bench not canvas · shelf not library · supplies not stickers |
| 9 | **Seven drifted tracking values → the shared `sectionLabel`** | Shipped six times, declared zero times |
| 10 | **`v21-typebar.html`'s twelve emoji stickers** | Die with the randomisation idea. ✅ Deferred, never built — nothing sunk |

**Not removed, despite looking removable:** the nudge/transform row (WCAG 2.5.7 conformance path, OD-11) · the `ERROR_BODY` constant (✅ live at `ProofScreen.kt:591`) · undo snackbars (better than confirmation dialogs) · the empty-state teaching (it is the onboarding).

**No new confirmation dialogs anywhere.** Undo-snackbar already does it better.

---

# 14. Frozen-spec implications

The rule: **frozen means implement parity; changing it means amending the reference explicitly.** Not silently, and never because implementation convenience suggests a better idea.

## Parity work — build what the freeze already draws

| Item | Freeze says |
|---|---|
| **Page reorder** | ✅ `v21-bench.html:766` — *"tap to jump, **drag to reorder**"* |
| **Decor context bar** | ✅ `v21-bench.html:625` — decor verb set `Replace, Ink, Delete` |
| **Font chip works** | ✅ `v21-bench.html:514` — `Aa Font` chip is drawn |
| **Replace works** | ✅ `v21-bench.html:623-624` — in the photo verb set |
| Island membership on TypeBar / BenchSnack / Proof fold diagram | ADR-102 §12.1 — *"the island is a property of the subtree"* |
| Remove the rule-of-thirds grid | ✅ absent from `v21-reframe.html` (zero hits over 362 lines) — Compose-only invention |

**This is the section's most useful finding: five of the seven "new features" are the freeze finally being implemented** — page reorder, the decor verb set, the Font chip, Replace, and (per A0) the third add verb itself. The freeze anticipated more of this plan than I did.

## ⚠ A0 — the amendment I initially missed, and the largest one

I claimed `v21-bench.html:773` *"fixes the add chooser at Text / Photo"*, making a third entry an amendment. ✅ **Falsified — it is the opposite, and the correction cuts both ways.**

The frozen header states it as law:

```
v21-bench.html:14   · Add stays three verbs: Text / Photo / Art
v21-bench.html:15   · Art is one surface: bundled offline, online search strictly opt-in
```

`:775` is the third entry — `data-a="art"`, *"Stickers, icons, frames, patterns · **bundled + online**"* — and `openArt()` (`:789-797`) draws a search field *"Search bundled + online"*, chips `['Illustrations','Icons','Frames','Patterns']` (`:786`), per-tile favourites (`☆`), and an **online opt-in panel**. A second entry point sits at `:567` (`art · online`).

**So the third add entry is parity, not an amendment.** Good news for Decor.

**But the frozen spec draws an online-capable asset surface, and §8/§11 of this document rule networking out permanently.** That is a head-on collision between a frozen prototype and NFR-1, and my §14 did not mention it.

### The ruling — and A0 is not an amendment at all

⚠ **Corrected again, and this time in the document's favour.** I wrote that `v21-bench.html:15` *"exceeded the freeze's authority"* and that striking it removed frozen capability. **The freeze never claimed that authority.** [V2-BENCH-REVIEW §E.6](V2-BENCH-REVIEW.md) — the owner-approved freeze itself — fenced the online search behind an asset-layer ADR and a legal pass, neither of which was ever written (`DECISIONS.md:2793-2797`). The online panel is **prototype exploration of an explicitly governed area**, which is why it never entered the frozen property table.

> **So A0 is not an amendment. It is [ADR-104](#16-decision-memo--the-asset-layer-adr-104) — the asset-layer ADR the freeze demanded — plus the legal pass it required.** Its ruling: **bundled-only, permanently. NFR-1 stands unamended.**

**No owner escalation is required.** Upholding an existing product requirement needs no authorisation; only changing one would.

| A0 sub-item | Action |
|---|---|
| Third add verb — **Art** | ✅ **PARITY.** Build it |
| *"bundled + online"* in the entry's subtitle | **STRIKE** — becomes *"Tape, stamps, cut paper"* |
| The search field | **STRIKE.** Sixteen curated primitives in four families need browsing, not searching |
| The four chips (Illustrations/Icons/Frames/Patterns) | **REPLACE** with the four authored families (§9.2) |
| Favourites (`☆`) | **DEFER** — earn from usage (§9.6). Not struck, just not first |
| The online opt-in panel and `:567`'s `art · online` | **STRIKE** |
| `v21-bench.html:15` itself | **AMEND** — the line is a scope claim a mockup may not make |

**None of this removes frozen capability** — the online half was never authorised to be built. What lands in `v21-bench.html` is a header note pointing at ADR-104 and the removal of controls the freeze itself forbade implementing.

**Naming, decided:** the freeze's **Art** stays as the add verb — it is parallel with Text and Photo, and all three name *what you are adding*. **Supplies** is the drawer the material lives in (`Copy.kt:320`, `SCREEN-INVENTORY.md:112`). A verb and a container are not a naming collision.

## The rest of the batch

| # | Amendment | Surface |
|---|---|---|
| A1 | ~~Add-chooser third entry~~ | ✅ **withdrawn — it is parity.** See A0 |
| A2 | **Add-chooser gains a fourth entry (Take a photo)** | `v21-bench.html:14` — *"Add stays three verbs"* |
| A3 | **`Duplicate` joins every context-bar verb set** | `v21-bench.html:623-625` fixes all three sets |
| A4 | **A font-voice selection surface** | `:514` draws the chip; what it opens is unspecified |
| A5 | **Page grid: current page `leaf` → `berry`; cells draw content** | ✅ `:763` renders bare numerals (`<button class="pgc">${i+1}</button>`), so drawing content is genuinely an amendment — not parity. ⚠ breaks two named tests — budgeted |
| A6 | **Page duplicate in the grid's long-press menu** | Reorder is specified (`:766`); duplicate is not |
| A7 | **Supplies tray** — a persistent drawer, distinct from the Art chooser | `Copy.kt:320`, `SCREEN-INVENTORY.md:112` (status 🔭, *"replaces the lone FAB"*), never built |

**Six, as one ADR, or not at all** — seven silent divergences would be exactly the failure the freeze rule exists to prevent.

## Freezes to declare

`v21-typebar.html` and `v21-reframe.html` → **FREEZE BOTH.** Freezing `v21-typebar.html` is what makes the Type-bar island fix a *permitted parity fix*; refusing to freeze it is what blocks the fix.

## Law to declare in `V21-SPEC.md` (shipped, never written down)

The line alphabet · the tilt law · the stamped-label rule · the four motion causes · exits, stagger and the no-overshoot ruling.

---

# 15. Implementation sequence

**NOW + NEXT together are the substantially complete product.** LATER is genuinely optional; EXPERIMENTAL touches nothing core.

## NOW — truth, defects, and coherence

*No new capability. This is the pass that makes the existing product honest.*

| # | Work | Depends on | Evidence |
|---|---|---|---|
| N1 | Measure `inkFaint` contrast on `bench` — clears AA by 0.04 on `paper`, unmeasured on `bench` | — | ✅ gap known |
| N2 | **Freeze `v21-typebar.html` + `v21-reframe.html`** | — | ✅ |
| N3 | TypeBar + BenchSnack take the existing island opt-out; delete the thirds grid from `ReframeOverlay` | N1, N2 | ✅ |
| N4 | **Unclip both z-order buttons on `EditorContextBar`**, 48dp targets. Both exist — this is not "add send-backward" | — | ✅ |
| N5 | **Ink pots to ≥48dp** | — | ✅ measured |
| N6 | **Terminology consolidation** — one name per concept | — | ✅ |
| N7 | **Subtraction pass** (§13 items 1, 6, 7, 10) | — | ✅ |
| N8 | **Declare the undeclared law** in `V21-SPEC.md`; consolidate onto `sectionLabel` | — | ✅ |
| N9 | **Promote the fold act** to the press run's continuation | — | ✅ |
| N10 | **Land ADR-103 + Amendment 2**; supersede `DESIGN-LANGUAGE.md` | — | — |
| N11 | **Unblock PNG** — stop hardcoding `ExportFormat.PDF` in `ZinelyNavHost` | — | ✅ |
| N12 | **Land ADR-104 (the asset layer, §16)** and mark `V2-BENCH-REVIEW §E.6`'s condition **discharged** | — | ✅ §16 |
| N13 | **Land the amendment batch (A2–A7) as one ADR.** ⚠ Five NEXT items depend on it and it had no slot. Six routine items — A0 left the batch and A1 was withdrawn | N10, N12 | ✅ §14 |

## NEXT — the complete product

| # | Work | Depends on | Cost basis |
|---|---|---|---|
| X1 | **Decor primitive + sixteen supplies.** ADR first. ⚠ fix `BenchContextBar.kt:125` first — it *throws*. Schema v1→v2 + migrator | N2, A3/A7 | ✅ verified blast radius (§9.5) |
| X2 | **Supplies tray** | X1 | 🟨 new surface, HTML spec first |
| X3 | **Take a photo** | — | ✅ `FileProvider` already declared |
| X3b | **Photocopier filter** ↑ *promoted from X13* — 1-bit Floyd–Steinberg over a downscaled bitmap | X3 | 🟨 pure Kotlin, fits `core:render`. With X3 it completes **shoot → dither → print**, the loop that makes Zinely a zine tool rather than a photo-layout tool |
| X4 | **Duplicate element** — one verb over `PlaceCommand` | A3 | ✅ |
| X5 | **Replace image** — closes D-038 | — | ✅ intent exists |
| X6 | **Font as three named voices** | A4 | ✅ **Source and place 8 static TTFs — no subsetting.** Averia carries RFNs `'Averia'`/`'Averia Libre'`; subsetting makes a Modified Version and clause 5 voids the licence on breach, so unmodified statics are both safer and less work (§16.5). ~840KB. No schema change (`TextStyle.fontFamily` exists) |
| X7 | **Page grid draws content**; current page `leaf` → `berry` | A5 | ✅ breaks 2 tests — budgeted |
| X8 | **Page reorder** (+ non-drag a11y actions) **and page duplicate** | X7, A6 | ✅ reorder is parity |
| X9 | **Spreads — all four** | X7 | ✅ geometry proven; ⚠ per-edge safe-area is the one engine change |
| X10 | **Page images, reading order** | N11 | ✅ exporter ships; add per-page mode |
| X11 | **Colophon** — paper default, typefaces + licences, one offline sentence, version | N6 | ✅ verified absent |
| X12 | **Share-sheet receive** | — | 🟨 manifest + one destination decision |
| ~~X13~~ | **Photocopier filter — promoted to X3b.** ✅ The zine vocabulary is a *process* vocabulary, not an asset one (§16.3): the cut-and-paste look exists because the photocopier made showing your method free. This is the single highest identity-per-line item in the product and it was sequenced thirteenth | — | — |
| X14 | **Actual-size preview** — true mm via `DisplayMetrics.xdpi` | — | 🟨 |
| X15 | Retire the Material icon tile; replace the kebab | N2 | ✅ |
| X16 | Collapse the Bench's chrome rows | 🔍 X15, and confirm the duplicate-component theory first | 🔍 |

**Sequencing:** X1 is the only document-model change and must not block anything — start it first, land it last. X3/X4/X5/X10/X11 are independent. X7 gates the three page-grid items.

## LATER — valuable, each waiting on a stated reason

| Item | Waiting on |
|---|---|
| Clippings tray | *Premature* — prototype + device pass before the schema takes a new container |
| Direct print (`PrintManager`) | *Incorrect-as-built* — a device-verified fixed-scale path; additive to Save PDF, never replacing it |
| Canvas zoom / pan | *Premature* — evidence that placement precision is actually being fought |
| Favourites / recents in Supplies | *Premature* — earn them from usage |
| Second supplies pack · hand-it-over Read mode · Direct Share targets · clipboard paste | *Refinements* of flows that will already work |

## EXPERIMENTAL — prove separately, touch nothing core

Stylus pressure and tilt · perspective-correct paper scan · camera texture capture · ~~the opt-in keyword-only asset library~~ — **removed from EXPERIMENTAL by [ADR-104](#16-decision-memo--the-asset-layer-adr-104)**; its constitutional status is the owner decision in §16.9 · QS tile.

---

# 16. Decision memo — the asset layer (ADR-104)

## 16.1 The question was never "freeze vs NFR-1"

I framed this as a frozen spec conflicting with a product requirement, and proposed to escalate. **That framing was wrong, and the corpus already said so.**

The owner-approved freeze carries this clause verbatim ([V2-BENCH-REVIEW §E.6](V2-BENCH-REVIEW.md), quoted at `DECISIONS.md:2793`):

> *"**Still governed — do NOT freeze into implementation until a review + legal pass clears them:** the asset-layer ADR (H3 online-search licensing, CC0/MIT-first) and the colour-namespace ADR (H4 `content.*` maker inks). These land behind their ADRs; the rest of the Bench does not wait on them."*

And `DECISIONS.md:2795` records the state:

> *"The colour-namespace ADR exists — ADR-072, `Accepted`… **The asset-layer ADR does not exist.** No ADR in this log covers the asset layer, and no legal pass is recorded anywhere in the repository… the invariant anticipates the keyword-only search, but **anticipating is not authorising, and the authorisation is the ADR that was never written**."*

**So the online search was never frozen into implementation.** ⚠ But §E.6 *fenced* the question pending an ADR and a legal pass — it did not answer it, and its own parenthetical (*"H3 online-search licensing, CC0/MIT-first"*) shows the freeze expected online search to proceed under a licensing rule rather than be ruled out. **E.6 authorises this document to decide; it does not itself decide.** `Copy.kt:233` (*"Art stays fenced behind C8 per OD-21"*) and `DECISIONS.md:2797` (*"C8 is therefore not a Phase C package at all"*) are the same ruling surfacing in code and log.

**So this ADR is two things at once:** the asset-layer ADR the freeze demanded, and the legal pass it required. Neither is an amendment removing frozen capability — the online half was never authorised to be built.

**What *was* an owner decision, and went to the owner:** closing the Constitution's permitted future touch permanently (§16.2). Escalated once with a drafted amendment and a recommendation; **adopted 2026-08-15** as [Amendment 3](V2-CONSTITUTION.md#amendment-log).

## 16.2 Decision

> ## Model D — curated + generative + personal. Bundled-only for everything that ships.
>
> Zinely's material library has **three sources**:
>
> | Source | What it is |
> |---|---|
> | **Supplies** | ~16 authored primitives, drawn by us, shipped in the APK |
> | **Variation** | Transformations that multiply them — rotate, mirror, scale, named-ink tint, layer, bleed |
> | **Your own stuff** | The camera, the photo picker, the share sheet, the clipboard — and the **photocopier filter** that turns any of it into zine material |
>
> **No network for assets, at any stage — now ratified as constitutional law.** NFR-1 stands unamended; §III and §V are amended by [Amendment 3](V2-CONSTITUTION.md#amendment-log), which withdrew the previously permitted opt-in keyword-only search.

### ⚠ The scope of this ruling, corrected

An earlier draft ruled *"no network — not now, not opt-in, **not later**."* **I was not empowered to rule the third one**, and I reached it without citing the document that governs it.

✅ [`V2-CONSTITUTION.md:253-255`](V2-CONSTITUTION.md) — §III, Data & privacy:
> *"**No networking libraries, no analytics SDKs, no cloud, no account.** Offline-first… **The single permitted future network touch — optional online asset search — sends only a keyword, never user content**, and the app is fully usable with it off."*

✅ [`:302`](V2-CONSTITUTION.md) — §V, *How the product grows*:
> *"| **Online asset library** | Optional, opt-in, **keyword-only** request; never user content; fully usable offline. The privacy invariant is not negotiable. |"*

✅ [`:313-316`](V2-CONSTITUTION.md) — §VI:
> *"This constitution is amended only by the **owner**, deliberately, as an explicit act — never implicitly through implementation, and **never by a design or engineering session on its own initiative**… Absent such an amendment, every statement here is final and binding on all downstream work."*

**The Constitution holds this door open as a named growth area.** Nailing it shut permanently is an amendment, and §VI reserves that to the owner. So:

| Claim | Authority |
|---|---|
| **Not now** — nothing in NOW/NEXT/LATER builds it | ✅ **Mine to rule.** Sequencing and scope |
| **Not opt-in in this product plan** — no toggle, no dead UI, no half-feature | ✅ **Mine to rule.** §16.7 |
| **Not ever** | ✅ **Owner-adopted 2026-08-15 as [Constitution Amendment 3](V2-CONSTITUTION.md#amendment-log).** Escalated once with a drafted amendment and a recommendation; adopted. §III and §V are amended, and the permitted future network touch is withdrawn |

This is the same failure Amendment 1 records against itself at `V2-CONSTITUTION.md:331` — *"This amendment exists because the conflict was caught, not because it was planned."* Same document, same session type, caught the same way.

This is **not option A with a door left open.** It is a different and better model than either A or C, because the third source is where the variety actually comes from — and it is already in the user's pocket.

## 16.3 Why

**The evidence for curation is not the evidence I expected.** ⚠ I was going to argue choice overload. That literature will not hold: Scheibehenne et al.'s meta-analysis (~50 studies, ~5,000 participants) finds a mean effect of assortment size on satisfaction of essentially **d ≈ 0**, and the jam study has failed direct replication. Dropped.

What holds:

| Argument | Evidence |
|---|---|
| **Constraint helps, up to a point** | ~145 empirical studies show an **inverted-U** between constraint and creativity — some helps, none and too much both hurt. This also warns against going *too* small |
| **The designers who did this say why** | PICO-8's Joseph White names the enemy directly: *"a cozy design space is not decision fatigue"*, and says the 16 colours were chosen partly *"to give PICO-8 cartridges their own particular look and feel."* Kid Pix spent its budget on **eight ways to erase the canvas** rather than more stamps. Playdate: *"if it gets too close to your phone, it loses its rationale for existence"* |
| **Tiny vocabularies genuinely do produce unbounded variety** | Truchet: **one** tile, four rotations, "an infinity of pleasing designs." 10 PRINT: **two** characters. The mechanism is orientation × adjacency, not quantity |
| **A big library without excellent search is worse than none** | Baymard: mediocre list/filter usability → **67–90% abandonment** vs **17–33%** with better tooling on the identical task. Search is exactly what we have no reason to build |
| **Stock homogenises** | The "Canva aesthetic" critique is consistent: shared stock + shared templates produce convergent output. ⚠ Honest counter: a curated set homogenises too — the difference is *whose* house style it is |

**The finding that reordered the plan:** the authentic zine vocabulary is a **process vocabulary, not an asset vocabulary.** Willis's material history shows the cut-and-paste look exists because the photocopier made *showing your production method* free — high contrast, flatness, generation loss. The commercial market has already crystallised this as **image-processing kits, not clip-art** (True Grit's "Halftone Zine Machine"). **So the photocopier filter matters more than the sticker set, and it moves up the plan.**

**Online fails on four independent grounds, any one sufficient:**

1. **It costs the claim on day one.** Adding a networking library permanently retires "zero networking libraries, verifiable in the manifest." Opt-in does not preserve this — the library is in the binary either way. A falsifiable claim and a promise are different products. The Musicolet precedent is instructive: reviewers praise it precisely because *the permission list is a claim they can check themselves*.
2. **Provenance cannot be delegated.** ✅ Openverse's own documentation states it *"does not verify licensing information for individual works, or whether the generated attribution is accurate."* An aggregator's licence label is a claim, not a warranty — and we would be passing that claim to someone who prints and sells the result.
3. **The named APIs are individually disqualifying.** Unsplash mandates hotlinking, mandates reporting download events back to Unsplash, and asserts ownership of user interaction telemetry — the literal inverse of the privacy invariant. Pexels allows **200 requests/hour** on a key that, shipped in an APK, is shared across every install. The Noun Project's asset URLs **expire within an hour**.
4. **Play policy exposure without the UGC upside.** ⚠ An asset search probably does *not* make Zinely a UGC app (Play's trigger is content *visible to other users*, and Zinely is single-device). But the IP policy applies regardless: *"Don't modify or use copyrighted content without permission"* — and recolouring is not a defence.

## 16.4 Sourcing rules for the sixteen

**We draw them ourselves** (§9.2) — an authored kit by one hand is the point, and at sixteen the licensing question mostly evaporates. External sources are a **fallback**, and these are the cleared rules:

| Verdict | Sources | Basis |
|---|---|---|
| ✅ **Permitted** | MIT / ISC / Apache-2.0 icon sets — Feather, **Lucide (ISC, not MIT)**, Tabler, Phosphor, Heroicons, Bootstrap Icons, Material Symbols | Attribution satisfied **in the APK**, never in the user's PDF. No copyleft |
| ✅ **Permitted** | CC0 illustration — Openclipart, Open Doodles, Humaaans | *"unlimited commercial use… even to manufacture products globally"*; no attribution anywhere |
| ❌ **Rejected** | **OpenMoji** (CC BY-SA 4.0) | Recolouring is a remix; ShareAlike would follow into **the user's zine**. Exactly the failure this product cannot absorb |
| ❌ **Rejected** | **The Noun Project** | Per-icon licence tiers, no bulk redistribution grant; CC BY 3.0 icons demand attribution wherever displayed — i.e. in the PDF |
| ⚠ **Unresolved — do not bundle on reputation** | Public Domain Vectors, The Met, Smithsonian, Wikimedia PD-old | Primary licence text could not be retrieved (403 / 429 / ECONNRESET). *Unresolved is not rejected* — but nothing ships on a site's name |

**The binding rule, in one line:** *no asset whose licence obligation can follow the user's exported zine.* That single test rejects ShareAlike and CC-BY alike, and it is why MIT/ISC/Apache/CC0 are the whole permitted set.

## 16.5 The fonts — two verified compliance findings

✅ **Finding 1: the exported PDF owes nothing.** The operative text is **clause 5**, not the preamble (the preamble is a non-operative statement of goals and would be weak authority):

> *"5) The Font Software, modified or unmodified, in part or in whole, must be distributed entirely under this license… **The requirement for fonts to remain under this license does not apply to any document created using the Font Software.**"*

The grant also covers **embed** by name. A user selling a zine set in Inter owes nobody anything. ⚠ One adversarial edge, closed rather than ignored: a PDF embeds a *subset*, which is a Modified Version under the definitions, so clause 2's notice requirement is arguably live for that embedded copy — but clause 5's carve-out resolves licence propagation, and SIL's FAQ (1.14–1.15) treats *extraction*, not embedding, as the disfavoured act.

⚠ **Finding 2: Averia carries Reserved Font Names, and my X6 plan would have breached them.** Verified from the bundled licence text:

| File | Copyright | RFN |
|---|---|---|
| `OFL-AveriaSansLibre.txt` | Dan Sayers, 2011 | ⚠ **`'Averia'` and `'Averia Libre'`** |
| `OFL-Fraunces.txt` | Fraunces Project Authors | ✅ none |
| `OFL-Inter.txt` | Inter Project Authors | ✅ none |
| `render-android/…/OFL.txt` | ✅ **byte-identical duplicate of Inter's** — three distinct notices, not four | — |

**Clause 3:** *"No Modified Version of the Font Software may use the Reserved Font Name(s)… This restriction only applies to the primary font name as presented to the users."*

**The definitions section makes subsetting a Modified Version on two independent limbs:** *"any derivative made by **adding to, deleting, or substituting** — in part or in whole — any of the components of the Original Version, **by changing formats**…"*

⚠ **Citation corrected:** I attributed *"becomes null and void"* to clause 5. It is the separate **TERMINATION** paragraph — *"This license becomes null and void if any of the above conditions are not met."* Clause 5 is the distribute-entirely-under-this-licence clause. The conclusion is unaffected (breach of any condition, clause 3 included, terminates) but the citation must be right in a document that will be quoted as legal basis.

So subsetting Averia for PDF embedding would have required rewriting its `name` table, and getting that wrong terminates the licence.

✅ **The cure is the `name` table, not the UI label.** Calling the face "Handwritten" in the app would not cure a breach — a subsetted binary still reports "Averia Sans Libre" to the OS and to any PDF embedding it. Conversely, shipping unmodified means clause 3 never attaches, so the UI may name it either way.

> **DECISION: ship the font statics unmodified. No subsetting.** **Unmodified means no Modified Version, so clause 3 never triggers** — the legally safe path is also the simpler one, and it *removes* work from X6 rather than adding it. This supersedes X6's "source, subset and licence-check" costing: it is source and place, no subsetting step.
>
> 🟨 **Size is INFERRED, not verified.** Only *two* Averia statics exist in the repo today (regular, bold), so "four faces" is extrapolation. Measured figures at [`V21-SPEC.md:103-104`](V21-SPEC.md): **105.8 KB / 110.3 KB raw → 59.6 KB / 62.0 KB in-APK**. So the raw ~840 KB is roughly **~420 KB installed** — but the honest label is 🟨 until the eight files exist. The *decision* is verified; the *number* is not.

✅ **Finding 3: the four licence files stay.** `feature/editor/src/main/assets/fonts/` holds the three notices and **no font files** — they cover the chrome fonts in `core/ui/res/font/`. Correct for distribution-inclusion. ⚠ Clause 2's *"easily viewed by the user"* qualifier attaches grammatically to the machine-readable-metadata option, so shipping stand-alone text files is arguably compliant already — but SIL's FAQ emphasises the recipient being able to *get at* it throughout. **The colophon removes the argument for an afternoon's work, and it is the same screen Apache-2.0 and MIT want anyway.**

## 16.6 What changes

| Area | Change |
|---|---|
| **Product scope** | None. NFR-1 stands unamended. The privacy invariant is **strengthened** — it moves from anticipated to ruled |
| **Frozen spec** | ⚠ **Smaller than I claimed, but it is still a frozen-file edit and is recorded as one.** `V2-BENCH-REVIEW.md:355` establishes the bench mockup as *"the frozen, canonical Bench specification"*; §E.6 fenced *implementation*, not the file. So striking the `art · online` control, the search field and the "bundled + online" subtitle is **an edit to a frozen spec, authorised by ADR-104** — not housekeeping. The four generic chips become the four authored families |
| **UX** | Art is the add verb (parity, `:14`/`:775`). Supplies is the drawer. No search field, no online toggle, no "coming soon" — §16.7 |
| **Architecture** | Supply identity is a **stable `supplyId` string**, not an index — so a future pack adds ids without renumbering. That is the only concession to a future external source, and it costs nothing today |
| **Documentation** | ADR-104 lands in `DECISIONS.md`; `V2-BENCH-REVIEW §E.6`'s condition is marked **discharged**; A0 leaves the amendment batch |
| **Roadmap** | The photocopier filter moves **up**; the supply set is no longer the centre of the Art story |

## 16.7 What does NOT change, and what must not ship

**Unchanged and still valid:** the sixteen primitives and four families (§9.2) · the `DecorElement` model (§9.1) · deterministic per-element tilt, never re-rolled · the decor verb set as parity (`v21-bench.html:625`) · every §8 decision outside the asset layer · the Art/Supplies naming split.

**Must not ship, per the brief's §6 and I agree entirely:**

> **No dead search field. No disabled online panel. No fake network controls. No "coming soon". No UI implying remote assets exist.**

The shipped Art experience is a complete drawer of materials, not a stock browser with its network unplugged. **A user must never be able to tell that an online version was ever drawn.** If Supplies feels thin, the answer is a second authored pack — never a search field that returns the same sixteen things.

## 16.8 Cost and risk

| Item | Cost | Label |
|---|---|---|
| Drawing 16 primitives | Design work, not engineering | 🟨 INFERRED |
| Supply tray UI | New surface; HTML spec first | 🟨 |
| `DecorElement` + schema v1→v2 | ⚠ one runtime-crash site, 7 compile sites, ~17 silent-degrade sites (§9.5) | ✅ VERIFIED |
| Font statics | Source and place 8 files, **no subsetting**. Size ~420KB installed | ✅ decision VERIFIED · 🟨 size INFERRED |
| Colophon | One screen, four contents | 🟨 |
| Photocopier filter | Floyd–Steinberg over a downscaled bitmap, pure Kotlin | 🟨 |
| Online search | **Not built.** Avoided cost: networking lib, caching, versioning, failure states, moderation, provenance review, API keys, rate limits, takedown handling | — |

**Risks I am accepting:**

1. **Sixteen may feel thin.** No published data exists on asset-library utilisation — I looked. Mitigation: a second pack, never a bigger first one.
2. **A curated set homogenises too.** True, and unavoidable. The defence is that it is *our* house style rather than Canva's, which is the same trade PICO-8 makes deliberately.
3. **Four sources unresolved** (Met, Smithsonian, PD Vectors, Wikimedia) — unresolved, not rejected. None ships without primary licence text.
4. **The OFL clause-3 reading has now been through a second reader** and survived on the merits — subsetting is a Modified Version on two limbs of the definitions clause, and the cure is the `name` table rather than the UI label. One citation was wrong (TERMINATION, not clause 5) and is corrected. The *safe* path (no subsetting) was chosen precisely because it does not depend on the reading being right.
5. **⚠ I ruled outside my authority once in this document and did not notice.** "Not later" is constitutional (`V2-CONSTITUTION.md:253`, `:302`), and §VI reserves amendments to the owner *"never by a design or engineering session on its own initiative."* Caught by review, not by me. The lesson generalises: **before ruling something out permanently, check whether a ratified document has already ruled it *in* as a future option** — the Constitution's growth table is exactly that kind of clause and it is easy to miss when reading for prohibitions.

## 16.9 Status — adopted and landed

✅ **The owner adopted Amendment 3 on 2026-08-15**, closing the Constitution's permitted future online asset search permanently. The owner's recorded reasoning:

> *"We should not build an asset system whose trustworthiness depends on external sources we do not control."*

Everything below has landed in the repository:

| Artifact | Where | State |
|---|---|---|
| **ADR-104** — the asset layer | [`DECISIONS.md`](../DECISIONS.md#adr-104) | ✅ `Accepted` |
| **Constitution Amendment 3** — §III and §V amended | [`V2-CONSTITUTION.md`](V2-CONSTITUTION.md#amendment-log) | ✅ ratified, with evidence note |
| **`v21-bench.html` amendment** — the four online controls struck, chips → four authored families, orphaned CSS removed | [`mockups/v21-bench.html`](mockups/v21-bench.html) | ✅ amended in-file, with rationale block |
| **`V2-BENCH-REVIEW §E.6`** — *"do NOT freeze into implementation until a review + legal pass clears them"* | [`V2-BENCH-REVIEW.md`](V2-BENCH-REVIEW.md) | ✅ **both conditions discharged** |
| **ADR-103 + Amendment 2** — the world metaphor | [`DECISIONS.md`](../DECISIONS.md#adr-103) | ✅ `Accepted` |

**§III's internal contradiction is resolved** in favour of its first sentence: *no networking libraries*, without exception. The privacy claim is now **falsifiable rather than promissory** — verifiable from the manifest and the dependency graph, which no policy statement can match.

**Documentation reconciled** (classified rather than blanket-replaced, per the owner's instruction to preserve decision history):

| Class | Files |
|---|---|
| **Amended** — stated the withdrawn rule as current | `V2-CONSTITUTION.md` §III/§V · `V2-IDENTITY.md` (three layers → two) · `COMPOSE-V2-HANDOVER.md` |
| **Amended (frozen spec)** | `mockups/v21-bench.html` — header, `art · online` control, search field, online panel, disclosure, subtitle, chips, and the orphaned `.online`/`.toggle`/`.disclosure`/`.onresults`/`.priv`/`.search` CSS |
| **Historical record, bannered not rewritten** | `V2-BENCH-RESEARCH.md` (the Openverse evaluation is *why* the decision could be made well — it is where the licence-accuracy disclaimer was found) · `V2-BENCH-IA-INTERACTION.md` |
| **Historical by nature, left intact** | `DECISIONS.md` pre-104 entries · `mockups/v2-bench.html` (already superseded) · `V2-BENCH-CRITIQUE.md` · `BETA-DIRECTION.md` (superseded) · `PRODUCT-DIRECTION.md` (research input) |

---

# 17. What I am accepting risk on

Stated so it can be checked later, not buried.

1. **Amendment 2 demotes a ratified constitutional image to a register.** I read "quiet café" as an emotional promise, not spatial law. If the owner reads it as spatial law, the amendment is wrong — the parity queue stands regardless.
2. **The amendment batch is six routine items.** A1 was withdrawn (the third add verb is parity) and **A0 left the batch entirely** — it is not an amendment but ADR-104, the asset-layer ADR the freeze demanded. Nothing in the batch removes frozen capability.
3. **X6 (fonts) is the estimate I was most wrong about, twice.** First I said the asset cost was already paid — false, only Inter is bundled for documents. Then I said the fix was to source *and subset* eight TTFs — which would have breached Averia's Reserved Font Names. The settled answer is source-and-place, unmodified. If sourcing the eight proves awkward, the fallback is two voices (Averia + Inter), not zero.
4. **Sixteen supplies is argued from shipped systems, not a study.** If the first device pass shows it feels thin, the fix is a second pack, not a bigger first one.
5. **My "already exists" claims now carry evidence labels because six of eight were previously wrong.** Every ✅ in this document was read or measured; every 🟨 and 🔍 is honest about not being.

## The one thing that is genuinely the owner's call

**Nothing in §8 is returned as a question** — the decisions are made. The single item that is properly an owner decision rather than a design one:

> ### None. The one escalation I raised has been withdrawn.
>
> I sent you the Art/online question as a scope decision with a licensing tail. ✅ **It was already routed.** The owner-approved freeze fenced the online search behind an asset-layer ADR and a legal pass ([V2-BENCH-REVIEW §E.6](V2-BENCH-REVIEW.md), `DECISIONS.md:2793`); neither was ever written; `DECISIONS.md:2795` states plainly that *"anticipating is not authorising."*
>
> **Upholding NFR-1 requires no authorisation — only changing it would.** §16 writes the ADR and performs the legal pass. It is `Proposed` and owed an independent review, not an owner signature.
>
> If you *want* the online library, that is a scope change you would initiate and it would need a PRD amendment. Nothing in this document assumes you will.

Everything in §8: decided, and ready to build.
