# Fix rendering: json/isometric_data_analysis.json

## File
`lottie4j/fxfileviewer/src/test/resources/json/isometric_data_analysis.json`

## Reproduce
```
mvn -pl fxfileviewer test \
    -Dtest=CompareFxViewWithWebViewTest#compareFxAndJsRenderingFullSize \
    -Dlottie.file=json/isometric_data_analysis.json
```

## Observed gap (2026-06-25 calibration)
- average: 99.49 % (one-hundredth shy of the target!)
- minimum: 95.81 % on **frame 180** (the out-point)
- floor:   98.9 % (override)
- target:  99.5 %

## Failing frames
The average is so close to the target that **fixing only frame 180** is enough to push
this file over the line. Per-frame data:

| frame range | overall      | R           | G           | B           | notes                            |
|-------------|--------------|-------------|-------------|-------------|----------------------------------|
| 0–10        | 99.59–99.72  | 99.45–99.62 | 99.53–99.69 | **99.79–99.85** | R lags G, G lags B          |
| 11–60       | 99.02–99.55  | drops to    | drops to    | stays       | **persistent R-skew**            |
|             |              | **98.74**   | **98.85**   | ≈99.47      |                                  |
| 61–179      | 99.30–99.72  | rises       | rises       | ≈99.85      | gradual recovery                 |
| 180         | **95.81**    | **94.21**   | 95.09       | 98.12       | **out-point cliff**              |

The skew direction (**R < G < B** consistently) is the **opposite** of `face-peeking`'s
R > G > B skew, ruling out the same gradient cause. This is *not* the gradient bug.

## Diff evidence
- `target/test-output/json/isometric_data_analysis-webview_scale_1.0/frame_180_95.81.png`
  — out-point cliff.
- `target/test-output/json/isometric_data_analysis-webview_scale_1.0/frame_31_99.10.png`
  — typical R-deficit frame for diagnosis.

## Lottie part diagnosis
- File contains many `"tt": 1` (alpha matte) layers parented to layer 56 (so 56 is the
  matte source for layers 37–43, etc.).
- Isometric scene with many panel/grid shapes.
- R/G/B skew of `R < G < B` (red is most attenuated) suggests the alpha-matte channel
  bias is opposite to the green-gradient case: matted blue-tinted content suffers less
  when the matte under-applies the source's alpha channel.

## Root cause hypothesis
1. **Frame 180 (out-point):** cross-cutting bug shared with at least 7 other files.
   See `Fix-renderer-outpoint-frame.md`.
2. **R-skew on frames 11–60:** alpha-matte source layer (`tt:1`) being composited with
   the wrong alpha-source mode. If `MatteRenderer` reads the matte source's RGB
   intensity instead of pure alpha for `tt:1`, blue-heavy content gets more "opacity"
   than red-heavy content. Same code path as
   `Fix-rendering-animated_background_patterns.md`.

## Proposed fix
- **Frame 180:** cross-cutting fix per `Fix-renderer-outpoint-frame.md`.
- `lottie4j/fxplayer/src/main/java/com/lottie4j/fxplayer/renderer/layer/MatteRenderer.java`
  — for `tt:1` (alpha matte), the destination's alpha must equal the matte source's
  alpha (channel A only), independent of RGB. Verify with a synthetic test: a fully
  opaque pure red matte source should pass through 100 % alpha; a 50 %-alpha pure green
  source should pass through 50 %.

## Validation
1. Re-run Reproduce.
2. Frame 180 must score ≥ 99.5 %; once that is fixed, the average becomes ~99.51 % and
   the file passes at the target — *no other change required*.
3. Remove the entry from `PER_FILE_FLOOR_OVERRIDE`.
4. Re-run the full suite.

## See also
- `Fix-renderer-outpoint-frame.md` (the critical fix for this file)
- `Fix-renderer-alpha-matte-channels.md` (shared with `animated_background_patterns`,
  `lottie4j`)
