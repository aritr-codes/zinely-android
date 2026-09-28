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
| **1** | "Last backup saved" fact, honest backup failures, no partial file left behind, no "Backup cancelled." after a complete file | ✅ **READY** — the one `backup-restore.html` amendment ([spec](#backup-restorehtml-amendment-specification)) was drawn, **owner-approved and re-frozen 2026-09-27** ([freeze record](../design/BACKUP-RESTORE-FREEZE.md)). The **backup-sheet polish** (no note, equal action tiles, file name on its own line) was frozen later the same day. **Step 1 ✅ COMPLETE 2026-09-28** (PR #81, [ADR-120](../DECISIONS.md#adr-120) Accepted), polish included |
| **1b** | Restore honesty (three defects) and skip-and-list backups — complete or explicitly partial, never silently partial | **READY AFTER** part 1 has merged (the amendment is already approved, 2026-09-27) **and** the ADR and product-law amendment are independently reviewed and landed in the implementation session, before any code (✅ landed 2026-09-28 as [ADR-122](../DECISIONS.md#adr-122), Proposed, and [`zinely-v1.md`](../zinely-v1.md) §5/§6; implementation ⏳ in progress), **and** [Part N](#part-n--restore-adds-whats-new) is frozen with its ADR (✅ done 2026-09-28, [ADR-121](../DECISIONS.md#adr-121)); owner ruling 2026-09-28: both change what a restore reports |
| **N** | Restore adds what's new: an identical zine is not added again, a changed version is added, nothing is replaced | ✅ **Design FROZEN 2026-09-28** ([ADR-121](../DECISIONS.md#adr-121), Accepted, design); implementation not started |
| **2** | "Changing phones?" line | **BLOCKED BY** the physical cross-device restore pass |
| **3** | Android device-to-device transfer | **BLOCKED BY** O1. Research leans to not doing it |

## Gates before an implementation session may start

Owner rulings: **Q8, approved 2026-09-26**, recorded in the
[decision gate](ZINELY-1X-DECISION-GATE.md#q8-the-next-release-and-the-backuprestore-defects). Q8 scheduled part 1b and
skip-and-list in wave 1, moved the late "Backup cancelled." into part 1, and set the product rule. A
supplementary ruling the same day (after the final planning audit) put a single poisoned or unreadable photo
under skip-and-list ([below](#a-photo-that-fails-its-check)). No further owner *ruling* is needed for parts 1
and 1b. The drawn HTML amendment was approved and re-frozen on 2026-09-27, with two further rulings on
whole-backup failures ([F1 and F2](#which-state-a-whole-backup-failure-shows-owner-rulings-f1-and-f2)).

Parts 1 and 1b:
1. **✅ Done — step 1a, approved and re-frozen 2026-09-27:** one amendment to `docs/design/mockups/backup-restore.html` covering
   every state in [the amendment specification](#backup-restorehtml-amendment-specification), then review, owner
   approval and re-freeze. It adds states and changes copy inside existing ones, so it is not purely additive.
2. **✅ Done — step 0** of the [plan sequence](ZINELY-1X-IMPLEMENTATION-PLAN.md#5-sequencing), the F3 fixture archive
   (PR #75; `LibraryBackupFixtureTest`, `core/data-storage/src/test/resources/fixtures/library-backup-v2.zine`).
   - It pins compatibility with the beta.4-r3 and beta.5 backup files already in makers' hands.
   - Parts 1 and 1b must keep it green and must **never regenerate** it; the fixture is frozen.

Part 1b only:
3. **✅ Done 2026-09-28 — in the implementation session, before code:** the [ADR](#adr--amendment-to-adr-110)
   and the [product-law amendment](#product-law-amendment) were independently reviewed (Review Agent) and landed
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

> **[ADR-122](../DECISIONS.md#adr-122) is the decision; where this spec and the ADR differ, the ADR wins.** Its
> review changed four details here: the package validator's per-entry rules (a zine's document size, times and schema
> range; a photo's type, dimensions and byte count) are skip points too; a read-side failure or unavailable source on a
> **document** at the writer fails the whole backup (only an integrity mismatch or a per-entry limit on a document is
> rebuilt without it); 0 of M is a third transport result, `NothingSaved`, never a save; and R1 uses only the restore
> transport's 64 KiB free-space probe, without errno sniffing.

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
| `documents.load` fails — `Io` / `OutOfSpace` / `Busy` | :431-433 | **Whole backup fails** (transient; a retry may succeed). State by [F1/F2](#which-state-a-whole-backup-failure-shows-owner-rulings-f1-and-f2): `OutOfSpace` → no space; `Busy` → busy; `Io` → not finished |
| Document size / hash `IOException` | :440-444, :451-455 | Whole backup fails (transient) → not finished ([F1](#which-state-a-whole-backup-failure-shows-owner-rulings-f1-and-f2)) |
| Malformed JSON / no `schemaVersion` | :445-450 | Skip, list |
| Asset missing | :479-481 | Skip **every** zine referencing it, list each; reason "photo" |
| Asset not a readable image | :482-486 | Same |
| Asset bytes don't match their hash (poisoned) | writer `INTEGRITY_MISMATCH`, `ZineLibraryBackupWriter.kt:140-142` → `Corrupt` (repo :514) | **Skip and list** (owner ruling, 2026-09-26): pre-hash each asset in the asset pass and treat a mismatch like a missing asset (reason "photo"). The writer check stays as a backstop and **never fails the whole backup** ([below](#a-photo-that-fails-its-check)) |
| Writer backstop mismatch on a document or asset | `ZineLibraryBackupWriter.kt:126-142`, after the pre-checks passed | **Rebuild without it** ([below](#a-photo-that-fails-its-check)): skip and list every zine using it, write a fresh private archive |
| Asset size `IOException`, or an `IOException` while pre-hashing an asset | :487-491; the new pre-hash | **Retry that read once; if it fails again, skip and list, reason "photo"** (owner ruling: an unreadable photo is skip-and-list). A single flaky read is absorbed by the retry; a photo that stays unreadable leaves its zines out, named, never silently |
| Asset over the per-asset limit (declared, or growing during the copy) | writer `LIMIT_EXCEEDED`, `ZineLibraryBackupWriter.kt:226-227,247` | **Skip and list**, reason "photo": checked in the pre-check against `maximumAssetBytes`; a copy that grows past it is a backstop rebuild |
| Writer `SOURCE_UNAVAILABLE` or a **read-side** `IO_FAILURE` on an asset entry | `ZineLibraryBackupWriter.kt:157,212` | **Rebuild without it**, reason "photo" (backstop). 🟦 The writer must say whether an `IOException` came from reading a named source or from writing the private archive; today `IO_FAILURE` wraps both (:155-160) |
| **Write-side** `IO_FAILURE` (the private archive itself), library-wide limits (manifest size, entry count, total expansion — :181, :184, :285) | writer | **Whole backup fails**, in backup wording by [F1](#which-state-a-whole-backup-failure-shows-owner-rulings-f1-and-f2): out of space → no space; other private-archive I/O → not finished (retry); a library-wide limit → not finished, *Got it* only. Not a photo problem: nothing is left out to fix it |

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
    private file can't be removed, the backup fails as a **local storage** error — "not finished" by
    [F1](#which-state-a-whole-backup-failure-shows-owner-rulings-f1-and-f2), never the save-error wording, never "damaged" and never blaming the photo; that is a failure of app-private storage, not of a photo;
  - each rebuild removes at least one entry, so it ends; 🟦 bound it by the number of distinct entries;
  - if no zine survives, it is the "0 of M" case: no file, the "none" state.
- **What still fails the whole backup after 1b, stated:** a write-side failure on the private archive (space,
  I/O); library-wide limits (manifest size, entry count, total expansion); a transient document read
  (`Io`/`Busy`); the private-archive clean-up failure above. **None of them is a single photo.** Which state
  each shows is [F1/F2](#which-state-a-whole-backup-failure-shows-owner-rulings-f1-and-f2).
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
named. Frozen copy (amendment 1a): "Zinely couldn't open 1 zine, so it isn't in this backup. It has no
readable name." (the earlier draft "1 zine without a readable name" read as a cause). A listed zine with no shelf row adds the sentence "Some of these
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

#### Which state a whole-backup failure shows (owner rulings F1 and F2)

**Approved 2026-09-27 — the contract step 1 implements.** Drawn in `backup-restore.html` (amendment 1a).
A whole-backup failure is shown by **where** it failed, not by its raw `DataError` kind. In order:

1. **Genuine out of space, anywhere** (the private archive or the chosen destination) → "Not enough space"
   (Try again + Got it). The actionable truth wins over where it happened.
2. **The completed archive can't be written or copied to the maker's chosen destination** → the save error,
   "Couldn't save the backup there" / "Pick another location and try again." (Try again + Got it). Used
   **only** for the chosen destination, never for a failure earlier in Zinely's own work.
3. **A failure while Zinely builds, validates or privately cleans up the archive** → "Couldn't finish that
   backup" / "Nothing about the zines on this shelf was changed." (`Copy.errorGenericTitle/Body(isBackup =
   true)`, red "!"):
   - Try again + Got it: a transient document read, private-archive I/O (not space), clean-up of the private
     or temporary archive, any other local backup-process failure;
   - **Got it only:** a deterministic archive-wide or library-wide limit (manifest size, entry count, total
     expansion). The same shelf hits the same limit.
4. **One photo's integrity or read failure** → skip-and-list, a partial success ([above](#a-photo-that-fails-its-check)).
   Only when every zine is left out: the no-file "No zines could be saved" path.

**F2:** `Busy` (a zine is still being put away) is an in-progress condition, not a failure. It keeps the
frozen "Give Zinely a moment" state and is never folded into "not finished".

This supersedes the earlier "as today" and "space / save-error wording" mappings for these failures. How
step 1 carries it into error kinds (existing or new) is implementation work, with tests: today a backup
`DataError.Io` maps to `SaveFailed` whether the destination or Zinely's own work failed
(`HomeViewModel.kt:477,481-486`), and a library-wide limit reaches the maker as `Corrupt` → "damaged"
(`RoomProjectRepository.kt:514`, `HomeViewModel.kt:473`). Both must change.

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
  name." Then the existing "What was already on this shelf stayed put." With Part N the lines run in its N5
  order: title → already-here line → this notice → "stayed put".
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

## ADR — amendment to ADR-110

The draft that stood here landed as [ADR-122](../DECISIONS.md#adr-122) (Proposed, 2026-09-28) in the step 1b
implementation session, after independent review; the review corrected it in substance (the shelf-entry premise,
the per-entry validator rules as skip points, a transient document read at the writer failing the whole backup, the
`NothingSaved` carrier for 0 of M, and R1 without errno sniffing). Read the ADR, not this brief, for the decision.

## Product-law amendment

The draft that stood here landed with ADR-122 in [`zinely-v1.md`](../zinely-v1.md) §5 (the backup row: *complete or
explicitly partial, never silently partial*) and §6 DoD 2 (the torture-matrix case). Read that document for the law.

## `backup-restore.html` amendment specification

> ✅ **Approved and re-frozen 2026-09-27.** The drawn amendment in `backup-restore.html` is now the
> authority for every string below; where the two differ, the HTML wins (its header lists the owner's
> choices: title kept, commit-phase body replaced, retry only where it can help, save-error / space copy
> reconciled to `Copy.kt`, the "not finished" state added, the stale "all/every zine" lines replaced).
> The table is kept as the specification it was drawn from.
>
> **Backup-sheet polish, frozen 2026-09-27** ([freeze record](../design/BACKUP-RESTORE-FREEZE.md)): `#trustNote`
> is removed on both shelves and nothing replaces it; Backup and Restore share one icon-tile treatment; item 1's
> file name sits on its own line under the date, with no "·". Row 1's copy below predates it.

One amendment for parts 1 and 1b. Every string below was 🟦 **proposed**; the drawing decided. Each new or
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
| 4 | Backup failures in backup wording (`#errorSheet`, new branches) | **Add**, separate from restore's `damaged`/`newer` | None readable: "No zines could be saved" / "Zinely couldn't read the zines on this phone, so no backup was saved." Plus "They may not appear on your shelf." **only** when some left-out zine has no shelf row. Photo variant, when every zine was left out for a photo: "Zinely couldn't read the photos in the zines on this phone, so no backup was saved." · Newer: "A zine here needs a newer Zinely" / "Update Zinely, then back up." Save-there / space / busy: **confirm** existing wording (`Copy.kt:1528-1533`), not redrawn. *Drawn (2026-09-27):* those three reconciled to `Copy.kt`, plus "Couldn't finish that backup" ([F1](#which-state-a-whole-backup-failure-shows-owner-rulings-f1-and-f2)) |
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
  and backup-failure strings separate from the restore ones. After the 2026-09-27 polish: the date and file
  name are two lines (no "·"), and the sheet's note strings are no longer shown.
- `data-android`: a `BackupRecordStore` (the date and, if reported, the file name) on the **existing**
  DataStore singleton. A second DataStore on the same file throws. Its interface lives beside the existing ones
  in `data-android/prefs` (`EditorOnboardingStore.kt`, `PreferredPaperStore.kt`), matching
  [plan §7](ZINELY-1X-IMPLEMENTATION-PLAN.md#7-ios-preparation-architecture-only).
- `data-android` transport: `LibrarySafTransport.backupTo` deletes the created document when the write fails
  or is cancelled **before the stream has closed successfully**, and marks the operation done once it has
  (the [latch](#interaction-details)). Once done it never deletes and a late Cancel is a no-op.
- `app`: write the record on success; the latch in `cancelBackupRestore`. In backup mode, map
  `Corrupt`/`Invalid` and `SchemaTooNew` to the two new backup-wording kinds (`HomeViewModel.kt:469-486`),
  separately from restore; classify every other backup failure by *where* it failed
  ([F1/F2](#which-state-a-whole-backup-failure-shows-owner-rulings-f1-and-f2)) — today `ioFailureKind()` maps every backup `Io` to `SaveFailed`, which F1 allows
  only for the chosen destination; expose the record to Home state.
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
1. ✅ The amended `backup-restore.html` is reviewed and re-frozen before any Compose work (2026-09-27).
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
  that is not in the stated list — now [ADR-122](../DECISIONS.md#adr-122) Decision 2, which supersedes the list [above](#a-photo-that-fails-its-check).
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

## Part N — restore adds what's new

> **✅ Frozen 2026-09-28; decision recorded as [ADR-121](../DECISIONS.md#adr-121) (Accepted, design).**
> The owner approved the contract, the matching rule and one refinement to the N1 copy. The drawing is
> Amendment N in [`backup-restore.html`](../design/mockups/backup-restore.html), and the freeze record is
> [BACKUP-RESTORE-FREEZE.md](../design/BACKUP-RESTORE-FREEZE.md). **Implementation is not started**: no
> Kotlin, tests or goldens yet (see [§11 of the plan](ZINELY-1X-IMPLEMENTATION-PLAN.md)). Until it ships, the
> app still adds a separate copy of every zine, and older builds always will.

### Owner rulings (2026-09-28)

| # | Ruling |
|---|---|
| 1 | **Contract:** *"Restore adds zines that aren't already on your shelf. It never replaces what's here."* Restoring the same backup again does not add another identical set. |
| 2 | **"Already on the shelf"** = same **title, format, paper size and content**. Never the id alone. Multiplicity counts: one identical shelf zine matches at most one identical backup zine; surplus identical backup zines are added. |
| 3 | **A differing version** (older or newer) is added alongside; never replaces, never silently skipped. |
| 4 | **Version-aware metadata and messages: deferred.** No origin id, no per-zine origin field. The result reports only how many were added, how many were already here, or "Nothing new to add". |
| 5 | **Order of work:** this amendment → owner review → freeze → ADR → **only then** part 1b (both change what restore reports; 1b's R3 changes the result). |
| 6 | **No confirmation** after choosing the file. Restore → choose `.zine` → restore begins. Revisit only if real usability testing shows the instant start still misleads ([deferred](#deferred-ux-observation-the-instant-restore)). |
| 7 | **PR #81 unchanged:** no deduplication in step 1, no ADR-120 change. |

### Current and new contract

| | Until 2026-09-28 (ADR-110) | Part N (ADR-121) |
|---|---|---|
| Same backup restored twice | A second full set of copies ([ADR-110](../DECISIONS.md#adr-110) consequences: *"Repeated restores may create safe project duplicates"*) | Nothing new to add; the shelf is untouched |
| A zine whose id is already on the shelf | Returns as a separate copy with a new id | Skipped if identical (title, format, paper, content); otherwise added with a new id |
| A changed version | Added (as every zine is) | Added alongside |
| Anything on the shelf | Never replaced | Never replaced (unchanged) |
| Another phone / a renamed file | Everything added | Everything that isn't already there is added; file name and phone never matter |
| A copy on purpose | Restore again, or Duplicate | **Duplicate** is the explicit way to make a copy |

### States and copy (frozen 2026-09-28)

| State | Title | Lines, in order |
|---|---|---|
| N1 chooser, content shelf | `Keep your zines` (unchanged) | Restore subline: *"Add zines from a Zinely backup. Zines already on this shelf aren't added again. If a zine has changed since the backup, the changed one is added too."* Empty-shelf subline unchanged (nothing there can match) |
| N2 all new | `7 zines added to your shelf` | *"What was already on this shelf stayed put."* (the frozen `restored` state, unchanged) |
| N3 some new | `2 zines added to your shelf` | *"The other 5 were already here, so they weren't added again."* · *"What was already on this shelf stayed put."* |
| N3, one already here | `6 zines added to your shelf` | *"The other zine was already here, so it wasn't added again."* · stayed put |
| N3, one added | `1 zine added to your shelf` | *"The other 6 were already here, so they weren't added again."* · stayed put |
| N4 nothing new | `Nothing new to add` | *"The zines from this backup are already on your shelf."* (one zine: *"The zine from this backup is already on your shelf."*) |
| N5 with 1b, partial archive | as N3 / N4 | already-here line · 1b's partial-archive notice · stayed put (N4: no stayed put) |
| N5 with 1b's R3 (lagging) | as N3 | already-here line · the frozen lagging line (no stayed put, as frozen) |

Rules the drawing fixes:
- **Every outcome is a success**: ✓ mark, `Done`, no warning colour. N4 is a successful no-op, never
  `0 zines added` and never an error. It avoids *"Nothing on this shelf was changed."*, because that is the
  frozen **failure** body, and it omits "stayed put", because nothing was added.
- **The title keeps the count added**, so 1b's frozen partial and lagging titles don't change. The owner's
  example `2 zines added · 5 already here` was not adopted: every title in this flow is one sentence, and the
  polish has just removed a `·` because a 200 % wrap strands it.
- **Product words only.** No "hash", "id", "match", "duplicate" or "skip" in copy. "Already here" is the
  maker's word for it.
- **Line order (N5):** title → already-here line → partial-archive notice → lagging line *or* "stayed put".
- **N4 never meets 1b's commit phase.** A restore with nothing new never commits, so *"This part can't be
  stopped."* and the lagging line never follow it.
- **Pass 2 question for the device check** (final review, 2026-09-28): after a maker edits a zine, the
  restore adds the backup's copy. Check that *"the changed one is added too"* isn't read as "my edited zine is
  added again". The owner's text stands.

### Matching model (for implementation; not product copy)

1. **Key.** Title (exact string; no trimming or case folding), format, paper size, and content. Title comes
   from `meta.json` on the shelf side and from the manifest entry on the backup side. Format and paper size
   live in the document and in the manifest entry; they are compared explicitly anyway.
   **Not in the key:** the cover pair (`coverSurface` / `coverStamp`) and the created / updated times. A
   backup zine that differs from a shelf zine only in its cover or its times is already here; its cover is
   not brought back.
2. **Content.**
   - Equal SHA-256 of the raw `document.json` bytes (shelf file vs the manifest's verified `documentSha256`) is
     a match.
   - Otherwise, compare the **decoded** `ZineDocument` values (data-class equality after migration). This
     catches equal content in older bytes: any save re-stamps the current schema
     (`JsonDocumentSerializer.kt:47-49`), so an older backup of unchanged content differs by bytes.
   - An equivalent implementation re-serializes each decoded document with the current serializer and
     compares those hashes, which gives one sortable key.
3. **Multiplicity.** Count shelf zines per key. Walk the backup entries in manifest order: if the key has a
   shelf zine left, use it up and count the entry as already here; otherwise add the entry. A shelf zine
   satisfies at most one backup entry. Because the key is an equality, this greedy count is exact.
4. **Any doubt adds.** A zine is added unless the shelf zine can be shown to have the same content.
   Byte-identical files match at once, whether or not they decode, because an identical copy adds nothing the
   shelf zine lacks. Otherwise the decoded content is compared (item 2). If equal content can't be shown, the
   zine is added. The cases below apply only when the bytes differ.
   - A title that differs, an unreadable shelf zine, a document that can't be decoded on either side, or an
     unequal value (including float noise such as `-0.0` vs `0.0`) all mean **add**, never a silent skip.
   - An unreadable shelf zine matches nothing, so the restore stays fail-closed on the backup and fails open to
     adding.
5. **Nothing on the shelf is written.** Added entries keep today's path: `RestoreProjectIdAllocator` keeps a
   free id and mints a new one on a clash (a changed version with the same id still gets a new id), and the
   committer still refuses to overwrite.
6. **Where it runs.**
   - Inside `prepareRestore`, before allocation, under the existing writer lease and repository mutex
     (`RoomProjectRepository.kt:316-360`).
   - It reads the shelf from **files** (`meta.json` + `documents.load`), not Room, the way
     `createLibraryBackup` does.
   - Cost: hash every shelf document (about a backup's reading); **decode** a shelf document only when its
     title, format and paper size equal some backup entry's and its bytes don't, so most restores decode
     nothing on the shelf side.
   - Pending undoable deletes are already committed before the picker opens
     (`HomeViewModel.requestBackupRestorePicker`), so a zine the maker just deleted can't absorb a match.
7. **Photos.** Unchanged: the committer installs the staged assets and skips hashes already present. A
   restore that adds nothing never commits, so it repairs no missing photo file on the shelf. This is accepted:
   it is a separate integrity question, not this contract.
8. **Out of scope.** v1 single-zine packages (library restore accepts only v2,
   `ZineLibraryBackupValidator.kt:38`), and cleaning up duplicates already on a shelf. Part N prevents new
   ones; it removes nothing.

Verified against the rulings with a standalone model of this rule; that model is not Zinely code. Checked cases:
- 1 shelf / 1 backup identical → 0 added, 1 here.
- 2 / 2 → 0, 2.
- 2 / 1 → 0, 1.
- 1 / 2 → 1, 1.
- Identical + changed version → 1, 1.
- Edited on the shelf since the backup → 1 added.
- Renamed zine → added.
- Equal content in older bytes → already here.
- Empty shelf or another phone → all added.
- The same backup restored twice → nothing new both times.

The file name and the phone are not inputs.

### Smallest implementation change (later; not now)

- **Empty commit.** When every entry is already here, today's path fails:
  - `prepareRestore` would hand an empty list to `AdditiveLibraryRestoreCommitter.commit`, whose validation
    `require(prepared.projects.isNotEmpty())` (`AdditiveLibraryRestoreCommitter.kt:134`) throws.
  - `restoreLibrary` turns that into `DataError.Io("failed to commit library restore")`
    (`RoomProjectRepository.kt:364-366`).
  - The ViewModel maps `Io` to `ReadFailed` (`HomeViewModel.kt:477`), so the maker would read "Couldn't read
    that file".
  - **Fix:** `restoreLibrary` returns success with nothing added *before* the `NonCancellable` commit block when
    the prepared list is empty. The committer keeps its guard.
  - A backup can't be empty (`ZineLibraryBackupValidator.kt:54`), so "nothing added" always means at least one
    zine already here.
- **Receipt.** `LibraryRestoreReceipt` gains an already-here count beside 1b's `addedCount` /
  `shelfUpToDate` (R3). One receipt change, designed once.
- **UI state.** `RestoreAdded` carries the already-here count. Either a `RestoreNothingNew` state is added, or
  `RestoreAdded` with zero added renders N4 (the implementation picks one and documents it). New `Copy` strings
  for N1, N3 and N4.
- **Unchanged:** the manifest, the writer, the stager, the allocator, the committer's guards, and the document
  schema.

### Tests required later

- **JVM, pure (the matching function):**
  - every multiplicity case above;
  - equal content in older bytes;
  - title / format / paper differences;
  - undecodable documents meaning add;
  - manifest order preserved.
- **Robolectric repository (`RoomProjectRepositoryRestoreTest`):**
  - Restore the same archive twice. The second returns success with 0 added and N already here; no project
    directory or row is created, and no commit journal is written.
  - A mixed archive adds only the new and changed entries.
  - A changed version with a clashing id gets a new id beside the original.
  - An unreadable shelf zine never absorbs a match.
  - The frozen fixture `library-backup-v2.zine` restored onto a shelf already holding its zines adds nothing;
    the fixture itself stays byte-identical.
  - The existing colliding-id test (`:156`) keeps passing unchanged: its entry differs in title and content,
    so it is a changed version.
- **ViewModel:** receipt → N2 / N3 / N4. N4 is never `Failed`, and Cancel before commit still says "Restore
  cancelled.".
- **Compose (`LibraryBackupRestoreSheetTest`):**
  - N3 / N4 copy and line order;
  - N4 has the ✓ and `Done`, and no retry;
  - 200 % text;
  - goldens for the new states only.
- **Unchanged and still green:** `RestoreProjectIdAllocatorTest` (the allocator still serves added entries;
  its repeated-restore case is now a unit fact about ids, not the product outcome) and
  `AdditiveLibraryRestoreCommitterTest`.
- **Device (Pass 1 + 2):**
  - Restore the same backup twice on a test shelf, never the owner's own zines: the second restore reads
    "Nothing new to add" and the shelf count is unchanged.
  - Edit one zine and restore again: exactly one zine is added.
  - Pass 2: does N4 read as success or as failure?

### Format and version impact

**None.**
- `packageVersion` stays 2.
- The document schema is unchanged; the v4 plan is unaffected.
- No new manifest or `meta.json` field.
- Matching uses only fields every v2 archive already carries (`title`, `format`, `paperSize`,
  `documentSha256`, the document itself).
- Older builds are unaffected: they don't read anything new. Restored onto an older build, the same file
  still adds copies, because that build keeps the old contract. Release notes say so.

### Interaction with part 1b

- **1b waits for this amendment's freeze and ADR.** Both change what a restore reports: 1b's R3 adds
  `addedCount` / `shelfUpToDate`, and Part N adds the already-here count and N4.
- **1b's partial-archive notice** composes with N3 / N4 (line order above). A restore with nothing new never
  enters 1b's commit phase (R2), and never lags (R3).
- **1b's ADR draft, item 7** ("Restore is unchanged in kind") must be reworded to reference the Part N ADR.
  1b's backup-side decisions (skip-and-list, `omitted`) are unaffected.

### Deferred UX observation: the instant restore

On 2026-09-28 the device log showed each "zines added" result follow **one tap in the system picker**. The
restoring sheet then flashes for about 200–300 ms, so the owner read the result as appearing unprompted. It
was not a race: no code path shows a result without a chosen file.
- Part N makes a repeated restore harmless (N4).
- The restore still begins as soon as a file is chosen.
- A confirmation step is **not** part of this amendment (ruling 6). Revisit it only if real usability testing
  shows the start still misleads.

### Lines changed at the Part N freeze

Changed in the freeze change (2026-09-28) unless the row says otherwise.

| Where | Until 2026-09-28 | Now |
|---|---|---|
| [BACKUP-RESTORE-FREEZE.md](../design/BACKUP-RESTORE-FREEZE.md) "Sheet", :120 | "The sheet says that an ID collision returns as a separate copy rather than replacing a zine." | The sheet says restore adds only zines not already on the shelf and replaces nothing |
| same, :121-123 | "…restores add separate zines. Since the 2026-09-27 polish the action sublines say this…" | "…restores add what's new…" |
| same, "Restore semantics" :145-149 | "Restore is additive…" | Add: an identical zine is not added again; a changed version is added; nothing is replaced |
| same, "Result rules" :208 | "Restore success reports the number of zines added and says the existing shelf stayed put." | Plus the already-here count, and "Nothing new to add" as a success |
| same, "Implementation status" :225-226 | "that exact file restored four additive copies" | Kept as historical device evidence, marked as the pre-Amendment-N contract |
| `backup-restore.html` restore subline (`#restoreOptionBody` and its content-shelf text in `showSheet("trust")`), plus the N states | "…a matching zine returns as a separate copy." | N1 is the default subline; the N states are frozen |
| This brief, 1b ADR draft item 7 (:616) | "Restore is unchanged in kind: staged, fail-closed, additive (ADR-110 §5)." | "…additive as amended by ADR-121 (Part N)" |
| This brief, "Restore of a partial backup" (:533-539) | Title + notice + stayed put | Line order N5 |
| [ADR-110](../DECISIONS.md#adr-110) consequences (:12717) | "Repeated restores may create safe project duplicates; any future merge semantics need a new decision." | A status note on ADR-110 points to [ADR-121](../DECISIONS.md#adr-121); ADR-110's text is not edited |
| [Torture matrix](../reviews/2026-08-21-zine-backup-torture-matrix.md) :37 | "Repeated restore of same backup \| Safe additive duplicates…" | A dated 28 September note records the new expectation; the row stays as the snapshot; its test is still owed |
| [1.x plan](ZINELY-1X-IMPLEMENTATION-PLAN.md) §5 sequence and §11 | 1b after step 1 | Part N (design frozen, ADR-121) before 1b; §11 row N and the §5 diagram updated |

### Decision record

The decision is [ADR-121](../DECISIONS.md#adr-121) (Accepted, design, 2026-09-28). It amends ADR-110 §5.
The draft that stood here landed there unchanged in substance, with its review record. Read the ADR, not this
brief, for the decision.

## Change log

| Date | Change |
|---|---|
| 2026-09-28 | **Part N frozen.** The owner approved the contract and matching rule, and refined the N1 line (*"…aren't added again. If a zine has changed since the backup, the changed one is added too."*). Final independent review: GO. Recorded as [ADR-121](../DECISIONS.md#adr-121); the ADR draft here was replaced by a pointer. The freeze applied the listed line changes (1b item 7, partial-restore order, readiness rows). Implementation not started. |
| 2026-09-28 | **Part N (PROPOSED, not frozen):** owner ruling that restore adds only zines not already on the shelf (same title, format, paper size and content; multiplicity-aware; a changed version is added; nothing replaced; no origin metadata; no confirmation). Drawn in `backup-restore.html` ("N · proposed"); spec, matching model, tests, lines to change on freeze and an ADR draft added. 1b now waits on Part N's freeze and ADR (owner ruling). No frozen line changed. |
| 2026-09-25 | Written at `5c40e7b`. |
| 2026-09-27 | **Backup-sheet polish frozen** (continuation of step 1a, owner-approved after the step 1 device check): the note is removed on both shelves, the two action tiles match, and the file name moves under the date ([freeze record](../design/BACKUP-RESTORE-FREEZE.md)). Visual only; F1, F2 and every behaviour are unchanged. Step 1 (PR #81) implements it before merging. |
| 2026-09-27 | **Step 1a approved and re-frozen.** The drawn `backup-restore.html` amendment (items 1–10) is owner-approved and frozen ([freeze record](../design/BACKUP-RESTORE-FREEZE.md)). New owner rulings recorded as the step 1 contract: **F1**, a whole-backup failure is shown by where it failed ([section](#which-state-a-whole-backup-failure-shows-owner-rulings-f1-and-f2)); **F2**, busy is not a failure. The skip-point table, the rewrite-target clean-up and "what still fails" now point at F1; the unnamed-zine copy follows the drawing; part 1 readiness is READY; 1b readiness no longer waits on the amendment. The 1b plan and the additive `omitted` manifest field are unchanged. |
| 2026-09-26 | **Owner rulings (Q8) applied; base `0aa7a7d`.** Late "Backup cancelled." moved into part 1 (outcome latch). New part 1b written spec: R1 full disk ≠ damaged, R2 no "cancelled" after commit, R3 committed restore reported as added; skip-and-list with the verbatim rule; partial state recorded as an additive `omitted` manifest field (no version bump; older builds' `ignoreUnknownKeys` verified at beta.4-r3 and beta.5); restore notice; last-backup line records partial. ADR-110 amendment and `zinely-v1.md:68` product-law amendment drafted (the quoted "all zines in one user-owned file" is not verbatim in `docs/`). One combined HTML amendment specified. Readiness: part 1 after the amendment; 1b after the amendment plus in-session review of the drafts. Q8 defect 5's backup half resolved by skip-and-list; restore half stays out. |
| 2026-09-26 | **Supplementary owner ruling (after the final planning audit):** a single poisoned or unreadable photo is skip-and-list, never a whole-backup failure. Added: the "A photo that fails its check" section (asset pre-hash; the writer backstop rebuilds a fresh private archive without the zines using the failing entry — verified the archive is private until complete, `RoomProjectRepository.kt:505`, `ZineLibraryBackupWriter.kt:94-104`); reason `photo` in the receipt and in the `omitted` manifest reason vocabulary (string with an `unreadable` fallback, `packageVersion` stays 2); amendment item 10 (photo-reason partial success) and a photo line in item 9; ADR draft Decision 3 (integrity checks never fail the whole backup), renumbered; acceptance item 4, a stop condition, tests and device plant. The restore half stays out. Then, after the independent review (GO WITH FIXES): every single-photo failure classified (a read retried once then skipped; over-limit skipped; the writer's `SOURCE_UNAVAILABLE` and read-side `IO_FAILURE` rebuilt, which needs the writer to separate read side from write side); the whole-backup failures that remain stated explicitly, none of them a photo; the rewrite target (delete and confirm, else a local-storage failure); restore clamps an untrusted `omitted`; item 4's "none" state gains a photo variant, and its shelf sentence appears only when a zine has no row; 1b readiness names part 1. |
| 2026-09-26 | Base moved to `eb75cf7`: Step 0 merged (PR #75); gate 2 (F3 fixture archive) marked done, and part 1 must keep the frozen fixture green. No scope change. |
| 2026-09-25 | Final planning review: an unreadable zine may or may not be visible (damaged after indexing keeps its row), so the copy covers both; partial-file deletion never deletes after a successful close (late Cancel); the earlier row's "`canBackup` brought into scope" is superseded (no `canBackup` change). |
| 2026-09-25 | After both reviews: an unreadable zine is invisible on the shelf, so the `canBackup` fix and state 5 are dropped and the failure says the zine isn't on the shelf; newer-version backup wording added; partial-file cleanup moved into part 1; skip-and-list routed to Q8 as an ADR-110 change; owner approves the amendment (copy changes); file name and year in the line; Article 4 cited; store interface placed in `data-android/prefs`. |
| 2026-09-25 | Re-based on `5f7707a`; folded in the readiness audit and the D1 deep audit: split into parts 1–3 with readiness per part; F3 no longer a prerequisite; "confirmed" redefined as "saved"; staleness state removed (restores keep source times); backup-failure wording and `canBackup` brought into scope; the title's safety claim flagged; option A's rule gaps and the side-loaded transfer reality recorded; product audit added. |
