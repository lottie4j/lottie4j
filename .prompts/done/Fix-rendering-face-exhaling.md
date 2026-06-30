# Fix rendering: json/face-exhaling.json

## File
`lottie4j/fxfileviewer/src/test/resources/json/face-exhaling.json`

## Reproduce
```
mvn -pl fxfileviewer test \
    -Dtest=CompareFxViewWithWebViewTest#compareFxAndJsRenderingFullSize \
    -Dlottie.file=json/face-exhaling.json
```

## Observed gap (2026-06-25 calibration)
- average: 99.60 % (already passes target)
- minimum: **89.31 % on frame 150** (out-point cliff)
- floor:   99.5 % (target)
- target:  99.5 %

## Failing frames
Mostly passing, with a few mid-animation sags and the out-point catastrophe:

| frame   | overall | R     | G     | B     | notes                                |
|---------|---------|-------|-------|-------|--------------------------------------|
| 13–18   | 99.26–99.49 | 99.34 | 99.17 | 99.27 | exhale onset                    |
| 28–38   | 98.80–99.50 | 98.80 | 98.74 | 98.82 | mid-exhale; B slightly worst     |
| 41      | **98.80** | 98.93 | 98.67 | 98.80 | start of exhale recovery (G low) |
| 150     | **89.31** | **96.91** | **94.31** | **76.73** | **out-point — B falls off a cliff** |

The frame-150 channel pattern (`R 96.91 / G 94.31 / B 76.73`) shows the **B channel
drops the most** — same fingerprint as `face-peeking.json`. Same code path likely.

## Diff evidence
- `target/test-output/json/face-exhaling-webview_scale_1.0/frame_150_89.31.png`
- `target/test-output/json/face-exhaling-webview_scale_1.0/frame_41_98.80.png`

## Lottie part diagnosis
- File contains `"tt": 1` at line 10764 → alpha matte.
- File contains `"tt": 2` at line 12894 → **luma matte** (Rec. 601 weighted brightness
  of source layer used as destination's alpha).
- File contains 2× `"ty": "gf"` (lines 12757, 13270) → gradient fills.

## Root cause hypothesis
Two compounding bugs (same as `face-peeking`):

1. **Blue-channel skew at out-point and on exhale frames:** gradient fill stop
   parsing, see `Fix-renderer-gradient-fill-channels.md`.
2. **Frame-150 out-point cliff:** see `Fix-renderer-outpoint-frame.md`.

The exhale animation (mouth/cheeks expanding) likely uses the gradient fills, so a
gradient bug shows on every exhale frame.

## Proposed fix
- `Fix-renderer-gradient-fill-channels.md` (cross-cutting)
- `Fix-renderer-outpoint-frame.md` (cross-cutting)
- Sanity-check luma matte computation: `MatteRenderer.java` for `tt:2` must use
  `0.299·R + 0.587·G + 0.114·B` (Rec. 601), not a flat average.

## Validation
1. Re-run Reproduce.
2. Every frame ≥ 99.5 % and average stays ≥ 99.5 %.
3. No override entry to remove.
4. Re-run the full suite.

## See also
- `Fix-rendering-face-peeking.md` (same gradient + matte stack)
- `Fix-renderer-gradient-fill-channels.md`
- `Fix-renderer-outpoint-frame.md`
- `Fix-renderer-luma-matte-rec601.md` (if exhale frames don't improve with gradient fix)
