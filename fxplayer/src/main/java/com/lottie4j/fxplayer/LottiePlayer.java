package com.lottie4j.fxplayer;

import com.lottie4j.core.definition.EffectType;
import com.lottie4j.core.definition.LayerType;
import com.lottie4j.core.model.AnimatedValueType;
import com.lottie4j.core.model.Animation;
import com.lottie4j.core.model.Asset;
import com.lottie4j.core.model.Layer;
import com.lottie4j.core.model.shape.BaseShape;
import com.lottie4j.core.model.shape.grouping.Group;
import com.lottie4j.core.model.shape.grouping.Transform;
import com.lottie4j.core.model.shape.modifier.TrimPath;
import com.lottie4j.fxplayer.renderer.layer.ImageRenderer;
import com.lottie4j.fxplayer.renderer.layer.PrecompRenderer;
import com.lottie4j.fxplayer.renderer.layer.TextRenderer;
import com.lottie4j.fxplayer.renderer.layer.TransformApplier;
import com.lottie4j.fxplayer.renderer.shape.*;
import com.lottie4j.fxplayer.util.ColorParser;
import com.lottie4j.fxplayer.util.FrameTiming;
import com.lottie4j.fxplayer.util.LayerActivity;
import javafx.animation.AnimationTimer;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    private final ImageRenderer imageRenderer = new ImageRenderer();
    private final TextRenderer textRenderer = new TextRenderer();
    private final TransformApplier transformApplier = new TransformApplier();
    private final PrecompRenderer precompRenderer = new PrecompRenderer(transformApplier, textRenderer, imageRenderer);
    private final ShapeRenderer pathRenderer = new PathRenderer();
    private final ShapeRenderer ellipseRenderer = new EllipseRenderer();
    private final ShapeRenderer rectangleRenderer = new RectangleRenderer();
    private final ShapeRenderer polystarRenderer = new PolystarRenderer();
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
        logger.finer("=== RENDER START ===");
        logger.finer("Canvas dimensions: " + getWidth() + "x" + getHeight());
        logger.finer("Rendering frame: " + frame);
        if (visibleLayerIndices != null) {
            logger.finer("Visible layers: " + visibleLayerIndices);
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

        // Render layers in reverse order with track matte support
        // Lottie renders layers bottom-to-top (last in array is drawn first, appears behind)
        for (int i = animation.layers().size() - 1; i >= 0; i--) {
            Layer layer = animation.layers().get(i);
            logger.finer("Processing layer: " + layer.name());

            // Skip layer if not in visible set (when filtering is active)
            Integer layerIndex = layer.indexLayer() != null ? layer.indexLayer().intValue() : i;
            if (visibleLayerIndices != null && !visibleLayerIndices.contains(layerIndex)) {
                logger.finer("Skipping layer " + layer.name() + " (filtered out)");
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
                            logger.fine("Rendering matted layer " + i + ": " + layer.name() + " with matte from " + matteSource.name());
                            renderLayerWithMatteSimple(gc, layer, matteSource, frame);
                            continue;
                        }
                    }
                    logger.warning("Layer " + layer.name() + " has matte mode but no valid matte source found");
                }

                // Skip matte source layers (td=1) - they should not be rendered directly
                if (layer.matteTarget() != null && layer.matteTarget() == 1) {
                    logger.fine("Skipping matte source layer: " + layer.name());
                    continue;
                }

                // Skip layers whose parent uses mattes or is a matte source
                if (shouldSkipDueToParent(layer)) {
                    logger.fine("Skipping layer with skipped parent: " + layer.name());
                    continue;
                }

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
        double blurRadius = getGaussianBlurRadius(layer, frame);
        if (blurRadius > 0.0) {
            renderLayerWithGaussianBlur(gc, layer, frame, blurRadius);
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
                logger.fine("Skipping layer " + layer.name() + " - opacity is " + opacity);
                gc.restore();
                return;
            }
        }

        // Log transform values for Tick layer at frame 88
        if ("Tick".equals(layer.name()) && Math.abs(frame - 88.0) < 0.5) {
            if (layer.transform() != null) {
                if (layer.transform().position() != null) {
                    double x = layer.transform().position().getValue(AnimatedValueType.X, frame);
                    double y = layer.transform().position().getValue(AnimatedValueType.Y, frame);
                    logger.warning("Tick layer at frame " + frame + " - position: (" + x + ", " + y + ")");
                }
                if (layer.transform().rotation() != null) {
                    double rot = layer.transform().rotation().getValue(0, frame);
                    logger.warning("Tick layer at frame " + frame + " - rotation: " + rot);
                }
                if (layer.transform().scale() != null) {
                    double sx = layer.transform().scale().getValue(AnimatedValueType.X, frame);
                    double sy = layer.transform().scale().getValue(AnimatedValueType.Y, frame);
                    logger.warning("Tick layer at frame " + frame + " - scale: (" + sx + ", " + sy + ")");
                }
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
            renderSolidColorLayer(gc, layer);
        }
        // Handle text layers
        else if (layer.layerType() == LayerType.TEXT) {
            textRenderer.render(gc, layer, frame);
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

    private void renderLayerWithMatteSimple(GraphicsContext gc, Layer matteUser, Layer matteSource, double frame) {
        // Use JavaFX BlendMode to simulate alpha masking
        // For ALPHA matte mode: content should only show where matte is opaque

        com.lottie4j.core.definition.MatteMode matteMode = matteUser.matteMode();
        logger.fine("Rendering with blend mode matte: " + matteMode);

        // First render the content layer normally
        renderLayer(gc, matteUser, frame);

        // Then apply the matte using blend mode
        // MULTIPLY blend mode: content * matte (darkens where matte is darker)
        // For now, just skip the matte to see content
        // TODO: Implement proper blend mode masking
    }

    private void renderLayerWithMatte(GraphicsContext gc, Layer matteUser, Layer matteSource, double frame) {
        long startTime = System.nanoTime();

        // Get current canvas dimensions
        // OPTIMIZATION: Use smaller canvas for better performance
        // In a production implementation, we should calculate the bounding box
        // For now, use half size which still maintains quality
        double width = getAnimationWidth();
        double height = getAnimationHeight();

        // Use full canvas size for now (optimize later once working)
        double matteWidth = width;
        double matteHeight = height;
        double scale = 1.0;

        // Create off-screen canvases for matte and content
        Canvas matteCanvas = new Canvas(matteWidth, matteHeight);
        Canvas contentCanvas = new Canvas(matteWidth, matteHeight);
        GraphicsContext matteGc = matteCanvas.getGraphicsContext2D();
        GraphicsContext contentGc = contentCanvas.getGraphicsContext2D();

        // Don't fill with anything - let canvas be transparent by default
        // The shapes will render with their own fills/strokes

        // Scale the rendering
        matteGc.scale(scale, scale);
        contentGc.scale(scale, scale);

        // Render the matte source to the matte canvas (WITH parent transforms for proper positioning)
        logger.fine("Rendering matte source layer: " + matteSource.name() + " (shapes: " + (matteSource.shapes() != null ? matteSource.shapes().size() : 0) + ", parent: " + matteSource.indexParent() + ")");
        renderLayer(matteGc, matteSource, frame);

        // Render the content layer to the content canvas (WITH parent transforms for proper positioning)
        logger.fine("Rendering matte user layer: " + matteUser.name() + " (shapes: " + (matteUser.shapes() != null ? matteUser.shapes().size() : 0) + ", parent: " + matteUser.indexParent() + ")");
        renderLayer(contentGc, matteUser, frame);

        // Get the matte mode
        com.lottie4j.core.definition.MatteMode matteMode = matteUser.matteMode();
        logger.fine("Applying matte mode: " + matteMode);

        // Snapshot both canvases with transparent background
        SnapshotParameters params = new SnapshotParameters();
        params.setFill(Color.TRANSPARENT);
        WritableImage matteImage = matteCanvas.snapshot(params, null);
        WritableImage contentImage = contentCanvas.snapshot(params, null);

        // DEBUG: Check if content and matte have actual pixels
        logger.fine("Matte image size: " + matteImage.getWidth() + "x" + matteImage.getHeight());
        logger.fine("Content image size: " + contentImage.getWidth() + "x" + contentImage.getHeight());

        // Sample multiple pixels to see what we're working with
        PixelReader matteReader = matteImage.getPixelReader();
        PixelReader contentReader = contentImage.getPixelReader();

        // Sample at different positions
        Color matteCenter = matteReader.getColor((int) matteImage.getWidth() / 2, (int) matteImage.getHeight() / 2);
        Color contentCenter = contentReader.getColor((int) contentImage.getWidth() / 2, (int) contentImage.getHeight() / 2);
        Color matte270 = matteReader.getColor(270, 270);
        Color content270 = contentReader.getColor(270, 270);

        logger.info("  Matte center [" + ((int) matteImage.getWidth() / 2) + "," + ((int) matteImage.getHeight() / 2) + "]: " + matteCenter);
        logger.info("  Content center [" + ((int) contentImage.getWidth() / 2) + "," + ((int) contentImage.getHeight() / 2) + "]: " + contentCenter);
        logger.info("  Matte at [270,270]: " + matte270);
        logger.info("  Content at [270,270]: " + content270);

        // Apply matte composition
        WritableImage result = applyMatte(contentImage, matteImage, matteMode);

        // IMPORTANT: Save the current composite operation and use SRC_OVER
        // This ensures transparent pixels in the matted result don't obscure
        // content below (which was showing as white background)
        gc.save();
        gc.setGlobalAlpha(1.0); // Full opacity for the matted result itself

        // Draw the result to the main canvas, scaling back up if needed
        // The SRC_OVER blend mode (default) will properly composite transparent pixels
        gc.drawImage(result, 0, 0, width, height);

        gc.restore();

        long endTime = System.nanoTime();
        logger.fine("Matte rendering took: " + ((endTime - startTime) / 1_000_000) + "ms");
    }

    private WritableImage applyMatte(WritableImage content, WritableImage matte, com.lottie4j.core.definition.MatteMode matteMode) {
        int width = (int) content.getWidth();
        int height = (int) content.getHeight();

        WritableImage result = new WritableImage(width, height);
        PixelReader contentReader = content.getPixelReader();
        PixelReader matteReader = matte.getPixelReader();
        PixelWriter resultWriter = result.getPixelWriter();

        // Use pixel buffer for better performance
        int[] contentBuffer = new int[width * height];
        int[] matteBuffer = new int[width * height];
        int[] resultBuffer = new int[width * height];

        contentReader.getPixels(0, 0, width, height, javafx.scene.image.PixelFormat.getIntArgbInstance(), contentBuffer, 0, width);
        matteReader.getPixels(0, 0, width, height, javafx.scene.image.PixelFormat.getIntArgbInstance(), matteBuffer, 0, width);

        for (int i = 0; i < contentBuffer.length; i++) {
            int contentPixel = contentBuffer[i];
            int mattePixel = matteBuffer[i];

            int cA = (contentPixel >> 24) & 0xFF;
            int cR = (contentPixel >> 16) & 0xFF;
            int cG = (contentPixel >> 8) & 0xFF;
            int cB = contentPixel & 0xFF;

            int mA = (mattePixel >> 24) & 0xFF;
            int mR = (mattePixel >> 16) & 0xFF;
            int mG = (mattePixel >> 8) & 0xFF;
            int mB = mattePixel & 0xFF;

            int resultA;
            switch (matteMode) {
                case ALPHA:
                    // Use matte's alpha to mask content
                    // The content's RGB stays, but alpha is multiplied by matte's alpha
                    resultA = (cA * mA) / 255;
                    break;

                case INVERTED_ALPHA:
                    // Use inverted matte's alpha to mask content
                    resultA = (cA * (255 - mA)) / 255;
                    break;

                case LUMA:
                    // Use matte's luminance as alpha
                    int luma = (299 * mR + 587 * mG + 114 * mB) / 1000;
                    resultA = (cA * luma) / 255;
                    break;

                case INVERTED_LUMA:
                    // Use inverted matte's luminance as alpha
                    int lumaInv = (299 * mR + 587 * mG + 114 * mB) / 1000;
                    resultA = (cA * (255 - lumaInv)) / 255;
                    break;

                default:
                    resultA = cA;
            }

            // IMPORTANT: Keep content's RGB, only modify alpha
            // If resultA is 0, make the pixel fully transparent
            if (resultA == 0) {
                resultBuffer[i] = 0; // Fully transparent
            } else {
                resultBuffer[i] = (resultA << 24) | (cR << 16) | (cG << 8) | cB;
            }
        }

        resultWriter.setPixels(0, 0, width, height, javafx.scene.image.PixelFormat.getIntArgbInstance(), resultBuffer, 0, width);
        return result;
    }

    private void renderPrecompositionLayer(GraphicsContext gc, Layer layer, double frame) {
        precompRenderer.renderPrecompositionLayer(
                gc,
                layer,
                frame,
                assetsById,
                animation,
                this::isLayerActiveAtFrame,
                this::renderSolidColorLayer,
                this::renderShapeTypeGroup,
                this::renderShapeTypeShape
        );
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

    private ShapeRenderer getShapeRenderer(BaseShape shape) {
        if (shape instanceof com.lottie4j.core.model.shape.shape.Path) {
            return pathRenderer;
        }
        if (shape instanceof com.lottie4j.core.model.shape.shape.Ellipse) {
            return ellipseRenderer;
        }
        if (shape instanceof com.lottie4j.core.model.shape.shape.Rectangle) {
            return rectangleRenderer;
        }
        if (shape instanceof com.lottie4j.core.model.shape.shape.Polystar) {
            return polystarRenderer;
        }
        return null;
    }

    private void renderShapeTypeShape(BaseShape shape, Group parentGroup, double frame) {
        ShapeRenderer renderer = getShapeRenderer(shape);
        if (renderer == null) {
            logger.warning("No renderer found for shape: " + shape.getClass().getSimpleName());
            return;
        }
        renderer.render(gc, shape, parentGroup, frame);
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

            // Check group opacity - skip rendering if transparent
            if (groupTransform != null) {
                if (groupTransform.opacity() != null) {
                    double opacity = groupTransform.opacity().getValue(0, frame);
                    if (opacity <= 0) {
                        logger.fine("Skipping group " + group.name() + " - opacity is " + opacity);
                        gc.restore();
                        return;
                    }
                }
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

        List<java.util.List<Double>> vertices = bezierDef.vertices();
        List<java.util.List<Double>> tangentsIn = bezierDef.tangentsIn();
        List<java.util.List<Double>> tangentsOut = bezierDef.tangentsOut();

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

    private void renderSolidColorLayer(GraphicsContext gc, Layer layer) {
        if (layer.solidColor() == null) {
            logger.warning("Solid color layer has no color: " + layer.name());
            return;
        }

        // Parse the solid color (format: "RRGGBBAA" or similar hex string)
        Color color = parseColorString(layer.solidColor());
        if (color == null) {
            logger.warning("Could not parse solid color: " + layer.solidColor());
            return;
        }

        // Get the layer dimensions if specified, otherwise use animation dimensions
        double width = layer.width() != null ? layer.width() : getAnimationWidth();
        double height = layer.height() != null ? layer.height() : getAnimationHeight();

        // Fill from negative coordinates to ensure full coverage in precompositions
        // This is important because solid color should fill the entire composition
        gc.setFill(color);
        gc.fillRect(-width, -height, width * 3, height * 3);

        logger.fine("Rendered solid color layer: " + layer.name() + " color=" + color + " size=" + width + "x" + height);
    }

    private Color parseColorString(String colorStr) {
        return ColorParser.parse(colorStr, logger);
    }

    private double getGaussianBlurRadius(Layer layer, double frame) {
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
                            logger.finer("Gaussian Blur: raw=" + rawBlur + " scaled=" + blurRadius + " for layer " + layer.name() + " at frame " + frame);
                        }
                        break;
                    }
                }
            }
            break;
        }

        return blurRadius;
    }

    private void renderLayerWithGaussianBlur(GraphicsContext gc, Layer layer, double frame, double blurRadius) {
        gc.save();

        // Apply the Gaussian blur effect
        GaussianBlur blur = new GaussianBlur(blurRadius);
        gc.setEffect(blur);

        // Render the layer normally
        renderLayerInternal(gc, layer, frame);

        gc.setEffect(null);
        gc.restore();
    }
}

