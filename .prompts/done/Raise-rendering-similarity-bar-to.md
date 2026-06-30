id: 9a08e7e6-410c-4928-84b2-fc39834f3c65
sessionId: deabda87-e54e-44f6-9067-8b61dcb741c4
date: '2026-06-24T10:16:17.648Z'
label: Raise rendering similarity bar to 99.5% per frame (color-aware, every frame)
---
# Raise rendering similarity bar to 99.5% per frame (color-aware, every frame)

## Goal

Tighten the JavaFX-vs-WebView rendering regression test so it expresses a clear long-term quality bar (99.5% per-frame, including color), enable AI-driven file-by-file improvement workflows, and increase coverage from every-5th-frame to every-frame. The threshold cannot be reached today, so the plan also introduces a per-file override map that lets the test pass against the *current* known floor while making the gap visible.

## Design

### 1. Better similarity metric (3 sub-improvements, all needed to make 99.5 meaningful)

The current `compareImages` in `CompareFxViewWithWebViewTest` has three weaknesses that combine to make 99.5% per-frame either trivially unreachable or, worse, *meaningless*:

- **Grayscale-only**: `pixelToGrayscale` collapses RGB to luminance before SSIM. Color regressions can pass.
- **Global SSIM** (one mean/variance for the whole image): too generous on large flat areas (Lottie animations have many), too punishing on small offsets. Standard practice is windowed SSIM (8×8 or 11×11 sliding windows averaged).
- **Alpha is silently dropped**: Lottie animations frequently have transparent backgrounds. If FX paints over scene-default while Chrome paints over `background: transparent`, scores are dominated by background mismatch instead of content.

We replace `compareImages` with a new `ImageSimilarity` utility class supporting:
- Per-channel SSIM averaged across R, G, B (color-aware).
- Windowed SSIM with configurable window (default 8×8, stride 8 for speed; document the trade-off).
- Alpha-aware compositing: both images composited over the same opaque background (white) before SSIM, so transparent-vs-opaque background mismatches don't dominate.
- Returns a structured `SimilarityResult` (overall %, per-channel %, plus optionally a heatmap-ready per-window score array) so we can render richer diffs in the failure PNG.

### 2. Tiered threshold with per-file overrides

`SIMILARITY_THRESHOLD = 98` becomes a **target** of `99.5`, with a per-file override map for animations that don't meet the target yet. Three knobs:

- `TARGET_PER_FRAME_SIMILARITY = 99.5` — the long-term per-frame floor.
- `TARGET_AVERAGE_SIMILARITY = 99.5` — long-term average floor (same value, but kept separate so we can tune independently).
- `Map<String, Double> PER_FILE_FLOOR_OVERRIDE` — current measured floor per file (e.g. `"json/sandy_loading.json" -> 96.0`). When present, that file's assertion uses the override; otherwise the target applies.

The test prints, for each file, both the achieved score and the gap to the target — so the override map is visible technical debt, not hidden tolerance.

Diff PNGs are saved for any frame below the *target* (not the override), so AI improvement loops always have material to work with even on "passing" files.

### 3. Every-frame sampling (drop `outPoint - 5`)

`buildSampledFrames` is called from both the generator and the test with `step=5` and `outPoint - 5`. We change call sites to `step=1` and `lastFrame=outPoint`. The helper itself stays generic (still accepts step) so it can be unit-tested.

`Thread.sleep(200)` between seek and snapshot becomes a problem at 5× the frame count. We replace it with a deterministic wait on the JavaFX pulse (one `Platform.runLater` round-trip after seek is normally enough; if not, a counted-pulse wait via `AnimationTimer`).

### 4. File-by-file callability (no code change, doc only)

`@ParameterizedTest @MethodSource("lottieJsonFiles")` already generates one JUnit invocation per file. AI tools can run a single file with:

```
mvn -pl fxfileviewer test \
  -Dtest='CompareFxViewWithWebViewTest#compareFxAndJsRenderingFullSize' \
  -Dsurefire.failIfNoSpecifiedTests=false
```

…but Surefire's parameter-row filter is awkward to quote. We add a *non-breaking* convenience: an optional `-Dlottie.file=<path>` system property that, when set, filters `lottieJsonFiles()` to only that entry. Empty/unset = full list (current behavior).

This keeps the single source of truth (the list in `lottieJsonFiles()`) and adds zero new files.

### 5. Storage sanity check for every-frame references

Going from step=5 → step=1 multiplies committed PNG count by ~5. Today's reference set is hundreds of files spread across ~20 animations; worst case is `face-peeking` at ~226 frames. Estimated total post-change: ~250 MB. Within GitHub's hard limits (100 MB/file, soft <1 GB repo) but worth measuring.

Mitigation built into the plan:
- Add `oxipng`-equivalent in-JVM PNG re-encoding pass in `WebViewScreenshotGenerator`: write the screenshot bytes, then re-encode through `ImageSaver` with max-compression deflate. Most reference frames are simple cartoons and shrink 30–60%.
- Document a `du -sh src/test/resources` step in the regeneration runbook.

