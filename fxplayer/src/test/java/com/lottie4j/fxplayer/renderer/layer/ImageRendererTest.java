package com.lottie4j.fxplayer.renderer.layer;

import com.lottie4j.core.model.animation.Animation;
import com.lottie4j.core.model.asset.Asset;
import com.lottie4j.core.model.layer.Layer;
import com.lottie4j.fxplayer.util.FxTestHelper;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

public class ImageRendererTest {

    @BeforeAll
    public static void initToolkit() {
        FxTestHelper.initToolkit();
    }

    @Test
    void rendersEmbeddedDataUriImage() {
        assumeFalse("headless".equalsIgnoreCase(System.getProperty("glass.platform")),
                "Pixel-level canvas rendering is not reliable in headless mode");
        ImageRenderer renderer = new ImageRenderer();
        Layer layer = imageLayer("asset-red", 3, 3);
        Animation animation = animationWithAssets(List.of(imageAsset("asset-red", dataUri(1, 1, 0xFFFF0000))));

        Color pixel = FxTestHelper.callAndWait(() -> {
            Canvas canvas = blackCanvas(6, 6);
            GraphicsContext gc = canvas.getGraphicsContext2D();
            renderer.render(gc, layer, animation);
            return samplePixel(canvas, 1, 1);
        });

        assertTrue(pixel.getRed() > 0.8);
        assertTrue(pixel.getGreen() < 0.2);
        assertTrue(pixel.getBlue() < 0.2);
    }

    @Test
    void usesIntrinsicImageSizeWhenLayerDimensionsAreMissing() {
        assumeFalse("headless".equalsIgnoreCase(System.getProperty("glass.platform")),
                "Pixel-level canvas rendering is not reliable in headless mode");
        ImageRenderer renderer = new ImageRenderer();
        Layer layer = imageLayer("asset-green", null, null);
        Animation animation = animationWithAssets(List.of(imageAsset("asset-green", dataUri(2, 1, 0xFF00FF00))));

        Color[] pixels = FxTestHelper.callAndWait(() -> {
            Canvas canvas = blackCanvas(5, 5);
            GraphicsContext gc = canvas.getGraphicsContext2D();
            renderer.render(gc, layer, animation);
            Color inside = samplePixel(canvas, 1, 0);
            Color outside = samplePixel(canvas, 3, 3);
            return new Color[]{inside, outside};
        });

        assertTrue(pixels[0].getGreen() > 0.8);
        assertTrue(pixels[1].getRed() < 0.1 && pixels[1].getGreen() < 0.1 && pixels[1].getBlue() < 0.1);
    }

    @Test
    void doesNothingWhenReferenceIdIsMissing() {
        ImageRenderer renderer = new ImageRenderer();
        Layer layer = imageLayer(null, 3, 3);
        Animation animation = animationWithAssets(List.of(imageAsset("asset-red", dataUri(1, 1, 0xFFFF0000))));

        assertUnchangedAfterRender(renderer, layer, animation);
    }

    @Test
    void doesNothingWhenAssetCannotBeFound() {
        ImageRenderer renderer = new ImageRenderer();
        Layer layer = imageLayer("missing", 3, 3);
        Animation animation = animationWithAssets(List.of(imageAsset("asset-red", dataUri(1, 1, 0xFFFF0000))));

        assertUnchangedAfterRender(renderer, layer, animation);
    }

    @Test
    void doesNothingWhenDataUriIsInvalid() {
        ImageRenderer renderer = new ImageRenderer();
        Layer layer = imageLayer("asset-invalid", 3, 3);
        Animation animation = animationWithAssets(List.of(imageAsset("asset-invalid", "data:image/png;base64,INVALID")));

        assertUnchangedAfterRender(renderer, layer, animation);
    }

    private static void assertUnchangedAfterRender(ImageRenderer renderer, Layer layer, Animation animation) {
        Color[] pixels = FxTestHelper.callAndWait(() -> {
            Canvas canvas = blackCanvas(6, 6);
            Color before = samplePixel(canvas, 1, 1);
            GraphicsContext gc = canvas.getGraphicsContext2D();
            renderer.render(gc, layer, animation);
            Color after = samplePixel(canvas, 1, 1);
            return new Color[]{before, after};
        });

        assertTrue(Math.abs(pixels[1].getRed() - pixels[0].getRed()) < 0.001);
        assertTrue(Math.abs(pixels[1].getGreen() - pixels[0].getGreen()) < 0.001);
        assertTrue(Math.abs(pixels[1].getBlue() - pixels[0].getBlue()) < 0.001);
        assertTrue(Math.abs(pixels[1].getOpacity() - pixels[0].getOpacity()) < 0.001);
    }

    private static Canvas blackCanvas(int width, int height) {
        Canvas canvas = new Canvas(width, height);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setFill(Color.BLACK);
        gc.fillRect(0, 0, width, height);
        return canvas;
    }

    private static Color samplePixel(Canvas canvas, int x, int y) {
        WritableImage image = canvas.snapshot(new SnapshotParameters(), null);
        return image.getPixelReader().getColor(x, y);
    }

    private static Animation animationWithAssets(List<Asset> assets) {
        return new Animation(null, null, null, null, null, null, null, null, null,
                assets, null, null, null);
    }

    private static Asset imageAsset(String id, String fileName) {
        return new Asset(id, null, null, fileName, 1, null, null, null, null, null, null);
    }

    private static Layer imageLayer(String referenceId, Integer width, Integer height) {
        return new Layer(
                "image", null, null, null, null, null, null,
                null, null, null, null,
                null, null, null, null, null, null, null, null, null,
                null, null, null, null, null,
                null, referenceId, width, height, null, null, null
        );
    }

    private static String dataUri(int width, int height, int argb) {
        try {
            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    image.setRGB(x, y, argb);
                }
            }
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            ImageIO.write(image, "png", output);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(output.toByteArray());
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create PNG data URI for test", e);
        }
    }
}

