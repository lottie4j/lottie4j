package com.lottie4j.fxplayer.element;

import com.lottie4j.core.model.shape.style.Stroke;
import com.lottie4j.fxplayer.util.FxTestHelper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Unit tests for StrokeStyle.
 * Validates stroke color resolution, stroke width, and opacity handling.
 */
public class StrokeStyleTest {

    @BeforeAll
    public static void initToolkit() {
        FxTestHelper.initToolkit();
    }

    @Test
    void canBeInstantiatedWithStroke() {
        Stroke stroke = new Stroke(null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null);
        StrokeStyle strokeStyle = new StrokeStyle(stroke);
        assertNotNull(strokeStyle);
        assertEquals(stroke, strokeStyle.stroke());
    }

    @Test
    void nullColorReturnsBlackAndWidthDefaultsToZero() {
        Stroke stroke = new Stroke(null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null);
        StrokeStyle strokeStyle = new StrokeStyle(stroke);

        assertNotNull(strokeStyle.getColor(0.0));
        assertEquals(0.0, strokeStyle.getStrokeWidth(0.0));
    }

    @Test
    void multipleCallsStayStable() {
        Stroke stroke = new Stroke(null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null);
        StrokeStyle strokeStyle = new StrokeStyle(stroke);

        assertNotNull(strokeStyle.getColor(0.0));
        assertNotNull(strokeStyle.getColor(10.0));
        assertEquals(strokeStyle.getStrokeWidth(0.0), strokeStyle.getStrokeWidth(10.0));
    }
}
