# Fix rendering: json/foojay-reporter.json

## File
`lottie4j/fxfileviewer/src/test/resources/json/foojay-reporter.json`

## Reproduce
```
mvn -pl fxfileviewer test \
    -Dtest=CompareFxViewWithWebViewTest#compareFxAndJsRenderingFullSize \
    -Dlottie.file=json/foojay-reporter.json
```

## Observed gap (2026-06-25 calibration)
- average: 99.89 %
- minimum: 98.78 % on frame 40
- floor:   99.5 % (target)
- target:  99.5 %

## Failing frames
A burst of failures between frames 31–71, then steady-state ≥ 99.97 % thereafter:

| frame range | overall      | R     | G     | B     | notes                                |
|-------------|--------------|-------|-------|-------|--------------------------------------|
| 31–40       | 98.78–99.89  | uniform drop | uniform drop | uniform drop | onset of an animated element |
| 40          | **98.78**    | 98.79 | 98.74 | 98.82 | min — channels nearly equal          |
| 41–58       | 98.81–99.98  | recovering   | recovering   | recovering   | animation completing         |
| 60–71       | 99.58–99.81  | small dips | | | second smaller burst                   |

R/G/B drop uniformly → geometric (positional / scaling) bug, not chromatic.

## Diff evidence
- `target/test-output/json/foojay-reporter-webview_scale_1.0/frame_40_98.78.png` — most
  informative.

## Lottie part diagnosis
- Foojay reporter animation; shapes that pop in/out around frames 30–60.
- No `gf`/`tt` confirmed in spot-check.

## Root cause hypothesis
**Keyframe boundary** off-by-one or sub-pixel positioning during fast motion. Same
class as `lottie_lego` and `sandy_loading`. The recovery curve from 98.78 → 99.97 over
~20 frames is a tell-tale sign of an interpolation curve not matching lottie-web's
bezier easing exactly.

## Proposed fix
- `lottie4j/fxplayer/src/main/java/com/lottie4j/fxplayer/renderer/shape/PathBezierInterpolator.java`
  — bezier easing solver fidelity vs lottie-web `BezierEasing`.
- `lottie4j/core/src/main/java/com/lottie4j/core/model/keyframe/` — segment boundary
  evaluation at exactly `t == k.t`.

## Validation
1. Re-run Reproduce.
2. Confirm frame 40 ≥ 99.5 % and the entire 31–71 cluster recovers above 99.5 %.

## See also
- `Fix-renderer-bezier-easing-fidelity.md`
- `Fix-renderer-keyframe-segment-boundary.md`
