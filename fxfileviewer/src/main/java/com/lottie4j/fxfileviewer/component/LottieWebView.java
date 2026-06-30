package com.lottie4j.fxfileviewer.component;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lottie4j.core.helper.ObjectMapperFactory;
import com.lottie4j.core.model.animation.Animation;
import com.lottie4j.core.model.dot.Manifest;

import javafx.application.Platform;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import tools.jackson.databind.ObjectMapper;

/**
 * A JavaFX component that renders Lottie animations using a WebView with the
 * {@code @lottiefiles/dotlottie-wc} Web Component (thorvg under the hood).
 * Provides controls for playing, pausing, seeking, and inspecting animation frames.
 * Includes optional debug overlay for troubleshooting rendering issues.
 *
 * @implNote The renderer is loaded as an ES module from a pinned unpkg CDN URL
 *           ({@link #DOTLOTTIE_WC_URL}) so reference screenshots remain reproducible.
 *           Animation JSON is passed via a {@code data:application/json;base64,…}
 *           URL so no temporary files are needed.
 */
public class LottieWebView extends Pane {
    private static final Logger logger = LoggerFactory.getLogger(LottieWebView.class);
    private static final ObjectMapper OBJECT_MAPPER = ObjectMapperFactory.getInstance();
    private static final int DEFAULT_SIZE = 500;

    /**
     * Pinned version of the {@code @lottiefiles/dotlottie-wc} Web Component used as the
     * rendering engine. Keep this in sync with
     * {@code WebViewScreenshotGenerator.DOTLOTTIE_WC_VERSION} so the live JavaFX viewer
     * and the committed Selenium reference PNGs use the same renderer build.
     */
    public static final String DOTLOTTIE_WC_VERSION = "0.7.1";

    /**
     * Fully-qualified unpkg URL for the pinned {@code dotlottie-wc} ES module.
     */
    public static final String DOTLOTTIE_WC_URL =
            "https://unpkg.com/@lottiefiles/dotlottie-wc@" + DOTLOTTIE_WC_VERSION + "/dist/dotlottie-wc.js";

    private final WebEngine webEngine;
    private final WebView webView;

    /**
     * Constructs a new LottieWebView with default size (500x500 pixels).
     * Initializes the WebView and sets up JavaScript error handlers.
     */
    public LottieWebView() {
        webView = new WebView();
        webView.setPrefSize(DEFAULT_SIZE, DEFAULT_SIZE);
        webView.setMaxSize(DEFAULT_SIZE, DEFAULT_SIZE);

        getChildren().add(webView);
        setPrefSize(DEFAULT_SIZE, DEFAULT_SIZE);
        setMaxSize(DEFAULT_SIZE, DEFAULT_SIZE);

        webEngine = webView.getEngine();

    }

    /**
     * Gets the current frame number of the animation.
     *
     * @return the current frame number, or -1 if unavailable
     */
    public int getCurrentFrame() {
        Object frameObj = executeScriptSync("window.getCurrentFrame && window.getCurrentFrame()", -1);
        if (frameObj instanceof Number number) {
            return number.intValue();
        }
        return -1;
    }

    /**
     * Gets debug information from the WebView renderer.
     *
     * @return a string containing debug information, or empty string if unavailable
     */
    public String getRenderDebug() {
        Object debugObj = executeScriptSync("window.getRenderDebug && window.getRenderDebug()", "");
        return debugObj != null ? String.valueOf(debugObj) : "";
    }

