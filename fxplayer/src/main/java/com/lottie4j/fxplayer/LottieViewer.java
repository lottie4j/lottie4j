package com.lottie4j.fxplayer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lottie4j.core.handler.FileLoader;
import com.lottie4j.core.model.Animation;
import com.lottie4j.core.model.Layer;
import com.lottie4j.core.model.shape.*;
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

/**
 * JavaFX Lottie Animation Viewer
 * This viewer can load and play Lottie animations using a basic rendering engine
 * that works with the lottie4j data model structure.
 */
public class LottieViewer extends Application {

    private Canvas canvas;
    private GraphicsContext gc;
    private Timeline timeline;
    private Animation animation;
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
            var jsonFromFile = FileLoader.loadFileAsString(file);
            animation = (new ObjectMapper()).readValue(jsonFromFile, Animation.class);

            // Update UI
            setupAnimationControls();
            currentFrame = animation.inPoint();
            renderFrame();

            showInfo("Animation loaded successfully!\n" +
                    "Name: " + animation.name() + "\n" +
                    "Duration: " + (animation.outPoint() - animation.inPoint()) / animation.framesPerSecond() + "s\n" +
                    "Size: " + animation.width() + "x" + animation.height() + "\n" +
                    "Layers: " + animation.layers().size());
        } catch (IOException e) {
            showError("Failed to load animation: " + e.getMessage());
        }
    }

    private void setupAnimationControls() {
        if (animation == null) return;

        // Enable controls
        playButton.setDisable(false);
        pauseButton.setDisable(false);
        stopButton.setDisable(false);
        frameSlider.setDisable(false);

        // Setup frame slider
        frameSlider.setMin(animation.inPoint());
        frameSlider.setMax(animation.outPoint());
        frameSlider.setValue(animation.inPoint());

        // Update labels
        fpsLabel.setText("FPS: " + String.format("%d", animation.framesPerSecond()));
        updateFrameLabel();
    }

    private void play() {
        if (animation == null) return;

        isPlaying = true;
        playButton.setDisable(true);

        if (timeline != null) {
            timeline.stop();
        }

        // Create animation timeline
        double frameDuration = 1000.0 / animation.framesPerSecond(); // milliseconds per frame
        timeline = new Timeline(new KeyFrame(Duration.millis(frameDuration), e -> {
            currentFrame++;
            if (currentFrame > animation.outPoint()) {
                currentFrame = animation.inPoint(); // Loop
            }
            renderFrame();
            updateFrameLabel();
            frameSlider.setValue(currentFrame);

            double progress = (currentFrame - animation.inPoint()) /
                    (animation.outPoint() - animation.inPoint());
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

        if (animation != null) {
            currentFrame = animation.inPoint();
            frameSlider.setValue(currentFrame);
            renderFrame();
            updateFrameLabel();
            progressBar.setProgress(0);
        }
    }

    private void renderFrame() {
        if (animation == null) {
            return;
        }

        // Clear canvas
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());

        // Set background
        gc.setFill(Color.WHITE);
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

        // Scale to fit canvas while maintaining aspect ratio
        double scaleX = canvas.getWidth() / animation.width();
        double scaleY = canvas.getHeight() / animation.height();
        double scale = Math.min(scaleX, scaleY);

        double offsetX = (canvas.getWidth() - animation.width() * scale) / 2;
        double offsetY = (canvas.getHeight() - animation.height() * scale) / 2;

        gc.save();
        gc.translate(offsetX, offsetY);
        gc.scale(scale, scale);

        // Render layers (simplified - just show placeholders)
        for (int i = 0; i < animation.layers().size(); i++) {
            Layer layer = animation.layers().get(i);

            // Check if layer is active at current frame
            if (currentFrame >= layer.inPoint() && currentFrame <= layer.outPoint()) {
                renderLayer(layer, i);
            }
        }

        gc.restore();

        // Draw frame border
        gc.setStroke(Color.LIGHTGRAY);
        gc.setLineWidth(2);
        gc.strokeRect(offsetX, offsetY, animation.width() * scale, animation.height() * scale);
    }

    private void renderLayer(Layer layer, int index) {
        if (layer.shapes() == null || layer.shapes().isEmpty()) {
            return;
        }

        gc.save();

        // Apply layer transform
        if (layer.transform() != null) {
            // Get transform properties
            double opacity = layer.transform().opacity() != null ?
                    layer.transform().opacity().getValue(currentFrame) / 100.0 : 1.0;

            // Calculate position
            double x = layer.transform().position() != null ?
                    layer.transform().position().getXValue(currentFrame) : 0;
            double y = layer.transform().position() != null ?
                    layer.transform().position().getYValue(currentFrame) : 0;

            // Calculate scale
            double scaleX = layer.transform().scale() != null ?
                    layer.transform().scale().getXValue(currentFrame) / 100.0 : 1.0;
            double scaleY = layer.transform().scale() != null ?
                    layer.transform().scale().getYValue(currentFrame) / 100.0 : 1.0;

            // Calculate rotation (in radians)
            double rotation = layer.transform().rotation() != null ?
                    Math.toRadians(layer.transform().rotation().getValue(currentFrame)) : 0;

            // Apply transforms in correct order
            gc.translate(x, y);
            gc.rotate(rotation);
            gc.scale(scaleX, scaleY);
            gc.setGlobalAlpha(opacity);
        }

        // Render all shapes in the layer
        for (BaseShape shape : layer.shapes()) {
            renderShape(shape);
        }

        gc.restore();
    }

    private void renderShape(BaseShape shape) {
        if (shape == null) {
            return;
        }

        if (shape instanceof Rectangle rectangle) {
            renderRectangle(rectangle);
        } else if (shape instanceof Ellipse ellipse) {
            renderEllipse(ellipse);
        } else if (shape instanceof Path path) {
            renderPath(path);
        } else if (shape instanceof Fill fill) {
            applyFill(fill);
        } else if (shape instanceof Stroke stroke) {
            applyStroke(stroke);
        }

    }

    private void renderRectangle(Rectangle shape) {
        double x = shape.rectangle().position().getXValue(currentFrame);
        double y = shape.rectangle().position().getYValue(currentFrame);
        double width = shape.rectangle().size().getXValue(currentFrame);
        double height = shape.rectangle().size().getYValue(currentFrame);
        double roundness = shape.rectangle().roundness() != null ?
                shape.rectangle().roundness().getValue(currentFrame) : 0;

        if (roundness > 0) {
            gc.fillRoundRect(x - width / 2, y - height / 2, width, height, roundness, roundness);
            gc.strokeRoundRect(x - width / 2, y - height / 2, width, height, roundness, roundness);
        } else {
            gc.fillRect(x - width / 2, y - height / 2, width, height);
            gc.strokeRect(x - width / 2, y - height / 2, width, height);
        }
    }

    private void renderEllipse(Ellipse shape) {
        double x = shape.ellipse().position().getXValue(currentFrame);
        double y = shape.ellipse().position().getYValue(currentFrame);
        double width = shape.ellipse().size().getXValue(currentFrame);
        double height = shape.ellipse().size().getYValue(currentFrame);

        gc.fillOval(x - width / 2, y - height / 2, width, height);
        gc.strokeOval(x - width / 2, y - height / 2, width, height);
    }

    private void renderPath(Path shape) {
        if (shape.path() == null || shape.path().vertices() == null) return;

        gc.beginPath();
        boolean first = true;

        for (Vertex vertex : shape.path().vertices()) {
            double x = vertex.x().getValue(currentFrame);
            double y = vertex.y().getValue(currentFrame);

            if (first) {
                gc.moveTo(x, y);
                first = false;
            } else {
                if (vertex.bezierIn() != null && vertex.bezierOut() != null) {
                    // Handle bezier curves
                    double cp1x = vertex.bezierIn().x().getValue(currentFrame);
                    double cp1y = vertex.bezierIn().y().getValue(currentFrame);
                    double cp2x = vertex.bezierOut().x().getValue(currentFrame);
                    double cp2y = vertex.bezierOut().y().getValue(currentFrame);
                    gc.bezierCurveTo(cp1x, cp1y, cp2x, cp2y, x, y);
                } else {
                    gc.lineTo(x, y);
                }
            }
        }

        if (shape.path().closed()) {
            gc.closePath();
        }

        gc.fill();
        gc.stroke();
    }

    private void applyFill(Fill shape) {
        Color fillColor = getFillColor(shape.fill(), currentFrame);
        gc.setFill(fillColor);
    }

    private void applyStroke(Stroke shape) {
        Color strokeColor = getStrokeColor(shape.stroke(), currentFrame);
        double width = shape.stroke().width().getValue(currentFrame);

        gc.setStroke(strokeColor);
        gc.setLineWidth(width);
    }

    private Color getFillColor(Fill fill, double frame) {
        // Convert color values from 0-1 range
        double r = fill.color().red().getValue(frame) / 255.0;
        double g = fill.color().green().getValue(frame) / 255.0;
        double b = fill.color().blue().getValue(frame) / 255.0;
        double a = fill.opacity() != null ?
                fill.opacity().getValue(frame) / 100.0 : 1.0;

        return new Color(r, g, b, a);
    }

    private Color getStrokeColor(Stroke stroke, double frame) {
        double r = stroke.color().red().getValue(frame) / 255.0;
        double g = stroke.color().green().getValue(frame) / 255.0;
        double b = stroke.color().blue().getValue(frame) / 255.0;
        double a = stroke.opacity() != null ?
                stroke.opacity().getValue(frame) / 100.0 : 1.0;

        return new Color(r, g, b, a);
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
            frameLabel.setText(String.format("Frame: %.0f / %d",
                    currentFrame, animation.outPoint()));
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
        alert.setContentText("""
                A JavaFX viewer for Lottie animations.
                Built with Lottie4J data model.
                Version 1.0
                """);
        alert.showAndWait();
    }
}