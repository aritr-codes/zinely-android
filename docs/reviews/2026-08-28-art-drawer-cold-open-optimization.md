# Art drawer cold-open optimization — 2026-08-28

## Scope

This follow-up addresses the remaining repo-side part of the Art drawer responsiveness gap recorded in
[2026-08-25-public-beta-stabilization-audit.md](2026-08-25-public-beta-stabilization-audit.md) and
[2026-08-27-release-gap-roadmap.md](2026-08-27-release-gap-roadmap.md): reduce the first Art opening's
path-construction cost without changing the frozen cabinet, renderer contract, or saved-document model.

## Change

The Art sheet now owns one shared `SupplyPainter` for the whole drawer lifetime instead of one painter
per tile, and it warms the canonical catalogue paths once on a background dispatcher before first use.
`SupplyPathCache` is synchronized so that background warmup and later UI-thread drawing share the same
cache safely.

This preserves the existing architecture:

- `SupplyCatalog` remains the pure authoritative source of outlines.
- `SupplyPainter` stays the Android-only adapter over the shared render contract.
- The sheet still draws the same outlines through the same render seam, so preview/export/PDF behavior
  is unchanged by construction.
- No UI copy, interaction, schema, reducer, or persistence behavior changed.

## Automated verification

- `:render-android:testDebugUnitTest --tests com.aritr.zinely.render.android.SupplyPathCacheTest`
- `:feature:editor:testDebugUnitTest --tests com.aritr.zinely.feature.editor.BenchArtSheetTest --tests com.aritr.zinely.feature.editor.a11y.BenchArtSheetPlatformA11yTest`

All passed on 2026-08-28.

## Samsung post-install measurement — gate remains open

The device rerun completed on 2026-08-28 on Samsung SM-A176B (`RZCYA1VBQ2H`), Android 16, at the
panel's active 60 Hz mode. The measured artifact was the verified release APK re-signed locally with
the standard debug key solely so it could update the existing debug-signed installation without an
uninstall or data loss. No source or packaged code changed, and this profiling artifact is not
distributable.

For each sample the APK was installed over itself, Zinely was launched from a stopped process, the
existing test zine was opened, the Add tray was allowed to settle, `dumpsys gfxinfo
com.aritr.zinely reset` isolated the transition, and Art was opened once:

| Sample | Frames | Janky frames | Median | P90 | Result |
|---|---:|---:|---:|---:|---|
| Release-parity cold 1 | 31 | 7 (22.58%) | 42 ms | 101 ms | Does not clear the gate |
| Release-parity cold 2 | 31 | 6 (19.35%) | 29 ms | 109 ms | Does not clear the gate |

Two exploratory debug-build cold samples agreed directionally (28 frames; 9 / 8 janky; 38 / 32 ms
medians) but are not used as the release decision. The repeated release-parity result remains materially
worse than the previously recorded settled warm transition (18 frames, 2 janky, 7–8 ms median), so the
prewarm implementation is retained as safe foundation work while responsiveness stays **Yellow**.

The Samsung was restored to the original debug build after profiling, app data was preserved, and
`font_scale` remained `1.0`.

## Follow-up trace diagnosis — 2026-08-30

A later release-parity pass after the painter/cache work still failed the cold-open gate:

| Sample | Frames | Janky frames | Median | P90 |
|---|---:|---:|---:|---:|
| Release-parity cold 1 | 30 | 10 (33.33%) | 40 ms | 200 ms |
| Release-parity cold 2 | 28 | 8 (28.57%) | 48 ms | 200 ms |
| Release-parity cold 3 | 31 | 8 (25.81%) | 29 ms | 101 ms |
| Release-parity cold 4 | 29 | 6 (20.69%) | 27 ms | 200 ms |
| Release-parity cold 5 | 29 | 8 (27.59%) | 38 ms | 200 ms |

An `atrace` capture (`gfx` and `view`) then isolated a second interaction-critical cost: production
closed the Add `Dialog` and created a new Art `Dialog`, while the frozen HTML kept one `#sheet` and
replaced only its `innerHTML`. The new Art window paid its own attach, measure, layout, and first-draw
work before the cabinet animation could settle. This explains why removing path construction alone did
not materially lower cold-open jank.

