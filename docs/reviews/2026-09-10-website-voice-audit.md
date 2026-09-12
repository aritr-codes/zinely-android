# Website voice audit

## Native maker's note implementation, 12 September 2026

Owner approved the updated app preview with "looks good. proceed". [ADR-116](../DECISIONS.md#adr-116) records the
canonical amendment and design freeze. Earlier draft-only notes below describe preceding review rounds.

- Promoted approved copy/styles to canonical About; experiment URL remains a thin wrapper, avoiding duplicate text.
- Added matching shared copy and Compose note with a semantic heading, themed ink and scalable text. Existing
  utility controls remain. Retain the main lazy list state across licence navigation so the returning row can
  receive focus instead of resetting to the new opening.
- Expanded focused tests for note content/heading, utilities, licence return focus and 320dp/1.8 text scaling.
  Updated shelf integration test to scroll to utilities and back. Website narrative remains unchanged.
- Initial focused run: 33 tests, 31 passed; both new licence-return focus assertions failed, although the row
  was visible. Moved restoration from the parent destination effect into the lazy row's effect so its focus
  target is attached first. Retained list scroll state. A second run reproduced the same failures because the
  focus tests still used touch mode. Corrected them to request keyboard mode, matching the existing shelf test
  and [AndroidX evidence](../RESEARCH.md#r21-about-return-focus-test-mode-12-september-2026). These failures alone
  are not proof of a user-facing focus defect. A corrected run is required.
- Independent source review accepted the copy contract and spacing fix (24dp note-to-utility break). The
  lifecycle fix above requires the second test run; source review alone did not catch the timing failure.
- Corrected run: all 8 About tests passed (XML timestamp `2026-09-12T16:54:55.629Z`), including keyboard focus
  after licence return at normal and 1.8 text scale. No uninitialized-focus warning. All 25 shelf integration
  tests passed in the preceding runs. Public-copy contracts and whitespace checks passed.
- QA APK packaging and visual/device validation in progress. No golden, physical-device parity, TalkBack,
  deployment or release pass is claimed.

## Separate app maker's note, 12 September 2026

- Owner approved the website story but requested different copy in the installed app, then approved the short
  maker's note. Updated only the app proposal: new heading, two-person credit and paper-book origin, page-order
  line and brief thank-you. Website copy is unchanged by this refinement.
- Source contracts now check each surface independently and reject the website opening in the app proposal.
  Canonical About, Compose and existing paper/licence/privacy utilities remain unchanged. The copy is approved;
  rendered layout review and canonical design freeze remain pending. See [ADR-116](../DECISIONS.md#adr-116).
- Validation: public-copy source checks and whitespace checks passed; local preview returned HTTP 200 with the
  new heading. No new em dashes. No native build, rendered acceptance or deployment is claimed.
- Independent source review: GO, no required fixes. Approved wording, surface separation and draft-only
  boundaries verified. Rendered review remains pending.

## Narrative refinement, 12 September 2026

- Owner approved the story direction for now and requested updating the About preview next. Replaced the
  disconnected product/team/joke paragraphs with one arc: wanting a physical object, looking for the app,
  building it, then handing the result to someone. Kept the page-order closing line.
- App proposal uses the agreed four paragraphs. Website About uses the same opening and maker story with a
  shorter ending, avoiding a second explanation of the hero/workflow. Removed the separate code/paper aside
  and cat/feed joke so the story is not interrupted by competing punchlines.
- Scope: copy only, source contracts and this decision/review record. No CSS, interaction, link, legal text,
  canonical About design, Compose, installed app or deployment changes.
- Validation: `node tools/check-public-copy.cjs` and `git diff --check` passed. Local About preview returned
  HTTP 200 with the updated story. New About copy contains no em dashes; links and release anchors pass contracts.
- Independent narrative-refinement review: GO, no required fixes or recommended improvements. Source-only review
  confirmed the narrative arc, accurate claims and unchanged utilities/legal text. Rendered day/night, large-text,
  keyboard and utility-discoverability review remains necessary before the app design freeze. No deployment claimed.

## Plain-language follow-up, 12 September 2026

Owner feedback: public history/plans were too technical; About should say what/who/why on both surfaces; public
accessibility copy should explain benefits, not standards. Owner reconfirmed Aastra, a two-person team.
[ADR-116](../DECISIONS.md#adr-116) owns the changed direction. Earlier passes below remain historical evidence.

### Changes and boundaries

- Homepage: literal Android/eight-page product definition, named two-person team, unmet-need origin, restrained
  paper-versus-feed humor. Retained hero, workflow, screenshots, fold instructions, feedback policy and download.
- Public links now read “What’s new” and “What’s next”; routes and existing release bookmarks are preserved.
- Roadmap: removed completed Reframe investigation from the public to-do list. Described Font PR #70 as still
  being checked and not in the download. Reopened creative-tool ideas are explicitly not announced features.
- Release history: translated shrinking/image-format and UI implementation details into user outcomes. Added the
  already-merged 10 September website work (PR #69 merged `2026-09-10T17:03:14Z`). No new app release is claimed.
- Accessibility: practical keyboard, text zoom, readable static folding instructions and reduced-motion wording;
  implementation requirements remain internal. This is neither a conformance certification nor a removal of support.
- App About: separate HTML story proposal wraps the canonical screen without editing its frozen design or Compose.
  Short what/who/why opening, existing paper preference/licences/privacy/version retained. Await owner review and
  canonical freeze before native implementation. No product capability was added.
- Creative-feature assessment lives in the [roadmap](../ROADMAP.md#creative-tools-assessment), grounded in model,
  font registry, rendering and supply code plus [R20](../RESEARCH.md#r20-creative-tools-feasibility-12-september-2026).
  Relative effort is a recommendation, not a calendar estimate. No fonts, stickers or models were downloaded.
- Confirmed both `com.aritr.zinely` and isolated `com.aritr.zinely.fontqa` on Samsung. No uninstall performed:
  owner asked for a recommendation, not deletion. Recommend keeping QA until its acceptance check, then removing
  only QA and its disposable document. Future QA builds should be visibly named to avoid duplicate-app confusion.

### Verification and handoff

- `node tools/check-public-copy.cjs`: passed product/team/download/status copy contracts, local links and fragments,
  unique IDs/main headings, stable release bookmarks, and proposal inline-script syntax. Added to Pages build checks.
- `node --check website/assets/site.js` and `git diff --check`: passed. JavaScript and CSS behavior unchanged.
- Rewritten roadmap/changelog and new app proposal contain no em dashes. Homepage retains three pre-existing
  occurrences in metadata/install warning; none were introduced by this copy change.
- Built-in automated Browser is unavailable in this VS Code environment; no fresh rendered/axe/keyboard pass is
  claimed. Prior rendered results below apply only to their historical commits, not this follow-up.
- Review/build/deployment status must be read from the current PR. This branch is independent of Font PR #70;
  it neither merges that app change nor substitutes for its human acceptance checks.

Review actual files, not this summary: check factual release/status boundaries, clear what/who/why, no invented
creative capabilities, unchanged link destinations/permissions/privacy, draft-only app story and preserved utilities.
Next owner decision: approve/refine the app story, then amend/freeze canonical About before any Compose work.

Independent review: GO for source copy/docs, with rendered acceptance pending. Required category fix ACCEPTED:
the public roadmap now separates Planned, Exploring and testing, and Ideas, not promises, preserving ADR-114 item 5.
No required source fix remains. [Draft PR #71](https://github.com/aritr-codes/zinely-android/pull/71) contains the work;
initial Pages build `34703800223` passed on `0e888b1`; the category follow-up requires its own run. Publishing was
correctly skipped because this is a draft PR. No website deployment or native About delivery is claimed.
Local About preview is served at `http://127.0.0.1:8766/docs/design/experiments/v21-about-story.html` (HTTP 200 checked,
not rendered acceptance). Keep this preview server running only while needed for owner review.

## Second pass: paper, personality, and interaction

The owner's first-pass feedback was that warmth alone did not deliver enough humor. This second pass is explicitly
authorized to improve presentation and interaction as well as copy, while preserving the existing information architecture.

### Research before implementation

- **Verified observation, creative studio:** [Mouthwash](https://mouthwash.studio/) uses a brief identity statement,
  project-led navigation, and editorial side projects. **Application:** make Zinely's maker story concrete and let the
  object do some explaining. Its agency vocabulary is not a voice model for Zinely.
- **Verified observation, unconventional brand:** [MSCHF](https://mschf.com/) presents named projects as invitations
  to investigate. [Dollar Shave Club](https://us.dollarshaveclub.com/) pairs irreverence with literal product and support
  information. **Application:** a short curiosity hook followed immediately by a useful explanation; no shock tactics.
- **Verified observation, indie / unusual products:** [Neal.fun](https://neal.fun/) identifies the person behind the
  experiments in plain language. [Are.na](https://www.are.na/) explains actions and its independent business directly.
  **Application:** say why two people built this particular thing, and show page arrangement with one reversible action.
- **Verified observation, internet culture:** [Cameron's World](https://www.cameronsworld.net/) preserves personal,
  eccentric web fragments. [Know Your Meme's This Is Fine entry](https://knowyourmeme.com/memes/this-is-fine) documents
  the mismatch between calm words and an absurd situation. **Application:** use understatement about writing software
  for folded paper. Do not reproduce memes, characters, slang, or somebody else's joke.
- **Verified gallery observations:** [Hoverstat.es](https://www.hoverstat.es/) describes portfolios using scattered
  stacks, contact sheets, restrained hover changes, and mechanical controls. These are gallery descriptions, not a
  claim that we interactively tested every featured site. **Application:** local paper movement and deliberate presses,
  never scroll effects that delay reading. Poolsuite and Awwwards were also opened, but their limited text responses
  do not support detailed interaction claims and did not drive implementation.
- **Verified accessibility guidance:** [W3C animation from interactions](https://www.w3.org/WAI/WCAG22/Understanding/animation-from-interactions.html)
  supports disabling nonessential interaction animation. **Application:** the same content and state changes work
  instantly with reduced motion, and touch/keyboard can reach every discovery.

### Direction and opportunities

Keep the main headline, product screenshots, workflow, ten-step instructions, and direct download path. Give the hero
a handwritten editorial aside, shorten excessive viewport-dependent empty space, and refine depth using paper edges.
Retain system fonts and existing assets. Plain facts surround the jokes.

Five planned moments: (1) a bounded pointer response on the hero paper with a native disclosure note for touch and
keyboard; (2) a page-order comparison that explains print arrangement, explicitly not a folding simulation; (3) a
small optional zine-idea picker; (4) folding-guide position feedback and a final-step aside without claiming physical
completion; (5) a native footer disclosure with a dry paper joke. No autoplay, random timed copy, sound, confetti,
tracking, animation library, or dependency is needed.

The homepage will carry the expressive changes. Secondary factual pages retain their release and policy content.
The public roadmap still contains older Reframe status; that factual maintenance is recorded for follow-up rather
than silently bundled into this creative task.

Voice rules live in [VOICE.md](../design/VOICE.md#public-website-voice), so this audit does not become a second voice authority.

### Second-pass implementation and validation

- Preserved the headline, core product definition, download destinations, screenshots, ten folding instructions,
  and factual accessibility/privacy/release material. Added specific dry asides, a more direct maker story, and
  selective screenshot captions. The purpose is stronger character without losing the plain-language floor.
- Implemented the five planned moments with CSS, native disclosures, and small JavaScript enhancements. The
  page-order comparison states the actual imposed order and never claims to simulate physical folding.
- Refined homepage spacing, paper edges, card shadows, editorial serif asides, mobile branding, and tap targets.
  No assets, fonts, runtime dependencies, navigation routes, or Android code were added or changed.
- Expanded `tools/check-website.cjs` and passed in headless Edge: actual print-position geometry and inversion,
  keyboard/reversible toggle behavior, native disclosures, idea changes, emulated touch, pointer reset on reduced
  motion, instantaneous reduced-motion comparison, existing folding tests, 320/390px and 200% text reflow,
  no-JavaScript and print fallback, axe checks, and zero page errors.
- Physical-device and novice paper-folding success are not established by browser automation.
- Final independent copy review caught an inherited factual error in the hero illustration that had also entered
  the new comparison. Both now match `SingleSheet8.TOP_ROW_ROTATED`: top 5, 4, 3, 2 inverted; bottom 6, 7, 8, 1
  upright. The browser suite reads the canonical Kotlin cell/rotation tables to verify both diagrams and the text
  alternative. Review also prompted removing a grammar error, excess About asides, and a changelog reliability joke.
- W3C Nu validation returns zero errors (one existing informational redundant-list-role advisory). JavaScript
  syntax and diff-whitespace checks pass. All 27 homepage link targets remain byte-identical; added lines across
  the complete patch contain zero em dashes. HTML, CSS, and JavaScript together add approximately 3.3 KB gzipped
  relative to PR #68, with no new network requests or dependencies.

## First-pass historical record (PR #68)

The following records the earlier copy-only pass, not the expanded scope of the second pass above.

**Date:** 2026-09-10

**Scope:** Public website information architecture and reader-facing copy.

**Outcome:** Selective copy revision only. Product behavior, routes, legal text, release facts, roadmap status,
store availability, and privacy or accessibility claims are unchanged.

## Research

### Direct observations

- ✅ **VERIFIED:** [Kinopio](https://kinopio.club/) explains its unusual canvas with familiar actions and concrete
  objects, then places its more eccentric lines at the edges of the page. Its
  [About page](https://kinopio.club/help/about) names the maker and the kind of software he wants to build. The
  personality feels credible because it is attached to a specific product philosophy and a real person.
- ✅ **VERIFIED:** [Are.na](https://www.are.na/) leads with plain product definitions, then earns more imaginative
  language by showing what its nouns mean through a short Capture, Arrange, Search, Connect sequence. Its lack of
  ads and personalized recommendations is stated as a concrete product property, not a broad virtue claim.
- ✅ **VERIFIED:** [Obsidian's manifesto](https://obsidian.md/about) turns differentiators into short principles
  backed by specific behavior: local data, open formats, privacy, and independence. It does not need superlatives
  to sound confident.
- ✅ **VERIFIED:** Mailchimp's [voice and tone guide](https://styleguide.mailchimp.com/voice-and-tone/) recommends
  plain language, warmth, subtle humor, and adjusting tone to the reader's emotional state. Its concise
  [summary](https://styleguide.mailchimp.com/tldr/) puts clarity and usefulness ahead of entertainment.

### Principles used

- 🟦 **RECOMMENDATION:** Make the core explanation work with nouns and verbs before adding personality.
- 🟦 **RECOMMENDATION:** Put wit in low-risk moments such as section headings, journey labels, and hidden details.
  Keep installation warnings, folding instructions, privacy, accessibility, and release history literal.
- 🟦 **RECOMMENDATION:** Let the makers speak in the first person only where the repository supports the claim.
  Zinely's README establishes the unmet product combination and identifies Aastra as an independent two-person
  team.
- 🟦 **RECOMMENDATION:** Prefer one memorable, product-specific line over a stack of jokes. A visitor should still
  understand Android, eight pages, one sheet, offline use, the beta status, and the printable output while scanning.

These findings extend the existing public-site evidence in
[RESEARCH R16](../RESEARCH.md#r16-public-product-site-and-optional-fold-motion) without changing its information
architecture or accessibility decisions.

## Copy audit

### Strong copy intentionally retained

- The hero headline, **“Make a little zine.”**, is short, direct, and specific.
- **“Eight pages. One sheet. Yours.”** explains both the constraint and the payoff without jargon.
- **“Physical media instead of social media.”** is the clearest compact expression of the product's point of view.
- The gallery labels, trust list, beta warning, folding guide, feedback explanation, and accessibility statement
  already answer concrete visitor questions. They were not rewritten for novelty.
- The 404 page's **“A loose page”** framing already has restrained personality and remains unchanged.

### Weaknesses selected for revision

- The hero repeated “bring together” language used again in the first how-it-works step.
- The three journey steps had parallel generic headings and hid Zinely's most unusual benefit, automatic page
  arrangement, behind “puts every page in the right place.”
- The About opening used “a simple wish,” language that could belong to almost any small creative app. It did not
  tell the more specific, repository-backed story: the team wanted this exact Android zine tool and could not find it.
- The Changelog and Roadmap link descriptions were correct but administrative.
- The final download block repeated a generic creation invitation instead of giving the reader a concrete spark.

## Changes

- Standardized both primary download actions as **“Get the Android beta”** and changed the hero's secondary action
  to **“Take a look inside.”** The repeated primary label is deliberate because both links perform the same action.
  Their destinations and download URL are unchanged.
- Made the three-step explanation more conversational: gather useful material, move it around freely, then let
  Zinely handle the unusual page arrangement needed for folding.
- Rewrote About around the product's real origin and two-person team. The revised story adds no capability claim.
- Changed the open-notebook heading and link summaries to sound like people reporting their work, not a product
  portal filing system.
- Changed the final invitation to **“There is probably a zine hiding in your camera roll.”** This is the one
  deliberately memorable line, grounded in the shipped photo-import path.
- Added one harmless source-level easter egg for visitors who inspect the HTML. It is optional and does not alter
  page behavior or visible content.

## Validation

- The repository's full `tools/check-website.cjs` suite passed in headless Edge: keyboard behavior, ten-step fold
  controls, reduced motion, mobile and 200% reflow, no-JavaScript and print fallbacks, axe checks, and zero page
  errors.
- W3C Nu 26.9.9 returned zero HTML errors. Its only message was the existing informational advisory that the
  folding guide's `role="list"` is unnecessary on an `ol` element.
- All 27 homepage link targets remain byte-identical to the prior page. `node --check website/assets/site.js` and
  `git diff --check` passed.
- Current desktop and 390-pixel mobile renders of every visibly changed section were inspected without clipping or
  reflow regressions.
- Added website-task lines and this audit contain zero em dash characters. Independent review returned **GO** with
  no required fixes.

## Boundaries and follow-up

- No legal, privacy, accessibility, install-safety, version, platform, availability, roadmap-status, or release copy
  changed.
- No navigation label, route, link target, CSS rule, script, screenshot, or app file changed.
- 🟨 **ASSUMPTION:** “Camera roll” is familiar enough to Android visitors to work as a warm shorthand for photos on
  the phone. First-visitor feedback should test that line rather than treating this review as user evidence.
- 🔭 **FUTURE:** If real visitor feedback shows uncertainty about sideloading, test a separate plain-language
  “How to install the beta” page. Do not make the hero carry more installation detail.
- 🔭 **FUTURE:** Consider a short maker note or build diary only when there is enough genuinely interesting material
  to maintain it. An empty blog would weaken the independent voice rather than strengthen it.
