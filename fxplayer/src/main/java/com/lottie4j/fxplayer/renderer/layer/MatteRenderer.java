package com.lottie4j.fxplayer.renderer.layer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lottie4j.core.definition.MatteMode;
import com.lottie4j.core.model.layer.Layer;
import com.lottie4j.fxplayer.util.RenderProfiler;

import javafx.geometry.Rectangle2D;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

/**
 * Renderer for track matte (masking) operations.
 * Handles alpha, inverted alpha, luma, and inverted luma matte modes.
 */
public class MatteRenderer {

    private static final Logger logger = LoggerFactory.getLogger(MatteRenderer.class);

    private final SnapshotParameters snapshotParameters = new SnapshotParameters();

    // Reused between frames while the matte size is unchanged (outermost call only, see renderDepth)
    private Canvas matteCanvas;
    private Canvas contentCanvas;
    private WritableImage matteSnapshot;
    private WritableImage contentSnapshot;
    private int[] contentBuffer;
    private int[] matteBuffer;
    private int[] resultBuffer;
    private Canvas probeCanvas;
    private int renderDepth = 0;

    // The area a matte needs is found by drawing the content layer at 1/PROBE_DIVISOR of the size first.
    // The margin (in probe pixels) covers anti-aliasing and features thinner than a probe pixel.
    private static final int PROBE_DIVISOR = 8;
    private static final int PROBE_MARGIN = 2;

    /**
     * Creates a new MatteRenderer.
     */
    public MatteRenderer() {
        snapshotParameters.setFill(Color.TRANSPARENT);
    }

    /**
     * Renders a layer with simple blend-mode-based matte (simpler but less accurate).
     *
     * @param gc            graphics context
     * @param matteUser     layer that uses the matte
     * @param matteSource   layer that provides the matte
     * @param frame         animation frame
     * @param layerRenderer callback to render layer content
     */
    public void renderLayerWithMatteSimple(GraphicsContext gc, Layer matteUser, Layer matteSource, double frame, LayerRenderer layerRenderer) {
        // Use JavaFX BlendMode to simulate alpha masking
        // For ALPHA matte mode: content should only show where matte is opaque

        MatteMode matteMode = matteUser.matteMode();
        logger.debug("Rendering with blend mode matte: {}", matteMode);

        // First render the content layer normally
        layerRenderer.render(gc, matteUser, frame);

        // Then apply the matte using blend mode
        // MULTIPLY blend mode: content * matte (darkens where matte is darker)
        // For now, just skip the matte to see content
        // TODO: Implement proper blend mode masking
    }

    /**
     * Renders a layer with pixel-accurate matte composition (more accurate but slower).
     *
     * @param gc              graphics context
     * @param matteUser       layer that uses the matte
     * @param matteSource     layer that provides the matte
     * @param frame           animation frame
     * @param animationWidth  animation width
     * @param animationHeight animation height
     * @param layerRenderer   callback to render layer content
     */
    public void renderLayerWithMatte(GraphicsContext gc, Layer matteUser, Layer matteSource, double frame,
                                     int animationWidth, int animationHeight, LayerRenderer layerRenderer) {
        renderLayerWithMatte(gc, matteUser, matteSource, frame, animationWidth, animationHeight, 1.0, layerRenderer);
    }

