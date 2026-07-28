# Compose Implementation Rules — the session opener

> **Read this at the start of every Compose implementation session.** It is the distilled, non-negotiable core of
> [COMPOSE-IMPLEMENTATION-GUIDE.md](COMPOSE-IMPLEMENTATION-GUIDE.md) and [V2-CONSTITUTION.md](design/V2-CONSTITUTION.md).
> If you only read one file before writing code, read this — then open the frozen HTML for the screen you're building.

---

## The checklist

Run down this list before and during every session. If you can't tick a box, stop and resolve it.

- ☐ **HTML is canonical.** The frozen prototype is the specification, not a reference. Match its result.
- ☐ **No redesign during implementation.** The design is frozen. Reproduce it; don't improve it.
- ☐ **If the HTML is wrong, fix the HTML first.** Any change to what a screen should look like or do goes into the
  frozen HTML spec first (owner gate) — then into Compose. Never the reverse.
- ☐ **Pixel parity before optimisation.** Make it faithful first, fast second.
- ☐ **Behaviour parity before refactoring.** Match the interaction/animation/editing feel before you tidy the code.
- ☐ **Accessibility is not optional.** Assert against the *platform* a11y tree (TalkBack / `adb uiautomator dump`),
  not only Compose semantics. Every gesture has a named custom-action twin and a visible non-gesture fallback.
- ☐ **Every deviation requires justification.** Platform truth, an HTML bug, or added a11y are the only reasons —
  and each is written down. "Looks better / easier to build" is not a reason.
- ☐ **Every completed phase ends with screenshots** (light + dark), attached to the review.
- ☐ **Every screen requires a side-by-side comparison** against its frozen HTML before it's called done.
- ☐ **No feature creep.** No control, state, screen, or capability that isn't in the frozen spec. Route new ideas
  to the owner.
- ☐ **Repository truth always beats assumptions.** Read the actual file/commit/test/HTML. A summary or memory is a
  claim, not ground truth.

---

## The invariants you can never break (know these cold)

These are constitutional; violating one is a NO-GO regardless of how good the screen looks.

- **One engine, one draw path** — preview == export == read (`CanvasReplayer`, [ADR-028](DECISIONS.md#adr-028)).
  Never a second way to render a page.
- **No per-edit render** — covers/pages are recipe-driven, not cached rasters ([ADR-069](DECISIONS.md#adr-069)).
- **The page never drifts, reflows, or resizes while editing** — rigid whole-page pan, settles back pixel-identical.
- **Never-silent failure + loss-safe back** — export errors always surface; leaving never loses work ([ADR-051](DECISIONS.md#adr-051)).
- **Print honesty** — no fake "Print"; 100% actual size; Save PDF + Share ([ADR-052](DECISIONS.md#adr-052)).
- **READ-first** — the finished-zine reveal belongs to Read, not the Bench ([ADR-058](DECISIONS.md#adr-058)).
- **Chrome = matcha + strawberry + consequence only.** Warmth lives in *content*, never in new chrome colour.
- **Privacy invariant** — no network library, no analytics SDK, no path that uploads user content. Offline-first.
- **Every screen answers its one user question** (Library "which zine?" · Bench "how do I change this page?" ·
  Read "what have I made?" · Proof "how do I print it right?" · Fold "how do I fold it?").

---

## When you're unsure

1. Open the **frozen HTML** for the screen. It probably answers you.
2. If not, check the screen's **authoring spec** ([V2-BENCH-*](design/), [V2-PROOF-*](design/), [V2-TOKENS.md](design/V2-TOKENS.md)) and the relevant **ADR**.
3. Still unsure, or the frozen artifact itself looks wrong? **Stop and raise it with the owner** — don't guess, and
   don't quietly diverge. A silent divergence creates a second source of truth, which is the one thing this whole
   workflow exists to prevent.
4. Then **log it in [V2-SPEC-DEFECTS.md](design/V2-SPEC-DEFECTS.md)** — the register for defects found *in* the
   frozen artifacts (contradictions, stale text, two frozen files disagreeing). Raising it in a session is not
   enough; the session ends and the finding goes with it. Entries are not blockers by default — most are logged,
   classified, and left for the design corpus to clean up — but an entry that genuinely blocks says so, and names
   the phase it blocks.

---

*A one-page distillation. Authority: [V2-CONSTITUTION.md](design/V2-CONSTITUTION.md) ·
[COMPOSE-IMPLEMENTATION-GUIDE.md](COMPOSE-IMPLEMENTATION-GUIDE.md) · [COMPOSE-V2-ROADMAP.md](COMPOSE-V2-ROADMAP.md).*
