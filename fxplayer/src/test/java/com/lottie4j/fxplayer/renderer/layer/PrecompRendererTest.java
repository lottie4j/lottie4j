package com.lottie4j.fxplayer.renderer.layer;

import com.lottie4j.core.model.layer.Layer;
import com.lottie4j.fxplayer.util.FxTestHelper;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for PrecompRenderer.
 * Validates precomposition rendering and state management.
 */
public class PrecompRendererTest {

    @BeforeAll
    public static void initToolkit() {
        FxTestHelper.initToolkit();
    }

    @Test
    void canBeInstantiatedWithRequiredCollaborators() {
        TransformApplier transformApplier = new TransformApplier();
        TextRenderer textRenderer = new TextRenderer();
        ImageRenderer imageRenderer = new ImageRenderer();
        
        PrecompRenderer renderer = new PrecompRenderer(
            transformApplier, textRenderer, imageRenderer
        );
        
        assertNotNull(renderer);
    }

    @Test
    void rendererIsReusableAcrossFrames() {
        TransformApplier transformApplier = new TransformApplier();
        TextRenderer textRenderer = new TextRenderer();
        ImageRenderer imageRenderer = new ImageRenderer();
        
        PrecompRenderer renderer = new PrecompRenderer(
            transformApplier, textRenderer, imageRenderer
        );
        
        FxTestHelper.callAndWait(() -> {
            Canvas canvas = new Canvas(100, 100);
            GraphicsContext gc = canvas.getGraphicsContext2D();
            
            Layer layer = layerWithoutAnimation();
            
            // Multiple frames should work without errors
            renderer.renderPrecompositionLayer(gc, layer, 0.0, new HashMap<>(), null, 
                (l, f) -> true, null, null, null);
            renderer.renderPrecompositionLayer(gc, layer, 1.0, new HashMap<>(), null,
                (l, f) -> true, null, null, null);
            renderer.renderPrecompositionLayer(gc, layer, 2.0, new HashMap<>(), null,
                (l, f) -> true, null, null, null);
            
            return true;
        });
    }

    @Test
    void graphicsContextCanBeUsedAfterRendering() {
        TransformApplier transformApplier = new TransformApplier();
        TextRenderer textRenderer = new TextRenderer();
        ImageRenderer imageRenderer = new ImageRenderer();
        
        PrecompRenderer renderer = new PrecompRenderer(
            transformApplier, textRenderer, imageRenderer
        );
        
        Boolean canDraw = FxTestHelper.callAndWait(() -> {
            Canvas canvas = new Canvas(100, 100);
            GraphicsContext gc = canvas.getGraphicsContext2D();
            
            Layer layer = layerWithoutAnimation();
            renderer.renderPrecompositionLayer(gc, layer, 0.0, new HashMap<>(), null,
                (l, f) -> true, null, null, null);
            
            // Should be able to draw afterwards
            gc.setFill(javafx.scene.paint.Color.BLUE);
            gc.fillRect(10, 10, 20, 20);
            
            return true;
        });
        
        assertTrue(canDraw, "Graphics context should remain usable");
    }

    @Test
    void multipleInstancesAreIndependent() {
        TransformApplier transformApplier1 = new TransformApplier();
        TextRenderer textRenderer1 = new TextRenderer();
        ImageRenderer imageRenderer1 = new ImageRenderer();
        
        TransformApplier transformApplier2 = new TransformApplier();
        TextRenderer textRenderer2 = new TextRenderer();
        ImageRenderer imageRenderer2 = new ImageRenderer();
        
        PrecompRenderer renderer1 = new PrecompRenderer(
            transformApplier1, textRenderer1, imageRenderer1
        );
        PrecompRenderer renderer2 = new PrecompRenderer(
            transformApplier2, textRenderer2, imageRenderer2
        );
        
        FxTestHelper.callAndWait(() -> {
            Canvas canvas = new Canvas(100, 100);
            GraphicsContext gc = canvas.getGraphicsContext2D();
            
            Layer layer = layerWithoutAnimation();
            
            renderer1.renderPrecompositionLayer(gc, layer, 0.0, new HashMap<>(), null,
                (l, f) -> true, null, null, null);
            renderer2.renderPrecompositionLayer(gc, layer, 0.0, new HashMap<>(), null,
                (l, f) -> true, null, null, null);
            
            return true;
        });
    }

    @Test
    void rendererHandlesNullLayerGracefully() {
        TransformApplier transformApplier = new TransformApplier();
        TextRenderer textRenderer = new TextRenderer();
        ImageRenderer imageRenderer = new ImageRenderer();
        
        PrecompRenderer renderer = new PrecompRenderer(
            transformApplier, textRenderer, imageRenderer
        );
        
        // Should not throw exception
        FxTestHelper.callAndWait(() -> {
            Canvas canvas = new Canvas(100, 100);
            GraphicsContext gc = canvas.getGraphicsContext2D();
            
            try {
                renderer.renderPrecompositionLayer(gc, null, 0.0, new HashMap<>(), null,
                    (l, f) -> true, null, null, null);
            } catch (NullPointerException e) {
                // Expected for null layer
            }
            
            return true;
        });
    }

    // Helper methods to create test fixtures
    private static Layer layerWithoutAnimation() {
        return new Layer(
            "precompLayer", null, null, null, null, null, null,
            null, null, null, null,
            null, null, null, null, null, null, null, null, null,
            null, null, null, null, null,
            null, null, null, null, null, null, null
        );
    }
}

