module com.lottie4j.lottieinspector {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.logging;
    requires com.google.gson;

    opens com.lottie4j.lottieinspector to javafx.fxml;
    exports com.lottie4j.lottieinspector;
}