# Fix rendering: json/foojay-duke.json

## File
`lottie4j/fxfileviewer/src/test/resources/json/foojay-duke.json`

## Reproduce
```
mvn -pl fxfileviewer test \
    -Dtest=CompareFxViewWithWebViewTest#compareFxAndJsRenderingFullSize \
    -Dlottie.file=json/foojay-duke.json
```

## Observed gap (2026-06-25 calibration)
- average: 99.96 % (passes target)
- minimum: 99.83 % (passes target)
- floor:   99.5 % (target)
- target:  99.5 %

## Failing frames
No frames below 99.5 %. The lowest is 99.83 %. This plan exists only as a regression
sentinel — record the lowest-scoring frames so a future change that breaks them is
visible.

| frame    | overall | R     | G     | B     | notes                                |
|----------|---------|-------|-------|-------|--------------------------------------|
| 56 / 61 / 63 / 68 | 99.90  | 99.90 | 99.90 | 99.90 | tiny uniform drop on Duke motion |
| 122–139  | 99.83–99.91 | uniform | uniform | uniform | larger uniform drop, ~0.15 pp |
| 144–157  | 99.85–99.94 | uniform | uniform | uniform | second motion burst |

Channels are virtually identical across R/G/B → pure geometric edge tax from
anti-aliased shapes during motion.

## Diff evidence
Only the regression sentinel: no diff PNGs are written (all frames ≥ 99.5).

## Lottie part diagnosis
- Duke character moving with shape layers. Uses similar precomp infrastructure to
  `angry_bird.json` (just less motion).
- No `gf`/`tt` checked.

## Root cause hypothesis
Same edge-AA / pre-comp buffer fidelity bug as `Fix-rendering-angry_bird.md`, but to a
much smaller extent. Fixing `Fix-renderer-blend-mode-offscreen-buffer.md` (or whatever
the angry_bird investigation surfaces) should also push these scores closer to 100 %.

## Proposed fix
No change needed today. If future calibration shows any frame dropping below 99.5 %,
revisit `Fix-renderer-blend-mode-offscreen-buffer.md`.

## Validation
1. Re-run Reproduce.
2. Confirm minimum frame remains ≥ 99.5 %.

## See also
- `Fix-rendering-angry_bird.md`
- `Fix-renderer-blend-mode-offscreen-buffer.md`
