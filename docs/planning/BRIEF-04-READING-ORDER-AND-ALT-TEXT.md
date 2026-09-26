# Brief 04 — Reading order, named undo, and alt text (accessibility)

> This brief incorporates the findings of [ZINELY-1X-READINESS-AUDIT.md](ZINELY-1X-READINESS-AUDIT.md). If this brief conflicts with an older research document, this brief and the cited authoritative ADR/decision take precedence. It never overrides an Accepted ADR, the V2 constitution or a frozen spec — where it needs one changed, it says so and names the amendment.

Status: **implementation brief; A1 + scrim ✅ COMPLETE** (2026-09-26, PR #78, [ADR-119](../DECISIONS.md#adr-119)) (owner rulings recorded 2026-09-26 in the
[decision gate](ZINELY-1X-DECISION-GATE.md); none of them changes D4's scope). Direction D4 of the [1.x plan](ZINELY-1X-IMPLEMENTATION-PLAN.md).
Base: `origin/main` @ `0aa7a7d` (code citations made at `5f7707a`; `src/main` unchanged since). Revised 2026-09-25 from the readiness audit; dependencies updated 2026-09-26.
**Readiness:**
- **A1 + scrim fix:** ✅ COMPLETE (2026-09-26, PR #78). It needed no owner ruling, no schema change and no visual
  change. Its ordering rule is a ZINELY-DESIGN-SYSTEM §4.5 clause recorded as [ADR-119](../DECISIONS.md#adr-119)
  (gate 2). The gate-1 spike worked: `traversalBefore` hints are asserted on the JVM platform tree.
- **A3:** READY AFTER PR #70 is settled (merged or closed; it amends the same `v21-bench.html` and `EditorScreen.kt`) **and** the owner-approved `v21-bench.html` undo-snack amendment (gate 3; it replaces frozen copy). It is its own session, after A1.
- **A2:** READY AFTER a wording spec and a TalkBack listen (gate 4). A2 has not been audited.
- **B:** BLOCKED BY the wave-1 release (approved by the owner on 2026-09-26 as a v3 release before v4, Q8), the F1 v4
  bump, a Read-semantics design and the Describe amendment (the F3 fixtures are done, step 0) (gates 5–8).

## Gates before an implementation session may start

No owner question gates D4. Q8 (ruled 2026-09-26) approved the wave-1 release on v3 that B waits for; A1 and A3
ship in that release. D4's open questions are copy, wording and accessibility calls; they are listed under
[decisions that can wait](ZINELY-1X-DECISION-GATE.md#decisions-that-can-wait). The owner approves each re-frozen HTML
amendment (gates 3, 7, 8).

1. **Spike: can Robolectric show `traversalBefore` hints?** Work; A1's first task.
   - Compose sets the hints only when `isEnabled` is true, which requires accessibility to be on **and** at least one
     enabled service.
   - Route 1: a shadow `AccessibilityManager` that reports enabled and returns a non-empty service list.
   - Route 2: `RootForTest.forceAccessibilityForTesting(true)`. It is public on androidx-main. ⚠️ Confirm it exists in the
     pinned Compose version.
   - If both fail, A1 still ships. The JVM tests then prove declaration order and `traversalIndex`, and the device proves
     traversal.
2. **Write down the ordering rule and its threshold.** Work, done inside the A1 change. The authority is one canvas clause
   in [ZINELY-DESIGN-SYSTEM §4.5](../ZINELY-DESIGN-SYSTEM.md); the `SpatialOrder` KDoc links to it rather than restating it.
   The design system amends only "by a document of this rank, recorded as an ADR" (`ZINELY-DESIGN-SYSTEM.md:101-102`), so
   the clause lands with a new ADR (the next free number at implementation time: 119 on `main` today; ADR-115 is
   reserved by PR #70, and steps 1b and 4 also add ADRs, so check `DECISIONS.md` in-session).
3. **Amend the frozen spec for A3: the undo snack in `docs/design/mockups/v21-bench.html`.** The file is frozen under
   [ADR-099](../DECISIONS.md#adr-099). The amendment is its own reviewed change, before any Compose work
   ([UI/UX proposal](#uiux-proposal)).
4. **Spec A2's wording, then have the owner listen to it.** Work.
   - A one-page spec sets the phrase list and the word budget.
   - It places the phrase relative to the element's label and state.
   - It says how the phrase fits beside the 13–15 custom actions on each node.
   - The owner then listens to two or three candidate phrasings with TalkBack.
   - Extend an existing accessibility doc rather than creating a new one.
5. **B waits for wave 1 to be released**, before any v4 bump ([plan §5](ZINELY-1X-IMPLEMENTATION-PLAN.md#5-sequencing), release wave 1 and step 6).
6. **B needs two earlier sessions:** the F1b v4 bump (plan step 6), which adds the `description` field itself with
   `explicitNulls = false`, and the F3 fixtures (✅ done in step 0, PR #75). v4 stays unreleased until B lands. The
   fixtures are a writer-produced v2 archive plus v1–v3 `document.json`, all fictional and frozen.
7. **B needs Read semantics designed first.** Design work: Read speaks nothing from the page today ([Problem 4](#problem)).
   The design comes with its own HTML amendment.
8. **B needs a Describe verb amendment** to `v21-bench.html` `toolsFor()` (`:694`). The owner approves the amended HTML.

## Problem

1. **TalkBack reading order is list order: neither paint order nor reading order.**
   - ✅ `ElementSemanticsLayer` walks `page.elements` with `traversalIndex = index` (`ElementSemanticsLayer.kt:84-87,132`).
     Its comment claims "document (back-to-front) order".
   - Paint order is sorted by `zIndex` (`SceneRenderer.kt:44-45`), and two things make the orders disagree:
     - **Restacking** rewrites `zIndex` only (`ZOrder.kt:30-45`, `Command.kt:36-47`).
     - **Make-spread** sends the source to the back and appends the partner half with the lowest `zIndex`
       (`EditorReducer.kt:237-242,258`, `Command.kt:123`). This diverges even before any restack.
   - ✅ No test asserts element traversal order. The only order test covers `ZinelyV2CanvasSemantics`, which is unused.
   - Z-order would be wrong for listeners anyway ([below](#what-others-teach-us)).
2. **Undo doesn't say what it undid.**
   - ✅ `stepHistory` (`EditorReducer.kt:582`) announces only "Changed page N", and only when undo jumps page (`:618`).
   - That string is hard-coded English outside `Copy` and the prose guard, and no reducer test covers it.
3. **Photos have no description.** ✅ Every photo is "Photo" (`EditorA11y.kt:34-38`). Art speaks its supply name.
4. **Read mode speaks nothing from the page.**
   - ✅ `ProofRead.kt:554,629,674` clear the leaf and the stack edges with `clearAndSetSemantics {}`. Only the tap edges and
     the page readout speak.
   - ⚠️ The frozen `v21-proof.html:634` makes `#book` `aria-live="polite"`, so the spec and the build already disagree.
   - The old criterion "empty descriptions fall back to today's text" had no text to fall back to.
5. **A scrim is still an unlabelled clickable node**, a beta.5 known limitation.
   - ✅ `ZineActionScrim` (`ZineActionSheet.kt:265-278`) lacks the one-modifier fix that `ZSheet` has (`ZSheet.kt:176-184`).
   - That fix is `clearAndSetSemantics {}` placed **before** `clickable`.

## Product audit

### User value
- **Blind and low-vision makers** gain a page that reads in page order, and an undo that says what it took back. Today a
  restack or a spread scrambles the order, and undo is mute unless it jumps page. That is a shipped defect against
  [ZINELY-DESIGN-SYSTEM §4.5](../ZINELY-DESIGN-SYSTEM.md): "the visual order and the accessibility order are the same order".
- **Sighted makers** get named undo too. An undo off-screen or on another page stops being a guess. 🟨 The benefit is
  smaller, and Pass 2 must show it is felt, not just tolerated.
- **Readers** gain only from B, and only once Read speaks descriptions.

### Creative value
Low and indirect: the confidence to try things. Alt text is authorship, the maker's words about their own picture.

### UX cost
None visible for A1, A2 or the scrim. A3 adds a snack per undo. B adds a verb to a crowded bar, hence its own amendment.

### Cognitive load
- 🟦 **The undo snack is the one noise risk.** Five undos means five snacks. Mitigations:
  - no button;
  - each snack replaces the last in the one existing snack slot (`EditorScreen.kt:342-347`);
  - two to four words.
- Pass 2 asks whether rapid undo feels chatty. If it does, the remedies, in order: shorter copy, a shorter duration,
  showing the snack only when the change is off-screen or on another page, or no visible snack at all (TalkBack still
  speaks, through the same live region). Never add a second channel.
- 🟨 A2 adds words to every node. CHI 2021 found cognitive load to be the main barrier, so A2 gets a word budget and a listen.

### Accessibility
This is the accessibility direction. The bar is the platform tree and a human ear ([below](#accessibility-considerations)).

### Offline-first and ownership
All on-device. Descriptions are plain text in the document, so they back up and restore with the zine after v4. They are
never generated: "alt text should be written by the maker — it's their voice" (research
[§16.3](../research/ZINELY-FUTURE-PRODUCT-RESEARCH.md#163-priority-order-for-intelligence)).

### Physical publishing (PDF and paper)
✅ `PdfDocument` has no tagging, alt-text or structure API ([reference](https://developer.android.com/reference/android/graphics/pdf/PdfDocument)).
There is **no accessible-PDF promise**. Descriptions reach readers only in Read, and later in a reader's edition.

### Product identity
- **The lens.** "Imperfect surface, perfect mechanics" is the owner's evaluation lens. Its ratification as research proposal
  D13 is still open.
- **Why it fits.** D4 is pure mechanics, and both mechanics are exact:
  - the order is deterministic and immune to restacking;
  - an undo name is never wrong.
- **Cut, because it would make Zinely a generic editor:**
  - a history panel;
  - coordinate readouts;
  - an alignment inspector;
  - a nagging accessibility checker;
  - AI descriptions.
- **The frozen header's own line:** "undo is a snack, not a dialogue". Named undo stays one transient scrap of paper.

## What others teach us

*What can we learn without turning Zinely into Canva?*

- **Screen readers read artboards in z-order, and that misleads.** ✅ Schaadhardt, Hiniker and Wobbrock, CHI 2021
  ([PDF](https://faculty.washington.edu/wobbrock/pubs/chi-21.03.pdf)).
  - **Scope:** 15 blind participants on **desktop**, using PowerPoint, Keynote and Google Slides with JAWS, NVDA or
    VoiceOver. No TalkBack, no phones.
  - **Findings:**
    - every screen reader observed announced objects in z-order, which misled participants;
    - most participants expected left-to-right, top-to-bottom order;
    - relative position was the weakest task, and one participant preferred "near the bottom" to measurements;
    - the authors ask that a robust artboard "describe what that manipulation was" and make undo easy.
  - **What we take:** row-major order (A1), coarse words and no coordinates (A2), and naming the change (A3).
  - **Limit:** the sample is small and desktop-only. That is a reason for the device listen, not a substitute for it.
- **WCAG 2.2 SC 1.3.2** ✅ ([Understanding](https://www.w3.org/WAI/WCAG22/Understanding/meaningful-sequence.html)): a correct
  reading sequence must be *programmatically determinable*. → The order must live in the platform tree: declared children
  **and** `traversalIndex`.
- **Procreate names the undone step, transiently.** ✅ "A notification will appear at the top of the interface to let you
  know which action your undo affected" ([Handbook](https://help.procreate.com/procreate/handbook/interface-gestures/gestures)).
  → Name the step transiently, with no history panel.
- **Canva and Google Docs offered nothing to learn from.** Canva's help describes plain Undo/Redo buttons with no step
  naming ([Help](https://www.canva.com/help/undo-and-redo-changes/)). ⚠️ Google Docs could not be verified from a primary
  source, so it is not used.
- **The announcement API is deprecated.** ✅ API 36 deprecates `View.announceForAccessibility`, because "accessibility services
  may choose to ignore events dispatched with this method" ([View](https://developer.android.com/reference/android/view/View#announceForAccessibility(java.lang.CharSequence))).
  → The snack's live region is the right single speaker.
- **Decorative images need no label.** ✅ WCAG SC 1.1.1 ([Understanding](https://www.w3.org/WAI/WCAG22/Understanding/non-text-content.html),
  Situation F) and Android's [content-label guidance](https://support.google.com/accessibility/android/answer/7158690) both say
  so. → This informs B's open "decorative Art" question without settling it.

## User story

*As a maker using TalkBack, I want to hear the things on my page in a predictable top-to-bottom, left-to-right order, and
to hear what Undo just took back — and later to describe my photos in my own words — so I can make a zine with less
guessing.*

🟨 Row-major order is predictable, not always the order a maker intends: a two-column layout whose columns don't share
row tops interleaves them. A2's relative phrases and the maker's own arrangement are the remedy, not a smarter guess.
*As a sighted maker, I want Undo to show what came back, so an undo off-screen or on another page isn't a mystery.*

## Experience

- **A1:** swiping goes top to bottom, then left to right, whatever the stacking. A full-page photo or a spread half is read
  first, as the page's ground.
- **A2** (pending its spec): one short relative phrase per element, e.g. "top half".
- **A3:** Undo shows a snack with no button that names what came back, plus the page when it changes page. TalkBack
  speaks it **once**. Whether Redo shows a snack, and whether Redo speaks, are open.
  - The words follow [V2-CONSTITUTION §II.4](../design/V2-CONSTITUTION.md) ("physical metaphors over software
    metaphors"): paper words like the frozen "Put back", not "Undid: move photo". The "Undid: …" strings below are
    **placeholders** for the copy spec, not proposed copy.
- **B:** a selected photo or Art piece offers an optional **Describe** field.
  - TalkBack speaks the maker's text on the Bench, and in Read once Read semantics exist.
  - Leaving it empty is fine. It never gates anything and never nags.

## UI/UX proposal

- **A1, A2 and the scrim: no UI change.**
  - Add a clause to [ZINELY-DESIGN-SYSTEM §4.5](../ZINELY-DESIGN-SYSTEM.md): on a free canvas, "visual order" means spatial
    reading order, not z-order.
  - §11 rule 6 says "Fixing traversal with an override is treating the symptom". A1 respects it: the fix is structural,
    with children **declared** in reading order. `traversalIndex` only restates that order.
- **A3: amend `v21-bench.html` first.**
  - ✅ The frozen prototype already has a post-undo snack: `toast('Put back',false)`, with no button. It shows after the
    snack's or the bar's Undo, for deletes only (`:808-812`, `toast` `:801`, markup `:619`).
  - ✅ Compose never built it: the bar's Undo hides the snack (`EditorScreen.kt:2048-2056`), and no Kotlin file contains
    "Put back". That is a latent parity gap.
  - The amendment extends the state to every undo, replaces "Put back" with the named copy, and adds a gallery state for
    the page-changing variant.
  - Replacing words inside a frozen state is a copy change, so the owner approves the amended HTML.
  - The amendment draws the undo snack at 360 dp **and at 200 % text**, with the longest label plus the page clause.
- **B: a Describe verb** on the image and decor bars, amending `toolsFor()` (`:694`).
  - The amendment must show Describe at 360 dp and at 200 % text.
  - Read semantics get their own amendment, starting from the `v21-proof.html:634` divergence.

## Interaction details

**A1 — ordering rule.** Pure, deterministic and independent of `zIndex`.
1. Compute each element's rotated, axis-aligned bounds in page points.
2. **Large-element guard.**
   - An element taller than *T* × the page height is **read first** and never opens a row.
   - 🟦 *T* ≈ 0.6. The exact value is set once, in the §4.5 clause; the `SpatialOrder` KDoc links to it.
   - The guard uses **height only**. A full-width, short banner is an ordinary row opener; that is intended, because it
     reads as a heading. A very wide element spanning two columns still reads before both columns.
   - Several large elements are read by top edge, then left edge, then id.
   - Without the guard, a full-page photo or a spread half (`0,0,W,H`, `EditorReducer.kt:231-236`) opens a row [0, H]
     that swallows everything else.
3. Sort the rest by top edge. The first unassigned element opens a row spanning [top, bottom]. Each next element joins
   that row if its **vertical centre** lies inside the opener's span; otherwise it opens the next row.
4. Within a row, sort by left edge. Break ties by element id, **never** by `zIndex`.
5. **Trap:** implement a procedure, not a `Comparator`. A pairwise "same row" test is intransitive, and `sortedWith` can
   throw "Comparison method violates its general contract".
6. Selection chrome and page-level nodes keep their current positions.

**A1 — wiring.**
- `ElementSemanticsLayer` iterates the `SpatialOrder` result and sets `traversalIndex` to each element's position in it.
  Child order then equals traversal order in every tool.
- The nodes are semantics-only (`ElementSemanticsLayer.kt:116-118`). ⚠️ The gold run confirms that no pixels change.

**A2** has no behaviour until its spec exists. The spec's constraints:
- coarse page regions only;
- at most one relation;
- a word budget;
- no coordinates;
- the element's name and state come first.

**A3 — named undo, corrected scope** ([audit §2 P1](ZINELY-1X-READINESS-AUDIT.md#2-plan-accuracy), [§4 F2](ZINELY-1X-READINESS-AUDIT.md#4-wave-0-audit)).
- ✅ **Every command already records what changed.**
  - Some carry before/after mementos (`Command.kt:24-25,38-39,74-75,94-95,114-117,148-149`).
  - The rest carry the placed or deleted element, or the page (`:50,58,160,168`).
- **So a pure `Command.editLabel(doc)` in `core:editor` returns `EditLabel(verb, kind, count)`.** `doc` is the pre-step
  document, where Transform and Reorder look up each id's element kind.
- **No `History` or `committing()` change** ([audit §15](ZINELY-1X-READINESS-AUDIT.md#15-what-not-to-change-yet)). The 23
  `committing(` call sites stay untouched.
- **Verbs come from the diff:**
  - Transform: position, size or rotation. When several change, the copy spec's precedence rule picks one.
  - EditImage: `assetId` → replace, `copier` → copier, the flip flags → flip, `crop`/`fit` → framing.
  - EditDecor: ink, supply or flip.
  - EditText: words or style.
  - Place, Delete and MakeImageSpread: named by their type.
- **Ambiguity 1: duplicate or add?** Both are a `PlaceCommand` (`EditorReducer.kt:416-419` vs `:65,79,102,114`).
  - **(a)** One copy fits both: "Undid: add photo". It is true for a duplicate too, and needs no code.
  - **(b)** A defaulted `PlaceCommand` flag, set only at the single `duplicateElement` call. `History` is never persisted.
  - 🟦 Lean (a). Take (b) only if the copy should mirror the existing "Duplicated" snack (`Copy.kt:657`).
- **Ambiguity 2: Reset framing, or a reframe that ends at `Crop.FULL`/`Fit.FILL`?** Both are an `EditImageCommand` with
  default framing in `after` (`EditorReducer.kt:188-192` vs `:173`), so the diffs are identical.
  - **(a)** One copy fits both: "Undid: framing change".
  - **(b)** A defaulted `EditImageCommand` flag, set only at `:192`.
  - Never call every return to default a "reset": after a manual reframe that is false. 🟦 Lean (a).
- *Observation:* an adjacent restack swaps two ranks (`ZOrder.kt:39-43`), so "A forward" and "B back" are the same diff.
  The kind-free "Undid: stacking change" fits every reorder.
- **One speaker.**
  - The snack's polite live region (`BenchSnack.kt:238-242`) is the only speaker.
  - The reducer replaces `Effect.Announce("Changed page N")` with a typed `Effect.HistoryStepped(label, isRedo, landedOnPage)`.
    There is no parallel `Effect.Announce`.
  - The effect is routed to the snack slot. Today the runner handles only Autosave, Announce and the picker
    (`EditorEffects.kt:114`).
  - Delete `Effect.Announce` once nothing emits it.
- **Repeats must speak.** A live region does not re-announce identical text. So the snack is **keyed per step** (a counter);
  whether TalkBack re-speaks the rebuilt node is checked on device.
- **Copy.**
  - A new `Copy.Undo` holds every word, including the page sentence, so "Changed page N" leaves the reducer.
  - ✅ `core:copy` has no project dependencies. `feature:editor` maps `EditLabel` to `Copy.Undo`, as `benchDeletedMessage`
    already does.
- **Open copy decisions** ([can wait](ZINELY-1X-DECISION-GATE.md#decisions-that-can-wait)):
  - the duplicate and framing words, (a) or (b);
  - **whether Redo shows a snack** — the frozen Redo is drawn disabled (`v21-bench.html:612`);
  - the Transform precedence words.

**B — alt text.**
- Plain text, trimmed, at most ~250 characters.
- Saved on dismiss as one whole-element `EditImageCommand`/`EditDecorCommand`, so undo needs no new code.
- An empty field is stored as absent, which needs `explicitNulls = false`.
- Open, to be decided by a listen:
  - "Photo: <description>" or the description alone;
  - whether Art can be marked decorative (skipped in Read, still reachable on the Bench).

## Current architecture touchpoints

All re-verified on `5f7707a`.

| Concern | Where |
|---|---|
| Semantics layer | `feature/editor/.../ElementSemanticsLayer.kt:84-87` (group + loop), `:116-118` (semantics-only), `:132` (`traversalIndex`) |
| Labels / custom actions | `EditorA11y.kt:34-38` / `:77` |
| Paint order; restack | `core/render/.../SceneRenderer.kt:44-45`; `core/editor/.../ZOrder.kt:30-45`, `Command.kt:36-47` |
| Spread | `Command.kt:111-124`; `EditorReducer.kt:220-273` (full page `:231-236`, z `:237-242,251,258`) |
| Commands; Reset / reframe; Duplicate | `Command.kt:22-177`; `EditorReducer.kt:188-192` / `:151-175`; `:320`, `:402-420` |
| History; `committing`; `stepHistory` | `EditorModel.kt:72`; `EditorReducer.kt:437-444`; `:582-620` (page announce `:618`) |
| Effects | `core/editor/.../Intent.kt:232-247`; `feature/editor/.../EditorEffects.kt:114` |
| Deprecated announce drain | `app/.../ZinelyNavHost.kt:911`, `:1005-1008`; `EditorViewModel.kt:157,316` |
| Snack | `BenchSnack.kt:189`, `:238-242`; `EditorScreen.kt:342-347`, `:2048-2056`; `Copy.kt:644-662` |
| Read | `ProofRead.kt:554,629,674`; spec `v21-proof.html:634` |
| Scrims | `ZSheet.kt:176-184` (fixed); `ZineActionSheet.kt:265-278` (not fixed) |
| JVM platform-tree harness | `feature/editor/src/test/.../a11y/SurfaceTraversalOrderTest.kt:92-96` |
| Schema (B) | `Document.kt:28`; `JsonDocumentSerializer.kt:38-43,119`; `ZineLibraryBackupStager.kt:367-376`; `ZineLibraryBackupValidator.kt:80`; `ZinePackageManifestValidator.kt:27`; `DefaultDocumentValidator.kt:25` |

## Files/modules likely affected

- **A1:** a new pure `SpatialOrder` in `core:editor` (jqwik 1.9.2 is already there, `libs.versions.toml:27`);
  `ElementSemanticsLayer.kt`; `ZineActionSheet.kt` (one modifier); the §4.5 clause; tests.
- **A3:**
  - `core/editor`: `editLabel`, `EditLabel`, `HistoryStepped`, `stepHistory`.
  - `core/copy`: `Copy.Undo`.
  - `feature/editor`: effect routing, the snack key, and the label-to-copy mapping.
- **A2:** a pure phrase helper in `core:editor`; `EditorA11y`; `Copy.A11y`.
- **B:** the Describe sheet and Read semantics. The `description: String?` field, the identity v3→v4 migrator and the
  six version readers' tests all land earlier, in the F1b session (plan step 6); B adds no shape change.

## Data model implications

- **A1, A2, A3:** no schema change.
- **B:** its field is added by F1b's single v4 bump (plan step 6), never by B itself; B is v4's first user, and v4 is
  released with B.
- ✅ **One v4 zine makes the whole library backup unrestorable on an older build.**
  - The stager throws `FUTURE_VERSION` (`ZineLibraryBackupStager.kt:367-376`), and the validator rejects the manifest (`:80`).
  - So B waits for the wave-1 release.
  - The bump's release notes must say that backups made after it can't be restored on earlier versions.
- **`explicitNulls = false`** must be added. Without it, every element gains `"description":null`.
  - It is `@ExperimentalSerializationApi`.
  - ✅ `core:model` has no nullable fields yet, so no existing bytes change.
- **The v3→v4 migrator is an identity step** (written in the F1b session). Tests there must show:
  - v4 is accepted and v5 is refused by all six readers;
  - the F3 fixtures still restore;
  - the wave-0 shape guard catches the field if it lands without the bump.

## Testing strategy

**Compose focus tests do not prove TalkBack behaviour.**
- `requestFocus` moves input focus, not TalkBack focus. This was proven on Samsung in beta.5
  ([release notes](../releases/0.9.0-beta.5.md), revert `d957f1f`).
- See also [issue 231606408](https://issuetracker.google.com/issues/231606408), "Accessibility: Request Focus is not working
  as expected".

- **JVM:**
  - jqwik: `SpatialOrder` is a permutation, deterministic, and unaffected by `zIndex` or list order.
  - Fixtures: rows, columns, rotation, the guard, a spread half, and restack-then-spread.
  - `editLabel`: one test per command type, and per sub-change for Transform, EditImage and EditDecor.
  - Reducer: exactly one `HistoryStepped` per step, none on an empty history, and `landedOnPage` only when the step
    navigates.
- **Robolectric:**
  - Declaration order and `traversalIndex` on the platform tree, via the `SurfaceTraversalOrderTest` harness.
  - `traversalBefore` hints, after the spike.
  - Correct that file's "the platform sets no hints" claim (`:92-96`). It is a setup artefact: Compose computes hints only
    when `isEnabled` holds ✅ ([source](https://raw.githubusercontent.com/androidx/androidx/androidx-main/compose/ui/ui/src/androidMain/kotlin/androidx/compose/ui/platform/AndroidComposeViewAccessibilityDelegateCompat.android.kt)).
  - `ZineActionScrim` has no clickable node in the Dialog window.
- **Roborazzi:**
  - A1 and the scrim must be visually neutral; prove it with `bash tools/grun.sh gold`.
  - A3 adds undo-snack goldens after its amendment.
- **Device:**
  - `uiautomator dump` **cannot** verify traversal. It lists child order and carries no traversal attributes.
  - ✅ Compose's `isEnabled` also excludes UIAutomator, so with TalkBack off there are no hints to dump at all.
  - Declaring children in sorted order makes the dump's child order meaningful.

| Automatable (how) | Device-only (what, where) |
|---|---|
| Spatial order is a permutation, deterministic, and unchanged by `zIndex` (jqwik, JVM) | Swipe order on Bench after a restack and after a spread: Samsung SM-A176B (TalkBack 16.x), plus a Pixel if available |
| Child declaration order plus `traversalIndex` (Robolectric semantics) | Whether TalkBack honours `traversalBefore` beyond child order |
| `traversalBefore` hints with accessibility enabled (Robolectric; spike first) | Where focus lands when Bench, Read or a sheet opens |
| Labels, relative phrases and description fallback (unit tests plus merged/unmerged semantics) | The actual speech: its length, and undo spoken once, not twice (owner listen pass; adb input bypasses TalkBack on this phone) |
| Custom actions per element | The custom-actions gesture works (Samsung vs Pixel) |
| `ZineActionScrim` has no clickable node (Robolectric in the Dialog window) | TalkBack lands inside the sheet; Back closes it |
| Undo labels and one emitted effect (reducer tests) | A repeated identical undo is re-announced |
| v3→v4 migration, round trip, backup rejects v5 (JVM plus fixtures) | Read speaks descriptions page by page |

## Accessibility considerations

- **Both device passes** ([CLAUDE.md](../../CLAUDE.md#device-verification-mandatory)).
  - The owner does the TalkBack listen ([OWNER-CHECKLIST §2.2](../OWNER-CHECKLIST.md)). 🟨 A sighted listener is **proxy
    evidence**: it proves what is spoken and in what order, not whether a blind maker finds it usable. Record it as such.
  - For B, add a blind or low-vision tester if one can be found.
  - Pass 1 also covers **Switch Access** and a **hardware keyboard** (Tab / arrow focus): both follow the same order, and
    neither may land on the scrim.
- **Don't retry programmatic TalkBack focus** ([audit §15](ZINELY-1X-READINESS-AUDIT.md#15-what-not-to-change-yet)). Leave the
  ~12 `.requestFocus()` matches in `src/main` alone.
- **Open decision: replace the deprecated announce API everywhere?**
  - `View.announceForAccessibility` is deprecated in API 36, which Zinely targets.
  - A3 stops using it for undo.
  - Reframe and style still use it (`ZinelyNavHost.kt:1005-1008`).
  - The import summary likely speaks twice today (`EditorViewModel.kt:482-483`).
- **Speech order:** name, then state, then at most one relation.

## Design-system implications

A3 reuses `BenchSnack` unchanged. B adds one text-field sheet on existing sheet tokens. The only design-system text change is
the §4.5 canvas clause.

## iOS portability notes

- `SpatialOrder`, the phrase helper and `editLabel` are pure `core:editor`. Their words live in `core:copy`. No Android types.
- **Trap:** coupling rule 1 bans `Math.` in core ([audit §10](ZINELY-1X-READINESS-AUDIT.md#10-architecture--ios-implications)).
  Compute rotated bounds with `kotlin.math`, and don't import `SelectionChromeGeometry` from `render-android`.
- The one-speaker rule ports as it is: one announcement of the same `Copy.Undo` string. VoiceOver's order comes from the same
  `SpatialOrder`.

## Acceptance criteria

**A1 + scrim (wave 1)**
1. `SpatialOrder` passes its jqwik properties and its fixtures, including the guard and a spread half.
2. On the JVM platform tree, element nodes are declared in spatial order with a matching `traversalIndex`. This holds before
   and after a restack, and after a spread.
3. The spike's result is recorded. If the spike worked, `traversalBefore` hints are asserted.
4. `ZineActionScrim` has no clickable node, and a tap on it still dismisses the sheet.
5. `grun.sh gold` shows no golden change.
6. The §4.5 clause states the rule and the threshold, once; the KDoc links to it.
7. Both device passes succeed, and TalkBack swipe order is spatial after a restack and after a spread.

**A3 (wave 1, after A1)**

8. The `v21-bench.html` amendment is approved before any Compose change.
9. Every command type yields a label, and no label is false.
10. Each undo emits exactly one `HistoryStepped`, and no parallel `Announce`.
11. "Changed page N" lives in `Copy.Undo`.
12. On device, undo is spoken once, and a repeated identical undo is spoken again. Pass 2 finds rapid undo not chatty.
13. `History` and `committing()` are unchanged.

**A2**

14. The wording spec exists, and the owner listened, before any A2 code.

**B (after the wave-1 release, with F1)**

15. Gates 5–8 are met, and v4 ships with the other F1 fields and `explicitNulls = false`.
16. Descriptions persist, back up, restore and undo. Older builds refuse v4 with "needs a newer Zinely".
17. The Bench, and Read as designed, speak descriptions. An empty description never creates an empty or duplicated stop.
18. No public claim ("readable by people who can't see") until B *and* a reader's edition are Available
    ([ADR-118](../DECISIONS.md#adr-118)).

## Stop conditions

Stop and ask if any of the following happens:

- A1 would need to change the document's `page.elements` order or any command.
- The spatial order changes a golden.
- A3 seems to need any of these:
  - a `History` or `committing()` change;
  - a persisted label;
  - a second speaker;
  - a snack button;
  - a new component.
- A label could only be produced by guessing: a third ambiguity beyond the two above.
- B is due to start before the wave-1 release, or the schema would be bumped outside F1.
- Someone cites a test as proof of where TalkBack goes.

## Out of scope

- A history panel or a history cap. ✅ There is no memory risk: history holds only small mementos.
- Programmatic TalkBack focus.
- Tagged PDF.
- The reader's-edition export.
- Large print.
- Automatic descriptions.
- Coordinate readouts.
- Everything in [audit §15](ZINELY-1X-READINESS-AUDIT.md#15-what-not-to-change-yet).

## Future extension

- "Describe this page" in Read: the spatial order plus the descriptions, spoken as one page. It builds on the Read-semantics
  design.
- A plain-text reader's edition, shared beside the PDF.
- Fold guidance that passes a text-only test (research A10).

## Change log

| Date | Change |
|---|---|
| 2026-09-26 | Owner rulings recorded (decision gate): no D4 scope change; A3 now also waits for PR #70 to be settled; B's wave-1 release approved as a v3 release before v4; ADR numbering note widened (steps 1b and 4 add ADRs too). Base `0aa7a7d`. |
| 2026-09-26 | Base moved to `eb75cf7`: Step 0 merged (PR #75), so the F3 fixtures are done and no longer a B blocker. No scope change. |
| 2026-09-25 | Folded readiness-audit corrections: command-derived undo labels, with no `History`/`committing()` change (P1, §4 F2); the duplicate and reset-framing ambiguities, with minimal options; one speaker, keyed per step; "Changed page N" moved into `Copy`; the row-rule collapse guard; the spread partner as a second cause of divergence; the `uiautomator` limits, the Robolectric `UNDEFINED` artefact and the spike; the `ZineActionScrim` fix bundled; Read speaks nothing, so B needs a Read design; A2 unaudited; the automatable/device-only table; the v4 whole-backup consequence and `explicitNulls`; the deprecated announce API; readiness per part. Added the product audit, the research, the frozen "Put back" state and the `v21-proof.html:634` divergence. Base moved to `5f7707a`, lines re-verified. |
