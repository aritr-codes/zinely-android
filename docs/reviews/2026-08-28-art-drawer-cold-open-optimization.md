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

## Remaining open verification

The required post-install Samsung measurement is still open. On 2026-08-28 the connected device
`RZCYA1VBQ2H` moved from `offline` to `unauthorized` after `adb reconnect` and an ADB server restart, so
the physical cold-open timing rerun could not be completed from the repository side.
