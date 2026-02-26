package com.lottie4j.fxplayer.element;

import com.lottie4j.core.model.AnimatedValueType;
import com.lottie4j.core.model.shape.style.Stroke;
import javafx.scene.paint.Color;

public class StrokeStyle {

    private final Stroke stroke;

    public StrokeStyle(Stroke stroke) {
        this.stroke = stroke;
    }

    public Color getColor(double frame) {
        if (stroke.color() == null) {
            return Color.BLACK;
        }

        // Lottie colors are already in 0-1.0 range
        double r = stroke.color().getValue(AnimatedValueType.RED, frame);
        double g = stroke.color().getValue(AnimatedValueType.GREEN, frame);
        double b = stroke.color().getValue(AnimatedValueType.BLUE, frame);

        // Get opacity and normalize from 0-100 to 0-1.0 range
        double opacity = stroke.opacity() != null ?
            stroke.opacity().getValue(AnimatedValueType.OPACITY, frame) / 100.0 : 1.0;

        return Color.color(r, g, b, opacity);
    }

    public Double getStrokeWidth(double frame) {
        if (stroke.strokeWidth() == null) {
            return 0D;
        }
        return stroke.strokeWidth().getValue(AnimatedValueType.WIDTH, frame);
    }
}
