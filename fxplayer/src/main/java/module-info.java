module com.lottie4j.fxplayer {
    requires com.fasterxml.jackson.databind;
    requires com.lottie4j.core;
    requires javafx.graphics;
    requires javafx.controls;
    requires java.logging;

    exports com.lottie4j.fxplayer to javafx.graphics, javafx.application;
}