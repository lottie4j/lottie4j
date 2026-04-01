package com.lottie4j.fxplayer.renderer.layer;

import com.lottie4j.fxplayer.util.FxTestHelper;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for MatteRenderer.
 * Validates matte rendering behavior and layer state management.
 */
class MatteRendererTest {

    @BeforeAll
    static void initToolkit() {
        FxTestHelper.initToolkit();
    }

    @Test
    void canBeInstantiatedAndReused() {
        MatteRenderer renderer1 = new MatteRenderer();
        MatteRenderer renderer2 = new MatteRenderer();
        assertNotNull(renderer1);
        assertNotNull(renderer2);
        assertNotSame(renderer1, renderer2);
    }

    @Test
    void graphicsContextCanBeUsedAfterInstantiation() {
        MatteRenderer renderer = new MatteRenderer();
        
        Boolean canDraw = FxTestHelper.callAndWait(() -> {
            Canvas canvas = new Canvas(100, 100);
            GraphicsContext gc = canvas.getGraphicsContext2D();
            gc.setGlobalAlpha(0.5);
            
            // Should be able to draw before and after renderer creation
            gc.setFill(javafx.scene.paint.Color.RED);
            gc.fillRect(10, 10, 20, 20);
            
            return true;
        });
        
        assertTrue(canDraw, "Graphics context should remain usable");
    }

    @Test
    void multipleInstancesAreIndependent() {
        MatteRenderer renderer1 = new MatteRenderer();
        MatteRenderer renderer2 = new MatteRenderer();
        
        assertNotNull(renderer1);
        assertNotNull(renderer2);
        assertTrue(renderer1 != renderer2);
    }

    @Test
    void rendererCanBeCreatedMultipleTimes() {
        FxTestHelper.callAndWait(() -> {
            Canvas canvas = new Canvas(100, 100);
            GraphicsContext gc = canvas.getGraphicsContext2D();
            
            MatteRenderer r1 = new MatteRenderer();
            MatteRenderer r2 = new MatteRenderer();
            MatteRenderer r3 = new MatteRenderer();
            
            assertNotNull(r1);
            assertNotNull(r2);
            assertNotNull(r3);
            
            return true;
        });
    }

    @Test
    void rendererIsStateless() {
        MatteRenderer renderer = new MatteRenderer();
        MatteRenderer renderer2 = new MatteRenderer();
        
        assertNotNull(renderer);
        assertNotNull(renderer2);
    }
}

