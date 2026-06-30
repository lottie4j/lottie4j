id: 2c2b2f8e-3442-416d-8016-252fcfd04975
sessionId: 818d1e50-3b9d-4036-a2d0-abd5d8cc35a8
date: '2026-06-29T13:10:12.105Z'
label: Archive obsolete per-file plans and recalibrate the 8 remaining ones
---
# Archive obsolete per-file plans and recalibrate the 8 remaining ones

## Goal
After the cross-cutting renderer fixes in `.prompts/done/` landed, only 8 of the
23 per-file rendering plans remain relevant (the ones whose entries are still in
`PER_FILE_FLOOR_OVERRIDE` inside
`lottie4j/fxfileviewer/src/test/java/com/lottie4j/fxfileviewer/CompareFxViewWithWebViewTest.java`).
This plan does two things:

1. **Archive** the 15 obsolete per-file plans by moving them from
   `.prompts/task-contexts/` to `.prompts/done/`.
2. **Re-calibrate** the 8 remaining files with the current code so the next
   investigation round starts from fresh observed averages — the numbers in those
   plans pre-date the out-point / keyframe-boundary / gradient / matte / bezier
   fixes and are no longer trustworthy.

## Design

### Why move, not delete
Done plans are useful future reference (why we made certain renderer changes, what
diagnoses played out). The convention already in use (`Fix-renderer-*.md` cross-
cutting plans live in `done/`) is to move, not delete. None of the 15 obsolete
plan filenames collide with anything currently in `done/`.

### Why re-calibrate before the next round of fixes
The per-file plans' "Observed gap", "Failing frames" and "Root cause hypothesis"
sections were captured during the **2026-06-25** calibration, *before*:

- out-point clamping (`FrameTiming.getLastRenderableFrame`)
- keyframe segment-boundary evaluation
- gradient fill per-channel parity
- alpha matte (`tt:1`) + luma matte (`tt:2`, Rec. 601) handling
- blend-mode offscreen buffer fidelity
- BezierEasing solver fidelity (`NEWTON_ITERATIONS = 4`, full lottie-web port)

All 8 remaining files are affected by at least one of those subsystem fixes, so
their current per-file numbers will have shifted. Working off stale diagnoses
risks chasing already-fixed root causes.

## Implementation Steps

### Step 1: Move the 15 obsolete per-file plans

Run from the repository root:

```bash
cd /Users/frank/Documents/GitHub/lottie4j

git mv .prompts/task-contexts/Fix-rendering-animated_background_patterns.md      .prompts/done/
git mv .prompts/task-contexts/Fix-rendering-box-moving-changing-color.md         .prompts/done/
git mv .prompts/task-contexts/Fix-rendering-face-exhaling.md                     .prompts/done/
git mv .prompts/task-contexts/Fix-rendering-foojay-duke.md                       .prompts/done/
git mv .prompts/task-contexts/Fix-rendering-foojay-reporter.md                   .prompts/done/
git mv .prompts/task-contexts/Fix-rendering-java_duke_flip.md                    .prompts/done/
git mv .prompts/task-contexts/Fix-rendering-loading.md                           .prompts/done/
git mv .prompts/task-contexts/Fix-rendering-lottie4j.md                          .prompts/done/
git mv .prompts/task-contexts/Fix-rendering-pi4j.md                              .prompts/done/
git mv .prompts/task-contexts/Fix-rendering-snake_ladder_loading_animation.md    .prompts/done/
git mv .prompts/task-contexts/Fix-rendering-success.md                           .prompts/done/
git mv .prompts/task-contexts/Fix-rendering-timeline_animation.md                .prompts/done/
git mv .prompts/task-contexts/Fix-rendering-dot-lottie4j.md                      .prompts/done/
git mv .prompts/task-contexts/Fix-rendering-dot-demo-2.md                        .prompts/done/
git mv .prompts/task-contexts/Fix-rendering-dot-demo-3.md                        .prompts/done/
```

