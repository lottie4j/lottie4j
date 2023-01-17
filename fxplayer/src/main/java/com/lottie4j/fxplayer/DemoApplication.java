package com.lottie4j.fxplayer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lottie4j.core.handler.FileLoader;
import com.lottie4j.core.model.Animation;
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

public class DemoApplication extends Application {

    private static final String TEST_FILE_LOTTIE = "/lottie/java_duke_still.json";
    private static final String TEST_FILE_IMAGE = "/lottie/java_duke.png";

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        File f = new File(this.getClass().getResource(TEST_FILE_LOTTIE).getFile());
        String jsonFromFile = FileLoader.loadFileAsString(f);
        ObjectMapper mapper = new ObjectMapper();
        Animation animation = mapper.readValue(jsonFromFile, Animation.class);

        System.out.println("Starting with W/H " + animation.width() + "/" + animation.height());
        System.out.println("Number of layers: " + animation.layers().size());
        for (var i = 0; i < animation.layers().size(); i++) {
            System.out.println("Layer " + (i + 1) + ", shapes: " + animation.layers().get(i).shapes().size());
        }
        LottiePlayer player = new LottiePlayer(animation);

        Group group = new Group();
        group.getChildren().add(player);
        group.getChildren().add(new TextField(TEST_FILE_LOTTIE));

        ImageView preview = new ImageView(new Image(TEST_FILE_IMAGE));
        preview.setFitHeight(animation.height());
        preview.setFitWidth(animation.width());

        HBox holder = new HBox();
        holder.setMinWidth(animation.width() * 2);
        holder.setMinHeight(animation.height());
        holder.getChildren().add(group);
        holder.getChildren().add(preview);

        Scene scene = new Scene(holder, animation.width() * 2, animation.height());
        primaryStage.setTitle(f.getName());
        primaryStage.setScene(scene);
        primaryStage.show();
    }
}
