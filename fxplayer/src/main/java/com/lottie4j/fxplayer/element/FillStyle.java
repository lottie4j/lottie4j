package com.lottie4j.fxplayer.element;

import com.lottie4j.core.model.AnimatedValueType;
import com.lottie4j.core.model.shape.style.Fill;
import javafx.scene.paint.Color;

public class FillStyle {

    private final Fill fill;

    public FillStyle(Fill fill) {
        this.fill = fill;
    }

    public Color getColor(double frame) {
        if (fill == null || fill.color() == null) {
            return Color.BLACK;
        }

        // Lottie colors are already in 0-1.0 range
        double r = fill.color().getValue(AnimatedValueType.RED, frame);
        double g = fill.color().getValue(AnimatedValueType.GREEN, frame);
        double b = fill.color().getValue(AnimatedValueType.BLUE, frame);

        // Get opacity and normalize from 0-100 to 0-1.0 range
        // Opacity is a single value, so use index 0 with frame for animation
        double opacity = fill.opacity() != null ?
            fill.opacity().getValue(0, frame) / 100.0 : 1.0;

        return Color.color(r, g, b, opacity);
    }
}
