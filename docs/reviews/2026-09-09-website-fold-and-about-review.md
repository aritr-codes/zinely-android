# Website, fold replay, and About review

**Date:** 2026-09-09
**Scope:** Public website information architecture and visual presentation; HTML fold-replay experiment;
in-app About simplification.
**Decision:** Build the public explanation first; keep the visual language expressive and zine-like while
keeping the navigation and writing simple; retain the useful in-app About surface; defer Compose replay.

## 1. Research that changed the plan

### Directly observed guidance

- The W3C carousel tutorial requires keyboard operation, communicated slide changes, focus care, and a way to
  pause automatic movement. It also notes that carousels are disputed. Zinely therefore uses a visible
  three-card gallery with no automatic rotation: <https://www.w3.org/WAI/tutorials/carousels/>.
- WCAG 2.1 requires keyboard access, reflow, sufficient text/non-text contrast, and control over moving
  content. Animation triggered by interaction must be disableable unless essential. Zinely keeps native
  buttons, persistent text, a live completion message, and a reduced-motion static result:
  <https://www.w3.org/TR/WCAG21/> and
  <https://www.w3.org/WAI/WCAG21/Understanding/animation-from-interactions.html>.
- W3C's accessibility-statement guidance recommends a commitment, standard, contact route, and known
  limitations in simple language, placed somewhere easy to find. It does not require a primary-navigation
  destination: <https://www.w3.org/WAI/planning/statements/>.
- Google Play requires screenshots and graphics to accurately represent app functionality. The public gallery
  therefore comes from the same canonical HTML screens used to specify the app, with fictional content and an
  explicit label that they are mockup renders: <https://support.google.com/googleplay/android-developer/answer/15191715>.
- Responsive-image guidance supports explicit intrinsic dimensions, responsive sizing, and lazy loading below
  the initial viewport. The first gallery image is eager; later images are lazy:
  <https://web.dev/learn/design/responsive-images>.
- Keep a Changelog recommends curated, chronological, human-readable, linkable entries rather than a raw commit
  dump. The public page is intentionally shorter than the engineering changelog: <https://keepachangelog.com/>.
- GOV.UK's interface-writing guidance recommends starting with less, putting important words first, and
  removing duplication. That supports moving the product story to the website and keeping the installed-app
  surface task-oriented: <https://www.gov.uk/service-manual/design/writing-for-user-interfaces>.

### Visual-reference observations

