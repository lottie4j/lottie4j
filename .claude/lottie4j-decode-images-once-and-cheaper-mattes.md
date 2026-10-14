---
id: task-2
title: lottie4j - decode image assets once and make matte rendering cheaper
status: In Progress
labels: [performance, lottie4j, dependency]
created_date: '2026-10-13'
---

## Description

The animated MelodyMatrix logo in `TopBar` (`LottiePlayer` from `com.lottie4j:fxplayer`
1.2.6, `logo/melodymatrix.json`) dominates the JavaFX application thread. In the profiling
run of 2026-10-13 (see task-1) **~80% of all FX-thread CPU samples** (1264 of 1573) were in
`ImageRenderer.loadImageFromDataUri`, another ~8% in `MatteRenderer`. Top JFR hot methods for
the whole JVM: `PNGImageLoader2.decodePalette` (34%) and `ByteRgba$ToByteBgraPreConv.doConvert`
(25%). This takes frame time from the visualizers and wastes battery.

Cause, in the lottie4j repo (`~/Documents/GitHub/lottie4j`, module `fxplayer`):

1. `renderer/layer/ImageRenderer.render()` is called for every image layer on **every frame**,
   and each time:
   - looks up the asset with `animation.assets().stream().filter(...)`,
   - `getImageFromAsset()` → `loadImageFromDataUri()` splits the data URI, base64-decodes it
     and creates a **new `javafx.scene.image.Image`** (full PNG decode);
   - `loadImageFromFile()` has the same per-frame load for external images.
2. `renderer/layer/MatteRenderer` per matte layer per frame:
   - two synchronous `Canvas.snapshot()` calls (GPU read-back, creates render-target textures),
   - a new `WritableImage` in `applyMatte`,
   - pixel sampling (`getPixelReader().getColor(...)`) that only feeds `logger.debug` but runs
     even when debug logging is off.
3. App side: `TopBar` calls `play(startMarker, endMarker)`, which loops forever, so the logo
   renders at display frame rate for the whole session, also when the window is in the background.

## Fix in lottie4j

1. `ImageRenderer`: cache decoded images per asset, e.g. `Map<String, Image>` keyed by asset id
   (one `ImageRenderer` per `LottiePlayer`, field `imageRenderer` in `LottiePlayer`, so an
   instance map is enough). Also index the assets by id once instead of streaming per frame.
   Better still: pre-decode all image assets in `LottiePlayer.buildRenderCaches()` so the first
   frame doesn't stall either. Cache misses (`null`) should be cached too, so a broken asset
   doesn't log a warning 60 times per second.
2. `MatteRenderer`:
   - guard the debug pixel sampling with `if (logger.isDebugEnabled())`,
   - reuse the `WritableImage`/`int[]` buffers between frames while the size is unchanged,
   - longer term: avoid `snapshot()` per frame (e.g. composite with `BlendMode`/clip on the
     canvas, or render the matte into reused buffers), since read-backs are slow on Metal.
3. Tests in `fxplayer/src/test/java/com/lottie4j/fxplayer/...`: render N frames of an
   animation with an embedded base64 image and assert the asset was decoded once (same
   `Image` instance for every frame, or a counting decoder hook).
4. Release a new lottie4j version.

## Follow-up in MelodyMatrix

1. Bump `lottie4j.version` in `../MainApplication/pom.xml` (currently 1.2.6).
2. Decide the intended logo behaviour in `TopBar`: if it doesn't need to loop forever, play it
   once (`playOnceFromStart()` / stop at the end marker), or at least `stop()` it while the
   stage is iconified or unfocused and resume on focus.
3. Re-run the profiling procedure (task-1) and check the summary section
   "FX thread - first app/library frame": `ImageRenderer.loadImageFromDataUri` should be gone.
   Also check whether the Metal `int[]` growth from task-3 changes, as the matte snapshots
   create render-target textures every frame.

## Acceptance criteria

- [x] Image assets are decoded once per player, not per frame (unit test in lottie4j).
- [x] No debug-only pixel reads when debug logging is disabled.
- [ ] MelodyMatrix uses the new lottie4j version; the profiling summary shows the logo below
      10% of FX-thread samples.

## Implementation notes (2026-10-13, lottie4j working tree, not yet released)

- `ImageRenderer`: `Map<String, Optional<Image>>` cache per asset id (misses cached too);
  `preloadImages(animation)` + `clearCache()` called from `LottiePlayer.buildRenderCaches()`.
- `MatteRenderer`: reuses the two off-screen canvases, the snapshot images, and the `int[]` buffers while the
  size is unchanged. Only the outermost call reuses them (`renderDepth`), because a matted precomp can contain
  mattes rendered with the same instance. The result `WritableImage` is still new per call: the canvas
  reads it when it renders, possibly after another matte in the same frame. Debug pixel sampling is behind
  `logger.isDebugEnabled()`.
- Tests: `ImageRendererTest` (decode once, preload, cached failures), `MatteRendererTest` (reused buffers
  hold no stale pixels, nested matte doesn't corrupt the outer one).
- Measured with `tools/profiling/run.sh marker 1 20000` (fxfileviewer MarkerViewer, melodymatrix.json):
  GC pauses 531 -> 177 per 22 s, PNG decode gone from the hot methods, `applyMatte` allocation 30% -> 0.1%.
  Remaining cost: two `Canvas.snapshot()` read-backs per matte per frame (the "longer term" item).
- Still open: release, MelodyMatrix bump + `TopBar` loop behaviour, snapshot-free matte compositing.

