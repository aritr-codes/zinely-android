# Public-beta stabilization audit — 2026-08-25

Reference rule: [RESEARCH.md R12.2-R12.3](../RESEARCH.md#r122-zinelys-public-beta-bar--recommendation).
This is an evidence checkpoint, not a declaration that Zinely is public-beta ready.

| Baseline | State | Evidence and remaining work |
|---|---|---|
| Project lifecycle | **Green** | Files remain authoritative, Room remains rebuildable, editor persistence is single-writer, and whole-library backup/restore has passed JVM, Android repository, SAF, invalid-input, collision, shared-asset, and real-device transaction checks. |
| Creative loop | **Yellow** | Create/open/edit, text, photos, the frozen sixteen Art supplies, manipulation, undo/redo, Proof and export are present. Recent D-089, D-083/D-103 and D-079 fixes have focused regression/device evidence. The Art drawer's first cold opening still produces a visible frame-cost spike and needs a separate measured follow-up. |
| Print confidence | **Green** | `PdfSurfaceParityInstrumentedTest` passed 5/5 on SM-A176B / Android 16 on 2026-08-25, closing the unexecuted vector/parity hole. The owner completed physical print and fold checks on 2026-08-25 with no blocking issue reported. Beta-cohort photocopier feedback remains useful ongoing evidence, not a baseline blocker. |
| Accessibility | **Green** | Focused semantic, large-text and platform-tree coverage exists and D-083/D-103 are closed. The owner completed the first-person TalkBack listen pass on 2026-08-25 with no blocking issue reported. |
| Responsiveness | **Yellow** | The Art drawer was the concrete interaction-critical hotspot found in this pass. Before caching canonical supply paths, an isolated settled Add-to-Art transition rendered 32 frames with 8 janky (25%), median 36 ms. After the cache, two settled warm transitions each rendered 18 frames with 2 janky (11.11%), medians 8 ms and 7 ms. A cold post-install opening remained expensive, so this optimization is useful but not the end of the performance work. |

## Renderer change in this checkpoint

`SupplyPainter` and `CanvasReplayer` now keep a renderer-instance identity cache of Android `Path` objects for
canonical immutable `SupplyOutline` values. This removes repeated outline-to-path construction during redraws
without adding a global cache, changing the pure render model, or changing raster/PDF semantics. Focused unit
coverage proves same-instance reuse and isolation for distinct outline objects; the renderer conformance tests and
the five-test hardware PDF parity suite protect output behavior.

## Gate before a larger Art library

The shipped frozen set of sixteen is complete. A larger searchable material library remains behind ADR-107 R5 and
D-080: the HTML Art sheet must be amended and ruled before new set membership, chips, or search enter production.
That owner/design act should also address the visual-composition gap observed against `37596.jpg`: the exact source
swatches are present, but large pale surfaces plus sparse Camaron/Matcha usage make the application feel calmer and
less vibrant than the reference. Do not solve that by silently changing frozen tokens or by saturating the printed
artifact; prototype stronger chrome color proportions and validate contrast first.
