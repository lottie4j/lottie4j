# Fix rendering: json/java_duke_flip.json

## File
`lottie4j/fxfileviewer/src/test/resources/json/java_duke_flip.json`

## Reproduce
```
mvn -pl fxfileviewer test \
    -Dtest=CompareFxViewWithWebViewTest#compareFxAndJsRenderingFullSize \
    -Dlottie.file=json/java_duke_flip.json
```

## Observed gap (2026-06-25 calibration)
- average: 99.88 %
- minimum: 99.61 % on frame 102
- floor:   99.5 % (target)
- target:  99.5 %

## Failing frames
All frames ≥ 99.5 % — file is passing. Mostly very uniform (`99.88 %` for the first
~60 frames is the steady-state). Sentinel of two transient frames:

| frame | overall | notes                                                   |
|-------|---------|---------------------------------------------------------|
| 80    | 99.77   | mid-flip burst                                          |
| 102   | **99.61** | brief flip-rebound dip (min)                          |
| 77    | 99.77   |                                                          |

R/G/B move together — sub-pixel motion noise during the flip animation, not chromatic.

## Diff evidence
No PNGs written (all frames ≥ 99.5). Reproduce to refresh.

## Lottie part diagnosis
- Same Duke layer as `java_duke_fadein.json` and `java_duke_slidein.json`, animated
  with rotation (the "flip"). Contains the same `gf` (gradient fill) signature
  (`s:[86.63,167.07], e:[141.35,167.07], t:2`).
- Despite sharing the gradient, the **flip frames don't show channel-skewed dips** —
  the suspect single-frame opacity bug in fadein/slidein doesn't trigger here because
  the layer is opaque throughout.

## Root cause hypothesis
No active bug. The 99.61 % min on frame 102 is sub-pixel rotation noise. Fixing
`Fix-renderer-bezier-easing-fidelity.md` (rotation keyframe easing) would close it.

## Proposed fix
No change. Keep this file as a regression sentinel for the shared `gf` code path —
when the gradient or keyframe fixes from `Fix-rendering-java_duke_fadein.md` land,
verify this file still scores ≥ 99.88 %.

## Validation
1. Re-run Reproduce.
2. Confirm minimum stays ≥ 99.5 %.
3. After applying the fadein/slidein fixes, verify this file's average **does not
   regress** below 99.88 %.

## See also
- `Fix-rendering-java_duke_fadein.md`
- `Fix-rendering-java_duke_slidein.md`
