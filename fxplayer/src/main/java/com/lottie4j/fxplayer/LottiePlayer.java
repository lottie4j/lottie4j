package com.lottie4j.fxplayer;

import com.lottie4j.core.definition.LayerType;
import com.lottie4j.core.model.animation.Animation;
import com.lottie4j.core.model.animation.Marker;
import com.lottie4j.core.model.asset.Asset;
import com.lottie4j.core.model.layer.Layer;
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
import com.lottie4j.fxplayer.util.OffscreenRenderer;
import javafx.animation.AnimationTimer;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;
import java.util.function.Consumer;

/**
 * JavaFX Canvas component for playing Lottie animations
 */
public class LottiePlayer extends Canvas {

    private static final Logger logger = LoggerFactory.getLogger(LottiePlayer.class);
    private static final double DEBUG_FPS_SMOOTHING = 0.2;
    private static final double MIN_ADAPTIVE_OFFSCREEN_SCALE = 0.45;
    private static final double MAX_ADAPTIVE_OFFSCREEN_SCALE = 1.0;
    private static final double ADAPTIVE_SCALE_DOWN_STEP = 0.06;
    private static final double ADAPTIVE_SCALE_UP_STEP = 0.02;
    private final Animation animation;
    private final GraphicsContext gc;
    private final DoubleProperty currentFrameProperty = new SimpleDoubleProperty(0);
    private final Map<Integer, Layer> layersByIndex;
    private final Map<String, Asset> assetsById;
    private final Map<Layer, Boolean> skipDueToParentCache = new IdentityHashMap<>();
    private final Map<Layer, List<Layer>> parentChainCache = new IdentityHashMap<>();
    private final Map<Layer, TrimPath> layerTrimPathCache = new IdentityHashMap<>();
    private final ImageRenderer imageRenderer = new ImageRenderer();
    private final TextRenderer textRenderer = new TextRenderer();
    private final TransformApplier transformApplier = new TransformApplier();
    private final PrecompRenderer precompRenderer = new PrecompRenderer(transformApplier, textRenderer, imageRenderer);
    private final ShapeRendererFactory shapeRendererFactory = new ShapeRendererFactory();
    private final ShapeGroupRenderer shapeGroupRenderer = new ShapeGroupRenderer(transformApplier, shapeRendererFactory);
    private final SolidColorRenderer solidColorRenderer = new SolidColorRenderer();
    private final EffectsRenderer effectsRenderer = new EffectsRenderer();
    private final MatteRenderer matteRenderer = new MatteRenderer();
    private final int baseRenderWidth;
    private final int baseRenderHeight;
    private AnimationTimer animationTimer;
    private long startTime;
    private boolean isPlaying = false;
    private boolean debug = false;
    private Marker loopStart = null;
    private Marker loopEnd = null;
    private int cropTop = 0;
    private int cropRight = 0;
    private int cropBottom = 0;
    private int cropLeft = 0;
    private Color backgroundColor = Color.WHITE;
    private Set<Integer> visibleLayerIndices = null;  // null means all layers visible
    private long lastDebugRenderNanos = 0L;
    private double measuredPlaybackFps = 0.0;
    private double adaptiveOffscreenScale = 1.0;
    private double smoothedRenderMillis = 16.67;
    private boolean adaptiveOffscreenScalingEnabled = true;
    private boolean invertColorsEnabled = false;

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
        this.baseRenderWidth = Math.max(1, width);
        this.baseRenderHeight = Math.max(1, height);

        // Set canvas size to specified dimensions
        setWidth(this.baseRenderWidth);
        setHeight(this.baseRenderHeight);
        logger.debug("Canvas size set to: {}x{}", getWidth(), getHeight());

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

        buildRenderCaches();

