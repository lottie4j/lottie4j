package com.lottie4j.fxplayer.renderer.layer;

import com.lottie4j.core.definition.EffectType;
import com.lottie4j.core.model.Layer;
import com.lottie4j.fxplayer.util.OffscreenRenderer;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.effect.BoxBlur;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.image.WritableImage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Renderer for layer effects such as Gaussian Blur.
 * Extracts effect values and applies JavaFX effects to the graphics context.
 */
public class EffectsRenderer {

    private static final Logger logger = LoggerFactory.getLogger(EffectsRenderer.class);

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
            logger.debug("Layer {} has NO effects", layer.name());
            return blurRadius;
        }

        logger.debug("Layer {} has {} effects", layer.name(), layer.effects().size());
        // Log track matte and mask info for blur layers
        logger.debug("  Layer matteMode: {}, hasMask: {}", layer.matteMode(), layer.masks() != null && !layer.masks().isEmpty());

        for (var effect : layer.effects()) {
            logger.debug("  Effect type: {}, enabled: {}", effect.type(), effect.enabled());
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
                logger.debug("  Effect has {} values", effect.values().size());
                for (var effectValue : effect.values()) {
                    logger.debug("    Value name: {}", effectValue != null ? effectValue.name() : "null");
                    if (effectValue != null && "Blurriness".equals(effectValue.name())) {
                        if (effectValue.value() != null) {
                            double rawBlur = effectValue.value().getValue(0, frame);

                            // The key insight: Lottie-web's blur values of 700-800 create VERY diffuse blur
                            // that makes shapes blend together into gradient-like appearances.
                            // JavaFX blur is fundamentally limited to 63px radius.
                            // Strategy: Use maximum blur (63) for all extreme values and rely on opacity
                            // to create the gradient effect when multiple blurred shapes overlap.

                            if (rawBlur > 400) {
                                // Extreme blur (400+) - always use maximum
                                blurRadius = 63.0;
                            } else if (rawBlur > 200) {
                                // Very high blur - scale to near maximum
                                blurRadius = 50 + ((rawBlur - 200) / 200.0) * 13.0;
                            } else if (rawBlur > 100) {
                                // High blur
                                blurRadius = 35 + ((rawBlur - 100) / 100.0) * 15.0;
                            } else if (rawBlur > 50) {
                                // Medium-high blur
                                blurRadius = 20 + ((rawBlur - 50) / 50.0) * 15.0;
                            } else if (rawBlur > 20) {
                                // Medium blur
                                blurRadius = 10 + ((rawBlur - 20) / 30.0) * 10.0;
                            } else {
                                // Light blur
                                blurRadius = rawBlur / 2.0;
                            }

                            // Ensure we never exceed JavaFX's limit
                            blurRadius = Math.min(63, blurRadius);

                            logger.debug("Gaussian Blur FOUND: raw={} scaled={} for layer {} at frame {}", rawBlur, blurRadius, layer.name(), frame);
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
        renderLayerWithGaussianBlur(gc, layer, frame, blurRadius, -1, -1, layerRenderer);
    }

    /**
     * Renders a layer with Gaussian Blur effect applied and optional bounds clipping.
     *
     * @param gc            graphics context
     * @param layer         layer to render
     * @param frame         animation frame
     * @param blurRadius    blur radius to apply
     * @param boundsWidth   clipping width in local coordinates; <= 0 disables bounds clipping
     * @param boundsHeight  clipping height in local coordinates; <= 0 disables bounds clipping
     * @param layerRenderer callback to render the layer content
     */
    public void renderLayerWithGaussianBlur(GraphicsContext gc,
                                            Layer layer,
                                            double frame,
                                            double blurRadius,
                                            double boundsWidth,
                                            double boundsHeight,
                                            LayerRenderer layerRenderer) {
        logger.debug("renderLayerWithGaussianBlur called for layer {}: blurRadius={}, boundsWidth={}, boundsHeight={}",
                layer.name(), blurRadius, boundsWidth, boundsHeight);

        if (boundsWidth > 0 && boundsHeight > 0) {
            // Render with explicit bounds clipping - applies transforms in offscreen, clips when drawing back
            renderWithBoundedBlur(gc, layer, frame, blurRadius, boundsWidth, boundsHeight, layerRenderer);
            return;
        }

        // Fallback: no bounds, just apply blur directly
        gc.save();
        applyBlurEffect(gc, blurRadius);
        layerRenderer.render(gc, layer, frame);
        gc.setEffect(null);
        gc.restore();
        logger.debug("Applied direct blur (no bounds) for layer {}: radius={}", layer.name(), blurRadius);
    }

    /**
     * Renders layer content with blur applied, clipped to specified bounds.
     *
     * @param gc            graphics context
     * @param layer         layer to render
     * @param frame         animation frame
     * @param blurRadius    blur radius
     * @param boundsWidth   clipping width
     * @param boundsHeight  clipping height
     * @param layerRenderer callback to render layer content
     */
    private void renderWithBoundedBlur(GraphicsContext gc,
                                       Layer layer,
                                       double frame,
                                       double blurRadius,
                                       double boundsWidth,
                                       double boundsHeight,
                                       LayerRenderer layerRenderer) {
        // Create offscreen buffer with padding for blur spread
        double padding = Math.ceil(blurRadius * 2);
        double offscreenWidth = boundsWidth + padding * 2;
        double offscreenHeight = boundsHeight + padding * 2;
        logger.debug("Creating offscreen buffer: {}x{} with padding={}", offscreenWidth, offscreenHeight, padding);

        // Render blurred content into expanded offscreen buffer to allow blur to spread
        WritableImage blurredImage = OffscreenRenderer.renderToImage(offscreenWidth, offscreenHeight, offscreenGc -> {
            offscreenGc.save();
            // Translate to account for padding
            offscreenGc.translate(padding, padding);
            applyBlurEffect(offscreenGc, blurRadius);
            layerRenderer.render(offscreenGc, layer, frame);
            offscreenGc.setEffect(null);
            offscreenGc.restore();
        });

        // Draw only the central (unpadded) region back to destination bounds.
        // This enforces cropping even when gc.clip interactions are inconsistent.
        gc.save();
        gc.drawImage(blurredImage,
                padding, padding, boundsWidth, boundsHeight,
                0, 0, boundsWidth, boundsHeight);
        gc.restore();

        logger.debug("Applied bounded blur crop for layer {}: radius={} src=({}, {}, {}, {}) dst=(0, 0, {}, {})",
                layer.name(), blurRadius, padding, padding, boundsWidth, boundsHeight, boundsWidth, boundsHeight);
    }

    /**
     * Applies the appropriate blur effect based on radius magnitude.
     * Uses BoxBlur for large radii and GaussianBlur for smaller ones.
     *
     * @param gc         graphics context
     * @param blurRadius blur radius
     */
    private void applyBlurEffect(GraphicsContext gc, double blurRadius) {
        if (blurRadius > 40) {
            gc.setEffect(new BoxBlur(blurRadius, blurRadius, 3));
        } else if (blurRadius > 20) {
            gc.setEffect(new BoxBlur(blurRadius, blurRadius, 2));
        } else {
            gc.setEffect(new GaussianBlur(blurRadius));
        }
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
