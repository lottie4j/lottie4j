package com.lottie4j.fxfileviewer;

import com.lottie4j.core.file.LottieFileLoader;
import com.lottie4j.core.model.animation.Animation;
import com.lottie4j.fxfileviewer.component.LottieWebView;
import com.lottie4j.fxfileviewer.util.ImageSaver;
import com.lottie4j.fxplayer.LottiePlayer;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.Label;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Diagnostic comparison test that snapshots JavaFX rendering against a JavaFX WebView JS renderer.
 *
 * <p>This is useful for debugging, but it is not an authoritative validator for FX correctness
 * because the embedded WebView/player path can diverge from the official Lottie Web rendering.</p>
 */
@DisabledIfEnvironmentVariable(named = "CI", matches = "true")
class CompareFxViewWithWebViewTest {
    private static final Logger logger = LoggerFactory.getLogger(CompareFxViewWithWebViewTest.class);
    private static final double SIMILARITY_THRESHOLD = 98;
    private static final int CANVAS_WIDTH = 800;
    private static final int CANVAS_HEIGHT = 600;
    private static Stage primaryStage;

    /**
     * Initializes JavaFX runtime and creates a primary stage used by all test cases.
     */
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
     * Provides the set of Lottie JSON files to validate.
     */
    static Stream<String> lottieJsonFiles() {
        return Stream.of(
                "json/angry_bird.json",
                "json/animated_background_patterns.json",
                "json/box-moving-changing-color.json",
                "json/isometric_data_analysis.json",
                "json/java_duke_fadein.json",
                "json/java_duke_flip.json",
                "json/java_duke_slidein.json",
                "json/loading.json",
                "json/lottie4j.json",
                "json/lottie_lego.json",
                "json/sandy_loading.json",
                "json/snake_ladder_loading_animation.json",
                "json/success.json",
                "json/timeline_animation.json",
                "dot/lottie4j.lottie",
                "dot/demo-1.lottie",
                "dot/demo-2.lottie"
        );
    }

    /**
     * Loads one Lottie file, compares FX vs JS renderer outputs at full size, and asserts average similarity.
     *
     * @param fileName resource name of the Lottie JSON animation
     */
    @ParameterizedTest
    @MethodSource("lottieJsonFiles")
    void compareFxAndJsRenderingFullSize(String fileName) throws Exception {
        compareFxAndJsRendering(fileName, 1.0);
    }

    /**
     * Loads one Lottie file, compares FX vs JS renderer outputs resized to half, and asserts average similarity.
     *
     * @param fileName resource name of the Lottie JSON animation
     */
    @ParameterizedTest
    @MethodSource("lottieJsonFiles")
    void compareFxAndJsRenderingHalfSize(String fileName) throws Exception {
        compareFxAndJsRendering(fileName, 0.5);
    }

    /**
     * Compares JavaFX and JavaScript rendering outputs for a Lottie animation file at a specified scale.
     * Loads the animation from the given file name, validates its structure, and performs frame-by-frame
     * comparison between the JavaFX renderer and WebView lottie-web renderer. The comparison is performed
     * at the specified scale factor and the average similarity across all sampled frames must meet the
     * defined similarity threshold.
     *
     * @param fileName the resource name of the Lottie JSON animation file to load and compare
     * @param scale    the scaling factor to apply to the animation dimensions, must be in range [0.1, 1.0]
     * @throws IllegalArgumentException if scale is outside the valid range [0.1, 1.0]
     * @throws AssertionError           if the resource is not found, file does not exist, animation fails to load,
     *                                  animation structure is invalid, or similarity threshold is not met
     * @throws Exception                if an error occurs during file loading, animation parsing, or frame comparison
     */
    private void compareFxAndJsRendering(String fileName, double scale) throws Exception {
        if (scale < 0.1 || scale > 1.0) {
            throw new IllegalArgumentException("Scale must be in range [0.1, 1.0], got: " + scale);
        }

        logger.info("Testing: {} at scale {}", fileName, scale);

        var resource = getClass().getResource("/" + fileName);
        assertNotNull(resource, "Resource not found: " + fileName);

        File file = new File(resource.getFile());
        assertTrue(file.exists(), "File does not exist: " + fileName);

        Animation animation = LottieFileLoader.load(file);
        assertNotNull(animation, "Failed to load animation from: " + fileName);
        assertNotNull(animation.layers(), "Animation layers should not be null for: " + fileName);
        assertFalse(animation.layers().isEmpty(), "Animation should have layers for: " + fileName);

        // Compare frame-by-frame at the specified scale
        var average = compareAnimationFrames(animation, fileName, scale);
        assertTrue(average >= SIMILARITY_THRESHOLD, "Similarity threshold not met for "
                + fileName + " at scale " + scale + ": " + average + "/" + SIMILARITY_THRESHOLD);
    }