If you'd rather skip git (e.g. you want to delete instead of archive), replace
`git mv` with `mv`. None of these filenames collide with existing entries in
`.prompts/done/`.

### Step 2: Verify the remaining set

After Step 1, `.prompts/task-contexts/` should contain **exactly 8** files:

```
Fix-rendering-angry_bird.md
Fix-rendering-dot-demo-1.md
Fix-rendering-face-peeking.md
Fix-rendering-isometric_data_analysis.md
Fix-rendering-java_duke_fadein.md
Fix-rendering-java_duke_slidein.md
Fix-rendering-lottie_lego.md
Fix-rendering-sandy_loading.md
```

Sanity check:

```bash
ls .prompts/task-contexts/ | wc -l   # → 8
```

And cross-check against the override map:

```bash
grep -E 'Map\.entry\("' \
  lottie4j/fxfileviewer/src/test/java/com/lottie4j/fxfileviewer/CompareFxViewWithWebViewTest.java
```

The eight file names in `PER_FILE_FLOOR_OVERRIDE` must match the eight remaining
plan files one-to-one (modulo path prefix differences `json/foo.json` ↔
`Fix-rendering-foo.md`).

### Step 3: Re-calibrate the 8 remaining files (fresh per-file averages)

For each remaining file, run the single-file harness and capture the
per-frame + summary output. Doing this *one at a time* gives a clean log per
file (the existing `.calibration.log` mixed all 20 runs together).

The exact command per file:

```bash
cd /Users/frank/Documents/GitHub/lottie4j

mvn -pl fxfileviewer -am test \
    -Dtest=CompareFxViewWithWebViewTest#compareFxAndJsRenderingFullSize \
    -Dlottie.file=<FILE> \
    2>&1 | tee .calibration-<SHORTNAME>.log
```

Run for each of:

| # | `<FILE>`                                    | `<SHORTNAME>`                     |
|---|---------------------------------------------|-----------------------------------|
| 1 | `json/angry_bird.json`                      | `angry_bird`                      |
| 2 | `json/face-peeking.json`                    | `face-peeking`                    |
| 3 | `json/isometric_data_analysis.json`         | `isometric_data_analysis`         |
| 4 | `json/java_duke_fadein.json`                | `java_duke_fadein`                |
| 5 | `json/java_duke_slidein.json`               | `java_duke_slidein`               |
| 6 | `json/lottie_lego.json`                     | `lottie_lego`                     |
| 7 | `json/sandy_loading.json`                   | `sandy_loading`                   |
| 8 | `dot/demo-1.lottie`                         | `dot-demo-1`                      |

> ⚠ Use `mvn test` (not `mvn surefire:test`) so `process-test-resources`
> re-populates `target/test-classes/json/<file>-webview/` — otherwise files
> whose `*-webview/` directories were added after the last clean build
> (`java_duke_fadein`, `java_duke_slidein`, `lottie_lego`) self-skip via
> `Assumptions.assumeTrue`.

#### Optional one-shot wrapper

If you want to fire all 8 in sequence and stash logs alongside each other:

```bash
cd /Users/frank/Documents/GitHub/lottie4j

mkdir -p .calibration

for entry in \
  "json/angry_bird.json|angry_bird" \
  "json/face-peeking.json|face-peeking" \
  "json/isometric_data_analysis.json|isometric_data_analysis" \
  "json/java_duke_fadein.json|java_duke_fadein" \
  "json/java_duke_slidein.json|java_duke_slidein" \
  "json/lottie_lego.json|lottie_lego" \
  "json/sandy_loading.json|sandy_loading" \
  "dot/demo-1.lottie|dot-demo-1"
do
  file="${entry%%|*}"
  short="${entry##*|}"
  echo "─── calibrating ${file} ───"
  mvn -pl fxfileviewer -am test \
      -Dtest=CompareFxViewWithWebViewTest#compareFxAndJsRenderingFullSize \
      -Dlottie.file="${file}" \
      2>&1 | tee ".calibration/${short}.log"
done
```

