package com.lottie4j.fxfileviewer.util;

import javafx.scene.control.Alert;

/**
 * Utility class for displaying JavaFX alert dialogs.
 * Provides convenience methods for showing error messages to users.
 */
public class AlertHelper {

    private AlertHelper() {
        // Hide constructor
    }

    /**
     * Displays an error alert dialog with the specified message.
     * The alert is modal and blocks until the user dismisses it.
     *
     * @param message the error message to display
     */
    public static void showError(String message) {
        var alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
