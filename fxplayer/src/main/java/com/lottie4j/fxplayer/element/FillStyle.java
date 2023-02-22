package com.lottie4j.fxplayer.element;

import com.lottie4j.core.model.Animated;
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
                fill.color().getValue(Animated.ValueType.RED, timestamp),
                fill.color().getValue(Animated.ValueType.GREEN, timestamp),
                fill.color().getValue(Animated.ValueType.BLEU, timestamp),
                fill.color().getValue(Animated.ValueType.OPACITY, timestamp)
        ); // value 0-1.0
    }
}
