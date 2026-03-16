package com.lottie4j.fxplayer.element;

import com.lottie4j.core.definition.AnimatedValueType;
import com.lottie4j.core.model.shape.style.Fill;
import com.lottie4j.fxplayer.util.LottieValueHelper;
import javafx.scene.paint.Color;

public class FillStyle {

    private final Fill fill;

    /**
     * Creates a fill style wrapper for a Lottie fill definition.
     *
     * @param fill source fill definition; may be null
     */
    public FillStyle(Fill fill) {
        this.fill = fill;
    }

    /**
     * Resolves the fill color at the provided frame, including animated opacity.
     *
     * @param frame animation frame to sample
     * @return resolved RGBA color, or black when no fill color is available
     */
    public Color getColor(double frame) {
        if (fill == null || fill.color() == null) {
            return Color.BLACK;
        }

        // Lottie colors should be in 0-1.0 range, but clamp to handle edge cases
        // (e.g., values like "004" parsed as 4 due to leading zeros)
        double rRaw = fill.color().getValue(AnimatedValueType.RED, frame);
        double gRaw = fill.color().getValue(AnimatedValueType.GREEN, frame);
        double bRaw = fill.color().getValue(AnimatedValueType.BLUE, frame);

        double r = LottieValueHelper.clamp(rRaw);
        double g = LottieValueHelper.clamp(gRaw);
        double b = LottieValueHelper.clamp(bRaw);

        // Get opacity and normalize from 0-100 to 0-1.0 range
        // Opacity is a single value, so use index 0 with frame for animation
        double opacity = fill.opacity() != null ?
                fill.opacity().getValue(0, frame) / 100.0 : 1.0;

        return Color.color(r, g, b, opacity);
    }
}
