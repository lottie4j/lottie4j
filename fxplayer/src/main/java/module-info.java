module com.lottie4j.fxplayer {
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.core;
    requires com.lottie4j.core;
    requires javafx.graphics;
    requires javafx.controls;
    requires java.logging;
    requires java.desktop;

    exports com.lottie4j.fxplayer;
    exports com.lottie4j.fxplayer.renderer.shape;
    exports com.lottie4j.fxplayer.renderer.grouping;
    exports com.lottie4j.fxplayer.renderer.style;
}

