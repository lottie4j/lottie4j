# Fix renderer: alpha matte (tt:1) channel handling

## Scope
Cross-cutting renderer plan covering layers with `tt:1` (alpha matte) where the matte
source's RGB colour should **not** influence the destination's RGB output — only its
alpha should. Affected files show a small but persistent per-channel skew where the
matted layers' colour is biased toward the matte source's dominant channel.

## Affected files
| File                          | observed bias            | scale                         |
|-------------------------------|--------------------------|-------------------------------|
| json/animated_background_patterns.json | R-channel skew on frames 6–8 | sub-pp, masked by frame-90 catastrophe |
| json/isometric_data_analysis.json | R lags G lags B persistently (frames 11–60) | 0.5–1 pp |
| json/lottie4j.json            | B-channel drops on frames 72–91 (web-code-tab) | 0.15 pp |
| dot/lottie4j.lottie           | same as json/lottie4j.json (same JSON) | 0.15 pp |

## Reproduce
```
mvn -pl fxfileviewer test \
    -Dtest=CompareFxViewWithWebViewTest#compareFxAndJsRenderingFullSize \
    -Dlottie.file=json/isometric_data_analysis.json
```

## Diagnosis
For `tt:1` (alpha matte) the destination layer's pixels should be multiplied by the
matte source layer's **alpha channel only**:
```
dest.rgba *= matteSource.a
```
A common bug is to instead multiply by the matte source's *luma* (Rec. 601):
```
dest.rgba *= (0.299·matteSource.r + 0.587·matteSource.g + 0.114·matteSource.b)
```
That would be correct for `tt:2` (luma matte) but wrong for `tt:1`. Two distinct
files (`isometric_data_analysis` with R-skew and `lottie4j` with B-skew) showing
*different* dominant skews while both using `tt:1` is consistent with this confusion
— the bias direction depends on the matte source's dominant colour.

A second possibility: the destination's RGB is itself multiplied by the matte's RGB
(green-screen-style colour matting) instead of being preserved.

## Proposed fix
- `lottie4j/fxplayer/src/main/java/com/lottie4j/fxplayer/renderer/layer/MatteRenderer.java`
  — for `tt:1`:
  ```java
  // dest = layer below; src = matte source (rendered above, used only as mask)
  for (int p : pixels) {
      int srcA = (src[p] >>> 24) & 0xFF;
      int newA = (((dest[p] >>> 24) & 0xFF) * srcA) / 255;
      dest[p] = (newA << 24) | (dest[p] & 0x00FFFFFF);
  }
  ```
- For `tt:3` (alpha inverted), use `(255 - srcA)` instead of `srcA`.
- For `tt:2` (luma matte): use Rec. 601 luma of the matte source, *not* its alpha:
  ```java
  int srcLuma = (299 * srcR + 587 * srcG + 114 * srcB) / 1000;
  int newA = (((dest[p] >>> 24) & 0xFF) * srcLuma) / 255;
  ```
- For `tt:4` (luma inverted), use `(255 - srcLuma)`.

- Add unit tests with synthetic matte sources (pure red, pure green, pure blue, plus
  alpha gradients) to confirm RGB is preserved end-to-end.

## Validation
After applying the fix:
1. Re-run single-file tests for each affected file.
2. Confirm R/G/B converge on matted frames (no persistent per-channel skew).
3. Remove `json/isometric_data_analysis.json` from `PER_FILE_FLOOR_OVERRIDE` if its
   average crosses 99.5 % (combined with the out-point fix from
   `Fix-renderer-outpoint-frame.md`, this should be enough).
4. Confirm `json/lottie4j.json` and `dot/lottie4j.lottie` minimum frame is ≥ 99.5 %.

## See also
- `Fix-rendering-animated_background_patterns.md`
- `Fix-rendering-isometric_data_analysis.md`
- `Fix-rendering-lottie4j.md`
- `Fix-rendering-dot-lottie4j.md`
- `Fix-renderer-luma-matte-rec601.md` (companion plan if `face-peeking`/`face-exhaling`
  reveal a `tt:2` regression)
