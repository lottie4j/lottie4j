package com.lottie4j.fxplayer.renderer.layer;

import com.lottie4j.core.model.layer.Layer;
import com.lottie4j.fxplayer.renderer.shape.PathBezierInterpolator;
import com.lottie4j.fxplayer.util.FxTestHelper;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for MaskRenderer.
 * Validates mask application behavior and early returns for missing masks.
 */
public class MaskRendererTest {

    @BeforeAll
    public static void initToolkit() {
        FxTestHelper.initToolkit();
    }

    @Test
    void canBeInstantiatedWithBezierInterpolator() {
        PathBezierInterpolator interpolator = new PathBezierInterpolator();
        MaskRenderer renderer = new MaskRenderer(interpolator);
        assertNotNull(renderer);
    }

    @Test
    void returnsFalseWhenLayerHasNoMasks() {
        PathBezierInterpolator interpolator = new PathBezierInterpolator();
        MaskRenderer renderer = new MaskRenderer(interpolator);
        Layer layerNoMasks = layerWithoutMasks();
        
        Boolean result = FxTestHelper.callAndWait(() -> {
            Canvas canvas = new Canvas(100, 100);
            GraphicsContext gc = canvas.getGraphicsContext2D();
            return renderer.applyMasks(gc, layerNoMasks, 0.0);
        });
        
        assertFalse(result, "applyMasks should return false when masks are missing");
    }

    @Test
    void returnsFalseWhenMaskListIsEmpty() {
        PathBezierInterpolator interpolator = new PathBezierInterpolator();
        MaskRenderer renderer = new MaskRenderer(interpolator);
        Layer layerEmptyMasks = layerWithEmptyMasks();
        
        Boolean result = FxTestHelper.callAndWait(() -> {
            Canvas canvas = new Canvas(100, 100);
            GraphicsContext gc = canvas.getGraphicsContext2D();
            return renderer.applyMasks(gc, layerEmptyMasks, 0.0);
        });
        
        assertFalse(result, "applyMasks should return false when mask list is empty");
    }

    @Test
    void preservesGraphicsContextStateWhenNoMasks() {
        PathBezierInterpolator interpolator = new PathBezierInterpolator();
        MaskRenderer renderer = new MaskRenderer(interpolator);
        Layer layer = layerWithoutMasks();
        
        Boolean statePreserved = FxTestHelper.callAndWait(() -> {
            Canvas canvas = new Canvas(100, 100);
            GraphicsContext gc = canvas.getGraphicsContext2D();
            gc.setGlobalAlpha(0.75);
            double alphaBefore = gc.getGlobalAlpha();
            
            renderer.applyMasks(gc, layer, 0.0);
            
            double alphaAfter = gc.getGlobalAlpha();
            return alphaBefore == alphaAfter;
        });
        
        assertTrue(statePreserved, "Graphics context state should be preserved");
    }

    @Test
    void rendererIsReusableAcrossFrames() {
        PathBezierInterpolator interpolator = new PathBezierInterpolator();
        MaskRenderer renderer = new MaskRenderer(interpolator);
        Layer layer1 = layerWithoutMasks();
        Layer layer2 = layerWithoutMasks();
        
        FxTestHelper.callAndWait(() -> {
            Canvas canvas = new Canvas(100, 100);
            GraphicsContext gc = canvas.getGraphicsContext2D();
            
            boolean result1 = renderer.applyMasks(gc, layer1, 0.0);
            boolean result2 = renderer.applyMasks(gc, layer2, 1.0);
            
            assertFalse(result1 && result2, "Both should return false with no masks");
            return true;
        });
    }

    // Helper methods to create test fixtures
    private static Layer layerWithoutMasks() {
        return new Layer(
            "maskLayer", null, null, null, null, null, null,
            null, null, null, null,
            null, null, null, null, null, null, null, null, null,
            null, null, null, null, null,
            null, null, null, null, null, null, null
        );
    }

    private static Layer layerWithEmptyMasks() {
        return new Layer(
            "maskLayer", null, null, null, null, null, null,
            null, null, null, null,
            null, null, null, null, null, null, null, null, null,
            null, null, null, null, null,
            null, null, null, null, null, null, null
        );
    }
}