The smallest coherent follow-up therefore keeps one production `ZSheet`/`Dialog` alive for Add and Art
and swaps only the hosted body. Direct cabinet entry still opens Art in that same host, sheet modality
and focus containment remain owned by `ZSheet`, and the standalone chooser/cabinet composables remain
available as focused component seams. No reducer, renderer, persistence, schema, copy, or saved-document
behavior changes.

Host verification for this candidate passed the complete `:feature:editor:testDebugUnitTest` suite,
`:app:lintDebug`, `:app:assembleDebug`, and `:app:assembleRelease`.

## Shared-host Samsung verification — 2026-08-30

The rebuilt release-parity artifact was measured on Samsung SM-A176B (`RZCYA1VBQ2H`) with the same
force-stop, launch, open-project, open-Add, reset-`gfxinfo`, tap-Art protocol:

| Sample | Frames | Janky frames | Median | P90 |
|---|---:|---:|---:|---:|
| Shared-host cold 1 | 3 | 2 | 150 ms | 200 ms |
| Shared-host cold 2 | 3 | 3 | 150 ms | 150 ms |
| Shared-host cold 3 | 3 | 3 | 150 ms | 150 ms |
| Shared-host cold 4 | 3 | 2 | 150 ms | 150 ms |
| Shared-host cold 5 | 3 | 2 | 150 ms | 200 ms |

The percentage alone is misleading for this transition because the optimized path renders only three
frames instead of animating 28–31 frames through a second dialog. Absolute missed frames fell from
6–10 (mean 8.0) to 2–3 (mean 2.4), a 70% reduction, while total transition frame work fell by about
90%. A follow-up trace confirmed that the remaining misses are the first Art-body display-list record
and raster in the existing shared window; no second `Dialog` is created.

The device regression pass also verified distinct Photo/Art chooser semantics, a complete and scrollable
Art cabinet at font scales 1.0 and 1.8, first/middle/final-page navigation, repeated page-panel dismissal,
final-page navigation-bar clearance, compact Flip/Reframe panels, and the named Ink swatches in dark
mode. Font scale was restored to 1.0; dark mode and accessibility settings were left at their captured
baselines. The performance gate is closed for this slice. The remaining 125–200 ms first-body draw is a
measured optimization opportunity, not a regression or a reason to reintroduce a second window.

## Residual first-body investigation — 2026-08-31

This follow-up investigated the remaining cost from the clean `9001fca` checkout without assuming that
the supply outlines or GPU raster were still dominant. The Samsung SM-A176B (`RZCYA1VBQ2H`) remained
at 60 Hz, font scale 1.0, dark mode, animation scales 1.0, and its captured accessibility baseline. The
measured APK was an uninstrumented release-parity build of `9001fca`, signed with the local debug key
for an in-place profiling install. It is not a tester artifact, and the frozen beta.4 APK in `dist/` was
not rebuilt or replaced.

Five process-cold samples used the same force-stop, launch, open-project, settle-Add, reset-`gfxinfo`,
tap-Art protocol:

| Sample | Transition frames | Janky frames | Median | P90 |
|---|---:|---:|---:|---:|
| Current-HEAD cold 1 | 3 | 2 | 150 ms | 150 ms |
| Current-HEAD cold 2 | 3 | 2 | 150 ms | 150 ms |
| Current-HEAD cold 3 | 3 | 2 | 133 ms | 150 ms |
| Current-HEAD cold 4 | 3 | 2 | 150 ms | 150 ms |
| Current-HEAD cold 5 | 3 | 2 | 150 ms | 150 ms |

The mean absolute janky-frame count was 2.0. `gfxinfo`'s separate `Number Missed Vsync` counter was zero
for each sample, so this report uses the absolute janky-frame count consistently with the earlier
shared-host comparison. Three repeated opens remained three frames and two janky frames, but their
median/P90 durations fell to 113/125 ms, 117/117 ms, and 105/105 ms. The warm improvement is real but
does not remove the first-body frame misses. These samples are slightly faster than the earlier tester
artifact numbers, but they come from a locally signed profiling build and should be treated as a
reproduction of the same 2-3-janky-frame band, not as a new release-performance claim.

