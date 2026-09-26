# Zinely 1.x — implementation-readiness audit

Status: **AUDIT — research only. Nothing here is decided, built or authorised.**
Date: 2026-09-25 · Author: research session (Claude) · Code audited: `origin/main` @ `5f7707a` (app code identical
to the `v0.9.0-beta.5` tag, `32da280`) · Audits: the [1.x plan](ZINELY-1X-IMPLEMENTATION-PLAN.md) and its five briefs.

> **Status note (2026-09-26).** This is an evidence record at `5f7707a`. Since then **Step 0 (F1a, F3, F4) has
> merged** (PR #75, merge `eb75cf7`), adding tests, fixtures, one Gradle task (run in the existing CI step), a `.gitattributes` line and ARCHITECTURE §4.1,
> and no `src/main` change. Where this audit says a guard or fixture "does not exist", read it as true at
> `5f7707a`. Implementation status lives in the [plan](ZINELY-1X-IMPLEMENTATION-PLAN.md), not here.

> **Where this sits.**
>
> ```
> research (docs/research/…) → this audit (evidence) → corrected brief (the spec) → fresh implementation session
> ```
>
> This audit is the **evidence record**. Its corrections are **folded into the plan and the briefs**, which
> are what an implementation session reads. An implementer never reconciles "old brief vs audit"; if a brief
> still carries a "Corrections pending" banner, it is not ready.
>
> Owner decisions are framed in the [decision gate](ZINELY-1X-DECISION-GATE.md) and recorded where
> [OWNER-CHECKLIST](../OWNER-CHECKLIST.md) points, never here. The per-session prompt is the
> [implementation handoff template](IMPLEMENTATION-HANDOFF-TEMPLATE.md).
>
> **How it was made:**
> - Six read-only audit passes: Wave 0, D1 (added in the second pass), D2, D3, D4, D5 + iOS. Each treated
>   the plan, the briefs and all KDoc as untrusted.
> - Independent reviews: code evidence, and product and process. Their findings are reconciled in
>   [the review log](#review-log).
>
> **Conventions:**
> - Evidence is `file:line` on `5f7707a` unless marked.
> - Labels: ✅ verified · 🟦 recommendation (research, not a decision) · 🟨 assumption · ⚠️ not verified.

---

## 1. Current product state

- **Released:** `0.9.0-beta.5` (versionCode 10), tag `v0.9.0-beta.5` on `32da280`, GitHub pre-release, APK only;
  not on Google Play. Website shows beta.5 (merge `5f7707a`). ✅
- **Code since the plan's original base (`5c40e7b`):** only beta.5's Save PDF permission host (`ZinelyNavHost`,
  `ExportViewModel`) and the `ZSheet` scrim fix changed. The plan's file:line citations survived: 25 spot-checked,
  none wrong, a few off by 1–2 lines. ✅ The planning set was re-based onto `5f7707a` on 2026-09-25.
- **beta.5 known limitations that matter for 1.x** ([release notes](../releases/0.9.0-beta.5.md#known-limitations)):
  TalkBack does not follow programmatic focus on Samsung (focus attempt reverted in `d957f1f`); the
  `ZineActionSheet` scrim is still an unlabelled clickable node; Android 7–9 Save PDF verified on an emulator only;
  see-through pictures flattened to white; no asset GC; no font choice.
- **Tests:** 269 unit-test files (117 in `feature:editor`), 132 Roborazzi goldens, jqwik in 4 modules, 10
  `androidTest` files that CI never runs. No checked-in `.zine`, `.zip` or document-JSON fixture anywhere. ✅

## 2. Plan accuracy

The plan's **map of the code is accurate**; its **premises are wrong in six places** that would mislead an
implementer:

| # | Plan / brief says | Reality | Consequence |
|---|---|---|---|
| P1 | F2: command types are too coarse; the **intent** must supply undo labels via `committing()` | Edit commands carry before/after mementos (`Command.kt:24-25,38-39,74,114-115,148-149`); Place, Delete and the page commands carry the element or page itself (:50, :58, :160). Either way the label is **derivable from the command**. Two cases a diff can't separate: *add vs duplicate*, and *Reset framing vs a reframe that ends at crop FULL / fit FILL* (`EditorReducer.kt:188-192`) | Smaller change: a pure `Command.editLabel()`; no `History` or `committing()` change (23 `committing(` call sites, 43 test refs untouched) |
| P2 | F3 must precede D1 | D1 stores the date in DataStore, outside the archive ([Brief 01](BRIEF-01-VISIBLE-OWNERSHIP.md#data-model-implications)) | F3 blocks cross-device/single-zine work and F1 fixtures, **not D1** |
| P3 | F1 shape guard "cannot see ADR-106's copier (added with a default)" | A new field appears in the descriptor, so the guard **would** have caught ADR-106; it cannot see a *changed default value* | Build the guard **now, against v3**, not in wave 2 |
| P4 | D2 needs no schema bump; option (b) "release-note the fallback" is acceptable | 🟨 Averia sets narrower than Inter (audit estimate ~9 %; Fraunces ~2 %; method not recorded, so re-measure). Wherever it is narrower, an older build renders Inter, lines get wider and the last lines are **clipped** (every command is clipped to its box in the replay loop, `CanvasReplayer.kt:87`, `localClip = box` at `SceneRenderer.kt:76`). The only way a newer zine reaches an older build is a library-backup restore (or a downgrade), and release notes never reach that reader | Option (b) conflicts with ROADMAP "unknown-font fallback must not silently become layout loss" (`ROADMAP.md:97` here, `:98` on `main`). A schema bump avoids the clipping but costs more than one zine (see [§6](#6-d2-audit--typefaces--voices) Q3) |
| P5 | D5 needs no freeze amendment | [ADR-107](../DECISIONS.md#adr-107) R1a: set membership, names and order are drawn in `v21-bench.html` (`openArt` :874, staged 16 ~:1752) | New frames need an **HTML amendment first** |
| P6 | D3: "everything in ADR-012 except the ruler shipped" | ADR-012's "all content, fold lines and cut marks inside a ~6 mm inset" is not implemented: the inset is a warning only; fold lines and backgrounds run to the paper edge. This is by decision: [ADR-039](../DECISIONS.md#adr-039) (Accepted) chose edge-to-edge tiling with no sheet margin | The plan overstates what exists; not a defect, but a D3 constraint |

## 3. Stale assumptions

- **Missing from the briefs entirely:**
  - D2's editing surface draws chrome Inter regardless of `fontFamily` (`BenchEditingSurface.kt:216`), so any new face would be edited in Inter and printed in another face. Italic is already a Compose-faked slant while editing.
  - D5's hit test is a rectangle (`HitTest.kt:30-36`), so tapping a frame's empty centre selects the frame, never the photo beneath. This already ships for all six holed supplies (next line).
- **"Frames don't exist"** is wrong: six holed supplies ship — `paper.window`, `paper.hole`, `shape.ring`, `fix.grommet` (`EYELET`, byte-identical outline to the ring), `mark.registration` (two circles under even-odd) and `fix.corner` (photo corner, inner triangle) (`SupplyCatalog.kt:162-169,241-262,284-287,412-414,490,503`).
- **D4: Read mode speaks nothing from the page** (`ProofRead.kt:554,629,674`, `clearAndSetSemantics {}`). Brief 04's criterion 8 ("empty descriptions fall back to today's text") has no "today's text" to fall back to.
- **D4: the `uiautomator dump` step cannot verify traversal order.** It lists child order and carries no traversal attributes, so it shows list order even when `traversalIndex` is right.
- **D4: spatial order has a second cause besides restacking.** A spread's partner half is appended to the list with the lowest `zIndex` (`Command.kt:123`, `EditorReducer.kt:237-259`), so list order and paint order disagree before any restack.
- **Announcements:**
  - `View.announceForAccessibility` is deprecated in API 36, and Zinely targets 36 ([View reference](https://developer.android.com/reference/android/view/View#announceForAccessibility(java.lang.CharSequence))).
  - The only reducer announcement, `"Changed page N"`, is hard-coded English (`EditorReducer.kt:618`). It sits outside `Copy` and outside the prose guard.
- **Fonts:**
  - Averia covers only Latin: Google Fonts lists only the `latin`/`menu` subsets. ⚠️ The audit pass counted 115 of 127 Latin Ext-A code points missing; re-count from the actual TTF cmap before relying on the number.
  - Fraunces has Latin Ext-A but no Greek or Cyrillic.
  - Raw size is 933,716 bytes, not ~840 KB (≈0.5 MB in the APK by inference).
  - Fraunces ships in several optical sizes (9pt / 72pt / 144pt plus Soft variants), so one must be chosen.
- **Printing:**
  - `DEFAULT_SAFE_AREA_INSET_PT = 17` is 5.997 mm. That meets ADR-012's "~6 mm" but sits just under its "0.25 in" (18 pt), and below documented worst cases on Letter: Canon PIXMA 6.4 mm ([Canon](https://support.usa.canon.com/kb/s/article/ART177413)).
  - `Copy.kt:1356-1358` "…check the cover's on top and the text is upright. Printers flip pages differently" is about orientation. 🟨 The audit reads "flip" as duplex language that a first-timer printing one side may misread; that is an interpretation, not a fact.
  - `DEVICE-VERIFICATION §3.2` is a photocopier paragraph filed under TalkBack. **No print procedure exists.**
- **Small drift:**
  - The `DocumentFontRegistry` KDoc says chrome fonts load in `:feature:editor` (they are in `core/ui`).
  - The `EditorA11y` KDoc says 13 actions.
  - `ExportScale.PaperSize` duplicates `ModelEnums.PaperSize` with rounded values.
  - `DefaultDocumentValidator.kt:25` is a sixth `CURRENT_SCHEMA_VERSION` reader the plan omitted.
  - The CHANGELOG `[Unreleased]` compare link still starts at `v0.9.0-beta.4-r3` (`CHANGELOG.md:678`).
  - The "imperfect surface, perfect mechanics" principle exists **only** in the research doc (proposed D13), not in any ratified record.
- **ADR-115** (Font control removal) exists only on PR #70's branch, which is still an open draft. `main`'s ADR-055 still excludes font choice.

## 4. Wave 0 audit

**F4 — CI check that the app has no network access**
- **Current reality:**
  - ⚠️ The merged release manifest declares exactly `WRITE_EXTERNAL_STORAGE` (maxSdk 28), `VIBRATE`, and androidx's `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`. This was reported by an audit pass; no merged-manifest artefact is in the tree, and the source manifests only are cited. Re-derive with `processReleaseManifest` before using the list as a test oracle.
  - No `INTERNET`, no `<queries>`. There is no Play services, Firebase, ML Kit, crash reporter, OkHttp or Coil in the catalog.
  - `allowBackup=false`.
  - Nothing enforces any of this mechanically; the only "no INTERNET" check was done by hand once (`docs/reviews/2026-07-22-beta-release-candidate.md:156`). CI never builds an APK.
- **What the plan says:** a merged-manifest `INTERNET` check plus a dependency allow-list.
- **What is missing or wrong:** nothing structural. Two different claims need two checks:
  - "no network access" is enforced by the permission set: without `INTERNET`, no library can open a socket;
  - "no networking libraries, no analytics SDKs" (CLAUDE.md privacy invariant) bans the libraries themselves, silent or not. Only the plan's dependency allow-list checks that.
- **Evidence:** `app/src/main/AndroidManifest.xml:7-13`, `feature/editor/.../AndroidManifest.xml:11`, `ci.yml:119-144` (`:121-146` at `eb75cf7`), `app/build.gradle.kts:289` (`:343` at `eb75cf7`), `app/src/debug/AndroidManifest.xml`.
- **🟦 Recommendation:**
  1. One Robolectric test, `app/src/test/.../PrivacyManifestTest.kt`, asserting that `requestedPermissions` equals the expected set and that `allowBackup` is false. About 30 lines, runs in the existing CI step.
     - **Pin `@Config(sdk = [28])`.** Robolectric parses the manifest with the real `PackageParser`, which drops a `maxSdkVersion` permission when the test SDK is above it. At SDK 29+ (the neighbouring tests run at 35), `WRITE_EXTERNAL_STORAGE` disappears and the test fails. The alternative is to parse the merged manifest XML directly.
     - With the pin, the `WRITE_EXTERNAL_STORAGE` assertion is what proves the test reads the merged manifest. Never "fix" a failure by deleting it.
     - Unit tests see the **debug** merged manifest, which adds debug-only test components. Assert on permissions and `allowBackup` only, not on the component list.
     - The cited tests (`AppEntryResourcesTest.kt:23`, `ShareInboxTest.kt:194-195`) show how to reach the package manager (`getActivityInfo`), not `requestedPermissions`.
  2. Keep the plan's dependency allow-list for the library half of the invariant. Dropping it is an owner/ADR call, not an audit call.
- **Implementation prerequisites:** re-derive the release permission set; the SDK-28 pin above.
- **Open decision:** none now. Adding Play Billing later changes the set, and that needs an ADR.

**F6 — check that the shared core stays portable**
- **Current reality:**
  - The seven non-UI `core:*` modules are `kotlin.jvm`, so Android APIs cannot compile there; the JDK can.
  - `java.*` imports: model 0, render 0, editor 0, copy 0, imposition 1 (`BigDecimal`), data 1 (`MessageDigest`), data-storage 46.
  - JVM calls that need no import: `codePointAt` / `Character.charCount` (`TextCoverage.kt:58-59`, core:model), `Math.addExact` (3×), `"%02x".format` (2×).
  - No architecture test exists.
- **What the plan says:** a denylist test in wave 0.
- **What is missing or wrong:** nothing, but the value is low today (3 violations, no iOS evidence).
- **Evidence:** see [§10](#10-architecture--ios-implications).
- **🟦 Recommendation:** optional; about 60 lines modelled on `CopyNoProseLiteralTest`.
  - Cover the five modules with no file access, from a test in `core:model` (it runs in the core-only CI job).
  - Deny `import java.` / `javax.`, `.format(`, `System.`, `Math.`, `Character.`, `codePointAt`, `Locale`, `UUID`, `Thread`.
  - **Use word-boundary regexes and strip comments first.** A bare `Math.` matches `TransformMath.` and `FramingMath.` (`EditorReducer.kt:163,170,316`, `LiveTransform.kt:51-62`, `ResizeHandle.kt:39`), and `Locale` matches a KDoc at `SvgProofSheetRenderer.kt:88`.
  - Allow-list the existing real violations by line, after running it once to see the true list.
  - Limit: it is a text grep, so a fully-qualified call bypasses it.
- **Implementation prerequisites:** none.
- **Open decision:** do it now or defer it. Neither choice blocks anything.

**F3 — `.zine` format spec + fixtures**
- **Current reality:**
  - The v2 library backup is specified only by [ADR-110](../DECISIONS.md#adr-110) plus the spike doc. `CURRENT_LIBRARY_BACKUP_VERSION=2`, and the validator requires exactly 2.
  - The single-zine v1 format has no production callers.
  - There are 60 tests across writer, stager, committer and validators, but the writer→stager round trip is built in memory. **A change made to both writer and stager in the same PR passes every test.**
- **What the plan says:** a spec plus a golden fixture archive before D1.
- **What is missing or wrong:** it is not a D1 prerequisite (P2). The real gap is the absence of a frozen artefact.
- **Evidence:** `ZineLibraryBackupManifest.kt:8`, `ZineLibraryBackupValidator.kt:38`, `ZinePackageManifest.kt:13`, `ZineLibraryBackupWriterTest.kt:42-66`.
- **🟦 Recommendation:**
  - Check in one real archive written by beta.5, plus v1/v2/v3 `document.json` files, under `core/data-storage/src/test/resources/` and `core/data/src/test/resources/`.
  - Every future build must restore them.
  - The "spec" is a short section in ADR-110 or ARCHITECTURE, not a new document. No format redesign.
- **Implementation prerequisites:** a beta.5 archive made from **fictional** content (never the owner's device library).
- **Open decision:** finish or delete the unused v1 single-zine package (can defer).

**F2 — Named undo**
- **Current reality:**
  - `History(undo, redo)` is uncapped, in-memory lists of small mementos with no bitmaps, so no memory risk (`EditorModel.kt:72-75`, `EditorReducer.kt:441`).
  - The ten command types and the intents that produce them:

    | Command | Produced by |
    |---|---|
    | Transform | CommitTransform, Nudge, ScaleBy, RotateBy |
    | Reorder | Reorder |
    | Place | PlaceText, PlaceTextAndEdit, CommitAddImage, PlaceSupply, DuplicateElement |
    | Delete | Delete; an emptied text box |
    | EditText | CommitText, StyleText |
    | EditImage | reframe, replace, reset, copier, flip |
    | MakeImageSpread | MakeImageSpread |
    | EditDecor | ink, replace, flip |
    | AddPage / DeletePage | nothing in the UI dispatches them |

  - Announcements travel `Effect.Announce` → `EditorViewModel` → `announceForAccessibility` (`ZinelyNavHost.kt:911`).
  - `BenchSnack` already has a polite live region (`BenchSnack.kt:239-242`). Undo from the bar hides the snack (`EditorScreen.kt:2051-2056`).
  - `core:editor` deliberately does not depend on `core:copy` (`Intent.kt:45-47`).
- **What the plan says:** labels supplied by the intent through `committing()`, merged into `stepHistory`'s announce.
- **What is missing or wrong:** P1 above. Using both the snack live region and `Effect.Announce` would **speak twice**, and a live region does not re-announce identical text.
- **Evidence:** as cited.
- **🟦 Recommendation:**
  1. Add a pure `Command.editLabel(doc): EditLabel(verb, kind, count)` in `core:editor`, derived by diffing the mementos. It takes the pre-step document, because Transform and Reorder commands carry only ids, so the element's kind (photo, text, Art) must be looked up. (Correction from the Brief 04 rewrite.)
  2. Replace `Announce("Changed page N")` with a typed `Effect.HistoryStepped(label, isRedo, landedOnPage)`.
  3. Put the words in a new `Copy.Undo` in `core:copy`, including the page sentence.
  4. `EditorScreen` shows a button-less snack whose live region is the **only** speaker, keyed per step so repeats are spoken.
  5. Tests: one label test per command type (and per sub-change for Transform/EditImage/EditDecor), reducer effect tests, a snack golden after the HTML amendment, and a device listen pass.
- **Implementation prerequisites:** a `v21-bench.html` amendment for the undo-message state. It is **not purely additive**: the frozen spec already shows a post-undo "Put back" toast (`v21-bench.html:808-812`) that Compose never implemented. That is a latent parity gap, and the amendment replaces it.
- **Open decision (can be decided during implementation):**
  - Do *duplicate*, *Reset framing* and *restack direction* need their own words? An adjacent restack is a two-element swap (`ZOrder.kt:39-43`), so "bring A forward" and "send B back" produce the same diff. Only these would need a label supplied by the intent; a kind-free copy ("stacking change") covers every reorder.
  - Does Redo also show a snack?

**Fold-clarity study** (the ROADMAP's first Exploring item; people, not code)
- **Question:** can a first-time maker, using only Zinely's in-app guide, turn a printed sheet into a booklet with pages 1→8 in order, and at which step do they fail?
- **Who:** 6–8 people who have never made a mini-zine, of mixed age and handedness, and nobody who built the product. All use the in-app guide; 2–3 also try the website guide.
- **Protocol (~30 minutes each):**
  - Hand them a sheet printed at actual size from a fictional zine, plus scissors and a phone open at step 1. Say only "make the booklet".
  - Record per step: a pause over 10 s, a wrong fold, going back, a cut error (wrong crease, one layer, too long), a request for help, the total time, and whether the result is correct.
  - Photograph each result. Ask "where were you unsure?"
- **Evidence:** the same failure at the same step for **≥2 people** is a finding; anything seen once is only logged. Nielsen's five-user heuristic ([NN/g, 2000](https://www.nngroup.com/articles/why-you-only-need-to-test-with-5-users/)) supports *finding* problems, not measuring error reduction, so judge any improvement qualitatively.
- **Decisions that depend on it:**
  - caption and diagram fixes (step 1 orientation, step 5 cut);
  - whether a native fold replay is ever built, and only if the recurring failures are about direction and a still-image fix doesn't resolve them in a 3–5-person retest;
  - O14.
- **Record it in:** `docs/reviews/` plus a [RESEARCH.md](../RESEARCH.md) entry.

**F1 — schema v4 + shape guard.**
- **Current reality:**
  - `CURRENT_SCHEMA_VERSION=3`. The migrations are identity steps v1→v2 and v2→v3.
  - The JSON config has `ignoreUnknownKeys=true` and `encodeDefaults=true`. Six readers key off the version.
  - **A v4 document makes the whole library backup unrestorable on an older build**, not just that zine: the stager throws `FUTURE_VERSION` when any project's `documentSchemaVersion` exceeds the build's (`ZineLibraryBackupStager.kt:367-376`), and the validator rejects the manifest (`ZineLibraryBackupValidator.kt:80`).
- **What the plan says:** a descriptor-based shape guard plus the bump, both in wave 2; the guard "cannot see ADR-106's copier".
- **What is missing or wrong:** P3; the sixth reader (`DefaultDocumentValidator.kt:25`); the whole-backup consequence above.
- **Evidence:** `Document.kt:28`, `JsonDocumentSerializer.kt:38-43,119-122`. No custom serializers exist, so a descriptor walk sees the whole shape. `core:model` has no nullable fields today, so `explicitNulls=false` changes no existing bytes (it is `@ExperimentalSerializationApi`).
- **🟦 Recommendation:**
  - **Split the guard from the bump:**
    - **The guard now (wave 0):** walk the serializer descriptors (`elementNames`, `getElementDescriptor`, `isElementOptional`, `kind`), including sealed subclasses and enums, and compare against a checked-in `schema-shape-v3.txt`.
    - **The bump after wave 1** (plan F1b), carrying the alt-text field and the layout-engine marker; `look` takes its own later bump, and D2 takes v5 if it lands after v4 is released (P4; corrected after the second review).
  - Add `explicitNulls = false` in the bump, or every element gains `"description":null` on disk.
  - The bump's release notes must say that backups made after it cannot be restored on earlier versions.
- **Implementation prerequisites:** the guard, none. The bump: F3 fixtures, and the release boundary ([plan §4 F1b](ZINELY-1X-IMPLEMENTATION-PLAN.md#4-foundations--only-what-the-repository-actually-needs): v4 is released with its first user, alt text; anything later takes v5).
- **Open decision:** what rides the bump (§6 Q3, §8 B).

## 5. D1 audit — last backed up / changing phones

> The deep D1 audit ran on 2026-09-25, in the second pass, against `5f7707a`. It was read-only code
> investigation plus separate web research on Android device transfer. The backup stack is unchanged since
> `v0.9.0-beta.4-r3` (only `Copy.kt` differs). Both shipped backup-capable builds write package v2 / schema 3.

**Current reality**

*What a backup contains* ✅
- **Flow:**
  - dock → `LibraryBackupRestoreSheet.kt:67,111`
  - → `HomeViewModel.startBackup` (`:236`), which only runs if the shelf has cards
  - → pending deletes are committed first (`:431`)
  - → SAF `CreateDocument` (`ZinelyNavHost.kt:193-197`)
  - → `backupPicked` (`:246`) → `LibrarySafTransport.backupTo` (`:101`)
  - → `RoomProjectRepository.createLibraryBackup` (`:404`) → `ZineLibraryBackupWriter`
  - → the archive is streamed to the chosen location and the private copy deleted (`LibrarySafTransport.kt:115-132`).
- **Contents:**
  - `manifest.json`: per zine, the title, format, paper, createdAt, updatedAt (the document file's mtime), schema version, a document hash, image hashes and the cover (`RoomProjectRepository.kt:456-470`);
  - `projects/<id>/document.json`, the raw bytes;
  - `assets/<sha256>`, holding **only the photos a zine actually uses**, deduplicated across the library (`:424-439,474-494`).
- **Not in the archive:** Room rows and thumbnails (ADR-110 §2 "derived"), exported PDFs, DataStore, SharedPreferences, onboarding flags, preferred paper, `.bak`/`.tmp` sidecars, orphaned photos, and zines waiting on delete-undo.
- **Limits:** 16 MiB per document, 128 MiB per photo, 8 GiB total, 10,000 zines (`ZineLibraryBackupManifest.kt:17-28`). Entries are stored uncompressed.
- The archive is built in the private cache first, so a backup briefly needs about the library's size in free space. 🟨

*Incomplete zines and in-flight edits* ✅
- **Autosave:** 1 s of quiet, at most 5 s after an edit; it also flushes on pause/stop and on close (`AutosaveCoordinator.kt:43-46,130-141`, `EditorAutosaveBinder.kt:84-88,145-156`).
- **The writer lease:** a backup takes a lease that fails while any editor still holds an autosave handle (`AutosaveCoordinatorFactory.kt:103-107`). So a backup contains the flushed state or refuses with "Give Zinely a moment" (`Copy.kt:1532-1533`).
- Blank zines are backed up. There is no draft concept.
- 🟨 If a teardown flush fails (disk full), the handle is still released, and the backup carries the last durable state.

*Restore* ✅
- **Additive, never replace:**
  - Colliding ids get a new UUID (`RestoreProjectIdAllocator.kt:22-31`), so restoring twice gives two copies.
  - Existing identical photos are reused.
- **Fail-closed:** these all refuse the whole restore as "damaged":
  - a photo listed but missing;
  - a photo present but unlisted or unreferenced;
  - any manifest/document mismatch.
- **Atomic:**
  - bounded copy to cache → staging, with every byte hashed;
  - fsync → a journal (temp → fsync → atomic replace);
  - photos first, then an ATOMIC_MOVE per zine;
  - rollback on failure, and journal recovery after a crash (`AdditiveLibraryRestoreCommitter.kt:90-130,204-250`).

*Across versions* ✅
- A backup holding any zine with `documentSchemaVersion > 3`, or `packageVersion > 2`, is refused whole with "This backup needs a newer Zinely" (`ZineLibraryBackupStager.kt:358-376` → `DataError.SchemaTooNew`).
- Older backups migrate in memory for validation and upgrade on the next save.
- 🟨 A future *enum* value (format, paper) or a changed required field would read as "damaged", not "newer", because the manifest fails to decode before the version check runs.
- The single-zine v1 package has no reader; a v1 file reads as "damaged".

*Changing phones today*
- ✅ `allowBackup="false"`. `data_extraction_rules.xml` (API 31+) and `backup_rules.xml` (≤30) exclude every domain from cloud backup and device transfer (`AndroidManifest.xml:13-15`).
- ✅ **External research:**
  - On API 31+, `allowBackup="false"` does **not** block device-to-device transfer. AOSP ignores it for D2D in apps targeting 31+, and the `<device-transfer>` exclusions are the real guard ([Android 12 backup changes](https://developer.android.com/about/versions/12/backup-restore); [AOSP `BackupEligibilityRules`](https://raw.githubusercontent.com/aosp-mirror/platform_frameworks_base/main/services/backup/java/com/android/server/backup/utils/BackupEligibilityRules.java)).
  - Google's "Copy apps & data" does not copy apps that are not from Google Play ([Google Help](https://support.google.com/android/answer/13761358)).
  - Samsung Smart Switch's handling of app data is undocumented. ⚠️
- So today a new phone gets an empty Zinely (or none), and the `.zine` file is the only way across. The website's Download page already says so.

*Where things live* ✅
- `filesDir/projects/<id>/…`, `filesDir/assets/<sha256>` (plus `.tmp`), `filesDir/.library-restore/`;
- Room at `databases/zinely.db`;
- DataStore at `files/datastore/editor_onboarding.preferences_pb`, with keys `move_resize_hint_seen`, `reframe_coach_seen` and `preferred_paper_size`;
- pending deletes in `shared_prefs/zinely_pending_deletes.xml`.
- No absolute paths are stored.

*Privacy of the archive* ✅
- Photos are re-encoded as JPEG q90 at ≤4096 px, so EXIF is not written (🟨 platform behaviour).
- They are named by hash; no original filenames, no device ids.
- The archive is **unencrypted** (ADR-110) and holds every photo at up to 4096 px, every title and all text, created/updated times and the app version.

**What the plan says:** [Brief 01](BRIEF-01-VISIBLE-OWNERSHIP.md) (before correction) proposed:
- a "Last backed up <date>" fact, written on confirmed success;
- a staleness state from "any project's updatedAt";
- a "Changing phones?" line gated on a cross-device restore;
- optional D2D (O1).

**What is missing or wrong** (all folded into the corrected Brief 01):
1. **"Confirmed written" means only that the provider stream closed without an error** (`LibrarySafTransport.kt:123-130`). There is no read-back, and a cloud provider may upload later. So the fact must be worded as *when a backup was saved*, never as *safe*. The current sheet title, "Your zines, kept safe" (`Copy.kt:1495`), already claims safety before any backup exists. 🟦 The HTML amendment should reconsider it.
2. **The staleness test is wrong as proposed.**
   - Restored zines keep their source mtime (`RoomProjectRepository.kt:560`), so a restore after the last backup adds zines the backup doesn't contain, and "any updatedAt newer than the backup" stays silent.
   - "updatedAt" is a display value, `max(row, document mtime)` (`:817-825`), not a column.
   - 🟦 Defer the staleness state, or compare the shelf's id set with the ids in the last backup.
3. **Backup failures use restore wording.**
   - A zine that can't be read (damaged, invalid or newer) aborts the **whole** backup (`RoomProjectRepository.kt:431-433`).
   - The maker then sees "This backup looks damaged — Zinely couldn't safely bring anything back from that file" (`HomeViewModel.kt:469-486`, sheet `:323-341`), which is restore wording for a backup failure.
   - A zine from a newer Zinely gets "This backup needs a newer Zinely" (`HomeViewModel.kt:474`), also restore wording.
   - *Corrected after review:* the unreadable zine may be **invisible**. A zine that is already unreadable when it is indexed never gets a shelf row (`RoomProjectRepository.kt:604-605,674-676`); one damaged after it was indexed keeps its row (rows go only on delete, :300, or when the file is gone, :687). The backup walks the ids on disk (:419) either way. So `canBackup` (`ZineLibraryScreen.kt:456`) offers *Back up*, and every backup fails, sometimes with nothing on the shelf to point at. The only in-app remedy is a skip-and-list backup, an ADR-110 change ([decision gate Q8](ZINELY-1X-DECISION-GATE.md#q8-the-next-release-and-the-backuprestore-defects)).
   - A "last backed up" line makes backing up more prominent, and so makes this defect more visible. It belongs in D1's scope, not out of it.
4. **Option A's rules were incomplete.** They ignore pending deletes, restore residue, photo temp files and `.bak` files. Transferring `database` keeps stale rows, because reconcile only adds and drops ids (`:669-688`). Excluding `datastore/` also drops preferred paper. Whether transfer preserves mtimes is ⚠️ unknown.
5. **F3 was listed as same-wave work.** It is not a prerequisite of part 1 (P2).
6. **The empty-shelf rule is ambiguous.** "Show neither line on an empty shelf": empty must mean *no zines at all*, not *no openable zines*.
7. **Placement is ambiguous.** "Under the sheet title" ignores the existing `SHEET_BODY` subtitle and the `DESTINATION_NOTE` pill (`LibraryBackupRestoreSheet.kt:77-101`).

**Defects found (not D1 features; recorded so they are not rediscovered)**, in order of maker harm:
- ⚠️ **Cancel during restore commit.** Tapping Cancel during the commit lets the commit finish but shows "Restore cancelled." The zines were in fact added.
- **A failure after commit misleads.** A Room reconcile failure after a successful commit says "Couldn't read that file", although the zines are on disk (`RoomProjectRepository.kt:367-389`).
- **A full disk during staging reads as "This backup looks damaged"** (`ZineLibraryBackupStager.kt:127-133`).
- **A poisoned local photo with the same hash shows "Couldn't read that file"**, blaming the backup for a local problem.
- **A failed or cancelled backup leaves an empty or partial file** at the chosen location. Nothing deletes it.
- **No janitor** cleans staging residue or orphaned photos after a process death.

🟦 These are engineering follow-ups for the owner to schedule. Only the backup-failure wording (item 3 above) is proposed as part of D1.

**Evidence:** file:line as cited above. Tests reviewed:
- `ZineLibraryBackupWriterTest`
- `ZineLibraryBackupStagerTest`
- `AdditiveLibraryRestoreCommitterTest`
- `RestoreProjectIdAllocatorTest`
- `RoomProjectRepositoryRestoreTest`
- `LibrarySafTransportTest`
- `HomeViewModelTest` `:910-1162`
- `RoomProjectRepositoryRestoreInstrumentedTest`, which covers one zine with no photos on one device.

Gaps:
- no pinned archive from a shipped build;
- no cross-device or cross-version test;
- no fresh-repository equality check;
- the JVM tests fake photo metadata, so the real JPEG/4096 gate is untested off-device;
- backup-mode error mapping is untested.

**🟦 Recommendation**
- **Part 1 — "when you last saved a backup", plus honest backup failures.** Needs no owner ruling.
  - A dated fact written only on the `DataResult.Success` branch (`HomeViewModel.kt:251`), stored behind a small store on the existing DataStore singleton.
  - Backup-failure copy that names the real cause (a zine Zinely can't open) instead of restore wording.
  - A failure message that says the zine isn't on the shelf, and deletion of the empty or partial file a failed backup leaves behind.
  - Not a `canBackup` change: the unreadable zine may have no row, so there is nothing reliable for it to count.
- **Part 2 — "Changing phones?" line.** Only after a **physical cross-device restore pass**: two phones, different Android versions, a real library with photos, checking that documents, covers, created/updated times and photo bytes arrive intact. The line must never imply the new phone may run an older Zinely.
- **Part 3 — device transfer (O1).** 🟦 Research leans to **B** (keep exclusions), because:
  - the system path skips side-loaded apps;
  - it would loosen the privacy stance;
  - `allowBackup="false"` would not restore that stance on API 31+.

  Revisit only if Zinely ships on Play.

**What a migration solution must preserve**
- Files are the truth, read under the writer lease.
- Restore stays additive and fail-closed: staging, fsync, journal, rollback, recovery before reconcile; ids are never overwritten.
- Photos stay content-addressed with exact closure; orphans stay out.
- The version check runs before any live write, with the distinct "newer Zinely" message.
- Compatibility with beta.4-r3 and beta.5 files (package v2, schema ≤3), pinned by the F3 fixture.
- EXIF-free re-encoded photos; no network; device state never enters the archive.

**UX required (for the HTML amendment)**
- A dated fact, not a safety claim.
- A stated place in the sheet relative to `SHEET_BODY` and `DESTINATION_NOTE`.
- Backup-failure states in backup wording.
- A "never saved a backup" state.
- An honest title.
- No nag, badge or notification (constitution §VI).
- 🟦 One sentence telling the maker the file holds all their zines and photos, and is theirs to keep somewhere they trust.

**Safely deferred**
- the v1 single-zine reader (or its deletion);
- a janitor for residue;
- the D2D rules change;
- the staleness state;
- deleting a partial file after a failed backup (*superseded:* Brief 01 now schedules this in part 1);
- the restore misclassifications listed above.

**Implementation prerequisites**
- Part 1: a `backup-restore.html` amendment covering the states above, reviewed, owner-approved and re-frozen (*superseded wording:* it changes frozen copy, so it is not purely additive; see Brief 01).
- Part 2: the physical cross-device restore pass (people and two phones), plus the F3 fixture archive.

**Open decisions**
- O1 (can wait; [decision gate](ZINELY-1X-DECISION-GATE.md#decisions-that-can-wait)).
- Whether the restore defects above are scheduled as engineering follow-ups (owner, via ROADMAP).

## 6. D2 audit — typefaces / voices

**Current reality**
- **One document family, and it is only reached by fallback.**
  - `DocumentFontRegistry.Bundled` registers only `"Inter"` (`:102-113`). Every stored text says `"sans-serif"` (`Document.kt:190`) and reaches Inter through the fallback.
  - No production code writes `fontFamily`. `DocumentDefaults.textStyle` exists and is unused.
- **Real faces, never faked.** `BundledFontResolver` loads four real TTFs per family and never synthesises bold or italic.
- **Layout and PDF.**
  - `SharedTextLayout` runs `StaticLayout` with simple line breaking and no hyphenation. Text that overflows its box is clipped.
  - The PDF (SkPDF) keeps text as vectors and embeds a TrueType subset. All candidate faces are `fsType 0`, so embeddable.
- **Coverage checks.**
  - `FontCoverage.requiredCodePoints()` demands Latin Ext-A, Greek and Cyrillic in all four faces of every registered family.
  - `analyzeTextCoverage` has no family parameter.
- **Edit vs print:** see §3. The editing surface uses chrome Inter.
- **The dead control and its paperwork.**
  - The Font control is disabled (`BenchContextBar.kt:145-146`).
  - PR #70 is still an open draft, and ADR-115 exists only on its branch.
  - `v21-typebar.html` says "PROPOSAL, NOT FROZEN", while ZINE-DIRECTION N2 marks it frozen.
- **Governance:** V2-CONSTITUTION §III (`V2-CONSTITUTION.md:146-153`) points both ways:
  - toward zine text: *"Averia carries headings, screen titles and **the maker's own short strings** … Fraunces carries long-form editorial: **zine body, captions, pull quotes**, guide prose."*
  - toward UI only: *"**No fourth UI typeface.**"* and the Amendment 1 log.
  - the binding rule: *"the imperfect face never sets running text … A violation of that sentence is a violation of this constitution."*

**What the plan says:** three named voices chosen per text element (Hand/Averia, Book/Fraunces, Plain/Inter), no schema bump, and the older-build fallback noted in release notes.

**Missing or wrong**
1. **Coverage makes Hand unusable for most European Latin.** It fails on Polish, Czech, Turkish, Romanian and Hungarian even for a three-letter word.
   - Latin Ext-A counts as "supported", so **no warning fires**. The missing glyphs silently fall back to a device font.
   - An explicit per-family fallback needs `Typeface.CustomFallbackBuilder`, which is API 29+. minSdk is 24.
   - 🟦 So the simplest honest behaviour is to disable a voice for text it can't set. That is a product behaviour for the D2 spec to decide, not settled here.
2. **The editing surface is missing** from the brief's touchpoints (§3).
3. **Older-build clipping** (P4).
4. **The brief reads Amendment 1 as binding on document text.** The clause names zine body and the maker's strings but closes on "No fourth UI typeface" (quoted above), so either reading is defensible. The owner must rule.
5. **Two sources disagree on the two-voice fallback.** Brief 02 option (c) is Book + Plain; ZINE-DIRECTION `:974` says **Averia + Inter**.
6. **Fraunces optical size is unspecified.** The chrome uses the 9pt cut.
7. **Printed size.** 🟨 Audit estimate of x-height at 10 pt (method not recorded; re-measure from the font files): Inter 1.93 mm, Averia 1.70 mm, Fraunces-9pt 1.66 mm, so both new faces would print like about 8.7 pt Inter. Averia's legibility at 10–12 pt on an inkjet is ⚠️ untested.

**Evidence:** `DocumentFontRegistry.kt:88-113`, `BundledFontResolver.kt:40-60`, `SharedTextLayout.kt:39-59`, `FontCoverage.kt:61-72`, `TextCoverage.kt:50`, `BenchEditingSurface.kt:213-224`, `EditorReducer.kt:77,503-510`, `V2-CONSTITUTION.md:138-153,329`, `ZINE-DIRECTION.md:259,353,375,716,739,926,974`. External sources:
- [Google Fonts METADATA — Averia](https://raw.githubusercontent.com/google/fonts/main/ofl/averiasanslibre/METADATA.pb)
- [Google Fonts METADATA — Fraunces](https://raw.githubusercontent.com/google/fonts/main/ofl/fraunces/METADATA.pb)
- [OFL FAQ](https://openfontlicense.org/ofl-faq/)

Licensing is low risk: the OFL allows subset embedding in PDFs, and Averia's Reserved Font Names are respected by shipping the files unmodified.

**Is "named voices" the right shape?** 🟦 Yes, for a first slice. A small set of named styles instead of a font list is a common mobile pattern (🟨 e.g. Instagram's named text styles; not sourced), and it fits "materials are finite". Pairing presets would suit Amendment 1 better, but text elements have no heading/body role, so presets would need a new model concept. The risks are coverage and the length rule, not the pattern.

**🟦 Recommendation:** **Do not start D2 in wave 1.** Start only when the prerequisites below are true and the owner has answered §6 item 3 (now decision-gate Q2.4).

**Implementation prerequisites**
1. A family-aware coverage table in `core:model`, verified against the real font files by a `render-android` test. `analyzeTextCoverage(text, family)`.
2. Per-family `requiredCodePoints`.
3. `BenchEditingSurface` builds a Compose `FontFamily` from the same assets, which also fixes the faked italic.
4. Canonical names: `"sans-serif"` and `"Inter"` both mean Plain.
5. One Fraunces optical-size cut chosen.
6. An ADR that supersedes ADR-055's exclusion; PR #70 / ADR-115 resolved; the typebar freeze conflict resolved.
7. A hash check before sharing files between chrome and documents, and a measured APK delta.
8. A paper test at 10 and 12 pt, A4 and Letter, inkjet and laser.

**Open decision — two typefaces or three?** This is the owner's decision. What to decide, in order:
1. **Scope:** does §III govern **text inside a maker's zine**, or only Zinely's own interface? The clause cuts both ways: it assigns Averia to "the maker's own short strings" and Fraunces to "zine body", yet closes on "No fourth **UI** typeface" (quoted in Current reality).
   - Only the interface → the constraint disappears; confirm it as an amendment-log clarification.
   - Zine text → pick one of (a), (b) or (c):
     - **(a) Hand for short text only.** Define the unit (characters, lines or box size) and what happens when the maker types past it: block, warn or keep. Silently switching the face is not allowed.
     - **(b) Amend §III.**
     - **(c) Two voices.** Also decide *which two*: Book + Plain (Brief 02) or Hand + Plain (ZINE-DIRECTION `:974`).
2. **Coverage policy:** is a Hand voice that can't set most European Latin acceptable for the launch languages? If not, the options are:
   - Book + Plain (both cover Latin Ext-A); or
   - a **different imperfect face** that covers Latin Ext-A, which would also touch the chrome's Averia under §III.
3. **Older builds.** Choose which failure a beta.5-or-older reader meets. They only get a newer zine by restoring a library backup, or by downgrading:
   - **No bump:** the backup restores and every zine opens, but text set in a narrower voice is rendered in Inter, and its last lines are clipped.
   - **Ride the v4 bump:** the **whole backup** refuses to restore on the older build as soon as it contains one v4 zine (`ZineLibraryBackupStager.kt:367-376`). That happens with any bump, not just D2's.
   - 🟦 Ride the bump; refusal is honest where clipping is silent. Either way, the release notes must say so.
4. **O8:** confirm the font set.

Evidence to gather before ruling:
- text lengths in real or tester zines;
- one printed page of Averia at 10–14 pt;
- the launch-language list.

## 7. D3 audit — printer test page

**Current reality**
- **Paper and page size.**
  - A4 is 595.276 × 841.890 pt and Letter 612 × 792, always laid out landscape (`ModelEnums.kt:13-24`).
  - `SheetComposer` rounds the page to whole points (`:62-64`), which clips about 0.1 mm. Harmless.
- **Layout.**
  - The 4×2 panel grid runs edge to edge, with the top row rotated 180° (`SingleSheet8Imposer.kt:33-53`, `Convention.kt:35-53`).
  - Every page's **bottom** edge sits on the sheet edge. Pages 1, 2, 5 and 6 also have one side on the sheet edge.
- **Guides.** Fold and cut guides are printed edge to edge (`SheetGuides.kt`). There is no ruler.
- **The 17 pt inset is a warning only.** Nothing outside `core:imposition` reads `safeLocalBounds`.
  - Bench applies it to all four edges of every page (`BenchStudioSurface.kt:305-310,439-450`).
  - Proof's "can't reach" band is a decorative 9 dp (`ProofSheet.kt:205-208`).
- **Current print copy:** "100% · Actual size — not Fit to page", "Landscape", "Single-sided" (`Copy.kt:1289-1297`), plus "Print one test sheet first and fold it… Printers flip pages differently" (`:1355-1358`).
- **Export and printing.** Export goes through Save/Share; ADR-052 means no in-app printing. A test PDF would inherit beta.5's Android 7–9 permission path.

**What the plan says:**
- A separate test PDF with a 50 mm ruler, 3/5/8 mm edge bands and a numbered folding dummy.
- Two questions asked back in the app.
- Part 2 (O13): a per-device reach profile that widens the warning.

**Missing or wrong**
1. **A shrunk print shows every band.** At 97 % the 3 mm band lands about 7.4 mm from the edge, so the band answer is only valid when the ruler answer is "yes".
2. **The bands straddle the threshold.** A printer reaching 5.5 mm (fine) and one reaching 7.9 mm (not fine) both answer "8". The test needs one line **at the 17 pt inset** and a yes/no question.
3. **A 50 mm ruler barely detects a 2–4 % shrink** (a 1–2 mm change).
4. **The "Actual size" fix may not exist on Android.** The system print dialog has no scaling control (ADR-052), and user reports say actual size is unreachable through some print services ([HP community](https://h30434.www3.hp.com/t5/Mobile-Printing-Cloud-Printing/Actual-size/td-p/9512233), search snippet only). ⚠️ It must be measured before the app promises it.
5. **17 pt is below the documented worst case on Letter** (Canon 6.4 mm, [rasterbator table](https://rasterbator.io/guides/printer-minimum-margins-borderless-printing)). Those long edges are every page's bottom.
6. 🟨 **"Printers flip pages differently"** may read as duplex advice on a single-sided sheet (interpretation, §3).
7. **The brief's verification section points at a print procedure that does not exist.**

**How a first print actually fails** (folds follow the paper, not the ink):
- **Fit-to-printable-area (95–98 %):** the quarter folds miss the printed lines by 1.6–3.3 mm, and uneven white borders appear. Nothing is lost.
- **Portrait fit (~71 %):** the zine is ruined.
- **An A4 PDF on Letter paper at 100 %:** 8.8 mm is clipped on each short edge, so outer-page content is lost.
- **A Letter PDF on A4 paper:** 3 mm is clipped top and bottom, plus the printer's own margin.

**Evidence:** as cited, plus [Minibook's instructions](https://make-a-zine.github.io/) and [Folio's calibration sheet](https://folioimposition.com/blog/print-zine-at-home). Folio also writes `/PrintScaling /None`; PDF.js ignores it ([bug 1243580](https://bugzilla.mozilla.org/show_bug.cgi?id=1243580)), and whether Android viewers honour it is 🟨 unknown.

**🟦 Recommendation — build in stages and stop at the first that is enough**

The brief asked which kind it should be. Stage 1 is **informational, inside Proof's Print step**. Stage 2 is **diagnostic but static**: a page the maker prints and reads, launched from Proof, with nothing answered back to the app. Neither stage is interactive.

1. **Stage 1: copy only, with a small `v21-proof.html` amendment.** Turn the existing "test sheet" advice into a real check that uses what already prints on every zine:
   - *"Fold it. The grey fold lines should sit in your creases — if they sit inside them, your print app shrank the page."*
   - Reword "Printers flip pages differently" around orientation only. "Single-sided — one side only" already ships (`Copy.kt:1297`), so no new line is needed.
2. **Stage 2, only if the owner still wants a test page:** a **static, self-explaining PDF**.
   - The full-width fold grid works as the long ruler (148.5 mm A4 / 139.7 mm Letter), with 50 mm as a secondary check.
   - One labelled line at the 17 pt inset on each edge.
   - A numbered 1–8 folding dummy and the paper size in words.
   - One optional line in Proof's Print step, delivered through the existing Save/Share.
   - **No questions back, no stored printer state.** This answers Proof's question ("How do I print it correctly?") and respects ADR-052.
3. **O13 (per-device reach profile): 🟦 no.** If the print pass shows real reach worse than 6 mm, raise `DEFAULT_SAFE_AREA_INSET_PT` for everyone (to 18–20 pt). It is one constant and a warning only, so nothing moves in existing zines. But it is **its own session**, not part of stage 1: it moves the frozen `v21-bench.html` keep-clear band (18.5 px), so it needs that HTML amendment, an ADR amending ADR-012 and the keep-clear rulings, and changes to its test and the goldens.

**Implementation prerequisites**
1. The **physical print pass first** (protocol below). It decides whether "Actual size" advice is honest on Android and whether 17 pt is enough.
2. For stage 2 only (a test page): an ADR amending ADR-039, which deferred the on-sheet ruler. Stage 1 is copy inside Proof and needs no ADR. An inset change amends ADR-012 instead (item 3 above).
3. The frozen `v21-proof.html` amendment.
4. A real "Print pass" section in DEVICE-VERIFICATION.
5. Optionally, remove the duplicate `ExportScale.PaperSize`.

**Physical print protocol (minimum)**
- **Printers:** at least HP inkjet, Brother laser and Canon PIXMA (Letter is Canon's worst case). A4, plus Letter where obtainable.
- **Print paths:**
  - (a) Files/Drive → system print dialog → the default service, settings untouched;
  - (b) the same path with every scaling option the UI offers;
  - (c) the vendor's own app.
- **Record:** phone, Android version, viewer, print service and version, printer, paper, the settings shown, and a photo of the sheet.
- **Measure:** outer fold-grid width (scale = measured ÷ expected), the gap between each quarter-fold line and its crease, whether a 17 pt mark survives on all four edges, and fold order.
- **Pass:** scale 99–101 %, all four marks visible, correct order.
- **Report separately** any print path with no reachable way to 100 %.

**Open decisions**
- Is stage 1 enough, or is a test page wanted?
- O13: a reach profile, or one global margin?
- May the app promise "Actual size" on Android before the print-path matrix proves the setting exists?

## 8. D4 audit — TalkBack reading order + alt text

**Current reality**
- **Traversal order.**
  - `ElementSemanticsLayer` walks `page.elements` in list order with `traversalIndex = index` (`:86-87,132`). Paint order sorts by `zIndex` (`SceneRenderer.kt:45`).
  - Restacking changes `zIndex` only, and spreads diverge too (§3). ✅ Defect confirmed.
  - No test asserts element traversal order. The only order test covers `ZinelyV2CanvasSemantics`, which no production code uses.
- **What TalkBack says.**
  - A photo is "Photo", so all photos sound the same.
  - Art is its supply name (a Button; it cannot be marked decorative).
  - Text is "Text: <full text>", not shortened.
  - Every element node has 13–15 custom actions.
- **Read mode speaks nothing from the page** (§3).
- **Focus.** About a dozen `.requestFocus()` matches remain across 7 files (some may be comments; count before touching). They move *input* focus, not TalkBack's: Samsung proved it in beta.5, and [Google's tracker](https://issuetracker.google.com/issues/231606408) agrees.
  - `paneTitle` is used on ZSheet, FlipTray, the Colophon screens and ZineActionSheet.
  - The `ZSheet` scrim fix is in (`ZSheet.kt:177-184`). `ZineActionScrim` (`ZineActionSheet.kt:266-278`) still needs the same one-modifier fix.
- **Announcements.** They go out through the deprecated `announceForAccessibility` (§3). "Changed page N" is the only reducer announcement.
- **Tagged PDF is impossible.** `PdfDocument` has no tags, alt text or metadata API ([reference](https://developer.android.com/reference/android/graphics/pdf/PdfDocument)).

**What the plan says:** [Brief 04](BRIEF-04-READING-ORDER-AND-ALT-TEXT.md) A1 spatial order, A2 relative phrases, A3 named undo (wave 1), and B maker-written alt text (wave 2, with the v4 bump).

**Missing or wrong**
1. **The row rule collapses** when one element spans most of the page: a full-page photo or a spread half opens a row [0, H] that swallows everything. Guard: an element taller than about 60 % of the page is read first and never opens a row. 🟦 The exact threshold is decided in the spec.
2. **The `uiautomator` dump cannot show traversal order** (§3).
   - The Robolectric harness saw every hint as `UNDEFINED` because Compose computes `traversalBefore` only when accessibility is enabled ([source](https://raw.githubusercontent.com/androidx/androidx/androidx-main/compose/ui/ui/src/androidMain/kotlin/androidx/compose/ui/platform/AndroidComposeViewAccessibilityDelegateCompat.android.kt)).
   - `SurfaceTraversalOrderTest.kt:92-96`'s conclusion that "the platform sets no hints" is a test-setup artefact.
3. **Named undo would speak twice** (F2).
4. **Read semantics** need their own design, which contradicts the brief's out-of-scope "Describe this page". The spec and the build already disagree: the frozen `v21-proof.html:634` marks `#book` `aria-live="polite"`, while Compose clears the page's semantics. The design starts from that gap.
5. **`explicitNulls = false`** is needed for "empty stored as absent".
6. The research claim checks out ([CHI 2021 PDF](https://faculty.washington.edu/wobbrock/pubs/chi-21.03.pdf): all screen readers observed read in z-order, and relative position was the main failure). Scope: 15 participants on desktop, no TalkBack.
7. **A2 (relative phrases) was not audited.** Its wording, length and interaction with custom actions have no evidence here.

**Evidence:** `ElementSemanticsLayer.kt:86-87,132`, `SceneRenderer.kt:45`, `Command.kt:123`, `EditorReducer.kt:237-259`, `ProofRead.kt:554,629,674`, `ZSheet.kt:177-184`, `ZineActionSheet.kt:266-278`, `SurfaceTraversalOrderTest.kt:92-96`, plus the external sources linked above.

**🟦 Recommendation**
- **A1 first.**
  - A pure `SpatialOrder` in `core:editor`, with the large-element guard, id tie-break and jqwik tests (jqwik 1.9.2 is already in the catalog).
  - Then **declare the child nodes in that order *and* set `traversalIndex`**, so child order equals traversal order and every tool shows the truth.
  - Bundle the `ZineActionScrim` fix.
- **A2** after its own short wording spec and a TalkBack listen of candidate phrasing.
- **A3** follows F2.
- **B** waits for F1 and for a Read-semantics design.
- **No automated Compose focus test may be cited as proof of TalkBack behaviour.**

**Implementation prerequisites**
- The large-element rule.
- A Robolectric spike confirming that enabling a shadow `AccessibilityManager` produces `traversalBefore` hints (🟨 assumption).
- For B: the F1 fixtures, and a decision on Read semantics.

**Open decisions (can be decided during implementation)**
- Can Art be marked decorative (skipped in Read and export, still reachable on Bench)?
- Is a described element read as "Photo: <description>" or as the description alone?
- Should the deprecated announce channel be replaced app-wide or only for undo?

**Automatable vs device-only**

| Automatable (how) | Device-only (what, where) |
|---|---|
| Spatial order is a permutation, deterministic, and unchanged by `zIndex` (jqwik, JVM) | Swipe order on Bench after a restack and after a spread: Samsung SM-A176B (TalkBack 16.x) plus a Pixel if available |
| Child declaration order plus `traversalIndex` (Robolectric semantics) | Whether TalkBack honours `traversalBefore` beyond child order |
| `traversalBefore` hints with accessibility enabled (Robolectric; spike first) | Where focus lands when Bench, Read or a sheet opens |
| Labels, relative phrases and description fallback (unit tests plus merged/unmerged semantics) | The actual speech: its length, and undo spoken once, not twice (owner listen pass; adb input bypasses TalkBack on this phone) |
| Custom actions per element | The custom-actions gesture works (Samsung vs Pixel) |
| `ZineActionScrim` has no clickable node (Robolectric in the Dialog window) | TalkBack lands inside the sheet; Back closes it |
| Undo labels and one emitted effect (reducer tests) | A repeated identical undo is re-announced |
| v3→v4 migration, round trip, backup rejects v5 (JVM plus fixtures) | Read speaks descriptions page by page |

## 9. D5 audit — frames

**Current reality**
- **Catalogue.** 32 supplies (`SupplyCatalog.kt:508-544`), grouped 8 / 10 / 9 / 5 (`Copy.kt:512-553`). The search map is `TAGS` (`:560`).
- **Six holed pieces already ship:** `paper.window`, `paper.hole`, `shape.ring`, `fix.grommet`, `mark.registration` and `fix.corner` (§3). Re-scan the catalogue for more before writing hit-test tests.
- **Outlines** live in a unit square and are fill-only with an even-odd rule. Paths are cached, so performance is a non-issue.
- **Scaling is non-uniform and resize is free-aspect** (`TransformMath.kt:77-78`).
  - SUPPLIES-SPEC's uniform-scale lock is **not implemented** (`AffineTransform2D.kt:60-64`).
  - So a window frame on a 4:3 box gets uneven borders, and a ring becomes an ellipse.
- **Hit test.** It is a rectangle test that picks the highest `(zIndex, listIndex)` (`HitTest.kt:21-36`). Consequences:
  - Tapping a frame's centre selects the frame, never the photo under it.
  - Double-tap to Reframe that photo is blocked the same way.
  - The only workarounds are Send backward or TalkBack.
- **Placement.** Every supply lands at page centre; the per-supply override map holds only `shape.rule`, and a test pins that (`SupplyPlacement.kt:117-138`).
- **Unknown `supplyId`** (e.g. a new frame opened in an older build):
  - it is kept on save and drawn as nothing;
  - it is still hit-testable, TalkBack reads it with a fallback label, and nothing "reports" it despite the validator KDoc.
  - The result is an invisible, selectable box.

**What the plan says:** [Brief 05](BRIEF-05-MATERIALS-FRAMES.md) proposes single-outline overlay supplies within the frozen families, with no model change and no freeze amendment.

**Missing or wrong**
- **Freeze amendment is needed** (P5).
- **Tap-through is not addressed.**
- **Stretch policy is not addressed.** A per-supply default aspect breaks the `{shape.rule}`-only pin and its "every extra entry is giving up" rule (`SupplyPlacement.kt:135`).
- **Older builds show an invisible, selectable box,** not just nothing.
- **Frames already exist** (the six holed pieces), so "no frames" is wrong.

**Evidence:** `SupplyCatalog.kt:162-169,241-262,284-287,412-414,490,503-505,508-544`, `HitTest.kt:21-36`, `TransformMath.kt:77-78`, `AffineTransform2D.kt:60-64`, `SupplyPlacement.kt:117-138`, `SupplyPlacementTest.kt:93`, [ADR-107](../DECISIONS.md#adr-107) R1a/R2. `core:render` depends only on `core:model`, so `core:editor → core:render` adds no cycle.

**🟦 Recommendation**
- **Keep frames as overlay decor.** No page-level frame, and no frame property on `ImageElement`. Such a property would be:
  - a schema change;
  - silently dropped on re-save by older builds (`ignoreUnknownKeys`);
  - in conflict with photo flip and Reframe's clip;
  - against ADR-107 R2's "maker arranges".
- **A page border** is just decor the maker sizes inside keep-clear.
- **"Imperfect surface, perfect mechanics" supports the overlay** only if the mechanics are perfect. So **fix hit-testing first, hole-aware and in two passes:**
  1. Pick the topmost element whose *hit area* contains the point. The hit area is the **box** for text and image elements (unchanged, so a tap on empty text or a `Fit.FIT` letterbox still selects them) and the **outline** only for decor: an even-odd point-in-outline test in unit space after undoing rotation, flip and scale.
  2. Fall back to today's rectangle test.
  - This also fixes the six shipping holed supplies.
- **Ratify the principle.** It exists only as research proposal D13; treat it as an owner decision.

**Implementation prerequisites**
- `core:editor` cannot see `SupplyCatalog`. Either add `api(:core:render)` (no cycle) or inject a lookup function. Put the point-in-outline test in `core:render` as a pure function with JVM tests.
- An owner ruling on stretch: free stretch, or per-supply uniform scale.
- The `v21-bench.html` set amendment.
- O9 and outline authorship.
- A release note that zines using new frames need the new version.

**Open decisions**
- O9: which frames ship.
- Who authors the outlines.
- The stretch policy.
- Whether hole-aware hit-testing, a behaviour change for the six shipping pieces, needs a Bench spec note.
- Ratifying D13.

## 10. Architecture / iOS implications

**Safe to share later**
- **Shareable after small fixes:** `core:model` (replace `TextCoverage`'s two JVM calls). **Shareable as is:** `core:copy`.
- **`core:editor`:** the reducer, `HitTest`, `TransformMath`, `Snap` and `ZOrder`.
- **`core:render`:** the `DrawCommand` tape, `SupplyCatalog` and `Photocopier` (IntArray only).
- **`core:imposition`:** shareable once `BigDecimal` is replaced by manual rounding.
- **`core:data`:** shareable apart from SHA-256 (needs a replacement) and `Math.addExact` (`ZineLibraryBackupValidator.kt:226`).
- **Pure files stranded in Android modules:**
  - verified pure: `ExportScale`, `SelectionChromeGeometry`, `DocumentFontRegistry`;
  - pure except one `String(bytes, charset)` → `decodeToString`: `CmapCoverage`;
  - also movable (not named in the plan): `EditorStore.kt`, `SupplyPlacement.kt`, `FramingDraft.kt`, `EditorEffects.kt`, `BenchState.kt`. Moving `EditorStore`/`EditorEffects` into `core:editor` adds a kotlinx.coroutines dependency there, and `SupplyPlacement`/`EditorEffects` would add `core:copy`. Decide those dependencies before moving the files.

**Android-specific by necessity**
- **Rendering and text:** `CanvasReplayer` / `SupplyOutline.toPath`; `SharedTextLayout` (StaticLayout).
- **PDF:** `SheetComposer`/`PdfPageRenderer` (`PdfDocument`); `PdfRasterizer`.
- **Images:** `ImageBlitter` and `ImportMasterDecoder` (BitmapFactory, EXIF).
- **Export:** `DownloadsWriter` (MediaStore) and `ZineExporter` (FileProvider).
- **Storage:** `AndroidFileSystemOps` (`Os.fsync`); Room (4 files, all in `data-android`, entity never leaks); DataStore.
- **App shell:** Hilt; the ViewModels; `ZinelyNavHost` (navigation-compose, picker permissions); emoji2; all Compose UI.

**Potentially difficult to port**
- **Text layout determinism.** No line breaks and no layout-engine version are stored, so CoreText will reflow differently.
- **`data-storage`:** atomic rename plus directory fsync, zip, `AtomicLong`.
- **SHA-256.**
- **HEIC/EXIF import.**
- **Vector parity of the PDF:** even-odd fill, no MaskFilter/PathEffect.

**Things to avoid coupling further — coupling rules (give these to every implementation session)**
1. No `java.*`, `javax.*` or `android.*` in `core:model`, `copy`, `editor`, `render` or `imposition`. No `Character.`, `codePointAt`, `.format(`, `Math.`, `System.`, `Charsets`, `java.time` or `UUID` there either.
2. `core:render` emits pure `DrawCommand`s only. Never add a command a backend can't draw as vectors.
3. Room, DataStore and SAF types stay inside `data-android`. Core sees repositories only.
4. New pure logic goes in `core:*`, not in `feature:editor` or `render-android`. Move the stranded pure files when you next touch them.
5. Hilt annotations stay in `app` and `data-android`. Core classes keep plain constructors.
6. Core never decides anything from measured text layout. Add a layout-engine marker in the next schema bump.
7. File I/O stays behind `FileSystemOps` and `AssetStore`; hashing behind a `ContentHasher` seam.
8. No `@Synchronized`, `@Volatile` or `java.util.concurrent` in core. Use a coroutine `Mutex`.

**🟦 Recommendation:** no KMP now. The plan's §7 counts are correct (its `BigDecimal`/`ContentHasher` citations point at usage lines, not imports). The cheapest insurance is rules 1–8 plus F6.

## 11. Owner decisions

Each row: why it exists → is it still open → does it block implementation or architecture → what evidence you need. The owner-facing form of this register is the [decision gate](ZINELY-1X-DECISION-GATE.md). Every O-item below is still open as of 2026-09-25, except O7: the ROADMAP records the owner's approval of PR #70's removal as a design judgment, leaving acceptance work (hands-on TalkBack, rendered HTML parity), not a decision. None is recorded as ruled in DECISIONS.

### Calendar-bound now (not implementation-blocking)

| # | Why it exists | Still open? | Blocks | Deadline / evidence |
|---|---|---|---|---|
| **O15** Android developer verification | Side-loaded APKs will need a verified developer | Yes | **Every public APK after the cut-over** — distribution, not code | Starts **30 Sep 2026** in four countries, global in 2027 ([research summary item 4](../research/ZINELY-FUTURE-PRODUCT-RESEARCH.md), detail in its [§23](../research/ZINELY-FUTURE-PRODUCT-RESEARCH.md#23-website-strategy); primary source [developer.android.com/developer-verification](https://developer.android.com/developer-verification)). ⚠️ Re-check Google's current timeline before acting |

### Must decide before implementation (of the direction it gates)

| # | Why it exists | Still open? | Blocks | Evidence you need |
|---|---|---|---|---|
| **O12** (reframed) | §III assigns faces to "the maker's own short strings" and "zine body" but closes on "No fourth UI typeface"; its running-text rule vs a Hand voice in zines | Yes | D2 implementation **and** its schema plan | Whether §III covers zine text; zine text lengths; an Averia print at 10–14 pt; launch languages (coverage, §6); the older-build trade-off (§6 Q3) |
| **O8** | PRD §13 Q3 never closed | Yes | D2 | Folds into O12 — the answer to "two or three, which ones" *is* O8 |
| **O7** | PR #70 removes the dead Font control; ADR-115 only on that branch | **No — approved** (ROADMAP In development); acceptance work remains (hands-on TalkBack, rendered HTML parity); draft since 2026-09-11 | D2 sequencing; also the **live** Bench still shows a dead control | The acceptance evidence, then a rebase (the branch conflicts with `main`) |
| **O10** | `v21-typebar.html` (and `v21-reframe.html`, per plan §9) "PROPOSAL, NOT FROZEN" headers vs ZINE-DIRECTION N2 | Yes | D2's HTML-amendment route | Your intent when N2 was written |
| **O14** | ROADMAP says the creative slice (fonts, Art pack, decorative frames) does not reorder the fold-study gate without owner approval | Yes | D2 **and D5** timing | Run the study (§4) — then O14 disappears |
| **O9** + stretch policy | ADR-107 R1 leaves ~19 backlog supplies; frames need outlines and a scaling rule | Yes | D5 | Which frames; who draws them; whether frames keep aspect |

### Can decide during implementation

| # | Why | Still open? | Blocks | Evidence |
|---|---|---|---|---|
| **O1** phone transfer | ADR-030 §7 excludes all D2D | Yes | only D1 part 3 | The D1 deep audit ([§5](#5-d1-audit--last-backed-up--changing-phones), item 4) is done; still needs a verified cross-device restore |
| **O13** reach profile | Brief 03 part 2 | Yes | only D3 part 2 | The physical print pass. 🟦 Recommendation: no — raise the global inset instead if needed (its own session, §7) |
| D3 stage-2 test page; may the app promise "Actual size"? | stage 1 may be enough; the setting may not exist on Android | New (audit) | D3 stage 2 / D3 copy | Print pass results |
| D4 sub-decisions | decorative Art; "Photo: <desc>" vs description only; announce channel app-wide | New (audit) | D4 details | A TalkBack listen of both wordings |
| F2 "duplicate" / "Reset framing" wording, Redo snack | the only labels a command can't derive | New (audit) | A3 details | None — a copy call |
| D5 stretch policy; D13 "imperfect surface, perfect mechanics" | frames need a scaling rule; D13 proposed in research, never ratified | New (audit) | D5 (stretch must be ruled before D5 starts, with O9) | None |

### Can defer (but some carry a clock)

| # | Why | Still open? | Blocks | When it stops being deferrable |
|---|---|---|---|---|
| **O6** Play signing key | Play App Signing enrolment is irreversible | Yes | Play publication | Before the first Play upload |
| **O5** paid-pack clock | Tribunal: decide within a year of the first bundled-supplies release | Yes | nothing in 1.x | ~Aug–Sep 2027 |
| **O2** starters | Tribunal KEEP vs DO NOT BUILD | Yes | D8 only | Never forced |
| **O3** ink lift | EXPERIMENTAL | Yes | D6-(4) only | After an asset-model ADR |
| **O4** tray / X2 | D-029 Q1–Q3 | Yes | Clippings tray only | Never forced |
| **O11** page count (D-030) | fixed 8 vs variable | Yes | 16-page only (not reorder) | Never forced in 1.x |

## 12. Recommended implementation sequence

**The sequence lives in [plan §5](ZINELY-1X-IMPLEMENTATION-PLAN.md#5-sequencing).** It was revised after the
D1 deep audit, and every step there says why it is placed where it is. The audit evidence that drove it:

- Fixtures first ([§4 F3](#4-wave-0-audit), [§5](#5-d1-audit--last-backed-up--changing-phones)): a symmetric
  writer and stager change would pass every test, while breaking backup files already in makers' hands.
- D1 part 1 early ([§5](#5-d1-audit--last-backed-up--changing-phones)): the backup sheet misleads today.
  Its title claims safety, backup failures use restore wording, and a failed backup may point at a zine the shelf doesn't show.
- The hole-aware hit test before any frame ([§9](#9-d5-audit--frames)): six shipping pieces are affected
  today.
- The v4 bump alone, after a release ([§4 F1](#4-wave-0-audit)): it makes whole backups unrestorable on older
  builds.
- D2 last among the creative work ([§6](#6-d2-audit--typefaces--voices)): four owner questions and two
  engineering prerequisites.

## 13. Questions requiring owner input

**These live in the [decision gate](ZINELY-1X-DECISION-GATE.md)**: eight questions (O7 moved out as already approved; tap-through scope and release shape added), each with what it gates,
the evidence so far, what is unknown, a recommended framing, and whether it must be decided now. None of them
blocks plan steps 0–3.

## 14. Risks

| Risk | Where | Mitigation |
|---|---|---|
| Layout loss in older builds (voices, clipped text) after a backup restore | D2 | Owner trade-off (§6 item 3, now decision-gate Q2.4): accept it, or ride a bump (v4 or v5, by the release boundary) |
| Whole backup unrestorable on an older build once it holds one v4 zine | F1 (any bump) | Release after wave 1; state it in release notes; D1 copy must not promise otherwise |
| Silent device-font fallback for uncovered glyphs | D2 | Family-aware coverage; 🟦 disable a voice for text it can't set (D2 spec decides) |
| WYSIWYG break while editing | D2 | Editing surface uses document fonts |
| Frames make photos untappable | D5 (and today, six holed supplies) | Hole-aware hit test first |
| A permission test that silently stops checking | F4 | SDK-28 pin; never delete the `WRITE_EXTERNAL_STORAGE` assertion |
| Invisible selectable ghosts on older builds | D5 | Release note; consider a visible placeholder later |
| "Actual size" advice users can't follow | D3 | Measure print paths before promising |
| Claiming TalkBack behaviour from Compose tests | D4 | Device listen pass is the acceptance evidence |
| Double-spoken announcements | D4/F2 | One channel (the live region) |
| A backup format change passing its own round-trip tests | F3 | Checked-in fixture archive |
| Unbumped schema field silently dropped | F1 | Shape guard now |
| Publishing personal data in fixtures or screenshots | F3, goldens | Fictional content only |
| Developer-verification cut-over interrupting GitHub distribution | O15 | Owner action, not code |

## 15. What NOT to change yet

- **Text engine.** StaticLayout, line breaking and the overflow clip.
- **Imposition geometry and the 17 pt inset**, until the print pass measures reach; then only in their own session with an HTML amendment and an ADR.
- **The PDF pipeline** (`SheetComposer`, one replayer serving four surfaces). No new export abstraction.
- **`History` / `committing()`.** Named undo needs neither.
- **The document model outside one planned v4 bump.** No page ids, no frame property on images, no heading/body role.
- **The backup archive format and `data_extraction_rules.xml`.** Nothing changes until O1, and a skip-and-list backup only through an ADR amending ADR-110 (the D1 audit in §5 is done).
- **Programmatic TalkBack focus.** Proven not to work on Samsung. Don't retry without a new mechanism and a device.
- **KMP / module moves**, beyond moving a stranded pure file when you already touch it.
- **Frozen HTML specs.** Amend first, in their own reviewed change.
- **ADRs.** New ADRs only when a direction starts. Two follow-ups sit outside every direction: ADR-116 (About maker's note) still reads "native verification pending" until its pixel-parity check runs, and ADR-115 lands only if PR #70 merges (O7).
- **Starters, ink lift, tray, 16 pages, transparency, iOS.**

---

## Appendix — what a fresh implementation session needs

Moved to [IMPLEMENTATION-HANDOFF-TEMPLATE.md](IMPLEMENTATION-HANDOFF-TEMPLATE.md), which is the single place that
lists it. Fill it in per session from the direction's brief and the recorded owner rulings.

---

## Review log

Two independent Review Agents reviewed this audit on 2026-09-25: one for code evidence and one for product, process and governance. Both returned **GO WITH FIXES**. Every finding was reconciled as below; ACCEPT means the change is in the text above.

| Finding | Class | Reconciliation |
|---|---|---|
| "This audit wins" made a second source of truth | Required | **PARTIAL.** Header rewritten: this is a review record; the plan and briefs stay authoritative and carry banners; folding corrections is step 0 of each direction. The file stays at the path the owner asked for, rather than moving to `docs/reviews/`. *(Superseded by the second pass below: Briefs 01, 04 and 05 were rewritten on 2026-09-25; Briefs 02 and 03 still carry their banners.)* |
| §III misquoted (only the running-text rule was quoted) | Required | ACCEPT — both sides quoted (`V2-CONSTITUTION.md:146-153`) |
| v4 bump refuses the whole backup, not one zine | Required | ACCEPT — verified at `ZineLibraryBackupStager.kt:367-376`; §2, §4 F1, §5, §6 Q3, §12, §13, §14 updated |
| Fold-study gate missing from S5/S6; O14 also gates D5 | Required | ACCEPT |
| O15 misfiled as deferrable; citation wrong | Required | ACCEPT — new calendar-bound group; primary source cited |
| `PrivacyManifestTest` fails at SDK ≥ 29 (maxSdk-28 permission dropped) | Required | ACCEPT — SDK-28 pin, or parse the XML |
| Six holed supplies, not three (the second review said five; the Brief 05 rewrite found `fix.corner`) | Required | ACCEPT — verified `SupplyCatalog.kt:162-169,412-414` |
| Missing Evidence headings (D1, D4, D5, F1) | Recommended | ACCEPT |
| D3 kind (informational/diagnostic/interactive) not answered | Recommended | ACCEPT |
| D2 overreach ("must be disabled"); missing "other imperfect face" option | Recommended | ACCEPT |
| F4 allow-list dropped against the privacy invariant | Recommended | ACCEPT — kept |
| §12 dependency errors; no release boundary; A2 unaudited | Recommended | ACCEPT |
| Raising the inset understated | Recommended | ACCEPT — its own session |
| Q8 is process, not an owner question | Recommended | ACCEPT |
| §5 facts not re-cited on `5f7707a` | Recommended | ACCEPT — marked ⚠️ |
| O10 omitted `v21-reframe.html` | Recommended | ACCEPT |
| Unsourced numbers (widths, x-heights, glyph count, Instagram, Nielsen) | Recommended | ACCEPT — labelled 🟨/⚠️ or sourced |
| Appendix gaps | Recommended | ACCEPT |
| P1 wording; 23 call sites; Reset framing ambiguity | Recommended | ACCEPT |
| F6 regex would false-positive; `Math.addExact` in core:data | Recommended | ACCEPT |
| D3 "add one side only" already ships | Recommended | ACCEPT |
| "Printers flip" duplex reading is an interpretation | Recommended | ACCEPT — labelled 🟨 |
| Merged-manifest claim has no artefact; tests see the debug manifest | Recommended | ACCEPT — marked ⚠️ |
| Hit area undefined for text/image | Recommended | ACCEPT |
| P4 clip citation; exposure path; `explicitNulls` experimental; requestFocus count; stranded-file dependencies; §10 wording; ADR-012 "~6 mm"; P6 → ADR-039; ADR-116 unexplained; missing "Still open?" column | Observation | ACCEPT — all folded in |
| §10 heading renamed from "things to avoid coupling further" | Observation | ACCEPT — heading restored |

### Second pass (2026-09-25, after the D1 deep audit and the brief rewrites)

**What changed since the first pass:**
- The D1 deep audit ([§5](#5-d1-audit--last-backed-up--changing-phones)) was completed.
- Briefs 01, 04 and 05 were rewritten, with the authority header.
- The Brief 05 rewrite found a **sixth** holed supply, `fix.corner` (`SupplyCatalog.kt:284-287`).
- The Brief 04 rewrite corrected F2: the label function is `editLabel(doc)`. It also found that the frozen "Put back" toast was never built, and that Read mode is silent against the frozen `aria-live`.
- The decision gate and the handoff template were created.

**Briefs 02 and 03 were not rewritten.** The session's permission classifier denied both rewrite agents, so both briefs still carry their "Corrections pending" banners. They stay **not ready** until the owner decides how they are rewritten; the template makes a session stop on that banner.

Two further independent reviews were run against the whole planning set. **Review A** used a technical lens (code, ADRs, feasibility); **Review B** used a product lens (value, UX, accessibility, identity, coherence). Both returned **GO WITH FIXES**.

| Finding | Review · class | Reconciliation |
|---|---|---|
| v4 contents contradict each other (`look` in, `description` out, migrator identity vs real); no release boundary, so a later D2 can't "ride" a released v4 | A · Required | **ACCEPT.** v4 is exactly `description` + layout-engine marker, with an identity migrator. `look` takes its own later bump. v4 is held unreleased until alt text lands; after that, D2 takes v5 (plan §4 F1b, §5 steps 6–8, §6; Brief 04; gate Q2) |
| Steps 0, 4 and 6 can't be opened with the template (Brief 05 blocked as a whole; no foundation brief) | A · Required | **ACCEPT.** Brief 05 readiness is split by part; the template names plan §4 as the spec for foundation steps |
| Hit-test scope (six vs all 32) and decider inconsistent; not in the gate | A · Required; B · Required | **ACCEPT.** It is an interaction change: the owner's new gate Q3 chooses holed-only, all Art, or none, and a `v21-bench.html` note comes first (plan step 4, Brief 05) |
| Touch tolerance swallows taps in small holes (eyelet ≈12 pt, registration ≈6.5 pt) | A · Required | **ACCEPT.** An exact hit on a lower element beats a near-hit on decor above; a test is added (Brief 05) |
| An unreadable zine has no shelf row, so `canBackup` can't count it; `SchemaTooNew` gets restore wording in backup mode | A · Required | **ACCEPT.** Verified at `RoomProjectRepository.kt:419,604-605,674-676` and `HomeViewModel.kt:474`. The `canBackup` fix and state 5 are dropped; the failure says the zine isn't on the shelf; newer-version wording is added; skip-and-list goes to gate Q8(c) as an ADR-110 change (Brief 01, §5, plan) |
| Stale cross-refs (audit §12 step numbers, review-log row, "(blocked) D1 audit", §15) | A · Required; B · Recommended | **ACCEPT** — all fixed |
| Q3 re-asked PR #70, which the ROADMAP already records as approved | B · Required | **ACCEPT.** O7 moved to "already recorded"; §11 corrected |
| O1 option B said "add in-app reminder" | B · Required | **ACCEPT** — removed (Article 4) |
| Q2's no-bump option silently overrides ROADMAP "must not silently become layout loss"; "interface only" is a §VI amendment | B · Required | **ACCEPT** (gate Q2) |
| O6 wrongly deferred: Play is ROADMAP *Planned* and O6 is irreversible | B · Required | **ACCEPT.** The reason is corrected and cross-linked with Q1 |
| Gate missed: the release-wave concept (ROADMAP schedules no build), the restore-honesty defects | B · Required | **ACCEPT.** New gate Q8, and plan step 1b |
| Release should not wait for the print study | A · Recommended; B · Recommended | **ACCEPT.** Step 5 joins wave 1 only if ready |
| Fold-study fixes need a step; the print study needs makers; the inset needs a slot; fold study → D5 edge | B · Recommended | **ACCEPT** (plan §5) |
| Brief 01: softer value claim, "somewhere other than this phone", file name, year, way forward, partial-file cleanup in part 1, owner approval of copy, Article 4 | B · Recommended | **ACCEPT.** Way forward: update for newer zines, skip-and-list via Q8 for damaged ones |
| Brief 04: physical undo copy (§II.4), wider Pass 2 remedies, 200 % undo snack, height-only guard noted, multi-column overpromise, Switch Access + keyboard, sighted listen = proxy | B · Recommended | **ACCEPT** |
| Brief 04: rule stated in both KDoc and §4.5 | A · Recommended | **ACCEPT** — §4.5 is the authority; the KDoc links to it |
| Plan overstated alt-text value | B · Recommended | **ACCEPT** — Read on the maker's phone only; PDF gains nothing |
| Gate: the "one sitting" claim; languages framed on principle; the shipped "100% · Actual size" copy | B · Recommended | **ACCEPT** |
| Gate Q6: favour stretch-tolerant frames; instant-photo and deckled examples fail the house test | B · Recommended | **ACCEPT** (gate Q6, Brief 05) |
| D3 ADR target inconsistent (ADR-039 vs ADR-012) | A · Recommended | **ACCEPT.** A test page amends ADR-039 (the ruler deferral); an inset change amends ADR-012; stage 1 needs none |
| ADR per step unnamed | A · Recommended | **PARTIAL.** The template's ADR field now requires "none, because…" when there is none; the briefs keep naming ADRs where one is needed |
| Store interface location | A · Recommended | **ACCEPT** — `data-android/prefs`, beside the existing stores |
| F3 fixture production unspecified | A · Recommended | **ACCEPT** (plan F3) |
| Template "every gate" | A · Recommended; B · Observation | **ACCEPT** — "every owner gate" |
| Line drift (`Copy.kt:1495`, `build.gradle.kts:10`), "seven more" → six, ROADMAP anchor, `EditLabel` arity | A · Observation | **ACCEPT** |
| D2 "not a font picker" vs ROADMAP "real picker" | B · Observation | **ACCEPT** — noted in gate Q2 for the owner |
| "Put back" fires on every prototype undo, not only deletes | A · Observation | **NOTED.** Harmless; the A3 amendment replaces it anyway |
| F4 SDK-28 reasoning not run | A · Observation | **NOTED.** The F4 session's own test proves it |
| **Re-check by Review A** (after the fixes above): RF3, RF4 and RF6 resolved. RF1 still had `look` in v4 in four places. RF2: Brief 05's stop conditions were not scoped to D5. RF5 overcorrected: a zine damaged *after* it was indexed keeps its row (`RoomProjectRepository.kt:300,687`). It also found three new contradictions: steps 0–2 vs 0–3, the wave-1 step range, and partial-file cleanup scheduled in two places | A · Required | **ACCEPT, all fixed.** `look` is removed from v4 everywhere. The stop conditions are scoped to D5. The copy and text now cover both the visible and the invisible case. Steps 0–3 are unblocked, with the owner still approving the HTML amendments for steps 1 and 3. The release is steps 0–4, plus 5 if ready. Cleanup stays in D1 part 1, and Q8 lists it only for completeness. While v4 is held, no release is cut between steps 6 and 7 |
| **Final consistency review** (a separate Review Agent, after the owner decision brief): 4 Required — the template put F6 in step 0; the §4.5 clause needs an ADR under the design system's amendment rule; step 1b had no spec and would amend frozen restore copy separately; audit O7 row still read as open. Plus 11 Recommended and 4 Observations (known-limits misquoted; late Cancel could delete a complete backup; poisoned-photo defect dropped; F3 archive overstated; 1b missing from the release; v4 hold vs D2; step-4 module ADR; checklist count; stale audit wording; "Q3" collision; PR #70 needs a rebase; gate count; "additive"; Brief 01 change log; brief-vs-gate ownership) | Final · Required/Recommended/Observation | **ACCEPT, all 19.** Evidence re-checked: `ZINELY-DESIGN-SYSTEM.md:101-102`, `releases/0.9.0-beta.5.md:45-52`, the checklist recount (70 → 81 at `5f7707a`), the PR #70 branch diff. Step 1b is marked not ready until it has a spec, and its states join the 1a amendment. The gate owns the question text; the brief mirrors it |
