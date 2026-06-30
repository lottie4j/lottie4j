id: 971ee99f-02ac-44ac-90db-979493460d82
sessionId: f0921a2e-cd98-443c-bd4e-435207b0f44a
date: '2026-06-30T15:13:34.994Z'
label: >-
  Follow-up: Close residual gap on interactive_mood_selector_ui (95.58 % → ≥
  99.5 %)
---
# Follow-up: Close residual gap on `interactive_mood_selector_ui.json`

This is the required follow-up plan called out in the *Required follow-up plan* section of
`lottie4j/.prompts/done/Fix-rendering-json-interactive-mood.md` (lines ~163–185). The previous
plan lifted the file from **92.40 % → 95.58 %** through HSL text-animator deltas and a
multi-scale Gaussian-blur scheme, then parked the residual ~3.9 pp behind a temporary floor of
**95.4 %** in `PER_FILE_FLOOR_OVERRIDE`.

## File
`lottie4j/fxfileviewer/src/test/resources/json/interactive_mood_selector_ui.json`

## Goal
Lift the file's average per-frame similarity from **95.58 %** to **≥ 99.5 %** so the
`PER_FILE_FLOOR_OVERRIDE` entry can be removed, **without** regressing any of the other 14
files that currently pass or any of the 8 files that carry their own legacy floors.

## Reproduce
```
mvn -pl fxfileviewer test \
    -Dtest=CompareFxViewWithWebViewTest#compareFxAndJsRenderingFullSize \
    -Dlottie.file=json/interactive_mood_selector_ui.json
```
or via the VS Code task `calibrate: interactive_mood_selector_ui` already installed in
`lottie4j/.vscode/tasks.json:73-83`.

## Observed gap going in (from the predecessor's `Outcome` table)
- average: **95.58 %** (floor 95.4)
- minimum: **93.08 %**
- frame 0 per-channel: **R 94.65 / G 95.97 / B 98.60** — B already at target, R/G both ~4 pp
  short, the same warm-channel skew that survives across the whole file
- Heatmap location reported by the predecessor: **warm-colour body edges, not halo rings**

This per-channel signature (R/G short, B fine) and the spatial location (gradient-filled emoji
bodies, not the soft halo ring) point at **gradient-fill sampling**, not blur. The blur cascade
is now plausibly correct in shape but locally non-Gaussian, so the secondary contributor is the
piecewise-linear upsample. Tertiary is the `o = 70` precomp opacity.

## Design — three steps, gated by re-measurement

The predecessor explicitly ordered these by expected magnitude. The plan honours that order so
the cheapest, highest-payoff change is shipped first and each subsequent step is justified by a
fresh measurement, not by speculation. **Stop and re-evaluate after each step.** If average
crosses 99.5 % earlier than expected, drop the remaining steps and skip to the cleanup.

| Step | Hypothesis                                            | Expected lift | Risk to other files                                       |
|------|-------------------------------------------------------|---------------|-----------------------------------------------------------|
| 1    | Path-vertex bounds underestimate gradient axis extent | ~2–3 pp       | Affects every path with a `gf` fill — must check sentinels |
| 2    | BoxBlur + bilinear is not a true Gaussian             | ~1–1.5 pp     | Only files with `Blurriness` > 200 — only mood-selector   |
| 3    | Precomp/layer opacity (`o = 70`) compositing          | residual      | Any file with non-trivial precomp `o`                     |

## Implementation Steps

### Step 1 — Use full path extent (vertices + bezier control reach) for gradient bounds

**Root cause.** `lottie4j/fxplayer/src/main/java/com/lottie4j/fxplayer/renderer/shape/PathRenderer.java:269-295`
computes the bounds passed to `GradientFillStyle.getPaint(...)` from path **vertices only**:

```java
for (List<Double> vertex : vertices) {
    if (vertex.size() >= 2) {
        double x = vertex.get(0);
        double y = vertex.get(1);
        minX = Math.min(minX, x);
        ...
    }
}
```

