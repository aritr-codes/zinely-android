# Owner checklist — what is blocked on you

**This document owns nothing.** It is an index of work an AI implementer *cannot* close, pointing at the
document that does own each item. Per the [Documentation Rule](../CLAUDE.md#documentation-rule-mandatory),
every ruling, defect and decision below lives in its authoritative file — this page only says where to look
and why you are the only one who can close it.

**Maintained by the Implementer Agent.** Items are added as they are discovered and struck as you close
them. If anything here could be closed by an implementer, that is a bug in this list — say so and I'll take it.

**Last swept:** 2026-08-18 · **69 open items**

---

## Why an item lands here

| Reason | Meaning |
|---|---|
| **Decision** | The frozen spec doesn't rule on it; choosing would be redesigning a frozen surface |
| **Judgement** | A first-time-user reading — knowing the implementation *disqualifies* me ([Pass 2](../CLAUDE.md#pass-2--first-time-user-verification)) |
| **Physical** | A real device, a real printer, a real pair of ears |
| **Credential** | A key, an account, a push, a person to send it to |

---

## Start here — the six that actually hold things up

Everything else can wait behind these.

| # | Item | Why it's first |
|---|---|---|
| **1** | **[Back up the keystore](RELEASING.md)** (§ below, item R-1) | **Irreversible.** The passwords exist only on this machine and were never printed. The first symptom of skipping it is a build you cannot ship — and every tester uninstalling and losing their zines |
| **2** | **[D-089](design/V2-SPEC-DEFECTS.md#d-089) — snack vs. context bar** | Blocks the S7-placement merge, and it is a *visible* happy-path defect: the message reads `Placed on the pa` |
| **3** | **Pass 2 on the supplies drawer** | Blocks the same merge. Pass 1 is ✅ done (2026-08-17, SM-A176B / Android 16) |
| **4** | **[Accept or reject ADR-098](DECISIONS.md#adr-098)** | Status is `Proposed`, deliberately. **Phase D cannot open at all** until you rule, and twelve further decisions sit behind it |
| **5** | **[CI-22](V1-CONFORMANCE-INVENTORY.md) — re-freeze the V1 HTML** | The declared **critical path** of the whole V1 conformance programme (CI-40 → CI-64 → CI-74). C0 is *not started* |
| **6** | **[CI-14](reviews/CI-14-motion-baseline-protocol.md) — motion & haptics baseline** | "Startable today." Until it exists, *any* duration change is made against a tie-break nobody has taken |

---

## 1. Rulings & decisions

### 1.1 V2 spec defects — [`design/V2-SPEC-DEFECTS.md`](design/V2-SPEC-DEFECTS.md)

Amending a frozen V2 surface is reserved to you (V2-CONSTITUTION §VI); an implementer may not edit the freeze.

| ☐ | Rule | Question | Blocks |
|---|---|---|---|
| ☐ | [D-089](design/V2-SPEC-DEFECTS.md#d-089) | What happens when the placement snack and the context bar want the same band? Three options named | **Visible defect**, S7 merge |
| ☐ | [D-086](design/V2-SPEC-DEFECTS.md#d-086) | What should the twelve unauthored, disabled supply tiles show? | Art sheet's production call site |
| ☐ | [D-080](design/V2-SPEC-DEFECTS.md#d-080) | Amend the frozen Art sheet: own glyph, filtering family chips, an empty state | Whichever phase builds it out |
| ☐ | [D-083](design/V2-SPEC-DEFECTS.md#d-083) | Relabel the ink popover so `Ink` (verb) / `Ink` (maker) / `Ink` (neutral) are distinguishable | Shipped a11y defect |
| ☐ | [D-052 / D-081 Q10](design/V2-SPEC-DEFECTS.md#d-081) | Per-page (not per-batch) placement cascade | The "it lost my photo" misread on consecutive shares |
| ☐ | [D-079](design/V2-SPEC-DEFECTS.md#d-079) | Which single privacy statement survives, and where does the colophon live? | Five instances ship; no colophon exists in any `.kt` |
| ☐ | [D-078](design/V2-SPEC-DEFECTS.md#d-078) | How does the transform context bar lay out when it doesn't fit? | `Send backward` absent from a11y tree; `Bring forward` is an 8dp target |
| ☐ | [D-030](design/V2-SPEC-DEFECTS.md#d-030) | Real fixed 8 pages, or does variable page count arrive? | Filmstrip/dots morph, grid add/delete |
| ☐ | [D-029 Q1–Q3](design/V2-SPEC-DEFECTS.md#d-029) | The keep-shelf's scope, home and lifetime (Q4 closed) | The tray/shelf capability entirely |
| ☐ | [D-038](design/V2-SPEC-DEFECTS.md#d-038) | Is `Replace` on the frozen photo bar a capability we ship? | — |
| ☐ | [D-036](design/V2-SPEC-DEFECTS.md#d-036) | Four frozen resize handles vs. eight shipped | — |
| ☐ | [D-027](design/V2-SPEC-DEFECTS.md#d-027) | Does the shelf sheet's metadata line say "Edited …", with week granularity? | — |
| ☐ | [D-023](design/V2-SPEC-DEFECTS.md#d-023) | Does the Library's `--paper` primary-button label become `--on-matcha`? | Library's primary action stays off-corpus |
| ☐ | [D-090](design/V2-SPEC-DEFECTS.md#d-090) | **Re-record the five decor goldens on the pinned CI image** (`record-goldens.yml` is `workflow_dispatch` on a pushed branch, so only you can run it) | The decor verb row and the decor ink palette are now observed — but on the Windows dev host, **not the host that gates them** |

### 1.2 Phase D decisions — [`DECISIONS.md` ADR-098 §5](DECISIONS.md#adr-098-gate)

**All of these are behind item 4 above** — accepting ADR-098 itself.

| ☐ | ID | Decision | Blocks |
|---|---|---|---|
| ☐ | OD-30 | Fraunces vs. Inter for the nine document-content selectors | D2, D5 |
| ☐ | OD-32 | Amend the Proof to add Loading/Error states, or rule them out of the freeze | D3 |
| ☐ | OD-33 | Does the Proof checklist get a real failure state? | D4 |
| ☐ | OD-34 | Does the mini-sheet track the A4/Letter selection? | D4 |
| ☐ | OD-35 | The fold animation: rendering, rest state, reduced-motion path | D6 |
| ☐ | OD-36 | Is Save terminal, or does the band return? | D7 |
| ☐ | OD-38 | The empty-state sticker cluster's a11y announcement; assign it an id | The phase gate's honesty |
| ☐ | OD-39 | Four corpus-integrity items in the frozen Proof (dead CSS, unused tokens, `flash()`) | D0, D3 |
| ☐ | OD-40 | Confirm the per-page a11y contract is binding from prose | D2, D10 |

### 1.3 Proposed ADRs never accepted — [`DECISIONS.md`](DECISIONS.md)

| ☐ | ADR | Subject | Forcing function |
|---|---|---|---|
| ☐ | [ADR-090](DECISIONS.md#adr-090) | The scrim amendment — *"awaiting owner adoption. Nothing in this section is in force"* | Enforcing "the artifact does not dim" against four surfaces that draw a dim |
| ☐ | [ADR-014](DECISIONS.md#adr-014) | Public-API stability rules for `core:model` geometry | `core:render`'s first external consumer |
| ☐ | [ADR-016](DECISIONS.md#adr-016) | Closed enums vs. open specs for paper sizes / zine formats | The second imposition format |
| ☐ | [ADR-017](DECISIONS.md#adr-017) | Bleed, clip and safe-area semantics | V2 print-shop export |
| ☐ | [ADR-018](DECISIONS.md#adr-018) | Versioning of imposition convention names and fold/cut ids | **Must be decided before any enters a persisted `.zine` schema** |
| ☐ | — | The butter allow-list conflict, V2.1 §3.2 vs §4.1 — *"owner ruling owed"*, stated twice | The next butter-token question |

### 1.4 V1 conformance C0 — [`V1-CONFORMANCE-INVENTORY.md`](V1-CONFORMANCE-INVENTORY.md)

The whole milestone is **Not started** ([ROADMAP.md](ROADMAP.md)) and is the declared critical path.

| ☐ | ID | Ruling | Blocks |
|---|---|---|---|
| ☐ | CI-20 / CI-21 / **CI-22** | Re-freeze the V1 HTML with the radius, type-register and spacing decisions | **CI-22 is the critical path** |
| ☐ | CI-06 | A-2: Field · Row · Notice · Menu (+ Sheet, popover) | C4 |
| ☐ | CI-07 | A-3: the consequence colour and four control states | *Today there is no colour for a delete or a failure* |
| ☐ | CI-08 | A-4: the Underway band, the fourth motion job, cancellation | — |
| ☐ | CI-11 | A-7: a screen class whose subject is the tool | Settings / About / Backup / Recovery |
| ☐ | CI-15 | Supersede or re-affirm ADR-033's editor empty state | C6 — restyling without a ruling *is* silent supersession |
| ☐ | CI-16 | Supersede or re-accept the optimistic "Saved ✨" | C6 |
| ☐ | CI-17 | Decide, or formally date the deferral of, the Read page turn | — |
| ☐ | CI-19 | Decide the imposed sheet's blank panels | — |
| ☐ | CI-23 | Delete or keep the two zero-call-site shared components | *"The ruling cannot be an engineer's"* — deletion also deletes goldens |
| ☐ | CI-80 | The card→editor morph mechanism | Not decided by any accepted document |
| ☐ | CI-98 | D-6: is hand-placement rotation *placement* or *effect*? | — |
| ☐ | CI-99 | Assign a register to six type roles — `Heading` cannot be guessed | — |

### 1.5 Product & design authorship

| ☐ | Item | Where | Note |
|---|---|---|---|
| ☐ | **Author or commission the twelve hand-drawn supply outlines** | [SUPPLIES-SPEC.md](design/SUPPLIES-SPEC.md) | Needs a house style. `outlineOf()` returns `null` for each. Blocks S5 and S9 |
| ☐ | Choose the bundled font set (which OFL families) — Q3 | [PRD.md §13](PRD.md) | Blocks typography |
| ☐ | Settle brand / visual identity direction — Q4 | [PRD.md §13](PRD.md) | Blocks UI theme |
| ☐ | Decide V2.1 prototypes for Read · Fold · first-run | [V21-SPEC.md](design/V21-SPEC.md) | Three surfaces, no frozen artifact |
| ☐ | Is `+ Add` suppressed while a card's green `Done` shows? — OD-14 | [BETA-UX-REVIEW.md](BETA-UX-REVIEW.md) | Never ruled; recorded as owed |
| ☐ | **Does the maker get a `Mirror` verb, and when?** | [Intent.kt:40](../core/editor/src/main/kotlin/com/aritr/zinely/core/editor/Intent.kt#L40) | `DecorElement.mirrored` **exists in the model and is unreachable from the UI** — zero callers. **Nine of the sixteen supplies are asymmetric**, so a torn tape or corner fix cannot be flipped. The frozen decor verb set is Replace/Ink/Delete, so adding a fourth verb is an amendment, not an implementation. The code calls it *"a maker verb that arrives later"* — this is the item that decides when "later" is |

---

## 2. Device & physical verification

### 2.1 Pass 2 — first-time-user reading of the supplies drawer
**Reason:** Judgement · **Blocks:** S7-placement merge · Pass 1 ✅ 2026-08-17

Three questions are pre-registered. Write down *why it felt wrong before you knew the reason* — that
sentence is the finding, and it's usually worth more than the fix.

1. **[D-086](design/V2-SPEC-DEFECTS.md#d-086)** — twelve of sixteen tiles are dim and inert. "Coming soon", or "broken"?
2. **[D-088](design/V2-SPEC-DEFECTS.md#d-088)** — Art and Photo ship the same glyph, deliberately.
3. **New** — `shape.rect` fills with `ink`, landing as a near-black square centred on whatever is already there. Correct by spec. Does it read as *art*, or as a redaction?

### 2.2 The TalkBack listen pass — [`DEVICE-VERIFICATION.md` §3.1](DEVICE-VERIFICATION.md)
**Reason:** Physical. Samsung TalkBack logs no utterances, and `uiautomator dump` **structurally cannot
expose `stateDescription`** — so no dump I take substitutes for an ear.

| ☐ | Question | Decides |
|---|---|---|
| ☐ | Is an import summary spoken **twice**? | Whether the `announce()` call is deleted |
| ☐ | Does `Copier` speak its On/Off state? | The only instrument that can check it |
| ☐ | Is an import landing mid-transition announced at all? | Robolectric can't land an emission in that window; a thumb can |
| ☐ | Do two ink swatches both just say "Ink"? | Confirms [D-083](design/V2-SPEC-DEFECTS.md#d-083) by ear. **Now reproduced visually**: the decor popover shows `Ink` in the maker band *and* `Ink` in the neutrals band, one above the other |
| ☐ | Does a supply's **`Change ink`** custom action work under real TalkBack? | [D-091](design/V2-SPEC-DEFECTS.md#d-091) shipped this action dead once. It is fixed and regression-tested, but `uiautomator dump` **cannot show custom actions** — the action menu has to be opened by hand |

### 2.3 The print pass — [`DEVICE-VERIFICATION.md` §3.2](DEVICE-VERIFICATION.md)

| ☐ | Item | Note |
|---|---|---|
| ☐ | Print a `Copier`-filtered page and judge it | Photocopy, or noise? Re-opens D-082 Q1 — 150 dpi is affirmed only provisionally, on *screen* evidence |
| ☐ | Physical printer for fold validation — PRD Q2 | Blocks imposition validation |
| ☐ | Collect beta-cohort feedback on the photocopier print | [CHANGELOG.md](../CHANGELOG.md): *"Please print one and say what you see — that feedback is the test"* |

### 2.4 Instrumented runs that have never executed

| ☐ | Item | Why it matters |
|---|---|---|
| ☐ | **Run the PDF-surface hole test on hardware** | `PdfDocument` does not run under Robolectric. `PdfSurfaceParityInstrumentedTest` is **compile-checked and has never executed.** It is the only thing standing behind the claim that supply outlines reach paper as vectors |
| ☐ | Two-pass device verification for every V2.1 surface as it lands | Reference device SM-A176B |

---

## 3. Release & credentials

### ☐ R-1 — Back up the keystore *(do this first)*
[`RELEASING.md`](RELEASING.md) — *"No agent, script, or CI job can do this or verify it was done."* The
passwords exist only in `keystore.properties` on this machine; they were generated in a shell and never
printed. Nothing in the repo or build output would reveal the backup is missing.

- ☐ Copy `zinely-release.jks` + `keystore.properties` to **two independently-failing** places
- ☐ Verify with `keytool -list -v … -alias zinely`

### ☐ Play Store path *(only if production is the goal)*

| ☐ | Item | Note |
|---|---|---|
| ☐ | Create + identity-verify a Play Console account | $25, **1–3 business days**. *"The only step that can miss a ship date on its own"* |
| ☐ | Start the 12-testers-for-14-continuous-days closed test | Real humans, wall-clock gated |
| ☐ | Enrol in Play App Signing at first upload | **One-time irreversible** console choice |
| ☐ | Complete data-safety form, content rating, target-audience declarations | Legal attestations signed by a person; answers pre-drafted |
| ☐ | Produce the feature graphic (1024×500) | *"The one asset with no source in this repository"* |
| ☐ | Take store screenshots on a real device from a release build | — |
| ☐ | Host the privacy policy at a public URL | Needs an account/domain you control |

### ☐ Repository state
- ☐ Local `main` is **46 commits ahead of `origin/main`** and diverged (`git pull --ff-only` fails). Pre-existing work of yours; I have left it untouched and will keep leaving it untouched.

---

## 4. Measurement

| ☐ | Item | Where | Note |
|---|---|---|---|
| ☐ | **Record the motion-and-haptics baseline (CI-14)** | [protocol](reviews/CI-14-motion-baseline-protocol.md) | Device recording + subjective banding. Blocks CI-08's duration half and every C3a duration change — *any duration changed before this is changed against a tie-break nobody has taken* |
| ☐ | Measure whether **150 dpi** survives a home printer | [DEVICE-VERIFICATION.md](DEVICE-VERIFICATION.md) | A provisional, unmeasured number. Only paper can measure it |
| ☐ | Confirm `f*` reaches the PDF file, and that AA is ignored by the PDF backend | [SUPPLIES-SPEC.md](design/SUPPLIES-SPEC.md) | *"Sourced-but-unmeasured."* Needs the instrumented run above |

---

## Standing constraints I observe

Recorded here so you can hold me to them:

- **Never touched:** `README.md` · `docs/RESEARCH.md` · `gradle.properties` · `37596.jpg` · `acdec/`
- **Commits only when you ask.**
- **I never self-approve** — every substantive change goes to an independent Review Agent first.
