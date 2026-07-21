# ZINELY V1

**Authority:** subordinate only to THE CONSTITUTION OF ZINELY. Where this document and the Constitution conflict, the Constitution wins. Where any roadmap, prototype, ADR, or implementation conflicts with this document, this document wins — until this document is amended, nothing outside it enters v1.
**Baseline honesty:** Zinely is not greenfield. The bench/shelf/proof trilogy, imposition engine, parity renderer, durability layer, and Save-to-phone are built and shipped (v0.8.0). This document defines what *v1 the product* is — much of it exists; the gap between v0.8.0 and this definition is the ship-blocker list (§7).

---

## 1. Executive Summary

Zinely v1 is **the complete finish loop, twice-embodied**: a person who has never made anything starts a zine, finishes it in one sitting, and holds the result in two bodies — a paper booklet if they have a printer, a flip-through digital zine they can share either way. Nothing in v1 exists for any other reason.

The team (1 founder, 5 engineers, 1 designer, no money for content production) buys exactly four things:

1. **Nobody's work lies to them or dies** — non-Latin honesty, user-held backup, preview-equals-output (Articles 2, 3, 5).
2. **The first minute produces something worth keeping** — fold moment, seeded starter, finished-looking starting points (Article 6).
3. **Every finished zine gets both bodies** — read mode + portable export as the digital body; the existing honest print path, de-risked with a test sheet, as the paper one (Article 2).
4. **One moment of theater** — the imposition reveal, the product's signature, because a finishing tool must make finishing *feel* like something (Articles 2, 5; North Star).

Everything else — supplies packs, page reordering, spreads, ritual polish — is deliberately absent, and §8 says so out loud. V1 is not "version one of a bigger app." It is the whole promise, kept small: **small enough to finish, real enough to give, yours enough to keep.**

## 2. Product Definition

**Zinely v1 is a free Android app that takes you from a spark to a finished 8-page zine in one sitting, entirely on your device — and hands you the finished thing as a paper booklet, a shareable digital zine, or both.**

One format (single sheet → 8 pages, A4/Letter). Two block types (text, photo). A small set of fonts and text styles. Six-to-eight finished-looking starters. No accounts, no network, no notifications, nothing that watches you. What you preview is what you get; what you make is yours, survives your phone, and never needs us again.

## 3. The Essential Experience

Five feelings, in order, that v1 must produce — this is the product; screens are just where it happens:

