# Fix rendering: json/loading.json

## File
`lottie4j/fxfileviewer/src/test/resources/json/loading.json`

## Reproduce
```
mvn -pl fxfileviewer test \
    -Dtest=CompareFxViewWithWebViewTest#compareFxAndJsRenderingFullSize \
    -Dlottie.file=json/loading.json
```

## Observed gap (2026-06-25 calibration)
- average: 99.99 %
- minimum: **99.83 % on frame 60** (single out-point dip)
- floor:   99.5 % (target)
- target:  99.5 %

## Failing frames
All frames ≥ 99.5 %; the file is passing. Sentinel of the single dip:

| frame | overall | R     | G     | B     | notes                          |
|-------|---------|-------|-------|-------|--------------------------------|
| 60    | 99.83   | 99.79 | 99.83 | 99.89 | **out-point dip — anim runs 0..60** |

Channels nearly equal; tiny edge-of-animation tax.

## Diff evidence
No PNGs written.

## Lottie part diagnosis
- Two simple shape layers with `el` (ellipse) + `st` (stroke) + `tm` (trim path) +
  `tr` (transform). Classic "spinning rings" loading animation. Confirmed in JSON
  inspection: lines 96 (`ty:el`), 118 (`ty:st`), 148 (`ty:tr`), 205 (`ty:tm`).
- `tm` (trim path) is animated.

## Root cause hypothesis
The frame-60 dip is the cross-cutting out-point bug. The fact that it only loses ~0.17 pp
(vs 18 pp in `animated_background_patterns`) means the trim-path animation closes the
ring at the out-point cleanly — there's just one frame where FX's trim end vs WebView's
disagree by a sub-pixel.

## Proposed fix
Fix is captured in `Fix-renderer-outpoint-frame.md`. The trim-path code itself is fine.

## Validation
1. Re-run Reproduce.
2. Confirm minimum stays ≥ 99.5 %.
3. After applying out-point fix, this file should hit 100 %.

## See also
- `Fix-renderer-outpoint-frame.md`