    /**
     * Waits until the animation is ready to be played.
     *
     * @param timeoutMs the maximum time to wait in milliseconds
     * @return true if the animation became ready within the timeout, false otherwise
     * @throws InterruptedException if the thread is interrupted while waiting
     */
    public boolean waitUntilReady(long timeoutMs) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            Object readyObj = executeScriptSync("window.isAnimationReady && window.isAnimationReady()", false);
            if (Boolean.TRUE.equals(readyObj)) {
                return true;
            }
            waitForFxPulse();
        }
        logger.warn("WebView animation did not become ready in {}ms. {}", timeoutMs, getRenderDebug());
        return false;
    }

    /**
     * Waits until the animation reaches a specific frame.
     *
     * @param expectedFrame the frame number to wait for
     * @param timeoutMs     the maximum time to wait in milliseconds
     * @return true if the expected frame was reached within the timeout, false otherwise
     * @throws InterruptedException if the thread is interrupted while waiting
     */
    public boolean waitUntilFrame(int expectedFrame, long timeoutMs) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            if (getCurrentFrame() == expectedFrame) {
                return true;
            }
            waitForFxPulse();
        }
        return false;
    }

    /**
     * Executes a JavaScript command synchronously, handling both FX and non-FX threads.
     *
     * @param script   the JavaScript code to execute
     * @param fallback the value to return if execution fails
     * @return the result of script execution, or fallback on error
     */
    private Object executeScriptSync(String script, Object fallback) {
        if (Platform.isFxApplicationThread()) {
            try {
                return webEngine.executeScript(script);
            } catch (Exception e) {
                logger.debug("Script execution failed: {}", e.getMessage());
                return fallback;
            }
        }

        CountDownLatch latch = new CountDownLatch(1);
        final Object[] result = new Object[]{fallback};
        Platform.runLater(() -> {
            try {
                result[0] = webEngine.executeScript(script);
            } catch (Exception e) {
                logger.debug("Script execution failed: {}", e.getMessage());
            } finally {
                latch.countDown();
            }
        });

        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return fallback;
        }
        return result[0];
    }

    /**
     * Waits for the next JavaFX pulse cycle to complete.
     *
     * @throws InterruptedException if the thread is interrupted while waiting
     */
    private void waitForFxPulse() throws InterruptedException {
        CountDownLatch pulse = new CountDownLatch(1);
        Platform.runLater(pulse::countDown);
        pulse.await();
    }

    /**
     * Seeks the animation to a specific frame.
     *
     * @param frame the frame number to seek to
     */
    public void setFrame(int frame) {
        try {
            webEngine.executeScript("window.seekToFrame(" + frame + ")");
            webView.requestLayout();
        } catch (Exception e) {
            logger.warn("Failed to seek JS animation: {}", e.getMessage());
        }
    }

    /**
     * Sets the size of the WebView component.
     *
     * @param width  the width in pixels
     * @param height the height in pixels
     */
    public void setSize(int width, int height) {
        setPrefSize(width, height);
        setMaxSize(width, height);
        setMinSize(width, height);
        webView.setPrefSize(width, height);
        webView.setMaxSize(width, height);
        webView.setMinSize(width, height);
    }

    /**
     * Starts playing the animation in a continuous loop.
     */
    public void play() {
        try {
            webEngine.executeScript("window.playAnimation()");
        } catch (Exception e) {
            logger.warn("Failed to start JS animation: {}", e.getMessage());
        }
    }

    /**
     * Plays the animation once from start to finish without looping.
     */
    public void playOnce() {
        try {
            webEngine.executeScript("window.playAnimationOnce()");
        } catch (Exception e) {
            logger.warn("Failed to start JS animation once: {}", e.getMessage());
        }
    }

    /**
     * Pauses the animation at the current frame.
     */
    public void pause() {
        try {
            webEngine.executeScript("window.pauseAnimation()");
        } catch (Exception e) {
            logger.warn("Failed to pause JS animation: {}", e.getMessage());
        }
    }

    /**
     * Loads a Lottie animation into the WebView using the {@code dotlottie-wc} Web Component.
     * The {@link Animation} object is serialized back to JSON before loading.
     *
     * @param animation {@link Animation}
     * @param width     the width of the player in pixels
     * @param height    the height of the player in pixels
     */
    public void loadLottie(Animation animation, int width, int height) {
        loadLottie(animation, width, height, false);
    }

    /**
     * Loads a Lottie animation into the WebView and optionally shows an in-WebView debug overlay.
     * The {@link Animation} object is serialized back to JSON before loading.
     *
     * @param animation     the Lottie animation to load
     * @param width         the width of the player in pixels
     * @param height        the height of the player in pixels
     * @param showDebugInfo if true, displays a debug overlay with rendering information
     */
    public void loadLottie(Animation animation, int width, int height, boolean showDebugInfo) {
        try {
            var lottieJson = OBJECT_MAPPER.writeValueAsString(animation);
            try {
                Files.writeString(Path.of("/tmp/lottie-webview-actual.json"), lottieJson);
                logger.info("Debug: Wrote JSON to /tmp/lottie-webview-actual.json");
            } catch (Exception e) {
                logger.warn("Failed to write debug JSON: {}", e.getMessage());
            }
            loadLottieRaw(lottieJson, width, height, showDebugInfo);
        } catch (Exception e) {
            logger.error("Failed to serialize Animation for WebView: {}", e.getMessage());
        }
    }

    /**
     * Loads a Lottie animation from a raw JSON string, bypassing Java object parsing.
     * Use this to rule out parse/serialize round-trip issues when comparing against reference renderers.
     *
     * @param rawJson the Lottie JSON string
     * @param width   the width of the player in pixels
     * @param height  the height of the player in pixels
     */
    public void loadLottie(String rawJson, int width, int height) {
        loadLottieRaw(rawJson, width, height, false);
    }

    /**
     * Loads a Lottie animation from a raw JSON string, bypassing Java object parsing.
     *
     * @param rawJson       the Lottie JSON string
     * @param width         the width of the player in pixels
     * @param height        the height of the player in pixels
     * @param showDebugInfo if true, displays a debug overlay with rendering information
     */
    public void loadLottie(String rawJson, int width, int height, boolean showDebugInfo) {
        loadLottieRaw(rawJson, width, height, showDebugInfo);
    }

    /**
     * Loads a Lottie animation from a JSON file, bypassing Java object parsing.
     * Use this to rule out parse/serialize round-trip issues when comparing against reference renderers.
     *
     * @param file  the Lottie JSON file
     * @param width  the width of the player in pixels
     * @param height the height of the player in pixels
     * @throws IOException if the file cannot be read
     */
    public void loadLottie(File file, int width, int height) throws IOException {
        loadLottieRaw(readRawJson(file), width, height, false);
    }

    /**
     * Loads a Lottie animation from a file, bypassing Java object parsing.
     * Supports both {@code .json} and {@code .lottie} files.
     *
     * @param file          the Lottie file ({@code .json} or {@code .lottie})
     * @param width         the width of the player in pixels
     * @param height        the height of the player in pixels
     * @param showDebugInfo if true, displays a debug overlay with rendering information
     * @throws IOException if the file cannot be read
     */
    public void loadLottie(File file, int width, int height, boolean showDebugInfo) throws IOException {
        loadLottieRaw(readRawJson(file), width, height, showDebugInfo);
    }

    private static String readRawJson(File file) throws IOException {
        String name = file.getName().toLowerCase(Locale.ROOT);
        if (name.endsWith(".json")) {
            return Files.readString(file.toPath(), StandardCharsets.UTF_8);
        }
        if (name.endsWith(".lottie")) {
            return readFirstAnimationFromDotLottie(file);
        }
        throw new IOException("unsupported file type: " + file.getName());
    }

    private static String readFirstAnimationFromDotLottie(File file) throws IOException {
        try (ZipFile zip = new ZipFile(file)) {
            ZipEntry manifestEntry = zip.getEntry("manifest.json");
            if (manifestEntry == null) {
                throw new IOException("missing manifest.json in .lottie archive");
            }
            Manifest manifest;
            try (var stream = zip.getInputStream(manifestEntry)) {
                manifest = OBJECT_MAPPER.readValue(stream, Manifest.class);
            }
            if (manifest.animations() == null || manifest.animations().isEmpty()) {
                throw new IOException("manifest contains no animations");
            }
            String animationId = manifest.animations().getFirst().id();
            ZipEntry animEntry = zip.getEntry("a/" + animationId + ".json");
            if (animEntry == null) {
                animEntry = zip.getEntry("animations/" + animationId + ".json");
            }
            if (animEntry == null) {
                throw new IOException("animation JSON not found for id: " + animationId);
            }
            try (var stream = zip.getInputStream(animEntry)) {
                return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            }
        }
    }

    private void loadLottieRaw(String lottieJson, int width, int height, boolean showDebugInfo) {
        try {
            setSize(width, height);
            var encodedJson = Base64.getEncoder().encodeToString(lottieJson.getBytes(StandardCharsets.UTF_8));

            var html = """
                    <!DOCTYPE html>
                    <html>
                    <head>
                        <meta charset="UTF-8">
                        <style>
                            body {
                                margin: 0;
                                padding: 0;
                                width: %1$spx;
                                height: %2$spx;
                                background-color: #ffffff;
                                overflow: hidden;
                                position: relative;
                            }
                            #lottie-container {
                                width: %1$spx;
                                height: %2$spx;
                                margin: 0;
                                padding: 0;
                                display: block;
                            }
                            #debug-overlay {
                                position: absolute;
                                top: 6px;
                                left: 6px;
                                z-index: 9999;
                                max-width: calc(100%% - 12px);
                                max-height: calc(100%% - 12px);
                                overflow: hidden;
                                white-space: pre-wrap;
                                word-break: break-word;
                                font-family: Menlo, Monaco, monospace;
                                font-size: 10px;
                                line-height: 1.25;
                                color: #111111;
                                background: rgba(255, 255, 200, 0.92);
                                border: 1px solid rgba(0, 0, 0, 0.25);
                                border-radius: 4px;
                                padding: 6px;
                                pointer-events: auto;
                                cursor: copy;
                                user-select: text;
                            }
                        </style>
                        <script type="module" src="%3$s"></script>
                    </head>
                    <body>
                        <dotlottie-wc id="lottie-container"
                                      src="data:application/json;base64,%6$s"
                                      style="width:%1$spx;height:%2$spx;display:block">
                        </dotlottie-wc>
                        <div id="debug-overlay">Initializing...</div>
                        <script>
                            var animationReady = false;
                            var currentFrame = 0;
                            var showDebugInfo = %5$s;
                            var debugOverlay = document.getElementById('debug-overlay');
                            var rendererBanner = 'renderer: dotlottie-wc/thorvg @ %4$s';

                            if (debugOverlay) {
                                debugOverlay.style.display = showDebugInfo ? 'block' : 'none';
                            }

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

                            function updateDebugOverlay() {
                                if (!debugOverlay || !showDebugInfo) return;
                                debugOverlay.textContent = [
                                    rendererBanner,
                                    'ready: ' + animationReady,
                                    'frame: ' + window.getCurrentFrame()
                                ].join('\\n');
                            }

                            function markReady(e) {
                                instance = resolveInstance()
                                    || (e && e.detail && (e.detail.dotLottie || e.detail.instance));
                                if (instance) {
                                    try { instance.setLoop(false); } catch (err) {}
                                    try { instance.pause(); } catch (err) {}
                                }
                                animationReady = true;
                                updateDebugOverlay();
                            }

                            if (player) {
                                ['ready', 'load', 'loadComplete', 'dotlottie-ready', 'play']
                                    .forEach(function(name) { player.addEventListener(name, markReady); });
                                player.addEventListener('frame', function(e) {
                                    var f = (e && e.detail && typeof e.detail.currentFrame === 'number')
                                        ? e.detail.currentFrame
                                        : (e && typeof e.currentFrame === 'number' ? e.currentFrame : currentFrame);
                                    currentFrame = Math.round(f);
                                    updateDebugOverlay();
                                });
                            }

                            // Polling fallback: events may have fired before this script ran (data:
                            // URL can load synchronously), so resolve from the instance directly
                            // until ready.
                            (function pollReady() {
                                if (animationReady) return;
                                var i = resolveInstance();
                                if (i && (i.totalFrames > 0 || typeof i.currentFrame === 'number')) {
                                    try { i.setLoop(false); } catch (err) {}
                                    try { i.pause(); } catch (err) {}
                                    animationReady = true;
                                    updateDebugOverlay();
                                    return;
                                }
                                setTimeout(pollReady, 50);
                            })();

                            window.isAnimationReady = function() {
                                return animationReady;
                            };

                            window.getCurrentFrame = function() {
                                var i = resolveInstance();
                                if (i && typeof i.currentFrame === 'number') {
                                    return Math.round(i.currentFrame);
                                }
                                return Math.round(currentFrame);
                            };

                            window.playAnimation = function() {
                                var i = resolveInstance();
                                if (!i) return;
                                try { i.setLoop(true); } catch (err) {}
                                try { i.play(); } catch (err) {}
                                updateDebugOverlay();
                            };

                            window.playAnimationOnce = function() {
                                var i = resolveInstance();
                                if (!i) return;
                                try { i.setLoop(false); } catch (err) {}
                                try { i.setFrame(0); } catch (err) {}
                                try { i.play(); } catch (err) {}
                                updateDebugOverlay();
                            };

                            window.pauseAnimation = function() {
                                var i = resolveInstance();
                                if (!i) return;
                                try { i.pause(); } catch (err) {}
                                updateDebugOverlay();
                            };

                            window.stopAnimation = function() {
                                var i = resolveInstance();
                                if (!i) return;
                                if (typeof i.stop === 'function') {
                                    try { i.stop(); } catch (err) {}
                                } else {
                                    try { i.setFrame(0); } catch (err) {}
                                    try { i.pause(); } catch (err) {}
                                }
                                currentFrame = 0;
                                updateDebugOverlay();
                            };

                            window.seekToFrame = function(frame) {
                                var f = Math.round(frame);
                                currentFrame = f;
                                var i = resolveInstance();
                                if (!i) return;
                                try { i.setFrame(f); } catch (err) {}
                                try { i.pause(); } catch (err) {}
                                updateDebugOverlay();
                            };

                            window.setBackgroundColor = function(color) {
                                var i = resolveInstance();
                                if (i && typeof i.setBackgroundColor === 'function') {
                                    try { i.setBackgroundColor(color); } catch (err) {}
                                }
                                document.body.style.backgroundColor = color;
                            };

                            window.setDebugOverlayVisible = function(visible) {
                                showDebugInfo = !!visible;
                                if (debugOverlay) {
                                    debugOverlay.style.display = showDebugInfo ? 'block' : 'none';
                                }
                                updateDebugOverlay();
                            };

                            window.getRenderDebug = function() {
                                return rendererBanner
                                    + ', ready: ' + animationReady
                                    + ', frame: ' + window.getCurrentFrame();
                            };
                        </script>
                    </body>
                    </html>
                    """.formatted(width, height, DOTLOTTIE_WC_URL, DOTLOTTIE_WC_VERSION,
                            showDebugInfo, encodedJson);

            webEngine.loadContent(html);
        } catch (Exception e) {
            logger.error("Failed to load Lottie file in WebView: {}", e.getMessage());
        }
    }

    /**
     * Sets the background color of the WebView animation container.
     *
     * @param backgroundColor the color to set as the background
     */
    public void setBackgroundColor(Color backgroundColor) {
        try {
            var colorHex = String.format("#%02X%02X%02X",
                    (int) (backgroundColor.getRed() * 255),
                    (int) (backgroundColor.getGreen() * 255),
                    (int) (backgroundColor.getBlue() * 255));
            webEngine.executeScript("window.setBackgroundColor('" + colorHex + "')");
        } catch (Exception e) {
            logger.warn("Failed to update JS background color: {}", e.getMessage());
        }
    }

    /**
     * Shows or hides the debug overlay in the WebView.
     *
     * @param visible true to show the debug overlay, false to hide it
     */
    public void setDebugInfoVisible(boolean visible) {
        try {
            webEngine.executeScript("window.setDebugOverlayVisible && window.setDebugOverlayVisible(" + visible + ")");
        } catch (Exception e) {
            logger.warn("Failed to toggle JS debug overlay: {}", e.getMessage());
        }
    }
}
