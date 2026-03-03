package com.lottie4j.fxplayer.util;

import javafx.scene.canvas.GraphicsContext;

/**
 * Helper class for handling stroke rendering with scale compensation.
 */
public class StrokeHelper {

    private static final double MINIMUM_STROKE_WIDTH = 0.001;

    private StrokeHelper() {
        // Hide constructor
    }

    /**
     * Check if a stroke width is large enough to be rendered.
     * Strokes with width below the threshold should be skipped to match JavaScript renderer behavior.
     *
     * @param strokeWidth the stroke width to check
     * @return true if the stroke should be rendered, false if it should be skipped
     */
    public static boolean shouldRenderStroke(double strokeWidth) {
        return strokeWidth > MINIMUM_STROKE_WIDTH;
    }

    /**
     * Calculate the compensated stroke width accounting for canvas transform scale.
     * When the canvas is scaled, stroke widths are scaled too. This compensates for that
     * to maintain consistent visual stroke thickness relative to the original design.
     *
     * @param gc the graphics context containing the current transform
     * @param strokeWidth the original stroke width from the Lottie file
     * @return the compensated stroke width adjusted for canvas scale
     */
    public static double getCompensatedStrokeWidth(GraphicsContext gc, double strokeWidth) {
        var transform = gc.getTransform();
        double scaleX = transform.getMxx();
        double scaleY = transform.getMyy();
        double avgScale = Math.sqrt(Math.abs(scaleX * scaleY));

        return strokeWidth / avgScale;
    }
}
