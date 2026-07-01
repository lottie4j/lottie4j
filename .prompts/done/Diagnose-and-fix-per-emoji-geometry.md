id: c4e3a788-13ba-4e22-8242-f30510866200
sessionId: 080e7f2a-1776-4cde-a5dc-5d9ff46f9e3b
date: '2026-07-01T06:34:31.574Z'
label: Diagnose and fix per-emoji geometry mismatch on interactive_mood_selector_ui
---
# Close residual gap on `interactive_mood_selector_ui.json` — gradient rendering of light-blue background elements

This is the next follow-up on `interactive_mood_selector_ui.json`. Two predecessor plans
(`Fix-rendering-json-interactive-mood.md` then `Follow-up--Close-residual-gap-on.md`) lifted
the file from **92.40 % → 95.58 %** and then ruled out three structural hypotheses (gradient-
fill bounds on emoji bodies, Gaussian-blur kernel shape, precomp opacity ordering).

**Pivot from the previous plan draft.** The last predecessor recommended per-emoji body
*geometry* investigation. Direct visual comparison of FX vs. the dotlottie-wc reference at
frame 0 shows something different: the dominant visible difference is in the **gradient-like
light-blue background elements**, not the emoji bodies. In FX the blue is brighter and the
gradient transitions are harder; in the reference the same regions are softer and more evenly
graded. There is also a smaller, secondary difference in the **text** rendering. Both point
at the *gradient / paint* pipeline, not at layer geometry.

## File
`lottie4j/fxfileviewer/src/test/resources/json/interactive_mood_selector_ui.json`

## Goal
Close the residual gap on `interactive_mood_selector_ui.json` from **95.54 %** to
**≥ 99.5 %** so the `PER_FILE_FLOOR_OVERRIDE` entry can be removed, **without** regressing
any of the 14 files currently passing the 99.5 % target or any of the 8 other files that
carry their own legacy floors.

## Reproduce
```
mvn -pl fxfileviewer test \
    -Dtest=CompareFxViewWithWebViewTest#compareFxAndJsRenderingFullSize \
    -Dlottie.file=json/interactive_mood_selector_ui.json
```
or via the VS Code task `calibrate: interactive_mood_selector_ui`
(`lottie4j/.vscode/tasks.json:73-83`).

After the run, three-panel diff PNGs (`FX | reference | heatmap`) land in
`fxfileviewer/target/test-output/json/interactive_mood_selector_ui-webview_scale_1.0/`,
named `frame_<n>_<similarity>.png`. Pre-generated reference PNGs live at
`fxfileviewer/src/test/resources/json/interactive_mood_selector_ui-webview/frame_<n>.png`.

## Observed gap going in
| metric                 | value (current)                                          |
|------------------------|----------------------------------------------------------|
| average                | **95.54 %** (floor 95.40)                                |
| minimum                | ~93.08 %                                                 |
| frame 0 R / G / B      | **94.65 / 95.97 / 98.60**                                |
| dominant visible diff  | light-blue "gradient" background — FX brighter & harder, reference softer |
| secondary visible diff | minor text differences                                   |
| consistency            | flat 93–96 % across all 659 frames — structural, not per-frame |

The per-channel signature `R < G < B` with **R about 4 pp short and B nearly at target**
is exactly what a *too-saturated* blue would produce: pixels that should be light-mid blue
(equal contributions from R and G alongside B) come out as pure saturated blue (only B
present), which drives R and G down while leaving B untouched. That is consistent with the
"brighter, harder" visual observation.

## What we now know about the file (contradicts predecessor's claims)

Two facts that were not correctly reflected in the predecessor plan:

1. **The file DOES have `"ty":"gf"` gradient fills** (orange, red, purple stops visible in
   the JSON — search hits: `[0.8,0.3216,0]`, `[0.7569,0,0]`, `[0.3647,0,0.5176]`, and
   others). Predecessor Step 1's bezier-extent bounds fix *does* run on these shapes, but
   the "no `gf`" claim in the predecessor's outcome section was wrong; the change either
   had no effect on these particular gradients or the gradient bounds are only part of
   the picture.
2. **The file has `"ty":"gs"` gradient strokes** on shape groups named "Gradient Stroke 1"
   with end points around `(275.8, 17)` (i.e. large layer-space coordinates). These have
   **never been investigated** in the predecessor plans.

