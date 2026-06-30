id: 23912944-3ae1-4e90-9754-cb7bd125a182
sessionId: f58a5080-412e-4f9a-b083-149f803290fd
date: '2026-06-30T08:19:08.543Z'
label: Reset PER_FILE_FLOOR_OVERRIDE and re-calibrate against new WebView references
---
# Reset PER_FILE_FLOOR_OVERRIDE and re-calibrate against new WebView references

## Goal
After the renderer rework and the full regeneration of WebView reference PNGs by
`WebViewScreenshotGenerator`, the historical per-file floors in
`PER_FILE_FLOOR_OVERRIDE` (and its half-size sibling) no longer reflect reality.
Wipe both maps, run the full test, and repopulate **only** if the new
measurements demand it. The expected outcome is that both maps stay empty.

## Design
- Treat the override maps as pure technical-debt registers: empty is the
  desired steady state.
- Keep the surrounding plumbing (`overrides` parameter, `floorFor`-style
  lookup via `getOrDefault`, the two `Map<String, Double>` constants) intact
  even if the maps empty out — it's the one knob we use when a single
  animation regresses, and removing it would force a wider edit the next
  time we need it.
- Discard the stale `.calibration/*.log` files in the same change so the
  repo doesn't carry pre-rework numbers that disagree with both code and
  reference PNGs.
- Re-run the full parameterized test (all 24 entries from `lottieJsonFiles()`)
  plus the half-size test, capture new per-file averages from CI/console
  output, and decide afterwards whether any entries need to come back.

## Implementation Steps

### Step 1: Empty both override maps
- `lottie4j/fxfileviewer/src/test/java/com/lottie4j/fxfileviewer/CompareFxViewWithWebViewTest.java`
  - Replace the body of `PER_FILE_FLOOR_OVERRIDE` (lines ~97–128) with an
    empty `Map.ofEntries()` (or equivalently `Map.of()`).
  - Replace the body of `PER_FILE_FLOOR_OVERRIDE_HALF` (lines ~132–136) with
    the same — drop the `animated_background_patterns` entry.
  - Update the Javadoc on `PER_FILE_FLOOR_OVERRIDE` so it reads as the
    *steady state* rather than referring to a past calibration run, e.g.:
    > "Currently empty: every animation meets `TARGET_AVERAGE_SIMILARITY`
    > under the regenerated WebView references. Repopulate only if a
    > re-run of `compareFxAndJsRenderingFullSize` shows an average below
    > the target — entries are tracked technical debt."
  - Same treatment for the `PER_FILE_FLOOR_OVERRIDE_HALF` Javadoc.
  - Leave `TARGET_PER_FRAME_SIMILARITY`, `TARGET_AVERAGE_SIMILARITY`,
    `compareFxWithPreGeneratedImages(..., Map<String, Double> overrides)`,
    and the `overrides.getOrDefault(fileName, TARGET_AVERAGE_SIMILARITY)`
    lookup unchanged — they're how we re-add a single entry quickly if
    one file regresses later.

### Step 2: Drop the stale calibration logs
- Delete every file under `lottie4j/.calibration/`:
  - `angry_bird.log`
  - `dot-demo-1.log`
  - `face-peeking.log`
  - `isometric_data_analysis.log`
  - `java_duke_fadein.log`
  - `java_duke_slidein.log`
  - `lottie_lego.log`
  - `sandy_loading.log`
- These all pre-date the renderer rework and the WebView reference
  regeneration; keeping them would mislead future readers. The directory
  itself can stay (the calibration script in
  `lottie4j/.vscode/calibrate-8-remaining.sh` recreates it via `mkdir -p`).
- Optional: also delete `lottie4j/.vscode/calibrate-8-remaining.sh` —
  it hard-codes the previous eight files. Either delete it or generalize
  it to read the file list from
  `CompareFxViewWithWebViewTest.lottieJsonFiles()`. **Recommendation:**
  leave it alone for this PR; it's a personal calibration tool, not
  test code, and rewriting it is scope creep.

### Step 3: Run the full test and capture per-file averages
- Build prerequisites and run the full parameterized test:
  ```bash
  cd lottie4j
  mvn -pl core,fxplayer install -DskipTests -q
  mvn -pl fxfileviewer test \
      -Dtest=CompareFxViewWithWebViewTest \
      -Dsurefire.failIfNoSpecifiedTests=false \
      | tee .calibration/full-rerun.log
  ```
