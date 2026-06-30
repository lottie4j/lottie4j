id: dbecac88-97c7-47dd-b272-4cfddf4cfac8
sessionId: 2b1bd759-3260-476f-9d1f-39b89bda959c
date: '2026-06-29T15:29:39.897Z'
label: Migrate WebView renderer to dotlottie-wc (thorvg)
---
# Migrate WebView renderer to dotlottie-wc (thorvg)

## Goal

Make the JavaFX `WebView` interactive viewer and the Selenium-based reference screenshot generator render Lottie animations using the same engine as the **official LottieFiles online preview** — `@lottiefiles/dotlottie-wc`, which uses **thorvg** under the hood.

This single migration resolves all three issues raised against `interactive_mood_selector_ui`:

1. **Hard-edged gradients-to-transparent** — caused by `lottie-web` 5.12.2's `canvas` renderer mishandling `gf` shapes with alpha stops. thorvg renders these correctly.
2. **Outdated `lottie-web`** — moot; we stop using `lottie-web` entirely.
3. **Mismatch with the official preview tool** — fixed by definition: both viewer and reference generator now use the same engine as `interactive_mood_selector_ui-webviewer.png`.

It also brings `LottieWebView.java`'s Javadoc (line 28: *"renders Lottie animations using a WebView with the dotlottie-wc library"*) back in sync with reality.

## Design

### Engine choice — `dotlottie-wc`

- Library: `@lottiefiles/dotlottie-wc` (Web Component wrapper over `@lottiefiles/dotlottie-web`).
- Loaded as an ES module from a CDN (`https://unpkg.com/@lottiefiles/dotlottie-wc@<version>/dist/dotlottie-wc.js`). Pin to an exact version so reference PNGs remain reproducible.
- API surface we need:
  - Custom element: `<dotlottie-wc src=… autoplay loop></dotlottie-wc>`
  - Programmatic instance: obtained via `element.getDotLottieInstance()` (or the `dotlottie-ready` / `ready` event payload).
  - Frame control: `instance.setFrame(n)`, `instance.pause()`, `instance.play()`, `instance.totalFrames`, `instance.currentFrame`.
  - Events: `ready` (initial decode), `frame` (per-tick frame index).

### Passing raw JSON to `<dotlottie-wc>`

`dotlottie-wc` expects `src` to be a URL. To inject the already-loaded Lottie JSON without writing temp files (current `LottieWebView` flow base64-encodes JSON into the HTML), we will:

- Use a `data:application/json;base64,…` URL for the `src` attribute. This already aligns with the existing `Base64.getEncoder().encodeToString(...)` path in `LottieWebView.loadLottieRaw` and `WebViewScreenshotGenerator.generateScreenshotsForFile`.

### JavaFX WebView compatibility

- JavaFX 17+ WebKit supports ES modules, Custom Elements v1, and WebAssembly (thorvg ships as WASM). No additional JavaFX changes expected, but verify on the target JavaFX version (check `pom.xml` / `module-info.java` for the current version).
- If module-script loading from CDN fails inside JavaFX WebView (it occasionally has CORS/MIME quirks), fallback is to bundle the `dotlottie-wc` UMD build as a classpath resource and serve it via a `data:`/`blob:` URL — but try the CDN path first.

### Headless-Chrome generator compatibility

- No restrictions; full Chrome supports everything `dotlottie-wc` needs.

### Reference-image invalidation

The committed PNGs under `src/test/resources/**/-webview/` were produced by `lottie-web` canvas. After this migration they will diverge from the new renderer (mostly in ways the user *wants* — accurate gradients, etc.). They must be regenerated, and the per-file similarity overrides in `CompareFxViewWithWebViewTest.PER_FILE_FLOOR_OVERRIDE` must be recalibrated against the new baseline.

The generator has a `hasScreenshots(outputDir)` guard that **skips** already-populated directories. The plan deletes the old `*-webview/` directories before regeneration so this guard does the right thing.

### Open question (worth confirming before regenerating)

- The current `outPoint - 1` clamp in both the generator (`WebViewScreenshotGenerator.java:128`) and the test (`CompareFxViewWithWebViewTest.java:236`) assumes `lottie-web`'s exclusive-`op` semantics. Verify thorvg/dotlottie-web behaves identically — `instance.totalFrames` is typically `op - ip`, so frame `totalFrames - 1` is the last renderable. Confirm before deleting old refs.

## Implementation Steps

### Step 1 — Pin and document the renderer version

- Decide on an exact `@lottiefiles/dotlottie-wc` version (e.g. `0.7.x` at time of writing). Both files must reference the same version, so define it in one place.
- `lottie4j/fxfileviewer/src/main/java/com/lottie4j/fxfileviewer/component/LottieWebView.java` — add a constant near `DEFAULT_SIZE`:
  ```java
  private static final String DOTLOTTIE_WC_VERSION = "X.Y.Z";
  private static final String DOTLOTTIE_WC_URL =
      "https://unpkg.com/@lottiefiles/dotlottie-wc@" + DOTLOTTIE_WC_VERSION + "/dist/dotlottie-wc.js";
  ```