## Implementation Steps

### Step 1: Introduce `ImageSimilarity` utility with color + windowed + alpha-aware SSIM

- `lottie4j/fxfileviewer/src/test/java/com/lottie4j/fxfileviewer/util/ImageSimilarity.java` (new) — moves the comparison math out of the test class. Public API:
  - `record SimilarityResult(double overall, double red, double green, double blue, double[][] windowScores)`.
  - `static SimilarityResult compare(WritableImage a, WritableImage b)` — composites both over opaque white, computes per-channel windowed SSIM (8×8 windows, stride 8), averages R/G/B for `overall`.
  - Keep the SSIM constants `c1 = 6.5025`, `c2 = 58.5225` (consistent with current code, just per-channel now).
  - `private static int compositeOverWhite(int argb)` — alpha blend each pixel onto `0xFFFFFFFF`.
- Add a small unit test `lottie4j/fxfileviewer/src/test/java/com/lottie4j/fxfileviewer/util/ImageSimilarityTest.java`:
  - identical images → 100.0
  - all-red vs all-blue → significantly < 100 (regression catches a future grayscale-only bug)
  - identical-but-with-different-transparent-pixels → still 100 (proves alpha compositing works)

### Step 2: Use `ImageSimilarity` in `CompareFxViewWithWebViewTest`

- `lottie4j/fxfileviewer/src/test/java/com/lottie4j/fxfileviewer/CompareFxViewWithWebViewTest.java`:
  - Delete `compareImages`, `pixelToGrayscale`, `calculateMean`, `calculateVariance`, `calculateCovariance` (lines 226–276 area).
  - Call `ImageSimilarity.compare(fxImg[0], reference)` and use `result.overall()`.
  - Extend `saveImage` to also render a third panel: a heatmap of `result.windowScores()` (red = bad windows). This makes per-frame failures actionable for AI improvement loops. If too much scope for one step, keep this in a follow-up — leave a `// TODO` and ship the 3-panel as Step 6 only if it falls out cleanly.

### Step 3: Tiered thresholds + per-file override map

- `CompareFxViewWithWebViewTest.java`:
  - Replace `private static final double SIMILARITY_THRESHOLD = 98;` with:
    ```java
    private static final double TARGET_PER_FRAME_SIMILARITY = 99.5;
    private static final double TARGET_AVERAGE_SIMILARITY = 99.5;
    private static final Map<String, Double> PER_FILE_FLOOR_OVERRIDE = Map.ofEntries(
        // Populated empirically on first run; entries are technical debt to drive down.
    );
    ```
  - Add `private static double floorFor(String fileName) { return PER_FILE_FLOOR_OVERRIDE.getOrDefault(fileName, TARGET_AVERAGE_SIMILARITY); }`.
  - Save a diff PNG for any frame below `TARGET_PER_FRAME_SIMILARITY` (not the per-file override) so AI loops always see "what's still wrong".
  - Assert: `assertTrue(average >= floorFor(fileName), …)` and include both the override and the target in the failure message, e.g. `"sandy_loading: 96.7 < floor 97.0 (target 99.5, gap 2.8)"`.
  - At end of each invocation, log the gap to target so it shows up in CI output.

### Step 4: Every-frame sampling

- `CompareFxViewWithWebViewTest.java` line 135 — change to `buildSampledFrames(inPoint, outPoint, 1)`.
- `WebViewScreenshotGenerator.java` line 112 — change to `buildSampledFrames(inPoint, outPoint, 1)`.
- Keep `buildSampledFrames(int firstFrame, int lastFrame, int step)` signature (still useful for tests, and harmless to leave generic).
- Add a unit test for `buildSampledFrames` in a new `WebViewScreenshotGeneratorTest` (covers step=1 inclusive of `outPoint`, step=5 still works, single-frame animations, `inPoint == outPoint`).

### Step 5: Replace `Thread.sleep(200/100)` with deterministic JavaFX sync

- `CompareFxViewWithWebViewTest.java`:
  - Replace the `seekToFrame` + `Thread.sleep(200)` pattern with a helper `waitForRender(LottiePlayer player, int frame)` that:
    1. Runs `seekToFrame(frame)` on FX thread inside a `CountDownLatch`.
    2. After that latch, queues a second `Platform.runLater` and waits — this ensures one pulse completed before snapshot.
  - If the player exposes any "frameRendered" hook, prefer that; otherwise the double-pulse pattern is standard.
- `WebViewScreenshotGenerator.java`: the existing 3-second poll on `getCurrentFrame()` is already deterministic; keep the final 100 ms safety pause (Selenium snapshot quirk), document it.

### Step 6: `-Dlottie.file=` filter for one-file runs

