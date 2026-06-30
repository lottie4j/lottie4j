# Fix rendering: dot/demo-3.lottie

## File
`lottie4j/fxfileviewer/src/test/resources/dot/demo-3.lottie`

## Reproduce
```
mvn -pl fxfileviewer test \
    -Dtest=CompareFxViewWithWebViewTest#compareFxAndJsRenderingFullSize \
    -Dlottie.file=dot/demo-3.lottie
```

## Observed gap (2026-06-25 calibration)
- average: 99.74 % (passes)
- minimum: **96.84 %** in the **first frames** (the animation has a stark first-frames
  cliff, mirror image of the typical last-frames cliff)
- floor:   99.5 % (target)
- target:  99.5 %

## Failing frames
Striking pattern: **the failure is at the START**, not the end. Frame 0 scores 99.94 %,
then 1 → 13 dip sharply, then recover to ≥ 99.5 % and stay there:

| frame range | overall      | R           | G           | B           | notes                                  |
|-------------|--------------|-------------|-------------|-------------|----------------------------------------|
| 0           | 99.94        | 99.96       | 99.94       | 99.91       | first frame ok                         |
| 1           | 98.96        | 98.89       | 98.92       | 99.06       | sudden dip                             |
| 2           | 98.00        | 97.82       | 97.87       | 98.31       |                                         |
| 3           | 97.31        | 97.02       | 97.10       | 97.81       |                                         |
| 4           | 96.98        | 96.66       | 96.75       | 97.54       |                                         |
| 5–6 (min)   | **96.84**    | 96.49       | 96.60       | 97.43       | **bottom of the dip**                  |
| 7–12        | 97.04–98.92  | recovering  | recovering  | recovering  | recovery curve                         |
| 13–261      | ≥ 99.5       | uniform     | uniform     | uniform     | steady state                           |

The recovery curve from 96.84 % → 99.49 % over 7 frames mirrors the *out-point cliff*
pattern but at the in-point. R/G/B nearly equal → geometric/animation timing bug.

## Diff evidence
- `target/test-output/dot/demo-3-webview_scale_1.0/frame_5_96.84.png`
- `target/test-output/dot/demo-3-webview_scale_1.0/frame_4_96.98.png`

## Lottie part diagnosis
Without unpacking the `.lottie` archive: likely an `ip > 0` (animation starts at frame
N > 0) with a brief entry-animation that lottie4j's player evaluates one frame
*differently* from WebView at the in-point boundary.

## Root cause hypothesis
**In-point (`ip`) boundary bug** — the symmetric counterpart to the out-point bug seen
in many other files. The renderer evaluates `t < ip` keyframes differently than
WebView's player, causing a few frames of wrong state until the keyframe segment kicks
in.

## Proposed fix
- Inspect `LottiePlayer.seekToFrame(int)` for `frame < ip` handling.
- `lottie4j/core/src/main/java/com/lottie4j/core/model/keyframe/` — segment search for
  `t < firstKeyframe.t` must return the first segment's start value (or the keyframe
  itself, depending on whether `firstKeyframe.t > ip`).
- Likely the same fix as `Fix-renderer-outpoint-frame.md` — combine into a single
  cross-cutting plan covering both `ip` and `op` boundaries.

## Validation
1. Re-run Reproduce.
2. Confirm frames 1–12 ≥ 99.5 %.

## See also
- `Fix-renderer-outpoint-frame.md` (extend to also cover in-point)
