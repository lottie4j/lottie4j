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
        Application.launch(LottieFileDebugViewer.class, args);
    }
}