For an emoji body shape, the vertex hull is a tight polygon connecting brow / cheek / chin
anchor points. The *visible silhouette* is a smooth cubic-bezier curve that bulges outside that
hull by the tangent control points (`tangentsIn` / `tangentsOut`). The Lottie gradient `s` / `e`
anchors (e.g. `s = (x, y_brow_top)`, `e = (x, y_chin_bottom)`) almost always sit **on the
visible silhouette**, i.e. outside the vertex hull. `GradientFillStyle.getPaint(...)` in
`lottie4j/fxplayer/src/main/java/com/lottie4j/fxplayer/element/GradientFillStyle.java:118-132`
then computes proportional coordinates as

```java
double propStartY = (startY - shapeY) / shapeHeight;
double propEndY   = (endY   - shapeY) / shapeHeight;
```

which over-stretches the gradient because `shapeHeight` is the *under-sized* vertex hull
height. Stops that should sit at proportional `0.0` and `1.0` end up at e.g. `-0.07` and `1.05`,
clipped by `CycleMethod.NO_CYCLE`, so a few rows of pixels at the top of the brow and bottom of
the chin receive the *flat end-stop colour* instead of the interpolated gradient. Those pixels
are exactly the warm-colour edges the predecessor identified.

This is the *Alternate hypothesis* the predecessor flagged and is also the unfinished work
called out in `lottie4j/.prompts/done/Fix-renderer-gradient-fill-channels.md` (the parsing
fixes there did **not** touch geometry). The fix is to compute the full *path extent* —
vertices **plus** the reach of their cubic-bezier handles — and use that as the gradient
bounds.

**Change set.**

- `lottie4j/fxplayer/src/main/java/com/lottie4j/fxplayer/renderer/shape/PathRenderer.java`
  - Rename `calculateBounds(List<List<Double>>)` to `calculateVertexBounds` (still used as a
    cheap fallback if tangent lists are null/short) and add
    `calculateGeometryBounds(vertices, tangentsIn, tangentsOut, closed)` that returns the
    `[minX, minY, width, height]` tight bounding box of the actual painted cubic-bezier path.
  - Implementation: for each cubic segment between vertex `i` and vertex `i+1` with control
    points `P1 = V[i] + tangentsOut[i]` and `P2 = V[i+1] + tangentsIn[i+1]`, derive the segment
    extrema analytically. For each axis solve `B'(t) = 3·(1-t)²·(P1-V[i]) + 6·(1-t)·t·(P2-P1)
    + 3·t²·(V[i+1]-P2) = 0` for `t ∈ (0, 1)` (quadratic in `t`), then include `B(t)` for each
    real root plus the segment endpoints. Repeat for the closing segment when `closed` is
    `true`. Add a private helper `addCubicExtrema(double v0, double c1, double c2, double v3,
    DoubleConsumer accept)` used twice per segment (X then Y).
  - At call site (line 73), replace `double[] bounds = calculateBounds(vertices);` with
    `double[] bounds = calculateGeometryBounds(vertices, tangentsIn, tangentsOut,
    bezierDef.closed());`. Fall back to `calculateVertexBounds(vertices)` if tangent lists are
    missing or any segment has fewer than two components.
  - Keep the existing behaviour for non-bezier (lineTo) segments — extrema reduce to the two
    endpoints when both tangents are zero.

- `lottie4j/fxplayer/src/test/java/com/lottie4j/fxplayer/renderer/shape/PathRendererBoundsTest.java` (new)
  - Unit test the new helper directly (no JavaFX canvas required). Cases:
    - Single straight segment (both tangents zero) ⇒ bounds equal vertex bounds.
    - Symmetric circle approximated by four cubic segments with the standard `0.5522847…`
      handle length ⇒ bounds match the inscribed circle's bounding square to within `1e-6`.
    - Asymmetric path with `tangentsOut[0]` extending well above the vertex hull ⇒ returned
      `minY` is below the smallest vertex Y by the analytical extremum (compare to a hand-
      computed value).
    - Tangents lists shorter than the vertex list ⇒ falls back to vertex bounds.
    - Open path (closed = false) ignores the implicit closing segment; closed path includes it
      and the test crafts a tangent that makes the closing segment widen the bounds.

- `lottie4j/fxplayer/src/main/java/com/lottie4j/fxplayer/renderer/shape/EllipseRenderer.java`
  - Ellipses already use the *bounding box* `(renderX, renderY, width, height)` so the same
    issue does **not** apply there — leave the calls at lines 98 and 280 alone, but add a
    short Javadoc note on the `getGradientFillStyle` helper at line 157 explaining why
    ellipses already pass their painted bounds.

