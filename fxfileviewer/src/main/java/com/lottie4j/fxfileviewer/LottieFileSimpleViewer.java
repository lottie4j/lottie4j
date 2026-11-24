package com.lottie4j.fxfileviewer;

import com.lottie4j.core.exception.LottieFileException;
import com.lottie4j.core.file.LottieFileLoader;
import com.lottie4j.fxfileviewer.component.ViewerMenuBar;
import com.lottie4j.fxfileviewer.util.AlertHelper;
import com.lottie4j.fxplayer.LottiePlayer;
import com.lottie4j.fxplayer.util.RenderProfiler;
import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import javafx.util.Duration;
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

            var bench = getParameters().getNamed().get("bench");
            if (bench != null && lottiePlayer != null) {
                runBenchmark(file, Double.parseDouble(bench));
            }
        }
    }

    /**
     * Measures real playback for a number of seconds, prints a summary and exits.
     * Start with {@code --bench=15} (seconds) next to the file name. The first second is
     * warm-up and not counted. Unlike the headless benchmark this includes the real
     * rendering pipeline and the display refresh limit of this machine.
     */
    private void runBenchmark(File file, double seconds) {
        lottiePlayer.setDebugInfoVisible(true);
        var warmUp = new PauseTransition(Duration.seconds(1));
        warmUp.setOnFinished(e -> {
            lottiePlayer.getRenderStats().reset();
            RenderProfiler.setEnabled(true);
            var measure = new PauseTransition(Duration.seconds(seconds));
            measure.setOnFinished(done -> {
                var stats = lottiePlayer.getRenderStats();
                System.out.printf("BENCH file=%s canvas=%.0fx%.0f adaptive=%s frames=%d (stats over the last %d)%n"
                                + "BENCH render avg=%.2f p50=%.2f p95=%.2f p99=%.2f max=%.2f ms, achievable=%.1f fps, delivered~%.1f fps (display capped)%n",
                        file.getName(), lottiePlayer.getWidth(), lottiePlayer.getHeight(),
                        adaptiveOffscreenScalingEnabled, stats.getTotalRecorded(), stats.getFrameCount(),
                        stats.getAverageMillis(), stats.getPercentileMillis(50), stats.getPercentileMillis(95),
                        stats.getPercentileMillis(99), stats.getMaxMillis(), stats.getAchievableFps(),
                        stats.getTotalRecorded() / seconds);
                RenderProfiler.snapshot().forEach((stage, t) -> System.out.printf(
                        "BENCH stage %-18s %8.2f ms/frame-share total=%.0f ms calls=%d avg=%.4f ms%n",
                        stage, t.totalMillis() / Math.max(1, stats.getTotalRecorded()), t.totalMillis(), t.calls(), t.averageMillis()));
                RenderProfiler.setEnabled(false);
                Platform.exit();
            });
            measure.play();
        });
        warmUp.play();
    }

    /**
     * Loads and displays a Lottie animation file.
     * Creates a new LottiePlayer instance and starts playback automatically.
     * The previous player is stopped first, as a running player stays referenced by its animation timer.
     *
     * @param file the Lottie animation file to load
     */
    private void loadAnimation(File file) {
        try {
            var animation = LottieFileLoader.load(file);
            if (lottiePlayer != null) {
                lottiePlayer.stop();
            }
            lottiePlayer = new LottiePlayer(animation);
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
     * Called when the application is being stopped.
     * Stops the playing animation.
     */
    @Override
    public void stop() {
        if (lottiePlayer != null) {
            lottiePlayer.stop();
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