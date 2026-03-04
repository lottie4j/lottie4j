package com.lottie4j.fxplayer.renderer.shape;

import com.lottie4j.core.definition.FillRule;
import com.lottie4j.core.definition.ShapeGroup;
import com.lottie4j.core.model.bezier.AnimatedBezier;
import com.lottie4j.core.model.bezier.BezierDefinition;
import com.lottie4j.core.model.bezier.FixedBezier;
import com.lottie4j.core.model.shape.BaseShape;
import com.lottie4j.core.model.shape.grouping.Group;
import com.lottie4j.core.model.shape.grouping.Transform;
import com.lottie4j.core.model.shape.modifier.TrimPath;
import com.lottie4j.core.model.shape.shape.Path;
import com.lottie4j.core.model.shape.style.Fill;
import com.lottie4j.fxplayer.element.FillStyle;
import com.lottie4j.fxplayer.renderer.layer.TransformApplier;
import javafx.scene.canvas.GraphicsContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Renderer for shape groups with support for transforms, trim paths, and combined path rendering.
 * Handles complex group hierarchies and fill rule application.
 */
public class ShapeGroupRenderer {

    private static final Logger logger = LoggerFactory.getLogger(ShapeGroupRenderer.class);

    private final TransformApplier transformApplier;
    private final ShapeRendererFactory shapeRendererFactory;

    /**
     * Creates a shape group renderer.
     *
     * @param transformApplier     transform applier for group transforms
     * @param shapeRendererFactory factory for obtaining shape renderers
     */
    public ShapeGroupRenderer(TransformApplier transformApplier, ShapeRendererFactory shapeRendererFactory) {
        this.transformApplier = transformApplier;
        this.shapeRendererFactory = shapeRendererFactory;
    }

    /**
     * Renders a shape group with transforms and modifiers applied.
     *
     * @param gc            graphics context
     * @param shape         group shape to render
     * @param frame         animation frame
     * @param layerTrimPath optional trim path from layer level
     */
    public void renderShapeTypeGroup(GraphicsContext gc, BaseShape shape, double frame, TrimPath layerTrimPath) {
        if (shape instanceof Transform) {
            logger.debug("Don't know how to render a Transform group yet (TODO)");
            return;
        }
        if (shape instanceof Group group) {
            logger.debug("Rendering group: {} with {} items", group.name(), group.shapes().size());
            gc.save();

            // Extract Transform and TrimPath from the group's shapes
            Transform groupTransform = null;
            TrimPath groupTrimPath = null;
            for (BaseShape item : group.shapes()) {
                if (item instanceof Transform transform) {
                    groupTransform = transform;
                } else if (item instanceof TrimPath trim) {
                    groupTrimPath = trim;
                }
            }

            // Check group opacity - skip rendering if transparent
            if (groupTransform != null) {
                if (groupTransform.opacity() != null) {
                    double opacity = groupTransform.opacity().getValue(0, frame);
                    if (opacity <= 0) {
                        logger.debug("Skipping group {} - opacity is {}", group.name(), opacity);
                        gc.restore();
                        return;
                    }
                }
                transformApplier.applyGroupTransform(gc, groupTransform, frame);
            }

            // Use group-level TrimPath if present, otherwise use layer-level TrimPath
            TrimPath effectiveTrimPath = groupTrimPath != null ? groupTrimPath : layerTrimPath;

            // Create a synthetic group that includes the effective trim path for renderers
            Group effectiveGroup = createGroupWithTrimPath(group, effectiveTrimPath);

            // Check if this group has multiple Path shapes with a single Fill (needs combined rendering)
            boolean hasMultiplePaths = renderGroupWithCombinedPaths(gc, group, effectiveGroup, frame, effectiveTrimPath);

            if (!hasMultiplePaths) {
                // Render all non-transform/non-modifier shapes in reverse order
                // Lottie renders shapes bottom-to-top (last in array is drawn first, appears behind)
                for (int i = group.shapes().size() - 1; i >= 0; i--) {
                    BaseShape item = group.shapes().get(i);
                    if (item instanceof Transform || item instanceof TrimPath) {
                        continue; // Skip modifiers, they're applied to the shapes
                    }

                    switch (item.shapeType().shapeGroup()) {
                        case GROUP -> renderShapeTypeGroup(gc, item, frame, effectiveTrimPath);
                        case SHAPE -> renderShape(gc, item, effectiveGroup, frame);
                        case STYLE -> {
                            // Skip - styles (Fill, Stroke, etc.) are handled within groups
                        }
                        default ->
                                logger.warn("Not defined how to render shape type: {}", item.shapeType().shapeGroup());
                    }
                }
            }

            gc.restore();
        }
    }