        // Initial render
        renderFrame(getInPoint());
    }

    /**
     * Pre-computes static layer metadata so the hot render loop avoids repeated tree traversal.
     */
    private void buildRenderCaches() {
        if (animation.layers() == null) {
            return;
        }

        skipDueToParentCache.clear();
        parentChainCache.clear();
        layerTrimPathCache.clear();

        precompRenderer.clearRenderCaches();
        precompRenderer.warmUpRenderCaches(assetsById);

        for (Layer layer : animation.layers()) {
            layerTrimPathCache.put(layer, resolveLayerTrimPath(layer));
            skipDueToParentCache.put(layer, computeSkipDueToParent(layer));
            parentChainCache.put(layer, buildParentChain(layer));
        }
    }

    private TrimPath resolveLayerTrimPath(Layer layer) {
        if (layer.shapes() == null) {
            return null;
        }
        for (BaseShape shape : layer.shapes()) {
            if (shape instanceof TrimPath trimPath) {
                return trimPath;
            }
        }
        return null;
    }

    private boolean computeSkipDueToParent(Layer layer) {
        Integer parentIndex = layer.indexParent();
        int guard = 0;
        while (parentIndex != null && guard++ < layersByIndex.size()) {
            Layer parent = layersByIndex.get(parentIndex);
            if (parent == null) {
                return false;
            }
            if (parent.matteMode() != null || (parent.matteTarget() != null && parent.matteTarget() == 1)) {
                return true;
            }
            parentIndex = parent.indexParent();
        }
        return false;
    }

    private List<Layer> buildParentChain(Layer layer) {
        List<Layer> chain = new ArrayList<>();
        Integer parentIndex = layer.indexParent();
        int guard = 0;
        while (parentIndex != null && guard++ < layersByIndex.size()) {
            Layer parent = layersByIndex.get(parentIndex);
            if (parent == null) {
                break;
            }
            chain.add(parent);
            parentIndex = parent.indexParent();
        }
        Collections.reverse(chain);
        return chain;
    }

    /**
     * Gets the starting frame index for playback.
     *
     * @return animation in-point frame
     */
    private int getInPoint() {
        return FrameTiming.getInPoint(animation);
    }

    /**
     * Gets the ending frame boundary for playback (exclusive).
     *
     * @return animation out-point frame boundary
     */
    private int getOutPointExclusive() {
        return FrameTiming.getOutPointExclusive(animation);
    }

    /**
     * Gets the last renderable frame (inclusive).
     */
    private int getLastRenderableFrame() {
        return FrameTiming.getLastRenderableFrame(animation);
    }

    /**
     * Gets the animation playback frame rate.
     *
     * @return frames per second
     */
    private int getFramesPerSecond() {
        return FrameTiming.getFramesPerSecond(animation);
    }

    /**
     * Returns the effective frame at which the loop restarts.
     * Uses the {@code loopStart} marker when set, otherwise falls back to {@link #getInPoint()}.
     *
     * @return loop-restart frame number
     */
    private double getEffectiveLoopStartFrame() {
        if (loopStart != null && loopStart.time() != null) {
            return loopStart.time();  // tm is stored as a frame number, not seconds
        }
        return getInPoint();
    }

    /**
     * Returns the effective frame at which the loop boundary lies.
     * Uses the {@code loopEnd} marker when set, otherwise falls back to {@link #getOutPointExclusive()}.
     *
     * @return loop-end frame number (exclusive boundary)
     */
    private double getEffectiveLoopEndFrame() {
        if (loopEnd != null && loopEnd.time() != null) {
            return loopEnd.time();  // tm is stored as a frame number, not seconds
        }
        return getOutPointExclusive();
    }

    /**
     * Sets the loop markers that control the loop region during playback.
     * <p>
     * When the animation reaches {@code loopEnd} it will restart from {@code loopStart}
     * instead of frame 0. Pass {@code null} for either marker to use the default
     * in-point / out-point boundary.
     *
     * @param loopStart marker for the restart frame (null = use animation in-point)
     * @param loopEnd   marker for the loop boundary frame (null = use animation out-point)
     */
    public void setLoopMarkers(Marker loopStart, Marker loopEnd) {
        this.loopStart = loopStart;
        this.loopEnd = loopEnd;
    }

    /**
     * Returns the current loop-start marker, or {@code null} if none is set.
     *
     * @return loop-start {@link Marker}
     */
    public Marker getLoopStart() {
        return loopStart;
    }

    /**
     * Returns the current loop-end marker, or {@code null} if none is set.
     *
     * @return loop-end {@link Marker}
     */
    public Marker getLoopEnd() {
        return loopEnd;
    }

    /**
     * Gets the animation composition width.
     *
     * @return animation width in pixels
     */
    private int getAnimationWidth() {
        return FrameTiming.getAnimationWidth(animation);
    }

    /**
     * Gets the animation composition height.
     *
     * @return animation height in pixels
     */
    private int getAnimationHeight() {
        return FrameTiming.getAnimationHeight(animation);
    }

    /**
     * Returns the cropped viewport width in composition coordinates.
     */
    private int getViewportWidth() {
        return Math.max(1, getAnimationWidth() - cropLeft - cropRight);
    }

    /**
     * Returns the cropped viewport height in composition coordinates.
     */
    private int getViewportHeight() {
        return Math.max(1, getAnimationHeight() - cropTop - cropBottom);
    }

    /**
     * Returns the current viewport horizontal origin in composition coordinates.
     */
    private int getViewportX() {
        return cropLeft;
    }

    /**
     * Returns the current viewport vertical origin in composition coordinates.
     */
    private int getViewportY() {
        return cropTop;
    }

    /**
     * Returns the current fit scale between the canvas and cropped viewport.
     */
    private double getCurrentViewportFitScale() {
        double sx = getWidth() / Math.max(1.0, getViewportWidth());
        double sy = getHeight() / Math.max(1.0, getViewportHeight());
        return Math.max(0.01, Math.min(sx, sy));
    }

    /**
     * Applies a canvas size matching the cropped viewport at the provided scale.
     */
    private void resizeCanvasToViewport(double fitScale) {
        int targetWidth = Math.max(1, (int) Math.round(getViewportWidth() * fitScale));
        int targetHeight = Math.max(1, (int) Math.round(getViewportHeight() * fitScale));
        setWidth(targetWidth);
        setHeight(targetHeight);
    }

    /**
     * Crops the rendered view by insets from the composition edges.
     * Rendering still uses full composition coordinates; only the final viewport is clipped.
     * The canvas is resized to the resulting cropped viewport.
     * <p>
     * Insets are ordered as {@code top, right, bottom, left}.
     * Values are clamped to keep at least a 1x1 visible viewport.
     * <p>
     * Example:
     * <pre>{@code
     * LottiePlayer player = new LottiePlayer(animation);
     * player.crop(40, 20, 0, 10); // hide 40px top, 20px right, 0px bottom, 10px left
     * player.play();
     * }</pre>
     *
     * @param top    pixels to hide from the top edge
     * @param right  pixels to hide from the right edge
     * @param bottom pixels to hide from the bottom edge
     * @param left   pixels to hide from the left edge
     */
    public void crop(int top, int right, int bottom, int left) {
        double fitScale = getCurrentViewportFitScale();

        int maxHorizontalInset = Math.max(0, getAnimationWidth() - 1);
        int maxVerticalInset = Math.max(0, getAnimationHeight() - 1);

        int clampedLeft = Math.clamp(left, 0, maxHorizontalInset);
        int clampedRight = Math.clamp(right, 0, maxHorizontalInset - clampedLeft);
        int clampedTop = Math.clamp(top, 0, maxVerticalInset);
        int clampedBottom = Math.clamp(bottom, 0, maxVerticalInset - clampedTop);

        this.cropTop = clampedTop;
        this.cropRight = clampedRight;
        this.cropBottom = clampedBottom;
        this.cropLeft = clampedLeft;

        resizeCanvasToViewport(fitScale);
        render(currentFrameProperty.get(), visibleLayerIndices);
    }

    /**
     * Clears any active crop and restores full composition viewport.
     * <p>
     * Use this after {@link #crop(int, int, int, int)} to return to full-frame rendering.
     */
    public void clearCrop() {
        crop(0, 0, 0, 0);
    }

    /**
     * Sets loop markers and starts playing the animation.
     * When the animation reaches {@code loopEnd} it restarts from {@code loopStart}.
     *
     * @param loopStart marker for the restart frame (null = use animation in-point)
     * @param loopEnd   marker for the loop boundary frame (null = use animation out-point)
     */
    public void play(Marker loopStart, Marker loopEnd) {
        setLoopMarkers(loopStart, loopEnd);
        play();
    }

    /**
     * Starts playing the animation from the current frame position.
     * Animation will loop continuously until stopped.
     */
    public void play() {
        if (isPlaying) return;

        logger.info("Starting animation");
        isPlaying = true;
        lastDebugRenderNanos = 0L;

        // Calculate the time offset based on current frame position
        double currentFrame = currentFrameProperty.get();
        double frameOffset = currentFrame - getInPoint();
        double timeOffset = frameOffset / getFramesPerSecond();
        startTime = System.nanoTime() - (long) (timeOffset * 1_000_000_000.0);

        animationTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                double elapsedSeconds = (now - startTime) / 1_000_000_000.0;
                double loopEndFrame = getEffectiveLoopEndFrame();
                double loopStartFrame = getEffectiveLoopStartFrame();
                double totalFrames = loopEndFrame - getInPoint();
                double animationDuration = totalFrames / getFramesPerSecond();

                if (elapsedSeconds >= animationDuration) {
                    // Loop: restart from loopStartFrame (= inPoint when no loopStart marker is set)
                    double loopElapsedSeconds = (loopStartFrame - getInPoint()) / (double) getFramesPerSecond();
                    startTime = now - (long) (loopElapsedSeconds * 1_000_000_000.0);
                    elapsedSeconds = loopElapsedSeconds;
                }

                double newFrame = getInPoint() + (elapsedSeconds * getFramesPerSecond());
                newFrame = Math.min(getLastRenderableFrame(), newFrame);
                currentFrameProperty.set(newFrame);
                render(newFrame, visibleLayerIndices);
            }
        };

        animationTimer.start();
    }


    /**
     * Plays the animation once from the beginning and stops.
     */
    public void playOnceFromStart() {
        playOnceFromStart(null);
    }

    /**
     * Plays the animation once from the beginning and executes a callback when finished.
     *
     * @param onFinished callback invoked when playback completes
     */
    public void playOnceFromStart(Consumer<File> onFinished) {
        if (isPlaying) {
            stop();
        }

        logger.info("Starting animation (play once from start)");
        isPlaying = true;
        lastDebugRenderNanos = 0L;

        // Start from the beginning
        seekToFrame(getInPoint());
        startTime = System.nanoTime();

        animationTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                double elapsedSeconds = (now - startTime) / 1_000_000_000.0;
                double totalFrames = getOutPointExclusive() - getInPoint();
                double animationDuration = totalFrames / getFramesPerSecond();

                if (elapsedSeconds >= animationDuration) {
                    // Stop when animation completes
                    stop();
                    seekToFrame(getLastRenderableFrame());
                    if (onFinished != null) {
                        onFinished.accept(null);
                    }
                    return;
                }

                double newFrame = getInPoint() + (elapsedSeconds * getFramesPerSecond());
                newFrame = Math.min(getLastRenderableFrame(), newFrame);
                currentFrameProperty.set(newFrame);
                render(newFrame, visibleLayerIndices);
            }
        };

        animationTimer.start();
    }

    /**
     * Stops playback if the animation is currently playing.
     */
    public void stop() {
        if (animationTimer != null) {
            animationTimer.stop();
        }
        logger.info("Animation stopped");
        isPlaying = false;
        lastDebugRenderNanos = 0L;
    }

    /**
     * Seeks to a specific frame and renders it.
     *
     * @param frame frame number to seek to (will be clamped to valid range)
     */
    public void seekToFrame(double frame) {
        double clampedFrame = Math.max(getInPoint(), Math.min(getLastRenderableFrame(), frame));
        currentFrameProperty.set(clampedFrame);
        render(clampedFrame, visibleLayerIndices);
    }

    /**
     * Gets the property tracking the current frame position.
     *
     * @return observable double property for current frame
     */
    public DoubleProperty currentFrameProperty() {
        return currentFrameProperty;
    }

    /**
     * Renders the specified frame, showing all layers.
     *
     * @param frame frame number to render
     */
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
        if (visibleLayerIndices != null) {
            logger.debug("Visible layers: {}", visibleLayerIndices);
        }

        long renderStartNanos = System.nanoTime();

        gc.clearRect(0, 0, gc.getCanvas().getWidth(), gc.getCanvas().getHeight());
        fillBackground(gc);

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
        double viewportWidth = getViewportWidth();
        double viewportHeight = getViewportHeight();
        double viewportX = getViewportX();
        double viewportY = getViewportY();

        double scaleX = gc.getCanvas().getWidth() / viewportWidth;
        double scaleY = gc.getCanvas().getHeight() / viewportHeight;
        double scale = Math.min(scaleX, scaleY);
        double offsetX = (gc.getCanvas().getWidth() - viewportWidth * scale) / 2;
        double offsetY = (gc.getCanvas().getHeight() - viewportHeight * scale) / 2;
        double renderResolutionScale = resolveRenderResolutionScale(gc);
        logger.debug("Animation/Canvas size: {}x{}/{}x{}, Scale: {}, Offset: {}/{}, renderResolutionScale={}",
                getAnimationWidth(), getAnimationHeight(),
                gc.getCanvas().getWidth(), gc.getCanvas().getHeight(),
                scale, offsetX, offsetY, renderResolutionScale);

        if (invertColorsEnabled) {
            renderInvertedAnimationContent(gc, frame, visibleLayerIndices,
                    viewportX, viewportY, viewportWidth, viewportHeight,
                    scale, offsetX, offsetY, renderResolutionScale);
        } else {
            renderAnimationContent(gc, frame, visibleLayerIndices,
                    viewportX, viewportY, viewportWidth, viewportHeight,
                    scale, offsetX, offsetY, renderResolutionScale);
        }

        long renderEndNanos = System.nanoTime();
        updateAdaptiveOffscreenScale((renderEndNanos - renderStartNanos) / 1_000_000.0);

        if (debug) {
            long nowNanos = System.nanoTime();
            if (lastDebugRenderNanos > 0L) {
                double seconds = (nowNanos - lastDebugRenderNanos) / 1_000_000_000.0;
                if (seconds > 0.0) {
                    double instantaneousFps = 1.0 / seconds;
                    if (measuredPlaybackFps == 0.0) {
                        measuredPlaybackFps = instantaneousFps;
                    } else {
                        measuredPlaybackFps = measuredPlaybackFps * (1.0 - DEBUG_FPS_SMOOTHING)
                                + instantaneousFps * DEBUG_FPS_SMOOTHING;
                    }
                }
            }
            lastDebugRenderNanos = nowNanos;

            drawDebugOverlay(gc, frame, scale);
        }

    }

    /**
     * Paints the player background without affecting later layer compositing.
     */
    private void fillBackground(GraphicsContext graphicsContext) {
        graphicsContext.save();
        graphicsContext.setFill(backgroundColor);
        graphicsContext.fillRect(0, 0, graphicsContext.getCanvas().getWidth(), graphicsContext.getCanvas().getHeight());
        graphicsContext.restore();
    }

    /**
     * Renders animation content to the target graphics context without painting the background.
     */
    private void renderAnimationContent(GraphicsContext graphicsContext,
                                        double frame,
                                        Set<Integer> visibleLayerIndices,
                                        double viewportX,
                                        double viewportY,
                                        double viewportWidth,
                                        double viewportHeight,
                                        double scale,
                                        double offsetX,
                                        double offsetY,
                                        double renderResolutionScale) {
        graphicsContext.save();
        graphicsContext.translate(offsetX, offsetY);
        graphicsContext.scale(scale, scale);
        graphicsContext.translate(-viewportX, -viewportY);

        // Clip to animation bounds - ensures blur and other effects don't extend beyond composition
        // This matches JavaScript/Lottie-web behavior where effects are clipped to canvas bounds
        graphicsContext.beginPath();
        graphicsContext.rect(viewportX, viewportY, viewportWidth, viewportHeight);
        graphicsContext.clip();

        // Set default colors for debugging
        graphicsContext.setFill(Color.BLUE);

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
                            matteRenderer.renderLayerWithMatte(graphicsContext,
                                    layer,
                                    matteSource,
                                    frame,
                                    getAnimationWidth(),
                                    getAnimationHeight(),
                                    renderResolutionScale,
                                    this::renderLayer);
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
                renderLayer(graphicsContext, layer, frame, renderResolutionScale);
            }
        }

        graphicsContext.restore();
    }

    /**
     * Renders animation content to a transparent buffer, inverts only non-transparent pixels,
     * and composites the result over the unchanged player background.
     */
    private void renderInvertedAnimationContent(GraphicsContext graphicsContext,
                                                double frame,
                                                Set<Integer> visibleLayerIndices,
                                                double viewportX,
                                                double viewportY,
                                                double viewportWidth,
                                                double viewportHeight,
                                                double scale,
                                                double offsetX,
                                                double offsetY,
                                                double renderResolutionScale) {
        double bufferWidth = Math.max(1, graphicsContext.getCanvas().getWidth());
        double bufferHeight = Math.max(1, graphicsContext.getCanvas().getHeight());

        WritableImage animationImage = OffscreenRenderer.renderToImage(bufferWidth, bufferHeight, offscreenGc ->
                renderAnimationContent(offscreenGc, frame, visibleLayerIndices,
                        viewportX, viewportY, viewportWidth, viewportHeight,
                        scale, offsetX, offsetY, renderResolutionScale));

        graphicsContext.drawImage(invertImageColors(animationImage), 0, 0);
    }

    /**
     * Returns a copy of the provided image with RGB channels inverted while preserving alpha.
     */
    private WritableImage invertImageColors(WritableImage sourceImage) {
        int width = (int) sourceImage.getWidth();
        int height = (int) sourceImage.getHeight();

        WritableImage invertedImage = new WritableImage(width, height);
        PixelReader pixelReader = sourceImage.getPixelReader();

        if (pixelReader == null) {
            return sourceImage;
        }

        var pixelWriter = invertedImage.getPixelWriter();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int argb = pixelReader.getArgb(x, y);
                int alpha = (argb >>> 24) & 0xFF;
                if (alpha == 0) {
                    pixelWriter.setArgb(x, y, 0x00000000);
                    continue;
                }

                int red = (argb >>> 16) & 0xFF;
                int green = (argb >>> 8) & 0xFF;
                int blue = argb & 0xFF;

                int invertedArgb = (alpha << 24)
                        | ((0xFF - red) << 16)
                        | ((0xFF - green) << 8)
                        | (0xFF - blue);
                pixelWriter.setArgb(x, y, invertedArgb);
            }
        }

        return invertedImage;
    }

    private void updateAdaptiveOffscreenScale(double renderMillis) {
        // Exponential smoothing to avoid resolution oscillation on bursty frames.
        smoothedRenderMillis = smoothedRenderMillis * 0.85 + renderMillis * 0.15;

        double targetMillis = 1000.0 / Math.max(1.0, getFramesPerSecond());
        if (smoothedRenderMillis > targetMillis * 1.25) {
            adaptiveOffscreenScale = Math.max(MIN_ADAPTIVE_OFFSCREEN_SCALE, adaptiveOffscreenScale - ADAPTIVE_SCALE_DOWN_STEP);
        } else if (smoothedRenderMillis < targetMillis * 0.85) {
            adaptiveOffscreenScale = Math.min(MAX_ADAPTIVE_OFFSCREEN_SCALE, adaptiveOffscreenScale + ADAPTIVE_SCALE_UP_STEP);
        }
    }

    /**
     * Draws a debug overlay showing frame, scale, and FPS information.
     *
     * @param gc    graphics context to draw on
     * @param frame current frame number
     * @param scale rendering scale factor
     */
    private void drawDebugOverlay(GraphicsContext gc, double frame, double scale) {
        gc.save();
        gc.setFont(Font.font("Monospaced", 10));

        double x = 6;
        double y = 6;
        double width = 250;
        double height = 80;

        gc.setFill(Color.rgb(255, 255, 200, 0.92));
        gc.fillRoundRect(x, y, width, height, 6, 6);
        gc.setStroke(Color.rgb(0, 0, 0, 0.25));
        gc.setLineWidth(1);
        gc.strokeRoundRect(x, y, width, height, 6, 6);

        gc.setFill(Color.rgb(17, 17, 17));
        gc.fillText("frame: " + String.format("%.1f", frame), x + 6, y + 16);
        gc.fillText("scale: " + String.format("%.2f", scale), x + 6, y + 30);
        gc.fillText("fps: " + String.format("%.1f", measuredPlaybackFps), x + 6, y + 44);
        gc.fillText("target fps: " + getFramesPerSecond(), x + 6, y + 58);
        gc.fillText("w/h: " + this.getWidth() + "/" + this.getHeight(), x + 6, y + 72);
        gc.restore();
    }

    /**
     * Checks whether a layer is active (visible) at the given frame.
     *
     * @param layer layer to check
     * @param frame frame to evaluate
     * @return true if the layer should be rendered at this frame
     */
    private boolean isLayerActiveAtFrame(Layer layer, double frame) {
        return LayerActivity.isActiveAtFrame(layer, frame);
    }

    /**
     * Checks whether a layer should be skipped due to its parent using mattes.
     *
     * @param layer layer to evaluate
     * @return true if the layer should be skipped
     */
    private boolean shouldSkipDueToParent(Layer layer) {
        return skipDueToParentCache.getOrDefault(layer, computeSkipDueToParent(layer));
    }

    /**
     * Renders a layer, applying effects like Gaussian blur if needed.
     *
     * @param gc    graphics context
     * @param layer layer to render
     * @param frame animation frame
     */
    private void renderLayer(GraphicsContext gc, Layer layer, double frame) {
        renderLayer(gc, layer, frame, resolveRenderResolutionScale(gc));
    }

    /**
     * Renders a layer with resolution-aware off-screen passes.
     */
    private void renderLayer(GraphicsContext gc, Layer layer, double frame, double renderResolutionScale) {
        double effectiveResolutionScale = Math.clamp(renderResolutionScale, 0.1, 1.0);

        double blurRadius = effectsRenderer.getGaussianBlurRadius(layer, frame);
        if (blurRadius > 0.0) {
            if (shouldUseStaticBlurLayerCache(layer, blurRadius)) {
                effectsRenderer.renderStaticLayerWithGaussianBlurCache(
                        gc,
                        layer,
                        frame,
                        blurRadius,
                        Math.max(1, getAnimationWidth()),
                        Math.max(1, getAnimationHeight()),
                        effectiveResolutionScale,
                        (targetGc, targetLayer, targetFrame) -> renderLayerInternal(
                                targetGc,
                                targetLayer,
                                targetFrame,
                                effectiveResolutionScale)
                );
            } else {
                effectsRenderer.renderLayerWithGaussianBlur(
                        gc,
                        layer,
                        frame,
                        blurRadius,
                        Math.max(1, getAnimationWidth()),
                        Math.max(1, getAnimationHeight()),
                        effectiveResolutionScale,
                        (targetGc, targetLayer, targetFrame) -> renderLayerInternal(
                                targetGc,
                                targetLayer,
                                targetFrame,
                                effectiveResolutionScale)
                );
            }
            return;
        }

        renderLayerInternal(gc, layer, frame, effectiveResolutionScale);
    }

    private boolean shouldUseStaticBlurLayerCache(Layer layer, double blurRadius) {
        return effectsRenderer.canUseStaticBlurLayerCache(layer, blurRadius)
                && !hasAnimatedParentTransformChain(layer);
    }

    private boolean hasAnimatedParentTransformChain(Layer layer) {
        Integer parentIndex = layer.indexParent();
        int guard = 0;
        while (parentIndex != null && guard++ < layersByIndex.size()) {
            Layer parent = layersByIndex.get(parentIndex);
            if (parent == null) {
                return false;
            }
            if (effectsRenderer.containsAnimation(parent.transform())) {
                return true;
            }
            parentIndex = parent.indexParent();
        }
        return false;
    }

    /**
     * Internal layer rendering method that handles transforms, opacity, and layer content.
     *
     * @param gc    graphics context
     * @param layer layer to render
     * @param frame animation frame
     */
    private void renderLayerInternal(GraphicsContext gc, Layer layer, double frame, double renderResolutionScale) {
        // If layer has a non-normal blend mode, render using offscreen buffer approach
        if (layer.blendMode() != null && layer.blendMode() != com.lottie4j.core.definition.BlendMode.NORMAL) {
            renderLayerWithBlendModeOffscreen(gc, layer, frame, renderResolutionScale);
            return;
        }

        gc.save();

        // Check layer opacity - skip rendering if transparent
        double layerOpacity = 1.0;
        boolean hasAnimatedOpacity = false;
        if (layer.transform() != null && layer.transform().opacity() != null) {
            layerOpacity = layer.transform().opacity().getValue(0, frame) / 100.0;
            logger.debug("LottiePlayer: Layer '{}' at frame {}: opacity raw value = {}, normalized = {}, current globalAlpha = {}",
                    layer.name(), frame, layer.transform().opacity().getValue(0, frame), layerOpacity, gc.getGlobalAlpha());
            if (layerOpacity <= 0) {
                logger.debug("Skipping layer {} - opacity is {}", layer.name(), layerOpacity);
                gc.restore();
                return;
            }
            hasAnimatedOpacity = layer.transform().opacity().animated() != null && layer.transform().opacity().animated() > 0;
        }

        // For layers with animated opacity containing shapes, use off-screen rendering
        // to ensure shapes composite before opacity is applied (matching JS behavior)
        if (hasAnimatedOpacity && layerOpacity < 1.0 && layer.layerType() != LayerType.NULL &&
                layer.shapes() != null && !layer.shapes().isEmpty()) {
            logger.debug("Layer {} has animated opacity - using off-screen rendering", layer.name());
            // Off-screen rendering handles parent transforms internally
            renderLayerWithOffscreenBuffer(gc, layer, frame, layerOpacity, renderResolutionScale);
            gc.restore();
            return;
        }

        // Apply parent transforms recursively
        applyParentTransforms(gc, layer, frame);

        // Apply this layer's transform (with opacity)
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

                TrimPath layerTrimPath = layerTrimPathCache.getOrDefault(layer, null);

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
                        case MODIFIER -> {
                            // Modifiers are consumed by group-level rendering passes.
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

    /**
     * Renders a precomposition layer by delegating to the precomp renderer.
     *
     * @param gc    graphics context
     * @param layer precomposition layer
     * @param frame parent timeline frame
     */
    private void renderPrecompositionLayer(GraphicsContext gc, Layer layer, double frame) {
        precompRenderer.renderPrecompositionLayer(
                gc,
                layer,
                frame,
                assetsById,
                animation,
                resolveRenderResolutionScale(gc),
                this::isLayerActiveAtFrame,
                (solidGc, solidLayer) -> solidColorRenderer.render(solidGc, solidLayer, getAnimationWidth(), getAnimationHeight()),
                (shapeGc, shape, f, trimPath) -> shapeGroupRenderer.renderShapeTypeGroup(shapeGc, shape, f, trimPath),
                this::renderShapeTypeShape
        );
    }

    /**
     * Recursively applies transforms from parent layers.
     *
     * @param gc    graphics context
     * @param layer layer whose parent transforms should be applied
     * @param frame animation frame
     */
    private void applyParentTransforms(GraphicsContext gc, Layer layer, double frame) {
        List<Layer> parentChain = parentChainCache.get(layer);
        if (parentChain == null || parentChain.isEmpty()) {
            return;
        }

        for (Layer parent : parentChain) {
            applyLayerTransform(gc, parent, frame);
        }
    }

    /**
     * Renders a primitive shape using the appropriate shape renderer.
     *
     * @param shape       shape to render
     * @param parentGroup parent group containing styles
     * @param frame       animation frame
     */
    private void renderShapeTypeShape(BaseShape shape, Group parentGroup, double frame) {
        ShapeRenderer renderer = shapeRendererFactory.getRenderer(shape);
        if (renderer == null) {
            logger.warn("No renderer found for shape: {}", shape.getClass().getSimpleName());
            return;
        }
        renderer.render(gc, shape, parentGroup, frame);
    }


    /**
     * Applies a shape group transform to the graphics context.
     *
     * @param gc        graphics context
     * @param transform transform definition
     * @param frame     animation frame
     */
    private void applyGroupTransform(GraphicsContext gc, Transform transform, double frame) {
        transformApplier.applyGroupTransform(gc, transform, frame);
    }

    /**
     * Applies layer transform including position, rotation, scale, and opacity.
     *
     * @param gc    graphics context
     * @param layer layer whose transform is applied
     * @param frame animation frame
     */
    private void applyLayerTransform(GraphicsContext gc, Layer layer, double frame) {
        transformApplier.applyLayerTransform(gc, layer, frame);
    }

    /**
     * Applies layer transform WITHOUT opacity - used for parent transforms where opacity should not inherit.
     *
     * @param gc    graphics context
     * @param layer layer whose transform is applied
     * @param frame animation frame
     */
    private void applyLayerTransformWithoutOpacity(GraphicsContext gc, Layer layer, double frame) {
        transformApplier.applyLayerTransformWithoutOpacity(gc, layer, frame);
    }

    /**
     * Internal convenience method for rendering a frame.
     *
     * @param frame frame number to render
     */
    private void renderFrame(double frame) {
        render(frame);
    }

    /**
     * Checks if the animation is currently playing.
     *
     * @return true if animation is playing
     */
    public boolean isPlaying() {
        return isPlaying;
    }

    /**
     * Gets the current frame being displayed.
     *
     * @return current frame number
     */
    public double getCurrentFrame() {
        return currentFrameProperty.get();
    }

    /**
     * Gets the animation model being rendered.
     *
     * @return animation object
     */
    public Animation getAnimation() {
        return animation;
    }

    /**
     * Gets the current background color.
     *
     * @return background color
     */
    public Color getBackgroundColor() {
        return backgroundColor;
    }

    /**
     * Sets the background color and re-renders the current frame.
     *
     * @param color new background color
     */
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

    /**
     * Resizes the render canvas in pixels and re-renders the current frame.
     *
     * <p>This changes the actual render workload (canvas pixel count), unlike node scaling transforms.
     *
     * @param width  target canvas width in pixels
     * @param height target canvas height in pixels
     */
    public void resizeRender(int width, int height) {
        int clampedWidth = Math.max(1, width);
        int clampedHeight = Math.max(1, height);

        if ((int) getWidth() == clampedWidth && (int) getHeight() == clampedHeight) {
            return;
        }

        setWidth(clampedWidth);
        setHeight(clampedHeight);
        render(currentFrameProperty.get(), visibleLayerIndices);
    }

    /**
     * Resizes the render canvas by a percentage of the initial render size.
     *
     * @param percent scale percentage in range [10, 100]
     */
    public void resizeRenderPercent(double percent) {
        double clampedPercent = Math.clamp(percent, 10.0, 100.0);
        double scaleFactor = clampedPercent / 100.0;
        int targetWidth = (int) Math.round(baseRenderWidth * scaleFactor);
        int targetHeight = (int) Math.round(baseRenderHeight * scaleFactor);
        resizeRender(targetWidth, targetHeight);
    }

    /**
     * Renders a layer to an off-screen buffer, applies opacity, and draws to the main context.
     * This ensures that all shapes composite before opacity is applied, matching JavaScript behavior.
     *
     * @param gc           graphics context to render to
     * @param layer        layer to render
     * @param frame        current frame
     * @param layerOpacity opacity to apply to the final composite
     */
    private void renderLayerWithOffscreenBuffer(GraphicsContext gc,
                                                Layer layer,
                                                double frame,
                                                double layerOpacity,
                                                double renderResolutionScale) {
        double effectiveScale = Math.clamp(renderResolutionScale, 0.1, 1.0);

        int offscreenWidth = Math.max(1, (int) Math.round(getAnimationWidth() * effectiveScale));
        int offscreenHeight = Math.max(1, (int) Math.round(getAnimationHeight() * effectiveScale));

        logger.debug("Rendering layer {} to off-screen buffer ({}x{}, scale={})",
                layer.name(), offscreenWidth, offscreenHeight, effectiveScale);

        WritableImage offscreenImage = OffscreenRenderer.renderToImage(offscreenWidth, offscreenHeight, offscreenGc -> {
            offscreenGc.save();
            offscreenGc.scale(effectiveScale, effectiveScale);

            applyParentTransforms(offscreenGc, layer, frame);
            applyLayerTransformWithoutOpacity(offscreenGc, layer, frame);
            renderLayerContent(offscreenGc, layer, frame);

            offscreenGc.restore();
        });

        double currentAlpha = gc.getGlobalAlpha();
        gc.setGlobalAlpha(currentAlpha * layerOpacity);
        gc.drawImage(offscreenImage,
                0, 0, offscreenWidth, offscreenHeight,
                0, 0, getAnimationWidth(), getAnimationHeight());
        gc.setGlobalAlpha(currentAlpha);

        logger.debug("Finished off-screen rendering for layer: {}", layer.name());
    }

    /**
     * Resolves the off-screen render resolution scale from current canvas-to-composition fit.
     */
    private double resolveRenderResolutionScale(GraphicsContext graphicsContext) {
        double sx = graphicsContext.getCanvas().getWidth() / Math.max(1.0, getViewportWidth());
        double sy = graphicsContext.getCanvas().getHeight() / Math.max(1.0, getViewportHeight());
        double fitScale = Math.clamp(Math.min(sx, sy), 0.1, 1.0);
        if (!adaptiveOffscreenScalingEnabled) {
            return fitScale;
        }
        return Math.clamp(Math.min(fitScale, adaptiveOffscreenScale), 0.1, 1.0);
    }

    /**
     * Enables or disables adaptive off-screen resolution scaling.
     * <p>
     * When enabled, the renderer may lower the resolution of selected off-screen passes
     * (such as opacity/blend/effect compositing) on heavy frames to improve frame-time stability.
     * This usually helps maintain smoother playback on large or complex animations.
     * <p>
     * When disabled, those passes render at fit-scale quality instead of adaptively downscaling.
     * This can improve edge fidelity (for example, reducing selective softness on thin overlays),
     * but can also increase render cost and therefore reduce frames per second on demanding scenes.
     *
     * @param enabled true to allow adaptive downscaling, false to render off-screen passes at fit scale
     */
    public void setAdaptiveOffscreenScalingEnabled(boolean enabled) {
        this.adaptiveOffscreenScalingEnabled = enabled;
        this.adaptiveOffscreenScale = 1.0;
    }

    /**
     * Returns whether adaptive off-screen resolution scaling is enabled.
     *
     * @return true when adaptive off-screen scaling is enabled, false otherwise
     */
    public boolean isAdaptiveOffscreenScalingEnabled() {
        return adaptiveOffscreenScalingEnabled;
    }

    /**
     * Renders the content of a layer (shapes, images, etc.) without applying transforms or opacity.
     * Used by off-screen rendering to composite content first, then apply opacity.
     *
     * @param gc    graphics context to render to
     * @param layer layer to render
     * @param frame current frame
     */
    private void renderLayerContent(GraphicsContext gc, Layer layer, double frame) {
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
        // Skip rendering shapes for NULL layers (type 3)
        else if (layer.layerType() != LayerType.NULL) {
            // Render layer shapes
            if (layer.shapes() != null && !layer.shapes().isEmpty()) {
                logger.debug("Layer has {} shapes", layer.shapes().size());

                TrimPath layerTrimPath = layerTrimPathCache.getOrDefault(layer, null);

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
                        case MODIFIER -> {
                            // Modifiers are consumed by group-level rendering passes.
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
    }

    /**
     * Set whether to show debug information during rendering.
     *
     * @param debug true to show debug info, false to hide
     */
    public void setDebugInfoVisible(boolean debug) {
        this.debug = debug;
        seekToFrame(getCurrentFrame());
    }

    /**
     * Enables or disables global color inversion for rendered output.
     *
     * @param enabled true to invert rendered colors, false to keep original colors
     */
    public void setInvertColorsEnabled(boolean enabled) {
        this.invertColorsEnabled = enabled;
        seekToFrame(getCurrentFrame());
    }

    /**
     * Returns whether global color inversion is enabled.
     *
     * @return true when color inversion is enabled, false otherwise
     */
    public boolean isInvertColorsEnabled() {
        return invertColorsEnabled;
    }

    /**
     * Renders a layer with a blend mode using offscreen buffer.
     * This matches HTML/CSS blend mode behavior.
     */
    private void renderLayerWithBlendModeOffscreen(GraphicsContext gc,
                                                   Layer layer,
                                                   double frame,
                                                   double renderResolutionScale) {
        javafx.scene.effect.BlendMode fxBlendMode = convertToFxBlendMode(layer.blendMode());
        if (fxBlendMode == null) {
            // Unsupported blend mode - render normally
            logger.debug("Unsupported blend mode {} for layer {}, rendering normally", layer.blendMode(), layer.name());
            renderLayerInternalWithoutBlendMode(gc, layer, frame, renderResolutionScale);
            return;
        }

        if (frame == 0.0) {
            logger.info("Layer '{}' rendering with blend mode {} using offscreen buffer", layer.name(), fxBlendMode);
        }

        // Use animation dimensions for buffer
        double bufferWidth = Math.max(100, getAnimationWidth());
        double bufferHeight = Math.max(100, getAnimationHeight());

        // Render layer to offscreen buffer
        WritableImage layerImage = OffscreenRenderer.renderToImage(bufferWidth, bufferHeight, offscreenGc -> {
            renderLayerInternalWithoutBlendMode(offscreenGc, layer, frame, renderResolutionScale);
        });

        // Composite with blend mode
        gc.save();
        gc.setGlobalBlendMode(fxBlendMode);
        gc.drawImage(layerImage, 0, 0);
        gc.restore();
    }

    /**
     * Renders a layer without checking/applying blend modes (used internally for offscreen rendering).
     */
    private void renderLayerInternalWithoutBlendMode(GraphicsContext gc,
                                                     Layer layer,
                                                     double frame,
                                                     double renderResolutionScale) {
        gc.save();

        // Check layer opacity - skip rendering if transparent
        double layerOpacity = 1.0;
        boolean hasAnimatedOpacity = false;
        if (layer.transform() != null && layer.transform().opacity() != null) {
            layerOpacity = layer.transform().opacity().getValue(0, frame) / 100.0;
            if (layerOpacity <= 0) {
                logger.debug("Skipping layer {} - opacity is {}", layer.name(), layerOpacity);
                gc.restore();
                return;
            }
            hasAnimatedOpacity = layer.transform().opacity().animated() != null && layer.transform().opacity().animated() > 0;
        }

        // For layers with animated opacity containing shapes, use off-screen rendering
        if (hasAnimatedOpacity && layerOpacity < 1.0 && layer.layerType() != LayerType.NULL &&
                layer.shapes() != null && !layer.shapes().isEmpty()) {
            logger.debug("Layer {} has animated opacity - using off-screen rendering", layer.name());
            renderLayerWithOffscreenBuffer(gc, layer, frame, layerOpacity, renderResolutionScale);
            gc.restore();
            return;
        }

        // Apply parent transforms recursively
        applyParentTransforms(gc, layer, frame);

        // Apply this layer's transform (with opacity)
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
                TrimPath layerTrimPath = layerTrimPathCache.getOrDefault(layer, null);

                // Render shapes in REVERSE order (bottom-to-top: last shape renders first)
                for (int i = layer.shapes().size() - 1; i >= 0; i--) {
                    BaseShape shape = layer.shapes().get(i);

                    // Skip modifiers and styles - they're applied within groups/shapes
                    if (shape instanceof TrimPath) {
                        continue;
                    }

                    switch (shape.shapeType().shapeGroup()) {
                        case GROUP -> shapeGroupRenderer.renderShapeTypeGroup(gc, shape, frame, layerTrimPath);
                        case SHAPE -> renderShapeTypeShape(shape, null, frame);
                        case STYLE -> logger.debug("Styles should be applied within groups");
                        case MODIFIER -> logger.debug("Modifiers should be applied within groups");
                        default -> logger.warn("Unknown shape group: {}", shape.shapeType().shapeGroup());
                    }
                }
            }
        } else {
            logger.debug("Rendering NULL layer: {}", layer.name());
        }

        gc.restore();
    }

    /**
     * Converts Lottie blend mode to JavaFX blend mode.
     *
     * @param lottieBlendMode Lottie blend mode
     * @return JavaFX blend mode, or null if not supported
     */
    private javafx.scene.effect.BlendMode convertToFxBlendMode(com.lottie4j.core.definition.BlendMode lottieBlendMode) {
        return switch (lottieBlendMode) {
            case NORMAL -> javafx.scene.effect.BlendMode.SRC_OVER;
            case MULTIPLY -> javafx.scene.effect.BlendMode.MULTIPLY;
            case SCREEN -> javafx.scene.effect.BlendMode.SCREEN;
            case OVERLAY -> javafx.scene.effect.BlendMode.OVERLAY;
            case DARKEN -> javafx.scene.effect.BlendMode.DARKEN;
            case LIGHTEN -> javafx.scene.effect.BlendMode.LIGHTEN;
            case COLOR_DODGE -> javafx.scene.effect.BlendMode.COLOR_DODGE;
            case COLOR_BURN -> javafx.scene.effect.BlendMode.COLOR_BURN;
            case HARD_LIGHT -> null; // Not supported in JavaFX
            case SOFT_LIGHT -> null; // Not supported in JavaFX
            case DIFFERENCE -> javafx.scene.effect.BlendMode.DIFFERENCE;
            case EXCLUSION -> javafx.scene.effect.BlendMode.EXCLUSION;
            case HUE -> null; // Not supported in JavaFX
            case SATURATION -> null; // Not supported in JavaFX
            case COLOR -> null; // Not supported in JavaFX
            case LUMINOSITY -> null; // Not supported in JavaFX
        };
    }
}

