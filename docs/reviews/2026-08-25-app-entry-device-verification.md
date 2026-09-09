# App entry device verification — launcher icon and system splash

Date: **2026-08-25**  
Device: Samsung `SM-A176B`, Android 16, 1080 × 2340  
Launcher observed: Niagara (`bitpit.launcher`), circular adaptive mask  
Build: signed `0.9.0-beta.2` release APK, installed in place with `adb install -r`

## Pass 1 — engineering and platform behaviour

- Release upgrade installed successfully without uninstalling; the existing app-private library remained present.
- Packaged manifest resolves `MainActivity` to `Theme.Zinely.Starting`; the application retains `Theme.Zinely`.
- The circular adaptive icon shows the full collage with a clear central Z. The 6dp foreground inset keeps the
  recognition mark away from the mask edge; small decorative scraps remain legible.
- A recorded dark-mode cold launch shows exactly one pink/logo system-splash interval, followed by the existing shelf
  loading state and shelf. No second splash Activity, black/white intermediate flash, or artificial hold appeared.
- A recorded warm task return goes directly from launcher to the existing shelf without replaying a redundant splash.
- A recorded light-mode cold launch uses the same pink/logo identity and hands off cleanly to the light shelf.
- The shelf still showed the existing single project after install, theme changes, cold starts, and warm return.
- Process log inspection showed no crash or application exception during the launch passes.

## Pass 2 — product observation

- At launcher size the Z reads before the small collage details, so the mark remains recognisable rather than becoming
  a tiny poster.
- The pink threshold feels related to the supplied artwork and does not pretend to be zine paper or recolour the Shelf.
- The splash is brief enough to feel like Android acknowledging the tap rather than Zinely presenting an intro screen.
- The handoff preserves the product's existing loading/shelf hierarchy; no extra instruction, slogan, or choice was
  introduced before making.

## Device-state restoration

The verification temporarily switched system light/dark mode only. Final state was checked and restored to the
pre-pass values:

- font scale: `0.9`
- physical density: `450`
- override density: `420`
- night mode: `yes`

## Result

**PASS.** The implementation matches [ADR-111](../DECISIONS.md#adr-111) and the frozen
[app-entry contract](../design/APP-ENTRY-FREEZE.md) on the representative physical device.

