# Lottie4J :: Java library to handle Lottie files

[![Build](https://github.com/lottie4j/lottie4j/actions/workflows/maven.yml/badge.svg)](https://github.com/lottie4j/lottie4j/actions/workflows/maven.yml)
[![Maven Central](https://img.shields.io/maven-central/v/com.lottie4j/lottie4j.svg?label=Maven%20Central)](https://central.sonatype.com/search?q=g:com.lottie4j)
[![License](https://img.shields.io/github/license/pi4j/pi4j?label=License)](http://www.apache.org/licenses/LICENSE-2.0)
[![Site](https://img.shields.io/badge/Website-lottie4j.com-green)](https://lottie4j.com)
[![Mastodon](https://img.shields.io/badge/Mastodon-@lottie4j-6364FF?logo=mastodon&logoColor=white)](https://foojay.social/@lottie4j)

API
documentation: [![APIdia](https://apidia.net/mvn/com.lottie4j/lottie4j/badge.svg)](https://apidia.net/mvn/com.lottie4j/lottie4j)

The aim is to create Java libraries to handle and play Lottie files with Java.

Further technical information about this project can be found on [lottie4j.com](https://lottie4j.com).

Lottie4J requires **Java 21** or higher.

[![Watch the video](https://img.youtube.com/vi/9lE6UO8XNpU/maxresdefault.jpg)](https://www.youtube.com/watch?v=9lE6UO8XNpU)

## What is Lottie?

[Lottie](https://lottiefiles.com/what-is-lottie) is a JSON-based animation file format that enables designers to ship
animations on any platform as easily as shipping static assets. Created by Airbnb, Lottie has become the industry
standard for vector animations on mobile and web.

More info is available on [lottie4j.com/lottie-format](https://lottie4j.com/lottie-format/).

## Showing a Lottie Animation in a JavaFX Application

Add the dependency in your `pom.xml`:

```xml

<dependency>
    <groupId>com.lottie4j</groupId>
    <artifactId>fxplayer</artifactId>
    <version>${lottie4j.version}</version>
</dependency>
```

This is the minimal code needed to display a Lottie animation.

```java
import com.lottie4j.core.file.LottieFileLoader;
import com.lottie4j.core.model.animation.Animation;
import com.lottie4j.fxplayer.LottiePlayer;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.File;

public class DemoApplication extends Application {

    static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) throws Exception {
        File lottieFile = new File("PATH_OF_LOTTIE_FILE.json");

        Animation animation = LottieFileLoader.load(lottieFile);

        var scene = new Scene(new LottiePlayer(animation), 1200, 800);
        stage.setTitle(lottieFile.getName());
        stage.setScene(scene);
        stage.show();

        animation.play();
    }
}
```

## Structure of the sources

### Implementation Libraries

* `core`
    * Java objects for the Lottie data model.
    * Reading and writing of Lottie files.
    * Based on the info of the Lottie JSON format
      on [lottie-docs](https://lottiefiles.github.io/lottie-docs/composition/).
* `fxplayer`
    * JavaFX component to play Lottie animations.
    * Uses the `core` library.

### Demo application

* `fxfileviewer`
    * JavaFX application to show a Lottie animation.
    * Visualizes the structure of the Lottie file.
    * Uses `core` and `fxplayer` libraries.
    * This repo contains test files in `src/main/resources`, of which some were downloaded
      as [free samples from lottiefiles.com](https://lottiefiles.com/featured-free-animations).

## More Info About Lottie Files

https://lottiefiles.github.io/lottie-docs/
Meet LottieDocs, the ultimate guide to the Lottie format!
This document contains a human-readable description of the Lottie format, complete with interactive examples, a JSON
editor to help you debug issues & more.
Watch the video tutorial at https://youtu.be/uHxi9nEfUR0
