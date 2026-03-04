package com.lottie4j.fxfileviewer;

import com.lottie4j.core.file.LottieFileLoader;
import com.lottie4j.core.model.Animation;
import com.lottie4j.fxfileviewer.component.LayerTileView;
import com.lottie4j.fxfileviewer.component.LayerTreeView;
import com.lottie4j.fxfileviewer.component.LottieTreeView;
import com.lottie4j.fxfileviewer.util.CompactFormatter;
import com.lottie4j.fxplayer.LottiePlayer;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.zip.CRC32;
import java.util.zip.Deflater;

/**
 * JavaFX Lottie Animation Viewer
 * This viewer can load and play Lottie animations using a basic rendering engine
 * that works with the lottie4j data model structure.
 * <p>
 * You can start this application with the file name as command line option,
 * for example, `"/basic/box-moving-changing-color.json"`.
 */
public class LottieFileViewer extends Application {
    private static final Logger logger = Logger.getLogger(LottieFileViewer.class.getName());

    static {
        Logger rootLogger = LogManager.getLogManager().getLogger("");
        Logger.getLogger("com.lottie4j").setLevel(Level.FINE);
        rootLogger.getHandlers()[0].setLevel(Level.FINE);
        rootLogger.getHandlers()[0].setFormatter(new CompactFormatter());

        // Add file handler
        try {
            FileHandler fileHandler = new FileHandler("lottie4j.log");
            fileHandler.setLevel(Level.FINE);
            fileHandler.setFormatter(new CompactFormatter());
            rootLogger.addHandler(fileHandler);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Canvas canvas;
    private GraphicsContext gc;
    private Animation animation;
    private int currentFrame = 0;
    private BorderPane root;

    // UI Controls
    private Button startButton;
    private Button pauseButton;
    private Slider frameSlider;
    private Label frameLabel;
    private Label fpsLabel;
    private HBox playersBox;
    private LottiePlayer lottiePlayer;
    private WebEngine webEngine;
    private WebView webView;
    private Color backgroundColor = Color.WHITE;
    private File currentAnimationFile;
    private Button screenshotButton;
    private Button screenshotAllButton;
    private LayerTreeView layerTreeView;
    private LayerTileView layerTileView;

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Lottie4J Animation Viewer");

        // Create main layout
        root = new BorderPane();

        // Create canvas for animation rendering
        canvas = new Canvas(500, 500);
        gc = canvas.getGraphicsContext2D();

        // Create WebView for JavaScript Lottie player
        webView = new WebView();
        webEngine = webView.getEngine();
        webView.setPrefSize(500, 500);
        webView.setMaxSize(500, 500);

        // Create HBox to hold both players side by side
        playersBox = new HBox(10);
        playersBox.setPadding(new Insets(10));
        playersBox.setAlignment(Pos.CENTER);

        var javaFXPlayerBox = new VBox(5);
        javaFXPlayerBox.setAlignment(Pos.TOP_CENTER);
        Label javaFXLabel = new Label("JavaFX Lottie Player");
        javaFXLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        javaFXPlayerBox.getChildren().addAll(javaFXLabel, canvas);

        var webPlayerBox = new VBox(5);
        webPlayerBox.setAlignment(Pos.TOP_CENTER);
        Label webLabel = new Label("HTML JS Lottie Player");
        webLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        webPlayerBox.getChildren().addAll(webLabel, webView);

        playersBox.getChildren().addAll(javaFXPlayerBox, webPlayerBox);
        root.setCenter(playersBox);

        root.setBottom(createControlPanel());
        root.setTop(createMenuBar(primaryStage));

        // Set up scene
        var scene = new Scene(root, 1600, 800);
        primaryStage.setScene(scene);
        primaryStage.show();

        // Initialize canvas
        clearCanvas();

        // Handle command-line arguments
        var args = getParameters();
        if (!args.getUnnamed().isEmpty()) {
            var fileName = args.getUnnamed().get(0);
            var r = this.getClass().getResource(fileName);
            if (r == null) {
                logger.warning("The Lottie file can not be found: " + fileName);
                return;
            }
            loadAnimation(new File(r.getFile()));
        }
    }

    @Override
    public void stop() {
        if (lottiePlayer != null) {
            lottiePlayer.stop();
            startButton.setDisable(false);
            pauseButton.setDisable(true);
            currentFrame = getInPoint();
            frameSlider.setValue(currentFrame);
            updateFrameLabel();
        }
    }

    private int getInPoint() {
        return animation != null && animation.inPoint() != null ? animation.inPoint() : 0;
    }

    private int getOutPoint() {
        return animation != null && animation.outPoint() != null ? animation.outPoint() : 60;
    }

    private int getFramesPerSecond() {
        return animation != null && animation.framesPerSecond() != null ? animation.framesPerSecond() : 30;
    }

    private MenuBar createMenuBar(Stage stage) {
        var menuBar = new MenuBar();

        var fileMenu = new Menu("File");
        MenuItem openItem = new MenuItem("Open Lottie File...");
        openItem.setOnAction(e -> openFile(stage));
        fileMenu.getItems().add(openItem);

        var helpMenu = new Menu("Help");
        MenuItem aboutItem = new MenuItem("About");
        aboutItem.setOnAction(e -> showAbout());
        helpMenu.getItems().add(aboutItem);

        menuBar.getMenus().addAll(fileMenu, helpMenu);
        return menuBar;
    }

    private HBox createControlPanel() {
        var controlPanel = new HBox(10);
        controlPanel.setPadding(new Insets(10));
        controlPanel.setStyle("-fx-background-color: #f0f0f0;");

        // Playback controls
        var playbackControls = new HBox(5);
        startButton = new Button("▶ Play");
        pauseButton = new Button("⏸ Pause");

        startButton.setOnAction(e -> startAnimation());
        pauseButton.setOnAction(e -> pauseAnimation());

        // Initially disable controls
        startButton.setDisable(true);
        pauseButton.setDisable(true);

        playbackControls.getChildren().addAll(startButton, pauseButton);

        // Frame controls
        var frameControls = new HBox(10);
        frameSlider = new Slider();
        frameSlider.setPrefWidth(350);
        frameSlider.setDisable(true);
        frameSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (lottiePlayer != null && !lottiePlayer.isPlaying()) {
                currentFrame = newVal.intValue();
                lottiePlayer.seekToFrame(currentFrame);
                updateFrameLabel();

                // Update layer tree view current frame for thumbnail generation
                if (layerTreeView != null) {
                    layerTreeView.setCurrentFrame(currentFrame);
                }

                // Sync JS player frame
                try {
                    webEngine.executeScript("window.seekToFrame(" + currentFrame + ")");
                } catch (Exception e) {
                    logger.warning("Failed to seek JS animation: " + e.getMessage());
                }
            }
        });

