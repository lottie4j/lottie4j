package com.lottie4j.fxplayer;

import com.lottie4j.core.definition.LayerType;
import com.lottie4j.core.model.AnimatedValueType;
import com.lottie4j.core.model.Animation;
import com.lottie4j.core.model.Asset;
import com.lottie4j.core.model.Layer;
import com.lottie4j.core.model.shape.BaseShape;
import com.lottie4j.core.model.shape.grouping.Group;
import com.lottie4j.core.model.shape.grouping.Transform;
import com.lottie4j.core.model.shape.modifier.TrimPath;
import com.lottie4j.fxplayer.renderer.shape.*;
import javafx.animation.AnimationTimer;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * JavaFX Canvas component for playing Lottie animations
 */
public class LottiePlayer extends Canvas {

    private static final Logger logger = Logger.getLogger(LottiePlayer.class.getName());

    private final Animation animation;
    private final GraphicsContext gc;
    private final DoubleProperty currentFrameProperty = new SimpleDoubleProperty(0);
    private final Map<Integer, Layer> layersByIndex;
    private final Map<String, Asset> assetsById;
    private AnimationTimer animationTimer;
    private long startTime;
    private boolean isPlaying = false;
    private boolean debug = false;
    private Color backgroundColor = Color.WHITE;

    /**
     * Creates a player with the dimensions as defined in the animation (or 500 as width and height if no size is defined).
     *
     * @param animation {@link Animation}
     */
    public LottiePlayer(Animation animation) {
        this(animation, false);
    }

    /**
     * Creates a player with the dimensions as defined in the animation (or 500 as width and height if no size is defined).
     *
     * @param animation {@link Animation}
     * @param debug     Flag to define if debug info should be displayed on top of the animation
     */
    public LottiePlayer(Animation animation, boolean debug) {
        this(animation,
                animation.width() != null ? animation.width() : 500,
                animation.height() != null ? animation.height() : 500,
                debug);
    }

    /**
     * Creates a player with the specified dimensions.
     *
     * @param animation {@link Animation}
     * @param width     Specifies the width of the canvas
     * @param height    Specifies the height of the canvas
     */
    public LottiePlayer(Animation animation, int width, int height) {
        this(animation, width, height, false);
    }

    /**
     * Creates a player with the specified dimensions.
     *
     * @param animation {@link Animation}
     * @param width     Specifies the width of the canvas
     * @param height    Specifies the height of the canvas
     * @param debug     Flag to define if debug info should be displayed on top of the animation
     */
    public LottiePlayer(Animation animation, int width, int height, boolean debug) {
        this.animation = animation;
        this.debug = debug;

        // Set canvas size to specified dimensions
        setWidth(width);
        setHeight(height);
        logger.info("Canvas size set to: " + getWidth() + "x" + getHeight());

        this.gc = getGraphicsContext2D();

        // Build layer index map for parenting support
        this.layersByIndex = new HashMap<>();
        if (animation.layers() != null) {
            for (Layer layer : animation.layers()) {
                if (layer.indexLayer() != null) {
                    layersByIndex.put(layer.indexLayer().intValue(), layer);
                }
            }
        }

        // Build asset map for precomposition support
        this.assetsById = new HashMap<>();
        if (animation.assets() != null) {
            for (Asset asset : animation.assets()) {
                if (asset.id() != null) {
                    assetsById.put(asset.id(), asset);
                }
            }
        }

        // Initial render
        renderFrame(getInPoint());
    }

    private int getInPoint() {
        return animation.inPoint() != null ? animation.inPoint() : 0;
    }

    private int getOutPoint() {
        return animation.outPoint() != null ? animation.outPoint() : 60;
    }

    private int getFramesPerSecond() {
        return animation.framesPerSecond() != null ? animation.framesPerSecond() : 30;
    }

    private int getAnimationWidth() {
        return animation.width() != null ? animation.width() : 500;
    }

    private int getAnimationHeight() {
        return animation.height() != null ? animation.height() : 500;
    }

