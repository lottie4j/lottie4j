package com.lottie4j.fxplayer.element;

import com.lottie4j.core.model.AnimatedValueType;
import com.lottie4j.core.model.shape.style.Stroke;
import com.lottie4j.fxplayer.util.LottieValueHelper;
import javafx.scene.paint.Color;

public record StrokeStyle(Stroke stroke) {

    /**
     * Creates a stroke style wrapper for a Lottie stroke definition.
     *
     * @param stroke source stroke definition
     */
    public StrokeStyle {
    }

    /**
     * Resolves the stroke color at the given frame, including animated opacity.
     *
     * @param frame animation frame to sample
     * @return resolved RGBA color, or black when stroke color is missing
     */
    public Color getColor(double frame) {
        if (stroke.color() == null) {
            return Color.BLACK;
        }

        // Lottie colors should be in 0-1.0 range, but clamp to handle edge cases
        // (e.g., values like "004" parsed as 4 due to leading zeros)
        double r = LottieValueHelper.clamp(stroke.color().getValue(AnimatedValueType.RED, frame));
        double g = LottieValueHelper.clamp(stroke.color().getValue(AnimatedValueType.GREEN, frame));
        double b = LottieValueHelper.clamp(stroke.color().getValue(AnimatedValueType.BLUE, frame));

        // Get opacity and normalize from 0-100 to 0-1.0 range
        // Opacity is a single value, so use index 0 with frame for animation
        double opacity = stroke.opacity() != null ?
                stroke.opacity().getValue(0, frame) / 100.0 : 1.0;

        return Color.color(r, g, b, opacity);
    }

    /**
     * Resolves the stroke width at the given frame.
     *
     * @param frame animation frame to sample
     * @return stroke width in layer units, or {@code 0} when not defined
     */
    public Double getStrokeWidth(double frame) {
        if (stroke.strokeWidth() == null) {
            return 0D;
        }
        // Stroke width is a single value, so use index 0 with frame for animation
        return stroke.strokeWidth().getValue(0, frame);
    }
}
