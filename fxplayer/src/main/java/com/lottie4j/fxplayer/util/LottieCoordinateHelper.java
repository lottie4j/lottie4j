package com.lottie4j.fxplayer.util;

import com.lottie4j.core.model.AnimatedValueType;
import com.lottie4j.core.model.shape.shape.Rectangle;

/**
 * Utility class for converting Lottie coordinate system to JavaFX coordinate system.
 * <p>
 * Lottie uses a center-based coordinate system where (0,0) is at the center of the composition.
 * JavaFX uses a top-left based coordinate system where (0,0) is at the top-left corner.
 * <p>
 * ## Lottie Coordinate System
 * <p>
 * The Lottie file format uses a **center-based coordinate system** for layers, which differs from many other graphics formats:
 * <p>
 * The coordinate at the centre of the your layer is 0,0 as its origin. On the x-axis, values to the right the centre-point is positive and to the left is negative. On the y-axis, downwards from the center are positive values and upwards from the center are negative values.
 * <p>
 * ### Key Points:
 * <p>
 * 1. **Origin (0,0)**: Located at the **center** of the layer/composition, not the top-left corner
 * 2. **X-axis**: Positive values extend to the right, negative values to the left
 * 3. **Y-axis**: Positive values extend downward from center, negative values upward from center
 * <p>
 * This is different from many web-based coordinate systems that typically use the top-left corner as the origin. The center-based approach aligns with After Effects' coordinate system (since Lottie animations are often exported from After Effects), where transformations like rotation and scaling work more intuitively when anchored to the center.
 * <p>
 * ### Practical Implications:
 * <p>
 * - If your composition is 400×400 pixels, the coordinate (0,0) would be at pixel position (200,200) if you were thinking in top-left coordinate terms
 * - To position an element at what would traditionally be the "top-left" of a composition, you'd use negative x and y coordinates
 * - This center-based system makes animations and transformations more predictable, especially for rotations and scaling operations
 * <p>
 * This coordinate system is consistent with the vector graphics nature of Lottie and its origins as an After Effects export format.
 */
public class LottieCoordinateHelper {

    /**
     * Extracts position and size data from a Lottie Rectangle and converts coordinates
     * from center-based (Lottie) to top-left based (JavaFX).
     *
     * @param rectangle The Lottie Rectangle shape
     * @param frame     The animation frame to get values for
     * @return RectanglePosition with both original and converted coordinates
     */
    public static RectanglePosition getRectanglePosition(Rectangle rectangle, double frame) {
        if (rectangle.size() == null || rectangle.position() == null) {
            throw new IllegalArgumentException("Rectangle missing size or position data");
        }

        // Get animated values at current frame - these are center-based coordinates
        double centerX = rectangle.position().getValue(AnimatedValueType.X, frame);
        double centerY = rectangle.position().getValue(AnimatedValueType.Y, frame);
        double width = rectangle.size().getValue(AnimatedValueType.WIDTH, frame);
        double height = rectangle.size().getValue(AnimatedValueType.HEIGHT, frame);

        // Convert from center-based to top-left based coordinates
        double topLeftX = centerX - (width / 2.0);
        double topLeftY = centerY - (height / 2.0);

        return new RectanglePosition(centerX, centerY, width, height, topLeftX, topLeftY);
    }

    /**
     * Converts a center-based coordinate to top-left based coordinate.
     *
     * @param centerX Center x coordinate
     * @param centerY Center y coordinate
     * @param width   Width of the shape
     * @param height  Height of the shape
     * @return Array with [topLeftX, topLeftY]
     */
    public static double[] centerToTopLeft(double centerX, double centerY, double width, double height) {
        return new double[]{
                centerX - (width / 2.0),
                centerY - (height / 2.0)
        };
    }

    /**
     * Converts a top-left based coordinate to center-based coordinate.
     *
     * @param topLeftX Top-left x coordinate
     * @param topLeftY Top-left y coordinate
     * @param width    Width of the shape
     * @param height   Height of the shape
     * @return Array with [centerX, centerY]
     */
    public static double[] topLeftToCenter(double topLeftX, double topLeftY, double width, double height) {
        return new double[]{
                topLeftX + (width / 2.0),
                topLeftY + (height / 2.0)
        };
    }

    /**
     * Rectangle positioning data extracted from Lottie format
     */
    public record RectanglePosition(
            double x,           // Center x position in Lottie coordinates
            double y,           // Center y position in Lottie coordinates
            double width,       // Rectangle width
            double height,      // Rectangle height
            double topLeftX,    // Converted top-left x for JavaFX
            double topLeftY     // Converted top-left y for JavaFX
    ) {
    }
}
