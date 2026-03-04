package com.lottie4j.fxfileviewer.util;

import javafx.scene.control.Alert;

public class AlertHelper {

    private AlertHelper() {
        // Hide constructor
    }

    public static void showError(String message) {
        var alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
