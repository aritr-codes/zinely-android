# CI-14 · Motion-and-haptics baseline recording protocol

**Milestone** C0 · **Inventory item** [CI-14](../V1-CONFORMANCE-INVENTORY.md) · **Kind** documentation
(preparation) · **Status** protocol ready — baseline **not yet recorded**

> **What this document is.** A turnkey protocol for the on-device session that records Zinely's motion
> and haptics baseline, plus the fill-in record template that session completes. **This document decides
> no duration, no easing, and no band.** It enumerates what to measure (from the code as it ships today),
> how to measure it on a device, and where the owner writes the numbers and the tie-break down.
>
> **What this document is not.** It is not the recording. The recording is an owner-plus-device task:
> only a real measurement may set a number, per the authorities below. Reading a value out of this
> document into a code change would repeat exactly the mistake CI-14 exists to prevent.

---

## 1. Why this gate exists

Three documents describe Zinely's timing and **disagree three ways**. Until a real device says which is
right, changing any motion duration is changing it against a tie-break nobody has taken.

- [DESIGN-LANGUAGE §10](../design/DESIGN-LANGUAGE.md) — three duration ranges (`~100–150` /
  `~200–300` / `~300–400 ms`), a gentle ease-out, and a "tiny 3–5% overshoot" for settles.
- The frozen HTML and [`ZinelyMotion.kt:15-19`](../../feature/editor/src/main/kotlin/com/aritr/zinely/ui/theme/ZinelyMotion.kt)
  ship **two** values — `--fast` 130 ms, `--base` 230 ms — on **one** easing curve,
  `cubic-bezier(.2,.7,.3,1)`.
