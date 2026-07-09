# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

Lottie4J is a Java/JavaFX library for parsing and rendering [Lottie](https://lottiefiles.com/what-is-lottie) animation
files. It is a multi-module Maven build with three JPMS modules:

* `core` — the Lottie JSON data model (Jackson 3, `tools.jackson.databind`), file loading/saving   (`LottieFileLoader`/`LottieFileSaver`), and shared helpers (keyframe/bezier deserialization, easing). No JavaFX dependency.
* `fxplayer` — `LottiePlayer`, a JavaFX `Canvas` component that plays an `Animation` by driving an `AnimationTimer` and dispatching per-frame drawing to a tree of layer/shape renderers. Depends on `core`.
* `fxfileviewer` — demo/debug JavaFX application (`Launcher` main class) that visualizes a Lottie file's layer structure, plus the project's visual-regression test harness (see below). Depends on `core` and `fxplayer`.

Java 21 is the minimum runtime for library consumers; the build itself now requires **Java 25** (see Testing below — headless JavaFX 26 needs it).

## Build, test, run

```bash
# Full build (skips tests)
mvn -Dmaven.test.skip=true install

# Run all tests headless (what CI runs — requires JDK 25, uses JavaFX 26 for headless glass platform)
mvn test -Pheadless-tests

# Run tests normally (non-headless, needs a real display; uses javafx.version=21.0.7 from the parent pom)
mvn test

# Run a single test class
mvn -pl fxplayer test -Dtest=GradientStopParserTest

# Run a single Lottie file through the visual-regression comparison test (see below)
mvn -pl fxfileviewer test \
    -Dtest=CompareFxViewWithWebViewTest \
    -Dlottie.file=json/angry_bird.json

# Launch the demo/debug viewer app
mvn -pl fxfileviewer javafx:run

# Validate JavaDoc (part of CI's build job)
mvn javadoc:javadoc
```

The `headless-tests` Maven profile bumps `javafx.version` to 26 and sets `-Dglass.platform=headless --enable-native-access=javafx.graphics`, which is what lets JavaFX run without a display in CI (`ubuntu-latest` GitHub Actions runners). Without the profile, tests need a real windowing system and JavaFX 21.0.7.

## Releasing

Releases are triggered manually via `.github/workflows/release.yml` (`workflow_dispatch`, takes a version string like `1.0.0`), not tied to the `pom.xml` SNAPSHOT version — the workflow does `mvn versions:set` before publishing. There is no automated semver tooling; the maintainer picks the next version by hand based on what changed since the last tag. After releasing: update the website's `index.md`/`releases.md`, tag on GitHub, and copy those release notes into the GitHub release description.

## Architecture: the rendering pipeline (`fxplayer`)

`LottiePlayer` (a `Canvas`) is the composition root: it owns one instance of each renderer (`ImageRenderer`, `TextRenderer`, `TransformApplier`, `PrecompRenderer`, `ShapeGroupRenderer` + `ShapeRendererFactory`, `SolidColorRenderer`, `EffectsRenderer`, `MatteRenderer`, `MaskRenderer`) and wires them together itself — there is no DI framework. Each `AnimationTimer` tick:

1. Computes the current frame via `FrameTiming`, walks `Animation`'s layers (indexed by `layersByIndex`, with `Asset`s in `assetsById` for precomps), and skips layers hidden by `LayerActivity`/in/out points.
2. For each visible layer, `TransformApplier` applies the layer transform, then dispatches by `LayerType` to the matching renderer in `renderer/layer/` (`PrecompRenderer` recurses into nested compositions; `ShapeGroupRenderer` walks shape trees, delegating individual shapes to `renderer/shape/*Renderer` classes chosen by `ShapeRendererFactory`).
3. `MatteRenderer` and `MaskRenderer` composite track mattes / vector masks; `EffectsRenderer` applies layer effects (e.g. blurs) — these all work through `OffscreenRenderer`-backed off-screen buffers since JavaFX's `Canvas` has no native layer/group compositing.
4. `LottiePlayer` supports an adaptive off-screen render scale (`MIN/MAX_ADAPTIVE_OFFSCREEN_SCALE`,
   scale-down/up step constants) that trades resolution for frame rate under load.

Gradients (`element/GradientFillStyle`, `GradientStrokeStyle`, `GradientStopParser`) and easing (`core/helper/BezierEasing`, ported byte-for-byte from lottie-web's `BezierEaser`) get special attention because JavaFX's native gradient/interpolation behavior differs subtly from browser renderers — see the testing section below for how that gap is measured and tracked.

## Architecture: visual-regression testing (`fxfileviewer`)

This is the most important non-obvious piece of the codebase. There is no independent "ground truth" renderer to assert against — correctness is defined by **matching the reference Lottie renderer**, `@lottiefiles/dotlottie-wc` (thorvg), driven headless via Selenium/Chrome. Key pieces:

* `WebViewScreenshotGenerator` (test-scope, run manually) renders every registered Lottie file through `DotLottieFrameRenderer` (the same code path used by the live `LottieWebView` debug component) and writes reference PNGs to `src/test/resources/**/*-webview/frame_N.png`. Run it whenever a new fixture file is added or the reference renderer changes.
* `CompareFxViewWithWebViewTest` renders the same files with the real JavaFX `LottiePlayer`, compares each frame against the committed reference PNGs using `ImageSimilarity` (color-aware, alpha-aware, windowed SSIM), and asserts against a similarity floor. It never starts a WebView itself, so it's safe headless in CI. If no reference images exist for a file, the test is skipped — generate them first.
* Per-file similarity floors are tracked in `CompareFxViewWithWebViewTest`'s `PER_FILE_FLOOR_OVERRIDE` map. Each entry is **known technical debt**: the long-term target is `TARGET_PER_FRAME_SIMILARITY` / `TARGET_AVERAGE_SIMILARITY` (99.5%) for every file, and an entry should be removed once its file reaches that bar. Floors follow `floor = floor(observed * 10) / 10 - 0.1` so a small regression trips the build rather than silently eroding fidelity further.
* `.prompts/done/*.md` holds the history of prior investigations into specific rendering-fidelity gaps (e.g. `Fix-renderer-luma-matte-rec601.md`, `Fix-renderer-gradient-fill-channels.md`). These document hypotheses already tried and ruled out for specific files/layer types — check here before re-investigating a similarity gap so you don't repeat dead ends. Comments in `CompareFxViewWithWebViewTest` above individual `PER_FILE_FLOOR_OVERRIDE` entries often cross-reference the relevant plan file directly.
* Reference PNGs are re-encoded at `Deflater.BEST_COMPRESSION` via `ImageSaver` to bound repo size; sanity check `du -sh` on the resource dirs before committing newly regenerated references (expected < 500 MB).

When chasing a rendering-fidelity bug: reproduce with the single-file `-Dlottie.file=...` test run above, consult `.prompts/done/` for prior art on that layer/effect type, and treat `MatteRenderer`, gradient handling, and blur/easing as the areas most likely to diverge from thorvg's output.