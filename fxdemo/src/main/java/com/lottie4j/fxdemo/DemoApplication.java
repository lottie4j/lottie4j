package com.lottie4j.fxdemo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lottie4j.core.handler.FileLoader;
import com.lottie4j.core.model.Animation;
import com.lottie4j.fxplayer.element.ShapeDrawer;
import com.lottie4j.fxplayer.player.LottiePlayer;
import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DemoApplication extends Application {

    private static final Logger logger = Logger.getLogger(ShapeDrawer.class.getName());
    private static final String TEST_FILE_LOTTIE = "/test/timeline.json"; // "/duke/java_duke_still.json";
    private static final String TEST_FILE_IMAGE = "/test/timeline_start.png"; // "/duke/java_duke.png";

    static {
        System.setProperty("java.util.logging.SimpleFormatter.format", "[%1$tF %1$tT] [%4$-7s] %2$s \t\t %5$s %n");
    }

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        var f = new File(this.getClass().getResource(TEST_FILE_LOTTIE).getFile());
        var jsonFromFile = FileLoader.loadFileAsString(f);
        var animation = (new ObjectMapper()).readValue(jsonFromFile, Animation.class);

        logger.log(Level.INFO, "Starting with W/H " + animation.width() + "/" + animation.height());
        logger.log(Level.INFO, "Number of layers: " + animation.layers().size());
        for (var i = 0; i < animation.layers().size(); i++) {
            var layer = animation.layers().get(i);
            logger.log(Level.INFO, "Layer " + (i + 1) + ", shapes: " + (layer.shapes() == null ? "empty" : layer.shapes().size()));
        }

        var player = new LottiePlayer(animation);

        var holder = new HBox();
        holder.setMinWidth(animation.width() * 2);
        holder.setMinHeight(animation.height());

        var imageUrl = DemoApplication.class.getResource(TEST_FILE_IMAGE);
        ImageView preview = new ImageView(new Image(imageUrl.toExternalForm()));
        preview.setFitHeight(animation.height());
        preview.setFitWidth(animation.width());
        holder.getChildren().add(preview);

        var group = new Group();
        group.getChildren().add(player);
        group.getChildren().add(new TextField(TEST_FILE_LOTTIE));
        holder.getChildren().add(group);

        var scene = new Scene(holder, animation.width() * 2, animation.height());
        primaryStage.setTitle(f.getName());
        primaryStage.setScene(scene);
        primaryStage.show();
    }
}
