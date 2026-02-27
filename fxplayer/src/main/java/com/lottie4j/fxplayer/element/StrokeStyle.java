package com.lottie4j.fxplayer.element;

import com.lottie4j.core.model.AnimatedValueType;
import com.lottie4j.core.model.shape.style.Stroke;
import com.lottie4j.fxplayer.util.LottieValueHelper;
import javafx.scene.paint.Color;

public class StrokeStyle {

    public final Stroke stroke;

    public StrokeStyle(Stroke stroke) {
        this.stroke = stroke;
    }

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

    public Double getStrokeWidth(double frame) {
        if (stroke.strokeWidth() == null) {
            return 0D;
        }
        // Stroke width is a single value, so use index 0 with frame for animation
        return stroke.strokeWidth().getValue(0, frame);
    }
}
