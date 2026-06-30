# Fix rendering: json/lottie4j.json

## File
`lottie4j/fxfileviewer/src/test/resources/json/lottie4j.json`

## Reproduce
```
mvn -pl fxfileviewer test \
    -Dtest=CompareFxViewWithWebViewTest#compareFxAndJsRenderingFullSize \
    -Dlottie.file=json/lottie4j.json
```

## Observed gap (2026-06-25 calibration)
- average: 99.89 %
- minimum: **99.50 % (target boundary)** — distributed sub-99.95 % dip on frames
  61–96
- floor:   99.5 % (target)
- target:  99.5 %

## Failing frames
File is passing the average target. Min frame is exactly 99.50 — fragile. Per-frame:

| frame range | overall      | R           | G           | B           | notes                          |
|-------------|--------------|-------------|-------------|-------------|--------------------------------|
| 61–71       | 99.74–99.95  | 99.76–99.95 | 99.71–99.95 | 99.68–99.96 | dip during web-code-tab anim  |
| 72–91       | 99.50–99.87  | uniform     | uniform     | **drops to 99.35** | B-channel tiny skew    |
| 95–105      | 99.71–99.81  | uniform     | uniform     | uniform     | gradual recovery               |

The B-channel dip on frames 72–91 (B ≈99.35–99.50 while R/G ≈99.65) is minor but is the
same fingerprint as `face-peeking.json` and `face-exhaling.json`.

## Diff evidence
At least one frame at 99.50 % triggers a diff PNG; check
`target/test-output/json/lottie4j-webview_scale_1.0/frame_80_99.50.png`.

## Lottie part diagnosis
- Layer `nm:"web-code-tab"` has `"tt":1, "tp":6` → alpha matte where the matte
  source is layer 6.
- No `gf` confirmed in this file (the B-skew here is much smaller than in face-peeking,
  so it's more likely a matte/blend issue than a gradient one).

## Root cause hypothesis
Same alpha-matte channel-bias as suspected in `animated_background_patterns` and
`isometric_data_analysis` — but in this file the matte source happens to have only mild
RGB variation, so the bias only shows up as a tiny B deficit. Fixing
`Fix-renderer-alpha-matte-channels.md` should push this file's minimum from 99.50 to
≥99.85.

## Proposed fix
Apply `Fix-renderer-alpha-matte-channels.md` (cross-cutting).

## Validation
1. Re-run Reproduce.
2. Confirm minimum > 99.5 % with margin.
3. No override to remove.

## See also
- `Fix-renderer-alpha-matte-channels.md`
- `Fix-rendering-dot-lottie4j.md` (same content as `.lottie` package — should track 1:1)
