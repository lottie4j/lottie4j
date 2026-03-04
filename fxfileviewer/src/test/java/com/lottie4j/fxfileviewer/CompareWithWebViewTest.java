package com.lottie4j.fxfileviewer;

import com.lottie4j.core.file.LottieFileLoader;
import com.lottie4j.core.model.Animation;
import com.lottie4j.fxfileviewer.util.ImageSaver;
import com.lottie4j.fxplayer.LottiePlayer;
import javafx.application.Platform;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

public class CompareWithWebViewTest {

    private static final Logger logger = LoggerFactory.getLogger(CompareWithWebViewTest.class);

    private static final double SIMILARITY_THRESHOLD = 80.0; // 80% similarity required (accounting for rendering differences)
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

    static Stream<String> lottieJsonFiles() {
        return Stream.of(
//                "angry_bird.json",
//                "animated_background_patterns.json",
//                "box-moving-changing-color.json",
//                "box-static.json",
//                "circle-static.json",
//                "interactive_mood_selector_ui.json",
//                "interactive_mood_selector_ui_bg_isolated.json",
//                "isometric_data_analysis.json",
//                "java_duke_fadein.json",
//                "java_duke_flip.json",
//                "java_duke_slidein.json",
//                "java_duke_still.json",
//                "loading.json",
//                "lottie_lego.json",
//                "polygon-static.json",
//                "sandy_loading.json",
//                "snake_ladder_loading_animation.json",
//                "star-static.json",
//                "success.json",
                "timeline_animation.json"//,
//                "timeline_animation_polygon_5.json"
        );
    }

    @ParameterizedTest
    @MethodSource("lottieJsonFiles")
    void testLoadLottieFile(String fileName) throws Exception {
        logger.info("Testing: {}", fileName);

        var resource = getClass().getResource("/" + fileName);
        assertNotNull(resource, "Resource not found: " + fileName);

        File file = new File(resource.getFile());
        assertTrue(file.exists(), "File does not exist: " + fileName);

        Animation animation = LottieFileLoader.load(file);
        assertNotNull(animation, "Failed to load animation from: " + fileName);
        assertNotNull(animation.layers(), "Animation layers should not be null for: " + fileName);
        assertFalse(animation.layers().isEmpty(), "Animation should have layers for: " + fileName);

        // Compare frame-by-frame
        compareAnimationFrames(file, animation, fileName);
    }