    /**
     * Renders a layer with pixel-accurate matte composition at a configurable off-screen resolution scale.
     *
     * @param gc                    graphics context
     * @param matteUser             layer that uses the matte
     * @param matteSource           layer that provides the matte
     * @param frame                 animation frame
     * @param animationWidth        composition width in local coordinates
     * @param animationHeight       composition height in local coordinates
     * @param renderResolutionScale off-screen raster scale factor in range {@code (0, 1]}
     * @param layerRenderer         callback to render layer content
     */
    public void renderLayerWithMatte(GraphicsContext gc, Layer matteUser, Layer matteSource, double frame,
                                     int animationWidth, int animationHeight, double renderResolutionScale, LayerRenderer layerRenderer) {
        long startTime = System.nanoTime();

        double width = Math.max(1, animationWidth);
        double height = Math.max(1, animationHeight);
        double scale = Math.clamp(renderResolutionScale, 0.1, 1.0);
        int matteWidth = (int) Math.max(1, Math.round(width * scale));
        int matteHeight = (int) Math.max(1, Math.round(height * scale));

        // Nested mattes (a matted precomp containing matted layers) render while the outer call is still
        // using the reusable buffers, so only the outermost call may use them.
        boolean reuseBuffers = renderDepth == 0;
        renderDepth++;
        try {
            Canvas matteCanvas = reuseBuffers ? reusableMatteCanvas(matteWidth, matteHeight) : new Canvas(matteWidth, matteHeight);
            Canvas contentCanvas = reuseBuffers ? reusableContentCanvas(matteWidth, matteHeight) : new Canvas(matteWidth, matteHeight);
            MatteMode matteMode = matteUser.matteMode();

            // Render the matte source and the content layer (WITH parent transforms for proper positioning)
            if (logger.isDebugEnabled()) {
                logger.debug("Rendering matte source layer: {} (shapes: {}), parent: {}, scale={}",
                        matteSource.name(), (matteSource.shapes() != null ? matteSource.shapes().size() : 0), matteSource.indexParent(), scale);
            }
            long t = RenderProfiler.start();
            renderScaled(matteCanvas, matteSource, frame, scale, layerRenderer);
            RenderProfiler.stop("matte.drawSource", t);
            if (logger.isDebugEnabled()) {
                logger.debug("Rendering matte user layer: {} (shapes: {}), parent: {}, scale={}",
                        matteUser.name(), (matteUser.shapes() != null ? matteUser.shapes().size() : 0), matteUser.indexParent(), scale);
            }
            t = RenderProfiler.start();
            renderScaled(contentCanvas, matteUser, frame, scale, layerRenderer);
            RenderProfiler.stop("matte.drawContent", t);

            // The matted result is transparent wherever the content layer is, so only the area the content
            // covers has to be snapshotted, read, composed and drawn. That is a small part of the composition
            // for most mattes, and the pixel work scales with the area.
            Rectangle2D area = contentArea(matteUser, frame, scale, matteWidth, matteHeight, layerRenderer, reuseBuffers);
            if (area == null) {
                return;
            }
            int areaX = (int) area.getMinX();
            int areaY = (int) area.getMinY();
            int areaWidth = (int) area.getWidth();
            int areaHeight = (int) area.getHeight();
            boolean wholeCanvas = areaWidth == matteWidth && areaHeight == matteHeight;
            SnapshotParameters areaParameters = snapshotParameters;
            if (!wholeCanvas) {
                areaParameters = new SnapshotParameters();
                areaParameters.setFill(Color.TRANSPARENT);
                areaParameters.setViewport(area);
            }

            // Snapshot both canvases with transparent background (into the previous images when the size is unchanged)
            t = RenderProfiler.start();
            WritableImage matteImage = matteCanvas.snapshot(areaParameters,
                    reuseBuffers ? reusableSnapshot(matteSnapshot, areaWidth, areaHeight) : new WritableImage(areaWidth, areaHeight));
            WritableImage contentImage = contentCanvas.snapshot(areaParameters,
                    reuseBuffers ? reusableSnapshot(contentSnapshot, areaWidth, areaHeight) : new WritableImage(areaWidth, areaHeight));
            RenderProfiler.stop("matte.snapshot", t);
            if (reuseBuffers) {
                matteSnapshot = matteImage;
                contentSnapshot = contentImage;
            }

            if (logger.isDebugEnabled()) {
                logDebugSamples(matteImage, contentImage, matteMode);
            }

            // Apply matte composition. The result must be a new image: the canvas only records the draw command
            // and reads the pixels when it renders, possibly after another matte in the same frame.
            WritableImage result = applyMatte(contentImage, matteImage, matteMode, reuseBuffers);

            // IMPORTANT: Save the current composite operation and use SRC_OVER
            // This ensures transparent pixels in the matted result don't obscure
            // content below (which was showing as white background)
            gc.save();
            gc.setGlobalAlpha(1.0); // Full opacity for the matted result itself

            // Draw the result to the main canvas, scaling back up if needed
            // The SRC_OVER blend mode (default) will properly composite transparent pixels
            double toLocalX = width / matteWidth;
            double toLocalY = height / matteHeight;
            gc.drawImage(result,
                    0, 0, areaWidth, areaHeight,
                    areaX * toLocalX, areaY * toLocalY, areaWidth * toLocalX, areaHeight * toLocalY);
            gc.restore();
        } finally {
            renderDepth--;
        }

        if (logger.isDebugEnabled()) {
            logger.debug("Matte rendering took: {}ms (scale={}, size={}x{})",
                    ((System.nanoTime() - startTime) / 1_000_000), scale, matteWidth, matteHeight);
        }
    }

