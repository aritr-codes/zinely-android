# Beta Release Candidate — `0.9.0-beta.1`

Cut 2026-07-22 from `main`. Companion to [the readiness review](2026-07-22-beta-release-readiness.md),
which asked whether we *could* ship; this one records what was actually produced and whether it
*should* go out. Categories are the four from [CLAUDE.md](../../CLAUDE.md#release-review-release-agent).

---

## 1. Released artefacts

| | |
|---|---|
| **File** | `dist/zinely-0.9.0-beta.1-release.apk` |
| **Size** | 15,759,764 bytes |
| **SHA-256** | `736a18c9c53b6dfa52a7da7e2d3208e370c6360bd23c475dc8dae5a3c3e3b358` |
| **Tester package** | [docs/releases/0.9.0-beta.1.md](../releases/0.9.0-beta.1.md) — ships beside the APK |

The APK lives in `dist/`, not `build/`, and the checksum above is of *that copy*. This is not
bookkeeping fussiness — see §6 finding F1: the build re-signs, so a checksum taken inside `build/`
can be invalidated by a later no-op build with nothing to indicate it happened.

**The superseded artifact** — SHA-256 `e56446a81364c7131b8ff43269d78e447c6ce4e97739f839622c9fc349db43d2`,
built 2026-07-21, `versionCode 2` — **must not be distributed**. It carries the same version *name*
and predates every fix made in response to the review of it. Its hash is recorded here so the two
files can be told apart after the fact, since their names are identical.

## 2. Release metadata

| | | Verified by |
|---|---|---|
| `versionName` | `0.9.0-beta.1` | `aapt2 dump badging` on the shipped file |
| `versionCode` | **3** | same — not from `output-metadata.json` |
| `applicationId` | `com.aritr.zinely` | same |
| `minSdk` / `targetSdk` | 24 (Android 7.0) / 36 | same |
| **Signer** | `CN=Zinely, O=Zinely, C=IN` · RSA 4096 | `apksigner verify --print-certs` |
| Certificate SHA-256 | `8ade47cf70c18b5e6f4901c89fdab97d40a2d17e4c5acf33e0e1f0b6de469b36` | same |
| Signature schemes | v2 only (v1/v3 absent) | same — v2 covers the whole minSdk 24+ range |
| **Tag** | `v0.9.0-beta.1` → `0bd50cb` | annotated, on the commit the artifact was built from |
| Changelog | [`[0.9.0-beta.1]` — 2026-07-22](../../CHANGELOG.md) | cut from `[Unreleased]`, link refs updated |
| Release notes | the changelog section + the tester package | — |
| Tests | **1017 run · 0 failures · 1 skipped** | JUnit XML across all ten modules |

The one skip is the `@Ignore`d `ReframeSessionTest` sibling — [readiness T4](2026-07-22-beta-release-readiness.md#3-technical-debt--track-schedule-never-ship-as-a-surprise), issue #57.

### The artifact matches the merged source

Three pieces of evidence, in increasing strength:

1. The version bump and changelog were **committed first** (`0bd50cb`), then the release assembled —
   so the artifact is downstream of the commit, not the other way round.
2. A **subsequent `:app:assembleRelease` reported every task `UP-TO-DATE`**, including
   `packageRelease`. A clean re-run that executes work would mean the artifact predates something in
   the tree; this one executed nothing.
3. The APK's own manifest reports `versionCode 3`, which exists **only** in the committed
   `app/build.gradle.kts`.

**With one honest deviation, named by the Release Agent (RI1).** `gradle.properties` is uncommitted,
and its diff is not purely machine tuning: alongside heap sizes and worker counts it sets
`android.defaults.buildfeatures.buildconfig=false`, `resvalues=false`, `shaders=false` and
`org.gradle.configuration-cache=true`. Those affect what the build produces. **So a clean clone at
`v0.9.0-beta.1` would build with a different configuration than the artifact was built with** — the
same source, not the same build. In practice the difference is additive and inert (the disabled
features generate classes and resources the app never references, which is why they were disabled),
and the tag's source is exactly what the APK reports. But the claim "reproducible from the tag" is
not one this release can make, and pretending otherwise would be the kind of unbacked assurance this
document exists to prevent. Recorded as **T9**; the fix is to split the file so build semantics are
committed and machine tuning is not.

### Device verification of the artifact itself

Galaxy A17 5G (SM-A176B), Android 16 — the same phone as the UX-P0 pass.

- **The signature break was verified rather than assumed.** Installing over the existing build was
  refused with exactly `INSTALL_FAILED_UPDATE_INCOMPATIBLE: Existing package com.aritr.zinely
  signatures do not match newer version` — the error the tester package quotes. The existing install
  was untouched by the failure. **§2 of the tester note is now evidence, not a prediction.**
- **Clean install** (uninstall → install → launch): succeeds, reports `versionCode 3`, lands on the
  empty shelf.
- **First journey smoke:** Start a zine → paper picker → A4 → editor (the D1 fix visible: the blank
  page's invitation is drawn on the paper) → the top-end entry → **Read**, "Your zine · Read · swipe
  to turn the page", "Page 1 of 8", **Print & fold** primary. The feature this release is named for
  works in the artifact being shipped, not merely on `main`.

This satisfies [RELEASING §2 step 7](../RELEASING.md#2-cutting-a-build) — a build only ever verified
as an upgrade has not been verified, and this one could not have been verified as an upgrade at all.

## 3. Remaining technical debt

Carried from [readiness §3](2026-07-22-beta-release-readiness.md#3-technical-debt--track-schedule-never-ship-as-a-surprise),
unchanged by the cut — T1 no Roborazzi golden for Read · T2 the Sheet "Back" secondary · T3 Read
re-decodes every image per draw pass · T4 the issue-#57 decoder flake · T5 the one inverted
HTML-first pass · T6 engineer-written Reframe refusal copy. Two additions from this cut:

| | Item | Note |
|---|---|---|
| **T7** | **The APK is not byte-reproducible.** | `packageRelease` re-signs, so two builds of one commit differ. Mitigated by copying to `dist/` and checksumming there ([RELEASING §2](../RELEASING.md#2-cutting-a-build)); a real fix would need reproducible-build work nobody is asking for yet. |
| **T8** | **The release is assembled by hand.** | Version bump, test sweep, assemble, verify, tag, checksum — six steps, each individually skippable, and skipping any of them is silent. It went right this time because it was done attentively, which is not a control. |
| **T9** | **The build configuration that produced the release is not in the repository.** | `gradle.properties` carries build semantics (`buildfeatures.*=false`, configuration cache) *and* machine tuning (heap, workers) in one uncommitted file, so the tag cannot reproduce its own build configuration. Split it. |
| **T10** | **The APK is signed with v2 only; v3 is absent.** | Verification is correct for the whole minSdk 24+ range, so nothing is broken. But **v3 is the scheme that permits key rotation** — without it, "every future build installs cleanly over this one" depends on the key surviving forever. It is a second, independent reason B4 is not optional. |

## 4. Remaining known limitations

All are in the shipped changelog and the tester package, in tester-facing words. In one place:

**Data.** No backup or restore — zines exist only on the phone, and uninstalling deletes them.
Deleting a photo doesn't reclaim its storage.

**Text.** One typeface (Inter). Non-Latin scripts and emoji render blank. Styling is per-block, not
per-character. Some inks fall below AA on white.

**Print.** No in-app print — Zinely makes the PDF and hands it off. The imposed sheet under
Print & fold shows page numbers, not artwork. Print at 100%, never "fit to page".

**Platform.** Android 7–9 asks once for a broad legacy storage permission on first save.

**This release only.** The one-time signature break: a tester on `0.8.0` or earlier must export,
uninstall, and reinstall — losing anything not exported. It ends here.

## 5. Recommended beta feedback areas

Ordered by how much the answer would change what gets built next. §3 of the tester package says the
same things in the tester's language.

1. **The first two minutes** — where a first-time user hesitates, in their words, before anyone
   explains anything. The one thing no amount of internal review can produce.
2. **Does Read answer "what did I make?"** — this release's entire thesis. If it doesn't land, the
   V1 plan built on top of it is wrong.
3. **Did anyone actually print and fold one?** Page order, rotation and scale on a real printer,
   with real paper and real scissors. The only wholly untestable-by-us part of the product.
4. **Words that lied** — any label, button or message whose behaviour didn't match its wording. The
   0.9.0-beta.1 cycle found two of these on-device, both invisible to the test suite.
5. **Discoverability of text styling** — `Aa` shipped in this build and is reachable only after
   selecting a text block. Nobody outside the project has tried to find it.

Feature requests for fonts, stickers and photo spanning are *already* on the V1 backlog; hearing
them again costs a tester's goodwill and tells us nothing.

## 6. Findings from the cut

| | Finding | Class |
|---|---|---|
| **F1** | **A checksum names a copy, not a version.** I hashed the APK, and a later no-op assemble re-ran `packageRelease` and produced a byte-different file from an unchanged tree — same contents, new signature, new hash. The recorded checksum was stale within minutes and nothing said so. Fixed by copying to `dist/` and documenting it ([RELEASING §2](../RELEASING.md#2-cutting-a-build)); also **T7** above. | Technical Debt |
| **F2** | **`RELEASING.md` §2 had two step 5s and two step 6s** after this addition. Renumbered. | Documentation defect (fixed) |
| **F3** | **The doorway still says "Preview".** The editor's top-end entry reads `Preview ›`; it now opens **Read**. The destination answers the review's central question, but the label a first-time user must tap to find their zine is still the word the review indicted. One-word change, deliberately **not** made — no new implementation until this report is evaluated. | Beta polish — recommended first |
| **F4** | **`versionCode 2` was already installed on the verification device**, which is what makes reusing it an install that silently refuses to update. Confirmed on-device before the bump, not inferred. | Observation |
| **F5** | **The launcher label is lowercase `zinely`**, against "Zinely" everywhere else. `aapt2 dump badging` → `application-label:'zinely'`. Cosmetic, but it is the app's name on the tester's home screen — the first word of the product they see, every time. | Beta polish |

F3 is not a blocker: nothing is broken, the entry works, and the screen behind it is right. It is
first on the polish list because it is the cheapest remaining move on the highest-value finding.

### What the independent Release Agent found that I did not

The review was run against the actual APK, the actual tags and the actual test XML — not this
document. It returned **GO WITH FIXES**, confirmed every binary claim in §1–§2 independently
(checksum, signer, versionCode, tag→tree, 1017/0/1 across 134 XML files, and that the shipped APK
requests **no `INTERNET` permission** — the privacy invariant verified at the artifact rather than
the source), and found **two tester-facing defects I had missed**. Both are now fixed. They are
recorded here rather than quietly repaired because the shape of the miss is the useful part:

| | Finding | Status |
|---|---|---|
| **RF1** | **The rescue instruction named buttons that do not exist in the builds it addresses.** §2 told a tester on an older Zinely to use *"Print & fold → Save PDF"* — **this build's** labels. `0.8.0` reads **Preview › → Print setup → Save PDF**; `0.6.0-alpha.1` **has no save at all** and can only share. So the one instruction standing between an alpha tester and permanent data loss was unfollowable, in the document they would trust most. Verified against `git show v0.8.0:…/ProofScreen.kt`. | **Fixed** — §2 now branches by version in each version's own words, and [RELEASING §3](../RELEASING.md#the-one-time-break-at-090-beta1) records the trap so the next cut doesn't regenerate it |
| **RF2** | **§3 told testers to tap "Read your zine"** — a string that exists nowhere in the app (`grep` → zero hits). The doorway is `Preview ›`. I had written the tester note as though **F3 above had been fixed**, in the same document where I recorded deciding not to fix it. | **Fixed** — §3 names the real label, and says out loud that the link says "Preview" and the screen says "Read", inviting the tester to report if it threw them |

**Why I missed both:** I verified the artifact exhaustively and the tester package only for
*completeness* — every required section present — never for *executability* by the person holding an
older build. A document can contain every mandated topic and still be impossible to follow. That is
precisely the Pass 1 / Pass 2 split [CLAUDE.md](../../CLAUDE.md#device-verification-mandatory) now
mandates for screens, and it applies with equal force to the writing that ships beside them: I ran
Pass 1 on the tester note and called it done.

Also accepted from the review: **RI1** → the `gradle.properties` deviation is now stated precisely in
§2 rather than waved past as "local tuning" (**T9**); **RI2** → the checksum is now *in* the tester
package, since the superseded APK shares its filename; **RI3** → the RELEASING fix above; **RI4** →
the data-loss warning is hoisted above the install steps, so it cannot be reached second; **O1** →
the note no longer predicts an "older version of Android" dialog that a targetSdk-36 build will not
trigger; **O2** → recorded as **T10**; **O3** → **F5** above.

## 7. Recommendation

# GO

Ship `dist/zinely-0.9.0-beta.1-release.apk` (SHA-256 `736a18c9…b358`) with
[the tester package](../releases/0.9.0-beta.1.md).

All four readiness blockers are closed: **B1** the artifact is re-cut at `versionCode 3` and verified
from clean install; **B2** the changelog is cut and `v0.9.0-beta.1` is tagged on the built commit;
**B3** the tester note exists and leads with the data-loss warning. **B4 — the keystore backup — is
the founder's, is not done, and cannot be done or verified by me.** Instructions are in
[RELEASING §1](../RELEASING.md#backing-it-up--founder-instructions).

The GO is on the **artifact**, and the artifact is finished. Three things stand between it and a
tester, none of them a code change and none of them mine to complete:

1. **Back up the keystore before the first tester installs.** Not because the install depends on it,
   but because the moment someone is holding this build, losing the key stops being an inconvenience
   and becomes *their* data loss: every future update would require an uninstall, and uninstalling
   deletes their zines. **T10** makes this worse, not better — the APK carries no v3 signature, so
   key rotation is not an escape hatch either. The window where this is cheap closes when you send
   the file.
2. **Fill in §5 of the tester package** — how to reach you. It is deliberately blank and marked. A
   bug report with nowhere to go is not a bug report.
3. **Decide about pushing.** `main` is **11 commits ahead of `origin`, and the tag is local only** —
   `git ls-remote --tags origin` does not list `v0.9.0-beta.1`. The two compare links at the foot of
   [CHANGELOG.md](../../CHANGELOG.md) therefore 404 today, and
   [RELEASING §2 step 8](../RELEASING.md#2-cutting-a-build) says to push both. Pushing publishes the
   whole history to GitHub, so I have not done it unasked. **Either push, or accept that the beta is
   distributed from a local tree** — both are defensible, but the changelog's links are only honest
   under the first.

**The verdict does not depend on any of the three.** They gate the send, not the build.
