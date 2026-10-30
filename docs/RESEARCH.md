# Zinely — Research & Evidence Base

> The cited evidence behind Zinely's product and technical decisions.
> Other documents **reference** this file (by section) instead of restating sources.
> Decisions derived from this evidence live in [DECISIONS.md](DECISIONS.md). Technical design lives in [ARCHITECTURE.md](ARCHITECTURE.md).

**Label legend**

| Label | Meaning |
|---|---|
| ✅ **VERIFIED** | Confirmed by ≥1 authoritative source (cited) |
| 🟦 **RECOMMENDATION** | Our reasoned call given the evidence |
| 🟨 **ASSUMPTION** | Inference not directly sourced; revisit if it matters |
| 🔭 **FUTURE** | Out of current scope; explore later |
| ⚠️ **DISPUTED** | Sources disagree; we picked one convention |

Research date: 2026-06-19. Method note: web research via search against first-party docs (developer.android.com, skia.org, protobuf.dev, sqlite.org, Adobe/iText). Two findings carry an explicit caveat where only secondary sources were quotable.

---

## R1. Imposition geometry — single-sheet 8-page mini-zine

The most correctness-critical research. Reconciled from a NASA/Chandra numbered diagram, a Cambridge museum fold guide, and university library zine guides.

### R1.1 Sheet & grid — ✅ VERIFIED
Flat sheet laid **LANDSCAPE**, divided into **2 rows × 4 columns = 8 panels**. Indexing: `row 0 = top`, `row 1 = bottom`; `col 0 = left` … `col 3 = right`. Each panel = `sheetWidth/4 × sheetHeight/2`.

### R1.2 Page → cell mapping (the oracle) — ✅ VERIFIED
Booklet page 1 = front cover, 2–7 = interior in reading order, 8 = back cover.

| Booklet page | Grid cell (row, col) | Rotation |
|---|---|---|
| 1 (front cover) | (1, 3) | 0° |
| 2 | (0, 3) | 180° |
| 3 | (0, 2) | 180° |
| 4 | (0, 1) | 180° |
| 5 | (0, 0) | 180° |
| 6 | (1, 0) | 0° |
| 7 | (1, 1) | 0° |
| 8 (back cover) | (1, 2) | 0° |

Flat-sheet view (number = booklet page; ↑ upright, ↓ rotated 180°):

```
        col0   col1   col2        col3
row0 │   5↓     4↓     3↓          2↓     │  ← TOP row printed UPSIDE-DOWN
row1 │   6↑     7↑     8(back)↑    1(front)↑ │  ← BOTTOM row printed UPRIGHT
```

**Engine rule:** pages **2–5 → 180°**; pages **1, 6, 7, 8 → 0°**. Front cover anchors at **(1,3)**, back cover at **(1,2)**.

### R1.3 Which row rotates & why — ✅ VERIFIED
The **TOP row (row 0)** is printed **rotated 180°**. Fold-topology reason: the final long-direction center fold brings the top half *down over* the bottom half, flipping any content on the top half; pre-rotating it 180° makes it land upright. The bottom half never flips.

### R1.4 Cut line — ✅ VERIFIED
A **single straight cut** along the **central horizontal fold** (between row 0 and row 1), spanning the **middle two columns only** (col1–col2). In sheet coordinates (origin top-left, width `W`, height `H`): the segment from `(W/4, H/2)` to `(3W/4, H/2)`. It does **not** reach the outer edges. This slit lets the folded sheet open into the booklet.

### R1.5 Fold lines — ✅ VERIFIED (derived)
- Horizontal center fold: `y = H/2`, full width.
- Vertical folds: `x = W/4`, `x = W/2`, `x = 3W/4`.

### R1.6 Letter vs A4 — ✅ VERIFIED (topology) / 🟨 ASSUMPTION (arithmetic)
Topology, rotation, and cut are **identical**. Only panel dimensions differ:

| Sheet | Landscape (W×H) | Panel (W×H) |
|---|---|---|
| US Letter | 11 × 8.5 in | 2.75 × 4.25 in |
| A4 | 297 × 210 mm | 74.25 × 105 mm |

Engine must treat panel size as **derived**, not hard-coded.

### R1.7 Variants & pitfalls — ⚠️ DISPUTED / 🟨 ASSUMPTION
- ⚠️ **Which row rotates** — some templates mirror vertically (cover row on top, bottom row rotated). Both fold validly but are **not cell-interchangeable**. **Canonical: top row rotated, cover at (1,3).** Mixing conventions is the #1 cause of a scrambled zine. Engine should expose a single config flag to flip convention but default to canonical.
- Common mistakes: rotating the wrong row; cutting full-width (sheet falls apart); cutting the vertical fold instead of horizontal; folding print-side in; reversing column order within a row.

