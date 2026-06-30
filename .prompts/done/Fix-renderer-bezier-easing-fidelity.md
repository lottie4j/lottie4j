# Fix renderer: bezier easing solver fidelity

## Scope
Cross-cutting renderer plan for the bezier-easing solver used inside keyframe
segments (`k[i].i.x`, `k[i].i.y`, `k[i].o.x`, `k[i].o.y` arrays). Mismatches between
lottie4j's solver and lottie-web's `BezierEasing` produce sub-pixel positioning /
opacity drift that **distributes** small score losses over many frames.

## Affected files (symptoms)
| File                          | symptom                                                   |
|-------------------------------|-----------------------------------------------------------|
| json/lottie_lego.json         | distributed 1.4 pp gap across the entire animation        |
| json/foojay-reporter.json     | recovery curve 98.78 → 99.97 over 20 frames               |
| json/sandy_loading.json       | spinning-shape sub-pixel rotation drift                   |
| json/angry_bird.json          | uniform 0.5 pp tax on most frames                         |
| json/pi4j.json                | frames 7–28 motion burst                                  |

These all show **R ≈ G ≈ B** drops (no chromatic skew) → the bug is geometric, not
colour. Distributed nature + small magnitude is the easing-solver signature.

## Reproduce
```
mvn -pl fxfileviewer test \
    -Dtest=CompareFxViewWithWebViewTest#compareFxAndJsRenderingFullSize \
    -Dlottie.file=json/lottie_lego.json
```

## Diagnosis
lottie-web's `BezierEasing.js` uses:
- 4-pt Newton-Raphson with `NEWTON_ITERATIONS = 4`
- Subdivision fallback when slope < `NEWTON_MIN_SLOPE` (0.001)
- A lookup-table of 11 samples at `STEPS_PER_T_VALUE = 10`

Any deviation in:
- iteration count
- table resolution
- initial guess strategy
- terminator slope threshold

produces a small but systematic difference at fractional `t` values. With per-frame
SSIM windowing, even sub-pixel jitter shows up.

`lottie4j/fxplayer/src/main/java/com/lottie4j/fxplayer/renderer/shape/PathBezierInterpolator.java`
is the prime suspect — verify it mirrors the lottie-web algorithm byte-for-byte. If
it uses Apache Commons Math or a custom solver, replace it with an inline port of
`BezierEasing` to remove solver-choice as a variable.

## Proposed fix
- `lottie4j/fxplayer/src/main/java/com/lottie4j/fxplayer/renderer/shape/PathBezierInterpolator.java`
  — audit constants and algorithm against the reference upstream
  (`https://github.com/airbnb/lottie-web/blob/master/player/js/utils/BezierEaser.js`).
  Match exactly:
    ```
    NEWTON_ITERATIONS = 4
    NEWTON_MIN_SLOPE  = 0.001
    SUBDIVISION_PRECISION = 0.0000001
    SUBDIVISION_MAX_ITERATIONS = 10
    KSPLINE_TABLE_SIZE = 11
    SAMPLE_STEP_SIZE = 1.0 / (KSPLINE_TABLE_SIZE - 1)
    ```
- Add a unit test feeding a known (`x1,y1,x2,y2`) and asserting the output of
  `solve(x)` matches lottie-web's output to 1e-6 at `x ∈ {0.0, 0.25, 0.5, 0.75, 1.0}`
  and a sweep at `1/30` steps.

## Validation
After applying:
1. Re-run single-file tests for affected files.
2. Distributed sub-99.5 % frames should rise above 99.5 % uniformly.
3. Remove `json/lottie_lego.json` from `PER_FILE_FLOOR_OVERRIDE` if average crosses
   99.5 % (combined with any other fixes).
4. Re-run the full suite — improvements should be visible everywhere.

## See also
- `Fix-rendering-lottie_lego.md`
- `Fix-rendering-foojay-reporter.md`
- `Fix-rendering-sandy_loading.md`
- `Fix-rendering-angry_bird.md`
- `Fix-rendering-pi4j.md`
- `Fix-renderer-keyframe-segment-boundary.md`
