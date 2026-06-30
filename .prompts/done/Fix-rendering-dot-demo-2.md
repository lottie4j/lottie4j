# Fix rendering: dot/demo-2.lottie

## File
`lottie4j/fxfileviewer/src/test/resources/dot/demo-2.lottie`

## Reproduce
```
mvn -pl fxfileviewer test \
    -Dtest=CompareFxViewWithWebViewTest#compareFxAndJsRenderingFullSize \
    -Dlottie.file=dot/demo-2.lottie
```

## Observed gap (2026-06-25 calibration)
- average: 99.96 % (passes)
- minimum: **99.83 % on frame 202**
- floor:   99.5 % (target)
- target:  99.5 %

## Failing frames
All ≥ 99.5 %. Sentinel:

| frame | overall | R     | G     | B     | notes                              |
|-------|---------|-------|-------|-------|------------------------------------|
| 202   | 99.83   | 99.85 | 99.84 | 99.80 | mid-animation sub-pixel dip        |
| 304   | 99.90   | 99.90 | 99.90 | 99.89 | second small dip                   |
| 306   | 99.88   | 99.88 | 99.89 | 99.89 |                                     |
| 307   | 99.87   | 99.86 | 99.86 | 99.88 |                                     |

R/G/B move together — pure sub-pixel geometric noise.

## Diff evidence
No PNGs written.

## Lottie part diagnosis
Unknown without unpacking the `.lottie` archive — likely simple shape layers.

## Root cause hypothesis
No active bug. Regression sentinel.

## Proposed fix
No change.

## Validation
Re-run Reproduce; confirm min ≥ 99.5 %.

## See also
- (none)
