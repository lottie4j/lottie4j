# Investigate rendering gaps per Lottie file — INDEX

This is the index/result page for the meta-plan
`.prompts/task-contexts/Investigate-rendering-gaps-per-Lottie.md`. It lists every
follow-up plan produced from the 2026-06-25 calibration run captured in
`lottie4j/.calibration.log` (4193 lines, all 20 successful single-file runs).

## Cross-cutting subsystem plans (do these first — they unblock multiple files)

| Plan                                              | Affected files | Priority |
|---------------------------------------------------|----------------|----------|
| `Fix-renderer-outpoint-frame.md`                  | 11             | **highest** — single biggest score gain across the suite |
| `Fix-renderer-keyframe-segment-boundary.md`       | 4              | **high** — kills the 78.14 % min on `java_duke_fadein`/`slidein` |
| `Fix-renderer-gradient-fill-channels.md`          | 5              | **high** — explains the B-skew on face-peeking/exhaling and G-skew on demo-1 |
| `Fix-renderer-alpha-matte-channels.md`            | 4              | medium — fixes persistent R/B skew on isometric/animated_background |
| `Fix-renderer-blend-mode-offscreen-buffer.md`     | 4              | medium — uniform 0.5 pp tax on angry_bird, foojay-duke, pi4j |
| `Fix-renderer-bezier-easing-fidelity.md`          | 5              | medium — distributed sub-pixel drift on lottie_lego, sandy_loading |
| `Fix-renderer-luma-matte-rec601.md`               | 2              | low — only revisit after gradient fix lands |

## Priority queue (per the original meta-plan, sorted by observed average ascending)

### Files in `PER_FILE_FLOOR_OVERRIDE` (technical-debt entries)

| # | File                                                | avg     | min     | plan |
|---|-----------------------------------------------------|---------|---------|------|
| 1 | json/java_duke_fadein.json                          | 97.47 % | 78.14 % | `Fix-rendering-java_duke_fadein.md` |
| 2 | json/lottie_lego.json                               | 98.14 % | (skipped) | `Fix-rendering-lottie_lego.md` |
| 3 | json/java_duke_slidein.json                         | 98.60 % | 78.14 % | `Fix-rendering-java_duke_slidein.md` |
| 4 | json/face-peeking.json                              | 98.72 % | 90.11 % | `Fix-rendering-face-peeking.md` |
| 5 | json/angry_bird.json                                | 99.26 % | 98.37 % | `Fix-rendering-angry_bird.md` |
| 6 | json/animated_background_patterns.json              | 99.36 % | 81.51 % | `Fix-rendering-animated_background_patterns.md` |
| 7 | json/sandy_loading.json                             | 99.39 % | 96.45 % | `Fix-rendering-sandy_loading.md` |
| 8 | dot/demo-1.lottie                                   | 99.44 % | 91.00 % | `Fix-rendering-dot-demo-1.md` |
| 9 | json/isometric_data_analysis.json                   | 99.49 % | 95.81 % | `Fix-rendering-isometric_data_analysis.md` |

### Sweep — files currently passing (target = 99.5 % average) but with at least one frame < 99.5 %

| # | File                                                | avg     | min     | plan |
|---|-----------------------------------------------------|---------|---------|------|
| 10 | json/box-moving-changing-color.json                | 99.98 % | 98.42 % | `Fix-rendering-box-moving-changing-color.md` |
| 11 | json/face-exhaling.json                            | 99.60 % | 89.31 % | `Fix-rendering-face-exhaling.md` |
| 12 | json/foojay-duke.json                              | 99.96 % | 99.83 % | `Fix-rendering-foojay-duke.md` |
| 13 | json/foojay-reporter.json                          | 99.89 % | 98.78 % | `Fix-rendering-foojay-reporter.md` |
| 14 | json/java_duke_flip.json                           | 99.88 % | 99.61 % | `Fix-rendering-java_duke_flip.md` |
| 15 | json/loading.json                                  | 99.99 % | 99.83 % | `Fix-rendering-loading.md` |
| 16 | json/lottie4j.json                                 | 99.89 % | 99.50 % | `Fix-rendering-lottie4j.md` |
| 17 | json/pi4j.json                                     | 99.82 % | 98.13 % | `Fix-rendering-pi4j.md` |
| 18 | json/snake_ladder_loading_animation.json           | 99.96 % | 99.50 % | `Fix-rendering-snake_ladder_loading_animation.md` |
| 19 | json/success.json                                  | 100.00 % | 99.97 % | `Fix-rendering-success.md` (regression sentinel) |
| 20 | json/timeline_animation.json                       | 99.84 % | 95.84 % | `Fix-rendering-timeline_animation.md` |
| 21 | dot/lottie4j.lottie                                | 99.89 % | 99.50 % | `Fix-rendering-dot-lottie4j.md` |
| 22 | dot/demo-2.lottie                                  | 99.96 % | 99.83 % | `Fix-rendering-dot-demo-2.md` |
| 23 | dot/demo-3.lottie                                  | 99.74 % | 96.84 % | `Fix-rendering-dot-demo-3.md` |

