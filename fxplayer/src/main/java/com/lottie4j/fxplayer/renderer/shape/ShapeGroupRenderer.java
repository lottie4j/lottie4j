package com.lottie4j.fxplayer.renderer.shape;

import com.lottie4j.core.definition.FillRule;
import com.lottie4j.core.definition.MergeMode;
import com.lottie4j.core.definition.ShapeGroup;
import com.lottie4j.core.model.AnimatedValueType;
import com.lottie4j.core.model.bezier.AnimatedBezier;
import com.lottie4j.core.model.bezier.BezierDefinition;
import com.lottie4j.core.model.bezier.FixedBezier;
import com.lottie4j.core.model.shape.BaseShape;
import com.lottie4j.core.model.shape.grouping.Group;
import com.lottie4j.core.model.shape.grouping.Transform;
import com.lottie4j.core.model.shape.modifier.Merge;
import com.lottie4j.core.model.shape.modifier.TrimPath;
import com.lottie4j.core.model.shape.shape.Ellipse;
import com.lottie4j.core.model.shape.shape.Path;
import com.lottie4j.core.model.shape.shape.Rectangle;
import com.lottie4j.core.model.shape.style.Fill;
import com.lottie4j.core.model.shape.style.GradientStroke;
import com.lottie4j.core.model.shape.style.Stroke;
import com.lottie4j.fxplayer.element.FillStyle;
import com.lottie4j.fxplayer.renderer.layer.TransformApplier;
import com.lottie4j.fxplayer.util.OffscreenRenderer;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;
import javafx.scene.transform.Affine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Renderer for shape groups with support for transforms, trim paths, and combined path rendering.
 * Handles complex group hierarchies and fill rule application.
 */
public class ShapeGroupRenderer {

    private static final Logger logger = LoggerFactory.getLogger(ShapeGroupRenderer.class);
    private static final double OFFSCREEN_BOUNDS_PADDING = 4.0;

    private final TransformApplier transformApplier;
    private final ShapeRendererFactory shapeRendererFactory;
    private final Set<String> unsupportedModifierWarnings = ConcurrentHashMap.newKeySet();
    private final PathBezierInterpolator pathBezierInterpolator = new PathBezierInterpolator();

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

            // Check if group has opacity that needs special handling
            double groupOpacity = 1.0;
            boolean hasAnimatedOpacity = false;
            if (groupTransform != null && groupTransform.opacity() != null) {
                groupOpacity = groupTransform.opacity().getValue(0, frame) / 100.0;
                if (groupOpacity <= 0) {
                    logger.debug("Skipping group {} - opacity is {}", group.name(), groupOpacity);
                    gc.restore();
                    return;
                }
                hasAnimatedOpacity = groupTransform.opacity().animated() != null && groupTransform.opacity().animated() > 0;
            }

            // Use group-level TrimPath if present, otherwise use layer-level TrimPath
            TrimPath effectiveTrimPath = groupTrimPath != null ? groupTrimPath : layerTrimPath;

            // Create a synthetic group that includes the effective trim path for renderers
            Group effectiveGroup = createGroupWithTrimPath(group, effectiveTrimPath);

            // Check if this group has multiple Path shapes with a single Fill (needs combined rendering)
            boolean hasMultiplePaths = renderGroupWithCombinedPaths(gc, group, frame, effectiveTrimPath);

