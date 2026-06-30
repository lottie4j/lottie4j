# Fix rendering: json/box-moving-changing-color.json

## File
`lottie4j/fxfileviewer/src/test/resources/json/box-moving-changing-color.json`

## Reproduce
```
mvn -pl fxfileviewer test \
    -Dtest=CompareFxViewWithWebViewTest#compareFxAndJsRenderingFullSize \
    -Dlottie.file=json/box-moving-changing-color.json
```

## Observed gap (2026-06-25 calibration)
- average: 99.98 %
- minimum: **98.42 % on frame 150** (out-point cliff)
- floor:   99.5 % (target)
- target:  99.5 %

## Failing frames
The animation is essentially perfect (every steady-state frame ≥ 99.9 %) **except** a
handful of small sub-pixel motion frames and the final frame:

| frame | overall | R     | G     | B     | notes                                |
|-------|---------|-------|-------|-------|--------------------------------------|
| 4     | 99.93   | 99.99 | 99.80 | 99.99 | one G-channel blip during box motion |
| 43    | 99.86   | 99.80 | 99.78 | 100   | edge AA on the box during motion     |
| 89    | 99.92   | 99.88 | 99.88 | 100   | colour transition mid-fade           |
| 105   | 99.84   | 99.76 | 99.76 | 100   | colour transition                    |
| 150   | **98.42** | **97.64** | **97.64** | 100 | **out-point — R/G both crash, B unchanged** |

R and G drop together while B stays pinned at 100 → the box is filled with a colour
that has R and G components but no B, and at the out-point the box is rendered with
the wrong opacity / position so its R+G area becomes "white" in the difference.

## Diff evidence
`target/test-output/json/box-moving-changing-color-webview_scale_1.0/frame_150_98.42.png`
(out-point) — single most informative frame.

## Lottie part diagnosis
- Single shape layer (`ty:4`, `nm:"Rectangle 7"`), one group with `rc` (rectangle) and
  `fl` (fill).
- Fill colour `c` is animated (`{"a":1, …}` with bezier keyframes) — explains "changing
  color".
- Layer `op:150` → frame 150 is exactly the out-point.

## Root cause hypothesis
Pure case of the cross-cutting **out-point frame** bug. There is no gradient, no matte,
no blend mode, no precomp — the box has a flat fill and an animated colour, and only
the out-point frame fails. Fixing `Fix-renderer-outpoint-frame.md` will close this gap
in full.

## Proposed fix
Apply `Fix-renderer-outpoint-frame.md` (cross-cutting).

## Validation
1. Re-run Reproduce.
2. Confirm frame 150 ≥ 99.5 % (or is no longer sampled if `[ip, op-1]` is the new
   clamp).
3. No `PER_FILE_FLOOR_OVERRIDE` entry to remove (this file is already at floor 99.5).
4. Re-run the full suite.

## See also
- `Fix-renderer-outpoint-frame.md`
