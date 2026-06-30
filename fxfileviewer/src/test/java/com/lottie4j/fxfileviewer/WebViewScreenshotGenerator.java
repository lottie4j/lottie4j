package com.lottie4j.fxfileviewer;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.imageio.ImageIO;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lottie4j.core.file.LottieFileLoader;
import com.lottie4j.core.model.animation.Animation;
import com.lottie4j.fxfileviewer.component.LottieWebView;
import com.lottie4j.fxfileviewer.util.ImageSaver;

/**
 * One-time utility for generating WebView reference screenshots for all Lottie animation files
 * using headless Chrome via Selenium. This doesn't use the JavaFX based webview component, because
 * some animations don't render completely correct in the integrated JavaFX browser.
 * Therefore, this generator uses Selenium to render the animation in a headless Chrome browser.
 *
 * <p>This was previously {@code lottie-web} canvas; switched to {@code @lottiefiles/dotlottie-wc}
 * (thorvg under the hood) to match the LottieFiles online preview and to correctly render
 * gradients-with-alpha (e.g. soft halos in {@code interactive_mood_selector_ui}).</p>
 *
 * <p>Run this class manually whenever a new animation file is added to
 * {@link CompareFxViewWithWebViewTest#lottieJsonFiles()}.
 * The generated PNG images are committed to {@code src/test/resources} and used by
 * {@link CompareFxViewWithWebViewTest} without requiring a live browser at test time.</p>
 *
 * <p>Requires Chrome to be installed. ChromeDriver is managed automatically by Selenium Manager.</p>
 *
 * <p>Output layout: {@code src/test/resources/json/angry_bird-webview/frame_0.png}, etc.</p>
 *
 * <p><b>Runbook</b>: every-frame sampling can produce hundreds of frames per animation. Before
 * committing regenerated references, sanity-check the on-disk size:
 * <pre>
 *   du -sh src/test/resources/json src/test/resources/dot   # expected &lt; 500 MB
 * </pre>
 * Reference PNGs are re-encoded through {@link ImageSaver} at
 * {@link java.util.zip.Deflater#BEST_COMPRESSION} to keep this bounded.</p>
 */
public class WebViewScreenshotGenerator {
    private static final Logger logger = LoggerFactory.getLogger(WebViewScreenshotGenerator.class);

    private static final int DEFAULT_WIDTH = 800;
    private static final int DEFAULT_HEIGHT = 600;

    /**
     * Pinned URL of the {@code @lottiefiles/dotlottie-wc} ES module. Mirrors
     * {@link LottieWebView#DOTLOTTIE_WC_URL} so the live JavaFX viewer and the committed
     * Selenium reference PNGs are produced by the exact same renderer build at version
     * {@link LottieWebView#DOTLOTTIE_WC_VERSION}.
     */
    private static final String DOTLOTTIE_WC_URL = LottieWebView.DOTLOTTIE_WC_URL;

    public static void main(String[] args) throws Exception {
        URL knownResource = WebViewScreenshotGenerator.class.getResource("/json/angry_bird.json");
        if (knownResource == null) {
            throw new IllegalStateException("Could not locate anchor resource json/angry_bird.json");
        }
        Path testResourcesDir = Path.of(knownResource.toURI())
                .getParent()  // json/
                .getParent()  // target/test-classes/
                .getParent()  // target/
                .getParent()  // <module-root>
                .resolve("src/test/resources");

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new", "--no-sandbox", "--disable-dev-shm-usage",
                "--disable-gpu", "--hide-scrollbars");

        logger.info("Renderer: dotlottie-wc/thorvg @ {} ({})",
                LottieWebView.DOTLOTTIE_WC_VERSION, DOTLOTTIE_WC_URL);

        List<String> failures = new ArrayList<>();

        try {
            var driver = new ChromeDriver(options);
            for (String fileName : CompareFxViewWithWebViewTest.lottieJsonFiles().toList()) {
                logger.info("Generating screenshots for: {}", fileName);
                try {
                    generateScreenshotsForFile(fileName, testResourcesDir, driver);
                } catch (Exception e) {
                    logger.error("FAILED {}: {}", fileName, e.getMessage(), e);
                    failures.add(fileName);
                }
            }
            driver.close();
        } catch (Exception e) {
            logger.error("Failed to initialize ChromeDriver: {}", e.getMessage(), e);
            System.exit(1);
        }

