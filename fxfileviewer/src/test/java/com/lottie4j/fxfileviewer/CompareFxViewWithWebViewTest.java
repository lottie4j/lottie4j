package com.lottie4j.fxfileviewer;

import com.lottie4j.core.file.LottieFileLoader;
import com.lottie4j.core.model.animation.Animation;
import com.lottie4j.fxfileviewer.util.ImageSaver;
import com.lottie4j.fxplayer.LottiePlayer;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.Image;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Validates JavaFX player rendering against pre-generated WebView reference screenshots.
 *
 * <p>Reference images are produced once by {@link WebViewScreenshotGenerator} and committed
 * to {@code src/test/resources}. This test never starts a WebView and is safe to run in CI
 * headless mode.</p>
 *
 * <p>If no reference images exist for a file, the test is skipped.
 * Run {@link WebViewScreenshotGenerator} locally to create or refresh them.</p>
 */
public class CompareFxViewWithWebViewTest {
    private static final Logger logger = LoggerFactory.getLogger(CompareFxViewWithWebViewTest.class);
    private static final double SIMILARITY_THRESHOLD = 98;
    private static final int CANVAS_WIDTH = 800;
    private static final int CANVAS_HEIGHT = 600;
    private static Stage primaryStage;

    @BeforeAll
    public static void initJavaFX() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        Platform.startup(() -> {
            primaryStage = new Stage();
            latch.countDown();
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS), "JavaFX Platform failed to start");
    }

    /**
     * Provides the set of Lottie animation files to validate.
     */
    static Stream<String> lottieJsonFiles() {
        return Stream.of(
                "json/angry_bird.json",
                "json/animated_background_patterns.json",
                "json/box-moving-changing-color.json",
                "json/isometric_data_analysis.json",
                "json/java_duke_fadein.json",
                "json/java_duke_flip.json",
                //"json/java_duke_slidein.json",
                "json/loading.json",
                "json/lottie4j.json",
                //"json/lottie_lego.json",
                "json/sandy_loading.json",
                "json/snake_ladder_loading_animation.json",
                "json/success.json",
                "json/timeline_animation.json",
                "dot/lottie4j.lottie",
                "dot/demo-1.lottie"
                //"dot/demo-2.lottie"
        );
    }

    @ParameterizedTest
    @MethodSource("lottieJsonFiles")
    void compareFxAndJsRenderingFullSize(String fileName) throws Exception {
        compareFxWithPreGeneratedImages(fileName, 1.0);
    }

    @Test
    void compareFxAndJsRenderingHalfSize() throws Exception {
        compareFxWithPreGeneratedImages(lottieJsonFiles().toList().getFirst(), 0.5);
     }

    // ── Core comparison logic ─────────────────────────────────────────────────

    private void compareFxWithPreGeneratedImages(String fileName, double scale) throws Exception {
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
        List<Integer> frames = WebViewScreenshotGenerator.buildSampledFrames(
                inPoint, Math.max(inPoint, outPoint - 5), 5);

        Path outputDir = Path.of("target/test-output",
                WebViewScreenshotGenerator.webViewDirPath(fileName) + "_scale_" + scale);
        clearDirectory(outputDir);

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Double> totalSimilarity = new AtomicReference<>(0.0);
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

                            // Seek FX player to frame
                            CountDownLatch seekLatch = new CountDownLatch(1);
                            Platform.runLater(() -> {
                                fxPlayer.seekToFrame(frame);
                                seekLatch.countDown();
                            });
                            seekLatch.await();
                            Thread.sleep(200);

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

                            double similarity = compareImages(fxImg[0], reference);
                            double rounded = Math.round(similarity * 100.0) / 100.0;
                            totalSimilarity.updateAndGet(v -> v + rounded);
                            frameCount.incrementAndGet();

                            System.out.printf("Frame %d @ %d%% scale: %.2f%% similar (threshold: %.0f%%)%n",
                                    frame, (int) (scale * 100), rounded, SIMILARITY_THRESHOLD);

                            if (rounded < SIMILARITY_THRESHOLD) {
                                Files.createDirectories(outputDir);
                                saveImage(fxImg[0], reference,
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

        assertTrue(latch.await(60, TimeUnit.SECONDS), "Frame comparison timed out");
        assertNull(threadError[0], "Error in comparison thread: " + threadError[0]);

        int compared = frameCount.get();
        Assumptions.assumeTrue(compared > 0, "No frames compared for " + fileName);

        double average = totalSimilarity.get() / compared;
        System.out.printf("%s @ %d%% — Average similarity: %.2f%%%n",
                fileName, (int) (scale * 100), average);
        assertTrue(average >= SIMILARITY_THRESHOLD,
                "Similarity threshold not met for " + fileName + " at scale " + scale
                        + ": " + average + " / " + SIMILARITY_THRESHOLD);
    }

    // ── Image utilities ───────────────────────────────────────────────────────

    /**
     * Loads a PNG from {@code url} and scales it to {@code targetWidth × targetHeight}
     * using JavaFX smooth bilinear scaling.
     */
    private WritableImage loadAndScale(URL url, int targetWidth, int targetHeight) {
        Image img = new Image(url.toString(), targetWidth, targetHeight, false, true);
        return new WritableImage(img.getPixelReader(), (int) img.getWidth(), (int) img.getHeight());
    }

    /**
     * Computes SSIM-based similarity between two images, returned as a percentage [0, 100].
     */
    private double compareImages(WritableImage img1, WritableImage img2) {
        int width = (int) Math.min(img1.getWidth(), img2.getWidth());
        int height = (int) Math.min(img1.getHeight(), img2.getHeight());

        if (width <= 0 || height <= 0) return 0.0;

        int[][] pixels1 = new int[height][width];
        int[][] pixels2 = new int[height][width];

        PixelReader reader1 = img1.getPixelReader();
        PixelReader reader2 = img2.getPixelReader();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                pixels1[y][x] = pixelToGrayscale(reader1.getArgb(x, y));
                pixels2[y][x] = pixelToGrayscale(reader2.getArgb(x, y));
            }
        }

        double mean1 = calculateMean(pixels1);
        double mean2 = calculateMean(pixels2);
        double variance1 = calculateVariance(pixels1, mean1);
        double variance2 = calculateVariance(pixels2, mean2);
        double covariance = calculateCovariance(pixels1, pixels2, mean1, mean2);

        double c1 = 6.5025;   // (0.01 * 255)^2
        double c2 = 58.5225;  // (0.03 * 255)^2
        double numerator = (2 * mean1 * mean2 + c1) * (2 * covariance + c2);
        double denominator = (mean1 * mean1 + mean2 * mean2 + c1) * (variance1 + variance2 + c2);

        if (denominator == 0) return 100.0;
        return Math.max(0, Math.min(100, ((numerator / denominator) + 1) * 50));
    }

    private int pixelToGrayscale(int argb) {
        int r = (argb >> 16) & 0xFF;
        int g = (argb >> 8) & 0xFF;
        int b = argb & 0xFF;
        return (int) (0.299 * r + 0.587 * g + 0.114 * b);
    }

    private double calculateMean(int[][] pixels) {
        double sum = 0;
        for (int[] row : pixels) for (int p : row) sum += p;
        return sum / ((long) pixels.length * pixels[0].length);
    }

    private double calculateVariance(int[][] pixels, double mean) {
        double sum = 0;
        for (int[] row : pixels) for (int p : row) sum += Math.pow(p - mean, 2);
        return sum / ((long) pixels.length * pixels[0].length);
    }

    private double calculateCovariance(int[][] pixels1, int[][] pixels2, double mean1, double mean2) {
        double sum = 0;
        for (int y = 0; y < pixels1.length; y++)
            for (int x = 0; x < pixels1[0].length; x++)
                sum += (pixels1[y][x] - mean1) * (pixels2[y][x] - mean2);
        return sum / ((long) pixels1.length * pixels1[0].length);
    }

    /** Saves a side-by-side PNG (FX left, reference right) for failed frames. */
    private void saveImage(WritableImage fxImage, WritableImage refImage, Path path) throws IOException {
        int fxW = (int) fxImage.getWidth(), fxH = (int) fxImage.getHeight();
        int refW = (int) refImage.getWidth(), refH = (int) refImage.getHeight();
        int totalW = fxW + refW;
        int totalH = Math.max(fxH, refH);

        int[] pixels = new int[totalW * totalH];

        int[] fxPixels = new int[fxW * fxH];
        fxImage.getPixelReader().getPixels(0, 0, fxW, fxH, PixelFormat.getIntArgbInstance(), fxPixels, 0, fxW);
        for (int y = 0; y < fxH; y++) System.arraycopy(fxPixels, y * fxW, pixels, y * totalW, fxW);

        int[] refPixels = new int[refW * refH];
        refImage.getPixelReader().getPixels(0, 0, refW, refH, PixelFormat.getIntArgbInstance(), refPixels, 0, refW);
        for (int y = 0; y < refH; y++) System.arraycopy(refPixels, y * refW, pixels, y * totalW + fxW, refW);

        try (FileOutputStream fos = new FileOutputStream(path.toFile())) {
            ImageSaver.writePNG(fos, pixels, totalW, totalH);
        }
    }

    private void clearDirectory(Path dir) throws IOException {
        if (!Files.exists(dir)) return;
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
