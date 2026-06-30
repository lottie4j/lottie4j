id: 1565fdee-ff69-4d46-b7e2-d96f631967e2
sessionId: 36dc5c81-b2b5-48d6-bef0-3e57d281b7a9
date: '2026-06-25T14:37:34.297Z'
label: >-
  Investigate rendering gaps per Lottie test file and produce per-file
  improvement plans
---
# Investigate rendering gaps per Lottie test file and produce per-file improvement plans

## Goal
Use `CompareFxViewWithWebViewTest` as the diagnostic harness, walk through every entry of
`lottieJsonFiles()` one by one, identify which Lottie feature(s) cause the JavaFX renderer to
diverge from the WebView reference on which frames, and produce **one dedicated improvement
plan per file** so each future fix can be validated independently by re-running the test in
single-file mode (`-Dlottie.file=…`).

The deliverable of this plan is **not code changes** — it is a set of follow-up task contexts
(one per Lottie file) capturing concrete findings and proposed fixes in `core` and/or
`fxplayer`. Each follow-up plan must link back to the file path so a developer (or Coder) can
reproduce, fix, and verify in a tight loop.

## Design

### Diagnostic harness
`CompareFxViewWithWebViewTest` already gives us everything needed:
- `compareFxAndJsRenderingFullSize(String fileName)` parameterized test over `lottieJsonFiles()`
- Per-frame SSIM (R/G/B + overall) from `ImageSimilarity.compare(...)`
- A diff PNG written to `target/test-output/<file>-webview_scale_1.0/frame_<n>_<score>.png`
  for **every** frame below `TARGET_PER_FRAME_SIMILARITY` (99.5%). The PNG is a 3-pane:
  `FX | WebView reference | heatmap` where red intensity = gap from target.
- Single-file mode via `-Dlottie.file=<path>` for iterating per file.
- `PER_FILE_FLOOR_OVERRIDE` already documents which files miss the bar today, with
  observed averages — this is our pre-prioritised work list.

### Per-file investigation method (apply once per file)
For each file in `lottieJsonFiles()`:

1. **Run the test in single-file mode** to generate fresh diff PNGs:
   ```
   mvn -pl fxfileviewer test \
       -Dtest=CompareFxViewWithWebViewTest#compareFxAndJsRenderingFullSize \
       -Dlottie.file=<file>
   ```
2. **Catalogue failing frames** from the test output (`Frame N @ 100% scale: X% similar
   (R … G … B …)`). Note:
   - which frame numbers are below 99.5%
   - per-channel breakdown (R/G/B) — large per-channel skew points at color/gradient/fill bugs
     vs geometric bugs
   - the minimum frame and its score
3. **Localise the divergence on each failing frame** using the diff PNG heatmap:
   - which screen region(s) light up red
   - is it the same region across frames (static asset bug) or moving (animation/interpolation bug)
   - edge halos vs filled blocks → anti-aliasing/stroke vs fill/color
4. **Map the region to the Lottie source** by opening the JSON in
   `lottie4j/fxfileviewer/src/test/resources/<file>`:
   - identify the **layer** (`layers[*]` by index/name and `nm`)
   - identify the **shape** type(s) the region corresponds to: `gr` (group), `sh` (path), `el`
     (ellipse), `rc` (rect), `sr` (polystar), `st` (stroke), `fl` (fill), `gf/gs` (gradient
     fill/stroke), `tm` (trim path), `tr` (transform), `mm` (merge), `rp` (repeater)
   - check for animated keyframes (`a:1`) around the failing frame on transforms, colors,
     opacities, trim-path properties, gradient stops, mask paths
   - check for `tt` (matte) / `td` (track matte type), `ks` transforms, `op/ip/st`, `parent`,
     and `hasMask` — these are common silent-divergence causes
5. **Form a hypothesis** about the lottie-part that diverges, e.g.:
   - "Gradient fill (`gf`) with > 2 stops not interpolated identically to WebView"
   - "Trim path (`tm`) start/end keyframe interpolation off by 1 frame"
   - "Layer matte (`tt=1` alpha matte) emitting hard edges where reference is soft"
   - "Polystar (`sr`) inner roundness keyframe ignored"
   - "Transform parent chain not applied when `parent` is set and parent is hidden"
   - "Color keyframe interpolated in sRGB where reference uses linear"
6. **Locate the responsible code** in `fxplayer` and/or `core`:
   - `fxplayer/src/main/java/com/lottie4j/fxplayer/renderer/shape/` — Path, Ellipse, Rect,
     Polystar, Group, Stroke, BezierInterpolator
   - `fxplayer/src/main/java/com/lottie4j/fxplayer/renderer/layer/` — Effects, Image, Mask,
     Matte, Precomp, SolidColor, Text, TransformApplier
   - `fxplayer/src/main/java/com/lottie4j/fxplayer/renderer/style/FillRenderer.java`
   - `core/src/main/java/com/lottie4j/core/model/{shape,layer,transform,keyframe,bezier}`
   - `core/src/main/java/com/lottie4j/core/helper/` for keyframe/easing interpolation helpers
