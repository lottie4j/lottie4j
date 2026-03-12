package com.lottie4j.fxplayer.renderer.layer;

import com.lottie4j.core.model.AnimatedValueType;
import com.lottie4j.core.model.Layer;
import com.lottie4j.core.model.shape.grouping.Transform;
import javafx.scene.canvas.GraphicsContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Applies Lottie layer and shape-group transform data to a JavaFX {@link GraphicsContext}.
 *
 * <p>The applier samples animated transform properties for a specific frame and mutates the provided graphics
 * context in Lottie's expected order: opacity, position, rotation, scale, then anchor compensation.
 * Separate entry points are provided for cases where opacity should be inherited and cases where callers need
 * only geometric transforms, such as parent propagation or off-screen intermediate rendering.
 *
 * <p>All methods operate on the caller's current graphics state. Callers are expected to bracket usage with
 * {@link GraphicsContext#save()} and {@link GraphicsContext#restore()} when they need isolation.
 */
public class TransformApplier {

    private static final Logger logger = LoggerFactory.getLogger(TransformApplier.class);

    /**
     * Creates a new transform applier.
     *
     * <p>The class is stateless and safe to reuse across frames and render passes.
     */
    public TransformApplier() {
        // Constructor for TransformApplier
    }

    /**
     * Applies a layer's sampled transform stack, including opacity, to the current graphics state.
     *
     * <p>This method multiplies the layer opacity into {@link GraphicsContext#getGlobalAlpha()} and then applies
     * translation, rotation, scale, and anchor offset using the values sampled at {@code frame}. The resulting
     * transform affects all subsequent drawing until the caller restores the previous state.
     *
     * @param gc    graphics context to mutate
     * @param layer layer whose transform block should be sampled; if the layer has no transform, no changes are made
     * @param frame animation frame to sample from animated transform values
     */
    public void applyLayerTransform(GraphicsContext gc, Layer layer, double frame) {
        applyLayerTransformInternal(gc, layer, frame, true);
    }

    /**
     * Applies only the geometric portion of a layer transform, omitting opacity multiplication.
     *
     * <p>This is used when a parent layer's transform must affect descendants without also duplicating its alpha,
     * or when opacity is handled later at a different compositing stage.
     *
     * @param gc    graphics context to mutate
     * @param layer layer whose transform block should be sampled
     * @param frame animation frame to sample from animated transform values
     */
    public void applyLayerTransformWithoutOpacity(GraphicsContext gc, Layer layer, double frame) {
        applyLayerTransformInternal(gc, layer, frame, false);
    }

    /**
     * Internal implementation for applying layer transforms with optional opacity inheritance.
     *
     * <p>The transform order matches the renderer's layer semantics: opacity first, then position,
     * rotation, simulated 3D tilt adjustments via scale factors, 2D scale, and finally anchor translation.
     * When sampled position values contain {@code NaN}, the method attempts a small positive frame offset as
     * a fallback before aborting the operation.
     *
     * @param gc             graphics context to mutate in place
     * @param layer          layer providing sampled transform values
     * @param frame          animation frame to sample
     * @param includeOpacity whether the sampled opacity should be multiplied into the current global alpha
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
     * <p>Group transforms are sampled from the supplied {@link Transform} object and applied in the same basic
     * order as layer transforms. The method updates the graphics context in place and does not save or restore
     * state on the caller's behalf.
     *
     * @param gc        graphics context to mutate
     * @param transform transform block from a Lottie shape group
     * @param frame     animation frame to sample from animated transform values
     */
    public void applyGroupTransform(GraphicsContext gc, Transform transform, double frame) {
        applyGroupTransformInternal(gc, transform, frame, true);
    }

    /**
     * Applies a shape-group transform block without multiplying its opacity into the graphics context.
     *
     * <p>This is useful when a group is being rendered into an intermediate buffer and the final alpha will be
     * applied during a later composite step.
     *
     * @param gc        graphics context to mutate
     * @param transform transform block from a Lottie shape group
     * @param frame     animation frame to sample from animated transform values
     */
    public void applyGroupTransformWithoutOpacity(GraphicsContext gc, Transform transform, double frame) {
        applyGroupTransformInternal(gc, transform, frame, false);
    }

    /**
     * Internal implementation for applying a group transform with optional opacity multiplication.
     *
     * <p>The method mutates {@code gc} in place by applying sampled opacity, translation, rotation, scale,
     * and anchor compensation in sequence. A {@code null} property inside {@code transform} is treated as
     * absent and simply skipped.
     *
     * @param gc             graphics context to mutate
     * @param transform      shape-group transform block to sample
     * @param frame          animation frame to sample
     * @param includeOpacity whether the group's opacity should be multiplied into the current global alpha
     */
    private void applyGroupTransformInternal(GraphicsContext gc, Transform transform, double frame, boolean includeOpacity) {
        logger.debug("Applying group transform{}",
                includeOpacity ? "" : " (without opacity)");

        if (transform.opacity() != null) {
            double opacityValue = transform.opacity().getValue(0, frame);
            double opacity = opacityValue / 100.0;
            logger.debug("Group opacity raw: {}, normalized: {}", opacityValue, opacity);
            if (opacity > 0 && includeOpacity) {
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
