package com.lottie4j.fxplayer.element;

import com.lottie4j.core.definition.GradientType;
import com.lottie4j.core.model.AnimatedValueType;
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
                double r = LottieValueHelper.clamp(colorAnimated.getValue(baseIdx + 1));
                double g = LottieValueHelper.clamp(colorAnimated.getValue(baseIdx + 2));
                double b = LottieValueHelper.clamp(colorAnimated.getValue(baseIdx + 3));

                // Get alpha for this offset, default to 1.0 if not specified
                double alpha = alphaByOffset.getOrDefault(offset, 1.0);

                stops.add(new Stop(offset, Color.color(r, g, b, alpha)));
            }
        }

        if (stops.isEmpty()) {
            return Color.BLACK;
        }

        // Check gradient type and create appropriate gradient
        if (gradientFill.gradientType() == GradientType.RADIAL) {
            // For radial gradients, calculate center point and radius
            double centerX = startX;
            double centerY = startY;
            double radius = Math.sqrt(Math.pow(endX - startX, 2) + Math.pow(endY - startY, 2));

            return new RadialGradient(
                    0, // focusAngle
                    0, // focusDistance
                    centerX, centerY,
                    radius,
                    false, // proportional = false (use absolute coordinates)
                    CycleMethod.NO_CYCLE,
                    stops
            );
        } else {
            // Create linear gradient (default)
            // The coordinates need to be normalized (0-1 range) for JavaFX LinearGradient
            // We'll use the actual start/end points from Lottie
            return new LinearGradient(
                    startX, startY, endX, endY,
                    false, // proportional = false (use absolute coordinates)
                    CycleMethod.NO_CYCLE,
                    stops
            );
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