- `lottie4j/fxplayer/src/main/java/com/lottie4j/fxplayer/renderer/shape/RectangleRenderer.java`
  - Rectangles likewise use shape bounds directly; no change but add a one-line Javadoc cross
    reference next to line 82.

**Verification gate for Step 1.**

1. `mvn -pl core install -DskipTests -q && mvn -pl fxplayer test -q` — new bounds tests pass.
2. `calibrate: interactive_mood_selector_ui` — record new average, minimum, and per-channel
   frame-0 numbers.
3. `calibrate: regression sentinels` — confirm no entry in `lottieJsonFiles()` regresses below
   its current floor. Pay particular attention to the eight files already in
   `PER_FILE_FLOOR_OVERRIDE` plus `face-exhaling`, `java_duke_fadein`, and `dot/demo-1.lottie`
   which use `gf` fills (per `Fix-renderer-gradient-fill-channels.md`).
4. If average ≥ 99.5 %: jump to *Cleanup*. Otherwise continue.

### Step 2 — Replace BoxBlur upsample with cascaded GaussianBlur(60) passes

Only attempt this step if Step 1 lands a measurable lift but the file is still under 99.5 %.

**Root cause.** `EffectsRenderer.applyBlurEffect` at
`lottie4j/fxplayer/src/main/java/com/lottie4j/fxplayer/renderer/layer/EffectsRenderer.java:301-309`
selects `BoxBlur(r, r, 3)` for radii above 40 px. The predecessor raised
`MAX_PASS_BLUR_RADIUS` to 200 to keep the downsample at 4× for raw 700–800. The remaining
problem is that the upsample from the 4× downsampled raster is JavaFX's *bilinear*
`drawImage`, which is a piecewise-linear filter; combined with the box-filter pass it produces
the soft-but-visibly-stepped halo edge described in the predecessor's "BoxBlur shape vs. true
Gaussian" bullet.

**Fix sketch.** Cascade `n` `GaussianBlur(60)` passes on the offscreen image — Gaussian σ
adds in quadrature so `σ_total = σ_pass · √n`. For a target effective radius `R` after the
downsample factor `d`, choose `n = ceil((R / d / 60)²)` and the per-pass radius `r_pass = (R /
d) / √n`. Two or three passes of `GaussianBlur(60)` reach σ ≈ 85–105 px per offscreen pixel
without ever touching `BoxBlur`, so the final image is a true Gaussian even after the 4×
bilinear upsample (Gaussian × bilinear ≈ Gaussian + minor blur).

**Change set.**

- `EffectsRenderer.java`
  - Add `private static final double GAUSSIAN_CASCADE_PASS_RADIUS = 60.0;` next to the
    existing `MAX_PASS_BLUR_RADIUS`.
  - Introduce `private void applyCascadedGaussian(GraphicsContext offscreenGc, double
    passRadius, WritableImage raster, double width, double height)` that:
    1. Computes `n = max(1, (int) Math.ceil(Math.pow(passRadius / GAUSSIAN_CASCADE_PASS_RADIUS,
       2.0)))`.
    2. Computes `perPassRadius = passRadius / Math.sqrt(n)`.
    3. For each of the `n` passes: install `new GaussianBlur(perPassRadius)`, `drawImage(...)`
       into a ping-pong buffer, swap. JavaFX `GaussianBlur` is documented to clamp at 63 px so
       the loop must assert `perPassRadius ≤ 63`; bump `n` and recompute if not.
    4. Returns the final ping-pong raster.
  - Modify both `renderWithBoundedBlur` (lines ~232–253) and `createStaticBlurCacheImage`
    (lines ~395–410) to use the cascade. The downsample factor still controls how much
    composition-space radius is bought "for free" via bilinear, but the per-pass kernel is
    now Gaussian, not BoxBlur.
  - Keep `applyBlurEffect` for the no-bounds path (line 218) — that path already caps at
    `MAX_PASS_BLUR_RADIUS = 200` and would not benefit from the cascade since there is no
    offscreen ping-pong buffer available.