    private void compareAnimationFrames(File lottieFile, Animation animation, String fileName) throws Exception {
        CountDownLatch latch = new CountDownLatch(1);

        // Create output directory for comparison images
        Path outputDir = Path.of("target/test-output", fileName.replace(".json", ""));

        // Delete existing files if directory exists
        if (Files.exists(outputDir)) {
            Files.walk(outputDir)
                    .sorted((a, b) -> -a.compareTo(b)) // Delete files before directories
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                            // Ignore
                        }
                    });
        }

        Files.createDirectories(outputDir);

        Platform.runLater(() -> {
            try {
                // Create LottiePlayer
                LottiePlayer player = new LottiePlayer(animation, CANVAS_WIDTH, CANVAS_HEIGHT);

                // Create WebView and add to scene (required for rendering)
                WebView webView = new WebView();
                webView.setPrefSize(CANVAS_WIDTH, CANVAS_HEIGHT);
                webView.setMaxSize(CANVAS_WIDTH, CANVAS_HEIGHT);
                webView.setMinSize(CANVAS_WIDTH, CANVAS_HEIGHT);

                // Create scene with WebView (needed for proper rendering)
                javafx.scene.layout.StackPane root = new javafx.scene.layout.StackPane(webView);
                javafx.scene.Scene scene = new javafx.scene.Scene(root, CANVAS_WIDTH, CANVAS_HEIGHT);
                primaryStage.setScene(scene);
                primaryStage.show();

                // Enable JavaScript console logging
                webView.getEngine().setOnAlert(event -> System.out.println("JS Alert: " + event.getData()));
                webView.getEngine().setOnError(event -> System.err.println("JS Error: " + event.getMessage()));

                // Load Lottie animation in WebView
                String htmlContent = generateLottieHtml(lottieFile);
                System.out.println("Loading HTML content into WebView...");

                // Set up listener BEFORE loading content
                webView.getEngine().getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
                    System.out.println("WebView load state: " + newState);
                    if (newState == javafx.concurrent.Worker.State.SUCCEEDED) {
                        System.out.println("WebView loaded successfully");
                        try {
                            // Additional wait for lottie animation to initialize
                            Thread.sleep(1000);

                            // Debug: Check if lottie library and animation loaded
                            Object lottieExists = webView.getEngine().executeScript("typeof lottie !== 'undefined'");
                            Object animReady = webView.getEngine().executeScript("window.isAnimationReady()");
                            System.out.println("Lottie library loaded: " + lottieExists);
                            System.out.println("Animation ready: " + animReady);

                            // Debug: Check actual dimensions
                            Object containerWidth = webView.getEngine().executeScript("document.getElementById('lottie').offsetWidth");
                            Object containerHeight = webView.getEngine().executeScript("document.getElementById('lottie').offsetHeight");
                            Object svgCount = webView.getEngine().executeScript("document.querySelectorAll('svg').length");
                            System.out.println("Lottie container size: " + containerWidth + "x" + containerHeight);
                            System.out.println("SVG elements found: " + svgCount);
                            System.out.println("WebView size: " + webView.getWidth() + "x" + webView.getHeight());

                            if (!Boolean.TRUE.equals(animReady)) {
                                System.err.println("Animation not ready after 1 second!");
                                Object errorCheck = webView.getEngine().executeScript(
                                        "typeof lottie === 'undefined' ? 'lottie not loaded' : " +
                                                "(animation === null ? 'animation null' : 'animation exists')"
                                );
                                System.err.println("Error check: " + errorCheck);
                            }

                            // Get frame range
                            int inPoint = animation.inPoint() != null ? animation.inPoint() : 0;
                            int outPoint = animation.outPoint() != null ? animation.outPoint() : 60;

                            // Sample every 5th frame to reduce test time
                            int frameStep = 5;
                            double totalSimilarity = 0;
                            int frameCount = 0;

                            for (int frame = inPoint; frame <= outPoint; frame += frameStep) {
                                // Render frame in LottiePlayer
                                player.seekToFrame(frame);
                                WritableImage playerImage = player.snapshot(new SnapshotParameters(), null);

                                // Render frame in WebView (includes waiting for render)
                                setWebViewFrame(webView, frame);
                                WritableImage webViewImage = webView.snapshot(new SnapshotParameters(), null);

                                // Compare images
                                double similarity = compareImages(playerImage, webViewImage);
                                totalSimilarity += similarity;
                                frameCount++;

                                // Save images for debugging if similarity is low
                                if (similarity < SIMILARITY_THRESHOLD) {
                                    System.out.printf("Similarity %.2f%% below threshold, saving frame %d%n", similarity, frame);
                                    saveImage(playerImage, webViewImage, outputDir.resolve("frame_" + frame + ".png"));
                                }

                                System.out.printf("Frame %d: %.2f%% similar%n", frame, similarity);
                            }

                            double averageSimilarity = totalSimilarity / frameCount;
                            System.out.printf("%s - Average similarity: %.2f%%%n", fileName, averageSimilarity);

                            // Assert average similarity meets threshold
                            assertTrue(averageSimilarity >= SIMILARITY_THRESHOLD,
                                    String.format("%s failed: Average similarity %.2f%% is below threshold %.2f%%",
                                            fileName, averageSimilarity, SIMILARITY_THRESHOLD));

                        } catch (Exception e) {
                            fail("Error comparing frames: " + e.getMessage());
                        } finally {
                            latch.countDown();
                        }
                    } else if (newState == javafx.concurrent.Worker.State.FAILED) {
                        fail("WebView failed to load");
                        latch.countDown();
                    }
                });

                // Now load the content (listener is already set up)
                webView.getEngine().loadContent(htmlContent);

            } catch (Exception e) {
                fail("Error setting up comparison: " + e.getMessage());
                latch.countDown();
            }
        });

        assertTrue(latch.await(60, TimeUnit.SECONDS), "Frame comparison timed out");
    }

    private String generateLottieHtml(File lottieFile) throws IOException {
        String lottieJson = Files.readString(lottieFile.toPath());
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <style>
                        * { margin: 0; padding: 0; box-sizing: border-box; }
                        html, body { width: 100%%; height: 100%%; overflow: hidden; background: white; }
                        #lottie {
                            width: %dpx;
                            height: %dpx;
                            position: absolute;
                            top: 50%%;
                            left: 50%%;
                            transform: translate(-50%%, -50%%);
                        }
                        #lottie svg {
                            width: 100%% !important;
                            height: 100%% !important;
                        }
                    </style>
                    <script src="https://cdnjs.cloudflare.com/ajax/libs/lottie-web/5.12.2/lottie.min.js"></script>
                </head>
                <body>
                    <div id="lottie"></div>
                    <script>
                        var animData = %s;
                        var animation = null;
                
                        // Initialize after DOM and lottie library are loaded
                        window.addEventListener('load', function() {
                            if (typeof lottie !== 'undefined') {
                                animation = lottie.loadAnimation({
                                    container: document.getElementById('lottie'),
                                    renderer: 'svg',
                                    loop: false,
                                    autoplay: false,
                                    animationData: animData
                                });
                            }
                        });
                
                        var currentFrame = -1;
                        var renderComplete = false;
                
                        window.goToFrame = function(frame) {
                            if (animation) {
                                renderComplete = false;
                                animation.goToAndStop(frame, true);
                                currentFrame = frame;
                
                                // Force a small delay to allow render
                                setTimeout(function() {
                                    renderComplete = true;
                                }, 50);
                
                                return true;
                            }
                            return false;
                        };
                
                        window.isAnimationReady = function() {
                            return animation !== null;
                        };
                
                        window.isRenderComplete = function() {
                            return renderComplete;
                        };
                
                        window.getCurrentFrame = function() {
                            return currentFrame;
                        };
                    </script>
                </body>
                </html>
                """.formatted(CANVAS_WIDTH, CANVAS_HEIGHT, lottieJson);
    }

    private void setWebViewFrame(WebView webView, int frame) throws InterruptedException {
        // Wait for animation to be ready
        int attempts = 0;
        while (attempts < 50) {
            Object ready = webView.getEngine().executeScript("window.isAnimationReady()");
            if (Boolean.TRUE.equals(ready)) {
                break;
            }
            Thread.sleep(100);
            attempts++;
        }

        System.out.println("Setting WebView to frame: " + frame);

        // Set the frame
        Object result = webView.getEngine().executeScript("window.goToFrame(" + frame + ")");
        if (!Boolean.TRUE.equals(result)) {
            throw new RuntimeException("Failed to set WebView frame to " + frame);
        }

        // Wait for render to complete
        attempts = 0;
        while (attempts < 20) {
            Object renderComplete = webView.getEngine().executeScript("window.isRenderComplete()");
            if (Boolean.TRUE.equals(renderComplete)) {
                break;
            }
            Thread.sleep(50);
            attempts++;
        }

        // Debug: Check what frame we're actually on
        Object actualFrame = webView.getEngine().executeScript("window.getCurrentFrame()");
        Object animationFrame = webView.getEngine().executeScript("animation ? animation.currentFrame : 'no animation'");
        System.out.println("  Requested frame: " + frame + ", Current frame: " + actualFrame + ", Animation frame: " + animationFrame);

        // Give WebView additional time to finish painting
        Thread.sleep(200);
    }

    private double compareImages(WritableImage img1, WritableImage img2) {
        int width = (int) Math.min(img1.getWidth(), img2.getWidth());
        int height = (int) Math.min(img1.getHeight(), img2.getHeight());

        int totalPixels = width * height;
        int matchingPixels = 0;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int argb1 = img1.getPixelReader().getArgb(x, y);
                int argb2 = img2.getPixelReader().getArgb(x, y);

                // Calculate color difference (with tolerance for anti-aliasing)
                int tolerance = 10; // Allow small differences
                if (colorDistance(argb1, argb2) <= tolerance) {
                    matchingPixels++;
                }
            }
        }

        return (matchingPixels * 100.0) / totalPixels;
    }

    private int colorDistance(int argb1, int argb2) {
        int a1 = (argb1 >> 24) & 0xFF;
        int r1 = (argb1 >> 16) & 0xFF;
        int g1 = (argb1 >> 8) & 0xFF;
        int b1 = argb1 & 0xFF;

        int a2 = (argb2 >> 24) & 0xFF;
        int r2 = (argb2 >> 16) & 0xFF;
        int g2 = (argb2 >> 8) & 0xFF;
        int b2 = argb2 & 0xFF;

        return Math.abs(a1 - a2) + Math.abs(r1 - r2) + Math.abs(g1 - g2) + Math.abs(b1 - b2);
    }

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