- `CompareFxViewWithWebViewTest.java`:
  - In `lottieJsonFiles()`, after building the full stream, apply a filter from `System.getProperty("lottie.file")` when non-empty.
  - Add a Javadoc note: `mvn test -pl fxfileviewer -Dtest=CompareFxViewWithWebViewTest -Dlottie.file=json/angry_bird.json`.

### Step 7: PNG size mitigation in the generator

- `WebViewScreenshotGenerator.java`:
  - After `driver.getScreenshotAs(OutputType.BYTES)`, re-encode via `ImageSaver.writePNG` with max-deflate compression (Deflater.BEST_COMPRESSION). Keep dimensions identical — content is byte-for-byte equivalent, just better packed.
  - Add to the runbook (Javadoc on `main`): "Run `du -sh src/test/resources/json src/test/resources/dot` before committing; expected < 500 MB."

### Step 8: Initial calibration pass (manual, but planned)

- Run the test once with empty `PER_FILE_FLOOR_OVERRIDE` and `TARGET_AVERAGE_SIMILARITY = 99.5` to capture the current floor per file from CI output. Populate `PER_FILE_FLOOR_OVERRIDE` with measured values *minus a small safety margin* (e.g. `floor(observed * 10) / 10 - 0.5`). Commit. This is the baseline from which 99.5 is the climb.
- Document in the test Javadoc: "Override entries are technical debt. Driving any entry to ≥ 99.5 means it can be removed."

### Step 9: Half-size test decision

- `compareFxAndJsRenderingHalfSize` (line 100) only runs file index 1 and applies bilinear smoothing on the reference, which inherently lowers SSIM. With per-channel windowed SSIM the absolute numbers shift; the test will likely need its own override.
- Keep the test as a parameterized test too (same file list), so half-size is also exercised file-by-file. Add a separate `PER_FILE_FLOOR_OVERRIDE_HALF` map. If this expands scope too much, leave half-size unchanged and add a `// TODO` referencing this plan.

## Reference Examples

- `lottie4j/fxfileviewer/src/test/java/com/lottie4j/fxfileviewer/CompareFxViewWithWebViewTest.java:51` — current threshold constant being replaced.
- `lottie4j/fxfileviewer/src/test/java/com/lottie4j/fxfileviewer/CompareFxViewWithWebViewTest.java:135` — current sampling call site (step=5, `outPoint - 5`).
- `lottie4j/fxfileviewer/src/test/java/com/lottie4j/fxfileviewer/CompareFxViewWithWebViewTest.java:226-276` — current SSIM math being replaced wholesale.
- `lottie4j/fxfileviewer/src/test/java/com/lottie4j/fxfileviewer/WebViewScreenshotGenerator.java:112` — generator's sampling call site.
- `lottie4j/fxfileviewer/src/test/java/com/lottie4j/fxfileviewer/WebViewScreenshotGenerator.java:228-238` — `buildSampledFrames` helper (kept generic).
- `lottie4j/fxfileviewer/src/main/java/com/lottie4j/fxfileviewer/util/ImageSaver.java` — pure-Java PNG writer; reuse for the re-encode in Step 7 and the heatmap in Step 2.

## Verification

After each step, before moving on:

1. **Compile**: `mvn -pl fxfileviewer -am compile test-compile`
2. **Unit tests for the new helpers**:
   - `mvn -pl fxfileviewer test -Dtest=ImageSimilarityTest`
   - `mvn -pl fxfileviewer test -Dtest=WebViewScreenshotGeneratorTest`
3. **Regenerate reference images** (local, one-off):
   - `mvn -pl fxfileviewer exec:java -Dexec.mainClass=com.lottie4j.fxfileviewer.WebViewScreenshotGenerator` (or run from IDE)
   - `du -sh lottie4j/fxfileviewer/src/test/resources/json lottie4j/fxfileviewer/src/test/resources/dot` — sanity-check size.
4. **Calibration run** (Step 8): full test, capture per-file averages from CI output, populate `PER_FILE_FLOOR_OVERRIDE`.
5. **Full regression**: `mvn -pl fxfileviewer test -Dtest=CompareFxViewWithWebViewTest`
6. **Single-file AI loop sanity check**: `mvn -pl fxfileviewer test -Dtest=CompareFxViewWithWebViewTest -Dlottie.file=json/angry_bird.json` — must run exactly one file.
7. **Visual spot-check**: open one diff PNG under `target/test-output/json/<name>-webview_scale_1.0/` and confirm the heatmap (if Step 2 included it) highlights real differences.

## Out of scope (potential follow-ups)

- Replacing the JavaFX `SnapshotParameters` path with direct canvas rasterization (would remove anti-alias differences vs the browser at the source).
- Tracking `PER_FILE_FLOOR_OVERRIDE` history over time (a CSV/markdown report so progress toward 99.5 is visible across PRs).
- Generalizing the heatmap diff into a standalone CLI tool for ad-hoc comparisons.
