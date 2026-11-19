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
    // (verified via `jar --describe-module` on the 4.51 artifacts).
    requires org.seleniumhq.selenium.api;
    requires org.seleniumhq.selenium.chrome_driver;
    requires org.seleniumhq.selenium.remote_driver;
    requires org.seleniumhq.selenium.support;
    // Selenium's HTTP client uses Guava (com.google.common.net.MediaType) but only declares it
    // "requires static", so on the module path nothing brings Guava into the module graph and
    // starting Chrome fails with NoClassDefFoundError. Requiring it here does.
    requires com.google.common;

    exports com.lottie4j.fxfileviewer to javafx.scene, javafx.graphics, javafx.application;
    exports com.lottie4j.fxfileviewer.component to javafx.application, javafx.graphics, javafx.scene, tools.jackson.databind;
    exports com.lottie4j.fxfileviewer.render;

    opens com.lottie4j.fxfileviewer to org.junit.platform.commons;
}