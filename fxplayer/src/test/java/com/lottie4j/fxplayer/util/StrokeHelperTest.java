package com.lottie4j.fxplayer.util;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import com.lottie4j.fxplayer.util.FxTestHelper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

class StrokeHelperTest {

    private static final double EPS = 0.001;

    @BeforeAll
    static void initToolkit() {
        FxTestHelper.initToolkit();
    }

    @Test
    void shouldRenderStrokeReturnsTrueForPositiveWidth() {
        assertTrue(StrokeHelper.shouldRenderStroke(1.0));
        assertTrue(StrokeHelper.shouldRenderStroke(0.5));
        assertTrue(StrokeHelper.shouldRenderStroke(10.0));
    }

    @Test
    void shouldRenderStrokeReturnsFalseForTinyWidth() {
        assertFalse(StrokeHelper.shouldRenderStroke(0.0001));
        assertFalse(StrokeHelper.shouldRenderStroke(0.0));
        assertFalse(StrokeHelper.shouldRenderStroke(-1.0));
    }

    @Test
    void shouldRenderStrokeUsesMinimumThreshold() {
        // Just above threshold
        assertTrue(StrokeHelper.shouldRenderStroke(0.002));
        // Just at or below threshold
        assertFalse(StrokeHelper.shouldRenderStroke(0.001));
    }

    @Test
    void getCompensatedStrokeWidthReturnsOriginalValue() {
        Double result = FxTestHelper.callAndWait(() -> {
            Canvas canvas = new Canvas(100, 100);
            GraphicsContext gc = canvas.getGraphicsContext2D();
            return StrokeHelper.getCompensatedStrokeWidth(gc, 5.0);
        });

        assertEquals(5.0, result, EPS);
    }

    @Test
    void getCompensatedStrokeWidthNoScaleCompensation() {
        Double result = FxTestHelper.callAndWait(() -> {
            Canvas canvas = new Canvas(100, 100);
            GraphicsContext gc = canvas.getGraphicsContext2D();
            gc.scale(2.0, 2.0);
            return StrokeHelper.getCompensatedStrokeWidth(gc, 3.5);
        });

        assertEquals(3.5, result, EPS);
    }
}