7. **Write a dedicated follow-up plan** for the file (see template below). Use
   `createTaskContext` so the plan becomes its own session-tracked artifact and is easy to
   pick up later.

### Output: one follow-up plan per file
Naming convention for follow-up plans:
> `Fix rendering: <file>` (e.g. `Fix rendering: json/angry_bird.json`)

Each follow-up plan must contain:
- **File link** — exact path `lottie4j/fxfileviewer/src/test/resources/<file>` so the JSON
  can be inspected directly.
- **Reproduce** — the single-file `mvn` command from step 1.
- **Observed gap** — current avg / min / floor / target similarity (copy from test output).
- **Failing frames** — list of frames < 99.5% with R/G/B scores.
- **Diff evidence** — relative path of the most informative diff PNG(s) produced under
  `target/test-output/…` (developers re-generate locally).
- **Lottie part diagnosis** — layer index/name + shape type(s) + animated properties suspected.
- **Root cause hypothesis** — which renderer/model class is wrong and why.
- **Proposed fix** — concrete change in `fxplayer` and/or `core` with file paths.
- **Validation** — re-run the same single-file command and confirm:
  - all listed failing frames now ≥ 99.5%, **or**
  - average ≥ `TARGET_AVERAGE_SIMILARITY` so the entry can be deleted from
    `PER_FILE_FLOOR_OVERRIDE` in
    `lottie4j/fxfileviewer/src/test/java/com/lottie4j/fxfileviewer/CompareFxViewWithWebViewTest.java`.

## Implementation Steps

### Step 1: Prepare reference images
Reference PNGs must exist locally — without them the test self-skips (`Assumptions.assumeTrue`).
- `lottie4j/fxfileviewer/src/test/java/com/lottie4j/fxfileviewer/WebViewScreenshotGenerator.java`
  — run `main()` once if the `<file>-webview/` directories under
  `src/test/resources/json/` or `src/test/resources/dot/` are missing. Requires Chrome.

### Step 2: Prioritised investigation queue
Investigate in this order (worst gap first — these are the entries in
`PER_FILE_FLOOR_OVERRIDE`, sorted by observed average ascending):

1. `json/java_duke_fadein.json`             — observed 97.47, min dips to 78.14 ← biggest gap
2. `json/lottie_lego.json`                  — observed 98.14
3. `json/java_duke_slidein.json`            — observed 98.60 (likely same off-frame as fadein)
4. `json/face-peeking.json`                 — observed 98.72
5. `json/angry_bird.json`                   — observed 99.26
6. `json/animated_background_patterns.json` — observed 99.36 (min 81.51)
7. `json/sandy_loading.json`                — observed 99.39
8. `dot/demo-1.lottie`                      — observed 99.44 (min 91.00)
9. `json/isometric_data_analysis.json`      — observed 99.49 (marginal)

Then sweep the remaining files (currently passing at TARGET) to catch silent regressions or
borderline frames just above the bar:

10. `json/box-moving-changing-color.json`
11. `json/face-exhaling.json`
12. `json/foojay-duke.json`
13. `json/foojay-reporter.json`
14. `json/java_duke_flip.json`
15. `json/loading.json`
16. `json/lottie4j.json`
17. `json/pi4j.json`
18. `json/snake_ladder_loading_animation.json`
19. `json/success.json`
20. `json/timeline_animation.json`
21. `dot/lottie4j.lottie`
22. `dot/demo-2.lottie`
23. `dot/demo-3.lottie`

For each file, perform the **6-step method** described above.

### Step 3: Per-file follow-up plan template
Use `createTaskContext` with the title `Fix rendering: <file>` and this body:

```markdown
# Fix rendering: <file>

## File
`lottie4j/fxfileviewer/src/test/resources/<file>`

## Reproduce
mvn -pl fxfileviewer test \
    -Dtest=CompareFxViewWithWebViewTest#compareFxAndJsRenderingFullSize \
    -Dlottie.file=<file>

## Observed gap (date: YYYY-MM-DD)
- average: <X.XX>%
- minimum: <X.XX>% on frame <N>
- floor:   <from PER_FILE_FLOOR_OVERRIDE or 99.5%>
- target:  99.5%

## Failing frames
| frame | overall | R | G | B | notes |
|-------|---------|---|---|---|-------|
| …     | …       | … | … | … | …     |

## Diff evidence
target/test-output/<file>-webview_scale_1.0/frame_<N>_<score>.png

## Lottie part diagnosis
- Layer: index <i>, name "<nm>", type <ty>
- Shape(s): <gr/sh/el/rc/sr/st/fl/gf/gs/tm/tr/mm/rp>
- Animated property: <ks.p / ks.s / ks.r / ks.o / o / c / s / e / …>
- Easing: <linear / bezier with i,o values / hold>

## Root cause hypothesis
<one-paragraph explanation, naming the suspected fxplayer/core class and method>

## Proposed fix
- `lottie4j/fxplayer/src/main/java/com/lottie4j/fxplayer/renderer/…/Foo.java` — <change>
- `lottie4j/core/src/main/java/com/lottie4j/core/…/Bar.java` — <change>

## Validation
1. Re-run the Reproduce command.
2. Confirm every frame listed in "Failing frames" is now ≥ 99.5%.
3. If the new average ≥ 99.5%, remove the entry for `<file>` from
   `PER_FILE_FLOOR_OVERRIDE` in
   `lottie4j/fxfileviewer/src/test/java/com/lottie4j/fxfileviewer/CompareFxViewWithWebViewTest.java`.
4. Re-run the full parameterised test to confirm no regression on other files.
```

