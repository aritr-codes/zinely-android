# DEVICE-VERIFICATION.md — the working recipe

The **mechanics** of on-device verification: how to drive a phone over `adb`, how to read the platform
accessibility tree, and the environment traps that have each cost an hour.

This document owns the *how*. The **policy** — that every UI change gets two passes, what each pass asks,
and that a feature is accepted only when both succeed — lives in
[CLAUDE.md § Device Verification](../CLAUDE.md#device-verification-mandatory) and is not restated here.

> Written up in **A8** (accessibility infrastructure). Until then this recipe existed only in session
> memory, and CLAUDE.md's "the recipe … is in the device-verification notes" pointed at nothing a new
> reader could open — which meant the mandatory pass was, in practice, undocumented.

---

## 1. The reference device

| | |
|---|---|
| Device | Samsung SM-A176B (Galaxy A17 5G) |
| OS | Android 16 / API 36 |
| Screen | 1080×2340 @ 420dpi — **density 2.625**, so `dp = px ÷ 2.625` |
| TalkBack | Samsung TalkBack `16.2.00.13`, package `com.samsung.android.accessibility.talkback` |
| `adb` | `C:\Program Files\platform-tools` |

Record device, OS version, build — and TalkBack version whenever the pass touches accessibility — in the
verification report. A pass without them is not reproducible.

---

## 2. The accessibility tree — the highest-value artefact

```
MSYS_NO_PATHCONV=1 adb shell uiautomator dump /sdcard/ui.xml
MSYS_NO_PATHCONV=1 adb exec-out cat /sdcard/ui.xml | <your reader>
```

> ⚠ **`MSYS_NO_PATHCONV=1` belongs on BOTH lines, and the first one is the one everybody forgets.** Under Git
> Bash, MSYS rewrites the `/sdcard/…` argument of the **dump** command, so the file lands somewhere like
> `/Files/Git/sdcard/ui.xml` while your reader — correctly guarded — reads `/sdcard/ui.xml` and quietly
> returns **whatever an earlier session left there**. The device tells you, if you look: `UI hierchary dumped
> to: /Files/Git/sdcard/ui.xml`. C5's Pass 1 lost four readings to this and caught it only because the tree
> claimed page 8 was current while the screen showed page 5.
>
> Two habits make it self-detecting: use a **fresh unique filename** per dump, and **cross-check one fact
> against the screenshot** before trusting the rest. *A stale accessibility dump is worse than no dump,
> because it answers.*
>
> `adb pull` into the scratchpad and shell redirects (`> file`) have both been seen to write nothing here
> while reporting success — pipe `exec-out cat` straight into the reader instead, and check the byte count.

**Or never write a device file at all** — one command, no `MSYS_NO_PATHCONV`, no staleness class:

```
adb exec-out uiautomator dump /dev/tty 2>/dev/null > ui-<screen>.xml
```

`/dev/tty` is a device path MSYS does not rewrite, and `exec-out` carries the XML back on stdout, so the
dump cannot be a leftover from an earlier run: there is no file on the device to go stale. `2>/dev/null`
drops the `UI hierchary dumped to:` line the tool writes to stderr. Still check the byte count — an empty
capture is the one failure this form can still have. Used for the whole of the
[2026-08-10 V2.1 Library pass](reviews/2026-08-10-v21-library-device-verification.md).

**`stateDescription` is not in this dump's schema at all.** There is no `state-desc` attribute, so a control
whose current/selected state is carried by `stateDescription` cannot be verified on device this way. Use the
CI-26 Robolectric harness (`platformNode`), which reads the real `AccessibilityNodeInfo`, and record the device
item as *limited by the instrument* rather than as passed.

`uiautomator dump` returns the real `AccessibilityNodeInfo` tree — **the exact thing TalkBack consumes**.
Per node it gives `content-desc`, `class`, `clickable`, `focusable`, `checked`, `enabled`, and `bounds` in
real pixels, so touch-target sizes are *measured* rather than argued about.

**Why this and not a Compose semantics assertion.** Compose's test tree is *merged*; the platform tree is
not, and they disagree. A control can pass `onNodeWithContentDescription` while the platform exposes the
label and the click action on **different nodes** — and a control passed `assertIsNotEnabled` in a green
Robolectric suite while telling the platform it was enabled ([ADR-058](DECISIONS.md#adr-058) branch,
`ReframeControls.ZoomButton`). A `Role` reaches the platform as the node's `class`
(`Button` / `RadioButton` / `CheckBox`) — but **only when the control collapses to one node**; a role on a
node that merges child content arrives as `android.view.View` instead ([ADR-059](DECISIONS.md#adr-059)).

**Much of this now runs before a device is involved.** The CI-26 harness
(`com.aritr.zinely.ui.a11y.platformNode` / `platformTraversalStops`, in `:core:ui`'s test fixtures) reads
the same platform tree from Robolectric on the JVM. It does not replace this pass — hidden-API access,
real TalkBack behaviour and anything about *sound* are only available here — but a defect it can catch
should be caught there, in CI, not on a phone.

### What to check per node
`class` · `clickable` · `enabled` · `content-desc` · `bounds` (÷ 2.625 for dp; the floor is 48dp).

---

## 3. TalkBack

Enable:

```
adb shell settings put secure enabled_accessibility_services \
    com.samsung.android.accessibility.talkback/com.samsung.android.marvin.talkback.TalkBackService
adb shell settings put secure accessibility_enabled 1
```

Disable:

```
adb shell settings delete secure enabled_accessibility_services
adb shell settings put secure accessibility_enabled 0
```

> ⚠️ **Enabling TalkBack pops a first-run permission dialog** ("make and manage phone calls") which steals
> focus and then **silently swallows every subsequent tap**. Decline it. Never grant a permission on the
> owner's phone. If taps have mysteriously stopped working, screenshot first — this is usually why.

**TalkBack's spoken output cannot be captured.** Samsung TalkBack logs no utterances. The *structure* is
verifiable over `adb`; *what is said* still needs a human ear, and any claim about wording is a Pass-2
observation, not a machine result.

### 3.1 What the current build owes a human ear — pre-registered questions

*A listen pass is worth far more when the questions are written **before** it, so the answer cannot be
rationalised afterwards. These are open, and each names what changes if the answer goes the wrong way.*

1. **Is an import summary spoken TWICE?** ([D-081](design/V2-SPEC-DEFECTS.md#d-081-rulings) Q3.) Share two
   photos where one cannot be decoded. The sentence rides two channels on purpose — a `Toast` and
   `announceForAccessibility` — and AOSP shows `ToastPresenter` already emits the toast's own text as a
   `TYPE_NOTIFICATION_STATE_CHANGED` event, with nothing deduplicating it against the announcement's
   `TYPE_ANNOUNCEMENT`. **If it doubles, the `announce()` call is deleted** (it is deprecated in API 36
   anyway) and the toast carries it alone.
2. **Does `Copier` say which way it is set?** ([D-082](design/V2-SPEC-DEFECTS.md#d-082-rulings) Q4.) Select a
   photo, focus `Copier`, listen; tap; listen again. It should say "On" / "Off" as *state*, after the name.
   `uiautomator dump` **structurally cannot show `stateDescription`** (§2), so this is the only instrument
   that can check it. The visual half already passed on hardware.
3. **Does an import landing mid-transition get announced at all?** ([D-081](design/V2-SPEC-DEFECTS.md#d-081-rulings)
   Q9.) Share into the app while moving between the bench and the Proof. Both collectors are gated on
   `RESUMED` so exactly one speaks — but during the transition *neither* is resumed, and the flows are
   replay-free. Robolectric cannot land an emission in that window; a thumb can. Silence here is a known,
   accepted few-hundred-millisecond gap — the question is whether it is actually reachable by a human hand.
4. **Do two ink swatches both say "Ink"?** ([D-083](design/V2-SPEC-DEFECTS.md#d-083).) Open the ink popover
   and sweep the palette. One constant serves a maker ink *and* a neutral, and the verb that opened the
   popover is the same word. This is a shipped defect found by reading; confirm what it sounds like.

### 3.2 What the current build owes a printer

**The photocopier filter's entire claim is about ink on paper**, and no screen can close it
([ADR-106](DECISIONS.md#adr-106)). Print one page with a `Copier`-filtered photo on it and answer: do the
dots read as a photocopy, or as noise? Is 150 dpi too coarse — or too fine to survive a home printer, which
is the failure that would not show on a screen at all? A "too coarse" answer re-opens
[D-082](design/V2-SPEC-DEFECTS.md#d-082-rulings) Q1, which was affirmed only *provisionally* and on screen
evidence.

---

## 4. Reading app-private files

Needs the **debug** variant. The release build is not `debuggable`, so `run-as` fails even though both
variants are debug-*signed*.

```
adb install -r app/build/outputs/apk/debug/zinely-<version>-debug.apk
adb shell run-as com.aritr.zinely cat files/projects/<id>/document.json
```

`assembleDebug` + `install -r` **keeps app data** (same `applicationId`, no suffix, same key), so a
reproduction survives the reinstall. This is how the duplicate-element-id corruption was diagnosed.

`document.json.bak` sits beside the document and is the **only** recovery path for a broken project —
there is no UI for it.

---

## 5. Traps, all hit the hard way

- **Bash mangles `/sdcard/…` into a Windows path.** Use the PowerShell tool for `adb`, or write
  `//sdcard/…`.
- **`adb shell input tap` does not reach the editor canvas.** Element selection, tap-to-deselect and
  tap-away-to-commit all no-op; supply-tray and page-strip buttons work fine. Do **not** read a canvas tap
  doing nothing as an app defect without cross-checking a screenshot.
- **The inline text editor commits only on IME Done** (or real focus loss). `KEYCODE_ENTER` inserts a
  newline; `KEYCODE_BACK` only hides the keyboard. Tap the keyboard's Done key by pixel.
- **Scrollable rows hide controls.** The `EditorContextBar` transform row scrolls; "Text style" and
  "Delete" sit off-screen until it is swiped. A node clipped to e.g. `12×48dp` at a screen edge is scroll
  clipping, **not** a touch-target defect. *(Retired by the F-2 wrap — the row no longer scrolls. Kept
  because the reasoning generalises to any scrolling row.)*
- **A synthetic `am start … ACTION_SEND` is not a share, and it fails in both directions.** It cannot
  *grant* what a real share grants — `--grant-read-uri-permission` on a `content://media/…` URI still
  yields `SecurityException: com.aritr.zinely has no access to …` in the app's own log, because the shell
  cannot delegate a MediaStore read it holds by a different route. And it cannot *reproduce* what a real
  share does, because the share sheet adds `FLAG_ACTIVITY_NEW_TASK` while `am start -n` does not — which
  is precisely the flag that exposed the `singleTop` second-task defect ([ARCHITECTURE
  §8](ARCHITECTURE.md#8-navigation)). Drive the real sender: launch Gallery, open a photo, tap Share, and
  find the target in the chooser's own node dump (`uiautomator dump | grep -i zinely` — it may sit behind
  **More**). Then confirm the task count with `dumpsys activity activities | grep aritr`: **more than one
  `MainActivity` record is itself the bug.**
- **A one-pixel shortfall in a reported hit-rect is rounding, not a small target.** Compose reports the
  *touch bounds* around the content, not the content's own box. Odd-width content puts both edges on a
  half-pixel, and `getBoundsInScreen` rounds the two edges in **opposite** directions (`0.5` up, `48.5`
  down), so a genuine 48px span is reported as 47. At this device's density that is under 0.4dp. See
  `ZinelyV2ControlPlatformA11yTest`.

---

## 6. Cross-references

[CLAUDE.md § Device Verification](../CLAUDE.md#device-verification-mandatory) (the policy — two passes,
acceptance, what each pass asks) · [ADR-058](DECISIONS.md#adr-058) (the merged-vs-platform defect class) ·
[ADR-059](DECISIONS.md#adr-059) (Role → `android.view.View`) · [ADR-078](DECISIONS.md#adr-078) (the A8
accessibility foundation and the CI-26 harness's move into `:core:ui`) ·
[V1-CONFORMANCE-INVENTORY.md](V1-CONFORMANCE-INVENTORY.md) (CI-26/29/30/31/93).
