package com.lottie4j.fxplayer.element;

import com.lottie4j.core.definition.AnimatedValueType;
import com.lottie4j.core.definition.GradientType;
import com.lottie4j.core.model.shape.style.GradientFill;
import com.lottie4j.fxplayer.util.LottieValueHelper;
import javafx.scene.paint.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        List<Stop> stops = new ArrayList<>();
        if (gradientFill.colors().colors() != null) {
            int numColors = gradientFill.colors().numberOfColors();
            var colorAnimated = gradientFill.colors().colors();

            // The gradient color data is stored as a flat array with two possible layouts:
            // Without alpha: [offset1, r1, g1, b1, offset2, r2, g2, b2, ...]
            // With alpha: [offset1, r1, g1, b1, ..., offsetN, rN, gN, bN, alpha_offset1, alpha1, alpha_offset2, alpha2, ...]

            // Access the keyframes list directly to get all elements
            int totalElements = colorAnimated.keyframes() != null ? colorAnimated.keyframes().size() : 0;

            // Build a map of alpha values by offset
            Map<Double, Double> alphaByOffset = new HashMap<>();

            // If there are more elements than numColors * 4, there's an alpha section
            int colorSectionSize = numColors * 4;
            if (totalElements > colorSectionSize) {
                // Parse alpha values (interleaved: offset, alpha, offset, alpha, ...)
                for (int i = colorSectionSize; i < totalElements; i += 2) {
                    Double alphaOffset = colorAnimated.getValue(i);
                    Double alphaValue = colorAnimated.getValue(i + 1);
                    if (alphaOffset != null && alphaValue != null) {
                        alphaByOffset.put(alphaOffset, LottieValueHelper.clamp(alphaValue));
                    }
                }
            }

            // Parse color stops
            for (int i = 0; i < numColors; i++) {
                int baseIdx = i * 4;
                double offset = colorAnimated.getValue(baseIdx);
                double rRaw = colorAnimated.getValue(baseIdx + 1);
                double gRaw = colorAnimated.getValue(baseIdx + 2);
                double bRaw = colorAnimated.getValue(baseIdx + 3);

                double r = LottieValueHelper.clamp(rRaw);
                double g = LottieValueHelper.clamp(gRaw);
                double b = LottieValueHelper.clamp(bRaw);

                // Get alpha for this offset, default to 1.0 if not specified
                double alpha = alphaByOffset.getOrDefault(offset, 1.0);

                Color stopColor = Color.color(r, g, b, alpha);
                stops.add(new Stop(offset, stopColor));
            }
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
