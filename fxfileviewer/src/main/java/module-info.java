/**
 * JavaFX file viewer application for Lottie animations.
 * Provides both simple and debug viewers with playback controls and layer inspection.
 */
module com.lottie4j.fxfileviewer {
    requires tools.jackson.databind;
    requires com.lottie4j.core;
    requires javafx.graphics;
    requires javafx.controls;
    requires java.logging;
    requires com.lottie4j.fxplayer;
    requires org.slf4j;
    requires java.desktop;

    // Selenium drives headless Chrome for the LottieWebView live renderer and the
    // reference screenshot generator. The selenium-* jars are explicit JPMS modules
    // (verified via `jar --describe-module` on the 4.27 artifacts).
    requires org.seleniumhq.selenium.api;
    requires org.seleniumhq.selenium.chrome_driver;
    requires org.seleniumhq.selenium.remote_driver;
    requires org.seleniumhq.selenium.support;
    // selenium-http declares `requires static dev.failsafe.core` — transitively pulled in
    // at runtime by ChromeDriver. Required here so it lands on the modulepath rather than
    // the classpath (a named module can't see classes in the unnamed module).
    requires dev.failsafe.core;

    exports com.lottie4j.fxfileviewer to javafx.scene, javafx.graphics, javafx.application;
    exports com.lottie4j.fxfileviewer.component to javafx.application, javafx.graphics, javafx.scene, tools.jackson.databind;
    exports com.lottie4j.fxfileviewer.render;

    opens com.lottie4j.fxfileviewer to org.junit.platform.commons;
}