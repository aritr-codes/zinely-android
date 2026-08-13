# P2 before it starts — the keep-clear, the snap guide, and the state nobody measured

**Date** 2026-08-13 · **For** the owner · **Status** Proposal; one question, and it is not the one on file.

P2 is `BenchKeepClear` + `SnapGuides`, colour and trigger together
([ADR-102 §12.3](../DECISIONS.md#adr-102-p1-recut)). [§12.6 row 4](../DECISIONS.md#adr-102-p1-corrections)
fenced it with a filed conflict: *"`.guideV` is butter, and V2.1's own spec forbids it"* — butter measured
at 1.73:1 against a spec that says butter carries **"no action, no text, and no state alone."**

**That conflict is real and it is not the blocker.** Measuring the whole surface instead of the one token
changes the question.

---

## 1. What is actually on the sheet, measured

The sheet is a light island in both themes, so one measurement answers both. Composited at the alpha each
mark actually ships or specifies, against the paper it is drawn on. **1.4.11 (AA) needs 3:1** for a
graphical object required to understand the content.

| Mark | Colour | α | Composited | Ratio | |
|---|---|---|---|---|---|
| **Frozen** keep-clear | `berry` | .85 | `(232,152,170)` | **2.06:1** | ✗ |
| **Frozen** snap guide | `butter` | .85 | `(247,188,72)` | **1.60:1** | ✗ |
| **Shipped** keep-clear, at rest | `inkFaint` | .32 | `(213,206,191)` | **1.40:1** | ✗ |
| **Shipped** snap guide | `matcha` | .60 | `(153,172,137)` | **2.18:1** | ✗ |
| **Shipped** keep-clear, **warning** | `strawberryText` | 1.0 | `(179,65,47)` | **5.06:1** | ✓ |

✅ **VERIFIED — four of the five marks fail 3:1, and four of them are failing *today*.** The freeze does
not introduce this. For the keep-clear the freeze is an **improvement** (1.40 → 2.06, still failing); for
the guide it is a mild worsening (2.18 → 1.60). Nobody has measured either mark before, in either design.

✅ **VERIFIED — no opacity rescues either frozen hue.** At α = 1.0, `berry` reaches 2.37:1 and `butter`
1.73:1 on paper. **These hues cannot carry a state at any alpha**, which is precisely what
`ZinelyV21ContrastTest:144-148` already says in its own words: *"butter alone never could [clear 1.4.11] —
the outline carries the state."*

## 2. The real blocker: the freeze deletes the only mark that passes

`v21-bench.html:186-190` gives the keep-clear **one colour and no warn state**. The shipped composable has
two: `strawberryText` when the interaction's geometry crosses the printer's reach, `inkFaint` otherwise.

That warn state is the single mark on this surface that both **carries information available nowhere else**
— *your content is heading off the printable area* — and **clears 1.4.11, at 5.06:1.**

So the shipped design, whatever else is wrong with it, already puts its contrast exactly where its
information is. Implementing the freeze as written would delete that and leave the surface with two
decorative lines and no accessible warning.

🟦 **This is the same shape as [§12.6 row 5](../DECISIONS.md#adr-102-p1-corrections)** — the handle halo the
freeze does not have, kept because *"a handle sits on the user's photograph"* and the owner's standing
priority puts accessibility above fidelity to the freeze. It is also [OD-11](../DECISIONS.md#adr-098-od11):
deleting the warn state removes a shipped capability during a parity phase.

## 3. What I recommend

**Keep the warn state; implement everything else as frozen; accept the two resting marks below 3:1 and say
so in writing.**

| | Ruled how |
|---|---|
| Keep-clear **warn** | **kept** — `jamText` (V2.1's `strawberryText`, 6.00:1 on paper), documented departure, halo family |
| Keep-clear **rest** | frozen `berry` at .85 — **2.06:1, accepted as decorative** |
| Snap guide | frozen `butter` at .85 — **1.60:1, accepted as decorative** |
| Trigger | frozen — both hidden at rest, revealed only while focusing |
| Geometry | frozen: 1.5dp dashed, guide inset 8px from the page's edges |
| Keep-clear inset | **shipped**, not frozen — the engine's real safe area, not the CSS's flat `14px` |

**Why the two resting marks can be accepted below 3:1, stated so it can be argued with:** they are
*redundant* in the 1.4.11 sense once the warn state exists. The resting keep-clear says "the printer's
reach is about here"; the warn state says "you are crossing it" at 5.06:1, and only the second is
information the user cannot otherwise obtain. The snap guide reports an alignment the user can see
happening to the element itself, transiently, during their own drag.

⚠️ **That argument is exactly [D-064](design/V2-SPEC-DEFECTS.md)'s open question** — whether the ≥3:1 floor
binds a decorative mark — applied to two more marks. It should be recorded as the same ruling, not invented
twice.

**The alternative, if the answer is that the floor binds:** neither frozen hue survives, at any alpha, and
both marks become an ink-class stroke (`inkSoft`, 4.35:1 at the frozen α) with the accent hue dropped or
demoted to a companion tone. That is a visible departure from the freeze on two of its marks, and it should
be an explicit choice rather than something I arrive at by arithmetic.

## 4. What P2 builds either way

Unaffected by the ruling, and where the work actually is:

1. **The trigger.** Both marks move from a resting state to `opacity:0 → .85` gated on focusing. The
   composables have no such gate today; `EditorScreen.kt:966` passes the keep-clear a `warn` flag and
   nothing else.
2. **The V2.1 palette.** Both read `ZinelyTheme.v2Colors` today and must read `v21Colors` inside the
   island.
3. **Geometry.** `Hairline` → 1.5dp; the guide gains its dash.
4. **The focus wash finding from P1's device pass** — it covers the page's own border and page number
   ([§12.7](../DECISIONS.md#adr-102-p1-review)). Narrowing it to the artifact is a behaviour change and it
   lives here.

Items 1–4 are ~a day. The colour ruling gates whether they land as one package.
