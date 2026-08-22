# Backup/Restore production UI — device verification

**Date:** 2026-08-22  
**Build:** debug APK from the working tree based on `3725ad6`  
**Device:** Samsung SM-A176B (Galaxy A17 5G)  
**OS:** Android 16 / API 36  
**Result:** Pass 1 PASS · Pass 2 PASS

## Pass 1 — developer verification

- The production shelf exposed the quiet `Backups` action and opened the frozen chooser.
- The real Android `CreateDocument` picker saved a 1.68 MB `.zine` file to Downloads. Zinely reported
  `Backup saved` and correctly stated that all four shelf projects were included.
- The real Android `OpenDocument` picker selected that exact file. Restore added four collision-safe copies,
  moving the shelf from four to eight projects without replacing the existing four.
- A restored copy opened with its photo and the pre-backup `Restored_Edit_0822` text intact.
- Before the round trip, a restored project accepted that text edit, autosaved it, survived shelf navigation
  and a force-stop/cold launch, and rendered correctly in Proof.
- A known invalid 42-byte file produced the friendly damaged-backup state. The existing shelf count and
  projects remained unchanged.
- Save-picker and restore-picker cancellation returned silently to the shelf with no partial project.
- At font scale 2.0, the shelf, chooser, and damaged-backup state remained readable and reachable; the error
  actions stacked vertically as frozen. The device font scale was restored to 1.0 afterward.
- A fresh UIAutomator platform-tree dump exposed `Back up this shelf` and `Restore a backup` as enabled
  `android.widget.Button` nodes. Their measured bounds were `[42,1633][1038,1868]` and
  `[42,1868][1038,2151]`, comfortably exceeding the 48 dp touch-target floor on the reference device.
- `AndroidRuntime` contained no error entries after the exercised flows.

The platform accessibility structure is verified here. This report does not claim a human-ear TalkBack
utterance check; no backup/restore state relies on an uninspectable spoken-only distinction.

## Pass 2 — first-time user verification

- `Backups` reads as a library-level action without competing with `Make a zine`.
- The chooser answers both user goals in one place and states the additive rule before restore:
  existing zines stay put and matching zines return as separate copies.
- Backup completion says what was protected rather than describing archive mechanics.
- Restore completion names the number added and repeats that existing work stayed put.
- Invalid-file language is calm and actionable; no ZIP, hash, schema, Room, staging, or transaction language
  reaches the user.
- Picker cancellation is uneventful, which is the expected outcome rather than an error state.

No confusion, misleading replacement promise, blocked action, clipping, or device-specific interaction defect
was observed. Both passes are accepted.

## Device-side test residue

The approved verification intentionally left `zinely-backup-ui-final-2026-08-22.zine` in Downloads. Because
restore is additive, the device shelf now contains eight projects. One restored photo project contains the
clearly identifiable `Restored_Edit_0822` test text. No project or user file was deleted.
