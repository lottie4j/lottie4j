# Fix rendering: json/face-peeking.json

## File
`lottie4j/fxfileviewer/src/test/resources/json/face-peeking.json`

## Reproduce
```
mvn -pl fxfileviewer test \
    -Dtest=CompareFxViewWithWebViewTest#compareFxAndJsRenderingFullSize \
    -Dlottie.file=json/face-peeking.json
```

## Observed gap (2026-06-29 calibration)
- average: 98.55 %
- minimum: 93.49 % on frame 80 (2nd motion-burst)
- floor:   98.50 % (override)
- target:  99.50 %
- gap:     +0.95 % below target

### Delta vs. 2026-06-25 baseline
- average: 98.72 → **98.55** (-0.17 pp — basically unchanged)
- minimum: 90.11 → **93.49** (+3.38 pp — out-point cliff was the main earlier dip)
- frame 231 (the out-point) no longer dips; resolved by
  `Fix-renderer-outpoint-frame.md` (done).
- The Rec. 601 luma matte fix (`Fix-renderer-luma-matte-rec601.md`, done) and the
  gradient-fill fix (`Fix-renderer-gradient-fill-channels.md`, done) have both landed
  but neither actually applies to this file — see "Important: matte type
  misidentification" below.

## Failing frames
The animation has 232 frames. Steady-state segments (frames 0–39, 56–119, 146–195,
210–230) score 99.5–99.6 %. Failing frames cluster around **four motion bursts**.

| frame    | overall     | R     | G     | B     | notes                              |
|----------|-------------|-------|-------|-------|------------------------------------|
| 41–55    | 93.79–99.7  | 97.06 | 93.23 | 91.08 | 1st peek (dip → 93.79 @ 45)        |
| 72–87    | 93.49–99.7  | 96.82 | 92.78 | 90.86 | 2nd peek (dip → 93.49 @ 80; min)   |
| 120–145  | 93.74–99.8  | 96.99 | 93.08 | 91.14 | 3rd peek (dip → 93.74 @ 122)       |
| 196–215  | 96.59–99.8  | 98.19 | 95.94 | 95.64 | 4th peek (dip → 96.59 @ 201)       |

Per-channel skew: **B drops the most, then G, then R stays high**.

## Important: matte type misidentification

Previous task descriptions and the `Fix-renderer-luma-matte-rec601.md` cross-cut plan
claimed that the `"tt": 2` on the `base shadow` layer (line 27620) was a **luma**
matte. This is **incorrect**.

Per the Lottie spec (and `MatteMode` enum at
`lottie4j/core/src/main/java/com/lottie4j/core/definition/MatteMode.java`):

| `tt` value | MatteMode         |
|-----------:|-------------------|
|        0   | `NORMAL`          |
|        1   | `ALPHA`           |
|        **2** | **`INVERTED_ALPHA`** |
|        3   | `LUMA`            |
|        4   | `INVERTED_LUMA`   |

So `tt:2` is **inverted alpha**, not luma. The Rec. 601 luma fix that already landed
does not run on this file at all — it improves only files with `tt:3`. (The previous
calibration delta improvements credited to that fix were actually from the
out-point-frame fix and the gradient-stop parse fix.)

`face-exhaling.json` shares the same situation (`"tt": 2` at line 12894).

## Diff evidence

Looking at the actual FX vs WebView pixel comparison at frame 44 (see PNG diff at
`target/test-output/json/face-peeking-webview_scale_1.0/frame_44_*.png`), the
**FX rendering shows the pure base-shadow color (235, 143, 0) covering large regions
of the face**, while the WebView rendering shows the face's yellow color blended
with a **scattered "stippled" pattern of shadow** distributed across the face's
shape.

| pixel        | FX (frame 44)      | WebView (frame 44) | interpretation                  |
|--------------|--------------------|--------------------|----------------------------------|
| (256, 512)   | (235, 143,  0)     | (255, 237, 109)    | FX shows pure shadow, REF yellow |
| (400, 400)   | (255, 229, 87)     | (235, 143,   0)    | FX shows yellow, REF shadow      |
| (512, 512)   | (239, 159, 14)     | (255, 225,  75)    | FX shows mostly shadow, REF yellow |
| (768, 512)   | (235, 143,  0)     | (255, 227,  82)    | FX shows pure shadow, REF yellow |

At **frame 0** (no motion) both FX and WebView agree (overall 99.57 %), and both
render the same scattered shadow stippling. The discrepancy only manifests during
the four motion bursts.

## Root cause hypothesis (refined)

The base shadow layer (`tt: 2`) has its matte source `base matte` (`td: 1`) parented
to the same `base normal` layer, offset by `(-2, -3)` in shape-local coordinates.
Both have an **identical** lemon-shaped path. With `INVERTED_ALPHA`, the result
should be:

- Shadow **invisible** wherever the matte source's alpha is opaque (most of the
  matte's path interior).
