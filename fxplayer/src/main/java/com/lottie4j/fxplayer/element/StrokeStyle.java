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
        return Color.color(
                stroke.color().getValue(AnimatedValueType.RED, frame),
                stroke.color().getValue(AnimatedValueType.GREEN, frame),
                stroke.color().getValue(AnimatedValueType.BLUE, frame),
                stroke.color().getValue(AnimatedValueType.OPACITY, frame)
        ); // value 0-1.0
    }

    public Double getStrokeWidth(double frame) {
        if (stroke.strokeWidth() == null) {
            return 0D;
        }
        return stroke.strokeWidth().getValue(AnimatedValueType.WIDTH, frame);
    }
}
