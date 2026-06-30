# Fix renderer: keyframe segment-boundary evaluation

## Scope
Cross-cutting renderer plan for the bug where evaluating a keyframe segment at exactly
`t == k.t` (segment start or end) returns the wrong segment's value, producing a
single-frame jolt. Highest-priority symptom is the **78.14 % minimum on
`java_duke_fadein` and `java_duke_slidein`** — both show the exact same dip, ruling
out file content as the cause.

## Affected files
| File                          | symptom                                   |
|-------------------------------|-------------------------------------------|
| json/java_duke_fadein.json    | single 78.14 % frame at fade boundary     |
| json/java_duke_slidein.json   | single 78.14 % frame at fade boundary     |
| json/foojay-reporter.json     | dip cluster 31–58 ending at 98.78 % min   |
| (likely) json/lottie_lego.json | distributed sub-pixel timing drifts       |

## Reproduce
```
mvn -pl fxfileviewer test \
    -Dtest=CompareFxViewWithWebViewTest#compareFxAndJsRenderingFullSize \
    -Dlottie.file=json/java_duke_fadein.json
```
> Note: needs `mvn test` (not `surefire:test`) so the reference images for these files
> are on the classpath — see `Fix-rendering-java_duke_fadein.md`.

## Diagnosis
Given keyframes `k0, k1, k2, …` with timestamps `t0 < t1 < t2 < …`, the standard
convention is that segment `i` covers `[ti, ti+1)`. A single-frame jolt at `t == ti+1`
suggests the evaluator selects segment `i+1` (or fails the segment search and returns
zero) instead of segment `i`'s end value.

The fact that two animations share the same 78.14 % min — but otherwise have *only*
the gradient fill (`gf`) in common — narrows the suspect to:
- The keyframe segment search itself in `lottie4j/core/src/main/java/com/lottie4j/core/model/keyframe/`.
- The gradient stop *animation* evaluator if `g.k.k` is animated and uses the same
  search.

## Proposed fix
1. `lottie4j/core/src/main/java/com/lottie4j/core/model/keyframe/` — locate the
   segment search (likely something like `findKeyframeAt(double t)` or
   `interpolate(double t)`). Ensure:
   - For `t == k[i].t` (`i > 0`): return segment `i-1`'s `end` value (or evaluate
     segment `i-1` at fraction 1.0), **not** segment `i` at fraction 0.0. They are
     usually equal for a continuous animation, but **gradient stops with bezier easing
     can have non-equal end/start due to `i`/`o` x/y arrays** ⇒ that's where 78 % shows
     up.
   - For `t == k[last].t`: return last keyframe's `start` value (or whatever lottie-
     web does — verify against `helpers/dynamicProperty.js` / `getValueAtTime`).

2. Add a unit test that builds a 3-keyframe sequence with sharp bezier easing and
   evaluates at every integer frame, including the exact boundaries.

3. If the keyframe model differs for property kinds (e.g. position uses
   `lottie4j/core/src/main/java/com/lottie4j/core/model/bezier/`), audit all of them.

## Validation
After applying the fix:
1. Re-run single-file tests for the affected files.
2. Confirm no single-frame catastrophic dip (≥ 99.5 % on every frame).
3. Remove `json/java_duke_fadein.json` and `json/java_duke_slidein.json` from
   `PER_FILE_FLOOR_OVERRIDE` if averages cross the target.
4. Re-run the full suite — fix is broad; sub-pp distributed drift in many other files
   may also tighten.

## See also
- `Fix-rendering-java_duke_fadein.md`
- `Fix-rendering-java_duke_slidein.md`
- `Fix-rendering-foojay-reporter.md`
- `Fix-rendering-lottie_lego.md`
- `Fix-renderer-bezier-easing-fidelity.md` (related but distinct)
