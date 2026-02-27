package com.lottie4j.fxfileviewer;

import com.lottie4j.core.handler.LottieFileLoader;
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
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
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
    private ProgressBar progressBar;
    private LottiePlayer lottiePlayer;

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Lottie4J Animation Viewer");

        // Create main layout
        root = new BorderPane();

        // Create canvas for animation rendering
        canvas = new Canvas(500, 500);
        gc = canvas.getGraphicsContext2D();
        root.setCenter(canvas);

        // Create control panel
        VBox controlPanel = createControlPanel();
        root.setBottom(controlPanel);

        // Create menu bar
        MenuBar menuBar = createMenuBar(primaryStage);
        root.setTop(menuBar);

        // Set up scene
        Scene scene = new Scene(root, 1400, 800);
        primaryStage.setScene(scene);
        primaryStage.show();

        // Initialize canvas
        clearCanvas();

        // Handle command-line arguments
        Parameters args = getParameters();
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
            progressBar.setProgress(0);
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
        HBox frameControls = new HBox(10);
        frameSlider = new Slider();
        frameSlider.setDisable(true);
        frameSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (lottiePlayer != null && !lottiePlayer.isPlaying()) {
                currentFrame = newVal.intValue();
                lottiePlayer.seekToFrame(currentFrame);
                updateFrameLabel();
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

            // Show preview image (if available)
            String imagePath = file.getAbsolutePath().replaceFirst("\\.[^.]+$", ".png");
            try {
                Image image = new Image(new File(imagePath).toURI().toString());
                ImageView imageView = new ImageView(image);
                imageView.setFitWidth(200);
                imageView.setPreserveRatio(true);
                root.setLeft(imageView);
            } catch (Exception e) {
                Label noImageLabel = new Label("no image available");
                noImageLabel.setPadding(new Insets(10));
                root.setLeft(noImageLabel);
            }

            // Show the lottie file structure
            var treeViewer = new LottieTreeView(file.getName(), animation);
            treeViewer.setPrefWidth(420);
            root.setRight(treeViewer);

            // Show new LottiePlayer
            lottiePlayer = new LottiePlayer(animation, true);
            root.setCenter(lottiePlayer);

            // Bind frame slider to lottie player's current frame
            lottiePlayer.currentFrameProperty().addListener((obs, oldVal, newVal) -> {
                if (!frameSlider.isValueChanging()) {
                    currentFrame = newVal.intValue();
                    frameSlider.setValue(currentFrame);
                    updateFrameLabel();
                    double progress = (currentFrame - getInPoint()) /
                            (getOutPoint() - getInPoint());
                    progressBar.setProgress(progress);
                }
            });

            // Reset the animation UI
            setupAnimationControls();
            currentFrame = getInPoint();

            // Auto-start animation
            startAnimation();
        } catch (IOException e) {
            logger.severe("Failed to load animation: " + e.getMessage());
            showError("Failed to load animation: " + e.getMessage());
        }
    }

    // Update play/pause/stop methods:
    private void startAnimation() {
        if (lottiePlayer != null) {
            lottiePlayer.play();
            startButton.setDisable(true);
        }
    }

    private void pauseAnimation() {
        if (lottiePlayer != null) {
            lottiePlayer.stop();
            startButton.setDisable(false);
        }
    }

    private void stopAnimation() {
        if (lottiePlayer != null) {
            lottiePlayer.stop();
            lottiePlayer.seekToFrame(getInPoint());
            currentFrame = getInPoint();
            frameSlider.setValue(currentFrame);
            updateFrameLabel();
            progressBar.setProgress(0);
            startButton.setDisable(false);
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
        Hyperlink link = new Hyperlink("https://lottie4j.com/");
        link.setOnAction(e -> {
            try {
                // Use JavaFX HostServices to open URL
                getHostServices().showDocument("https://lottie4j.com/");
            } catch (Exception ex) {
                showError("Could not open browser: " + ex.getMessage());
            }
        });

        HBox linkBox = new HBox(new Label("More info on:"), link);
        linkBox.setAlignment(Pos.BASELINE_LEFT);
        return linkBox;
    }
}