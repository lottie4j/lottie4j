# Fix rendering: dot/demo-1.lottie

## File
`lottie4j/fxfileviewer/src/test/resources/dot/demo-1.lottie`

## Status (post cross-cutting fixes, 2026-06-29 re-calibration)
- average: **99.46 %** (was 99.44)
- minimum: **97.03 %** on frame 149 (was 91.00 % on frame 370)
- floor:   99.4 % (override, currently passing)
- target:  99.5 %  →  remaining gap −0.04 pp

The single biggest source of failure (the 91.00 % "out-point cliff" on frame 370)
was eliminated by the cross-cutting `Fix-renderer-outpoint-frame.md` plan: the
sampler now stops at `op − 1 = 369`, matching the JavaFX player's
`FrameTiming.getLastRenderableFrame()`.

## Reproduce
```
mvn -pl fxfileviewer test \
    -Dtest=CompareFxViewWithWebViewTest#compareFxAndJsRenderingFullSize \
    -Dlottie.file=dot/demo-1.lottie
```

## Original (2026-06-25) observed gap
- average: 99.44 %
- minimum: 91.00 % on frame 370 (out-point cliff)
- floor:   98.9 % (override)
- target:  99.5 %

A 371-frame animation. Many segments below 99.5 % with a mild **G-channel skew**
(green drops most), plus the final-frame cliff:

| frame range | overall      | R           | G           | B           | notes                           |
|-------------|--------------|-------------|-------------|-------------|---------------------------------|
| 0–34        | 99.95–99.96  | ≈99.96      | ≈99.96      | ≈99.95      | steady opening                  |
| 35–40       | 98.50–99.89  | drops       | **drops more** | rises    | onset of first motion           |
| 41–60       | 98.33–99.30  | ≈98.4       | **≈97.9**   | ≈98.9       | mid-anim dip                    |
| 145–149     | 97.01–97.65  | ≈97.4       | **≈95.99**  | ≈97.7       | worst steady-state dip          |
| 195–200     | 97.96–98.73  | drops       | drops       | drops       | second mid dip                  |
| 220–235     | 97.85–98.81  | drops       | drops       | drops       | third mid dip                   |
| 370         | **91.00**    | 91.55       | **93.10**   | **88.34**   | out-point cliff (now eliminated) |

## Root-cause analysis (corrected)

The original plan hypothesised that the persistent **G < R, B** skew came from a
gradient-stop parsing bug in `FillRenderer.java` (`Fix-renderer-gradient-fill-
channels.md`). Inspection of the unpacked `.lottie` archive shows that hypothesis
is **wrong for this file**:

> `demo-1.lottie` contains **no gradients at all** (`gf`/`gs`). Every fill is a
> flat solid color (`ty:fl`) and every stroke is a flat solid color
> (`ty:st`). The cross-cutting gradient-channels fix benefits other files (e.g.
> `face-peeking`, `face-exhaling`, Duke variants) but has no effect on demo-1.

The remaining `G < R, B` drift on motion frames is therefore **not gradient-
related**. It is consistent with sub-pixel differences in:

1. **Animated bezier path interpolation** — the "box", "mouth 2", and "face "
   layers morph their `sh` shapes between keyframes (e.g. `mouth 2` at
   t=63, 86, 140, 200, 235, 280; `box` body at t=70, 110, 135, 150, 192, 210,
   250, 290). The shape-path interpolation logic in `PathBezierInterpolator`
   may produce slightly different cubic control points vs lottie-web's
   `Shape.js`.
2. **Rounded stroke caps/joins** — the box outline uses
   `"w":5, "lc":2, "lj":2` (round line cap, round line join). JavaFX's
   `setLineCap(LineCap.ROUND)` / `setLineJoin(LineJoin.ROUND)` may rasterise
   joins slightly differently from HTML Canvas's `lineCap: 'round'`.
3. **Rotation interpolation** — the "horn", "needle", and "face " layers have
   eased rotation keyframes whose Bezier-easing solver output may differ at
   sub-pixel resolution.

The G-channel sensitivity is incidental: green has the highest luminance
weighting in SSIM's color-aware metric, so any sub-pixel anti-aliasing
mismatch shows up there first.

## Outcome
- The cross-cutting `Fix-renderer-outpoint-frame.md` plan closed the 8-pp
  catastrophe on frame 370 (91.00 → not sampled).
- The file is now **passing the test** at 99.46 % vs the 99.4 % override floor.
- The remaining −0.04 pp gap to the 99.5 % target is sub-pixel drift in
  animated path / stroke / rotation rendering — too small to address with a
  single targeted change. Any further improvement would benefit from a
  general "stroke and path anti-aliasing parity" study covering multiple files
  (e.g. `dot/demo-3.lottie`, `sandy_loading`, `lottie_lego`).
- `PER_FILE_FLOOR_OVERRIDE` entry remains at 99.4 % to preserve safety margin.

## See also
- `Fix-renderer-outpoint-frame.md` (applied — addressed the main gap)
- `Fix-renderer-gradient-fill-channels.md` (applied — not relevant to demo-1
  because the file has no gradients)
