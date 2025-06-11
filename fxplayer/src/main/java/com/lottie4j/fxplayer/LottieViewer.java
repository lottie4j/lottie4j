package com.lottie4j.fxplayer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lottie4j.core.handler.FileLoader;
import com.lottie4j.core.model.Animation;
import com.lottie4j.fxplayer.player.LottiePlayer;
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
    private Animation animation;
    private int currentFrame = 0;
    private boolean isPlaying = false;
    private BorderPane root;

    // UI Controls
    private Button playButton;
    private Button pauseButton;
    private Slider frameSlider;
    private Label frameLabel;
    private Label fpsLabel;
    private ProgressBar progressBar;
    private LottiePlayer lottiePlayer;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Lottie4J Animation Viewer");

        // Create main layout
        root = new BorderPane();

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

    @Override
    public void stop() {
        if (lottiePlayer != null) {
            lottiePlayer.stop();
            isPlaying = false;
            playButton.setDisable(false);
            currentFrame = animation.inPoint();
            frameSlider.setValue(currentFrame);
            updateFrameLabel();
            progressBar.setProgress(0);
        }
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

        playButton.setOnAction(e -> play());
        pauseButton.setOnAction(e -> pause());

        // Initially disable controls
        playButton.setDisable(true);
        pauseButton.setDisable(true);

        playbackControls.getChildren().addAll(playButton, pauseButton);

        // Frame controls
        HBox frameControls = new HBox(10);
        frameSlider = new Slider();
        frameSlider.setDisable(true);
        frameSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (!isPlaying) {
                currentFrame = newVal.intValue();
                //renderFrame();
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

    private void setupAnimationControls() {
        if (animation == null) return;

        // Enable controls
        playButton.setDisable(false);
        pauseButton.setDisable(false);
        frameSlider.setDisable(false);

        // Setup frame slider
        frameSlider.setMin(animation.inPoint());
        frameSlider.setMax(animation.outPoint());
        frameSlider.setValue(animation.inPoint());

        // Update labels
        fpsLabel.setText("FPS: " + String.format("%d", animation.framesPerSecond()));
        updateFrameLabel();
    }

    private void loadAnimation(File file) {
        try {
            var jsonFromFile = FileLoader.loadFileAsString(file);
            animation = (new ObjectMapper()).readValue(jsonFromFile, Animation.class);

            // Remove existing LottiePlayer if any
            if (lottiePlayer != null) {
                root.getChildren().remove(lottiePlayer);
            }

            // Create new LottiePlayer
            lottiePlayer = new LottiePlayer(animation);

            // Replace canvas with LottiePlayer - use the stored root reference
            root.setCenter(lottiePlayer);

            // Update UI
            setupAnimationControls();
            currentFrame = animation.inPoint();

            showInfo("Animation loaded successfully!\n" +
                    "Name: " + animation.name() + "\n" +
                    "Duration: " + (animation.outPoint() - animation.inPoint()) / animation.framesPerSecond() + "s\n" +
                    "Size: " + animation.width() + "x" + animation.height() + "\n" +
                    "Layers: " + animation.layers().size());
        } catch (IOException e) {
            showError("Failed to load animation: " + e.getMessage());
        }
    }

    // Update play/pause/stop methods:
    private void play() {
        if (lottiePlayer != null) {
            lottiePlayer.play();
            isPlaying = true;
            playButton.setDisable(true);
        }
    }

    private void pause() {
        if (lottiePlayer != null) {
            lottiePlayer.pause();
            isPlaying = false;
            playButton.setDisable(false);
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