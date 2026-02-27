package com.lottie4j.fxplayer.element;

import com.lottie4j.core.definition.GradientType;
import com.lottie4j.core.model.AnimatedValueType;
import com.lottie4j.core.model.shape.style.GradientFill;
import com.lottie4j.fxplayer.util.LottieValueHelper;
import javafx.scene.paint.*;

import java.util.ArrayList;
import java.util.List;

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

            // The gradient color data is stored as a flat array:
            // [offset1, r1, g1, b1, offset2, r2, g2, b2, ...]
            // Where each color stop has 4 values: offset (0-1), R, G, B
            for (int i = 0; i < numColors; i++) {
                double offset = gradientFill.colors().colors().getValue(i * 4, frame);
                double r = LottieValueHelper.clamp(gradientFill.colors().colors().getValue(i * 4 + 1, frame));
                double g = LottieValueHelper.clamp(gradientFill.colors().colors().getValue(i * 4 + 2, frame));
                double b = LottieValueHelper.clamp(gradientFill.colors().colors().getValue(i * 4 + 3, frame));

                stops.add(new Stop(offset, Color.color(r, g, b)));
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

    public double getOpacity(double frame) {
        if (gradientFill.opacity() != null) {
            return gradientFill.opacity().getValue(0, frame) / 100.0;
        }
        return 1.0;
    }
}
