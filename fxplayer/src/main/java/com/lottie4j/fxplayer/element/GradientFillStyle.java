package com.lottie4j.fxplayer.element;

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

    public GradientFillStyle(GradientFill gradientFill) {
        this.gradientFill = gradientFill;
    }

    public Paint getPaint(double frame) {
        if (gradientFill == null || gradientFill.colors() == null) {
            return Color.BLACK;
        }


        // Get start and end points
        double startX = 0, startY = 0, endX = 100, endY = 100;
        if (gradientFill.startingPoint() != null) {
            startX = gradientFill.startingPoint().getValue(com.lottie4j.core.model.AnimatedValueType.X, frame);
            startY = gradientFill.startingPoint().getValue(com.lottie4j.core.model.AnimatedValueType.Y, frame);
        }
        if (gradientFill.endPoint() != null) {
            endX = gradientFill.endPoint().getValue(com.lottie4j.core.model.AnimatedValueType.X, frame);
            endY = gradientFill.endPoint().getValue(com.lottie4j.core.model.AnimatedValueType.Y, frame);
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
            // Use proportional mode with normalized coordinates
            // The gradient start/end points define the direction across the shape
            double dx = endX - startX;
            double dy = endY - startY;
            double length = Math.sqrt(dx * dx + dy * dy);

            if (length < 0.001) {
                // Degenerate gradient - fallback to first color
                if (!stops.isEmpty()) {
                    return stops.get(0).getColor();
                }
                return Color.BLACK;
            }

            // Normalize direction vector
            double dirX = dx / length;
            double dirY = dy / length;

            // Create proportional gradient that spans from 0.5 - length/2 to 0.5 + length/2
            // in the direction of the gradient
            double pStart = 0.0; // Start at 0
            double pEnd = 0.5 + 0.5;   // End at 1

            double pStartX = 0.5 - dirX * 0.5;
            double pStartY = 0.5 - dirY * 0.5;
            double pEndX = 0.5 + dirX * 0.5;
            double pEndY = 0.5 + dirY * 0.5;

            return new LinearGradient(
                    pStartX, pStartY, pEndX, pEndY,
                    true, // proportional = true
                    CycleMethod.NO_CYCLE,
                    stops
            );
        }
    }

    public double getOpacity(double frame) {
        if (gradientFill.opacity() != null) {
            return gradientFill.opacity().getValue(0, frame) / 100.0;
        }
        return 1.0;
    }
}
