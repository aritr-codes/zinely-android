# Zinely app entry — logo, launcher icon, and system splash

Status: **DESIGN FREEZE — 2026-08-25**  
Prototype: [`mockups/app-entry.html`](mockups/app-entry.html)  
Canonical artwork: repository-root [`APP_LOGO.png`](../../APP_LOGO.png)

## 1. Ruling

The supplied collage is Zinely's launcher mark. It is used without recolouring, redrawing, generative
reinterpretation, or added typography. Android may crop its outer paper scraps through the user's chosen adaptive
icon mask; the central hand-cut `Z` remains the recognition target.

The matching splash is a **brief Android system launch transition**, not a marketing screen. It uses the same mark
on the artwork's pink paper ground and hands off immediately to the shelf. There is no custom splash Activity,
minimum display time, logo animation, slogan, loading copy, or second branded screen.

## 2. Frozen asset roles

| Surface | Frozen treatment |
|---|---|
| Adaptive launcher icon | Full supplied collage as the foreground; `#FBCDD0` edge-matched pink behind it |
| Legacy launcher icon | Deterministically resized full supplied collage |
| Round legacy icon | Same artwork with only the platform-compatible circular alpha mask |
| Themed/monochrome icon | Simplified hand-cut `Z` silhouette; no colour baked into the resource |
| System splash | Supplied mark on `#FBCDD0`, then immediate handoff to `Theme.Zinely` |
| In-app chrome | No persistent logo added to Shelf, Bench, Proof, menus, or sheets |

`APP_LOGO.png` is the immutable source. Generated density assets may change only by rerunning the checked-in asset
generator from that source.

## 3. Adaptive-mask and accessibility rules

1. The central `Z` must remain readable under circle, rounded-square, squircle, and teardrop masks.
2. Decorative star/heart edges may be clipped; the central mark may not be.
3. The monochrome layer is a high-contrast silhouette that lets the launcher own themed-icon colour.
4. No semantic information exists only in the small star, heart, grid, or paper texture.
5. The application label remains `Zinely`; the icon does not add text that would become unreadable at launcher size.

## 4. Launch behaviour

- Use the AndroidX SplashScreen compatibility API and the platform splash contract.
- Install the splash before `Activity.onCreate()` continues, then release it without a keep condition.
- Use no custom exit animation. The platform transition is the motion treatment.
- Cold start, warm start, configuration recreation, share-in, and task reuse all land in the existing single
  `MainActivity`; no second activity or navigation route is introduced.
- The launch window and artwork edge share `#FBCDD0` so a provider/platform crop does not create a white or black
  flash around the mark.
- Dark theme still uses the same pink threshold. It is brand artwork, not document paper and not a theme-dependent
  application surface.

## 5. Verification gate

1. Resource linking and release packaging resolve adaptive, round, legacy, and monochrome icon resources.
2. The launcher activity owns the starting theme, while the application and post-splash theme remain
   `Theme.Zinely`.
3. Cold and warm launches show no duplicate splash, blank intermediate frame, or artificial pause.
4. Physical-device review checks the Samsung launcher mask, Recents/task identity, share-in entry, light/dark launch,
   and immediate Shelf handoff.
5. The supplied source file and deterministic generator output are reviewed for unintended visual changes.

