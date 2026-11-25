# Contributing to Lottie4J

Thanks for helping out! This page covers getting the code, building it, and the one thing may surprise you: the repository is large because of the reference images used by the
visual-regression tests.

## Requirements

* JDK 25 to build (Java 21 is the minimum runtime for library consumers).
* Maven 3.9+.
* Chrome, only if you want to (re)generate reference images.

## Why is the repository so big?

`fxfileviewer/src/test/resources/**/*-webview/` holds one reference PNG per frame per test animation,
rendered by the reference renderer (`@lottiefiles/dotlottie-wc`, thorvg). Together they are roughly
**1 GB of checkout** (and the git history is larger). They are only needed to run
`CompareFxViewWithWebViewTest`. **If the images are missing, that test is skipped, not failed**, so you
can work on most of the code without them.

## Getting the code

Pick the option that fits what you want to do.

### Option 1: everything (needed to run or change the visual-regression tests)

```bash
git clone https://github.com/lottie4j/lottie4j.git
```

### Option 2: everything except the reference images (recommended for most changes)

A partial clone skips downloading file contents until they are needed, and sparse checkout keeps
the image directories out of your working tree:

```bash
git clone --filter=blob:none --no-checkout https://github.com/lottie4j/lottie4j.git
cd lottie4j
git sparse-checkout set --no-cone '/*' '!/fxfileviewer/src/test/resources/**/*-webview/'
git checkout main
```

You can build and run every test except the image comparison. To add the images later:

```bash
git sparse-checkout disable
```

### Option 3: only the images of one animation

To work on one file, for example `json/angry_bird.json`, keep everything but only that file's images:

```bash
git sparse-checkout set --no-cone '/*' \
    '!/fxfileviewer/src/test/resources/**/*-webview/' \
    '/fxfileviewer/src/test/resources/json/angry_bird-webview/'
git checkout main
```

Then run only that file (see below). Add more `-webview/` directories as needed.

### Option 4: shallow clone (less history, smaller download)

```bash
git clone --depth 1 https://github.com/lottie4j/lottie4j.git
```

This can be combined with option 2 or 3. Run `git fetch --unshallow` if you need the history.

> Tip: `git sparse-checkout list` shows what is active, `git sparse-checkout disable` brings everything back.

## Building and testing

```bash
# Build without tests
mvn -Dmaven.test.skip=true install

# All tests, headless (what CI runs, needs JDK 25)
mvn test -Pheadless-tests

# One test class
mvn -pl fxplayer test -Dtest=GradientStopParserTest

# One animation through the visual-regression test (needs that file's -webview images)
mvn -pl fxfileviewer test -Pheadless-tests \
    -Dtest=CompareFxViewWithWebViewTest -Dlottie.file=json/angry_bird.json

# Validate JavaDoc (part of CI)
mvn javadoc:javadoc
```

See `CLAUDE.md` for the architecture and for how the visual-regression tests work.

## Adding or regenerating reference images

Needed when you add an animation to `CompareFxViewWithWebViewTest.lottieJsonFiles()` or when the
reference renderer changes. Requires the images of all files you regenerate, and Chrome.

1. Add the file to `lottieJsonFiles()` and put it in `fxfileviewer/src/test/resources/`.
2. Run `WebViewScreenshotGenerator` (a test-scope `main`). It skips files whose image directory
   already exists, so delete a directory to regenerate it. Use `-Dlottie.file=<path>` to do one file.
3. Run the comparison for that file and add an entry to `PER_FILE_FLOOR_OVERRIDE` (and the `_HALF`
   map) if it is below the 99.5% target, using `floor = floor(observed * 10) / 10 - 0.1`.
4. Check the size before committing: `du -sh fxfileviewer/src/test/resources/json fxfileviewer/src/test/resources/dot`
   (expected < 1 GB in total).

Every frame is stored, so a long animation is expensive: 1200 frames at 1200×800 is about 126 MB.
Prefer a short fixture, and only add a large one when it earns its place (for example the
winter-town playback benchmark).

## Pull requests

* Keep a change focused, and run `mvn test -Pheadless-tests` before opening the PR.
* A performance change should add a row to `BENCHMARK-LOG.md` (what changed, before, after).
* Do not lower a similarity floor without explaining why in the PR.
