# Brief 03 — Printer test page (Proof)

> ⚠️ **Corrections pending.** The [1.x readiness audit](ZINELY-1X-READINESS-AUDIT.md#7-d3-audit--printer-test-page) (2026-09-25, reviewed) found flaws in the band test, an unverified "Actual size" promise, and a staged alternative. Fold them into this document before any implementation session uses it ([audit §12](ZINELY-1X-READINESS-AUDIT.md#12-recommended-implementation-sequence)); until then, read that section beside this one.

> **Status 2026-09-26: still blocked on the physical print study.** The owner ruled Q7
> ([decision gate](ZINELY-1X-DECISION-GATE.md#q7-print-o13--d3-stage-2)): design truthful, result-based
> guidance now; ship nothing until the [print study](STUDY-PRINT-AND-FOLD-PROTOCOL.md) has measured the real
> Zinely print path; any "100 %" advice is conditional on the print app exposing a scale control. This brief
> is rewritten after the study reports.

Status: **implementation brief, not authorised.** Part of the [1.x plan](ZINELY-1X-IMPLEMENTATION-PLAN.md)
(Direction D3, wave 1). Base: `origin/main` @ `5c40e7b`.
**Decisions first:** a new ADR amending [ADR-039](../DECISIONS.md#adr-039)'s ruler deferral; an additive
amendment to the frozen `v21-proof.html`; access to at least three printer brands. This is **more than
finishing ADR-012**: ADR-012 asked for an on-sheet ruler, while this brief adds a separate test page and
(part 2, optional) a device reach profile that would also change the Bench keep-clear cue. Part 2 is a new
product decision (O13) and amends `v21-bench.html` and the keep-clear rulings
([P2 keep-clear proposal](../proposals/2026-08-13-p2-keep-clear-and-snap-guides.md)) as well.

## Problem

[ADR-012](../DECISIONS.md#adr-012) (Accepted) requires exact paper size, a ~6 mm safe inset, a **50 mm
calibration ruler** and "Actual size, fit-to-page off" guidance. Everything except the ruler shipped.
ADR-039 deferred the ruler **with cause**: the single-sheet format tiles the sheet edge to edge, so there is
no margin to hold it. Proof already tells makers to *"Print one test sheet first and fold it"*
(`Copy.kt:1357`) but gives them nothing designed to be a test. First prints fail in a few predictable ways
(shrunk by fit-to-page, clipped by the printer's reach, A4/Letter mismatch), and the maker blames themselves.

## User story

*As a maker about to print for the first time, I want a quick test that tells me whether my printer prints
at true size and how close to the edge it reaches, so my zine comes out right the first time.*

## Experience

In Proof's Print act, one optional line: **"First time with this printer? Print a test page."**

1. Save or share the test page (the same two honest exits as the zine PDF; no in-app print — ADR-052).
2. The page carries: a **50 mm ruler** ("measure this: it should be exactly 5 cm"); **edge bands** at 3, 5
   and 8 mm on all four sides ("which is the first band you can see?"); a **folding dummy** — the real
   8-panel grid, numbered 1–8 in reading order, so folding it teaches the fold and proves the order; the paper
   size it was made for, in words.
3. Back in Zinely, two questions: *"Did the ruler measure 5 cm?"* (Yes / No, it was smaller) and *"Which band
   could you see first?"* (3 / 5 / 8 mm / none).
4. **Part 1 (ship first):** the ruler answer. "Smaller" shows the fit-to-page fix in plain words and offers
   to reprint the test. The band answer is reassurance: 3 mm or 5 mm → "your printer reaches past Zinely's
   6 mm safe margin — nothing to change".
5. **Part 2 (only if O13 says yes):** if the 8 mm band was the first visible (the printer cannot reach
   6 mm), the warning widens **on sheet-edge panel edges only**. Nothing else changes.

## UI/UX proposal

- Amend `v21-proof.html` (frozen 2026-08-10): the entry line in the Print act near the existing "print one
  test sheet" advice; a small answers sheet; the test page itself as a specified artboard (A4 and Letter).
- Keep it optional and skippable; never gate the zine's own Save/Share on it.
- One remembered profile ("This printer reaches 5 mm") shown as a quiet fact with *Change*; not a list of
  printers — Android does not tell us which printer the user will pick.

## Interaction details

- The profile is **device-local and singular**: last answers win. No printer names (unknowable offline and
  after Share hands off to another app).
- **Why answers below 6 mm change nothing:** the imposition's safe inset (17 pt ≈ 6 mm) is the floor, so
  only a printer that cannot reach 6 mm gives new information. "None visible" → suggest checking paper size
  and scale.
- **The current warning cannot express a per-printer reach yet** (verified): `keepClearInsetPx`
  (`BenchStudioSurface.kt:305-309`) hard-codes `Imposer.DEFAULT_SAFE_AREA_INSET_PT` with no parameter;
  `ElementSemanticsLayer.kt:114` uses the same default; and `crossesKeepClear` applies one inset to **all four
  edges of every page**, fold edges included. Printer reach only matters where a panel edge is the **sheet**
  edge, and the top row of panels is rotated. Part 2 therefore needs a pure per-page, per-edge inset
  function derived from the imposition (which page edges lie on the sheet edge, after rotation), used by the
  Bench cue, the spoken state and Proof.
- Proof's "printer can't reach here" band is a fixed decorative 9 px on a schematic (`ProofSheet.kt:208`),
  not a measurement; part 2 must decide whether it becomes one.
- Ruler "No" → explain *Actual size / 100 % / fit-to-page off* (text already exists around
  `Copy.ProofPrint` `SCALE_VALUE` "100% · Actual size") and offer to reprint the test.
- **It never re-imposes the zine.** Panel edges are fold lines; shrinking the grid inside a margin would
  move them off the physical folds. On `SINGLE_SHEET_8` the inset is a content safe-zone inside each panel.

## Current architecture touchpoints

| Concern | Where |
|---|---|
| Imposition inset | `core/imposition/.../Imposer.kt:26,37-45` (`DEFAULT_SAFE_AREA_INSET_PT = 17.0`), `SingleSheet8Imposer.kt:29-43`, `LayoutValidator.kt:85` |
| Bench keep-clear cue | `feature/editor/.../BenchStudioSurface.kt:305-309` (`keepClearInsetPx`, hard-coded default inset), `BenchStudio.crossesKeepClear` (all four edges) |
| Proof reach band | `ProofSheet.kt:208` (fixed 9 px, decorative) |
| Spoken reach state | `ElementSemanticsLayer.kt:113-131`, `Copy.A11y.OUTSIDE_PRINT_REACH` (:184) |
| Proof print act / legend | `ProofPrint.kt`, `ProofSheet.kt:301` (`LEGEND_PRINTER_REACH`), `ProofScreen.kt` |
| PDF pipeline | `render-android/.../SheetComposer.kt:52-86` (one-page `PdfDocument`), `app/.../export/ZineExporter.kt` (Save → Downloads, Share → FileProvider) |
| Draw tape | `core/render/.../DrawCommand.kt` (`FillRect`, `DrawShape`, `DrawTextBox`) |
| Local prefs | `data-android/.../di/PreferencesModule.kt` (DataStore) |

## Files/modules likely affected

- A **pure** `TestPageProducer(paper, cells) → List<DrawCommand>` built from existing commands; exact
  geometry in points (50 mm = 141.732 pt). `core:render` cannot see `SingleSheet8Imposer` (it depends on
  model and copy only), so either put the producer where both are visible (`:app` or `render-android`) or pass
  the panel cells in. `SheetComposer.writePdf(sheet, panels = [], overlay)` already supports a non-zine page.
- Part 2 only: a pure per-edge reach function (from the imposition) replacing the single inset in the Bench
  cue, the spoken state and Proof.
- `render-android` / `app`: export the test page through `SheetComposer` and `ZineExporter` with its own file
  name (`zinely-printer-test-A4.pdf`).
- `data-android`: a `PrinterReachStore` (one value).
- `feature/editor`: entry line, answers sheet, reach-aware legend and keep-clear inset.
- `core/copy`: all strings.
- Docs: the ADR; ROADMAP; CHANGELOG; DEVICE-VERIFICATION print pass.

## Data model implications

None in documents. The reach profile is device state (DataStore), excluded from backups; a zine never
carries printer data.

## Testing strategy

- Pure geometry: the ruler is exactly 141.73 pt ± 0.01 on A4 and Letter; bands at 3/5/8 mm from each edge;
  dummy panels match `SingleSheet8Imposer` cells for the same paper (property test across both sizes).
- PDF: page size **equals the zine export's page size** for the same paper (`PdfDocument.PageInfo` takes
  whole points — `SheetComposer.kt:59-60` rounds, so A4 is 595 × 842); a raster golden.
- Mapping: 3/5 mm → no change; 8 mm → part-2 widening on sheet-edge panel edges only (property test over
  all eight pages and both rotations); "none" → default.
- UI: Roborazzi for entry, sheet and legend states; TalkBack on the answers sheet.
- **Physical print pass** ([DEVICE-VERIFICATION §3.2](../DEVICE-VERIFICATION.md)) on three brands (e.g. HP,
  Brother, Canon/Epson) and both paper sizes: ruler measured with a real ruler, band visibility recorded,
  dummy folded. This is the acceptance evidence, not an optional extra.

## Accessibility considerations

- The test page's instructions are printed text, readable without colour; bands are labelled with numbers,
  not only tints.
- Answer controls are single-select groups with spoken labels ("5 millimetres").
- A maker who cannot measure (low vision) can skip; nothing depends on answering.
- The reach state stays spoken on elements (`outsidePrintReachState`) with the tuned inset.

## Design-system implications

Existing Proof tokens; the test page uses print inks only (black on paper) so any printer shows it. No new
colour role; the reach band keeps `berry` ("the printer's-reach guide", ZINE-DIRECTION).

## Acceptance criteria

1. ADR amending ADR-039 accepted; `v21-proof.html` amendment reviewed and frozen.
2. The test PDF measures 50.0 mm ± 0.5 mm on three printer brands at 100 %, A4 and Letter.
3. Part 1: answers change only guidance. Part 2 (if approved): only the warning on sheet-edge panel edges
   changes; the zine's imposition and PDF are byte-for-byte unchanged by any answer (golden proves it).
4. Skipping leaves today's behaviour exactly.
5. No in-app print, no permission, no network.
6. Pixel parity, both device passes, independent review.

## Out of scope

PrintManager / in-app printing (ADR-052); duplex and flip-direction tests (a two-sided format, later);
multiple printer profiles; copy-shop card; changing the imposition inset itself.

## Future extension

The back-side arrow for a future two-sided 16-page format (manual duplex); a copy-shop "print this for me"
card from the same producer; "print one, check, then print ten" guidance at the right moment.
