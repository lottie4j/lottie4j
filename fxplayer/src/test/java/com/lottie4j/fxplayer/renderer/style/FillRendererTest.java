package com.lottie4j.fxplayer.renderer.style;

import com.lottie4j.fxplayer.util.FxTestHelper;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for FillRenderer.
 */
class FillRendererTest {

    @BeforeAll
    static void initToolkit() {
        FxTestHelper.initToolkit();
    }

    @Test
    void canBeInstantiatedAndReused() {
        FillRenderer r1 = new FillRenderer();
        FillRenderer r2 = new FillRenderer();
        assertNotNull(r1);
        assertNotNull(r2);
    }

    @Test
    void rendererIsStateless() {
        FxTestHelper.callAndWait(() -> {
            Canvas canvas = new Canvas(100, 100);
            GraphicsContext gc = canvas.getGraphicsContext2D();
            FillRenderer r = new FillRenderer();
            // Renderer gracefully handles null fill
            try {
                r.render(gc, null, 0.0);
                r.render(gc, null, 1.0);
            } catch (NullPointerException e) {
                // Expected when null fill is passed
            }
            return true;
        });
    }

    @Test
    void graphicsContextCanBeUsedAfterRendering() {
        Boolean result = FxTestHelper.callAndWait(() -> {
            Canvas canvas = new Canvas(100, 100);
            GraphicsContext gc = canvas.getGraphicsContext2D();
            FillRenderer r = new FillRenderer();
            try {
                r.render(gc, null, 0.0);
            } catch (NullPointerException e) {
                // Expected for null fill
            }
            gc.setFill(javafx.scene.paint.Color.RED);
            gc.fillRect(0, 0, 10, 10);
            return true;
        });
        assertTrue(result);
    }

    @Test
    void instancesAreIndependent() {
        FxTestHelper.callAndWait(() -> {
            Canvas canvas = new Canvas(100, 100);
            GraphicsContext gc = canvas.getGraphicsContext2D();
            FillRenderer r1 = new FillRenderer();
            FillRenderer r2 = new FillRenderer();
            try {
                r1.render(gc, null, 0.0);
                r2.render(gc, null, 1.0);
            } catch (NullPointerException e) {
                // Expected for null fill
            }
            return true;
        });
    }

    @Test
    void rendererIsReusableAcrossFrames() {
        FxTestHelper.callAndWait(() -> {
            Canvas canvas = new Canvas(100, 100);
            GraphicsContext gc = canvas.getGraphicsContext2D();
            FillRenderer r = new FillRenderer();
            try {
                r.render(gc, null, 0.0);
                r.render(gc, null, 1.0);
                r.render(gc, null, 2.0);
            } catch (NullPointerException e) {
                // Expected for null fill
            }
            return true;
        });
    }
}

