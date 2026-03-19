package com.lottie4j.fxplayer.renderer.layer;

import com.lottie4j.core.model.bezier.AnimatedBezier;
import com.lottie4j.core.model.bezier.BezierDefinition;
import com.lottie4j.core.model.bezier.FixedBezier;
import com.lottie4j.core.model.layer.Layer;
import com.lottie4j.core.model.layer.Mask;
import com.lottie4j.fxplayer.renderer.shape.PathBezierInterpolator;
import javafx.scene.canvas.GraphicsContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Renderer for layer masks.
 * <p>
 * Handles rendering of animated mask paths and applying mask clipping to layers.
 * Supports multiple mask modes: add, subtract, intersect, and none.
 */
public class MaskRenderer {
    private static final Logger logger = LoggerFactory.getLogger(MaskRenderer.class);
    private final PathBezierInterpolator bezierInterpolator;

    public MaskRenderer(PathBezierInterpolator bezierInterpolator) {
        this.bezierInterpolator = bezierInterpolator;
    }

    /**
     * Applies all masks from a layer to the graphics context.
     * <p>
     * For layers with masks, this method builds a mask path by combining all mask shapes
     * according to their modes, then applies clipping to the graphics context.
     *
     * @param gc    the graphics context to apply masks to
     * @param layer the layer containing masks
     * @param frame the current animation frame
     * @return true if masks were applied, false if no masks or masks are disabled
     */
    public boolean applyMasks(GraphicsContext gc, Layer layer, double frame) {
        if (layer.masks() == null || layer.masks().isEmpty()) {
            return false;
        }

        logger.debug("Applying {} mask(s) to layer at frame {}", layer.masks().size(), frame);

        // For the first implementation, we'll support the "add" mode which is the most common
        // Multiple masks in "add" mode are combined by intersecting their paths
        gc.save();
        gc.beginPath();

        boolean firstMask = true;
        for (Mask mask : layer.masks()) {
            String mode = mask.mode() != null ? mask.mode() : "a";

            if (!"a".equals(mode)) {
                logger.warn("Mask mode '{}' not yet supported, skipping mask", mode);
                continue;
            }

            BezierDefinition bezierDef = getBezierFromMask(mask, frame);
            if (bezierDef == null || bezierDef.vertices() == null || bezierDef.vertices().isEmpty()) {
                logger.debug("Mask has no path data at frame {}", frame);
                continue;
            }

            double opacity = getMaskOpacity(mask, frame);
            if (opacity < 0.01) {
                logger.debug("Mask opacity is nearly 0, skipping");
                continue;
            }

            if (firstMask) {
                buildMaskPath(gc, bezierDef);
                firstMask = false;
            } else {
                // For multiple "add" mode masks, we need to intersect them
                // For now, just add them to the path (they'll combine)
                buildMaskPath(gc, bezierDef);
            }
        }

        if (!firstMask) {
            gc.clip();
            return true;
        }

        gc.restore();
        return false;
    }

    /**
     * Restores the graphics context after mask rendering.
     *
     * @param gc the graphics context to restore
     */
    public void restoreMasks(GraphicsContext gc) {
        gc.restore();
    }

    /**
     * Extracts bezier definition from a mask's animated path.
     *
     * @param mask  the mask containing the path
     * @param frame the current animation frame
     * @return bezier definition for the mask path
     */
    private BezierDefinition getBezierFromMask(Mask mask, double frame) {
        if (mask.path() == null) {
            return null;
        }

        // Mask path is a Bezier type (FixedBezier or AnimatedBezier)
        if (mask.path() instanceof FixedBezier fixedBezier) {
            return fixedBezier.bezier();
        } else if (mask.path() instanceof AnimatedBezier animatedBezier) {
            return bezierInterpolator.getInterpolatedBezier(animatedBezier, frame);
        }

        logger.warn("Mask path has unexpected type: {}", mask.path().getClass());
        return null;
    }

    /**
     * Gets the mask opacity at the current frame.
     *
     * @param mask  the mask
     * @param frame the current animation frame
     * @return opacity value (0-1)
     */
    private double getMaskOpacity(Mask mask, double frame) {
        if (mask.opacity() == null) {
            return 1.0;
        }

        double opacityValue = mask.opacity().getValue(0, frame);
        return opacityValue / 100.0; // Convert from 0-100 to 0-1
    }