**Sources:** [Chandra/NASA — Putting together your zine (PDF)](https://www.chandra.harvard.edu/make/images/Zine_folding.pdf) · [Cambridge Sedgwick Museum — How to fold a Zine (PDF)](https://www.museums.cam.ac.uk/sites/default/files/how%20to%20fold%20a%20zine.pdf) · [Whitworth University Library](https://libguides.whitworth.edu/c.php?g=1144938&p=8357213) · [U. Puget Sound Collins Library](https://library.pugetsound.edu/zines/making) · [Lone Star College](https://nhresearch.lonestar.edu/Zines/Folding) · [ICA Boston](https://www.icaboston.org/articles/make-your-own-mini-zine/) · [Ex Why Zed](https://exwhyzed.com/how-to-fold-a-zine/)

---

## R2. On-device PDF & print correctness (Android)

### R2.1 Bottom line — 🟦 RECOMMENDATION
For a single-sheet zine at home-print quality, stay 100% on the platform SDK — **no third-party PDF library, no cloud**.

### R2.2 `android.graphics.pdf.PdfDocument` — ✅ VERIFIED
- Draws multi-page PDFs onto a `Canvas`; page size set in **points (1/72")** via `PageInfo.Builder(w,h,pageNo)`; optional content rect.
- **Text via `Canvas.drawText` becomes true vector, selectable text with embedded *subset* fonts** — the backend is Skia's PDF device (`SkPDFDevice`). So we get crisp, searchable, screen-reader-able text **without** a third-party lib. *(Bundle our own licensed TTF so output is reproducible across devices.)*
- **Limits:** only one page box (effective MediaBox) + content rect — **no TrimBox/BleedBox/CropBox**. **sRGB/DeviceRGB only — no CMYK, spot, Lab, or ICC/PDF-X output intents.** Per-page PDF format ceiling 14,400 pt (200") — irrelevant for a single sheet.
- **Conclusion:** adequate and high-quality for home print (sRGB, margin-safe). The wall is **color (CMYK/ICC) + page boxes** — exactly the home-print ↔ commercial-prepress boundary.

**Sources:** [PdfDocument](https://developer.android.com/reference/android/graphics/pdf/PdfDocument) · [PageInfo.Builder](https://developer.android.com/reference/android/graphics/pdf/PdfDocument.PageInfo.Builder) · [Skia PDF backend](https://skia.org/docs/dev/design/pdftheory/) · [PDF page boxes](https://www.prepressure.com/pdf/basics/page-boxes) · [PDF/X-4](https://www.prepressure.com/pdf/basics/pdfx-4)

### R2.3 System print framework — 🟦 RECOMMENDATION
Write the PDF **directly** with `PdfDocument` for file export/share (MVP path). Use `PrintManager` + `PrintDocumentAdapter` (+ `PrintedPdfDocument`) only for an in-app "Print" button later — it builds `PageInfo` from user `PrintAttributes` (media size, margins, resolution). The PDF surface stays in points regardless of `Resolution`. **Sources:** [Printing custom documents](https://developer.android.com/training/printing/custom-docs) · [PrintAttributes.Resolution](https://developer.android.com/reference/android/print/PrintAttributes.Resolution)

### R2.4 "Fit to page" silently breaks imposition — ✅ VERIFIED
Consumer viewers/drivers default to *Fit*/*Shrink oversized pages*, rescaling fixed geometry and shifting fold/cut lines. Defenses: **export at exact paper size**; instruct **"Actual size / 100%, Fit-to-page OFF"**; print an **on-sheet calibration ruler** (1 in / 50 mm) for scale verification; keep geometry inside a safe inset; offer A4 and Letter as **distinct** exports (cross-size "fit" is the worst offender). **Sources:** [Adobe: page size/scaling](https://helpx.adobe.com/acrobat/desktop/print-documents/set-up-and-print-pdfs/page-size.html) · [Adobe: scale/resize printed pages](https://helpx.adobe.com/acrobat/kb/scale-or-resize-printed-pages.html)

### R2.5 Non-printable margins / full-bleed — ✅ VERIFIED + 🟦 RECOMMENDATION
Consumer printers can't print to the edge (typical unprintable border ≈ 6 mm / 0.25"; laser ≈ 4 mm). True full-bleed is inkjet-photo-only and works by oversizing artwork (itself a rescale). **MVP: design margin-safe with a safe-area inset ≈ 6 mm / 0.25"; keep all content, fold lines, cut marks inside it; accept a white border.** Bleed mode is 🔭 FUTURE (pairs only with a commercial export path). **Sources:** [HP borderless](https://h30434.www3.hp.com/t5/LaserJet-Printing/Borderless-printing/td-p/6151769) · [OnlineLabels non-printable margin](https://www.onlinelabels.com/support/faq/306)

### R2.6 Raster export at 300 DPI & memory — ✅ VERIFIED
- US Letter = **2550 × 3300 px**; A4 = **2480 × 3508 px** at 300 DPI.
- ARGB_8888 = 4 B/px → Letter ≈ **33.7 MB**, A4 ≈ **34.8 MB** per full-sheet bitmap. One sheet is fine; OOM comes from holding several at once.
- **OOM avoidance:** decode bounds first (`inJustDecodeBounds`), compute `inSampleSize`, decode to target; downsample user images to placement size; **normalize EXIF orientation** (`ExifInterface` + `Matrix`); stream `bitmap.compress()` straight to `OutputStream`; `recycle()` immediately; one sheet at a time, off-main-thread. PNG for line-art, JPG for photo-heavy. `RGB_565` (2 B/px ≈ 17 MB) only when no alpha/gradients.

**Sources:** [Loading large bitmaps efficiently](https://developer.android.com/topic/performance/graphics/load-bitmap) · [Manage your app's memory](https://developer.android.com/topic/performance/memory) · [paper sizes in pixels](https://feettopixels.org/paper-sizes-in-pixels)

### R2.7 Third-party PDF libs — 🔭 FUTURE
Not for MVP. Only justified for commercial prepress (CMYK/spot/ICC, Trim/Bleed, crop marks, PDF/X). Licensing: **iText 7 is AGPL or paid** — AGPL is viral for a distributed app. Permissive options **PdfBox-Android (Apache 2.0)** and **OpenPDF (LGPL/MPL)** exist but **neither does real CMYK/spot**. Genuine prepress realistically belongs in a server/desktop step — which conflicts with offline-first. **Sources:** [iText AGPLv3](https://itextpdf.com/how-buy/AGPLv3-license) · [PdfBox-Android color #485](https://github.com/TomRoush/PdfBox-Android/issues/485) · [OpenPDF](https://github.com/dmavrodiev/OpenPDF)

---

## R3. Comparable products

### R3.1 Identity corrections — ✅ VERIFIED
"Dirty Little Zine" is a **browser tool, not a native app**. No active standalone desktop "ZineMaker" verified; the real dedicated desktop tool is **Electric Zine Maker**.

### R3.2 Landscape summary
| Product | Platform | Offline | Account | Gap we exploit |
|---|---|---|---|---|
| [Dirty Little Zine](https://dirtylittlezine.com/) | Web | client-side | none | Web-only, no native gallery/gestures, fixed 8-page, single zine |
| [Electric Zine Maker](https://alienmelon.itch.io/electric-zine-maker) | Win/macOS | full | none | **No mobile at all** |
| [Scribus](https://www.scribus.net/) | Desktop | full | none | Steep/dated UI; overkill for casual; desktop-only |
| [Zeenster](https://zeenster.com/) | Web | offline-capable | none | Web-only, **no backup** (cache-clear deletes), no drawing |
| [Canva](https://www.canva.com/help/set-up-offline-access/) | incl. Android | **edit-only**, can't create offline | **required** | Hard account wall; cloud-first |
| [Adobe Express](https://helpx.adobe.com/express/web/adobe-express-subscription/free.html) | incl. Android | **none** | required | No offline at all |
| [Affinity](https://www.canva.com/newsroom/news/all-new-affinity/) | Mac/Win | after activation | now Canva acct | **No Android/phone** |
| [Phonto](https://play.google.com/store/apps/details?id=com.youthhr.phonto) | Android/iOS | full | none | Single-image; **no multi-page/zine layout** |
| [Mixbook](https://www.mixbook.com/photo-books) | Web/iOS | cloud-only | required | **No offline, no PDF/file export, total lock-in** |
| [Book Creator](https://apps.apple.com/us/app/book-creator-for-ipad/id442378070) | iPad/Web | iPad offline | iPad none | **No Android app** |

### R3.3 Synthesis — ✅ VERIFIED gap / 🟦 RECOMMENDATION
- **Copy:** fixed fold-and-cut imposition + fold/cut guides + 300 DPI A4/Letter export (DLZ, Zeenster, Scribus); frictionless no-account fully-offline entry (Phonto, Book Creator iPad); tap-to-place text + large bundled fonts + custom `.ttf` (Phonto); drag-drop templates (Comic Life, Canva); on-device drawing alongside photo import (Electric Zine Maker).
- **Avoid:** mandatory account walls; cloud upload of user images; no data portability; watermarks/ads; subscription creep.
- **WHERE WE WIN (the wedge):** **offline-first + account-free/local-only + native Android phone + multi-page zine layout** is occupied by *no surveyed product*. Dedicated zine tools are web/desktop-only; offline+no-account products are single-image or iPad-bound; layout-capable products are cloud/account-gated or have no Android.

---

## R4. Offline-first storage patterns

### R4.1 Architectures compared — ✅ VERIFIED
- **(a) Room-tree** (row per page/element/transform): strong SQL query-ability + cheap partial updates; but graph reconstruction, write fan-out, and per-shape SQL migrations.
- **(b) Room-metadata + serialized doc blob** (kotlinx.serialization JSON or Protobuf): whole-doc atomic save, 1:1 in-memory↔on-disk model that maps cleanly to Compose state + undo snapshots + single-file export; weak query-into-content + whole-file rewrite per save.

Official guidance mandates a single-source-of-truth + repository boundary, not a relational tree. **Source:** [Build an offline-first app](https://developer.android.com/topic/architecture/data-layer/offline-first) · [Data layer](https://developer.android.com/topic/architecture/data-layer)

### R4.2 Recommendation — 🟦 RECOMMENDATION
**Architecture (b): Room metadata table + a versioned serialized document blob per zine; images copied-in; SAF for backup.** JSON for MVP (human-readable, easy to debug, schema-version via optional/defaulted fields + `ignoreUnknownKeys`); **Protobuf** is the 🔭 FUTURE option if write-amplification/blob size hurts. Reconsider toward (a)/hybrid only if cross-document content search becomes core. **Sources:** [Room migrations](https://developer.android.com/training/data-storage/room/migrating-db-versions) · [kotlinx.serialization](https://github.com/Kotlin/kotlinx.serialization/blob/master/docs/basic-serialization.md) · [proto3 evolution](https://protobuf.dev/programming-guides/proto3/)

### R4.3 Crash safety — ✅ VERIFIED
Debounced autosave → **write-temp-then-atomic-rename** (the DataStore pattern) so a crash never corrupts the last good file; Room WAL covers the metadata table; offer "restore unsaved changes" from a working/journal file on relaunch. **Sources:** [DataStore](https://developer.android.com/topic/libraries/architecture/datastore) · [SQLite WAL](https://sqlite.org/wal.html)

### R4.4 Image import — ✅ VERIFIED + 🟦 RECOMMENDATION
**Copy-in** to app-specific storage (no permission, survives source delete/move, self-contained export) beats URI-reference (fragile, lost on uninstall). Select via **Photo Picker** (no permission). **Sources:** [App-specific files](https://developer.android.com/training/data-storage/app-specific) · [Photo picker](https://developer.android.com/training/data-storage/shared/photo-picker)

### R4.5 SAF backup & scoped storage — ✅ VERIFIED
`ACTION_CREATE_DOCUMENT` (export `.zine` bundle), `ACTION_OPEN_DOCUMENT` (import/restore, `takePersistableUriPermission`). App-specific storage needs no permission across all API levels; Photo Picker/SAF hand one file with no storage permission. Bundle = zip of `document.json` + `/images/*`; copy contents in on import so restored zines are self-contained. **Sources:** [SAF](https://developer.android.com/guide/topics/providers/document-provider) · [Photo picker](https://developer.android.com/training/data-storage/shared/photo-picker)

---

## R5. Canvas / scene-graph editor architecture

### R5.1 Undo/redo — ✅ VERIFIED
Real editors converge on a **hybrid**: command objects whose `undo()` carries a small **memento of only the touched fields**. tldraw captures store-mutation diffs flushed at marks; Excalidraw stores per-entry deltas; Figma stores per-node property changes. Full snapshots grow linearly (bad for big docs); pure inverse-commands have a hand-written-inverse bug surface — the memento hybrid avoids both. **Sources:** [Command pattern](https://gameprogrammingpatterns.com/command.html) · [Memento](https://refactoring.guru/design-patterns/memento) · [tldraw History](https://tldraw.dev/sdk-features/history) · [Excalidraw history.ts](https://github.com/excalidraw/excalidraw/blob/master/packages/excalidraw/history.ts)

### R5.2 MVI for the editor — ✅ VERIFIED
`Intent → pure reducer → new State` exposed as `StateFlow` aligns with Compose "state down, events up." Keep the reducer pure/synchronous; route I/O (autosave, image decode) to a side-effect channel. **Sources:** [UI layer/UDF](https://developer.android.com/topic/architecture/ui-layer) · [State and Compose](https://developer.android.com/develop/ui/compose/state)

### R5.3 Coalescing a drag into ONE undo step — ✅ VERIFIED
**begin/update/commit** transaction: `onDragStart` opens a txn + snapshots initial transform; `onDrag` mutates **preview** state only (not recorded); `onDragEnd` builds **one** `TransformCommand(ids, before, after)` and dispatches it; `onDragCancel` restores. Drive the live drag through a `Modifier.graphicsLayer { }` **lambda** reading a `mutableStateOf` so per-frame updates hit only the draw phase — the reducer never sees frame spam. **Sources:** [drag gestures](https://developer.android.com/develop/ui/compose/touch-input/pointer-input/drag-swipe-fling) · [graphics modifiers](https://developer.android.com/develop/ui/compose/graphics/draw/modifiers) · [Compose phases](https://developer.android.com/develop/ui/compose/phases)

### R5.4 Scene model, hit-testing, snapping — ✅ VERIFIED / 🟨 ASSUMPTION
- **Flat ordered `List<Element>` keyed by stable id**, list index = z-order; optional `parentId` makes grouping additive later. Each element carries decomposed translation/scale/rotation composed to a `Matrix` at draw/hit-test.
- **Hit-test:** inverse-transform the touch point into each element's local space, AABB-test local bounds, iterate front-to-back, first hit wins.
- **Snapping:** candidate lines from edges+centers of other elements + page edges/center; snap within a zoom-adjusted threshold (`~8px/zoom`); applied to the **preview** during update, baked into the commit command; guides are render-only, never in history. **Sources:** [Penpot data model](https://help.penpot.app/technical-guide/developer/data-model/) · [Compose multitouch](https://developer.android.com/develop/ui/compose/touch-input/pointer-input/multi-touch)

### R5.5 Recommendation — 🟦 RECOMMENDATION
Single immutable `EditorState` in an MVI reducer; **command objects with field-level mementos** as the undo unit; gesture coalescing via begin/update/commit; live transforms through `graphicsLayer{}` lambda; snapping & hit-testing as pure, testable functions outside history. Flat element list + optional `parentId`. 🔭 FUTURE: persist command history, groups/frames, collaboration (undo rebase).

---

## R6. Local autosave durability & crash safety — ✅ VERIFIED + 🟦 RECOMMENDATION
*Evidence base for [ADR-021](DECISIONS.md#adr-021). Research date 2026-06-19.*

### R6.1 Android lifecycle & kill realities — ✅ VERIFIED
`onStop()` is the last reliably-delivered callback for "save things the user is editing"; the docs explicitly direct saving permanent data there because the activity may be killed at any time after it returns. An abrupt low-memory kill terminates the process with **no further callback** — `onDestroy()`/ViewModel teardown are skipped. ⇒ all durability must be on disk before `onStop()` returns; nothing later can be relied on. **Sources:** [Activity lifecycle](https://developer.android.com/guide/components/activities/activity-lifecycle) · [Process lifecycle](https://developer.android.com/guide/components/activities/process-lifecycle).

### R6.2 Atomic-write pattern — ✅ VERIFIED
Crash-safe file replace = write a temp file → `fsync` the temp file → atomic `rename()` over the target → **`fsync` the containing directory**. `rename` is atomic on one filesystem, but without the temp-file fsync (data) and the directory fsync (the rename itself) a crash/power-loss can resurrect the old file or an empty one. Java has no portable directory-fsync, so the directory step is best-effort on Android — keep a prior-good `.bak` and validate on open as the backstop. **Sources:** [evanjones.ca — durability](https://www.evanjones.ca/durability-filesystem.html) · [crash-consistency: fsync/rename](https://0xkiire.com/crash-consistency-fsync-rename/).

### R6.3 Op-log vs full-save; SQLite WAL — ✅ VERIFIED
SQLite in WAL mode makes committed transactions durable across **application** crashes regardless of `synchronous`; only power loss can roll back to the last checkpoint under `synchronous=NORMAL`. A write-ahead op-log bounds loss to the last committed operation but adds synchronous-per-edit I/O and replay/recovery code. Local-first editors (Excalidraw, tldraw) just debounce a full serialized-JSON snapshot — no op-log. **Sources:** [SQLite PRAGMA](https://sqlite.org/pragma.html) · [SQLite forum: durability](https://sqlite.org/forum/info/9d6f13e346231916) · [tldraw persistence](https://tldraw.dev/docs/persistence).

### R6.4 Recommendation — 🟦 RECOMMENDATION
For a small in-memory JSON tree with in-memory undo: **debounced atomic full-save (≈1 s debounce, ≈5 s max-latency cap) + a synchronous `onStop` flush + single-writer actor + prior-good `.bak`; no op-log for MVP.** Honest loss bound: *since the last completed save* (≈0 on clean exit). Add an op-log only if "zero-loss"/power-loss durability becomes a hard requirement.

## R7. Content-addressed asset ownership & garbage collection — ✅ VERIFIED + 🟦 RECOMMENDATION
*Evidence base for [ADR-022](DECISIONS.md#adr-022). Research date 2026-06-19.*

### R7.1 Production content-addressed stores converge on reachability + grace — ✅ VERIFIED
git (`git gc` prunes only objects unreachable **and** older than `gc.pruneExpire`, default 2 weeks, to avoid deleting about-to-be-referenced objects), Nix (mark from GC roots, move dead paths to trash then empty), IPFS (sweep all but **pinned** roots), Docker/OCI (SHA-256 layers shared across images; prune only unreferenced blobs), and Jackrabbit Oak (**mark across all repositories** sharing a datastore before sweeping) all use mark-and-sweep from declared roots plus a grace period — not naive refcounts. **Sources:** [git-gc](https://git-scm.com/docs/git-gc) · [Nix Pills GC](https://nixos.org/guides/nix-pills/11-garbage-collector.html) · [IPFS persistence/pinning](https://docs.ipfs.tech/concepts/persistence/) · [Oak BlobStore](https://jackrabbit.apache.org/oak/docs/plugins/blobstore.html).

### R7.2 Refcount vs mark-and-sweep — ✅ VERIFIED
Reference counting reclaims immediately but a single missed increment/decrement (crash mid-update, lost edge) silently corrupts the count and can delete live bytes; mark-and-sweep recomputes liveness from authoritative roots each run and is self-correcting. For an app where the document JSON is the source of truth and projects are few, a periodic sweep is cheap and robust; asset→asset cycles don't exist. **Sources:** [GC vs ARC](https://medium.com/computed-comparisons/garbage-collection-vs-automatic-reference-counting-a420bd4c7c81) · [Cornell CS6120 — unified GC](https://www.cs.cornell.edu/courses/cs6120/2020fa/blog/unified-theory-gc).

### R7.3 Tombstones / grace windows prevent resurrection — ✅ VERIFIED
Cassandra/ScyllaDB defer the destructive step with a tombstone kept for `gc_grace_seconds` to avoid data resurrection — exactly the undo-safety property needed (a "removed" image must keep bytes while undo can restore it). **Sources:** [ScyllaDB tombstones](https://www.scylladb.com/2022/06/30/preventing-data-resurrection-with-repair-based-tombstone-garbage-collection/) · [Cassandra tombstones](https://cassandra.apache.org/doc/latest/cassandra/managing/operating/compaction/tombstones.html).

### R7.4 Recommendation — 🟦 RECOMMENDATION
Global `assets/<sha256>` store; **mark-and-sweep liveness from (all project documents) ∪ (live undo stacks)**, deferred via WorkManager, deleting only orphans older than a ≥24 h grace window; Room index is a cache, documents are the source of truth; `.zine` export = copy-projection of the project's blobs; import dedup-merges by hash. Refcount only ever as a fast-path hint, never the authority.

## R8. Imported image fidelity & storage — ✅ VERIFIED + 🟦 RECOMMENDATION
*Evidence base for [ADR-023](DECISIONS.md#adr-023). Research date 2026-06-19.*

### R8.1 Pixel math at 300 DPI — ✅ VERIFIED
`px = inches × 300`. Full-bleed full sheet: Letter 2550×3300 (8.4 MP), A4 2480×3508 (8.7 MP); one of 8 panels ≈ 638×1650 (Letter) / 620×1754 (A4). Largest longest-edge a single photo needs at print scale = **3508 px** (A4). A full-sheet ARGB_8888 bitmap ≈ 34 MB. 300 PPI is the print-quality target; **240 PPI** is widely accepted as visually indistinguishable, giving crop headroom. **Sources:** [Printivity — 300 PPI](https://www.printivity.com/insights/what-resolution-should-i-use-for-printing-answer-300-ppi) · [breathingcolor — DPI/PPI](https://www.breathingcolor.com/blogs/news/dpi-ppi-guide-to-printing-resolution).

### R8.2 Proxy-and-master + Android decode realities — ✅ VERIFIED
Photo/video editing edits a small proxy and re-applies to a high-res master at export; Canva caps the working surface rather than editing raw upload pixels. Android large-bitmap decoding needs `inJustDecodeBounds`→`inSampleSize`→re-decode (a 50 MP full decode ≈ 200 MB ⇒ OOM), `BitmapRegionDecoder` for crops, and one-time **EXIF orientation** normalisation; Coil downsamples to the target size by default. **Sources:** [Android — Loading Large Bitmaps](https://developer.android.com/topic/performance/graphics/load-bitmap) · [Canva size limits](https://www.canva.com/help/resize/) · [Coil changelog](https://coil-kt.github.io/coil/changelog/).

### R8.3 Recommendation — 🟦 RECOMMENDATION
Store one **import master at 4096 px longest edge** (JPEG q≈90 photos, lossless for alpha/graphics), EXIF-normalised; discard the camera original. 4096 clears the 3508 px A4 worst case with margin and crop headroom (50 % linear crop → 2048 px ≈ a full panel ≥240 PPI). Derive edit/preview via Coil downsample; export by sampling the master at the target pixel box. ~5 MB/photo vs 15–25 MB originals.

## R9. Editor reference — `zine-maker` cross-product study — ✅ VERIFIED + 🟦 RECOMMENDATION
*Distilled 2026-06-25 from a code read of [virgilvox/zine-maker](https://github.com/virgilvox/zine-maker) (a web zine maker) as a reference for the S4 editor + imposition. Feeds the S4 editor MVI spike and extends [R3](#r3-comparable-products) / [R5](#r5-canvas--scene-graph-editor-architecture). We will **not** copy code — ideas only.*

### R9.1 Stack & license — ✅ VERIFIED
Vue 3 + Vite + **Pinia** (store) + **Konva.js** (retained-mode canvas) + **jsPDF**, TypeScript, fully client-side. **License: MIT** (© 2026 Moheeb Zara) — permissive; legally reusable but architecturally irrelevant (Vue/Konva/jsPDF don't map to Kotlin/Compose/Android `Canvas`). Treat as **ideas-only**; no code lifted. **Sources:** [repo](https://github.com/virgilvox/zine-maker) · [LICENSE](https://raw.githubusercontent.com/virgilvox/zine-maker/main/LICENSE) · [types/index.ts](https://raw.githubusercontent.com/virgilvox/zine-maker/main/src/types/index.ts) · [stores/project.ts](https://raw.githubusercontent.com/virgilvox/zine-maker/main/src/stores/project.ts) · [composables/useZineExport.ts](https://raw.githubusercontent.com/virgilvox/zine-maker/main/src/composables/useZineExport.ts).

### R9.2 Document/element model & the transform-commit pattern — ✅ VERIFIED
Flat tree `Project → pages[] → content[]`; each `content` item is a **uniform struct** `{ id, type, x, y, width, height, rotation, zIndex, properties, visible, locked, groupId, name }` where `type ∈ text|image|shape|drawing` and `properties` is a **discriminated union** per type. Coordinates are **points @ 72 DPI** (Letter sheet = 792×612, half = 396) — independent ✅ confirmation of our points-based model. **The key insight:** geometry is stored as **resolved absolute fields, never a live scale factor** — Konva's Transformer applies `scaleX/scaleY` live during a gesture, and on `transformEnd` the editor **bakes scale into width/height** (`width = width * scaleX; node.scaleX(1)`) and writes back `{x,y,w,h,rotation}`. Drag commits `{x,y}`; resize/rotate commits `{x,y,w,h,rotation}`.

### R9.3 Layering, undo, imposition — ✅ VERIFIED
- **Layering** = integer `zIndex` per item; the store re-sorts `content` by `zIndex` after each mutation; a `reorder(order: id[])` action drives the layers panel.
- **Undo/redo** = command-pattern history with explicit `{undo, redo}` closures **plus batching** (`beginBatch/endBatch` coalesces a multi-item drag into one entry) — but it snapshots via `JSON.parse(JSON.stringify())` **per edit** (memory-heavy).
- **Imposition is data-driven**: each template carries `PrintLayout { sheetWidth, sheetHeight, pagePositions: PagePosition[] }`, `PagePosition = { pageNumber, x, y, width, height, rotation, isFlipped, side:'front'|'back' }`. Page-ordering is per-format math, e.g. **booklet/saddle-stitch** per sheet *i*: `leftFront = N−2i, rightFront = 1+2i, leftBack = 2+2i, rightBack = N−1−2i`; quarter-fold/accordion use static slot tables with alternating 180° rows. Print marks (fold/cut/crop), `bleed` (points), and per-format human **fold instructions** ship with each template; PDF = `stage.toDataURL({pixelRatio})` → jsPDF `unit:'in', format:[w/72,h/72]`, one page per sheet.

### R9.4 Recommendation — 🟦 RECOMMENDATION
Adopt the **data shapes** and two patterns, re-expressed in MVI + immutable Kotlin:
- 🟦 **Uniform `Element` (sealed) + per-type `properties` (sealed)** for a homogeneous document tree (matches [R5.4](#r54-scene-model-hit-testing-snapping)).
- 🟦 **Persist resolved geometry; derive `AffineTransform2D` at render time** — the editor folds live gesture deltas (pan/zoom/rotate) into committed `{x,y,w,h,rotationDeg}` on `DragEnd`/`TransformEnd` (their "bake scale into w/h" trick), avoiding stored-matrix drift. Reinforces [R5.3](#r53-coalescing-a-drag-into-one-undo-step).
- 🟦 **Lift their imposition ordering formulas as the spec** for a pure `:core:imposition` function `Template → List<SheetPlacement{pageNumber, rect(pts), rotationDeg, flipped, side}>` and unit-test the booklet/quarter-fold/accordion math. This is the single most directly transferable item; cross-check against our [R1](#r1-imposition-geometry--single-sheet-8-page-mini-zine) oracle.
- ⚠️ **Do differently:** (1) MVI, not imperative Pinia — borrow data shapes, not event flow; (2) immutable `data class .copy()` / field-memento commands for undo, **not** deep-clone-per-edit (perf trap on Android — [R5.1](#r51-undoredo)); (3) Compose/Android `Canvas` is immediate-mode — we must build selection handles + hit-testing + gesture→transform that Konva's Transformer gives for free; (4) ignore its network/upload/RSS features — they violate our offline/privacy invariant.
- 🔭 **PRD/ROADMAP coverage check:** confirm the roadmap states which **fold formats** are MVP vs V1 (quarter/half/tri/booklet/accordion), and that **duplex front/back + flip**, fold/cut/crop marks, bleed, and per-format fold instructions are in scope.

## R10. Editor accessibility — WCAG 2.2 AA + Android/Material 3 guidelines — ✅ VERIFIED + 🟦 RECOMMENDATION
*Distilled 2026-06-25 from an accessibility audit of the S4 editor HTML mockups (`docs/design/editor-mockups/`) + the Compose preview host. The mockups are throwaway, so these are **requirements for the real Compose editor**, not mockup defects. Inputs to the [S4 editor MVI spike](spikes/s4-editor-mvi.md).*

### R10.1 What the mockups already get right — ✅ VERIFIED
**Text contrast (WCAG 1.4.3) is clean** — every M3 token pair computes ≥4.5:1 (secondary text 9.06:1, on-primary 6.27:1, primary links 6.09:1, on-secondary-container 12.35:1). `--touch:48px` is correctly applied to bottom-bar tools, app-bar icon buttons, and primary buttons; FAB 56dp; M3 bottom-sheet drag handle 36×4; `aria-label` on icon-only buttons; `role`/`aria-pressed`/`aria-hidden` used. These map cleanly to Compose `contentDescription`/`Modifier.semantics` and should be preserved. **Sources:** [WCAG 2.2 1.4.3](https://www.w3.org/WAI/WCAG22/Understanding/contrast-minimum.html) · [Material 3 accessibility](https://m3.material.io/foundations/overview).

### R10.2 Requirements the real editor MUST meet — ✅ VERIFIED / 🟦 RECOMMENDATION
Two of these are **AA blockers** invisible in the HTML — they are architectural decisions for the MVI editor, not styling:
- 🟦 **Single-pointer alternative to drag/pinch/rotate (WCAG 2.5.7 Dragging Movements — AA, NEW in 2.2).** All transforms are gesture-only; TalkBack/Switch-Access can't pinch or rotate. The selection contextbar must expose **non-gesture controls** — directional nudge + scale/rotate steppers — wired to the **same** gesture→`AffineTransform2D` intents, plus `Modifier.semantics { customActions = [...] }` per selected element. *Load-bearing for the gesture→transform reducer.* **Sources:** [WCAG 2.2 2.5.7](https://www.w3.org/WAI/WCAG22/Understanding/dragging-movements.html) · [Compose custom actions](https://developer.android.com/develop/ui/compose/accessibility/custom-actions).
- 🟦 **Focus visible (WCAG 2.4.7) + Compose semantics on the page Canvas.** No focus indication exists in the mockups; and `PagePreview`'s `Canvas` is semantically silent (only a `testTag`). The real editor needs visible focus on every clickable, and the page surface must expose semantics (a page description, and a focusable semantic node per placed element as selection lands) or back the canvas with an accessible layers list. **Sources:** [WCAG 2.2 2.4.7](https://www.w3.org/WAI/WCAG22/Understanding/focus-visible.html) · [Compose semantics](https://developer.android.com/develop/ui/compose/accessibility/semantics).
- 🟦 **Target size ≥48dp (Material 3 / Android; WCAG 2.5.8 floor is 24px).** Mockup transform handles (20px), export checkboxes (20px), and the contextbar icon buttons (hardcoded 44px) are too small. Real Compose: handles get a ≥48dp padded hit area; use `IconButton`/`Modifier.minimumInteractiveComponentSize()`; chips keep ~32dp visual but a 48dp hit target. **Sources:** [WCAG 2.2 2.5.8](https://www.w3.org/WAI/WCAG22/Understanding/target-size-minimum.html) · [Compose a11y defaults](https://developer.android.com/develop/ui/compose/accessibility/api-defaults).
- 🟦 **Non-text contrast ≥3:1 (WCAG 1.4.11).** 1px hairline boundaries use surface-variant on surface = **1.25:1** (fail). Use `--md-outline` (4.37:1) for any boundary that conveys structure. Selection/handle strokes at primary (6.09:1) pass — keep that token, and add a contrasting halo for handles over busy photos. **Source:** [WCAG 2.2 1.4.11](https://www.w3.org/WAI/WCAG22/Understanding/non-text-contrast.html).
- 🟦 **Semantics & structure (WCAG 1.3.1).** Project tiles must be real buttons (not `div`s); galleries/layers as collection semantics; one heading per screen; nav/main containers. In Compose: `Modifier.clickable(role=Role.Button)` + `contentDescription`, `semantics { heading() }`, list/collection semantics. **Source:** [WCAG 2.2 1.3.1](https://www.w3.org/WAI/WCAG22/Understanding/info-and-relationships.html).
- 🟦 **Edge-to-edge + WindowInsets (Android 15+).** Mockups fake the status bar; the real app must enable edge-to-edge and pad bars/FAB/sheets with `WindowInsets.systemBars`/`navigationBars` (+ `imePadding` for the text sheet) so the bottom bar/FAB never sit under the gesture-nav handle. **Source:** [Compose insets / edge-to-edge](https://developer.android.com/develop/ui/compose/system/insets).
- 🟦 Minor: contextbar TalkBack traversal order (`isTraversalGroup`/`traversalIndex`) + selection-change announcement; drive all colors from `MaterialTheme.colorScheme` (support dynamic color + dark theme), mapping the dark contextbar to a named M3 role (e.g. `inverseSurface`).

---

## R11. Reading a zine vs. printing one — reader spreads, printer spreads, and the preview triad — ✅ VERIFIED + 🟦 RECOMMENDATION

Gathered 2026-07-21 for the [Beta UX Review](BETA-UX-REVIEW.md), which found that Zinely showed users the
imposed print sheet and never the finished zine. Landed here because the distinction is durable and
outlives that review; [ADR-058](DECISIONS.md#adr-058) cites it.

### R11.1 The print world already separates the two views — ✅ VERIFIED

- ✅ **Reader spreads vs printer spreads is the standing vocabulary**, and the rule is stated as a slogan:
  *"Always design in reader spreads. Always print from printer spreads."*
  **Source:** [PDF Press — how to create a zine](https://pdfpress.app/blog/how-to-create-zine).
- ✅ **InDesign keeps the same split structurally**: you author in reader spreads, and imposition is a
  separate *Print Booklet* step rather than a view of the document.
  **Source:** [Adobe — impose documents for booklet printing](https://helpx.adobe.com/indesign/desktop/print/print-booklets/impose-documents-for-booklet-printing.html).
- 🟦 **Implication for Zinely:** the imposed sheet answers *"what comes out of the printer?"*. It cannot
  also answer *"what did I make?"*, and a product with only the printer spread has no answer to the second
  question at all.

### R11.2 The three-mode triad, and the advisory framing worth stealing — ✅ VERIFIED

- ✅ **Blurb's BookWright separates Edit / Preview / Review**, and Preview's job is defined by what it
  *removes*: *"there are no trim areas visible in Preview mode."* Review then lists pre-print warnings the
  user *"can choose to ignore."*
  **Source:** [Blurb — Preview vs online preview](https://support.blurb.com/hc/en-us/articles/360015822551-What-s-the-difference-between-BookWright-s-Preview-feature-and-my-book-s-online-preview-on-Blurb-s-website).
- 🟦 **Advisory-not-blocking is the transferable idea**: production warnings inform without gating, which
  suits a beginner-first tool that must never make a first zine feel like a compliance exercise.

### R11.3 A page-by-page reading view is absent from comparable zine tools — ✅ VERIFIED

- ✅ **Zeenster (`zine-maker`) has no reading mode**: a canvas editor plus an "Export Zine for Printing"
  modal, with the imposed sheet shown only at the moment of committing to print.
  **Source:** [zine-maker user guide](https://github.com/virgilvox/zine-maker/blob/main/docs/03-user-guide.md).
  (Same product studied in [R9](#r9-editor-reference--zine-maker-cross-product-study----verified--recommendation).)
- ✅ Page-by-page preview is standard in **photo-book** tools and absent from **zine** tools.
- 🟦 **Therefore a swipeable reader is a differentiator rather than catch-up** — the basis for
  [ADR-058](DECISIONS.md#adr-058) treating Read as a feature, not a gap-fill.

### R11.4 Framing vocabulary — 🟦 RECOMMENDATION

- ✅ **Figma's fill/fit wording is the clearest in the field**: *Fill* "may clip the image", *Fit* "ensures
  the entire image is visible"; and Figma must separately reassure users that *"the cropped area does not
  get deleted."*
  **Source:** [Figma — crop and adjust images](https://help.figma.com/hc/en-us/articles/360040028034-Crop-and-adjust-images).
- 🟦 **A verb about the *frame* ("Reframe") reads as safer than one about the *image* ("Crop")** — the
  reassurance Figma has to write is the cost of the other choice. Zinely's *Fill / Whole photo* labels are
  a good plain-English rendering of the same distinction; keep them.

## R12. Public-beta baseline — focused creative tools, not feature breadth — ✅ VERIFIED + 🟦 RECOMMENDATION

### R12.1 Comparable-product baseline — ✅ VERIFIED

- Canva groups its editor around layouts, elements, images, text, page settings, guides, layers, and navigation;
  these establish the manipulation and discoverability expectations of a general-purpose creator. Adobe Express
  similarly allows mobile creation from scratch or personal media and exposes page/canvas sizing. Desygner treats
  camera import, mobile editing, download/share, PDFs, and print as one continuous making flow. Book Creator makes
  simple creation and PDF output central rather than requiring professional layout knowledge.
  **Sources:** [Canva editor help](https://www.canva.com/help/editing-designing/) ·
  [Adobe Express mobile FAQ](https://helpx.adobe.com/express/mobile/get-started/faq.html) ·
  [Adobe Express mobile page sizing](https://helpx.adobe.com/uk/express/mobile/arrange-pages-and-elements/crop-pages.html) ·
  [Desygner mobile app](https://desygner.com/design-app) ·
  [Book Creator](https://bookcreator.com/)
- Print trust is baseline behavior for print-oriented products. Canva's print guidance calls out bleed/safety zones,
  proofing, crop marks, color shifts, and resolution; Desygner describes high-resolution PDF output; Book Creator
  treats PDF print/export as a core publishing path.
  **Sources:** [Canva print troubleshooting](https://www.canva.com/help/fix-canva-print-order/) ·
  [Desygner print](https://desygner.com/print/) ·
  [Book Creator publishing](https://bookcreator.com/features/publishing/)
- Accessibility is also a public product expectation. Book Creator documents WCAG 2.2 AA alignment, assistive-
  technology testing, keyboard operability, clear labels, visible focus, and logical reading order.
  **Source:** [Book Creator accessibility](https://bookcreator.com/accessibility/)

### R12.2 Zinely's public-beta bar — 🟦 RECOMMENDATION

Zinely should not chase Canva's breadth. Its credible public-beta baseline is the smaller promise that a person can
open the app, make a zine, trust the printed result, and safely keep their work:

1. **Trustworthy project lifecycle:** create, open, edit, autosave, close, reopen, recover, back up, and restore
   without corruption or unexplained loss.
2. **Reliable creative loop:** text, local photos, and curated art; predictable select/move/resize/rotate; dependable
   undo/redo; inviting shelf and page entry points.
3. **Print/output confidence:** honest proof/PDF parity, correct imposition and fold order, readable margins, and
   image-quality handling appropriate to the physical output.
4. **Accessible device behavior:** usable large-font layouts, meaningful TalkBack labels and state announcements,
   predictable Back behavior, and understandable error/cancellation states.
5. **Interaction responsiveness:** no obvious lag in dragging, transforming, page switching, opening Art, proofing,
   or export; optimize image decoding only where measurement confirms a real cost.

Cloud accounts, social publishing, collaboration, AI generation, template marketplaces, large online asset feeds,
and desktop-publishing controls are **not** prerequisites. They conflict with the current focused, private,
offline-first product promise or add breadth before the core is sufficiently trustworthy.

### R12.3 Launch-readiness decision rule — 🟦 RECOMMENDATION

Audit each baseline area as **green** (credible for public beta), **yellow** (functional but insufficiently reliable or
clear), or **red** (blocker). Public beta requires no unresolved red item in persistence, editor ownership,
rendering/export, backup/restore, navigation, or accessibility, and no obvious interaction-critical performance
failure on a representative physical device.

## R13. Android launcher identity and launch transition — ✅ VERIFIED + 🟦 RECOMMENDATION

### R13.1 Adaptive icon contract — ✅ VERIFIED

Android adaptive icons separate foreground, background, and optional monochrome layers, then let each launcher apply
its own mask and visual treatment. The foreground must keep its recognition-critical content inside the platform safe
zone; themed icons use the monochrome layer rather than a developer-chosen colour.
**Source:** [Android adaptive icon guidance](https://developer.android.com/develop/ui/compose/system/icon_design_adaptive).

### R13.2 Splash contract — ✅ VERIFIED

Android 12 and newer always provide a system splash. AndroidX `core-splashscreen` carries the same theme-driven
contract back to earlier supported versions: a starting theme supplies background and icon, `installSplashScreen()`
runs before `super.onCreate()`, and `postSplashScreenTheme` returns the Activity to its normal theme. The current
stable artifact is `androidx.core:core-splashscreen:1.2.0`.
**Sources:** [SplashScreen overview](https://developer.android.com/develop/ui/views/launch/splash-screen) ·
[migration guidance](https://developer.android.com/develop/ui/views/launch/splash-screen/migrate) ·
[AndroidX Core releases](https://developer.android.com/jetpack/androidx/releases/core).

### R13.3 Zinely application — 🟦 RECOMMENDATION

Use the supplied `APP_LOGO.png` unchanged for launcher identity, an edge-matched pink background for adaptive crops,
and a simplified Z-only monochrome layer. Use only the system splash transition, with no custom Activity, branding
panel, copy, animation, or keep condition. This preserves Zinely's fast-entry rule: identity appears at the threshold,
then gets out of the maker's way. Frozen in [APP-ENTRY-FREEZE.md](design/APP-ENTRY-FREEZE.md).

## R14. Art cabinet and Ink tray hierarchy — ✅ VERIFIED + 🟦 RECOMMENDATION

### R14.1 Creative libraries combine search, grouping and readable identity — ✅ VERIFIED

Figma's Assets panel combines a search field with library filters and grouped assets, and its component descriptions
are visible in the Assets panel and participate in search. Adobe Express similarly places shape discovery inside the
focused `Elements → Shapes` hierarchy rather than presenting an undifferentiated stock wall.

**Sources:** [Figma — Explore the navigation bar and left sidebar](https://help.figma.com/hc/en-us/articles/360039831974-Explore-the-navigation-bar-and-left-sidebar) ·
[Figma — Add descriptions to styles, components, and variables](https://help.figma.com/hc/en-us/articles/7938814091287-Add-descriptions-to-styles-components-and-variables) ·
[Adobe Express — Add and customize shapes](https://helpx.adobe.com/express/web/add-images-and-visuals/charts-tables-shapes/add-and-customize-shapes.html).

### R14.2 Colour tools expose the active colour and reusable combinations — ✅ VERIFIED

Procreate Pocket keeps the active primary and secondary colours visible in its colour interface; tapping a swatch sets
the current colour, and a default palette remains available for reuse. Adobe Express recommends small colour themes of
two to five frequently paired colours and makes multiple common themes directly reachable.

**Sources:** [Procreate Pocket — Colors interface](https://help.procreate.com/pocket/handbook/colors/colors-interface) ·
[Procreate Pocket — Palettes](https://help.procreate.com/pocket/handbook/4.0/colors/colors-palettes) ·
[Adobe Express — Apply color themes](https://helpx.adobe.com/express/web/add-images-and-visuals/images-and-backgrounds/apply-color-themes.html).

### R14.3 Touch size need not become visual bulk — ✅ VERIFIED

Android recommends at least 48dp interactive targets and explicitly permits a smaller painted icon inside that larger
touch area. Controls can therefore remain visually light while preserving reliable touch and accessibility behavior.

**Source:** [Android Accessibility Help — Touch target size](https://support.google.com/accessibility/android/answer/7101858?hl=en).

### R14.4 Zinely application — 🟦 RECOMMENDATION

For Art, retain one local catalogue with search and the four authored families, but replace the anonymous four-column
glyph wall with named material cards, compact horizontally scrolling family filters, and Recent/Favourites rails.
Family styling must be deterministic; catalogue order and placement remain unrandomised. The favourite action belongs
in the card's label band rather than over the material silhouette.

For Ink, show the current ink by both colour and name, place smaller painted pots inside 48dp choices, and use a stable
five-column grid so ten spot inks never create an orphan row. Palette recipes should remain directly reachable, but
their copy must describe the implemented action: under OD-24 a recipe applies its primary ink, not a complete zine-wide
theme. The interactive specification is Bench amendment A16, owner-approved and DESIGN FROZEN on 2026-08-26
through D-080 and D-107.
## R15. Deterministic emoji output — launch renderer gate (2026-08-26)

**Verified:** Android's Emoji2 documentation distinguishes a bundled font from the default downloadable-font
configuration. The bundled artifact keeps availability offline and consistent, but Google's own guidance records a
material app-size cost. Emoji2 1.6.0 is the current stable line and advertises Emoji 16.0 support. `EmojiCompat`
materialises emoji through spans; Android's `ReplacementSpan` contract draws replacements directly into the target
`Canvas`, so the PDF backend must be measured rather than assumed to preserve vector glyphs.

**Verified:** Google's Noto Emoji repository licenses the fonts under OFL and its SVG artwork under Apache 2.0.
Noto's usage guidance distinguishes the monochrome Noto Emoji font from the colour font and describes the former as
the print-oriented/vector option, while warning that its encoded coverage is only a subset of the complete emoji
set. Coverage of ZWJ sequences therefore cannot be inferred from the project name.

**Measured outcome:** bundled Emoji2 1.6.0 was selected and forced with `replaceAll=true`; system/OEM and
downloadable fallback stay out of document rendering. On the Samsung `SM-A176B`, the fixed launch corpus produced
bundled spans and real raster/PDF ink at 10/24/48 pt. Warm 24-pt raster measured 0.817 ms with emoji versus
0.727 ms plain; font startup measured 301.80 ms off the UI thread and 9,886 KiB PSS; the signed release APK grew
9.42 MiB. See [ADR-112](DECISIONS.md#adr-112) for the accepted bounds and implementation seam.

Sources: [Android Emoji2 guide](https://developer.android.com/develop/ui/views/text-and-emoji/emoji2) ·
[AndroidX Emoji2 releases](https://developer.android.com/jetpack/androidx/releases/emoji2) ·
[ReplacementSpan API](https://developer.android.com/reference/android/text/style/ReplacementSpan) ·
[Noto Emoji repository](https://github.com/googlefonts/noto-emoji) ·
[Noto usage guidance](https://github.com/notofonts/noto-docs/blob/main/docs/website/use.md)

## R16. Public product site and optional fold motion — ✅ VERIFIED + 🟦 RECOMMENDATION

### R16.1 Show the small product directly — ✅ VERIFIED

W3C's carousel guidance requires complete keyboard operation, announced changes, focus care, and a way to pause
automatic movement, while noting that carousel usability is disputed. Google Play requires promotional images to
represent the app accurately. Responsive-image guidance recommends intrinsic dimensions and deferring images below
the initial viewport. A visible, non-rotating gallery of three truthful app stages is therefore a better fit for
Zinely than a carousel or a private-device photo shoot.

**Sources:** [W3C carousel tutorial](https://www.w3.org/WAI/tutorials/carousels/) ·
[Google Play preview-asset guidance](https://support.google.com/googleplay/android-developer/answer/15191715) ·
[web.dev responsive images](https://web.dev/learn/design/responsive-images).

### R16.2 Motion must remain optional and redundant — ✅ VERIFIED

WCAG 2.1 requires keyboard access, reflow, sufficient contrast, and user control of moving content. Its animation
guidance requires motion triggered by interaction to be disableable unless essential. A fold replay may supplement
an instruction, but may not become the only explanation. It should start only from a native control, preserve the
written step and final diagram, announce the result without moving focus, and collapse to a static result when the
user requests reduced motion.

**Sources:** [WCAG 2.1](https://www.w3.org/TR/WCAG21/) ·
[W3C animation from interactions](https://www.w3.org/WAI/WCAG21/Understanding/animation-from-interactions.html).

### R16.3 Public information should be curated, short, and qualified — ✅ VERIFIED + 🟦 RECOMMENDATION

W3C recommends that an accessibility statement identify the commitment or standard, contact path, and known
limitations in simple language, placed somewhere easy to find; it does not require primary navigation. Keep a
Changelog recommends curated, chronological, user-facing entries rather than raw commit logs. GOV.UK interface
writing recommends starting with less, leading with important words, and removing duplication. For Zinely, embed a
concise accessibility statement in About, give growing Changelog and Roadmap material their own pages, and separate
roadmap items into Planned, Exploring, and Not committed without dates or guarantees.

**Sources:** [W3C accessibility statements](https://www.w3.org/WAI/planning/statements/) ·
[Keep a Changelog](https://keepachangelog.com/) ·
[GOV.UK writing for user interfaces](https://www.gov.uk/service-manual/design/writing-for-user-interfaces).

### R16.4 Visual references should inform materiality, not branding — ✅ VERIFIED + 🟦 RECOMMENDATION

NIC Ice Creams and The Belgian Waffle Co. use confident display type, crafted section transitions, playful
category framing, and tactile product presentation. Zinely should borrow that energy without copying food imagery,
logos, commercial density, or repeated promotional content. Its own translation is a professional zine workbench:
paper layers, tape, ink marks, imperfect alignment, and direct interactions around truthful product views.

**Sources:** [NIC Ice Creams](https://www.nicicecreams.com/) ·
[The Belgian Waffle Co.](https://thebelgianwaffle.co/).

### R16.5 Complete folding instructions before optional motion

The website follow-up presents every physical step in a static ordered guide. Photoworks' single-sheet instructions
confirm the critical operation: cut from the folded side, then reopen and refold into the booklet. The diagram and
wording must identify both the folded edge and the stopping point; an animated cue must not suggest a different cut.
W3C technique C39 supports suppressing non-essential interaction animation with `prefers-reduced-motion`.
Zinely hides optional tracing controls in that mode while retaining all diagrams and written steps.

**Sources:** [Photoworks single-sheet zine](https://photoworks.org.uk/learn/how-to/single-sheet-zine/) ·
[W3C reduced-motion technique C39](https://www.w3.org/WAI/WCAG21/Techniques/css/C39.html).

<a id="r17-feedback-and-quality-of-life-review"></a>
## R17. Feedback and quality-of-life review

**Reviewed 2026-09-09.** Provider capabilities are current documentation claims, not an audit of a configured
Zinely form. Product recommendations remain uncommitted in [ROADMAP.md](ROADMAP.md#current-priorities).

### Evidence

- **Verified:** GitHub Pages serves a static site. A real submission box therefore needs an external receiver;
  drawing a form alone does not deliver messages. [GitHub Pages](https://docs.github.com/en/pages/getting-started-with-github-pages/what-is-github-pages).
- **Verified:** W3C recommends short forms asking only for necessary information, visible labels, clear
  instructions and usable feedback. [W3C forms tutorial](https://www.w3.org/WAI/tutorials/forms/).
  **Recommendation:** require only the message in Zinely's initial suggestion form.
- **Verified provider claims:** Tally offers a free tier subject to fair-use conditions and free owner email
  notifications. It documents keyboard/screen-reader support, but explicitly does not guarantee full accessibility
  for every form. Automatic response retention is a Business feature; a free setup needs an owner-operated deletion
  practice. A provider's GDPR statement does not make Zinely's particular workflow compliant automatically.
  [FAQ](https://tally.so/help/faq), [notifications](https://tally.so/help/self-email-notifications),
  [accessibility](https://tally.so/help/accessibility), [settings](https://tally.so/help/form-settings),
  [data responsibility](https://tally.so/help/gdpr).
- **Verified:** Google Forms supports anyone-with-link responder access; limiting to one response requires Google
  sign-in. Shared response summaries can expose full written responses. A feedback form should not enable either
  setting. [Google Forms sharing/settings](https://support.google.com/docs/answer/2839588).
- **Verified:** GitHub issue forms support structured reports, but create repository issues rather than a private
  support inbox. They fit technical contributors; do not make public issue creation the only feedback path for
  an app built around personal photos. [GitHub issue templates](https://docs.github.com/en/communities/using-templates-to-encourage-useful-issues-and-pull-requests/configuring-issue-templates-for-your-repository).
- **Verified:** Android warns that UI-thread bitmap loading can hurt responsiveness. This supports investigating
  the observed synchronous Reframe master decode, not claiming a measured stall. Zinely already samples normal
  imports; any preview change must retain source geometry and export quality.
  [Android bitmap guidance](https://developer.android.com/develop/ui/views/graphics).
- **Verified:** Moderated usability tasks can reveal where an existing service or prototype causes difficulty.
  **Recommendation:** run a short create/edit/export/fold session before adding more onboarding, menus or search.
  [GOV.UK usability testing](https://www.gov.uk/service-manual/user-research/using-moderated-usability-testing).

### Initial recommendation — superseded by owner selection below

Use a normal link to a short Tally form after owner/provider/privacy approval; keep visible email fallback.
Linking avoids adding a third-party embed to every page visit and keeps the website's existing lightweight
structure. Google Forms is acceptable if the owner prefers its existing account workflow. Neither needs a custom
backend, app network permission, analytics SDK, voting board or chat service. Do not call submissions anonymous:
the message may contain personal information and the provider may process technical metadata.

Only the message is required; category and reply email are optional. Avoid uploads and collecting unfinished
answers. Test the exact form's keyboard/zoom/screen-reader error and success states plus delivery/deletion before
publishing. Privacy text must distinguish voluntary website feedback from the offline Android app. No provider
account, terms acceptance, form endpoint or submission has been created in this review.

### Owner decision and public feedback route — 9 September

The owner selected Google Forms, created/published the three-question form, and approved 90-day deletion
of identifiable feedback. Only message is required; category and reply email are optional. Screenshots and
recordings go by email, not form upload. The public responder page was fetched without authentication;
no response was submitted during this check. This is not an audit of private response-summary settings.
The website links rather than embeds the form, without campaign query parameters. Google Forms uploads
require sign-in; optional email does not imply anonymous processing by Google.
[Google question types](https://support.google.com/docs/answer/7322334),
[Google privacy](https://policies.google.com/privacy),
[Zinely policy](PRIVACY-POLICY.md#feedback-and-email).

## R18. Deliberate folding instructions — 9 September

- **Verified:** [Origami Guide](https://origami.guide/origami-boxes/easy-origami-basket/) pairs numbered
  actions with pictures and explicit next/previous navigation. [iFixit EDU](https://edu.ifixit.com/editing-project/edit-edit-edit)
  recommends sufficient procedural detail rather than vague directions. **Recommendation:** one action card
  at a time, persistent instructions, and a complete all-steps view; never hide cut safety to fit a line count.
- **Verified:** [Photoworks](https://photoworks.org.uk/learn/how-to/single-sheet-zine/) identifies cutting
  from the folded edge toward the midpoint. **Recommendation:** retain folded-edge and stop-dot cues, and
  animate the cut only to its prescribed endpoint. The diagram remains authoritative without animation.
- **Verified:** [Origami Simulator](https://amandaghassaei.com/projects/origami_simulator) solves creases
  simultaneously using WebGL, not a sequential human tutorial. **Rejected for Zinely:** a physics engine,
  draggable camera, or general origami renderer would add dependencies and cognitive load for one booklet.
- **Verified:** [WCAG 2.2.2](https://www.w3.org/WAI/WCAG21/Understanding/pause-stop-hide.html) addresses
  qualifying automatic motion (Level A); [2.3.3](https://www.w3.org/WAI/WCAG21/Understanding/animation-from-interactions.html)
  supports disabling interaction motion (Level AAA, beyond the AA baseline). **Recommendation:** no autoplay,
  deliberate Show direction, pause/resume/reset, reduced-motion static diagrams, and no focus-triggered motion.
- **Implementation boundary:** dependency-free SVG geometry and a requestAnimationFrame clock demonstrate
  simple hinge folds, the cut, and a labelled top view of the slit opening. Steps 4, 7, 8, and 10 stay static;
  do not invent transitions for reorientation or book closing. All steps stay readable without JavaScript.
  Three semantic instruction lines may wrap with zoom/mobile; a shared card footprint reserves the tallest
  content. No line clamping, tiny-font fitting, or forced three-line overflow.
- **Unproven:** improved physical folding success has not been measured with novice makers. This website
  prototype does not authorize porting animation to Compose; a paper task is still the next usability check.

## R19. Public website personality and paper interaction, 10 September

- **Verified:** [MSCHF](https://mschf.com/) uses compact named projects to invite curiosity;
  [Neal.fun](https://neal.fun/) names its maker plainly; [Are.na](https://www.are.na/) explains concrete actions
  and its independent context. **Recommendation:** make the odd little object and the people behind it specific,
  with a literal product explanation before optional wit. Do not borrow wording or visual identity.
- **Verified:** [Hoverstat.es](https://www.hoverstat.es/) documents tactile portfolio motifs including stacks,
  contact sheets, and mechanical controls. These are gallery observations, not firsthand interaction tests.
  **Recommendation:** bounded local movement and reversible page arrangement, not an animated agency template.
- **Verified:** [W3C interaction animation guidance](https://www.w3.org/WAI/WCAG22/Understanding/animation-from-interactions.html)
  supports disabling nonessential interaction animation. **Implementation:** no motion at rest, instant reduced-motion
  states, native keyboard/touch disclosures, no-JavaScript content, and no new runtime dependencies.
- The [website audit](reviews/2026-09-10-website-voice-audit.md#second-pass-paper-personality-and-interaction)
  records the broader agency, indie, unconventional-brand, internet-culture, and experimental-web research, its
  limits, and selected applications. The [voice guide](design/VOICE.md#public-website-voice) is the writing authority.
- **Unproven:** humor, memorability, and improved product understanding still need first-visitor feedback.

## R20. Creative tools feasibility, 12 September 2026

- **Verified:** Android supports [bundled font families and styles](https://developer.android.com/develop/ui/compose/text/fonts).
  **Recommendation:** bundle a curated set for Zinely, not runtime-downloaded fonts, so editing and printing remain
  offline. This platform capability is not evidence that Zinely already has a working document-font picker.
- **Verified:** Android [Canvas](https://developer.android.com/reference/android/graphics/Canvas) exposes `clipPath`
  and alpha-layer operations. **Recommendation:** carry masks and opacity through Zinely's shared document/render
  model, not a Compose-only visual effect that disappears from exported pages. API availability alone does not prove
  every PDF/bitmap/device combination; explicit export and physical-print checks remain necessary.
- **Repository verified:** `DocumentFontRegistry.Bundled` registers only Inter; the image model and `DrawImage`
  have no opacity or arbitrary mask fields; `Crop`/`localClip` are rectangular; authored `DecorElement`/`SupplyCatalog`
  already provide an extension point for a curated art pack. UI-font credits are not document-font availability.
- **Recommendation:** preserve original image assets for non-destructive cutouts. Treat rectangular crop presets,
  shaped masks, freehand paths, and automatic segmentation as different scopes, not one “better crop” ticket.
- **Unproven:** development duration, desired font set, font-file size impact, cutout quality and first-time usability.
  Review each candidate font's actual licence and required script/style coverage before bundling. No licence change,
  font acquisition, model download, product promise or implementation is authorized by this assessment.
- The [roadmap assessment](ROADMAP.md#creative-tools-assessment) owns relative effort and proposed sequencing.

## R21: About return-focus test mode, 12 September 2026

- **Verified:** AndroidX's [ClickableTest](https://android.googlesource.com/platform/frameworks/support/+/c8a071114c193cd7b43a05ba1489e72d21f3b833/compose/foundation/foundation/src/androidInstrumentedTest/kotlin/androidx/compose/foundation/ClickableTest.kt)
  explicitly switches to keyboard mode for focus requests and states that clickable is focusable in non-touch mode.
  The existing Zinely shelf focus test follows the same pattern.
- **Application:** About tests that assert keyboard focus must request `InputMode.Keyboard`; touch visibility and
  click tests alone cannot prove or disprove keyboard restoration. This does not verify TalkBack accessibility focus.
- **Recommendation:** preserve the About list state across licence navigation and request return focus inside the
  restored lazy row, where its target is attached. The final regression result, not source reasoning, gates acceptance.

## R22. Organising a small library of your own work on a phone — ✅ VERIFIED + 🟦 RECOMMENDATION

Research for Shelf folders ([ADR-125](DECISIONS.md#adr-125)), 6 October 2026. Sources are official
help pages unless stated, all opened on that date. Some pages were read through a tool that returns a summary
of the page, so this section **paraphrases and does not quote**: each sentence is the claim its source supports,
not the source's words. What could not be opened is listed in R22.7 and is not relied on.

### R22.1 A container either holds a work or points at it, and deleting follows from that — ✅ VERIFIED

Apps where an item can sit in several containers keep the items when a container is deleted: Apple Books
(deleting a collection leaves its books in the library), Apple Photos (photos stay in the library and in any
other album) and Google Photos. Apps where an item lives in exactly one container take the contents with it:
Samsung Notes (deleting a folder removes what is in it), Apple Notes, Goodnotes and Sketchbook. Every
one of that second group has a trash that catches them (Recently Deleted or Trash; Concepts too). No sampled app
destroys a folder's contents with no way back.

**Sources:** [Apple Books](https://support.apple.com/guide/iphone/organize-books-iphab219b91/ios) ·
[Apple Photos](https://support.apple.com/guide/iphone/create-and-work-with-photo-albums-iphc0fc668ab/ios) ·
[Google Photos](https://support.google.com/photos/answer/6128849?co=GENIE.Platform%3DAndroid) ·
[Samsung Notes](https://www.samsung.com/us/support/answer/ANS10004548) ·
[Apple Notes](https://support.apple.com/guide/iphone/organize-in-folders-ipha61270292/ios) ·
[Goodnotes, delete](https://support.goodnotes.com/hc/en-us/articles/7353743683599-Delete-documents-notebooks-and-folders-from-your-library) ·
[Sketchbook](https://help.sketchbook.com/docs/gallery-folders) ·
[Concepts](https://concepts.app/en/android/manual/gallery).

### R22.2 Creative apps move a work into one place; several-at-once is a reading and photo pattern — ✅ VERIFIED

Procreate, Goodnotes, Samsung Notes, Concepts, Sketchbook, ibis Paint and Apple Notes folders all move an item.
Being in several containers at once appears in Apple Books, Apple Photos, Google Keep labels and Apple Notes
tags. No creative-document app that was opened documents one work living in two folders.

**Sources:** as R22.1, plus [Procreate](https://help.procreate.com/procreate/handbook/gallery/gallery-organize) ·
[Goodnotes, organise](https://support.goodnotes.com/hc/en-us/articles/15303674514191-Organize-folders-and-documents-in-Goodnotes) ·
[ibis Paint](https://ibispaint.com/lecture/index.jsp?no=193) ·
[Google Keep](https://support.google.com/keep/answer/6191044?co=GENIE.Platform%3DAndroid) ·
[Apple Notes tags](https://support.apple.com/en-nz/102288).

### R22.3 Dragging one onto another is the phone's own gesture, and the careful apps also give a menu — ✅ VERIFIED

Phone home screens make a folder by dragging one app onto another (iOS, Pixel), and Procreate, ibis Paint and
FlipaClip copy the gesture. Procreate documents two routes (Select mode, or drag and drop), as do
ibis Paint and Goodnotes; Sketchbook is menu-only. WCAG 2.2 SC 2.5.7 (Level AA) requires that anything done by
dragging can also be done with a single pointer without dragging, and Android's
accessibility guidance says to expose a drag as a custom action so TalkBack, Voice Access and Switch Access can
perform it. A custom action serves assistive technology only, so a visible menu route is still needed.

**Sources:** [iPhone User Guide](https://support.apple.com/guide/iphone/organize-your-apps-in-folders-iph822ece7dd/ios) ·
[Pixel Help](https://support.google.com/pixelphone/answer/2781850?hl=en) ·
[Procreate](https://help.procreate.com/procreate/handbook/gallery/gallery-organize) ·
[FlipaClip](https://support.flipaclip.com/article/79-organizing-projects-with-stacks) ·
[WCAG 2.2, Understanding 2.5.7](https://www.w3.org/WAI/WCAG22/Understanding/dragging-movements) ·
[Android accessibility principles](https://developer.android.com/guide/topics/ui/accessibility/principles).

### R22.4 A home-screen folder cannot be empty; a folder made by a "New folder" command can — ✅ VERIFIED

On the iPhone home screen a folder that is emptied is deleted automatically. Apps with an explicit New
folder or New collection command (Apple Books, Goodnotes, Play Books, Apple Notes) create the container empty,
so empty ones persist and need managing.

**Sources:** [iPhone User Guide](https://support.apple.com/guide/iphone/organize-your-apps-in-folders-iph822ece7dd/ios) ·
[Play Books](https://support.google.com/googleplay/answer/9402600?hl=en&co=GENIE.Platform%3DAndroid).

### R22.5 Visual work is shown as a preview of what is inside — ✅ VERIFIED

Procreate draws a Stack as a pile of layered thumbnails and Concepts shows a folder with a thumbnail of what is
in it. Goodnotes uses a coloured icon tile and Sketchbook a thumbnail strip.

**Sources:** [Procreate](https://help.procreate.com/procreate/handbook/gallery/gallery-organize) ·
[Concepts](https://concepts.app/en/android/manual/gallery).

### R22.6 People navigate to their own things and keep structures shallow — ✅ VERIFIED, with a 🟨 ASSUMPTION edge

Bergman et al. (2008) report that people navigated for 56–68% of file retrievals and searched for 4–15%, with
search a last resort. Bergman, Whittaker et al. (2010) found retrieval from a mean depth of 2.86 folders with
about 12 files per folder, and that people avoided both deep structures and large folders. NN/g finds deep
hierarchies harder to use than flat ones. These studies are of desktop file systems and websites, not a phone
grid of covers.

🟨 **ASSUMPTION:** whether filing pays off at all may be disputed. A 2011 email study (Whittaker et al.) is
reported second-hand as finding foldering inefficient; its primary source could not be opened, so it is recorded
as a lead and not as a counter-finding.

🟨 **ASSUMPTION:** folders start to help at roughly more than one screenful of covers and add cost below that.
No research was found that gives a library-size threshold for a phone grid.

**Sources:** [Bergman et al. 2008, abstract](https://cris.openu.ac.il/en/publications/improved-search-engines-and-navigation-preference-in-personal-inf-2/) ·
[Bergman, Whittaker et al. 2010](https://research.ibm.com/publications/the-effect-of-folder-structure-on-personal-file-navigation) ·
[NN/g, flat vs deep hierarchy](https://www.nngroup.com/articles/flat-vs-deep-hierarchy/).

### R22.7 Not verified — 🟨 ASSUMPTION

Kindle Collections, Canva mobile folders and Notability could not be opened and are not relied on. The Procreate
handbook does not say whether a Stack can hold a Stack, what deleting one does, or whether it dissolves when
emptied. Material 3's guidance on bottom sheets for item actions could not be read (the page is script-rendered).
No documented example was found of an older app version reading a newer backup and letting unknown folder
structure fall back to the root; Zinely's own behaviour is established from its code in ADR-125, not from the web.

### R22.8 Zinely application — 🟦 RECOMMENDATION

For a Shelf of roughly 3 to 40 zines: one level (R22.6); a zine sits in one place (R22.2); a folder that is
removed gives its zines back to the Shelf, because Zinely has no trash and every app that deletes contents has one
(R22.1); no empty folders, so there is nothing to manage (R22.4); a folder drawn as a pile of its own covers so
the Shelf still answers by recognition (R22.5); every action available from a menu, with any drag gesture a later
shortcut and never the only route (R22.3). Because nothing shows folders help below a screenful (R22.6), the
design must cost nothing for someone who never makes one. [ADR-125](DECISIONS.md#adr-125) owns the decision.

## R23. The smallest size for Book on a mini-zine page — ✅ VERIFIED + 🟦 RECOMMENDATION, with a 🟨 ASSUMPTION edge

Asked by the owner on 2026-10-08: what do other zine tools use as a minimum text size, and what should Book's
be? Web research, one pass, 2026-10-08. Sources were opened unless marked otherwise. Not independently
re-checked source by source.

### R23.1 No zine tool found sets a minimum — ✅ VERIFIED (as an absence), partly 🟨 ASSUMPTION

- The zine-making guides of two university libraries
  ([University of Delaware](https://guides.lib.udel.edu/zines/making),
  [Niagara County Community College](https://libguides.niagaracc.suny.edu/zines/makezines)) give no text-size
  advice. Delaware lists Electric Zine Maker and Canva, with none either.
- 🟨 Searches found no documented minimum for Electric Zine Maker or Canva's zine templates. Mixam's artwork
  guide could not be opened. Adobe Express, Blurb, Affinity and InDesign tutorials, and web imposers were not
  searched. The "9 to 12 pt for zine body text" figure that turns up comes from pages with no source and is
  not relied on.
- [Amazon KDP](https://kdp.amazon.com/en_US/help/topic/G202145450) requires "a minimum font size of 7
  points". That is a floor for any printed text, not a size for body text.

So a minimum in Zinely follows no convention among zine tools. It is a legibility decision of its own.

### R23.2 Clear print is measured by the height of the small letters — ✅ VERIFIED

[UKAAF, *Creating clear print and large print documents*](https://webs.uab.cat/act/wp-content/uploads/sites/126/2017/03/ukaaf-creating-clear-print-and-large-print-documents.pdf):
clear print is 12 pt at least, 14 pt recommended; large print 16 pt at least, 18 preferred. Its note 3.1 says
point size "varies from typeface to typeface" and gives the measure meant: small letters 2 mm tall for
"12 point", 2.3 mm for "14 point". It finds "no definitive evidence" that serif or sans is easier to read.
🟨 The 18 pt large-print figures of [APH](https://www.aph.org/app/uploads/2022/04/Research-Based-Large-Print-Guidelines.pdf)
and [ACB](https://www.acb.org/node/173) are from search summaries; the pages were not opened.

### R23.3 Fraunces has a cut made for small text, and Zinely bundles it — ✅ VERIFIED

The [Fraunces README](https://github.com/undercasetype/Fraunces): the optical-size axis runs 9 to 144, and as
it goes down "the x-height increases, spacing opens up, and the characters expand in width". The static
instances are 9pt, 72pt and 144pt; the four document faces are the 9pt ones
([Brief 02, The four faces](planning/BRIEF-02-THREE-VOICES.md#the-four-faces)). ⚠️ DISPUTED for paper: one
[review](https://pimpmytype.com/fraunces/) calls Fraunces "almost too delicate" below 14 px, which is a
judgement of a screen.

### R23.4 Zinely application — 🟦 RECOMMENDATION

**12 pt.** By Brief 02's measurement Book's small letters are 1.66 mm at 10 pt and 1.99 mm at 12 pt, so 12 pt
is where Book all but reaches UKAAF's 2 mm and 10 pt is clearly under it. Book at 12 pt is about Plain at
10 pt (1.93 mm, itself just under 2 mm), which the app allows, so the two are held to the same standard as each
other; 14 pt (2.32 mm) would hold Book to a
stricter one and, on a page about 74 mm wide, costs line length and two size steps. With a display cut of
Fraunces the answer would be 14 pt.

**Not settled by any of this:** how the 9pt cut's thin strokes and its bold print on a home inkjet. Nothing
was found on that. Only the printed page answers it
([Brief 02, Procedure A](planning/BRIEF-02-THREE-VOICES.md#procedure-a-the-printed-book-page)). The ruling is
in the [decision gate, Q2](planning/ZINELY-1X-DECISION-GATE.md#q2-typefaces-o12--o8).

## R24. The Font row: what other editors and the guidance say — ✅ VERIFIED + 🟦 RECOMMENDATION, with a 🟨 ASSUMPTION edge

A second pass before [`v21-typebar.html` A29](design/mockups/v21-typebar.html) was frozen, asked for by the owner
on 2026-10-08. Its brief was to overturn the twelve recommendations (V-1 to V-12), not to confirm them. Web
research, one pass. ✅ marks a page that was opened; 🟨 a search summary or an inference. **Not found or not
opened:** the Material 3 guideline pages for segmented buttons and snackbars (they would not render), Canva's
and Adobe's help pages (refused), and anything on how CapCut, Phonto, Picsart, Procreate, GoDaddy Studio or
Adobe Express name the control. No study comparing a fade with cut-off content as a sign of scrolling was found.

### R24.1 The word is "Font", and it comes before Size — ✅ VERIFIED

[Google Docs on Android](https://support.google.com/docs/answer/1663349?co=GENIE.Platform%3DAndroid) lists
"Style, Font, Size, Text color". [Keynote on iPhone](https://support.apple.com/guide/keynote-iphone/tan7b17ae845/ios)
says "tap Font". 🟨 Canva's text toolbar is reported to start with Font. Nothing was found that calls the
control Voice, Lettering or Type. 🟨 Moving controls a returning user knows has a cost; nothing was found on
a shift of one row.

### R24.2 A font behind a button named "Size" is likely to be missed — ✅ VERIFIED

[NN/g on information scent](https://www.nngroup.com/articles/information-scent/): with a vague label, "people
might miss a good source of information". No evidence was found for the other side.

### R24.3 A state told by a 2.21:1 fill is told by colour alone — ✅ VERIFIED

[WCAG 1.4.1](https://www.w3.org/WAI/WCAG22/Understanding/use-of-color.html) counts a difference in colour as a
second sign only from 3:1. [WCAG 1.4.11](https://www.w3.org/WAI/WCAG22/Understanding/non-text-contrast.html)
wants a graphic that carries a state to reach 3:1 against what is beside it.

### R24.4 Scrolling inside a panel is allowed; the sign that works is content cut off — ✅ VERIFIED (the sources give advice, not measurements)

[WCAG 1.4.4](https://www.w3.org/WAI/WCAG22/Understanding/resize-text.html) fails text that is "clipped,
truncated or obscured" at 200 %; scrolling to reach it passes.
[NN/g, the illusion of completeness](https://www.nngroup.com/articles/illusion-of-completeness/) recommends
content that peeks past the edge and does not mention fades;
[UIE](https://archive.uie.com/brainsparks/2006/08/02/utilizing-the-cut-off-look-to-encourage-users-to-scroll)
says the same, without data. [NN/g on bottom sheets](https://www.nngroup.com/articles/bottom-sheet/) warns
against a sheet "obscuring relevant background content".

### R24.5 Something that cannot be used should stay reachable and say why — ✅ VERIFIED for the web, 🟨 on Android

[Smashing Magazine](https://www.smashingmagazine.com/2021/08/frustrating-design-patterns-disabled-buttons/)
("Disabled buttons don't explain what's wrong") and
[CSS-Tricks](https://css-tricks.com/making-disabled-buttons-more-inclusive/). 🟨 Their reason is the web's,
where a disabled control leaves the focus order. On Android a disabled node is reported to stay reachable by
TalkBack and to be read as "disabled", so a control that is enabled and does nothing may promise more than
one that is disabled. Not settled here.

### R24.6 A missing font is flagged beside its name, and replacing it is treated as a change worth a warning — ✅ VERIFIED, with a 🟨 ASSUMPTION edge

[Figma, the missing-font alert](https://help.figma.com/hc/en-us/articles/360039956994-Missing-font-alert-in-Figma-Design).
For words that leave their box, [Affinity Publisher](https://affinity.help/publisher2/en-US.lproj/pages/Text/flowingText.html)
turns the flow button red; 🟨 InDesign is reported to show a plus sign.

### R24.7 Platform facts that bind the build — ✅ VERIFIED

- [Android accessibility help](https://support.google.com/accessibility/android/answer/7101858): touch targets
  of 48 dp at least, both ways.
- [Material Components for Android](https://github.com/material-components/material-components-android/blob/master/docs/components/ToggleButtonGroup.md):
  "Segmented buttons are being deprecated in the Material 3 expressive update".
- [Android 14](https://developer.android.com/about/versions/14/features): text scaling to 200 % is not linear
  ("4sp + 20sp might not equal 24sp"), so sizes worked out by adding sp values cannot be relied on; the page
  also says not to use sp for padding.

### R24.8 Zinely application — 🟦 RECOMMENDATION

Nine of the twelve answers stand. One is reversed (V-12) and two keep their answer with a change (V-6, V-10),
all three in A29 before it is frozen; the rest are conditions on the build:

- **V-12 changes.** With a row named Style in the card, "Text style changed" names the wrong row after a
  font change. Outside evidence for this is thin (🟨 one design system's wording pattern); the collision is
  the app's own. A font change says "Font changed" and "Font put back".
- **V-6 is sharpened.** The evidence is for a cut row, and none was found for a shade of 1.4 to 1.8:1. The
  cut row is the first sign and the shade the second (R24.4).
- **V-10 is sharpened.** The line said "It is shown in Plain" beside a Plain that was not chosen, and a tap
  on Plain then replaced the stored font with nothing to see. The line now says the tap replaces the font
  (R24.6), and it shows ahead of Book's own reason while the font is unknown.
- **For the build:** a 48 dp touch target whatever is drawn; the row built as the Align row is, not from the
  Material segmented button; the card's fit at the largest text measured, not worked out (R24.7).
- **For the phone passes:** whether the row is found behind "Size" (R24.2 expects not); whether the chosen
  "Book" reads by day; whether "unavailable" on an enabled control is right under TalkBack (R24.5); whether
  the card covering the words being changed gets in the way (R24.4).
- **A known limit, not mended:** words pushed out of their box by a change of font get no sign (R24.6).

[Decision gate Q2](planning/ZINELY-1X-DECISION-GATE.md#q2-typefaces-o12--o8) owns the ruling; A29 owns the design.

## R25. Selection controls that take room from the canvas (2026-10-13)

Asked for [D-113](design/V2-SPEC-DEFECTS.md#d-113) item 2: the Bench adds a two-row bar on select and the page
shrinks to make room. Decision: [ADR-129](DECISIONS.md#adr-129).

### R25.1 The tap-only way to move something may be one step away — ✅ VERIFIED

[WCAG 2.5.7 Dragging Movements](https://www.w3.org/WAI/WCAG22/Understanding/dragging-movements.html) asks
that anything done by dragging can also be done with a single pointer without dragging. Its own examples
are a list where an item is selected and then moved with arrow buttons, and a menu opened from the item.
Neither is on screen before the item is chosen.

### R25.2 Other editors put precise positioning one tap deeper — ✅ VERIFIED for Keynote, 🟨 for the rest

[Keynote on iPhone](https://support.apple.com/guide/keynote-iphone/tanb46504b79/ios) reaches exact position
and size through the Format button and an Arrange tab. The Research Agent for this change (2026-10-13) also
reported that Canva's phone editor does the same from a Position entry in its bottom bar; it gave no source
for that, so it is an assumption here.

### R25.3 A layout that moves is a fault only when it is not expected — ✅ VERIFIED for the quote (web source), 🟦 for its use here

[web.dev on Cumulative Layout Shift](https://web.dev/articles/cls): "A layout shift is only bad if the user
isn't expecting it." 🟦 The reading taken here: a panel that opens because its button was tapped is
expected; a page that changes size because a piece on it was touched is less so. The source does not say
that. Its own metric treats a shift that follows a tap as expected, which could be read the other way.

### R25.4 A row that scrolls sideways needs something cut off at its edge — ✅ VERIFIED

[NN/g on horizontal scrolling](https://www.nngroup.com/articles/horizontal-scrolling/) finds arrows and
bars often go unnoticed and recommends content that visibly continues past the edge.
[eBay's carousel pattern](https://opensource.ebay.com/evo-web/components/carousel) shows part of the next
item for that reason. Applied in D-113 item 6: the verb bar fades at the side that has more, over a verb
left cut off. R24.4 above reached the same advice for panels that scroll down.

### R25.5 Zinely application — 🟦 RECOMMENDATION

Ranked by the research pass: swap the page strip for the buttons; a Move entry opening a docked tray; float
the bar; reserve the rows always; keep as built. ADR-129 takes the second, and says why not the first.

## R26. Which paper a home printer takes (2026-10-14)

Asked for [ADR-133](DECISIONS.md#adr-133): which sizes after A4 and US Letter, and whether paper should stay
a closed, named set. Web research, one pass, 2026-10-14. Model lists differ by region and by tray, so these
are samples, not a census.

### R26.1 Home printers take Legal and, mostly, A5 — ✅ VERIFIED for the models named

- Canon's spec pages for the [PIXMA TR8550](https://www.canon-europe.com/printers/pixma-tr8550/specifications/)
  and [TR7550](https://www.canon-europe.com/printers/pixma-tr7550/specifications/) list the rear tray as
  "A4, A5, B5, LTR, LGL, …"; their front cassettes take A4, A5, B5 and Letter, not Legal.
  [TR4550](https://www.canon.co.uk/printers/pixma-tr4550/specifications/): A4, A5, B5, LTR, LGL.
- 🟨 Retailer listings, not the maker's: the [Canon G1230](https://www.dell.com/en-us/shop/canon-pixma-megatank-g1230-inkjet-printer/apd/ac461205/printers-ink-toner)
  takes Letter, Legal, A4, A5, B5 and A6; the [HP DeskJet 4355](https://www.shi.com/product/51015669/HP-Deskjet-4355-All-in-One)
  takes Letter, Legal, A4, B5 and A6, and A5 is not listed; the [HP DeskJet 3755](https://www.shi.com/product/32321488/HP-Deskjet-3755-All-in-One)
  gives Legal as its largest size. HP's own [DeskJet D2600 page](https://support.hp.com/ca-en/product/hp-deskjet-d2600-printer-series/3742933)
  lists A4, A5, A6 and B5.

### R26.2 Makers draw 8-page minis for Legal — ✅ VERIFIED

The *CATS* minizines ([vol. 5](https://mrnabbles.itch.io/cats-vol-5-minizine)) and
[*How to be a Punk*](https://mrnabbles.itch.io/how-to-be-a-punk) are sold as 8-page foldable zines "best
printed on Legal size paper", and there is a [free 8-page template for Legal](https://lincodega.itch.io/8-16-page-zine-template/purchase).
Most guides, such as [Niagara County Community College's](https://libguides.niagaracc.suny.edu/zines/makezines),
assume ordinary printer paper, Letter or A4.

### R26.3 A3 and Tabloid need a wide-format printer — ✅ VERIFIED (vendor sources)

[Sharp](https://business.sharpusa.com/a3-vs-a4-printers-whats-the-difference) and
[Loffler](https://www.loffler.com/blog/a3-vs-a4-printers) describe A4-class printers as unable to take A3,
Tabloid or Ledger, some taking Legal at most. Both sell office printers, so the framing is commercial.

### R26.4 A5 is A4 halved and keeps its shape — ✅ VERIFIED

[ISO 216](https://en.wikipedia.org/wiki/ISO_216): A-series sheets
have sides in the ratio √2, so halving one gives the next with the same shape; A4 is 210 × 297 mm, A5
148 × 210 mm. A Zinely page on A5 is the A4 page at 1/√2, within a point.

### R26.5 Android names its print sizes and allows custom ones — ✅ VERIFIED

[`PrintAttributes.MediaSize`](https://developer.android.com/reference/android/print/PrintAttributes.MediaSize)
has named constants, among them `NA_LETTER`, `NA_LEGAL` (8.5 × 14 in), `ISO_A4` and `ISO_A5` (148 × 210 mm),
and a public constructor for a custom size in thousandths of an inch. The platform keeps a named list and
an escape hatch, the shape ADR-133 takes: named for people, any size for the engine.

### R26.6 Half-Letter is not on the lists checked — 🟨 ASSUMPTION (an absence)

5.5 × 8.5 in does not appear in any list above. A maker wanting it would cut Letter in half by hand. Not
searched: Epson and Brother spec sheets.

### R26.7 Zinely application — 🟦 RECOMMENDATION

US Legal first (every printer checked but one older HP takes it, and makers use it) and A5 second (most
printers checked take it, and it is the small zine on A-series paper; 🟨 no maker demand was found). Not half-Letter, A3 or Tabloid. Keep the sizes a
named set. ADR-133 records the decision.

---

## R27. One hand for a set of single-ink pieces (2026-10-14)

Evidence for the Art house style ([SUPPLIES-SPEC §4.4](design/SUPPLIES-SPEC.md#house-style),
[ADR-132](DECISIONS.md#adr-132), [D-117](design/V2-SPEC-DEFECTS.md#d-117)). Gathered by a Research Agent on
2026-10-14. Sources it could not open (Material Design's icon pages, Shopify Polaris, MoMA on Matisse) are
not cited.

### R27.1 A set reads as one hand when weight, detail and view agree — ✅ VERIFIED

- [IBM UI icons](https://www.ibm.com/design/language/iconography/ui-icons/design/): "no icon should appear
  heavier or lighter than another… A single icon shouldn't contain a mix of different stroke weights…
  They'll feel unbalanced and look like a mistake." [IBM pictograms](https://www.ibm.com/design/language/iconography/pictograms/design/)
  fix one stroke for the whole set and say "Avoid using perspective… Create depth through flat layers".
- [Apple HIG, Icons](https://developer.apple.com/design/human-interface-guidelines/icons): icons "need to use a
  consistent size, level of detail, stroke thickness (or weight), and perspective", and "Too many details can
  make an interface icon confusing or unreadable."
- [Atlassian iconography](https://atlassian.design/foundations/iconography): "Use simplified shapes with the
  minimum detail required to express a metaphor."

🟦 Applied as H1 (one weight), H3 (flat views), H4 (one margin) and H8 (three ideas at most).

### R27.2 Knockouts need more room than ink — ✅ VERIFIED, with a 🟨 house number

[Onlineprinters on line weight](https://www.onlineprinters.ie/c/print-files/line-weight): positive lines at
least 0.25pt, negative (knocked-out) lines at least 0.5pt. 🟨 No source gives a minimum counter for filled
icon sets, so H2's 1.5-unit gap is a house number, set at about twice the printer's floor at the smallest
landing size. It should be checked by printing and photocopying (owed, ADR-132).

### R27.3 Torn paper is irregular at more than one scale — ✅ VERIFIED (physics), 🟨 for the reading

[Alava et al., arXiv cond-mat/0608052](https://arxiv.org/abs/cond-mat/0608052): paper fracture lines show
multiscaling at small scales and self-affine scaling at larger ones. [Wikipedia, Deckle edge](https://en.wikipedia.org/wiki/Deckle_edge)
says machine deckles are cut "into patterns" and are valued for "the impression that it is handmade" (marked
"citation needed" there). 🟨 That a regular zig-zag reads as clip-art is an assumption consistent with both;
no perception study was found. Applied as H6: a slow bow plus an uneven fine jag, from one hand-written table.

### R27.4 Print and photocopy floors — ✅ VERIFIED

[Tradeprint on hairlines](https://help.tradeprint.co.uk/hairlines): hairlines "print differently on different
printers… use at least 0.25pt." [Kumar et al., AJFSFM](https://journals.nauss.edu.sa/index.php/AJFSFM/article/view/1825):
line features distort across photocopy generations, "much more pronounced… beyond the second generation".
🟦 H2's 2-unit ink (about 1mm at stamp size) and 1.5-unit gap (about 0.74mm) sit far above both floors.

### R27.5 Printer's marks have conventional forms — ✅ VERIFIED

- Crop marks are "short, thin, solid, horizontal and vertical lines, placed outside of the trim area at each
  corner" ([University of Melbourne print room](https://ms-kb.msd.unimelb.edu.au/print-room/file-preperation/bleed-and-crop-marks)).
- A registration mark is printed on every separation; its usual form is a ring with a cross
  ([Wikipedia, Registration mark](https://en.wikipedia.org/wiki/Registration_mark), shown in its illustration).
- A halftone is a regular grid of dots that vary in size ([Wikipedia, Halftone](https://en.wikipedia.org/wiki/Halftone)).
  ⚠️ DISPUTED: screen-printing sources differ on 45° versus 22.5° for the angle. A34 uses 45°, the angle
  most readers know.

🟦 Applied in the *printer's mark* voice of §4.4: these are drawn exact, not by hand.

## R28. Tap-only arrange controls one step away, and what to call them (2026-10-14)

Asked before deciding [ADR-129](DECISIONS.md#adr-129): does any evidence argue for keeping the bar that
shrinks the page, and what should the way in be called? Research Agent pass, 2026-10-14. R25 is the first
pass and is not repeated here.

### R28.1 Mainstream editors put exact controls one step away, not in a bar in the layout — ✅ VERIFIED, 🟨 for Canva and Adobe Express

- [Google Slides on Android](https://support.google.com/docs/answer/1696521?hl=en&co=GENIE.Platform%3DAndroid):
  move, resize and turn are drags; layer order is behind the selection menu's More.
- [Keynote on iPhone](https://support.apple.com/guide/keynote-iphone/tanb46504b79/ios): nudging is a
  multi-finger gesture; exact position is in Format > **Arrange**.
- [Procreate](https://help.procreate.com/procreate/handbook/transform/transform-interface-gestures): tap-only
  nudges exist, but only inside Transform mode, opened from a button. The closest precedent found for
  single-tap nudges one step away.
- [Word](https://support.microsoft.com/en-us/office/align-or-arrange-objects-bfd91078-2078-4b35-8672-f6270690b3b8):
  layer order sits in an **Arrange** group that appears only when a shape is selected (desktop).
- 🟨 Canva's phone editor: third-party guides describe a Position entry in the selection toolbar; Canva's own
  pages refused the fetch. Adobe Express mobile: one user report only.
- None of the official sources keeps a nudge bar in the layout.

### R28.2 The tap-only path may sit one step away; the element's own screen-reader actions do not replace it — ✅ VERIFIED, 🟦 for the inference

- [WCAG 2.5.7 Understanding](https://www.w3.org/WAI/WCAG22/Understanding/dragging-movements.html): the
  alternative "does not have to be the same component, so long as the functionality is equivalent"; its
  examples are select-then-arrow-buttons and a menu. The same page says keyboard equivalence alone does not
  meet the criterion.
- [Android accessibility principles](https://developer.android.com/guide/topics/ui/accessibility/principles):
  expose gestures as custom actions for TalkBack, Voice Access and Switch Access. Zinely already does, on
  every element (`EditorA11y.elementCustomActions`).
- 🟦 Inference: custom actions serve assistive-technology users; a person tapping without one never sees them.
  The visible tap path must stay, which is why the tray exists rather than nothing.

### R28.3 The layout-shift argument cuts both ways — ⚠️ DISPUTED

[web.dev on CLS](https://web.dev/articles/cls) also says shifts in response to a user's own input "are
generally fine, as long as the shift occurs close enough to the interaction that the relationship is clear".
By that standard the shrink on select is not clearly a fault. So ADR-129 does not rest on CLS. It rests on
the size of the loss (about a third of the page's height on a 360 × 640dp phone, at the moment the maker is
judging the layout) and on R28.1.

### R28.4 Hiding controls has a measured cost — ✅ VERIFIED

[NN/g, "Hamburger Menus and Hidden Navigation Hurt UX Metrics"](https://www.nngroup.com/articles/hamburger-menus/)
(with its follow-up, [Find navigation on mobile](https://www.nngroup.com/articles/find-navigation-mobile-even-hamburger/)):
hidden menus were found less often and used more slowly on phones. 🟦 That study is website navigation, not
editor tools; the analogy is ours. That is the case for keeping the bar. It is why
the way in is a labelled verb that is always on the verb bar, never in an overflow, and why the tray stays
open while the maker moves from piece to piece.

### R28.5 The name — 🟦 RECOMMENDATION

"Arrange" has two first-party precedents for a group of position, size and layer controls (Keynote, Office).
"Position" is Canva's (unverified on phones). "Transform" is Procreate's and too technical for beginners.
"Move" names a quarter of what the tray does. "Adjust" means colour and light in photo apps. Taken:
**Arrange**. Whether beginners understand it is a first-time-user question for the device pass; no study
compares these words.

## R29. How apps ask for paper size, and when (2026-10-15)

Asked for [A35](design/mockups/v21-library.html) (the paper chooser for [ADR-133](DECISIONS.md#adr-133)'s
four sizes): where comparable apps ask for paper, whether it can change later, how a default is set, and
what large text and screen readers need. One web pass, 2026-10-15. Canva's help pages refused the fetch
(HTTP 403, again on a second pass) and the tutorials that describe its Resize disagree on which plans have
it, so Canva is not cited; Procreate and Affinity Publisher stand in for apps that set a canvas when the work
is created (R29.1).

### R29.1 Document and canvas apps let size change later, and say the work may not survive it — ✅ VERIFIED (Apple, Procreate, Affinity), 🟨 (Word)

- Apple Pages sets paper size per document, on Mac in the Document sidebar and on iPhone or iPad in
  Document Setup, and it can be changed in an existing document; it advises "choose US Letter" for
  standard US printer paper ([Apple, "Set paper size, page orientation, and margins in Pages"](https://support.apple.com/119612)).
- 🟨 Microsoft Word changes size from Layout › Size
  ([How-To Geek](https://www.howtogeek.com/445123/how-to-change-the-paper-size-in-microsoft-word/)).
  [Office Watch](https://office-watch.com/2024/paper-sizing-tricks-in-microsoft-word/) warns that
  "Changing paper sizes changes the margins, header/footer etc." and "Positioning of objects like images,
  tables and charts can go awry". Third-party guides; no Microsoft page was found in the pass.
- 🟨 An Apple Community thread reports an A4 template that still "prints off the page" after Page Setup was
  set to US Letter; the answer given is to rebuild on a Letter document
  ([discussions.apple.com](https://discussions.apple.com/thread/2797098)). A forum, not a measurement.
- Procreate, a drawing app whose work is placed by hand on a canvas: the size is set when a canvas is
  created, in pixels or a physical measurement, and changed later only through Crop and Resize, where "The
  edges of this box represent the new edges of your canvas", with a warning that resampling "can create
  unexpected results" ([Procreate Handbook, Canvas](https://help.procreate.com/procreate/handbook/actions/actions-canvas)).
- Affinity Publisher: "You can modify your document's setup at any time", but a page-size change makes the
  person choose between content that stays "the same size" and content rescaled "in relation to the new
  page size" ([Affinity Publisher 2 Help, Document Setup](https://affinity.help/publisher2/en-US.lproj/pages/Pages/DocumentSetup.html)).

A document that reflows can afford a later change. The canvas apps, whose work is placed by hand as a
zine's is, ask at creation and turn a later change into a choice between cutting and shrinking the work. A
zine is laid out on eight fixed panels, and a beginner should not meet that choice, which is why ADR-133
decision 5 asks once, at the start.

### R29.2 The default comes from a setting the person controls, and changes only new work — ✅ VERIFIED

- macOS: the default paper size set in Printers & Scanners "is also used as the page size whenever you
  create a new document in an app, such as Pages", and "Changing the default size affects only new
  documents; it doesn't change previously created documents"
  ([Apple, Mac User Guide](https://support.apple.com/en-asia/guide/mac-help/mh26678/12.0/mac)).
- Google Docs: in File › Page setup, "Set as default" applies the settings "to any new documents you
  create" ([Google Docs Help](https://support.google.com/docs/answer/10296604)).

This is the shape Zinely already has: About › "Paper for new zines" leads the chooser and never rewrites
an existing zine (`v21-colophon.html`).

### R29.3 Zine tools offer two sizes, sometimes a custom one — ✅ VERIFIED (each tool's own page)

- [ZineArranger](https://nashhigh.itch.io/zinearranger), a browser tool that imposes a PDF as a zine:
  "Paper size (US Letter / A4 / Custom)".
- [EZZINE](https://apps.apple.com/app/id6758923332), an iOS mini-zine maker: "Supports A4 or Letter
  format"; its version 2.2 notes say Letter was added as an option. Where the choice is made is not said.
- Electric Zine Maker's beta notes (2019): "printing reverts to US Letter and that makes it difficult for
  A4 folks", with a promise of settings to choose better
  ([Nathalie Lawhead](https://www.nathalielawhead.com/candybox/?p=4178)). Whether it shipped was not found.

None found offers Legal or A5 by name. 🟨 With four sizes Zinely offers more than these peers; the two
less common ones need a line that says some printers do not take them (R26.1 names an older HP DeskJet
without Legal, an HP DeskJet 4355 listing without A5, and Canon models that take Legal only in the rear
tray), so they are not read as everyday paper.

### R29.4 Large text and spoken names — ✅ VERIFIED

- Android 14 scales fonts up to 200%, on a nonlinear curve so large text grows less than small text, and
  asks apps to test at the maximum "to ensure that your app … can accommodate larger font sizes without
  impacting usability" ([Android 14 features](https://developer.android.com/about/versions/14/features)).
  A flat 200% is therefore the worst case for a layout check.
- WCAG 2.5.3 Label in Name: the accessible name "contains the text that is presented visually", and "a
  best practice is to have the text of the label at the start of the name"
  ([W3C, Understanding 2.5.3](https://www.w3.org/WAI/WCAG22/Understanding/label-in-name.html)).

### R29.5 Where each paper is everyday — ✅ VERIFIED (encyclopedic source)

[Wikipedia, "Paper size"](https://en.wikipedia.org/wiki/Paper_size): ISO 216 "has been adopted by all
countries in the world except the United States and Canada", while US Letter is "still in common use" in
Mexico, Costa Rica, Colombia, Venezuela, Chile and the Philippines. A35's lines ("the everyday paper in most
of the world" for A4, "in North America" for US Letter) rest on this; the Letter line undersells its reach
into Latin America and the Philippines, which is the safe direction.

### R29.6 Zinely application — 🟦 RECOMMENDATION

Ask once, when the zine is started, and keep it: the work is laid out by hand and a later change would cut
or shrink it (R29.1; ADR-133 decision 5). Let the About setting lead the list and change only new zines
(R29.2). Name all four, say each sheet's page size, and on US Legal and A5 say that the zine prints on
that paper only and that the printer must take it (R29.3, R26.1), and say where each everyday
paper is everyday (R29.5). Lay rows out so they grow at a flat 200% and start each spoken name with the
paper's name, units said in full (R29.4). A35 does this.

## R30. Telling someone their work went back to an earlier copy (2026-10-15)

Asked before deciding [ADR-134](DECISIONS.md#adr-134) and freezing library A37
([D-120](design/V2-SPEC-DEFECTS.md#d-120), issue #128): where should an app say that it repaired a damaged
file from an earlier copy, in what words, and how does a screen-reader user hear it? Pages that would not
load in the fetcher (material.io's banner and snackbar pages, Apple's HIG on alerts, the Compose
`LiveRegionMode` reference) are not cited.

### R30.1 A message the person must see stays until they dismiss it; a snackbar, by default, does not — ✅ VERIFIED

- Material's banner, as Flutter's implementation documents it
  ([MaterialBanner](https://api.flutter.dev/flutter/material/MaterialBanner-class.html)): "A banner displays
  an important, succinct message, and provides actions for users to address (or dismiss the banner). A user
  action is required for it to be dismissed." and "Banners should be displayed at the top of the screen,
  below a top app bar."
- Android's own snackbar guidance
  ([Compose snackbar](https://developer.android.com/develop/ui/compose/components/snackbar)): "a brief
  notification … It provides feedback about an operation or action without interrupting the user
  experience", and "Snackbars disappear after a few seconds." Its examples are confirmations (Undo after a
  delete), offline status and "saved". The same page documents `SnackbarDuration.Indefinite`, which "Keeps
  the snackbar displayed until the user or program dismisses it", so a snackbar *can* stay; its purpose is
  still feedback on an operation, which a repair at start is not.
- ✅ The banner guidance comes from Material 2: the `androidx.compose.material3` 1.4.0 artifact this build
  resolves (Compose BOM 2026.02.01) has no `Banner` class (checked in the local Gradle cache, 2026-10-15).
  It is cited for where a must-see message goes and how it leaves, not as a Material 3 component.
- 🟦 So a repair the maker did not ask for, and which may have undone their last change, is banner-shaped,
  not snackbar-shaped: it is about their work, it was not the result of anything they just did, and a
  message that leaves by itself at app start is easily missed. Zinely has no Material banner component; A37
  draws one in the Shelf's own material (a paper note) under the heading, which is where the Material 2 banner
  guidance puts it.

### R30.2 Recovery is told after the fact, plainly, and kept until reviewed — ✅ VERIFIED

- Microsoft Office's Document Recovery ([Recover your Word files and documents](https://support.microsoft.com/topic/5851ef9c-dcd6-4c42-9364-ad3416d8497d)):
  "Document Recovery automatically opens the next time you open the Office app to help you get your file
  back", and closing it before every file is reviewed offers "Yes, I want to view these files later (safest
  option)" or "No, remove the files. I have saved the files I need". The recovery is told at the next start,
  and it stays until the person says they are done with it.
- 🟦 Office asks the person to choose between versions because it keeps several. Zinely keeps one earlier
  copy of `meta.json` and the newer one is gone (D-116), so there is nothing to choose: the note informs
  and is dismissed. A dialog would interrupt for a decision that does not exist.

### R30.3 How to word it — ✅ VERIFIED

- [Microsoft, Error Message Guidelines](https://learn.microsoft.com/en-us/windows/win32/debug/error-message-guidelines):
  a well-written message says "What happened and why?", "What is the end result for the user?" and "What can
  the user do to prevent it from happening again?"; "Do not use
  technical jargon"; "Do not make the user feel at fault"; "Use complete but simple sentences"; "You can use
  past tense to describe a distinct event that occurred in the past"; and "Avoid the word 'please'."
- [Nielsen Norman Group, Error-Message Guidelines](https://www.nngroup.com/articles/error-message-guidelines/):
  "Concisely and precisely describe the issue", "Avoid technical jargon and use language familiar to your
  users instead", "Take a positive tone and don't blame the user", and "Merely stating the problem is also not
  enough; offer some potential remedies."
- 🟦 Applied in A37.3: what happened (a small file was damaged), why the name changed (the copy from before
  the last change was used) and the end result (named: "is called … again"). When the name went back the
  note gives the one remedy, "You can rename it again." (NN/g's "offer some potential remedies"); the zine's
  own Rename row is where it always is. Microsoft's third question, how to prevent it, has no answer the maker
  can act on (storage damage is not their doing) and is left out rather than invented. The note makes no
  claim about the pages: a page document can be put back from its own earlier copy silently (D-120 item 4),
  so "its pages weren't touched" could be false. No "backup" (Zinely's Backups are library backups, [ADR-120](DECISIONS.md#adr-120)), no "error",
  "corrupt" or file names, no "please" and no apology.

### R30.4 A status message is spoken without moving focus — ✅ VERIFIED

- [WCAG 2.2, Understanding 4.1.3 Status Messages](https://www.w3.org/WAI/WCAG22/Understanding/status-messages.html),
  a web rule ("In content implemented using markup languages, …") applied here to Android by analogy:
  "status messages can be programmatically determined through role or properties such that they can be
  presented to the user by assistive technologies without receiving focus." A status message is a "change
  in content that is not a change of context".
- Flutter's semantics reference ([SemanticsProperties.liveRegion](https://api.flutter.dev/flutter/semantics/SemanticsProperties/liveRegion.html))
  says that on Android a live region produces a polite announcement even when the widget does not have
  accessibility focus, and that "This announcement may not be spoken if the OS accessibility services are
  already announcing something else."
- 🟦 Applied in A37.10: the words are a polite live region announced only when the note arrives on a Shelf
  already showing its zines; a note present when the Shelf draws is read in order, and later changes to its
  words are silent, so it is spoken once. The note is also in reading order right after the heading, so a
  dropped announcement still leaves it found. 🟨 Whether TalkBack speaks a live region that
  is *added* already filled, rather than one whose text changes, is not settled by these sources: that is the
  device pass's question (ADR-134, owed).

---

## R31. How a screen reader should meet a described photo in a paged reader (2026-10-15)

Asked before deciding [ADR-135](DECISIONS.md#adr-135): in Read, what should TalkBack say for a photo with a
description, for one without, and for Art; where in the page should it come; and what can the printed sheet
and the PDF carry? Research pass, 2026-10-15. Brief 04's sources are not repeated here
([Brief 04, "What others teach us"](planning/BRIEF-04-READING-ORDER-AND-ALT-TEXT.md#what-others-teach-us)).

### R31.1 A picture that carries meaning needs words; a decorative one should be skipped — ✅ VERIFIED

- [WCAG 2.2 SC 1.1.1](https://www.w3.org/WAI/WCAG22/Understanding/non-text-content.html): "All non-text
  content that is presented to the user has a text alternative that serves the equivalent purpose". Content
  that "is pure decoration … is implemented in a way that it can be ignored by assistive technology".
- [Android Accessibility Help, Content labels](https://support.google.com/accessibility/android/answer/7158690):
  "Decorative images or images that don't convey meaningful information graphically do not require content
  labels." Also: "Don't include an element's type or state in its content label."
- [Amazon KDP, Image Guidelines](https://kdp.amazon.com/en_US/help/topic/G75V4YX5X8GRGXWV): "all images must have
  text in the alt attribute", which "should be short, concise and specific"; for a decorative image "set
  alt = “” or role = “presentation” … so that it can be ignored by assistive technology."
- [Apple Books Asset Guide, Accessibility](https://help.apple.com/itc/booksassetguide/en.lproj/itcc20886c20.html):
  alt text "should contain what you would put in prose if the image were not included"; "Don’t use the word
  "image" in the alt text"; for a decorative image, "use the empty attribute `alt=""`" "to suppress irrelevant
  information for VoiceOver users."
- [DAISY Knowledge Base, Image descriptions](https://kb.daisy.org/publishing/docs/html/images-desc.html): users
  "only want a very brief description of an image in its alternative text"; "If an image is purely decorative,
  the alt attribute should be left empty."
- [WebAIM, Alternative text](https://webaim.org/techniques/alttext/): do "not include phrases like "image of
  ..."", "be succinct", and with no alt "screen readers might read the image filename".

### R31.2 What TalkBack says depends on the node's role, and an image role invites a machine caption — ✅ VERIFIED (source)

Read from the open-source TalkBack ([google/talkback](https://github.com/google/talkback), `master` at
`229212f`, 2026-03-23) and Compose (`androidx-main`). Samsung phones run Google's TalkBack; the owner's
SM-A176B ran 16.2 ([ADR-119](DECISIONS.md#adr-119)). 🟨 The open-source tree may lag the shipped app.

- Compose reports `Role.Image` to the platform as `android.widget.ImageView`
  ([`SemanticsUtils.android.kt`](https://github.com/androidx/androidx/blob/androidx-main/compose/ui/ui/src/androidMain/kotlin/androidx/compose/ui/platform/SemanticsUtils.android.kt),
  `toLegacyClassName`), which TalkBack maps to `ROLE_IMAGE` when the node is not clickable, `ROLE_IMAGE_BUTTON`
  when it is (`utils/.../Role.java`).
- For `ROLE_IMAGE`, TalkBack speaks the label and then the role name **"Image"** (`value_image`), while the
  listener keeps "speak element type" on (`NonTextViewsDescription.nodeRole`, `getNodeRoleName`).
- An image node whose text, label and id are all empty speaks **"Unlabelled"** (`value_unlabelled`, `nodeState`).
  A label, any label, silences it.
- The node's own text or label always wins. Only when both are empty does TalkBack fall back to its own
  caption: text recognition, icon detection, image description (`getNodeTextOrLabelDescription`).
- Every `ROLE_IMAGE` node is "captionable" (`ImageCaptionUtils.isCaptionable`), labelled or not, so TalkBack's
  menu can offer to describe it. Any other childless node is captionable only while it has **no** text or label
  and is at most 150dp tall (`ENABLE_CAPTION_FOR_LEAF_VIEW`, `isSmallSizeNode`), so a labelled node with no role
  is not. Google: on eligible phones TalkBack describes images on the device with Gemini Nano, and the
  menu's "Describe Image" sends the image "to Gemini 1.5 Flash on the server"
  ([Android Developers Blog, 2024-09](https://android-developers.googleblog.com/2024/09/talkback-uses-gemini-nano-to-increase-low-vision-accessibility.html)).
  The same post: "The average TalkBack user comes across 90 unlabeled images per day."
- A node with no role reports no class TalkBack names, so TalkBack speaks the label alone (`getNodeRoleName`
  returns "" outside its list). The Bench's element nodes are buttons, so a photo there is "Photo" and then
  "Button" (`EditorA11y.label`; `ElementSemanticsLayer` sets `Role.Button`, since a Bench stop selects).
- **A double tap on a node with no click action still taps the screen.** `FocusActor.clickNode` performs
  `ACTION_CLICK` when the node has it; otherwise it asks the platform to click at the focused node
  (`TouchInteractionController.performClick()`), and as a last resort dispatches a tap gesture at the centre of
  the node's bounds (`simulateClickOnNode`, `rect.centerX(), rect.centerY()`). So "no click" on a node does not
  make a double tap inert: whatever takes taps under the node's centre receives one.

### R31.3 Paged readers: words in reading order, and a page of pictures without words is "not readable" — ✅ VERIFIED

- [Kindle, nonvisual reading labels](https://digprjsurvey.amazon.com/csad/help/node/TzZzGnAX8N8zL1qWfT): a book is
  "Readable in read aloud and braille" when "all meaningful non-text content has text alternatives"; it is
  "Partially readable" when "some meaningful non-text content may not have text alternatives"; and "Print
  replica and image-based Kindle books (e.g., comics, manga) do not currently support nonvisual reading."
- [EPUB Accessibility Techniques 1.1](https://www.w3.org/TR/epub-a11y-tech-11/) §3.1–3.2: a comic with alt text
  for each image still has no textual access mode, and "Photo books without text would be examples of works
  with a purely visual sufficient access mode." §5.1: a page-break marker carries a label that "is announced
  to the user", which is how a paged book says where you are.
- [WCAG 2.2 SC 1.3.2](https://www.w3.org/WAI/WCAG22/Understanding/meaningful-sequence.html) (cited in Brief 04):
  the reading sequence must be programmatically determinable. Zinely already has one rule for that on a free
  canvas ([ZINELY-DESIGN-SYSTEM §4.5](ZINELY-DESIGN-SYSTEM.md#45-reading-order), [ADR-119](DECISIONS.md#adr-119)).

### R31.4 The PDF cannot carry a description; that takes a tagged PDF — ✅ VERIFIED

- `android.graphics.pdf.PdfDocument`'s whole public API is `startPage`, `finishPage`, `writeTo`, `getPages` and
  `close`; `Page.getCanvas`/`getInfo`; `PageInfo`'s width, height, content rect and number; and its `Builder`
  ([AOSP source](https://raw.githubusercontent.com/aosp-mirror/platform_frameworks_base/main/graphics/java/android/graphics/pdf/PdfDocument.java),
  [reference](https://developer.android.com/reference/android/graphics/pdf/PdfDocument)). No tags, no structure
  tree, no alt text, no document metadata.
- A PDF image gets its words through an `/Alt` entry on a structure tag, in "tagged PDF documents with images"
  only ([WCAG technique PDF1](https://www.w3.org/WAI/WCAG22/Techniques/pdf/PDF1)).
- ✅ (repository) Zinely's PDF is the imposed sheet: `ZineExporter` runs `SingleSheet8Imposer` and
  `PdfPageRenderer` draws the eight panels, four of them upside down, on one `PdfDocument` page. Even its
  selectable text runs in sheet order, not reading order. Its reader is a printer.

### R31.5 Zinely application — 🟦 RECOMMENDATION

- **Say the maker's words, marked as a photo.** In Read a description sits among the zine's printed words, so
  the listener must hear which words are on the paper and which describe a picture. Two ways to mark it: the
  `Image` role, or the word "Photo" in the label. The role says "Image" only while the listener keeps element
  types on, so with it off a description would sound like printed words. An undescribed photo would still need a
  label (with none TalkBack says "Unlabelled", or speaks its own caption if the listener turned those on), so it
  would be "Photo, Image". And every image node stays captionable from TalkBack's menu, where a labelled node
  with no role is not (R31.2); Brief 04 rules descriptions are the maker's own and never generated. So: no role,
  and the label carries the kind, `Photo: <description>`, in the Bench's grammar (`Text: …`). This departs from
  Android's "don't include an element's type" on purpose: with no role, nothing else says it.
- **An undescribed photo keeps its stop and says "Photo".** A photo carries meaning in a zine, and the listener
  should know it is there (R31.1, R31.3's "partially readable").
- **Art with no description is decoration in Read**, skipped as WCAG and Android allow; with a description it
  is spoken like a photo, named by its piece.
- **One order for words and pictures:** the Bench's spatial order (R31.3).
- **The paper and the PDF carry nothing**, and the app must not suggest otherwise (R31.4, and R31.3's comics
  label). A plain-text reader's edition is the honest later route (Brief 04, "Future extension"). 🔭
- 🟨 The source says TalkBack offers no description of its own on a labelled node with no role (R31.2); the
  shipped TalkBack may differ from the open-source tree, so the listen checks it.

---

## R32. What makers hate, Art at scale, paper colour and a hidden signature (2026-10-15)

The evidence behind [ADR-136](DECISIONS.md#adr-136) and [ADR-137](DECISIONS.md#adr-137). Research Agent passes,
2026-10-15. Each report was checked by its own reviewer; R32.6 was not. Quotes were taken by a fetch summariser
or from search snippets; spot-check before quoting one elsewhere.

### R32.1 What users hate in creative, journal, zine and print apps — ✅ VERIFIED (counts 🟨)

Six sweeps (app-store reviews, forums and HN, dark-pattern literature, home and zine printing, Android UX,
asset libraries and AI) plus a critic pass. Counts are distinct sources, merged roughly.

- **Losing work comes first** (about 30 sources): *"it is so frustrating to have your day's work systematically
  deleted"* ([Phonto reviews](https://apps.apple.com/us/app/phonto-text-on-photos/id438429273?see-all=reviews)).
  Offline apps also lose work through uninstall, a page that slows as it fills, and a delete whose undo has gone.
- **Printing is where zine makers suffer most** (about 55): page order, fit-to-page shrink, hidden margins,
  upside-down backs, A4 against Letter ([Adobe forum](https://community.adobe.com/questions-15/disable-scale-to-fit-8456)).
- **Watermarks and branding on the work** (about 10): *"there is always a tacky watermark"*
  ([Frames reviews](https://justuseapp.com/en/app/536422900/frames-picture-collage-maker/reviews)). A printed
  "made with" page is the same thing by another name.
- **Bloat and redesigns that change saved work** (about 40): *"far too bloated… I dont need 90%"*
  ([Day One forum](https://forums.dayoneapp.com/forums/topic/feedback-from-a-13-year-user-app-complexity-workflow-changes-and-pricing-conc/));
  *"Please roll back the changes"* ([Snapseed reviews](https://apps.apple.com/us/app/snapseed/id439438619?see-all=reviews)).
- Also: generative AI pushed in, assets of unknown provenance, account walls, money mechanics, nagging and
  tours. The resulting Never list is in [ZINE-DIRECTION §8](design/ZINE-DIRECTION.md#never).
- ⚠️ Caveats: Reddit and Google Play were unreachable, so the evidence leans to iOS; comics, sticker-maker and
  print-shop apps were not swept.

### R32.2 Delivering a larger Art cabinet — ✅ VERIFIED (sizes 🟨)

- ✅ Google Drive cannot host it: *"Using Drive as a replacement for a large-scale CDN is not allowed"*
  ([Drive API terms](https://developers.google.com/drive/api/terms)), and an API key in an APK is shared by every
  install ([limits](https://developers.google.com/workspace/drive/api/guides/limits)). The network is closed by
  Amendment 3 in any case.
- ✅ A placed piece stores only its id, and an unknown id draws nothing (`SupplyCatalog.kt`), so a renamed or
  removed id, or a backup restored onto an older build, prints with the Art silently missing. Hence: ids are
  only ever added, held by a test.
- 🟨 Size: the beta.7 APK is 15.6 MB; path data for 5,000 pieces would add about 2.5 MB, but Kotlin source hits
  the JVM's 64 KB limit on one static initialiser: one compiled build measured about 480 bytes per piece, near
  135 pieces (an earlier estimate said 200). Raster pieces would break single-ink tinting.
- ✅ A maker's own file on Drive is reachable through the system file picker, which needs no network permission
  in Zinely; it is blocked today by JPEG-only storage, not by the network.

### R32.3 Public-domain and open-licence sources, per item — ✅ VERIFIED

- ✅ [Smithsonian Open Access](https://www.si.edu/openaccess): items marked CC0 per item; commercial use, no
  attribution. Answers ADR-104's ⚠ for the Smithsonian, per item.
- ✅ [Rijksmuseum](https://data.rijksmuseum.nl): Public Domain Mark per image; the collection also holds CC BY
  items, which are excluded.
- ✅ [Cleveland Museum of Art Open Access](https://www.clevelandart.org/open-access): CC0 per item; its dates are
  sometimes wrong.
- ⚠️ Still per item only, or unresolved: the Met, Art Institute of Chicago, NGA, NYPL, Library of Congress,
  Flickr Commons, Wikimedia (a host, not a licensor), Public Domain Vectors (an aggregator), Openclipart (uploads
  may be traces). ❌ fromoldbooks.org.
- 🟦 Best fits: postmarks and fancy cancels for Stamps & marks; blackwork, vignettes, cartouches and corner
  ornaments for Cut paper or Stamps & marks, redrawn rather than traced. Covers with a hateful or tragic subject
  were left out on content grounds.

### R32.4 Showing paper colour without printing it — ✅ VERIFIED

- ✅ Professional tools preview the paper and never print it: Acrobat's Output Preview has "Simulate Paper
  Color" ([Adobe](https://helpx.adobe.com/acrobat/current/previewing-output-acrobat-pro.html); the page returned
  403 on re-check).
- ✅ Ink is translucent and white prints as bare paper
  ([Jukebox](https://support.jukeboxprint.com/en/articles/3189624-how-will-printed-colours-look-on-stocks-from-the-colour-collection));
  colour toner is transparent by design ([US7264910](https://patents.justia.com/patent/7264910)). Multiply is the
  usual screen model; its flaw, objects multiplying each other, is avoided by multiplying the flattened page once.
- ✅ The saved format ignores unknown keys but not unknown enum values (`JsonDocumentSerializer.kt`), so every
  named paper added later needs its own schema bump: choose the first set before v5.
- 🟦 No dark papers or brights until measured, with exposure and white balance locked.

### R32.5 A hidden signature in the PDF — ✅ VERIFIED for current Skia only

- ✅ Skia writes its own Info dictionary with `/Producer (Skia/PDF mNNN)` and a classic xref
  ([SkPDFDocument.cpp](https://skia.googlesource.com/skia/+/refs/heads/main/src/pdf/SkPDFDocument.cpp),
  [SkPDFMetadata.cpp](https://skia.googlesource.com/skia/+/refs/heads/main/src/pdf/SkPDFMetadata.cpp)).
- ✅ An incremental update only appends bytes, so it never removes Skia's string; a same-length rewrite while
  streaming does, and keeps the xref offsets valid.
- ⚠️ Android 7–13 ship older Skia; check on API 24 and the newest API before relying on it. Nothing printed can
  be hidden, so a printed mark was rejected.

### R32.6 Critique of the A34 redraw — 🟨 not independently reviewed

Measured on rendered sheets of A34: the tear still reads as a zig-zag; ink coverage runs from 6.8% (Saddle
stitch) to 90.5% (Rectangle); about ten pieces misread or read as a neighbour; Crop marks and Eyelet regressed
against the shipped set. Proposed (🟦): optical size per shape class; a coverage band of 25–65%; a fine weight
for printer's marks; a tear rewritten as a running walk; a curve-quality rule (no kinks, smooth joins); a
recognition check at tile size. Outside sets do not match the house style: Phosphor (MIT) is a puffy UI style.
These are the open numbers A39 sets ([SUPPLIES-SPEC §4.5](design/SUPPLIES-SPEC.md#one-world)).

## R33. Mixing and keeping your own ink (2026-10-22)

The evidence behind the ink-mixer lane (ADR-138, amendment A40). Research Agent pass, 2026-10-22. Findings come from
search summaries and one fetch; this section has **not** had its own reviewer, so spot-check a quote before reusing
it. A mixed ink is just another `ColorRgba` ([Document.kt:200](../core/model/src/main/kotlin/com/aritr/zinely/core/model/Document.kt)),
so nothing here touches the saved-document format.

### R33.1 How comparable apps let people mix and keep colours — ✅ VERIFIED (counts and scope as cited)

| App | How a colour is made | What is kept, and where |
|---|---|---|
| Procreate | Disc, Classic (square + H/S/B sliders), Harmony, Value (sliders plus numeric and hex), Palettes, History ([handbook](https://help.procreate.com/procreate/handbook/colors/colors-interface)) | History holds the **last 10** colours; a Default Palette shows under every tab; palettes are app-wide |
| Google Docs / Slides | Custom, then add: type hex or RGB, or use an eyedropper (Chrome/Edge only) ([help](https://support.google.com/docs/answer/13267978?hl=en)) | Custom colours are scoped to the **file**; a teacher reports they do not carry to another presentation ([post](https://alicekeeler.com/2012/10/12/color-picker-in-google-presentation/)) |
| GoodNotes | Colour wheel, Custom palette, or a HEX value ([support](https://support.goodnotes.com/hc/en-us/articles/17476182494479)) | Per **tool** (Pen, Pencil, Highlighter…): 3 presets shown, up to **12 slots**; sets do not sync across tools, so a user copies the hex by hand |
| Affinity | Colour panel adds to Swatches | **Document**, **application** and **system** palettes; add-current-colour, import and export ([help](https://affinity.help/photo2/en-US.lproj/pages/Panels/swatchesPanel.html)) |
| Canva | Hex field in the picker; eyedropper | Brand Kit colours, apparently a paid feature (third-party tutorials only; no official page found) |

- ✅ Every tool above offers a typed hex path. GoodNotes users keep asking for an eyedropper and re-type hex between
  tools, which suggests the *scope* of "kept" is the friction, not the picker
  ([GoodNotes feedback](https://feedback.goodnotes.com/forums/191274-customer-suggestions-for-goodnotes/suggestions/43309686-an-eyedropper-tool-so-that-you-can-copy-colours?page=3)).
- ✅ Kept colours are small sets: 10 (Procreate history), 12 (GoodNotes). None is unbounded in the apps checked.
- ⚠️ Adobe Fresco and Express, and Material You / a system colour picker, were not verified: the searches returned
  nothing usable, and I could not confirm an Android system colour-picker dialog.
- 🟦 App-wide beats per-document for a *maker's own* ink (Affinity's application palette, Procreate's palettes).
  Google's per-file scope is the cautionary case.

### R33.2 Hex entry conventions — ✅ VERIFIED (practice from forums and component docs; no style guide exists)

- Accept **with or without `#`**, and 3-digit shorthand (`#abc` is `#aabbcc`). Photoshop and Affinity accept no `#`;
  a Qt report asks for the same ([Subform](https://talk.subformapp.com/t/copy-paste-color-with/706.html),
  [Qt](https://bugreports-test.qt.io/browse/QTBUG-111742), [Drupal](https://www.drupal.org/node/3055344)).
- **Paste is where fixed-length fields break**: a 6-character limit silently drops the last digit of a pasted
  `#rrggbb` ([Discourse](https://meta.discourse.org/t/allow-pasting-a-hex-color-string-with-a-leading-in-a-color-text-input-theme-customization/134715)).
  Strip `#`, whitespace and hidden characters, then validate length. Illustrator users report copied hex failing
  ([Adobe community](https://community.adobe.com/t5/illustrator-discussions/color-picker-not-wanting-to-validate-and-apply-correct-hex-color/m-p/9496436)).
- Case-insensitive on input, **one normalised form stored** (six characters, upper case)
  ([Semrush](https://developer.semrush.com/intergalactic/components/color-picker/color-picker)).
- Show the swatch beside the text; show an inline invalid state when the text cannot be read as a colour
  ([Verbb](https://docs.verbb.io/plugin-kit/web/components/color-input)).
- 🟦 No 8-digit (alpha) hex: inks are opaque.

### R33.3 Accessibility of a colour picker — ✅ VERIFIED (platform facts) / ⚠️ (colour naming has no official source)

- ✅ A clickable is at least **48dp x 48dp** ([Android codelab](https://developer.android.com/codelabs/jetpack-compose-accessibility));
  a swatch is padded to it.
- ✅ A purely visual swatch needs a `contentDescription` of its colour; decorative images take `null` (same codelab).
- ✅ For a custom slider, Compose documents `progressBarRangeInfo` (value, range, **step**) with `setProgress`
  ([Compose semantics](https://developer.android.com/develop/ui/compose/semantics),
  [setProgress](https://composables.com/docs/androidx.compose.ui/ui/functions/setProgress)).
- ✅ The W3C slider pattern: arrow keys one step, Page Up/Down several (10 in its example), Home/End to the ends
  ([APG colour viewer](https://www.w3.org/WAI/ARIA/apg/example-index/slider/slider-color-viewer.html)).
- ✅ WCAG 1.4.11: slider thumb and track boundaries need **3:1** against their surroundings
  ([Adobe Spectrum](https://spectrum.adobe.com/page/color-slider)).
- ⚠️ A community write-up (May 2026, cites a Google issue; **not checked against the tracker**) says a node value
  changing at frame rate can freeze TalkBack; throttle what the semantics tree sees while dragging
  ([gist](https://gist.github.com/Digipom/d1dc1aef7623d8f58ff6b57cb6935d5b)).
- ⚠️ I found **no** official guidance on naming colours for TalkBack and none for Material colour-picker semantics.
- 🟦 A drag on a 2-D square cannot be swiped. The accessible path is the **hex field**, plus two adjustable strips
  with `setProgress` steps and a spoken colour name. Large text: the sheet scrolls; the hex field and Done never
  clip at 200% font scale.

### R33.4 Printed legibility of a pale ink — ✅ VERIFIED (the measure) / 🟨 ASSUMPTION (what printers do)

- ✅ WCAG 1.4.3: **4.5:1** for text, **3:1** for large text (18 pt, or 14 pt bold); ratio is `(L1+0.05)/(L2+0.05)`
  ([W3C](https://www.w3.org/TR/WCAG22/#dfn-relative-luminance);
  [Understanding 1.4.3](https://w3c.github.io/wcag/understanding/contrast-minimum)). Values are not rounded: 4.499
  fails. The luminance constants were not on the fetched page; the repo already implements them in
  `core/ui/src/test/.../theme/WcagContrast.kt:21-41` (sRGB, 0.03928 knee, 0.2126/0.7152/0.0722), a test-source helper.
- ✅ Washed-out home print is a common report: draft mode gives lighter output, and low or dry ink or a dirty head
  lightens colour ([Canon community](https://community.usa.canon.com/t5/Desktop-Inkjet-Printers/washed-out-color/td-p/301546));
  white prints as bare paper (R32.4).
- 🟨 Dot gain, toner density and ICC behaviour: searches found nothing citable. Treat on-screen contrast as the
  **best case**: a screen pass is a floor, not a guarantee.
- Worked values, ink on white (repo formula): #767676 4.54, #808080 3.95, #949494 **3.03**, #B0B0B0 2.17,
  #E8A0A0 2.11, #9ACD32 1.88, #F2C14E 1.68, #FFD60A 1.41. On the screen page `#F4EFE6`
  ([ZinelyColors.kt:176](../core/ui/src/main/kotlin/com/aritr/zinely/ui/theme/ZinelyColors.kt),
  [Theme.kt:155](../core/ui/src/main/kotlin/com/aritr/zinely/ui/theme/Theme.kt)) the same inks score about 0.4
  lower (#949494 is 2.65), so the screen page is the wrong thing to measure.
- **Which paper to measure against.** A page with no fill is "transparent / paper-white"
  ([Document.kt:215](../core/model/src/main/kotlin/com/aritr/zinely/core/model/Document.kt)); `ColorRgba.WHITE` is
  `Document.kt:208`; paper colour is shown on screen only (R32.4, [ADR-137](DECISIONS.md#adr-137)). Printed ink lands
  on **white**: measure against `#FFFFFF`.
- Precedent: [D-028](design/V2-SPEC-DEFECTS.md) deliberately imposes **no contrast floor** on in-page text ink.
  A *warning* keeps that ruling; a block would reverse it.

### R33.5 Where "My inks" lives — ✅ VERIFIED (guidance and repo pattern)

- ✅ Google: Preferences DataStore for small key-value settings, Proto DataStore for a typed schema, Room for partial
  updates, referential integrity or large or complex data
  ([Android blog](https://android-developers.googleblog.com/2020/09/prefer-storing-data-with-jetpack.html),
  [Proto codelab](https://developer.android.com/codelabs/android-proto-datastore)).
- ✅ The repo already keeps small app-wide settings this way: `DataStorePreferredPaperStore`
  (`data-android/src/main/kotlin/com/aritr/zinely/data/android/prefs/DataStorePreferredPaperStore.kt:15`), behind a
  `Flow`-returning interface, wired in `PreferencesModule.kt`.

### R33.6 Zinely application — 🟦 RECOMMENDATION

| Decision | Recommendation | Why |
|---|---|---|
| "My inks" cap | **8** | Under Procreate's 10 and GoodNotes' 12; one tray row; when full say so and ask which to remove, never drop one silently |
| Scope | **App-wide**, offered in every zine | R33.1; a used ink is already in that zine's own document, so the zine never depends on the list |
| Order | Newest first; reusing moves it to the front | Procreate history behaviour |
| Hex input | `#` optional, 3 or 6 digits, any case, whitespace and zero-width characters stripped, **no alpha**; stored `RRGGBB` upper case; invalid shows inline text, never a toast | R33.2 |
| Warning threshold | contrast against `#FFFFFF` **below 3.0:1** (about luminance 0.30, lighter than `#949494`) | R33.4; 3:1 not 4.5 because zine text is mostly large and marks are graphics |
| Warning behaviour | One gentle line under the preview ("Pale ink may print faintly"); **never blocks** Done or Save; no second stage | D-028; owner: warn, never block |
| Measure on | White, not the screen page or a paper tint | R32.4, R33.4 |
| Slider steps | Hue 5 degrees (72 stops); lightness 5% (20 stops); large step 5x; each change speaks the colour name; announcements throttled while dragging | R33.3 |
| Touch targets | Swatches and Done 48dp minimum | R33.3 |

**Naming (no network).** A pure `ColorRgba -> String` in `core/model` or `core/copy`, table-driven: convert to HSL;
if saturation is below about 10% name by lightness only ("black", "dark grey", "grey", "light grey", "white");
otherwise pick a hue word from about 12 sectors (red, orange, yellow, lime, green, teal, cyan, blue, indigo, purple,
pink, plus brown for dark orange or red) and prefix "dark" or "light" by lightness band; low-to-mid saturation gives
forms like "dark green-grey". 🟨 This is a design, not tested with TalkBack users. Also speak the hex for the exact
case ("Green-grey, hex 5F7A6E"). Reuse a named ink's own name when the colour matches it exactly. Pin the sector
edges in a unit test.

**Storage.** **Preferences DataStore**, one string key holding at most 8 `RRGGBB` values joined by commas, behind a
`MyInksStore` interface exposing `Flow<List<ColorRgba>>`, following `DataStorePreferredPaperStore`. Reasons: tiny,
no queries or relations (Room's cases in R33.5), same pattern as the repo, and no document change (v3 stays). Room is
rejected: a migration, a DAO and a table for 8 strings. 🟨 Open: whether `ZineLibraryBackupManifest` should carry
preferences; check before deciding that a restore brings "My inks" back.

**Not now.** No screen-wide eyedropper, no R/G/B boxes, no alpha, no harmony suggestions (Procreate's Harmony tab is
where a picker becomes a palette tool). Revisit the eyedropper only if makers ask to match photo colours.

### R33.7 Saving inks together: palettes in other tools (2026-10-22)

Question: how do established tools let people save and reuse named colour combinations, and does a palette
*reference* its colours or *copy* them? Sources are vendor help pages fetched or surfaced by search on 2026-10-22.
Where a page did not say, this is stated; nothing below is inferred silently.

| Tool | Create | Name / duplicate names | Edit, duplicate, delete, reorder | Cap | Reference or copy |
|---|---|---|---|---|---|
| **Illustrator** | Select swatches, then *New Swatch Group*; or *Selected Artwork* builds a group from the artwork's unique colours; a name is typed in the dialog ([Adobe: group swatches](https://helpx.adobe.com/vn_vi/illustrator/desktop/manage-colors/use-swatches/group-swatches.html), via search snippet) | Typed in the dialog; duplicate-name handling not stated | Recolor Artwork's *Swatch Groups* list edits, deletes and creates groups; *Delete Swatch Group* removes the group ([Adobe: Recolor options](https://helpx.adobe.com/illustrator/desktop/manage-colors/learn-color-basics/edit-colors-dialog-overview.html)) | Not stated | A group is a folder of the panel's swatches. *Convert Process to Global* turns colours into linked global swatches, so editing one updates the artwork. ✅ VERIFIED for global; effect of deleting a swatch that sits in a group ⚠️ not documented in anything reachable (Adobe's page returned 403 to a direct fetch) |
| **Photoshop** | Groups are folders in the Swatches panel; no official page found for the create flow | Not found | Community answer: no "merge groups"; people drag swatches into a new group and delete the emptied ones ([Adobe community](https://community.adobe.com/t5/photoshop-ecosystem-discussions/photoshop-swatch-group-gt-how-to-save-to-quot-my-library-quot/m-p/14643041/highlight/true)) | Not found | Painting with a swatch **copies** the colour into the foreground; nothing in the document refers back to the swatch 🟨 (general behaviour, no page found) |
| **Procreate** | Palettes tab, **+**; starts empty, named "Untitled", becomes the active palette | Tap the title to rename; no duplicate-name rule stated, the copy from Duplicate is appended at the bottom | Three-dot menu: Duplicate, Delete (confirm; "cannot be restored"), Set as active; press-and-hold a title and drag to reorder; a full palette's swatch is replaced by press-and-hold then *Set* ([Procreate handbook](https://help.procreate.com/procreate/handbook/colors/colors-palettes)) | A row holds 10 swatches (compact) or 3 (cards); the handbook states **no total cap**; a third-party page says 30, unconfirmed | Tapping a swatch sets the current colour (a **copy**); drag onto artwork flood-fills. Deleting a palette cannot touch artwork, because artwork holds pixels |
| **Figma** | Colour *styles* and *variables* are created from a selection or the panel | Names are paths (`brand/primary`); same-name handling not read | Edit, delete, detach in the properties panel | Not stated | **Reference.** "Figma will automatically apply those changes to any objects using that style" ([Figma: Styles](https://help.figma.com/hc/en-us/articles/360039238753-Styles-in-Figma)). *Detach* keeps the object's existing colour as a plain value ([Figma help via search](https://figma-signup.helpjuice.com/use-libraries/apply-styles-to-layers-and-objects)). What deletion does to users of a deleted style: the page fetched did not say; ⚠️ not verified |
| **Canva** | Brand Kit, Colors, add-to-category, *Add palette*; add colours with *Add new* | Click the palette's name to rename | More-options menu deletes; **the last palette cannot be deleted** ("at least one palette") | Free: 1 palette of 3 colours; Pro and up: 100 palettes of 100 colours (from the China-region help page only) | Applying is by colour pick or a *colour theme* that recolours background, elements and text together; themes cannot be authored from scratch ([Canva: colour palettes](https://www.canva.com/help/article/color-palettes/), [themes](https://www.canva.com/help/color-themes/)). Reference vs copy not stated |
| **Adobe Express** | *Brands*, *Create brand*, name it; add colours by picker or hex/RGB; *Add Color palette* | Brand named at creation | Each tile has a three-dot menu: edit, duplicate, delete ([Adobe Express: brand kit](https://helpx.adobe.com/ee/express/how-to/create-brand-kit.html)) | Not stated | Applied from the colour picker in the editor; no documented link back |

**What generalises.**

- ✅ VERIFIED (Procreate, Canva, Express): **create = a "+" that makes an empty, auto-named, immediately active item; rename is by tapping the name in place.** Nobody asks for a name before the colours exist.
- ✅ VERIFIED: **delete is confirmed and not undoable** in Procreate; Canva refuses to delete the last one rather than leave none.
- ✅ VERIFIED: no tool found enforces unique names; none documents a duplicate-name error. Duplicate appends a copy at the end.
- ✅ VERIFIED: **applying a palette colour to a target is "set the one colour"**. The palette is a picker, not a bundle that follows the target. Only Canva themes and Figma styles/variables apply many colours as one, and those are design-system features with a link.
- ✅ VERIFIED: **linked semantics exist only where the document keeps the link** (Figma styles, Illustrator global swatches). Raster-style tools (Procreate, Photoshop) copy the value, so deleting a palette is harmless to finished work.
- ⚠️ NOT VERIFIED: what Figma or Illustrator do to the *users* of a deleted style or swatch. Do not cite them for "delete detaches and keeps the value".

### R33.8 Zinely application (palettes) — 🟦 RECOMMENDATION

- **Copy, not reference.** A zine already stores each used ink's own value in its document (R33.6), so a saved combination is a list of `RRGGBB` values. Deleting a combination or one of its inks never changes any zine. That is Procreate's semantics, and it avoids the unverified delete-while-in-use case entirely.
- **Create:** a "Save these inks" action that makes the combination already named ("Untitled", or the first ink's colour name), with the name field focused for rename in place. No name-first dialog.
- **Names:** allow duplicates (the market norm); show the swatch strip beside the name so two "Sunset" rows stay tellable. Trim whitespace; empty falls back to "Untitled".
- **Controls:** rename, duplicate (copy appended at the end), delete with a confirm that says it cannot be undone, and reorder. Never delete the last one silently; a zero-state is fine for Zinely, so Canva's last-one rule is not needed.
- **Cap:** pick a small, stated number and say so when full, asking which to remove. Combinations hold at most the "My inks" cap of 8 colours. A combination cap is a product call; Procreate states none and Canva's 100 is a paid-tier figure, so neither is a precedent for a free offline app.
- **Apply to a target that holds one colour:** do what every tool does, set the single colour. Offer a combination as a row of tappable swatches, each applying one ink; do not recolour a whole page in one tap (that is Canva's theme, a different and larger feature).

## R34. The A39 Art picks: gate measurements, the Never #19 dry run and the licences (2026-10-22)

The evidence behind [ADR-139](DECISIONS.md#adr-139) and `v21-bench.html` A39. Measured on 2026-10-22 from the
owner's 125 picks (`.claude/handoffs/2026-10-22-a39-picks.txt`, also verbatim in A39) and the candidate sheet's
data (`zinely-archive/2026-10-15-wave2/art-a39/candidates.json`, every path, gate note and licence). Every pick was
matched by script to exactly one record. The measuring scripts are an independent copy of the sheet's own verifier
(`art-a39/qa/verify.py`): each outline is rasterised at 20 px per unit with the even-odd rule; holes are paper
regions a flood fill from the border does not reach; thin ink and narrow gaps are what a morphological opening
of radius 1 unit (0.75 for printer's marks at the fine weight) removes. An independent review of the plan re-derived the counts, the
holes and the ink boxes with a separate parser at 12 px per unit and found the same. The scripts lived in the
session scratchpad and are not in the repository; the method above is what reproduces them.

### R34.1 Counts — ✅ VERIFIED

- **125 picks**: 38 A39, 30 A34, 17 shipped, 40 open-source. Per family: Tape & fixings 24, Stamps & marks 38, Cut
  paper 44, Cut shapes 19.
- **124 offered outlines**: the A34 and A39 Circles are one drawing (R34.4), so the A39 Circle holds
  `shape.circle` and the A34 Circle gets no id. Cut shapes offers 18.
- **34 shipped ids remain, 90 are added.** 17 shipped drawings are kept on their ids; 17 ids take a picked
  redraw (R34.2). Every final id matches `DefaultDocumentValidator.SUPPLY_ID` (`^[a-z]+\.[a-z]+$`), none repeats,
  and every display name is unique.

### R34.2 Never #19 dry run and the transform per redrawn id — ✅ VERIFIED

The test of [ADR-136](DECISIONS.md#adr-136) decision 3: the new outline's ink box (on-curve points with every cubic
flattened, inside the unit square) must sit inside the shipped outline's ink box. The rule tried (see
[ADR-139](DECISIONS.md#adr-139) decision 2): A39 first, else A34; a drawing qualifies if a move and/or a uniform scale of at least 0.90 fits it,
and the scaled drawing does not lose more than 1 square unit to G2 (the verifier re-run after the fit; R34.3).
The scale is about the drawing's own ink-box centre, then the smallest move that fits; coordinates are rounded to
two decimals and the box re-checked after rounding. Boxes are [left, top, right, bottom] in units of the 24 box.

| id | shipped ink box | picked redraws tried | holder | transform | final ink box |
|---|---|---|---|---|---|
| `tape.torn` | [0, 0, 24, 24] | A39 needs ×1, G2 Δ +0/+0; A34 needs ×1, G2 Δ +0/+0 | **A39** | none | [1.3, 0.5, 22.78, 23.5] |
| `fix.corner` | [0, 0, 24, 24] | A39 needs ×1, G2 Δ +0/+0; A34 needs ×1, G2 Δ +0/+0 | **A39** | none | [0.5, 0.5, 21.4, 21.4] |
| `fix.staple` | [0, 7.2, 24, 24] | A39 needs ×1, G2 Δ +0/+0 | **A39** | move +0, +0.95 | [1, 7.2, 23, 18.95] |
| `fix.clip` | [1.44, 0, 21.12, 24] | A39 needs ×0.857 ✗; A34 needs ×1, G2 Δ +0/+0 | **A34** | none | [3.25, 0.5, 20.75, 23.5] |
| `tape.masking` | [0, 3, 24, 21] | A39 needs ×0.947, G2 Δ +0.03/+0.01; A34 needs ×0.94, G2 Δ +0.02/+0.02 | **A39** | ×0.947 | [1.39, 3, 22.48, 21] |
| `mark.asterisk` | [0, 0.59, 24, 23.41] | A39 needs ×1, G2 Δ +0.08/+0 | **A39** | move +0, +0.04 | [0.7, 0.59, 23.3, 22.08] |
| `mark.arrow` | [0, 1.44, 24, 22.56] | A39 needs ×1, G2 Δ +0/+0; A34 needs ×1, G2 Δ +0/+0 | **A39** | none | [0.5, 6.29, 23.42, 17.71] |
| `mark.hand` | [0, 5, 24, 18] | A39 needs ×0.788 ✗; A34 needs ×0.965, G2 Δ -0.05/-0.05 | **A34** | ×0.965, move +0, -1.835 | [0.89, 5, 22.51, 18] |
| `mark.bar` | [0, 4, 24, 20] | A39 needs ×0.952, G2 Δ +63.25/+1.02 ✗; A34 needs ×1, G2 Δ +0/+0 | **A34** | none | [0.5, 5, 23.5, 19] |
| `paper.strip` | [0, 3.84, 24, 20.16] | A39 needs ×1, G2 Δ +0/+0; A34 needs ×1, G2 Δ +0/+0 | **A39** | none | [0.5, 5.14, 23.5, 19.21] |
| `paper.tag` | [0, 0, 24, 24] | A39 needs ×1, G2 Δ +0/+0; A34 needs ×1, G2 Δ +0/+0 | **A39** | none | [1.5, 2, 22.5, 22.6] |
| `paper.underline` | [0, 9.84, 24, 16.32] | A39 needs ×0.919, G2 Δ -0.05/+0.01; A34 needs ×1, G2 Δ +0/+0 | **A39** | ×0.919, move +0, +0.443 | [1.89, 9.84, 22.03, 16.32] |
| `shape.rect` | [0, 0, 24, 24] | A39 needs ×1, G2 Δ +0/+0; A34 needs ×1, G2 Δ +0/+0 | **A34** (ruling) | none | [0.5, 0.5, 23.5, 23.5] |
| `shape.circle` | [0, 0, 24, 24] | A39 needs ×1, G2 Δ +0/+0; A34 needs ×1, G2 Δ +0/+0 | **A39** | none | [0.75, 0.75, 23.25, 23.25] |
| `shape.triangle` | [0, 0, 24, 24] | A39 needs ×1, G2 Δ +0/+0; A34 needs ×1, G2 Δ +0/+0 | **A39** | none | [0.6, 1.18, 23.4, 21.2] |
| `shape.rule` | [0, 9.6, 24, 14.4] | A34 needs ×0.97, G2 Δ +0/+0 | **A34** | ×0.969, move +0, -0.023 | [0.86, 9.6, 23.14, 14.4] |
| `shape.ring` | [0, 0, 24, 24] | A39 needs ×1, G2 Δ +0/+0; A34 needs ×1, G2 Δ +0/+0 | **A39** | none | [0.75, 0.75, 23.25, 23.25] |

- **All 17 pass**, so no shipped id is left without a drawing and nothing is retired.
- `shape.rect` goes to A34 by ruling although A39's passes: A39's is a 21 × 14.9 landscape, so every saved
  full-page panel would shrink to about 62% of its height.
- `mark.bar` is the one place the G2 re-run decides. A39's tone-patch bar fits at ×0.952, but its rails are 1.5
  and its tint dots 2.0, so the shrink puts both under the minimum (raster thin ink 2.6 → 65.9 sq units). The A34
  bar fits untouched and holds the id; A39's is offered as `mark.barb` at full size
  ([D-130](design/V2-SPEC-DEFECTS.md#d-130)).
- `mark.hand` takes A34 at ×0.965 and `fix.clip` takes A34 untouched; A39's would need ×0.788 and ×0.857.
- `shape.rule` (A34, the only picked redraw of its subject) fits at ×0.969, which keeps its band at 4.8 high.

### R34.3 Gate measurements — ✅ VERIFIED (measured) · 🟦 RECOMMENDATION (the numbers written into §4.4)

**What was measured, per id** (table below): coverage (ink share of the 24 box, %), raster thin ink and narrow
gap (sq units narrower than G2's minimum; a point that tapers, which H2 allows, shows up here too, so a small
number is not a failure), and holes (count, total area in sq units). The class is the G3/G4 shape class.

**G3 classes and G4 bands.** Four classes cover the set. Measured coverage: line 4.0–30.5%, mark 22.5–58.1%,
frame (a band of paper round a hole) 30.8–62.7%, sheet 29.5–90.5%. Proposed bands, written into
[SUPPLIES-SPEC §4.4](design/SUPPLIES-SPEC.md#house-style): line 4–35%, mark 25–65%, frame 30–65%, sheet 40–85%.
Five pieces sit outside their band and are named exceptions: `shape.rect` 90.5% (a rectangle is a box-filling
panel, by ruling); `paper.deckle` 88.7% and `paper.stamp` 85.1% and `fix.pushpin` 22.5% (shipped drawings the owner
kept); `tape.plaster` 29.5% (an outline idiom the owner picked).

**G5 and G6** were measured by the sheet's own tooling and are read from each candidate's gate note in
`candidates.json`, not re-derived here: G5 is the lag-1 autocorrelation of a tear's offsets (at least 0, so never a
zig-zag); G6 is join angle, handle length and polyline curves. Known failures among the picks: `fix.sticker` G6
(4–14° kinks and micro-segments at both peel corners); `shape.flower` G2 (1.2 sq units of gap under 1.5 at the
petal cusps). At a minimum: `mark.scissors` (finger holes at 1.5), `tape.plaster` (2.5 dots, 1.5 gaps). G8 risk:
`shape.hex` (true geometry, no cut sign). These are [D-131](design/V2-SPEC-DEFECTS.md#d-131).

**G7 holes: 31 pieces.** The eight of A32 (`fix.corner`, `paper.window`, `mark.registration`, `fix.grommet`,
`paper.hole`, `shape.ring`, `paper.border`, `paper.round`) and 23 new ids. The full list: `fix.corner`, `fix.cornerb`, `fix.grommet`, `fix.grommetb`, `fix.grommetc`, `fix.needle`, `tape.plaster`, `fix.sticker`, `mark.registration`, `mark.registrationb`, `mark.registrationc`, `mark.scissors`, `mark.squiggle`, `paper.window`, `paper.windowb`, `paper.windowc`, `paper.cutwindow`, `paper.hole`, `paper.holeb`, `paper.holec`, `paper.holed`, `paper.border`, `paper.borderb`, `paper.borderc`, `paper.borderd`, `paper.round`, `paper.roundb`, `paper.roundc`, `paper.gifttag`, `shape.ring`, `shape.ringb`.
The recon's "34 holed picks" was an arithmetic slip; the review and this run both count 31. Eight A34 variants
(`fix.cornerb`, `fix.grommetb`, `mark.registrationb`, `paper.windowb`, `paper.holeb`, `paper.borderb`,
`paper.roundb`, `shape.ringb`) wind each hole the same way as the outer contour; reversing one contour (the inner
one, or for `fix.cornerb` the outer) makes even-odd equal non-zero (residual 0.0 sq units) with the fill unchanged,
and A39 carries them so. The four shipped holed drawings kept as picked still differ under non-zero; the renderer
fills even-odd, so this is harmless. The
smallest holes are the scissors' finger loops and the gift tag's eyelet; a tap there hits what lies beneath, as
[ADR-124](DECISIONS.md#adr-124) intends.

| id | source | class | coverage % | thin ink u² | narrow gap u² | holes (area u²) |
|---|---|---|---|---|---|---|
| `tape.torn` | A39 | sheet | 78 | 0.9 | 0.1 |  |
| `tape.tornb` | A34 | sheet | 79.9 | 3.95 | 1.01 |  |
| `fix.corner` | A39 | mark | 35.3 | 0.8 | 0.52 | 1 (57.59) |
| `fix.cornerb` | A34 | mark | 35.3 | 2.9 | 1.68 | 1 (58.86) |
| `fix.staple` | A39 | line | 17 | 0.14 | 0.23 |  |
| `fix.clip` | A34 | mark | 37.1 | 0.27 | 0.08 |  |
| `fix.clipb` | A39 | mark | 35.5 | 0.23 | 0.14 |  |
| `fix.clipc` | oss | mark | 36.9 | 0.21 | 0.12 |  |
| `tape.masking` | A39 | sheet | 58 | 1.12 | 0.04 |  |
| `tape.maskingb` | A34 | sheet | 66.2 | 2.22 | 0.3 |  |
| `fix.stitch` | shipped | line | 25.4 | 2.12 | 0 |  |
| `fix.stitchb` | A34 | line | 6.8 | 0.14 | 0 |  |
| `fix.stitchc` | A39 | line | 25.7 | 0.7 | 2.44 |  |
| `fix.grommet` | shipped | mark | 51.9 | 0.02 | 0.02 | 1 (154.9) |
| `fix.grommetb` | A34 | mark | 45 | 0.08 | 12.29 | 2 (156.92) |
| `fix.grommetc` | A39 | mark | 49.3 | 17.2 | 6.85 | 2 (114.52) |
| `fix.pushpin` | shipped | mark | 22.5 | 3.29 | 0.08 |  |
| `fix.pushpinb` | A39 | mark | 29.6 | 3.24 | 0.45 |  |
| `fix.pushpinc` | A39 | mark | 25.4 | 3.34 | 0.5 |  |
| `fix.pushpind` | oss | mark | 36.2 | 0.24 | 0.36 |  |
| `fix.pushpine` | oss | mark | 47.2 | 0.17 | 0.3 |  |
| `fix.needle` | oss | mark | 29.4 | 0.31 | 3.99 | 1 (12.23) |
| `tape.plaster` | oss | sheet | 29.5 | 0.55 | 3.88 | 1 (147.03) |
| `fix.sticker` | oss | mark | 30.4 | 0.11 | 2.94 | 2 (145.66) |
| `mark.asterisk` | A39 | mark | 30.5 | 7.87 | 0.31 |  |
| `mark.asteriskb` | oss | mark | 38.4 | 4.48 | 0.4 |  |
| `mark.asteriskc` | oss | mark | 26.3 | 0.32 | 2.54 |  |
| `mark.arrow` | A39 | line | 16.9 | 0.2 | 1.87 |  |
| `mark.arrowb` | A34 | line | 16 | 3.76 | 0.47 |  |
| `mark.arrowc` | A39 | mark | 36.2 | 2.83 | 0.32 |  |
| `mark.arrowd` | oss | line | 16.9 | 0.43 | 2.48 |  |
| `mark.arrowe` | oss | line | 17 | 0.15 | 1.59 |  |
| `mark.halftone` | shipped | mark | 29.9 | 3.99 | 0.24 |  |
| `mark.halftoneb` | A39 | mark | 25 | 1.49 | 0.13 |  |
| `mark.registration` | shipped | line | 15.3 | 50.19 | 0.96 | 1 (65.81) |
| `mark.registrationb` | A34 | line | 30.5 | 1.31 | 4.37 | 4 (31.11) |
| `mark.registrationc` | A39 | line | 22.6 | 41.95 | 3.4 | 4 (116.42) |
| `mark.hand` | A34 | mark | 31.6 | 1.4 | 0.82 |  |
| `mark.handb` | A39 | mark | 43.5 | 1.09 | 1.45 |  |
| `mark.handc` | oss | mark | 40.6 | 0.24 | 0.96 |  |
| `mark.crop` | shipped | line | 22.5 | 3.15 | 0.63 |  |
| `mark.cropb` | A34 | line | 13.1 | 5.04 | 0 |  |
| `mark.cropc` | A39 | line | 12.4 | 4.34 | 0 |  |
| `mark.bar` | A34 | mark | 42 | 2.52 | 0 |  |
| `mark.barb` | A39 | mark | 33.7 | 2.6 | 16.96 |  |
| `mark.scan` | shipped | mark | 50.6 | 3.13 | 0 |  |
| `mark.scanb` | A34 | mark | 31.8 | 7.12 | 0 |  |
| `mark.scanc` | A39 | mark | 29.3 | 3.92 | 0.05 |  |
| `mark.perf` | shipped | line | 11.4 | 3.15 | 0 |  |
| `mark.perfb` | A34 | line | 7.2 | 3.15 | 8.01 |  |
| `mark.perfc` | A39 | line | 4 | 3.25 | 0 |  |
| `mark.burst` | shipped | mark | 41.4 | 21.77 | 3.27 |  |
| `mark.burstb` | A34 | mark | 51.8 | 9.57 | 2.15 |  |
| `mark.burstc` | A39 | mark | 53.8 | 13.22 | 3.33 |  |
| `mark.burstd` | oss | mark | 33.1 | 15.23 | 1.56 |  |
| `mark.sparkle` | oss | mark | 33.8 | 7.57 | 0.07 |  |
| `mark.cancel` | oss | mark | 31.3 | 0.55 | 0.2 |  |
| `mark.heart` | oss | mark | 58.1 | 0.27 | 0.2 |  |
| `mark.tick` | oss | line | 14.1 | 0.18 | 0.2 |  |
| `mark.cross` | oss | line | 23.3 | 0.36 | 0.61 |  |
| `mark.scissors` | oss | line | 26.8 | 0.28 | 1.82 | 2 (8.84) |
| `mark.squiggle` | oss | line | 26.4 | 0.26 | 1.73 | 1 (52.93) |
| `paper.strip` | A39 | sheet | 47.9 | 2.04 | 0.43 |  |
| `paper.stripb` | A34 | sheet | 48.8 | 4.13 | 1.34 |  |
| `paper.stripc` | A39 | sheet | 41.2 | 1.25 | 0.27 |  |
| `paper.window` | shipped | frame | 48 | 1.06 | 0.63 | 1 (299.29) |
| `paper.windowb` | A34 | frame | 61.9 | 1.06 | 0.64 | 1 (165.78) |
| `paper.windowc` | A39 | frame | 57.6 | 0.97 | 0.63 | 1 (111.05) |
| `paper.cutwindow` | oss | frame | 35.6 | 0.05 | 0.61 | 1 (187.34) |
| `paper.tag` | A39 | sheet | 54.9 | 3.05 | 0.28 |  |
| `paper.tagb` | A34 | sheet | 65.1 | 3.42 | 0.26 |  |
| `paper.chat` | oss | sheet | 54.9 | 0.12 | 0.06 |  |
| `paper.underline` | A39 | line | 11.4 | 1.74 | 0.01 |  |
| `paper.underlineb` | A34 | line | 14 | 0.37 | 0.01 |  |
| `paper.stub` | shipped | sheet | 70.3 | 1.74 | 0.03 |  |
| `paper.stubb` | A34 | sheet | 56.7 | 1.48 | 0.12 |  |
| `paper.stubc` | A39 | sheet | 47 | 1.73 | 0.08 |  |
| `paper.ticket` | oss | sheet | 58.6 | 0.21 | 0.28 |  |
| `paper.stamp` | shipped | sheet | 85.1 | 5.49 | 2.7 |  |
| `paper.stampb` | A34 | sheet | 79 | 5.44 | 0.62 |  |
| `paper.stampc` | A39 | sheet | 61 | 3.13 | 0.49 |  |
| `paper.deckle` | shipped | sheet | 88.7 | 0.62 | 0.14 |  |
| `paper.deckleb` | A34 | sheet | 80.1 | 2 | 0.54 |  |
| `paper.decklec` | A39 | sheet | 60.3 | 0.6 | 0.13 |  |
| `paper.hole` | shipped | frame | 56.6 | 0.5 | 0.25 | 1 (102.6) |
| `paper.holeb` | A34 | frame | 46.1 | 0.91 | 0.58 | 1 (82) |
| `paper.holec` | A39 | frame | 58 | 1.17 | 0.31 | 1 (109.2) |
| `paper.holed` | A39 | frame | 44.1 | 1.53 | 1.2 | 1 (188.84) |
| `paper.dogear` | shipped | sheet | 67.6 | 0.86 | 0 |  |
| `paper.dogearb` | A34 | sheet | 75.8 | 4.22 | 0.16 |  |
| `paper.dogearc` | A39 | sheet | 55.7 | 3.84 | 13.24 |  |
| `paper.page` | oss | sheet | 58.3 | 2.99 | 0.03 |  |
| `paper.sticky` | oss | sheet | 64.8 | 2.91 | 0.02 |  |
| `paper.border` | shipped | frame | 30.8 | 14.6 | 0.6 | 1 (375.24) |
| `paper.borderb` | A34 | frame | 34.9 | 1.08 | 0.67 | 1 (310.23) |
| `paper.borderc` | A39 | frame | 34 | 0.87 | 1.02 | 1 (245.09) |
| `paper.borderd` | A39 | frame | 34.9 | 0.43 | 2.25 | 1 (226.5) |
| `paper.round` | shipped | frame | 62.7 | 1.06 | 0.07 | 1 (191.17) |
| `paper.roundb` | A34 | frame | 55.6 | 1.06 | 0.01 | 1 (198.66) |
| `paper.roundc` | A39 | frame | 50 | 0.97 | 0.02 | 1 (154.9) |
| `paper.balloon` | oss | sheet | 55 | 0.25 | 0.1 |  |
| `paper.thought` | oss | sheet | 54.3 | 0.29 | 0.47 |  |
| `paper.shout` | oss | sheet | 46.8 | 0.24 | 0.55 |  |
| `paper.bookmark` | oss | sheet | 57.6 | 0.14 | 0.13 |  |
| `paper.ribbon` | oss | sheet | 56.7 | 0.84 | 0.2 |  |
| `paper.gifttag` | oss | sheet | 56.6 | 0.09 | 0.06 | 1 (7.08) |
| `shape.rect` | A34 | sheet | 90.5 | 1.06 | 0 |  |
| `shape.rectb` | A39 | sheet | 54.4 | 0.97 | 0 |  |
| `shape.rectc` | oss | sheet | 67.7 | 0.26 | 0 |  |
| `shape.rectd` | oss | sheet | 51.4 | 0.3 | 0 |  |
| `shape.circle` | A39 | sheet | 69.2 | 0.03 | 0 |  |
| `shape.triangle` | A39 | sheet | 41.2 | 2.06 | 0 |  |
| `shape.triangleb` | A34 | sheet | 45.7 | 2.24 | 0 |  |
| `shape.rule` | A34 | line | 17.8 | 1.06 | 0 |  |
| `shape.ruleb` | oss | line | 10.2 | 0.2 | 0.03 |  |
| `shape.rulec` | oss | line | 10.5 | 0.14 | 0.07 |  |
| `shape.ring` | A39 | sheet | 47.8 | 0.03 | 0.02 | 1 (123.03) |
| `shape.ringb` | A34 | sheet | 48.8 | 0.03 | 0.04 | 1 (131.58) |
| `shape.blob` | oss | sheet | 67.1 | 0.07 | 0.03 |  |
| `shape.scallop` | oss | sheet | 60.4 | 0.21 | 0.2 |  |
| `shape.flower` | oss | sheet | 58.2 | 0.28 | 1.4 |  |
| `shape.hex` | oss | sheet | 59.9 | 0.06 | 0.01 |  |
| `shape.cloud` | oss | sheet | 42 | 0.07 | 0.1 |  |
| `shape.cloudb` | oss | sheet | 44 | 0.1 | 0.14 |  |

### R34.4 Variant similarity — ✅ VERIFIED (measure) · 🟨 ASSUMPTION (that it stands in for the eye)

Pairwise intersection-over-union of the picks of one subject, after stretching each ink box to the full square,
best over mirror x, mirror y and a half turn, at 240 px. A high score means scale or mirror nearly makes one from
the other (the §4.3.1 filter). For solid silhouettes it under-weights edge character (a torn or hand-cut edge), so
it is a review list, not a verdict.

- **≥ 0.99, the same drawing:** A34 / A39 Circle, 1.00. Collapsed ([ADR-139](DECISIONS.md#adr-139) decision 4).
- **0.95–0.98, kept as picked** (the owner's eye is the G8 ruling, confirmed at the first-time-user pass): A34 /
  A39 Rectangle 0.98, and with the two hand-cut Doodle rectangles 0.95–0.98; A34 / A39 Ring 0.98; A34 / A39
  Triangle 0.96.
- **0.86–0.94, the look-alike groups G8 checks:** Torn tape 0.94; Deckle edge ×3 0.92–0.94; Bookmark ribbon /
  Bookmark ribbon (brush) 0.94; Folded corner ×5 with Dog-eared page and Sticky note 0.86–0.94; Speech tag 0.93
  and with Speech balloon (square) 0.86; Round window ×3 0.91–0.93; Ticket stub shipped / A34 0.93 and with
  Admission ticket 0.86–0.88; Masking tape 0.92; Torn strip ×3 0.88–0.91; Postage stamp ×3 0.88–0.90; Cloud ×2
  0.90; Window frame 0.89; Snipped border A39 / Scalloped border 0.89.
- The sheet's own look-alike notes add pairs across subjects: Eyelet, Ring, Registration cross and Round window;
  Blob and Circle; Scalloped seal and Punched flower; Wavy rule, Squiggle and Marker underline; Admission ticket
  against tape, stamp and rectangle; the two slanted paper clips; the two tilted push pins.

### R34.5 Licences of the 40 imported pieces — ✅ VERIFIED (read on the pinned licence files)

Every set is on [ADR-104](DECISIONS.md#adr-104)'s permitted list (MIT, ISC, CC0-1.0); none puts an obligation on
the user's zine. Each item's file, URL at the pin and sha256 are in `art-a39/oss-src-*/LICENSE-RECORD.txt`.

| Set | Pieces (final ids) | SPDX | Holder, from the licence file at the pin | Pin |
|---|---|---|---|---|
| [Tabler Icons](https://github.com/tabler/tabler-icons/blob/bbed884d15354b5cebf2493371f20dc2d5e83eaf/LICENSE) | 9: `fix.clipc`, `tape.plaster`, `fix.sticker`, `mark.asteriskc`, `mark.cancel`, `mark.squiggle`, `shape.ruleb`, `shape.blob`, `shape.flower` | MIT | © 2020-2026 Paweł Kuna | v3.49.0 = `bbed884` |
| [Fluent Emoji, High Contrast](https://github.com/microsoft/fluentui-emoji/blob/1ffb34c752ecf5d402f04cfb4b392c77f57c54bc/LICENSE) | 6: `fix.needle`, `mark.burstd`, `mark.sparkle`, `paper.ticket`, `paper.thought`, `paper.shout` | MIT | © Microsoft Corporation | `1ffb34c` |
| [Doodle Icons by Oreo Design](https://github.com/oreo-design/doodle-icons/blob/38de0ac9fc7524870c8e1f18373cf56f32a5028b/LICENSE) | 9: `fix.pushpind`, `fix.pushpine`, `mark.arrowe`, `paper.chat`, `paper.page`, `paper.sticky`, `paper.bookmark`, `paper.gifttag`, `shape.cloudb` | MIT | © 2026 Nicole Tang | `38de0ac` |
| Doodle Icons by Khushmeen Sidhu ([creator](https://khushmeen.com/icons.html); [mirror](https://github.com/svatsa159/react-doodle-icons/tree/b51da0371e992b5d03e768fd1f570e8e9500c617)) | 13: `mark.asteriskb`, `mark.arrowd`, `mark.handc`, `mark.heart`, `mark.tick`, `mark.cross`, `mark.scissors`, `paper.cutwindow`, `paper.balloon`, `paper.ribbon`, `shape.rectc`, `shape.rectd`, `shape.cloud` | [CC0-1.0](https://creativecommons.org/publicdomain/zero/1.0/legalcode) (creator: "Free icons for commercial and personal use under CC0 license - no attribution required") | none required; the mirror's own MIT (© 2022 Srivatsa Rampalli) covers its React package code | mirror `b51da03` |
| [Phosphor Icons core](https://github.com/phosphor-icons/core/blob/v2.0.8/LICENSE) | 2: `shape.scallop`, `shape.hex` | MIT | © 2023 Phosphor Icons | v2.0.8 |
| [Lucide](https://github.com/lucide-icons/lucide/blob/1.57.0/LICENSE) | 1: `shape.rulec` (`waves-horizontal`) | ISC | © 2026 Lucide Icons and Contributors; the same file carries Feather's MIT notice (© 2013-present Cole Bemis) for the Feather-derived icons, and `waves-horizontal` is not one of them | 1.57.0 |

- ⚠️ **DISPUTED: Doodle Icons by Khushmeen Sidhu.** The creator's page says CC0. One redistributor relabels the
  set "MIT © Khushmeen Sidhu". The pieces come from the MIT-licensed mirror. Under either reading the obligation
  is a notice in the APK at most (ADR-104). The credit therefore names the creator under CC0 and ships the
  mirror's MIT text as a precaution; the diff against the creator's own release (a $0 Gumroad download behind an
  email) is owed before beta.8.
- 🟨 **ASSUMPTION: Oreo Design's names mirror Lucide.** If an Oreo glyph redraws a Lucide or Feather path, their
  ISC and MIT notices also apply (still APK-only). Lucide's licence text ships anyway, so the fix, if needed, is
  one credit line.
- Six pieces are Fluent **emoji** silhouettes in one ink. The licence is fine; the world-match question
  (SUPPLIES-SPEC §4.5 G9, "not … an emoji", and ADR-136's cut of "Fluent emoji stickers") is the owner's pick,
  recorded in ADR-139.

<a id="r36"></a>

## R36. Choosing several items in a list or grid (2026-10-26)

Asked before deciding [ADR-142](DECISIONS.md#adr-142) and freezing library A47
([`v21-library.html`](design/mockups/v21-library.html), rules A47.1–A47.13): how do Android apps let someone choose
several items, act on them, and leave, and how does a screen-reader user follow it? **Numbering:** R35 was reserved
for the W1 lane (wave-3 working rules, `.superpowers/wave3-scratch/RULES.md`) and was never written, which is why
this section is R36. Collected by the Research Agent on 2026-10-26. Two gaps in the reading: the Android Developers
blog post on a multiselect photo grid returned 403 and rests on a search-result summary only; m3.material.io
rendered empty in the fetcher, so Material's behaviour is cited from the
[MDC-Android](https://github.com/material-components/material-components-android) component docs instead.

Already settled elsewhere and not restated: several-at-once is a reading and photo pattern, not a creative-app one
([R22.2](#r222-creative-apps-move-a-work-into-one-place-several-at-once-is-a-reading-and-photo-pattern---verified));
deleting follows from what a container holds ([R22.1](#r221-a-container-either-holds-a-work-or-points-at-it-and-deleting-follows-from-that---verified));
every gesture has a menu twin ([R22.3](#r223-dragging-one-onto-another-is-the-phones-own-gesture-and-the-careful-apps-also-give-a-menu---verified),
[R22.8](#r228-zinely-application---recommendation)); a screen-reader action does not replace a visible route
([R28.2](#r282-the-tap-only-path-may-sit-one-step-away-the-elements-own-screen-reader-actions-do-not-replace-it---verified--for-the-inference))
and hiding controls has a cost ([R28.4](#r284-hiding-controls-has-a-measured-cost---verified)); snackbar versus a
must-see message, wording, and status messages spoken without moving focus
([R30.1](#r301-a-message-the-person-must-see-stays-until-they-dismiss-it-a-snackbar-by-default-does-not---verified),
[R30.3](#r303-how-to-word-it---verified), [R30.4](#r304-a-status-message-is-spoken-without-moving-focus---verified));
the editor's accessibility floor ([R10.2](#r102-requirements-the-real-editor-must-meet---verified---recommendation));
a state told by colour alone is not told ([R24.3](#r243-a-state-told-by-a-2211-fill-is-told-by-colour-alone---verified)).

### R36.1 A long press starts selection in Google's own apps — ✅ VERIFIED, with a 🟨 ASSUMPTION edge

- Google Photos Help ([Select multiple photos](https://support.google.com/photos/answer/6128858?co=GENIE.Platform%3DAndroid)):
  "Tap and hold a photo… You can select multiple photos".
- Material's checkable Card ([MDC-Android, Making cards checkable](https://github.com/material-components/material-components-android/blob/master/docs/components/Card.md#making-cards-checkable))
  toggles its checked state on a long click.
- The Android Developers tutorial on a photo grid with multiselect in Compose
  ([Medium, Android Developers](https://medium.com/androiddevelopers/create-a-photo-grid-with-multiselect-behavior-using-jetpack-compose-9a8d588a9b63))
  has a long press enter selection and a drag extend it. ⚠️ The page returned 403; this rests on a search-result
  summary, not the article's text.
- 🟨 **ASSUMPTION:** once selecting, a plain tap toggles an item instead of opening it. Google Photos and Files
  behave this way; no normative text saying so was found.

**Sources:** as linked above.

### R36.2 A hidden trigger needs a visible twin — ✅ VERIFIED

- NN/g, [Designing effective contextual menus](https://www.nngroup.com/articles/contextual-menus/): "There should
  always be an additional way" to reach what a hidden menu offers.
- NN/g, [Contextual swipe](https://www.nngroup.com/articles/contextual-swipe/): "most users won't discover these
  actions" when a gesture is the only route.
- A long press is such a hidden trigger, so selection needs a visible way in as well (the same rule as
  [R22.3](#r223-dragging-one-onto-another-is-the-phones-own-gesture-and-the-careful-apps-also-give-a-menu---verified)).

**Sources:** as linked above.

### R36.3 The selection bar: close, count, actions, and it ends when nothing is chosen — ✅ VERIFIED

- MDC-Android, [Top app bar — contextual action bar](https://github.com/material-components/material-components-android/blob/master/docs/components/TopAppBar.md#contextual-action-bar):
  "An app bar can transform into a contextual action bar, remaining active until an action is taken or it is
  dismissed"; it shows a close button, a title such as "1 selected", and the contextual actions.
- AOSP Files, [`ActionModeController.java`](https://android.googlesource.com/platform/packages/apps/DocumentsUI/+/refs/heads/main/src/com/android/documentsui/ActionModeController.java)
  (around lines 81–104): the title is a plural count (`R.plurals.elements_selected`), it is also set as the window
  title, the close control is relabelled "Cancel", and `mActionMode.finish()` is called when the selection becomes
  empty, so selection ends at zero.

**Sources:** as linked above.

### R36.4 Back leaves selection first — ✅ VERIFIED

- Android Developers, [Predictive back gesture](https://developer.android.com/guide/navigation/custom-back/predictive-back-gesture):
  a `BackHandler` / `PredictiveBackHandler` should be enabled only while it has something to handle, and the
  innermost enabled handler runs first. A selection-mode handler, enabled only while selecting, therefore takes Back
  before an outer one (such as an open folder's).

**Sources:** as linked above.

### R36.5 A chosen item is shown by more than colour — ✅ VERIFIED

- MDC-Android Card ([Making cards checkable](https://github.com/material-components/material-components-android/blob/master/docs/components/Card.md#making-cards-checkable)):
  a checked card shows a check-circle icon, changes its foreground colour, and its stroke changes from
  `colorOutline` to `colorSecondary`. The icon carries the state for anyone who cannot tell the colours apart
  ([R24.3](#r243-a-state-told-by-a-2211-fill-is-told-by-colour-alone---verified)).

**Sources:** as linked above.

### R36.6 What a screen reader needs — ✅ VERIFIED

- Compose [semantics](https://developer.android.com/develop/ui/compose/accessibility/semantics): toggleable and
  selectable semantics, and `stateDescription` for a custom state's words.
- Android [accessibility principles](https://developer.android.com/guide/topics/ui/accessibility/principles): label
  the long-click action (`onLongClickLabel`) so TalkBack says what a long press does.
- [API 36 diff, `android.view.View`](https://developer.android.com/sdk/api_diff/36/changes/android.view.View):
  `announceForAccessibility` is deprecated; occasional changes such as a count are told by a live region instead
  (as in [R30.4](#r304-a-status-message-is-spoken-without-moving-focus---verified)).

**Sources:** as linked above.

### R36.7 Acting on several at once: undo, not a dialog — ✅ VERIFIED, with a 🟨 ASSUMPTION edge

- NN/g, [Contextual swipe](https://www.nngroup.com/articles/contextual-swipe/): destructive actions should "support
  easy undo", and the undo should be "highly salient".
- MDC-Android [Snackbar](https://github.com/material-components/material-components-android/blob/master/docs/components/Snackbar.md):
  an Undo action on the snackbar, with notes on how it behaves under TalkBack.
- Google Photos Help ([Select multiple photos](https://support.google.com/photos/answer/6128858?co=GENIE.Platform%3DAndroid)):
  deleted photos go to the trash and stay there for 30 days.
- 🟨 **ASSUMPTION:** no published convention was found for what to tell someone when some items of a batch fail and
  the rest succeed.

**Sources:** as linked above.

### R36.8 Zinely application — 🟦 RECOMMENDATION

[ADR-142](DECISIONS.md#adr-142) owns the decision and A47 the design; this is the trail from the evidence to them.
Followed: long press enters with that zine chosen (R36.1); a visible way in, the sheet row "Select several"
(R36.2), not an overflow menu, because the zine sheet is already where a zine's actions live; a tap toggles while
selecting (R36.1, 🟨); a close control and the count "N selected" (R36.3), told by a polite live region rather than
an announcement call (R36.6); selection ends when the last zine is let go, as in AOSP Files (R36.3); Back leaves
selection before the folder (R36.4); a chosen zine shows a mark, not only a colour (R36.5); one Undo for the batch,
no confirm (R36.7). **Departs from M3:** the actions sit in the Shelf's dock as words, not as icons in a top bar,
because the Shelf has no app bar and its actions already live in the dock within thumb reach (ADR-142 ruling R2).
For a partial failure, where no convention was found (R36.7), ADR-142 uses skip-and-list: the rest stay done and one
message names those that came back.

### R36.9 Selected emphasis: the badge leads, plus one light device — ✅ VERIFIED (Material, WCAG), 🟨 ASSUMPTION (the rest)

Added 2026-10-29 for the lighter chosen ring (owner request; [ADR-142](DECISIONS.md#adr-142) ruling R9, A47.2 amendment).

- ✅ **VERIFIED.** MDC-Android Card ([checkable](https://github.com/material-components/material-components-android/blob/master/docs/components/Card.md)):
  a checked card shows a 24dp check-circle icon top-end and changes the stroke colour; the default stroke is 1dp.
  The reference pattern is a thin stroke plus an icon, not a heavy ring.
- ✅ **VERIFIED.** [WCAG 2.2 1.4.11 Non-text Contrast](https://www.w3.org/WAI/WCAG22/Understanding/non-text-contrast.html):
  the visual information needed to tell a component's state (the example is "selected") needs 3:1 against adjacent
  colours. The requirement is on the information that carries the state, not on every decoration, so the tick can
  carry it and the ring can be lighter.
- ✅ **VERIFIED (secondary sources).** Google Photos: a check circle at the top-left, which turns blue when chosen,
  and on the web the thumbnail shrinks inside a blue box
  ([Digital Camera World](https://digitalcameraworld.com/tutorials/how-to-select-all-photos-in-google-photos-download-everything-at-once),
  [Alphr](https://alphr.com/google-photos-how-to-select-all)).
- 🟨 **ASSUMPTION.** Apple Photos shows a blue check in the corner and no ring: the guides
  ([Trusted Reviews](https://www.trustedreviews.com/how-to/how-to-select-multiple-photos-on-iphone-4426041)) only
  confirm the check; the rest is recollection.
- 🟨 **ASSUMPTION.** Samsung Gallery (check circle plus a dim), Pinterest, Notion and Google Keep (a thin outline
  plus a check badge): searches returned nothing usable; do not cite them in an ADR.
- **Pattern across what is verified:** the check badge is the primary signal, and any extra emphasis is one light
  device (a thin stroke, a scale-down or a tint), never a heavy stroke on top of a badge. Zinely stacked four
  (ring, badge fill, halo, hard shadow); A47.2 now keeps one light ring beside the unchanged tick.

**Sources:** as linked above.

<a id="r37"></a>

## R37. Turning a piece by hand on a phone (2026-10-29)

Asked before deciding [ADR-143](DECISIONS.md#adr-143) and Bench A48
([`v21-bench.html`](design/mockups/v21-bench.html)): how do touch editors expose rotation, and what does
accessibility require of a dragged turn? Collected by the Research Agent on 2026-10-29 (a working report, not in the
repo); the sources below were read then and were not re-fetched for this entry.

### R37.1 The Bench once specified a turn handle — ✅ VERIFIED (repo)

The V2 IA lists "Rotate-handle drag | Rotate | visible rotate handle"
([`V2-BENCH-IA-INTERACTION.md`](design/V2-BENCH-IA-INTERACTION.md)), and the V1 mockups drew one
([`editor-mockups/editor.html`](design/editor-mockups/editor.html), `transform.html`,
`research-notes-editor.md`). The V2.1 freeze draws eight handles and no turn handle, while A33 still named one. The
same IA says two fingers act on the **page**; the build lets them turn and scale the selected piece (recorded in
ADR-143 decision 9, not reopened).

### R37.2 How touch editors expose rotation — ✅ VERIFIED where cited

| Product | What it does | Evidence |
|---|---|---|
| Canva | A rotate handle outside the selection border; an angle guide while tilting. Mobile snapping is not documented. ⚠️ Third-party guides disagree whether the handle sits above or below | [Canva Help: Flip and rotate elements](https://www.canva.com/en_gb/help/flip-and-rotate-variantb/) · [bloggingtips](https://bloggingtips.com/?p=115813) · [softwarehow](https://softwarehow.com/?p=24528) |
| Procreate Pocket | A rotation node above the selection, turning about the midpoint, with a live angle readout; a Rotate button turns 45°; snapping covers move and scale, not angle | [Transform interface](https://help.procreate.com/pocket/handbook/transform/transform-interface-gestures) · [Snapping](https://help.procreate.com/pocket/handbook/transform/snapping) |
| Procreate Pocket QuickShape | A second finger held while dragging rotates in 15° steps | [QuickShape](https://help.procreate.com/pocket/handbook/guides/quickshape) |
| Keynote for iPad | Two fingers start the turn, one finger can continue it, a guide shows the angle | [Keynote for iPad: rotate](https://help.apple.com/keynote/ipad/2.5/en.lproj/tand090a1c14.html) |
| Google Quickoffice / Slides (Android) | A round handle with a circular-arrow symbol, turning about the centre; exact angle in format options | [Google Quickoffice help](https://support.google.com/quickoffice/answer/2989693?hl=en) · secondary: [MobiSystems](https://support.mobisystems.com/hc/en-us/articles/21587578098205-Rotate-objects-in-Slides), [PhoneScoop](https://phonescoop.com/articles/article.php?a=20750) |
| Figma (desktop and iPad) | Desktop: drag just outside a corner, Shift snaps to 45°. iPad: the rotation field or a rotate-90° button | [Figma forum: rotate on iPad](https://forum.figma.com/t/rotate-an-object-on-ipad/51712) · [uxcel](https://app.uxcel.com/courses/figma-intro/layer-alignment-097/rotate-layers-9201) |
| Adobe Express | Snaps to 0/90/180/270 with no off switch; one iPad build had no rotate handle on graphics, reported by users as a bug (community sources, not official) | [snapping thread](https://community.adobe.com/t5/adobe-express-bugs/snapping/idi-p/14369447) · [missing handle](https://community.adobe.com/bug-reports-328/problems-rotating-objects-on-ipad-1634882) |
| Instagram Stories | Two fingers rotate and scale; a guide shows when an item is straight again. 🟨 Dated (2017) | [Refinery29, 2017](https://www.refinery29.com/en-us/2017/10/174941/instagram-stories-color-picker-alignment-tool) |

Common ground: every editor with a visible handle puts it detached from the box on the ring's axis and turns about
the centre; three show the angle while turning; several offer a fixed-step button; snapping to straight appears in
Adobe Express, Instagram and (with Shift) Figma desktop. 🟨 ASSUMPTION, no source found, so not used as evidence:
the Figma phone app's handle, Canva's snapping on mobile, and how other popular sticker and camera apps expose
rotation.

### R37.3 What accessibility requires — ✅ VERIFIED

- [WCAG 2.2 SC 2.5.1 Pointer Gestures](https://www.w3.org/WAI/WCAG22/Understanding/pointer-gestures.html) (A):
  a two-finger turn needs a single-pointer way to the same result. Arrange > Turn already gives it.
- [WCAG 2.2 SC 2.5.7 Dragging Movements](https://www.w3.org/WAI/WCAG22/Understanding/dragging-movements) (AA): a
  dragged handle needs a non-drag alternative for the **result**, not the path
  ([tabnav](https://tabnav.com/academy/wcag/success-criterion-2.5.7)). The 15° taps and Straighten reach every
  multiple of 15°; a free angle such as 7° is a drag only. 🟦 Acceptable: a free angle is an aesthetic choice.
- Touch targets at least 48 × 48dp with 8dp spacing
  ([Android Accessibility Help](https://support.google.com/accessibility/android/answer/7101858?hl=en);
  [M3 icon buttons](https://m3.material.io/components/icon-buttons/accessibility)).
- Element actions through `semantics { customActions }`
  ([Compose accessibility](https://developer.android.com/develop/ui/compose/quick-guides/content/video/accessibility-in-compose));
  the Bench already has Rotate clockwise / counterclockwise there.

### R37.4 Zinely application — 🟦 RECOMMENDATION

[ADR-143](DECISIONS.md#adr-143) owns the decision and A48 the design. Followed: a detached knob on the ring's axis,
turning about the centre (R37.2); a readout only while turning (Procreate, Keynote); snapping to straight (Adobe
Express, Instagram), on the knob only: the two-finger turn does not snap, so a deliberate small tilt stays
reachable and tilts already saved are never straightened by a touch (ADR-143 decision 2); Straighten as the one-tap way back to upright
beside the existing 15° arrows. **Departs:** no exact-angle field (🔭 later if asked), and the knob sits below the
piece rather than above (Procreate), so a finger on it does not hide the piece; it flips above only when below
does not fit.

**Sources:** as linked above.

<a id="r38"></a>

## R38. Cancel beside a committing button on a sheet (2026-10-29)

Asked before deciding [ADR-144](DECISIONS.md#adr-144) and freezing library A49: how do design systems draw a dismiss
beside a committing button, especially when the commit is disabled until valid? **Numbering:** R38 was reserved for this lane. Collected by the Research Agent on 2026-10-29; the in-repo precedent (the frozen
mockups' Cancels) is in the ADR, not here. Gaps: m3.material.io and NN/g did not render or return anything citable; the
Google Keep, Drive and Files dialogs were not retrieved and nothing here rests on them.

- ✅ VERIFIED: Material 3's `AlertDialog` takes a `confirmButton` and an optional `dismissButton`, and the reference sample
  fills both with `TextButton` ([AlertDialog](https://developer.android.com/reference/kotlin/androidx/compose/material3/AlertDialog.composable);
  [composables.com](https://composables.com/components/material-3-compose/alertdialog)). Both are text: the pair is symmetric.
  Zinely's pair (filled + text) is not an M3 pair, so "M3 uses text for Cancel" does not carry over.
- ✅ VERIFIED: `TextButton` is for lower-emphasis actions, outlined sits between text and filled
  ([TextButton](https://developer.android.com/reference/kotlin/androidx/compose/material3/TextButton.composable);
  [Material buttons](https://material.io/go/design-buttons)).
- ✅ VERIFIED: a text cancel beside a filled submit is documented where the submit is live, and the same pages advise against
  disabled buttons ([Cox Automotive Prism](https://prism.coxautoinc.com/docs/components/action-components/button/);
  [Scottish Government](https://designsystem.gov.scot/components/button)). Zinely keeps its disabled-until-valid commit
  (A28.10, with a hint line), so the mitigation is on the dismiss: give it a body.
- ✅ VERIFIED: an Android touch target is at least 48dp, with at least 8dp between targets
  ([Android accessibility](https://developer.android.com/guide/topics/ui/accessibility/apps)). Both options keep 48dp.
- 🟦 RECOMMENDATION: the surface-filled bordered sibling (ADR-144 decision 1), because every other frozen Cancel already
  is one.
