# Selected-text Font-control removal experiment

**Status:** HTML counter-prototype prepared; no user result, freeze amendment, owner supersession, or Compose
change is claimed.

## Question and boundary

The selected-text toolbar currently includes an unavailable **Font** action because OD-9 explicitly ruled that the
frozen control must remain visible without inventing functionality. This experiment asks one narrower question:
does temporarily removing that dead action improve first-time use without hiding or confusing the five live actions?

The experiment wrapper loads the canonical [`v21-bench.html`](../design/mockups/v21-bench.html) in both conditions.
Condition B filters only `Font` from the selected-text action list after every canonical amendment has loaded. It does
not edit the frozen HTML, delete the shared Font icon, implement font choice, or change Compose.

## Conditions

Serve the repository root over HTTP and open the same viewport in both conditions:

- **A — current:** `docs/design/experiments/v21-font-control-removal.html?v=0`. The wrapper marks Font disabled
  to match the current Android behavior; the frozen mockup itself is unchanged.
- **B — no Font:** `docs/design/experiments/v21-font-control-removal.html?v=1`
- **Text-size stress:** append `&scale=1.8` to either URL. This is a web-layout approximation, not Android
  font-scale acceptance.

Use randomized, balanced A/B cohorts. Each first-time maker receives one scored condition from the same fresh state
and the same script; any later crossover is exploratory and must not enter the primary result. Avoid naming the
difference. Use a 360 CSS-pixel viewport for the narrow pass and the prototype's ordinary responsive presentation
for the default pass.

## Uncoached task

Ask a first-time maker to select an existing text element, then point to — without activating — the first control
they would use to:

1. edit its words;
2. change its size;
3. change its ink;
4. duplicate it;
5. delete the duplicate.

Record the first control chosen for each task, hesitation, spontaneous Font attempts, searching outside the toolbar,
and whether the maker believes font choice exists. Do not teach the toolbar during the task. This is a first-choice
discoverability comparison only: the canonical mockup does not implement every downstream action (notably Size), so
this prototype cannot validate action completion.

## Decision rule

Recruit at least 16 first-time makers: eight randomized to A and eight to B. Recommend removal only if at least three
of eight A makers show Font-specific harm — hesitation, spontaneous activation, or a false capability expectation —
while no more than one of eight B makers shows the same harm. For every live action, B's intended-first-choice count
must be no more than one maker below A's count; a larger drop is a material B regression. Do not authorize removal
from a single participant or anecdote.

If nobody notices or attempts Font, benefit remains unsupported and OD-9 stays in force. A passing comparison still
does not change Compose: the owner must explicitly supersede OD-9, amend and re-freeze canonical HTML first, then
approve keyboard/accessibility focus-order, human TalkBack, 360dp/default, 1.8× text, golden, and device gates.

## Current verification limit

The repository artifact can be inspected and its exact A/B action lists verified mechanically. No browser was
available in the authoring session, so visual and keyboard browser acceptance remain pending. The connected Samsung
can verify the current installed app but cannot represent the unshipped HTML-only condition B.
