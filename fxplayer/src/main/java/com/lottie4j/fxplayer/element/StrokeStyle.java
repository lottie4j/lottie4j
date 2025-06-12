package com.lottie4j.fxplayer.element;

import com.lottie4j.core.model.AnimatedValueType;
import com.lottie4j.core.model.shape.Stroke;
import javafx.scene.paint.Color;

public class StrokeStyle {

    private final Stroke stroke;

    public StrokeStyle(Stroke stroke) {
        this.stroke = stroke;
    }

    public Color getColor(long timestamp) {
        if (stroke.color() == null) {
            return Color.BLACK;
        }
        return Color.color(
                stroke.color().getValue(AnimatedValueType.RED, timestamp),
                stroke.color().getValue(AnimatedValueType.GREEN, timestamp),
                stroke.color().getValue(AnimatedValueType.BLUE, timestamp),
                stroke.color().getValue(AnimatedValueType.OPACITY, timestamp)
        ); // value 0-1.0
    }

    public Double getStrokeWidth(long timestamp) {
        if (stroke.strokeWidth() == null) {
            return 0D;
        }
        return stroke.strokeWidth().getValue(AnimatedValueType.WIDTH, timestamp);
    }
}
