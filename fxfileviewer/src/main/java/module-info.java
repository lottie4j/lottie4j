/**
 * JavaFX file viewer application for Lottie animations.
 * Provides both simple and debug viewers with playback controls and layer inspection.
 */
module com.lottie4j.fxfileviewer {
    requires com.fasterxml.jackson.databind;
    requires com.lottie4j.core;
    requires javafx.graphics;
    requires javafx.controls;
    requires javafx.web;
    requires java.logging;
    requires com.lottie4j.fxplayer;
    requires org.slf4j;
    requires java.desktop;

    exports com.lottie4j.fxfileviewer to javafx.scene, javafx.graphics, javafx.application;
    exports com.lottie4j.fxfileviewer.component to javafx.application, javafx.graphics, javafx.scene;

    opens com.lottie4j.fxfileviewer to org.junit.platform.commons;
}