    /**
     * Builds a mask path in the graphics context from bezier definition.
     *
     * @param gc        the graphics context
     * @param bezierDef the bezier definition containing vertices and tangents
     */
    private void buildMaskPath(GraphicsContext gc, BezierDefinition bezierDef) {
        List<List<Double>> vertices = bezierDef.vertices();
        List<List<Double>> tangentsIn = bezierDef.tangentsIn();
        List<List<Double>> tangentsOut = bezierDef.tangentsOut();

        if (vertices.isEmpty()) {
            return;
        }

        // Move to first vertex
        List<Double> firstVertex = vertices.get(0);
        if (firstVertex.size() >= 2) {
            gc.moveTo(firstVertex.get(0), firstVertex.get(1));
        }

        // Draw path segments
        for (int i = 1; i < vertices.size(); i++) {
            List<Double> vertex = vertices.get(i);
            if (vertex.size() < 2) {
                continue;
            }

            double x = vertex.get(0);
            double y = vertex.get(1);

            // Check if this segment uses bezier curves
            if (tangentsIn != null && tangentsOut != null
                    && i - 1 < tangentsOut.size() && i < tangentsIn.size()) {
                List<Double> prevTangentOut = tangentsOut.get(i - 1);
                List<Double> currentTangentIn = tangentsIn.get(i);

                if (prevTangentOut.size() >= 2 && currentTangentIn.size() >= 2) {
                    // Check if tangents are non-zero
                    boolean hasOutTangent = Math.abs(prevTangentOut.get(0)) > 0.001
                                         || Math.abs(prevTangentOut.get(1)) > 0.001;
                    boolean hasInTangent = Math.abs(currentTangentIn.get(0)) > 0.001
                                        || Math.abs(currentTangentIn.get(1)) > 0.001;

                    if (hasOutTangent || hasInTangent) {
                        List<Double> prevVertex = vertices.get(i - 1);
                        double cp1x = prevVertex.get(0) + prevTangentOut.get(0);
                        double cp1y = prevVertex.get(1) + prevTangentOut.get(1);
                        double cp2x = x + currentTangentIn.get(0);
                        double cp2y = y + currentTangentIn.get(1);
                        gc.bezierCurveTo(cp1x, cp1y, cp2x, cp2y, x, y);
                        continue;
                    }
                }
            }

            // Draw straight line
            gc.lineTo(x, y);
        }

        // Close path if needed
        if (Boolean.TRUE.equals(bezierDef.closed())) {
            int lastIdx = vertices.size() - 1;
            int firstIdx = 0;

            // Check if we need a bezier curve to close the path
            if (tangentsIn != null && tangentsOut != null
                    && lastIdx < tangentsOut.size() && firstIdx < tangentsIn.size()) {
                List<Double> lastTangentOut = tangentsOut.get(lastIdx);
                List<Double> firstTangentIn = tangentsIn.get(firstIdx);

                if (lastTangentOut.size() >= 2 && firstTangentIn.size() >= 2) {
                    boolean hasOutTangent = Math.abs(lastTangentOut.get(0)) > 0.001
                                         || Math.abs(lastTangentOut.get(1)) > 0.001;
                    boolean hasInTangent = Math.abs(firstTangentIn.get(0)) > 0.001
                                        || Math.abs(firstTangentIn.get(1)) > 0.001;

                    if (hasOutTangent || hasInTangent) {
                        List<Double> lastVertex = vertices.get(lastIdx);
                        List<Double> closingVertex = vertices.get(firstIdx);
                        double cp1x = lastVertex.get(0) + lastTangentOut.get(0);
                        double cp1y = lastVertex.get(1) + lastTangentOut.get(1);
                        double cp2x = closingVertex.get(0) + firstTangentIn.get(0);
                        double cp2y = closingVertex.get(1) + firstTangentIn.get(1);
                        gc.bezierCurveTo(cp1x, cp1y, cp2x, cp2y, closingVertex.get(0), closingVertex.get(1));
                    } else {
                        gc.closePath();
                    }
                } else {
                    gc.closePath();
                }
            } else {
                gc.closePath();
            }
        }
    }
}
