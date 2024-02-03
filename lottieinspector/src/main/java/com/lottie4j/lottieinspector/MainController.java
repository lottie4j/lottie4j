package com.lottie4j.lottieinspector;

import com.lottie4j.lottieinspector.services.Services;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.AnchorPane;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.logging.Logger;

public class MainController implements Initializable {

    @FXML
    private AnchorPane root;

    @FXML
    private TreeView<String> treeView;

    private Services services;
    private Logger logger = Logger.getLogger("info");

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        Platform.runLater(() -> {
            services = new Services(treeView);

            TreeItem<String> rootItem = new TreeItem<>("No File");
            treeView.setRoot(rootItem);
            root.setOnDragOver(this::handleDragOver);
            root.setOnDragDropped(this::handleDragDropped);
        });
    }

    private void handleDragOver(DragEvent event) {
        Dragboard dragboard = event.getDragboard();
        if (dragboard.hasFiles()) {
            event.acceptTransferModes(TransferMode.COPY);
        } else {
            event.consume();
        }
    }

    private void handleDragDropped(DragEvent event) {
        Dragboard dragboard = event.getDragboard();
        if (dragboard.hasFiles()) {
            for (File file : dragboard.getFiles()) {
                try {
                    treeView.getRoot().getChildren().clear();
                    logger.info(file.getAbsolutePath());
                    services.handleJsonFile(file.getAbsolutePath());
                } catch (Exception e) {
                    logger.warning("Error handling dropped file: " + e.getMessage());
                }
            }
            event.setDropCompleted(true);
        } else {
            event.setDropCompleted(false);
        }
        event.consume();
    }


}
