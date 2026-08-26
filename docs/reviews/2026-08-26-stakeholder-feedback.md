# Stakeholder feedback — launch-readiness pass

**Date received:** 2026-08-26  
**Launch target:** 2026-09-11  
**Source:** nine user-supplied WhatsApp screenshots in the local `feedback/` directory. The screenshots are
working evidence, may contain personal conversation context, and are intentionally not added to Git by this
review.

## Provenance limit

The screenshots do not identify an APK version, commit, or capture date. Findings below are therefore
cross-checked against the current checkout before being treated as current defects. A screenshot observation
is not proof that the same behavior remains in the release candidate.

## What the session established

The stakeholder described the app as **“amazing”**, **“great”**, and **“very fast.”** They successfully
discovered rotation and resize after experimenting. This is useful product evidence: direct manipulation is
working and responsive, but some of its capabilities are learned by accident rather than communicated by the
surface.

| Finding | Current-checkout reading | Launch disposition |
|---|---|---|
| The first-run sheet should visibly explain eight equal pages | **Already present in current code.** `ZineShelfEmpty.SheetIllustration` draws three vertical divisions plus one horizontal division, yielding eight panels; focused raster tests pin all four rules. Verify the release APK rather than changing the geometry from an unidentified build. | **Verify in RC; no code change yet** |
| Text needs centre alignment | **Capability already exists.** The Type bar offers left, centre and right alignment and reducer/model tests cover it. The report is a discoverability gap, not a missing formatter. | **P1 discoverability** |
| Font / Size / Ink look disabled while typing and should not be shown if unavailable | **Resolved by [D-108 / OD-52](../design/V2-SPEC-DEFECTS.md#d-108).** The typing row now contains live Ink, no fake Font/Size buttons, and the quiet cue `More styles after Done`; Ink commits the active draft before opening its tray. | **Closed — focused tests/goldens and normal/maximum-font Samsung passes complete** |
| Resize and rotation were initially assumed missing | **Capabilities exist and were later discovered.** Handles, gestures and accessible transform actions are implemented. | **P1 first-use guidance; protect responsiveness** |
| Edge handles were expected to crop like Canva | **Mental-model mismatch.** Zinely intentionally separates element resize from non-destructive photo `Reframe`; changing handles to crop would break transform consistency and accessibility parity. | **P1 teach `Reframe`; do not rewrite handles** |
| Page thumbnails should enlarge when tapped | The current eight-page navigator has a summoned all-pages grid, but the screenshots do not prove whether the stakeholder found or used it. | **Reproduce on RC; improve affordance only if still unclear** |
| Emoji should print | **Known fidelity limitation.** Unsupported emoji are saved and warned about rather than silently dropped. True print fidelity needs a deliberate vector-emoji/content strategy; it is not a safe deadline patch to the text renderer. | **Disclose for launch; post-launch feature unless scope is explicitly changed** |
| Preview animation is unnecessary | The screenshot does not identify which motion is meant. Proof has several distinct transitions and a reduced-motion path; removing one without reproducing the complaint would be speculative. | **Reproduce and measure; no blind removal** |
| Open the PDF after saving | **Current behavior is deliberate:** `Save PDF` writes to Downloads and keeps the maker in Proof for the fold hand-off; automatic `ACTION_VIEW` was retired. A user-controlled `Open PDF` action after success could satisfy inspection without hijacking the flow. | **P1 design amendment candidate** |

## Launch interpretation

The session does not justify a new editor architecture or a broad tutorial. It points to one coherent problem:
**Zinely's power is ahead of its discoverability.** The launch response should be small and contextual:

1. verify the first-run sheet in the actual release candidate;
2. replace or redesign the inert typing chips through the HTML-first process;
3. teach resize, rotate, alignment and Reframe at the moment each becomes relevant;
4. make the all-pages affordance and post-save outcome self-evident;
5. retain honest emoji limitations until output fidelity can be guaranteed.

The positive speed feedback is a constraint: no guidance change may make manipulation slower, noisier, or more
modal.
