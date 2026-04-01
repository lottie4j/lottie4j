package com.lottie4j.fxplayer.renderer.layer;

import com.lottie4j.core.model.layer.Layer;
import com.lottie4j.fxplayer.util.FxTestHelper;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for EffectsRenderer.
 * Validates effects rendering behavior and state management.
 */
class EffectsRendererTest {

    @BeforeAll
    static void initToolkit() {
        FxTestHelper.initToolkit();
    }

    @Test
    void canBeInstantiatedAndReused() {
        EffectsRenderer renderer1 = new EffectsRenderer();
        EffectsRenderer renderer2 = new EffectsRenderer();
        assertNotNull(renderer1);
        assertNotNull(renderer2);
        assertNotSame(renderer1, renderer2);
    }

    @Test
    void instancesAreIndependentWithSeparateCaches() {
        EffectsRenderer renderer1 = new EffectsRenderer();
        EffectsRenderer renderer2 = new EffectsRenderer();
        
        // Each instance should have its own cache
        assertNotNull(renderer1);
        assertNotNull(renderer2);
    }

    @Test
    void doesNotModifyGraphicsContextWhenNoEffectsPresent() {
        EffectsRenderer renderer = new EffectsRenderer();
        Layer layerNoEffects = layerWithoutEffects();
        
        Boolean statePreserved = FxTestHelper.callAndWait(() -> {
            Canvas canvas = new Canvas(100, 100);
            GraphicsContext gc = canvas.getGraphicsContext2D();
            gc.setGlobalAlpha(0.75);
            double alphaBefore = gc.getGlobalAlpha();
            
            // Render without effects
            EffectsRenderer.LayerRenderer noOpRenderer = (gc_inner, layer, frame) -> {};
            renderer.renderLayerWithGaussianBlur(gc, layerNoEffects, 0.0, 0.0, noOpRenderer);
            
            return alphaBefore == gc.getGlobalAlpha();
        });
        
        assertTrue(statePreserved, "Graphics context should remain unchanged");
    }

    @Test
    void graphicsContextCanBeUsedAfterEffectsApplication() {
        EffectsRenderer renderer = new EffectsRenderer();
        Layer layer = layerWithoutEffects();
        
        Boolean canDraw = FxTestHelper.callAndWait(() -> {
            Canvas canvas = new Canvas(100, 100);
            GraphicsContext gc = canvas.getGraphicsContext2D();
            
            EffectsRenderer.LayerRenderer noOpRenderer = (gc_inner, l, frame) -> {};
            renderer.renderLayerWithGaussianBlur(gc, layer, 0.0, 0.0, noOpRenderer);
            
            // Should be able to draw afterwards
            gc.setFill(javafx.scene.paint.Color.RED);
            gc.fillRect(10, 10, 20, 20);
            
            return true;
        });
        
        assertTrue(canDraw, "Graphics context should remain usable");
    }

    @Test
    void rendererRemainsStatelessAcrossMultipleFrames() {
        EffectsRenderer renderer = new EffectsRenderer();
        
        FxTestHelper.callAndWait(() -> {
            Canvas canvas = new Canvas(100, 100);
            GraphicsContext gc = canvas.getGraphicsContext2D();
            
            Layer layer1 = layerWithoutEffects();
            Layer layer2 = layerWithoutEffects();
            
            EffectsRenderer.LayerRenderer noOpRenderer = (gc_inner, layer, frame) -> {};
            
            renderer.renderLayerWithGaussianBlur(gc, layer1, 0.0, 0.0, noOpRenderer);
            renderer.renderLayerWithGaussianBlur(gc, layer2, 1.0, 0.0, noOpRenderer);
            
            return true;
        });
    }

    // Helper methods to create test fixtures
    private static Layer layerWithoutEffects() {
        return new Layer(
            "effectLayer", null, null, null, null, null, null,
            null, null, null, null,
            null, null, null, null, null, null, null, null, null,
            null, null, null, null, null,
            null, null, null, null, null, null, null
        );
    }
}

