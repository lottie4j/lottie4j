package com.lottie4j.fxplayer.renderer.layer;

import com.lottie4j.core.definition.MatteMode;
import com.lottie4j.core.model.layer.Layer;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Renderer for track matte (masking) operations.
 * Handles alpha, inverted alpha, luma, and inverted luma matte modes.
 */
public class MatteRenderer {

    private static final Logger logger = LoggerFactory.getLogger(MatteRenderer.class);

    /**
     * Creates a new MatteRenderer.
     */
    public MatteRenderer() {
        // Constructor for MatteRenderer
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
        double matteWidth = Math.max(1, Math.round(width * scale));
        double matteHeight = Math.max(1, Math.round(height * scale));

        // Create off-screen canvases for matte and content
        Canvas matteCanvas = new Canvas(matteWidth, matteHeight);
        Canvas contentCanvas = new Canvas(matteWidth, matteHeight);
        GraphicsContext matteGc = matteCanvas.getGraphicsContext2D();
        GraphicsContext contentGc = contentCanvas.getGraphicsContext2D();

        // Scale the rendering
        matteGc.scale(scale, scale);
        contentGc.scale(scale, scale);

        // Render the matte source to the matte canvas (WITH parent transforms for proper positioning)
        logger.debug("Rendering matte source layer: {} (shapes: {}), parent: {}, scale={}",
                matteSource.name(), (matteSource.shapes() != null ? matteSource.shapes().size() : 0), matteSource.indexParent(), scale);
        layerRenderer.render(matteGc, matteSource, frame);

        // Render the content layer to the content canvas (WITH parent transforms for proper positioning)
        logger.debug("Rendering matte user layer: {} (shapes: {}), parent: {}, scale={}",
                matteUser.name(), (matteUser.shapes() != null ? matteUser.shapes().size() : 0), matteUser.indexParent(), scale);
        layerRenderer.render(contentGc, matteUser, frame);

        // Get the matte mode
        MatteMode matteMode = matteUser.matteMode();
        logger.debug("Applying matte mode: {}", matteMode);

        // Snapshot both canvases with transparent background
        SnapshotParameters params = new SnapshotParameters();
        params.setFill(Color.TRANSPARENT);
        WritableImage matteImage = matteCanvas.snapshot(params, null);
        WritableImage contentImage = contentCanvas.snapshot(params, null);

        // DEBUG: Check if content and matte have actual pixels
        logger.debug("Matte image size: {}x{}", matteImage.getWidth(), matteImage.getHeight());
        logger.debug("Content image size: {}x{}", contentImage.getWidth(), contentImage.getHeight());

        // Sample multiple pixels to see what we're working with
        PixelReader matteReader = matteImage.getPixelReader();
        PixelReader contentReader = contentImage.getPixelReader();

        // Sample at different positions
        Color matteCenter = matteReader.getColor((int) matteImage.getWidth() / 2, (int) matteImage.getHeight() / 2);
        Color contentCenter = contentReader.getColor((int) contentImage.getWidth() / 2, (int) contentImage.getHeight() / 2);

        logger.debug("  Matte center [{},{}]: {}",
                ((int) matteImage.getWidth() / 2), ((int) matteImage.getHeight() / 2), matteCenter);
        logger.debug("  Content center [{},{}]: {}",
                ((int) contentImage.getWidth() / 2), ((int) contentImage.getHeight() / 2), contentCenter);

        // Apply matte composition
        WritableImage result = applyMatte(contentImage, matteImage, matteMode);

        // IMPORTANT: Save the current composite operation and use SRC_OVER
        // This ensures transparent pixels in the matted result don't obscure
        // content below (which was showing as white background)
        gc.save();
        gc.setGlobalAlpha(1.0); // Full opacity for the matted result itself

        // Draw the result to the main canvas, scaling back up if needed
        // The SRC_OVER blend mode (default) will properly composite transparent pixels
        gc.drawImage(result,
                0, 0, matteWidth, matteHeight,
                0, 0, width, height);
        gc.restore();

        long endTime = System.nanoTime();
        logger.debug("Matte rendering took: {}ms (scale={}, size={}x{})",
                ((endTime - startTime) / 1_000_000), scale, matteWidth, matteHeight);
    }

    /**
     * Applies matte composition at the pixel level.
     *
     * @param content   content image
     * @param matte     matte image
     * @param matteMode matte composition mode
     * @return composited image with matte applied
     */
    private WritableImage applyMatte(WritableImage content, WritableImage matte, MatteMode matteMode) {
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

