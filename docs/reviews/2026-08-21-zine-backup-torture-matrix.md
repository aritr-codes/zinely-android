# `.zine` v2 whole-library backup torture matrix

**Opened:** 2026-08-21  
**Decision:** [ADR-110](../DECISIONS.md#adr-110)  
**Historical scope (21 August):** executable contract tests first; backup/restore was not yet user-operable.

**9 September reconciliation:** the table and foundation verdict below are a dated snapshot, not the
current shipping verdict. Repository integration and the production UI subsequently landed; see the
[22 August UI acceptance](2026-08-22-backup-restore-ui-device-verification.md) and
[28 August bounded clean-reinstall recovery pass](2026-08-28-backup-wipe-restore-device-verification.md).
Those passes do not close second-device/API, provider failure, disk-full, or full-media/print stress
coverage. Preserve those limitations; current priorities live in [ROADMAP.md](../ROADMAP.md#current-priorities).

**28 September:** [ADR-121](../DECISIONS.md#adr-121) changes the expected result of the row "Repeated restore of
same backup" from safe additive duplicates to *nothing new to add* (zines identical to ones already on the
shelf are not added again; changed zines are). The row below is kept as the dated snapshot; the new
expectation is untested until ADR-121 is implemented.

This is the gate required by the V1 execution plan. A row is green only at the layer that can actually prove it. Pure structural tests cannot stand in for byte-level, transactional, SAF, or physical-device evidence.

| Case | Expected result | Evidence layer | Status |
|---|---|---|---|
| Canonical multi-project v2 manifest round-trip | Identity, cover recipe, document metadata, and deduplicated asset table survive | `:core:data` unit | ✅ Green |
| v1 package presented to v2 validator | Refused by v2 without changing the v1 contract | `:core:data` unit | ✅ Green |
| Unknown/newer package kind or version | Honest refusal; no live writes | `:core:data` unit + later integration | ✅ Pure refusal green; repository integration pending |
| Newer document schema | Honest refusal; no downgrade/save | `:core:data` unit + codec decode | ✅ Green |
| Duplicate project id or document path | Refused | `:core:data` unit | ✅ Green |
| Unknown or partial cover metadata | Warning; work remains restorable and shelf degrades coverlessly | `:core:data` unit | ✅ Green |
| Missing, duplicate, unreferenced, or unexpected asset/entry | Refused | `:core:data` unit | ✅ Green |
| Absolute, drive-qualified, backslash, empty-segment, `.` or `..` path | Refused before extraction escapes staging | `:core:data` unit + codec integration | ✅ Green |
| Declared byte count differs from streamed bytes | Refused | `:core:data` unit + codec integration | ✅ Green |
| Manifest/document/asset/total expansion limit exceeded | Refused without OOM or partial import | unit + JVM codec stress | ✅ Pure limits green |
| Document SHA-256 mismatch | Refused; no live writes | JVM codec integration | ✅ Green |
| Asset SHA-256 mismatch or same name/different bytes | Refused; no dedupe poisoning | JVM codec integration | ✅ Green |
| Decoded document image references differ from its manifest asset list | Refused; no missing image or undeclared payload can commit | JVM codec integration | ✅ Green |
| Extreme compression ratio / ZIP bomb | Streaming abort at configured expanded-byte/count boundary | JVM codec stress | ✅ Green at configured ratio/count/expanded limits |
| Truncated/corrupt ZIP or malformed manifest JSON | Actionable failure; existing library unchanged | JVM/Android integration | ✅ JVM green; Android error mapping pending |
| Disk full during staging | Failure with an exit; existing library unchanged; staging cleaned | Android/device | ⬜ Pending |
| Cancellation/process death during staging | Existing library unchanged; stale staging recoverable/cleanable | Android/device | 🟨 JVM cancellation cleanup green; process/device pending |
| Failure during commit | All-or-nothing project visibility; Room rebuilds from files | JVM/Android integration | 🟨 Journal/rollback primitive green; repository lock, recovery wiring, and Room integration pending |
| Existing project id collision | Restore mints a new local id; never overwrites | repository integration | 🟨 Pure allocator green; repository integration pending |
| Repeated restore of same backup (**superseded by ADR-121**; see the step 1b rows below) | Safe additive duplicates; assets deduplicate by verified hash | repository integration | 🟨 Pure id/allocation and verified-asset dedupe green; repository integration pending |
| Backup → uninstall/wipe → restore | All zines, text, photos, covers, timestamps, and print output survive | physical device | 🟨 Samsung clean reinstall pass: one real zine and committed text survived; media, cover, timestamp, and print-output parity still pending |
| Restore onto a second device/API level | Same library and rendered/printed result | three-device gate incl. API 24 | ⬜ Pending |
| SAF provider revokes/returns null/throws mid-stream | Calm retry/alternate exit; no partial restore | Android/device | ⬜ Pending |
| Airplane mode full journey | No behavior change and no network dependency | physical device | ⬜ Pending |

**1.x step 1b rows ([ADR-122](../DECISIONS.md#adr-122), [ADR-121](../DECISIONS.md#adr-121); added 2026-09-29, on branch
`fix/1x-step1b-backup-restore`, not yet merged).** Evidence is `:data-android` Robolectric integration
(`RoomProjectRepositoryRestoreTest`) unless named otherwise; device rows stay with the step 1b device pass.

| Case | Expected result | Evidence layer | Status |
|---|---|---|---|
| Unreadable zine at backup | Left out and named; the rest saved; `omitted` in the file | repository integration | ✅ Green |
| Poisoned photo shared by two zines | Both zines left out, reason `photo`; the photo's bytes never enter the archive | repository integration | ✅ Green |
| Writer backstop after the pre-check | The incomplete private archive is removed, the entry's zines left out, the survivors rewritten | repository integration | ✅ Green |
| All zines unreadable | No archive; `NothingSaved`; the destination discarded; no last-backup record | repository + transport + VM | ✅ Green |
| Partial archive on an old reader | Restores what it holds, without the partial notice (ADR-122 Consequences) | `ignoreUnknownKeys` + frozen partial fixture (`LibraryBackupFixtureTest`) on this build; beta.4-r3 / beta.5 device check | 🟨 Current-build fixture green; old-build device check pending |
| Disk full while staging a restore (R1) | "Not enough space" when the private disk is under the 64 KiB probe, else "Couldn't finish that restore"; never "damaged" | stager seam + repository integration | 🟨 Unit/integration green; device pending |
| Cancel racing the restore commit (R2) | Cancel first: nothing committed, "Restore cancelled."; commit first: Cancel is a no-op | repository + VM | ✅ Green |
| Room fails after a committed restore (R3) | A success with the lagging line; the shelf is re-read | repository + VM | ✅ Green |
| Repeated restore of the same backup (ADR-121) | "Nothing new to add"; no project directory created | repository integration | ✅ Green (supersedes the dated row above) |

## Historical foundation verdict — 21 August

The pure v2 contract passed independent review after its one required fix and is **GO as foundation work**. The feature itself remains **NO-GO for users** until every pending correctness row through transactional restore is green; the release gate additionally requires the physical-device rows and frozen UI flow.

## 2026-08-28 Samsung recovery evidence

On Samsung SM-A176B (`RZCYA1VBQ2H`, Android 16), the current debug APK created a real A4 zine with
committed text `Restore-verify`, exported it through **Back up this shelf** to a 2,097-byte `.zine` in
Downloads, then underwent uninstall, reinstall, and in-app restore. The fresh installation reported
**“1 zine added to your shelf”**; the shelf contained one zine and reopening it exposed the persisted
`Text: Restore-verify` element. The archive was a normal production export, not a synthetic or corrupt
fixture. See [the device report](2026-08-28-backup-wipe-restore-device-verification.md) for the bounded
claim and exact flow.
