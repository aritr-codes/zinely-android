# Changelog

All notable changes to Zinely are documented here.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

> **Pre-1.0 policy.** Zinely is pre-release. While the major version is `0`, the
> minor version (`0.y`) is bumped per completed **vertical-slice milestone** and the
> patch (`0.y.z`) per fix/refinement within one. The public surface is not yet stable.
> **`1.0.0` ships when a first-time user can create *and* export a real zine** — i.e.
> the [MVP exit criteria](docs/ROADMAP.md#mvp--one-great-format-done-right) are met.
> Each released `0.y.0` maps to a build phase in the [roadmap](docs/ROADMAP.md).

## [Unreleased]

## [0.9.0-beta.4] — 2026-08-31 — Replace a photo without rebuilding the page

The fourth build for the beta cohort. `versionCode 6`, signed with the same release key as the earlier
beta builds, so it installs over an existing tester copy and keeps its zines.

### Added — Replace Photo · [D-038](docs/design/V2-SPEC-DEFECTS.md#d-038)

- Select a photo, choose **Replace**, and pick another image through Android's existing photo picker.
  Zinely uses the same decode and asset-storage path as Add Photo; there is no second import route.
- The replacement changes only the selected photo's source. Its element identity, position, size,
  rotation, crop, fit, layer, opacity, photocopier setting, flip state, and selection stay intact.
- One **Undo** restores the previous photo; **Redo** reapplies the replacement. Cancelling the picker,
  a decode/storage failure, or choosing the same stored image makes no document or history change.
- Art's visually identical **Replace** action remains a route to the Art cabinet, not the photo picker.

### Accessibility

- The two visible **Replace** captions keep the frozen design, while the platform accessibility tree
  now names them **Replace photo** and **Replace supply** so TalkBack does not announce two ambiguous
  buttons with different destinations.
- At 1.8× text on Samsung SM-A176B, the selected-photo action strip remains horizontally scrollable;
  Replace is reachable, enabled, clickable, and measures about 77 × 63 dp.

### Known limitations

- Android owns the system photo picker. Depending on the phone, choosing one image may add a separate
  **Preview** / **Done** confirmation before returning to Zinely.
- Replaced and deleted photo assets are retained. This protects other zines or elements that may share
  the same content-addressed asset, but app storage is not reclaimed yet.
- Font choice is still unavailable. Text outside the supported print scripts is kept and warned about,
  but it does not print yet.
- Zinely creates and saves the PDF; printing itself happens in the phone's PDF or print app.

## [0.9.0-beta.3] — 2026-08-30 — Make the editor lighter, clearer, and more dependable

The third build for the beta cohort. `versionCode 5`, signed with the same release key as earlier
beta builds, so it installs over an existing tester copy and keeps its zines.

This release includes the editor, accessibility, reliability, backup, rendering, and performance
work documented below. Highlights include persistent zine deletion, the warmer **About Zinely**
screen, clearer dark-mode editing controls, more compact Flip/Reframe surfaces, reliable page-panel
navigation, and reduced Art cold-open contention. Replace Photo is not part of this release.

### Added — nothing you type disappears without a word · [ADR-070](docs/DECISIONS.md#adr-070)

Zinely prints a defined set of scripts (Latin, Cyrillic, Greek). Type a character outside it — Bengali,
Tamil, an emoji — and until now it would quietly vanish when the page was drawn, with nothing to warn
you. That silent loss is fixed.

- **You're told, the moment it matters.** While you're editing, a calm, non-blocking note appears and
  **names the script** — "Bengali characters can't print yet…" — so a refusal is specific, never a
  vague shrug. It clears itself the instant you remove the character.
- **Your text is kept, always.** The character is **never deleted** — it stays in your zine, so it
  will simply print if that script is supported in a future version. Nothing you write is thrown away.
- **It stays honest as Zinely grows.** The note is built from what you actually typed, so when more
  scripts are added later it stops warning about them on its own — no stale message left behind.

This is permanent behaviour, not a stopgap: the promise is that **you never lose work silently**.
Bundling more script families is a separate, deliberate decision
([proposal](docs/proposals/expanded-script-support.md)), evaluated one family at a time.

### Removed — the shelf-thumbnail pipeline that nothing displayed · [ADR-069](docs/DECISIONS.md#adr-069)

**No visible change, and that is the point.** The app was rendering a page-1 thumbnail for every zine
— replay the page, encode a PNG, write it to the cache, read it back, decode it — **on every document
edit**, plus a decode per zine on **every cold start** and up to 24 bitmaps held in memory. Nothing on
screen ever showed the result: the shelf draws a generated riso cover per zine, and the field the
thumbnail landed in was read by no part of the interface.

- **What you get:** less battery and less storage used on every edit, and a faster cold start on a
  full shelf. The shelf looks exactly as it did — verified: all 65 screenshot goldens are
  byte-identical.
- **Your zines are untouched.** The deleted cache was derived and rebuildable, never your work. No
  file-format change, no migration, nothing to do.
- **Existing installs keep an inert `thumbnails` folder in the app's cache** — Android reclaims it
  under storage pressure, and "Clear cache" removes it. Nothing writes to it any more.

Closes **ship blocker #3** as written — nothing runs unread. Whether the shelf should one day show
your real page 1 instead of the generated cover is now an open product question with no
half-implementation waiting behind it.

### Added — V1 conformance guardrails (C1)

Merged to `main` (`a139fac`; completion record `ed4c46c`) on 2026-07-23. The first milestone of the
[V1 conformance programme](docs/ROADMAP.md#v1-conformance-programme-c0c10) — eleven milestones (C0–C10)
that migrate the shipped app onto the accepted V1 design corpus.

- **The regression net before the migration.** Editor golden coverage, a platform-`AccessibilityNodeInfo`
  assertion harness, a static token-discipline gate, CI wiring for two previously never-run test suites,
  and WCAG-AA contrast assertions — so that when later milestones start changing drawn values, a moved
  pixel or a broken semantic is loud.

**No user-visible change.** This is **test infrastructure only** — `git diff main -- '**/src/main/**'`
is empty; no behavioural or visual difference, no file-format change, offline/privacy invariants intact.
C1 is not *fully* closed until C0 (specification reconciliation) resolves two parked items. See
[the C1 milestone record](docs/reviews/C1-conformance-guardrails.md) and
[ADR-059](docs/DECISIONS.md#adr-059).

## [0.9.0-beta.2] — 2026-08-16 — Share a photo straight in, and photocopy it

The second build for the beta cohort. `versionCode 4`, signed with the same release key as
`beta.1`, so it installs **over** your existing copy and keeps your zines.

### Added — send a photo to Zinely from anywhere on your phone

Zinely now appears in your phone's own **Share** sheet. Find a photo in your gallery, your camera
roll, a chat — tap Share, choose Zinely, and it lands on the page you were working on. Share several
at once and they cascade down the page instead of stacking in one spot.

- **Nothing disappears quietly.** If a photo can't be read, you're told how many arrived and how many
  didn't — in a sentence, not a code.
- **It opens the zine you were in**, not a new empty one. That sounds obvious; it took a fix to be true
  (see below).

### Added — the photocopier

A per-photo filter that makes an image look the way a photo actually reproduces on a home printer or a
copy shop machine: broken into dots, high contrast, honest about being a copy. It's a toggle on a
selected photo, and it's reversible — your original is never altered.

### Fixed — a shared-in zine could become impossible to open again

Sharing a photo into Zinely opened a **second copy of the app in its own task**. The zine you shared
into was then unreachable: tapping the icon showed you the other copy. Photos were never lost, but the
work could look lost, which is the same thing from where you're sitting. Found by inspecting the
phone's own task records — no automated test can see them.

### Fixed — screen-reader labels for supplies

TalkBack read the internal name of each supply rather than its real one — *"Rect shape"*, *"Corner
fix"*. It now reads the actual names.

### Groundwork you can't see yet

Decorative supplies — tape, staples, torn paper, halftone marks — now exist in the file format and the
drawing engine, and four of the sixteen are drawn. **Nothing appears on a page yet**: the drawing
step that puts them on paper is the next piece of work. This is listed here because it changes the
saved file format, which the next section is about.

### ⚠ Known limitations — please read before reporting these

- **Zines saved in `beta.2` cannot be opened by `beta.1`.** The document format moved from v1 to v2.
  Upgrading is safe and your existing zines open normally; **going back is not**. If you uninstall or
  roll back to `beta.1`, anything you made or edited in `beta.2` will not open.
- **The photocopier has not been verified in print.** Its entire claim is about ink on paper, and no
  screen can test that. The dot size (150 dpi) is a provisional choice, and a home printer may render
  it too coarse or too fine. Please print one and say what you see — that feedback is the test.
- **The screen-reader listen pass has not been run.** Four specific questions are written down and
  unanswered, including whether an import summary is announced **twice** (once as a toast, once as an
  announcement). If you use TalkBack and hear something doubled or wrong, that's a known gap, not a
  surprise.
- **Supplies are not usable.** The Art sheet, the decor controls, and twelve of the sixteen outlines
  are not built. The verb is disabled deliberately.
- **The word "Ink" still means three things** in the app — a control, a colour, and a shade of grey —
  and a screen reader says all three identically.

## [0.9.0-beta.1] — 2026-07-22 — Read your zine

The first build handed to the named beta cohort, and the first signed with the real Zinely release
key. Cut from `main` at the UX-P0 merge; `versionCode 3`. Readiness assessed in
[the release readiness review](docs/reviews/2026-07-22-beta-release-readiness.md).

### Installing this build — please read first

- **This build cannot install over an older Zinely.** Every build up to `0.8.0` was signed with a
  throwaway debug key; this one is signed with the real release key, and Android refuses to replace
  an app with a differently-signed one (`INSTALL_FAILED_UPDATE_INCOMPATIBLE`).
- **So if you have an older Zinely: save anything you want to keep first** (open the zine →
  **Print & fold** → **Save PDF**, which writes a PDF into your Downloads), then uninstall, then
  install this one. **Uninstalling deletes your zines** — there is no backup or restore yet.
- **This is the last build that will ask you to do that.** Every build after this one installs
  straight over it and keeps your work.

### Added — Read your zine · [ADR-058](docs/DECISIONS.md#adr-058)

From the [beta UX review](docs/BETA-UX-REVIEW.md), whose one-sentence finding was: **you could not see your zine.**

- **"Preview" now opens your zine.** It lands on **Read** — your pages, one per screen, in reading order, with nothing else on them. Swipe to turn the page. Previously the only thing labelled "Preview" opened straight onto the printer's imposed sheet: eight numbered rectangles, four of them upside-down, and none of them showing your work. It was doing its job — explaining how a folded sheet works — but answering a question you had not asked yet.
- **The printing steps are now behind a button that says so.** **Print & fold** leads to the same three steps as before, unchanged: the sheet, the print recipe, and the fold guide.

### Fixed — the page, and the controls that act on it

- **A blank page's invitation is drawn on the page.** It was centred on the whole canvas rather than on the sheet, so on a portrait page it sat off the paper with its lines running off the screen. This is what looked like undo corrupting the layout — undo had emptied a page and revealed it.
- **The sheet and its contents are always drawn at the same size.** They could briefly disagree while you were dragging something or typing — most noticeably when the keyboard opened — leaving the paper in one place and your photos and words in another until you finished.
- **Reframe controls now show when they cannot do anything.** In "Whole photo" the whole photo is shown, so moving and zooming do nothing; in "Fill" you cannot zoom out past the point where the photo fills the frame, and a photo that already fills it edge to edge cannot be moved. Those buttons were lit and tappable and did nothing, which read as the app having frozen. They are now dimmed and unavailable — and if you are using a screen reader or a keyboard, they say why, and how to get back to adjusting.

### Added — Style your text · [ADR-055](docs/DECISIONS.md#adr-055)

Implemented as B1–B4 (style intent · Type bar · keyboard + haptics · parity fix and doc reconciliation), each batch independently reviewed. Signed off on 2026-07-21: the pixel-parity goldens were recorded on the pinned CI image and reviewed against the frozen design, and the accessibility pass was run on a physical phone against a release-signed build of this version — so [ADR-055](docs/DECISIONS.md#adr-055) is Accepted. (That pass ran on the 2026-07-21 assembly, `versionCode 2`; the artifact distributed as `0.9.0-beta.1` is `versionCode 3`, which adds the Read work below and changes nothing in the text-styling surface.)

- **Text can be styled.** Select a text block and tap the new **Text style** (`Aa`) control to open the Type bar: set the **size**, the **alignment** (left / center / right), **bold**, **italic**, and the **colour** from the five Zinely text inks. The block updates live on the page — what you see is what prints.
- **Every change is one undo.** Any style change can be undone in a single step, including a whole run of size taps. There is no "apply" or "cancel" — undo is the cancel.
- **Keyboard.** `Ctrl/Cmd + B` and `Ctrl/Cmd + I` toggle bold and italic on a selected text block.

**Accessibility.** Every control is a real labelled button — no jargon dropdowns, no gesture-only affordances. Alignment announces as a single-select group, bold and italic as independent toggles, and each change is spoken. Every control is tappable well beyond its painted size, so the frozen layout is kept without shrinking any target: the steppers and toggles reach a full 48dp, and the closely-spaced colour swatches reach 48dp tall by at least 40dp wide. The Type bar buzzes on each accepted change (silent when reduced motion is on).

**Compatibility.** Existing zines are unaffected and open unchanged at their current styling — no file-format change, no migration. Offline/privacy invariants intact: no network, no account, no new dependency.

### Fixed — reopening a zine and adding to it could make it unopenable

Found during the on-device accessibility pass, on a physical phone, and fixed before the beta.

- **Adding an element to a reopened zine could permanently break it.** Every element gets an id, and the counter those ids come from was rebuilt from scratch each time a project was opened instead of continuing from what the project already contained. So the first photo or text box added after reopening a zine reused an id that was already in use. The result was silent at the time and fatal afterwards: the zine saved normally, and then would not open again — the editor showed "Couldn't open this project." from that point on, with no way back through the app. It also made the two elements sharing an id behave as one: both read as selected to a screen reader, and undo would have removed both.

  It is fixed at the source — the counter now starts past every id the zine already holds — so **an affected zine repairs itself the next time it is opened, with no file-format change and nothing for you to do**. A zine that was *already* saved with duplicate ids stays unopenable, because the damage is on disk rather than in the code; no beta tester can be carrying one, because the beta requires a fresh install (see the signing note in the release notes).

### Known limitations

**Your work is not backed up.**

- **No backup or restore.** Your zines live only on this phone, in Zinely's own storage. Uninstalling
  Zinely — or losing the phone — deletes them. Save a PDF of anything you care about.
- **Deleting a photo does not reclaim its space.** Storage only grows for now; there is no sweeper yet
  ([ADR-031 §2](docs/DECISIONS.md#adr-031)).

**Text.**

- **Font choice is not in this milestone.** Text renders in the single bundled Inter family; choosing a font is planned for V1 and needs more families bundled first. Bold and italic use real bundled Inter faces, not synthesised ones.
- **Non-Latin text renders blank** — tied to the Latin-first font bundle ([ADR-028](docs/DECISIONS.md#adr-028)). Bengali, Hindi, CJK and emoji will not appear.
- **Styling is per-block, not per-character.** A block has one size, colour, alignment, and weight; mixed styling inside one block is not supported.
- **Some text inks are low-contrast.** The five inks include authorial values (teal in particular) that fall below AA as body text on white. They are offered as-is; a beginner-safe default is a later call.

**Printing and saving.**

- **No in-app print.** Zinely hands the finished sheet to your phone (Save PDF, or Share) rather than
  driving the printer itself ([ADR-052](docs/DECISIONS.md#adr-052)). Print at **100% / Actual size** —
  a printer's "fit to page" breaks the fold alignment.
- **The imposed print sheet shows page numbers, not your artwork.** The sheet under **Print & fold**
  is a map of where each page lands, not a picture of the zine. Read is where you see the zine
  ([ADR-058](docs/DECISIONS.md#adr-058) Decision 7).
- **On Android 7–9, the first save asks for a storage permission**, and on those versions it is a
  broad legacy permission rather than a Downloads-scoped one. Zinely writes only its own export file.

## [0.8.0] — 2026-07-15 — Save to your phone

### Added — Save a copy to your phone · [ADR-054](docs/DECISIONS.md#adr-054)

Merged to `main` as B1–B4 (merge `7e2fa74`; Downloads backend · export-flow wiring · frozen Proof behaviour · doc reconciliation), each batch independently reviewed (GO / GO WITH FIXES, reconciled). Packaged as a debug-signed side-load build (a real release keystore is still deferred, per [ADR-047](docs/DECISIONS.md#adr-047)).

- **Save PDF writes to Downloads.** "Save PDF" now saves a permanent copy of your zine into the device's shared **Downloads** — one tap, no chooser — where it stays, visible in the Files/Downloads app, after Zinely closes. A confirmation names the file: _Saved “…” to Downloads_, with the "Fold now" hand-off.
- **Share is unchanged.** "Share" still sends a copy through the OS share sheet (`ACTION_SEND`) exactly as before.

**Accessibility.** Existing behaviour preserved — the confirmation stays a `role=status` announcement; focus, keyboard flow, and reduced-motion behaviour are unchanged.

**Compatibility.** API 29+ writes through MediaStore (no permission needed); API 24–28 uses the legacy public-Downloads File path. Offline/privacy invariants intact — no network, no account, no new dependency; Zinely writes only its own export file.

### Known limitations

- **API 24–28 asks for a storage permission.** On Android 7–9 the first save triggers a one-time runtime permission prompt (`WRITE_EXTERNAL_STORAGE`); later saves are one-tap. A denial routes to the existing "Couldn't make the PDF" surface.
- **Broad legacy storage permission.** On API ≤28 that permission grants broad shared-storage access rather than Downloads-scoped; Zinely only ever writes its own export file there.
- **Replace Picture UI still absent** — Reframe's replace-photo affordance is not yet wired (Future Enhancement).
- **Long-press context menu still absent** — the element long-press visual menu is not yet built (Future Enhancement).

## [0.7.0] — 2026-07-14 — Image Framing

### Added — Image Framing (Reframe) · [ADR-053](docs/DECISIONS.md#adr-053)

Merged to `main` as IF1–IF5 (final implementation commit `685f753`); device pixel-parity (P1–P5, M1) and accessibility semantics both re-verified on-device — PASS. Not yet in a packaged build.

- **Reframe a photo in place.** Double-tap a photo (or its "Reframe photo" action) to open a dedicated Reframe surface: pan and pinch-zoom the picture inside its fixed frame, over a live scrim + rule-of-thirds guide.
- **Fill / Whole photo fit control.** Fill (crop-to-cover) is the new default for newly placed photos; "Whole photo" fits the whole image with margins. Existing documents are byte-identical — new placements only.
- **One reframe = one undo.** A whole Reframe session bakes to a single undoable edit; leaving the panel commits, Cancel discards, switching pages commits then navigates. **Reset framing** returns a photo to its placement default.

**Accessibility.** Every Reframe control is labeled and reachable in a logical traversal order; the fit toggle exposes selected state; zoom exposes a spoken readout; a full hardware-keyboard path (arrows nudge, Shift = coarser, +/− zoom, Enter save, Esc cancel) drives the same actions as touch; announcements force-speak on repeat and the first-run coach-mark respects reduced-motion.

**Implementation.** Framing persists via the existing `ImageElement.crop`/`fit` — no `:core:model` or `:core:render` change; the live draft is preview-only and never enters the reducer mid-gesture (`preview == commit` by shared pure math). The visual surface implements the DESIGN-FROZEN [`bench.html`](docs/design/v1/bench.html), re-verified on-device (pixel-parity P1–P5/M1 and accessibility semantics both PASS).

**Compatibility.** Offline/privacy invariants intact — no network, account, or new dependency; no document-format migration (existing zines open unchanged; only new photos default to Fill).

_Next up: post-alpha S7.x — save-a-copy export landed (see [Unreleased]); text styling next._

## [0.6.0-alpha.1] — 2026-07-07 — First installable alpha

The first build handed to early testers: the full create → edit → export → print spine.
Bundles the never-separately-released **0.5.0** (`SUX` first-time creation) and **0.6.0**
(S5 export/share + S6 project layer & Home) milestone work plus the S7 alpha-push slices.
Gate evidence: the physical print/fold test passed (printed at 100%, folded, 1→8 order/
rotation/scale correct), and the "text missing in preview" field report was triaged to the
known [ADR-028](docs/DECISIONS.md#adr-028) Latin-first charset limitation (see below) —
full triage in [the release assessment](docs/reviews/2026-07-04-alpha-release-assessment.md).

### Known limitations (alpha)

- **Export is share/open-only — no copy is saved to your phone yet.** "Print at home" and
  "Save as image" hand the finished sheet to the app you pick (share sheet / viewer); Zinely
  writes only a temporary internal file, nothing appears in your gallery or Downloads unless
  the app you share to saves it. A real "Save to your phone" is the first post-alpha slice
  ([ADR-039](docs/DECISIONS.md#adr-039) deferral).
- **Text is English-first Latin only.** The bundled font covers the
  [MVP charset](docs/DECISIONS.md#adr-028) (ASCII + Latin-1 letters + common punctuation);
  other scripts (e.g. Bengali, Hindi, CJK) and emoji may render blank or degraded.
- **The editor page sits left with a gap on the right** on some screens (page is fit
  top-left, not centred). Cosmetic only — preview, export, and print are unaffected.
- **Alpha builds are debug-signed**: a future properly-signed build will require
  uninstall + reinstall (projects on the device are lost).
- **App storage grows with every photo import** (replaced/deleted photos are not yet
  reclaimed — [ADR-031 §2](docs/DECISIONS.md#adr-031)).
- Print at **100% / Actual size** — printer "fit to page" breaks the fold alignment
  (the in-app note says the same).
- Alpha scope vs the MVP (single-style text, no fit/fill control, no layout presets, no
  calibration ruler): [PRD §7.3](docs/PRD.md#73-alpha-release-scope--v060-alpha1-adr-047).

### Added
- **S7.1 — choose your paper when you start a zine** ([ADR-047](docs/DECISIONS.md#adr-047)):
  **Start a zine** (empty-shelf CTA and shelf FAB alike) now asks *"What paper will you print
  on?"* — **Letter** (8.5 × 11 in) or **A4** (210 × 297 mm) — and creates the zine on the paper
  you pick; nothing is created until you choose, and "Not now" backs out. Ends the shelf's
  hardcoded Letter: the imposition, render, export, and project store have carried the paper
  size end-to-end since S1, so an A4 zine previews, exports, and folds correctly. The
  v0.6.0-alpha.1 scope (what ships now vs. post-alpha: text styling, layout presets,
  calibration ruler, asset GC) is recorded in [PRD §7.3](docs/PRD.md#73-alpha-release-scope--v060-alpha1-adr-047).

- **S6.5 — nav re-root: Home is the app** ([ADR-046](docs/DECISIONS.md#adr-046), the final
  S6 slice): `ZinelyNavHost` now starts at a new `HomeRoute` — the single back-stack root —
  hosting the S6.2–6.4 "My zines" shelf. A card tap (or **Start a zine**, which now creates
  *and opens* the new zine, single-flight) pushes `EditorRoute(id)` with `launchSingleTop`;
  returning is only ever a pop, and Completion's "Keep editing" still pops to the existing
  editor entry. A fast reopen of a just-closed zine no longer risks the spurious
  "Couldn't open" boot error: the editor bootstrap awaits the single-writer release through
  the same 5 s `AutosaveSessionGate` policy the shelf mutations use
  (`EditorAutosaveBinderFactory.awaitNoSession`; timeout ⇒ a warm "still saving" error).
  Leaving the shelf commits pending undoable deletes (leaving = snackbar dismissal; a failed
  commit un-hides the card and messages, and never blocks the open), and an open that lands
  while the shelf is away is discarded on return — navigation only ever follows a fresh tap.
  The shelf re-reads the store on every return (`WhileSubscribed(0)`), so recency labels and
  thumbnails are fresh after an editing session. `HomeScreen` gained an explicit `storeEmpty` signal so a shelf
  filtered to zero by pending deletes never shows the empty invitation. New host-level
  Robolectric nav tests (`TestNavHostController` + a debug-only `HiltTestActivity`) cover
  the whole back-stack policy — the graph's first.

- **S6.4 — Home shelf thumbnails (built on the unwired shelf)**
  ([ADR-045](docs/DECISIONS.md#adr-045)): each "My zines" card now shows a page-1
  miniature rendered through the proven parity path — the `SceneRenderer` tape replayed
  by the shared `CanvasReplayer` via a new thin `:render-android` `ThumbnailRenderer`
  (paper-white, 320 px longest edge, the export font/image stack) — so **a thumbnail is
  a miniature of the export by construction**. Thumbnails are produced pull-based on
  shelf observation by an `:app` `ShelfThumbnailProducer` (IO, one mutex, capped
  in-memory LRU) and cached as a **derived, never-authoritative** PNG at
  `cacheDir/thumbnails/<id>.png`, invalidated by a single stamp: the PNG's mtime is set
  to `document.json`'s mtime and validity is exact equality. Rename doesn't regenerate
  (content unchanged); duplicate renders fresh; any failure shows a warm paper
  placeholder — the shelf never breaks. New narrow `:data-android` seam
  `ProjectDocumentLayout.documentFile` (over the internal `ProjectPaths` chokepoint);
  ADR-031's no-sweeper invariant untouched (a thumbnail is never a GC root). Navigation
  unchanged — Home remains unwired until S6.5.
- **S6.3 — Home shelf actions: create · rename · duplicate · undoable delete
  (testable-only until S6.5)** ([ADR-044](docs/DECISIONS.md#adr-044)): "Start a zine"
  returns to the empty shelf (ending the ADR-043 §5 named deviation) and as a content-shelf
  FAB — one tap creates "My zine" (`SINGLE_SHEET_8` · `LETTER`, the bootstrap-seed defaults);
  each card gains an overflow menu with Rename (gentle **[ Keep name ] [ Rename ]** dialog,
  blank disabled, trimmed in the VM), Duplicate, and a confirm-less Delete with a snackbar
  undo window (the card hides instantly; Undo restores it with no store call; dismissal
  commits `deleteProject`; a failed commit un-hides the card — the shelf never lies). The
  **ADR-042 open-editor exclusion is now enforced inside `RoomProjectRepository`**: a
  `ProjectSessionGate` over `AutosaveCoordinatorFactory`'s new by-id `awaitReleased`
  (the ADR-030 Rec1 seam realised) gates rename/delete targets and the duplicate source;
  a session still live at the bound refuses with the new **`DataError.Busy`** ("That zine
  is still saving — try again in a moment."). Navigation is untouched: Home remains
  unwired, so every action is reachable only in tests until the S6.5 re-root.
- **S6.2 — Home · "My zines" read-only shelf, built-but-unwired**
  ([ADR-043](docs/DECISIONS.md#adr-043)): a stateless `HomeScreen` in `:feature:editor`
  (paper-card list — title, "8-page mini · Letter/A4", warm "Edited …" recency — plus a
  CTA-less empty-state invitation) and an MVVM `HomeViewModel` in `:app` observing
  `ProjectRepository.observeProjects()`. Deliberately **not registered in `ZinelyNavHost`**
  (Codex Required Fix): the app still boots into `EditorRoute("default")`, and no
  create/duplicate/delete/rename affordance exists anywhere (ADR-042 hard invariants).
  Shelf actions arrive in S6.3, thumbnails S6.4, navigation wiring + re-root S6.5.
- **S6.1 — Room-backed project store** ([ADR-042](docs/DECISIONS.md#adr-042)): the
  `ProjectRepository` contract gets its first real implementation in `:data-android` —
  an observable multi-project metadata index (Room `projects` table, schema exported)
  over the per-project files, which stay the source of truth (`document.json` + a new
  atomic `meta.json` sidecar for title/createdAt). Existing on-disk projects — including
  the S4 `"default"` seed — are adopted by an idempotent reconcile scan (no destructive
  migration; nav unchanged). Create/rename/duplicate/delete land file-first with the row
  re-derived; duplicates share content-addressed assets (new GC live-root by
  construction, no sweeper shipped). Data layer only — the Home/My-zines shelf arrives
  in S6.2+.
- Design language + onboarding philosophy as canonical pre-implementation design:
  [docs/design/DESIGN-LANGUAGE.md](docs/design/DESIGN-LANGUAGE.md) and refreshed
  interactive mockups under [docs/design/mockups/](docs/design/mockups/).
- Editor **empty-state invitation** — a cozy, encouraging first-run surface on a blank
  page with discoverable "add a photo" / "add words" supply actions (replaces the dead
  blank canvas; contextual guidance instead of hidden gestures). "Add words" places an
  empty text box and opens its editor immediately — straight to typing, no placeholder.

> The `SUX`-era entries above were built under the **0.5.0** milestone; the alpha is the
> first release that ships them.

### Fixed
- **S7.0 — photo import works on real devices**
  ([ADR-031 §Review 2b](docs/DECISIONS.md#adr-031)): every on-device import failed with
  "That image couldn't be added." — `ImportMasterDecoder.readBounds` null-guarded the result
  of a bounds-only `BitmapFactory.decodeStream` (null **by contract**) instead of the stream.
  Fixed at the guard, with the decoder's first headless regression suite
  (`ImportMasterDecoderTest`, Robolectric NATIVE + fresh-stream-per-open shadow resolver).

### Removed
- **The `"default"` seed-on-miss bootstrap** ([ADR-030](docs/DECISIONS.md#adr-030) §4,
  retired by [ADR-046](docs/DECISIONS.md#adr-046) §3): the editor no longer silently
  creates a blank document for a missing id — `NotFound` is an honest boot error with a
  **Back to your shelf** action. First run lands on the empty shelf's **Start a zine**
  invitation instead of a pre-seeded editor. Existing installs keep their zine: the on-disk
  `"default"` project was already adopted as an ordinary shelf row by the ADR-042 reconcile
  (zero migration), and deleting it now really deletes it (no re-seed on next boot —
  the ADR-042 hard-invariant #1 and ADR-044 §3 delete-honesty arcs close).

### Changed
- Roadmap re-sequenced: the next milestone targets the **first-time creation experience**
  (onboarding, empty state, discoverability, supply tray, contextual hints) ahead of more
  editor power, per [ADR-008](docs/DECISIONS.md#adr-008). See [ROADMAP.md](docs/ROADMAP.md).

## [0.4.0] — 2026-06-28 — Editor foundation

The editor is mounted in the real app and every page of the zine is reachable.

### Added
- `:core:editor` — pure MVI reducer (intents, command/memento undo, hit-test, snap,
  z-order, transform math); fully unit-tested ([ADR-029](docs/DECISIONS.md#adr-029)).
- `:feature:editor` — interaction surface: store + effect runner, gesture pipeline,
  selection chrome with live document-order preview, opposite-anchor resize handles,
  live snap guides (preview == commit), accessibility context bar + element semantics
  (WCAG 2.5.7), race-safe text-edit session, host `EditorScreen`.
- `:app` — single-Activity navigation mounting the editor on a fixed `"default"` project,
  `EditorViewModel`/`EditorBootstrap` (seed-on-miss + imposition-derived page size),
  autosave binder, content-addressed asset store, and interactive image import
  ([ADR-030](docs/DECISIONS.md#adr-030), [ADR-031](docs/DECISIONS.md#adr-031)).
- **Scrapbook page navigator** — all eight pages reachable via a styled page strip
  (`Intent.GoToPage`); "workbench" theme foundation replacing the default template theme.
- Roborazzi selection-chrome goldens (CI-gated).

### Changed
- Canonical docs reconciled with the real checkout (truthful module/persistence/export
  state); `AssetStore`/`:core:data-storage` GC comments aligned with the deferred sweeper.

## [0.3.0] — 2026-06-25 — Rendering pipeline

One scene model, two backends, with proven preview/export parity.

### Added
- `:core:render` — pure scene → ordered draw-command tape (`SceneRenderer`,
  `computeImageBlit`); only depends on `:core:model`, zero Android
  ([ADR-027](docs/DECISIONS.md#adr-027)).
- `:render-android` — single `CanvasReplayer` + two export providers (PDF in PostScript
  points, raster @ 300 DPI), point-space `SharedTextLayout`, crop-aware `ImageBlitter`,
  bundled **Inter** font with an MVP-charset cmap coverage guard
  ([ADR-028](docs/DECISIONS.md#adr-028)).
- Headless-CI Roborazzi raster + text parity goldens; image + PDF write/parity proofs
  verified on-device (compile-checked in CI).

## [0.2.0] — 2026-06-20 — Persistence

Durable, offline-first, on-device storage.

### Added
- `:core:data` — versioned `@Serializable` document tree, `DocumentSerializer` +
  migration, validation, repository / `DataResult` contracts, content-addressed asset +
  `.zine` manifest contracts ([ADR-003](docs/DECISIONS.md#adr-003)).
- `:core:data-storage` — pure-JVM durability core: atomic file store
  (temp → fsync → atomic rename → dir-fsync + `.bak` recovery), autosave coordinator,
  content-addressed `FileAssetStore` ([ADR-021](docs/DECISIONS.md#adr-021),
  [ADR-022](docs/DECISIONS.md#adr-022), [ADR-025](docs/DECISIONS.md#adr-025)).
- `:data-android` — Android adapters: real `Os.fsync` file-system ops, file-only
  `DocumentRepository`, autosave coordinator factory + lifecycle binder, Hilt graph
  ([ADR-026](docs/DECISIONS.md#adr-026)).

### Changed
- `minSdk` ratified at **24** ([ADR-024](docs/DECISIONS.md#adr-024)); CI runs the
  pure-Kotlin core Android-free via `ZINELY_CORE_ONLY`.

### Deferred
- Room project metadata, `ProjectRepository`, and the asset GC sweeper remain unbuilt
  (persistence is currently file-only and single-project).

## [0.1.0] — 2026-06-19 — Imposition engine

The riskiest, most isolatable thing first: the math that makes a folded zine correct.

### Added
- `:core:model` — `ZineDocument` / `Page` / `Element` / geometry in physical points.
- `:core:imposition` — single-sheet 8-page mapping (panels + rotations, fold/cut guides,
  SVG proof sheet); pure Kotlin, golden-tested against the imposition oracle
  ([ADR-007](docs/DECISIONS.md#adr-007)). Tagged `v0.1.0-imposition-engine`.

[Unreleased]: https://github.com/aritr-codes/zinely-android/compare/v0.9.0-beta.4...HEAD
[0.9.0-beta.4]: https://github.com/aritr-codes/zinely-android/compare/v0.9.0-beta.3...v0.9.0-beta.4
[0.9.0-beta.3]: https://github.com/aritr-codes/zinely-android/compare/v0.9.0-beta.2...v0.9.0-beta.3
[0.9.0-beta.2]: https://github.com/aritr-codes/zinely-android/compare/v0.9.0-beta.1...v0.9.0-beta.2
[0.9.0-beta.1]: https://github.com/aritr-codes/zinely-android/compare/v0.8.0...v0.9.0-beta.1
[0.8.0]: https://github.com/aritr-codes/zinely-android/compare/v0.7.0...v0.8.0
[0.7.0]: https://github.com/aritr-codes/zinely-android/compare/v0.6.0-alpha.1...v0.7.0
[0.6.0-alpha.1]: https://github.com/aritr-codes/zinely-android/compare/v0.4.0...v0.6.0-alpha.1
[0.4.0]: https://github.com/aritr-codes/zinely-android/compare/v0.3.0...v0.4.0
[0.3.0]: https://github.com/aritr-codes/zinely-android/compare/v0.2.0...v0.3.0
[0.2.0]: https://github.com/aritr-codes/zinely-android/compare/v0.1.0...v0.2.0
[0.1.0]: https://github.com/aritr-codes/zinely-android/releases/tag/v0.1.0-imposition-engine
