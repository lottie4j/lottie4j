package com.lottie4j.fxplayer.renderer.layer;

import com.lottie4j.core.model.AnimatedValueType;
import com.lottie4j.core.model.Layer;
import com.lottie4j.core.model.shape.grouping.Transform;
import javafx.scene.canvas.GraphicsContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Applies transform operations from Lottie layers and shape groups to JavaFX graphics context.
 * Handles translation, rotation, scaling, anchors, and opacity transforms with proper nesting support.
 */
public class TransformApplier {

    private static final Logger logger = LoggerFactory.getLogger(TransformApplier.class);

    /**
     * Creates a new TransformApplier.
     */
    public TransformApplier() {
    }

    /**
     * Applies the full transform stack for a layer, including animated opacity.
     *
     * @param gc    graphics context to transform
     * @param layer layer providing transform values
     * @param frame animation frame to sample
     */
    public void applyLayerTransform(GraphicsContext gc, Layer layer, double frame) {
        applyLayerTransformInternal(gc, layer, frame, true);
    }

    /**
     * Applies layer transforms without inheriting opacity, used for parent-transform propagation.
     *
     * @param gc    graphics context to transform
     * @param layer layer providing transform values
     * @param frame animation frame to sample
     */
    public void applyLayerTransformWithoutOpacity(GraphicsContext gc, Layer layer, double frame) {
        applyLayerTransformInternal(gc, layer, frame, false);
    }

    /**
     * Internal layer transform applier that optionally includes opacity.
     *
     * @param gc             graphics context to transform
     * @param layer          layer providing transform values
     * @param frame          animation frame to sample
     * @param includeOpacity whether opacity should be multiplied into global alpha
     */
    private void applyLayerTransformInternal(GraphicsContext gc, Layer layer, double frame, boolean includeOpacity) {
        if (layer.transform() == null) {
            logger.debug("No transform for layer: {}", layer.name());
            return;
        }

        if (includeOpacity && layer.transform().opacity() != null) {
            double opacity = layer.transform().opacity().getValue(0, frame);
            logger.debug("Setting layer opacity: {} (normalized: {})", opacity, (opacity / 100.0));
            if (opacity > 0) {
                gc.setGlobalAlpha(gc.getGlobalAlpha() * (opacity / 100.0));
            }
        } else if (!includeOpacity) {
            logger.debug("Skipping opacity transform inheritance for parent layer: {}", layer.name());
        } else {
            logger.debug("No opacity transform");
        }

        if (layer.transform().position() != null) {
            double x = layer.transform().position().getValue(AnimatedValueType.X, frame);
            double y = layer.transform().position().getValue(AnimatedValueType.Y, frame);

            if (includeOpacity && (Double.isNaN(x) || Double.isNaN(y))) {
                logger.warn("Position contains NaN at frame {} for layer {} - trying fallback frame {}",
                        frame, layer.name(), (frame + 0.001));
                double fallbackX = layer.transform().position().getValue(AnimatedValueType.X, frame + 0.001);
                double fallbackY = layer.transform().position().getValue(AnimatedValueType.Y, frame + 0.001);

                if (!Double.isNaN(fallbackX) && !Double.isNaN(fallbackY)) {
                    x = fallbackX;
                    y = fallbackY;
                    logger.warn("Using fallback position: ({}, {})", x, y);
                } else {
                    logger.warn("Fallback also returned NaN - skipping layer rendering");
                    gc.restore();
                    return;
                }
            }

            if (includeOpacity) {
                logger.debug("Translating by position: {}, {}", x, y);
                if (Math.abs(x) > 10000 || Math.abs(y) > 10000) {
                    logger.warn("WARNING: Extreme translation values detected for layer {} at frame {}! x={}, y={} - layer may be off-screen",
                            layer.name(), frame, x, y);
                }
            } else {
                logger.debug("Translating by position (without opacity): {}, {}", x, y);
                if (Math.abs(x) > 1000 || Math.abs(y) > 1000) {
                    logger.warn("WARNING: Large translation values detected! x={}, y={}", x, y);
                }
            }
            gc.translate(x, y);
        } else {
            logger.debug("No position transform");
        }

        if (layer.transform().rotation() != null) {
            double rotationDegrees = layer.transform().rotation().getValue(0, frame);
            if (includeOpacity) {
                logger.debug("Rotating by: {} degrees", rotationDegrees);
            } else {
                logger.debug("Rotating by (without opacity): {} degrees", rotationDegrees);
            }
            gc.rotate(rotationDegrees);
        } else {
            logger.debug("No rotation transform");
        }

        double rx3DScale = 1.0;
        double ry3DScale = 1.0;

        if (layer.transform().rx() != null) {
            double rxDegrees = layer.transform().rx().getValue(0, frame);
            rx3DScale = Math.cos(Math.toRadians(rxDegrees));
            if (includeOpacity) {
                logger.debug("Applying 3D X-axis rotation: {} degrees (scaleY factor: {})", rxDegrees, rx3DScale);
            } else {
                logger.debug("Applying 3D X-axis rotation (without opacity): {} degrees (scaleY factor: {})", rxDegrees, rx3DScale);
            }
        }

        if (layer.transform().ry() != null) {
            double ryDegrees = layer.transform().ry().getValue(0, frame);
            ry3DScale = Math.cos(Math.toRadians(ryDegrees));
            if (includeOpacity) {
                logger.debug("Applying 3D Y-axis rotation: {} degrees (scaleX factor: {})", ryDegrees, ry3DScale);
            } else {
                logger.debug("Applying 3D Y-axis rotation (without opacity): {} degrees (scaleX factor: {})", ryDegrees, ry3DScale);
            }
        }

        if (layer.transform().scale() != null) {
            double scaleX = layer.transform().scale().getValue(AnimatedValueType.X, frame) / 100.0;
            double scaleY = layer.transform().scale().getValue(AnimatedValueType.Y, frame) / 100.0;

            scaleX *= ry3DScale;
            scaleY *= rx3DScale;

            if (includeOpacity) {
                logger.debug("Scaling by: {},{}", scaleX, scaleY);
            } else {
                logger.debug("Scaling by (without opacity): {},{}", scaleX, scaleY);
            }

            if (scaleX <= 0 || scaleY <= 0) {
                logger.debug("WARNING: Zero or negative scale detected! scaleX={}, scaleY={}", scaleX, scaleY);
            }

            gc.scale(scaleX, scaleY);
        } else {
            logger.debug("No scale transform");
        }

        if (layer.transform().anchor() != null) {
            double anchorX = layer.transform().anchor().getValue(AnimatedValueType.X, frame);
            double anchorY = layer.transform().anchor().getValue(AnimatedValueType.Y, frame);
            if (includeOpacity) {
                logger.debug("Layer anchor: {},{}", anchorX, anchorY);
            } else {
                logger.debug("Layer anchor (without opacity): {},{}", anchorX, anchorY);
            }
            gc.translate(-anchorX, -anchorY);
        }


        if (includeOpacity) {
            logger.debug("=== LAYER TRANSFORM APPLIED ===");
        } else {
            logger.debug("=== LAYER TRANSFORM APPLIED (WITHOUT OPACITY) ===");
        }
    }