    /**
     * Creates a group with trim path added to its shapes.
     *
     * @param original original group
     * @param trimPath trim path to add
     * @return new group with trim path, or original if trimPath is null
     */
    private Group createGroupWithTrimPath(Group original, TrimPath trimPath) {
        if (trimPath == null) {
            return original;
        }
        // Create a new group that includes the TrimPath in its shapes list
        List<BaseShape> newShapes = new ArrayList<>(original.shapes());
        if (!newShapes.contains(trimPath)) {
            newShapes.add(trimPath);
        }
        return new Group(
                original.name(),
                original.matchName(),
                original.hidden(),
                original.blendMode(),
                original.index(),
                original.clazz(),
                original.id(),
                original.d(),
                original.cix(),
                original.numberOfProperties(),
                newShapes
        );
    }

    /**
     * Handle groups with multiple Path shapes that share a single Fill.
     * These need to be rendered together with a fill rule to create holes/rings.
     * Returns true if the group was rendered as combined paths, false otherwise.
     *
     * @param gc                graphics context
     * @param group             original group
     * @param effectiveGroup    group with effective trim path
     * @param frame             animation frame
     * @param effectiveTrimPath effective trim path
     * @return true if rendered as combined paths, false otherwise
     */
    private boolean renderGroupWithCombinedPaths(GraphicsContext gc, Group group, Group effectiveGroup, double frame, TrimPath effectiveTrimPath) {
        // Count Path shapes and check for Fill
        List<Path> paths = new ArrayList<>();
        Fill fill = null;

        for (BaseShape item : group.shapes()) {
            if (item instanceof Path path) {
                paths.add(path);
            } else if (item instanceof Fill f) {
                fill = f;
            }
        }

        // Only use combined rendering if we have multiple paths with a fill
        if (paths.size() < 2 || fill == null) {
            return false;
        }

        logger.debug("Rendering {} combined paths with fill rule for group: {}", paths.size(), group.name());

        // Set fill rule
        javafx.scene.shape.FillRule fxFillRule = fill.fillRule() == FillRule.EVEN_ODD ?
                javafx.scene.shape.FillRule.EVEN_ODD : javafx.scene.shape.FillRule.NON_ZERO;

        gc.save();
        gc.setFillRule(fxFillRule);
        gc.beginPath();

        // Add all paths to the canvas path in reverse order (last to first)
        for (int i = paths.size() - 1; i >= 0; i--) {
            addPathToCanvas(gc, paths.get(i), frame);
        }

        // Apply fill color and opacity
        var fillColor = new FillStyle(fill).getColor(frame);
        gc.setFill(fillColor);

        double fillOpacity = fill.opacity() != null ? fill.opacity().getValue(0, frame) / 100.0 : 1.0;
        if (fillOpacity < 1.0) {
            double currentAlpha = gc.getGlobalAlpha();
            gc.setGlobalAlpha(currentAlpha * fillOpacity);
        }

        gc.fill();
        gc.restore();

        // Render any nested groups
        for (int i = group.shapes().size() - 1; i >= 0; i--) {
            BaseShape item = group.shapes().get(i);
            if (item.shapeType().shapeGroup() == ShapeGroup.GROUP) {
                renderShapeTypeGroup(gc, item, frame, effectiveTrimPath);
            }
        }

        return true;
    }

