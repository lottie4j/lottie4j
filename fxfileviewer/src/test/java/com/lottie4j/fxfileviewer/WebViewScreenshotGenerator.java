package com.lottie4j.fxfileviewer;

import com.lottie4j.core.file.LottieFileLoader;
import com.lottie4j.core.model.animation.Animation;
import com.lottie4j.fxfileviewer.component.LottieWebView;
import com.lottie4j.fxfileviewer.util.ImageSaver;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Rectangle2D;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

/**
 * One-time utility for generating WebView reference screenshots for all Lottie animation files.
 *
 * <p>Run this class manually whenever a new animation file is added to {@link CompareFxViewWithWebViewTest#lottieJsonFiles()}.
 * The generated PNG images are committed to {@code src/test/resources} and used by
 * {@link CompareFxViewWithWebViewTest} without requiring a live WebView at test time.</p>
 *
 * <p>To run: temporarily remove {@code @Disabled} or use {@code -Dtest=WebViewScreenshotGenerator}.</p>
 *
 * <p>Output layout: {@code src/test/resources/json/angry_bird-webview/frame_0.png}, etc.</p>
 */
public class WebViewScreenshotGenerator extends Application {
    private static final Logger logger = LoggerFactory.getLogger(WebViewScreenshotGenerator.class);

    private static final int CANVAS_WIDTH = 800;
    private static final int CANVAS_HEIGHT = 600;

    // Initialised inside start() on the FX thread — must NOT be a static field
    static LottieWebView webView;

    public static void main(String[] args) {
        assumeFalse(GraphicsEnvironment.isHeadless(), "No graphical display available for JavaFX");
        Application.launch(args);
    }

    @Override
    public void start(Stage stage) throws URISyntaxException {
        // Create WebView here, on the FX thread, after the platform is running.
        // Wrap in a Group so the scene never force-resizes the WebView to fill it.
        webView = new LottieWebView();

        stage.setScene(new Scene(new Group(webView), 1600, 800));
        stage.show();
        logger.info("WebView started");

        // Anchor on a known resource to derive the src/test/resources directory.
        // getResource("/") can return null with module-path classpaths.
        URL knownResource = WebViewScreenshotGenerator.class.getResource("/json/angry_bird.json");
        assertNotNull(knownResource, "Could not locate anchor resource json/angry_bird.json");
        Path testResourcesDir = Path.of(knownResource.toURI())
                .getParent()  // json/
                .getParent()  // target/test-classes/
                .getParent()  // target/
                .getParent()  // <module-root>
                .resolve("src/test/resources");

        // Run the processing loop on a background thread so start() returns immediately
        // and does not block the FX thread (which would deadlock Platform.runLater calls).
        new Thread(() -> {
            List<String> failures = new ArrayList<>();
            for (String fileName : CompareFxViewWithWebViewTest.lottieJsonFiles().toList()) {
                logger.info("Generating screenshots for: {}", fileName);
                try {
                    generateScreenshotsForFile(fileName, testResourcesDir);
                } catch (Exception e) {
                    logger.error("FAILED {}: {}", fileName, e.getMessage(), e);
                    failures.add(fileName);
                }
            }
            if (failures.isEmpty()) {
                logger.info("All screenshots generated successfully.");
            } else {
                logger.error("Failed to generate screenshots for: {}", failures);
            }
            Platform.exit();
        }, "screenshot-generator").start();
    }

    private static void generateScreenshotsForFile(String fileName, Path testResourcesDir) throws Exception {
        URL resource = WebViewScreenshotGenerator.class.getResource("/" + fileName);
        assertTrue(resource != null, "Resource not found: " + fileName);

        Animation animation = LottieFileLoader.load(new File(resource.getFile()));

        int animWidth = animation.width() != null ? animation.width() : CANVAS_WIDTH;
        int animHeight = animation.height() != null ? animation.height() : CANVAS_HEIGHT;

        Path outputDir = testResourcesDir.resolve(webViewDirPath(fileName));
        recreateDirectory(outputDir);

        int inPoint = animation.inPoint() != null ? animation.inPoint() : 0;
        int outPoint = animation.outPoint() != null ? animation.outPoint() : 60;
        List<Integer> frames = buildSampledFrames(inPoint, Math.max(inPoint, outPoint - 5), 5);

        CountDownLatch doneLatch = new CountDownLatch(1);
        RuntimeException[] error = new RuntimeException[1];

        Platform.runLater(() -> {
            try {
                webView.setSize(animWidth, animHeight);
                webView.loadLottie(animation, animWidth, animHeight);

                new Thread(() -> {
                    try {
                        assertTrue(webView.waitUntilReady(25_000), "WebView not ready: " + fileName);
                        captureFrames(webView, frames, outputDir, animWidth, animHeight);
                        System.out.printf("Generated %d frames for %s → %s%n", frames.size(), fileName, outputDir);
                    } catch (Exception e) {
                        error[0] = new RuntimeException(e);
                    } finally {
                        doneLatch.countDown();
                    }
                }).start();
            } catch (Exception e) {
                error[0] = new RuntimeException(e);
                doneLatch.countDown();
            }
        });

        assertTrue(doneLatch.await(120, TimeUnit.SECONDS), "Timed out generating: " + fileName);
        if (error[0] != null) throw error[0];
    }

    private static void captureFrames(LottieWebView webView, List<Integer> frames, Path outputDir,
                                       int animWidth, int animHeight)
            throws InterruptedException, IOException {
        for (int frame : frames) {
            CountDownLatch seekLatch = new CountDownLatch(1);
            Platform.runLater(() -> {
                webView.setFrame(frame);
                seekLatch.countDown();
            });
            seekLatch.await();
            webView.waitUntilFrame(frame, 3_000);
            Thread.sleep(200);

            CountDownLatch snapLatch = new CountDownLatch(1);
            WritableImage[] img = new WritableImage[1];
            Platform.runLater(() -> {
                // Snapshot exactly the animation area, regardless of the stage/scene size
                SnapshotParameters params = new SnapshotParameters();
                params.setViewport(new Rectangle2D(0, 0, animWidth, animHeight));
                img[0] = webView.snapshot(params, new WritableImage(animWidth, animHeight));
                snapLatch.countDown();
            });
            snapLatch.await();

            savePng(img[0], outputDir.resolve("frame_" + frame + ".png"));
        }
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

    // ── Private utilities ────────────────────────────────────────────────────

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

    private static void savePng(WritableImage image, Path path) throws IOException {
        int w = (int) image.getWidth();
        int h = (int) image.getHeight();
        int[] pixels = new int[w * h];
        image.getPixelReader().getPixels(0, 0, w, h, PixelFormat.getIntArgbInstance(), pixels, 0, w);
        try (FileOutputStream fos = new FileOutputStream(path.toFile())) {
            ImageSaver.writePNG(fos, pixels, w, h);
            logger.info("Saved PNG: {}", path);
        }
    }
}
