package com.lottie4j.fxfileviewer;

import com.lottie4j.core.exception.LottieFileException;
import com.lottie4j.core.file.LottieFileLoader;
import com.lottie4j.fxfileviewer.component.ViewerMenuBar;
import com.lottie4j.fxfileviewer.util.AlertHelper;
import com.lottie4j.fxplayer.LottiePlayer;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * JavaFX Lottie Simple Animation Viewer
 * This viewer can load and play Lottie animations using a basic rendering engine
 * that works with the lottie4j data model structure.
 * <p>
 * You can start this application with the file name as command line option,
 * for example, `"box-moving-changing-color.json"`.
 */
public class LottieFileSimpleViewer extends Application {
    private static final Logger logger = LoggerFactory.getLogger(LottieFileSimpleViewer.class);
    private BorderPane root;
    private LottiePlayer lottiePlayer;
    private ViewerMenuBar viewerMenuBar;
    private double currentScalePercent = 100.0;
    private boolean adaptiveOffscreenScalingEnabled = false;
    private boolean invertColorsEnabled = false;

    /**
     * Creates a new LottieFileDebugViewer.
     */
    public LottieFileSimpleViewer() {
        // Constructor for LottieFileSimpleViewer
    }

    /**
     * Initializes the JavaFX application stage and UI components.
     * Sets up the menu bar, scene, and handles command-line arguments for loading animations.
     *
     * @param primaryStage the primary stage for this application
     */
    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Lottie4J Animation Simple Viewer");

        // Create main layout
        root = new BorderPane();

        viewerMenuBar = new ViewerMenuBar(
                primaryStage,
                this::loadAnimation,
                debugVisible -> {
                    if (lottiePlayer != null) {
                        lottiePlayer.setDebugInfoVisible(debugVisible);
                    }
                },
                true,
                scalePercent -> {
                    currentScalePercent = scalePercent;
                    applyScaleToPlayer();
                },
                currentScalePercent,
                enabled -> {
                    adaptiveOffscreenScalingEnabled = enabled;
                    if (lottiePlayer != null) {
                        lottiePlayer.setAdaptiveOffscreenScalingEnabled(enabled);
                        lottiePlayer.seekToFrame(lottiePlayer.getCurrentFrame());
                    }
                },
                adaptiveOffscreenScalingEnabled,
                enabled -> {
                    invertColorsEnabled = enabled;
                    if (lottiePlayer != null) {
                        lottiePlayer.setInvertColorsEnabled(enabled);
                    }
                },
                invertColorsEnabled
        );
        root.setTop(viewerMenuBar);

        // Set up scene
        var scene = new Scene(root, 1600, 1200);
        primaryStage.setScene(scene);
        primaryStage.show();

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
     * Loads and displays a Lottie animation file.
     * Creates a new LottiePlayer instance and starts playback automatically.
     *
     * @param file the Lottie animation file to load
     */
    private void loadAnimation(File file) {
        try {
            lottiePlayer = new LottiePlayer(LottieFileLoader.load(file));
            lottiePlayer.setAdaptiveOffscreenScalingEnabled(adaptiveOffscreenScalingEnabled);
            lottiePlayer.setInvertColorsEnabled(invertColorsEnabled);
            lottiePlayer.setDebugInfoVisible(viewerMenuBar != null && viewerMenuBar.isDebugInfoSelected());
            applyScaleToPlayer();
            root.setCenter(lottiePlayer);
            lottiePlayer.play();
        } catch (LottieFileException e) {
            AlertHelper.showError("Can't load animation:\n\n" + e.getMessage());
        }
    }

    /**
     * Applies the currently selected scale percent to the active player.
     */
    private void applyScaleToPlayer() {
        if (lottiePlayer == null) {
            return;
        }
        lottiePlayer.resizeRenderPercent(currentScalePercent);
    }
}