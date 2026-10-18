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

### Added

- **My Shelf now tells you when a zine's details went back to an earlier copy.** If the small file that holds a
  zine's name, cover and folder is damaged, Zinely uses the copy from before the last change, as it did
  before; now a note under the heading names the zine (for example, "Letters to Gran" is called "Letters home"
  again), says what went back, and stays until **Got it**, across restarts. Before, this happened without a
  word. The note covers those details only: a zine's pages repaired the same way still say nothing
  ([#146](https://github.com/aritr-codes/zinely-android/issues/146)). Checked on an emulator, including the
  largest text on a 360dp screen; not yet on a real phone or with a screen reader by ear
  ([`v21-library.html` A37](docs/design/mockups/v21-library.html), [ADR-134](docs/DECISIONS.md#adr-134), [#128](https://github.com/aritr-codes/zinely-android/issues/128)).
- Designed, not built: the Type bar's stacked Align row drawn as the app already shows it (A36), a paper
  picker for four sizes (A35, waiting on the format change after v4), and what Read says aloud for each page
  ([ADR-135](docs/DECISIONS.md#adr-135) Proposed). Nothing in the app changes for these.
- Art house style and redraw designed: all 34 Art pieces redrawn to one hand in the bench spec (A34, ADR-132
  Proposed). Not in the app yet; the app still draws the current Art.
- Two new pieces in Art, at the end of Cut paper: **Snipped border**, a thin border cut with scissors, and
  **Round window**, a square of paper with a round hole. Each is one ink, and each can be flipped, stretched,
  duplicated and replaced like any other piece. A tap in the hole reaches the photo or words underneath; a tap
  on the paper selects the frame. A frame is laid over a photo; it does not hold, crop or follow it. Art now
  has 34 pieces. Known limitations: zines that use the new frames need this version or later to show and print
  them (an older version opens the zine, keeps the frame, and draws nothing for it, though the unseen piece
  can still be selected where it sits and takes a tap meant for what is under it); Snipped border's band is
  thin, so over a photo it can be hard to pick back up; and a frame stretched out of square has a band that
  is thicker on two sides than on the other two
  ([`v21-bench.html` A32](docs/design/mockups/v21-bench.html), [ADR-124](docs/DECISIONS.md#adr-124)).
- The public roadmap page is rebuilt phone first as ledger sheets: one torn-off, taped sheet of ruled paper per
  area, each item a row with its status as a rubber stamp (in a ruled margin on tablets and wider), in place of
  three tilted columns of unequal length. Each sheet folds: closed, it shows its title and a count of each
  status on it; a tap unfolds the rows. A short "On this page" row jumps to each section. Small labels are
  at least 12 px. On every page the header and footer links are now finger-sized (44 px), which only the
  homepage had. No roadmap wording changed.
- The roadmap and the public roadmap page say more plainly where things stand: which shipped features are
  not finished being checked, the known problems in beta.7, the two frames as In development with nothing to
  try yet (on the homepage too), better-drawn Art as planned and not started, and the newer customisation ideas as ideas only.
- The public website now offers 0.9.0-beta.7 as the current download, with its version, size, versionCode 12,
  APK link and published SHA-256, a beta.7 entry in What's new with its known limitations, and folders and
  the second font moved from In development to Available. The lines below about them being In development
  describe the website before this. Google Play and iPhone stay shown as unavailable.
- The public roadmap lists folders on the Shelf and a second font as In development, each saying there is
  nothing to try yet. The roadmap and What's new pages use plainer words (for example "a screen reader" where
  they said TalkBack alone), and the homepage status table now starts each status on its own line.
- The public website now offers 0.9.0-beta.6 as the current download, with its version, size, versionCode 11,
  APK link and published SHA-256, a beta.6 entry in What's new with its known limitations, and the roadmap's
  wave-1 items moved to Available. Google Play and iPhone stay shown as unavailable.
- The public roadmap page lists what is planned:
  folders on the Shelf, a second typeface, two frames, page backgrounds, photo descriptions, help at the printer and
  more paper sizes. No date is given for any of it, and no Android behavior or download changed.
- Rebuilt the public website story around Shelf, Bench and Proof, told in the order a visitor asks: what it is, why
  paper, what you can make, how it works, what is different, how to try it and where it is going. Added a Download
  page with install steps, backup and phone-move advice, the published SHA-256, and honest Google Play and iPhone
  status. The roadmap now uses one scheme (Available, In development, Planned, Exploring) by horizon, and the public
  changelog tells the app's story back to its early builds ([ADR-118](docs/DECISIONS.md#adr-118)).
  No Android behavior, download or release artifact changed.
- Added an HTML-only A/B experiment for evaluating removal of the unavailable selected-text Font action. It leaves
  the canonical design and Android app unchanged; first-time comparison evidence and an owner decision remain gates.
- Added full Reframe photo-overlay golden coverage for centred Fill, representative panned/zoomed framing,
  both-axis Flip, and Whole-photo containment in light and dark themes. This is repository test coverage only;
  it does not change the published app.
- Rebuilt the public website around an expressive, DIY-inspired working-desk presentation with three
  sanitized light-mode renders produced from the canonical HTML mockups. No private device photos are used.
- Added concise public Changelog and Roadmap pages. The roadmap separates planned work, explorations, and
  uncommitted ideas so it does not present tentative work as a promise.
- Added an optional, non-autoplaying HTML fold-replay experiment with keyboard-operable controls, persistent
  written instructions, and a reduced-motion path. It remains a prototype and does not alter the app.

### Changed

- **The page keeps its size when something is selected.** The move, size, turn and layer buttons are now
  behind one **Arrange** button in the bar under the page, instead of in a bar of their own that shrank the
  page each time a piece was selected (on a small phone, by about a third of its height). Arrange opens a
  tray with the same buttons; it stays open while you pick other pieces, and Done or Back closes it. The
  first-run hint now names Arrange, is readable in dark mode, and steps aside while the Type bar is open
  ([`v21-bench.html` A33](docs/design/mockups/v21-bench.html), [ADR-129](docs/DECISIONS.md#adr-129), decided
  under the owner's delegation; it reverses the owner's OD-11 ruling, which the owner can overrule). At the
  largest text size the first-run hint clipped its first and last words (also in beta.7); fixed, see *Fixed*.
- Internal: two tests that failed now and then on CI now wait for the work they check
  ([#132](https://github.com/aritr-codes/zinely-android/issues/132), [#136](https://github.com/aritr-codes/zinely-android/issues/136)). No app change.
- Internal: the imposition engine now lays out any sheet size, property-tested from 4 to 18 in a side; US Legal
  and A5 are chosen as the next paper sizes ([ADR-133](docs/DECISIONS.md#adr-133)). No paper size a person can choose has
  changed.
- Docs: ZINE-DIRECTION notes the two-voice ruling (Book and Plain, ADR-126 Proposed) beside its four "three
  voices" lines; 21 drifted line citations into the frozen type bar, Reframe and Library pages re-pointed to
  named blocks or pinned commits (#97, #98).
- The public website now offers 0.9.0-beta.5 as the current download: version, size, versionCode 10, the APK
  link and its published SHA-256 on the Download page; a beta.5 entry in What's new with its known limitations;
  a short "New in 0.9.0-beta.5" note on the homepage; and the roadmap moves beta.5 to Available. Google Play
  and iPhone stay shown as unavailable. No Android behavior or release artifact changed.
- Rewrote public update pages in plain language, refreshed their status, and moved homepage accessibility copy
  from standard names to practical benefits, and expanded the Aastra origin story. No download, legal text or
  release artifact changed.
- Reopened creative tools for a scoped feasibility assessment, not a release commitment: fonts, Art, frames,
  photo transparency, shaped cutouts and crop improvements. The roadmap separates their relative effort.
- Gave the public homepage a more tactile paper presentation and stronger independent voice, with an interactive
  print-order comparison, optional zine ideas, folding progress, and small native-disclosure surprises. All work
  without essential hover or motion; product claims, download links, and Android behavior remain unchanged.
- Refined the public homepage voice with clearer page-arrangement language, a repository-grounded two-person origin
  story, warmer calls to action, and restrained indie personality. Routes, product behavior, and factual policy or
  release claims are unchanged.
- Evaluated the proposed long-press element menu and retained the visible selected-element toolbar, contextual
  guidance and accessibility actions. No app behavior or published APK changed; a post-fix first-time task round
  demonstrating repeated discoverability failure remains the threshold for reconsidering another menu.
- Refined the website fold guide into a deliberate one-step-at-a-time paper desk, with pause/replay,
  static checkpoints, keyboard controls, stable instruction space, and all-steps/print/no-JS fallback.
- Linked the owner-published Google feedback form, retained email for screenshots, and introduced Aastra
  as the two-person team. Privacy wording covers voluntary feedback separately from the offline app.
- Replaced the website's three-example fold demo with a complete ten-step illustrated guide. Original SVG
  diagrams show creases, the single cut, and the finished booklet. Optional direction tracing and subtle
  button/link effects respect reduced motion; all instructions remain available without JavaScript.
- Updated the website About story with Zinely's indie origins and an invitation to share suggestions.
- Reconciled the roadmap against current code, issues and releases: distinguish the conditional Google Play
  publication plan and approved Reframe investigation from uncommitted app improvements; keep completed
  website work in the public changelog. No Android behavior or public APK changes in this website slice.
- Replaced the standalone website Accessibility page with a short accessibility statement and contact route
  in the home-page About section. Accessibility remains a site-wide implementation requirement rather than a
  primary navigation destination.

### Known limitations

- **With larger text on a small phone, the fold guide and the screen after Save PDF scroll.** In the fold
  guide a step opens at its top and its instruction is below; after a save, the top bar scrolls away with the
  page. That scroll after a save is animated even when the phone is set to reduce motion.
- **At the largest text size on a small phone, the low-storage notice shown when a change can't be saved can
  run taller than the screen**: its two buttons squeeze the words into a narrow column. Found by a test, not seen on a device; the fix needs a
  design change first ([#149](https://github.com/aritr-codes/zinely-android/issues/149), [D-119](docs/design/V2-SPEC-DEFECTS.md#d-119)).
- **None of the changes below was listened to with a screen reader.** They were checked by reading the
  screen's accessibility information, not by ear.

### Fixed

- At the largest text size on a small phone, the first-run hint no longer cuts off its first and last words.
  A tall hint now has rounded corners instead of round ends. Checked on an emulator set to a small phone, not
  yet on a real one ([D-119](docs/design/V2-SPEC-DEFECTS.md#d-119), [#134](https://github.com/aritr-codes/zinely-android/issues/134)).
  At that size the hint still covers the piece it asks you to drag ([#148](https://github.com/aritr-codes/zinely-android/issues/148)).
- A zine's title on the Shelf now always matches its saved metadata file. Before, if the file was restored
  from its backup copy after damage (it holds the previous version), the Shelf could keep showing the newer
  title until something rewrote it (D-116, [#106](https://github.com/aritr-codes/zinely-android/issues/106)).
- At the largest text size, "Right" in the Type bar's Align row no longer breaks across three lines on a
  small phone: when a row does not fit beside its label, its choices move under it
  ([#102](https://github.com/aritr-codes/zinely-android/issues/102), D-115).
- The notice shown while typing no longer says characters "can't print yet" when your phone's own fonts
  draw them. It now says they print in a font from your phone and may look different on another phone;
  characters no font on the phone has still get "can't print yet" ([#103](https://github.com/aritr-codes/zinely-android/issues/103), ADR-130).
- **Changing a text's look no longer hides the text.** The Text panel and the Ink panel now stop short of
  the top of the page, and the page slides up so the words or the piece you are changing stay in view above
  them. Before, the Text panel covered the text box and the Ink panel covered the whole page
  ([ADR-128](docs/DECISIONS.md#adr-128)).
- The page no longer jumps down and back each time **Saved** appears.
- Adjusting a photo that sits low on the page no longer cuts its bottom off.
- After flipping a piece, the "Flipped" message and its **Undo** show above the Flip panel instead of behind it.
- The app now stays upright when a phone is turned on its side. No screen was designed for landscape, and
  turned sideways most of them lost their lower half ([ADR-127](docs/DECISIONS.md#adr-127)). Tablets and
  split-screen are not covered.
- On small phones or with large text, these no longer cut off what did not fit; they scroll or wrap instead:
  the empty Shelf and the "could not open" Shelf, the zine menu, Move to folder, the Backups sheets, the
  Preview buttons and its page counter, the print legend, the fold guide's instruction, the Add menu and
  the photo-across-two-pages sheet. Sheets keep their title in place and no longer run under the status
  bar. These were checked in tests and on an emulator set to a small phone (360 x 640), not on a real small
  phone ([D-113](docs/design/V2-SPEC-DEFECTS.md#d-113)).
- At ordinary text size the fold guide now shows a whole step at once on a small phone: the drawing gets
  smaller so the step counter, the key and the instruction all fit. Before, the instruction was below the
  bottom edge with nothing to show the guide scrolled ([D-113](docs/design/V2-SPEC-DEFECTS.md#d-113)).
- In Preview, with larger text **Save PDF** and **Share** sit one above the other so neither label is cut or
  broken inside a word, and the "Ready when you are" title wraps instead of being cut short.
- The row of photo buttons now fades at the edge when there are more buttons to scroll to. On narrow phones
  **Delete** was off the end with nothing to show it.
- A notice in the Library no longer lands on top of the "Moved to folder" message; it waits until that
  message, and its **Undo**, have gone.
- The greyed-out **Make folder** button keeps its whole shadow. Its right and bottom edges were cut off.
- After deleting a zine with a long name, **Undo** keeps its full size; the name wraps instead. Before, Undo
  could be squeezed to a sliver at the edge of the bar.
- The delete Undo bar and the Library's short notices now sit above **Make a zine** instead of on top of it,
  and the Undo bar no longer runs edge to edge.
- The All pages sheet keeps its title and Close in place while the pages scroll, and the Flip panel keeps
  its title and **Done** in place. Both matter on small phones or with large text; neither was seen broken on
  a phone ([D-112](docs/design/V2-SPEC-DEFECTS.md#d-112)).
- The Ink panel keeps its title and **Done** in place while the colours scroll. On phones where the panel
  did not quite fit, scrolling to the Starting palettes left both cut in half at the top
  ([D-112](docs/design/V2-SPEC-DEFECTS.md#d-112)).
- The website no longer requests fonts from Google. It now serves the app's own fonts itself, which makes the
  privacy policy's "no third-party fonts" true. Averia is shipped unmodified, and each licence sits beside
  the files.
- The website's Bench demo and screenshot no longer show a "Your shelf · 4 things kept" tray that the app does not
  have. The screenshot was re-rendered from the frozen prototype with only that tray hidden.
- The website's not-found page now finds its styles and links at any URL depth.

- Restored the Reframe accessibility regression that proves a measurable but undisplayable photo keeps every
  adjustment inert and silent. Its precondition now comes from an immutable composition-scoped loader seam rather
  than stream-consumer ordering, eliminating the CI race tracked by issue #57.

## [0.9.0-beta.7] — 2026-10-11 — Folders, and a second font

This release carries `versionCode 12`. It installs over earlier release-signed beta builds without
uninstalling. The saved-document format and the backup format version are unchanged; a zine can now carry a
folder name and a text can name the Book font, and earlier versions read both without failing (see Known
limitations for what they then show). It is published as an APK only.

Folders and the second font ship **built and merged, not yet accepted**: the checks still owed are listed
under Known limitations and in [ADR-125](docs/DECISIONS.md#adr-125) and [ADR-126](docs/DECISIONS.md#adr-126).

### Added

- **Folders on the Shelf.** From a zine's three dots, *Move to a folder* makes a folder or puts the zine in
  one. A folder shows as a pile of its zines' covers; tap it to look inside, or tap its three dots to rename
  it or take its zines out. Moving, making a folder and taking zines out can be undone for a few seconds;
  renaming cannot. A backup keeps each zine's folder ([ADR-125](docs/DECISIONS.md#adr-125)).
- **A second font: Book, beside Plain.** Select a text and tap **Text**: the card that opens has a new first
  row, Font. Book is a serif face with its own bold and italic; Plain is the face every zine has used so far.
  Book has no Greek or Cyrillic letters and is not offered below 12 pt; in both cases the card says why.
  Undo says "Font changed" and "Font put back" ([ADR-126](docs/DECISIONS.md#adr-126)).
- While you type, the text is drawn in its own font, including a real italic.

### Changed

- The button on a selected text that opens the text card is named **Text**. It was named "Size", which named
  one of the card's five rows. The row inside the card that changes the size is still "Size".
- The download grows by about a quarter of a megabyte, for the four Book font files.

### Fixed

- A zine you deleted and then restored from a backup, without closing the app in between, could stay hidden
  from the Shelf until the app was restarted, and could be deleted again when the delete's Undo message
  closed or you left the Shelf. A restored zine now shows at once and stays
  ([ADR-121](docs/DECISIONS.md#adr-121), note of 2026-10-09).
- The lines of a longer text sat closer together while you typed than on the finished page, so the text
  moved when you tapped Done. They now sit as far apart as on the page, to within about a pixel a line.

### Known limitations

- **A text that just fits its box can lose its last line when you switch it to Book**, with no sign: the two
  fonts differ in width and height and the box does not grow. Make the box taller to see the line again.
- Book's smallest size, 12 pt, has not yet been confirmed on a printed page.
- **If a zine with Book text is opened or restored on 0.9.0-beta.6 or earlier**, that version draws the text
  in Plain. Every word is kept and is Book again on this version. But lines break in other places, and lines
  that no longer fit their box are missing from the page and from the PDF on the older version, which does
  not say so. In our test an italic paragraph that just fitted lost its last line.
- An older Zinely restores a backup that has folders with every zine on My Shelf. A backup made by an older
  Zinely restores with every zine on My Shelf. Restoring a backup onto a phone that already has the same
  zines adds nothing, so it brings no folders.
- A second folder action tapped while the first is still being written is dropped without a message.
- Folders and the Font row have not been checked by ear with TalkBack; they were read from the phone's
  accessibility tree. With TalkBack, focus does not move to the result after a folder action.
- A selected text shows **Text** on the upper bar and **Text style** on the lower bar; both open the same
  card.
- The message after the last zine leaves a folder is two lines long and stays for four seconds, with its Undo.
- At large text sizes the draft can still sit about a pixel a line away from the finished page.
- The update was installed over beta.6 on one phone, and the Shelf opened with its zines; no existing zine
  was opened afterwards. The new features were run on that phone as the same commit under a test name, with
  a new zine. The [tester package](docs/releases/0.9.0-beta.7.md#verification) says what was checked.
- Tapping through a holed piece has not yet been checked by ear with TalkBack. A very thin frame over a photo
  can take a careful tap. Once selected, dragging from its edge resizes it; drag from the middle of the
  selection to move it.
- A second tap on small selected words can land on a resize handle and not open the words for editing.
  Double-tap the words, or use **Edit**.
- With TalkBack, a repeated identical undo or redo line may be spoken only once.
- A backup that left zines out is restored by versions before beta.6 without saying so.
- Transparency in imported pictures isn't kept: the see-through parts are flattened onto white when the
  picture comes in. Exports and prints are right, because the paper is white. On the cream editor page,
  though, a transparent logo shows as a faint white rectangle, and placed over a photo it shows a white box.
- On Android 7–9, if you decline storage access and ask Android not to ask again, Save PDF keeps showing
  "Couldn’t make the PDF" until you allow storage for Zinely in the phone's settings. Share still works.
- With TalkBack on, **About**, **Licences & credits** and a font licence open with TalkBack on the Back
  button, and going Back returns TalkBack to the top of the previous screen.
- Replaced and deleted photo assets are retained; app storage is not reclaimed yet.
- Unsupported print scripts are kept and warned about but do not print.
- Zinely saves the PDF; printing happens in the phone's PDF or print app.

## [0.9.0-beta.6] — 2026-10-06 — Backups that tell you what happened

This release carries `versionCode 11`. It installs over earlier release-signed beta builds without
uninstalling, and it does not change the saved-document format. A backup file can now list zines it had to
leave out; earlier versions restore such a file without mentioning them. It is published as an APK only.

### Added

- Undo and redo now say what they did: a short line on the Bench names the change, and TalkBack speaks it.
  A repeated identical line may be spoken only once ([ADR-123](docs/DECISIONS.md#adr-123)).
- Taps now pass through the empty part of six holed Art pieces (window frame, torn hole, ring, eyelet, photo
  corner, registration cross): a tap in the hole reaches the photo, words or piece underneath, and a tap on the
  drawn ink still selects the piece. On blank paper a tap within 8 dp of one of these pieces' ink picks it up.
  Every other piece, crop marks included, keeps its whole box, and the code that screen readers select
  through was not touched ([ADR-124](docs/DECISIONS.md#adr-124), not yet accepted).

### Changed

- Restoring a backup adds only the zines that aren't already on your shelf. A zine with the same title, format,
  paper size and content isn't added again; one that has changed since the backup is added alongside, and nothing
  on the shelf is replaced. The result says how many were already here, or "Nothing new to add"
  ([ADR-121](docs/DECISIONS.md#adr-121)).
- The Font button is gone from the selected-words toolbar, because it did not let you choose a font. Edit,
  Size, Ink, Duplicate and Delete work as before. This is a simplification, not a measured usability result,
  and it does not rule out font choice later ([ADR-115](docs/DECISIONS.md#adr-115)).

### Fixed

- The Backups sheet now says when a backup was last saved on this phone, with the file's name when the storage
  app gives one, on its own line under the date, or that none has been saved yet. It no longer calls the zines
  "kept safe" or promises every zine before a backup has run. Back up and Restore now look like equal choices, and
  the note that only repeated them is gone ([ADR-120](docs/DECISIONS.md#adr-120)).
- A backup that fails now says where it failed. "Couldn't save the backup there" is shown only when the chosen
  location refused the file, and a problem inside Zinely says "Couldn't finish that backup", never restore's "This
  backup looks damaged". A failure that trying again can't fix offers only "Got it". After a failed or cancelled
  backup, Zinely tries to remove the empty or unfinished file it was saving to ([ADR-120](docs/DECISIONS.md#adr-120)).
- One zine Zinely can't open, or a photo that fails its check, no longer stops the whole backup. The rest is saved,
  and the result says so: "5 of 6 zines saved", naming up to three of the zines left out, and why. The backup file
  lists them too, and the Backups sheet's last-backup line adds "— 5 of 6 zines." When no zine can be saved, no
  file is kept and the sheet says "No zines could be saved". A backup that would leave out more than 10,000 zines
  is not saved at all, so a file never lists fewer than it left out. Restoring a partial backup says what it was
  saved without; versions of Zinely before this one restore it without saying so
  ([ADR-122](docs/DECISIONS.md#adr-122)).
- A restore now says what happened. A full phone while preparing it says "Not enough space", not "This backup
  looks damaged". Once zines start being added, Cancel goes away and the sheet says "This part can't be stopped.",
  so "Restore cancelled." is only ever shown when nothing was added. If the zines were added but the shelf hasn't
  caught up yet, it's a success that says they may take a moment to appear, not "Couldn't read that file"
  ([ADR-122](docs/DECISIONS.md#adr-122)).
- Pressing Cancel just as a backup finishes no longer says "Backup cancelled." about a file that was saved
  ([ADR-120](docs/DECISIONS.md#adr-120)).
- With TalkBack, the Bench now reads a page from top to bottom and left to right, whatever is stacked on top.
  A full-page photo or a spread half is read first. Moving something to the front or back, or making a spread,
  no longer changes the order ([ADR-119](docs/DECISIONS.md#adr-119)).
- With TalkBack, the dimmed area behind the zine actions sheet is no longer read as an unlabelled button. Tapping
  it or pressing Back still closes the sheet ([ADR-119](docs/DECISIONS.md#adr-119)).

### Known limitations

- Tapping through a holed piece was checked with scripted taps on one phone. It has not yet been checked by
  ear with TalkBack; the accessibility tree was unchanged in a device dump.
- A holed piece that sits over a photo is picked up by its drawn ink, or within a small distance of it. A very
  thin frame can take a careful tap. Once selected, dragging from its edge resizes it; drag from the middle of
  the selection to move it.
- A second tap on small selected words can land on a resize handle and not open the words for editing.
  Double-tap the words, or use **Edit**.
- With TalkBack, a repeated identical undo or redo line may be spoken only once.
- A backup saved by this version that left zines out is restored by earlier versions without saying so.
- Transparency in imported pictures isn't kept: the see-through parts are flattened onto white when the
  picture comes in. Exports and prints are right, because the paper is white. On the cream editor page,
  though, a transparent logo shows as a faint white rectangle, and placed over a photo it shows a white box.
- On Android 7–9, if you decline storage access and ask Android not to ask again, Save PDF keeps showing
  "Couldn’t make the PDF" until you allow storage for Zinely in the phone's settings. Share still works.
- With TalkBack on, **About**, **Licences & credits** and a font licence open with TalkBack on the Back
  button, and going Back returns TalkBack to the top of the previous screen.
- Replaced and deleted photo assets are retained; app storage is not reclaimed yet.
- Font choice remains unavailable. Unsupported print scripts are kept and warned about but do not print.
- Zinely saves the PDF; printing happens in the phone's PDF or print app.

## [0.9.0-beta.5] — 2026-09-23 — Steadier editing and saving

This maintenance release carries `versionCode 10`. It installs over earlier release-signed beta builds
without uninstalling, and it does not change the saved-document or backup format. It is published as an
APK only.

### Changed

- **About** now opens with a short note from Aastra, the two people behind Zinely, in place of the earlier
  tagline and introduction. The font licence notices moved behind one **Licences & credits** row, a tap
  away. Your default paper, the offline promise and the app version stay on the main About screen.
- Reframe opens a large photo more smoothly. The photo now loads off the main thread, and the on-screen
  preview is capped at 2048 px on its longest edge. Crop, Flip and the printed result still use the
  full-size photo.

### Fixed

- On Android 7–9, **Save PDF** now asks for storage access until you allow it. Before, it never asked, so every
  save on those versions failed with "Couldn’t make the PDF". If you decline, Zinely shows that same message
  and saves nothing; **Share** never needs this access. Android 10 and later are unchanged.
- On Android 7–9, a Save PDF that fails part-way no longer leaves an empty or broken file in Downloads.
- Sending a mirrored photo across the fold now puts each half on the correct page. Before, both halves were
  mirrored in place, so the printed spread broke at the fold. The fold warning stays hidden for a matched
  pair and comes back if you later flip only one half.
- An Undo that jumps to another page no longer keeps something selected from the page you left. Before, the
  element's actions stayed on screen and did nothing.
- A picture with a see-through background, such as a logo or sticker PNG, now comes in on white. Before, the
  see-through parts turned black.
- A move or resize that ends exactly where it started no longer wipes Redo or adds an empty Undo step.
- With TalkBack on, the dimmed area behind most sheets, such as **Start a zine** and **Backups**, is no longer
  read out as an unlabelled button, so TalkBack starts inside the sheet. Close a sheet with Back, as before.
  The zine actions sheet on the shelf still has this unlabelled area.

### Known limitations

- Transparency in imported pictures isn't kept: the see-through parts are flattened onto white when the
  picture comes in. Exports and prints are right, because the paper is white. On the cream editor page,
  though, a transparent logo shows as a faint white rectangle, and placed over a photo it shows a white box.
  Keeping transparency is future work.
- On Android 7–9, if you decline storage access and ask Android not to ask again, Save PDF keeps showing
  "Couldn’t make the PDF" until you allow storage for Zinely in the phone's settings. Share still works.
- With TalkBack on, **About**, **Licences & credits** and a font licence open with TalkBack on the Back
  button, and going Back returns TalkBack to the top of the previous screen rather than to the row or button
  that opened it. Everything stays reachable by swiping.
- Replaced and deleted photo assets are retained; app storage is not reclaimed yet.
- Font choice remains unavailable. Unsupported print scripts are kept and warned about but do not print.
- Zinely saves the PDF; printing happens in the phone's PDF or print app.

## [0.9.0-beta.4-r3] — 2026-08-31 — Readable dark-mode confirmations

This focused beta.4 maintenance revision carries `versionCode 9`. It installs over earlier
release-signed beta builds without uninstalling and changes no saved-document, rendering, export,
storage, or backup behavior.

### Fixed

- Restored WCAG 2 AA contrast for editor confirmation messages such as **Duplicated** and their
  **Undo** action in dark mode. The snackbar now keeps the room palette when it is positioned inside
  the light-paper sheet host: cream `#FFF9DB` on dark brown `#46352E` measures `10.96:1`, replacing
  the unreadable mixed-palette pair that measured `1.31:1`.
- Preserved the existing shared sheet, snackbar placement, wording, timing, semantics, Undo behavior,
  large-text reachability, and light-mode appearance.

### Known limitations

- Replaced and deleted photo assets are retained; app storage is not reclaimed yet.
- Font choice remains unavailable. Unsupported print scripts are kept and warned about but do not print.
- Zinely saves the PDF; printing happens in the phone's PDF or print app.
- The public privacy-policy page remains blocked on owner-provided publication details.

## [0.9.0-beta.4-r2] — 2026-08-31 — Smaller beta 4 maintenance package

This packaging-only beta.4 revision carries `versionCode 8`. It changes neither saved documents nor
product behavior and installs over earlier release-signed beta builds without uninstalling.

### Changed

- Enabled release-only R8 code shrinking and Android resource shrinking; debug builds remain unshrunk.
- Replaced the generated 2.55 MiB adaptive/splash PNG with a lossless WebP projection. The immutable
  `APP_LOGO.png` master is unchanged, and the generator verifies decoded-pixel identity.
- Preserved the bundled offline emoji font and shared rendering pipeline so editor, PNG, and PDF output
  retain the accepted emoji and print-parity behavior.

### Known limitations

- Replaced and deleted photo assets are retained; app storage is not reclaimed yet.
- Font choice remains unavailable. Unsupported print scripts are kept and warned about but do not print.
- Zinely saves the PDF; printing happens in the phone's PDF or print app.
- The public privacy-policy page remains blocked on owner-provided publication details.

## [0.9.0-beta.4-r1] — 2026-08-31 — Current-source beta 4 maintenance build

This is a maintenance revision of beta.4, not a new feature release. It carries `versionCode 7` so
Android can install the refreshed release-signed APK over beta.4 without uninstalling or deleting zines.

### Changed

- Rebuilt the unchanged beta.4 application from the current verified release branch after closing its
  release-checklist and residual Art-performance investigation records.
- Preserved beta.4's Replace Photo behavior, shared Add/Art host, rendering pipeline, accessibility
  contracts, saved-document format, and offline/privacy behavior without modification.
- Preserved the original beta.4 APK and checksum as a separate frozen tester artifact.

### Known limitations

- Replaced and deleted photo assets are retained; app storage is not reclaimed yet.
- Font choice remains unavailable. Unsupported print scripts are kept and warned about but do not print.
- Zinely saves the PDF; printing happens in the phone's PDF or print app.
- The public privacy-policy page remains blocked on owner-provided publication details.

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

[Unreleased]: https://github.com/aritr-codes/zinely-android/compare/v0.9.0-beta.4-r3...HEAD
[0.9.0-beta.4-r3]: https://github.com/aritr-codes/zinely-android/compare/v0.9.0-beta.4-r2...v0.9.0-beta.4-r3
[0.9.0-beta.4-r2]: https://github.com/aritr-codes/zinely-android/compare/v0.9.0-beta.4-r1...v0.9.0-beta.4-r2
[0.9.0-beta.4-r1]: https://github.com/aritr-codes/zinely-android/compare/v0.9.0-beta.4...v0.9.0-beta.4-r1
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
