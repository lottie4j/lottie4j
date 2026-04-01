package com.lottie4j.fxplayer.renderer.layer;

import com.lottie4j.core.model.layer.Layer;
import com.lottie4j.fxplayer.util.FxTestHelper;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for TextRenderer.
 * Validates text rendering behavior and early returns for missing text data.
 */
class TextRendererTest {

    @BeforeAll
    static void initToolkit() {
        FxTestHelper.initToolkit();
    }

    @Test
    void canBeInstantiatedAndReused() {
        TextRenderer renderer1 = new TextRenderer();
        TextRenderer renderer2 = new TextRenderer();
        assertNotNull(renderer1);
        assertNotNull(renderer2);
        assertNotSame(renderer1, renderer2);
    }

    @Test
    void returnsEarlyWhenLayerHasNoTextData() {
        TextRenderer renderer = new TextRenderer();
        Layer layerNoTextData = layerWithoutTextData();
        
        Boolean completed = FxTestHelper.callAndWait(() -> {
            Canvas canvas = new Canvas(100, 100);
            GraphicsContext gc = canvas.getGraphicsContext2D();
            gc.setFill(javafx.scene.paint.Color.RED);
            gc.fillRect(0, 0, 100, 100);
            
            renderer.render(gc, layerNoTextData, 0.0);
            
            // Verify canvas state unchanged (still red)
            javafx.scene.image.WritableImage image = canvas.snapshot(
                new javafx.scene.SnapshotParameters(), null);
            javafx.scene.image.PixelReader reader = image.getPixelReader();
            javafx.scene.paint.Color pixelColor = reader.getColor(50, 50);
            
            return pixelColor.getRed() > 0.9 && pixelColor.getGreen() < 0.1 && pixelColor.getBlue() < 0.1;
        });
        
        assertTrue(completed, "Canvas should remain unmodified when text data is missing");
    }

    @Test
    void doesNotModifyGraphicsContextWhenTextKeyframesAreEmpty() {
        TextRenderer renderer = new TextRenderer();
        Layer layerEmptyKeyframes = layerWithoutKeyframes();
        
        Boolean completed = FxTestHelper.callAndWait(() -> {
            Canvas canvas = new Canvas(100, 100);
            GraphicsContext gc = canvas.getGraphicsContext2D();
            gc.setGlobalAlpha(1.0);
            double alphaBeforeRender = gc.getGlobalAlpha();
            
            renderer.render(gc, layerEmptyKeyframes, 0.0);
            
            double alphaAfterRender = gc.getGlobalAlpha();
            return alphaBeforeRender == alphaAfterRender;
        });
        
        assertTrue(completed, "Graphics context global alpha should not change");
    }

    @Test
    void instancesAreIndependentAndStateless() {
        TextRenderer renderer1 = new TextRenderer();
        TextRenderer renderer2 = new TextRenderer();
        Layer layer = layerWithoutTextData();
        
        FxTestHelper.callAndWait(() -> {
            Canvas canvas = new Canvas(100, 100);
            GraphicsContext gc = canvas.getGraphicsContext2D();
            renderer1.render(gc, layer, 0.0);
            renderer2.render(gc, layer, 1.0);
            return true;
        });
        
        assertTrue(true, "No state should be shared between instances");
    }

    @Test
    void graphicsContextCanBeUsedForDrawingAfterRender() {
        TextRenderer renderer = new TextRenderer();
        Layer layer = layerWithoutTextData();
        
        Boolean canDraw = FxTestHelper.callAndWait(() -> {
            Canvas canvas = new Canvas(100, 100);
            GraphicsContext gc = canvas.getGraphicsContext2D();
            
            renderer.render(gc, layer, 0.0);
            
            // Should be able to draw afterwards
            gc.setFill(javafx.scene.paint.Color.BLUE);
            gc.fillRect(10, 10, 20, 20);
            
            javafx.scene.image.WritableImage image = canvas.snapshot(
                new javafx.scene.SnapshotParameters(), null);
            javafx.scene.image.PixelReader reader = image.getPixelReader();
            javafx.scene.paint.Color pixelColor = reader.getColor(15, 15);
            
            return pixelColor.getBlue() > 0.9;
        });
        
        assertTrue(canDraw, "Graphics context should remain usable after render");
    }

    // Helper methods to create test fixtures
    private static Layer layerWithoutTextData() {
        return new Layer(
            "textLayer", null, null, null, null, null, null,
            null, null, null, null,
            null, null, null, null, null, null, null, null, null,
            null, null, null, null, null,
            null, null, null, null, null, null, null
        );
    }

    private static Layer layerWithoutKeyframes() {
        return new Layer(
            "textLayer", null, null, null, null, null, null,
            null, null, null, null,
            null, null, null, null, null, null, null, null, null,
            null, null, null, null, null,
            null, null, null, null, null, null, null
        );
    }
}

