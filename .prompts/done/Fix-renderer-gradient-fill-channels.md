# Fix renderer: gradient fill (gf) per-channel parity

## Scope
Cross-cutting renderer plan affecting files with `"ty": "gf"` (gradient fill, linear
`t:1` or radial `t:2`). Affected files show **systematically biased per-channel
similarity** — one of R/G/B drops noticeably below the other two on every frame that
exercises a gradient.

## Affected files
| File                          | observed bias                  | typical scores                   |
|-------------------------------|--------------------------------|----------------------------------|
| json/face-peeking.json        | **B drops most**, R highest    | R 97.95 / G 94.46 / B 77.92 @ f231 |
| json/face-exhaling.json       | **B drops most**, R highest    | R 96.91 / G 94.31 / B 76.73 @ f150 |
| dot/demo-1.lottie             | **G drops most**, R/B similar  | R 99.66 / G 98.79 / B 99.33 @ f60 |
| json/java_duke_fadein.json    | inferred (skipped in last run) | 78.14 % single frame             |
| json/java_duke_slidein.json   | inferred (skipped in last run) | 78.14 % single frame             |

The fact that *different* files show *different* dominant channels (B-bias in
face-peeking/exhaling, G-bias in dot/demo-1) rules out a constant offset and points
at **per-gradient stop interpretation**: each gradient's specific stop colours expose
the bug differently.

## Reproduce
```
mvn -pl fxfileviewer test \
    -Dtest=CompareFxViewWithWebViewTest#compareFxAndJsRenderingFullSize \
    -Dlottie.file=json/face-peeking.json
```

## Diagnosis
Lottie packs gradient stops compactly in `g.k.k` (or `g.k.k[*].s` if animated):

- For `n` colour stops: `[pos1, r1, g1, b1, pos2, r2, g2, b2, …, posN, rN, gN, bN]`
- For `m` alpha stops (optional, appended): `[pos1, a1, pos2, a2, …, posM, aM]`

The total length is `4n + 2m`. `g.p` (number of stops) tells the parser how many
colour stops to expect. Failure modes:

1. Parser reads `4 · gp` floats as colour stops but `gp` actually counts colour+alpha
   pairs — drops alpha or shifts colours by one slot.
2. Parser handles only the colour stops, ignoring the alpha tail entirely — visible
   only when gradient has an alpha stop list.
3. Parser swaps r/g/b component order (e.g. reads `b,g,r,a` like Android's `ARGB_8888`
   bytes instead of normalised `[r,g,b]` in `[0..1]`).
4. Stop positions are in `[0..1]` but the parser treats them as `[0..100]` — first
   stop dominates the entire gradient.

Lottie-web reference: `helpers/properties/GradientProperty.js` and
`utils/shapes/StrokeElement.js`.

## Proposed fix
- `lottie4j/fxplayer/src/main/java/com/lottie4j/fxplayer/renderer/style/FillRenderer.java`
  — extract the stop parsing into a separate function/class with these unit tests:
  - 2 colour stops, no alpha tail
  - 3 colour stops, no alpha tail
  - 2 colour stops, 2 alpha stops (typical `face-peeking`)
  - 3 colour stops, 3 alpha stops
  - radial vs linear (`t:1` vs `t:2`)
  Compare against expected `javafx.scene.paint.LinearGradient` /
  `javafx.scene.paint.RadialGradient` stop lists.

- If the model is responsible for the `g.k` array shape, also fix in
  `lottie4j/core/src/main/java/com/lottie4j/core/model/shape/GradientFill.java`
  (or whatever holds the gradient model).

- Add a renderer test that loads
  `lottie4j/fxfileviewer/src/test/resources/json/java_duke_still.json` (single-frame
  Duke variant with the same `gf`) and snapshots a few stop positions.

## Validation
After applying the fix:
1. Re-run single-file tests for each affected file.
2. Confirm the per-channel skew (B-low / G-low / R-low) is eliminated on motion
   frames.
3. Remove `json/face-peeking.json`, `json/java_duke_fadein.json`,
   `json/java_duke_slidein.json`, `dot/demo-1.lottie` from `PER_FILE_FLOOR_OVERRIDE`
   if their averages now exceed 99.5 %.
4. Re-run the full suite.

## See also
- `Fix-rendering-face-peeking.md`
- `Fix-rendering-face-exhaling.md`
- `Fix-rendering-dot-demo-1.md`
- `Fix-rendering-java_duke_fadein.md`
- `Fix-rendering-java_duke_slidein.md`
- `Follow-up--Close-residual-gap-on.md` — resolved the *bounds* part of the
  gradient-fill story (`PathRenderer.calculateGeometryBounds` now uses the full
  cubic-bezier extent, including tangent reach, rather than the under-sized vertex
  hull). All currently-floored `gf` files (`face-exhaling`, `java_duke_*`,
  `demo-1.lottie`, `isometric_data_analysis`) verified non-regressing under the
  change in the regression sentinels.