    /**
     * Finds the area of the matte canvas the content layer paints in, by drawing the layer at a fraction of
     * the size and taking the bounds of its non-transparent pixels plus a margin.
     *
     * @return the area in pixels of the matte canvas, or {@code null} when the layer paints nothing
     */
    private Rectangle2D contentArea(Layer content, double frame, double scale, int canvasWidth, int canvasHeight,
                                    LayerRenderer layerRenderer, boolean reuseBuffers) {
        long t = RenderProfiler.start();
        int probeWidth = Math.max(1, (canvasWidth + PROBE_DIVISOR - 1) / PROBE_DIVISOR);
        int probeHeight = Math.max(1, (canvasHeight + PROBE_DIVISOR - 1) / PROBE_DIVISOR);
        Canvas probe;
        if (reuseBuffers) {
            probeCanvas = prepareCanvas(probeCanvas, probeWidth, probeHeight);
            probe = probeCanvas;
        } else {
            probe = new Canvas(probeWidth, probeHeight);
        }
        // the probe covers the same composition at 1/PROBE_DIVISOR of the matte canvas
        renderScaled(probe, content, frame, scale * probeWidth / canvasWidth, layerRenderer);
        WritableImage probeImage = probe.snapshot(snapshotParameters, new WritableImage(probeWidth, probeHeight));
        int[] pixels = new int[probeWidth * probeHeight];
        probeImage.getPixelReader().getPixels(0, 0, probeWidth, probeHeight, PixelFormat.getIntArgbInstance(), pixels, 0, probeWidth);

        int minX = probeWidth, minY = probeHeight, maxX = -1, maxY = -1;
        for (int y = 0; y < probeHeight; y++) {
            for (int x = 0; x < probeWidth; x++) {
                if ((pixels[y * probeWidth + x] >>> 24) != 0) {
                    minX = Math.min(minX, x);
                    maxX = Math.max(maxX, x);
                    minY = Math.min(minY, y);
                    maxY = Math.max(maxY, y);
                }
            }
        }
        RenderProfiler.stop("matte.probe", t);
        if (maxX < 0) {
            return null;
        }
        int x0 = Math.max(0, (minX - PROBE_MARGIN) * PROBE_DIVISOR);
        int y0 = Math.max(0, (minY - PROBE_MARGIN) * PROBE_DIVISOR);
        int x1 = Math.min(canvasWidth, (maxX + 1 + PROBE_MARGIN) * PROBE_DIVISOR);
        int y1 = Math.min(canvasHeight, (maxY + 1 + PROBE_MARGIN) * PROBE_DIVISOR);
        return new Rectangle2D(x0, y0, Math.max(1, x1 - x0), Math.max(1, y1 - y0));
    }

    /**
     * Renders a layer into an off-screen canvas at the given scale, leaving the canvas' base state untouched.
     */
    private void renderScaled(Canvas canvas, Layer layer, double frame, double scale, LayerRenderer layerRenderer) {
        GraphicsContext offscreenGc = canvas.getGraphicsContext2D();
        offscreenGc.save();
        offscreenGc.scale(scale, scale);
        layerRenderer.render(offscreenGc, layer, frame);
        offscreenGc.restore();
    }

    private Canvas reusableMatteCanvas(int width, int height) {
        matteCanvas = prepareCanvas(matteCanvas, width, height);
        return matteCanvas;
    }