            if (!hasMultiplePaths) {
                // If the group has opacity that varies per frame and contains overlapping shapes,
                // we need to render to an off-screen buffer to combine shapes properly
                if (hasAnimatedOpacity && groupOpacity < 1.0 && groupContainsOverlappingShapes(group)) {
                    logger.debug("Group {} has animated opacity with overlapping shapes - using off-screen rendering", group.name());
                    renderGroupWithOffscreenBuffer(gc, group, effectiveGroup, frame, effectiveTrimPath, groupTransform);
                } else {
                    // Standard rendering path: apply transforms directly and render shapes
                    if (groupTransform != null) {
                        transformApplier.applyGroupTransform(gc, groupTransform, frame);
                    }

                    // Render all non-transform/non-trim shapes in reverse order
                    // Lottie renders shapes bottom-to-top (last in array is drawn first, appears behind)
                    for (int i = group.shapes().size() - 1; i >= 0; i--) {
                        BaseShape item = group.shapes().get(i);
                        if (item instanceof Transform || item instanceof TrimPath) {
                            continue; // Transform is applied at group level, TrimPath is resolved via effectiveTrimPath
                        }

                        switch (item.shapeType().shapeGroup()) {
                            case GROUP -> renderShapeTypeGroup(gc, item, frame, effectiveTrimPath);
                            case SHAPE -> renderShape(gc, item, effectiveGroup, frame);
                            case STYLE -> {
                                // Skip - styles (Fill, Stroke, etc.) are handled within groups
                            }
                            case MODIFIER -> handleModifier(item);
                            default ->
                                    logger.warn("Not defined how to render shape type: {}", item.shapeType().shapeGroup());
                        }
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
     * @param frame             animation frame
     * @param effectiveTrimPath effective trim path
     * @return true if rendered as combined paths, false otherwise
     */
    private boolean renderGroupWithCombinedPaths(GraphicsContext gc, Group group, double frame, TrimPath effectiveTrimPath) {
        // Count Path shapes and check for Fill
        List<Path> paths = new ArrayList<>();
        Fill fill = null;
        Transform groupTransform = null;

        for (BaseShape item : group.shapes()) {
            if (item instanceof Path path) {
                paths.add(path);
            } else if (item instanceof Fill f) {
                fill = f;
            } else if (item instanceof Transform transform) {
                groupTransform = transform;
            }
        }

        logger.debug("GROUP: {} - found {} paths, fill={}, transform={}",
                group.name(), paths.size(), fill != null, groupTransform != null);

        if (renderGroupWithMergeModifier(gc, group, fill, groupTransform, frame, effectiveTrimPath)) {
            logger.debug("  -> used MERGE rendering");
            return true;
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

        // Apply group transform BEFORE rendering combined paths
        if (groupTransform != null) {
            transformApplier.applyGroupTransform(gc, groupTransform, frame);
        }

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

    private boolean renderGroupWithMergeModifier(GraphicsContext gc,
                                                 Group group,
                                                 Fill fill,
                                                 Transform groupTransform,
                                                 double frame,
                                                 TrimPath effectiveTrimPath) {
        Merge merge = null;
        int mergeIndex = -1;
        for (int i = 0; i < group.shapes().size(); i++) {
            BaseShape shape = group.shapes().get(i);
            if (shape instanceof Merge m) {
                merge = m;
                mergeIndex = i;
                break;
            }
        }

        if (merge == null || fill == null || mergeIndex < 1) {
            logger.debug("MERGE SKIP - merge={}, fill={}, mergeIndex={} in group: {}",
                    merge != null, fill != null, mergeIndex, group.name());
            return false;
        }

        logger.debug("MERGE FOUND: group={}, mergeIndex={}, mergeMode={}, fill_color={}",
                group.name(), mergeIndex, merge.mergeMode(), fill.color());

        List<javafx.scene.shape.Shape> mergeInputs = new ArrayList<>();
        for (int i = 0; i < mergeIndex; i++) {
            BaseShape input = group.shapes().get(i);
            logger.debug("  MERGE INPUT [{}]: type={}", i, input.getClass().getSimpleName());
            javafx.scene.shape.Shape fxShape = toFxShape(input, frame);
            if (fxShape != null) {
                mergeInputs.add(fxShape);
                logger.debug("    -> converted to FxShape: {}", fxShape.getClass().getSimpleName());
            } else {
                logger.debug("    -> toFxShape returned NULL");
            }
        }

        logger.debug("MERGE INPUTS COLLECTED: {} inputs for group: {}", mergeInputs.size(), group.name());

        if (mergeInputs.isEmpty()) {
            logger.debug("MERGE SKIP - no inputs collected in group: {}", group.name());
            return false;
        }

        // Single input still renders (no-op merge)
        if (mergeInputs.size() == 1) {
            logger.debug("MERGE RENDERING: single-input merge (no-op) in group: {}", group.name());
        }

        javafx.scene.shape.Shape merged = mergeInputs.get(0);
        MergeMode mode = merge.mergeMode() != null ? merge.mergeMode() : MergeMode.NORMAL;
        for (int i = 1; i < mergeInputs.size(); i++) {
            merged = applyMergeMode(merged, mergeInputs.get(i), mode);
        }

        gc.save();
        if (groupTransform != null) {
            transformApplier.applyGroupTransform(gc, groupTransform, frame);
        }

        javafx.scene.shape.FillRule fxFillRule = fill.fillRule() == FillRule.EVEN_ODD
                ? javafx.scene.shape.FillRule.EVEN_ODD
                : javafx.scene.shape.FillRule.NON_ZERO;
        var fillColor = new FillStyle(fill).getColor(frame);
        logger.debug("MERGE RENDERING PATH: merged shape type={}, fillRule={}, fillColor={}",
                merged.getClass().getSimpleName(), fxFillRule, fillColor);

        if (!renderMergedShape(gc, merged, fxFillRule, fillColor)) {
            logger.debug("MERGE RENDER FAILED: renderMergedShape returned false for group: {}", group.name());
            gc.restore();
            return false;
        }
        gc.restore();

        logger.debug("MERGE RENDER SUCCESS: {} with {} inputs in group {}", mode, mergeInputs.size(), group.name());

        for (int i = group.shapes().size() - 1; i >= 0; i--) {
            BaseShape item = group.shapes().get(i);
            if (item.shapeType().shapeGroup() == ShapeGroup.GROUP) {
                renderShapeTypeGroup(gc, item, frame, effectiveTrimPath);
            }
        }

        logger.debug("Rendered merge modifier {} with {} inputs in group {}", mode, mergeInputs.size(), group.name());
        return true;
    }

    private javafx.scene.shape.Shape applyMergeMode(javafx.scene.shape.Shape left,
                                                    javafx.scene.shape.Shape right,
                                                    MergeMode mergeMode) {
        return switch (mergeMode) {
            case NORMAL, ADD -> javafx.scene.shape.Shape.union(left, right);
            case SUBTRACT -> javafx.scene.shape.Shape.subtract(left, right);
            case INTERSECT -> javafx.scene.shape.Shape.intersect(left, right);
            case EXCLUDE -> {
                javafx.scene.shape.Shape union = javafx.scene.shape.Shape.union(left, right);
                javafx.scene.shape.Shape intersection = javafx.scene.shape.Shape.intersect(left, right);
                yield javafx.scene.shape.Shape.subtract(union, intersection);
            }
        };
    }

    private javafx.scene.shape.Shape toFxShape(BaseShape shape, double frame) {
        if (shape instanceof Path path) {
            logger.debug("  toFxShape: Path -> converting bezier");
            BezierDefinition bezierDefinition = resolveBezier(path, frame);
            if (bezierDefinition == null) {
                logger.debug("    toFxShape: Path -> bezier was NULL");
                return null;
            }
            logger.debug("    toFxShape: Path -> created FxPath");
            return createFxPath(bezierDefinition);
        }
        if (shape instanceof Ellipse ellipse) {
            if (ellipse.size() == null) {
                logger.debug("  toFxShape: Ellipse -> size is NULL");
                return null;
            }
            double width = ellipse.size().getValue(AnimatedValueType.WIDTH, frame);
            double height = ellipse.size().getValue(AnimatedValueType.HEIGHT, frame);
            double centerX = ellipse.position() != null ? ellipse.position().getValue(AnimatedValueType.X, frame) : 0;
            double centerY = ellipse.position() != null ? ellipse.position().getValue(AnimatedValueType.Y, frame) : 0;
            logger.debug("  toFxShape: Ellipse -> created at ({},{}) size {}x{}", centerX, centerY, width, height);
            return new javafx.scene.shape.Ellipse(centerX, centerY, width / 2.0, height / 2.0);
        }
        if (shape instanceof Rectangle rectangle) {
            if (rectangle.size() == null) {
                logger.debug("  toFxShape: Rectangle -> size is NULL");
                return null;
            }
            double width = rectangle.size().getValue(AnimatedValueType.WIDTH, frame);
            double height = rectangle.size().getValue(AnimatedValueType.HEIGHT, frame);
            double centerX = rectangle.position() != null ? rectangle.position().getValue(AnimatedValueType.X, frame) : 0;
            double centerY = rectangle.position() != null ? rectangle.position().getValue(AnimatedValueType.Y, frame) : 0;
            javafx.scene.shape.Rectangle fxRect = new javafx.scene.shape.Rectangle(centerX - width / 2.0, centerY - height / 2.0, width, height);
            if (rectangle.roundedCornerRadius() != null) {
                double radius = rectangle.roundedCornerRadius().getValue(0, frame);
                fxRect.setArcWidth(radius * 2.0);
                fxRect.setArcHeight(radius * 2.0);
            }
            logger.debug("  toFxShape: Rectangle -> created at ({},{}) size {}x{}", centerX, centerY, width, height);
            return fxRect;
        }
        logger.debug("  toFxShape: UNSUPPORTED type: {}", shape.getClass().getSimpleName());
        return null;
    }

    private BezierDefinition resolveBezier(Path path, double frame) {
        if (path.bezier() instanceof FixedBezier fixedBezier) {
            return fixedBezier.bezier();
        }
        if (path.bezier() instanceof AnimatedBezier animatedBezier) {
            return pathBezierInterpolator.getInterpolatedBezier(animatedBezier, frame);
        }
        return null;
    }

    private javafx.scene.shape.Path createFxPath(BezierDefinition bezierDefinition) {
        javafx.scene.shape.Path fxPath = new javafx.scene.shape.Path();
        List<List<Double>> vertices = bezierDefinition.vertices();
        List<List<Double>> tangentsIn = bezierDefinition.tangentsIn();
        List<List<Double>> tangentsOut = bezierDefinition.tangentsOut();

        if (vertices == null || vertices.isEmpty()) {
            return fxPath;
        }

        boolean first = true;
        for (int i = 0; i < vertices.size(); i++) {
            List<Double> vertex = vertices.get(i);
            if (vertex.size() < 2) {
                continue;
            }

            double x = vertex.get(0);
            double y = vertex.get(1);
            if (first) {
                fxPath.getElements().add(new MoveTo(x, y));
                first = false;
                continue;
            }

            if (tangentsIn != null && tangentsOut != null && i - 1 < tangentsOut.size() && i < tangentsIn.size()) {
                List<Double> prevTangentOut = tangentsOut.get(i - 1);
                List<Double> currentTangentIn = tangentsIn.get(i);
                if (prevTangentOut.size() >= 2 && currentTangentIn.size() >= 2) {
                    List<Double> previousVertex = vertices.get(i - 1);
                    fxPath.getElements().add(new CubicCurveTo(
                            previousVertex.get(0) + prevTangentOut.get(0),
                            previousVertex.get(1) + prevTangentOut.get(1),
                            x + currentTangentIn.get(0),
                            y + currentTangentIn.get(1),
                            x,
                            y
                    ));
                    continue;
                }
            }

            fxPath.getElements().add(new LineTo(x, y));
        }

        if (Boolean.TRUE.equals(bezierDefinition.closed()) && vertices.size() > 1) {
            int lastIdx = vertices.size() - 1;
            if (tangentsIn != null && tangentsOut != null && lastIdx < tangentsOut.size() && !tangentsIn.isEmpty()) {
                List<Double> lastTangentOut = tangentsOut.get(lastIdx);
                List<Double> firstTangentIn = tangentsIn.get(0);
                if (lastTangentOut.size() >= 2 && firstTangentIn.size() >= 2) {
                    List<Double> lastVertex = vertices.get(lastIdx);
                    List<Double> firstVertex = vertices.get(0);
                    fxPath.getElements().add(new CubicCurveTo(
                            lastVertex.get(0) + lastTangentOut.get(0),
                            lastVertex.get(1) + lastTangentOut.get(1),
                            firstVertex.get(0) + firstTangentIn.get(0),
                            firstVertex.get(1) + firstTangentIn.get(1),
                            firstVertex.get(0),
                            firstVertex.get(1)
                    ));
                }
            }
            fxPath.getElements().add(new ClosePath());
        }

        return fxPath;
    }

    private boolean drawFxShapePath(GraphicsContext gc, javafx.scene.shape.Shape shape, javafx.scene.shape.FillRule fillRule) {
        if (!(shape instanceof javafx.scene.shape.Path path) || path.getElements().isEmpty()) {
            return false;
        }

        path.setFillRule(fillRule);
        gc.setFillRule(fillRule);
        gc.beginPath();

        for (PathElement element : path.getElements()) {
            if (element instanceof MoveTo moveTo) {
                gc.moveTo(moveTo.getX(), moveTo.getY());
            } else if (element instanceof LineTo lineTo) {
                gc.lineTo(lineTo.getX(), lineTo.getY());
            } else if (element instanceof CubicCurveTo cubicCurveTo) {
                gc.bezierCurveTo(
                        cubicCurveTo.getControlX1(), cubicCurveTo.getControlY1(),
                        cubicCurveTo.getControlX2(), cubicCurveTo.getControlY2(),
                        cubicCurveTo.getX(), cubicCurveTo.getY()
                );
            } else if (element instanceof ClosePath) {
                gc.closePath();
            }
        }

        return true;
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

    /**
     * Checks if a group contains multiple overlapping shape elements.
     * This is a heuristic check to determine if off-screen rendering is needed.
     *
     * @param group group to check
     * @return true if the group likely contains overlapping shapes
     */
    private boolean groupContainsOverlappingShapes(Group group) {
        // Count the number of shape elements (excluding modifiers)
        int shapeCount = 0;
        boolean hasFill = false;

        for (BaseShape item : group.shapes()) {
            if (item instanceof Group) {
                // Nested groups could have overlapping content
                shapeCount++;
            } else if (item instanceof Path) {
                shapeCount++;
            } else if (item instanceof Fill) {
                hasFill = true;
            }
        }

        // If we have multiple shapes (2+) with a fill, they likely overlap
        return shapeCount > 1 && hasFill;
    }

    /**
     * Renders a group to an off-screen buffer, applies opacity, and draws to the main context.
     * This ensures that overlapping shapes combine before opacity is applied.
     *
     * @param gc                graphics context
     * @param group             group to render
     * @param effectiveGroup    group with effective trim path
     * @param frame             animation frame
     * @param effectiveTrimPath effective trim path
     * @param groupTransform    group transform (may contain opacity)
     */
    private void renderGroupWithOffscreenBuffer(GraphicsContext gc, Group group, Group effectiveGroup,
                                                double frame, TrimPath effectiveTrimPath,
                                                Transform groupTransform) {
        Bounds transformedBounds = estimateTransformedGroupBounds(group, frame, groupTransform);
        if (transformedBounds == null || transformedBounds.getWidth() <= 0 || transformedBounds.getHeight() <= 0) {
            logger.debug("Group {} produced no tight bounds; falling back to direct rendering", group.name());
            if (groupTransform != null) {
                transformApplier.applyGroupTransform(gc, groupTransform, frame);
            }
            renderGroupItems(gc, group, effectiveGroup, frame, effectiveTrimPath);
            return;
        }

        double minX = Math.floor(transformedBounds.getMinX());
        double minY = Math.floor(transformedBounds.getMinY());
        double maxX = Math.ceil(transformedBounds.getMaxX());
        double maxY = Math.ceil(transformedBounds.getMaxY());
        double offscreenWidth = Math.max(1.0, maxX - minX);
        double offscreenHeight = Math.max(1.0, maxY - minY);

        WritableImage offscreenImage = OffscreenRenderer.renderToImage(offscreenWidth, offscreenHeight, offscreenGc -> {
            offscreenGc.save();
            offscreenGc.translate(-minX, -minY);
            if (groupTransform != null) {
                transformApplier.applyGroupTransformWithoutOpacity(offscreenGc, groupTransform, frame);
            }
            renderGroupItems(offscreenGc, group, effectiveGroup, frame, effectiveTrimPath);
            offscreenGc.restore();
        });

        double groupOpacity = 1.0;
        if (groupTransform != null && groupTransform.opacity() != null) {
            groupOpacity = groupTransform.opacity().getValue(0, frame) / 100.0;
        }

        double currentAlpha = gc.getGlobalAlpha();
        gc.setGlobalAlpha(currentAlpha * groupOpacity);
        gc.drawImage(offscreenImage, minX, minY);
        gc.setGlobalAlpha(currentAlpha);

        logger.debug("Finished off-screen rendering for group: {} using tight bounds [{}x{} @ {},{}]",
                group.name(), offscreenWidth, offscreenHeight, minX, minY);
    }

    private void renderGroupItems(GraphicsContext gc, Group group, Group effectiveGroup, double frame, TrimPath effectiveTrimPath) {
        for (int i = group.shapes().size() - 1; i >= 0; i--) {
            BaseShape item = group.shapes().get(i);
            if (item instanceof Transform || item instanceof TrimPath) {
                continue;
            }

            switch (item.shapeType().shapeGroup()) {
                case GROUP -> renderShapeTypeGroup(gc, item, frame, effectiveTrimPath);
                case SHAPE -> renderShape(gc, item, effectiveGroup, frame);
                case STYLE -> {
                    // Skip
                }
                case MODIFIER -> handleModifier(item);
                default ->
                        logger.warn("Unsupported shape type in off-screen rendering: {}", item.shapeType().shapeGroup());
            }
        }
    }

    private Bounds estimateTransformedGroupBounds(Group group, double frame, Transform groupTransform) {
        Affine transform = new Affine();
        appendGroupTransform(transform, groupTransform, frame);
        return estimateGroupBounds(group, frame, transform);
    }

    private Bounds estimateGroupBounds(Group group, double frame, Affine accumulatedTransform) {
        Bounds union = null;
        for (BaseShape item : group.shapes()) {
            if (item instanceof Transform || item instanceof TrimPath) {
                continue;
            }
            if (item instanceof Group childGroup) {
                Affine childTransform = new Affine(accumulatedTransform);
                appendGroupTransform(childTransform, extractGroupTransform(childGroup), frame);
                union = unionBounds(union, estimateGroupBounds(childGroup, frame, childTransform));
                continue;
            }
            union = unionBounds(union, estimateShapeBounds(item, frame, accumulatedTransform));
        }

        if (union == null) {
            return null;
        }

        return expandBounds(union, resolveStrokePadding(group, frame, accumulatedTransform) + OFFSCREEN_BOUNDS_PADDING);
    }

    private Bounds estimateShapeBounds(BaseShape shape, double frame, Affine accumulatedTransform) {
        javafx.scene.shape.Shape fxShape = toFxShape(shape, frame);
        if (fxShape == null) {
            return null;
        }
        return accumulatedTransform.transform(fxShape.getLayoutBounds());
    }

    private Transform extractGroupTransform(Group group) {
        for (BaseShape item : group.shapes()) {
            if (item instanceof Transform transform) {
                return transform;
            }
        }
        return null;
    }

    private void appendGroupTransform(Affine affine, Transform transform, double frame) {
        if (transform == null) {
            return;
        }
        if (transform.position() != null) {
            affine.appendTranslation(
                    transform.position().getValue(AnimatedValueType.X, frame),
                    transform.position().getValue(AnimatedValueType.Y, frame)
            );
        }
        if (transform.rotation() != null) {
            affine.appendRotation(transform.rotation().getValue(0, frame));
        }
        if (transform.scale() != null) {
            affine.appendScale(
                    transform.scale().getValue(AnimatedValueType.X, frame) / 100.0,
                    transform.scale().getValue(AnimatedValueType.Y, frame) / 100.0
            );
        }
        if (transform.anchor() != null) {
            affine.appendTranslation(
                    -transform.anchor().getValue(AnimatedValueType.X, frame),
                    -transform.anchor().getValue(AnimatedValueType.Y, frame)
            );
        }
    }

    private double resolveStrokePadding(Group group, double frame, Affine accumulatedTransform) {
        double maxStrokeWidth = 0.0;
        for (BaseShape item : group.shapes()) {
            if (item instanceof Stroke stroke && stroke.strokeWidth() != null) {
                maxStrokeWidth = Math.max(maxStrokeWidth, stroke.strokeWidth().getValue(0, frame));
            } else if (item instanceof GradientStroke gradientStroke && gradientStroke.strokeWidth() != null) {
                maxStrokeWidth = Math.max(maxStrokeWidth, gradientStroke.strokeWidth().getValue(0, frame));
            }
        }
        if (maxStrokeWidth <= 0.0) {
            return 0.0;
        }

        double scaleX = Math.hypot(accumulatedTransform.getMxx(), accumulatedTransform.getMyx());
        double scaleY = Math.hypot(accumulatedTransform.getMxy(), accumulatedTransform.getMyy());
        double scale = Math.max(1.0, Math.max(scaleX, scaleY));
        return (maxStrokeWidth * scale) / 2.0;
    }

    private Bounds unionBounds(Bounds left, Bounds right) {
        if (left == null) {
            return right;
        }
        if (right == null) {
            return left;
        }
        double minX = Math.min(left.getMinX(), right.getMinX());
        double minY = Math.min(left.getMinY(), right.getMinY());
        double maxX = Math.max(left.getMaxX(), right.getMaxX());
        double maxY = Math.max(left.getMaxY(), right.getMaxY());
        return new BoundingBox(minX, minY, maxX - minX, maxY - minY);
    }

    private Bounds expandBounds(Bounds bounds, double padding) {
        if (bounds == null) {
            return null;
        }
        return new BoundingBox(
                bounds.getMinX() - padding,
                bounds.getMinY() - padding,
                bounds.getWidth() + padding * 2.0,
                bounds.getHeight() + padding * 2.0
        );
    }

    /**
     * Handles non-trim shape modifiers that are not yet supported by the JavaFX renderer.
     * Logs once per modifier type to avoid frame-by-frame log spam.
     */
    private void handleModifier(BaseShape modifier) {
        if (modifier instanceof TrimPath || modifier instanceof Merge) {
            return;
        }

        String modifierType = modifier.shapeType() != null ? modifier.shapeType().name() : modifier.getClass().getSimpleName();
        if (unsupportedModifierWarnings.add(modifierType)) {
            logger.warn("Skipping unsupported modifier: {}", modifierType);
        } else {
            logger.debug("Skipping unsupported modifier: {}", modifierType);
        }
    }

    private void addPathToCanvas(GraphicsContext gc, Path path, double frame) {
        BezierDefinition bezierDefinition = resolveBezier(path, frame);
        if (bezierDefinition == null) {
            return;
        }

        javafx.scene.shape.Path fxPath = createFxPath(bezierDefinition);
        for (PathElement element : fxPath.getElements()) {
            if (element instanceof MoveTo moveTo) {
                gc.moveTo(moveTo.getX(), moveTo.getY());
            } else if (element instanceof LineTo lineTo) {
                gc.lineTo(lineTo.getX(), lineTo.getY());
            } else if (element instanceof CubicCurveTo cubicCurveTo) {
                gc.bezierCurveTo(
                        cubicCurveTo.getControlX1(), cubicCurveTo.getControlY1(),
                        cubicCurveTo.getControlX2(), cubicCurveTo.getControlY2(),
                        cubicCurveTo.getX(), cubicCurveTo.getY()
                );
            } else if (element instanceof ClosePath) {
                gc.closePath();
            }
        }
    }

    private boolean renderMergedShape(GraphicsContext gc,
                                      javafx.scene.shape.Shape merged,
                                      javafx.scene.shape.FillRule fillRule,
                                      Color fillColor) {
        if (drawFxShapePath(gc, merged, fillRule)) {
            logger.debug("MERGED SHAPE RENDERED via path: {}", merged.getClass().getSimpleName());
            gc.setFill(fillColor);
            gc.fill();
            return true;
        }

        // JavaFX boolean ops may return non-Path shapes; snapshot rendering keeps these visible.
        logger.debug("MERGED SHAPE RENDERING via snapshot: {} (non-Path result from boolean op)", merged.getClass().getSimpleName());
        return drawShapeViaSnapshot(gc, merged, fillRule, fillColor);
    }

    private boolean drawShapeViaSnapshot(GraphicsContext gc,
                                         javafx.scene.shape.Shape shape,
                                         javafx.scene.shape.FillRule fillRule,
                                         Color fillColor) {
        Bounds bounds = shape.getLayoutBounds();
        if (bounds == null || bounds.getWidth() <= 0 || bounds.getHeight() <= 0) {
            return false;
        }

        shape.setStroke(null);
        shape.setFill(fillColor);
        if (shape instanceof javafx.scene.shape.Path path) {
            path.setFillRule(fillRule);
        }

        double padding = 2.0;
        int imageWidth = Math.max(1, (int) Math.ceil(bounds.getWidth() + 2 * padding));
        int imageHeight = Math.max(1, (int) Math.ceil(bounds.getHeight() + 2 * padding));

        javafx.scene.Group node = new javafx.scene.Group(shape);
        node.setTranslateX(-bounds.getMinX() + padding);
        node.setTranslateY(-bounds.getMinY() + padding);

        SnapshotParameters snapshotParameters = new SnapshotParameters();
        snapshotParameters.setFill(Color.TRANSPARENT);
        WritableImage image = node.snapshot(snapshotParameters, new WritableImage(imageWidth, imageHeight));

        gc.drawImage(image, bounds.getMinX() - padding, bounds.getMinY() - padding);
        return true;
    }
}