    public void play() {
        if (isPlaying) return;

        logger.info("Starting animation");
        isPlaying = true;

        // Calculate the time offset based on current frame position
        double currentFrame = currentFrameProperty.get();
        double frameOffset = currentFrame - getInPoint();
        double timeOffset = frameOffset / getFramesPerSecond();
        startTime = System.nanoTime() - (long) (timeOffset * 1_000_000_000.0);

        animationTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                double elapsedSeconds = (now - startTime) / 1_000_000_000.0;
                double totalFrames = getOutPoint() - getInPoint();
                double animationDuration = totalFrames / getFramesPerSecond();

                if (elapsedSeconds >= animationDuration) {
                    // Loop animation
                    startTime = now;
                    elapsedSeconds = 0;
                }

                double newFrame = getInPoint() + (elapsedSeconds * getFramesPerSecond());
                currentFrameProperty.set(newFrame);
                renderFrame(newFrame);
            }
        };

        animationTimer.start();
    }

    public void stop() {
        if (animationTimer != null) {
            animationTimer.stop();
        }
        logger.info("Animation stopped");
        isPlaying = false;
    }

    public void seekToFrame(double frame) {
        double clampedFrame = Math.max(getInPoint(), Math.min(getOutPoint(), frame));
        currentFrameProperty.set(clampedFrame);
        renderFrame(clampedFrame);
    }

    public DoubleProperty currentFrameProperty() {
        return currentFrameProperty;
    }

    public void render(double frame) {
        logger.finer("=== RENDER START ===");
        logger.finer("Canvas dimensions: " + getWidth() + "x" + getHeight());
        logger.finer("Rendering frame: " + frame);

        gc.clearRect(0, 0, gc.getCanvas().getWidth(), gc.getCanvas().getHeight());
        gc.setFill(backgroundColor);
        gc.fillRect(0, 0, gc.getCanvas().getWidth(), gc.getCanvas().getHeight());

        if (debug) {
            gc.setStroke(Color.RED);
            gc.setLineWidth(2);
            gc.strokeRect(1, 1, gc.getCanvas().getWidth() - 2, gc.getCanvas().getHeight() - 2);
        }

        if (animation == null) {
            logger.warning("No animation to render");
            return;
        }

        if (animation.layers() == null || animation.layers().isEmpty()) {
            logger.warning("No layers in animation");
            gc.setFill(Color.BLACK);
            gc.fillText("No layers found in animation", 10, 30);
            return;
        }

        logger.finer("Animation has " + animation.layers().size() + " layers");

        // Calculate scaling to fit canvas while maintaining aspect ratio
        double scaleX = gc.getCanvas().getWidth() / getAnimationWidth();
        double scaleY = gc.getCanvas().getHeight() / getAnimationHeight();
        double scale = Math.min(scaleX, scaleY);
        double offsetX = (gc.getCanvas().getWidth() - getAnimationWidth() * scale) / 2;
        double offsetY = (gc.getCanvas().getHeight() - getAnimationHeight() * scale) / 2;
        logger.finer("Animation size: " + getAnimationWidth() + "x" + getAnimationHeight());
        logger.finer("Canvas size: " + gc.getCanvas().getWidth() + "x" + gc.getCanvas().getHeight());
        logger.finer("Scale: " + scale + ", Offset: " + offsetX + ", " + offsetY);

        gc.save();
        gc.translate(offsetX, offsetY);
        gc.scale(scale, scale);

        // Set default colors for debugging
        gc.setFill(Color.BLUE);

        // Render layers in reverse order
        // Lottie renders layers bottom-to-top (last in array is drawn first, appears behind)
        for (int i = animation.layers().size() - 1; i >= 0; i--) {
            Layer layer = animation.layers().get(i);
            logger.finer("Processing layer: " + layer.name());

            if (isLayerActiveAtFrame(layer, frame)) {
                logger.fine("Rendering layer: " + layer.name());
                renderLayer(gc, layer, frame);
            }
        }

        gc.restore();

        if (debug) {
            gc.setFill(Color.BLACK);
            gc.fillText("Frame: " + String.format("%.1f", frame), 10, gc.getCanvas().getHeight() - 30);
            gc.fillText("Scale: " + String.format("%.2f", scale), 10, gc.getCanvas().getHeight() - 10);
        }
    }

    private boolean isLayerActiveAtFrame(Layer layer, double frame) {
        boolean active = frame >= layer.inPoint() && frame <= layer.outPoint();
        logger.finer("Layer " + layer.name() + " active at frame " + frame + ": " + active +
                " (in: " + layer.inPoint() + ", out: " + layer.outPoint() + ")");
        return active;
    }

    private void renderLayer(GraphicsContext gc, Layer layer, double frame) {
        gc.save();

        // Apply parent transforms recursively
        applyParentTransforms(gc, layer, frame);

        // Apply this layer's transform
        applyLayerTransform(gc, layer, frame);

        // Handle precomposition layers
        if (layer.layerType() == LayerType.PRECOMPOSITION) {
            renderPrecompositionLayer(gc, layer, frame);
        }
        // Skip rendering shapes for NULL layers (type 3), but transforms are still applied
        else if (layer.layerType() != LayerType.NULL) {
            // Render layer shapes
            if (layer.shapes() != null && !layer.shapes().isEmpty()) {
                logger.fine("Layer has " + layer.shapes().size() + " shapes");

                // First pass: collect any layer-level modifiers (like TrimPath)
                TrimPath layerTrimPath = null;
                for (BaseShape shape : layer.shapes()) {
                    if (shape instanceof TrimPath trim) {
                        layerTrimPath = trim;
                        logger.finer("Found layer-level TrimPath");
                    }
                }

                // Second pass: render shapes in REVERSE order (bottom-to-top: last shape renders first)
                for (int i = layer.shapes().size() - 1; i >= 0; i--) {
                    BaseShape shape = layer.shapes().get(i);

                    // Skip modifiers and styles - they're applied within groups/shapes
                    if (shape instanceof TrimPath) {
                        continue;
                    }

                    logger.fine("Shape class: " + shape.getClass().getSimpleName() + ", type: " + shape.type() + ", shapeGroup: " + shape.type().shapeGroup());

                    switch (shape.type().shapeGroup()) {
                        case GROUP -> renderShapeTypeGroup(shape, frame, layerTrimPath);
                        case SHAPE -> renderShapeTypeShape(shape, null, frame);
                        case STYLE -> {
                            // Skip - styles (Fill, Stroke, etc.) are handled within groups
                        }
                        default -> logger.warning("Unsupported shape type: " + shape.type().shapeGroup());
                    }
                }
            } else {
                logger.finer("Layer has no shapes");
            }
        } else {
            logger.finer("Skipping shape rendering for NULL layer: " + layer.name());
        }

        gc.restore();
    }

    private void renderPrecompositionLayer(GraphicsContext gc, Layer layer, double frame) {
        if (layer.referenceId() == null) {
            logger.warning("Precomposition layer has no referenceId: " + layer.name());
            return;
        }

        Asset asset = assetsById.get(layer.referenceId());
        if (asset == null) {
            logger.warning("Asset not found for referenceId: " + layer.referenceId());
            return;
        }

        if (asset.layers() == null || asset.layers().isEmpty()) {
            logger.warning("Precomposition asset has no layers: " + layer.referenceId());
            return;
        }

        logger.finer("Rendering precomposition: " + layer.referenceId() + " with " + asset.layers().size() + " layers");

        // Build a temporary layer index map for this precomposition's parenting
        Map<Integer, Layer> precompLayersByIndex = new HashMap<>();
        for (Layer precompLayer : asset.layers()) {
            if (precompLayer.indexLayer() != null) {
                precompLayersByIndex.put(precompLayer.indexLayer().intValue(), precompLayer);
            }
        }

        // Render all layers in the precomposition in reverse order
        // Lottie renders layers bottom-to-top (last in array is drawn first, appears behind)
        for (int i = asset.layers().size() - 1; i >= 0; i--) {
            Layer precompLayer = asset.layers().get(i);
            if (isLayerActiveAtFrame(precompLayer, frame)) {
                logger.fine("Rendering precomp layer [" + i + "] ind=" + precompLayer.indexLayer() + " name='" + precompLayer.name() + "'");
                renderPrecompLayer(gc, precompLayer, frame, precompLayersByIndex);
            }
        }
    }

    private void renderPrecompLayer(GraphicsContext gc, Layer layer, double frame, Map<Integer, Layer> precompLayersByIndex) {
        gc.save();

        // Apply parent transforms recursively within the precomp
        applyPrecompParentTransforms(gc, layer, frame, precompLayersByIndex);

        // Apply this layer's transform
        applyLayerTransform(gc, layer, frame);

        // Handle different layer types
        if (layer.layerType() == LayerType.PRECOMPOSITION) {
            renderPrecompositionLayer(gc, layer, frame);
        } else if (layer.layerType() != LayerType.NULL) {
            // Render layer shapes
            if (layer.shapes() != null && !layer.shapes().isEmpty()) {
                logger.fine("Layer has " + layer.shapes().size() + " shapes");

                // First pass: collect any layer-level modifiers (like TrimPath)
                TrimPath layerTrimPath = null;
                for (BaseShape shape : layer.shapes()) {
                    if (shape instanceof TrimPath trim) {
                        layerTrimPath = trim;
                        logger.finer("Found layer-level TrimPath");
                    }
                }

                // Second pass: render shapes in REVERSE order, passing down layer-level modifiers
                // Lottie renders shapes from last to first (last appears behind, first appears on top)
                java.util.List<BaseShape> shapes = layer.shapes();
                for (int i = shapes.size() - 1; i >= 0; i--) {
                    BaseShape shape = shapes.get(i);
                    // Skip modifiers and styles - they're applied within groups/shapes
                    if (shape instanceof TrimPath) {
                        continue;
                    }

                    switch (shape.type().shapeGroup()) {
                        case GROUP -> renderShapeTypeGroup(shape, frame, layerTrimPath);
                        case SHAPE -> renderShapeTypeShape(shape, null, frame);
                        case STYLE -> {
                        } // Skip - styles (Fill, Stroke, etc.) are handled within groups
                        default -> {
                            logger.finer("Unsupported shape type: " + shape.type().shapeGroup());
                        }
                    }
                }
            } else {
                logger.finer("Layer has no shapes");
            }
        } else {
            logger.finer("Skipping shape rendering for NULL layer: " + layer.name());
        }

        gc.restore();
    }

    private void applyPrecompParentTransforms(GraphicsContext gc, Layer layer, double frame, Map<Integer, Layer> precompLayersByIndex) {
        if (layer.indexParent() == null) {
            return; // No parent
        }

        Layer parent = precompLayersByIndex.get(layer.indexParent());
        if (parent == null) {
            logger.warning("Parent layer not found in precomp: " + layer.indexParent());
            return;
        }

        // logger.fine("Applying parent transform from: " + parent.name() + " to child: " + layer.name());

        // Recursively apply parent's parent transforms first
        applyPrecompParentTransforms(gc, parent, frame, precompLayersByIndex);

        // Then apply this parent's transform (but NOT opacity - opacity doesn't inherit)
        applyLayerTransformWithoutOpacity(gc, parent, frame);
    }

    private void renderChildrenAfterParent(GraphicsContext gc, Layer parent, double frame,
                                           java.util.List<Layer> allLayers,
                                           Map<Integer, Layer> precompLayersByIndex,
                                           java.util.Set<Integer> renderedIndices) {
        Integer parentInd = parent.indexLayer() != null ? parent.indexLayer().intValue() : null;
        if (parentInd == null) return;

        // Find and render all direct children of this parent in REVERSE order
        // Render from high index to low so that lower indices appear on top
        for (int i = allLayers.size() - 1; i >= 0; i--) {
            Layer layer = allLayers.get(i);
            Integer childInd = layer.indexLayer() != null ? layer.indexLayer().intValue() : null;
            if (childInd == null) continue;

            // Check if this layer is a child of the parent
            if (layer.indexParent() != null && layer.indexParent().intValue() == parentInd) {
                // Skip if already rendered
                if (renderedIndices.contains(childInd)) continue;

                if (isLayerActiveAtFrame(layer, frame)) {
                    logger.fine("  -> Rendering child layer ind=" + childInd + " name='" + layer.name() + "' after parent '" + parent.name() + "'");
                    renderPrecompLayer(gc, layer, frame, precompLayersByIndex);
                    renderedIndices.add(childInd);

                    // Recursively render this child's children
                    renderChildrenAfterParent(gc, layer, frame, allLayers, precompLayersByIndex, renderedIndices);
                }
            }
        }
    }

    private void applyParentTransforms(GraphicsContext gc, Layer layer, double frame) {
        if (layer.indexParent() == null) {
            return; // No parent
        }

        Layer parent = layersByIndex.get(layer.indexParent());
        if (parent == null) {
            logger.warning("Parent layer not found: " + layer.indexParent());
            return;
        }

        // logger.fine("Applying parent transform from: " + parent.name() + " to child: " + layer.name());

        // Recursively apply parent's parent transforms first
        applyParentTransforms(gc, parent, frame);

        // Then apply this parent's transform
        applyLayerTransform(gc, parent, frame);
    }

    public void renderShapeTypeGroup(BaseShape shape, double frame, TrimPath layerTrimPath) {
        if (shape instanceof Transform) {
            logger.warning("Don't know how to render a Transform group yet");
            return;
        }
        if (shape instanceof Group group) {
            logger.fine("Rendering group: " + group.name() + " with " + group.shapes().size() + " items");
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

            if (groupTransform != null) {
                applyGroupTransform(gc, groupTransform, frame);
            }

            // Use group-level TrimPath if present, otherwise use layer-level TrimPath
            TrimPath effectiveTrimPath = groupTrimPath != null ? groupTrimPath : layerTrimPath;
            // if (effectiveTrimPath != null) {
            //     logger.fine("Using TrimPath for group: " + group.name());
            // }

            // Create a synthetic group that includes the effective trim path for renderers
            Group effectiveGroup = createGroupWithTrimPath(group, effectiveTrimPath);

            // Check if this group has multiple Path shapes with a single Fill (needs combined rendering)
            boolean hasMultiplePaths = renderGroupWithCombinedPaths(group, effectiveGroup, frame, effectiveTrimPath);

            if (!hasMultiplePaths) {
                // Render all non-transform/non-modifier shapes in reverse order
                // Lottie renders shapes bottom-to-top (last in array is drawn first, appears behind)
                for (int i = group.shapes().size() - 1; i >= 0; i--) {
                    BaseShape item = group.shapes().get(i);
                    if (item instanceof Transform || item instanceof TrimPath) {
                        continue; // Skip modifiers, they're applied to the shapes
                    }

                    switch (item.type().shapeGroup()) {
                        case GROUP -> renderShapeTypeGroup(item, frame, effectiveTrimPath);
                        case SHAPE -> renderShapeTypeShape(item, effectiveGroup, frame);
                        case STYLE -> {
                            // Skip - styles (Fill, Stroke, etc.) are handled within groups
                        }
                        default -> logger.warning("Not defined how to render shape type: " + item.type().shapeGroup());
                    }
                }
            }

            gc.restore();
        }
    }

    private Group createGroupWithTrimPath(Group original, TrimPath trimPath) {
        if (trimPath == null) {
            return original;
        }
        // Create a new group that includes the TrimPath in its shapes list
        java.util.List<BaseShape> newShapes = new java.util.ArrayList<>(original.shapes());
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
     */
    private boolean renderGroupWithCombinedPaths(Group group, Group effectiveGroup, double frame, TrimPath effectiveTrimPath) {
        // Count Path shapes and check for Fill
        java.util.List<com.lottie4j.core.model.shape.shape.Path> paths = new java.util.ArrayList<>();
        com.lottie4j.core.model.shape.style.Fill fill = null;

        for (BaseShape item : group.shapes()) {
            if (item instanceof com.lottie4j.core.model.shape.shape.Path path) {
                paths.add(path);
            } else if (item instanceof com.lottie4j.core.model.shape.style.Fill f) {
                fill = f;
            }
        }

        // Only use combined rendering if we have multiple paths with a fill
        if (paths.size() < 2 || fill == null) {
            return false;
        }

        logger.fine("Rendering " + paths.size() + " combined paths with fill rule for group: " + group.name());

        // Set fill rule
        javafx.scene.shape.FillRule fxFillRule = fill.fillRule() == com.lottie4j.core.definition.FillRule.EVEN_ODD ?
                javafx.scene.shape.FillRule.EVEN_ODD : javafx.scene.shape.FillRule.NON_ZERO;

        gc.save();
        gc.setFillRule(fxFillRule);
        gc.beginPath();

        // Add all paths to the canvas path in reverse order (last to first)
        for (int i = paths.size() - 1; i >= 0; i--) {
            addPathToCanvas(paths.get(i), frame);
        }

        // Apply fill color and opacity
        var fillColor = new com.lottie4j.fxplayer.element.FillStyle(fill).getColor(frame);
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
            if (item.type().shapeGroup() == com.lottie4j.core.definition.ShapeGroup.GROUP) {
                renderShapeTypeGroup(item, frame, effectiveTrimPath);
            }
        }

        return true;
    }

    /**
     * Add a Path shape's bezier curves to the current canvas path
     */
    private void addPathToCanvas(com.lottie4j.core.model.shape.shape.Path path, double frame) {
        if (path.bezier() == null) return;

        com.lottie4j.core.model.bezier.BezierDefinition bezierDef;
        if (path.bezier() instanceof com.lottie4j.core.model.bezier.FixedBezier fixedBezier) {
            bezierDef = fixedBezier.bezier();
        } else if (path.bezier() instanceof com.lottie4j.core.model.bezier.AnimatedBezier animatedBezier) {
            bezierDef = getInterpolatedBezier(animatedBezier, frame);
        } else {
            return;
        }

        if (bezierDef == null || bezierDef.vertices() == null || bezierDef.vertices().isEmpty()) return;

        java.util.List<java.util.List<Double>> vertices = bezierDef.vertices();
        java.util.List<java.util.List<Double>> tangentsIn = bezierDef.tangentsIn();
        java.util.List<java.util.List<Double>> tangentsOut = bezierDef.tangentsOut();

        boolean first = true;
        for (int i = 0; i < vertices.size(); i++) {
            java.util.List<Double> vertex = vertices.get(i);
            if (vertex.size() < 2) continue;

            double x = vertex.get(0);
            double y = vertex.get(1);

            if (first) {
                gc.moveTo(x, y);
                first = false;
            } else {
                if (tangentsIn != null && tangentsOut != null &&
                        i - 1 < tangentsOut.size() && i < tangentsIn.size()) {
                    java.util.List<Double> prevTangentOut = tangentsOut.get(i - 1);
                    java.util.List<Double> currentTangentIn = tangentsIn.get(i);

                    if (prevTangentOut.size() >= 2 && currentTangentIn.size() >= 2) {
                        java.util.List<Double> prevVertex = vertices.get(i - 1);
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
                java.util.List<Double> lastTangentOut = tangentsOut.get(lastIdx);
                java.util.List<Double> firstTangentIn = tangentsIn.get(0);
                if (lastTangentOut.size() >= 2 && firstTangentIn.size() >= 2) {
                    java.util.List<Double> lastVertex = vertices.get(lastIdx);
                    java.util.List<Double> firstVertex = vertices.get(0);
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

    private com.lottie4j.core.model.bezier.BezierDefinition getInterpolatedBezier(
            com.lottie4j.core.model.bezier.AnimatedBezier animatedBezier, double frame) {
        // This method should already exist in PathRenderer - we need to extract it or duplicate it
        // For now, return null and we'll need to implement it
        logger.warning("Animated bezier not yet supported in combined path rendering");
        return null;
    }

    private void applyGroupTransform(GraphicsContext gc, Transform transform, double frame) {
        logger.finer("Applying group transform");

        // Apply opacity first (doesn't affect transform order)
        if (transform.opacity() != null) {
            // Opacity is a single value, get with frame for animation interpolation
            double opacityValue = transform.opacity().getValue(0, frame);
            double opacity = opacityValue / 100.0;
            logger.finer("Group opacity raw: " + opacityValue + ", normalized: " + opacity);
            if (opacity > 0) {
                gc.setGlobalAlpha(gc.getGlobalAlpha() * opacity);
            }
        }

        // Correct transform order for Lottie/After Effects:
        // 1. Translate by position
        // 2. Apply rotation
        // 3. Apply scale
        // 4. Translate by -anchor (to offset the coordinate system)

        // Step 1: Apply position
        if (transform.position() != null) {
            double x = transform.position().getValue(AnimatedValueType.X, frame);
            double y = transform.position().getValue(AnimatedValueType.Y, frame);
            logger.finer("Group translation: " + x + ", " + y);
            gc.translate(x, y);
        }

        // Step 2: Apply rotation
        if (transform.rotation() != null) {
            double rotationDegrees = transform.rotation().getValue(0, frame);
            logger.finer("Group rotation: " + rotationDegrees + " degrees");
            gc.rotate(rotationDegrees);  // JavaFX rotate() expects degrees, not radians
        }

        // Step 3: Apply scale
        if (transform.scale() != null) {
            double scaleX = transform.scale().getValue(AnimatedValueType.X, frame) / 100.0;
            double scaleY = transform.scale().getValue(AnimatedValueType.Y, frame) / 100.0;
            logger.finer("Group scale: " + scaleX + ", " + scaleY);
            gc.scale(scaleX, scaleY);
        }

        // Step 4: Apply anchor point offset (anchor point is the origin for transforms)
        // This must be done AFTER rotation and scale
        if (transform.anchor() != null) {
            double anchorX = transform.anchor().getValue(AnimatedValueType.X, frame);
            double anchorY = transform.anchor().getValue(AnimatedValueType.Y, frame);
            logger.finer("Group anchor: " + anchorX + ", " + anchorY);
            // Translate by negative anchor to offset the coordinate system
            gc.translate(-anchorX, -anchorY);
        }
    }

    @SuppressWarnings("unchecked")
    public void renderShapeTypeShape(BaseShape shape, Group parentGroup, double frame) {
        if (gc == null || shape == null) {
            logger.warning("Skipping render: gc or shape is null");
            return;
        }

        logger.fine("Rendering shape: " + shape.getClass().getSimpleName() +
                " (name: " + (shape.name() != null ? shape.name() : "unnamed") + ")");

        var renderer = getShapeRenderer(shape.getClass());
        if (renderer != null) {
            logger.fine("Using renderer: " + renderer.getClass().getSimpleName());

            try {
                // The unchecked cast is still here but now with proper error handling
                renderer.render(gc, shape, parentGroup, frame);
            } catch (ClassCastException e) {
                logger.severe("Type mismatch when rendering shape: " + e.getMessage());
                // Fall back to placeholder rendering
                renderPlaceholder(gc);
            }
        } else {
            logger.severe("No renderer found for shape type: " + shape.getClass().getSimpleName());

            // Draw a placeholder for unhandled shapes
            gc.save();
            gc.setFill(Color.ORANGE);
            gc.fillRect(0, 0, 50, 20);
            gc.setFill(Color.BLACK);
            gc.fillText("?", 20, 15);
            gc.restore();
        }
    }

    private void renderPlaceholder(GraphicsContext gc) {
        gc.save();
        gc.setFill(Color.ORANGE);
        gc.fillRect(0, 0, 50, 20);
        gc.setFill(Color.BLACK);
        gc.fillText("?", 20, 15);
        gc.restore();
    }

    private ShapeRenderer getShapeRenderer(Class<? extends BaseShape> shapeType) {
        switch (shapeType.getSimpleName()) {
            case "Rectangle":
                return new RectangleRenderer();
            case "Ellipse":
                return new EllipseRenderer();
            case "Path":
                return new PathRenderer();
            case "Polystar":
                return new PolystarRenderer();
            default:
                return null;
        }
    }

    private void applyLayerTransform(GraphicsContext gc, Layer layer, double frame) {
        if (layer.transform() == null) {
            logger.finer("No transform for layer: " + layer.name());
            return;
        }

        // Apply opacity
        if (layer.transform().opacity() != null) {
            // Opacity is a single value, so use index 0 with current frame for animation
            double opacity = layer.transform().opacity().getValue(0, frame);
            logger.finer("Setting layer opacity: " + opacity + " (normalized: " + (opacity / 100.0) + ")");

            if (opacity > 0) {
                // Multiply with existing alpha to properly composite nested opacities
                gc.setGlobalAlpha(gc.getGlobalAlpha() * (opacity / 100.0));
            }
        } else {
            logger.finer("No opacity transform");
        }

        // Correct transform order for Lottie/After Effects (matching group transform):
        // 1. Translate by position
        // 2. Apply rotation
        // 3. Apply scale
        // 4. Translate by -anchor (to offset the coordinate system)

        // Step 1: Apply position
        if (layer.transform().position() != null) {
            double x = layer.transform().position().getValue(AnimatedValueType.X, frame);
            double y = layer.transform().position().getValue(AnimatedValueType.Y, frame);
            logger.finer("Translating by position: " + x + ", " + y);

            // Check for extreme values that might push content off-screen
            if (Math.abs(x) > 1000 || Math.abs(y) > 1000) {
                logger.warning("WARNING: Large translation values detected! x=" + x + ", y=" + y);
            }

            gc.translate(x, y);
        } else {
            logger.finer("No position transform");
        }

        // Step 2: Apply rotation
        if (layer.transform().rotation() != null) {
            double rotationDegrees = layer.transform().rotation().getValue(0, frame);
            logger.finer("Rotating by: " + rotationDegrees + " degrees");
            gc.rotate(rotationDegrees);  // JavaFX rotate() expects degrees, not radians
        } else {
            logger.finer("No rotation transform");
        }

        // Step 3: Apply 3D rotation approximation (rx, ry)
        // Approximate 3D rotation by scaling perpendicular axis
        double rx3DScale = 1.0;
        double ry3DScale = 1.0;

        if (layer.transform().rx() != null) {
            // X-axis rotation: scale Y dimension by cos(rx) for flip effect
            double rxDegrees = layer.transform().rx().getValue(0, frame);
            rx3DScale = Math.cos(Math.toRadians(rxDegrees));
            logger.finer("Applying 3D X-axis rotation: " + rxDegrees + " degrees (scaleY factor: " + rx3DScale + ")");
        }

        if (layer.transform().ry() != null) {
            // Y-axis rotation: scale X dimension by cos(ry) for flip effect
            double ryDegrees = layer.transform().ry().getValue(0, frame);
            ry3DScale = Math.cos(Math.toRadians(ryDegrees));
            logger.finer("Applying 3D Y-axis rotation: " + ryDegrees + " degrees (scaleX factor: " + ry3DScale + ")");
        }

        // Step 4: Apply scale (combine regular scale with 3D rotation scale approximation)
        if (layer.transform().scale() != null) {
            double scaleX = layer.transform().scale().getValue(AnimatedValueType.X, frame) / 100.0;
            double scaleY = layer.transform().scale().getValue(AnimatedValueType.Y, frame) / 100.0;

            // Apply 3D rotation approximation scaling
            scaleX *= ry3DScale;
            scaleY *= rx3DScale;

            logger.finer("Scaling by: " + scaleX + ", " + scaleY);

            // Check for zero or negative scale that would make content invisible
            if (scaleX <= 0 || scaleY <= 0) {
                logger.warning("WARNING: Zero or negative scale detected! scaleX=" + scaleX + ", scaleY=" + scaleY);
            }

            gc.scale(scaleX, scaleY);
        } else {
            logger.finer("No scale transform");
        }

        // Step 5: Apply anchor point offset (anchor point is the origin for transforms)
        // This must be done AFTER rotation and scale
        if (layer.transform().anchor() != null) {
            double anchorX = layer.transform().anchor().getValue(AnimatedValueType.X, frame);
            double anchorY = layer.transform().anchor().getValue(AnimatedValueType.Y, frame);
            logger.finer("Layer anchor: " + anchorX + ", " + anchorY);
            // Translate by negative anchor to offset the coordinate system
            gc.translate(-anchorX, -anchorY);
        }

        logger.finer("=== LAYER TRANSFORM APPLIED ===");
    }

    /**
     * Apply layer transform WITHOUT opacity - used for parent transforms where opacity should not inherit
     */
    private void applyLayerTransformWithoutOpacity(GraphicsContext gc, Layer layer, double frame) {
        if (layer.transform() == null) {
            logger.finer("No transform for layer: " + layer.name());
            return;
        }

        // NOTE: Opacity is NOT applied here - this method is used for parent transforms
        // where opacity should not be inherited by child layers

        // Correct transform order for Lottie/After Effects (matching group transform):
        // 1. Translate by position
        // 2. Apply rotation
        // 3. Apply scale
        // 4. Translate by -anchor (to offset the coordinate system)

        // Step 1: Apply position
        if (layer.transform().position() != null) {
            double x = layer.transform().position().getValue(AnimatedValueType.X, frame);
            double y = layer.transform().position().getValue(AnimatedValueType.Y, frame);
            logger.finer("Translating by position (without opacity): " + x + ", " + y);

            // Check for extreme values that might push content off-screen
            if (Math.abs(x) > 1000 || Math.abs(y) > 1000) {
                logger.warning("WARNING: Large translation values detected! x=" + x + ", y=" + y);
            }

            gc.translate(x, y);
        } else {
            logger.finer("No position transform");
        }

        // Step 2: Apply rotation
        if (layer.transform().rotation() != null) {
            double rotationDegrees = layer.transform().rotation().getValue(0, frame);
            logger.finer("Rotating by (without opacity): " + rotationDegrees + " degrees");
            gc.rotate(rotationDegrees);  // JavaFX rotate() expects degrees, not radians
        } else {
            logger.finer("No rotation transform");
        }

        // Step 3: Apply 3D rotation approximation (rx, ry)
        // Approximate 3D rotation by scaling perpendicular axis
        double rx3DScale = 1.0;
        double ry3DScale = 1.0;

        if (layer.transform().rx() != null) {
            // X-axis rotation: scale Y dimension by cos(rx) for flip effect
            double rxDegrees = layer.transform().rx().getValue(0, frame);
            rx3DScale = Math.cos(Math.toRadians(rxDegrees));
            logger.finer("Applying 3D X-axis rotation (without opacity): " + rxDegrees + " degrees (scaleY factor: " + rx3DScale + ")");
        }

        if (layer.transform().ry() != null) {
            // Y-axis rotation: scale X dimension by cos(ry) for flip effect
            double ryDegrees = layer.transform().ry().getValue(0, frame);
            ry3DScale = Math.cos(Math.toRadians(ryDegrees));
            logger.finer("Applying 3D Y-axis rotation (without opacity): " + ryDegrees + " degrees (scaleX factor: " + ry3DScale + ")");
        }

        // Step 4: Apply scale (combine regular scale with 3D rotation scale approximation)
        if (layer.transform().scale() != null) {
            double scaleX = layer.transform().scale().getValue(AnimatedValueType.X, frame) / 100.0;
            double scaleY = layer.transform().scale().getValue(AnimatedValueType.Y, frame) / 100.0;

            // Apply 3D rotation approximation scaling
            scaleX *= ry3DScale;
            scaleY *= rx3DScale;

            logger.finer("Scaling by (without opacity): " + scaleX + ", " + scaleY);

            // Check for zero or negative scale that would make content invisible
            if (scaleX <= 0 || scaleY <= 0) {
                logger.warning("WARNING: Zero or negative scale detected! scaleX=" + scaleX + ", scaleY=" + scaleY);
            }

            gc.scale(scaleX, scaleY);
        } else {
            logger.finer("No scale transform");
        }

        // Step 5: Apply anchor point offset (anchor point is the origin for transforms)
        // This must be done AFTER rotation and scale
        if (layer.transform().anchor() != null) {
            double anchorX = layer.transform().anchor().getValue(AnimatedValueType.X, frame);
            double anchorY = layer.transform().anchor().getValue(AnimatedValueType.Y, frame);
            logger.finer("Layer anchor (without opacity): " + anchorX + ", " + anchorY);
            // Translate by negative anchor to offset the coordinate system
            gc.translate(-anchorX, -anchorY);
        }

        logger.finer("=== LAYER TRANSFORM APPLIED (WITHOUT OPACITY) ===");
    }

    private void renderFrame(double frame) {
        render(frame);
    }

    public boolean isPlaying() {
        return isPlaying;
    }

    public double getCurrentFrame() {
        return currentFrameProperty.get();
    }

    public Animation getAnimation() {
        return animation;
    }

    public Color getBackgroundColor() {
        return backgroundColor;
    }

    public void setBackgroundColor(Color color) {
        this.backgroundColor = color;
        // Re-render current frame with new background
        renderFrame(currentFrameProperty.get());
    }
}