    private Canvas reusableContentCanvas(int width, int height) {
        contentCanvas = prepareCanvas(contentCanvas, width, height);
        return contentCanvas;
    }

    /**
     * Returns a cleared canvas of the requested size, reusing the given one when the size matches.
     * A full clear with an identity transform also resets the canvas' recorded command buffer.
     */
    private static Canvas prepareCanvas(Canvas canvas, int width, int height) {
        if (canvas == null || (int) canvas.getWidth() != width || (int) canvas.getHeight() != height) {
            return new Canvas(width, height);
        }
        GraphicsContext offscreenGc = canvas.getGraphicsContext2D();
        offscreenGc.setTransform(1, 0, 0, 1, 0, 0);
        offscreenGc.clearRect(0, 0, width, height);
        return canvas;
    }

    /**
     * Returns the given image when its size matches, or null so a new snapshot image is created.
     * A snapshot into an existing image only renders the area of that image, so a cached image of another
     * size would crop (or pad) the snapshot when mattes of different sizes are rendered.
     */
    private static WritableImage reusableSnapshot(WritableImage image, int width, int height) {
        return image != null && (int) image.getWidth() == width && (int) image.getHeight() == height ? image : null;
    }

    /**
     * Logs a few sampled pixels of the matte and content images. Only called when debug logging is enabled,
     * as each sample is a pixel read.
     */
    private void logDebugSamples(WritableImage matteImage, WritableImage contentImage, MatteMode matteMode) {
        int centerX = (int) matteImage.getWidth() / 2;
        int centerY = (int) matteImage.getHeight() / 2;
        logger.debug("Applying matte mode: {}, image size: {}x{}", matteMode, matteImage.getWidth(), matteImage.getHeight());
        logger.debug("  Matte center [{},{}]: {}", centerX, centerY, matteImage.getPixelReader().getColor(centerX, centerY));
        logger.debug("  Content center [{},{}]: {}", centerX, centerY, contentImage.getPixelReader().getColor(centerX, centerY));
    }

    /**
     * Applies matte composition at the pixel level.
     *
     * @param content      content image
     * @param matte        matte image
     * @param matteMode    matte composition mode
     * @param reuseBuffers whether the cached pixel buffers may be used
     * @return composited image with matte applied
     */
    private WritableImage applyMatte(WritableImage content, WritableImage matte, MatteMode matteMode, boolean reuseBuffers) {
        int width = (int) content.getWidth();
        int height = (int) content.getHeight();
        int pixelCount = width * height;

        int[] contentPixels;
        int[] mattePixels;
        int[] resultPixels;
        if (reuseBuffers) {
            if (contentBuffer == null || contentBuffer.length < pixelCount) {
                contentBuffer = new int[pixelCount];
                matteBuffer = new int[pixelCount];
                resultBuffer = new int[pixelCount];
            }
            contentPixels = contentBuffer;
            mattePixels = matteBuffer;
            resultPixels = resultBuffer;
        } else {
            contentPixels = new int[pixelCount];
            mattePixels = new int[pixelCount];
            resultPixels = new int[pixelCount];
        }

        long t = RenderProfiler.start();
        content.getPixelReader().getPixels(0, 0, width, height, PixelFormat.getIntArgbInstance(), contentPixels, 0, width);
        matte.getPixelReader().getPixels(0, 0, width, height, PixelFormat.getIntArgbInstance(), mattePixels, 0, width);
        RenderProfiler.stop("matte.readPixels", t);

        t = RenderProfiler.start();
        composeMatte(contentPixels, mattePixels, resultPixels, pixelCount, matteMode);
        RenderProfiler.stop("matte.compose", t);

        // setPixels copies the buffer, so the result buffer can be reused on the next call
        t = RenderProfiler.start();
        WritableImage result = new WritableImage(width, height);
        result.getPixelWriter().setPixels(0, 0, width, height, PixelFormat.getIntArgbInstance(), resultPixels, 0, width);
        RenderProfiler.stop("matte.writeResult", t);
        return result;
    }

