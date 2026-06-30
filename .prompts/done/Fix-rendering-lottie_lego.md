# Fix rendering: json/lottie_lego.json

## File
`lottie4j/fxfileviewer/src/test/resources/json/lottie_lego.json`

## Reproduce
```
mvn -pl fxfileviewer test \
    -Dtest=CompareFxViewWithWebViewTest#compareFxAndJsRenderingFullSize \
    -Dlottie.file=json/lottie_lego.json
```
> **Pre-requisite:** the latest calibration log skipped this file (`frame_0.png` not
> on surefire classpath even though `json/lottie_lego-webview/` exists in
> `src/test/resources`). Use `mvn -pl fxfileviewer test`, not `mvn surefire:test`, so
> resources are re-copied.

## Observed gap (from `PER_FILE_FLOOR_OVERRIDE` comment)
- average: 98.14 %
- minimum: unknown (no fresh data — calibration skipped it)
- floor:   97.6 % (override)
- target:  99.5 %

## Failing frames
Re-run the Reproduce command to populate the table — for each frame below 99.5 % the
harness writes
`target/test-output/json/lottie_lego-webview_scale_1.0/frame_<N>_<score>.png`.

| frame | overall | R | G | B | notes |
|-------|---------|---|---|---|-------|
| TBD   | TBD     | …  | … | … | populate after Reproduce |

## Diff evidence
`target/test-output/json/lottie_lego-webview_scale_1.0/` (regenerate).

## Lottie part diagnosis
- Many `ty:4` shape layers — confirmed `nm:"1 "`, `nm:"2 "`, `nm:"3 "`, … (stacked LEGO
  brick layers).
- Motion-heavy animation (bricks falling/stacking) → continuous `ks.p` (position) and
  `ks.r` (rotation) keyframes on most layers.
- No `tt` (matte) or `gf` (gradient fill) → likely a pure shape + transform rendering
  problem.

## Root cause hypothesis
98.14 % average without an obvious single-frame outlier suggests a **distributed
sub-pixel positioning** issue affecting many frames, not a single keyframe bug.
Two candidates:

1. **Position interpolation tolerance** — bezier easing on `ks.p` evaluated with
   slightly different rounding than the WebView reference, causing 1–2 px offsets on
   moving bricks. Inspect
   `lottie4j/fxplayer/src/main/java/com/lottie4j/fxplayer/renderer/shape/PathBezierInterpolator.java`
   and the keyframe segment math in
   `lottie4j/core/src/main/java/com/lottie4j/core/model/keyframe/`.
2. **Rotation pivot** — if rotation is non-trivial (`ks.r` keyframes), a mismatched
   anchor (`ks.a`) translates into a rotating-and-translating brick. Inspect
   `lottie4j/fxplayer/src/main/java/com/lottie4j/fxplayer/renderer/layer/TransformApplier.java`.

The diff PNG heatmap will distinguish them: edge halos around moving bricks = pivot,
blob of red on every brick = positioning.

## Proposed fix
- `lottie4j/fxplayer/src/main/java/com/lottie4j/fxplayer/renderer/shape/PathBezierInterpolator.java`
  — verify the bezier solver matches lottie-web's `BezierEasing` exactly (binary search
  count / Newton-Raphson iterations / starting guess).
- `lottie4j/fxplayer/src/main/java/com/lottie4j/fxplayer/renderer/layer/TransformApplier.java`
  — verify anchor-translate order (translate, rotate around `a`, translate back) and the
  sign of `a` matches Lottie spec.

## Validation
1. Re-run Reproduce; populate the "Failing frames" table.
2. Confirm every listed failing frame ≥ 99.5 %.
3. Remove the entry from `PER_FILE_FLOOR_OVERRIDE` if average ≥ 99.5 %.
4. Re-run the full suite.

## See also
- `Fix-renderer-bezier-easing-fidelity.md` (cross-cutting plan if positioning is the cause)
