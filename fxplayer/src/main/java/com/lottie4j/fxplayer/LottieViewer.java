package com.lottie4j.fxplayer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * JavaFX Lottie Animation Viewer
 * This viewer can load and play Lottie animations using a basic rendering engine
 * that works with the lottie4j data model structure.
 */
public class LottieViewer extends Application {

    private Canvas canvas;
    private GraphicsContext gc;
    private Timeline timeline;
    private LottieAnimation currentAnimation;
    private double currentFrame = 0;
    private boolean isPlaying = false;

    // UI Controls
    private Button playButton;
    private Button pauseButton;
    private Button stopButton;
    private Slider frameSlider;
    private Label frameLabel;
    private Label fpsLabel;
    private ProgressBar progressBar;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Lottie4J Animation Viewer");

        // Create main layout
        BorderPane root = new BorderPane();

        // Create canvas for animation rendering
        canvas = new Canvas(800, 600);
        gc = canvas.getGraphicsContext2D();
        root.setCenter(canvas);

        // Create control panel
        VBox controlPanel = createControlPanel();
        root.setBottom(controlPanel);

        // Create menu bar
        MenuBar menuBar = createMenuBar(primaryStage);
        root.setTop(menuBar);

        // Set up scene
        Scene scene = new Scene(root, 1000, 700);
        primaryStage.setScene(scene);
        primaryStage.show();