An app-tagged `atrace` capture of the uninstrumented current-HEAD build changed the attribution:

| Span or counter | Observed duration | Interpretation |
|---|---:|---|
| Main-thread `Choreographer#doFrame` | 119.5 ms | Complete dominant UI frame |
| `Record View#draw()` in the shared dialog root | 98.2 ms | Coarse container; not pure raster work |
| `AndroidOwner:measureAndLayout` inside that record | 89.6 ms | Dominant directly observed Compose work |
| `Recomposer:recompose` | 17.1 ms | Material but not dominant alone |
| 27 `TextStringSimpleNode::measure` spans | 26.1 ms aggregate; 1.6 ms max | Visible tile/search/section text contributes cumulatively |
| App RenderThread `DrawFrames` | 33.9 ms max | Secondary render-thread work |
| First GPU completion fence | 49.2 ms | Secondary asynchronous GPU completion |

A disposable diagnostic build added draw-only trace sections without changing layout or content. Its
frame decomposition reproduced the clean trace (`measureAndLayout` 89.9 ms; recompose 18.6 ms), while
the complete Art body draw section was 7.6 ms and individual visible tile draw sections were below
1 ms each. Those diagnostic markers were removed and are not part of the repository.

### Observed facts

- `AndroidOwner:measureAndLayout` consumed about 89.6-89.9 ms inside the first shared-dialog
  `Record View#draw()` span in both the clean and diagnostic traces.
- `Recomposer:recompose` consumed about 17.1-18.6 ms in the same first frame.
- The complete diagnostic `ArtBody.draw` section was 7.6 ms, and each visible tile draw section stayed
  below 1 ms.
- Twenty-seven `TextStringSimpleNode::measure` spans totaled about 26.1 ms, with no single span above
  1.6 ms.
- RenderThread `DrawFrames` peaked at 33.9 ms and the first GPU completion fence at 49.2 ms.

### Findings and confidence

- **High:** the remaining UI-thread cost is primarily first composition/subcomposition plus measurement
  and layout of the visible `LazyColumn` cabinet body. The earlier display-list label included this
  deferred Compose work; it did not prove that supply-outline recording itself was dominant.
- **High:** supply-path generation, painter warmup, and per-tile outline drawing are not the dominant
  actionable cost. The existing warmup remains useful, but another path or painter cache would optimize
  a small measured span.
- **Medium-high:** text measurement is a meaningful part of the initial layout cost, but no single label
  or glyph dominates. Reducing labels or visible tile content would violate the frozen visual and
  accessibility contracts.
- **Medium:** RenderThread/GPU work is secondary and overlaps the long UI frame. It may explain part of
  the remaining warm cost, but the trace does not identify a safe editor-only raster change that would
  preserve preview/export/PDF parity.
- **Medium:** semantics construction can occur within the same first composition/layout work, but the
  trace does not isolate it as the dominant span. Removing semantics as an optimization would be both
  unsupported by the evidence and prohibited by the accessibility contract.

### Hypotheses and decision

Initial body composition and first grid measurement are supported. Avoidable sheet-host churn is
rejected because the same dialog root remains alive. Supply-outline generation, painter/resource
warmup, the favourite-star glyph, and repeated state invalidation are rejected as dominant causes by
the trace spans and draw-only diagnostic. Pure raster-bound cost is present but is not the primary
main-thread bottleneck.

The plausible ways to avoid the 89.6 ms first measurement are to precompose/premeasure a hidden cabinet,
retain a parallel composed body, defer visible/semantic content, or simplify the cabinet. Those options
either move substantial work into the Add interaction, introduce hidden semantics/lifecycle risk, or
change frozen content and reachability. None is a small, evidence-backed optimization with benefit large
enough to justify its correctness risk. **Decision: defer with no production code change.** Do not add a
second dialog, parallel host, raster cache, or speculative precomposition path.

The next highest-value experiment, only if this opportunity is reopened, is an isolated profileable
benchmark that brackets `LazyColumn` item subcomposition, text measurement, and semantics construction
separately at font scales 1.0 and 1.8. It should compare the real cabinet with diagnostic-only variants
that preserve geometry, then discard those variants. A production change should remain gated on a
measured reduction in the same Samsung transition and full visual/accessibility verification.
