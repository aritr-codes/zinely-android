# Brief 02 — Three voices (Bench typography)

> ⚠️ **Corrections pending.** The [1.x readiness audit](ZINELY-1X-READINESS-AUDIT.md#6-d2-audit--typefaces--voices) (2026-09-25, reviewed) found coverage gaps, a missing editing-surface change, older-build clipping vs whole-backup refusal, and a reframed O12. Fold them into this document before any implementation session uses it ([audit §12](ZINELY-1X-READINESS-AUDIT.md#12-recommended-implementation-sequence)); until then, read that section beside this one.

## Pre-rewrite record (2026-09-26) — rulings in, asset facts measured, rewrite still pending

**Still not ready.** The owner ruled Q2 and Q4 on 2026-09-26 ([decision gate](ZINELY-1X-DECISION-GATE.md#q2-typefaces-o12--o8));
the body below predates those rulings and is superseded wherever it disagrees. A rewrite session folds this
record, the audit's §6 and the rulings into one brief, then removes the banner.

**Rulings the rewrite must implement (owner, 2026-09-26):**
- **Two document voices: Book = Fraunces, Plain = Inter.** Hand (Averia) is **deferred** as a document voice;
  Averia stays the interface voice. The title and "three voices" framing below are superseded.
- **Scope ruling, not a rewrite:** text inside a zine may use a document voice; a document voice is not a UI
  typeface under [V2-CONSTITUTION §III](../design/V2-CONSTITUTION.md) as written.
- **No schema bump for document voices.** Compatibility rule: older builds keep restoring and preserving the
  content (the `fontFamily` string survives); they may draw an unknown voice as Inter. That is a **layout
  change, not data loss**, and the rewrite must document and test its extent honestly (re-wrap, possible
  overflow of a fixed box).
- **Typebar and Reframe freeze** (Q4): the Voice control amends `v21-typebar.html` after it is frozen
  ([plan step Q4-F](ZINELY-1X-IMPLEMENTATION-PLAN.md#5-sequencing)).
- **Fold study is no longer a gate** (Q5). PR #70 (ADR-115) must still be settled first.

**What Zinely actually bundles (✅ measured 2026-09-26 with fontTools from the files in this tree).** The
owner's caveat holds upstream — Fraunces ships Roman and Italic variable fonts (wght, opsz, SOFT, WONK axes)
— but **none of that is in the repository:**

| File (tracked) | Name table | Weight | Style | Variable? | Latin Ext-A | Greek / Cyrillic |
|---|---|---|---|---|---|---|
| `core/ui/src/main/res/font/fraunces_regular.ttf` (102 KB) | "Fraunces 9pt" | 400 | Roman | static | 126/128 (no ŉ ſ) | 0 / 0 |
| `…/fraunces_medium.ttf` (80 KB) | "Fraunces 9pt Medium" | 500 | Roman | static; **instanced by us** from upstream (`opsz=9 wght=500`, [DECISIONS.md ADR at ~:1944](../DECISIONS.md)) | 126/128 | 0 / 0 |
| `…/fraunces_semibold.ttf` (105 KB) | "Fraunces 9pt SemiBold" | 600 | Roman | static | 126/128 | 0 / 0 |

- **No Fraunces Bold (700), no Italic, no BoldItalic** exists anywhere in the tree; `post.italicAngle` is 0
  on all three. All are `fsType 0` (embeddable), OFL (`feature/editor/src/main/assets/fonts/OFL-Fraunces.txt`).
- **They are chrome fonts, in the wrong module.** They live in `:core:ui` `res/font`; document fonts live in
  `render-android/src/main/assets/fonts/`, which holds only Inter's four faces.
- **The registry needs four real static faces per family and forbids variable fonts** (minSdk 24):
  `DocumentFontRegistry.kt:3-9`, one `DocumentFontFamily(regular, bold, italic, boldItalic)` row per family
  (:102-113). DECISIONS.md already names the route: *"four static instances … the three weights in
  `core/ui/src/main/res/font/` are the wrong set and carry no italic"* (~:6165-6169).
- **So the D2 asset work is:** instance four static 9 pt faces from the upstream variable fonts — Roman 400,
  Roman 700, Italic 400, Italic 700 (🟦 the weights are a recommendation; 9 pt matches the chrome cut and the
  document's small sizes) — pin each by hash like `fraunces_medium.ttf`, add one registry row and the OFL,
  and extend `FontCoverageGuardTest`. Expect ~400 KB of new assets (🟨 estimate from the 80–105 KB statics).
- **Width, x-height:** Regular is ~2 % narrower than Inter on a pangram; x-height at 10 pt is 1.66 mm vs
  Inter's 1.93 mm (research 2026-09-26). The Bold and Italic instances are **not yet measured**.

**Still needed before the rewrite is ready:**
1. Instance and measure the four faces (widths per style vs Inter, for the older-build layout note).
2. **Minimum print size — owner decision after the physical print test** (one printed page at 10/12/14 pt on
   a home inkjet — a separate physical test; the [print-and-fold study](STUDY-PRINT-AND-FOLD-PROTOCOL.md) excludes it). Not decided here.
3. Coverage handling for scripts Fraunces lacks (Greek, Cyrillic): 🟦 recommend Book is disabled with a
   reason for text containing them, per the audit's per-family coverage rule; the rewrite specifies it.
4. The editing surface draws chrome Inter with synthesised italic (`BenchEditingSurface.kt:213-224`); the
   rewrite specifies drawing the document face.
5. PR #70 settled; `v21-typebar.html` frozen (Q4); the next free ADR number at that time (D2's ADR
   supersedes ADR-055's exclusion and records the scope ruling's consequence).

Status (superseded by the record above): **implementation brief, not authorised.** Part of the [1.x plan](ZINELY-1X-IMPLEMENTATION-PLAN.md)
(Direction D2, wave 1). Base: `origin/main` @ `5c40e7b`.
**Owner decisions first:** O7 (PR #70: remove the dead *Font* control now, or let this replace it), O8
(confirm the three faces; OWNER-CHECKLIST still lists PRD §13 Q3 "choose the bundled font set" as open),
O10 (whether `v21-typebar.html` is frozen), **O12, the Amendment 1 ruling** (below), and O14 (whether a
creative tool may start before the ROADMAP's fold-clarity study reports). This is a new owner
decision, not only a "finish": after ZINE-DIRECTION X6 said FINISH, the owner approved *removing* the dead
control (ROADMAP In development) and reopened fonts for evaluation, not scheduling.

## Problem

Every Zinely zine is set in Inter. `DocumentFontRegistry.Bundled` registers one document family
(`render-android/.../DocumentFontRegistry.kt:102-113`); any other `fontFamily` falls back to Inter (:88-89).
Meanwhile the Bench shows a permanently disabled **Font** verb
(`BenchContextBar.kt:144-147`, `enabled = false`, "Not available yet"). ZINE-DIRECTION §8 already ruled
**"Font as three named voices — FINISH … a dead control is a launch blocker"**, and §15 X6 specifies how:
*"Source and place 8 static TTFs — no subsetting … ~840KB. No schema change (`TextStyle.fontFamily`
exists)."* The ROADMAP creative-tools assessment puts fonts first.

## User story

*As a maker, I want the words in my zine to have a voice — rough, bookish or plain — so the zine sounds like
me, without having to become a typographer.*

## Experience

Select a text element → tap **Font** → three large samples, each set in its own face:

| Voice | Face | Character | Constitutional role ([V2-CONSTITUTION](../design/V2-CONSTITUTION.md), typography) |
|---|---|---|---|
| **Hand** *(working name)* | Averia Sans Libre | imperfect, made-by-a-person | "the maker's own short strings" — **never running text** |
| **Book** | Fraunces | editorial, old-press | "zine body, captions, pull quotes" |
| **Plain** | Inter | clear, today's default | work |

⚠ **Amendment 1 is binding on document text.** V2-CONSTITUTION: *"the imperfect face never sets running text
… A violation of that sentence is a violation of this constitution, not a style preference."* So Hand cannot
be offered for any text of any length without an owner ruling (O12). Options for the owner: (a) Hand offered
only for short text (a character/line threshold defined in the spec) and disabled with a reason above it;
(b) Hand offered always, with the constitution amended; (c) two voices only (Book + Plain). This brief does
not choose.

Tap one; the text changes; one undo step. The choice is per text element. New text keeps today's default
(Inter) unless the owner rules otherwise. Names are placeholders: the HTML amendment settles them against
ZINE-DIRECTION's "Voice / Editorial / Work" roles and BP-4 (don't teach a word the maker didn't ask for).

## UI/UX proposal

- **HTML first.** ZINE-DIRECTION A4 records that the frozen Bench "draws the chip; what it opens is
  unspecified" (`v21-bench.html` text verbs include `['Font', ICON.font]` with no handler). Specify the A4
  surface in `v21-bench.html` (and `v21-typebar.html` if O10 says it is frozen): a compact three-row popover
  anchored to the verb, the same family as the ink popover, not a sheet.
- Each row shows the **same sample word** from the selected text (fallback "Zine") in its face, plus the
  voice name in UI chrome type. Current voice marked with a check, not colour alone.
- No font list, no search, no size control here (ZINE-DIRECTION DO NOT BUILD: "free font picker").

## Interaction details

- Opening the popover does not change the document; tapping a voice dispatches one intent and closes it.
- Re-selecting the current voice is a no-op (no undo entry).
- Bold/italic toggles keep working. **Every face needs all four statics.** `DocumentFontFamily` requires four
  non-null assets and `BundledFontResolver` loads them at construction and **never synthesises**
  (`render-android/.../FontResolver.kt`, KDoc "never synthesised"). That is X6's "8 static TTFs"
  (Averia R/B/I/BI + Fraunces R/B/I/BI; upstream ships all eight; Inter's four exist). No synthesis branch.
- **Script coverage is the hard part (ADR-070).** `FontCoverage.requiredCodePoints()` requires Latin Ext-A,
  Greek and Cyrillic of **every** registered family, and `incompleteFamilies` guards it. Averia and Fraunces
  very likely lack Greek/Cyrillic (verify against their cmaps first). And `analyzeTextCoverage`
  (`core/model/.../TextCoverage.kt`) has **no family parameter**: Greek typed in Fraunces would get no
  warning and fall back to device glyphs, breaking deterministic export. Required design work:
  1. a family-aware coverage model in core (supported scripts per voice, from bundled cmaps);
  2. per-family required sets in `FontCoverage` (Inter keeps today's set);
  3. a rule for text whose script a voice lacks: the voice is disabled for that text with a spoken reason,
     or the existing warning fires per voice. Never silent device fallback.

## Current architecture touchpoints

| Concern | Where |
|---|---|
| Model | `core/model/.../Document.kt:189-196` `TextStyle(fontFamily = "sans-serif", …)` |
| Registry / resolver | `render-android/.../DocumentFontRegistry.kt`, `FontResolver.kt`, assets `render-android/src/main/assets/fonts/` (Inter ×4 + OFL.txt) |
| Layout (all surfaces + PDF) | `render-android/.../SharedTextLayout.kt:38-60` via `CanvasReplayer` |
| Coverage | `render-android/.../FontCoverage.kt`, `CmapCoverage.kt`; `core/model/.../TextCoverage.kt:50` |
| Verb and style UI | `feature/editor/.../BenchContextBar.kt:144-147`, `TypeBar.kt`, `BenchStyleRow.kt` |
| Reducer | `core/editor/.../EditorReducer.kt`, `Command.kt` `EditTextCommand` (field memento) |
| Copy | `core/copy/.../Copy.kt` `BenchVerbs.FONT` (:212), `NOT_YET` (:248) |
| Licences UI | Colophon `Licences & credits` ([ADR-117](../DECISIONS.md#adr-117)) |

The UI already ships Averia and Fraunces as **chrome** fonts (`core/ui/src/main/res/font/`: Averia R/B,
Fraunces R/M/SB); document fonts are a separate set loaded from `render-android` assets. Do not share files
across the two sets unless byte-identical and licence-listed once.

## Files/modules likely affected

- `render-android`: new asset files; `DocumentFontRegistry.Bundled` gains two families; `FontCoverage` tests
  per face; golden tests for each voice.
- `core/editor`: an intent like `SetTextVoice(elementId, family)` → `EditTextCommand` (or the existing style
  path); property test that it round-trips through undo/redo.
- `feature/editor`: the popover; enabling the verb; removing `unavailableBecause`.
- `core/copy`: voice names and descriptions; remove `NOT_YET` usage if unused.
- Colophon licences: add Averia and Fraunces document-font notices (OFL texts).
- Docs: a new ADR superseding ADR-055's "excludes font choice"; ROADMAP; CHANGELOG.

## Data model implications

**No schema bump needed to keep the data.** `fontFamily` is a known serialised field; nothing rebuilds
`TextStyle` and the style patch leaves it alone (`EditorReducer.kt:461`). Store stable family ids, never
display names.

- **Wire names:** existing text stores `"sans-serif"` (the default, `Document.kt:190`), which is not
  registered and reaches Inter only by fallback; the registry name is `"Inter"`. Define the Plain voice as
  `{"sans-serif", "Inter"}` for display (checkmark, no-op rule) and write one canonical id for new choices.
- **Older builds:** they keep the id but **render and export Inter** (`isRegistered` has no production
  callers). That is exactly the ROADMAP's warning ("unknown-font fallback must not silently become layout
  loss"): line breaks and page fit can change. Choose one, recorded in the ADR: (a) ship fonts inside the
  plan's F1 v4 bump so older builds refuse the zine ("needs a newer Zinely"), or (b) accept the fallback and
  say so in release notes. 🟦 Recommendation: in wave 1, (b) — the plan keeps wave 1 free of schema bumps;
(a) only if the owner moves D2 into wave 2 beside F1.

## Testing strategy

- Pure: registry resolves each id/style; `"sans-serif"` and `"Inter"` both count as Plain; unknown id →
  Inter with the original value preserved.
- Reducer: voice change is one undo entry; redo restores; no-op on same voice.
- Coverage: each face's cmap against its declared script set; Greek/Cyrillic in a voice that lacks them
  takes the chosen rule (disabled or warned), never device fallback; Bengali/Tamil still warn.
- Render goldens: each voice × regular/bold/italic on the Bench and in the exported PDF raster; Read mode.
  Run `bash tools/grun.sh gold`; read every `*_compare.png` before recording.
- APK size measured before/after (budget ≈ +0.8 MB per X6) and recorded in the ADR.
- Device passes: Samsung, TalkBack on the popover; PDF opened in two viewers.

## Accessibility considerations

- The popover is a single-select group: each row announces name + "selected"; ≥48 dp rows.
- The sample word is decorative for TalkBack (announce the voice name, not "Z-i-n-e").
- Chrome follows system font scale to 200 %; **zine text does not** (it is print) — keep that invariant.
- A voice that is unavailable for the selected text (Amendment 1 length rule, or script coverage) is shown
  disabled with a spoken reason, never hidden.

## Design-system implications

A new popover instance using existing tokens (ink popover family). No new colours, no new type roles in
chrome. Document faces are content, not design tokens.

## Acceptance criteria

1. A4 surface specified in HTML, reviewed, frozen; O7, O8, O10, O12 (Amendment 1) and O14 answered by the owner.
2. Three voices selectable per text element; Bench, page nav, Read and PDF render identically (goldens).
3. Bold/italic behave per the spec for every face; PDF matches screen.
4. Old zines open unchanged; an unknown family renders Inter and survives a save.
5. Script coverage is family-aware; no text ever renders with device fallback glyphs in any voice.
6. Font files are unmodified OFL statics with licences in Colophon; APK delta recorded.
7. Pixel parity, both device passes, independent review.

## Out of scope

More than three voices; font size or tracking controls; downloadable fonts; a font picker list; changing
chrome typography; per-zine default voice (possible later extension).

## Future extension

A per-zine "house voice" default for new text; a fourth face only via the Tribunal ("font packs pipeline —
DEFERRED"); more scripts through faces that cover them (ADR-070's deferred work).