        // Initialize canvas
        clearCanvas();
    }

    private MenuBar createMenuBar(Stage stage) {
        MenuBar menuBar = new MenuBar();

        Menu fileMenu = new Menu("File");
        MenuItem openItem = new MenuItem("Open Lottie File...");
        openItem.setOnAction(e -> openFile(stage));
        fileMenu.getItems().add(openItem);

        Menu helpMenu = new Menu("Help");
        MenuItem aboutItem = new MenuItem("About");
        aboutItem.setOnAction(e -> showAbout());
        helpMenu.getItems().add(aboutItem);

        menuBar.getMenus().addAll(fileMenu, helpMenu);
        return menuBar;
    }

    private VBox createControlPanel() {
        VBox controlPanel = new VBox(10);
        controlPanel.setPadding(new Insets(10));
        controlPanel.setStyle("-fx-background-color: #f0f0f0;");

        // Playback controls
        HBox playbackControls = new HBox(5);
        playButton = new Button("▶ Play");
        pauseButton = new Button("⏸ Pause");
        stopButton = new Button("⏹ Stop");

        playButton.setOnAction(e -> play());
        pauseButton.setOnAction(e -> pause());
        stopButton.setOnAction(e -> stop());

        // Initially disable controls
        playButton.setDisable(true);
        pauseButton.setDisable(true);
        stopButton.setDisable(true);

        playbackControls.getChildren().addAll(playButton, pauseButton, stopButton);

        // Frame controls
        HBox frameControls = new HBox(10);
        frameSlider = new Slider();
        frameSlider.setDisable(true);
        frameSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (!isPlaying) {
                currentFrame = newVal.doubleValue();
                renderFrame();
            }
        });

        frameLabel = new Label("Frame: 0 / 0");
        frameControls.getChildren().addAll(new Label("Frame:"), frameSlider, frameLabel);

        // Info panel
        HBox infoPanel = new HBox(20);
        fpsLabel = new Label("FPS: --");
        progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(200);
        infoPanel.getChildren().addAll(fpsLabel, progressBar);

        controlPanel.getChildren().addAll(playbackControls, frameControls, infoPanel);
        return controlPanel;
    }

    private void openFile(Stage stage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open Lottie Animation");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Lottie Files", "*.json", "*.lottie")
        );

        File file = fileChooser.showOpenDialog(stage);
        if (file != null) {
            loadAnimation(file);
        }
    }

    private void loadAnimation(File file) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(file);

            // Parse basic Lottie structure
            currentAnimation = new LottieAnimation();
            currentAnimation.version = rootNode.path("v").asText();
            currentAnimation.frameRate = rootNode.path("fr").asDouble(30.0);
            currentAnimation.inPoint = rootNode.path("ip").asDouble(0.0);
            currentAnimation.outPoint = rootNode.path("op").asDouble(60.0);
            currentAnimation.width = rootNode.path("w").asInt(800);
            currentAnimation.height = rootNode.path("h").asInt(600);
            currentAnimation.name = rootNode.path("nm").asText(file.getName());

            // Parse layers (simplified)
            JsonNode layersNode = rootNode.path("layers");
            currentAnimation.layers = new ArrayList<>();
            if (layersNode.isArray()) {
                for (JsonNode layerNode : layersNode) {
                    LottieLayer layer = new LottieLayer();
                    layer.name = layerNode.path("nm").asText();
                    layer.type = layerNode.path("ty").asInt();
                    layer.startTime = layerNode.path("st").asDouble(0.0);
                    layer.inPoint = layerNode.path("ip").asDouble(0.0);
                    layer.outPoint = layerNode.path("op").asDouble(currentAnimation.outPoint);
                    currentAnimation.layers.add(layer);
                }
            }

            // Update UI
            setupAnimationControls();
            currentFrame = currentAnimation.inPoint;
            renderFrame();

            showInfo("Animation loaded successfully!\n" +
                    "Name: " + currentAnimation.name + "\n" +
                    "Duration: " + (currentAnimation.outPoint - currentAnimation.inPoint) / currentAnimation.frameRate + "s\n" +
                    "Size: " + currentAnimation.width + "x" + currentAnimation.height + "\n" +
                    "Layers: " + currentAnimation.layers.size());

        } catch (IOException e) {
            showError("Failed to load animation: " + e.getMessage());
        }
    }

    private void setupAnimationControls() {
        if (currentAnimation == null) return;

        // Enable controls
        playButton.setDisable(false);
        pauseButton.setDisable(false);
        stopButton.setDisable(false);
        frameSlider.setDisable(false);

        // Setup frame slider
        frameSlider.setMin(currentAnimation.inPoint);
        frameSlider.setMax(currentAnimation.outPoint);
        frameSlider.setValue(currentAnimation.inPoint);

        // Update labels
        fpsLabel.setText("FPS: " + String.format("%.1f", currentAnimation.frameRate));
        updateFrameLabel();
    }

    private void play() {
        if (currentAnimation == null) return;

        isPlaying = true;
        playButton.setDisable(true);

        if (timeline != null) {
            timeline.stop();
        }

        // Create animation timeline
        double frameDuration = 1000.0 / currentAnimation.frameRate; // milliseconds per frame
        timeline = new Timeline(new KeyFrame(Duration.millis(frameDuration), e -> {
            currentFrame++;
            if (currentFrame > currentAnimation.outPoint) {
                currentFrame = currentAnimation.inPoint; // Loop
            }
            renderFrame();
            updateFrameLabel();
            frameSlider.setValue(currentFrame);

            double progress = (currentFrame - currentAnimation.inPoint) /
                    (currentAnimation.outPoint - currentAnimation.inPoint);
            progressBar.setProgress(progress);
        }));

        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }

    private void pause() {
        isPlaying = false;
        playButton.setDisable(false);
        if (timeline != null) {
            timeline.pause();
        }
    }

    @Override
    public void stop() {
        isPlaying = false;
        playButton.setDisable(false);
        if (timeline != null) {
            timeline.stop();
        }

        if (currentAnimation != null) {
            currentFrame = currentAnimation.inPoint;
            frameSlider.setValue(currentFrame);
            renderFrame();
            updateFrameLabel();
            progressBar.setProgress(0);
        }
    }

    private void renderFrame() {
        if (currentAnimation == null) return;

        // Clear canvas
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());

        // Set background
        gc.setFill(Color.WHITE);
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

        // Scale to fit canvas while maintaining aspect ratio
        double scaleX = canvas.getWidth() / currentAnimation.width;
        double scaleY = canvas.getHeight() / currentAnimation.height;
        double scale = Math.min(scaleX, scaleY);

        double offsetX = (canvas.getWidth() - currentAnimation.width * scale) / 2;
        double offsetY = (canvas.getHeight() - currentAnimation.height * scale) / 2;

        gc.save();
        gc.translate(offsetX, offsetY);
        gc.scale(scale, scale);

        // Render layers (simplified - just show placeholders)
        for (int i = 0; i < currentAnimation.layers.size(); i++) {
            LottieLayer layer = currentAnimation.layers.get(i);

            // Check if layer is active at current frame
            if (currentFrame >= layer.inPoint && currentFrame <= layer.outPoint) {
                renderLayer(layer, i);
            }
        }

        gc.restore();

        // Draw frame border
        gc.setStroke(Color.LIGHTGRAY);
        gc.setLineWidth(2);
        gc.strokeRect(offsetX, offsetY, currentAnimation.width * scale, currentAnimation.height * scale);
    }

    private void renderLayer(LottieLayer layer, int index) {
        // Simplified layer rendering - this would be much more complex in a real implementation
        gc.setFill(Color.hsb(index * 60 % 360, 0.7, 0.9));
        gc.setStroke(Color.hsb(index * 60 % 360, 0.9, 0.7));
        gc.setLineWidth(2);

        double x = 50 + (index * 20) % (currentAnimation.width - 100);
        double y = 50 + (index * 30) % (currentAnimation.height - 100);
        double size = 50 + (index * 10) % 100;

        // Animate position based on current frame
        double progress = (currentFrame - layer.inPoint) / (layer.outPoint - layer.inPoint);
        double animatedX = x + Math.sin(progress * Math.PI * 2) * 50;
        double animatedY = y + Math.cos(progress * Math.PI * 2) * 30;

        gc.fillOval(animatedX, animatedY, size, size);
        gc.strokeOval(animatedX, animatedY, size, size);

        // Draw layer name
        gc.setFill(Color.BLACK);
        gc.fillText(layer.name != null ? layer.name : "Layer " + index,
                animatedX + 5, animatedY + size + 15);
    }

    private void clearCanvas() {
        gc.setFill(Color.LIGHTGRAY);
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

        gc.setFill(Color.DARKGRAY);
        gc.fillText("Load a Lottie animation to begin",
                canvas.getWidth() / 2 - 100, canvas.getHeight() / 2);
    }

    private void updateFrameLabel() {
        if (currentAnimation != null) {
            frameLabel.setText(String.format("Frame: %.0f / %.0f",
                    currentFrame, currentAnimation.outPoint));
        }
    }

    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Information");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showAbout() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("About");
        alert.setHeaderText("Lottie4J Animation Viewer");
        alert.setContentText("A JavaFX viewer for Lottie animations\n" +
                "Built with lottie4j data model\n" +
                "Version 1.0");
        alert.showAndWait();
    }

    // Data model classes (simplified representation of lottie4j model)
    public static class LottieAnimation {
        public String version;
        public double frameRate;
        public double inPoint;
        public double outPoint;
        public int width;
        public int height;
        public String name;
        public List<LottieLayer> layers;
    }

    public static class LottieLayer {
        public String name;
        public int type; // 0: precomp, 1: solid, 2: image, 3: null, 4: shape, 5: text
        public double startTime;
        public double inPoint;
        public double outPoint;
        public boolean visible = true;
    }
}