# Fix rendering: dot/lottie4j.lottie

## File
`lottie4j/fxfileviewer/src/test/resources/dot/lottie4j.lottie`

## Reproduce
```
mvn -pl fxfileviewer test \
    -Dtest=CompareFxViewWithWebViewTest#compareFxAndJsRenderingFullSize \
    -Dlottie.file=dot/lottie4j.lottie
```

## Observed gap (2026-06-25 calibration)
- average: 99.89 %
- minimum: 99.50 % (target boundary)
- floor:   99.5 % (target)
- target:  99.5 %

## Failing frames
Identical scores to `json/lottie4j.json` — this `.lottie` archive contains the same
JSON. The two should track 1:1. See per-frame breakdown in
`Fix-rendering-lottie4j.md`.

## Diff evidence
`target/test-output/dot/lottie4j-webview_scale_1.0/`.

## Lottie part diagnosis
Same as `json/lottie4j.json` — alpha matte layer "web-code-tab".

## Root cause hypothesis
Same alpha-matte channel bias as `json/lottie4j.json`. Cross-cutting:
`Fix-renderer-alpha-matte-channels.md`.

## Proposed fix
Same fix as `Fix-rendering-lottie4j.md`. No additional changes needed.

## Validation
After applying the matte fix, re-run Reproduce for both this file and
`json/lottie4j.json`. They should both improve identically.

## See also
- `Fix-rendering-lottie4j.md`
- `Fix-renderer-alpha-matte-channels.md`
