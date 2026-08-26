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
| Resize and rotation were initially assumed missing | **Resolved by [D-109 / OD-53](../design/V2-SPEC-DEFECTS.md#d-109).** The selected-object toolbar now names Move, Resize, Rotate, Layer and text Style directly; the first-use coach points to those controls without blocking the canvas. | **Closed — focused tests/goldens and Samsung verification complete** |
| Edge handles were expected to crop like Canva | **Mental-model mismatch.** Zinely intentionally separates element resize from non-destructive photo `Reframe`; changing handles to crop would break transform consistency and accessibility parity. | **P1 teach `Reframe`; do not rewrite handles** |
| Page thumbnails should enlarge when tapped | **Resolved by [D-065 / OD-54](../design/V2-SPEC-DEFECTS.md#d-065).** Tapping the navigator opens a three-column all-pages overview whose miniatures use the shared page renderer and therefore show the live page content; choosing one closes the overview on that page. | **Closed — focused tests/goldens and Samsung verification complete** |
| Emoji should print | **Known fidelity limitation.** Unsupported emoji are saved and warned about rather than silently dropped. True print fidelity needs a deliberate vector-emoji/content strategy; it is not a safe deadline patch to the text renderer. | **Disclose for launch; post-launch feature unless scope is explicitly changed** |
| Preview animation is unnecessary | **Resolved by the frozen P3 amendment in [`v21-proof.html`](../design/mockups/v21-proof.html).** The evidence identifies the Read page swing specifically. Page changes are now immediate; the physical spine/stack model, gestures, page announcement, fold instruction motion and chrome transitions remain intact. | **Closed — focused interaction/accessibility tests and Samsung verification complete** |
| Open the PDF after saving | **Resolved by the frozen P2 amendment in [`v21-proof.html`](../design/mockups/v21-proof.html).** Save still keeps the maker in Proof, but the completion now offers an explicit `Open PDF` action for the exact durable Downloads item beside the primary fold hand-off. | **Closed — focused tests/goldens plus normal/large-font Samsung passes complete** |

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
