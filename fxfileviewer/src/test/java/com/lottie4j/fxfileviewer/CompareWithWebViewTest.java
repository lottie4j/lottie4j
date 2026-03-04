package com.lottie4j.fxfileviewer;

import com.lottie4j.core.file.LottieFileLoader;
import com.lottie4j.core.model.Animation;
import com.lottie4j.fxfileviewer.util.ImageSaver;
import com.lottie4j.fxplayer.LottiePlayer;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.HBox;
import javafx.scene.web.WebView;
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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

public class CompareWithWebViewTest {

    private static final Logger logger = LoggerFactory.getLogger(CompareWithWebViewTest.class);

    private static final double SIMILARITY_THRESHOLD = 95;
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
                "angry_bird.json",
                "animated_background_patterns.json",
                "box-moving-changing-color.json",
                "box-static.json",
                "circle-static.json",
                //           "interactive_mood_selector_ui.json",
                //           "interactive_mood_selector_ui_bg_isolated.json",
                "isometric_data_analysis.json",
                "java_duke_fadein.json",
                "java_duke_flip.json",
                "java_duke_slidein.json",
                "java_duke_still.json",
                "loading.json",
                "lottie_lego.json",
                "polygon-static.json",
                "sandy_loading.json",
                "snake_ladder_loading_animation.json",
                "star-static.json",
                "success.json",
                "timeline_animation.json",
                "timeline_animation_polygon_5.json"
        );
    }

    @ParameterizedTest
    @MethodSource("lottieJsonFiles")
    @DisabledIfEnvironmentVariable(named = "CI", matches = "true")
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
        var average = compareAnimationFrames(file, animation, fileName);
        assertTrue(average >= SIMILARITY_THRESHOLD, "Similarity threshold not met for "
                + fileName + " " + average + "/" + SIMILARITY_THRESHOLD);
    }

    private double compareAnimationFrames(File lottieFile, Animation animation, String fileName) throws Exception {
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

        // Use actual animation dimensions (matching LottieFileDebugViewer approach)
        int animWidth = animation.width() != null ? animation.width() : CANVAS_WIDTH;
        int animHeight = animation.height() != null ? animation.height() : CANVAS_HEIGHT;

        AtomicReference<Double> totalSimilarity = new AtomicReference<>((double) 0);
        AtomicInteger frameCount = new AtomicInteger(0);

        Platform.runLater(() -> {
            try {
                // Create LottiePlayer with actual animation dimensions
                var fxPlayer = new LottiePlayer(animation, animWidth, animHeight);

                // Create WebView with same dimensions
                WebView webView = new WebView();
                webView.setPrefSize(animWidth, animHeight);
                webView.setMaxSize(animWidth, animHeight);
                webView.setMinSize(animWidth, animHeight);

                // Add both player and webView to scene (matching LottieFileDebugViewer approach)
                var playersBox = new HBox(10);
                playersBox.getChildren().addAll(fxPlayer, webView);
                Scene scene = new Scene(playersBox, animWidth * 2 + 10, animHeight);
                primaryStage.setScene(scene);
                primaryStage.setAlwaysOnTop(true); // Keep visible during test
                primaryStage.toFront();
                primaryStage.show();
                primaryStage.requestFocus();

                // Enable JavaScript console logging
                webView.getEngine().setOnAlert(event -> System.out.println("JS Alert: " + event.getData()));
                webView.getEngine().setOnError(event -> System.err.println("JS Error: " + event.getMessage()));

                // Load Lottie animation in WebView using actual dimensions
                String htmlContent = generateLottieHtml(lottieFile, animWidth, animHeight);

                // Debug: Save HTML to file for inspection
                try {
                    Files.writeString(Path.of("target/test-output/generated.html"), htmlContent);
                    System.out.println("HTML saved to target/test-output/generated.html");
                } catch (IOException e) {
                    System.err.println("Failed to save HTML: " + e.getMessage());
                }

                System.out.println("Loading HTML content into WebView...");

                // Set up listener BEFORE loading content
                webView.getEngine().getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
                    if (newState == Worker.State.SUCCEEDED) {
                        try {
                            // Wait for lottie animation to initialize and first render to complete
                            Thread.sleep(1500);

                            // Get frame range
                            int inPoint = animation.inPoint() != null ? animation.inPoint() : 0;
                            int outPoint = animation.outPoint() != null ? animation.outPoint() : 60;

                            // Run frame comparison in separate thread
                            new Thread(() -> {
                                try {
                                    // Sample every 5th frame to reduce test time
                                    int frameStep = 5;

                                    for (int frame = inPoint; frame <= outPoint; frame += frameStep) {
                                        final int currentFrame = frame;
                                        CountDownLatch updateLatch = new CountDownLatch(1);

                                        // Update frames on JavaFX thread
                                        Platform.runLater(() -> {
                                            try {
                                                fxPlayer.seekToFrame(currentFrame);
                                                webView.getEngine().executeScript("window.seekToFrame(" + currentFrame + ")");

                                                // Force WebView to render by requesting layout
                                                webView.requestLayout();
                                            } finally {
                                                updateLatch.countDown();
                                            }
                                        });

                                        // Wait for update
                                        updateLatch.await();

                                        // Wait for both FX and JS rendering to complete
                                        // WebView needs more time than LottiePlayer for rendering
                                        Thread.sleep(300);

                                        // Verify WebView actually updated to correct frame
                                        CountDownLatch verifyLatch = new CountDownLatch(1);
                                        final int[] actualFrame = new int[1];
                                        final String[] debugInfo = new String[1];
                                        Platform.runLater(() -> {
                                            try {
                                                Object frameObj = webView.getEngine().executeScript("window.getCurrentFrame()");
                                                actualFrame[0] = frameObj instanceof Number ? ((Number) frameObj).intValue() : -1;

                                                // Check if SVG content exists
                                                Object svgCount = webView.getEngine().executeScript("document.querySelectorAll('svg').length");
                                                Object svgDisplay = webView.getEngine().executeScript(
                                                        "var svg = document.querySelector('svg'); svg ? window.getComputedStyle(svg).display : 'no svg'"
                                                );
                                                debugInfo[0] = "SVG count: " + svgCount + ", display: " + svgDisplay;
                                            } finally {
                                                verifyLatch.countDown();
                                            }
                                        });
                                        verifyLatch.await();

                                        if (currentFrame == inPoint) {
                                            System.out.println("WebView debug: " + debugInfo[0]);
                                            System.out.printf("WebView is at frame %d (expected %d)%n", actualFrame[0], currentFrame);
                                        }

                                        if (actualFrame[0] != currentFrame) {
                                            System.out.printf("WARNING: WebView at frame %d but expected %d, waiting longer...%n",
                                                    actualFrame[0], currentFrame);
                                            Thread.sleep(500);
                                        }

                                        // Additional Platform.runLater to ensure rendering pipeline has completed
                                        CountDownLatch renderLatch = new CountDownLatch(1);
                                        Platform.runLater(() -> renderLatch.countDown());
                                        renderLatch.await();

                                        // Take snapshot on JavaFX thread
                                        CountDownLatch snapshotLatch = new CountDownLatch(1);
                                        final WritableImage[] images = new WritableImage[2]; // player, webView

                                        Platform.runLater(() -> {
                                            try {
                                                // Snapshot the entire playersBox (matching LottieFileDebugViewer approach at line 661)
                                                WritableImage combinedImage = playersBox.snapshot(new SnapshotParameters(), null);

                                                // Extract left half (player) and right half (webView) from combined image
                                                WritableImage playerImage = new WritableImage(animWidth, animHeight);
                                                WritableImage webViewImage = new WritableImage(animWidth, animHeight);

                                                playerImage.getPixelWriter().setPixels(0, 0, animWidth, animHeight,
                                                        combinedImage.getPixelReader(), 0, 0);
                                                webViewImage.getPixelWriter().setPixels(0, 0, animWidth, animHeight,
                                                        combinedImage.getPixelReader(), animWidth + 10, 0); // +10 for HBox spacing

                                                images[0] = playerImage;
                                                images[1] = webViewImage;
                                            } finally {
                                                snapshotLatch.countDown();
                                            }
                                        });

                                        // Wait for snapshot
                                        snapshotLatch.await();

                                        WritableImage playerImage = images[0];
                                        WritableImage webViewImage = images[1];

                                        // Compare images
                                        double similarity = compareImages(playerImage, webViewImage);
                                        totalSimilarity.updateAndGet(v -> v + similarity);
                                        frameCount.incrementAndGet();

                                        // Save images
                                        if (similarity < SIMILARITY_THRESHOLD) {
                                            saveImage(playerImage, webViewImage, outputDir.resolve("frame_" + currentFrame + "_similarity_" + similarity + ".png"));
                                        }

                                        System.out.printf("Frame %d: %.2f%% similar%n", currentFrame, similarity);
                                    }
                                } catch (Exception e) {
                                    fail("Error in frame comparison thread: " + e.getMessage());
                                } finally {
                                    latch.countDown();
                                }
                            }).start();

                        } catch (Exception e) {
                            fail("Error comparing frames: " + e.getMessage());
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

        double averageSimilarity = totalSimilarity.get() / frameCount.get();
        System.out.printf("%s - Average similarity: %.2f%%%n", fileName, averageSimilarity);

        return averageSimilarity;
    }

    private String generateLottieHtml(File lottieFile, int width, int height) throws IOException {
        String lottieJson = Files.readString(lottieFile.toPath());

        // Escape JSON for embedding in JavaScript (matching LottieFileDebugViewer approach)
        String escapedJson = lottieJson.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");

        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <style>
                        body {
                            margin: 0;
                            padding: 0;
                            width: %spx;
                            height: %spx;
                            background-color: #ffffff;
                            overflow: hidden;
                        }
                        #lottie-container {
                            width: %spx;
                            height: %spx;
                            margin: 0;
                            padding: 0;
                        }
                    </style>
                    <script src="https://cdnjs.cloudflare.com/ajax/libs/bodymovin/5.12.2/lottie.min.js"></script>
                </head>
                <body>
                    <div id="lottie-container"></div>
                    <script>
                        var animationData = JSON.parse("%s");
                        var animation = lottie.loadAnimation({
                            container: document.getElementById('lottie-container'),
                            renderer: 'svg',
                            loop: true,
                            autoplay: false,
                            animationData: animationData,
                            rendererSettings: {
                                preserveAspectRatio: 'xMidYMid meet'
                            }
                        });
                
                        window.seekToFrame = function(frame) {
                            animation.goToAndStop(frame, true);
                        };
                
                        window.isAnimationReady = function() {
                            return animation !== null;
                        };
                
                        window.getCurrentFrame = function() {
                            return animation ? Math.round(animation.currentFrame) : -1;
                        };
                    </script>
                </body>
                </html>
                """.formatted(width, height, width, height, escapedJson);
    }

    private void setWebViewFrame(WebView webView, int frame) throws InterruptedException {
        // Wait for animation to be ready (simple approach from LottieFileDebugViewer)
        int attempts = 0;
        while (attempts < 50) {
            Object ready = webView.getEngine().executeScript("window.isAnimationReady()");
            if (Boolean.TRUE.equals(ready)) {
                break;
            }
            Thread.sleep(100);
            attempts++;
        }

        // Set the frame (matching LottieFileDebugViewer)
        webView.getEngine().executeScript("window.seekToFrame(" + frame + ")");

        // Give WebView MORE time to finish rendering - WebView rendering is async
        Thread.sleep(500);
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