- `lottie4j/fxplayer/src/test/java/com/lottie4j/fxplayer/renderer/layer/EffectsRendererTest.java`
  - Extend the existing class (the predecessor added it). New cases:
    - `applyCascadedGaussian` chooses `n` = 1 for `passRadius ≤ 60`, `n` = 4 for
      `passRadius = 120` (so per-pass = 60), `n` = 9 for `passRadius = 180`.
    - Property: for `passRadius ∈ {50, 70, 100, 150, 200}`, `n · perPassRadius²` is within
      `1e-9` of `passRadius²` (quadrature identity).
    - Per-pass radius never exceeds the documented `GaussianBlur` cap of 63 px.
    - Bounded smoke render at raw=800: peak red of the cascaded path is **lower** than the
      pre-change `BoxBlur` path on a single-pixel red source (Gaussian spreads energy further
      and more symmetrically than a 3-iteration box). The test fixture should be a single
      red dot rendered into 200×200 with bounds 200×200 — the cascaded mean intensity at the
      kernel edge should be smoother (assert monotonic decrease in red intensity sampled along
      the diagonal).

**Verification gate for Step 2.**

1. `test: fxplayer unit tests`.
2. `calibrate: interactive_mood_selector_ui`.
3. `calibrate: regression sentinels`. Only `interactive_mood_selector_ui` has `Blurriness > 200`
   so a regression elsewhere from a Gaussian cascade is implausible — verify, don't assume.
4. If average ≥ 99.5 %: jump to *Cleanup*. Otherwise continue.

### Step 3 — Investigate precomp/layer opacity (`o = 70`) compositing

Only attempt if Steps 1+2 combined still leave the file below 99.5 %.

