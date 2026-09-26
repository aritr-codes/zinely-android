# Brief 01 — Visible ownership (Shelf)

> This brief incorporates the findings of [ZINELY-1X-READINESS-AUDIT.md](ZINELY-1X-READINESS-AUDIT.md). If this brief conflicts with an older research document, this brief and the cited authoritative ADR/decision take precedence. It never overrides an Accepted ADR, the V2 constitution or a frozen spec — where it needs one changed, it says so and names the amendment.

Status: **implementation brief, not authorised.** Direction D1 of the [1.x plan](ZINELY-1X-IMPLEMENTATION-PLAN.md).
Base: `origin/main` @ `eb75cf7` (Step 0 merged; code citations made at `5f7707a`, `src/main` unchanged since). Revised 2026-09-25 from the readiness audit and the D1 deep
audit ([audit §5](ZINELY-1X-READINESS-AUDIT.md#5-d1-audit--last-backed-up--changing-phones)).

**Readiness, by part:**

| Part | What | Readiness |
|---|---|---|
| **1** | "Last backup saved" fact, honest backup failures, no partial file left behind | **READY AFTER** the `backup-restore.html` amendment, which the owner approves: it changes copy inside frozen states (the title, the failure wording). No other owner ruling is needed |
| **2** | "Changing phones?" line | **BLOCKED BY** the physical cross-device restore pass |
| **3** | Android device-to-device transfer | **BLOCKED BY** O1. Research leans to not doing it |

## Gates before an implementation session may start

Part 1:
1. **Work (design), then owner approval:** an amendment to `docs/design/mockups/backup-restore.html` covering
   every state in [UI/UX proposal](#uiux-proposal), then review, owner approval and re-freeze. It adds states and
   changes copy inside existing ones, so it is not purely additive.
2. **✅ Done — step 0** of the [plan sequence](ZINELY-1X-IMPLEMENTATION-PLAN.md#5-sequencing), the F3 fixture archive
   (PR #75; `LibraryBackupFixtureTest`, `core/data-storage/src/test/resources/fixtures/library-backup-v2.zine`).
   - It pins compatibility with the beta.4-r3 and beta.5 backup files already in makers' hands.
   - It was never a hard prerequisite of part 1, whose code does not touch the archive. Part 1 must keep it green
     and must not regenerate it; the fixture is frozen.

Part 2:
3. **Work (physical):** a cross-device restore pass.
   - Two phones on different Android versions, and a real library with photos, made from fictional content.
   - Checked: documents byte-equal, covers, created and updated times, photo bytes, and the zine count.
   - Record it in [DEVICE-VERIFICATION.md](../DEVICE-VERIFICATION.md).

Part 3:
4. **Owner:** O1 ([decision gate](ZINELY-1X-DECISION-GATE.md#decisions-that-can-wait)). If the answer is
   "enable transfer", it also needs an ADR amending [ADR-030](../DECISIONS.md#adr-030) §7, and device evidence:
   - a Samsung Smart Switch pass and a Pixel pass, with the APK pre-installed on the new phone;
   - proof that file modification times survive;
   - rules that also exclude pending deletes (`shared_prefs/zinely_pending_deletes.xml`), `files/.library-restore/`,
     `files/assets/.tmp` and the last-backup key. `database` should not travel: reconcile keeps stale rows
     (`RoomProjectRepository.kt:669-688`).

## Problem

A maker's zines exist only inside Zinely on one phone. Android's cloud backup and phone-to-phone transfer are
both excluded (ADR-030 §7). Uninstalling deletes everything.

The whole-library `.zine` backup ([ADR-110](../DECISIONS.md#adr-110)) works on one device and is careful:
- additive;
- fail-closed;
- journalled;
- content-addressed.

But it is invisible until needed, and three things around it mislead:

- **Nothing says whether, or when, a backup was ever saved.** Meanwhile the sheet's title, "Your zines, kept
  safe" (`Copy.kt:1495`), claims safety before any backup exists.
- **A backup that fails reads like a restore that failed.**
  - One zine Zinely cannot open (damaged, invalid, or from a newer version) aborts the whole backup
    (`RoomProjectRepository.kt:431-433`).
  - The maker is then told "This backup looks damaged — Zinely couldn't safely bring anything back from that
    file" (`HomeViewModel.kt:469-486`; sheet `LibraryBackupRestoreSheet.kt:323-341`). That is restore wording
    for a backup failure. A zine from a newer Zinely (after a downgrade) gets "This backup needs a newer
    Zinely" (`HomeViewModel.kt:474`, `Copy.kt:1523`), also restore wording.
- **The zine that breaks the backup may not be findable.** Two cases:
  - already unreadable when it was indexed (e.g. restored or copied in damaged, or from a newer build): it never
    gets a shelf row, so it is invisible (`RoomProjectRepository.kt:674-676`, `syncRowFromDisk` :604-605);
  - damaged after it was indexed: it keeps its row (rows go only on delete, :300, or when the file is gone,
    :687), so it is on the shelf but fails to open.

  Either way the backup walks the project ids on disk (:419, :431-433), *Back up* is offered
  (`canBackup = zines.isNotEmpty()`, `ZineLibraryScreen.kt:456`), and every backup fails. In the first case
  nothing on the shelf points at the cause.
- **A failed backup can leave a file behind.** The picker creates the file before the write; on failure the
  empty or partial file stays where the maker put it (`LibrarySafTransport.kt:101-132`).

## Product audit

### User value

This is the only protection a maker has against losing every zine to a lost, broken or replaced phone. The
loss is total and irreversible, and today it is silent. 🟨 For existing beta users it matters more than any
new creative feature (an assumption from the size of the loss, not measured).

### Creative value

None directly. Indirectly it is large: a maker who trusts that the work survives makes more of it. It does
not make Zinely more expressive, and it should not pretend to.

### UX cost

- One dated line and a few reworded error states, all on a sheet the maker opens on purpose.
- No new screen, no new control, no settings.
- The cost is design time: one HTML amendment.

### Cognitive load

Low if it stays a **fact**, not a **task**. The danger is turning it into a nag:
- a badge;
- a reminder;
- a red "not backed up" state;
- "X days ago".

Each of those converts a fact into guilt, and the constitution forbids guilt mechanics
([Article 4](../zinely-constitution.md), "The quiet").

### Accessibility

- Plain text in reading order, before the action it explains.
- The date is spoken as a date.
- No colour-only state.
- It must work at 200 % font scale.
- The error states are sheet text, reachable by swiping, not a transient toast.

### Offline-first and ownership

This direction *is* the ownership model made visible:
- Nothing leaves the phone except by the maker's own action.
- The archive stays a file the maker chooses where to keep.
- Device state (the last-backup date) never enters the archive.

Research leans towards keeping transfer excluded (Part 3), because enabling it would loosen the privacy stance
for little gain.

### Physical publishing (PDF and paper)

Not applicable. Backups carry documents and photos, not PDFs.

### Product identity

"Imperfect surface, perfect mechanics" is the owner's evaluation lens. Its formal ratification as research
proposal D13 is still open.

Backup is pure mechanics, and the lens says those must be exact. Today they are exact underneath (staging,
fsync, journal, rollback) but **not exact in what they say**:
- a title that claims safety;
- backup errors in restore words;
- a failure that points at nothing the maker can find.

Fixing the words is the "perfect mechanics" half of the identity. Nothing here risks generic-editor drift.

## What others teach us

- ✅ **Privacy-first apps don't rely on system transfer.** They hand the user a file and say plainly that it is
  theirs:
  - KeePassDX: "copy the password database file to another phone" ([FAQ](https://github.com/Kunzisoft/KeePassDX/wiki/FAQ));
  - Joplin: JEX is "a lossless format… convenient for backup purposes" ([docs](https://joplinapp.org/help/apps/import_export/)).

  Zinely's `.zine` already matches. The lesson is to say where the file is and whose it is, not to add sync.
- ✅ **Signal's help is blunt about loss:** "You will be unable to restore a backup without the passphrase" ([support](https://support.signal.org/hc/en-us/articles/360007059752-Backup-and-Restore-Messages)).
  Honest statements of what the file does and doesn't do are normal in this category. They don't read as
  alarming.
- ✅ **The system transfer path skips side-loaded apps.**
  - Google's "Copy apps & data" does not copy apps that aren't from Google Play ([Google Help](https://support.google.com/android/answer/13761358)).
  - On API 31+, AOSP ignores `allowBackup="false"` for device transfer. Only `<device-transfer>` exclusions
    guard it ([Android 12 changes](https://developer.android.com/about/versions/12/backup-restore)).

  So, for Zinely as distributed, "Android will move it for you" would be false.
- Not learned from Canva and similar tools: cloud accounts and automatic sync are the opposite of Zinely's
  model and are out of scope by principle.

## User story

*As a maker with a shelf of zines, I want to see when and under what name I last saved a backup, and to be
told the truth when a backup can't be made, so that I don't believe a backup exists when it doesn't.*

## Experience

- **After a backup has been saved:** the Backups sheet shows **"Last backup saved 12 September 2026"**, and
  the file's name when the storage provider reports one (🟦 `OpenableColumns.DISPLAY_NAME` on the returned
  URI; the folder is not reliably knowable, so it is not shown).
  - The words are 🟦 a draft for the HTML amendment. It reports when a file was saved, never that the zines
    are "safe".
- **If none has ever been saved:** **"No backup saved yet"**, as a fact in secondary text, not a warning.
- **The sheet title.** 🟦 "Your zines, kept safe" is reconsidered in the amendment. For example, "Keep your
  zines" states the purpose without claiming the result.
- **One sentence on what the file is.** 🟦 For example: "The backup file holds every zine and photo on this
  shelf. Keep a copy somewhere other than this phone."
- **When a backup can't be made because a zine can't be opened,** the sheet says that in backup words, and
  covers both cases without claiming which. 🟦 For example: "A zine on this phone can't be opened, so no backup
  was saved. It may not appear on your shelf." 🟨 Naming the zine would need the failing id carried to the UI
  and a title that may itself be unreadable; the amendment decides whether to try. From a newer Zinely: "A zine on this phone was made by a newer Zinely.
  Update Zinely, then back up."
  - **Way forward.** For a newer-version zine, updating is the way forward. For a damaged zine there is none
    in part 1: an invisible one can't be seen or deleted, and a visible one can be deleted only by a maker who
    knows it is the cause. A **skip-and-list backup** (back up the readable zines and say
    how many were left out) is the real remedy, but it changes what a backup contains, so it needs an ADR
    amending [ADR-110](../DECISIONS.md#adr-110) and an owner call
    ([Q8](ZINELY-1X-DECISION-GATE.md#q8-the-next-release-and-the-backuprestore-defects)). Part 1 only makes the
    failure true.
- **After a failed backup,** the empty or partial file is deleted (🟦 `DocumentsContract.deleteDocument`
  in the transport, best-effort), so the maker isn't left holding a file that looks like a backup.
- **Part 2 only, after the cross-device pass:** **"Changing phones? Save a backup here, then restore it in
  Zinely on the new phone."**
  - It must not imply the new phone may run an older Zinely. A newer backup refuses to restore on an older
    build ([audit §4 F1](ZINELY-1X-READINESS-AUDIT.md#4-wave-0-audit)).
- **No badges, dots, notifications, reminders or relative times** (constitution Article 4).

## UI/UX proposal

`docs/design/mockups/backup-restore.html` is **frozen** (2026-08-22). Amend it first, additively, and
re-freeze. The amendment must draw:

1. **The last-backup line: its two states, the file name, and its position.** Place it relative to both existing elements,
   the `SHEET_BODY` subtitle and the `DESTINATION_NOTE` pill (`LibraryBackupRestoreSheet.kt:77-101`).
2. **The title decision.** Keep it, or replace it.
3. **The "what this file holds" sentence.**
4. **Backup-failure states in backup wording.** Draw them separately from the restore failures, which keep
   their current wording:
   - a zine on this phone can't be opened (it may or may not be on the shelf);
   - a zine was made by a newer Zinely;
   - no space, "save there" failed, busy: these already use backup-neutral copy (`Copy.kt:1528-1533`); the
     amendment confirms them rather than redrawing them.
5. **The title and every changed string at 360 dp and 200 % text.**
6. **Part 2 (a later amendment):** the "Changing phones?" line, directly above *Back up this shelf*.

The Shelf itself does not change. The dock's *Backups* entry keeps its label. The website's Download page
(`#new-phone`) already gives the same advice; keep the two consistent.

## Interaction details

- **Writing the date.** It is written **only** in the `DataResult.Success` branch of `HomeViewModel.backupPicked`
  (`HomeViewModel.kt:251`). All other outcomes are distinct and write nothing:
  - failure (`:255`);
  - cancel (the catch at `:448-461`);
  - picker cancel (`:248`);
  - picker failure (`:279`).
- **What "success" means.** Only that the storage provider accepted the stream without error
  (`LibrarySafTransport.kt:123-130`). There is no read-back, and a cloud provider may upload later. That is
  why the copy says "saved", not "safe" or "uploaded".
- **Restore does not change the date.** A restore is not a backup of this shelf.
- **Date format:** the device locale's medium date. No relative time ("3 days ago"): it becomes false while
  the sheet is open, and it reads as a nag.
- **Empty shelf.** "Empty" means **no visible zines**; then show neither line, as today (*Back up* isn't
  offered). A shelf whose only zines are unreadable looks empty; that is the existing ADR-042 limitation, not
  part 1's to fix.
- **No "changed since your last backup" state in part 1.** The obvious test, any zine's recency newer than the
  backup, is wrong:
  - Restored zines keep their source time (`RoomProjectRepository.kt:560`).
  - A restore after the last backup would add zines that aren't in it, and nothing would say so.
  - Recency is also a display value, `max(row, document mtime)` (`:817-825`).
  - If wanted later, compare the shelf's zine ids with the ids in the last backup. That is 🟦 future work.

## Current architecture touchpoints

Re-verified on `5f7707a`.

| Concern | Where |
|---|---|
| Backup sheet UI | `feature/editor/.../feature/library/LibraryBackupRestoreSheet.kt` (`onSaveBackup` :67/:111; text :77-101; error rendering :323-347) |
| Backup orchestration | `app/.../home/HomeViewModel.kt` (`startBackup` :236, `backupPicked` :246, success :251, error mapping :469-486, suggested filename :488); `app/.../editor/ZinelyNavHost.kt:193-197` (SAF `CreateDocument`) |
| Backup-capable check | `feature/editor/.../ZineLibraryScreen.kt:283,456` (`canBackup = zines.isNotEmpty()`; visible zines only). Unreadable zines have no row: `RoomProjectRepository.kt:604-605,674-676` |
| Transport | `data-android/.../LibrarySafTransport.kt` (`backupTo` :101, stream :115-132) |
| Writer, lease and closure | `data-android/.../RoomProjectRepository.kt` (`createLibraryBackup` :404-505; aborts on an unreadable zine :431-433); `core/data-storage/.../ZineLibraryBackupWriter.kt` |
| Copy | `core/copy/.../Copy.kt` `object LibraryBackup` (:1491–~:1541; title :1495; "needs a newer Zinely" :1523; neutral backup failures :1528-1533) |
| Local preferences | `data-android/.../di/PreferencesModule.kt:39-62` (one `@Singleton DataStore<Preferences>`, file `files/datastore/editor_onboarding.preferences_pb`); pattern to copy: `DataStoreEditorOnboardingStore.kt:36-48` |
| Transfer rules | `app/src/main/AndroidManifest.xml:13-15`; `res/xml/data_extraction_rules.xml`, `backup_rules.xml` |

## Files/modules likely affected

- `core/copy`: the last-backup line (two states), the title if changed, the "what this file holds" sentence,
  and backup-failure strings separate from the restore ones.
- `data-android`: a `BackupRecordStore` (the date and, if reported, the file name) on the **existing**
  DataStore singleton. A second DataStore on the same file throws. Its interface lives beside the existing ones
  in `data-android/prefs` (`EditorOnboardingStore.kt`, `PreferredPaperStore.kt`), matching
  [plan §7](ZINELY-1X-IMPLEMENTATION-PLAN.md#7-ios-preparation-architecture-only).
- `data-android` transport: `LibrarySafTransport.backupTo` deletes the created document when the write fails
  or is cancelled **before the stream has closed successfully**. Once the stream has closed successfully it never
  deletes: a late Cancel must not destroy a complete backup. (Its "Backup cancelled." message is a step-1b defect.)
- `app`: write the record on success. In backup mode, map `Corrupt`/`Invalid` and `SchemaTooNew` to the two
  new backup-wording kinds (`HomeViewModel.kt:469-486`), separately from restore; expose the record to Home
  state.
- `feature/editor` (`feature/library` package): render the line and the new states.
- **Not touched:** the archive format, writer, stager, committer, `data_extraction_rules.xml` and
  `backup_rules.xml`.

## Data model implications

**None in documents, no schema change.** The timestamp is device state:
- It must never enter `ZineDocument`, `meta.json` or the archive, because a restored backup would otherwise
  claim a backup time from another phone.
- DataStore lives in app-private storage, which is excluded from cloud backup and device transfer today. So
  the fact honestly resets on a new phone.
- If Part 3 ever enables transfer, the rules must exclude this key's file. A separate DataStore file, or
  `noBackupFilesDir`, would keep preferred paper transferable while excluding the date.

## Testing strategy

| Level | What | Proves |
|---|---|---|
| JVM `core/copy` | Date line, both states; failure strings | No string says "safe", "automatic", "cloud" or "synced"; backup failures don't reuse restore wording |
| JVM store | Fake-backed `BackupRecordStore` | Written on success; untouched on cancel, failure, picker cancel, picker failure and restore |
| ViewModel (fakes) | Given a successful backup, When the sheet reopens, Then the line shows today's date. Given an unreadable zine on disk, When backup runs, Then the failure is the backup-wording state; Given a newer-version zine, Then the update state | Branch correctness, including the error mapping (untested today) |
| JVM/Robolectric transport | Given a Cancel after the stream closed successfully, Then the file is kept. Given a write that fails or is cancelled before that, Then the created document is deleted | No partial file left behind |
| Compose semantics + Roborazzi | Every amended state, light and dark, at 200 % font | Parity with the amended HTML |
| Device (Pass 1 + 2) | Samsung SM-A176B: save a backup to Files and to a cloud provider; reopen; TalkBack reads the line before the action | The platform tree, and the maker's understanding |
| **Device (Part 2 only)** | Cross-device restore pass (Gate 3) | The "Changing phones?" promise is true |

Automatable: everything except TalkBack speech and the cross-device restore. **Compose semantics tests do not
prove TalkBack behaviour.**

## Accessibility considerations

- The line and the explanatory sentence come before the action they explain, in reading order.
- The date is spoken as a date ("Last backup saved 12 September 2026").
- Failure states are sheet text, reachable by swiping, not toasts that disappear.
- No colour-only state.
- Large text: the lines wrap, and actions stay at least 48 dp.

## Design-system implications

None new: existing sheet text styles and tokens. No icon, no warning colour. A missing backup is not an error
state; a failed backup is.

## iOS portability notes

- The "last backup" record is behind a small interface. DataStore stays in `data-android` (coupling rule 3), so
  an iOS port swaps the store, not the logic.
- Failure-to-copy mapping belongs in pure code (`core/copy` strings plus a pure mapper), not in a Composable.
- Don't add platform date formatting to core. Format at the UI edge.

## Acceptance criteria

1. The amended `backup-restore.html` is reviewed and re-frozen before any Compose work.
2. After a successful backup, the sheet shows that day's date. Cancel, failure, picker cancel, picker failure
   and restore leave it unchanged.
3. With zines on the shelf and no backup ever saved, the sheet shows the "no backup saved yet" state. An empty
   shelf shows neither line.
4. A backup that fails because a zine can't be read shows the backup-wording state, never "This backup looks
   damaged"; a newer-version zine shows the update state, never "This backup needs a newer Zinely". Restore
   failures keep their current wording.
5. A failed or cancelled backup leaves no file at the chosen location (where the provider allows deletion).
6. No string in the sheet says or implies "safe", "automatic", "cloud" or "synced" about a backup the app
   cannot verify.
7. No new permission, no network, no notification, no background work, no schema change, no archive-format
   change.
8. Pixel parity with the amended HTML. Both device passes. TalkBack reads the line before the action.
9. **Part 2 only:** the "Changing phones?" line ships only after Gate 3 passes and is recorded.
10. Public copy (website Download page) and in-app copy say the same thing.

## Stop conditions

- The fix appears to need a change to the archive format, the writer, the stager or the transfer rules
  (skip-and-list is an ADR-110 change, not part 1).
- The design calls for a reminder, badge, notification, relative time or safety claim.
- Fixing the backup-failure wording seems to require changing **restore** behaviour. The restore defects in
  [audit §5](ZINELY-1X-READINESS-AUDIT.md#5-d1-audit--last-backed-up--changing-phones) are separate
  follow-ups.
- Part 2 work starts before Gate 3 is recorded.

## Out of scope

- Automatic or scheduled backups; backup to a cloud provider.
- Single-zine sharing; changing what a backup contains; a Shelf badge; encryption.
- The "changed since last backup" state.
- The restore-side defects the D1 audit found:
  - a cancel during commit that says "cancelled";
  - a post-commit failure that says "couldn't read";
  - a full disk read as "damaged";
  - a Cancel that lands after the stream finished, which says "Backup cancelled." although a complete file was
    saved;
  - no janitor for leftover staging files.
- Skip-and-list backups (an ADR-110 change; [Q8](ZINELY-1X-DECISION-GATE.md#q8-the-next-release-and-the-backuprestore-defects)).

  These are engineering follow-ups; [Q8](ZINELY-1X-DECISION-GATE.md#q8-the-next-release-and-the-backuprestore-defects)
  asks the owner to schedule them (plan step 1b).
- Device-to-device transfer (Part 3) unless O1 says otherwise.

## Future extension

- A "changed since your last backup" fact based on zine ids, not times.
- A PDF and a README inside each backed-up zine (research §18.3).
- Single-zine `.zine` share and import (the unused v1 package).
- Making the cross-device restore pass a standing release gate for every later backup change.

## Change log

| Date | Change |
|---|---|
| 2026-09-25 | Written at `5c40e7b`. |
| 2026-09-26 | Base moved to `eb75cf7`: Step 0 merged (PR #75); gate 2 (F3 fixture archive) marked done, and part 1 must keep the frozen fixture green. No scope change. |
| 2026-09-25 | Final planning review: an unreadable zine may or may not be visible (damaged after indexing keeps its row), so the copy covers both; partial-file deletion never deletes after a successful close (late Cancel); the earlier row's "`canBackup` brought into scope" is superseded (no `canBackup` change). |
| 2026-09-25 | After both reviews: an unreadable zine is invisible on the shelf, so the `canBackup` fix and state 5 are dropped and the failure says the zine isn't on the shelf; newer-version backup wording added; partial-file cleanup moved into part 1; skip-and-list routed to Q8 as an ADR-110 change; owner approves the amendment (copy changes); file name and year in the line; Article 4 cited; store interface placed in `data-android/prefs`. |
| 2026-09-25 | Re-based on `5f7707a`; folded in the readiness audit and the D1 deep audit: split into parts 1–3 with readiness per part; F3 no longer a prerequisite; "confirmed" redefined as "saved"; staleness state removed (restores keep source times); backup-failure wording and `canBackup` brought into scope; the title's safety claim flagged; option A's rule gaps and the side-loaded transfer reality recorded; product audit added. |
