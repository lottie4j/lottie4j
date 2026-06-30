# Fix rendering: json/snake_ladder_loading_animation.json

## File
`lottie4j/fxfileviewer/src/test/resources/json/snake_ladder_loading_animation.json`

## Reproduce
```
mvn -pl fxfileviewer test \
    -Dtest=CompareFxViewWithWebViewTest#compareFxAndJsRenderingFullSize \
    -Dlottie.file=json/snake_ladder_loading_animation.json
```

## Observed gap (2026-06-25 calibration)
- average: 99.96 % (passes)
- minimum: **99.50 % on frame 300** (out-point boundary)
- floor:   99.5 % (target)
- target:  99.5 %

## Failing frames
File is essentially perfect except for two narrow dip clusters and the out-point:

| frame range | overall      | R     | G     | B     | notes                              |
|-------------|--------------|-------|-------|-------|------------------------------------|
| 4–11        | 99.81–99.99  | 99.96 | 99.73 | 99.74 | onset (sub-pixel G/B dip)          |
| 84–95       | 99.79–99.95  | 99.96 | 99.70 | 99.72 | mid-animation small dip            |
| 96–215      | 99.78–99.99  | 99.82 | 99.78 | **100.00** | persistent very small G/R skew |
| 288–300     | 99.81–100    | dips to 99.50 | dips | dips | out-point boundary               |
| 300         | **99.50**    | 99.94 | 99.20 | 99.37 | **out-point — exactly at threshold** |

The persistent `B = 100.00` on frames 96–215 with R/G ~99.78 is a tell: those frames'
B channel is bit-for-bit identical to the reference, but R and G drift slightly.

## Diff evidence
- `target/test-output/json/snake_ladder_loading_animation-webview_scale_1.0/frame_300_99.50.png`

## Lottie part diagnosis
- Many ellipse shape layers with animated opacity, scattered across the canvas (the
  "snakes and ladders" board).
- Confirmed JSON has many `ty:gr → ty:el → ty:fl → ty:tr` chains.
- No `gf`/`tt` checked.

## Root cause hypothesis
1. **Frame 300 (out-point):** cross-cutting; `Fix-renderer-outpoint-frame.md`.
2. **R/G drift over 96–215:** anti-aliased ellipse edges; sub-pixel resolution
   difference between `EllipseRenderer.java` and Chrome's canvas. Negligible alone but
   would close the last 0.2 pp if fixed.

## Proposed fix
- `Fix-renderer-outpoint-frame.md` (cross-cutting).
- Stretch: review
  `lottie4j/fxplayer/src/main/java/com/lottie4j/fxplayer/renderer/shape/EllipseRenderer.java`
  for the parametric → bezier conversion (Lottie ellipses are sometimes rendered as a
  4-cubic-bezier path; the kappa constant should be `0.5522847498`).

## Validation
1. Re-run Reproduce.
2. Confirm minimum > 99.5 %.

## See also
- `Fix-renderer-outpoint-frame.md`