    /**
     * Composites a content buffer with a matte buffer in-place into {@code resultBuffer}.
     *
     * <p>This is the pure, pixel-level core of {@link #applyMatte(WritableImage, WritableImage, MatteMode, boolean)}
     * extracted so it can be unit-tested without a JavaFX runtime. All buffers are straight-alpha
     * ARGB (as produced by {@code PixelFormat.getIntArgbInstance()}).</p>
     *
     * <p>Invariants enforced here, per the Lottie / After-Effects track-matte spec:</p>
     * <ul>
     *   <li>The content layer's RGB channels are <strong>preserved exactly</strong>; the matte
     *       source's RGB never leaks into the result. This is what makes
     *       {@link MatteMode#ALPHA} an <em>alpha</em> matte rather than a colour matte.</li>
     *   <li>Only the result's alpha is modulated. For {@link MatteMode#ALPHA} the modulator is
     *       the matte's alpha; for {@link MatteMode#LUMA} it is the matte's Rec. 601 luma.</li>
     *   <li>Inverted variants substitute {@code (255 - x)} for the modulator.</li>
     * </ul>
     *
     * @param contentBuffer straight-alpha ARGB pixels of the content layer
     * @param matteBuffer   straight-alpha ARGB pixels of the matte source layer
     * @param resultBuffer  output buffer (must be the same length as the inputs)
     * @param matteMode     matte composition mode
     */
    static void composeMatte(int[] contentBuffer, int[] matteBuffer, int[] resultBuffer, MatteMode matteMode) {
        composeMatte(contentBuffer, matteBuffer, resultBuffer, contentBuffer.length, matteMode);
    }

    /**
     * Same as {@link #composeMatte(int[], int[], int[], MatteMode)} for the first {@code pixelCount} pixels only.
     */
    static void composeMatte(int[] contentBuffer, int[] matteBuffer, int[] resultBuffer, int pixelCount, MatteMode matteMode) {
        for (int i = 0; i < pixelCount; i++) {
            int contentPixel = contentBuffer[i];
            int mattePixel = matteBuffer[i];

            int cA = (contentPixel >>> 24) & 0xFF;
            int cR = (contentPixel >> 16) & 0xFF;
            int cG = (contentPixel >> 8) & 0xFF;
            int cB = contentPixel & 0xFF;

            int mA = (mattePixel >>> 24) & 0xFF;
            int mR = (mattePixel >> 16) & 0xFF;
            int mG = (mattePixel >> 8) & 0xFF;
            int mB = mattePixel & 0xFF;

            int resultA;
            switch (matteMode) {
                case ALPHA:
                    // tt:1 — content alpha is multiplied by the matte's alpha only.
                    // The matte source's RGB MUST NOT influence the result; it is a mask, not a colour.
                    resultA = (cA * mA) / 255;
                    break;

                case INVERTED_ALPHA:
                    // tt:2 — same as ALPHA but with the matte's alpha inverted.
                    resultA = (cA * (255 - mA)) / 255;
                    break;

                case LUMA: {
                    // tt:3 — content alpha is multiplied by the matte's Rec. 601 luma.
                    int luma = (299 * mR + 587 * mG + 114 * mB) / 1000;
                    resultA = (cA * luma) / 255;
                    break;
                }

                case INVERTED_LUMA: {
                    // tt:4 — same as LUMA but with the matte's luma inverted.
                    int luma = (299 * mR + 587 * mG + 114 * mB) / 1000;
                    resultA = (cA * (255 - luma)) / 255;
                    break;
                }

                default:
                    resultA = cA;
            }

            // Preserve content RGB; only alpha is modulated. If the result is fully transparent,
            // zero out RGB as well so downstream consumers do not see stale colour data.
            if (resultA == 0) {
                resultBuffer[i] = 0;
            } else {
                resultBuffer[i] = (resultA << 24) | (cR << 16) | (cG << 8) | cB;
            }
        }
    }

    /**
     * Callback interface for rendering layer content.
     */
    @FunctionalInterface
    public interface LayerRenderer {
        /**
         * Renders a layer to a graphics context.
         *
         * @param gc    graphics context
         * @param layer layer to render
         * @param frame animation frame
         */
        void render(GraphicsContext gc, Layer layer, double frame);
    }
}