        frameLabel = new Label("Frame: 0 / 0");
        frameControls.getChildren().addAll(new Label("Frame:"), frameSlider, frameLabel);

        // FPS
        fpsLabel = new Label("FPS: --");

        // Background color picker
        var colorPicker = new ColorPicker(backgroundColor);
        colorPicker.setOnAction(e -> {
            backgroundColor = colorPicker.getValue();
            updateBackgroundColor();
        });

        // Screenshot buttons
        screenshotButton = new Button("Screenshot");
        screenshotButton.setDisable(true);
        screenshotButton.setOnAction(e -> takeScreenshot());

        screenshotAllButton = new Button("Screenshot All");
        screenshotAllButton.setDisable(true);
        screenshotAllButton.setOnAction(e -> takeAllScreenshots());

        controlPanel.getChildren().addAll(playbackControls, colorPicker, screenshotButton, screenshotAllButton, fpsLabel, frameControls);
        return controlPanel;
    }

    private void openFile(Stage stage) {
        var fileChooser = new FileChooser();
        fileChooser.setTitle("Open Lottie Animation");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Lottie Files", "*.json", "*.lottie")
        );

        var file = fileChooser.showOpenDialog(stage);
        if (file != null) {
            loadAnimation(file);
        }
    }

    private void setupAnimationControls() {
        if (animation == null) return;

        // Enable controls - Play enabled, Pause disabled initially
        startButton.setDisable(false);
        pauseButton.setDisable(true);
        frameSlider.setDisable(false);
        screenshotButton.setDisable(false);
        screenshotAllButton.setDisable(false);

        // Setup frame slider
        frameSlider.setMin(getInPoint());
        frameSlider.setMax(getOutPoint());
        frameSlider.setValue(getInPoint());

        // Update labels
        fpsLabel.setText("FPS: " + String.format("%d", getFramesPerSecond()));
        updateFrameLabel();
    }

    private void loadAnimation(File file) {
        // Pause any currently playing animation
        if (lottiePlayer != null && lottiePlayer.isPlaying()) {
            pauseAnimation();
        }

        // Store current animation file for screenshot naming
        currentAnimationFile = file;

        try {
            animation = LottieFileLoader.load(file);

            // Remove existing LottiePlayer if any
            if (lottiePlayer != null) {
                root.getChildren().remove(lottiePlayer);
            }

            // Show the lottie file structure
            var treeViewer = new LottieTreeView(file.getName(), animation);
            treeViewer.setPrefWidth(420);

            // Create layer tree view (will be populated after LottiePlayer is created)
            TabPane rightPane = new TabPane();
            Tab propertiesTab = new Tab("Properties", treeViewer);
            propertiesTab.setClosable(false);
            rightPane.getTabs().add(propertiesTab);

            rightPane.setPrefWidth(420);
            root.setRight(rightPane);

            // Get animation size
            var originalWidth = animation.width() != null ? animation.width() : 500;
            var originalHeight = animation.height() != null ? animation.height() : 500;

            // Scale down proportionally if width or height > 500
            int width = originalWidth;
            int height = originalHeight;
            if (width > 500 || height > 500) {
                double scale = Math.min(500.0 / width, 500.0 / height);
                width = (int) (width * scale);
                height = (int) (height * scale);
            }

            // Show new LottiePlayer with scaled size
            lottiePlayer = new LottiePlayer(animation, width, height);
            lottiePlayer.setBackgroundColor(backgroundColor);

            // Create and add LayerTreeView tab
            layerTreeView = new LayerTreeView(animation, lottiePlayer);
            layerTreeView.setBackgroundColor(backgroundColor);
            Tab layersTab = new Tab("Layers Tree", layerTreeView);
            layersTab.setClosable(false);
            rightPane.getTabs().add(layersTab);

            // Create and add LayerTileView tab with live previews
            layerTileView = new LayerTileView(animation, lottiePlayer.currentFrameProperty());
            layerTileView.setBackgroundColor(backgroundColor);
            Tab tilesTab = new Tab("Live Tiles", layerTileView);
            tilesTab.setClosable(false);
            rightPane.getTabs().add(tilesTab);

            // Listen for layer visibility changes in tree view and update player
            layerTreeView.getRoot().getChildren().forEach(treeItem -> {
                if (treeItem.getValue().getType() == LayerTreeView.LayerNodeType.LAYER) {
                    treeItem.getValue().getVisibleProperty().addListener((obs, oldVal, newVal) -> {
                        // Update player's visible layer indices
                        lottiePlayer.setVisibleLayerIndices(layerTreeView.getVisibleLayerIndices());
                    });
                }
            });

            // Listen for layer visibility changes in tile view and update player
            layerTileView.getLayerVisibilityMap().forEach((layerIndex, visibleProperty) -> {
                visibleProperty.addListener((obs, oldVal, newVal) -> {
                    // Update player's visible layer indices
                    lottiePlayer.setVisibleLayerIndices(layerTileView.getVisibleLayerIndices());
                });
            });

            // Update the JavaFX player box
            var playersBox = (HBox) root.getCenter();
            var javaFXPlayerBox = (VBox) playersBox.getChildren().get(0);
            if (javaFXPlayerBox.getChildren().size() > 1) {
                javaFXPlayerBox.getChildren().remove(1);
            }
            javaFXPlayerBox.getChildren().add(lottiePlayer);
            javaFXPlayerBox.setPrefSize(width, height);

            // Update WebView size to match animation
            webView.setPrefSize(width, height);
            webView.setMaxSize(width, height);
            webView.setMinSize(width, height);

            // Load animation into JavaScript player
            loadLottieInWebView(file, width, height);

            // Bind frame slider to lottie player's current frame
            lottiePlayer.currentFrameProperty().addListener((obs, oldVal, newVal) -> {
                if (!frameSlider.isValueChanging()) {
                    currentFrame = newVal.intValue();
                    frameSlider.setValue(currentFrame);
                    updateFrameLabel();
                }
            });

            // Reset the animation UI
            setupAnimationControls();
            currentFrame = getInPoint();
        } catch (IOException e) {
            logger.severe("Failed to load animation: " + e.getMessage());
            showError("Failed to load animation: " + e.getMessage());
        }
    }

    // Update play/pause methods:
    private void startAnimation() {
        if (lottiePlayer != null) {
            // Synchronize both players - start FX player first
            lottiePlayer.play();
            startButton.setDisable(true);
            pauseButton.setDisable(false);

            // Then immediately start JS player
            try {
                webEngine.executeScript("window.playAnimation()");
            } catch (Exception e) {
                logger.warning("Failed to start JS animation: " + e.getMessage());
            }
        }
    }

    private void pauseAnimation() {
        if (lottiePlayer != null) {
            lottiePlayer.stop();
            startButton.setDisable(false);
            pauseButton.setDisable(true);

            // Pause JS player as well
            try {
                webEngine.executeScript("window.pauseAnimation()");
            } catch (Exception e) {
                logger.warning("Failed to pause JS animation: " + e.getMessage());
            }
        }
    }

    private void clearCanvas() {
        gc.setFill(Color.LIGHTGRAY);
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

        gc.setFill(Color.DARKGRAY);
        gc.fillText("Load a Lottie animation to begin",
                canvas.getWidth() / 2 - 100, canvas.getHeight() / 2);
    }

    private void updateFrameLabel() {
        if (animation != null) {
            frameLabel.setText(String.format("Frame: %d / %d",
                    currentFrame, getOutPoint()));
        }
    }

    private void showError(String message) {
        var alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showAbout() {
        var alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("About");
        alert.setHeaderText("Lottie4J File Viewer");
        VBox content = new VBox(5);
        content.getChildren().addAll(
                new Label("A JavaFX viewer for Lottie animations."),
                new Label("Built with the Lottie4J library."),
                createClickableLink()
        );

        alert.getDialogPane().setContent(content);
        alert.showAndWait();
    }

    private HBox createClickableLink() {
        var link = new Hyperlink("https://lottie4j.com/");
        link.setOnAction(e -> {
            try {
                // Use JavaFX HostServices to open URL
                getHostServices().showDocument("https://lottie4j.com/");
            } catch (Exception ex) {
                showError("Could not open browser: " + ex.getMessage());
            }
        });

        var linkBox = new HBox(new Label("More info on:"), link);
        linkBox.setAlignment(Pos.BASELINE_LEFT);
        return linkBox;
    }

    private void loadLottieInWebView(File lottieFile, int width, int height) {
        try {
            // Read the Lottie JSON file
            var lottieJson = Files.readString(lottieFile.toPath());

            // Escape JSON for embedding in JavaScript
            var escapedJson = lottieJson.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r");

            // Create HTML with lottie-web player
            var html = """
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
                    
                            // Expose control functions to JavaFX
                            window.playAnimation = function() {
                                animation.play();
                            };
                    
                            window.pauseAnimation = function() {
                                animation.pause();
                            };
                    
                            window.stopAnimation = function() {
                                animation.stop();
                            };
                    
                            window.seekToFrame = function(frame) {
                                animation.goToAndStop(frame, true);
                            };
                    
                            window.setBackgroundColor = function(color) {
                                document.body.style.backgroundColor = color;
                            };
                        </script>
                    </body>
                    </html>
                    """.formatted(width, height, width, height, escapedJson);

            webEngine.loadContent(html);
        } catch (IOException e) {
            logger.severe("Failed to load Lottie file in WebView: " + e.getMessage());
        }
    }

    private void updateBackgroundColor() {
        // Update FX player background
        if (lottiePlayer != null) {
            lottiePlayer.setBackgroundColor(backgroundColor);
        }

        // Update layer tree view background for preview windows
        if (layerTreeView != null) {
            layerTreeView.setBackgroundColor(backgroundColor);
        }

        // Update layer tile view background
        if (layerTileView != null) {
            layerTileView.setBackgroundColor(backgroundColor);
        }

        // Update JS player background
        try {
            var colorHex = String.format("#%02X%02X%02X",
                    (int) (backgroundColor.getRed() * 255),
                    (int) (backgroundColor.getGreen() * 255),
                    (int) (backgroundColor.getBlue() * 255));
            webEngine.executeScript("window.setBackgroundColor('" + colorHex + "')");
        } catch (Exception e) {
            logger.warning("Failed to update JS background color: " + e.getMessage());
        }
    }

    private void takeScreenshot() {
        if (lottiePlayer == null || webView == null || currentAnimationFile == null) {
            return;
        }

        takeScreenshotPlayers();
    }

    private void takeAllScreenshots() {
        if (lottiePlayer == null || webView == null || currentAnimationFile == null || animation == null) {
            return;
        }

        // Pause animation if playing
        boolean wasPlaying = lottiePlayer.isPlaying();
        if (wasPlaying) {
            pauseAnimation();
        }

        // Disable buttons during screenshot capture
        screenshotButton.setDisable(true);
        screenshotAllButton.setDisable(true);

        // Run screenshot capture in separate thread
        new Thread(() -> {
            try {
                int startFrame = getInPoint();
                int endFrame = getOutPoint();

                // Save all frames
                for (int frame = startFrame; frame <= endFrame; frame++) {
                    final int currentFrame = frame;
                    CountDownLatch latch = new CountDownLatch(1);

                    // Update UI on JavaFX thread
                    Platform.runLater(() -> {
                        try {
                            this.currentFrame = currentFrame;
                            lottiePlayer.seekToFrame(currentFrame);
                            frameSlider.setValue(currentFrame);
                            updateFrameLabel();

                            // Sync JS player
                            try {
                                webEngine.executeScript("window.seekToFrame(" + currentFrame + ")");
                            } catch (Exception e) {
                                logger.warning("Failed to seek JS animation to frame " + currentFrame + ": " + e.getMessage());
                            }
                        } finally {
                            latch.countDown();
                        }
                    });

                    // Wait for UI update
                    latch.await();

                    // Wait for rendering to complete
                    Thread.sleep(100);

                    // Take screenshot on JavaFX thread
                    CountDownLatch screenshotLatch = new CountDownLatch(1);

                    Platform.runLater(() -> {
                        try {
                            takeScreenshotPlayers();
                        } finally {
                            screenshotLatch.countDown();
                        }
                    });

                    // Wait for screenshot to complete
                    screenshotLatch.await();
                }

                int totalFrames = endFrame - startFrame + 1;
                logger.info("All screenshots saved (" + totalFrames + " frames)");

                // Re-enable buttons on JavaFX thread
                Platform.runLater(() -> {
                    screenshotButton.setDisable(false);
                    screenshotAllButton.setDisable(false);
                });

            } catch (InterruptedException e) {
                logger.severe("Failed to save screenshots: " + e.getMessage());
                Platform.runLater(() -> {
                    showError("Failed to save screenshots: " + e.getMessage());
                    screenshotButton.setDisable(false);
                    screenshotAllButton.setDisable(false);
                });
            }
        }).start();
    }

    private void takeScreenshotPlayers() {
        try {
            // Create the screenshot directory if it doesn't exist
            Path screenshotDir = Path.of("screenshot");
            Files.createDirectories(screenshotDir);

            // Get base filename without extension
            String baseName = currentAnimationFile.getName().replaceFirst("[.][^.]+$", "");
            String filename = baseName + "_frame_" + currentFrame + ".png";

            // Capture JavaFX and JavaScript player
            WritableImage playerImage = playersBox.snapshot(null, null);
            File screenshotFile = screenshotDir.resolve(filename).toFile();

            // Save
            saveWritableImage(playerImage, screenshotFile);

            logger.info("Screenshot saved: " + filename);
        } catch (IOException e) {
            logger.severe("Failed to save screenshot: " + e.getMessage());
            showError("Failed to save screenshot: " + e.getMessage());
        }
    }

    private void saveWritableImage(WritableImage image, File file) throws IOException {
        int width = (int) image.getWidth();
        int height = (int) image.getHeight();
        PixelReader pixelReader = image.getPixelReader();

        // Get pixel data in ARGB format
        int[] pixels = new int[width * height];
        pixelReader.getPixels(0, 0, width, height, PixelFormat.getIntArgbInstance(), pixels, 0, width);

        // Write PNG file manually
        try (FileOutputStream fos = new FileOutputStream(file)) {
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
        CRC32 crc = new CRC32();
        crc.update(type);
        crc.update(data);
        return (int) crc.getValue();
    }
}