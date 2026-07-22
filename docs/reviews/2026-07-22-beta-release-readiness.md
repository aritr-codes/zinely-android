# Beta release readiness — `0.9.0-beta.1`

Assessed 2026-07-22 against `main` at the UX-P0 merge. Categories are the four from
[CLAUDE.md](../../CLAUDE.md#release-review-release-agent) — Release Blocker / Known Limitation /
Technical Debt / Future Enhancement — which are never conflated.

---

## 0. The headline

**The beta was built but never cut.** A release-signed `zinely-0.9.0-beta.1-release.apk` exists from
2026-07-21 and passed a ten-step smoke test, but there is **no `v0.9.0-beta.1` tag**, no dated CHANGELOG
section, and no tester note. Everything since then — the three P0 defects and Read mode — landed *after*
that artifact was produced.

**So the APK on disk is not the beta to ship.** It predates the fixes that were made *because of* the
review of it. Ship a rebuilt artifact or ship nothing.

---

## 1. Release blockers

| | Item | Why it blocks |
|---|---|---|
| **B1** | **Re-cut the artifact.** Rebuild release-signed from `main`, bump `versionCode` 2 → 3. | The existing APK lacks every fix from this review cycle. `versionCode 2` is already installed on at least one device, so reusing it means an install that silently refuses to update. |
| **B2** | **Cut `[Unreleased]` into a dated `[0.9.0-beta.1]` section** and tag it. | The CHANGELOG currently claims these changes are unreleased while an APK bearing that version number exists. Two sources of truth about one build. |
| **B3** | **Write the tester note.** It must say: side-loading is an unknown source; **uninstalling deletes your zines** (no backup/restore exists); export anything you care about with Save PDF first; and this build is the last one requiring an uninstall — every build after it installs cleanly over its predecessor. | [RELEASING.md](../RELEASING.md) requires it, and the signature break is a one-time data-loss trap that only the note can prevent. |
| **B4** | **Back up `keystore.properties` and `zinely-release.jks` off-machine.** *(Owner: founder — I cannot do this.)* | The password was generated in-shell and never printed; `keystore.properties` is the only copy. Lose it and **no future build can ever update an installed Zinely** — every tester uninstalls, and uninstalling deletes their zines. This is the single highest-consequence item on the page. |

Nothing else blocks. The four defects the UX review classified as release blockers (D1, D2, D3, and the
"Preview" misnomer) are fixed, merged, and device-verified; the misnomer was resolved by building Read
rather than by renaming.

---

## 2. Known limitations — must appear in the release notes

Carried forward from `0.8.0`/`[Unreleased]` and still true:

- **No font choice**; single bundled Inter family. **Non-Latin text renders blank** (Latin-first bundle).
- **Styling is per-block, not per-character.**
- **Some text inks are low-contrast** — authorial values below AA as body text.
- **API 24–28 asks for a one-time storage permission** on first save, and on those versions it is a broad
  legacy permission rather than a Downloads-scoped one.
- **No asset garbage collection** — deleting a photo does not reclaim its bytes; storage only grows.
- **No backup or restore.** Uninstalling deletes everything.
- **The imposed print sheet shows page numbers, not artwork** — a documented deferral, now un-blocked by
  Read threading the document into the Proof surface, but still deferred ([ADR-058](../DECISIONS.md#adr-058) Decision 7).
- **No in-app print** — Save PDF / Share is the honest home-print hand-off ([ADR-052](../DECISIONS.md#adr-052)).

---

## 3. Technical debt — track, schedule, never ship as a surprise

| | Item | Note |
|---|---|---|
| **T1** | **No Roborazzi golden for the Read act.** | Read has behavioural + device coverage but no pixel gate. Goldens are recordable only on the pinned CI image. |
| **T2** | **A "Back" secondary on the Sheet action bar** is the better answer than the act-aware top-bar back that shipped. | Deferred solely because it changes pixels on an act with a committed golden ([ADR-058](../DECISIONS.md#adr-058) D6). Pairs naturally with T1 — one CI recording pass closes both. |
| **T3** | **Read re-decodes every image on every draw pass.** | `ImageBlitter` decodes → blits → recycles per call, so memory is fine, but a fling through a photo-heavy 8-page zine does that work per frame. Pre-existing architecture, amplified by Read. **Wants one device pass on a full 8-photo zine before V1.** |
| **T4** | **`ReframeSessionTest` flakes on the CI decoder** (issue #57). | One failure in two clean full runs. Environment, never a product defect — but the `@Ignore`d sibling test is absent coverage wearing a green tick. |
| **T5** | **The HTML-first workflow was inverted once**, for Read. | Spec back-filled and labelled as such in `proof.html`. The next change returns to HTML-first. |
| **T6** | **Reframe refusal copy is engineer-written.** | Four spoken lines invented to keep the keyboard path from going silent. Marked replaceable, following the `ReframeUnavailableAnnouncement` precedent. |

---

## 4. Beta improvements — during the beta, on feedback

From the approved [Beta UX Review](../BETA-UX-REVIEW.md) §6, unchanged and none of them blocking:

- Live cover thumbnails on the library card; drop the wordmark row and the "On this device" chip (**M**)
- Put `Aa` first in the toolbar, plus a keyboard-accessory `Aa`, so typography is discoverable (**S**)
- Tie the inline text editor visually to its box (**S**)
- Stop the page resizing when the toolbar changes height (**M**)

---

## 5. Version 1.0 backlog

| Item | Size | State |
|---|---|---|
| ~~Read mode~~ | M | ✅ **shipped** — was the review's highest-value single item |
| In-place text editing | M | queued |
| Font voices + coverage warning | M | queued (closes the non-Latin limitation) |
| Starter graphics set / stickers | M | queued — research favours themed packs of ten, recolourable |
| Photo spanning across 2 or 4 pages | L | queued — **needs no imposition work**: every facing pair is a contiguous, uniformly-rotated strip on the sheet, and pages 2–5 are exactly the top row |
| Thumbnail caching | M | queued |
| Per-panel artwork on the imposed sheet | M | newly un-blocked by ADR-058 |
| Asset GC / sweeper | M | long-standing ([ADR-031](../DECISIONS.md#adr-031) §2) |
| Backup / restore | L | the gap that makes the uninstall trap dangerous |

---

## 6. Verdict

**Not shippable today; four blockers, none of them code.** B1–B3 are mechanical and can be done in one
sitting. B4 is the founder's and is the one that cannot be undone if it goes wrong.

The product itself is in the best shape it has been: the "it lost my work" reading is gone, the central
artefact is finally visible, and the two-pass device gate that found the last defect is now written down
as a rule rather than a habit.
