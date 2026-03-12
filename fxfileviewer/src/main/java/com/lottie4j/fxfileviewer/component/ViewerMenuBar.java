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

/**
 * Menu bar component for the Lottie file viewer application.
 * Provides File and Help menus with options to open Lottie files and view application information.
 */
public class ViewerMenuBar extends MenuBar {
    private static final Logger logger = LoggerFactory.getLogger(ViewerMenuBar.class);

    private final Consumer<File> onFileSelected;
    private final CheckMenuItem debugInfoMenuItem;

    /**
     * Creates a menu bar for the viewer application.
     *
     * @param stage          the primary stage for displaying file chooser dialogs
     * @param onFileSelected callback invoked when a Lottie file is selected
     */
    public ViewerMenuBar(Stage stage, Consumer<File> onFileSelected) {
        this(stage, onFileSelected, null, false);
    }

    /**
     * Creates a menu bar with an optional debug info toggle in the View menu.
     *
     * @param stage              the primary stage for displaying file chooser dialogs
     * @param onFileSelected     callback invoked when a Lottie file is selected
     * @param onDebugInfoChanged callback invoked when debug info visibility changes; null disables the toggle menu
     * @param debugInfoSelected  initial debug toggle state
     */
    public ViewerMenuBar(Stage stage,
                         Consumer<File> onFileSelected,
                         Consumer<Boolean> onDebugInfoChanged,
                         boolean debugInfoSelected) {
        this.onFileSelected = onFileSelected;

        var fileMenu = new Menu("File");
        MenuItem openItem = new MenuItem("Open Lottie File...");
        openItem.setOnAction(e -> openFile(stage));
        fileMenu.getItems().add(openItem);

        var viewMenu = new Menu("View");
        debugInfoMenuItem = new CheckMenuItem("Debug Info");
        debugInfoMenuItem.setSelected(debugInfoSelected);
        if (onDebugInfoChanged != null) {
            debugInfoMenuItem.setOnAction(e -> onDebugInfoChanged.accept(debugInfoMenuItem.isSelected()));
            viewMenu.getItems().add(debugInfoMenuItem);
        } else {
            debugInfoMenuItem.setDisable(true);
        }

        var helpMenu = new Menu("Help");
        MenuItem aboutItem = new MenuItem("About");
        aboutItem.setOnAction(e -> showAbout());
        helpMenu.getItems().add(aboutItem);

        if (viewMenu.getItems().isEmpty()) {
            getMenus().addAll(fileMenu, helpMenu);
        } else {
            getMenus().addAll(fileMenu, viewMenu, helpMenu);
        }
    }

    public boolean isDebugInfoSelected() {
        return debugInfoMenuItem != null && debugInfoMenuItem.isSelected();
    }

    /**
     * Opens a file chooser dialog for selecting Lottie animation files.
     *
     * @param stage the stage to use as parent for the file chooser dialog
     */
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

    /**
     * Displays an information dialog with application details and links.
     */
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

    /**
     * Creates a clickable hyperlink to the Lottie4J website.
     *
     * @return HBox containing the link with label
     */
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
