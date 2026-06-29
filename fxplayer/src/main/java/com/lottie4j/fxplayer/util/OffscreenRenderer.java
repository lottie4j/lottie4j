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
     * Creates a new OffscreenRenderer.
     */
    public OffscreenRenderer() {
    }

    /**
     * Renders content to an off-screen canvas and returns it as an image.
     *
     * <p>Buffer dimensions are rounded <em>up</em> to the next integer pixel so that
     * the right and bottom sub-pixel edges of fractional layer bounds are preserved
     * during snapshot. Rounding down would silently drop the last column/row of
     * anti-aliased coverage, causing a uniform fidelity tax on every composited
     * layer (most visible with blend modes such as MULTIPLY and SCREEN).</p>
     *
     * @param width    width of the off-screen canvas (may be fractional)
     * @param height   height of the off-screen canvas (may be fractional)
     * @param renderer function that performs the rendering
     * @return WritableImage containing the rendered content
     */
    public static WritableImage renderToImage(double width, double height, OffscreenRenderingTask renderer) {
        int pixelWidth = Math.max(1, (int) Math.ceil(width));
        int pixelHeight = Math.max(1, (int) Math.ceil(height));

        // Create an off-screen canvas sized to the same integer pixel grid as the
        // snapshot target so the snapshot captures every rendered pixel without
        // sub-pixel truncation.
        Canvas offscreenCanvas = new Canvas(pixelWidth, pixelHeight);
        GraphicsContext offscreenGc = offscreenCanvas.getGraphicsContext2D();

        // Clear the off-screen canvas (transparent background)
        offscreenGc.clearRect(0, 0, pixelWidth, pixelHeight);

        // Perform the rendering
        renderer.render(offscreenGc);

        // Create an image from the canvas with transparent fill.
        WritableImage image = new WritableImage(pixelWidth, pixelHeight);
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