    /**
     * Applies a shape-group transform block to the graphics context.
     *
     * @param gc        graphics context to transform
     * @param transform transform block from a Lottie group
     * @param frame     animation frame to sample
     */
    public void applyGroupTransform(GraphicsContext gc, Transform transform, double frame) {
        logger.debug("Applying group transform");

        if (transform.opacity() != null) {
            double opacityValue = transform.opacity().getValue(0, frame);
            double opacity = opacityValue / 100.0;
            logger.debug("Group opacity raw: {}, normalized: {}", opacityValue, opacity);
            if (opacity > 0) {
                gc.setGlobalAlpha(gc.getGlobalAlpha() * opacity);
            }
        }

        if (transform.position() != null) {
            double x = transform.position().getValue(AnimatedValueType.X, frame);
            double y = transform.position().getValue(AnimatedValueType.Y, frame);
            logger.debug("Group translation: {},{}", x, y);
            gc.translate(x, y);
        }

        if (transform.rotation() != null) {
            double rotationDegrees = transform.rotation().getValue(0, frame);
            logger.debug("Group rotation: {} degrees", rotationDegrees);
            gc.rotate(rotationDegrees);
        }

        if (transform.scale() != null) {
            double scaleX = transform.scale().getValue(AnimatedValueType.X, frame) / 100.0;
            double scaleY = transform.scale().getValue(AnimatedValueType.Y, frame) / 100.0;
            logger.debug("Group scale: {},{}", scaleX, scaleY);
            gc.scale(scaleX, scaleY);
        }

        if (transform.anchor() != null) {
            double anchorX = transform.anchor().getValue(AnimatedValueType.X, frame);
            double anchorY = transform.anchor().getValue(AnimatedValueType.Y, frame);
            logger.debug("Group anchor: {},{}", anchorX, anchorY);
            gc.translate(-anchorX, -anchorY);
        }
    }
}
