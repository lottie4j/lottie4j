package com.lottie4j.fxplayer.element;

import java.util.List;

import com.lottie4j.core.definition.AnimatedValueType;
import com.lottie4j.core.definition.GradientType;
import com.lottie4j.core.model.shape.style.GradientFill;

import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Paint;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;

public class GradientFillStyle {
    private final GradientFill gradientFill;

    /**
     * Creates a gradient fill style wrapper for a Lottie gradient fill definition.
     *
     * @param gradientFill source gradient fill definition
     */
    public GradientFillStyle(GradientFill gradientFill) {
        this.gradientFill = gradientFill;
    }

    /**
     * Builds a JavaFX paint object from the gradient definition at the given frame.
     *
     * @param frame animation frame to sample
     * @return linear/radial gradient paint, or black when gradient data is unavailable
     */
    public Paint getPaint(double frame) {
        return getPaint(frame, 0, 0, 0, 0);
    }

    /**
     * Builds a JavaFX paint object from the gradient definition at the given frame,
     * with shape bounds for proper coordinate transformation.
     *
     * @param frame       animation frame to sample
     * @param shapeX      X position of the shape being filled
     * @param shapeY      Y position of the shape being filled
     * @param shapeWidth  Width of the shape being filled
     * @param shapeHeight Height of the shape being filled
     * @return linear/radial gradient paint, or black when gradient data is unavailable
     */
    public Paint getPaint(double frame, double shapeX, double shapeY, double shapeWidth, double shapeHeight) {
        if (gradientFill == null || gradientFill.colors() == null) {
            return Color.BLACK;
        }

        // Get start and end points
        double startX = 0;
        double startY = 0;
        double endX = 100;
        double endY = 100;
        if (gradientFill.startingPoint() != null) {
            startX = gradientFill.startingPoint().getValue(AnimatedValueType.X, frame);
            startY = gradientFill.startingPoint().getValue(AnimatedValueType.Y, frame);
        }
        if (gradientFill.endPoint() != null) {
            endX = gradientFill.endPoint().getValue(AnimatedValueType.X, frame);
            endY = gradientFill.endPoint().getValue(AnimatedValueType.Y, frame);
        }

        // Get gradient colors
        // Lottie packs gradient stops as [pos1, r1, g1, b1, ..., posN, rN, gN, bN]
        // optionally followed by alpha stops [pos1, a1, ..., posM, aM] where the
        // alpha offsets are independent from the colour offsets. Delegate to the
        // dedicated parser so colour and alpha tracks are merged at every offset.
        // The parser also densifies each adjacent stop-pair with linearly-interpolated
        // sub-stops sampled in linear-RGB space so the resulting JavaFX gradient
        // approximates thorvg's / lottie-web's linear-RGB midpoints even though
        // JavaFX itself always interpolates in sRGB.
        List<Stop> stops;
        if (gradientFill.colors().colors() != null) {
            Integer numColorsBoxed = gradientFill.colors().numberOfColors();
            int numColors = numColorsBoxed != null ? numColorsBoxed : 0;
            stops = GradientStopParser.parseStopsForLinearRgb(gradientFill.colors().colors(), numColors);
        } else {
            stops = List.of();
        }

        if (stops.isEmpty()) {
            return Color.BLACK;
        }

        // ALWAYS use proportional mode when bounds are provided
        // Proportional mode ensures gradient is shape-relative and works with transforms
        boolean hasShapeBounds = (shapeWidth > 0 && shapeHeight > 0);

        // Check gradient type and create appropriate gradient
        if (gradientFill.gradientType() == GradientType.RADIAL) {
            // For radial gradients, use proportional coordinates
            // The start point becomes the center, and we calculate radius from the distance to end point
            double radius = Math.sqrt(Math.pow(endX - startX, 2) + Math.pow(endY - startY, 2));

            if (hasShapeBounds) {
                // Transform gradient coordinates from Lottie space to shape-relative proportional coordinates
                double centerX = (startX - shapeX) / shapeWidth;
                double centerY = (startY - shapeY) / shapeHeight;

                // Calculate proportional radius based on average of width/height
                double avgDimension = (shapeWidth + shapeHeight) / 2.0;
                double propRadius = radius / avgDimension;

                return new RadialGradient(
                        0, // focusAngle
                        0, // focusDistance
                        centerX, centerY,
                        propRadius,
                        true, // proportional = true (use relative coordinates)
                        CycleMethod.NO_CYCLE,
                        stops
                );
            } else {
                // Fallback: use absolute coordinates
                return new RadialGradient(
                        0, // focusAngle
                        0, // focusDistance
                        startX, startY,
                        radius,
                        false, // proportional = false (use absolute coordinates)
                        CycleMethod.NO_CYCLE,
                        stops
                );
            }
        } else {
            // Create linear gradient
            if (hasShapeBounds) {
                // Transform gradient coordinates from Lottie space to shape-relative proportional coordinates
                double propStartX = (startX - shapeX) / shapeWidth;
                double propStartY = (startY - shapeY) / shapeHeight;
                double propEndX = (endX - shapeX) / shapeWidth;
                double propEndY = (endY - shapeY) / shapeHeight;

                return new LinearGradient(
                        propStartX, propStartY, propEndX, propEndY,
                        true, // proportional = true (use relative coordinates)
                        CycleMethod.NO_CYCLE,
                        stops
                );
            } else {
                // Fallback: use absolute coordinates
                return new LinearGradient(
                        startX, startY, endX, endY,
                        false, // proportional = false (use absolute coordinates)
                        CycleMethod.NO_CYCLE,
                        stops
                );
            }
        }
    }

    /**
     * Resolves gradient opacity at a specific frame.
     *
     * @param frame animation frame to sample
     * @return normalized opacity in the range {@code [0, 1]}
     */
    public double getOpacity(double frame) {
        if (gradientFill.opacity() != null) {
            return gradientFill.opacity().getValue(0, frame) / 100.0;
        }
        return 1.0;
    }
}
