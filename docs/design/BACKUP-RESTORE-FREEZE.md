# Backup / Restore UX — Design Freeze

Status: DESIGN FREEZE  
Date: 2026-08-22  
Canonical visual source: [mockups/backup-restore.html](mockups/backup-restore.html)

This document freezes the first production `.zine` backup/restore user flow. The repository and
current V2.1 shelf were reviewed, the interactive HTML was critiqued on the Samsung SM_A176B, and
accessibility and large-text constraints were reviewed before this freeze. Compose must implement
this contract without inventing a second interaction model.

## Entry point

- The shelf does not gain a settings destination.
- Backup / restore enters from the dock's quiet secondary action.
- Content shelf label: `Backups`.
- Empty shelf label: `Restore a backup`.
- Loading and shelf-error states do not expose the trust action.

Rationale:

- The dock is the only cross-state workspace chrome the current product already owns.
- The per-zine action sheet remains per-zine and does not absorb whole-library actions.
- The screen keeps one loud verb: `Make a zine`. Backup / restore stays secondary.

## Flow

### Sheet

- Tap the dock secondary action to open one library-level sheet.
- On a non-empty shelf, the sheet title is `Your zines, kept safe`.
- On an empty shelf, the sheet becomes restore-only rather than offering a meaningless empty backup.
- On a non-empty shelf the sheet offers exactly two actions:
  - `Back up this shelf`
  - `Restore a backup`
- On an empty shelf it offers only `Restore a backup`.
- The sheet says restore is additive before the picker is opened.
- The sheet says that an ID collision returns as a separate copy rather than replacing a zine.
- The sheet repeats the privacy promise truthfully: Zinely does not send a backup anywhere on its
  own, while the user may deliberately choose a local or cloud-backed document provider.

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

The retry action relaunches the appropriate picker rather than retrying hidden state.

## Accessibility

- The dock secondary action remains a full control, not decorative text.
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
- No new settings branch is introduced in this package.
- The dock remains the only primary-action host on the shelf.

## Implementation status

The frozen flow is implemented in Compose with focused state, cancellation, focus, large-text, and
light/dark golden coverage. Repository, lint, debug, and release gates are green. Both production-UI device
passes are accepted on the Samsung SM-A176B / Android 16: a four-zine shelf was saved through the real
Android document picker, that exact file restored four additive copies, invalid input left the shelf intact,
picker cancellation was silent, restored content remained editable and autosaved across cold relaunch, and
the large-text and platform accessibility checks passed. The reproducible evidence is recorded in
[the device-verification report](../reviews/2026-08-22-backup-restore-ui-device-verification.md).
