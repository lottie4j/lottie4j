module com.lottie4j.fxplayer {
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.core;
    requires com.lottie4j.core;
    requires javafx.graphics;
    requires javafx.controls;
    requires java.logging;

    exports com.lottie4j.fxplayer.player;
    exports com.lottie4j.fxplayer.element;
}

