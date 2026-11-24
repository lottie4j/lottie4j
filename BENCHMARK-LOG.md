# Playback performance log

Running log of measured playback speed and the changes that moved it. Every entry is meant to be
copied into release notes or a status post: **what we changed, fps before, fps after**.

Fixture: `fxfileviewer/src/test/resources/dot/winter-town.lottie`
(1200×800, 60 fps, 1200 frames, 348 layers: 318 shape layers, 30 null layers, 1 Gaussian blur,
1 track matte; no precomps, masks, gradients or images).

## How to measure

Frame rate delivered by a running player is capped by the display refresh rate, so it reads 60 for
anything that renders within 16.6 ms. We therefore report the **render time per frame** and the
**achievable fps** (`1000 / average render ms`).

```bash
# 1. Headless, frames rendered back to back (no display cap). Append rows to a CSV.
mvn -pl fxfileviewer test -Pheadless-tests -Dtest=PlaybackBenchmarkTest -Dlottie.bench=true \
    -Dlottie.file=dot/winter-town.lottie -Dlottie.bench.sizes=1200x800 \
    -Dlottie.bench.csv=docs/benchmarks/<date>-<change>.csv

# 2. Real playback in a window (includes the real pipeline), prints a BENCH summary after N seconds.
cd fxfileviewer && mvn javafx:run \
    -Djavafx.args="--bench=15 $PWD/src/test/resources/dot/winter-town.lottie"
```

Columns: `record` = time of `render(frame)` (draws into the canvas command buffer, a lower bound);
`raster` = `snapshotFrame(frame)` (also rasterizes and reads pixels back, a slight overestimate).
Stage rows (`layer.plain`, `layer.matte`, `offscreen.draw`, `offscreen.snapshot`) are inclusive and
nested, they do not add up to the frame time.

Always note: machine, JDK, JavaFX version, canvas size, adaptive scaling on/off.

## Baseline (2026-11-24, before any renderer change)

Machine: Apple M2 Max, macOS 26.7.3, OpenJDK 27. Headless: JavaFX 26 (`-Pheadless-tests`);
window: JavaFX 21.0.12. 120 frames spread evenly over the 1200, 200 frames warm-up.

| Where | Canvas | Adaptive scaling | Avg render | p95 | Achievable fps |
|-------|--------|------------------|-----------|-----|----------------|
| Real window (viewer `--bench=15`) | 1200×800 | off | 360.6 ms | 411.5 ms | **2.8** |
| Headless, `record` | 1200×800 | off | 316.9 ms | 358.0 ms | **3.2** |
| Headless, `raster` | 1200×800 | off | 366.2 ms | 400.0 ms | **2.7** |
| Headless, `record` | 1200×800 | on | 112.1 ms | 136.4 ms | 8.9 |
| Headless, `record` | 600×400 | off | 135.5 ms | 170.4 ms | 7.4 |
| Headless, `raster` | 600×400 | off | 173.0 ms | 220.9 ms | 5.8 |

The target is 60 fps (16.6 ms). Today the animation plays at roughly **3 fps** at its native size.
The raw rows are in `docs/benchmarks/2026-11-24-baseline.csv`. The adaptive rows of `raster` are
not meaningful: `snapshotFrame` switches adaptive scaling off by design.

### Where the time goes

Real window, per frame (`layer.plain` covers 346 layers, `layer.matte` 1 layer):

| Stage | ms per frame | Calls per frame |
|-------|-------------|-----------------|
| `layer.plain` (all shape layers, inclusive) | 351.7 | 346 |
| of which `offscreen.snapshot` | **343.7** | **100** (3.4 ms each) |
| of which `offscreen.draw` | 1.0 | 100 |
| `layer.matte` | 8.3 | 1 |

**About 95% of a frame is spent on 100 full-canvas off-screen snapshots.** The fixture has 100
layers with an animated opacity, and each of those is drawn into its own 1200×800 buffer, snapshot
(rasterized and read back), and composited. The other 246 layers and all the actual drawing
together cost about 8 ms.

Hypotheses for the next steps (ordered by expected gain):

1. Apply layer opacity directly (`setGlobalAlpha`) when a layer is a single group with one fill or
   one stroke, so there are no overlapping parts that would need a buffer. Skips ~100 snapshots.
2. Where a buffer is still needed, size it to the bounds of the layer instead of the full canvas.
3. Reuse buffers and snapshot images instead of allocating a canvas and image per layer per frame.
4. Matte (8 ms) and blur: smaller buffers, as in 2.

## Changes

Add a row per change: what changed, the numbers from the same machine and commands as the
baseline, and the effect. Keep one change per row so the numbers can be attributed.

| # | Change | Before (avg ms / achievable fps) | After | Notes |
|---|--------|----------------------------------|-------|-------|
| 0 | Phase 1: render stats, stage profiler, headless benchmark and viewer `--bench` added (no renderer change) | 360.6 ms / 2.8 fps (window) | same | Baseline established |