        if (failures.isEmpty()) {
            logger.info("All screenshots generated successfully.");
        } else {
            logger.error("Failed to generate screenshots for: {}", failures);
            System.exit(1);
        }
    }

    private static void generateScreenshotsForFile(String fileName, Path testResourcesDir,
                                                   ChromeDriver driver) throws Exception {
        URL resource = WebViewScreenshotGenerator.class.getResource("/" + fileName);
        if (resource == null) {
            throw new IllegalArgumentException("Resource not found: " + fileName);
        }

        Animation animation = LottieFileLoader.load(new File(resource.getFile()));

        int animWidth = animation.width() != null ? animation.width() : DEFAULT_WIDTH;
        int animHeight = animation.height() != null ? animation.height() : DEFAULT_HEIGHT;

        Path outputDir = testResourcesDir.resolve(webViewDirPath(fileName));
        if (Files.isDirectory(outputDir) && hasScreenshots(outputDir)) {
            logger.info("Skipping {} — screenshots already exist in {}", fileName, outputDir);
            return;
        }
        recreateDirectory(outputDir);

        int inPoint = animation.inPoint() != null ? animation.inPoint() : 0;
        int outPoint = animation.outPoint() != null ? animation.outPoint() : 60;
        // Both lottie-web and dotlottie-web/thorvg treat outPoint as exclusive: totalFrames is
        // op - ip and the last renderable frame index is op - 1. Sampling frame == op would
        // produce a reference that disagrees with the JavaFX player (which clamps seekToFrame
        // to op - 1 via getLastRenderableFrame), so we stop at op - 1.
        int lastFrame = Math.max(inPoint, outPoint - 1);
        // Every frame (step=1, inclusive of lastFrame). See CompareFxViewWithWebViewTest for the rationale.
        List<Integer> frames = buildSampledFrames(inPoint, lastFrame, 1);

        String rawJson = readRawJson(new File(resource.getFile()));
        String encodedJson = Base64.getEncoder().encodeToString(rawJson.getBytes(StandardCharsets.UTF_8));

        Path tempHtml = writeHtml(encodedJson, animWidth, animHeight);
        try {
            // Use CDP to set the viewport to exactly the animation size, bypassing outer window chrome
            driver.executeCdpCommand("Emulation.setDeviceMetricsOverride", Map.of(
                    "width", animWidth,
                    "height", animHeight,
                    "deviceScaleFactor", 1,
                    "mobile", false));
            driver.get(tempHtml.toUri().toString());

            // 60s instead of 25s — thorvg + WASM cold-start can be slow for larger JSONs,
            // and dotlottie-wc fetches its WASM blob on first load.
            try {
                new WebDriverWait(driver, Duration.ofSeconds(60))
                        .until(d -> Boolean.TRUE.equals(
                                ((JavascriptExecutor) d).executeScript(
                                        "return window.isAnimationReady && window.isAnimationReady()")));
            } catch (TimeoutException te) {
                Object jsErrors = driver.executeScript(
                        "return (window.__lottieJsErrors || []).slice();");
                Object instanceInfo = driver.executeScript(
                        "var p = document.getElementById('lottie-container');"
                                + "return { hasElement: !!p,"
                                + "         elementTag: p ? p.tagName : null,"
                                + "         hasDotLottieProp: !!(p && p.dotLottie),"
                                + "         hasGetter: !!(p && typeof p.getDotLottieInstance==='function'),"
                                + "         customElementDefined: !!customElements.get('dotlottie-wc') };");
                logger.error("Readiness timeout for {}. JS errors: {}. Element state: {}",
                        fileName, jsErrors, instanceInfo);
                throw te;
            }

            WebElement container = driver.findElement(By.id("lottie-container"));

            for (int frame : frames) {
                driver.executeScript("window.seekToFrame(" + frame + ")");

                long deadline = System.currentTimeMillis() + 3_000;
                while (System.currentTimeMillis() < deadline) {
                    Object current = driver
                            .executeScript("return window.getCurrentFrame && window.getCurrentFrame()");
                    if (current instanceof Number n && n.intValue() == frame) break;
                    Thread.sleep(50);
                }
                Thread.sleep(100);

                byte[] png = driver.getScreenshotAs(OutputType.BYTES);
                // Re-encode through ImageSaver at BEST_COMPRESSION. Most reference frames are
                // simple cartoons and shrink 30–60% — meaningful because every-frame sampling
                // multiplies committed PNG count by ~5 versus the previous step=5.
                writeRecompressedPng(png, outputDir.resolve("frame_" + frame + ".png"));
            }

            logger.info("Generated {} frames for {} → {}", frames.size(), fileName, outputDir);
        } finally {
            Files.deleteIfExists(tempHtml);
        }
    }

    /**
     * Decode a PNG byte buffer and re-encode it through {@link ImageSaver} at
     * {@link Deflater#BEST_COMPRESSION}. Output dimensions and pixel values are identical;
     * only the on-disk byte count changes.
     */
    private static void writeRecompressedPng(byte[] pngBytes, Path target) throws IOException {
        BufferedImage img = ImageIO.read(new ByteArrayInputStream(pngBytes));
        if (img == null) {
            // Fallback: just keep the raw bytes if decoding fails for some reason.
            Files.write(target, pngBytes);
            return;
        }
        int width = img.getWidth();
        int height = img.getHeight();
        int[] argb = img.getRGB(0, 0, width, height, null, 0, width);
        try (FileOutputStream fos = new FileOutputStream(target.toFile())) {
            ImageSaver.writePNG(fos, argb, width, height, Deflater.BEST_COMPRESSION);
        }
    }

    private static Path writeHtml(String encodedJson, int width, int height) throws IOException {
        Path tempFile = Files.createTempFile("lottie-screenshot-", ".html");
        String html = """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <style>
                        * { margin: 0; padding: 0; box-sizing: border-box; }
                        body { width: %1$spx; height: %2$spx; overflow: hidden; background: transparent; }
                        #lottie-container { width: %1$spx; height: %2$spx; display: block; }
                    </style>
                    <script type="module" src="%3$s"></script>
                    <script>
                        window.__lottieJsErrors = [];
                        window.addEventListener('error', function(e) {
                            window.__lottieJsErrors.push(String(e.message || e));
                        });
                        window.addEventListener('unhandledrejection', function(e) {
                            window.__lottieJsErrors.push('promise: ' + String((e && e.reason) || e));
                        });
                    </script>
                    </head>
                    <body>
                    <dotlottie-wc id="lottie-container"
                                  src="data:application/json;base64,%4$s"
                                  style="width:%1$spx;height:%2$spx;display:block">
                    </dotlottie-wc>
                    <script>
                        var animationReady = false;
                        var currentFrame = 0;
                        var player = document.getElementById('lottie-container');
                        var instance = null;

                        function resolveInstance() {
                            if (instance) return instance;
                            try {
                                if (player && typeof player.getDotLottieInstance === 'function') {
                                    instance = player.getDotLottieInstance();
                                }
                            } catch (e) { /* not ready yet */ }
                            if (!instance && player && player.dotLottie) {
                                instance = player.dotLottie;
                            }
                            return instance;
                        }

                        function markReady(e) {
                            instance = resolveInstance()
                                || (e && e.detail && (e.detail.dotLottie || e.detail.instance));
                            if (instance) {
                                try { instance.setLoop(false); } catch (err) {}
                                try { instance.pause(); } catch (err) {}
                            }
                            animationReady = true;
                        }

                        if (player) {
                            ['ready', 'load', 'loadComplete', 'dotlottie-ready', 'play']
                                .forEach(function(name) { player.addEventListener(name, markReady); });
                            player.addEventListener('frame', function(e) {
                                var f = (e && e.detail && typeof e.detail.currentFrame === 'number')
                                    ? e.detail.currentFrame
                                    : (e && typeof e.currentFrame === 'number' ? e.currentFrame : currentFrame);
                                currentFrame = Math.round(f);
                            });
                        }

                        // Polling fallback: events may have fired before this script ran (data: URL
                        // can load synchronously), so resolve from the instance directly until ready.
                        (function pollReady() {
                            if (animationReady) return;
                            var i = resolveInstance();
                            if (i && (i.totalFrames > 0 || typeof i.currentFrame === 'number')) {
                                try { i.setLoop(false); } catch (err) {}
                                try { i.pause(); } catch (err) {}
                                animationReady = true;
                                return;
                            }
                            setTimeout(pollReady, 50);
                        })();

                        window.isAnimationReady = function() { return animationReady; };
                        window.getCurrentFrame = function() {
                            var i = resolveInstance();
                            if (i && typeof i.currentFrame === 'number') {
                                return Math.round(i.currentFrame);
                            }
                            return Math.round(currentFrame);
                        };
                        window.seekToFrame = function(frame) {
                            var f = Math.round(frame);
                            currentFrame = f;
                            var i = resolveInstance();
                            if (!i) return;
                            try { i.setFrame(f); } catch (err) {}
                            try { i.pause(); } catch (err) {}
                        };
                    </script>
                </body>
                </html>
                """.formatted(width, height, DOTLOTTIE_WC_URL, encodedJson);
        Files.writeString(tempFile, html, StandardCharsets.UTF_8);
        return tempFile;
    }

    // ── Shared helpers used by CompareFxViewWithWebViewTest ──────────────────

    /**
     * Derives the classpath-relative webview directory path for a given animation file.
     * Example: {@code "json/angry_bird.json"} → {@code "json/angry_bird-webview"}
     */
    static String webViewDirPath(String fileName) {
        int slash = fileName.lastIndexOf('/');
        String dir = fileName.substring(0, slash);
        String base = fileName.substring(slash + 1).replaceAll("\\.(json|lottie)$", "");
        return dir + "/" + base + "-webview";
    }

    /**
     * Builds a sampled list of frame indices from {@code firstFrame} to {@code lastFrame}
     * (inclusive) stepping by {@code step}, always including the final frame.
     */
    static List<Integer> buildSampledFrames(int firstFrame, int lastFrame, int step) {
        if (step <= 0) step = 1;
        List<Integer> frames = new ArrayList<>();
        for (int f = firstFrame; f <= lastFrame; f += step) {
            frames.add(f);
        }
        if (frames.isEmpty() || frames.get(frames.size() - 1) != lastFrame) {
            frames.add(lastFrame);
        }
        return frames;
    }

    // ── Private utilities ─────────────────────────────────────────────────────

    private static String readRawJson(File file) throws IOException {
        String name = file.getName().toLowerCase(Locale.ROOT);
        if (name.endsWith(".json")) {
            return Files.readString(file.toPath(), StandardCharsets.UTF_8);
        }
        if (name.endsWith(".lottie")) {
            try (ZipFile zip = new ZipFile(file)) {
                ZipEntry manifestEntry = zip.getEntry("manifest.json");
                if (manifestEntry == null) throw new IOException("missing manifest.json in " + file.getName());
                // Find the first animation entry under a/ or animations/
                var entries = zip.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry entry = entries.nextElement();
                    String n = entry.getName();
                    if (!entry.isDirectory() && n.endsWith(".json")
                            && (n.startsWith("a/") || n.startsWith("animations/"))) {
                        return new String(zip.getInputStream(entry).readAllBytes(), StandardCharsets.UTF_8);
                    }
                }
                throw new IOException("no animation JSON found in " + file.getName());
            }
        }
        throw new IOException("unsupported file type: " + file.getName());
    }

    private static boolean hasScreenshots(Path dir) throws IOException {
        try (Stream<Path> paths = Files.list(dir)) {
            return paths.anyMatch(p -> p.toString().endsWith(".png"));
        }
    }

    private static void recreateDirectory(Path dir) throws IOException {
        if (Files.exists(dir)) {
            try (Stream<Path> paths = Files.walk(dir)) {
                paths.sorted((a, b) -> -a.compareTo(b)).forEach(p -> {
                    try {
                        Files.delete(p);
                    } catch (IOException e) {
                        // ignore stale file cleanup failures
                    }
                });
            }
        }
        Files.createDirectories(dir);
    }
}
