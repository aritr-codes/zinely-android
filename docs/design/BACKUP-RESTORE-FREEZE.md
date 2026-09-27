# Backup / Restore UX — Design Freeze

Status: DESIGN FREEZE  
Date: 2026-08-22  
Canonical visual source: [mockups/backup-restore.html](mockups/backup-restore.html)

**D-079 amendment · 2026-08-24; copy amended 2026-08-29:** the Shelf has two equal quiet dock actions: this
flow's `Backups` / `Restore a backup`, and `About`. The sibling `About Zinely` destination (internally the
Colophon) owns the sole product-level offline/privacy sentence; this flow retains only operational
destination, additive-restore, and recovery copy.

**Amendment 1a · drawn 2026-09-26 · owner-approved and re-frozen 2026-09-27** (1.x steps 1 and 1b; spec
[Brief 01](../planning/BRIEF-01-VISIBLE-OWNERSHIP.md#backup-restorehtml-amendment-specification), rulings
[Q8](../planning/ZINELY-1X-DECISION-GATE.md#q8-the-next-release-and-the-backuprestore-defects)). The HTML
header lists every item and where it is drawn; this section records what the amendment freezes.

- **Chooser.** Title `Keep your zines` (the old title claimed safety). Body: *"The backup file holds the zines
  and photos on this shelf. Keep a copy somewhere other than this phone."* A last-backup fact sits between
  the body and the note, as secondary text: `No backup saved yet` · `Last backup saved 12 September 2026 ·
  <file name>` (the name only when the provider reports one) · `… — 5 of 6 zines.` for a partial backup. A
  date, never a relative time. Not shown on an empty shelf.
- **No promise of every zine before the result.** The save option reads *"Choose where to keep the backup
  file."* and the running backup body *"Putting zines together in one file."* Only a complete result says
  "All N zines…".
- **Partial backup is a success.** Title `N of M zines saved`, ✓ mark, no warning colour. It names the
  left-out zines (up to three, then "and N more") with one sentence per reason, in the order: couldn't open ·
  newer Zinely · couldn't read a photo. A zine with no readable name is counted, not named. `Some of these
  aren't on your shelf.` when a left-out zine has no shelf row.
- **A photo that fails its check is skip-and-list** (owner ruling 2026-09-26): its zines are left out and
  named with the photo reason, as a partial success. It is never "damaged" and never blames the location.
- **Backup failures use backup words**, never restore's `damaged` / `newer` copy: `No zines could be saved`
  (0 of M; a photo variant when every reason is a photo; "They may not appear on your shelf." only when a
  left-out zine has no row), `A zine here needs a newer Zinely`, and a part-1-only interim state that no
  release carries.
- **Where a whole backup failed decides its state (owner ruling F1, 2026-09-27)**, in this order:
  1. genuine out of space, anywhere → `Not enough space`;
  2. the completed archive can't be written or copied to the maker's chosen destination → `Couldn't save the
     backup there` / *"Pick another location and try again."*. Only ever for the chosen destination;
  3. a failure while Zinely builds, validates or privately cleans up the archive → `Couldn't finish that
     backup` / *"Nothing about the zines on this shelf was changed."*. Try again + Got it for retryable
     transient or local failures; **Got it only** for a deterministic archive-wide or library-wide limit;
  4. one photo's integrity or read failure → skip-and-list (above). Only when every zine is left out: the
     no-file `No zines could be saved` path.
- **Busy is not a failure (owner ruling F2, 2026-09-27).** `Give Zinely a moment` stays unchanged and is never
  folded into "not finished".
- **Retry only where a retry can help.** A failure no retry can fix (none saved, newer, the interim, a limit)
  offers only `Got it`.
- **Save-error and space copy** now match the shipped strings (`Copy.kt` `LibraryBackup`); the HTML's older
  "Your zines are still safe here." is gone.
- **Restore honesty.** A full disk while staging a restore shows `Not enough space` (restore retry: `Try
  another backup`). Once commit starts, Cancel leaves the tree and Back does nothing: body *"Adding zines to
  your shelf."*, hint *"This part can't be stopped."*. A committed restore whose shelf index lags says *"They
  may take a moment to appear. If they don't, close Zinely and open it again."*. Restoring a partial backup
  names what it was saved without, then *"What was already on this shelf stayed put."*.
- **Unchanged:** the empty-shelf variants, restore option, complete `saved` / `restored` results, and the
  restore error branches (`damaged`, `newer`, `read`, `generic`).
- **Known drift, not changed by 1a:** the frozen `saved` title "Your backup is saved" vs shipped "Backup
  saved"; the restore `generic` retry label and the `newer` / `read` bodies vs shipped copy.

This document freezes the first production `.zine` backup/restore user flow. The repository and
current V2.1 shelf were reviewed, the interactive HTML was critiqued on the Samsung SM_A176B, and
accessibility and large-text constraints were reviewed before this freeze. Compose must implement
this contract without inventing a second interaction model.

## Entry point

- The shelf does not gain a generic settings destination; its separate statement of craft is
  [`About Zinely`](COLOPHON-FREEZE.md), internally the Colophon surface.
- Backup / restore enters from the first of the dock's two quiet secondary actions.
- Content shelf label: `Backups`.
- Empty shelf label: `Restore a backup`.
- Loading and shelf-error states do not expose the trust action.
- `About` is the second quiet action beside it in content and empty states; loading and shelf-error
  states continue to answer their current question before exposing either utility.

Rationale:

- The dock is the only cross-state workspace chrome the current product already owns.
- The per-zine action sheet remains per-zine and does not absorb whole-library actions.
- The screen keeps one loud verb: `Make a zine`. Backup / restore stays secondary.

## Flow

### Sheet

- Tap the dock secondary action to open one library-level sheet.
- On a non-empty shelf, the sheet title is `Keep your zines` (amendment 1a; was `Your zines, kept safe`).
- On an empty shelf, the sheet becomes restore-only rather than offering a meaningless empty backup.
- On a non-empty shelf the sheet offers exactly two actions:
  - `Back up this shelf`
  - `Restore a backup`
- On an empty shelf it offers only `Restore a backup`.
- The sheet says restore is additive before the picker is opened.
- The sheet says that an ID collision returns as a separate copy rather than replacing a zine.
- The sheet explains the operation rather than repeating the product privacy promise: backups save as a
  file the user chooses, and restores add separate zines. The Android picker remains truthful about the
  selected provider.

### Backup

1. User chooses `Back up this shelf`.
2. Android opens the real save picker.
3. After the user chooses a destination, Zinely shows a running sheet.
4. The running sheet offers explicit cancellation.
5. Success returns a success sheet.
6. Failure returns an error sheet.

### Restore

1. User chooses `Restore a backup`.
2. Android opens the real open picker.
3. After the user chooses a file, Zinely shows a running sheet.
4. The running sheet says nothing changes until checks pass.
5. Success returns a success sheet.
6. Failure returns an error sheet.

## Restore semantics

- Restore is additive.
- Existing zines are preserved.
- The UI must never imply device-level replacement, wipe, or full-phone restore.
- The UI may say that restored zines are added to the current shelf.
- The UI may say that what is already on the shelf stays put.

## Cancellation

- Picker cancellation is silent.
- In-app cancellation is explicit. Restore cancellation leaves the shelf unchanged. Backup
  cancellation stops Zinely's work and must never claim that the user-selected provider removed an
  unfinished destination unless that cleanup is actually verified.
- Cancelling a running restore must not leave an error sheet behind.
- Cancellation feedback is lightweight rather than alarming.
- System Back while work is running is the semantic twin of the visible Cancel action.
- Process recreation keeps the operation state because the destination ViewModel owns it.
- Process death cancels the operation; reopening Zinely returns to the shelf rather than claiming success.

## Error model

The user sees product language, never storage/runtime jargon.

Frozen user-facing error families:

- Damaged / invalid backup
- Newer backup than this app can read
- Couldn’t save to that location
- Couldn’t read that file
- Not enough space
- Generic failure
- A brief writer-busy state while a zine is still being put away

Amendment 1a adds the backup-side families (none saved, newer zine here, not finished) and the F1 rule for
choosing between them; see [Amendment 1a](#backup--restore-ux--design-freeze) at the top of this document.

The retry action relaunches the appropriate picker rather than retrying hidden state.

## Accessibility

- Both quiet dock actions remain full controls, not decorative text.
- Running state exposes indeterminate progress semantics.
- Cancellation and retry are reachable without gestures.
- Success and error sheets remain understandable at large font sizes.
- The additive restore promise is visible text, not a tooltip or screen-reader-only note.
- Every sheet has a pane title and takes focus when shown. Focus returns to the dock action only after
  the chooser's exit animation has released its modal window.
- Sheet content scrolls rather than clipping at maximum font scale.
- The two error actions stack vertically from 1.5× font scale so neither label or touch target clips.
- Reduced motion replaces the moving paper sweep with a static progress treatment.

## Navigation and Android handoff

- `HomeDestination` owns Android document launchers; the feature screen remains free of `Uri`.
- Backup uses Android's create-document flow with a suggested `.zine` filename.
- Restore uses Android's open-document flow and passes the chosen `Uri` to the existing SAF adapter.
- Cancelling either system picker is a silent no-op.
- The visible sheet owns no ZIP, validation, repository, or Room logic.
- A single running operation is allowed. Starting backup/restore commits any pending undoable shelf
  deletes first so the operation matches the shelf the user can currently see.

## Result rules

- Backup success reports the number of zines saved.
- Restore success reports the number of zines added and says the existing shelf stayed put.
- A failed restore says the shelf was unchanged.
- A failed backup says the zines in Zinely were unchanged; it does not make unverifiable claims
  about a provider-owned destination.
- Retry returns to the relevant Android picker rather than reusing a hidden stale `Uri`.

## Compose constraints

- The implementation reuses the trusted backup repositories and SAF transport.
- The UI does not duplicate archive validation logic.
- No settings/Colophon logic is introduced in the backup package; `Backups` and `About` are sibling Shelf routes.
- The dock remains the only primary-action host on the shelf.

## Implementation status

The frozen flow is implemented in Compose with focused state, cancellation, focus, large-text, and
light/dark golden coverage. Repository, lint, debug, and release gates are green. Both production-UI device
passes are accepted on the Samsung SM-A176B / Android 16: a four-zine shelf was saved through the real
Android document picker, that exact file restored four additive copies, invalid input left the shelf intact,
picker cancellation was silent, restored content remained editable and autosaved across cold relaunch, and
the large-text and platform accessibility checks passed. The reproducible evidence is recorded in
[the device-verification report](../reviews/2026-08-22-backup-restore-ui-device-verification.md).
