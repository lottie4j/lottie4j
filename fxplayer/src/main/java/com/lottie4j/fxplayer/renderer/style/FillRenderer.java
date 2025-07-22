package com.lottie4j.fxplayer.renderer.style;

import com.lottie4j.core.model.AnimatedValueType;
import com.lottie4j.core.model.shape.style.Fill;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.logging.Logger;

public class FillRenderer {
    private static final Logger logger = Logger.getLogger(FillRenderer.class.getName());

    public void render(GraphicsContext gc, Fill fill, double frame) {
        if (fill.color() != null) {
            // Get RGB components individually
            double r = fill.color().getValue(AnimatedValueType.RED, frame) / 255.0;
            double g = fill.color().getValue(AnimatedValueType.GREEN, frame) / 255.0;
            double b = fill.color().getValue(AnimatedValueType.BLUE, frame) / 255.0;

            // Get opacity
            double opacity = fill.opacity() != null ?
                    fill.opacity().getValue(0, frame) / 100.0 : 1.0;

            gc.setFill(Color.color(r, g, b, opacity));
        }
    }
}
