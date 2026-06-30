# Fix renderer: out-point (and in-point) frame handling

## Scope
Cross-cutting renderer plan affecting **at least 9 files** in the suite. Many Lottie
animations show a catastrophic similarity drop on the **last sampled frame** (=
`animation.outPoint()`) — and one file (`dot/demo-3.lottie`) shows a mirror-image
catastrophic drop on the first frames after `animation.inPoint()`. Resolving this
single bug closes the largest single source of test failures across the suite.

## Affected files (from 2026-06-25 calibration)
| File                                              | last-frame score | drop vs steady |
|---------------------------------------------------|------------------|----------------|
| json/animated_background_patterns.json @ frame 90 | 81.51 %          | −18 pp         |
| json/face-peeking.json @ frame 231                | 90.11 %          | −9 pp          |
| json/face-exhaling.json @ frame 150               | 89.31 %          | −10 pp         |
| json/timeline_animation.json @ frame 31           | 95.84 %          | −4 pp          |
| json/box-moving-changing-color.json @ frame 150   | 98.42 %          | −1.5 pp        |
| json/isometric_data_analysis.json @ frame 180     | 95.81 %          | −4 pp          |
| json/sandy_loading.json @ frame 72                | 96.45 %          | −3 pp          |
| json/loading.json @ frame 60                      | 99.83 %          | −0.16 pp       |
| json/snake_ladder_loading_animation.json @ frame 300 | 99.50 %       | −0.5 pp        |
| dot/demo-1.lottie @ frame 370                     | 91.00 %          | −8 pp          |
| dot/demo-3.lottie @ frames 1–12 (in-point side)   | 96.84 %          | −3 pp          |

## Reproduce
Use any of the affected files via single-file mode, e.g.
```
mvn -pl fxfileviewer test \
    -Dtest=CompareFxViewWithWebViewTest#compareFxAndJsRenderingFullSize \
    -Dlottie.file=json/animated_background_patterns.json
```
and inspect the last frame's diff PNG.

## Diagnosis
The harness samples every frame in `[inPoint, outPoint]` inclusive:
```java
List<Integer> frames = WebViewScreenshotGenerator.buildSampledFrames(
        inPoint, Math.max(inPoint, outPoint), 1);
```
The WebView reference generator (`WebViewScreenshotGenerator`) and the JavaFX player
(`LottiePlayer.seekToFrame`) disagree on what to display when the requested `t` equals
exactly `outPoint`:

- **lottie-web** treats `op` as **exclusive**: the last *rendered* frame is `op - 1`.
  At `t == op` it shows the same frame as `t == op - 1` (effectively clamps).
- **lottie4j fxplayer** likely treats `op` as **inclusive** *and* re-evaluates keyframes
  at `t == op`, which can land outside any keyframe segment and yield uninitialized /
  zeroed output (matching the 18-pp drop seen in `animated_background_patterns`).

The mirror image at `t == ip` (frame 1+ on `demo-3`) suggests the same boundary bug
also fires for layers whose `ks` keyframes start at `t > ip`: lottie-web clamps to
the first keyframe's value, fxplayer extrapolates.

## Proposed fix

Pick one of the two options; option A is preferred because it matches lottie-web's
behaviour.

### Option A — clamp the seek (preferred)
- `lottie4j/fxplayer/src/main/java/com/lottie4j/fxplayer/LottiePlayer.java` (or
  whichever method handles `seekToFrame(int frame)`):

  ```java
  int clamped = Math.max(ip, Math.min(frame, op - 1)); // op exclusive
  ```

  ⚠ Check what the player currently does — if it's already clamping but to
  `[ip, op]` inclusive, change the upper bound to `op - 1`.

- `lottie4j/fxfileviewer/src/test/java/com/lottie4j/fxfileviewer/WebViewScreenshotGenerator.java`
  — verify how it generates the *reference* set. If it generates `frame_<op>.png`
  too, those PNGs are dead and the test should not sample them. Easier: stop sampling
  `op` in `buildSampledFrames` (use `Math.max(ip, op - 1)` upper bound).

### Option B — fix the keyframe evaluation
- `lottie4j/core/src/main/java/com/lottie4j/core/model/keyframe/Keyframe.java` (and
  any `KeyframeInterpolator`): when `t > lastKeyframe.t`, return
  `lastKeyframe.endValue` (or `lastKeyframe.startValue` if no `e` is present), not
  zero/null. When `t < firstKeyframe.t`, return `firstKeyframe.startValue`.

A and B together are robust: A matches lottie-web at the playback level, B prevents
zeroed output if anyone seeks beyond `op`.

## Related code paths to inspect
- `lottie4j/fxplayer/src/main/java/com/lottie4j/fxplayer/LottiePlayer.java`
- `lottie4j/fxplayer/src/main/java/com/lottie4j/fxplayer/renderer/layer/TransformApplier.java`
  (uses `animation.outPoint()` indirectly when evaluating `ks`)
- `lottie4j/core/src/main/java/com/lottie4j/core/model/animation/Animation.java`
  (definition of `inPoint`/`outPoint`)
- `lottie4j/core/src/main/java/com/lottie4j/core/model/keyframe/`
- `lottie4j/fxfileviewer/src/test/java/com/lottie4j/fxfileviewer/WebViewScreenshotGenerator.java`
  (`buildSampledFrames` — reference generation; mismatch here would reproduce the
  same symptom)

## Validation
After applying the fix:
1. Re-run **each** affected file in the table via single-file mode.
2. Confirm every file's minimum frame is ≥ 99.5 %.
3. Remove these entries from `PER_FILE_FLOOR_OVERRIDE` (`CompareFxViewWithWebViewTest.java`):
   - `json/animated_background_patterns.json`
   - `json/face-peeking.json`
   - `json/isometric_data_analysis.json`
   - `json/sandy_loading.json`
   - `dot/demo-1.lottie`
   (the others already pass at floor 99.5 % — they should improve, not gate.)
4. Re-run the full parameterised suite — minimum scores across the suite should
   improve uniformly.

## See also (per-file plans that depend on this)
- `Fix-rendering-animated_background_patterns.md`
- `Fix-rendering-box-moving-changing-color.md`
- `Fix-rendering-face-exhaling.md`
- `Fix-rendering-face-peeking.md`
- `Fix-rendering-isometric_data_analysis.md`
- `Fix-rendering-loading.md`
- `Fix-rendering-sandy_loading.md`
- `Fix-rendering-snake_ladder_loading_animation.md`
- `Fix-rendering-timeline_animation.md`
- `Fix-rendering-dot-demo-1.md`
- `Fix-rendering-dot-demo-3.md`
