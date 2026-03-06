package com.lottie4j.fxfileviewer;

import com.lottie4j.core.file.LottieFileLoader;
import com.lottie4j.core.model.Animation;
import com.lottie4j.fxfileviewer.util.ImageSaver;
import com.lottie4j.fxplayer.LottiePlayer;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.Label;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
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

class CompareWithWebViewTest {

    private static final Logger logger = LoggerFactory.getLogger(CompareWithWebViewTest.class);

    private static final double SIMILARITY_THRESHOLD = 98;
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
                "interactive_mood_selector_ui.json",
                "isometric_data_analysis.json",
                "java_duke_fadein.json",
                "java_duke_flip.json",
                "java_duke_slidein.json",
                "loading.json",
                "lottie4j.json",
                "lottie_lego.json",
                "sandy_loading.json",
                "snake_ladder_loading_animation.json",
                "success.json",
                "timeline_animation.json"
        );
    }

    @ParameterizedTest
    @MethodSource("lottieJsonFiles")
    @Disabled("WebView rendering requires display - run locally only")
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

                // Create similarity score label overlay
                Label similarityLabel = new Label("Similarity: -- %");
                similarityLabel.setStyle("-fx-font-size: 30; -fx-text-fill: black; -fx-font-weight: bold;");
                similarityLabel.setBackground(new Background(new BackgroundFill(
                        Color.web("#FFFF99", 0.9),
                        new CornerRadii(15),
                        new Insets(20)
                )));
                similarityLabel.setPadding(new Insets(30, 60, 30, 60));

                // Create StackPane to overlay the label on top of the playersBox
                StackPane rootPane = new StackPane(playersBox, similarityLabel);
                StackPane.setAlignment(similarityLabel, javafx.geometry.Pos.CENTER);

                Scene scene = new Scene(rootPane, animWidth * 2 + 10, animHeight);
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
                                        double roundedSimilarity = Math.round(similarity * 100.0) / 100.0;
                                        totalSimilarity.updateAndGet(v -> v + roundedSimilarity);
                                        frameCount.incrementAndGet();

                                        // Update similarity label on JavaFX thread
                                        Platform.runLater(() -> {
                                            similarityLabel.setText(String.format("Frame %d | Similarity: %.2f %%", currentFrame, roundedSimilarity));
                                            // Change color based on similarity
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

                                        // Save images
                                        if (roundedSimilarity < SIMILARITY_THRESHOLD) {
                                            saveImage(playerImage, webViewImage, outputDir.resolve("frame_" + currentFrame + "_similarity_" + roundedSimilarity + ".png"));
                                        }

                                        System.out.printf("Frame %d: %.2f%% similar%n", currentFrame, roundedSimilarity);
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

        // Use structural similarity metric for better accuracy
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

    private int pixelToGrayscale(int argb) {
        int r = (argb >> 16) & 0xFF;
        int g = (argb >> 8) & 0xFF;
        int b = argb & 0xFF;
        // Use standard grayscale conversion
        return (int) (0.299 * r + 0.587 * g + 0.114 * b);
    }

    private double calculateMean(int[][] pixels) {
        double sum = 0;
        for (int[] row : pixels) {
            for (int pixel : row) {
                sum += pixel;
            }
        }
        return sum / (pixels.length * pixels[0].length);
    }

    private double calculateVariance(int[][] pixels, double mean) {
        double sum = 0;
        for (int[] row : pixels) {
            for (int pixel : row) {
                sum += Math.pow(pixel - mean, 2);
            }
        }
        return sum / (pixels.length * pixels[0].length);
    }

    private double calculateCovariance(int[][] pixels1, int[][] pixels2, double mean1, double mean2) {
        double sum = 0;
        for (int y = 0; y < pixels1.length; y++) {
            for (int x = 0; x < pixels1[0].length; x++) {
                sum += (pixels1[y][x] - mean1) * (pixels2[y][x] - mean2);
            }
        }
        return sum / (pixels1.length * pixels1[0].length);
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