All 23 entries in the meta-plan's `lottieJsonFiles()` are covered.

## Pre-requisite — refresh reference fixtures

Three files were **skipped** by the calibration run because their reference images
were not on the surefire classpath even though they exist in
`src/test/resources/json/<file>-webview/`:

- `json/java_duke_fadein.json`
- `json/java_duke_slidein.json`
- `json/lottie_lego.json`

Cause: the calibration task in `lottie4j/.vscode/tasks.json` ("calibrate: clean + full
suite to log") runs `mvn surefire:test` after `mvn test-compile`, which does **not**
re-run `process-test-resources`. After adding new `*-webview/` directories under
`src/test/resources/`, run a full `mvn -pl fxfileviewer test` (or
`mvn -pl fxfileviewer process-test-resources test-compile surefire:test`) once so
`target/test-classes/<…>-webview/frame_0.png` exists; subsequent
calibration runs will then exercise these files. The per-file plans for those three
files mention this requirement.

## Recommended execution order

1. **`Fix-renderer-outpoint-frame.md`** (cross-cutting) — fixes ≥11 files in one
   change. Pre-load: this alone could remove
   `animated_background_patterns`, `face-peeking`, `face-exhaling`,
   `isometric_data_analysis`, `sandy_loading`, `dot/demo-1` from
   `PER_FILE_FLOOR_OVERRIDE`.
2. **`Fix-renderer-keyframe-segment-boundary.md`** — kills the 78.14 % min on the
   Duke fade/slide pair (highest-magnitude single-frame drop in the suite). Re-run
   the full suite afterwards.
3. **`Fix-renderer-gradient-fill-channels.md`** — fixes per-channel skew on face
   and Duke animations. After this, evaluate luma matte (`Fix-renderer-luma-matte-
   rec601.md`).
4. **`Fix-renderer-alpha-matte-channels.md`** — fixes persistent R/B skew and the
   small `lottie4j` regression.
5. **`Fix-renderer-blend-mode-offscreen-buffer.md`** — closes the uniform 0.5 pp tax
   on `angry_bird`, `foojay-duke`, `pi4j`.
6. **`Fix-renderer-bezier-easing-fidelity.md`** — final polish; tightens distributed
   sub-pixel drift on `lottie_lego`, `sandy_loading`, `foojay-reporter`.

After each step, re-run the full parameterised test
(`mvn -pl fxfileviewer test -Dtest=CompareFxViewWithWebViewTest`) and prune entries
from `PER_FILE_FLOOR_OVERRIDE` whose average crosses 99.5 %. Use
`json/success.json` (100.00 % control) as the regression sentinel.

## Verification of this meta-plan

Per Step 5 / Verification in the meta-plan:

1. ✅ Each Lottie file in `lottieJsonFiles()` has a corresponding
   `Fix-rendering-<file>.md` plan (23 files → 23 plans).
2. ✅ Every per-file plan contains a `Reproduce` command using
   `-Dlottie.file=…` single-file mode.
3. ✅ Every per-file plan names at least one `fxplayer` or `core` class as the
   proposed change site (cross-cutting plans share the same site).
4. ✅ Cross-cutting findings have been consolidated into 7 subsystem plans, each of
   which links back to the per-file plans that surface it.
5. ✅ Validation steps in every plan reference the same single-file Reproduce
   command and the `PER_FILE_FLOOR_OVERRIDE` map (lines 84–112 of
   `CompareFxViewWithWebViewTest.java`) as the single source of truth for "is this
   file fixed?".
