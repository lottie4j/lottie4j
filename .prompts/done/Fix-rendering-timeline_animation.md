# Fix rendering: json/timeline_animation.json

## File
`lottie4j/fxfileviewer/src/test/resources/json/timeline_animation.json`

## Reproduce
```
mvn -pl fxfileviewer test \
    -Dtest=CompareFxViewWithWebViewTest#compareFxAndJsRenderingFullSize \
    -Dlottie.file=json/timeline_animation.json
```

## Observed gap (2026-06-25 calibration)
- average: 99.84 % (passes)
- minimum: **95.84 % on frame 31** (out-point cliff)
- floor:   99.5 % (target)
- target:  99.5 %

## Failing frames

| frame | overall | R     | G     | B     | notes                                |
|-------|---------|-------|-------|-------|--------------------------------------|
| 14    | 99.91   | 99.84 | 99.93 | 99.98 | one sub-pixel dip mid-anim           |
| 31    | **95.84** | 96.08 | 95.39 | 96.05 | **out-point — animation runs 0..31** |

Steady-state is 99.97–99.99 %. One sub-pixel dip + one catastrophic last frame.

## Diff evidence
- `target/test-output/json/timeline_animation-webview_scale_1.0/frame_31_95.84.png`

## Lottie part diagnosis
Short looping timeline animation, 32 frames (`op:31`). No `gf`/`tt`. Probably a few
simple shape layers with a polygon (`ty:sr`?) or path drawing in/out — check JSON.

## Root cause hypothesis
Pure case of the cross-cutting out-point bug. Frame 14 is sub-pixel noise.

## Proposed fix
Apply `Fix-renderer-outpoint-frame.md` (cross-cutting).

## Validation
1. Re-run Reproduce.
2. Confirm frame 31 ≥ 99.5 % (or no longer sampled).

## See also
- `Fix-renderer-outpoint-frame.md`
