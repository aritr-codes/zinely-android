# Brief 01 — Visible ownership (Shelf)

> This brief incorporates the findings of [ZINELY-1X-READINESS-AUDIT.md](ZINELY-1X-READINESS-AUDIT.md). If this brief conflicts with an older research document, this brief and the cited authoritative ADR/decision take precedence. It never overrides an Accepted ADR, the V2 constitution or a frozen spec — where it needs one changed, it says so and names the amendment.

Status: **implementation brief, not authorised.** Direction D1 of the [1.x plan](ZINELY-1X-IMPLEMENTATION-PLAN.md): plan
steps **1** (part 1) and **1b** (restore honesty and explicit partial backups).
Base: `origin/main` @ `0aa7a7d`. Code citations re-verified there (`src/main` unchanged since `5f7707a`; beta
tags checked where stated). Revised 2026-09-26 from the owner's rulings
([decision gate Q8](ZINELY-1X-DECISION-GATE.md#q8-the-next-release-and-the-backuprestore-defects)); earlier revisions from
the readiness audit and the D1 deep audit ([audit §5](ZINELY-1X-READINESS-AUDIT.md#5-d1-audit--last-backed-up--changing-phones)).

**Readiness, by part:**

| Part | What | Readiness |
|---|---|---|
| **1** | "Last backup saved" fact, honest backup failures, no partial file left behind, no "Backup cancelled." after a complete file | **READY AFTER** the one `backup-restore.html` amendment ([spec](#backup-restorehtml-amendment-specification)) is drawn and **owner-approved** (it changes copy inside frozen states) |
| **1b** | Restore honesty (three defects) and skip-and-list backups — complete or explicitly partial, never silently partial | **READY AFTER** part 1 has merged, the same amendment is approved, **and** the [ADR draft](#adr-draft--amendment-to-adr-110) and [product-law draft](#product-law-amendment-draft) are independently reviewed and landed in the implementation session, before any code |
| **2** | "Changing phones?" line | **BLOCKED BY** the physical cross-device restore pass |
| **3** | Android device-to-device transfer | **BLOCKED BY** O1. Research leans to not doing it |

## Gates before an implementation session may start

Owner rulings: **Q8, approved 2026-09-26**, recorded in the
[decision gate](ZINELY-1X-DECISION-GATE.md#q8-the-next-release-and-the-backuprestore-defects). Q8 scheduled part 1b and
skip-and-list in wave 1, moved the late "Backup cancelled." into part 1, and set the product rule. A
supplementary ruling the same day (after the final planning audit) put a single poisoned or unreadable photo
under skip-and-list ([below](#a-photo-that-fails-its-check)). No further owner *ruling* is needed for parts 1
and 1b; the owner still approves the drawn HTML amendment.

Parts 1 and 1b:
1. **Work (design), then owner approval:** one amendment to `docs/design/mockups/backup-restore.html` covering
   every state in [the amendment specification](#backup-restorehtml-amendment-specification), then review, owner
   approval and re-freeze. It adds states and changes copy inside existing ones, so it is not purely additive.
2. **✅ Done — step 0** of the [plan sequence](ZINELY-1X-IMPLEMENTATION-PLAN.md#5-sequencing), the F3 fixture archive
   (PR #75; `LibraryBackupFixtureTest`, `core/data-storage/src/test/resources/fixtures/library-backup-v2.zine`).
   - It pins compatibility with the beta.4-r3 and beta.5 backup files already in makers' hands.
   - Parts 1 and 1b must keep it green and must **never regenerate** it; the fixture is frozen.

Part 1b only:
3. **Work (review), in the implementation session, before code:** the [ADR draft](#adr-draft--amendment-to-adr-110)
   and the [product-law draft](#product-law-amendment-draft) are independently reviewed (Review Agent) and landed
   in `DECISIONS.md` and `docs/zinely-v1.md`. `zinely-v1.md` outranks ADRs (`docs/zinely-v1.md:3`), so the two
   land together.

Part 2:
4. **Work (physical):** a cross-device restore pass.
   - Two phones on different Android versions, and a real library with photos, made from fictional content.
   - Checked: documents byte-equal, covers, created and updated times, photo bytes, and the zine count.
   - Record it in [DEVICE-VERIFICATION.md](../DEVICE-VERIFICATION.md).

Part 3:
5. **Owner:** O1 ([decision gate](ZINELY-1X-DECISION-GATE.md#decisions-that-can-wait)). If the answer is
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

But it is invisible until needed, and several things around it mislead:

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
  nothing on the shelf points at the cause. **The maker has no remedy short of a code change** — part 1b's
  skip-and-list is that remedy.
- **A failed backup can leave a file behind.** The picker creates the file before the write; on failure the
  empty or partial file stays where the maker put it (`LibrarySafTransport.kt:101-132`).
- **A late Cancel says "Backup cancelled." about a complete file.** ✅ After `output.use { copyBounded(…) }`
  closes the stream (`LibrarySafTransport.kt:124-125`), `DataResult.Success` is returned from `withContext(io)`
  (:130). A cancel landing then makes `withContext` throw on return, and the ViewModel's catch
  (`HomeViewModel.kt:448-460`) shows `BACKUP_CANCELLED` (`Copy.kt:1540`) although the file was saved.
- **Restore says things that did not happen** (part 1b, [below](#part-1b--restore-honesty-and-explicit-partial-backups)).

## Product audit

### User value

This is the only protection a maker has against losing every zine to a lost, broken or replaced phone. The
loss is total and irreversible, and today it is silent. 🟨 For existing beta users it matters more than any
new creative feature (an assumption from the size of the loss, not measured).

### Creative value

None directly. Indirectly it is large: a maker who trusts that the work survives makes more of it. It does
not make Zinely more expressive, and it should not pretend to.

### UX cost

- One dated line and a few reworded states, all on a sheet the maker opens on purpose.
- Part 1b: a success state that can name left-out zines, a restore notice for a partial backup, and three
  corrected restore outcomes.
- No new screen, no new control, no settings.
- The cost is design time: one HTML amendment.

### Cognitive load

Low if it stays a **fact**, not a **task**. The danger is turning it into a nag:
- a badge;
- a reminder;
- a red "not backed up" state;
- "X days ago".

Each of those converts a fact into guilt, and the constitution forbids guilt mechanics
([Article 4](../zinely-constitution.md), "The quiet"). A partial backup is reported once, as a fact with names,
and remembered in the last-backup line — never escalated.

### Accessibility

- Plain text in reading order, before the action it explains.
- The date is spoken as a date.
- No colour-only state.
- It must work at 200 % font scale, including a list of left-out zine names.
- The error states are sheet text, reachable by swiping, not a transient toast.

### Offline-first and ownership

This direction *is* the ownership model made visible:
- Nothing leaves the phone except by the maker's own action.
- The archive stays a file the maker chooses where to keep.
- Device state (the last-backup date) never enters the archive. The *archive's own* partial state does
  ([part 1b](#how-the-partial-state-is-recorded)): it describes the file, not the phone.

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
- a failure that points at nothing the maker can find;
- restore outcomes that contradict what was committed.

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

*As a maker with a shelf of zines, I want to see when and under what name I last saved a backup, to be told
the truth when a backup can't be made or leaves something out, and to be told the truth about what a restore
did, so that I don't believe a backup exists — or is complete — when it isn't.*

## Experience

- **After a backup has been saved:** the Backups sheet shows **"Last backup saved 12 September 2026"**, and
  the file's name when the storage provider reports one (🟦 `OpenableColumns.DISPLAY_NAME` on the returned
  URI; the folder is not reliably knowable, so it is not shown).
  - The words are 🟦 a draft for the HTML amendment. It reports when a file was saved, never that the zines
    are "safe".
  - After part 1b, a partial last backup says so (see [part 1b](#the-last-backup-record-after-part-1b)).
- **If none has ever been saved:** **"No backup saved yet"**, as a fact in secondary text, not a warning.
- **The sheet title.** 🟦 "Your zines, kept safe" is reconsidered in the amendment. For example, "Keep your
  zines" states the purpose without claiming the result.
- **One sentence on what the file is.** 🟦 For example: "The backup file holds the zines and photos on this
  shelf. Keep a copy somewhere other than this phone." (Not "every zine": after part 1b a backup may leave
  one out, and says so.)
- **When a backup can't be made because a zine can't be opened** (part 1, before skip-and-list), the sheet says
  that in backup words, and covers both cases without claiming which. 🟦 For example: "A zine on this phone
  can't be opened, so no backup was saved. It may not appear on your shelf." From a newer Zinely: "A zine on
  this phone was made by a newer Zinely. Update Zinely, then back up."
  - **Way forward.** Part 1 only makes the failure true. Part 1b (same wave) replaces the abort with
    skip-and-list, after which these two states remain only for the case where **no** zine could be saved.
- **After a failed backup,** the empty or partial file is deleted (🟦 `DocumentsContract.deleteDocument`
  in the transport, best-effort), so the maker isn't left holding a file that looks like a backup.
- **A Cancel that lands after the file is complete does nothing:** the maker sees the saved state and the date
  is recorded.
- **Part 2 only, after the cross-device pass:** **"Changing phones? Save a backup here, then restore it in
  Zinely on the new phone."**
  - It must not imply the new phone may run an older Zinely. A newer backup refuses to restore on an older
    build ([audit §4 F1](ZINELY-1X-READINESS-AUDIT.md#4-wave-0-audit)).
- **No badges, dots, notifications, reminders or relative times** (constitution Article 4).

## UI/UX proposal

`docs/design/mockups/backup-restore.html` is **frozen** (2026-08-22). It is amended **once** for parts 1 and 1b
and re-frozen; the full list is the [amendment specification](#backup-restorehtml-amendment-specification).
The Shelf itself does not change. The dock's *Backups* entry keeps its label. The website's Download page
(`#new-phone`) already gives the same advice; keep the two consistent.

## Interaction details

- **Writing the date.** It is written **only** when a backup is saved: the `DataResult.Success` branch of
  `HomeViewModel.backupPicked` (`HomeViewModel.kt:251`), including a Cancel that lands after the stream closed
  (below). All other outcomes are distinct and write nothing:
  - failure (`:255`);
  - cancel before the stream closed (the catch at `:448-461`);
  - picker cancel (`:248`);
  - picker failure (`:279`).
- **The late Cancel (part 1).** ✅ Today a cancel after the stream closed is indistinguishable from one before
  (`LibrarySafTransport.kt:124-130`, `HomeViewModel.kt:287-292,448-460`). **Required:** once the stream has
  closed successfully the backup is done: Cancel is a no-op, the result is `BackupSaved`, the record is
  written, and the file is never deleted. 🟦 Mechanism: one atomic outcome latch per operation
  (`NOT_DONE → CANCEL_REQUESTED` from `cancelBackupRestore`, `NOT_DONE → DONE` from the transport right after
  `output.use` returns); whichever wins decides. `cancelBackupRestore` only cancels the job when it wins. A
  cancel that won before the close finished is an honest cancel: part 1's clean-up deletes the file. Part 1b
  reuses the same latch for restore ([defect R2](#r2-restore-cancelled-after-zines-were-committed)).
- **What "success" means.** Only that the storage provider accepted the stream without error
  (`LibrarySafTransport.kt:123-130`). There is no read-back, and a cloud provider may upload later. That is
  why the copy says "saved", not "safe" or "uploaded".
- **Restore does not change the date.** A restore is not a backup of this shelf.
- **Date format:** the device locale's medium date. No relative time ("3 days ago"): it becomes false while
  the sheet is open, and it reads as a nag.
- **Empty shelf.** "Empty" means **no visible zines**; then show neither line, as today (*Back up* isn't
  offered). A shelf whose only zines are unreadable looks empty; that is the existing ADR-042 limitation, not
  part 1's to fix.
- **No "changed since your last backup" state.** The obvious test, any zine's recency newer than the
  backup, is wrong:
  - Restored zines keep their source time (`RoomProjectRepository.kt:560`).
  - A restore after the last backup would add zines that aren't in it, and nothing would say so.
  - Recency is also a display value, `max(row, document mtime)` (`:817-825`).
  - If wanted later, compare the shelf's zine ids with the ids in the last backup. That is 🟦 future work.

## Part 1b — restore honesty and explicit partial backups

The written spec for plan step 1b. **The product rule, verbatim (owner, 2026-09-26):**

> **"A backup may be complete or explicitly partial, but it must never be silently partial."**

Restore stays **fail-closed** (ADR-110 §5, `DECISIONS.md:12710`): a restore still validates the whole archive
before touching the shelf. Only *backup* becomes skip-and-list.

### R1. A full disk during restore staging is called "This backup looks damaged"

- **✅ Path.** `ZineLibraryBackupStager.stage` wraps every `IOException` as `MALFORMED_ARCHIVE`
  (`ZineLibraryBackupStager.kt:127-133`), including write-side failures while copying each entry into the
  staging tree (`copyEntry` → `Files.newOutputStream(target, CREATE_NEW)`, :332-338). The repository turns any
  staging exception that isn't `FUTURE_VERSION` into `DataError.Corrupt` (`RoomProjectRepository.kt:347`), and
  the ViewModel maps `Corrupt` to `Damaged` (`HomeViewModel.kt:473`). Same family: a `createTempDirectory`
  failure (stager :110-111, outside the try) and a full disk in `prepareRestore` (repository :353-358) become
  `DataError.Io` → `ReadFailed` "Couldn't read that file" (`HomeViewModel.kt:477,485`) — also false.
- **Required behaviour.** A failure to *write the local staging copy* is never reported as a damaged or
  unreadable backup. Out of space → the existing "Not enough space" state (restore wording); any other local
  write failure → the existing generic restore failure ("Couldn't finish that restore" / "Nothing on this shelf
  was changed.", `Copy.kt:1550-1562`). A failure *reading the archive* keeps today's mapping.
- **Error-kind changes.** 🟦 A new `ZineBackupStagingException.Reason.STAGING_WRITE_FAILED` for write-side
  `IOException`s (split the copy's read and write sides; move the staging-directory creation inside the
  try). The repository classifies it — and `prepareRestore` failures — with the same free-space probe the
  transport already uses (`LibrarySafTransport.kt:165-172`): `DataError.OutOfSpace`, else a carrier that renders
  the generic restore state (e.g. `DataError.Unknown`; the implementation picks and documents it). No new
  `LibraryBackupRestoreFailureKind`.
- **Copy.** Existing strings; no new copy.
- **Tests.** JVM (`core:data-storage`): a stager given a thin output seam that throws
  `IOException("No space left on device")` raises `STAGING_WRITE_FAILED`, not `MALFORMED_ARCHIVE`; a truncated
  ZIP still raises `MALFORMED_ARCHIVE`. Robolectric (repository, fake free-space probe): full → `OutOfSpace`;
  not full → the generic carrier. ViewModel (fake transport): `OutOfSpace` in restore mode → `NotEnoughSpace`,
  never `Damaged` or `ReadFailed`.
- **Device (Pass 1).** Fill the phone to under one archive's size, restore a real backup: "Not enough space",
  shelf unchanged, nothing left in `files/.library-restore/`.

### R2. "Restore cancelled." after zines were committed

- **✅ Path.** Commit and reconcile run in `withContext(NonCancellable)` (`RoomProjectRepository.kt:361-393`),
  but the enclosing `withContext(io)` (:316) re-checks cancellation on return, so a Cancel during commit
  surfaces as `CancellationException` after the zines are on disk. The ViewModel then shows
  `RESTORE_CANCELLED` (`HomeViewModel.kt:448-460`, `Copy.kt:1541`). Cancel stays live throughout: the running
  sheet's button and its dismiss both call `onCancel` (`LibraryBackupRestoreSheet.kt:221-226,274-275`).
- **Required behaviour.** Once commit starts, the restore can't be cancelled and its real outcome is reported.
  Before commit starts, Cancel works as today and "Restore cancelled." is true (nothing was added).
- **Error-kind changes.** 🟦 The repository calls a commit-start hook inside the lock immediately before the
  `NonCancellable` block; the hook runs the [outcome latch](#interaction-details)
  (`NOT_DONE → COMMITTING`). If Cancel won first, the repository throws before committing. Once `COMMITTING`
  wins, `cancelBackupRestore` is a no-op and the UI state becomes `Running(mode, cancellable = false)`: the
  Cancel button is removed and dismiss does not cancel.
- **Copy (🟦 proposed, drawn in the amendment).** Running, after commit starts: hint
  "Adding zines to your shelf. This part can't be stopped." (replaces `RESTORE_RUNNING_HINT` for that phase).
- **Tests.** ViewModel with a fake transport that signals commit start then suspends: Cancel after the signal
  → `RestoreAdded`, no `RESTORE_CANCELLED` message; Cancel before → `RESTORE_CANCELLED`, fake reports nothing
  committed. Latch unit test (pure): both interleavings. Robolectric repository: cancel requested before the
  hook → no files moved; after → receipt returned. Compose semantics: no Cancel node while `cancellable = false`.
- **Device (Pass 1 + 2).** Restore a large backup and press Cancel at the "can't be stopped" moment: the
  button is gone, the result is "N zines added", and TalkBack reads the hint.

### R3. "Couldn't read that file" after a restore that committed

- **✅ Path.** After `restoreCommitter.commit` succeeds, a strict reconcile failure (`RoomProjectRepository.kt:368-376`)
  or a `dao.findById` failure / missing row (:379-389) returns `DataError.Io`, which the ViewModel maps to
  `ReadFailed` "Couldn't read that file" (`HomeViewModel.kt:477,485`) — while the zines are committed on disk.
  Files are the truth; Room is rebuildable and retries on next use (ADR-042; `reconciled = false`, :367).
- **Required behaviour.** Once commit has succeeded, the outcome is a success: the count added is
  `prepared.ids.size`, known without Room. If the shelf could not be updated yet, say so honestly.
- **Error-kind changes.** 🟦 `LibraryRestoreReceipt` carries `addedCount` and `shelfUpToDate: Boolean` (the
  per-project summaries may be incomplete); post-commit index failures return `Success(shelfUpToDate = false)`
  instead of `Failure`. The ViewModel shows `RestoreAdded` with a pending line and triggers a shelf refresh
  (`HomeViewModel.retry()`, :232). A commit failure (:364-366) stays a failure: the committer rolls back.
- **Copy (🟦 proposed).** Title unchanged ("3 zines added to your shelf"); body: "They may take a moment to
  appear. If they don't, close Zinely and open it again."
- **Tests.** Robolectric repository with a DAO seam that throws on `findById` after commit: `Success`,
  `addedCount` correct, files present, `reconciled` false; next shelf read shows the zines. ViewModel: pending
  body shown, never `ReadFailed`.
- **Device.** Not reproducible on demand; covered by tests. Pass 2 reads the copy once on the drawn state.

### Skip-and-list backups

A backup that meets a zine it can't include leaves that zine out, saves the rest, reports **"N of M zines
saved"** and names every left-out zine it can. It is never silent.

**✅ The fail-closed points today** (`RoomProjectRepository.createLibraryBackup`, :404-525), and their 1b rule:

| Point | Line | 1b rule |
|---|---|---|
| Unsafe project path | :427-428 | Skip, list |
| `meta.json` missing or unreadable | :429-430 | Skip, list (🟦 see note) |
| `documents.load` fails — `Corrupt` / `Invalid` / `SchemaTooNew` | :431-433 | Skip, list; reason "newer Zinely" for `SchemaTooNew` |
| `documents.load` fails — `Io` / `OutOfSpace` / `Busy` | :431-433 | **Whole backup fails**, as today (transient; a retry may succeed) |
| Document size / hash `IOException` | :440-444, :451-455 | Whole backup fails (transient) |
| Malformed JSON / no `schemaVersion` | :445-450 | Skip, list |
| Asset missing | :479-481 | Skip **every** zine referencing it, list each; reason "photo" |
| Asset not a readable image | :482-486 | Same |
| Asset bytes don't match their hash (poisoned) | writer `INTEGRITY_MISMATCH`, `ZineLibraryBackupWriter.kt:140-142` → `Corrupt` (repo :514) | **Skip and list** (owner ruling, 2026-09-26): pre-hash each asset in the asset pass and treat a mismatch like a missing asset (reason "photo"). The writer check stays as a backstop and **never fails the whole backup** ([below](#a-photo-that-fails-its-check)) |
| Writer backstop mismatch on a document or asset | `ZineLibraryBackupWriter.kt:126-142`, after the pre-checks passed | **Rebuild without it** ([below](#a-photo-that-fails-its-check)): skip and list every zine using it, write a fresh private archive |
| Asset size `IOException`, or an `IOException` while pre-hashing an asset | :487-491; the new pre-hash | **Retry that read once; if it fails again, skip and list, reason "photo"** (owner ruling: an unreadable photo is skip-and-list). A single flaky read is absorbed by the retry; a photo that stays unreadable leaves its zines out, named, never silently |
| Asset over the per-asset limit (declared, or growing during the copy) | writer `LIMIT_EXCEEDED`, `ZineLibraryBackupWriter.kt:226-227,247` | **Skip and list**, reason "photo": checked in the pre-check against `maximumAssetBytes`; a copy that grows past it is a backstop rebuild |
| Writer `SOURCE_UNAVAILABLE` or a **read-side** `IO_FAILURE` on an asset entry | `ZineLibraryBackupWriter.kt:157,212` | **Rebuild without it**, reason "photo" (backstop). 🟦 The writer must say whether an `IOException` came from reading a named source or from writing the private archive; today `IO_FAILURE` wraps both (:155-160) |
| **Write-side** `IO_FAILURE` (the private archive itself), library-wide limits (manifest size, entry count, total expansion — :181, :184, :285) | writer | **Whole backup fails**, as today, in backup wording (space / save-error). Not a photo problem: nothing is left out to fix it |

- **Assets are shared** (content-addressed, ADR-110 §2). The asset pass runs after the project pass, so 1b
  makes it two-phase: collect candidate zines, check every referenced asset, drop zines that reference a bad
  one, then rebuild the asset table from the survivors only. Exact closure (ADR-110 §4) must still hold: an
  asset referenced only by left-out zines is not written.
- **All left out (0 of M):** no file (part 1's clean-up deletes it) and the part-1 backup-wording failure,
  reworded for "none" ([amendment](#backup-restorehtml-amendment-specification) item 4).
- **Note on `meta.json`.** A zine whose sidecar is unreadable but whose document loads is still openable (the
  shelf uses a fallback title, :750-759). 🟦 Skipping it follows the Q8 scope; backing it up with its row's
  title is a possible later refinement, not wave 1.
- **The poisoned local photo (Q8 defect 5), backup half — ruled.** Owner, 2026-09-26 (after the final planning
  audit): *a single poisoned or unreadable photo is handled by skip-and-list, never by invalidating the whole
  backup.* Its *restore* half (`AdditiveLibraryRestoreCommitter.kt:186-191`, "Couldn't read that file") stays
  out of wave 1, as Q8 ruled.

#### A photo that fails its check

- **✅ Evidence.** `createLibraryBackup` writes a **private local archive** (`backupWriter.write(…, destination)`,
  `RoomProjectRepository.kt:505`; `CREATE_NEW` on a local path, `ZineLibraryBackupWriter.kt:94-104`), and the
  writer removes an incomplete archive best-effort (its KDoc; `finally` at :161). The user's chosen location is
  written by the transport **afterwards**. So a check failing inside the writer has not touched the maker's
  file, and a rebuild is invisible to them.
- **Pre-check (the normal path).** The asset pass hashes each referenced asset (🟦 streaming SHA-256) and
  compares it with its content-addressed name and with `maximumAssetBytes`. A mismatch, a missing file, an
  unreadable image, an over-limit file, or a read that fails twice leaves out **every** zine that references
  that asset, reason **photo**.
- **Backstop (the rare path).** If the writer still fails on **one named source entry** — `INTEGRITY_MISMATCH`
  (bytes changed between the pre-check and the write, or a document hash differs), `LIMIT_EXCEEDED` on that
  entry, `SOURCE_UNAVAILABLE`, or a read-side `IO_FAILURE` — the backup is **not** failed:
  - 🟦 the exception names the failing entry and says read side vs write side (structured fields, not only
    the message);
  - the repository leaves out every zine using that entry (asset → reason **photo**; document → reason
    **unreadable**), rebuilds the manifest from the survivors (exact closure) and rewrites the private archive;
  - **the rewrite target:** the repository deletes the incomplete archive at `destination` and confirms it is
    gone before rewriting there (the writer refuses an existing path, `DESTINATION_EXISTS`, :96-107). If that
    private file can't be removed, the backup fails as a **local storage** error (existing save-error wording),
    never as "damaged" and never blaming the photo; that is a failure of app-private storage, not of a photo;
  - each rebuild removes at least one entry, so it ends; 🟦 bound it by the number of distinct entries;
  - if no zine survives, it is the "0 of M" case: no file, the "none" state.
- **What still fails the whole backup after 1b, stated:** a write-side failure on the private archive (space,
  I/O); library-wide limits (manifest size, entry count, total expansion); a transient document read
  (`Io`/`Busy`); the private-archive clean-up failure above. **None of them is a single photo.**
- **Restore reads `omitted` from an untrusted archive.** 🟦 Clamp, never refuse: show at most the first 50
  entries' titles (the rest counted), truncate each title (e.g. 120 characters), render as plain text. A
  malformed or oversized `omitted` is ignored rather than making a valid archive unrestorable (restore's
  fail-closed validation of the zines themselves is unchanged). 🟦 Default decoding would throw on a
  wrongly typed value (`"omitted": 5`), so this one field is decoded leniently (e.g. as a `JsonElement`,
  mapped by hand). That is **not** the stop condition's "change to restore's fail-closed validation": it
  loosens nothing about the zines, only a display-only field.
- **What the maker sees.** Exactly what a pre-check omission shows: the partial success naming the zine, with
  the photo reason ([amendment](#backup-restorehtml-amendment-specification) item 10). No state claims the
  backup "looks damaged", and no state blames the storage location.
- **Part 1 only (before 1b lands):** a bad photo still fails the whole backup, in part 1's backup wording
  ("A zine on this phone can't be opened, so no backup was saved."). Parts 1 and 1b ship in the same wave, so
  no release carries that interim. If a release ever carried step 1 without 1b, its notes must state it.
- **Tests.** JVM `core:data-storage`: a writer given an asset whose bytes differ from its name raises
  `INTEGRITY_MISMATCH` carrying that hash. Robolectric repository:
  - one poisoned asset used by one zine → `Success`, that zine omitted with reason photo, the rest present,
    the asset absent from the archive;
  - one asset shared by two zines → both omitted, both named;
  - a fake writer that fails once with a named mismatch, then succeeds → `Success` with that zine omitted, and
    the first private archive gone;
  - an asset read that fails once then succeeds → complete backup; fails twice → that zine omitted, reason photo;
  - an over-limit asset (fake limit) → its zines omitted, reason photo;
  - a write-side `IO_FAILURE` on the private archive → whole backup fails in backup wording, nothing omitted;
  - every zine's photo bad → no file, "none" state;
  - restore of an archive whose `omitted` is oversized or malformed → the zines restore; the notice is clamped
    or absent.

**How a left-out zine is named.** 🟦 Title source in order: readable `meta.json` title (the authority,
ADR-042) → the zine's shelf-row title if it has a row → none. A zine with no readable title is counted, not
named: "1 zine without a readable name". A listed zine with no shelf row adds the sentence "Some of these
aren't on your shelf." Names are shown up to three, then "and N more".

**Receipt and state.** 🟦 `LibraryBackupReceipt` gains `totalCount` and `omitted: List<OmittedZine>` (`title:
String?`, `reason: UNREADABLE | NEWER_VERSION | PHOTO`, `onShelf: Boolean`); `BackupSaved` carries them.
Partial ⇔ `omitted` non-empty.

**Copy (🟦 proposed, drawn in the amendment).**
- Partial success title: "5 of 6 zines saved"; body: "Zinely couldn't open 1 zine, so it isn't in this
  backup: “Moth Club Bulletin”." Newer-version reason: "…was made by a newer Zinely, so it isn't in this
  backup. Update Zinely, then back up again." Photo reason: "Zinely couldn't read a photo in 1 zine, so that
  zine isn't in this backup: “Moth Club Bulletin”." (It says *read*, not *damaged*: a missing photo and a
  changed one look the same from here.) Mixed reasons: one sentence per reason, in the order above.
- Complete success keeps "All 6 zines are together in one backup file." (`Copy.kt:1544-1545`).

### How the partial state is recorded

**✅ Evidence.**
- Manifest shape: `ZineLibraryBackupManifest` (`core/data/.../ZineLibraryBackupManifest.kt`) — `packageVersion`
  2, `kind`, `appVersion`, `createdAtEpochMs`, `projects`, `assets = emptyList()`; ARCHITECTURE
  [§4.1](../ARCHITECTURE.md#41-compatibility-guards-and-the-zine-v2-format-note) (`ARCHITECTURE.md:293`).
- **Older builds ignore unknown manifest keys.** The stager decodes with `Json { ignoreUnknownKeys = true }`
  (`ZineLibraryBackupStager.kt:105`, decode :195), and the repository constructs it with that default
  (`RoomProjectRepository.kt:122`). Checked at tags **`v0.9.0-beta.4-r3` and `v0.9.0-beta.5`**: both lines are
  identical. The structural validator works on the decoded object, so it never sees an unknown key.
- **A version bump is refused whole by older builds:** `packageVersion > 2` → `FUTURE_VERSION`
  (`ZineLibraryBackupStager.kt:359-365`) → "This backup needs a newer Zinely".
- The writer encodes with `encodeDefaults = true` (`ZineLibraryBackupWriter.kt:79`).
- The frozen fixture's manifest has no such key; `LibraryBackupFixtureTest` pins the archive SHA-256,
  `packageVersion`, `kind`, `appVersion` and per-project entries (:36-54).

**Options.**

| | Additive field, `packageVersion` stays 2 | Bump to `packageVersion` 3 |
|---|---|---|
| Older build restoring a partial archive | Restores the zines it holds; **says nothing about the omission** (its success copy "N zines added" is still true) | Refuses the **whole** file: a partial backup becomes useless on every older build |
| Older build restoring a complete archive | Unchanged | Refused too, unless complete backups keep writing 2 (two formats to maintain) |
| Frozen v2 fixture | Decodes with the default → complete; stays green, unregenerated | Stays green, but the new format needs its own guard corpus |
| Cost | One defaulted field | Version routing, refusal copy, release-note burden |

**🟦 RECOMMENDATION: an additive, defaulted manifest field; no version bump.**
```kotlin
val omitted: List<ZineBackupOmission> = emptyList()   // on ZineLibraryBackupManifest
@Serializable data class ZineBackupOmission(val title: String? = null, val reason: String)
// reason: "unreadable" | "newer_version" | "photo"; unknown values read as "unreadable"
```
- Partial ⇔ `omitted` non-empty; the count is its size, so there is no second field to disagree with.
- No `sourceProjectId`, path or bytes of a left-out zine enter the archive: only what the maker was already
  told on screen.
- Complete backups written by 1b carry `"omitted":[]` (encodeDefaults); harmless to every reader.
- **Compatibility consequence, stated honestly:** a partial archive restored on beta.4-r3 / beta.5 / a
  pre-1b wave build restores what it holds **without** the partial notice. That is the one place the rule is
  weaker, and it is outside what a new build can change; the maker was told at backup time and the last-backup
  line still says so on the phone that made it. Release notes say it.
- ADR-110's closure rule is untouched: omissions reference no entries.

**Tests.** JVM `core:data`: a manifest with `omitted` round-trips; one without it decodes to empty. JVM
`core:data-storage`: 🟦 a **new** frozen fixture `library-backup-v2-partial.zine` (fictional content, pinned
SHA-256) stages with its omissions; the existing v2 fixture stays byte-identical and green; a guard test that a
manifest with an unknown top-level key still stages (pins `ignoreUnknownKeys`, so no future build turns strict).

### Restore of a partial backup

- The repository reads `staged.manifest.omitted` and returns it in the receipt; `RestoreAdded` carries it.
- **Copy (🟦 proposed).** Title unchanged ("3 zines added to your shelf"); an extra line: "This backup was
  saved without 1 zine that couldn't be opened then: “Moth Club Bulletin”." Photo reason: "…without 1 zine
  whose photo couldn't be read then: “Moth Club Bulletin”." Unnamed: "…without 1 zine that had no readable
  name." Then the existing "What was already on this shelf stayed put."
- The reason vocabulary is a string with an "unreadable" fallback, so adding "photo" needs no version change;
  older builds ignore the whole field.

### The last-backup record after part 1b

- `BackupRecordStore` (part 1) also stores `savedCount` and `totalCount` (count only; names would go stale).
- **Copy (🟦 proposed).** Partial: "Last backup saved 12 September 2026 — 5 of 6 zines." Complete: unchanged.
- Written only on success, as in part 1; a partial backup *is* a success.

### Part 1b acceptance criteria

1. The ADR and product-law amendment are reviewed and landed before code; the amended HTML is re-frozen.
2. R1–R3: each shipped false message is gone; each test above passes.
3. A backup meeting any skip point saves the rest, names what it can, reports "N of M", and records the
   omissions in the manifest. None left → no file.
4. A transient **document** read failure still fails the whole backup. A **photo** read is retried once; a
   photo that stays unreadable, is missing, over the limit, or fails its check — at the pre-check or at the
   writer's backstop — **never** fails the whole backup: its zines are left out and named with the photo
   reason ([owner ruling](#a-photo-that-fails-its-check)). Only the stated non-photo failures (private-archive
   write, library-wide limits, transient document reads, private clean-up) fail the whole backup.
5. The frozen v2 fixture is byte-identical and green. `packageVersion` stays 2.
6. A partial archive restores on the new build with the notice; on the current release build without it.
7. No permission, network, notification or background work; no document-schema change.
8. Pixel parity with the amended HTML; both device passes (below).

**Device (Pass 1 + 2), part 1b.** Samsung SM-A176B: plant an unreadable zine (damaged `document.json`, via
`run-as`) and a zine whose photo file's bytes are altered (same name) beside good ones; back up to Files;
read the partial success with both reasons and the last-backup line; restore that
file and read the partial notice; TalkBack reads names in order. Pass 2: does "5 of 6 zines saved" read as a
success the maker can trust, or as a failure?

## ADR draft — amendment to ADR-110

> **DRAFT. Not recorded.** It lands in `DECISIONS.md` only in the implementation session, after independent
> review, numbered then. Next free on `main` is 119 (taken by step 2 if it lands first); ADR-115 is reserved
> on PR #70.

### ADR-NNN (next free at implementation time) — A backup is complete or explicitly partial, never silently partial

**Status:** Proposed · **Amends:** [ADR-110](../DECISIONS.md#adr-110) (the "all zines in one user-owned file" premise and the
backup fail-closed clause) · **Extends:** ADR-042, ADR-110

#### Context

ADR-110 (`DECISIONS.md:12704`): *"Zinely's local-only promise makes a user-held backup a durability requirement.
The original `ZinePackageManifest` is version 1 and contains one project; V1 product law now requires **all
zines in one user-owned file**."*

ADR-110, backup production (`DECISIONS.md:12728`): *"`RoomProjectRepository` exposes `LibraryBackupRepository`
under the same writer lease and fails closed on unreadable metadata, missing assets, or poisoned bytes."*

Fail-closed backup made one unreadable zine block every backup, permanently. A zine unreadable at indexing has
no shelf row (`RoomProjectRepository.kt:674-676`), so the maker can't find or delete it: the protection against
total loss is withdrawn by the very damage it exists for. The owner ruled on 2026-09-26 (Q8).

#### Decision

1. **Product rule:** *"A backup may be complete or explicitly partial, but it must never be silently partial."*
2. **Backup is skip-and-list.** A zine that can't be read — unsafe path, unreadable `meta.json`, a document
   that fails to load as corrupt, invalid or newer-version, malformed JSON, or a missing, unreadable or
   hash-mismatched, over-limit or persistently unreadable asset it references — is left out; the rest is
   saved. Transient **document** reads, write failures on the private archive and library-wide limits still
   fail the whole backup. Zero zines saved writes no file.
3. **One photo never fails the whole backup** (owner ruling, 2026-09-26: *a single poisoned or unreadable
   photo is handled by skip-and-list*). Assets are checked before the archive is built (hash, size, one
   retried read); a failure leaves out every zine that uses the asset. The writer's checks stay as a
   backstop: when one trips on a named source entry, the zines using it are left out and the private archive
   is rewritten. The archive is private until complete, so the maker's chosen file is never touched by the
   retry.
4. **Explicit to the maker:** the result reports "N of M" and names every left-out zine with a readable title,
   with its reason (couldn't open · newer Zinely · couldn't read a photo).
5. **Explicit in the file:** `ZineLibraryBackupManifest` gains the defaulted `omitted` list (title?, reason:
   `unreadable` · `newer_version` · `photo`; unknown values read as `unreadable`).
   `packageVersion` stays 2. Older builds ignore the key (`ignoreUnknownKeys`, `ZineLibraryBackupStager.kt:105`,
   verified at beta.4-r3 and beta.5).
6. **Exact closure is preserved** (ADR-110 §4): the asset table is rebuilt from the saved zines only.
7. **Restore is unchanged in kind:** staged, fail-closed, additive (ADR-110 §5). A restore of a partial
   archive tells the maker what it lacked. (A poisoned photo met **during restore** stays out of this ADR's
   scope, as Q8 ruled.)
8. **Restore reports what happened:** local staging-write failures aren't "damaged"; once commit starts Cancel is
   withdrawn; a committed restore is reported as added even if the shelf index lags.

ADR-110's premise sentence is read as: *V1 product law requires one user-owned file holding every zine Zinely
can read, naming any it could not.* Its backup fail-closed clause (including "poisoned bytes") is superseded
by Decisions 2–6.

#### Consequences

- A damaged zine no longer blocks backups; the maker is told which zine and, when it's on the shelf, can act.
- A partial archive restored on a pre-amendment build restores without the partial notice (accepted, in
  release notes).
- The frozen v2 fixture stays valid; a new frozen partial fixture joins it. The
  [torture matrix](../reviews/2026-08-21-zine-backup-torture-matrix.md) gains rows: unreadable zine at backup,
  poisoned asset shared by two zines, writer backstop tripping after the pre-check, all zines unreadable,
  partial archive on an old reader.
- Backup reads each asset twice (pre-hash); bounded by the existing limits. A backstop retry rewrites the
  private archive, at most once per failing entry.

#### Alternatives

- **Keep fail-closed, add a delete-by-id remedy:** needs a UI for an invisible zine; still no backup meanwhile.
- **`packageVersion` 3 for partial archives:** older builds refuse the whole file; rejected.
- **Record partiality only on the phone:** lost with the phone — the case backups exist for; rejected.
- **Put "partial" in the suggested file name:** the name is chosen in the picker before the scan runs
  (`HomeViewModel.kt:236-239`), so it can't know; rejected.

#### Review

To be filled by the implementation session's independent review (verdict, findings, reconciliation).

## Product-law amendment draft

**Source.** ADR-110 cites "V1 product law … **all zines in one user-owned file**". That exact phrase does not
occur anywhere in `docs/` (searched: `docs/**/*.md` for "one user-owned file", "user-owned file", "all zines
in one", "product law"; `PRD.md`, `zinely-constitution.md`, `zinely-v1-product-vision.md`,
`zinely-v1-execution-plan.md`). The V1 product law is `docs/zinely-v1.md` (it outranks every ADR, :3); the
sentences ADR-110 paraphrases are:

- `docs/zinely-v1.md:68` (§5 Feature Tribunal): *"| User-held backup / restore / device migration (one file,
  user destinations only) | **REQUIRED** | Art 3 — constitutional duty; ownership includes survival. No Zinely
  servers, ever. |"*
- `docs/zinely-v1.md:55` (§4 journey): *"Somewhere quiet: "Back up your zines" — one tap, one file, any
  destination they own."*

**Draft amendment** (to land with the ADR, per the amendment rule at `docs/zinely-v1.md:170`):

- `:68` → *"| User-held backup / restore / device migration (one file, user destinations only; **complete or
  explicitly partial, never silently partial** — a zine Zinely can't read is left out and named, never
  silently dropped) | **REQUIRED** | Art 3 — constitutional duty; ownership includes survival. Art 5 — the
  result says what it holds. No Zinely servers, ever. |"*
- `:55`: unchanged (it describes the gesture, not completeness).
- 🟦 `:107` (DoD 2, torture matrix): add "back up a library holding an unreadable zine (the rest is saved and
  the missing one named)".

## `backup-restore.html` amendment specification

One amendment for parts 1 and 1b. Every string below is 🟦 **proposed**; the drawing decides. Each new or
changed state is drawn light and dark, at **360 dp width and 200 % text**, with actions stacking and staying
≥ 48 dp. New states get their own `data-sheet` control beside the existing ones (:803-814).

**✅ Existing frozen states** (`docs/design/mockups/backup-restore.html`): chooser `#trustSheet` (:715-741:
`#trustTitle` :721, `#trustBody` :722, `#trustNote` :724, `#saveOption` :725-731, `#restoreOption` :732-738);
running `#workSheet` (:743-759, `#workCancel` :755); success `#successSheet` (:761-776); error `#errorSheet`
(:778-794) with the branches in `showSheet` (:903-931); empty-shelf variants (:855-867).

| # | State | Change | Proposed copy |
|---|---|---|---|
| 1 | Last-backup line in `#trustSheet` | **Add**, between `#trustBody` and `#trustNote`, secondary text; three variants | "No backup saved yet" · "Last backup saved 12 September 2026 · zinely-backup-2026-09-12.zine" · "Last backup saved 12 September 2026 — 5 of 6 zines." |
| 2 | `#trustTitle` (non-empty shelf) | **Change** or keep — decided in the drawing | "Keep your zines" |
| 3 | "What this file holds" | **Change** `#trustBody` or add under it | "The backup file holds the zines and photos on this shelf. Keep a copy somewhere other than this phone." |
| 4 | Backup failures in backup wording (`#errorSheet`, new branches) | **Add**, separate from restore's `damaged`/`newer` | None readable: "No zines could be saved" / "Zinely couldn't read the zines on this phone, so no backup was saved." Plus "They may not appear on your shelf." **only** when some left-out zine has no shelf row. Photo variant, when every zine was left out for a photo: "Zinely couldn't read the photos in the zines on this phone, so no backup was saved." · Newer: "A zine here needs a newer Zinely" / "Update Zinely, then back up." Save-there / space / busy: **confirm** existing wording (`Copy.kt:1528-1533`), not redrawn |
| 5 | Partial-backup success (`#successSheet`) | **Add** | "5 of 6 zines saved" / "Zinely couldn't open 1 zine, so it isn't in this backup: “Moth Club Bulletin”." Draw 1 named, 1 unnamed, and 5 left out (three names + "and 2 more"), plus the "Some of these aren't on your shelf." variant |
| 6 | R1 full disk on restore | **Confirm** `space` branch (:915-918) is used for restore staging; no redraw | existing |
| 7 | R2 running, commit phase (`#workSheet`) | **Add** variant: `#workCancel` removed | hint "Adding zines to your shelf. This part can't be stopped." |
| 8 | R3 committed, shelf lagging (`#successSheet`, restored) | **Add** | "3 zines added to your shelf" / "They may take a moment to appear. If they don't, close Zinely and open it again." |
| 9 | Restore of a partial archive (`#successSheet`, restored) | **Add** | "3 zines added to your shelf" / "This backup was saved without 1 zine that couldn't be opened then: “Moth Club Bulletin”. What was already on this shelf stayed put." Also draw the photo-reason line ("…without 1 zine whose photo couldn't be read then: …") |
| 10 | Partial-backup success, **photo reason** (`#successSheet`, saved) — a zine left out because a photo failed its check (owner ruling, 2026-09-26) | **Add** | "5 of 6 zines saved" / "Zinely couldn't read a photo in 1 zine, so that zine isn't in this backup: “Moth Club Bulletin”." Draw: one zine; one photo shared by two zines (both named); a mix with an unreadable zine (one sentence per reason). It is a **success** state: no warning colour, no "damaged", no blame on the storage location. Same `omitted` record as item 5, reason `photo` |

**Untouched frozen states:** `#restoreOption` and all empty-shelf variants (:855-867); the backup and restore
running copy apart from item 7; complete `saved` and `restored` success (:889-896); the restore error branches
`damaged`, `newer`, `read`, `generic` (:903-931) — restore failures keep their wording; `save-error`, `space`,
`busy` wording. Part 2's "Changing phones?" line is a **later** amendment, directly above *Back up this shelf*.

## Current architecture touchpoints

Re-verified on `0aa7a7d`.

| Concern | Where |
|---|---|
| Backup sheet UI | `feature/editor/.../feature/library/LibraryBackupRestoreSheet.kt` (`onSaveBackup` :67/:111; text :77-101; running sheet + Cancel :221-275; error rendering :323-347); states `LibraryBackupRestoreUi.kt:10-37` |
| Backup / restore orchestration | `app/.../home/HomeViewModel.kt` (`startBackup` :236, `backupPicked` :246, success :251, `restorePicked` :263, `cancelBackupRestore` :287, `launchBackupRestore` + cancel catch :439-467, error mapping :469-486, suggested filename :488); `app/.../editor/ZinelyNavHost.kt:193-197` (SAF `CreateDocument`) |
| Backup-capable check | `feature/editor/.../ZineLibraryScreen.kt:283,456` (`canBackup = zines.isNotEmpty()`; visible zines only). Unreadable zines have no row: `RoomProjectRepository.kt:604-605,674-676` |
| Transport | `data-android/.../LibrarySafTransport.kt` (`restoreFrom` :65-99, `backupTo` :101-135, stream :123-130, free-space probe :165-172) |
| Writer, lease and closure | `data-android/.../RoomProjectRepository.kt` (`createLibraryBackup` :404-525; fail-closed points :427-491; manifest :496-503); `core/data-storage/.../ZineLibraryBackupWriter.kt` (Json :79, integrity :126-142) |
| Restore | `RoomProjectRepository.restoreLibrary` :316-397 (staging map :334-350, prepare :353-358, commit + reconcile :361-393); `ZineLibraryBackupStager.kt` (Json :105, IOException wrap :127-133, `copyEntry` :332-338, version check :359-376); `AdditiveLibraryRestoreCommitter.kt` |
| Manifest | `core/data/.../asset/ZineLibraryBackupManifest.kt`; [ARCHITECTURE §4.1](../ARCHITECTURE.md#41-compatibility-guards-and-the-zine-v2-format-note) |
| Copy | `core/copy/.../Copy.kt` `object LibraryBackup` (:1492–~:1565; title :1495; "needs a newer Zinely" :1523; neutral backup failures :1528-1533; cancelled :1540-1541; generic :1550-1562) |
| Local preferences | `data-android/.../di/PreferencesModule.kt:39-62` (one `@Singleton DataStore<Preferences>`, file `files/datastore/editor_onboarding.preferences_pb`); pattern to copy: `DataStoreEditorOnboardingStore.kt:36-48` |
| Transfer rules | `app/src/main/AndroidManifest.xml:13-15`; `res/xml/data_extraction_rules.xml`, `backup_rules.xml` |

## Files/modules likely affected

Part 1:
- `core/copy`: the last-backup line (two states), the title if changed, the "what this file holds" sentence,
  and backup-failure strings separate from the restore ones.
- `data-android`: a `BackupRecordStore` (the date and, if reported, the file name) on the **existing**
  DataStore singleton. A second DataStore on the same file throws. Its interface lives beside the existing ones
  in `data-android/prefs` (`EditorOnboardingStore.kt`, `PreferredPaperStore.kt`), matching
  [plan §7](ZINELY-1X-IMPLEMENTATION-PLAN.md#7-ios-preparation-architecture-only).
- `data-android` transport: `LibrarySafTransport.backupTo` deletes the created document when the write fails
  or is cancelled **before the stream has closed successfully**, and marks the operation done once it has
  (the [latch](#interaction-details)). Once done it never deletes and a late Cancel is a no-op.
- `app`: write the record on success; the latch in `cancelBackupRestore`. In backup mode, map
  `Corrupt`/`Invalid` and `SchemaTooNew` to the two new backup-wording kinds (`HomeViewModel.kt:469-486`),
  separately from restore; expose the record to Home state.
- `feature/editor` (`feature/library` package): render the line and the new states.
- **Not touched in part 1:** the archive format, writer, stager, committer, `data_extraction_rules.xml` and
  `backup_rules.xml`.

Part 1b adds: `core:data` (manifest `omitted`), `core:data-storage` (stager write-side reason; output seam;
the writer's `INTEGRITY_MISMATCH` names the failing entry), `data-android` (skip-and-list in
`createLibraryBackup`, asset pre-hash and backstop rebuild, restore receipt, commit-start hook, record counts),
`app` (restore latch, mappings), `core/copy`, `feature/library`. **Still not touched:** the committer, the
transfer rules, the document schema.

## Data model implications

**No document schema change.** The timestamp and counts are device state:
- They must never enter `ZineDocument`, `meta.json` or the archive, because a restored backup would otherwise
  claim a backup time from another phone.
- DataStore lives in app-private storage, which is excluded from cloud backup and device transfer today. So
  the fact honestly resets on a new phone.
- If Part 3 ever enables transfer, the rules must exclude this key's file. A separate DataStore file, or
  `noBackupFilesDir`, would keep preferred paper transferable while excluding the date.

**Part 1b: one additive archive-manifest field** (`omitted`, [above](#how-the-partial-state-is-recorded)),
`packageVersion` unchanged. It describes the file, not the phone.

## Testing strategy

| Level | What | Proves |
|---|---|---|
| JVM `core/copy` | Date line, all states; failure strings; "N of M" plurals | No string says "safe", "automatic", "cloud" or "synced"; backup failures don't reuse restore wording |
| JVM store | Fake-backed `BackupRecordStore` | Written on success (incl. partial, incl. late Cancel); untouched on cancel, failure, picker cancel, picker failure and restore |
| ViewModel (fakes) | Given a successful backup, When the sheet reopens, Then the line shows today's date. Given an unreadable zine on disk, When backup runs, Then the backup-wording state (part 1) / partial success (1b); Given a newer-version zine, Then the update state / "newer" reason. Given Cancel after the stream closed, Then `BackupSaved` | Branch correctness, including the error mapping (untested today) |
| JVM/Robolectric transport | Given a Cancel after the stream closed successfully, Then the file is kept. Given a write that fails or is cancelled before that, Then the created document is deleted | No partial file left behind; no false "cancelled" |
| Part 1b | R1–R3 and skip-and-list tests in [part 1b](#part-1b--restore-honesty-and-explicit-partial-backups) | The three restore messages are true; never silently partial |
| Compose semantics + Roborazzi | Every amended state, light and dark, at 200 % font | Parity with the amended HTML |
| Device (Pass 1 + 2) | Samsung SM-A176B: save a backup to Files and to a cloud provider; reopen; TalkBack reads the line before the action; part 1b items above | The platform tree, and the maker's understanding |
| **Device (Part 2 only)** | Cross-device restore pass (Gate 4) | The "Changing phones?" promise is true |

Automatable: everything except TalkBack speech and the cross-device restore. **Compose semantics tests do not
prove TalkBack behaviour.** Run `bash tools/grun.sh gold` before claiming a change is visually neutral.

## Accessibility considerations

- The line and the explanatory sentence come before the action they explain, in reading order.
- The date is spoken as a date ("Last backup saved 12 September 2026").
- Failure and partial states are sheet text, reachable by swiping, not toasts that disappear.
- Left-out names are read as a list in order; "and N more" is text, not an ellipsis.
- A withdrawn Cancel is removed from the tree, not merely disabled-looking (check the platform tree, not only
  Compose semantics).
- No colour-only state.
- Large text: the lines wrap, and actions stay at least 48 dp.

## Design-system implications

None new: existing sheet text styles and tokens. No icon, no warning colour. A missing backup is not an error
state; a failed backup is; a partial backup is a success with a fact.

## iOS portability notes

- The "last backup" record is behind a small interface. DataStore stays in `data-android` (coupling rule 3), so
  an iOS port swaps the store, not the logic.
- Failure-to-copy mapping and the outcome latch belong in pure code (`core/copy` strings plus a pure mapper),
  not in a Composable.
- The manifest field lives in `core:data`, shared.
- Don't add platform date formatting to core. Format at the UI edge.

## Acceptance criteria

Part 1:
1. The amended `backup-restore.html` is reviewed and re-frozen before any Compose work.
2. After a successful backup, the sheet shows that day's date. Cancel before the stream closed, failure, picker
   cancel, picker failure and restore leave it unchanged. A Cancel after the stream closed shows the saved
   state and records the date.
3. With zines on the shelf and no backup ever saved, the sheet shows the "no backup saved yet" state. An empty
   shelf shows neither line.
4. A backup that fails because a zine can't be read shows the backup-wording state, never "This backup looks
   damaged"; a newer-version zine shows the update state, never "This backup needs a newer Zinely". Restore
   failures keep their current wording.
5. A failed or cancelled backup leaves no file at the chosen location (where the provider allows deletion);
   a complete one is never deleted.
6. No string in the sheet says or implies "safe", "automatic", "cloud" or "synced" about a backup the app
   cannot verify.
7. No new permission, no network, no notification, no background work, no schema change, no archive-format
   change.
8. Pixel parity with the amended HTML. Both device passes. TalkBack reads the line before the action.
9. **Part 2 only:** the "Changing phones?" line ships only after Gate 4 passes and is recorded.
10. Public copy (website Download page) and in-app copy say the same thing.

Part 1b: [its own criteria](#part-1b-acceptance-criteria).

## Stop conditions

- Part 1 appears to need a change to the archive format, the writer, the stager or the transfer rules.
- Part 1b appears to need a `packageVersion` bump, a document-schema change, a change to restore's fail-closed
  validation, or a regenerated fixture.
- The design calls for a reminder, badge, notification, relative time or safety claim.
- A backup could end partial without the maker being told, on screen and in the manifest.
- One photo (missing, unreadable after one retry, over the limit, or failing its hash — at the pre-check or
  the writer's backstop) could still fail the whole backup after part 1b, or a whole-backup failure appears
  that is not in the [stated list](#a-photo-that-fails-its-check).
- Part 2 work starts before Gate 4 is recorded.

## Out of scope

- Automatic or scheduled backups; backup to a cloud provider.
- Single-zine sharing; a Shelf badge; encryption.
- The "changed since last backup" state.
- The poisoned local photo on **restore** (`AdditiveLibraryRestoreCommitter.kt:186-191`; Q8 defect 5, restore
  half).
- A janitor for leftover staging files after a process death.
- Backing up a zine with an unreadable `meta.json` under a fallback title.
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
| 2026-09-26 | **Owner rulings (Q8) applied; base `0aa7a7d`.** Late "Backup cancelled." moved into part 1 (outcome latch). New part 1b written spec: R1 full disk ≠ damaged, R2 no "cancelled" after commit, R3 committed restore reported as added; skip-and-list with the verbatim rule; partial state recorded as an additive `omitted` manifest field (no version bump; older builds' `ignoreUnknownKeys` verified at beta.4-r3 and beta.5); restore notice; last-backup line records partial. ADR-110 amendment and `zinely-v1.md:68` product-law amendment drafted (the quoted "all zines in one user-owned file" is not verbatim in `docs/`). One combined HTML amendment specified. Readiness: part 1 after the amendment; 1b after the amendment plus in-session review of the drafts. Q8 defect 5's backup half resolved by skip-and-list; restore half stays out. |
| 2026-09-26 | **Supplementary owner ruling (after the final planning audit):** a single poisoned or unreadable photo is skip-and-list, never a whole-backup failure. Added: the "A photo that fails its check" section (asset pre-hash; the writer backstop rebuilds a fresh private archive without the zines using the failing entry — verified the archive is private until complete, `RoomProjectRepository.kt:505`, `ZineLibraryBackupWriter.kt:94-104`); reason `photo` in the receipt and in the `omitted` manifest reason vocabulary (string with an `unreadable` fallback, `packageVersion` stays 2); amendment item 10 (photo-reason partial success) and a photo line in item 9; ADR draft Decision 3 (integrity checks never fail the whole backup), renumbered; acceptance item 4, a stop condition, tests and device plant. The restore half stays out. Then, after the independent review (GO WITH FIXES): every single-photo failure classified (a read retried once then skipped; over-limit skipped; the writer's `SOURCE_UNAVAILABLE` and read-side `IO_FAILURE` rebuilt, which needs the writer to separate read side from write side); the whole-backup failures that remain stated explicitly, none of them a photo; the rewrite target (delete and confirm, else a local-storage failure); restore clamps an untrusted `omitted`; item 4's "none" state gains a photo variant, and its shelf sentence appears only when a zine has no row; 1b readiness names part 1. |
| 2026-09-26 | Base moved to `eb75cf7`: Step 0 merged (PR #75); gate 2 (F3 fixture archive) marked done, and part 1 must keep the frozen fixture green. No scope change. |
| 2026-09-25 | Final planning review: an unreadable zine may or may not be visible (damaged after indexing keeps its row), so the copy covers both; partial-file deletion never deletes after a successful close (late Cancel); the earlier row's "`canBackup` brought into scope" is superseded (no `canBackup` change). |
| 2026-09-25 | After both reviews: an unreadable zine is invisible on the shelf, so the `canBackup` fix and state 5 are dropped and the failure says the zine isn't on the shelf; newer-version backup wording added; partial-file cleanup moved into part 1; skip-and-list routed to Q8 as an ADR-110 change; owner approves the amendment (copy changes); file name and year in the line; Article 4 cited; store interface placed in `data-android/prefs`. |
| 2026-09-25 | Re-based on `5f7707a`; folded in the readiness audit and the D1 deep audit: split into parts 1–3 with readiness per part; F3 no longer a prerequisite; "confirmed" redefined as "saved"; staleness state removed (restores keep source times); backup-failure wording and `canBackup` brought into scope; the title's safety claim flagged; option A's rule gaps and the side-loaded transfer reality recorded; product audit added. |
