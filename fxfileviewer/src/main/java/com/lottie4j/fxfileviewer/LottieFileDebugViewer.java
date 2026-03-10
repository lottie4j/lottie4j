package com.lottie4j.fxfileviewer;

import com.lottie4j.core.file.LottieFileLoader;
import com.lottie4j.core.model.Animation;
import com.lottie4j.fxfileviewer.component.LayerTileView;
import com.lottie4j.fxfileviewer.component.LayerTreeView;
import com.lottie4j.fxfileviewer.component.LottieTreeView;
import com.lottie4j.fxfileviewer.component.ViewerMenuBar;
import com.lottie4j.fxfileviewer.util.AlertHelper;
import com.lottie4j.fxfileviewer.util.ImageSaver;
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
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;

/**
 * JavaFX Lottie Animation Viewer
 * This viewer can load and play Lottie animations using a basic rendering engine
 * that works with the lottie4j data model structure.
 * <p>
 * You can start this application with the file name as command line option,
 * for example, `"box-moving-changing-color.json"`.
 */
public class LottieFileDebugViewer extends Application {
    private static final Logger logger = LoggerFactory.getLogger(LottieFileDebugViewer.class);

    private Canvas canvas;
    private GraphicsContext gc;
    private Animation animation;
    private int currentFrame = 0;
    private BorderPane root;

    // UI Controls
    private Button playLoopButton;
    private Button playOnceButton;
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

    /**
     * Creates a new LottieFileDebugViewer.
     */
    public LottieFileDebugViewer() {
    }

    /**
     * Initializes the JavaFX application with dual-view rendering.
     * Sets up both JavaFX and WebView players side-by-side with controls and layer inspection tools.
     *
     * @param primaryStage the primary stage for this application
     */
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
        root.setTop(new ViewerMenuBar(primaryStage, this::loadAnimation));

        // Set up scene
        var scene = new Scene(root, 1600, 800);
        primaryStage.setScene(scene);
        primaryStage.show();

        // Initialize canvas
        clearCanvas();

