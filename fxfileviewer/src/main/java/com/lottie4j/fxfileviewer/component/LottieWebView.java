package com.lottie4j.fxfileviewer.component;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lottie4j.core.helper.ObjectMapperFactory;
import com.lottie4j.core.model.animation.Animation;
import com.lottie4j.core.model.dot.Manifest;
import com.lottie4j.fxfileviewer.render.DotLottieFrameRenderer;

import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import tools.jackson.databind.ObjectMapper;

/**
 * A JavaFX component that renders Lottie animations by driving the
 * {@code @lottiefiles/dotlottie-wc} Web Component (thorvg) out-of-process in headless
 * Chrome and displaying the resulting PNG frames in an {@link ImageView}.
 *
 * <p>JavaFX's bundled WebKit is older than the Chrome that the LottieFiles online preview
 * uses, and it does not reliably load ES modules + WebAssembly from {@code loadContent()}
 * — so we render the animation in headless Chrome via Selenium (which gives pixel-for-pixel
 * parity with the committed reference PNGs) and ship the rendered frames into JavaFX.</p>
 *
 * <p>Frames are pre-rendered into an in-memory cache on every {@code loadLottie*} call.
 * Playback ({@link #play()}, {@link #playOnce()}, {@link #pause()}, {@link #setFrame(int)})
 * runs entirely against that cache via a JavaFX {@link AnimationTimer} at the animation's
 * declared FPS.</p>
 *
 * <p>This class is not thread-safe; all public methods are expected to be invoked from the
 * JavaFX application thread. Background pre-rendering happens on a dedicated worker thread
 * that the class manages internally.</p>
 *
 * <p><strong>Implementation note:</strong> public method signatures (and the
 * {@code window.*}-style semantics implied by {@link #waitUntilReady}/{@link #waitUntilFrame})
 * are preserved from the previous {@code javafx.scene.web.WebView}-backed implementation so
 * existing callers ({@code LottieFileDebugViewer}, etc.) compile and behave unchanged.</p>
 */
public class LottieWebView extends StackPane {
    private static final Logger logger = LoggerFactory.getLogger(LottieWebView.class);
    private static final ObjectMapper OBJECT_MAPPER = ObjectMapperFactory.getInstance();
    private static final int DEFAULT_SIZE = 500;

    /**
     * Pinned version of the {@code @lottiefiles/dotlottie-wc} Web Component used as the
     * rendering engine. Delegates to {@link DotLottieFrameRenderer#DOTLOTTIE_WC_VERSION}.
     */
    public static final String DOTLOTTIE_WC_VERSION = DotLottieFrameRenderer.DOTLOTTIE_WC_VERSION;

    /**
     * Fully-qualified unpkg URL for the pinned {@code dotlottie-wc} ES module. Delegates to
     * {@link DotLottieFrameRenderer#DOTLOTTIE_WC_URL}.
     */
    public static final String DOTLOTTIE_WC_URL = DotLottieFrameRenderer.DOTLOTTIE_WC_URL;

    private final ImageView imageView;
    private final Label statusOverlay;

    // Frame cache + playback state. All mutation is on the FX thread except the writes from
    // the background renderer thread, which are guarded via Platform.runLater for visibility.
    private final List<Image> frameCache = new ArrayList<>();
    private int inPoint;
    private int outPoint;
    private int framesPerSecond = 30;
    private int currentFrame;
    private boolean ready;
    private boolean playing;
    private boolean loop;
    private Color backgroundColor = Color.WHITE;
    private boolean debugInfoVisible;

    // Background rendering. The renderer + its thread are kept alive across loads so we only
    // pay ChromeDriver's startup cost once per LottieWebView lifetime.
    private DotLottieFrameRenderer renderer;
    private Thread renderThread;
    private final AtomicBoolean cancelRender = new AtomicBoolean(false);
    private final AnimationTimer playbackTimer;
    private long playbackStartNanos;
    private int playbackStartFrame;