**Hypothesis (predecessor's words).** Several emoji layers carry `"o": 70` (70 % opacity).
JavaFX `gc.setGlobalAlpha(0.7)` premultiplies *after* the layer raster has been blurred, but
the dotlottie-wc reference applies opacity inside the layer compositor *before* the halo
gradient meets the parent precomp background, so the visible halo colour is darker in the
reference than in the FX output by a small constant factor.

**Exploration first, then fix.** Unlike Steps 1 and 2 this is speculative until measured.

- Grep the JSON for `"o": 70` and identify which layers carry it. Cross-check whether they
  are the same layers that have Gaussian blur (likely) or distinct layers.
- Add a temporary `logger.debug` line in
  `lottie4j/fxplayer/src/main/java/com/lottie4j/fxplayer/renderer/layer/LayerRenderer.java`
  (or wherever the per-layer `opacity()` is applied) printing the order of operations
  (alpha set, then blur, vs. blur, then alpha set).
- If the order is "blur first, then alpha", the offscreen ping-pong buffer from Step 2 already
  gives us a place to fold alpha *into* the raster before the bilinear upsample. Add a single
  `gc.setGlobalAlpha(opacity)` immediately before drawing the layer into the offscreen, and
  remove the post-blur alpha multiplication.
- If the predecessor's fix already handles this correctly, this step lands no production code
  and only updates the plan's *Outcome* section.

**Verification gate for Step 3.** Same as Step 2.

### Cleanup (do this no matter which step closed the gap)

- In `lottie4j/fxfileviewer/src/test/java/com/lottie4j/fxfileviewer/CompareFxViewWithWebViewTest.java:104-117`
  remove the `Map.entry("json/interactive_mood_selector_ui.json", 95.4)` line and its
  surrounding multi-line comment block (lines ~107–112). Leave the eight pre-existing
  entries untouched — they belong to other open plans.
- Update the `PER_FILE_FLOOR_OVERRIDE` Javadoc at lines 89–103: drop the `interactive_mood…`
  bullet, keep the *floor follows `floor(observed * 10) / 10 - 0.1`* sentence intact.
- Move `Fix-rendering-json-interactive-mood.md` from `lottie4j/.prompts/done/` is **not**
  required (the predecessor *is* done — the work landed; the follow-up is this plan, not a
  reopening). Add a final `## Outcome` section to *this* plan once the floor is removed and
  move *this* plan into `lottie4j/.prompts/done/`.
- Add a one-line cross-reference at the end of `Fix-renderer-gradient-fill-channels.md`'s
  `## See also` block noting that the *bounds* part of its `gf` story was resolved here.

## Reference Examples

- `lottie4j/fxplayer/src/main/java/com/lottie4j/fxplayer/renderer/shape/PathRenderer.java:269-295`
  — the vertex-only bounds calculation that under-sizes the painted region.
- `lottie4j/fxplayer/src/main/java/com/lottie4j/fxplayer/element/GradientFillStyle.java:90-145`
  — how proportional gradient coordinates are derived from the supplied bounds; this code is
  **correct**, the bug is upstream in the bounds.
- `lottie4j/fxplayer/src/main/java/com/lottie4j/fxplayer/renderer/layer/EffectsRenderer.java:232-253`
  — the offscreen render + bilinear-upsample pipeline that Step 2 cascades a Gaussian into.
- `lottie4j/fxplayer/src/main/java/com/lottie4j/fxplayer/renderer/layer/EffectsRenderer.java:301-309`
  — `applyBlurEffect` is the *no-bounds* branch the cascade does **not** replace.
- `lottie4j/.prompts/done/Fix-rendering-json-interactive-mood.md` — predecessor plan with the
  measured before/after table and the explicit "follow-up needed" handoff in the closing
  section.
- `lottie4j/.prompts/done/Fix-renderer-gradient-fill-channels.md` — companion plan that fixed
  the *stop-parsing* side of `gf` channel skew; the *bounds* side is what this plan tackles.

## Verification

Final acceptance (after all applicable steps):

1. `mvn -pl core install -DskipTests -q && mvn -pl fxplayer test -q` — green.
2. `calibrate: interactive_mood_selector_ui` — `interactive_mood_selector_ui.json` reports
   average ≥ 99.5 % and the floor entry has been removed.
3. `mvn -pl fxfileviewer test -Dtest=CompareFxViewWithWebViewTest#compareFxAndJsRenderingFullSize`
   — full suite green (0 failures, 0 errors) with the eight remaining floors untouched.
4. The per-channel signature on frame 0 of `interactive_mood_selector_ui` reads
   R / G / B all ≥ 99.0 (currently 94.65 / 95.97 / 98.60).
5. The heatmap PNG for `interactive_mood_selector_ui` no longer shows warm-colour edges around
   the emoji bodies. Compare visually against the predecessor's reference heatmap if archived.

## Outcome

**Headline:** The two leading hypotheses from the predecessor plan were investigated and
both ruled out. The residual ~3.9 pp gap on `interactive_mood_selector_ui.json` remains, so
the `PER_FILE_FLOOR_OVERRIDE` entry stays at **95.4**. Step 1 still landed as defensive
correctness for the other files that *do* use `gf` fills.

### Measured before / after on `interactive_mood_selector_ui`
| metric                    | before | after Step 1 | after Step 2 (then reverted) |
|---------------------------|--------|--------------|------------------------------|
| average                   | 95.58 %| 95.58 %      | 91.39 %                      |
| minimum                   | 93.09 %| 93.09 %      | 90.95 %                      |
| frame 0 R / G / B         | 94.65 / 95.97 / 98.60 | 94.65 / 95.98 / 98.60 | 86.37 / 89.97 / 97.46 |

### What landed

#### Step 1 — full cubic-bezier extent for gradient bounds (`PathRenderer`)
- `PathRenderer.calculateBounds` renamed to `calculateVertexBounds` and kept as the
  fallback path when tangent data is missing.
- New `calculateGeometryBounds(vertices, tangentsIn, tangentsOut, closed)` walks every
  cubic segment, computes the analytical extrema of `B'(t) = 0` (a quadratic in `t`) per
  axis, and merges them with the segment endpoints. Closed paths additionally walk the
  implicit closing segment.
- New `addCubicExtrema(v0, c1, c2, v3, acc, xAxis)` does the per-axis quadratic solve,
  with the derivation verified by hand:
  - `a = -v0 + 3 c1 - 3 c2 + v3`
  - `b = 2 (v0 - 2 c1 + c2)`
  - `c = c1 - v0`
- Ellipse and Rectangle renderers already pass their painted bounding box — added a
  one-line Javadoc cross-reference so the next reader doesn't repeat this investigation.

##### `PathRendererBoundsTest` (new)
Six cases, all running without a JavaFX toolkit:
- empty vertices → null
- single straight segment (zero tangents) ⇒ matches vertex bounds exactly
- symmetric four-segment circle with κ = 0.5522847… handle length ⇒ bounds match the
  inscribed bounding square to 1e-6
- two-vertex open path with `outTangent[0]` pushing Y above the vertex hull ⇒ analytical
  extremum at `t = 1/3` is reproduced
- short tangent list ⇒ falls back to vertex bounds
- closed-flag toggle ⇒ closing-segment bulge only contributes when `closed = true`

#### Step 2 — Cascaded `GaussianBlur(60)` (REVERTED)
A cascade chained via `Effect.setInput` was implemented and tested per the plan's
specification (quadrature identity, per-pass cap at 63 px, peak-red invariant). It
**regressed** `interactive_mood_selector_ui` from 95.58 % to **91.39 %** (frame 0 R/G
went 94.65/95.97 → 86.37/89.97, i.e. back to the pre-fix baseline).

Root cause of the regression: the cascade's target σ ≈ `passRadius` is roughly twice
the σ ≈ `passRadius/2` that `BoxBlur(passRadius, passRadius, 3)` was effectively
producing. The thorvg reference matches the box approximation closer than a true
`σ = passRadius` Gaussian, so making the kernel "more Gaussian-shaped" actually moved
the FX output *away* from the reference. Both the production cascade code and its tests
were removed.

#### Step 3 — Precomp `o = 70` opacity (NOT ACTIONED)
After Step 2 was reverted, Step 3 remained speculative. A code read of
`LottiePlayer.renderLayer` → `EffectsRenderer.renderWithBoundedBlur` →
`renderLayerInternal` confirmed that for the bounded-blur path the layer opacity is
applied **inside** the offscreen render via `gc.setGlobalAlpha(opacity)` *before* the
blur snapshot is captured. The blur effect is set on the offscreen `GraphicsContext`
before any shape is drawn, so JavaFX applies it per-draw with the lower alpha already
in effect. Since blur and scalar alpha multiplication commute (`blur(α · S) = α ·
blur(S)`), the order issue the plan worried about does not exist for this code path.
No code change was made for Step 3.

### What did NOT land (and why)

- **No removal of the `Map.entry("json/interactive_mood_selector_ui.json", 95.4)`
  floor** — the file did not reach 99.5 %, so per the plan's own gating the entry
  remains in `PER_FILE_FLOOR_OVERRIDE`. The Javadoc comment in
  `CompareFxViewWithWebViewTest` was updated to document the negative results from
  Steps 1 and 2 so the next AI loop does not retry the same hypotheses without new
  evidence.

### Suite state after the change (`calibrate: clean + full suite to log`, 24 files)
- 24 tests, **0 failures, 0 errors**.
- `interactive_mood_selector_ui` passes at 95.54 % (floor 95.40 %). Minor frame-by-frame
  variation around the previous 95.58 measurement is within the normal SKIP-frame
  noise; well above floor.
- All eight pre-existing floor entries hold:
  - `angry_bird` 98.32 (floor 98.20)
  - `animated_background_patterns` 99.33 (floor 99.20)
  - `face-peeking` 98.88 (floor 98.60)
  - `java_duke_flip` 95.84 (floor 95.70)
  - `java_duke_slidein` 98.98 (floor 98.80)
  - `lottie_lego` 98.15 (floor 98.00)
  - `sandy_loading` 99.48 (floor 99.30)
  - `dot/demo-1.lottie` 99.46 (floor 99.30)
- Defensively important `gf`-using files not on floors all pass cleanly:
  - `isometric_data_analysis` 99.69 % (heavy `gf` usage)
  - `face-exhaling` 99.73 %
  - `java_duke_fadein` 99.98 %

### Recommendation for the next follow-up

The constant ~4 pp warm-channel skew on the emoji bodies that survives all three
investigated mechanisms is most likely **per-emoji geometry mismatch** rather than
gradient, blur or opacity. A productive next plan would:

1. Save the diff-PNG for frame 0 (which the test already does into
   `target/test-output/.../frame_0_*.png`).
2. Sample a single emoji body's silhouette in both FX and reference renders.
3. Compare anchor positions, layer-precomp scale, and any animator-driven path
   transforms to find the systematic geometric offset.

That investigation is out of scope for this follow-up; it is documented here so the
work is not lost.