- Shadow **visible** wherever the matte source's alpha is zero — i.e. outside the
  matte's path but still inside the shadow's path (the thin offset crescent).

But WebView's reference shows the shadow visible **in a distributed pattern across
the face's interior**, not just a thin offset crescent. This means the matte
source's canvas is **not uniformly opaque inside its path** in WebView's rendering;
there are scattered transparent positions inside the path.

The matte source's fill is a radial gradient (5 colour stops at offsets `0.48`,
`0.691`, `0.902`, `0.966`, `1.0`, fully opaque, from yellow to orange). For this
gradient:

- JavaFX `RadialGradient` extends the **first stop's colour** (opaque yellow) to all
  positions before offset `0.48`, so the matte canvas is uniformly opaque inside
  the path → INVERTED_ALPHA → shadow invisible inside the entire path.
- WebView's canvas renderer **may not extend the first stop** the same way, leaving
  positions inside offset `0.48` (i.e. inside radius `0.48 × 56.225 ≈ 27` units
  from the gradient centre) with a different alpha behaviour, or producing
  positional anti-aliasing artefacts when the gradient is anchored inside a path
  whose extents differ from the gradient's outer circle.

At rest (frame 0) the two renderers happen to produce visually similar results
because the matte sweeps over a stationary face. During motion (Null ALL's animated
`p.x` / `p.y` between t=42 and t=62, then again in three other bursts), the matte
sweeps across the face faster than the gradient's anti-aliasing resolves, and the
mismatch becomes visible per-frame.

## Proposed investigation (not a fix yet)

The previous task description's hypotheses (animated gradient stops, motion blur,
per-stop alpha off-by-one) have all been ruled out by inspection of the JSON:
- All three `gf` entries (lines 11523, 25926, 27483) have `g.k.a == 0` (static).
- No `mb` (motion blur) anywhere in the file.
- The per-stop alpha count matches what `GradientStopParser` handles correctly
  (covered by `GradientStopParserTest.alphaOffsetsDifferentFromColorOffsetsAreMerged`).

What remains to investigate:

1. **Radial-gradient extrapolation parity** — compare how `javafx.scene.paint
   .RadialGradient` and HTML5 canvas `createRadialGradient` render positions before
   the first stop offset. If the rendered alpha differs, that is the root cause.
   Add a JavaFX vs reference test that renders a synthetic radial gradient
   (5 stops at 0.48–1.0, no alpha tail) on a path that extends beyond the gradient's
   outer circle, snapshot both, and diff.
2. **Radial-gradient outer-circle behaviour** — the lemon path's furthest extent
   (`(-58, 0)` and `(58, 0)`) is `≈ 58` units from the gradient centre, which is
   slightly beyond the gradient's outer radius of `56.225`. Verify whether JavaFX
   paints this thin ring with the last stop's colour (extension) or with
   transparent, and compare to WebView. If WebView treats it as transparent, that
   thin ring will contribute to the visible-shadow region in REF.
3. **`tt:2` matte mode** — confirm that JavaFX's `INVERTED_ALPHA` math matches
   what lottie-web's canvas renderer does. Audit the matte composite path in
   `MatteRenderer.composeMatte()` against `lottie-web` 5.12.2's
   `effects/SVGMatte3Effect.js` / `CVCompElement.prototype.renderInnerContent`.

## Suggested next-step fixes (in order of likelihood)

1. **Update `GradientFillStyle.java`** so radial gradients whose first stop offset
   is `> 0` insert an explicit "transparent" or "extended" stop at offset 0
   (depending on what WebView does), matching WebView's rendering.
2. **Update `GradientFillStyle.java`** so the gradient's outer-circle behaviour
   matches WebView (`NO_CYCLE` is the default, but check whether the path's
   anti-aliased ring beyond the gradient outer circle should be painted with the
   last-stop colour or be clipped to the gradient extent).
3. **If both above are correct**, the residual is in the matte composition path
   itself (`MatteRenderer.renderLayerWithMatte`) — verify the parent-transform
   chain is being applied identically to the matte source and content canvases
   during motion frames.

## Validation
1. Re-run Reproduce.
2. Confirm motion-burst frames (41–55, 72–87, 120–145, 196–215) all cross
   99.5 %; the gating frame is currently frame 80 at 93.49 %.
3. Remove `json/face-peeking.json` from `PER_FILE_FLOOR_OVERRIDE` once the average
   is ≥ 99.5 %.
4. Re-run the full suite — `face-exhaling.json` (same `tt: 2` pattern) should also
   improve, as should the `java_duke_*` triplet if their per-channel skew shares
   the same root.

## See also
- `Fix-rendering-face-exhaling.md` (shares the `tt: 2` matte pattern)
- `Fix-renderer-gradient-fill-channels.md` (cross-cutting, static parse — done)
- `Fix-renderer-outpoint-frame.md` (cross-cutting — done)
- `Fix-renderer-luma-matte-rec601.md` (done, but doesn't apply here — see
  "Important: matte type misidentification" above)
