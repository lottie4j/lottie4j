package com.lottie4j.fxfileviewer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lottie4j.core.file.LottieFileLoader;
import com.lottie4j.core.model.animation.Animation;
import com.lottie4j.fxfileviewer.util.ImageSaver;
import com.lottie4j.fxfileviewer.util.ImageSimilarity;
import com.lottie4j.fxplayer.LottiePlayer;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.Image;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

/**
 * Validates JavaFX player rendering against pre-generated WebView reference screenshots.
 *
 * <p>Reference images are produced once by {@link WebViewScreenshotGenerator} and committed
 * to {@code src/test/resources}. This test never starts a WebView and is safe to run in CI
 * headless mode.</p>
 *
 * <p>If no reference images exist for a file, the test is skipped.
 * Run {@link WebViewScreenshotGenerator} locally to create or refresh them.</p>
 *
 * <h2>Quality bar</h2>
 * <p>The long-term per-frame similarity target is
 * {@value #TARGET_PER_FRAME_SIMILARITY}% measured by color-aware, alpha-aware, windowed SSIM
 * (see {@link ImageSimilarity}). Some animations do not meet this bar yet; their current
 * floor is encoded in {@link #PER_FILE_FLOOR_OVERRIDE}. Each entry is visible technical
 * debt — driving any entry to &ge; {@value #TARGET_AVERAGE_SIMILARITY} means it can be
 * deleted from the map.</p>
 *
 * <h2>Single-file runs (AI improvement loops)</h2>
 * <p>To exercise one animation only, set the system property {@code lottie.file}:
 * <pre>
 *   mvn -pl fxfileviewer test \
 *       -Dtest=CompareFxViewWithWebViewTest \
 *       -Dlottie.file=json/angry_bird.json
 * </pre>
 * Empty or unset = full list (default).</p>
 */
class CompareFxViewWithWebViewTest {
    private static final Logger logger = LoggerFactory.getLogger(CompareFxViewWithWebViewTest.class);

    /** Long-term per-frame similarity floor. The bar we are climbing to. */
    private static final double TARGET_PER_FRAME_SIMILARITY = 99.5;

    /** Long-term average similarity floor. Kept separate from the per-frame target so they
     * can be tuned independently if needed. */
    private static final double TARGET_AVERAGE_SIMILARITY = 99.5;

    /**
     * Per-file override of the average-similarity floor for animations that don't yet
     * reach {@link #TARGET_AVERAGE_SIMILARITY}. Entries are technical debt — populated
     * empirically from a calibration run, with a small safety margin below the observed
     * floor. Driving any entry to &ge; the target means it can be removed from this map.
     *
     * <p>The map is intentionally empty by default; populate from CI output after the
     * first calibration run.</p>
     */
    private static final Map<String, Double> PER_FILE_FLOOR_OVERRIDE = Map.ofEntries(
            // Populate empirically on first run; entries are technical debt to drive down.
    );

    /**
     * Per-file override for the half-size variant. Half-size scaling introduces extra
     * bilinear smoothing on the reference image which inherently lowers SSIM, so it
     * needs its own override map.
     */
    private static final Map<String, Double> PER_FILE_FLOOR_OVERRIDE_HALF = Map.ofEntries(
            // Populate empirically on first run.
    );

    private static final int CANVAS_WIDTH = 800;
    private static final int CANVAS_HEIGHT = 600;
    private static Stage primaryStage;

