package com.lottie4j.fxfileviewer;

import com.lottie4j.core.exception.LottieFileException;
import com.lottie4j.core.file.LottieFileLoader;
import com.lottie4j.fxplayer.LottiePlayer;
import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.File;
import java.util.Map;

/**
 * JavaFX Lottie Animation Viewer that plays an example Lottie file with markers in a loop.
 * <p>
 * Crop insets can be supplied via named CLI arguments and are passed to
 * {@link LottiePlayer#crop(int, int, int, int)}.
 * <p>
 * Supported argument names:
 * <ul>
 *   <li>{@code --top}, {@code --right}, {@code --bottom}, {@code --left}</li>
 *   <li>Legacy aliases: {@code --cropTop}, {@code --cropRight}, {@code --cropBottom}, {@code --cropLeft}</li>
 * </ul>
 * Example:
 * <pre>{@code
 * --top=60 --right=20 --bottom=0 --left=20
 * }</pre>
 */
public class LottieFileMarkerViewer extends Application {
    /**
     * Creates a new LottieFileDebugViewer.
     */
    public LottieFileMarkerViewer() {
        // Constructor for LottieFileSimpleViewer
    }

    /**
     * Initializes the JavaFX application stage and UI components.
     * Loads a sample animation, applies optional crop arguments, then starts playback with marker looping.
     * <p>
     * Crop args are optional and default to {@code 0} when omitted.
     *
     * @param primaryStage the primary stage for this application
     */
    @Override
    public void start(Stage primaryStage) throws LottieFileException {
        primaryStage.setTitle("Lottie4J Animation Viewer with Marker Loop");

        Map<String, String> args = getParameters().getNamed();
        int cropTop = parseCropInsetArg(args, "top", "cropTop");
        int cropRight = parseCropInsetArg(args, "right", "cropRight");
        int cropBottom = parseCropInsetArg(args, "bottom", "cropBottom");
        int cropLeft = parseCropInsetArg(args, "left", "cropLeft");

        // Load animation
        var animation = LottieFileLoader.load(new File(getClass().getResource("/melodymatrix.json").getFile()));
        var player = new LottiePlayer(animation);
        player.setBackgroundColor(Color.LIGHTBLUE);

        if (cropTop > 0 || cropRight > 0 || cropBottom > 0 || cropLeft > 0) {
            player.crop(cropTop, cropRight, cropBottom, cropLeft);
        }

        // Set up scene
        var scene = new Scene(new Pane(player), player.getWidth(), player.getHeight());
        primaryStage.setScene(scene);
        primaryStage.setOnShown(e -> {
            PauseTransition delay = new PauseTransition(Duration.seconds(1));
            delay.setOnFinished(event -> player.play(animation.markers().getFirst(), animation.markers().getLast()));
            delay.play();
        });
        primaryStage.show();
    }

    /**
     * Parses one crop inset from named arguments.
     * Accepts both modern key names (for example {@code top}) and legacy names
     * (for example {@code cropTop}) for backwards compatibility.
     *
     * @param args      named command line args
     * @param key       modern key name
     * @param legacyKey legacy key name
     * @return non-negative inset value in pixels
     */
    private int parseCropInsetArg(Map<String, String> args, String key, String legacyKey) {
        String rawValue = args.getOrDefault(key, args.get(legacyKey));
        if (rawValue == null || rawValue.isBlank()) {
            return 0;
        }

        try {
            return Math.max(0, Integer.parseInt(rawValue));
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Invalid --" + key + " value '" + rawValue + "'. Expected a non-negative integer.", ex);
        }
    }
}