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

        // Get RGB values and normalize from 0-255 to 0-1.0 range
        double r = fill.color().getValue(AnimatedValueType.RED, frame);
        double g = fill.color().getValue(AnimatedValueType.GREEN, frame);
        double b = fill.color().getValue(AnimatedValueType.BLUE, frame);

        // Get opacity and normalize from 0-100 to 0-1.0 range
        double opacity = fill.opacity() != null ? fill.opacity().getValue(1, frame) : 1.0;

        return Color.color(r, g, b, opacity);
    }
}