Both [NIC Ice Creams](https://www.nicicecreams.com/) and
[The Belgian Waffle Co.](https://thebelgianwaffle.co/) use bold category presentation, crafted section changes,
large display type, and tactile product framing. The useful borrowing for Zinely is confidence, materiality,
and visual rhythm--not their commercial density, repeated calls to action, food photography, or branding.
Zinely translates this into paper layers, tape, ink marks, uneven card placement, and small direct interactions.
The information hierarchy remains conventional and calm so the result is professional as well as creative.

## 2. Information architecture decision

Primary navigation contains only the three tasks a first-time visitor is most likely to need:

1. **See Zinely** -- what the app actually looks like.
2. **How it works** -- phone to printed booklet in three steps.
3. **Fold guide** -- the optional interaction experiment.

About is a substantial home-page section rather than a separate journey. Changelog, Roadmap, Privacy,
Accessibility, contact, and GitHub remain easy to find in the footer. Changelog and Roadmap receive pages
because they are growing reference material; the other items do not need separate pages.

The former standalone Accessibility page is removed. Accessibility remains a non-negotiable property of every
page, while the About section keeps a concise WCAG 2.1 AA target, implemented provisions, and contact route.

## 3. Product imagery decision

The public gallery uses three sanitized light-mode renders from:

- `docs/design/mockups/v21-library.html`
- `docs/design/mockups/v21-bench.html`
- `docs/design/mockups/v21-proof.html`

Each canonical file accepts a gallery query that hides prototype tooling and fixes light mode without altering
the default design-reference view. Headless Edge renders the 390 x 812 view, and FFmpeg performs a lossless WebP
conversion. The published files contain fictional zines and no private phone photos.

This is the right default for the website because it protects privacy, keeps the three images visually
consistent, and makes regeneration deterministic. Real-device screenshots remain verification evidence, not
marketing assets. Future store images may frame the sanitized renders editorially, but must keep the app UI
truthful and clearly representative.

## 4. About decision

The public About copy answers three useful questions: what Zinely is, what it makes, and why it avoids accounts,
feeds, and cloud processing. It does not contain a founder biography, mission deck, or repeated feature list.

The in-app **About Zinely** surface stays. Code inspection shows that it is not merely a product-story page: it
owns the default-paper preference, three bundled-font licence routes, offline/privacy reassurance, and installed
version. Removing it would require new screens or make useful/legal information harder to reach. Its decorative
tagline and origin paragraph are removed because the website now explains the product.

## 5. Fold experiment and Compose gate

The canonical Proof HTML now includes an optional **Show this fold** replay for each of its eight steps. It does
not autoplay; the existing caption and final diagram remain; focus stays on the initiating button; completion is
announced; and reduced motion displays the result without animation. The public website demonstrates three
representative physical actions rather than duplicating all eight instructions.

The experiment is useful as an illustration of the idea, but it does not yet justify Compose implementation.
The cut and inward-push examples add visible direction; the simplest fold is not materially clearer in a still
inspection, and an honest app implementation would require eight carefully authored paper states plus large-text,
TalkBack, and device validation. Without comparison testing with people following the physical folds, that cost
and surface area exceed the evidence. The app's existing accessible tap-through guide therefore remains intact.

**Next highest-value experiment:** observe several first-time users folding a printed sheet, alternating the
current static guide and HTML replay. Record wrong folds, requests to replay, time per step, and where the diagram
or wording causes hesitation. Implement Compose replay only if the motion measurably reduces errors or hesitation.

## 6. Public change history and roadmap

The public Changelog is a curated reader-facing projection of `CHANGELOG.md`, newest first. The public Roadmap
separates **Planned**, **Exploring**, and **Not committed**, contains no delivery dates, and links back to the
repository's canonical engineering roadmap. This makes both pages useful without creating a second planning
authority or promising speculative features.

## 7. Verification record

### Website and assets

- W3C Nu validator 26.9.9 returned an empty `messages` array for `website/index.html`,
  `website/changelog/index.html`, and `website/roadmap/index.html`.
- `node --check website/assets/site.js` passed.
- Headless Edge inspection covered the 1440 px desktop hero, exact 720 px and 390 px responsive layouts, public
  Changelog, public Roadmap, and all three 390 x 812 gallery images. CDP keyboard checks reached every interactive
  control with a visible 4 px focus outline. At an exact 320 px viewport with 200% root text, no meaningful text,
  link, button, image, or caption crossed the visual viewport; the remaining measured document overhang came only
  from intentionally clipped decorative pseudo-elements.
- The public fold controls retained focus and announced the complete current instruction. With
  `prefers-reduced-motion: reduce`, the finished state appeared immediately and no playing animation was entered.
- All gallery assets decode as 390 x 812 lossless WebP with alpha. Total size is 678,542 bytes. SHA-256:
  `6feb4cfc3fff6209579ab78fb318b1c9350fdf149c3af2199c5b246ad755bd29` (Shelf),
  `48eaeaf66f739953a9a117162611b2671b10d76cc3a03c904002547ab9c8043d` (Bench), and
  `8edd82153993ce40647ce0707d9556be3c44f72b035467c4641398ce13100395` (Proof).
- Manual contrast calculation for every foreground/background pairing used by body and helper text found a
  minimum of **4.71:1** (`#6A452F` on `#BBCA6F`), meeting the WCAG AA normal-text threshold. Primary text ranges
  from 4.73:1 to 14.43:1 across the published palette.
- The canonical Proof prototype was visually inspected at steps 1, 5, and 7, including an active replay; step 5
  was also inspected at 1.8x text. The drawer remained scrollable and the static instruction remained present.

### App and rendering gates

- Focused `ColophonScreenTest`: passed.
- Complete `:core:copy:test`: passed.
- The first pinned golden verification reported exactly four changes: light, dark, 1.8x-light, and 1.8x-dark
  About images. All four comparisons were inspected and contained only the intentional removal of the opening
  story plus the resulting upward flow. They were re-recorded through the pinned Roborazzi task.
- Final `tools/grun.sh gold`: passed. This ran the complete 949-test editor suite and the feature-editor,
  render-android, and core-ui golden gates with `--rerun-tasks`.
- `:app:testDebugUnitTest`, `:app:lintDebug`, `:app:assembleDebug`, and `:app:assembleRelease`: passed together.
- Samsung SM-A176B developer pass: the release-signed task build was installed in place after its signing
  certificate was verified against the installed app. A4/US Letter selection and restoration, all three font
  licence routes, back navigation, scroll reachability, semantics, installed version, offline/privacy copy,
  light and dark modes, and 1.8x text were checked without clipping or unreadable content.
- Samsung first-time-user pass: a fresh Shelf -> About entry presented the useful choice first, followed by clearly
  labelled font licences, local-data reassurance, and the installed version. The removed origin story was not
  needed to understand or use this screen.
- Device baseline was restored exactly: font scale `1.0`, night mode `yes`, and window/transition/animator scales
  `1.0`. A4 was restored as the default paper. The frozen public beta.4-r3 APK was reinstalled in place and read
  back with SHA-256 `3522042340e85042b7e3e314ae07de49d85fe7b4da553462438cd9f1ab97c3a1`.

### Review and deployment

- Independent review returned **GO** after its findings were fixed: truthful tester/public
  changelog labels, stable repository links, complete fold announcements, a gated canonical Proof experiment,
  durable visual-inspiration citations, and 320 px/200% text reflow.
- GitHub Pages run `34353636558` deployed source commit `4bcd0a9` successfully. Live read-back returned HTTP 200
  for Home, Changelog, Roadmap, and Privacy with the expected titles and contact address. The logo, three gallery
  WebPs, stylesheet, script, and beta.4-r3 APK all returned HTTP 200 with their expected media types and byte
  sizes. A fresh live desktop render was visually inspected with the deployed logo and artsy workbench layout.