    /**
     * Runs frame-by-frame comparison between JavaFX player and WebView lottie-web rendering at the specified scale.
     *
     * @param animation parsed animation model
     * @param fileName  file name used for logging and output directories
     * @param scale     scale factor in range [0.1, 1.0] to resize both renderers
     * @return average similarity percentage across sampled frames
     */
    private double compareAnimationFrames(Animation animation, String fileName, double scale) throws Exception {
        CountDownLatch latch = new CountDownLatch(1);

        // Create output directory for comparison images
        Path outputDir = Path.of("target/test-output", fileName.replace(".json", "").replace(".lottie", "") + "_scale_" + scale);

        // Delete existing files if directory exists
        if (Files.exists(outputDir)) {
            // Empty the directory
            try (Stream<Path> paths = Files.walk(outputDir)) {
                paths.sorted((a, b) -> -a.compareTo(b)) // Delete files before directories
                        .forEach(path -> {
                            try {
                                Files.delete(path);
                            } catch (IOException e) {
                                // Ignore cleanup failures for stale artifacts
                            }
                        });
            }
            outputDir.toFile().delete();
        }

        // Use actual animation dimensions, scaled by the provided factor
        int animWidth = animation.width() != null ? animation.width() : CANVAS_WIDTH;
        int animHeight = animation.height() != null ? animation.height() : CANVAS_HEIGHT;

        int scaledWidth = Math.max(1, (int) Math.round(animWidth * scale));
        int scaledHeight = Math.max(1, (int) Math.round(animHeight * scale));

        AtomicReference<Double> totalSimilarity = new AtomicReference<>((double) 0);
        AtomicInteger frameCount = new AtomicInteger(0);

        Platform.runLater(() -> {
            try {
                // Create FX player and resize to scaled dimensions
                var fxPlayer = new LottiePlayer(animation, animWidth, animHeight);
                fxPlayer.resizeRenderPercent(scale * 100.0);

                var webView = new LottieWebView();
                webView.setSize(scaledWidth, scaledHeight);

                var playersBox = new HBox(10);
                playersBox.getChildren().addAll(fxPlayer, webView);

                Label similarityLabel = new Label("Similarity: -- % @ " + (int) (scale * 100) + "%");
                similarityLabel.setStyle("-fx-font-size: 30; -fx-text-fill: black; -fx-font-weight: bold;");
                similarityLabel.setBackground(new Background(new BackgroundFill(
                        Color.web("#FFFF99", 0.9),
                        new CornerRadii(15),
                        new Insets(20)
                )));
                similarityLabel.setPadding(new Insets(30, 60, 30, 60));

                StackPane rootPane = new StackPane(playersBox, similarityLabel);
                StackPane.setAlignment(similarityLabel, javafx.geometry.Pos.CENTER);

                Scene scene = new Scene(rootPane, scaledWidth * 2 + 10, scaledHeight);
                primaryStage.setScene(scene);
                primaryStage.setAlwaysOnTop(true);
                primaryStage.toFront();
                primaryStage.show();
                primaryStage.requestFocus();

                webView.loadLottie(animation, scaledWidth, scaledHeight);

                // Do not block JavaFX thread; perform waits/comparison in background thread.
                new Thread(() -> {
                    try {
                        assertTrue(webView.waitUntilReady(25_000), "WebView animation not ready");

                        int inPoint = animation.inPoint() != null ? animation.inPoint() : 0;
                        int outPointExclusive = animation.outPoint() != null ? animation.outPoint() : 60;
                        int lastFrame = Math.max(inPoint, outPointExclusive - 5);
                        List<Integer> framesToCompare = buildSampledFrames(inPoint, lastFrame, 5);

                        seekAndSyncFrame(fxPlayer, webView, inPoint);

                        for (int currentFrame : framesToCompare) {
                            seekAndSyncFrame(fxPlayer, webView, currentFrame);

                            // Allow renderers to fully settle before snapshot
                            Thread.sleep(200);

                            CountDownLatch snapshotLatch = new CountDownLatch(1);
                            final WritableImage[] images = new WritableImage[2];

                            Platform.runLater(() -> {
                                try {
                                    // Verify frame is still correct before snapshot
                                    int fxCurrentFrame = (int) fxPlayer.getCurrentFrame();
                                    int webViewCurrentFrame = webView.getCurrentFrame();

                                    if (webViewCurrentFrame != currentFrame) {
                                        System.out.printf("FRAME MISMATCH on snapshot: expected %d, WebView at %d%n", currentFrame, webViewCurrentFrame);
                                    }

                                    WritableImage combinedImage = playersBox.snapshot(new SnapshotParameters(), null);

                                    WritableImage playerImage = new WritableImage(scaledWidth, scaledHeight);
                                    WritableImage webViewImage = new WritableImage(scaledWidth, scaledHeight);

                                    playerImage.getPixelWriter().setPixels(0, 0, scaledWidth, scaledHeight,
                                            combinedImage.getPixelReader(), 0, 0);
                                    webViewImage.getPixelWriter().setPixels(0, 0, scaledWidth, scaledHeight,
                                            combinedImage.getPixelReader(), scaledWidth + 10, 0);

                                    images[0] = playerImage;
                                    images[1] = webViewImage;
                                } finally {
                                    snapshotLatch.countDown();
                                }
                            });

                            snapshotLatch.await();

                            WritableImage playerImage = images[0];
                            WritableImage webViewImage = images[1];

                            double similarity = compareImages(playerImage, webViewImage);
                            double roundedSimilarity = Math.round(similarity * 100.0) / 100.0;
                            totalSimilarity.updateAndGet(v -> v + roundedSimilarity);
                            frameCount.incrementAndGet();

                            Platform.runLater(() -> {
                                similarityLabel.setText(String.format("Frame %d @ %d%% | Similarity: %.2f %%", currentFrame, (int) (scale * 100), roundedSimilarity));
                                if (roundedSimilarity >= SIMILARITY_THRESHOLD) {
                                    similarityLabel.setBackground(new Background(new BackgroundFill(
                                            Color.web("#99FF99", 0.9),
                                            new CornerRadii(15),
                                            new Insets(20)
                                    )));
                                } else {
                                    similarityLabel.setBackground(new Background(new BackgroundFill(
                                            Color.web("#FF9999", 0.9),
                                            new CornerRadii(15),
                                            new Insets(20)
                                    )));
                                }
                            });

                            if (roundedSimilarity < SIMILARITY_THRESHOLD) {
                                Files.createDirectories(outputDir);
                                saveImage(playerImage, webViewImage, outputDir.resolve("frame_" + currentFrame + "_similarity_" + roundedSimilarity + ".png"));
                            }

                            System.out.printf("Frame %d @ %d%% scale: %.2f%% similar (threshold: %.0f%%)%n",
                                    currentFrame, (int) (scale * 100), roundedSimilarity, SIMILARITY_THRESHOLD);
                        }
                    } catch (Exception e) {
                        fail("Error in frame comparison thread: " + e.getMessage());
                    } finally {
                        latch.countDown();
                    }
                }).start();

            } catch (Exception e) {
                fail("Error setting up comparison: " + e.getMessage());
                latch.countDown();
            }
        });

        assertTrue(latch.await(60, TimeUnit.SECONDS), "Frame comparison timed out");

        double averageSimilarity = totalSimilarity.get() / frameCount.get();
        System.out.printf("%s @ %d%% - Average similarity: %.2f%%%n", fileName, (int) (scale * 100), averageSimilarity);

        return averageSimilarity;
    }

