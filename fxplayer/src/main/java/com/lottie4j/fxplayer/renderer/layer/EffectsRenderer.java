package com.lottie4j.fxplayer.renderer.layer;

import com.lottie4j.core.definition.EffectType;
import com.lottie4j.core.model.Layer;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.effect.GaussianBlur;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Renderer for layer effects such as Gaussian Blur.
 * Extracts effect values and applies JavaFX effects to the graphics context.
 */
public class EffectsRenderer {

    private static final Logger logger = LoggerFactory.getLogger(EffectsRenderer.class.getName());

    /**
     * Extracts the Gaussian Blur radius from layer effects at a specific frame.
     *
     * @param layer layer to check for blur effects
     * @param frame animation frame to sample
     * @return blur radius scaled for JavaFX, or 0.0 if no blur effect active
     */
    public double getGaussianBlurRadius(Layer layer, double frame) {
        // Default to no blur
        double blurRadius = 0.0;

        // Check if the layer has effects
        if (layer.effects() == null) {
            return blurRadius;
        }

        for (var effect : layer.effects()) {
            // Look for Gaussian Blur effect (type 14)
            if (effect.type() != EffectType.GAUSSIAN_BLUR) {
                continue;
            }

            // Skip if effect is disabled
            if (effect.enabled() != null && effect.enabled() == 0) {
                continue;
            }

            // Look for "Blurriness" property in the effect values
            if (effect.values() != null) {
                for (var effectValue : effect.values()) {
                    if (effectValue != null && "Blurriness".equals(effectValue.name())) {
                        if (effectValue.value() != null) {
                            double rawBlur = effectValue.value().getValue(0, frame);
                            // Scale blur radius for JavaFX - After Effects blurriness is much stronger
                            // Use much higher divisors to match the visual strength
                            if (rawBlur > 100) {
                                blurRadius = rawBlur / 10.0;  // Heavy blur - strong scale down
                            } else if (rawBlur > 50) {
                                blurRadius = rawBlur / 6.0;  // Medium blur
                            } else {
                                blurRadius = rawBlur / 3.5;  // Light blur
                            }
                            logger.debug("Gaussian Blur: raw=" + rawBlur + " scaled=" + blurRadius + " for layer " + layer.name() + " at frame " + frame);
                        }
                        break;
                    }
                }
            }
            break;
        }

        return blurRadius;
    }

    /**
     * Renders a layer with Gaussian Blur effect applied.
     *
     * @param gc            graphics context
     * @param layer         layer to render
     * @param frame         animation frame
     * @param blurRadius    blur radius to apply
     * @param layerRenderer callback to render the layer content
     */
    public void renderLayerWithGaussianBlur(GraphicsContext gc, Layer layer, double frame, double blurRadius, LayerRenderer layerRenderer) {
        gc.save();

        // Apply the Gaussian blur effect
        GaussianBlur blur = new GaussianBlur(blurRadius);
        gc.setEffect(blur);

        // Render the layer normally
        layerRenderer.render(gc, layer, frame);

        gc.setEffect(null);
        gc.restore();
    }

    /**
     * Callback interface for rendering layer content.
     */
    @FunctionalInterface
    public interface LayerRenderer {
        /**
         * Renders the layer content to the graphics context.
         *
         * @param gc    graphics context
         * @param layer layer to render
         * @param frame animation frame
         */
        void render(GraphicsContext gc, Layer layer, double frame);
    }
}

