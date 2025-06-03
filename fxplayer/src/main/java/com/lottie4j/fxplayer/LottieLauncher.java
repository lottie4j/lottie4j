package com.lottie4j.fxplayer;

import javafx.application.Application;

/**
 * Launcher class to avoid module system issues
 */
public class LottieLauncher {
    public static void main(String[] args) {
        Application.launch(LottieViewer.class, args);
    }
}
