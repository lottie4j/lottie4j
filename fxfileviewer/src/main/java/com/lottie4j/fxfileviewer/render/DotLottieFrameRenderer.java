package com.lottie4j.fxfileviewer.render;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Duration;
import java.util.Comparator;
import java.util.Base64;
import java.util.Map;
import java.util.Set;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Renders Lottie frames out-of-process via headless Chrome (Selenium) using the pinned
 * {@code @lottiefiles/dotlottie-wc} Web Component (thorvg under the hood).
 *
 * <p>This is the shared rendering path used by both:</p>
 * <ul>
 *   <li>{@code LottieWebView} — the JavaFX live viewer, which pre-renders all frames into
 *       an in-memory cache and displays them via {@code ImageView}. JavaFX's bundled WebKit
 *       does not load ES modules from {@code loadContent()}, so we render in Chrome and ship
 *       the pixels into JavaFX instead.</li>
 *   <li>{@code WebViewScreenshotGenerator} — the one-off reference PNG generator.</li>
 * </ul>
 *
 * <p>Lifecycle: instantiate once per consumer, call {@link #load(String, int, int)} per
 * animation (re-uses the underlying browser session), and {@link #close()} when done.
 * The class is not thread-safe; serialize access from a single worker thread.</p>
 *
 * <p><strong>Implementation note:</strong> the renderer is loaded as an ES module from
 * {@link #DOTLOTTIE_WC_URL}. The animation JSON is passed via a
 * {@code data:application/json;base64,\u2026} URL so no temporary JSON files are needed.</p>
 */
public class DotLottieFrameRenderer implements AutoCloseable {
    private static final Logger logger = LoggerFactory.getLogger(DotLottieFrameRenderer.class);

    /**
     * Pinned version of the {@code @lottiefiles/dotlottie-wc} Web Component.
     */
    public static final String DOTLOTTIE_WC_VERSION = "0.7.1";

    /**
     * Fully-qualified unpkg URL for the pinned {@code dotlottie-wc} ES module.
     */
    public static final String DOTLOTTIE_WC_URL =
            "https://unpkg.com/@lottiefiles/dotlottie-wc@" + DOTLOTTIE_WC_VERSION + "/dist/dotlottie-wc.js";

    private static final Duration READY_TIMEOUT = Duration.ofSeconds(60);
    private static final long FRAME_SEEK_TIMEOUT_MS = 3_000;

    private final ChromeDriver driver;
    private int width;
    private int height;
    private boolean loaded;
    private Path pageFile;
    private Path tempDir;

    /**
     * Creates a new renderer backed by a fresh headless Chrome instance. Throws if Chrome
     * isn't installed or Selenium Manager can't provision ChromeDriver.
     */
    public DotLottieFrameRenderer() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new", "--no-sandbox", "--disable-dev-shm-usage",
                "--disable-gpu", "--hide-scrollbars");
        this.driver = new ChromeDriver(options);
        logger.debug("DotLottieFrameRenderer initialized (dotlottie-wc {})", DOTLOTTIE_WC_VERSION);
    }

    /**
     * Loads a Lottie animation into the headless page at the given dimensions and waits for
     * the renderer to become ready.
     *
     * @param lottieJson the raw Lottie JSON
     * @param width      animation width in CSS pixels (also viewport width)
     * @param height     animation height in CSS pixels (also viewport height)
     * @throws TimeoutException if the renderer doesn't become ready within {@link #READY_TIMEOUT}
     * @throws IOException      if the temporary HTML file can't be written
     */
    public void load(String lottieJson, int width, int height) throws IOException {
        this.width = width;
        this.height = height;
        this.loaded = false;

        String encodedJson = Base64.getEncoder().encodeToString(lottieJson.getBytes(StandardCharsets.UTF_8));
        String html = buildHtml(encodedJson, width, height);

        // Use a file:// URL rather than data:text/html. ES module imports from a null-origin
        // data: document are blocked by Chrome's CORS rules, which prevents dotlottie-wc from
        // loading. Recycle a single temp file per renderer instance.
        if (pageFile == null) {
            if (tempDir == null) {
                tempDir = createPrivateTempDirectory();
            }
            pageFile = Files.createTempFile(tempDir, "lottie-renderer-", ".html");
            pageFile.toFile().deleteOnExit();
        }
        Files.writeString(pageFile, html, StandardCharsets.UTF_8);

        driver.executeCdpCommand("Emulation.setDeviceMetricsOverride", Map.of(
                "width", width,
                "height", height,
                "deviceScaleFactor", 1,
                "mobile", false));
        driver.get(pageFile.toUri().toString());

        try {
            new WebDriverWait(driver, READY_TIMEOUT)
                    .until(d -> Boolean.TRUE.equals(
                            ((JavascriptExecutor) d).executeScript(
                                    "return window.isAnimationReady && window.isAnimationReady()")));
        } catch (TimeoutException te) {
            Object jsErrors = driver.executeScript("return (window.__lottieJsErrors || []).slice();");
            Object state = driver.executeScript(
                    "var p = document.getElementById('lottie-container');"
                            + "return { hasElement: !!p,"
                            + "         elementTag: p ? p.tagName : null,"
                            + "         hasDotLottieProp: !!(p && p.dotLottie),"
                            + "         hasGetter: !!(p && typeof p.getDotLottieInstance==='function'),"
                            + "         customElementDefined: !!customElements.get('dotlottie-wc') };");
            logger.error("dotlottie-wc readiness timeout. JS errors: {}. Element state: {}", jsErrors, state);
            throw te;
        }
        this.loaded = true;
    }

    /**
     * Seeks to {@code frame}, waits until the page reports the requested frame is current,
     * then captures a PNG screenshot of the viewport.
     *
     * @param frame target frame index (clamped by the engine to its valid range)
     * @return PNG bytes of the rendered frame, sized {@code width \u00d7 height}
     * @throws InterruptedException if interrupted while polling for the frame sync
     * @throws IllegalStateException if called before {@link #load(String, int, int)}
     */
    public byte[] renderFrame(int frame) throws InterruptedException {
        if (!loaded) {
            throw new IllegalStateException("renderFrame() called before load()");
        }
        driver.executeScript("window.seekToFrame(" + frame + ")");

        long deadline = System.currentTimeMillis() + FRAME_SEEK_TIMEOUT_MS;
        while (System.currentTimeMillis() < deadline) {
            Object current = driver.executeScript(
                    "return window.getCurrentFrame && window.getCurrentFrame()");
            if (current instanceof Number n && n.intValue() == frame) {
                break;
            }
            Thread.sleep(50);
        }
        Thread.sleep(100); // settle paint before screenshot

        try {
            return driver.getScreenshotAs(OutputType.BYTES);
        } catch (org.openqa.selenium.WebDriverException wde) {
            // Selenium's JdkHttpClient wraps InterruptedException in WebDriverException.
            // Unwrap it so the caller's interrupt-handling path fires correctly.
            if (wde.getCause() instanceof InterruptedException ie) {
                Thread.currentThread().interrupt();
                throw ie;
            }
            throw wde;
        }
    }

    /**
     * Returns the loaded animation width.
     *
     * @return the loaded animation's width in CSS pixels (only valid after {@link #load}).
     */
    public int width() {
        return width;
    }

    /**
     * Returns the loaded animation height.
     *
     * @return the loaded animation's height in CSS pixels (only valid after {@link #load}).
     */
    public int height() {
        return height;
    }

    @Override
    public void close() {
        try {
            driver.quit();
        } catch (Exception e) {
            logger.warn("Failed to quit ChromeDriver cleanly: {}", e.getMessage());
        }
        if (pageFile != null) {
            try {
                Files.deleteIfExists(pageFile);
            } catch (IOException ignored) {
                // best-effort cleanup; deleteOnExit will catch anything left behind
            }
        }
        if (tempDir != null) {
            try {
                try (var paths = Files.walk(tempDir)) {
                    paths.sorted(Comparator.reverseOrder())
                            .forEach(path -> {
                                try {
                                    Files.deleteIfExists(path);
                                } catch (IOException ignored) {
                                    // best-effort cleanup; deleteOnExit covers the page file itself
                                }
                            });
                }
            } catch (IOException ignored) {
                // best-effort cleanup for temp dir
            }
        }
    }

    private static Path createPrivateTempDirectory() throws IOException {
        Path baseDir = Path.of(System.getProperty("user.home"), ".lottie4j", "tmp");
        Files.createDirectories(baseDir);
        Path dir = Files.createTempDirectory(baseDir, "renderer-");

        // Restrict directory permissions when POSIX permissions are supported.
        if (Files.getFileStore(dir).supportsFileAttributeView("posix")) {
            Set<PosixFilePermission> perms = PosixFilePermissions.fromString("rwx------");
            Files.setPosixFilePermissions(dir, perms);
        }

        return dir;
    }

    private static String buildHtml(String encodedJson, int width, int height) {
        return """
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
    }
}