1. **"Oh, it turns into a book."** (first 10 seconds — the fold moment)
2. **"I can do this."** (first minute — a starter that already looks like a zine, made mine with one edit)
3. **"I'm actually going to finish this."** (minute 5 — visible 8-page ladder, no dead ends, everything undoable)
4. **"Watch this."** (the reveal — my pages scramble onto the sheet and fold into my book)
5. **"Here."** (the hand-off — a printed booklet in a palm, or a flip-through zine on a friend's screen)

If a proposed change doesn't serve one of these five, it isn't v1.

## 4. Core User Journey

**Minute 0 — install → open.** No account, no permission wall, no tour. A 4-second wordless fold animation answers "what is this app": flat sheet → fold → tiny book flips open. One question: "What's your first zine about?" (photos / words / a gift / surprise me — skippable). The shelf appears with one seeded starter already on it, titled, cover showing. *(Art 6; Art 4 — nothing was asked of them, nothing was collected.)*

**Minute 1 — first mark.** They open the starter. It already looks like a zine. The cover invites the first edit ("tap to make it yours"). One tap → keyboard → their word replaces ours. Undo is visible. Nothing they can do is unrecoverable. *(Art 6, 7 — starter bright line: overwriting is the path of least resistance.)*

**Minute 5 — momentum.** They've replaced a photo (≤3 taps from tray to placed, drag/pinch to reframe, steppers as the accessible twin), typed on two pages. The rail shows 8 pages, some filled — the ending is visible from here. Empty pages invite specifically; the tray holds exactly Text and Photo. If a character can't print, they're told now, kindly, not at export. *(Art 1, 5, 6.)*

**First finished zine — the reveal.** They tap Finish (Proof). Their eight pages animate onto the flat sheet — some rotating upside-down — and the sheet folds itself into their book and flips through. The scramble is explained by being *performed*. Then two doors, equal weight: **Hold it** (print path) and **Send it** (digital body). *(Art 2, 5; the signature moment.)*

**Printing.** The existing honest recipe (scale 100% · landscape · single-sided · paper size), Save-to-phone (shipped), and one new kindness: **print the test-fold sheet first** — a near-blank page with fold guides and an alignment mark, so the first wasted sheet is a cheap deliberate one. If it came out wrong, a visual triage answers *shrunk? sideways?* instead of a dead end. Then the fold steps (existing Act 3). *(Art 2, 5.)*

**Digital sharing.** **Read mode:** their zine as a flip-through little book on screen — this is also how they *understand what they made*. **Send:** export page images (share-sheet ready, carousel-shaped) or the PDF. Everything leaves by their hand, through the OS share sheet, to destinations they control. *(Art 2, 3.)*

**Second zine.** Back on the shelf their zine sits as a real little book — its actual cover, not a placeholder. "Start another" offers blank, starters, and a small browsable prompt list. *(Art 5, 7 — their work displayed as theirs; Art 4 — the prompt list waits, it never arrives.)*

**Returning next week.** Nothing summoned them — they came back. A Continue card resumes the in-flight zine at the top of the shelf ("page 5 of 8"). Their finished zines are there, on their device, exactly as left. Somewhere quiet: "Back up your zines" — one tap, one file, any destination they own. *(Art 3, 4; North Star — return rate, unbribed.)*

## 5. Feature Tribunal

Verdicts: **REQUIRED** (v1 ships with it) · **OPTIONAL** (ships if it costs nothing it shouldn't; first to cut) · **DEFER** (post-v1, article-approved) · **REJECT** (constitutionally dead). Every verdict cites Articles.

> **Formal note:** where this table defers items the Constitution's tribunal marked KEEP (supplies, user packs, page ops, facing pairs, fold ritual), this document is exercising the Constitution's own rank rule — the tribunal is a ratified *application*, amendable at roadmap rank — and this section constitutes that amendment. No article is overturned; sequencing is.

> **"Small" means:** the smallest *constitutionally complete* product — not calendar-small. Eighteen REQUIRED rows on this team is quarters of work, and the honest defense is that every row killed below was already the cheaper alternative's funeral.

| Feature | Verdict | Articles / reasoning |
|---|---|---|
| Text honesty: input-time coverage warning + **scoped** fallback coverage | **REQUIRED** | Art 5, 2 — silent blank text is the standing violation. **Scope, stated honestly:** v1 ships a defined script set — Latin + Latin-Extended, Cyrillic, Greek (Noto subsets, single-digit-MB APK budget). Complex-shaping scripts (Arabic/RTL, Indic, CJK) are *out of v1*: any character outside the set is flagged kindly at typing time, never silently blanked. Widening the set is a post-v1 priority, not a v1 promise. |
| User-held backup / restore / device migration (one file, user destinations only) | **REQUIRED** | Art 3 — constitutional duty; ownership includes survival. No Zinely servers, ever. |
| Real covers on shelf (wire existing thumbnail pipeline) | **REQUIRED** | Art 5, 7 — showing placeholders instead of their work is a standing dishonesty; pipeline already built. |
| Read mode (flip-through the finished zine) | **REQUIRED** | Art 2 — the digital body's face; also how users understand what they made. |
| Export: page images (share-ready) + PDF via share sheet | **REQUIRED** | Art 2, 3 — the portable body; leaves only by the user's hand. |
| Imposition reveal (pages → sheet → folded book, animated; reduced-motion path) | **REQUIRED, with a floor** | Art 2, 5 — truth performed; the signature. The one delight investment v1 makes. **Degradation floor (so it can shrink instead of slipping the date):** minimum acceptable = pages animating into their sheet positions + a simple fold-to-book transition; the flip-through flourish is the cuttable layer. |
| First-run: 4s fold moment + one question + seeded starter — skippable at every step | **REQUIRED** | Art 6 — the first minute; never gates the editor. |
| 4–8 finished-looking starters | **REQUIRED** | Art 6, 7 — the evidence-backed "aha"; starter bright line applies (overwrite = path of least resistance). Honest cost note: starters + fonts + presets + first-run *are* content production on one designer — the same test that deferred supplies. Four excellent starters beat eight adequate ones; the count flexes down, never the quality. |
| Direct manipulation (drag/pinch reframe, drag blocks) with steppers/buttons as accessible twins | **REQUIRED** | Art 6, Sacred 8 — the editing philosophy; twins are constitutional, not optional. |
| Contextual verb bar (selected block → its verbs; nothing selected → add) | **REQUIRED** | Art 6 — progressive disclosure in its load-bearing spot. |
| Snapping + alignment guides (center, margins, safe area) | **REQUIRED** | Art 6, 2 — converts fat fingers into finished-looking pages; print punishes "almost." |
| Small bundled font set (~8–12 OFL faces, vibe-grouped, rendered in-face) + 6–10 text style presets | **REQUIRED** | Art 7, 6 — authorship needs a voice; presets encode taste so beginners don't need any. This is v1's entire "supplies." |
| Errors get exits (retry + alternate path + honest cause) | **REQUIRED** | Art 5 — warm dead-ends are banned. |
| Test-fold sheet (fold guides + alignment mark) + post-print triage | **REQUIRED** | Art 2, 5 — the paper body treated with respect; nearly free (one static PDF + one screen). |
| Continue card on shelf | **REQUIRED** | North Star — finishing's cheapest lever; resumes the almost-done zine. |
| Settings (backup, licenses, fold help, about, theme) | **REQUIRED** | Art 3, 5 — backup and license attribution need a home; minimal surface. |
| Dark theme (follow system) | **OPTIONAL, leaning keep** | Tokens exist; following the system setting is near-free and matters to low-vision/photosensitive users (Art 6-adjacent). Cut only under real schedule pressure, and say so in release notes if cut. |
| Static prompt list inside "Start another" (~20 prompts, bundled) | **OPTIONAL** | Art 6 — cheap (a list); Art 4 bounds its form (browsed, never delivered). |
| Copy-shop guidance (one screen: "any copy shop can print this PDF") | **OPTIONAL** | Art 2 — the PDF already works there; this is copy, not code. |
| Share card ("look what I made" composite image) | **OPTIONAL** | Art 2 — page-image export already covers the need; the card is garnish. |
| Bundled sticker/tape/texture packs | **DEFER** | Art 7 permits, Art 1 (applied to our own inventory) and the cost test defer: content production is designer-months v1 doesn't have, and text+photo+fonts already finishes zines. First post-v1 priority. |
| User-created packs from own photos | **DEFER** | Art 7's purest expression, but it needs pack UI that shouldn't be built before any pack exists. Ships with supplies. |
| Page duplication / reordering | **DEFER** | Art 1-compatible convenience; nothing in the finish loop needs it at 8 pages. |
| Facing-pairs strip | **DEFER** | Art 2 — the reveal teaches the fold first; strip is second-year depth. |
| Fold-along ritual upgrade (full-screen) | **DEFER** | Existing Act 3 fold steps suffice; polish later. |
| Panoramic spreads + smart slicing | **DEFER** (indefinitely, per Constitution) | Charm without a constitutional debt paid. |
| 16-page / other formats | **DEFER** | Art 1 procedure exists (visible ending + one sitting + paper prototype); not v1's question. |
| Drawing / handwriting layer | **DEFER** | Art 7 someday; different input pipeline; after debts. |
| Downloadable pack catalog / manifests / seasonal drops | **REJECT** | Art 4 (treadmill), Art 3 (strains) — constitutionally dead. |
| Prompt-of-the-day / daily framing / streaks / graduation counters | **REJECT** | Art 4 — the quiet is sacred. (A finished zine may *look* finished on the shelf — state, not score.) |
| Accounts, cloud sync, telemetry, notifications, social, authoring AI | **REJECT** | Arts 3, 4, 7 — "We proudly do not build." |
| Monetization (any) | **REJECT for v1** | Constitution's trial clock starts at first supplies release, which is post-v1. V1 is free, entirely. |

**The kill ledger (what keeping REQUIRED cost):** supplies packs, user packs, page ops, facing pairs, spreads, fold ritual, share card polish, education kit production. Each was killed *by* something above it: backup killed supplies (trust before materials); read mode killed facing pairs (the digital body before fold pedagogy); the reveal killed the fold-along upgrade (one theater budget, spent once).

## 6. Definition of Done — user truths

V1 ships when every sentence below is true, verified with real people (the consented instruments: named beta cohort, field sessions — Art 3 forbids learning these any other way):

1. **A first-time user finishes a zine in one sitting without asking anyone anything.** (North Star; Art 6)
2. **Work survives a named torture matrix.** Force-kill mid-edit, process death mid-save, disk-full during save, uninstall-after-backup, restore onto a second device, restore a newer backup into an older app (honest refusal, not corruption) — every case in the written matrix passes, on real hardware. (Art 3)
3. **What you preview is what you get.** All four surfaces (print PDF, page images, read mode, editor preview) render from the same display list; PDF and page images are pixel-verified against golden renders at defined resolutions; read mode is the same composition at screen scale. Zero layout differences, ever. (Art 2)
4. **Nothing renders blank, silently.** Every character in the supported script set (Latin/Latin-Ext, Cyrillic, Greek) renders on all four surfaces; every character outside it is flagged at typing time, kindly, before any work is invested. Zero paths where text shows in the editor and vanishes in a body. (Art 5, 2)
5. **The first print is not a surprise.** Test sheet first; recipe stated in plain words; if it still comes out wrong, the app names why and what to change. (Art 2, 5)
6. **A user who never prints still finishes with a real thing** — flip-through-able in read mode, sendable as images or PDF, complete without a printer anywhere in the story. (Art 2)
7. **The user can point at the zine and say "I made this"** — starters get overwritten in practice (watch the beta cohort; if starters ship unmodified, Article 7 is failing and starters get redesigned). (Art 7)
8. **Every action has an accessible path.** TalkBack completes the entire journey — start, edit, finish, both bodies. (Art 6)
9. **Airplane mode changes nothing.** Install-to-share, zero network. (Art 3)
10. **Nothing ever asked them to come back.** Zero notifications shipped; returning is unbribed. (Art 4)

## 7. Ship Blockers (gap between v0.8.0 and this document)

Ordered; each cites the truth it unblocks:

1. Non-Latin fallback + input-time honesty → DoD 4 (worst standing violation)
2. Backup/restore/migration → DoD 2
3. Real shelf covers (wire ADR-045 pipeline) → DoD 3, 7
4. Read mode + page-image export → DoD 6
5. First-run (fold moment, question, seeded starter) + 6–8 starters → DoD 1, 7
6. Direct-manipulation reframe/move + verb bar + snapping (twins kept) → DoD 1, 8
7. Font set + text presets → DoD 1, 7
8. Imposition reveal → Essential Experience #4
9. Test-fold sheet + print triage + errors-get-exits → DoD 5
10. Settings surface (backup + licenses need the home) → DoD 2
11. Full-journey TalkBack pass + beta-cohort verification of DoD 1–10 → ship gate

Note: the Continue card is new work — it is blocker 5's sibling (shelf surface, ships with first-run changes).

Existing and already compliant (keep, don't rebuild — these are why v1's journey holds together without rows in §5): imposition engine, parity renderer, durability/atomic saves, Proof three-act structure + fold steps, Save-to-phone, editor MVI with undo/redo, TypeBar, Reframe stepper model, honest print recipe, per-zine Rename / Duplicate / Share / undoable Delete on the shelf, per-block Duplicate / Delete / z-order verbs, canvas zoom. Table-stakes are assumed *because they exist*; if any turns out weaker than claimed, it becomes a ship blocker automatically.

## 8. Things Intentionally Missing (say it before reviewers do)

No stickers or decorative packs — text, photos, and type are enough to finish a real zine, and materials come after trust. No page reordering. No spreads. No second format. No collaboration. No cloud. No accounts. No AI. No notifications. No prices. If you missed one of these in v1: that's the product working as constituted — §5 records which article made the call. V1 is complete the way a pencil is complete.

## 9. First Post-v1 Priorities (pre-committed, so v1 stays small honestly)

1. **Supplies, small and alive** — 2–3 excellent license-clean bundled packs **+ user packs from own photos** (they ship together; the pack UI serves both). Starts the Constitution's monetization decision clock.
2. **Page duplication/reordering** (cheapest deferred convenience).
3. **Facing-pairs strip** (fold awareness while editing).
4. **Education kit + zine-fair presence** (channels — near-zero code, high fit; can begin any time people are free, it's outside the app).
5. **Fold-along ritual + share card polish** (spend the second theater budget only after the first proves out).

---

*Amendment rule: features enter v1 only by amending this document, with a tribunal row citing Articles. The Constitution wins every conflict. Cut mercilessly; when uncertain, cut.*
