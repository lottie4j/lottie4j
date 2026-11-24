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
    -Dlottie.bench.csv=/tmp/bench.csv

# 2. Real playback in a window (includes the real pipeline), prints a BENCH summary after N seconds.
mvn -pl fxfileviewer javafx:run -Dlottie.viewer=simple \
    -Djavafx.args="--bench=15 fxfileviewer/src/test/resources/dot/winter-town.lottie"
```

Columns: `record` = time of `render(frame)` (draws into the canvas command buffer, a lower bound);
`raster` = `snapshotFrame(frame)` (also rasterizes and reads pixels back, a slight overestimate).
Stage rows (`layer.plain`, `layer.matte`, `offscreen.draw`, `offscreen.snapshot`) are inclusive and
nested, they do not add up to the frame time.

Always note: machine, JDK, JavaFX version, canvas size, adaptive scaling on/off.

## Baseline

_To be filled in once the first measurements are done._

## Changes

| # | Change | Before (avg ms / achievable fps) | After | Notes |
|---|--------|----------------------------------|-------|-------|
| 0 | Baseline, no renderer changes | — | — | Phase 1: metrics, profiler, benchmark added |