    /**
     * Add a Path shape's bezier curves to the current canvas path.
     *
     * @param gc    graphics context
     * @param path  path shape
     * @param frame animation frame
     */
    private void addPathToCanvas(GraphicsContext gc, Path path, double frame) {
        if (path.bezier() == null) return;

        BezierDefinition bezierDef;
        if (path.bezier() instanceof FixedBezier fixedBezier) {
            bezierDef = fixedBezier.bezier();
        } else if (path.bezier() instanceof AnimatedBezier animatedBezier) {
            bezierDef = getInterpolatedBezier(animatedBezier, frame);
        } else {
            return;
        }

        if (bezierDef == null || bezierDef.vertices() == null || bezierDef.vertices().isEmpty()) return;

        List<List<Double>> vertices = bezierDef.vertices();
        List<List<Double>> tangentsIn = bezierDef.tangentsIn();
        List<List<Double>> tangentsOut = bezierDef.tangentsOut();

        boolean first = true;
        for (int i = 0; i < vertices.size(); i++) {
            List<Double> vertex = vertices.get(i);
            if (vertex.size() < 2) continue;

            double x = vertex.get(0);
            double y = vertex.get(1);

            if (first) {
                gc.moveTo(x, y);
                first = false;
            } else {
                if (tangentsIn != null && tangentsOut != null &&
                        i - 1 < tangentsOut.size() && i < tangentsIn.size()) {
                    List<Double> prevTangentOut = tangentsOut.get(i - 1);
                    List<Double> currentTangentIn = tangentsIn.get(i);

                    if (prevTangentOut.size() >= 2 && currentTangentIn.size() >= 2) {
                        List<Double> prevVertex = vertices.get(i - 1);
                        double cp1x = prevVertex.get(0) + prevTangentOut.get(0);
                        double cp1y = prevVertex.get(1) + prevTangentOut.get(1);
                        double cp2x = x + currentTangentIn.get(0);
                        double cp2y = y + currentTangentIn.get(1);
                        gc.bezierCurveTo(cp1x, cp1y, cp2x, cp2y, x, y);
                    } else {
                        gc.lineTo(x, y);
                    }
                } else {
                    gc.lineTo(x, y);
                }
            }
        }

        // Handle closing bezier curve
        if (bezierDef.closed() != null && bezierDef.closed() && vertices.size() > 1) {
            int lastIdx = vertices.size() - 1;
            if (tangentsIn != null && tangentsOut != null &&
                    lastIdx < tangentsOut.size() && 0 < tangentsIn.size()) {
                List<Double> lastTangentOut = tangentsOut.get(lastIdx);
                List<Double> firstTangentIn = tangentsIn.get(0);
                if (lastTangentOut.size() >= 2 && firstTangentIn.size() >= 2) {
                    List<Double> lastVertex = vertices.get(lastIdx);
                    List<Double> firstVertex = vertices.get(0);
                    double cp1x = lastVertex.get(0) + lastTangentOut.get(0);
                    double cp1y = lastVertex.get(1) + lastTangentOut.get(1);
                    double cp2x = firstVertex.get(0) + firstTangentIn.get(0);
                    double cp2y = firstVertex.get(1) + firstTangentIn.get(1);
                    gc.bezierCurveTo(cp1x, cp1y, cp2x, cp2y, firstVertex.get(0), firstVertex.get(1));
                    // Note: Don't call closePath() here - JavaFX will handle it
                } else {
                    gc.closePath();
                }
            } else {
                gc.closePath();
            }
        }
    }

    /**
     * Gets interpolated bezier for animated paths.
     *
     * @param animatedBezier animated bezier data
     * @param frame          animation frame
     * @return interpolated bezier definition, or null if not supported yet
     */
    private BezierDefinition getInterpolatedBezier(AnimatedBezier animatedBezier, double frame) {
        // This method should already exist in PathRenderer - we need to extract it or duplicate it
        // For now, return null and we'll need to implement it
        logger.warn("Animated bezier not yet supported in combined path rendering");
        return null;
    }

    /**
     * Renders a single shape using the appropriate renderer.
     *
     * @param gc          graphics context
     * @param shape       shape to render
     * @param parentGroup parent group for context
     * @param frame       animation frame
     */
    private void renderShape(GraphicsContext gc, BaseShape shape, Group parentGroup, double frame) {
        ShapeRenderer renderer = shapeRendererFactory.getRenderer(shape);
        if (renderer == null) {
            logger.warn("No renderer found for shape: {}", shape.getClass().getSimpleName());
            return;
        }
        renderer.render(gc, shape, parentGroup, frame);
    }
}

