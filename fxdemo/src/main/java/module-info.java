module com.lottie4j.fxdemo {
    requires com.fasterxml.jackson.databind;
    requires com.lottie4j.core;
    requires com.lottie4j.fxplayer;
    requires javafx.graphics;
    requires javafx.controls;
    requires java.logging;

    exports com.lottie4j.fxdemo to javafx.scene, javafx.graphics, javafx.application;
}