- [ZINELY-DESIGN-SYSTEM §3.8](../ZINELY-DESIGN-SYSTEM.md) names **three bands** — *Instant · Brief ·
  Deliberate* — with **no numbers**, and [validation A-4](../ZINELY-DESIGN-SYSTEM-VALIDATION.md#a-4)
  proposes a **fourth** ("Underway", for real work with a real, cancellable duration).

The gate, in the words of the authorities:

- [§3.8 `> Open:`](../ZINELY-DESIGN-SYSTEM.md) — *"the existing durations and easings in DESIGN-LANGUAGE
  §10 remain the implementation of these bands until a motion baseline is recorded on device. That
  recording is a **precondition** for changing any of them."*
- [ZINELY-DESIGN-SYSTEM §15, open item 2](../ZINELY-DESIGN-SYSTEM.md) — *"§3.8's bands have no numbers
  until motion is recorded on a device. The existing durations stand until then."*
- [V1-DESIGN-REFINEMENT, "Where this document stops"](../V1-DESIGN-REFINEMENT.md) — *"It does not
  specify motion timings for animations nobody has recorded… Record first."*
- [CLAUDE.md · Device Verification, Pass 1](../../CLAUDE.md#device-verification-mandatory) — read the
  **platform's own state**, not the framework's; the number that governs is what the device shows, not
  what a constant claims.

---

## 2. The disagreement to adjudicate (fill the tie-break in §7)

The code already ships **more than two** de-facto durations: the two `ZinelyMotion` tokens (130 / 230),
plus hardcoded screen/reveal timings that no token covers (act change 340; fold reveal 320–950; shelf
entrances 700–820; sweep 1500). The measurement's job is to let the owner say which of the three
descriptions the app **actually** implements, and therefore which becomes canonical.

| Named band (§3.8) | DESIGN-LANGUAGE §10 range | `ZinelyMotion` / HTML today | Where it lives in code (§4) | **Measured (fill §6)** | **Canonical after tie-break (fill §7)** |
|---|---|---|---|---|---|
| **Instant** — touch ack, state flips | `~100–150 ms` | `--fast` = **130** | ZButton press; ZToast; ZSnackbar | `____ ms` | `____` |
| **Brief** — settling, tray arriving | `~200–300 ms` | `--base` = **230** | ZSheet; ShelfCover lift; Saved✨ | `____ ms` | `____` |
| **Deliberate** — the ending reveal only | `~300–400 ms` (screen) | *no token* (340/320–950 hardcoded) | ProofScreen act change; ProofFold | `____ ms` | `____` |
| **Underway** *(A-4 proposal — a fourth band?)* | *not in §10* | *no token* (1500 sweep loop) | ZSweep; ShelfStates entrance | `____ ms` | keep 3 bands / adopt 4? `____` |
| **Easing** | gentle ease-out + 3–5% settle overshoot | `cubic-bezier(.2,.7,.3,1)`, **no overshoot** | `ZinelyEasing` | *(describe curve/overshoot observed)* | `____` |

> The measurement does not by itself decide the band count. It supplies the evidence; the owner records
> the ruling in §7 and, only then, a follow-up change may touch a constant.

---

## 3. Session preconditions

- A **release** (or release-parity) build of the current `main`, so timings are not debug-slowed.
- Developer options reachable (for the animator-scale cross-check).
- A capture method that yields **per-frame** timing — screen recording at a **known, fixed fps**, and/or
  `dumpsys gfxinfo` frame stats (see §5).
- The four surfaces that need a specific app state reached before capture: a **selected photo** (Reframe
  chip + gestures), the **Proof** screen (act change + fold reveal), an **empty shelf / loading shelf**
  (ShelfStates, ZSweep), and a **forced save failure** (EditorSaveFailure banner) — see each row in §4.

---

## 4. What to measure — the animated surfaces that actually ship

Enumerated from the code on `main`. Each row: the surface, its code site, the value it uses **today**
(token or hardcoded), its easing, its reduced-motion behaviour, and whether it is a 1:1 direct
manipulation (relevant to the §8.4 interruptibility check). **Durations shown are the source constants,
not measurements** — the session records what the device actually renders in §6.

### 4.1 Instant-band candidates — acknowledgement of touch

| # | Surface | Code site | Uses today | Easing | Reduced motion |
|---|---|---|---|---|---|
| I-1 | **ZButton press** scale (primary) | [`ZButton.kt:122-124`](../../feature/editor/src/main/kotlin/com/aritr/zinely/ui/components/ZButton.kt) | `motion.fast()` = **130** | `ZinelyEasing` | → 0 ms |
| I-2 | **ZButton press** scale (secondary variant) | [`ZButton.kt:325-327`](../../feature/editor/src/main/kotlin/com/aritr/zinely/ui/components/ZButton.kt) | `motion.fast()` = **130** | `ZinelyEasing` | → 0 ms |

### 4.2 Brief-band candidates — settling · tray arriving · control revealing

| # | Surface | Code site | Uses today | Easing | Reduced motion |
|---|---|---|---|---|---|
| B-1 | **ZToast** enter (fade + slide-up) | [`ZToast.kt:56-59`](../../feature/editor/src/main/kotlin/com/aritr/zinely/ui/components/ZToast.kt) | `motion.fast()` = **130** | `ZinelyEasing` | → 0 ms |
| B-2 | **ZSnackbar** enter (fade + slide-up) | [`ZSnackbar.kt:75-78`](../../feature/editor/src/main/kotlin/com/aritr/zinely/ui/components/ZSnackbar.kt) | `motion.fast()` = **130** | `ZinelyEasing` | → 0 ms |
| B-3 | **ZSheet** scrim fade + sheet slide-in/out | [`ZSheet.kt:92-114`](../../feature/editor/src/main/kotlin/com/aritr/zinely/ui/components/ZSheet.kt) | `motion.base()` = **230** | `ZinelyEasing` | → 0 ms |
| B-4 | **ShelfCover** select-lift: tilt / rise / squeeze | [`ShelfCover.kt:127-143`](../../feature/editor/src/main/kotlin/com/aritr/zinely/feature/editor/ShelfCover.kt) | `motion.base()` = **230** | `ZinelyEasing` | → 0 ms |
| B-5 | **ShelfCover** first-appearance settle | [`ShelfCover.kt:81-85`](../../feature/editor/src/main/kotlin/com/aritr/zinely/feature/editor/ShelfCover.kt) | hardcoded **380** | `ZinelyEasing` | early-return (no anim) |
| B-6 | **"Saved ✨"** confirmation fade in/out | [`EditorSavedConfirmation.kt:85-89`](../../feature/editor/src/main/kotlin/com/aritr/zinely/feature/editor/EditorSavedConfirmation.kt) | hardcoded **150 in / 200 out** | **none** (default) | → `None` |
| B-7 | **Save-failure** banner fade in/out | [`EditorSaveFailure.kt:138-142`](../../feature/editor/src/main/kotlin/com/aritr/zinely/feature/editor/EditorSaveFailure.kt) | hardcoded **150 in / 200 out** | **none** (default) | → `None` |
| B-8 | **Reframe** coach-mark pulse ×2 | [`ReframeControls.kt:445-447`](../../feature/editor/src/main/kotlin/com/aritr/zinely/feature/editor/ReframeControls.kt) | hardcoded **300 up / 600 down** | **none** (default) | caller-gated off |

> **Audit flags for the owner (not decisions):** B-6/B-7/B-8 use **bare `tween`** with the platform
> default easing, *not* `ZinelyEasing`; B-5/B-6/B-7/B-8 use **hardcoded** durations that no `ZinelyMotion`
> token covers. Record whether each *feels* like the same band as its token siblings, or whether the
> spread (130 / 150 / 200 / 230 / 300 / 380) is one band, two, or noise.

### 4.3 Screen / act transitions — a de-facto third band with no token

| # | Surface | Code site | Uses today | Easing | Reduced motion |
|---|---|---|---|---|---|
| S-1 | **ProofScreen** act change (READ↔PRINT↔SHEET): fade + horizontal slide ±16 dp | [`ProofScreen.kt:321-336`](../../feature/editor/src/main/kotlin/com/aritr/zinely/feature/editor/ProofScreen.kt); const `PROOF_ACT_MILLIS = 340` at [`:127`](../../feature/editor/src/main/kotlin/com/aritr/zinely/feature/editor/ProofScreen.kt); `slidePx` = 16 dp at [`:203`](../../feature/editor/src/main/kotlin/com/aritr/zinely/feature/editor/ProofScreen.kt) | hardcoded **340** | `ZinelyEasing` | → 0 ms |
| S-2 | **Nav** transitions Home → Editor → Proof | [`ZinelyNavHost.kt:64-97`](../../app/src/main/java/com/aritr/zinely/editor/ZinelyNavHost.kt) — **no** `enterTransition`/`exitTransition` set | navigation-compose **library default** | library default | library default |

> **S-2 is audit-incomplete — verify on device.** The nav graph specifies no Zinely transition, so
> Home/Editor/Proof push-pop uses the navigation-compose default (not a Zinely token, not reduced-motion
> aware here). Record the observed default duration/curve and whether it reads as consistent with S-1.

### 4.4 The Deliberate band — the one signature reveal (`ProofFold`)

This is the ending (§9.4) — the only place the Deliberate band is allowed to exist. Measure each beat and
the cadence between them.

| # | Beat | Code site | Uses today | Easing |
|---|---|---|---|---|
| D-1 | Fold-**diagram** step-in (per step) | [`ProofFold.kt:319-327`](../../feature/editor/src/main/kotlin/com/aritr/zinely/feature/editor/ProofFold.kt) | tween **320** | `ZinelyEasing` |
| D-2 | **Cover** swing shut (−148°→6°→0°) | [`ProofFold.kt:376-390`](../../feature/editor/src/main/kotlin/com/aritr/zinely/feature/editor/ProofFold.kt) | keyframes **950** (6° overshoot at 570) | `ZinelyEasing` |
| D-3 | Book **settle** onto desk | [`ProofFold.kt:393-397`](../../feature/editor/src/main/kotlin/com/aritr/zinely/feature/editor/ProofFold.kt) | tween **550** | `ZinelyEasing` |
| D-4 | **Shelf-line** draw | [`ProofFold.kt:398-402`](../../feature/editor/src/main/kotlin/com/aritr/zinely/feature/editor/ProofFold.kt) | tween **600** | `ZinelyEasing` |
| D-5 | **Heading** arrive | [`ProofFold.kt:403-407`](../../feature/editor/src/main/kotlin/com/aritr/zinely/feature/editor/ProofFold.kt) | tween **420** | `ZinelyEasing` |
| D-6 | **Paragraph** arrive | [`ProofFold.kt:408-410`](../../feature/editor/src/main/kotlin/com/aritr/zinely/feature/editor/ProofFold.kt) | tween **420** | `ZinelyEasing` |
| D-7 | **Beat cadence** (delays between beats) | driver [`ProofScreen.kt:249-261`](../../feature/editor/src/main/kotlin/com/aritr/zinely/feature/editor/ProofScreen.kt); const `CLIMAX_BEAT_DELAYS = [980, 200, 260, 260, 300]` at [`:121`](../../feature/editor/src/main/kotlin/com/aritr/zinely/feature/editor/ProofScreen.kt) | hardcoded array | — |

> D-2's `6°` overshoot is the only place the "3–5% settle overshoot" of DESIGN-LANGUAGE §10 appears —
> record whether the reveal reads as "a moment watched on purpose" (§3.8 Deliberate) and whether it is
> the **only** surface that overshoots.

### 4.5 Entrance / loading — the A-4 "Underway" candidates

| # | Surface | Code site | Uses today | Easing | Reduced motion |
|---|---|---|---|---|---|
| U-1 | **ShelfStates** entrance: score / ghost reveal | [`ShelfStates.kt:447-464`](../../feature/editor/src/main/kotlin/com/aritr/zinely/feature/editor/ShelfStates.kt) | tween **820 / 700** | `ZinelyEasing` | pre-completed to final |
| U-2 | **ZSweep** loading shimmer (looping) | [`ZSweep.kt:36-45`](../../feature/editor/src/main/kotlin/com/aritr/zinely/ui/components/ZSweep.kt) | `InfiniteRepeatable` tween **1500** | `ZinelyEasing` | → static band (no loop) |

> U-2 is the only **looping** animation and the clearest test of the A-4 "Underway"/progress question:
> record whether it reads as honest progress or as decoration, and confirm the reduced-motion static
> fallback (§8.6).

### 4.6 Direct-manipulation gestures — no tween, 1:1 finger (the §8.4 interruptibility subjects)

These have no duration constant; they track the pointer frame-by-frame. What the session records is
**interruptibility from current position and velocity** (§8.4) and whether release **settles** or snaps
(DESIGN-LANGUAGE §10: *"releasing settles with a soft ease-out"*).

| # | Gesture surface | Code site | Note |
|---|---|---|---|
| G-1 | Editor pan / zoom / element move | [`EditorGestures.kt:60-118`](../../feature/editor/src/main/kotlin/com/aritr/zinely/feature/editor/EditorGestures.kt) (custom `pointerInput`, touch-slop gate) | Live 1:1; ephemeral gesture frame. |
| G-2 | Resize handles drag | [`ResizeHandles.kt:148-175`](../../feature/editor/src/main/kotlin/com/aritr/zinely/feature/editor/ResizeHandles.kt) (`detectDragGestures`) | "preview equals commit" — verify. |
| G-3 | Reframe pan / zoom within frame | [`ReframeOverlay.kt:99-105`](../../feature/editor/src/main/kotlin/com/aritr/zinely/feature/editor/ReframeOverlay.kt) (`detectTransformGestures`, FILL fit only) | Static for non-FILL fits. |

> **Audit-incomplete — verify on device:** no fling/decay/settle-on-release animation was found for
> G-1/G-2/G-3. DESIGN-LANGUAGE §10 promises a soft ease-out on release. Record whether release **snaps
> instantly** (spec gap) or settles, and whether a mid-gesture reversal returns from current
> position/velocity (§8.4) rather than completing then reversing.

---

## 5. How to measure on device

### 5.1 Duration + easing (per animated surface, §4.1–4.5)

1. **Screen recording at a known fps.** Record at a fixed, known frame rate (e.g. `adb shell
   screenrecord --bit-rate 20000000 /sdcard/cap.mp4`; note the panel's actual refresh rate — see §5.4).
   Step the video frame-by-frame; **count frames** from first visible movement to rest.
   `duration_ms ≈ frames × (1000 / fps)`. Record the frame count, not just the derived ms.
2. **Cross-check with frame stats.** `adb shell dumpsys gfxinfo <package> framestats` (or Perfetto /
   `--jank`) over a single triggered animation gives per-frame timestamps for the same interval; use it
   to confirm the frame-count reading and to catch dropped frames that would inflate a video count.
3. **Easing character.** From the frame-by-frame position samples, note the shape: decelerating
   (ease-out), symmetric, linear, or overshoot-and-settle — and for D-2 specifically, the overshoot
   magnitude (the spec's "3–5%" claim).
4. **Interruptibility (§8.4, and G-1/G-2/G-3).** Start the animation/gesture, then reverse or abandon it
   mid-flight. Record whether it returns from its current position/velocity or completes-then-reverses.

### 5.2 Reduced-motion cross-check (animator scale 0)

Set **Developer options → Animator duration scale → Off** (equivalently `adb shell settings put global
animator_duration_scale 0`). Re-trigger each §4 surface and confirm it degrades to an **instant,
already-correct static state** (§8.6 / §11), that no information is lost, and that ZSweep (U-2) stops
looping and shows its static band. Note any surface that still animates or that *loses* a signal when
motion is off.

### 5.3 Haptic-event audit (the four verbs — §6.3)

Zinely defines exactly four verbs
([`ZinelyHaptics.kt:24-36`](../../feature/editor/src/main/kotlin/com/aritr/zinely/ui/theme/ZinelyHaptics.kt)):
`Tick [8]`, `Snap [6,20,10]`, `Boundary [24]`, `Success [12,30,12,30]`. Reduced motion silences all four
([`ZinelyHaptics.kt:55-57, 86-90`](../../feature/editor/src/main/kotlin/com/aritr/zinely/ui/theme/ZinelyHaptics.kt)).
For each firing site in the table below: trigger it on a device **with a vibrator**, confirm the verb
fires, and judge — against DESIGN-LANGUAGE §11 (*"No: … routine button taps … navigation"*) — whether
the triggering event is **physical-feeling** or a **routine tap / navigation**. Mark the latter with a
`⚠` in the record; **do not remove any haptic** — that is a later, adjudicated change.

| Verb | Fires at | Trigger to reproduce | Physical event? |
|---|---|---|---|
| **Success** | fold reveal / export landed — [`ProofScreen.kt:255`](../../feature/editor/src/main/kotlin/com/aritr/zinely/feature/editor/ProofScreen.kt) | Finish a fold in Proof | — |
| **Snap** | paper chosen [`ShelfSheets.kt:90`](../../feature/editor/src/main/kotlin/com/aritr/zinely/feature/editor/ShelfSheets.kt); sort applied `:211`; duplicate `:248` | Pick paper / sort / duplicate in a sheet | — |
| **Boundary** | delete [`ShelfSheets.kt:253`](../../feature/editor/src/main/kotlin/com/aritr/zinely/feature/editor/ShelfSheets.kt); card long-press actions [`ShelfCard.kt:157`](../../feature/editor/src/main/kotlin/com/aritr/zinely/feature/editor/ShelfCard.kt) | Delete a zine / long-press a card | — |
| **Tick** | detent [`ShelfSheets.kt:316`](../../feature/editor/src/main/kotlin/com/aritr/zinely/feature/editor/ShelfSheets.kt); fold act ticks [`ProofScreen.kt:227,233,241,259`](../../feature/editor/src/main/kotlin/com/aritr/zinely/feature/editor/ProofScreen.kt) | Fold act steps | — |
| **Tick** ⚠? | **card open = navigation** [`ShelfCard.kt:156`](../../feature/editor/src/main/kotlin/com/aritr/zinely/feature/editor/ShelfCard.kt) | Tap a zine card to open the editor | `____` |
| **Tick** ⚠? | **routine buttons** — Proof Print/Save/Share/Send [`ProofPrint.kt:160,179,187,213,234,239`](../../feature/editor/src/main/kotlin/com/aritr/zinely/feature/editor/ProofPrint.kt); Make-another / Back-to-bench [`ProofScreen.kt:384,385`](../../feature/editor/src/main/kotlin/com/aritr/zinely/feature/editor/ProofScreen.kt); Home sort/create/action [`HomeScreen.kt:207,235,248,259`](../../feature/editor/src/main/kotlin/com/aritr/zinely/feature/editor/HomeScreen.kt) | Tap each button | `____` |

> **Platform (non-Zinely) haptic to note:** the inline text editor fires the Compose platform
> `HapticFeedbackType.TextHandleMove`, not a Zinely verb —
> [`TypeBar.kt:123`](../../feature/editor/src/main/kotlin/com/aritr/zinely/feature/editor/TypeBar.kt).
> Record it for completeness; it sits outside the four-verb vocabulary.

### 5.4 Read the platform's own state, not the constant

Per [CLAUDE.md Pass 1](../../CLAUDE.md#device-verification-mandatory): the governing number is what the
panel renders, which depends on the **actual refresh rate** (60/90/120 Hz changes the ms-per-frame in
§5.1) and on **release vs debug** build speed. Record the refresh rate the panel was actually running at
during capture, not the display's maximum.

---

## 6. RECORD TEMPLATE — the owner fills this during the session

> Complete every blank. Leave a surface's row blank only if it could not be reached, and write why.
> This filled section, dated and committed, **is** the CI-14 baseline.

### 6.1 Session identity

| Field | Value |
|---|---|
| Date recorded | `____-__-__` |
| Recorder | `____` |
| Device model | `____` |
| Android / OS build | `____` |
| Panel refresh rate during capture (Hz) | `____` |
| App `versionName` / `versionCode` | `____` / `____` |
| Build variant (release / release-parity) | `____` |
| Capture method(s) used (§5.1) | `____` |
| Recording fps | `____` |

### 6.2 Per-surface durations (from §4)

| # | Surface | Constant today | Frames counted | **Measured ms** | Easing observed | Interruptible (§8.4)? | Reduced-motion OK? | Notes |
|---|---|---|---|---|---|---|---|---|
| I-1 | ZButton press (primary) | 130 | `__` | `__` | `__` | `__` | `__` | `__` |
| I-2 | ZButton press (secondary) | 130 | `__` | `__` | `__` | `__` | `__` | `__` |
| B-1 | ZToast enter | 130 | `__` | `__` | `__` | `__` | `__` | `__` |
| B-2 | ZSnackbar enter | 130 | `__` | `__` | `__` | `__` | `__` | `__` |
| B-3 | ZSheet scrim + sheet | 230 | `__` | `__` | `__` | `__` | `__` | `__` |
| B-4 | ShelfCover select-lift | 230 | `__` | `__` | `__` | `__` | `__` | `__` |
| B-5 | ShelfCover settle | 380 | `__` | `__` | `__` | `__` | `__` | `__` |
| B-6 | "Saved ✨" fade | 150/200 | `__` | `__` | `__` (no ZinelyEasing) | `__` | `__` | `__` |
| B-7 | Save-failure banner | 150/200 | `__` | `__` | `__` (no ZinelyEasing) | `__` | `__` | `__` |
| B-8 | Reframe coach pulse | 300/600 | `__` | `__` | `__` (no ZinelyEasing) | `__` | `__` | `__` |
| S-1 | Proof act change | 340 | `__` | `__` | `__` | `__` | `__` | `__` |
| S-2 | Nav Home/Editor/Proof | *library default* | `__` | `__` | `__` | `__` | `__` | audit-incomplete |
| D-1 | Fold diagram step-in | 320 | `__` | `__` | `__` | `__` | `__` | `__` |
| D-2 | Cover swing shut | 950 | `__` | `__` | overshoot? `__` | `__` | `__` | `__` |
| D-3 | Book settle | 550 | `__` | `__` | `__` | `__` | `__` | `__` |
| D-4 | Shelf-line draw | 600 | `__` | `__` | `__` | `__` | `__` | `__` |
| D-5 | Heading arrive | 420 | `__` | `__` | `__` | `__` | `__` | `__` |
| D-6 | Paragraph arrive | 420 | `__` | `__` | `__` | `__` | `__` | `__` |
| D-7 | Beat cadence | [980,200,260,260,300] | `__` | `__` | — | — | `__` | `__` |
| U-1 | ShelfStates entrance | 820/700 | `__` | `__` | `__` | `__` | `__` | `__` |
| U-2 | ZSweep shimmer (loop) | 1500 | `__` | `__` | `__` | — | `__` (static?) | `__` |

### 6.3 Gesture interruptibility (from §4.6)

| # | Gesture | 1:1 with finger? | Release settles or snaps? | Mid-gesture reversal returns from position/velocity (§8.4)? | Notes |
|---|---|---|---|---|---|
| G-1 | Editor pan/zoom/move | `__` | `__` | `__` | `__` |
| G-2 | Resize handles | `__` | `__` | `__` | `__` |
| G-3 | Reframe pan/zoom | `__` | `__` | `__` | `__` |

### 6.4 Haptic audit (from §5.3)

| Verb @ site | Fired? | Event felt physical or routine? | ⚠ non-physical (§11)? | Notes |
|---|---|---|---|---|
| Success @ ProofScreen:255 | `__` | `__` | `__` | `__` |
| Snap @ ShelfSheets:90/211/248 | `__` | `__` | `__` | `__` |
| Boundary @ ShelfSheets:253 / ShelfCard:157 | `__` | `__` | `__` | `__` |
| Tick @ fold acts (ProofScreen) | `__` | `__` | `__` | `__` |
| Tick @ card open = navigation (ShelfCard:156) | `__` | `__` | `__` | `__` |
| Tick @ Proof/Home routine buttons | `__` | `__` | `__` | `__` |
| Platform TextHandleMove @ TypeBar:123 | `__` | `__` | `__` | `__` |

### 6.5 Reduced-motion cross-check (§5.2)

| Check | Pass? | Notes |
|---|---|---|
| Every §4 surface degrades to instant, already-correct static state | `__` | `__` |
| No information is lost when motion is off (§11) | `__` | `__` |
| ZSweep (U-2) stops looping, static band shown | `__` | `__` |
| All four haptic verbs silenced under reduced motion | `__` | `__` |

---

## 7. The three-way tie-break — the owner's ruling (fill after §6)

Using the measured evidence in §6, record the adjudication. **This is the only place a number becomes
canonical**, and it happens here on measured grounds, not in this protocol.

| Question | Ruling (owner) | Evidence (§6 rows) |
|---|---|---|
| How many bands does the app actually implement — 2 (fast/base), 3 (+screen/Deliberate), or 4 (+Underway per A-4)? | `____` | `____` |
| Canonical **Instant** duration | `____ ms` | `____` |
| Canonical **Brief** duration | `____ ms` | `____` |
| Canonical **Deliberate** duration(s) — is the fold a single band or a scored sequence? | `____` | `____` |
| Adopt A-4 **"Underway"** band? If so, its duration/cancellation rule | `____` | `____` |
| Canonical **easing** — keep `cubic-bezier(.2,.7,.3,1)`; is the 3–5% settle overshoot real (D-2) or absent elsewhere? | `____` | `____` |
| Do the hardcoded off-token durations (B-5/6/7/8, S-1, D-*, U-*) fold into named bands or stay bespoke? | `____` | `____` |
| Haptic firings flagged ⚠ (routine taps / navigation) — keep, or file for an adjudicated removal? | `____` | `____` |
| Nav transition (S-2) — leave at library default or give it a Zinely token? | `____` | `____` |

> Any code change that follows this ruling (retokenising a duration, adding a band, removing a haptic)
> is a **separate** change with its own review and, where it is a decision with consequences, its own
> ADR. This document and its filled record do not authorise the edit; they are its precondition.

---

## 8. Acceptance

Per [CI-14 · Verify](../V1-CONFORMANCE-INVENTORY.md):

- The completed record (§6) is **dated**, names **device · OS build · APK version**, and is **committed
  under `docs/reviews/`** (this file, once §6–§7 are filled).
- The record's **commit date is earlier than** any commit that changes a duration constant
  (`ZINELY_FAST_MILLIS`, `ZINELY_BASE_MILLIS`, `PROOF_ACT_MILLIS`, `CLIMAX_BEAT_DELAYS`, or any hardcoded
  tween duration enumerated in §4). A duration change whose commit predates this record has changed a
  value against a tie-break nobody took — the exact CI-14 risk.
- Until §6–§7 are filled and committed, the existing durations stand ([§15 open item 2](../ZINELY-DESIGN-SYSTEM.md)).

---

## Appendix · Enumeration provenance

Surfaces were enumerated by auditing `main` for animation APIs (`AnimatedContent`, `AnimatedVisibility`,
`animate*AsState`, `Animatable`, `InfiniteTransition`, `tween`/`keyframes`) and gesture APIs
(`detectTransformGestures`, `detectDragGestures`, custom `pointerInput`), plus every `ZinelyHaptic.*`
firing site and the `LocalZinelyMotion` token definitions. Two surfaces are marked **audit-incomplete —
verify on device**: the nav transition (S-2, no Zinely transition specified) and gesture
release-settling (G-1/G-2/G-3, no decay/settle animation found). Line numbers reference `main` at the
time of writing and should be re-confirmed if the code has moved.