- The summary line for each file looks like:
  ```
  json/angry_bird.json @ 100% — average 99.26%  min 98.37%  floor 99.50%  target 99.50%  gap +0.24%
  ```
  Grep them out:
  ```bash
  grep -E 'average .* gap' .calibration/full-rerun.log
  ```

### Step 4: Decide based on the rerun
Three outcomes, handled in order:

1. **All 24 files pass and all averages ≥ `TARGET_AVERAGE_SIMILARITY`
   (99.50).**
   Both maps stay empty. Commit the test edits + log deletions.
   This is the success case the user is hoping for.

2. **Some files fail (`average < floor` where floor = target since map
   is empty), but only by a small margin (e.g. ≥ 99.0).**
   Add the minimum number of entries back to `PER_FILE_FLOOR_OVERRIDE`
   to make the test green. New entry format (match the existing style
   so the next person reading the map can track progress):
   ```java
   // <file>: <one-line rationale>.                                   (observed <avg>)
   Map.entry("json/<file>.json", <floor>),
   ```
   Where `<floor>` is `floor(observed_avg * 10) / 10 - 0.1` — i.e. one
   decimal below the observed average, giving ~0.1pt of CI jitter
   headroom. Example: observed 99.26 → floor 99.1.
   This is a tighter safety margin than the old `~0.5pt` because the
   measurements are now expected to be much closer to the target.

3. **Some files fail badly (e.g. average < 99.0).**
   Stop — this is a real regression introduced by the rework, not
   leftover technical debt. Don't add a wide-floor override to paper
   over it. Surface it to the user, attach the failing frame diff PNGs
   from `lottie4j/fxfileviewer/target/test-output/`, and plan a
   targeted fix before reopening this task.

- Half-size: same logic against `PER_FILE_FLOOR_OVERRIDE_HALF`, but only
  one file is tested (`lottieJsonFiles().toList().get(1)`, currently
  `json/animated_background_patterns.json`).

### Step 5: Re-record the new calibration baseline (only if Step 4 added entries)
- If any entries had to come back, also save the per-file output for
  those files only into `lottie4j/.calibration/<short>.log` (matching
  the existing naming) so the technical debt has a baseline to measure
  progress from. Skip this step entirely if both maps stay empty.

## Reference Examples
- `lottie4j/fxfileviewer/src/test/java/com/lottie4j/fxfileviewer/CompareFxViewWithWebViewTest.java:97`
  — current shape of `PER_FILE_FLOOR_OVERRIDE`, including the
  per-entry comment convention `(observed XX.XX)` to preserve.
- `lottie4j/fxfileviewer/src/test/java/com/lottie4j/fxfileviewer/CompareFxViewWithWebViewTest.java:336`
  — `overrides.getOrDefault(fileName, TARGET_AVERAGE_SIMILARITY)`: the
  one line that makes both maps strictly additive. Verifies that an
  empty map degrades to "everyone is held to the target," which is
  what we want.
- `lottie4j/.vscode/calibrate-8-remaining.sh` — pattern for per-file
  reruns if Step 4 needs to bisect which file is the outlier.
- `lottie4j/.prompts/done/Raise-rendering-similarity-bar-to.md` — the
  original design doc for this override mechanism; useful background
  if you need to re-justify keeping the plumbing in place.

## Verification
1. The two map literals in `CompareFxViewWithWebViewTest.java` compile
   with an empty body (`Map.ofEntries()` is valid Java).
2. `mvn -pl fxfileviewer test -Dtest=CompareFxViewWithWebViewTest`
   passes locally and in CI.
3. `grep -E 'average .* gap' .calibration/full-rerun.log` shows every
   file with `gap ≤ 0.00%` (i.e. averages ≥ 99.50). If any line shows
   `gap > 0`, Step 4 applies.
4. `lottie4j/.calibration/` either contains only `full-rerun.log` (ideal
   case) or `full-rerun.log` plus one targeted log per surviving
   override entry.
5. Map size sanity check — open `CompareFxViewWithWebViewTest.java` and
   confirm both maps render as `Map.ofEntries()` (no body) in the ideal
   case, matching the new Javadoc claim.