    /**
     * Constructs a new LottieWebView with default size (500x500 pixels).
     */
    public LottieWebView() {
        this.imageView = new ImageView();
        this.imageView.setSmooth(true);
        this.imageView.setPreserveRatio(false);
        this.imageView.setFitWidth(DEFAULT_SIZE);
        this.imageView.setFitHeight(DEFAULT_SIZE);

        this.statusOverlay = new Label("Initializing renderer\u2026");
        this.statusOverlay.setStyle(
                "-fx-background-color: rgba(255, 255, 200, 0.92);"
                        + "-fx-text-fill: #111111;"
                        + "-fx-font-family: 'Menlo', 'Monaco', monospace;"
                        + "-fx-font-size: 11px;"
                        + "-fx-padding: 6 8 6 8;"
                        + "-fx-border-color: rgba(0, 0, 0, 0.25);"
                        + "-fx-border-radius: 4;"
                        + "-fx-background-radius: 4;");
        this.statusOverlay.setWrapText(true);
        this.statusOverlay.setMaxWidth(Double.MAX_VALUE);
        StackPane.setAlignment(this.statusOverlay, Pos.TOP_LEFT);
        StackPane.setMargin(this.statusOverlay, new javafx.geometry.Insets(6));

        getChildren().addAll(this.imageView, this.statusOverlay);
        setPrefSize(DEFAULT_SIZE, DEFAULT_SIZE);
        setMaxSize(DEFAULT_SIZE, DEFAULT_SIZE);
        applyBackgroundStyle();

        this.playbackTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                tickPlayback(now);
            }
        };
    }

    /**
     * Gets the current frame number of the animation.
     *
     * @return the current frame number, or -1 if unavailable
     */
    public int getCurrentFrame() {
        return ready ? currentFrame : -1;
    }

    /**
     * Gets debug information from the renderer.
     *
     * @return a string containing debug information, or empty string if unavailable
     */
    public String getRenderDebug() {
        return "renderer: dotlottie-wc/thorvg @ " + DOTLOTTIE_WC_VERSION
                + ", ready: " + ready
                + ", frame: " + currentFrame
                + ", cachedFrames: " + frameCache.size();
    }

    /**
     * Waits until the animation is ready to be played (all frames pre-rendered).
     *
     * @param timeoutMs the maximum time to wait in milliseconds
     * @return true if the animation became ready within the timeout, false otherwise
     * @throws InterruptedException if the thread is interrupted while waiting
     */
    public boolean waitUntilReady(long timeoutMs) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            if (ready) {
                return true;
            }
            waitForFxPulse();
        }
        logger.warn("LottieWebView did not become ready in {}ms. {}", timeoutMs, getRenderDebug());
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

    private void waitForFxPulse() throws InterruptedException {
        CountDownLatch pulse = new CountDownLatch(1);
        Platform.runLater(pulse::countDown);
        pulse.await();
    }

    /**
     * Seeks the animation to a specific frame. No-op while frames are still being rendered
     * beyond the requested index.
     *
     * @param frame the frame number to seek to
     */
    public void setFrame(int frame) {
        currentFrame = clampToCache(frame);
        showCachedFrame(currentFrame);
    }

    /**
     * Sets the size of the rendered output.
     *
     * @param width  the width in pixels
     * @param height the height in pixels
     */
    public void setSize(int width, int height) {
        setPrefSize(width, height);
        setMaxSize(width, height);
        setMinSize(width, height);
        imageView.setFitWidth(width);
        imageView.setFitHeight(height);
    }

    /**
     * Starts playing the animation in a continuous loop.
     */
    public void play() {
        if (!ready || frameCache.isEmpty()) {
            return;
        }
        loop = true;
        startTimer(currentFrame);
    }

    /**
     * Plays the animation once from start to finish without looping.
     */
    public void playOnce() {
        if (!ready || frameCache.isEmpty()) {
            return;
        }
        loop = false;
        startTimer(inPoint);
    }

    /**
     * Pauses the animation at the current frame.
     */
    public void pause() {
        playing = false;
        playbackTimer.stop();
    }

    /**
     * Loads a Lottie animation into the renderer.
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
     * Loads a Lottie animation and optionally shows a debug status overlay.
     *
     * @param animation     the Lottie animation to load
     * @param width         the width of the player in pixels
     * @param height        the height of the player in pixels
     * @param showDebugInfo if true, displays a status overlay with rendering information
     */
    public void loadLottie(Animation animation, int width, int height, boolean showDebugInfo) {
        try {
            var lottieJson = OBJECT_MAPPER.writeValueAsString(animation);
            loadLottieRaw(lottieJson, animation, width, height, showDebugInfo);
        } catch (Exception e) {
            logger.error("Failed to serialize Animation for renderer: {}", e.getMessage());
        }
    }

    /**
     * Loads a Lottie animation from a raw JSON string.
     *
     * @param rawJson the Lottie JSON string
     * @param width   the width of the player in pixels
     * @param height  the height of the player in pixels
     */
    public void loadLottie(String rawJson, int width, int height) {
        loadLottieRaw(rawJson, parseAnimationOrNull(rawJson), width, height, false);
    }

    /**
     * Loads a Lottie animation from a raw JSON string.
     *
     * @param rawJson       the Lottie JSON string
     * @param width         the width of the player in pixels
     * @param height        the height of the player in pixels
     * @param showDebugInfo if true, displays a status overlay
     */
    public void loadLottie(String rawJson, int width, int height, boolean showDebugInfo) {
        loadLottieRaw(rawJson, parseAnimationOrNull(rawJson), width, height, showDebugInfo);
    }

    /**
     * Loads a Lottie animation from a JSON file.
     *
     * @param file   the Lottie JSON file
     * @param width  the width of the player in pixels
     * @param height the height of the player in pixels
     * @throws IOException if the file cannot be read
     */
    public void loadLottie(File file, int width, int height) throws IOException {
        String json = readRawJson(file);
        loadLottieRaw(json, parseAnimationOrNull(json), width, height, false);
    }

    /**
     * Loads a Lottie animation from a {@code .json} or {@code .lottie} file.
     *
     * @param file          the Lottie file
     * @param width         the width of the player in pixels
     * @param height        the height of the player in pixels
     * @param showDebugInfo if true, displays a status overlay
     * @throws IOException if the file cannot be read
     */
    public void loadLottie(File file, int width, int height, boolean showDebugInfo) throws IOException {
        String json = readRawJson(file);
        loadLottieRaw(json, parseAnimationOrNull(json), width, height, showDebugInfo);
    }

    /**
     * Sets the background color shown behind transparent regions of the animation.
     *
     * @param backgroundColor the color to set as the background
     */
    public void setBackgroundColor(Color backgroundColor) {
        this.backgroundColor = backgroundColor;
        applyBackgroundStyle();
    }

    /**
     * Shows or hides the status / debug overlay.
     *
     * @param visible true to show the overlay, false to hide it
     */
    public void setDebugInfoVisible(boolean visible) {
        this.debugInfoVisible = visible;
        updateStatusVisibility();
    }

    /**
     * Shuts down the background ChromeDriver instance. Idempotent. Call from your scene's
     * cleanup path to release the headless browser when the viewer closes.
     */
    public void shutdown() {
        cancelRender.set(true);
        playbackTimer.stop();
        if (renderThread != null) {
            renderThread.interrupt();
            try {
                renderThread.join(2_000);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
        }
        if (renderer != null) {
            renderer.close();
            renderer = null;
        }
    }

    // ── Internal: loading + rendering ─────────────────────────────────────────

    private void loadLottieRaw(String lottieJson, Animation parsedAnimation,
                               int width, int height, boolean showDebugInfo) {
        setSize(width, height);
        this.debugInfoVisible = showDebugInfo;
        this.ready = false;
        this.playing = false;
        this.playbackTimer.stop();
        this.frameCache.clear();
        this.imageView.setImage(null);
        updateStatus("Initializing renderer\u2026");
        updateStatusVisibility();

        // Resolve frame range + FPS from the parsed Animation when available. Fallbacks
        // mirror what LottieFileDebugViewer uses elsewhere.
        if (parsedAnimation != null) {
            this.inPoint = parsedAnimation.inPoint() != null ? parsedAnimation.inPoint() : 0;
            this.outPoint = parsedAnimation.outPoint() != null ? parsedAnimation.outPoint() : 60;
            this.framesPerSecond = parsedAnimation.framesPerSecond() != null
                    ? parsedAnimation.framesPerSecond() : 30;
        } else {
            this.inPoint = 0;
            this.outPoint = 60;
            this.framesPerSecond = 30;
        }
        this.currentFrame = this.inPoint;

        // Cancel any in-flight rendering and kick off a fresh one.
        cancelRender.set(true);
        if (renderThread != null && renderThread.isAlive()) {
            renderThread.interrupt();
        }
        cancelRender.set(false);

        final String jsonForRender = lottieJson;
        final int renderWidth = width;
        final int renderHeight = height;
        final int firstFrame = this.inPoint;
        // Both lottie-web and dotlottie-web/thorvg treat outPoint as exclusive: the last
        // renderable frame is op - 1.
        final int lastFrame = Math.max(firstFrame, this.outPoint - 1);

        renderThread = new Thread(() -> renderAllFrames(
                jsonForRender, renderWidth, renderHeight, firstFrame, lastFrame),
                "lottie-webview-renderer");
        renderThread.setDaemon(true);
        renderThread.start();
    }

    private void renderAllFrames(String lottieJson, int width, int height,
                                 int firstFrame, int lastFrame) {
        try {
            if (renderer == null) {
                logger.info("Starting headless Chrome (this can take a few seconds on first run)…");
                Platform.runLater(() -> updateStatus("Starting headless Chrome…"));
                long t0 = System.currentTimeMillis();
                renderer = new DotLottieFrameRenderer();
                logger.info("ChromeDriver up in {} ms", System.currentTimeMillis() - t0);
            }
            logger.info("Loading animation into dotlottie-wc ({}x{})…", width, height);
            Platform.runLater(() -> updateStatus("Loading animation…"));
            long t1 = System.currentTimeMillis();
            renderer.load(lottieJson, width, height);
            logger.info("dotlottie-wc ready after {} ms", System.currentTimeMillis() - t1);

            int total = Math.max(1, lastFrame - firstFrame + 1);
            logger.info("Rendering {} frames ({}…{})…", total, firstFrame, lastFrame);
            long tFrames = System.currentTimeMillis();
            for (int i = 0; i < total; i++) {
                if (cancelRender.get() || Thread.currentThread().isInterrupted()) {
                    logger.info("Render cancelled after {} frames", i);
                    return;
                }
                int frame = firstFrame + i;
                byte[] png = renderer.renderFrame(frame);
                Image img = new Image(new ByteArrayInputStream(png));
                final int progress = i + 1;
                Platform.runLater(() -> appendFrameAndMaybeShow(img, firstFrame, progress, total));
            }
            logger.info("All {} frames rendered in {} ms", total, System.currentTimeMillis() - tFrames);
            Platform.runLater(this::markReady);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            logger.info("Render thread interrupted");
        } catch (Throwable e) {
            // Catch Throwable (not just Exception) so we also surface things like Errors
            // and Selenium's WebDriverException wrappers that don't extend Exception cleanly.
            logger.error("Background render failed: {}", e.toString(), e);
            Platform.runLater(() -> updateStatus("Render failed: " + e.getClass().getSimpleName()
                    + " — see log"));
        }
    }

    private void appendFrameAndMaybeShow(Image img, int firstFrame, int progress, int total) {
        frameCache.add(img);
        // Show the very first rendered frame immediately so the viewer isn't blank while the
        // rest of the cache fills.
        if (frameCache.size() == 1) {
            currentFrame = firstFrame;
            imageView.setImage(img);
        }
        updateStatus(String.format("Rendering frames\u2026 %d / %d", progress, total));
    }

    private void markReady() {
        ready = true;
        updateStatus(getRenderDebug());
        updateStatusVisibility();
    }

    // ── Internal: playback ────────────────────────────────────────────────────

    private void startTimer(int startFrame) {
        currentFrame = clampToCache(startFrame);
        playbackStartFrame = currentFrame;
        playbackStartNanos = System.nanoTime();
        playing = true;
        showCachedFrame(currentFrame);
        playbackTimer.start();
    }

    private void tickPlayback(long now) {
        if (!playing || frameCache.isEmpty()) {
            return;
        }
        double elapsedSec = (now - playbackStartNanos) / 1_000_000_000.0;
        int frameOffset = (int) Math.floor(elapsedSec * framesPerSecond);
        int range = frameCache.size();
        int relative = playbackStartFrame - inPoint + frameOffset;

        if (loop) {
            if (range > 0) {
                relative = Math.floorMod(relative, range);
            }
        } else if (relative >= range) {
            relative = range - 1;
            playing = false;
            playbackTimer.stop();
        }
        currentFrame = inPoint + relative;
        showCachedFrame(currentFrame);
    }

    private void showCachedFrame(int frame) {
        int idx = frame - inPoint;
        if (idx < 0 || idx >= frameCache.size()) {
            return;
        }
        imageView.setImage(frameCache.get(idx));
        if (debugInfoVisible) {
            updateStatus(getRenderDebug());
        }
    }

    private int clampToCache(int frame) {
        if (frameCache.isEmpty()) {
            return inPoint;
        }
        int max = inPoint + frameCache.size() - 1;
        if (frame < inPoint) return inPoint;
        if (frame > max) return max;
        return frame;
    }

    // ── Internal: UI helpers ──────────────────────────────────────────────────

    private void updateStatus(String message) {
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(() -> statusOverlay.setText(message));
            return;
        }
        statusOverlay.setText(message);
    }

    private void updateStatusVisibility() {
        boolean show = debugInfoVisible || !ready;
        statusOverlay.setVisible(show);
        statusOverlay.setManaged(show);
    }

    private void applyBackgroundStyle() {
        String css = String.format(Locale.ROOT, "-fx-background-color: rgba(%d,%d,%d,%.3f);",
                (int) (backgroundColor.getRed() * 255),
                (int) (backgroundColor.getGreen() * 255),
                (int) (backgroundColor.getBlue() * 255),
                backgroundColor.getOpacity());
        setStyle(css);
    }

    // ── Internal: I/O helpers ─────────────────────────────────────────────────

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

    /**
     * Best-effort parse of a raw JSON string to extract {@code ip}, {@code op}, and
     * {@code fr}. Returns {@code null} (and falls back to defaults) when the JSON can't be
     * parsed as a Lottie animation — the rendering itself doesn't depend on the parsed
     * model, only the playback timing does.
     */
    private static Animation parseAnimationOrNull(String json) {
        try {
            return OBJECT_MAPPER.readValue(json, Animation.class);
        } catch (Exception e) {
            return null;
        }
    }
}
