# Fix rendering: json/pi4j.json

## File
`lottie4j/fxfileviewer/src/test/resources/json/pi4j.json`

## Reproduce
```
mvn -pl fxfileviewer test \
    -Dtest=CompareFxViewWithWebViewTest#compareFxAndJsRenderingFullSize \
    -Dlottie.file=json/pi4j.json
```

## Observed gap (2026-06-25 calibration)
- average: 99.82 % (passes)
- minimum: **98.13 % on frame 19**
- floor:   99.5 % (target)
- target:  99.5 %

## Failing frames
File passes. Two failure clusters and one outlier:

| frame range | overall      | R           | G           | B           | notes                            |
|-------------|--------------|-------------|-------------|-------------|----------------------------------|
| 7–28        | 98.13–99.97  | drops first | drops       | drops       | first motion burst (pi4j logo)   |
| 18–20 (min) | 98.13–98.25  | **98.03**   | 98.15       | 98.20       | **R-channel slightly worst**     |
| 66–86       | 99.43–99.97  | varies      |             |             | second motion burst              |
| 70 (min2)   | 99.53        | 99.69       | 99.41       | 99.50       | small G-channel skew             |
| 150         | 99.72        | 99.74       | 99.72       | 99.71       | minor                            |

Two distinct events: (1) initial logo build-up causes near-equal R/G/B drop —
geometric; (2) a smaller R-skew on a few frames after the build-up.

## Diff evidence
- `target/test-output/json/pi4j-webview_scale_1.0/frame_19_98.13.png` — most
  informative.

## Lottie part diagnosis
- Animated pi4j logo using multiple shape layers with `bm` (blend modes).
- Likely uses `PrecompRenderer` offscreen buffers (similar to `angry_bird`).

## Root cause hypothesis
Same anti-aliased-edge / blend-mode buffer fidelity issue as
`Fix-rendering-angry_bird.md`. The R-skew is too small to point at a colour-channel
bug; it's almost certainly the offscreen buffer composite over a pi4j-red background.

## Proposed fix
Apply `Fix-renderer-blend-mode-offscreen-buffer.md` (cross-cutting).

## Validation
1. Re-run Reproduce.
2. Confirm frame 19 ≥ 99.5 %.

## See also
- `Fix-rendering-angry_bird.md`
- `Fix-renderer-blend-mode-offscreen-buffer.md`
