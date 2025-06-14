package com.lottie4j.fxdemo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lottie4j.core.handler.FileLoader;
import com.lottie4j.core.model.Animation;
import com.lottie4j.fxplayer.LottiePlayer;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import java.io.File;
import java.util.logging.Logger;

public class DemoApplication extends Application {

    private static final Logger logger = Logger.getLogger(DemoApplication.class.getName());

    private static final String TEST_FILE_LOTTIE = "/test/timeline-square.json";

    static {
        System.setProperty("java.util.logging.SimpleFormatter.format", "[%1$tF %1$tT] [%4$-7s] %2$s \t\t %5$s %n");
    }

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        var r = this.getClass().getResource(TEST_FILE_LOTTIE);
        if (r == null) {
            logger.warning("The Lottie file can not be found");
            return;
        }

        var f = new File(r.getFile());
        var jsonFromFile = FileLoader.loadFileAsString(f);
        var animation = (new ObjectMapper()).readValue(jsonFromFile, Animation.class);

        logger.info("Starting with W/H " + animation.width() + "/" + animation.height());
        logger.info("Number of assets: " + (animation.assets() == null ? "0" : animation.assets().size()));
        logger.info("Number of layers: " + (animation.layers() == null ? "0" : animation.layers().size()));

        var group = new HBox();
        group.getChildren().add(new LottiePlayer(animation, true));
        group.setMinWidth(animation.width());
        group.setMinHeight(animation.height());

        var scene = new Scene(group, animation.width(), animation.height());
        primaryStage.setTitle(f.getName());
        primaryStage.setScene(scene);
        primaryStage.show();
    }
}