    @BeforeAll
    static void initJavaFX() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        Platform.startup(() -> {
            primaryStage = new Stage();
            latch.countDown();
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS), "JavaFX Platform failed to start");
    }

    /**
     * Provides the set of Lottie animation files to validate.
     *
     * <p>When the system property {@code lottie.file} is set, the stream is filtered to
     * only that entry. This enables single-file AI improvement loops without forking
     * the list.</p>
     */
    static Stream<String> lottieJsonFiles() {
        Stream<String> all = Stream.of(
                "json/angry_bird.json",
                "json/animated_background_patterns.json",
                "json/box-moving-changing-color.json",
                "json/face-exhaling.json",
                "json/face-peeking.json",
                "json/foojay-duke.json",
                "json/foojay-reporter.json",
                "json/isometric_data_analysis.json",
                // "json/java_duke_fadein.json", // Not used, black borders are not merged
                "json/java_duke_flip.json",
                // "json/java_duke_slidein.json", // Not used, animation timing differences
                "json/loading.json",
                "json/lottie4j.json",
                //"json/lottie_lego.json", // Not used, animation timing differences
                "json/pi4j.json",
                "json/sandy_loading.json",
                "json/snake_ladder_loading_animation.json",
                "json/success.json",
                "json/timeline_animation.json",
                "dot/lottie4j.lottie",
                "dot/demo-1.lottie",
                "dot/demo-2.lottie",
                "dot/demo-3.lottie"
        );
        String filter = System.getProperty("lottie.file");
        if (filter != null && !filter.isBlank()) {
            String wanted = filter.trim();
            return all.filter(wanted::equals);
        }
        return all;
    }

    @ParameterizedTest
    @MethodSource("lottieJsonFiles")
    void compareFxAndJsRenderingFullSize(String fileName) throws Exception {
        compareFxWithPreGeneratedImages(fileName, 1.0, PER_FILE_FLOOR_OVERRIDE);
    }

    @Test
    void compareFxAndJsRenderingHalfSize() throws Exception {
        // Half-size variant exercises a single file at 50% scale. Reference images get bilinearly
        // scaled, which inherently lowers SSIM relative to full-size — hence the separate override
        // map.
        compareFxWithPreGeneratedImages(
                lottieJsonFiles().toList().get(1), 0.5, PER_FILE_FLOOR_OVERRIDE_HALF);
    }

    // ── Core comparison logic ─────────────────────────────────────────────────
    private void compareFxWithPreGeneratedImages(String fileName, double scale,
                                                 Map<String, Double> overrides) throws Exception {
        String webviewDir = "/" + WebViewScreenshotGenerator.webViewDirPath(fileName) + "/";

        // Skip if no reference images have been generated yet
        URL probe = getClass().getResource(webviewDir + "frame_0.png");
        Assumptions.assumeTrue(probe != null,
                "No pre-generated WebView images for " + fileName + " — run WebViewScreenshotGenerator first");

        logger.info("Testing: {} at scale {}", fileName, scale);

        URL resource = getClass().getResource("/" + fileName);
        assertNotNull(resource, "Resource not found: " + fileName);
        File file = new File(resource.getFile());

        Animation animation = LottieFileLoader.load(file);
        assertNotNull(animation, "Failed to load animation: " + fileName);
        assertNotNull(animation.layers(), "Null layers: " + fileName);
        assertFalse(animation.layers().isEmpty(), "Empty layers: " + fileName);

        int animWidth = animation.width() != null ? animation.width() : CANVAS_WIDTH;
        int animHeight = animation.height() != null ? animation.height() : CANVAS_HEIGHT;
        int scaledWidth = Math.max(1, (int) Math.round(animWidth * scale));
        int scaledHeight = Math.max(1, (int) Math.round(animHeight * scale));

        int inPoint = animation.inPoint() != null ? animation.inPoint() : 0;
        int outPoint = animation.outPoint() != null ? animation.outPoint() : 60;
        // Every frame (step=1, inclusive of outPoint) — see plan §3. This is 5× the prior
        // sampling rate, so anything other than a deterministic per-frame sync would be
        // unreliable.
        List<Integer> frames = WebViewScreenshotGenerator.buildSampledFrames(
                inPoint, Math.max(inPoint, outPoint), 1);

        Path outputDir = Path.of("target/test-output",
                WebViewScreenshotGenerator.webViewDirPath(fileName) + "_scale_" + scale);
        clearDirectory(outputDir);

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Double> totalSimilarity = new AtomicReference<>(0.0);
        AtomicReference<Double> minSimilarity = new AtomicReference<>(Double.MAX_VALUE);
        AtomicInteger frameCount = new AtomicInteger(0);
        String[] threadError = new String[1];

        Platform.runLater(() -> {
            try {
                LottiePlayer fxPlayer = new LottiePlayer(animation, animWidth, animHeight);
                fxPlayer.resizeRenderPercent(scale * 100.0);
                primaryStage.setScene(new Scene(new StackPane(fxPlayer), scaledWidth, scaledHeight));

                new Thread(() -> {
                    try {
                        for (int frame : frames) {
                            URL imgUrl = getClass().getResource(webviewDir + "frame_" + frame + ".png");
                            if (imgUrl == null) {
                                System.out.printf("SKIP frame %d: no pre-generated image%n", frame);
                                continue;
                            }

                            waitForRender(fxPlayer, frame);

                            // Snapshot FX player
                            WritableImage[] fxImg = new WritableImage[1];
                            CountDownLatch snapLatch = new CountDownLatch(1);
                            Platform.runLater(() -> {
                                fxImg[0] = fxPlayer.snapshot(new SnapshotParameters(), null);
                                snapLatch.countDown();
                            });
                            snapLatch.await();

                            // Load pre-generated reference image, scaled to match
                            WritableImage reference = loadAndScale(imgUrl, scaledWidth, scaledHeight);

                            ImageSimilarity.SimilarityResult result =
                                    ImageSimilarity.compare(fxImg[0], reference);
                            double rounded = Math.round(result.overall() * 100.0) / 100.0;
                            totalSimilarity.updateAndGet(v -> v + rounded);
                            minSimilarity.updateAndGet(v -> Math.min(v, rounded));
                            frameCount.incrementAndGet();

                            System.out.printf(
                                    "Frame %d @ %d%% scale: %.2f%% similar (R %.2f G %.2f B %.2f)%n",
                                    frame, (int) (scale * 100), rounded,
                                    result.red(), result.green(), result.blue());

                            // Save a diff PNG for any frame below the *target* (not the per-file
                            // override) so AI loops always see "what's still wrong".
                            if (rounded < TARGET_PER_FRAME_SIMILARITY) {
                                Files.createDirectories(outputDir);
                                saveImage(fxImg[0], reference, result,
                                        outputDir.resolve("frame_" + frame + "_" + rounded + ".png"));
                            }
                        }
                    } catch (Exception e) {
                        threadError[0] = e.getMessage();
                    } finally {
                        latch.countDown();
                    }
                }).start();
            } catch (Exception e) {
                threadError[0] = e.getMessage();
                latch.countDown();
            }
        });

        assertTrue(latch.await(300, TimeUnit.SECONDS), "Frame comparison timed out");
        assertNull(threadError[0], "Error in comparison thread: " + threadError[0]);

        int compared = frameCount.get();
        Assumptions.assumeTrue(compared > 0, "No frames compared for " + fileName);

        double average = totalSimilarity.get() / compared;
        double floor = overrides.getOrDefault(fileName, TARGET_AVERAGE_SIMILARITY);
        double gapToTarget = TARGET_AVERAGE_SIMILARITY - average;

        System.out.printf(
                "%s @ %d%% — average %.2f%%  min %.2f%%  floor %.2f%%  target %.2f%%  gap %+.2f%%%n",
                fileName, (int) (scale * 100),
                average, minSimilarity.get(),
                floor, TARGET_AVERAGE_SIMILARITY, gapToTarget);

        assertTrue(average >= floor, String.format(
                "%s: average %.2f%% < floor %.2f%% (target %.2f%%, gap %+.2f%%)",
                fileName, average, floor, TARGET_AVERAGE_SIMILARITY, gapToTarget));
    }

    /**
     * Seek the JavaFX player to {@code frame} and wait until at least one render pulse has
     * completed, so the subsequent snapshot reflects the new frame.
     *
     * <p>The double-{@link Platform#runLater} pattern works because runnables submitted
     * via {@code runLater} are dispatched in FIFO order on the FX application thread,
     * <em>after</em> any pending pulse work — by the time the second runnable executes,
     * the seek and its pulse have already been processed.</p>
     */
    private void waitForRender(LottiePlayer player, int frame) throws InterruptedException {
        CountDownLatch seekLatch = new CountDownLatch(1);
        Platform.runLater(() -> {
            player.seekToFrame(frame);
            seekLatch.countDown();
        });
        seekLatch.await();
        // Drain one pulse to make sure the seek has been rendered before snapshotting.
        CountDownLatch pulseLatch = new CountDownLatch(1);
        Platform.runLater(pulseLatch::countDown);
        pulseLatch.await();
    }

    // ── Image utilities ───────────────────────────────────────────────────────

    /**
     * Loads a PNG from {@code url} and scales it to {@code targetWidth × targetHeight}
     * using JavaFX smooth bilinear scaling.
     *
     * <p>{@code backgroundLoading} is deliberately {@code false}: at every-frame sampling,
     * asynchronous loads can occasionally lose the race and yield a 0×0 image, which then
     * blows up the {@link WritableImage} constructor with
     * {@code "Image dimensions must be positive"}.</p>
     */
    private WritableImage loadAndScale(URL url, int targetWidth, int targetHeight) {
        Image img = new Image(url.toString(), targetWidth, targetHeight, false, true, false);
        int w = (int) img.getWidth();
        int h = (int) img.getHeight();
        if (w <= 0 || h <= 0 || img.getPixelReader() == null) {
            throw new IllegalStateException(
                    "Reference image failed to load (w=" + w + ", h=" + h + "): " + url);
        }
        return new WritableImage(img.getPixelReader(), w, h);
    }

    /**
     * Saves a three-panel diff PNG (FX | reference | heatmap) for failed frames.
     *
     * <p>The heatmap colours each window red proportional to its similarity gap from
     * {@link #TARGET_PER_FRAME_SIMILARITY}, making per-frame failures actionable for AI
     * improvement loops.</p>
     */
    private void saveImage(WritableImage fxImage, WritableImage refImage,
                           ImageSimilarity.SimilarityResult result, Path path) throws IOException {
        int fxW = (int) fxImage.getWidth(), fxH = (int) fxImage.getHeight();
        int refW = (int) refImage.getWidth(), refH = (int) refImage.getHeight();
        int heatW = Math.min(fxW, refW);
        int heatH = Math.min(fxH, refH);
        int totalW = fxW + refW + heatW;
        int totalH = Math.max(Math.max(fxH, refH), heatH);

        int[] pixels = new int[totalW * totalH];

        int[] fxPixels = new int[fxW * fxH];
        fxImage.getPixelReader().getPixels(0, 0, fxW, fxH, PixelFormat.getIntArgbInstance(), fxPixels, 0, fxW);
        for (int y = 0; y < fxH; y++) System.arraycopy(fxPixels, y * fxW, pixels, y * totalW, fxW);

        int[] refPixels = new int[refW * refH];
        refImage.getPixelReader().getPixels(0, 0, refW, refH, PixelFormat.getIntArgbInstance(), refPixels, 0, refW);
        for (int y = 0; y < refH; y++) System.arraycopy(refPixels, y * refW, pixels, y * totalW + fxW, refW);

        int[] heatPixels = renderHeatmap(result.windowScores(), heatW, heatH);
        for (int y = 0; y < heatH; y++) {
            System.arraycopy(heatPixels, y * heatW, pixels, y * totalW + fxW + refW, heatW);
        }

        try (FileOutputStream fos = new FileOutputStream(path.toFile())) {
            ImageSaver.writePNG(fos, pixels, totalW, totalH);
        }
    }

    /**
     * Render a heatmap of per-window similarity scores. Red intensity reflects the gap
     * from {@link #TARGET_PER_FRAME_SIMILARITY}; perfect windows render white.
     */
    private static int[] renderHeatmap(double[][] windowScores, int width, int height) {
        int[] out = new int[width * height];
        if (windowScores.length == 0 || windowScores[0].length == 0) {
            for (int i = 0; i < out.length; i++) out[i] = 0xFFFFFFFF;
            return out;
        }
        int winsY = windowScores.length;
        int winsX = windowScores[0].length;
        for (int y = 0; y < height; y++) {
            int wy = Math.min(winsY - 1, (int) ((long) y * winsY / Math.max(1, height)));
            for (int x = 0; x < width; x++) {
                int wx = Math.min(winsX - 1, (int) ((long) x * winsX / Math.max(1, width)));
                double score = windowScores[wy][wx];
                double gap = Math.max(0, TARGET_PER_FRAME_SIMILARITY - score);
                // map gap 0..100 → red intensity 0..255
                int intensity = (int) Math.min(255, Math.round(gap * 255.0 / 100.0));
                int g = 255 - intensity;
                int b = 255 - intensity;
                out[y * width + x] = 0xFF000000 | (255 << 16) | (g << 8) | b;
            }
        }
        return out;
    }

    private void clearDirectory(Path dir) throws IOException {
        if (!Files.exists(dir)) {
            return;
        }
        try (Stream<Path> paths = Files.walk(dir)) {
            paths.sorted((a, b) -> -a.compareTo(b)).forEach(p -> {
                try {
                    Files.delete(p);
                } catch (IOException e) {
                    // ignore
                }
            });
        }
    }
}
