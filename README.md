# Lottie4J :: Java library to handle Lottie files

The aim is to create Java libraries to handle and play Lottie files with Java.

Further technical information about this project can be found on [lottie4j.com](https://lottie4j.com).

## Showing a Lottie Animation in a JavaFX Application

This is the minimal code needed to display a Lottie animation:

```java
import com.lottie4j.core.handler.LottieFileLoader;
import com.lottie4j.core.model.Animation;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import java.io.File;

public class DemoApplication extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) throws Exception {
        Animation animation = LottieFileLoader.load(new File("PATH_OF_LOTTIE_FILE.json"));

        logger.info("Starting with W/H " + animation.width() + "/" + animation.height());
        logger.info("Number of assets: " + (animation.assets() == null ? "0" : animation.assets().size()));
        logger.info("Number of layers: " + (animation.layers() == null ? "0" : animation.layers().size()));

        var group = new HBox();
        group.getChildren().add(new LottiePlayer(animation));
        group.setMinWidth(animation.width());
        group.setMinHeight(animation.height());

        var scene = new Scene(group, animation.width(), animation.height());
        stage.setTitle(f.getName());
        stage.setScene(scene);
        stage.show();
    }
}
```

## Structure of the sources

### Implementation Libraries

* `core`
    * Java objects for the Lottie data model.
    * Reading and writing of Lottie files.
* `fxplayer`
    * JavaFX component to play Lottie animations.
    * Uses the `core` library.
    * Contains a simple test application `DemoApplication.java`.

### Demo application

* `fxfileviewer`
    * JavaFX application to show the Lottie animation
    * Visualizes the structure of the Lottie file.
    * Uses `core` and `fxplayer` libraries.

## More Info About Lottie Files

https://lottiefiles.github.io/lottie-docs/
Meet LottieDocs, the ultimate guide to the Lottie format!
This document contains a human-readable description of the Lottie format, complete with interactive examples, a JSON
editor to help you debug issues & more.
Watch the video tutorial at https://youtu.be/uHxi9nEfUR0