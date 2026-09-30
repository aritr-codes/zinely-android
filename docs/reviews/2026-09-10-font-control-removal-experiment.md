# Selected-text Font-control removal experiment

**Status:** Owner accepted removal as a design judgment on 2026-09-11 after reviewing the prototype.
[ADR-115](../DECISIONS.md#adr-115) is the decision authority, supersedes the Font portion of OD-9, and waives
the cohort-study gate. Bench A24 is amended and frozen; implementation verification is pending. No cohort
result or measured usability benefit is claimed.

## Current implementation boundary

The canonical HTML now omits Font. The historical wrapper reconstructs the old disabled control only in A,
so the two original alternatives remain inspectable. The protocol below is retained as historical research
design, not as a still-required gate or a study that was conducted. Owner feedback was "looks good" followed
by explicit approval of the owner-led removal recommendation. Automated browser access was unavailable in
the VS Code session; HTTP availability and owner inspection are not keyboard or visual-parity certification.

## Question and boundary

The pre-A24 selected-text toolbar included an unavailable **Font** action because OD-9 explicitly ruled that the
frozen control must remain visible without inventing functionality. The proposed experiment asked one narrower question:
does temporarily removing that dead action improve first-time use without hiding or confusing the five live actions?

The experiment wrapper loads the canonical [`v21-bench.html`](../design/mockups/v21-bench.html) in both conditions.
Condition B filters only `Font` from the selected-text action list after every canonical amendment has loaded. It does
not edit the frozen HTML, delete the shared Font icon, implement font choice, or change Compose.

## Conditions

Serve the repository root over HTTP and open the same viewport in both conditions:

- **A — historical baseline:** `docs/design/experiments/v21-font-control-removal.html?v=0`. The wrapper reconstructs
  the disabled Font action from the pre-A24 builder.
- **B — no Font:** `docs/design/experiments/v21-font-control-removal.html?v=1`
- **Text-size stress:** append `&scale=1.8` to either URL. This is a web-layout approximation, not Android
  font-scale acceptance.

Use randomized, balanced A/B cohorts. Each first-time maker receives one scored condition from the same fresh state
and the same script; any later crossover is exploratory and must not enter the primary result. Avoid naming the
difference. Use a 360 CSS-pixel viewport for the narrow pass and the prototype's ordinary responsive presentation
for the default pass.

## Uncoached task

Ask a first-time maker to select an existing text element, then point to — without activating — the first control
they would use to:

1. edit its words;
2. change its size;
3. change its ink;
4. duplicate it;
5. delete the duplicate.

Record the first control chosen for each task, hesitation, spontaneous Font attempts, searching outside the toolbar,
and whether the maker believes font choice exists. Do not teach the toolbar during the task. This is a first-choice
discoverability comparison only: the canonical mockup does not implement every downstream action (notably Size), so
this prototype cannot validate action completion.

## Decision rule

Recruit at least 16 first-time makers: eight randomized to A and eight to B. Recommend removal only if at least three
of eight A makers show Font-specific harm — hesitation, spontaneous activation, or a false capability expectation —
while no more than one of eight B makers shows the same harm. For every live action, B's intended-first-choice count
must be no more than one maker below A's count; a larger drop is a material B regression. Do not authorize removal
from a single participant or anecdote.

Under the original protocol, absent Font-specific harm would have left OD-9 in force. A passing comparison would
still have required owner approval and an HTML amendment before Compose. ADR-115 instead records the explicit
owner-led decision; keyboard/accessibility focus-order, human TalkBack, narrow/default, 1.8× text, golden, and device
gates remain required.

## Original experiment verification limit

The repository artifact was inspectable and its exact A/B action lists mechanically checkable. No automated browser
was available in the authoring session. At that point the connected Samsung ran the unchanged release, not condition B.

## Implementation verification and review handoff (2026-09-11)

Branch: `editor/remove-unavailable-font`. [Draft PR #70](https://github.com/aritr-codes/zinely-android/pull/70).
Implementation commit: `88e9413`, test-import correction `173e85f`, pinned goldens `c48960d`.
Decision: [ADR-115](../DECISIONS.md#adr-115). Public APK unchanged.

- **Passed:** `tools/grun.sh ed --tests "*BenchContextBarTest" --tests "*BenchContextBarPlatformA11yTest"`:
  31 toolbar tests and four platform accessibility tests, zero failures. Includes 320dp/default and 320dp/1.8 text
  reachability, five live authored-text actions, blank-text guards, order, roles, and callback dispatch.
- **Passed, source-level only:** all 12 canonical HTML inline scripts and the wrapper script parse. Executing the
  actual toolbar builder and amendments produces the five-action A24 list and historical six-action list; A24 does
  not change Photo/Art lists. This does not certify browser rendering, keyboard behavior, or pixel parity.
- **Independent review:** accepted and fixed stale production comments, missing selected-text golden coverage, and
  non-vacuity of initial captures. `BenchTextGoldenTest` asserts exact order, absence of Font, enabled controls and
  actual per-control full-strength ink pixels before capturing light/dark 360dp frames. Review remains pending gates.
- **Passed:** pinned [golden recorder](https://github.com/aritr-codes/zinely-android/actions/runs/34625105273)
  on `173e85f`, artifact `10274078010`. Implementer and independent reviewer inspected the two new light/dark images:
  exactly five legible controls, centered pill, no clipping or overlap, distinct Delete tint. Only these two PNGs were
  imported; all existing baselines were preserved. The full recording contains unrelated pixel differences, which were
  not accepted or copied. CI on `c48960d` passed all three modules' `verifyRoborazziDebug` tasks with `--rerun-tasks`.
  The duplicate local `tools/grun.sh gold --no-daemon --console=plain` run remains in progress at this handoff;
  render and core-UI comparisons completed, editor suite is active. No local full-gold pass is claimed.
  The preceding recorder failed on an invalid top-level `assertDoesNotExist` import, corrected in `173e85f`.
- **Build passed; CI lint passed:** isolated QA `:app:assembleDebug` completed and the APK installed. The
  combined lint task crashed inside `AsyncExecutionService.getService` while analyzing the new test, not on a reported
  lint rule. The independent [CI run](https://github.com/aritr-codes/zinely-android/actions/runs/34625933344) on `c48960d`
  passed its Hilt graph, app lint, Android unit suites, and explicit golden verification; both PR checks are green on
  that code/golden commit. The local-only
  Gradle init changes application ID to
  `com.aritr.zinely.fontqa` and writes to ignored `build/font-qa-20260911/app`. No production build configuration or
  version is changed. A cross-drive KSP failure was corrected by using the project drive; a stale Gradle JAR lock
  required stopping the daemon and retrying serially.
- **Developer device observations:** Samsung SM-A176B / Android 16, density 420dpi, isolated debug QA app with
  unchanged version label `0.9.0-beta.4-r3`. Fresh platform trees show exactly five enabled/clickable Button nodes in
  order. Default targets are at least 131 x 131px (49.9dp); at system font scale 1.8, at least 131 x 165px. All five fit
  the physical device's approximately 411dp width. Separate automated tests cover 320dp, including enlarged-text scroll
  reachability; this is not a claim of a 320dp physical-device run.
- **Action checks on disposable QA text:** Edit changed the words; Size changed 12pt to 14pt; Ink applied Forest,
  visible in the enlarged screenshot; Duplicate produced two separate text nodes; Delete removed only the duplicate.
  The shared Add > Text entry remained present and usable.
- **Independent second visual reading passed:** reviewer inspected default and enlarged screenshots, found no
  first-choice ambiguity among the five tasks, and confirmed labels, urgency of Delete, spacing, and lack of clipping.
  This is a screenshot-based first-time reading, not a hands-on owner pass, spoken TalkBack, or HTML-render parity.
- **Native keyboard order passed using ADB-injected Tab:** fresh focused platform nodes followed Preview, Edit, Size,
  Ink, Duplicate, Delete, All pages. No Font stop. This checks focus navigation on Android, not a physical-keyboard
  peripheral or browser keyboard behavior. Fresh `tab-1.xml` through `tab-7.xml` preserve the observed sequence.
- **Owner/device safety:** release package `com.aritr.zinely` still reports last update `2026-09-10 13:55:00`; no owner
  data was cleared or edited. Font scale was restored and read back as `1.0`. QA remains separately installed for owner
  inspection. Samsung TalkBack version `16.2.00.13`; spoken output has not been certified.
- **Pending acceptance:** rendered frozen-HTML parity/browser keyboard check,
  owner hands-on reading and human TalkBack. No automatic browser is available in this VS Code session. The prototype
  approval does not substitute for these implementation checks.

Local diagnostic artifacts are under `C:/Users/HP/AppData/Local/Temp/zinely-font-qa-20260911/`: `selected.png`,
`enlarged.png`, and fresh XML captures `blank-selected` (authored text despite filename), `size`, `size-changed`,
`duplicated`, `deleted-copy`, `edited`, and `enlarged`. These contain only disposable QA content. Committed golden hashes:
light `99e9098281c2d5dba987bd5d333fbb4108283a27cdef945e3bffb32df0147029`;
dark `a84fff59c272bbc6365b93b1f338c4d243ef376ac242887559d35b79a36bd22d`.
The two temporary screenshot files were removed from the phone's Downloads folder after successful laptop retrieval;
their local copies remain available. No owner artifact was removed.

Reviewer: validate the actual diff, not this summary. Confirm only the selected-text unavailable Font control was
removed; preserve blank guards, all live handlers, Add/Text icon, Photo/Art, document model and export. Inspect new
golden pixels and provenance, fresh device state, and owner acceptance. Do not merge while a required gate is pending.
No cohort study ran and no measured improvement is claimed.

Final independent review: GO for code, tests, documentation, and pinned images; no required implementation fix.
Overall merge NO-GO pending owner hands-on/TalkBack and rendered HTML/device parity/browser keyboard acceptance.
The local lint-engine crash and unfinished duplicate local golden run do not replace or negate the equivalent green
pinned-CI checks. PR #70 stays draft and unmerged. Next step is owner acceptance, not another product change.

## Settlement on current main (2026-09-29)

The owner accepted the implementation on 2026-09-29; see [ADR-115 Acceptance](../DECISIONS.md#adr-115-acceptance).
This section records the evidence behind that acceptance. It supersedes the pending-gate lines above; those
lines are kept as the 2026-09-11 record.

### Merging main

The branch head `f68e9c5` was 79 commits behind `origin/main` (`b5c6d55`). Main was merged in with a normal
merge commit, `aad2084`, not rebased.

- **Conflicts.** Only `CHANGELOG.md` and `DECISIONS.md` conflicted, and both were append-only. Every line from
  main was kept. ADR-115 was inserted between ADR-114 and ADR-116.
- **Carried in the same commit.** These are main housekeeping, not PR #70's work:
  - The ADR-121 and ADR-122 index-row status cells were corrected to match what the repository records.
  - The ROADMAP section the branch had added duplicated main's "In development" entry, so it was dropped.
- **Scope.** Against main, the PR still touches only its own 15 files. Main had not changed
  `BenchContextBar.kt`, `EditorScreen.kt`, `Copy.kt`, `v21-bench.html` or the toolbar tests.
- **Review.** An independent review of the merge returned GO with no required fix. It left one follow-up for
  main: the ADR-121 body still says the implementation is owed, which predates this merge.

### Automated checks on `aad2084`

- **Before the run.** `git status --porcelain` was empty, both before and after the run.
- **Full local suite.** 2,390 tests, 0 failures, 0 errors, 0 skipped, run with
  `tools/grun.sh test :app:lintDebug :app:checkDependencyAllowlist :app:assembleRelease`.
  - `:feature:editor` ran 1,024 of them.
  - Lint, the dependency allow-list and the release build passed.
- **Goldens.** `tools/grun.sh gold` (`--rerun-tasks`) passed for all three modules, and no image was re-recorded.
- **CI.** [Run 36546365541](https://github.com/aritr-codes/zinely-android/actions/runs/36546365541) passed
  both jobs, including the Roborazzi verify step.

### Rendered HTML parity and keyboard

The canonical `v21-bench.html`, with A24, was opened from disk in Microsoft Edge, driven by Playwright. The
first text element was clicked.

- **Actions.** In light and dark, `#ctx` held exactly Edit, Size, Ink, Duplicate, Delete, all with icons.
  `toolsForBeforeA24('text')` returned the same list with Font in second place.
- **Other kinds.** A24 leaves the Photo and Art lists unchanged.
- **Geometry.** The bar is centred on the phone: centre 450 = 450. The five controls are each 50 x 47 CSS px
  with 1 px gaps. Delete carries the danger colour: `rgb(169,48,61)` in light, `rgb(255,156,164)` in dark.
- **Script errors.** None.
- **Keyboard.** Tab from the selected text reaches Edit, Size, Ink, Duplicate, Delete, then leaves the bar.
  That is five stops, with no Font. Shift+Tab from Delete runs Duplicate, Ink, Size, Edit.
- **Against the device.** Compared with the device screenshot in dark and the committed light and dark goldens:
  - Order, labels, pill shape and centring, even spacing and the Delete tint correspond.
  - The icons differ: the HTML draws stroke icons, while Compose uses Material filled icons. The Compose bar
    already used `Icons.Filled.*` at `ad86586`, so this predates A24 and is not a PR #70 divergence.

### Device

The device was the SM-A176B on Android 16 (serial `RZCYA1VBQ2H`).

- **QA build.**
  - `:app:assembleDebug` from `aad2084`, with a local-only init script that sets the application ID to
    `com.aritr.zinely.fontqa` through `androidComponents.onVariants`.
  - The first attempt set the ID through the DSL, which the build script then overrode. That APK still had the
    release ID. It was caught by reading the package with `aapt` and deleted before any install.
  - The installed APK is package `com.aritr.zinely.fontqa`, with provider authorities under the same prefix.
    Its version label is `0.9.0-beta.5` (versionCode 10). SHA-256 is
    `79f82827cd520907189cfa2b7b516826f3d19b201e5fc5c1461cc2395ae7afe0`.
  - The release install `com.aritr.zinely` (beta.5) kept `lastUpdateTime 2026-09-29 12:09:04` throughout.
- **Platform tree with authored text selected.** Exactly five `android.widget.Button` nodes, enabled and
  clickable: Edit, Size, Ink, Duplicate, Delete. No node's text or content description contains "font". The
  same held at system font scale 1.8: all five on screen, the smallest target 131 x 165 px, no label clipped.
- **Actions, once each, on disposable QA text:**
  - Duplicate made a second text node.
  - Size opened the type panel, and Larger moved the copy from 14 to 16 pt.
  - Ink changed the copy from Forest to Brick.
  - Edit opened inline editing. The typing row was unchanged: Ink, "Align, size & more after Done", Done.
    It committed "QATiny paper test PR70".
  - Delete removed only the copy.
  - Undo and Redo then left the page with only its original text.
- **Add > Text.** The sheet lists "Text. Type words onto the page" with its unchanged `A` icon, and it
  creates a text box in edit mode.
- **Blank text.** No way was found to leave a blank text box selected on this build:
  - a new empty box, and one holding only spaces, are removed on Done;
  - an existing text erased to nothing is removed on Done;
  - tapping a blank box in edit mode reopens the editor.

  The disabled Size, Ink and Duplicate state, with "Type something first", therefore rests on
  `BenchContextBarTest` and `BenchContextBarPlatformA11yTest`.

### Owner TalkBack listen

Samsung TalkBack 16.2.00.13, turned on over adb. The device's original setting was accessibility off, and it
was restored afterwards.

- **What the owner reported.** With authored text selected at font scale 1.0:
  - swiping right from Edit gave **Edit → Size → Ink → Duplicate → Delete**, with no Font item;
  - swiping left from Delete gave **Delete → Duplicate → Ink → Size → Edit**;
  - a blank text element was announced as **"Selected, empty text, button."** That is the element's own
    announcement. The owner did not report the bar's disabled Size/Ink/Duplicate wording for it, so that
    wording is not owner-listened. TalkBack may reach a selected blank box that adb taps could not (see
    Blank text above); that route was not explored.
- **The owner's verdict:** "These are working as intended."
- **Not listened to in this first listen.** The 1.8 traversal and Add > Text under TalkBack. The owner accepted
  the implementation without them at the time. Both were listened to afterwards; see the follow-up below.
- **Font scale afterwards.** It read 0.9, set by the owner, so it was left as found.

### Follow-up TalkBack listens at font scale 1.8 (2026-09-29 and 2026-09-30)

These close the two checks the first listen left out. The device was the same SM-A176B on Android 16, with
Samsung TalkBack 16.2.00.13. The QA build `com.aritr.zinely.fontqa` was rebuilt from the final PR head
`bdd5990`. Every task was up to date and the APK's SHA-256 matched the `aad2084` build
(`79f82827…`), because the commits in between are docs only. The release install kept
`lastUpdateTime 2026-09-29 12:09:04`. Font scale was 1.8 and TalkBack was on.

- **Authored-text traversal, 2026-09-29.** With "QATiny paper test" selected, the owner reported:
  - swiping right gave **Edit → Size → Ink → Duplicate → Delete**;
  - swiping left gave **Delete → Duplicate → Ink → Size → Edit**, the exact reverse;
  - there was no Font stop and no extra control, and nothing was clipped or unreachable.
- **An unclear first attempt at Add > Text, 2026-09-29.** The owner reported "not selected text". The screen
  then showed the Add sheet closed, with an empty text box open for editing, so the words most likely came
  from a text element on the page, not from the sheet. It was not counted, and the check was repeated.
- **Add > Text, 2026-09-30.** The Add sheet was opened before TalkBack was turned on, so the listen began
  inside it. The owner reported:
  - the heading was read as "Add to your page";
  - the Text option was announced as **"Text. Type words onto the page button. Double tap to activate"**;
  - the Photo and Art options followed, each read with its description.

  Add > Text remained reachable, and choosing it still creates a text box: the owner heard the new box
  announced as "Selected, empty text, button" on 2026-09-29. Removing the selected-text Font action did
  not remove, rename or otherwise affect Add > Text.

Afterwards font scale was set to 1.0 and TalkBack turned off. Accessibility was restored to off with no
services. Stay-awake was left at 15.

### Local artefacts

These are under the session scratchpad and are not committed:

- `html-ctx-text-{light,dark}.png` and `html-phone-text-{light,dark}.png`, with `probe.json`;
- `dev/04-selected.png` (device, dark), with `dev/*.xml` platform dumps for each step;
- `dev/26-scale-sel.png` (font scale 1.8).

PR #70 is now ready for the owner's final review. It is not merged, and merging is the owner's call.