This populates `.calibration/<shortname>.log` per file. (Feel free to add the
directory to `.gitignore` if you don't want the logs committed.)

### Step 4: Capture the fresh numbers in each plan

For each of the 8 logs, extract the summary line that the test prints at the
end of the run, e.g.:

```
json/java_duke_fadein.json @ 100% — average 99.83%  min 99.51%  floor 97.50%  target 99.50%  gap -0.33%
```

Then update the corresponding plan in `.prompts/task-contexts/`:

- Replace the "Observed gap (2026-06-25 calibration)" block with a new
  "Observed gap (<today's date>)" block containing the fresh
  `average / minimum / floor / target` values.
- Refresh the "Failing frames" table from the per-frame lines
  `Frame N @ 100% scale: X% similar (R … G … B …)` that scored below 99.5%.
- Trim "Root cause hypothesis" entries that the cross-cutting fixes already
  closed (the *Fix-renderer-\** plans now in `.prompts/done/`):
  - keyframe segment boundary
  - out-point / in-point frame
  - gradient fill per-channel parity
  - alpha matte channels (`tt:1`)
  - luma matte Rec. 601 (`tt:2`, `tt:4`)
  - blend-mode offscreen buffer
  - bezier easing fidelity

  If the fresh numbers show the gap is gone, the plan itself can move to
  `.prompts/done/` and the file's entry can be removed from
  `PER_FILE_FLOOR_OVERRIDE` in `CompareFxViewWithWebViewTest.java`.

(I can do this update step in a follow-up once the calibration logs exist;
just paste the 8 summary lines back to me.)

## Reference Examples

- Override map and target constants:
  `lottie4j/fxfileviewer/src/test/java/com/lottie4j/fxfileviewer/CompareFxViewWithWebViewTest.java:75-117`
- Single-file mode wiring:
  `lottie4j/fxfileviewer/src/test/java/com/lottie4j/fxfileviewer/CompareFxViewWithWebViewTest.java:140-181`
- Per-frame log format produced by the harness:
  `lottie4j/fxfileviewer/src/test/java/com/lottie4j/fxfileviewer/CompareFxViewWithWebViewTest.java:285-289`
- End-of-run summary line:
  `lottie4j/fxfileviewer/src/test/java/com/lottie4j/fxfileviewer/CompareFxViewWithWebViewTest.java:316-321`
- Cross-cutting plans whose conclusions should be subtracted from the per-file
  diagnoses:
  - `.prompts/done/Fix-renderer-outpoint-frame.md`
  - `.prompts/done/Fix-renderer-keyframe-segment-boundary.md`
  - `.prompts/done/Fix-renderer-gradient-fill-channels.md`
  - `.prompts/done/Fix-renderer-alpha-matte-channels.md`
  - `.prompts/done/Fix-renderer-luma-matte-rec601.md`
  - `.prompts/done/Fix-renderer-blend-mode-offscreen-buffer.md`
  - `.prompts/done/Fix-renderer-bezier-easing-fidelity.md`

## Verification

1. `ls .prompts/task-contexts/` lists exactly the 8 files in Step 2.
2. `ls .prompts/done/ | grep '^Fix-rendering-' | wc -l` returns 15
   (the moved plans).
3. Each of the 8 calibration commands in Step 3 ends with a single
   `BUILD SUCCESS` line and a final
   `<file> @ 100% — average X%  min Y%  floor Z%  target 99.50%  gap …`
   line. No assertion should fail — every file has a current `floor` in
   `PER_FILE_FLOOR_OVERRIDE` that the present code is already known to clear.
4. For any file whose fresh `average ≥ 99.50%`, its entry in
   `PER_FILE_FLOOR_OVERRIDE` can be removed in the same change; re-run the
   single-file test once more to confirm it still passes at the bare target.
