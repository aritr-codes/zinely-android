# `.zine` v2 whole-library backup torture matrix

**Opened:** 2026-08-21  
**Decision:** [ADR-110](../DECISIONS.md#adr-110)  
**Scope today:** executable contract tests first; no claim that backup/restore is user-operable yet.

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
| Repeated restore of same backup | Safe additive duplicates; assets deduplicate by verified hash | repository integration | 🟨 Pure id/allocation and verified-asset dedupe green; repository integration pending |
| Backup → uninstall/wipe → restore | All zines, text, photos, covers, timestamps, and print output survive | physical device | 🟨 Samsung clean reinstall pass: one real zine and committed text survived; media, cover, timestamp, and print-output parity still pending |
| Restore onto a second device/API level | Same library and rendered/printed result | three-device gate incl. API 24 | ⬜ Pending |
| SAF provider revokes/returns null/throws mid-stream | Calm retry/alternate exit; no partial restore | Android/device | ⬜ Pending |
| Airplane mode full journey | No behavior change and no network dependency | physical device | ⬜ Pending |

## Current package verdict

The pure v2 contract passed independent review after its one required fix and is **GO as foundation work**. The feature itself remains **NO-GO for users** until every pending correctness row through transactional restore is green; the release gate additionally requires the physical-device rows and frozen UI flow.

## 2026-08-28 Samsung recovery evidence

On Samsung SM-A176B (`RZCYA1VBQ2H`, Android 16), the current debug APK created a real A4 zine with
committed text `Restore-verify`, exported it through **Back up this shelf** to a 2,097-byte `.zine` in
Downloads, then underwent uninstall, reinstall, and in-app restore. The fresh installation reported
**“1 zine added to your shelf”**; the shelf contained one zine and reopening it exposed the persisted
`Text: Restore-verify` element. The archive was a normal production export, not a synthetic or corrupt
fixture. See [the device report](2026-08-28-backup-wipe-restore-device-verification.md) for the bounded
claim and exact flow.
