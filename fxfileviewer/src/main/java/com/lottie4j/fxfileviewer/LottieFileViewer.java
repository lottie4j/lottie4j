package com.lottie4j.fxfileviewer;

import com.lottie4j.core.loader.LottieFileLoader;
import com.lottie4j.core.model.Animation;
import com.lottie4j.fxfileviewer.component.LottieTreeView;
import com.lottie4j.fxfileviewer.util.CompactFormatter;
import com.lottie4j.fxplayer.LottiePlayer;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.logging.LogManager;
import java.util.logging.Logger;

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
        rootLogger.getHandlers()[0].setFormatter(new CompactFormatter());
    }

    private Canvas canvas;
    private GraphicsContext gc;
    private Animation animation;
    private int currentFrame = 0;
    private BorderPane root;

    // UI Controls
    private Button startButton;
    private Button pauseButton;
    private Button stopButton;
    private Slider frameSlider;
    private Label frameLabel;
    private Label fpsLabel;
    private LottiePlayer lottiePlayer;
    private WebEngine webEngine;
    private Color backgroundColor = Color.WHITE;

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Lottie4J Animation Viewer");

        // Create main layout
        root = new BorderPane();

        // Create canvas for animation rendering
        canvas = new Canvas(500, 500);
        gc = canvas.getGraphicsContext2D();

        // Create WebView for JavaScript Lottie player
        var webView = new WebView();
        webEngine = webView.getEngine();
        webView.setPrefSize(500, 500);
        webView.setMaxSize(500, 500);

        // Create HBox to hold both players side by side
        var playersBox = new HBox(10);
        playersBox.setPadding(new Insets(10));
        playersBox.setAlignment(Pos.CENTER);

        var javaFXPlayerBox = new VBox(5);
        javaFXPlayerBox.setAlignment(Pos.TOP_CENTER);
        Label javaFXLabel = new Label("JavaFX Lottie4J Player");
        javaFXLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        javaFXPlayerBox.getChildren().addAll(javaFXLabel, canvas);

        var webPlayerBox = new VBox(5);
        webPlayerBox.setAlignment(Pos.TOP_CENTER);
        Label webLabel = new Label("JavaScript Lottie-Web Player");
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
        stopButton = new Button("⏹ Stop");

        startButton.setOnAction(e -> startAnimation());
        pauseButton.setOnAction(e -> pauseAnimation());
        stopButton.setOnAction(e -> stopAnimation());

        // Initially disable controls
        startButton.setDisable(true);
        pauseButton.setDisable(true);
        stopButton.setDisable(true);

        playbackControls.getChildren().addAll(startButton, pauseButton, stopButton);

        // Frame controls
        var frameControls = new HBox(10);
        frameSlider = new Slider();
        frameSlider.setDisable(true);
        frameSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (lottiePlayer != null && !lottiePlayer.isPlaying()) {
                currentFrame = newVal.intValue();
                lottiePlayer.seekToFrame(currentFrame);
                updateFrameLabel();

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

        controlPanel.getChildren().addAll(playbackControls, colorPicker, fpsLabel, frameControls);
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

        // Enable controls
        startButton.setDisable(false);
        pauseButton.setDisable(false);
        stopButton.setDisable(false);
        frameSlider.setDisable(false);

        // Setup frame slider
        frameSlider.setMin(getInPoint());
        frameSlider.setMax(getOutPoint());
        frameSlider.setValue(getInPoint());

        // Update labels
        fpsLabel.setText("FPS: " + String.format("%d", getFramesPerSecond()));
        updateFrameLabel();
    }

    private void loadAnimation(File file) {
        stopAnimation();

        try {
            animation = LottieFileLoader.load(file);

            // Remove existing LottiePlayer if any
            if (lottiePlayer != null) {
                root.getChildren().remove(lottiePlayer);
            }

            // Show the lottie file structure
            var treeViewer = new LottieTreeView(file.getName(), animation);
            treeViewer.setPrefWidth(420);
            root.setRight(treeViewer);

            // Show new LottiePlayer
            lottiePlayer = new LottiePlayer(animation, true);
            lottiePlayer.setBackgroundColor(backgroundColor);

            // Get animation size
            var width = animation.width() != null ? animation.width() : 500;
            var height = animation.height() != null ? animation.height() : 500;

            // Update the JavaFX player box
            var playersBox = (HBox) root.getCenter();
            var javaFXPlayerBox = (VBox) playersBox.getChildren().get(0);
            if (javaFXPlayerBox.getChildren().size() > 1) {
                javaFXPlayerBox.getChildren().remove(1);
            }
            javaFXPlayerBox.getChildren().add(lottiePlayer);
            javaFXPlayerBox.setPrefSize(width, height);

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

            // Ensure JS player also starts at the initial frame
            try {
                webEngine.executeScript("window.seekToFrame(" + getInPoint() + ")");
            } catch (Exception e) {
                logger.warning("Failed to initialize JS animation frame: " + e.getMessage());
            }
        } catch (IOException e) {
            logger.severe("Failed to load animation: " + e.getMessage());
            showError("Failed to load animation: " + e.getMessage());
        }
    }

    // Update play/pause/stop methods:
    private void startAnimation() {
        if (lottiePlayer != null) {
            // Synchronize both players - start FX player first
            lottiePlayer.play();
            startButton.setDisable(true);

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

            // Pause JS player as well
            try {
                webEngine.executeScript("window.pauseAnimation()");
            } catch (Exception e) {
                logger.warning("Failed to pause JS animation: " + e.getMessage());
            }
        }
    }

    private void stopAnimation() {
        if (lottiePlayer != null) {
            lottiePlayer.stop();
            lottiePlayer.seekToFrame(getInPoint());
            currentFrame = getInPoint();
            frameSlider.setValue(currentFrame);
            updateFrameLabel();
            startButton.setDisable(false);

            // Stop JS player as well
            try {
                webEngine.executeScript("window.stopAnimation()");
            } catch (Exception e) {
                logger.warning("Failed to stop JS animation: " + e.getMessage());
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
            webEngine.documentProperty().addListener((obs, oldDoc, newDoc) -> {
                // Start the animation if the page is loaded
                startAnimation();
            });
        } catch (IOException e) {
            logger.severe("Failed to load Lottie file in WebView: " + e.getMessage());
        }
    }

    private void updateBackgroundColor() {
        // Update FX player background
        if (lottiePlayer != null) {
            lottiePlayer.setBackgroundColor(backgroundColor);
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
}