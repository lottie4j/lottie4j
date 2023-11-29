module com.lottie4j.fxdemo {
    requires com.fasterxml.jackson.databind;
    requires com.lottie4j.core;
    requires javafx.graphics;
    requires javafx.controls;
    requires java.logging;
    requires com.lottie4j.fxplayer;

    exports com.lottie4j.fxfileviewer to javafx.scene, javafx.graphics, javafx.application;
}