package com.lottie4j.fxfileviewer;

import com.lottie4j.core.file.LottieFileLoader;
import com.lottie4j.core.model.Animation;
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
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import java.util.zip.Deflater;

import static org.junit.jupiter.api.Assertions.*;

public class CompareWithWebViewTest {

    private static final Logger logger = LoggerFactory.getLogger(CompareWithWebViewTest.class);

    private static final double SIMILARITY_THRESHOLD = 95.0; // 95% similarity required
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
                "timeline_animation.json",
                "timeline_animation_polygon_5.json"
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

                // Load Lottie animation in WebView
                String htmlContent = generateLottieHtml(lottieFile);

                // Set up listener BEFORE loading content
                webView.getEngine().getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
                    if (newState == javafx.concurrent.Worker.State.SUCCEEDED) {
                        try {
                            // Additional wait for lottie animation to initialize
                            Thread.sleep(1000);

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

                                // Render frame in WebView
                                setWebViewFrame(webView, frame);
                                Thread.sleep(100); // Allow WebView to render
                                WritableImage webViewImage = webView.snapshot(new SnapshotParameters(), null);

                                // Compare images
                                double similarity = compareImages(playerImage, webViewImage);
                                totalSimilarity += similarity;
                                frameCount++;

                                // Save images for debugging if similarity is low
                                if (similarity < SIMILARITY_THRESHOLD) {
                                    System.out.printf("Similarity %.2f%% below threshold, saving frame %d%n", similarity, frame);
                                    saveImage(playerImage, outputDir.resolve("frame_" + frame + "_fx.png"));
                                    saveImage(webViewImage, outputDir.resolve("frame_" + frame + "_js.png"));
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
                        body { margin: 0; padding: 0; overflow: hidden; background: white; }
                        #lottie { width: %dpx; height: %dpx; }
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
                
                        window.goToFrame = function(frame) {
                            if (animation) {
                                animation.goToAndStop(frame, true);
                                return true;
                            }
                            return false;
                        };
                
                        window.isAnimationReady = function() {
                            return animation !== null;
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

        Object result = webView.getEngine().executeScript("window.goToFrame(" + frame + ")");
        if (!Boolean.TRUE.equals(result)) {
            throw new RuntimeException("Failed to set WebView frame to " + frame);
        }
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

    private void saveImage(WritableImage image, Path path) throws IOException {
        int width = (int) image.getWidth();
        int height = (int) image.getHeight();
        PixelReader pixelReader = image.getPixelReader();

        // Get pixel data in ARGB format
        int[] pixels = new int[width * height];
        pixelReader.getPixels(0, 0, width, height, PixelFormat.getIntArgbInstance(), pixels, 0, width);

        // Write PNG file
        try (FileOutputStream fos = new FileOutputStream(path.toFile())) {
            writePNG(fos, pixels, width, height);
        }
    }

    private void writePNG(FileOutputStream fos, int[] pixels, int width, int height) throws IOException {
        // PNG signature
        fos.write(new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A});

        // IHDR chunk
        writeChunk(fos, "IHDR", createIHDR(width, height));

        // IDAT chunk (image data)
        writeChunk(fos, "IDAT", compressImageData(pixels, width, height));

        // IEND chunk
        writeChunk(fos, "IEND", new byte[0]);
    }

    private byte[] createIHDR(int width, int height) {
        ByteBuffer buffer = ByteBuffer.allocate(13);
        buffer.order(ByteOrder.BIG_ENDIAN);
        buffer.putInt(width);
        buffer.putInt(height);
        buffer.put((byte) 8);  // bit depth
        buffer.put((byte) 6);  // color type (RGBA)
        buffer.put((byte) 0);  // compression method
        buffer.put((byte) 0);  // filter method
        buffer.put((byte) 0);  // interlace method
        return buffer.array();
    }

    private byte[] compressImageData(int[] pixels, int width, int height) throws IOException {
        // Convert ARGB pixels to RGBA bytes with filter byte per scanline
        int rowBytes = width * 4 + 1;  // 4 bytes per pixel + 1 filter byte
        byte[] imageData = new byte[rowBytes * height];

        for (int y = 0; y < height; y++) {
            int rowStart = y * rowBytes;
            imageData[rowStart] = 0;  // filter type: none

            for (int x = 0; x < width; x++) {
                int pixel = pixels[y * width + x];
                int idx = rowStart + 1 + x * 4;

                // Convert ARGB to RGBA
                imageData[idx] = (byte) ((pixel >> 16) & 0xFF);  // R
                imageData[idx + 1] = (byte) ((pixel >> 8) & 0xFF);  // G
                imageData[idx + 2] = (byte) (pixel & 0xFF);  // B
                imageData[idx + 3] = (byte) ((pixel >> 24) & 0xFF);  // A
            }
        }

        // Compress with deflate
        return deflate(imageData);
    }

    private byte[] deflate(byte[] data) throws IOException {
        Deflater deflater = new Deflater();
        deflater.setInput(data);
        deflater.finish();

        byte[] buffer = new byte[data.length + 100];
        int compressedSize = deflater.deflate(buffer);
        deflater.end();

        byte[] result = new byte[compressedSize];
        System.arraycopy(buffer, 0, result, 0, compressedSize);
        return result;
    }

    private void writeChunk(FileOutputStream fos, String type, byte[] data) throws IOException {
        // Write length
        writeInt(fos, data.length);

        // Write type
        fos.write(type.getBytes());

        // Write data
        fos.write(data);

        // Write CRC
        int crc = calculateCRC(type.getBytes(), data);
        writeInt(fos, crc);
    }

    private void writeInt(FileOutputStream fos, int value) throws IOException {
        fos.write((value >> 24) & 0xFF);
        fos.write((value >> 16) & 0xFF);
        fos.write((value >> 8) & 0xFF);
        fos.write(value & 0xFF);
    }

    private int calculateCRC(byte[] type, byte[] data) {
        int crc = 0xFFFFFFFF;

        // Process type
        for (byte b : type) {
            crc = updateCRC(crc, b);
        }

        // Process data
        for (byte b : data) {
            crc = updateCRC(crc, b);
        }

        return crc ^ 0xFFFFFFFF;
    }

    private int updateCRC(int crc, byte b) {
        crc ^= (b & 0xFF);
        for (int i = 0; i < 8; i++) {
            if ((crc & 1) != 0) {
                crc = (crc >>> 1) ^ 0xEDB88320;
            } else {
                crc = crc >>> 1;
            }
        }
        return crc;
    }
}
