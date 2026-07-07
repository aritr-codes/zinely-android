# v0.6.0-alpha.1 release assessment — everything known, in one place

**Date:** 2026-07-04 · **Snapshot:** `main` @ `57ed568` (S7.2 checkpoint reconciliation merged)
plus three *uncommitted* build-script changes in the working tree (release debug-signing,
version-derived APK naming, `zinelyVersionName = "0.6.0-alpha.0"`).

**Purpose:** the single consolidated triage of every known bug, UX rough edge, recorded deferral,
tech-debt item, and pending ADR **before** the `v0.6.0-alpha.1` tag decision. This is a
point-in-time *review artifact* (like the [2026-06-27 onboarding review](2026-06-27-onboarding-review-claude-brief.md)):
it **indexes and links** the owning documents — deferral decisions live in
[ADR-047](../DECISIONS.md#adr-047) / [PRD §7.3](../PRD.md#73-alpha-release-scope--v060-alpha1-adr-047) /
[ROADMAP](../ROADMAP.md) — it does not re-decide them.

**Tiers:** 🔴 **Alpha gate** (blocks the `alpha.1` tag) · 🟠 **S7.x** (post-alpha, before `1.0.0`) ·
🟡 **V1** · 🔵 **V2** · 🔭 **Future** · **Scope:** S (≤half-day) / M (1–3 days) / L (slice-sized).

---

## A. Alpha gate — what actually blocks the tag

| # | Item | Why it gates | Scope |
|---|---|---|---|
| A1 | **Physical print/fold verification** — print exported sheet at 100%, fold, verify 1→8 order/rotation/scale | The ROADMAP milestone row's explicit gate; the product's one core promise | external (hardware) |
| A2 | **User-report triage: "text missing in preview"** (see B3) | Until reproduced/understood it *may* violate `preview == export` ([ADR-006](../DECISIONS.md#adr-006)) — the release criterion. Owner's device shows text correctly; awaiting reporter details (which screen, where text DOES appear, script typed, timing, device) | triage S; fix unknown |
| A3 | **Step 4 packaging** — bump `alpha.0` → `alpha.1`, CHANGELOG, ROADMAP milestone ✅, tag, artifact; **commit the three working-tree build changes** (signing/naming/version) in the same PR | The release itself; the working tree must not stay dirty under a tag | S |

Nothing else below blocks the tag.

## B. Real-user feedback (2026-07-04 alpha test)

| # | Report | Verdict | Tier | Scope & notes |
|---|---|---|---|---|
| B1 | Exported image/PDF doesn't appear in gallery/files | **Missing feature, recorded deferral** — [ADR-039](../DECISIONS.md#adr-039) writes to private `cacheDir` + `FileProvider` share only; `MediaStore`/`ACTION_CREATE_DOCUMENT` "save a copy" named deferred in [ARCHITECTURE §6](../ARCHITECTURE.md#6-export-pipeline) | 🟠 S7.x — **first post-alpha slice**; testers hit it immediately | **M.** "Save to your phone" on Export/Completion via `MediaStore.Downloads` (API 29+) — minSdk 24 needs a pre-29 legacy path + permission decision. Needs a new ADR (§G1). Privacy invariant unaffected (local write) |
| B2 | Pages 2–3, 4–5, 6–7 should be mergeable (spreads) | **Feature request** — already ROADMAP V2 "multi-page spreads" | 🔵 V2 (pull to V1 if demand repeats) | **L.** Geometry note: under `TOP_ROW_ROTATED` those exact facing pairs ARE sheet-adjacent with matching rotation, so cross-fold artwork is imposable without new engine machinery — cheaper than V2 assumes, but touches model + editor + imposition contract + preview. Needs ADR (§G4) |
| B3 | Text does not appear "in the preview" | **Bug until disproven** — same `SceneRenderer → PagePreview` path serves canvas, booklet Preview, thumbnails, export, so a real parity break is serious. Leading hypotheses: (a) which-screen misunderstanding; (b) **Inter MVP-charset gap** — non-Latin/emoji text silently unrenderable ([ADR-028](../DECISIONS.md#adr-028) cmap coverage guard) → that's C1, a known limitation surfacing; (c) commit-on-navigate race in the text-edit session | 🔴 A2 (triage) | Triage S. Fix scope depends on cause: (a) none, (b) M–L font/i18n policy (§G2), (c) S–M session fix |
| B4 | Persistent gap on the right side of the page in the editor | **Known limitation, documented in code** — page is fit *top-left anchored*, pan pinned to zero ("true centring/zoom is a follow-up", `EditorScreen.kt`); S7.2's paper backing made the letterbox stripe more visible | 🟠 S7.x editor polish | **M.** Real fix = centred `SetViewport` pan; all layers read `view.pageOffset` so it should propagate, but gesture math is regression-prone — wrong thing to rush pre-tag. A visual-only centring hack would desync taps from pixels; rejected |

## C. Self-found correctness risks (development / review / ADB testing; not previously consolidated)

| # | Item | Detail | Tier | Scope |
|---|---|---|---|---|
| C1 | **Bundled-font charset coverage is Latin-MVP only** | Inter with an MVP-charset cmap guard ([ADR-028](../DECISIONS.md#adr-028)). A tester writing Bengali/Hindi/CJK/emoji gets silently missing or degraded glyphs — likely the strongest hypothesis for B3, and a real-world certainty for a non-English tester pool | 🟠 S7.x if B3 confirms it (else 🟡 V1 with "bundled font expansion") | M–L; needs an i18n/coverage policy ADR (§G2) — minimum honest fix: detect uncovered chars and warn rather than render blanks |
| C2 | **Export files live in evictable `cacheDir`** | A share target that defers reading (or the OS trimming cache) can hit a dead `content://` URI later; unique filenames fixed staleness, not eviction | 🟠 rides B1 (persistent copy solves both) | folded into B1 |
| C3 | **"Saved ✨" is optimistic, not a write receipt** | Recorded honestly in [ADR-034](../DECISIONS.md#adr-034) (mark-dirty signal; failure banner corrects after the fact). Worst case: chip shows, write fails seconds later, banner corrects. Accepted trade-off — track, don't fix | 🔭 monitor | — |
| C4 | **Shelf thumbnail staleness — ms-scale flush race** | Recorded in [ADR-045](../DECISIONS.md#adr-045)/[ADR-046](../DECISIONS.md#adr-046) (`WhileSubscribed(0)` re-read; mtime-stamp validity). Cosmetic worst case: one stale miniature until next shelf visit | 🔭 monitor | — |
| C5 | **Auto post-export landing has no automated back-stack test** | [ADR-041](../DECISIONS.md#adr-041) shipped on manual QA; host-nav unit tests exist since ADR-046 but the export→Completion auto-nav path specifically is uncovered | 🟠 S7.x test debt | S (extend the ADR-046 `TestNavHostController` suite) |
| C6 | **"Add words" then cancel leaves an empty committed text box; place+edit = two undo steps** | Tracked in-code (`addTextAndEdit` kdoc follow-up, `EditorScreen.kt`): should become one reducer-owned intent that removes a cancelled brand-new empty text and collapses to one undo step | 🟠 S7.x (bundle with text-styling slice) | S–M |

## D. UX rough edges (self-found, not release-gating)

| # | Item | Detail | Tier | Scope |
|---|---|---|---|---|
| D1 | Editor has no zoom / pan | Only fit-to-canvas; fine for 8-page minis, limiting for detail work. Same slice as B4 centring | 🟡 V1 | M–L |
| D2 | No busy indicator during a slow create (single-flight guard is silent) | **Rejected-by-decision** ([ADR-046 §5](../DECISIONS.md#adr-046), reaffirmed ADR-047 review): VM guard is the contract. Revisit only if testers report confusion | 🔭 monitor | S if ever |
| D3 | `deskTextSoft` (0.8-alpha onBackground) chosen to match mockups, not contrast-audited | Roles are correct post-S7.2; the alpha value itself has no WCAG check and no screenshot test guards theme roles against regression | 🟡 V1 (roadmap already holds "full accessibility pass; dark theme") | S audit; M for theme screenshot tests |
| D4 | No per-project paper *change* after create | Named non-goal in [ADR-047](../DECISIONS.md#adr-047); model carries `paperSize`, editor affordance is V1 | 🟡 V1 | M |
| D5 | Alpha text is single-style; images are FIT-only; no layout presets | The recorded [PRD §7.3](../PRD.md#73-alpha-release-scope--v060-alpha1-adr-047) deferrals — listed here only for completeness | 🟠 S7.x (text styling first, per ADR-047) | slice-sized each |
| D6 | Export mockup (`export.html`) is a manually-synced copy of the canonical imposition order | Documented with a sync comment (S7.2); prototype-only, but it is still a second hardcoded copy | 🔭 accept (prototypes are references, not truth) | — |

## E. Recorded deferrals (ownership: ADR-047 / PRD §7.3 / ROADMAP — indexed here)

| Item | Owner | Tier |
|---|---|---|
| Text styling (FR-3 style clause) | [ADR-047](../DECISIONS.md#adr-047) | 🟠 S7.x (named next editor slice) |
| Per-page layout presets (FR-4) | ADR-047 | 🟠 post-alpha |
| Image fit/fill control (FR-2 clause) | ADR-047 amendment | 🟠 S7.x |
| Calibration ruler | ADR-047 / [ADR-039](../DECISIONS.md#adr-039) (deferred with cause — no margin) | 🟡 V1 ("calibration test sheet") |
| Asset GC sweeper — **storage grows with every import** | [ADR-031 §2](../DECISIONS.md#adr-031) (blocked on import-pins + ADR-022 race tests) | 🟠 S7.x if testers report storage pressure, else V1 |
| Save-a-copy (`MediaStore`) + `PrintManager` in-app print | ARCHITECTURE §6 / ROADMAP V1 | 🟠 B1 pulls save-a-copy forward; PrintManager stays 🟡 V1 |
| `.zine` backup/restore (SAF) | [ADR-009](../DECISIONS.md#adr-009) / ROADMAP V1 | 🟡 V1 |
| Welcome screen (first-run flag) + Settings screen | PRD §9 note (proposal pending ratification) / SCREEN-INVENTORY | 🟡 V1; Welcome needs the PRD flow ratification + nav ADR first |
| Snapping/alignment guides beyond current, crop/adjustments, templates, page reorder | ROADMAP V1 | 🟡 V1 |
| Spreads, 16-page saddle-stitch, stickers/drawing, custom fonts, print-shop groundwork | ROADMAP V2 | 🔵 V2 |

## F. Tech debt & build/release hygiene

| # | Item | Detail | Tier | Scope |
|---|---|---|---|---|
| F1 | **Release build signed with the machine-local debug keystore** | Deliberate for alpha side-loading (comment in `app/build.gradle.kts`); signature is machine-bound — a different build machine breaks tester upgrade continuity. A real release keystore (+ storage/secrets policy) must exist before any store distribution | 🟡 V1 gate, decide at S7.x | S + ADR (§G3) |
| F2 | `isMinifyEnabled = false` on release | Honest unshrunk 14 MB alpha; enable R8 + keep-rules before store distribution | 🟡 V1 | M (keep-rule debugging) |
| F3 | Version truth: `0.6.0-alpha.0` + APK naming + signing are **uncommitted** | Must land in the Step 4 PR (A3) — a tagged release must not sit on a dirty tree | 🔴 in A3 | — |
| F4 | Roborazzi goldens are capture-no-op locally; recording/verification lives on the pinned CI image | By design (ADR-028 §7.5), but means local runs can't catch pixel drift; parity relies on CI | 🔭 accept | — |
| F5 | Instrumented suites (`conn`/`edc`) are manual, device-required | PDF write/parity proofs compile-checked in CI, executed only on-device by hand | 🟠 S7.x: run once as part of each release candidate checklist | S (checklist item) |
| F6 | Dev-env workarounds: `tools/grun.sh` (hook intercepts build-tool names), adb MCP broken → raw `adb` | Dev-only friction, no product impact; documented in session memory | 🔭 accept | — |

## G. ADRs / design docs to write (when the owning slice starts)

| # | ADR | Triggered by |
|---|---|---|
| G1 | **Export persistence policy** — save-a-copy via `MediaStore.Downloads`, pre-API-29 strategy, filename/collision policy, cache-eviction stance | B1/C2 (first S7.x slice) |
| G2 | **Text i18n / font-coverage policy** — bundled-font expansion vs. system-font fallback vs. honest unsupported-character warning; interacts with vector-PDF text fidelity (ADR-001) | B3 outcome / C1 |
| G3 | **Release signing & distribution** — keystore creation, storage, upgrade continuity from the debug-signed alphas (testers must uninstall on signature change — say so in release notes) | F1, before first store build |
| G4 | **Spread/merged-pages document model** — how a spread element crosses panel boundaries; imposition contract extension | B2 (V2) |
| G5 | **Editor viewport (centring/zoom/pan)** — probably slice-doc not full ADR; gesture-space contract already exists via `view.pageOffset` | B4/D1 |

## H. Summary matrix

- 🔴 **Alpha gate:** A1 print/fold test · A2 = B3 triage · A3 Step 4 packaging (incl. F3).
- 🟠 **S7.x (ordered recommendation):** B1 save-a-copy (+C2, G1) → text styling slice (D5, +C6) → B4 editor centring → C1/G2 if B3 implicates fonts → C5 nav-test debt → F5 RC checklist → GC (ADR-031) on first storage complaint.
- 🟡 **V1:** D1 zoom/pan · D3 a11y/dark-theme pass · D4 paper change · calibration sheet · PrintManager · SAF backup · Welcome/Settings · F1/G3 signing · F2 R8.
- 🔵 **V2:** B2 spreads (+G4) · roadmap V2 set.
- 🔭 **Monitor/accept:** C3, C4, D2, D6, F4, F6.

> **Maintenance note:** this file is a snapshot, not a tracker. When an item is picked up, its
> decision goes to an ADR and its phasing to ROADMAP; update those, not this.