## Ruled-out hypotheses (do not retry without new evidence)

Documented negative results from the predecessor, recorded in the Javadoc of
`PER_FILE_FLOOR_OVERRIDE` (`CompareFxViewWithWebViewTest.java:89-117`):

1. **Cascaded `GaussianBlur(60)` instead of `BoxBlur(r, r, 3)`** — regressed 95.58 → 91.39.
   The `BoxBlur` selection in `EffectsRenderer.applyBlurEffect:301-309` is **correct** —
   do not change it.
2. **Layer opacity (`o = 70`) compositing order** — verified by code-read that the bounded
   blur path applies `gc.setGlobalAlpha(opacity)` inside the offscreen render before the
   blur snapshot; since `blur(α · S) = α · blur(S)`, ordering does not matter.
3. **Gradient bounds fix for `PathRenderer`** did land, but did *not* close this file's
   gap. Do not re-litigate the bezier-extent computation itself.

## Rendering-code discoveries relevant to this plan

Grepping the codebase for the gradient render path surfaces four points worth flagging
up-front so the plan can proceed without re-doing the exploration:

1. **`GradientStroke` is only handled by `RectangleRenderer`.** Search of
   `lottie4j/fxplayer/src/main/java/com/lottie4j/fxplayer/renderer/**` shows a single
   consumer of `GradientStrokeStyle` (in `RectangleRenderer.java:114-124`). `PathRenderer`,
   `EllipseRenderer`, and `PolystarRenderer` never construct a `GradientStrokeStyle`.
   `PathStrokeRenderer.getStrokeStyle` at
   `lottie4j/fxplayer/src/main/java/com/lottie4j/fxplayer/renderer/shape/PathStrokeRenderer.java:172-183`
   filters for `Stroke` only, i.e. any `GradientStroke` item on a path group is silently
   dropped. `ShapeGroupRenderer.java:874` only inspects `GradientStroke` for max-stroke-
   width offscreen sizing — never to paint.
2. **`GradientFillStyle` and `GradientStrokeStyle` share the "proportional coordinates"
   pattern.** For linear gradients with valid shape bounds they compute
   `propStart = (startX - shapeX) / shapeWidth` etc. When start/end lie outside the
   shape's bounding box (typical when a Lottie designer places the gradient's `s`/`e`
   anchors along an emoji silhouette or well outside a small element sitting inside a
   large canvas), these proportional coordinates go outside `[0, 1]` and JavaFX's
   `CycleMethod.NO_CYCLE` clamps the far end to a solid colour — producing "harder"
   gradient edges. See
   `lottie4j/fxplayer/src/main/java/com/lottie4j/fxplayer/element/GradientFillStyle.java:126-144`
   and `GradientStrokeStyle.java:97-115`.
3. **All JavaFX gradient interpolation is sRGB.** `LinearGradient` / `RadialGradient`
   interpolate stops in gamma-encoded sRGB space. thorvg / dotlottie-wc — the reference
   engine — typically interpolate in **linear RGB** for perceptually smoother midpoints
   ("softer" transitions). This is a documented visual difference between raster engines
   and there is no direct JavaFX knob to change it.
4. **`GradientStopParser` correctly merges alpha and colour tracks**
   (`lottie4j/fxplayer/src/main/java/com/lottie4j/fxplayer/element/GradientStopParser.java`);
   this is not the bug and does not need to be changed here.

## Plan — diagnose first, then a targeted fix

The predecessor's Step 2 attempt (Gaussian cascade) regressed the file when applied
without measurement. Follow the same rule here: **measure first, then commit to a fix**.

### Step 1 — Locate the light-blue background element and characterise the gradient

**Goal.** Identify which JSON shape / layer produces the light-blue background the user
sees, and confirm the visual difference is a gradient-rendering artifact (not a blur or
geometry artifact).

**Tasks.**

1. Run `calibrate: interactive_mood_selector_ui` to regenerate diff PNGs. Open
   `fxfileviewer/target/test-output/json/interactive_mood_selector_ui-webview_scale_1.0/frame_0_*.png`.
