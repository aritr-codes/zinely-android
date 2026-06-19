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
