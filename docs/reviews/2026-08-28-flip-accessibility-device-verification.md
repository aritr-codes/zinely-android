# Flip accessibility — device verification

**Date:** 2026-08-28  
**Device:** Samsung SM-A176B (`RZCYA1VBQ2H`), Android 16 / API 36, 1080 × 2340  
**Build:** `zinely-0.9.0-beta.2-debug.apk` from `feat/zine-backup-v2`

## Scope

This bounded follow-up verifies the WCAG 2 AA accessibility repair discovered while preparing the
broader Flip release pass. It does not replace the remaining Flip device gate recorded in the
[release-gap roadmap](2026-08-27-release-gap-roadmap.md).

## Finding and repair

The initial Samsung `uiautomator` tree exposed the two Flip-axis controls as clickable, checkable
`android.view.View` nodes with empty names. Their visible Compose child labels did not reach the
platform node that accessibility services operate.

`FlipChoice` now makes its actionable semantic owner explicit: concise axis name, button role,
checked state, click action, and test tag. The visual design and interaction remain the frozen
two-toggle tray; this is an accessibility implementation repair, not a design change.

## Verified evidence

1. On Samsung before the repair, `uiautomator dump` exposed both Flip-axis controls as unnamed,
   clickable, checkable `android.view.View` nodes. That is the concrete defect this follow-up fixes.
2. The patched `FlipChoice` now assigns the actionable semantic owner an explicit axis name, checked
   state, click action, and retained test tag.
3. `FlipTrayPlatformA11yTest` now asserts the Android platform node directly, instead of relying on
   the merged Compose tree that masked the defect.

## Automated evidence

- `:core:editor:test --tests FlipReducerTest`: 5 tests, 0 failures.
- `:feature:editor:testDebugUnitTest` for `EditorFlipA11yTest`, `FlipTrayTest`,
  `FlipTrayPlatformA11yTest`, and `ReframeFlipTest`: 7 tests, 0 failures.
- `FlipTrayPlatformA11yTest` asserts the real Android platform node has the name, toggle state,
  click affordance, and at least a 48dp target; it prevents the discovered unnamed-node regression.

## Remaining device gate — still open

The patched Samsung rerun could not be completed on August 28, 2026 because `adb devices` kept
reporting `RZCYA1VBQ2H` as `offline` even after `adb reconnect`, `adb kill-server`, and
`adb start-server`. No device accessibility setting was changed, so there is no setting to restore.

The following device scenarios therefore remain open and are not claimed by this note:

- Photo and Art horizontal/vertical toggle verification on the patched build.
- Undo/redo, reopen, Reframe, and Proof/PDF parity on the patched build.
- Large-font and TalkBack verification on the patched build.
- The required second-reader confirmation.