2. Identify the light-blue region: sample a mid-gradient pixel in the reference panel
   (right) and in the FX panel (left). Record the RGB values. This tells us:
   - the target colour (from the reference)
   - the FX-produced colour
   - the delta on R, G, B channels
3. Locate the responsible layer(s) in the JSON. Since the file cannot be read whole (634 KB
   exceeds the 256 KB reader cap), use targeted `searchInWorkspace` queries:
   - Search for the RGB triple of a light-blue stop (e.g. `0.153,0.655,0.976` or a value
     near what was sampled) — the gradient's stop array will contain those numbers.
   - Follow the search hit to the containing shape group. Note down: layer name (`nm`),
     shape type (`ty`), gradient type (`gf` linear/radial, or `gs`), stop array, `s`/`e`
     anchor points, and any parent effect (`ef`) including Gaussian blur.
4. Cross-reference the containing shape with the **discoveries** above:
   - If it is a `gs` on a path/ellipse/polystar → sub-hypothesis (X): missing renderer.
   - If it is a `gf` linear with `s`/`e` outside the shape bbox → sub-hypothesis (Y):
     over-extended proportional coordinates.
   - If it is a `gf` radial with a very wide radius → sub-hypothesis (Y) variant: same
     clamp mechanic on the radial's `[0..1]` distance.
   - If neither of the above and the FX vs. reference midpoint colours differ in the
     characteristic sRGB-vs-linear way (midpoint darker/more saturated in FX, brighter
     in reference) → sub-hypothesis (Z): sRGB vs linear-RGB interpolation.
5. Do the same for at least one text glyph the user flagged — either the `fh/fs/fb`
   animator-driven text (already investigated) is still producing a slightly-off colour,
   or there is a font-metrics / anti-aliasing difference (search `TextRenderer` for
   font size resolution and letter tracking).

Record findings in a `## Diagnosis` section appended to this plan before moving on
(`editTaskContext`).

**Verification gate for Step 1.**

The diagnosis section must name **the specific shape/layer** and **at least one measured
RGB delta**. If it does not, stop and re-run with different pixel samples. Do not proceed
to Step 2 speculatively.

### Step 2 — Fix, driven by the diagnosis

Three variants; Step 1's diagnosis selects exactly one (or, if evidence is layered, an
ordered combination). **Ship one at a time and re-measure between each** — this is the
lesson from the predecessor's Gaussian-cascade regression.

#### Step 2X — Wire `GradientStroke` into `PathRenderer` (and Ellipse/Polystar)

Only if Step 1 found the light-blue element is drawn by a `gs` gradient stroke on a shape
that is **not** a rectangle.

**Change set.**

- `lottie4j/fxplayer/src/main/java/com/lottie4j/fxplayer/renderer/shape/PathStrokeRenderer.java`
  - Add a companion to `getStrokeStyle` that returns
    `Optional<GradientStrokeStyle>`. Model after
    `RectangleRenderer.getGradientStrokeStyle` at
    `lottie4j/fxplayer/src/main/java/com/lottie4j/fxplayer/renderer/shape/RectangleRenderer.java:198-208`.
  - In `renderStroke`, if the plain stroke lookup returns empty, try the gradient
    variant. If present:
    - Compute the path's bounds using the same `calculateGeometryBounds` helper that
      `PathRenderer` already exposes for gradient *fills* (`PathRenderer.java:73`) —
      extract it to a `package-private static` method if needed so both renderers can
      call it without duplicating the bezier-extrema math.
    - Set `gc.setStroke(gradientStrokeStyle.getPaint(frame, bounds[0], bounds[1],
      bounds[2], bounds[3]))`.
    - Set `gc.setLineWidth(compensatedWidth)` from the gradient stroke's own width.
    - Preserve trim-path support: if trim paths are present, `renderTrimmedPath` must
      also use the gradient paint (currently `renderTrimmedPath` re-reads the plain
      stroke colour — thread the gradient variant through).
    - Apply the gradient stroke's global alpha via `gc.setGlobalAlpha(opacity)` around
      the stroke call (same pattern as `RectangleRenderer.java:122`).
- `lottie4j/fxplayer/src/main/java/com/lottie4j/fxplayer/renderer/shape/EllipseRenderer.java`
  - Ellipses already have a `getGradientFillStyle` (documented Javadoc note about
    already-correct bounds). Add the parallel `getGradientStrokeStyle` and stroke branch
    next to the fill branch. Use the existing `(renderX, renderY, width, height)` bounds
    (same as the fill path).
