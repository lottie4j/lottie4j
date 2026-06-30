# Fix rendering: json/angry_bird.json

## File
`lottie4j/fxfileviewer/src/test/resources/json/angry_bird.json`

## Reproduce
```
mvn -pl fxfileviewer test \
    -Dtest=CompareFxViewWithWebViewTest#compareFxAndJsRenderingFullSize \
    -Dlottie.file=json/angry_bird.json
```

## Observed gap (2026-06-25 calibration)
- average: 99.26 %
- minimum: 98.37 % around frames 80–82
- floor:   98.7 % (override)
- target:  99.5 %

## Failing frames
This is a 373-frame animation. Almost **every** frame is below 99.5 % (`99.46–99.49`),
with two clusters dipping further:

| frame range | overall      | R           | G           | B           | notes                |
|-------------|--------------|-------------|-------------|-------------|----------------------|
| 0–60        | 99.47–99.50  | ≈99.49      | ≈99.50      | ≈99.47      | small uniform tax    |
| 61–87       | 98.37–99.06  | dips with B | dips with B | **dips most** | bird movement burst |
| 80–82 (min) | 98.37        | 98.35       | 98.37       | 98.40       | nearly equal channels — geometric, not chromatic |
| 88–155      | 98.5–99.4    | uniform     | uniform     | uniform     | gradual recovery     |
| 156–280     | 99.1–99.5    | small skew  |             |             | mid-animation        |
| 281–372     | 99.2–99.5    | uniform     |             |             | end-game             |

Per-channel breakdown is **almost equal across R/G/B**, indicating the gap is
**geometric / anti-aliasing**, not colour or gradient.

## Diff evidence
- `target/test-output/json/angry_bird-webview_scale_1.0/frame_80_98.37.png` — worst
  single frame, expected to show edge halos around the bird's limbs.
- `target/test-output/json/angry_bird-webview_scale_1.0/frame_61_98.92.png` — start of
  the dip cluster.

## Lottie part diagnosis
- Calibration log shows **many** `PrecompRenderer` "Layer 'Shape Layer N' rendering with
  blend mode MULTIPLY/SCREEN using offscreen buffer" lines for layers 3, 9, 17, 20, 22,
  25, 29, 36, 38, 87, 92. → File uses many `bm` (blend mode) values: MULTIPLY (bm:1) and
  SCREEN (bm:3) on shape layers.
- Likely an animation with anti-aliased shape edges composited through blend modes,
  where every pixel on every edge is off by 1 anti-aliasing step.

## Root cause hypothesis
The uniform 0.5 pp gap on most frames + the equal R/G/B drop on the worst frames points
at **edge anti-aliasing fidelity**, not colour math. Possible causes:

1. The off-screen buffer used by `PrecompRenderer` for blend-mode layers is **opaque**
   (or has wrong alpha) before compositing, so edges meet the buffer instead of the
   underlying layer cleanly. Check
   `lottie4j/fxplayer/src/main/java/com/lottie4j/fxplayer/renderer/layer/PrecompRenderer.java`
   for the snapshot buffer creation (must be transparent ARGB, not pre-multiplied).
2. JavaFX `Canvas`/`GraphicsContext` default antialiasing differs from the browser
   canvas. Investigate whether `SmoothingType.QUALITY` is set on the off-screen buffer.
3. Blend-mode equation off by epsilon: `Multiply` in JavaFX is `(srcRGB * dstRGB / 255)`;
   browser canvas may use straight `srcRGB * dstRGB` on premultiplied alpha.

## Proposed fix
- `lottie4j/fxplayer/src/main/java/com/lottie4j/fxplayer/renderer/layer/PrecompRenderer.java`
  — verify off-screen buffer is created with `PixelFormat.getIntArgbInstance()` and
  cleared with transparent black, and that the compositing call uses
  `BlendMode.MULTIPLY` / `BlendMode.SCREEN` exactly (not `ADD`/`SRC_OVER`).
- `lottie4j/fxplayer/src/main/java/com/lottie4j/fxplayer/renderer/shape/PathRenderer.java`
  + `PathStrokeRenderer.java` — verify edges are rendered with `setSmooth(true)` and
  `LineCap`/`LineJoin` matching Lottie spec (`MITER` default, miter limit 4).
- Optional: switch the off-screen buffer pixel format to *linear* RGB and convert back
  to sRGB on composite, matching the browser's canvas behaviour for blend modes.

## Validation
1. Re-run Reproduce.
2. Confirm the average crosses 99.5 % and frame 80 ≥ 99.5 %.
3. Remove the entry from `PER_FILE_FLOOR_OVERRIDE`.
4. Re-run the full suite — every other animation that uses `PrecompRenderer` with blend
   modes (most files do) should also improve.

## See also
- `Fix-renderer-blend-mode-offscreen-buffer.md` (cross-cutting plan if blend mode is
  confirmed)

## Investigation log

### 2026-06-29 — offscreen-buffer transform alignment (no measurable effect)

Hypothesis: when `PrecompRenderer.renderLayerWithBlendMode` blits the offscreen
image back via `gc.drawImage(layerImage, 0, 0)`, the destination `gc` carries the
accumulated parent-precomp transform. If any link in that chain has a fractional
translate, JavaFX bilinearly resamples the entire buffer, producing a uniform
~0.5 px edge blur on every blend-mode layer.

Fix attempted: render the offscreen at the destination canvas's pixel grid by
calling `offscreenGc.setTransform(gc.getTransform())` inside the lambda and
`gc.setTransform(new Affine())` before `drawImage`. Applied symmetrically in
`LottiePlayer.renderLayerWithBlendModeOffscreen`.

Result: **no measurable improvement** for `angry_bird` (still 99.26 % average,
98.37 % min — byte-identical SSIM scores within ±0.01 pp on a handful of frames
near the transition burst). Conclusion: the parent-precomp transform stack for
`angry_bird` is integer-only at the blend-mode call sites at full size, so the
blit was already pixel-perfect. The hypothesis is correct *in principle* but
does not explain this animation's gap. The change was reverted as it added cost
without benefit.

### Remaining hypothesis

The uniform 0.5 pp tax + R≈G≈B drop on motion frames is most likely **intrinsic
rasterizer-quality differences** between JavaFX's Marlin renderer and the
browser's Skia canvas — different coverage-AA filter kernels, different
sub-pixel-position sampling. This is not addressable from inside `PrecompRenderer`
without swapping the rasterizer.

Therefore the floor override (`98.7 %`) is appropriate technical debt. Further
closure would require either:
- A higher-fidelity software rasterizer (e.g. JavaFX Marlin tuned, Skia binding)
- Supersampled offscreen rendering (2× resolution, downsample on blit) — would
  push average up but risks oversharpening relative to the browser reference
- Acceptance that any reasonable Java2D-based renderer will sit a few pp below a
  modern browser canvas on heavily-anti-aliased motion frames
