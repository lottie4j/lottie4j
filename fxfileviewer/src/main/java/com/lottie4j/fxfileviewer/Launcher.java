package com.lottie4j.fxfileviewer;

import javafx.application.Application;

/**
 * Launcher class to avoid module system issues.
 * This class serves as the entry point for the JavaFX application without directly
 * exposing the Application subclass to the module system.
 */
public class Launcher {
    /**
     * Creates a new Launcher.
     */
    public Launcher() {
        // Constructor for Launcher
    }

    /**
     * Application entry point that delegates to the JavaFX Application launcher.
     *
     * @param args command-line arguments passed to the application
     */
    public static void main(String[] args) {
        // --bench=SECONDS starts the simple viewer in playback benchmark mode
        // (-Dlottie.viewer=simple starts the simple viewer without it)
        boolean bench = java.util.Arrays.stream(args).anyMatch(arg -> arg.startsWith("--bench"));
        if (bench || "simple".equals(System.getProperty("lottie.viewer"))) {
            Application.launch(LottieFileSimpleViewer.class, args);
            return;
        }
        Application.launch(LottieFileDebugViewer.class, args);
    }
}
