# Fix rendering: json/success.json

## File
`lottie4j/fxfileviewer/src/test/resources/json/success.json`

## Reproduce
```
mvn -pl fxfileviewer test \
    -Dtest=CompareFxViewWithWebViewTest#compareFxAndJsRenderingFullSize \
    -Dlottie.file=json/success.json
```

## Observed gap (2026-06-25 calibration)
- average: 100.00 %
- minimum: 99.97 %
- floor:   99.5 % (target)
- target:  99.5 %

## Failing frames
None. **Best-scoring file in the suite.** All frames render at 99.97–100.00 %.

## Diff evidence
No PNGs written — every frame is above target.

## Lottie part diagnosis
Simple checkmark animation. The fact that this file scores 100 % on every frame
proves that the JavaFX renderer can reach perfect parity with WebView when the
animation uses only the well-supported subset (simple paths, flat fills, single
layer, no precomp, no matte).

## Root cause hypothesis
Nothing to fix. **This file is the "control":** any regression that drops it below
99.97 % indicates a renderer change with side effects.

## Proposed fix
No change. Use as the regression sentinel for every other fix.

## Validation
After applying any cross-cutting fix from another plan, re-run Reproduce and confirm
this file still scores ≥ 99.97 %.

## See also
- (none — sentinel)
