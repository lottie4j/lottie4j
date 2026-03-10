package com.lottie4j.fxplayer.util;

import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

/**
 * Utility class for rendering content to an off-screen buffer.
 * Useful for applying opacity and other effects to composite shapes.
 */
public class OffscreenRenderer {

    /**
     * Renders content to an off-screen canvas and returns it as an image.
     *
     * @param width    width of the off-screen canvas
     * @param height   height of the off-screen canvas
     * @param renderer function that performs the rendering
     * @return WritableImage containing the rendered content
     */
    public static WritableImage renderToImage(double width, double height, OffscreenRenderingTask renderer) {
        // Create an off-screen canvas
        Canvas offscreenCanvas = new Canvas(width, height);
        GraphicsContext offscreenGc = offscreenCanvas.getGraphicsContext2D();

        // Clear the off-screen canvas (transparent background)
        offscreenGc.clearRect(0, 0, width, height);

        // Perform the rendering
        renderer.render(offscreenGc);

        // Create an image from the canvas with transparent fill.
        WritableImage image = new WritableImage((int) width, (int) height);
        SnapshotParameters snapshotParameters = new SnapshotParameters();
        snapshotParameters.setFill(Color.TRANSPARENT);
        offscreenCanvas.snapshot(snapshotParameters, image);

        return image;
    }

    /**
     * Functional interface for off-screen rendering tasks.
     */
    @FunctionalInterface
    public interface OffscreenRenderingTask {
        /**
         * Performs rendering to an off-screen graphics context.
         *
         * @param gc off-screen graphics context
         */
        void render(GraphicsContext gc);
    }
}
