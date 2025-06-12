package com.lottie4j.fxfileviewer;

import javafx.application.Application;

/**
 * Launcher class to avoid module system issues
 */
public class Launcher {
    public static void main(String[] args) {
        Application.launch(LottieFileViewer.class, args);
    }
}
