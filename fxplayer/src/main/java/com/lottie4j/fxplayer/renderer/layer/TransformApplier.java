package com.lottie4j.fxplayer.renderer.layer;

import com.lottie4j.core.model.AnimatedValueType;
import com.lottie4j.core.model.Layer;
import com.lottie4j.core.model.shape.grouping.Transform;
import javafx.scene.canvas.GraphicsContext;

import java.util.logging.Logger;

public class TransformApplier {

    private static final Logger logger = Logger.getLogger(TransformApplier.class.getName());

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
            logger.finer("No transform for layer: " + layer.name());
            return;
        }

        if (includeOpacity && layer.transform().opacity() != null) {
            double opacity = layer.transform().opacity().getValue(0, frame);
            logger.finer("Setting layer opacity: " + opacity + " (normalized: " + (opacity / 100.0) + ")");
            if (opacity > 0) {
                gc.setGlobalAlpha(gc.getGlobalAlpha() * (opacity / 100.0));
            }
        } else if (!includeOpacity) {
            logger.finer("Skipping opacity transform inheritance for parent layer: " + layer.name());
        } else {
            logger.finer("No opacity transform");
        }

        if (layer.transform().position() != null) {
            double x = layer.transform().position().getValue(AnimatedValueType.X, frame);
            double y = layer.transform().position().getValue(AnimatedValueType.Y, frame);

            if (includeOpacity && (Double.isNaN(x) || Double.isNaN(y))) {
                logger.warning("Position contains NaN at frame " + frame + " for layer " + layer.name()
                        + " - trying fallback frame " + (frame + 0.001));
                double fallbackX = layer.transform().position().getValue(AnimatedValueType.X, frame + 0.001);
                double fallbackY = layer.transform().position().getValue(AnimatedValueType.Y, frame + 0.001);

                if (!Double.isNaN(fallbackX) && !Double.isNaN(fallbackY)) {
                    x = fallbackX;
                    y = fallbackY;
                    logger.warning("Using fallback position: (" + x + ", " + y + ")");
                } else {
                    logger.warning("Fallback also returned NaN - skipping layer rendering");
                    gc.restore();
                    return;
                }
            }

            if (includeOpacity) {
                logger.finer("Translating by position: " + x + ", " + y);
                if (Math.abs(x) > 10000 || Math.abs(y) > 10000) {
                    logger.warning("WARNING: Extreme translation values detected for layer " + layer.name() + " at frame " + frame
                            + "! x=" + x + ", y=" + y + " - layer may be off-screen");
                }
            } else {
                logger.finer("Translating by position (without opacity): " + x + ", " + y);
                if (Math.abs(x) > 1000 || Math.abs(y) > 1000) {
                    logger.warning("WARNING: Large translation values detected! x=" + x + ", y=" + y);
                }
            }
            gc.translate(x, y);
        } else {
            logger.finer("No position transform");
        }

        if (layer.transform().rotation() != null) {
            double rotationDegrees = layer.transform().rotation().getValue(0, frame);
            if (includeOpacity) {
                logger.finer("Rotating by: " + rotationDegrees + " degrees");
            } else {
                logger.finer("Rotating by (without opacity): " + rotationDegrees + " degrees");
            }
            gc.rotate(rotationDegrees);
        } else {
            logger.finer("No rotation transform");
        }

        double rx3DScale = 1.0;
        double ry3DScale = 1.0;

        if (layer.transform().rx() != null) {
            double rxDegrees = layer.transform().rx().getValue(0, frame);
            rx3DScale = Math.cos(Math.toRadians(rxDegrees));
            if (includeOpacity) {
                logger.finer("Applying 3D X-axis rotation: " + rxDegrees + " degrees (scaleY factor: " + rx3DScale + ")");
            } else {
                logger.finer("Applying 3D X-axis rotation (without opacity): " + rxDegrees + " degrees (scaleY factor: " + rx3DScale + ")");
            }
        }

        if (layer.transform().ry() != null) {
            double ryDegrees = layer.transform().ry().getValue(0, frame);
            ry3DScale = Math.cos(Math.toRadians(ryDegrees));
            if (includeOpacity) {
                logger.finer("Applying 3D Y-axis rotation: " + ryDegrees + " degrees (scaleX factor: " + ry3DScale + ")");
            } else {
                logger.finer("Applying 3D Y-axis rotation (without opacity): " + ryDegrees + " degrees (scaleX factor: " + ry3DScale + ")");
            }
        }

        if (layer.transform().scale() != null) {
            double scaleX = layer.transform().scale().getValue(AnimatedValueType.X, frame) / 100.0;
            double scaleY = layer.transform().scale().getValue(AnimatedValueType.Y, frame) / 100.0;

            scaleX *= ry3DScale;
            scaleY *= rx3DScale;

            if (includeOpacity) {
                logger.finer("Scaling by: " + scaleX + ", " + scaleY);
            } else {
                logger.finer("Scaling by (without opacity): " + scaleX + ", " + scaleY);
            }

            if (scaleX <= 0 || scaleY <= 0) {
                logger.warning("WARNING: Zero or negative scale detected! scaleX=" + scaleX + ", scaleY=" + scaleY);
            }

            gc.scale(scaleX, scaleY);
        } else {
            logger.finer("No scale transform");
        }

        if (layer.transform().anchor() != null) {
            double anchorX = layer.transform().anchor().getValue(AnimatedValueType.X, frame);
            double anchorY = layer.transform().anchor().getValue(AnimatedValueType.Y, frame);
            if (includeOpacity) {
                logger.finer("Layer anchor: " + anchorX + ", " + anchorY);
            } else {
                logger.finer("Layer anchor (without opacity): " + anchorX + ", " + anchorY);
            }
            gc.translate(-anchorX, -anchorY);
        }

        if (includeOpacity) {
            logger.finer("=== LAYER TRANSFORM APPLIED ===");
        } else {
            logger.finer("=== LAYER TRANSFORM APPLIED (WITHOUT OPACITY) ===");
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
        logger.finer("Applying group transform");

        if (transform.opacity() != null) {
            double opacityValue = transform.opacity().getValue(0, frame);
            double opacity = opacityValue / 100.0;
            logger.finer("Group opacity raw: " + opacityValue + ", normalized: " + opacity);
            if (opacity > 0) {
                gc.setGlobalAlpha(gc.getGlobalAlpha() * opacity);
            }
        }

        if (transform.position() != null) {
            double x = transform.position().getValue(AnimatedValueType.X, frame);
            double y = transform.position().getValue(AnimatedValueType.Y, frame);
            logger.finer("Group translation: " + x + ", " + y);
            gc.translate(x, y);
        }

        if (transform.rotation() != null) {
            double rotationDegrees = transform.rotation().getValue(0, frame);
            logger.finer("Group rotation: " + rotationDegrees + " degrees");
            gc.rotate(rotationDegrees);
        }

        if (transform.scale() != null) {
            double scaleX = transform.scale().getValue(AnimatedValueType.X, frame) / 100.0;
            double scaleY = transform.scale().getValue(AnimatedValueType.Y, frame) / 100.0;
            logger.finer("Group scale: " + scaleX + ", " + scaleY);
            gc.scale(scaleX, scaleY);
        }

        if (transform.anchor() != null) {
            double anchorX = transform.anchor().getValue(AnimatedValueType.X, frame);
            double anchorY = transform.anchor().getValue(AnimatedValueType.Y, frame);
            logger.finer("Group anchor: " + anchorX + ", " + anchorY);
            gc.translate(-anchorX, -anchorY);
        }
    }
}
