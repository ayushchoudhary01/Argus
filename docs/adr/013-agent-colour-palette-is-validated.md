# 013 — The Agent Colour Palette Is Measured, Not Chosen

## Status
Accepted

## Context
The dashboard distinguishes three agents — Structural, Risk and Contrarian — largely by
colour. Agent identity is load-bearing here in a way it is not in most dashboards: the
entire product is "these three analysts disagree, and here is where", so a reader who
cannot tell two agents apart cannot read the output at all.

The original palette was chosen by eye: blue `#3b82f6` for Structural, amber `#f59e0b`
for Risk, violet `#8b5cf6` for Contrarian.

## Decision
Categorical colours are selected by running a palette validator against the actual
rendering surface, and only values that pass every gate are used. The current set is:

| Agent | Hex |
|---|---|
| Structural | `#3987e5` |
| Risk | `#d95926` |
| Contrarian | `#199e70` |

Returns use a diverging pair — gain `#199e70`, loss `#e66767`, neutral midpoint
`#383835` — and status colours are a separate fixed set that never doubles as a series
colour.

## Reasoning
The eyeballed palette failed outright when measured against the card surface `#18181b`:

```
[FAIL] Lightness band      #f59e0b outside band (L 0.769)
[FAIL] CVD separation      #8b5cf6 <-> #3b82f6  dE 1.3 (deutan)
[FAIL] Normal-vision floor #8b5cf6 <-> #3b82f6  dE 12.0 — below the 15 floor
```

A deuteranopic reader could not distinguish the Structural agent from the Contrarian
agent — the two whose conflict is the system's headline output. At dE 12.0 the pair is
marginal even with full colour vision.

The replacement passes all gates all-pairs in dark mode: worst CVD separation dE 9.4,
worst normal-vision separation dE 20.9, every slot inside the lightness band and above
3:1 contrast against the surface.

The gain/loss pair measures dE 6.5 under protanopia, which sits in the band that is only
permissible alongside secondary encoding. Every return in the historical panel therefore
renders a signed value and a direction arrow next to the bar — colour never carries the
sign alone. The pair was kept despite the warning because green-up / red-down is a
near-universal convention in finance, and breaking it to gain separation would cost more
comprehension than it bought.

## Consequences
- Agent identity survives colourblindness, which it previously did not
- Colour values cannot be changed casually; the tokens in `index.css` carry their
  measurements in a comment, and substitutions must be re-validated against the surface
- Status colours (`good` / `warning` / `serious` / `critical`) are reserved and always
  ship with an icon and a word, so no state is signalled by hue alone
- One correctness constraint is inherited: the diverging pair's secondary encoding is
  mandatory, not decorative, and removing the arrows or the sign would make the chart
  non-compliant rather than merely plainer

## Note
This was found by running a validator, not by inspection. The failing pair looked
perfectly distinct to the author in both cases.
