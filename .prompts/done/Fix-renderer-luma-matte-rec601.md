# Fix renderer: luma matte (tt:2 / tt:4) Rec. 601 weighting

## Scope
Companion to `Fix-renderer-alpha-matte-channels.md`. Specifically for `tt:2` (luma
matte) and `tt:4` (luma matte inverted), the matte source's brightness — computed
with Rec. 601 weights `0.299·R + 0.587·G + 0.114·B` — should drive the destination's
alpha. A wrong weight (`tt:1`-style alpha, or arithmetic average, or Rec. 709) leaves
the destination's RGB unchanged but its alpha biased.

## Affected files
| File                          | tt:2 occurrence                                  |
|-------------------------------|--------------------------------------------------|
| json/face-exhaling.json       | `"tt": 2` at line 12894 (luma matte)             |
| json/face-peeking.json        | `"tt": 2` at line 27620 (luma matte)             |

Both files already show the gradient bug from
`Fix-renderer-gradient-fill-channels.md`. **Apply the gradient fix first**, then
re-evaluate this plan — the luma matte tax is small enough to be hidden by the
gradient noise today.

## Reproduce
```
mvn -pl fxfileviewer test \
    -Dtest=CompareFxViewWithWebViewTest#compareFxAndJsRenderingFullSize \
    -Dlottie.file=json/face-exhaling.json
```

## Diagnosis
For `tt:2`:
- correct: `destAlpha *= (0.299·srcR + 0.587·srcG + 0.114·srcB) / 255`
- wrong (alpha matte): `destAlpha *= srcA / 255`
- wrong (averaging): `destAlpha *= (srcR + srcG + srcB) / (3·255)`
- wrong (Rec. 709): `destAlpha *= (0.2126·srcR + 0.7152·srcG + 0.0722·srcB) / 255`

Rec. 601 is what lottie-web uses (see `effects/SVGMaskFilter.js`); verify against
that reference.

## Proposed fix
- `lottie4j/fxplayer/src/main/java/com/lottie4j/fxplayer/renderer/layer/MatteRenderer.java`
  — implement `tt:2` and `tt:4` with the Rec. 601 formula above (integer math is
  cheaper):
  ```java
  int luma = (299 * srcR + 587 * srcG + 114 * srcB) / 1000;
  int newA = (((dest[p] >>> 24) & 0xFF) * luma) / 255;
  ```
- For `tt:4` (luma inverted), substitute `(255 - luma)`.
- Unit test with synthetic colour mattes (pure red, pure green, pure blue) to assert
  the expected luma weights are applied.

## Validation
After both this fix and `Fix-renderer-gradient-fill-channels.md`:
1. Re-run `json/face-peeking.json` and `json/face-exhaling.json` single-file.
2. Confirm motion-burst frames (e.g. face-peeking 41–55, 72–87, 120–145) rise above
   99.5 %.
3. Remove `json/face-peeking.json` from `PER_FILE_FLOOR_OVERRIDE` if average ≥ 99.5 %.

## See also
- `Fix-renderer-alpha-matte-channels.md` (paired plan)
- `Fix-renderer-gradient-fill-channels.md` (must be done first)
- `Fix-rendering-face-peeking.md`
- `Fix-rendering-face-exhaling.md`
