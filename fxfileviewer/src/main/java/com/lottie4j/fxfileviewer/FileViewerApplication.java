package com.lottie4j.fxfileviewer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lottie4j.core.handler.FileLoader;
import com.lottie4j.core.model.Animation;
import com.lottie4j.fxplayer.player.LottiePlayer;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.File;
import java.util.logging.Logger;

public class FileViewerApplication extends Application {

    private static final Logger logger = Logger.getLogger(FileViewerApplication.class.getName());

    private static final String TEST_FILE_LOTTIE = "/test/timeline-square.json";
    private static final String TEST_FILE_IMAGE = "/test/timeline-square.png";

    //private static final String TEST_FILE_LOTTIE = "/duke/java_duke_still.json";
    //private static final String TEST_FILE_IMAGE = "/duke/java_duke.png";

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

        var holder = new HBox();
        holder.setMinWidth(animation.width() * 2);
        holder.setMinHeight(animation.height());

        // First column
        var viewers = new VBox();
        holder.getChildren().add(viewers);

        var imageUrl = FileViewerApplication.class.getResource(TEST_FILE_IMAGE);
        if (imageUrl == null) {
            logger.warning("The image file can not be found");
        } else {
            ImageView preview = new ImageView(new Image(imageUrl.toExternalForm()));
            preview.setFitHeight(animation.height());
            preview.setFitWidth(animation.width());
            viewers.getChildren().add(preview);
        }

        viewers.getChildren().add(new LottiePlayer(animation));

        // Second column
        var treeView = new LottieTreeView(TEST_FILE_LOTTIE, animation);
        treeView.setMinWidth(600);
        holder.getChildren().add(treeView);

        var scene = new Scene(holder, animation.width() + 600 + 50, animation.height() * 2);
        primaryStage.setTitle(f.getName());
        primaryStage.setScene(scene);
        primaryStage.show();
    }
}