- `lottie4j/fxplayer/src/main/java/com/lottie4j/fxplayer/renderer/shape/PolystarRenderer.java`
  - Mirror the pattern. Polystars compute their own bezier hull for outline rendering;
    reuse or compute geometry bounds similarly to `PathRenderer`.

**Unit test coverage.**

- `lottie4j/fxplayer/src/test/java/com/lottie4j/fxplayer/renderer/shape/PathStrokeRendererGradientTest.java` (new)
  - Renders a simple square-shaped path with a `GradientStroke` (linear, two stops:
    red→blue). Snapshots via JavaFX's headless canvas (an existing pattern —
    `PathRendererBoundsTest` uses non-JavaFX helpers, so this test will need a
    `PlatformImpl.startup` setup or should live under a `@EnableJavaFX`-style helper if
    one exists in `fxplayer`).
  - Asserts a mid-side pixel of the stroke shows a mixed colour (roughly halfway between
    the stops), not solid red or solid blue.
  - A second case: same shape with a `Stroke` (plain colour) still renders correctly
    (regression guard for the routing change).
- `PathStrokeRendererGradientTest.strokeUsesPathGeometryBounds()` — assert that a
  gradient whose `s`/`e` anchors sit *outside* the path's vertex hull still produces a
  visible gradient (not solid end-stop) at the mid-side pixel. This mirrors the
  bezier-extent fix's intent for the *fill* path.

#### Step 2Y — Extend the proportional-coordinate clamp / cycle policy

Only if Step 1 found the light-blue element is drawn by a `gf` (or `gs` on a rectangle)
whose `s`/`e` sit well outside the shape's bounding box, producing "hard" clipped
end-stops in FX.

**Root-cause detail.** JavaFX `LinearGradient` uses `CycleMethod.NO_CYCLE` (per the
existing code), so any `t` outside `[0, 1]` in the proportional space is drawn as the
end-stop's flat colour. If Lottie's designer placed `s`/`e` anchors far outside the
painted region on purpose (which is common — e.g. gradient axis longer than the shape so
the visible portion only spans, say, `[0.2, 0.6]` of the gradient), FX correctly clips
the outside but the *inside* segment is also computed differently than thorvg because:

- thorvg maps `s`/`e` to *layer-local coordinates* (absolute pixel units), and any point
  in the shape that projects outside `[s..e]` is painted with the extension colour —
  this matches JavaFX behaviour for absolute coordinates.
- FX uses **proportional** coordinates with the shape's bounding box as the reference
  frame (line 128 of `GradientFillStyle`). When the gradient's designer intent is
  "gradient axis is in layer space, not shape space", the FX proportional transformation
  effectively re-projects the axis onto the shape bounding box, distorting its span.

**Fix sketch.**

- Add a static helper to `GradientFillStyle` and `GradientStrokeStyle` that decides
  whether to use proportional or absolute mode based on whether `s`/`e` lie inside
  `[shapeX, shapeX + shapeWidth] × [shapeY, shapeY + shapeHeight]`. Outside → use
  absolute layer-space coordinates (the existing `hasShapeBounds = false` branch); this
  produces a paint that maps `s`/`e` to actual pixel coordinates rather than remapping
  them onto the bounding box.
- Alternatively (safer, more surgical): keep proportional mode but change the
  denominator. Currently `(startX - shapeX) / shapeWidth` — replace `shapeWidth` with
  the actual span `endX - startX` for linear gradients, so the resulting proportional
  span is always `[0, 1]`. But this only works if we then also translate the shape's
  bounding box into the same proportional frame — non-trivial.
- The absolute-mode fallback is much simpler and matches thorvg's model when the
  gradient axis is layer-space anyway. Guard with a threshold — only switch to absolute
  if `s`/`e` lies more than, say, `2 · shapeWidth` outside the bbox — so tightly-
  contained gradients still use proportional (avoiding any pixel-vs-proportional
  transform mismatch across scaled precomps).
