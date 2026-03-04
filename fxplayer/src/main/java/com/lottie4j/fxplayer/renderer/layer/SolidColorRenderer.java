package com.lottie4j.fxplayer.renderer.layer;

import com.lottie4j.core.model.Layer;
import com.lottie4j.fxplayer.util.ColorParser;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.logging.Logger;

/**
 * Renderer for solid color layers.
 * Renders a filled rectangle with the layer's solid color.
 */
public class SolidColorRenderer {

    private static final Logger logger = Logger.getLogger(SolidColorRenderer.class.getName());

    /**
     * Renders a solid color layer as a filled rectangle.
     *
     * @param gc              graphics context
     * @param layer           solid color layer to render
     * @param animationWidth  default width if layer has no explicit width
     * @param animationHeight default height if layer has no explicit height
     */
    public void render(GraphicsContext gc, Layer layer, int animationWidth, int animationHeight) {
        if (layer.solidColor() == null) {
            logger.warning("Solid color layer has no color: " + layer.name());
            return;
        }

        // Parse the solid color (format: "RRGGBBAA" or similar hex string)
        Color color = ColorParser.parse(layer.solidColor(), logger);
        if (color == null) {
            logger.warning("Could not parse solid color: " + layer.solidColor());
            return;
        }

        // Get the layer dimensions if specified, otherwise use animation dimensions
        double width = layer.width() != null ? layer.width() : animationWidth;
        double height = layer.height() != null ? layer.height() : animationHeight;

        // Fill from negative coordinates to ensure full coverage in precompositions
        // This is important because solid color should fill the entire composition
        gc.setFill(color);
        gc.fillRect(-width, -height, width * 3, height * 3);

        logger.fine("Rendered solid color layer: " + layer.name() + " color=" + color + " size=" + width + "x" + height);
    }
}

