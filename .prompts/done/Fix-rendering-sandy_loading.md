# Fix rendering: json/sandy_loading.json

## File
`lottie4j/fxfileviewer/src/test/resources/json/sandy_loading.json`

## Reproduce
```
mvn -pl fxfileviewer test \
    -Dtest=CompareFxViewWithWebViewTest#compareFxAndJsRenderingFullSize \
    -Dlottie.file=json/sandy_loading.json
```

## Observed gap (2026-06-25 calibration)
- average: 99.39 %
- minimum: 96.45 % on **frame 72** (the out-point — animation runs 0..72)
- floor:   98.8 % (override)
- target:  99.5 %

## Failing frames
Distributed sub-99.5 % gap across nearly all frames, plus a final-frame cliff:

| frame | overall | R     | G     | B     | notes                                |
|-------|---------|-------|-------|-------|--------------------------------------|
| 7     | 99.18   | 98.95 | 99.37 | 99.22 | spinning shape sub-pixel rotation    |
| 15    | 98.91   | 98.72 | 99.06 | 98.95 | continuing rotation                  |
| 17    | 98.75   | 98.67 | 98.88 | 98.71 |                                       |
| 23    | 98.87   | 98.79 | 98.94 | 98.86 |                                       |
| 53    | 98.69   | 98.82 | 98.67 | 98.58 |                                       |
| 59    | 98.98   | 98.90 | 99.06 | 98.97 |                                       |
| 72    | **96.45** | 96.66 | 96.39 | 96.29 | **out-point — all channels drop together** |

R/G/B drop near-equally on every failing frame → geometric (rotation/position) bug,
not chromatic.

## Diff evidence
- `target/test-output/json/sandy_loading-webview_scale_1.0/frame_72_96.45.png` — last
  frame, expected to show wrong content (out-point bug).
- `target/test-output/json/sandy_loading-webview_scale_1.0/frame_17_98.75.png` — mid-
  animation, expected to show rotational edge halos on the spinning loader shape.

## Lottie part diagnosis
- Spinning shapes (the "loader") with continuous `ks.r` (rotation) keyframes.
- No `gf`, no `tt` checked → pure shape + transform.
- Rotation is per-frame fractional — typical of `r:{a:1, k:[…]}` with linear or bezier
  easing from 0° to 360°.

## Root cause hypothesis
1. **Frame 72 cliff (3 pp drop):** cross-cutting out-point bug, same as
   `animated_background_patterns` @90 and others. See `Fix-renderer-outpoint-frame.md`.
2. **0.6 pp distributed gap:** rotation pivot or sub-pixel rotation rounding. The hot
   spot is `lottie4j/fxplayer/src/main/java/com/lottie4j/fxplayer/renderer/layer/TransformApplier.java`
   — verify the rotation is applied around `ks.a` (anchor) **before** translation, and
   the rotation matrix uses `Math.toRadians(angle)` not degrees.

## Proposed fix
- **Cross-cutting frame-72 fix** per `Fix-renderer-outpoint-frame.md`.
- `lottie4j/fxplayer/src/main/java/com/lottie4j/fxplayer/renderer/layer/TransformApplier.java`
  — confirm: `translate(p.x, p.y); rotate(r); translate(-a.x, -a.y);` (i.e. anchor is
  applied as a *post-rotation* translation back, not pre-rotation translation in).
- `lottie4j/fxplayer/src/main/java/com/lottie4j/fxplayer/renderer/shape/PathBezierInterpolator.java`
  — if rotation keyframes use bezier easing, verify the solver iterations and starting
  guess match lottie-web's `BezierEasing`.

## Validation
1. Re-run Reproduce.
2. Average ≥ 99.5 %; frame 72 ≥ 99.5 %.
3. Remove the entry from `PER_FILE_FLOOR_OVERRIDE`.
4. Re-run the full suite — improvements should be visible on any animation with
   rotational shapes (e.g. `snake_ladder_loading_animation`, `lottie4j`).

## See also
- `Fix-renderer-outpoint-frame.md`
- `Fix-renderer-bezier-easing-fidelity.md` (if rotation-keyframe easing is the cause)
