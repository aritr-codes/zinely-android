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
