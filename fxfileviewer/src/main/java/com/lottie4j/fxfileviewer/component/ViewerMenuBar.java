package com.lottie4j.fxfileviewer.component;

import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.File;
import java.net.URI;
import java.util.function.Consumer;

import static com.lottie4j.fxfileviewer.util.AlertHelper.showError;

public class ViewerMenuBar extends MenuBar {
    private static final Logger logger = LoggerFactory.getLogger(ViewerMenuBar.class);

    private final Consumer<File> onFileSelected;

    public ViewerMenuBar(Stage stage, Consumer<File> onFileSelected) {
        this.onFileSelected = onFileSelected;

        var fileMenu = new Menu("File");
        MenuItem openItem = new MenuItem("Open Lottie File...");
        openItem.setOnAction(e -> openFile(stage));
        fileMenu.getItems().add(openItem);

        var helpMenu = new Menu("Help");
        MenuItem aboutItem = new MenuItem("About");
        aboutItem.setOnAction(e -> showAbout());
        helpMenu.getItems().add(aboutItem);

        getMenus().addAll(fileMenu, helpMenu);
    }

    private void openFile(Stage stage) {
        var fileChooser = new FileChooser();
        fileChooser.setTitle("Open Lottie Animation");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Lottie Files", "*.json", "*.lottie")
        );

        var file = fileChooser.showOpenDialog(stage);
        if (file != null && onFileSelected != null) {
            logger.info("Opening file: {}", file);
            onFileSelected.accept(file);
        }
    }

    private void showAbout() {
        var alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("About");
        alert.setHeaderText("Lottie4J File Viewer");
        VBox content = new VBox(5);
        content.getChildren().addAll(
                new Label("A JavaFX viewer for Lottie animations."),
                new Label("Built with the Lottie4J library."),
                createClickableLink()
        );

        alert.getDialogPane().setContent(content);
        alert.showAndWait();
    }

    private HBox createClickableLink() {
        var link = new Hyperlink("https://lottie4j.com/");
        link.setOnAction(e -> {
            try {
                // Use Desktop API to open URL in default browser
                if (Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().browse(new URI("https://lottie4j.com/"));
                }
            } catch (Exception ex) {
                showError("Could not open browser: " + ex.getMessage());
            }
        });

        var linkBox = new HBox(new Label("More info on:"), link);
        linkBox.setAlignment(Pos.BASELINE_LEFT);
        return linkBox;
    }
}
