# Fix rendering: json/java_duke_slidein.json

## File
`lottie4j/fxfileviewer/src/test/resources/json/java_duke_slidein.json`

## Reproduce
```
mvn -pl fxfileviewer test \
    -Dtest=CompareFxViewWithWebViewTest#compareFxAndJsRenderingFullSize \
    -Dlottie.file=json/java_duke_slidein.json
```
> **Pre-requisite:** like `java_duke_fadein.json`, the most recent calibration run
> skipped this file because `frame_0.png` for `json/java_duke_slidein-webview` was not
> on the surefire classpath. PNGs exist on disk — run `mvn -pl fxfileviewer test`
> (not `surefire:test`) so `target/test-classes/json/java_duke_slidein-webview/` is
> populated.

## Observed gap (from `PER_FILE_FLOOR_OVERRIDE` comment)
- average: 98.60 %
- minimum: 78.14 % (identical dip to `java_duke_fadein.json`)
- floor:   98.1 % (override)
- target:  99.5 %

## Failing frames
| frame    | overall | notes                                          |
|----------|---------|------------------------------------------------|
| min N    | 78.14   | Same single-frame dip as fadein                |
| rest     | ≥99     | Steady-state frames render correctly           |

The shared minimum (78.14 %) is strong evidence that fadein and slidein share the same
off-frame; only the *transform* (position vs opacity) differs between the two files. The
slidein file has a higher average (98.60 vs 97.47) because its non-fading frames have
better steady-state match — the dip is the same, just one frame in a longer animation.

## Diff evidence
`target/test-output/json/java_duke_slidein-webview_scale_1.0/frame_<N>_<score>.png`
(regenerate via Reproduce).

## Lottie part diagnosis
- Layer:    same Duke layer with a fade-in (`ks.o` keyframes) **and** an additional
  position slide (`ks.p` keyframes).
- Shape(s): same gradient fill (`{"ty":"gf", "s":{"k":[86.63,167.07]},
  "e":{"k":[141.35,167.07]}, "t":2, …}`).
- Animated property: `ks.o` (opacity) **and** `ks.p` (position) over the slide-in interval.
- Easing: bezier — same `i`/`o` arrays as fadein.

## Root cause hypothesis
Same as `Fix-rendering-java_duke_fadein.md`: a one-frame interpolation snap at the end
of the opacity keyframe segment, in either the keyframe segment search or the opacity
clamp in `TransformApplier`. The fact that the *exact* min (78.14 %) is shared with the
fadein file rules out per-file content as the cause and points at the renderer.

## Proposed fix
Same as `Fix-rendering-java_duke_fadein.md`:
- `lottie4j/core/src/main/java/com/lottie4j/core/model/keyframe/` — segment-boundary
  evaluation.
- `lottie4j/fxplayer/src/main/java/com/lottie4j/fxplayer/renderer/layer/TransformApplier.java`
  — opacity application.

If the slidein min frame is *not* identical to fadein's after re-running, treat slidein
as a separate cause: the slide-in adds `ks.p` keyframes whose own boundary could trigger
a similar dip at a different frame, in which case the position-keyframe spline math in
`PathBezierInterpolator.java` is the next suspect.

## Validation
1. Re-run the Reproduce command.
2. Confirm every frame is ≥ 99.5 %.
3. Remove the entry from `PER_FILE_FLOOR_OVERRIDE` once average ≥ 99.5 %.
4. Re-run the full suite to confirm no regression.

## See also
- `Fix-rendering-java_duke_fadein.md` (almost certainly the same root cause)
- `Fix-rendering-java_duke_flip.md` (same gradient, currently passing)
- `Fix-renderer-keyframe-segment-boundary.md`