- Unit tests: extend `GradientFillStyleTest` (or create `GradientFillStyleBoundsTest`)
  with three cases:
  - `s`/`e` inside bbox → proportional mode chosen (existing behaviour).
  - `s`/`e` far outside bbox → absolute mode chosen; paint's `getStartX()` /
    `getEndX()` match the raw Lottie coordinates.
  - `s`/`e` at bbox corner → proportional mode chosen (edge case).

#### Step 2Z — Approximate linear-RGB interpolation via stop densification

Only if Step 1 found no obvious `gs` or bounds problem and the FX vs. reference midpoint
colours differ in the characteristic sRGB-vs-linear pattern (FX midpoint too saturated /
too dark, reference softer).

**Rationale.** JavaFX has no API for choosing the interpolation colour space; both
linear and radial gradients interpolate in sRGB. We can approximate linear-RGB
interpolation *externally* by inserting extra stops between each pair of designer stops,
computed by:

1. Convert designer stops from sRGB to linear space (`c_lin = c_srgb^2.2` per channel,
   or the exact sRGB transfer function).
2. Sample the linear-space linear interpolation at, say, 5 intermediate offsets between
   each pair.
3. Convert each sampled colour back to sRGB for the JavaFX `Stop` array.
4. Pass the densified stops to the existing `LinearGradient` / `RadialGradient`
   constructor. JavaFX's per-pair sRGB interpolation between two very close stops
   approximates a linear-space interpolation between the far stops.

**Change set.**

- `lottie4j/fxplayer/src/main/java/com/lottie4j/fxplayer/element/GradientStopParser.java`
  - New public method
    `List<Stop> densifyForLinearRgbApproximation(List<Stop> designerStops, int subdivisions)`.
    Callable from `GradientFillStyle.getPaint(...)` and `GradientStrokeStyle.getPaint(...)`
    just before constructing the paint.
  - `subdivisions = 4` is a reasonable starting point; expose the constant so tests can
    dial it up or down.
- Add sRGB↔linear conversion helpers (private static in `GradientStopParser` or a new
  `ColorSpace` utility class). Use the exact piecewise sRGB transfer function, not the
  γ = 2.2 approximation — the exact function is what thorvg / most browsers use.
- Unit tests in `GradientStopParserTest`:
  - Two-stop `red → white` gradient, densify by 4: midpoint stop should be closer to
    `Color.color(1.0, 0.735, 0.735)` (linear midpoint converted back to sRGB) than
    to the sRGB midpoint `Color.color(1.0, 0.5, 0.5)`. Numerical check.
  - Densifying a solid-colour gradient (both stops the same colour) is a no-op —
    stops are all identical.
  - Densifying an already-densified gradient is idempotent to within `1e-6` on each
    channel.

**Risk to other files.** Every file with a gradient exercises this code path. Regression
sentinels are critical: run `calibrate: regression sentinels` and inspect each of the 8
floor-overridden files plus the gradient-heavy files `isometric_data_analysis`,
`angry_bird`, `java_duke_*`, `face-exhaling`, `face-peeking`, `sandy_loading`.

### Step 3 — Text differences (secondary)

Only after Steps 2X/2Y/2Z land the gradient fix. Reserve budget so the plan doesn't
scope-creep.

**Tasks.**