        // Handle command-line arguments
        var args = getParameters();
        if (!args.getUnnamed().isEmpty()) {
            var fileName = args.getUnnamed().getFirst();
            var file = new File(fileName);
            if (!file.exists()) {
                logger.warn("The Lottie file can not be found: {}", fileName);
                return;
            }
            loadAnimation(file);
        }
    }

    /**
     * Called when the application is being stopped.
     * Ensures any playing animation is stopped and UI is reset.
     */
    @Override
    public void stop() {
        if (lottiePlayer != null) {
            lottiePlayer.stop();
            playLoopButton.setDisable(false);
            playOnceButton.setDisable(false);
            pauseButton.setDisable(true);
            currentFrame = getInPoint();
            frameSlider.setValue(currentFrame);
            updateFrameLabel();
        }
    }

    /**
     * Gets the starting frame (in-point) of the current animation.
     *
     * @return the in-point frame number, or 0 if not defined
     */
    private int getInPoint() {
        return animation != null && animation.inPoint() != null ? animation.inPoint() : 0;
    }

    /**
     * Gets the ending frame (out-point) of the current animation.
     *
     * @return the out-point frame number, or 60 if not defined
     */
    private int getOutPoint() {
        return animation != null && animation.outPoint() != null ? animation.outPoint() : 60;
    }

    /**
     * Gets the frames per second rate of the current animation.
     *
     * @return the FPS value, or 30 if not defined
     */
    private int getFramesPerSecond() {
        return animation != null && animation.framesPerSecond() != null ? animation.framesPerSecond() : 30;
    }

    /**
     * Creates the control panel with playback controls, frame slider, and other UI elements.
     *
     * @return HBox containing all control panel components
     */
    private HBox createControlPanel() {
        var controlPanel = new HBox(10);
        controlPanel.setPadding(new Insets(10));
        controlPanel.setStyle("-fx-background-color: #f0f0f0;");

        // Playback controls
        var playbackControls = new HBox(5);
        playLoopButton = new Button("▶ Play Loop");
        playOnceButton = new Button("▶ Play Once");
        pauseButton = new Button("⏸ Pause");

        playLoopButton.setOnAction(e -> startAnimationLoop());
        playOnceButton.setOnAction(e -> startAnimationOnce());
        pauseButton.setOnAction(e -> pauseAnimation());

        // Initially disable controls
        playLoopButton.setDisable(true);
        playOnceButton.setDisable(true);
        pauseButton.setDisable(true);

        playbackControls.getChildren().addAll(playLoopButton, playOnceButton, pauseButton);

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
                    logger.warn("Failed to seek JS animation: {}", e.getMessage());
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

    /**
     * Configures animation control UI elements based on the loaded animation properties.
     * Enables buttons, sets up the frame slider range, and updates labels.
     */
    private void setupAnimationControls() {
        if (animation == null) return;

        // Enable controls - Play buttons enabled, Pause disabled initially
        playLoopButton.setDisable(false);
        playOnceButton.setDisable(false);
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

    /**
     * Loads a Lottie animation file and sets up all viewer components.
     * Creates player instances, layer trees, and synchronizes UI elements.
     *
     * @param file the Lottie animation file to load
     */
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
            logger.error("Failed to load animation: {}", e.getMessage());
            AlertHelper.showError("Failed to load animation: " + e.getMessage());
        }
    }

    /**
     * Starts animation playback in loop mode for both JavaFX and WebView players.
     * Synchronizes both renderers and updates button states.
     */
    private void startAnimationLoop() {
        if (lottiePlayer != null) {
            // Synchronize both players - start FX player first
            lottiePlayer.play();
            playLoopButton.setDisable(true);
            playOnceButton.setDisable(true);
            pauseButton.setDisable(false);

            // Then immediately start JS player
            try {
                webEngine.executeScript("window.playAnimation()");
            } catch (Exception e) {
                logger.warn("Failed to start JS animation: " + e.getMessage());
            }
        }
    }

    /**
     * Plays animation once from start to end for both players.
     * Re-enables play buttons when animation completes.
     */
    private void startAnimationOnce() {
        if (lottiePlayer != null) {
            // Start FX player once
            lottiePlayer.playOnceFromStart(file -> {
                // Re-enable buttons when animation completes
                playLoopButton.setDisable(false);
                playOnceButton.setDisable(false);
                pauseButton.setDisable(true);
            });
            playLoopButton.setDisable(true);
            playOnceButton.setDisable(true);
            pauseButton.setDisable(false);

            // Start JS player once (note: lottie-web doesn't have built-in play once, so we pause at end)
            try {
                webEngine.executeScript("window.playAnimationOnce()");
            } catch (Exception e) {
                logger.warn("Failed to start JS animation once: " + e.getMessage());
            }
        }
    }

    /**
     * Pauses both JavaFX and WebView animation players.
     * Updates button states to reflect paused state.
     */
    private void pauseAnimation() {
        if (lottiePlayer != null) {
            lottiePlayer.stop();
            playLoopButton.setDisable(false);
            playOnceButton.setDisable(false);
            pauseButton.setDisable(true);

            // Pause JS player as well
            try {
                webEngine.executeScript("window.pauseAnimation()");
            } catch (Exception e) {
                logger.warn("Failed to pause JS animation: " + e.getMessage());
            }
        }
    }

    /**
     * Clears the canvas with a placeholder message when no animation is loaded.
     */
    private void clearCanvas() {
        gc.setFill(Color.LIGHTGRAY);
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

        gc.setFill(Color.DARKGRAY);
        gc.fillText("Load a Lottie animation to begin",
                canvas.getWidth() / 2 - 100, canvas.getHeight() / 2);
    }

    /**
     * Updates the frame label to display the current frame and total frames.
     */
    private void updateFrameLabel() {
        if (animation != null) {
            frameLabel.setText(String.format("Frame: %d / %d",
                    currentFrame, getOutPoint()));
        }
    }

    /**
     * Loads a Lottie animation into the WebView using the lottie-web JavaScript library.
     * Generates HTML with embedded animation data and control functions.
     *
     * @param lottieFile the Lottie JSON file to load
     * @param width the width of the player in pixels
     * @param height the height of the player in pixels
     */
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
                                animation.loop = true;
                                animation.play();
                            };
                    
                            window.playAnimationOnce = function() {
                                animation.loop = false;
                                animation.goToAndPlay(0, true);
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
            logger.error("Failed to load Lottie file in WebView: {}", e.getMessage());
        }
    }

    /**
     * Updates the background color for all player views and components.
     * Applies the color to JavaFX player, layer views, and WebView.
     */
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
            logger.warn("Failed to update JS background color: {}", e.getMessage());
        }
    }

    /**
     * Takes a screenshot of both players at the current frame.
     * Saves the combined image to the screenshot directory.
     */
    private void takeScreenshot() {
        if (lottiePlayer == null || webView == null || currentAnimationFile == null) {
            return;
        }

        takeScreenshotPlayers();
    }

    /**
     * Takes screenshots of all frames from in-point to out-point.
     * Runs asynchronously to avoid blocking the UI thread.
     */
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
                                logger.warn("Failed to seek JS animation to frame {}: {}", currentFrame, e.getMessage());
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
                logger.info("All screenshots saved ({} frames)", totalFrames);

                // Re-enable buttons on JavaFX thread
                Platform.runLater(() -> {
                    screenshotButton.setDisable(false);
                    screenshotAllButton.setDisable(false);
                });

            } catch (InterruptedException e) {
                logger.error("Failed to save screenshots: {}", e.getMessage());
                Platform.runLater(() -> {
                    AlertHelper.showError("Failed to save screenshots: " + e.getMessage());
                    screenshotButton.setDisable(false);
                    screenshotAllButton.setDisable(false);
                });
            }
        }).start();
    }

    /**
     * Captures and saves a snapshot of both player views to a PNG file.
     * Creates the screenshot directory if it doesn't exist.
     */
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

            logger.info("Screenshot saved: {}", filename);
        } catch (IOException e) {
            logger.error("Failed to save screenshot: {}", e.getMessage());
            AlertHelper.showError("Failed to save screenshot: " + e.getMessage());
        }
    }

    /**
     * Saves a WritableImage to a PNG file using custom PNG encoder.
     *
     * @param image the image to save
     * @param file the destination file
     * @throws IOException if writing the file fails
     */
    private void saveWritableImage(WritableImage image, File file) throws IOException {
        int width = (int) image.getWidth();
        int height = (int) image.getHeight();
        PixelReader pixelReader = image.getPixelReader();

        // Get pixel data in ARGB format
        int[] pixels = new int[width * height];
        pixelReader.getPixels(0, 0, width, height, PixelFormat.getIntArgbInstance(), pixels, 0, width);

        // Write PNG file manually
        try (FileOutputStream fos = new FileOutputStream(file)) {
            ImageSaver.writePNG(fos, pixels, width, height);
        }
    }
}