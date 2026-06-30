# Fix rendering: json/animated_background_patterns.json

## File
`lottie4j/fxfileviewer/src/test/resources/json/animated_background_patterns.json`

## Reproduce
```
mvn -pl fxfileviewer test \
    -Dtest=CompareFxViewWithWebViewTest#compareFxAndJsRenderingFullSize \
    -Dlottie.file=json/animated_background_patterns.json
```

## Observed gap (2026-06-25 calibration)
- average: 99.36 %
- minimum: 81.51 % on **frame 90** (the out-point — animation has 91 frames, indices
  0..90)
- floor:   98.8 % (override)
- target:  99.5 %

## Failing frames
Every frame 0–89 is **already passing per-frame at 99.5 %** or extremely close
(99.52–99.56). The entire average-miss is caused by one frame:

| frame | overall | R     | G     | B     | notes                                |
|-------|---------|-------|-------|-------|--------------------------------------|
| 6     | 99.50   | 99.09 | 99.56 | 99.85 | R-channel skew on a few frames       |
| 7     | 99.42   | 98.96 | 99.48 | 99.84 | R-channel skew                       |
| 8     | 99.47   | 99.00 | 99.57 | 99.84 | R-channel skew                       |
| 90    | **81.51** | **79.60** | **82.13** | 82.81 | **out-point** — catastrophic       |

The R-channel skew across frames 6–8 (sub-pixel) is a separate, smaller issue.

## Diff evidence
- `target/test-output/json/animated_background_patterns-webview_scale_1.0/frame_90_81.51.png`
  — the only severe failure; expected to show entirely-wrong content or empty canvas.
- `target/test-output/json/animated_background_patterns-webview_scale_1.0/frame_7_99.42.png`
  — the per-frame R-channel skew.

## Lottie part diagnosis
- File contains many `"tt": 1` (alpha matte) layers, all parented to layer 22 (the
  master shape).
- 91 frames means `op = 90`, so frame 90 is **at or just past** the out-point. WebView
  reference at frame 90 likely renders an empty (or last-valid) state while FX
  re-evaluates beyond the keyframe range and produces garbage.
- The R-channel skew on early frames (no `gf` in this file — checked) is more likely a
  **matte composition** issue, not a gradient one.

## Root cause hypothesis
Two distinct bugs:

1. **Out-point frame 90 is catastrophic** — strongest evidence yet of an `op` boundary
   handling bug. R/G/B all drop by ~18 pp simultaneously, meaning the entire image is
   wrong. This is shared by ≥6 files (`box-moving-changing-color` @150, `face-exhaling`
   @150, `face-peeking` @231, `isometric_data_analysis` @180, `loading` @60,
   `sandy_loading` @72, `dot/demo-1` @370). Cross-cutting; see
   `Fix-renderer-outpoint-frame.md`.
2. **Alpha-matte R-channel bias on frames 6–8** — matte layers are parented to a
   master with animated rotation. If alpha-matte luma is computed wrong (e.g. using
   only R or only G), the matted layer would gain/lose R-only opacity.
   `lottie4j/fxplayer/src/main/java/com/lottie4j/fxplayer/renderer/layer/MatteRenderer.java`
   should be checked: alpha matte (`tt:1`) must use the source layer's **alpha**
   channel as the mask, *not* its luma.

## Proposed fix
- **Frame 90:** apply the cross-cutting `Fix-renderer-outpoint-frame.md` fix
  (`PlayerTimeline` / `LottiePlayer.seekToFrame` should clamp the frame index to
  `[ip, op-1]` or `[ip, op]` depending on whether `op` is exclusive — match
  lottie-web exactly).
- **Matte R-bias:**
  `lottie4j/fxplayer/src/main/java/com/lottie4j/fxplayer/renderer/layer/MatteRenderer.java`
  — for `tt:1` (alpha matte), copy the matte source's alpha into the destination's alpha
  channel only; for `tt:3` (alpha-inverted), invert that alpha. Do **not** touch RGB.

## Validation
1. Re-run Reproduce.
2. Frame 90 must be ≥ 99.5 % or the harness must stop generating frame 90 (if the
   correct fix is to clamp to `[ip, op-1]`, the harness's `buildSampledFrames` will not
   request frame 90 and the test will skip it). Either way, average ≥ 99.5 %.
3. Remove the entry from `PER_FILE_FLOOR_OVERRIDE`.
4. Re-run the full suite — most other files with last-frame dips should improve.

## See also
- `Fix-renderer-outpoint-frame.md` (cross-cutting; affects ≥7 files)
- `Fix-renderer-alpha-matte-channels.md` (cross-cutting; same matte code path used by
  `isometric_data_analysis`, `lottie4j`)
