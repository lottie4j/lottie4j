package com.lottie4j.fxplayer;

import com.lottie4j.core.definition.LayerType;
import com.lottie4j.core.model.Animation;
import com.lottie4j.core.model.Asset;
import com.lottie4j.core.model.Layer;
import com.lottie4j.core.model.shape.BaseShape;
import com.lottie4j.core.model.shape.grouping.Group;
import com.lottie4j.core.model.shape.grouping.Transform;
import com.lottie4j.core.model.shape.modifier.TrimPath;
import com.lottie4j.fxplayer.renderer.layer.*;
import com.lottie4j.fxplayer.renderer.shape.ShapeGroupRenderer;
import com.lottie4j.fxplayer.renderer.shape.ShapeRenderer;
import com.lottie4j.fxplayer.renderer.shape.ShapeRendererFactory;
import com.lottie4j.fxplayer.util.FrameTiming;
import com.lottie4j.fxplayer.util.LayerActivity;
import javafx.animation.AnimationTimer;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * JavaFX Canvas component for playing Lottie animations
 */
public class LottiePlayer extends Canvas {

    private static final Logger logger = LoggerFactory.getLogger(LottiePlayer.class);

    private final Animation animation;
    private final GraphicsContext gc;
    private final DoubleProperty currentFrameProperty = new SimpleDoubleProperty(0);
    private final Map<Integer, Layer> layersByIndex;
    private final Map<String, Asset> assetsById;
    private final ImageRenderer imageRenderer = new ImageRenderer();
    private final TextRenderer textRenderer = new TextRenderer();
    private final TransformApplier transformApplier = new TransformApplier();
    private final PrecompRenderer precompRenderer = new PrecompRenderer(transformApplier, textRenderer, imageRenderer);
    private final ShapeRendererFactory shapeRendererFactory = new ShapeRendererFactory();
    private final ShapeGroupRenderer shapeGroupRenderer = new ShapeGroupRenderer(transformApplier, shapeRendererFactory);
    private final SolidColorRenderer solidColorRenderer = new SolidColorRenderer();
    private final EffectsRenderer effectsRenderer = new EffectsRenderer();
    private final MatteRenderer matteRenderer = new MatteRenderer();
    private AnimationTimer animationTimer;
    private long startTime;
    private boolean isPlaying = false;
    private boolean debug = false;
    private Color backgroundColor = Color.WHITE;
    private Set<Integer> visibleLayerIndices = null;  // null means all layers visible

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
        logger.info("Canvas size set to: {}x{}", getWidth(), getHeight());

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
        return FrameTiming.getInPoint(animation);
    }

    private int getOutPoint() {
        return FrameTiming.getOutPoint(animation);
    }

    private int getFramesPerSecond() {
        return FrameTiming.getFramesPerSecond(animation);
    }

    private int getAnimationWidth() {
        return FrameTiming.getAnimationWidth(animation);
    }

    private int getAnimationHeight() {
        return FrameTiming.getAnimationHeight(animation);
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
                render(newFrame, visibleLayerIndices);
            }
        };

        animationTimer.start();
    }

    public void playOnceFromStart() {
        playOnceFromStart(null);
    }

    public void playOnceFromStart(Consumer<File> onFinished) {
        if (isPlaying) {
            stop();
        }

        logger.info("Starting animation (play once from start)");
        isPlaying = true;

        // Start from the beginning
        seekToFrame(getInPoint());
        startTime = System.nanoTime();

        animationTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                double elapsedSeconds = (now - startTime) / 1_000_000_000.0;
                double totalFrames = getOutPoint() - getInPoint();
                double animationDuration = totalFrames / getFramesPerSecond();

                if (elapsedSeconds >= animationDuration) {
                    // Stop when animation completes
                    stop();
                    seekToFrame(getOutPoint());
                    if (onFinished != null) {
                        onFinished.accept(null);
                    }
                    return;
                }

                double newFrame = getInPoint() + (elapsedSeconds * getFramesPerSecond());
                currentFrameProperty.set(newFrame);
                render(newFrame, visibleLayerIndices);
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
        render(clampedFrame, visibleLayerIndices);
    }

    public DoubleProperty currentFrameProperty() {
        return currentFrameProperty;
    }

    public void render(double frame) {
        render(frame, null);
    }

    /**
     * Render a specific frame with optional layer filtering
     *
     * @param frame               Frame number to render
     * @param visibleLayerIndices Set of layer indices to render, null to render all layers
     */
    public void render(double frame, Set<Integer> visibleLayerIndices) {
        renderFrame(gc, frame, visibleLayerIndices);
    }

    /**
     * Render a frame to a specific graphics context with optional layer filtering.
     * Useful for generating thumbnails or rendering to custom canvases.
     *
     * @param gc                  Graphics context to render to
     * @param frame               Frame number to render
     * @param visibleLayerIndices Set of layer indices to render, null to render all layers
     */
    public void renderFrame(GraphicsContext gc, double frame, Set<Integer> visibleLayerIndices) {
        logger.debug("=== RENDER START ===");
        logger.debug("Canvas dimensions: {}x{}", getWidth(), getHeight());
        logger.debug("Rendering frame: {}", frame);
        if (visibleLayerIndices != null) {
            logger.debug("Visible layers: {}", visibleLayerIndices);
        }

        gc.clearRect(0, 0, gc.getCanvas().getWidth(), gc.getCanvas().getHeight());
        gc.setFill(backgroundColor);
        gc.fillRect(0, 0, gc.getCanvas().getWidth(), gc.getCanvas().getHeight());

        if (debug) {
            gc.setStroke(Color.RED);
            gc.setLineWidth(2);
            gc.strokeRect(1, 1, gc.getCanvas().getWidth() - 2, gc.getCanvas().getHeight() - 2);
        }

        if (animation == null) {
            logger.warn("No animation to render");
            return;
        }

        if (animation.layers() == null || animation.layers().isEmpty()) {
            logger.warn("No layers in animation");
            gc.setFill(Color.BLACK);
            gc.fillText("No layers found in animation", 10, 30);
            return;
        }

        logger.debug("Animation has {} layers", animation.layers().size());

        // Calculate scaling to fit canvas while maintaining aspect ratio
        double scaleX = gc.getCanvas().getWidth() / getAnimationWidth();
        double scaleY = gc.getCanvas().getHeight() / getAnimationHeight();
        double scale = Math.min(scaleX, scaleY);
        double offsetX = (gc.getCanvas().getWidth() - getAnimationWidth() * scale) / 2;
        double offsetY = (gc.getCanvas().getHeight() - getAnimationHeight() * scale) / 2;
        logger.debug("Animation size: {}x{}", getAnimationWidth(), getAnimationHeight());
        logger.debug("Canvas size: {}x{}", gc.getCanvas().getWidth(), gc.getCanvas().getHeight());
        logger.debug("Scale: {}, Offset: {}, {}", scale, offsetX, offsetY);

        gc.save();
        gc.translate(offsetX, offsetY);
        gc.scale(scale, scale);

        // Set default colors for debugging
        gc.setFill(Color.BLUE);

        // Render layers in reverse order with track matte support
        // Lottie renders layers bottom-to-top (last in array is drawn first, appears behind)
        for (int i = animation.layers().size() - 1; i >= 0; i--) {
            Layer layer = animation.layers().get(i);
            logger.debug("Processing layer: {}", layer.name());

            // Skip layer if not in visible set (when filtering is active)
            Integer layerIndex = layer.indexLayer() != null ? layer.indexLayer().intValue() : i;
            if (visibleLayerIndices != null && !visibleLayerIndices.contains(layerIndex)) {
                logger.debug("Skipping layer {} (filtered out)", layer.name());
                continue;
            }

            if (isLayerActiveAtFrame(layer, frame)) {
                // Check if this layer uses a track matte (has tt set)
                if (layer.matteMode() != null) {
                    // This layer uses a matte - the next layer (i+1) should be the matte source
                    if (i + 1 < animation.layers().size()) {
                        Layer matteSource = animation.layers().get(i + 1);
                        if (matteSource.matteTarget() != null && matteSource.matteTarget() == 1
                                && isLayerActiveAtFrame(matteSource, frame)) {
                            logger.debug("Rendering matted layer {}: {} with matte from {}", i, layer.name(), matteSource.name());
                            matteRenderer.renderLayerWithMatte(gc, layer, matteSource, frame,
                                    getAnimationWidth(), getAnimationHeight(), this::renderLayer);
                            continue;
                        }
                    }
                    logger.debug("Layer {} has matte mode but no valid matte source found", layer.name());
                }

                // Skip matte source layers (td=1) - they should not be rendered directly
                if (layer.matteTarget() != null && layer.matteTarget() == 1) {
                    logger.debug("Skipping matte source layer: {}", layer.name());
                    continue;
                }

                // Skip layers whose parent uses mattes or is a matte source
                if (shouldSkipDueToParent(layer)) {
                    logger.debug("Skipping layer with skipped parent: {}", layer.name());
                    continue;
                }

                logger.debug("Rendering layer: {}", layer.name());
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
        return LayerActivity.isActiveAtFrame(layer, frame);
    }

    private boolean shouldSkipDueToParent(Layer layer) {
        if (layer.indexParent() == null) {
            return false; // No parent
        }

        Layer parent = layersByIndex.get(layer.indexParent());
        if (parent == null) {
            return false; // Parent not found
        }

        // Check if parent should be skipped
        if (parent.matteMode() != null || (parent.matteTarget() != null && parent.matteTarget() == 1)) {
            return true;
        }

        // Recursively check parent's parent
        return shouldSkipDueToParent(parent);
    }

    private void renderLayer(GraphicsContext gc, Layer layer, double frame) {
        // Check for Gaussian Blur effect and render with blur if needed
        double blurRadius = effectsRenderer.getGaussianBlurRadius(layer, frame);
        if (blurRadius > 0.0) {
            effectsRenderer.renderLayerWithGaussianBlur(gc, layer, frame, blurRadius, this::renderLayerInternal);
            return;
        }

        renderLayerInternal(gc, layer, frame);
    }

    private void renderLayerInternal(GraphicsContext gc, Layer layer, double frame) {
        gc.save();

        // Check layer opacity - skip rendering if transparent
        if (layer.transform() != null && layer.transform().opacity() != null) {
            double opacity = layer.transform().opacity().getValue(0, frame);
            if (opacity <= 0) {
                logger.debug("Skipping layer {} - opacity is {}", layer.name(), opacity);
                gc.restore();
                return;
            }
        }

        // Apply parent transforms recursively
        applyParentTransforms(gc, layer, frame);

        // Apply this layer's transform
        applyLayerTransform(gc, layer, frame);

        // Handle precomposition layers
        if (layer.layerType() == LayerType.PRECOMPOSITION) {
            renderPrecompositionLayer(gc, layer, frame);
        }
        // Handle image layers
        else if (layer.layerType() == LayerType.IMAGE) {
            imageRenderer.render(gc, layer, animation);
        }
        // Handle solid color layers
        else if (layer.layerType() == LayerType.SOLD_COLOR) {
            solidColorRenderer.render(gc, layer, getAnimationWidth(), getAnimationHeight());
        }
        // Handle text layers
        else if (layer.layerType() == LayerType.TEXT) {
            textRenderer.render(gc, layer, frame);
        }
        // Skip rendering shapes for NULL layers (type 3), but transforms are still applied
        else if (layer.layerType() != LayerType.NULL) {
            // Render layer shapes
            if (layer.shapes() != null && !layer.shapes().isEmpty()) {
                logger.debug("Layer has {} shapes", layer.shapes().size());

                // First pass: collect any layer-level modifiers (like TrimPath)
                TrimPath layerTrimPath = null;
                for (BaseShape shape : layer.shapes()) {
                    if (shape instanceof TrimPath trim) {
                        layerTrimPath = trim;
                        logger.debug("Found layer-level TrimPath");
                    }
                }

                // Second pass: render shapes in REVERSE order (bottom-to-top: last shape renders first)
                for (int i = layer.shapes().size() - 1; i >= 0; i--) {
                    BaseShape shape = layer.shapes().get(i);

                    // Skip modifiers and styles - they're applied within groups/shapes
                    if (shape instanceof TrimPath) {
                        continue;
                    }

                    logger.debug("Shape class: {}, type: {}, shapeGroup: {}", shape.getClass().getSimpleName(), shape.shapeType(), shape.shapeType().shapeGroup());

                    switch (shape.shapeType().shapeGroup()) {
                        case GROUP -> shapeGroupRenderer.renderShapeTypeGroup(gc, shape, frame, layerTrimPath);
                        case SHAPE -> renderShapeTypeShape(shape, null, frame);
                        case STYLE -> {
                            // Skip - styles (Fill, Stroke, etc.) are handled within groups
                        }
                        default -> logger.warn("Unsupported shape type: {}", shape.shapeType().shapeGroup());
                    }
                }
            } else {
                logger.debug("Layer has no shapes");
            }
        } else {
            logger.debug("Skipping shape rendering for NULL layer: {}", layer.name());
        }

        gc.restore();
    }

    private void renderPrecompositionLayer(GraphicsContext gc, Layer layer, double frame) {
        precompRenderer.renderPrecompositionLayer(
                gc,
                layer,
                frame,
                assetsById,
                animation,
                this::isLayerActiveAtFrame,
                (solid_gc, solid_layer) -> solidColorRenderer.render(solid_gc, solid_layer, getAnimationWidth(), getAnimationHeight()),
                (shape, f, trimPath) -> shapeGroupRenderer.renderShapeTypeGroup(gc, shape, f, trimPath),
                this::renderShapeTypeShape
        );
    }

    private void applyParentTransforms(GraphicsContext gc, Layer layer, double frame) {
        if (layer.indexParent() == null) {
            return; // No parent
        }

        Layer parent = layersByIndex.get(layer.indexParent());
        if (parent == null) {
            logger.warn("Parent layer not found: {}", layer.indexParent());
            return;
        }

        // Recursively apply parent's parent transforms first
        applyParentTransforms(gc, parent, frame);

        // Then apply this parent's transform
        applyLayerTransform(gc, parent, frame);
    }

    private void renderShapeTypeShape(BaseShape shape, Group parentGroup, double frame) {
        ShapeRenderer renderer = shapeRendererFactory.getRenderer(shape);
        if (renderer == null) {
            logger.warn("No renderer found for shape: {}", shape.getClass().getSimpleName());
            return;
        }
        renderer.render(gc, shape, parentGroup, frame);
    }


    private void applyGroupTransform(GraphicsContext gc, Transform transform, double frame) {
        transformApplier.applyGroupTransform(gc, transform, frame);
    }

    private void applyLayerTransform(GraphicsContext gc, Layer layer, double frame) {
        transformApplier.applyLayerTransform(gc, layer, frame);
    }

    /**
     * Apply layer transform WITHOUT opacity - used for parent transforms where opacity should not inherit
     */
    private void applyLayerTransformWithoutOpacity(GraphicsContext gc, Layer layer, double frame) {
        transformApplier.applyLayerTransformWithoutOpacity(gc, layer, frame);
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
        render(currentFrameProperty.get(), visibleLayerIndices);
    }

    /**
     * Get the current visible layer indices.
     *
     * @return Set of visible layer indices, null if all layers are visible
     */
    public Set<Integer> getVisibleLayerIndices() {
        return visibleLayerIndices;
    }

    /**
     * Set which layers should be visible when rendering.
     *
     * @param visibleLayerIndices Set of layer indices to render, null to render all layers
     */
    public void setVisibleLayerIndices(Set<Integer> visibleLayerIndices) {
        this.visibleLayerIndices = visibleLayerIndices;
        // Re-render current frame with new visibility
        if (!isPlaying) {
            render(currentFrameProperty.get(), visibleLayerIndices);
        }
    }

}

