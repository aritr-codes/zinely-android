# Replace Photo — device verification

**Date:** 2026-08-30–31  
**Device:** Samsung SM-A176B (`RZCYA1VBQ2H`), Android 16 / API 36, 1080 × 2340,
420 dpi  
**TalkBack:** 16.2.00.13  
**Branch:** `feat/zine-backup-v2`  
**Final accessibility repair:** `0e20cb2` (`fix(a11y): disambiguate replace verbs`)

## Scope

This closes [D-038](../design/V2-SPEC-DEFECTS.md#d-038): a selected photo can be replaced through
the existing Android picker/decode/AssetStore pipeline while the reducer remains the sole document
mutation owner. The pass also protects the identically drawn Decor Replace route, large-text reachability,
and the surrounding editor regressions named in the implementation brief.

Testing used only a disposable project (`75b59ee7-ec1d-4a29-a20f-1c04e2106911`). Existing user projects
were not opened or changed.

## Pass 1 — developer verification

Pass 1 verified the complete behavior path:

- Add Photo and Replace Photo both opened Android's existing system photo picker.
- A successful replacement changed only `ImageElement.assetId`. The same element id, transform, crop,
  fit, z-index, opacity, photocopier flag, horizontal/vertical flip state, and selection were preserved.
- One Undo restored the old asset; Redo restored the new one. Reopening the zine retained the replacement.
- Cancelling the picker left the document, selection, history, and feedback unchanged.
- Decor Replace opened the Art cabinet and never entered the photo picker.
- First, middle, and final page navigation; Add, Reframe cancel, Flip, and Duplicate regressions passed.
- At font scale 1.8 the photo action strip was a scrollable `android.widget.HorizontalScrollView`; Replace
  was reachable, enabled, clickable, and above the 48dp minimum target.

The stored document evidence showed the original asset
`c1eb1e3bfe57c647d5a23a1388b71e5d7afc1120dabcccc306c3ac018b88391b` replaced by
`9cea4eddf29e88832074aedaa4c396a4837514570cdabe763a08dda652043f10` without another image-field change.
The old asset remained stored, as required for content-addressed/shared references.

## Pass 2 — first-time-user verification

Pass 2 independently completed the ordinary user path:

- Replace opened the expected system picker; cancel returned to the same selected photo and action row.
- A visibly different photo replaced the original; Undo and Redo gave immediate, trustworthy visual
  confirmation.
- Add clearly distinguished Text, Photo, and Art. Selecting a Decor element exposed its own Replace verb,
  which reopened the Art cabinet.
- Page navigation completed `1 → 4 → 8 → 1`; the current-page fraction and empty-page invitation were clear.
- At 1.8× text, the selected-photo action row remained horizontally scrollable and the Replace target
  measured `[823,1390][1026,1555]`, approximately 77 × 63 dp at 420 dpi.

The reader initially returned **NO-GO** because both Photo and Decor platform nodes were named only
`Replace`. Without a reliably announced container, TalkBack could not distinguish two controls with
different destinations. The visible frozen captions stayed unchanged; `0e20cb2` added the type-specific
spoken names `Replace photo` and `Replace supply` through `core:copy`.

The repaired build was independently re-read on Samsung at 1.8×:

| Selected element | Platform class | Spoken name | Enabled / clickable | Bounds |
|---|---|---|---|---|
| Photo | `android.widget.Button` | `Replace photo` | yes / yes | `[823,1390][1026,1555]` |
| Eyelet Decor | `android.widget.Button` | `Replace supply` | yes / yes | `[99,1390][302,1555]` |

Pass 2 then returned **GO**. The system picker's extra `Preview` / `Done` confirmation may cause a brief
first-use hesitation on this Android build; it is external picker behavior and a documented, non-blocking
limitation rather than a second Zinely flow.

## Automated and review evidence

- Focused reducer request/replacement, effect-runner, context-bar, platform accessibility, and EditorScreen
  routing tests passed.
- The complete `:feature:editor:testDebugUnitTest` suite passed after the accessibility repair.
- Complete local editor/core regression suites, `:app:lintDebug`, debug/release assembly, and
  `tools/grun.sh gold` passed during the vertical slice.
- Pinned GitHub Actions run `33315707575` passed both pure-Kotlin and Android graph/lint/test jobs.
- Independent code review returned **GO** with no required findings.

## Restored device state

After the final read, the Samsung reported:

- `font_scale=1.0`
- `ui_night_mode=2` (dark mode)
- `enabled_accessibility_services=null`
- `accessibility_enabled=0`

Both hardware passes are accepted. D-038 is closed.
