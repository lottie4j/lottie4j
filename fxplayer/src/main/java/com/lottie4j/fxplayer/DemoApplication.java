package com.lottie4j.fxplayer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lottie4j.core.handler.FileLoader;
import com.lottie4j.core.model.Animation;
import com.lottie4j.fxplayer.player.LottiePlayer;
import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.File;

public class DemoApplication extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        File f = new File(this.getClass().getResource("/lottie/java_duke.json").getFile());
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

        Scene scene = new Scene(group, animation.width(), animation.height());
        primaryStage.setTitle(f.getName());
        primaryStage.setScene(scene);
        primaryStage.show();
    }
}