    /**
     * Builds sampled frame indices and guarantees inclusion of the final frame.
     */
    private List<Integer> buildSampledFrames(int firstFrame, int lastFrame, int step) {
        List<Integer> frames = new ArrayList<>();
        if (step <= 0) {
            step = 1;
        }
        for (int f = firstFrame; f <= lastFrame; f += step) {
            frames.add(f);
        }
        // Ensure exact last frame is always validated.
        if (frames.isEmpty() || frames.get(frames.size() - 1) != lastFrame) {
            frames.add(lastFrame);
        }
        return frames;
    }

    /**
     * Seeks both renderers to the target frame and waits until both are confirmed rendered.
     * Uses FX pulse cycles to ensure rendering is complete without blocking threads.
     */
    private void seekAndSyncFrame(LottiePlayer fxPlayer, LottieWebView webView, int targetFrame) throws InterruptedException {
        // Step 1: Seek both renderers to target frame
        CountDownLatch updateLatch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                fxPlayer.seekToFrame(targetFrame);
                webView.setFrame(targetFrame);
            } finally {
                updateLatch.countDown();
            }
        });
        updateLatch.await();

        // Step 2: Wait for WebView to actually render the frame
        boolean webViewSynced = webView.waitUntilFrame(targetFrame, 3_000);
        if (!webViewSynced) {
            int actualFrame = webView.getCurrentFrame();
            System.out.printf("WARNING: WebView frame mismatch. Expected %d, got %d. Retrying seek...%n", targetFrame, actualFrame);
            // Retry once if sync failed
            retrySeekFrame(webView, targetFrame);
        }

        // Step 3: Wait for both renderers to be stable and ready for snapshot
        waitForBothRenderersStable(webView, targetFrame, 4_000);
    }

    private void retrySeekFrame(LottieWebView webView, int targetFrame) throws InterruptedException {
        CountDownLatch retryLatch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                webView.setFrame(targetFrame);
            } finally {
                retryLatch.countDown();
            }
        });
        retryLatch.await();

        boolean synced = webView.waitUntilFrame(targetFrame, 2_000);
        if (!synced) {
            int actualFrame = webView.getCurrentFrame();
            System.out.printf("ERROR: WebView still not synced after retry. Expected %d, got %d%n", targetFrame, actualFrame);
        }
    }

    private void waitForBothRenderersStable(LottieWebView webView, int targetFrame, long timeoutMs) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMs;

        // Poll WebView frame multiple times to ensure it's stable
        int consecutiveMatches = 0;
        final int REQUIRED_MATCHES = 3; // Frame must match 3 consecutive times

        while (System.currentTimeMillis() < deadline && consecutiveMatches < REQUIRED_MATCHES) {
            int currentWebViewFrame = webView.getCurrentFrame();

            if (currentWebViewFrame == targetFrame) {
                consecutiveMatches++;
            } else {
                consecutiveMatches = 0; // Reset counter if frame changes
                // Wait a bit longer if frame is still changing
                Thread.sleep(50);
            }

            if (consecutiveMatches < REQUIRED_MATCHES) {
                Thread.sleep(100);
            }
        }

        if (consecutiveMatches < REQUIRED_MATCHES) {
            System.out.printf("WARNING: WebView frame did not stabilize at %d. Last frame: %s%n",
                    targetFrame, webView.getRenderDebug());
        }

        // Final JavaFX pulse to ensure rendering is complete
        CountDownLatch finalPulseLatch = new CountDownLatch(1);
        Platform.runLater(() -> {
            AnimationTimer timer = new AnimationTimer() {
                private int pulseCount = 0;

                @Override
                public void handle(long now) {
                    pulseCount++;
                    if (pulseCount >= 3) { // Wait for 3 pulses to ensure all rendering complete
                        this.stop();
                        finalPulseLatch.countDown();
                    }
                }
            };
            timer.start();
        });

        try {
            finalPulseLatch.await(2_000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw e;
        }
    }

    /**
     * Computes similarity between two images using structural similarity (SSIM) metric.
     * Treats minor rendering differences (anti-aliasing, sub-pixel rounding) more leniently.
     *
     * @return similarity percentage in range [0, 100]
     */
    private double compareImages(WritableImage img1, WritableImage img2) {
        int width = (int) Math.min(img1.getWidth(), img2.getWidth());
        int height = (int) Math.min(img1.getHeight(), img2.getHeight());

        if (width <= 0 || height <= 0) {
            return 0.0;
        }

        // Use structural similarity metric for better accuracy with anti-aliasing differences
        int[][] pixels1 = new int[height][width];
        int[][] pixels2 = new int[height][width];

        PixelReader reader1 = img1.getPixelReader();
        PixelReader reader2 = img2.getPixelReader();

        // Read all pixels and convert to grayscale for comparison
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int argb1 = reader1.getArgb(x, y);
                int argb2 = reader2.getArgb(x, y);

                // Convert to grayscale
                pixels1[y][x] = pixelToGrayscale(argb1);
                pixels2[y][x] = pixelToGrayscale(argb2);
            }
        }

        // Calculate mean and variance for both images
        double mean1 = calculateMean(pixels1);
        double mean2 = calculateMean(pixels2);
        double variance1 = calculateVariance(pixels1, mean1);
        double variance2 = calculateVariance(pixels2, mean2);
        double covariance = calculateCovariance(pixels1, pixels2, mean1, mean2);

        // SSIM formula: (2*mean1*mean2 + c1) * (2*covariance + c2) / ((mean1^2 + mean2^2 + c1) * (variance1 + variance2 + c2))
        double c1 = 6.5025; // (0.01 * 255)^2
        double c2 = 58.5225; // (0.03 * 255)^2

        double numerator = (2 * mean1 * mean2 + c1) * (2 * covariance + c2);
        double denominator = (mean1 * mean1 + mean2 * mean2 + c1) * (variance1 + variance2 + c2);

        if (denominator == 0) return 100.0;
        double ssim = numerator / denominator;

        // Convert SSIM (-1 to 1) to percentage (0 to 100)
        return Math.max(0, Math.min(100, (ssim + 1) * 50));
    }

    /**
     * Converts ARGB pixel to grayscale luminance.
     */
    private int pixelToGrayscale(int argb) {
        int r = (argb >> 16) & 0xFF;
        int g = (argb >> 8) & 0xFF;
        int b = argb & 0xFF;
        // Use standard grayscale conversion
        return (int) (0.299 * r + 0.587 * g + 0.114 * b);
    }

    /**
     * Calculates mean pixel intensity for a 2D grayscale image.
     */
    private double calculateMean(int[][] pixels) {
        double sum = 0;
        for (int[] row : pixels) {
            for (int pixel : row) {
                sum += pixel;
            }
        }
        return sum / (pixels.length * pixels[0].length);
    }

    /**
     * Calculates variance for a 2D grayscale image.
     */
    private double calculateVariance(int[][] pixels, double mean) {
        double sum = 0;
        for (int[] row : pixels) {
            for (int pixel : row) {
                sum += Math.pow(pixel - mean, 2);
            }
        }
        return sum / (pixels.length * pixels[0].length);
    }

    /**
     * Calculates covariance between two grayscale images.
     */
    private double calculateCovariance(int[][] pixels1, int[][] pixels2, double mean1, double mean2) {
        double sum = 0;
        for (int y = 0; y < pixels1.length; y++) {
            for (int x = 0; x < pixels1[0].length; x++) {
                sum += (pixels1[y][x] - mean1) * (pixels2[y][x] - mean2);
            }
        }
        return sum / (pixels1.length * pixels1[0].length);
    }

    /**
     * Saves both renderer snapshots as one side-by-side PNG.
     */
    private void saveImage(WritableImage imageFx, WritableImage imageJs, Path path) throws IOException {
        int fxWidth = (int) imageFx.getWidth();
        int fxHeight = (int) imageFx.getHeight();
        int jsWidth = (int) imageJs.getWidth();
        int jsHeight = (int) imageJs.getHeight();

        // Combined dimensions: side by side
        int totalWidth = fxWidth + jsWidth;
        int totalHeight = Math.max(fxHeight, jsHeight);

        // Create combined pixel array
        int[] pixels = new int[totalWidth * totalHeight];

        // Get pixel readers
        PixelReader fxReader = imageFx.getPixelReader();
        PixelReader jsReader = imageJs.getPixelReader();

        // Copy FX image to left side
        int[] fxPixels = new int[fxWidth * fxHeight];
        fxReader.getPixels(0, 0, fxWidth, fxHeight, PixelFormat.getIntArgbInstance(), fxPixels, 0, fxWidth);
        for (int y = 0; y < fxHeight; y++) {
            System.arraycopy(fxPixels, y * fxWidth, pixels, y * totalWidth, fxWidth);
        }

        // Copy JS image to right side
        int[] jsPixels = new int[jsWidth * jsHeight];
        jsReader.getPixels(0, 0, jsWidth, jsHeight, PixelFormat.getIntArgbInstance(), jsPixels, 0, jsWidth);
        for (int y = 0; y < jsHeight; y++) {
            System.arraycopy(jsPixels, y * jsWidth, pixels, y * totalWidth + fxWidth, jsWidth);
        }

        // Write PNG file
        try (FileOutputStream fos = new FileOutputStream(path.toFile())) {
            ImageSaver.writePNG(fos, pixels, totalWidth, totalHeight);
        }
    }
}
