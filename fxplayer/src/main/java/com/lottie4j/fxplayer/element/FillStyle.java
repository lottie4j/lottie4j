package com.lottie4j.fxplayer.element;

import com.lottie4j.core.model.AnimatedValueType;
import com.lottie4j.core.model.shape.Fill;
import javafx.scene.paint.Color;

public class FillStyle {

    private final Fill fill;

    public FillStyle(Fill fill) {
        this.fill = fill;
    }

    public Color getColor(long timestamp) {
        if (fill == null) {
            return Color.BLACK;
        }
        return Color.color(
                fill.color().getValue(AnimatedValueType.RED, timestamp),
                fill.color().getValue(AnimatedValueType.GREEN, timestamp),
                fill.color().getValue(AnimatedValueType.BLEU, timestamp),
                fill.color().getValue(AnimatedValueType.OPACITY, timestamp)
        ); // value 0-1.0
    }
}
