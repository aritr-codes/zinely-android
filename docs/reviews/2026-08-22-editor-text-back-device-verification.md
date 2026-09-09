# Editor Add Text / Back — device verification

**Date:** 2026-08-22  
**Device:** Samsung SM-A176B (`RZCYA1VBQ2H`), Android 16  
**Build:** `zinely-0.9.0-beta.2-debug.apk`

This is a stabilization fix inside the frozen Bench interaction model. It introduces no new screen or
visual treatment.

## Finding

On the physical device, choosing **Add → Text** while another text element was selected could target the
wrong editing context. After the keyboard had been dismissed, Android Back could then leave the editor for
the shelf instead of dismissing the text surface. A cancelled fresh text action could also leave a blank
element behind.

## Pass 1 — developer verification

- Add → Text now places a reducer-minted blank element and opens that exact element's edit session in one
  reduction, including when an older text element is selected.
- On this device, the first Back hides the Samsung keyboard. The next Back cancels the text session and
  remains in the editor; it does not reach the shelf navigation callback.
- The fresh blank element is coalesced away and leaves no undo entry.
- The previously existing `Restored_Edit_0822` text remains visible and unchanged.
- After force-stop and cold launch, the original text remains and no blank text element reappears.
- The UIAutomator platform tree after cancellation contains `Preview`, `Add`, and
  `Text: Restored_Edit_0822`; it contains neither `Your shelf` nor `Empty text`.
- Recent logcat output contains no Zinely `AndroidRuntime` failure or matching crash/exception.

## Pass 2 — second-reader verification

An independent reader reviewed the post-cancellation device screenshot and behavior evidence. Result:
**PASS**. The maker remains in a calm, understandable editor state; the draft UI and temporary blank are
gone, the original text remains visible, and no stale edit affordance suggests accidental content loss.

## Automated and review evidence

- Focused reducer and editor-host regression tests: PASS.
- Full `:core:editor:test`: PASS.
- Full `:feature:editor:testDebugUnitTest --rerun-tasks`: PASS.
- Explicit `:feature:editor:verifyRoborazziDebug --rerun-tasks`: PASS; no golden was re-recorded.
- `:app:testDebugUnitTest`, `:app:lintDebug`, `:app:assembleDebug`, and `:app:assembleRelease`: PASS.
- Independent code/adversarial review: **GO**.

One combined all-gates Gradle invocation exhausted the configured daemon metaspace and lost a temporary
test-results file. The affected full feature suite and golden verification were rerun separately and both
passed; this was a runner-resource failure, not a test assertion failure.