- Sample a text pixel from the frame 0 diff in both panels and record R/G/B. Compare
  against `TextRenderer.getAnimatorFillColor` (post-predecessor's HSL fix).
- If colour is correct but position differs by a fractional pixel: font-metrics / letter-
  spacing issue. `TextRenderer` uses `javafx.scene.text.Font` — thorvg uses its own
  glyph renderer. Some difference is expected; verify it is not larger than the SSIM
  window (8 px).
- If colour differs beyond noise: revisit the animator HSL fix under fresh scrutiny —
  the predecessor's decision to use HSL instead of HSB may have been correct
  mathematically but off from thorvg's specific colourimetric implementation.

If the text delta is small enough that its contribution to the SSIM gap is under 0.1 pp,
document that in `## Outcome` and skip further text changes.

### Step 4 — Cleanup

- If `calibrate: interactive_mood_selector_ui` reports average ≥ 99.5 %:
  - Remove `Map.entry("json/interactive_mood_selector_ui.json", 95.4)` and the
    accompanying Javadoc bullet and inline comment in
    `lottie4j/fxfileviewer/src/test/java/com/lottie4j/fxfileviewer/CompareFxViewWithWebViewTest.java:89-117`.
  - Append `## Outcome` (measured before / after per channel, which sub-step landed the
    lift, negative results for the other sub-steps) and move this plan into
    `lottie4j/.prompts/done/`.
- If average does not cross 99.5 %: document what was measured, do not remove the floor,
  and update the `PER_FILE_FLOOR_OVERRIDE` Javadoc with the new negative findings so a
  future loop does not repeat them.

## Reference Examples

- `lottie4j/fxplayer/src/main/java/com/lottie4j/fxplayer/element/GradientFillStyle.java:126-158`
  — proportional-vs-absolute branching for linear gradients; Step 2Y target.
- `lottie4j/fxplayer/src/main/java/com/lottie4j/fxplayer/element/GradientStrokeStyle.java:97-115`
  — parallel linear-gradient handling for `gs`; will need the same Step 2Y logic.
- `lottie4j/fxplayer/src/main/java/com/lottie4j/fxplayer/renderer/shape/RectangleRenderer.java:114-124`
  — the only renderer that currently invokes `GradientStrokeStyle`. Step 2X copies its
  pattern into path/ellipse/polystar renderers.
- `lottie4j/fxplayer/src/main/java/com/lottie4j/fxplayer/renderer/shape/PathStrokeRenderer.java:172-183`
  — `getStrokeStyle` filters for `Stroke` only, silently dropping `GradientStroke`.
- `lottie4j/fxplayer/src/main/java/com/lottie4j/fxplayer/element/GradientStopParser.java`
  — the shared stop-parser; Step 2Z adds `densifyForLinearRgbApproximation` here.
- `lottie4j/fxplayer/src/main/java/com/lottie4j/fxplayer/renderer/shape/PathRenderer.java:73`
  — `calculateGeometryBounds` (predecessor's bezier-extent fix). Step 2X reuses this for
  gradient strokes on paths.
- `lottie4j/.prompts/done/Fix-rendering-json-interactive-mood.md` — original 92.40 → 95.58
  plan (Gaussian-blur cap + HSL text animator fix).
- `lottie4j/.prompts/done/Follow-up--Close-residual-gap-on.md` — first follow-up; ruled
  out gradient-fill *bounds*, Gaussian-cascade, and opacity ordering.
- `lottie4j/.prompts/done/Fix-renderer-gradient-fill-channels.md` — earlier plan that
  fixed `gf` colour-channel skew via `GradientStopParser`. Related but distinct from the
  gradient-*rendering* issues this plan targets.

## Verification

Final acceptance:

1. `test: fxplayer unit tests` — green, including the new tests added in Step 2X/2Y/2Z.
2. `calibrate: interactive_mood_selector_ui` — average **≥ 99.5 %** and frame 0
   per-channel R/G/B all ≥ 99.0 (currently 94.65 / 95.97 / 98.60).
3. `calibrate: regression sentinels` — the 8 floor-overridden files still meet their
   existing floors, and the gradient-heavy files (`isometric_data_analysis`,
   `angry_bird`, `java_duke_fadein`, `java_duke_slidein`, `java_duke_flip`,
   `face-exhaling`, `face-peeking`, `sandy_loading`) do not regress.
4. `calibrate: clean + full suite to log` — 24 tests, 0 failures, 0 errors.
5. Visually confirm the light-blue background in the frame 0 diff PNG looks like the
   reference: softer transitions, mid-range colours instead of over-saturated pure blue.
6. The `Map.entry("json/interactive_mood_selector_ui.json", 95.4)` line is removed from
   `CompareFxViewWithWebViewTest.PER_FILE_FLOOR_OVERRIDE`.

## Out of scope

- The pre-existing 8 floor entries for unrelated files — each has its own follow-up plan.
- The `_HALF` variant override map — half-size scaling is exercised by a different test.
- The per-emoji-body *geometry* investigation the previous predecessor recommended. That
  hypothesis is superseded by direct visual evidence pointing at gradient rendering as
  the dominant contributor; revisit only if the gradient fix under-delivers and the
  residual matches an emoji-body-geometry signature (asymmetric warm-colour rim around
  every body, per-channel `R < G` skew persisting after gradient rework).