### Step 4: Cross-cutting findings
While walking through files, the same root cause will often surface in multiple files
(e.g. gradient interpolation will hit `animated_background_patterns`, `sandy_loading`, and
maybe `lottie_lego`). When that happens:
- In each per-file plan, reference the others ("see also: Fix rendering: <other>")
- Create one **extra cross-cutting plan** titled
  `Fix renderer: <subsystem>` (e.g. `Fix renderer: gradient fill interpolation`) describing
  the shared root cause and the consolidated fix, and link all affected file plans to it.

### Step 5: Track progress
- The single source of truth for "is this file fixed?" is its row in `PER_FILE_FLOOR_OVERRIDE`
  (`CompareFxViewWithWebViewTest.java`, lines around 84–112). A plan is considered done only
  when its entry can be removed from that map or its floor raised to ≥ 99.5%.
- Use `listTaskContexts` to enumerate the per-file plans created so far and check status.

## Reference Examples

- Harness entry point and quality bar:
  `lottie4j/fxfileviewer/src/test/java/com/lottie4j/fxfileviewer/CompareFxViewWithWebViewTest.java:60`
  — `TARGET_PER_FRAME_SIMILARITY = 99.5`
- Pre-classified technical-debt list (use as priority queue):
  `lottie4j/fxfileviewer/src/test/java/com/lottie4j/fxfileviewer/CompareFxViewWithWebViewTest.java:84`
  — `PER_FILE_FLOOR_OVERRIDE` map with observed averages
- Single-file mode wiring:
  `lottie4j/fxfileviewer/src/test/java/com/lottie4j/fxfileviewer/CompareFxViewWithWebViewTest.java:165`
  — `lottieJsonFiles()` reads `-Dlottie.file=…`
- Diff PNG writer (FX | reference | heatmap):
  `lottie4j/fxfileviewer/src/test/java/com/lottie4j/fxfileviewer/CompareFxViewWithWebViewTest.java:402`
  — `saveImage(...)`
- Per-channel + windowed SSIM result we read for diagnosis:
  `lottie4j/fxfileviewer/src/test/java/com/lottie4j/fxfileviewer/util/ImageSimilarity.java:53`
  — `SimilarityResult(overall, red, green, blue, windowScores)`
- Reference image generation (run if `<file>-webview/` is missing):
  `lottie4j/fxfileviewer/src/test/java/com/lottie4j/fxfileviewer/WebViewScreenshotGenerator.java:1`
- Renderer code paths to inspect during diagnosis:
  - `lottie4j/fxplayer/src/main/java/com/lottie4j/fxplayer/renderer/shape/` (Path, Ellipse,
    Rect, Polystar, Group, Stroke, `PathBezierInterpolator`)
  - `lottie4j/fxplayer/src/main/java/com/lottie4j/fxplayer/renderer/layer/` (Mask, Matte,
    Precomp, SolidColor, Text, `TransformApplier`, Effects)
  - `lottie4j/fxplayer/src/main/java/com/lottie4j/fxplayer/renderer/style/FillRenderer.java`
- Model / interpolation:
  - `lottie4j/core/src/main/java/com/lottie4j/core/model/keyframe/`
  - `lottie4j/core/src/main/java/com/lottie4j/core/model/bezier/`
  - `lottie4j/core/src/main/java/com/lottie4j/core/helper/`

## Verification

This is a meta-plan whose output is other plans, so verification is on the process and on
each follow-up:

1. After investigating each file, confirm a corresponding `Fix rendering: <file>` plan exists
   (use `listTaskContexts`).
2. Each follow-up plan must contain a concrete `Reproduce` command and at least one named
   class in `fxplayer` or `core` as the proposed change site — otherwise it is not actionable.
3. After applying a follow-up plan's fix, the validation steps inside that plan must show
   either all listed failing frames ≥ 99.5% or average ≥ 99.5%, and the file's row in
   `PER_FILE_FLOOR_OVERRIDE` must be removed or its floor raised.
4. Re-run the full parameterised test to confirm fixes don't regress other files:
   ```
   mvn -pl fxfileviewer test -Dtest=CompareFxViewWithWebViewTest
   ```
