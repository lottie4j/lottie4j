package com.lottie4j.fxplayer.renderer.layer;

import com.lottie4j.core.model.Animation;
import com.lottie4j.core.model.Asset;
import com.lottie4j.core.model.Layer;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;

import java.io.ByteArrayInputStream;
import java.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Renders image layers from Lottie animations.
 * Supports both external image files and embedded base64 data URIs.
 */
public class ImageRenderer {

    private static final Logger logger = LoggerFactory.getLogger(ImageRenderer.class.getName());

    /**
     * Render an image layer
     *
     * @param gc        the graphics context to draw on
     * @param layer     the image layer
     * @param animation the animation containing the asset
     */
    public void render(GraphicsContext gc, Layer layer, Animation animation) {
        if (layer.referenceId() == null || animation.assets() == null) {
            logger.debug("Image layer has no referenceId or animation has no assets");
            return;
        }

        // Find the asset with matching ID
        Asset asset = animation.assets().stream()
                .filter(a -> layer.referenceId().equals(a.id()))
                .findFirst()
                .orElse(null);

        if (asset == null) {
            logger.warn("Could not find asset with ID: " + layer.referenceId());
            return;
        }

        // Get the image data from the asset
        Image image = getImageFromAsset(asset);
        if (image == null) {
            logger.warn("Could not load image from asset: " + asset.id());
            return;
        }

        // Draw the image at the layer's position and size
        double width = layer.width() != null ? layer.width() : image.getWidth();
        double height = layer.height() != null ? layer.height() : image.getHeight();

        // Draw the image (0, 0 because transforms are already applied)
        gc.drawImage(image, 0, 0, width, height);

        logger.debug("Rendered image layer: " + layer.name() + " (" + width + "x" + height + ")");
    }

    /**
     * Extract image from asset, supporting both external files and embedded data URIs
     *
     * @param asset asset that may contain embedded data URI or external file reference
     * @return loaded image, or {@code null} when the asset cannot be resolved
     */
    private Image getImageFromAsset(Asset asset) {
        if (asset.fileName() == null) {
            return null;
        }

        // Check if it's embedded base64 data
        if (asset.fileName() instanceof String dataStr) {
            if (dataStr.startsWith("data:image/")) {
                return loadImageFromDataUri(dataStr);
            }
            // External file path - try to load from resources
            return loadImageFromFile(dataStr);
        }

        // If fileName is a Boolean, it's an external file reference
        if (asset.fileName() instanceof Boolean && (Boolean) asset.fileName()) {
            // External file - would need file path from somewhere
            if (asset.name() != null) {
                return loadImageFromFile(asset.name());
            }
        }

        return null;
    }

    /**
     * Load image from a data URI (base64 encoded)
     *
     * @param dataUri URI in the form {@code data:image/<type>;base64,<payload>}
     * @return loaded image, or {@code null} when decoding/loading fails
     */
    private Image loadImageFromDataUri(String dataUri) {
        try {
            // Parse data URI: data:image/png;base64,<data>
            String[] parts = dataUri.split(",", 2);
            if (parts.length != 2) {
                logger.warn("Invalid data URI format");
                return null;
            }

            String base64Data = parts[1];
            byte[] imageData = Base64.getDecoder().decode(base64Data);

            ByteArrayInputStream inputStream = new ByteArrayInputStream(imageData);
            Image image = new Image(inputStream);

            if (image.isError()) {
                logger.warn("Failed to load image from data URI: " + image.getException());
                return null;
            }

            logger.debug("Successfully loaded image from data URI (" + imageData.length + " bytes)");
            return image;
        } catch (IllegalArgumentException e) {
            logger.warn("Failed to decode base64 image data: " + e.getMessage());
            return null;
        } catch (Exception e) {
            logger.warn("Error loading image from data URI: " + e.getMessage());
            return null;
        }
    }

    /**
     * Load image from file path
     *
     * @param filePath relative resource path or absolute/relative file system path
     * @return loaded image, or {@code null} when the file cannot be read
     */
    private Image loadImageFromFile(String filePath) {
        try {
            // Try to load from resources first
            var resource = getClass().getResourceAsStream("/" + filePath);
            if (resource != null) {
                Image image = new Image(resource);
                if (!image.isError()) {
                    logger.debug("Successfully loaded image from resources: " + filePath);
                    return image;
                }
            }

            // Try as URL/file path
            Image image = new Image("file:" + filePath);
            if (!image.isError()) {
                logger.debug("Successfully loaded image from file: " + filePath);
                return image;
            }

            logger.warn("Could not load image: " + filePath);
            return null;
        } catch (Exception e) {
            logger.warn("Error loading image from file: " + filePath + " - " + e.getMessage());
            return null;
        }
    }
}
