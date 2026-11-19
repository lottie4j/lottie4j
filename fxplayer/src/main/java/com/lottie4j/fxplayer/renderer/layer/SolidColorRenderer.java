package com.lottie4j.fxplayer.renderer.layer;

import com.lottie4j.core.model.layer.Layer;
import com.lottie4j.fxplayer.util.ColorParser;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Renderer for solid color layers.
 * Renders a filled rectangle with the layer's solid color.
 */
public class SolidColorRenderer {

    private static final Logger logger = LoggerFactory.getLogger(SolidColorRenderer.class);

    /**
     * Creates a new SolidColorRenderer.
     */
    public SolidColorRenderer() {
        // Constructor for SolidColorRenderer
    }

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
            logger.warn("Solid color layer has no color: {}", layer.name());
            return;
        }

        // Parse the solid color (format: "RRGGBBAA" or similar hex string)
        Color color = ColorParser.parse(layer.solidColor());
        if (color == null) {
            logger.warn("Could not parse solid color: {}", layer.solidColor());
            return;
        }

        gc.setFill(color);
        if (layer.solidWidth() != null && layer.solidHeight() != null) {
            // The solid is a rectangle of its own size at the origin of the layer; the layer
            // transform, applied by the caller, places it.
            double width = layer.solidWidth();
            double height = layer.solidHeight();
            gc.fillRect(0, 0, width, height);
            logger.debug("Rendered solid color layer: {} color={} size={}x{}", layer.name(), color, width, height);
            return;
        }

        // A solid without a size of its own ("sw", "sh"): fill the composition, generously, so
        // that it is covered whatever the layer transform is.
        double width = layer.width() != null ? layer.width() : animationWidth;
        double height = layer.height() != null ? layer.height() : animationHeight;
        gc.fillRect(-width, -height, width * 3, height * 3);

        logger.debug("Rendered solid color layer: {} color={} size={}x{}", layer.name(), color, width, height);
    }
}

