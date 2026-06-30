# Fix renderer: blend-mode offscreen buffer fidelity

## Scope
Cross-cutting renderer plan for `PrecompRenderer`'s use of off-screen JavaFX buffers to
implement `bm` (blend mode) values like MULTIPLY (`bm:1`) and SCREEN (`bm:3`). When a
shape layer has a non-default blend mode, it is rendered to a transparent off-screen
image first and then composited onto the canvas with the chosen blend mode. The
composite output diverges slightly from the browser canvas in two ways:
- a uniform 0.5 pp tax across many frames in animations with many blend-mode layers
  (e.g. `angry_bird` has 14 different `Shape Layer N rendering with blend mode …`
  log lines)
- the per-channel breakdown is uniform (R ≈ G ≈ B), pointing at a geometric / AA
  fidelity loss, not colour-channel maths.

## Affected files
| File                                 | symptom                                  |
|--------------------------------------|------------------------------------------|
| json/angry_bird.json                 | uniform 0.5 pp tax over 373 frames       |
| json/foojay-duke.json                | tiny uniform tax on motion frames        |
| json/pi4j.json                       | min 98.13 % during logo build-up burst   |
| json/foojay-reporter.json (partial)  | tiny uniform tax outside motion frames   |

## Reproduce
```
mvn -pl fxfileviewer test \
    -Dtest=CompareFxViewWithWebViewTest#compareFxAndJsRenderingFullSize \
    -Dlottie.file=json/angry_bird.json
```
Watch the calibration output for the log line:
> `Layer 'Shape Layer N' rendering with blend mode MULTIPLY using offscreen buffer`

## Diagnosis
`PrecompRenderer` likely uses a `WritableImage` snapshot of a transient `Canvas` to
implement the off-screen buffer, then draws that snapshot back with
`GraphicsContext.setGlobalBlendMode(BlendMode.MULTIPLY)`. The fidelity loss can come
from:

1. **Buffer pixel format / pre-multiplication**: JavaFX `WritableImage` stores
   non-premultiplied ARGB by default; if the renderer assumes premultiplied alpha
   for the blend math, every anti-aliased edge pixel composites with the wrong
   alpha-weighted RGB.
2. **Buffer size rounding**: if the buffer dimensions are rounded down to integer
   pixels but the layer's bounding box is fractional, the right/bottom edges lose
   sub-pixel coverage.
3. **`setSmooth(true)` not applied** when blitting the buffer back — JavaFX defaults
   may use NEAREST not BILINEAR.
4. **Blend mode equation mismatch**: JavaFX's `BlendMode.MULTIPLY` uses
   `outRGB = srcRGB * dstRGB / 255` on straight alpha; the browser canvas uses
   `outRGB = srcRGB * dstRGB` on **premultiplied** alpha, then unpremultiplies.

## Proposed fix
- `lottie4j/fxplayer/src/main/java/com/lottie4j/fxplayer/renderer/layer/PrecompRenderer.java`
  — audit the snapshot path:
  1. Create the buffer with explicit `new SnapshotParameters()` whose `setFill(
     Color.TRANSPARENT)` is set.
  2. Round buffer dimensions **up**, not down, to capture sub-pixel right/bottom
     edges.
  3. When blitting back, set `GraphicsContext.setImageSmoothing(true)` (JavaFX 16+) or
     equivalent.
  4. For each documented blend mode, write a regression test rendering a known 2-layer
     stack and asserting the composite matches lottie-web's output pixel-for-pixel.

- `lottie4j/fxplayer/src/main/java/com/lottie4j/fxplayer/renderer/shape/PathRenderer.java`
  + `PathStrokeRenderer.java` — verify
  `GraphicsContext.setLineCap(StrokeLineCap.BUTT)` /
  `setLineJoin(StrokeLineJoin.MITER)` /
  `setMiterLimit(4.0)` match Lottie defaults.

## Validation
After applying:
1. Re-run `json/angry_bird.json` single-file and check the uniform 99.47 % steady
   state rises to ≥ 99.7 %.
2. Remove `json/angry_bird.json` from `PER_FILE_FLOOR_OVERRIDE` if average ≥ 99.5 %.
3. Re-run `json/foojay-duke.json` and `json/pi4j.json` — both should also tighten.
4. Re-run the full suite — any animation using blend modes should improve.

## See also
- `Fix-rendering-angry_bird.md`
- `Fix-rendering-foojay-duke.md`
- `Fix-rendering-pi4j.md`
- `Fix-rendering-foojay-reporter.md`