- `lottie4j/fxfileviewer/src/test/java/com/lottie4j/fxfileviewer/WebViewScreenshotGenerator.java` — mirror the same constants (they live in different modules' source roots, so a small duplication is acceptable; alternatively expose them from a shared test util).

### Step 2 — Rewrite the HTML/JS shim in `LottieWebView.loadLottieRaw`

- File: `lottie4j/fxfileviewer/src/main/java/com/lottie4j/fxfileviewer/component/LottieWebView.java:357` (the `loadLottieRaw` method, lines ~357–470).
- Replace the `<script src="…lottie.min.js">` tag with `<script type="module" src="$DOTLOTTIE_WC_URL"></script>`.
- Replace the `<div id="lottie-container"></div>` + `lottie.loadAnimation({...})` block with:
  ```html
  <dotlottie-wc id="player"
                src="data:application/json;base64,%s"
                style="width:%spx;height:%spx;display:block"
                autoplay="false"
                loop="false">
  </dotlottie-wc>
  ```
- Rewrite the `window.*` JS bridge functions on top of `dotlottie-wc`'s imperative API:
  - `window.isAnimationReady` ← set by `player.addEventListener('ready', …)`.
  - `window.getCurrentFrame` ← `Math.round(instance.currentFrame)`.
  - `window.playAnimation` ← `instance.setLoop(true); instance.play();`.
  - `window.playAnimationOnce` ← `instance.setLoop(false); instance.setFrame(0); instance.play();`.
  - `window.pauseAnimation` ← `instance.pause();`.
  - `window.stopAnimation` ← `instance.stop();` (or `setFrame(0) + pause()` if `stop` is absent).
  - `window.seekToFrame(f)` ← `instance.setFrame(Math.round(f));` then `instance.pause();` so the frame holds for screenshot capture.
  - `window.setBackgroundColor(color)` ← either `instance.setBackgroundColor(color)` (preferred — exposed on the dotLottie player) or fall back to `document.body.style.backgroundColor = color`.
  - `window.setDebugOverlayVisible(visible)` and `window.getRenderDebug` — keep as-is, just update the banner text to say `renderer: dotlottie-wc/thorvg @ <version>`.
- Acquiring the `instance`: listen for the `ready` event on the custom element; assign `window.dotLottiePlayer = e.detail.dotLottie` (API name to verify when implementing — see Reference Examples below).
- Keep the existing `setSize(width, height)` and the `<style>` block (the data: URL flow doesn't change the JavaFX sizing dance).

### Step 3 — Rewrite the HTML/JS shim in `WebViewScreenshotGenerator.writeHtml`

- File: `lottie4j/fxfileviewer/src/test/java/com/lottie4j/fxfileviewer/WebViewScreenshotGenerator.java:201` (`writeHtml` and the inline JS).
- Apply the same `<dotlottie-wc>` + ES-module-import rewrite as Step 2.
- Selenium readiness wait (`WebViewScreenshotGenerator.java:148`):
  ```java
  new WebDriverWait(driver, Duration.ofSeconds(25))
      .until(d -> Boolean.TRUE.equals(
          ((JavascriptExecutor) d).executeScript(
              "return window.isAnimationReady && window.isAnimationReady()")));
  ```
  remains identical — only the meaning of `isAnimationReady` changes.
- Frame-sync polling (lines 159–166) stays identical — `window.getCurrentFrame` is the contract; only its implementation moves to `instance.currentFrame`.
- Consider increasing the per-frame `Thread.sleep(100)` (line 167) marginally if thorvg's first-frame settle is slower; calibrate empirically.

### Step 4 — Delete stale reference PNGs

- Remove every committed `*-webview/` directory under `lottie4j/fxfileviewer/src/test/resources/`:
  ```
  lottie4j/fxfileviewer/src/test/resources/json/*-webview/
  lottie4j/fxfileviewer/src/test/resources/dot/*-webview/
  ```
- These are regenerated by running `WebViewScreenshotGenerator.main` (see Verification).
- Do **not** delete `interactive_mood_selector_ui-webviewer.png` — that's the user-supplied ground truth from the official preview and the whole point of comparison.

### Step 5 — Regenerate reference PNGs

- Run `WebViewScreenshotGenerator.main` against the full `lottieJsonFiles()` list.
- Sanity-check disk size per the existing runbook in the class Javadoc (`du -sh src/test/resources/json src/test/resources/dot`, expected `< 500 MB`).
- Spot-check `interactive_mood_selector_ui-webview/frame_0.png` (and a mid-animation frame) against `interactive_mood_selector_ui-webviewer.png` — confirm the gradients are now soft.

### Step 6 — Recalibrate `PER_FILE_FLOOR_OVERRIDE`

- File: `lottie4j/fxfileviewer/src/test/java/com/lottie4j/fxfileviewer/CompareFxViewWithWebViewTest.java:91` (`PER_FILE_FLOOR_OVERRIDE`) and `:122` (`PER_FILE_FLOOR_OVERRIDE_HALF`).
- Run `CompareFxViewWithWebViewTest` against the freshly-generated references.
- For each file the test now fails on (assertion line `:341`), follow the existing pattern: add an entry with `observed average − ~0.5pt` safety margin, and an inline comment carrying the observed average for trend tracking.
- For files that previously had an override but now meet `TARGET_AVERAGE_SIMILARITY = 99.5`, delete the entry (this is the existing "drive to ≥ target ⇒ remove from map" workflow already documented in the Javadoc above the map).

### Step 7 — Update the Javadoc / runbook

- `LottieWebView.java:28` — already says "dotlottie-wc"; add an `@implNote` referencing the pinned version and the `data:` URL approach.
- `WebViewScreenshotGenerator.java:39` — current Javadoc says *"using the dotlottie-wc library"* (line ~50 of the class Javadoc — verify exact location). Tweak wording: *"This was previously lottie-web canvas; switched to dotlottie-wc (thorvg) to match the LottieFiles online preview and to correctly render gradients-with-alpha."*
- `CompareFxViewWithWebViewTest.java:39–62` Javadoc — note the reference renderer is now thorvg via dotlottie-wc.

### Step 8 — (optional follow-up, not in this PR) parametrise the renderer

Once the dotlottie-wc path is the default, a second pass could re-introduce `lottie-web` SVG as an opt-in fallback for cross-renderer A/B debugging. Keep this out of the current change to keep the diff focused.

## Reference Examples

- `lottie4j/fxfileviewer/src/main/java/com/lottie4j/fxfileviewer/component/LottieWebView.java:357` — the existing `loadLottieRaw` HTML template; mirror the data-URL/Base64 pattern (`encodedJson` is already on hand at line ~362) when pointing `<dotlottie-wc src>` at the JSON.
- `lottie4j/fxfileviewer/src/test/java/com/lottie4j/fxfileviewer/WebViewScreenshotGenerator.java:201` — the test-side HTML template; keep its `window.*` bridge contract identical so the surrounding Selenium code at lines 148–167 doesn't change.
- `lottie4j/fxfileviewer/src/test/java/com/lottie4j/fxfileviewer/CompareFxViewWithWebViewTest.java:88` — existing per-file-override comment style for `PER_FILE_FLOOR_OVERRIDE`; reuse that format verbatim when recalibrating in Step 6.
- `lottie4j/fxfileviewer/src/test/resources/json/interactive_mood_selector_ui-webviewer.png` — ground-truth comparison target. New `interactive_mood_selector_ui-webview/frame_*.png` outputs should visually match this (gradients soft, halos translucent).

## Verification

1. **Unit test (unchanged contract):**
   ```
   mvn -pl fxfileviewer test -Dtest=WebViewScreenshotGeneratorTest
   ```
   Should still pass without modification — only HTML output changed, not `buildSampledFrames` semantics.

2. **Regenerate references:** delete `lottie4j/fxfileviewer/src/test/resources/{json,dot}/*-webview/` then:
   ```
   mvn -pl fxfileviewer test-compile exec:java \
       -Dexec.classpathScope=test \
       -Dexec.mainClass=com.lottie4j.fxfileviewer.WebViewScreenshotGenerator
   ```
   (Or run `WebViewScreenshotGenerator.main` from the IDE.) Expect no `FAILED` log lines.

3. **Visual spot-check (manual, the user's original concern):**
   - Open `lottie4j/fxfileviewer/src/test/resources/json/interactive_mood_selector_ui-webview/frame_0.png` and compare side-by-side with `…/interactive_mood_selector_ui-webviewer.png`. Halos should fade smoothly, not hard-edged.
   - Repeat for one mid-animation frame to confirm motion/keyframes look right.

4. **Comparison test, single-file mode** (focused AI-loop iteration):
   ```
   mvn -pl fxfileviewer test \
       -Dtest=CompareFxViewWithWebViewTest \
       -Dlottie.file=json/interactive_mood_selector_ui.json
   ```
   Read the average / min similarity log line and decide whether an override entry is needed.

5. **Full comparison test:**
   ```
   mvn -pl fxfileviewer test -Dtest=CompareFxViewWithWebViewTest
   ```
   Iterate Step 6 (calibrate overrides) until green.

6. **Interactive viewer smoke test:** launch `LottieFileSimpleViewer` (or whichever entry point uses `LottieWebView`) and load `interactive_mood_selector_ui.json` manually — confirm the live JavaFX WebView now shows soft gradients matching the official preview.
