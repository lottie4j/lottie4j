package com.lottie4j.fxplayer.element;

import com.lottie4j.core.model.shape.style.Stroke;
import com.lottie4j.fxplayer.util.FxTestHelper;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for StrokeStyle.
 * Validates stroke color resolution and stroke width handling.
 */
public class StrokeStyleBasicTest {

    @BeforeAll
    public static void initToolkit() {
        FxTestHelper.initToolkit();
    }

    @Test
    void canBeInstantiatedWithStroke() {
        Stroke stroke = new Stroke(null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
        StrokeStyle strokeStyle = new StrokeStyle(stroke);
        assertNotNull(strokeStyle);
        assertEquals(stroke, strokeStyle.stroke());
    }

    @Test
    void nullColorReturnsBlack() {
        Stroke stroke = new Stroke(null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
        StrokeStyle strokeStyle = new StrokeStyle(stroke);
        
        Color color = strokeStyle.getColor(0.0);
        assertEquals(Color.BLACK, color);
    }

    @Test
    void nullStrokeWidthReturnsZero() {
        Stroke stroke = new Stroke(null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
        StrokeStyle strokeStyle = new StrokeStyle(stroke);
        
        Double width = strokeStyle.getStrokeWidth(0.0);
        
        assertEquals(0.0, width, 0.001);
    }

    @Test
    void multipleFramesReturnValidValues() {
        Stroke stroke = new Stroke(null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
        StrokeStyle strokeStyle = new StrokeStyle(stroke);
        
        Color color1 = strokeStyle.getColor(0.0);
        Color color2 = strokeStyle.getColor(10.0);
        Double width1 = strokeStyle.getStrokeWidth(0.0);
        Double width2 = strokeStyle.getStrokeWidth(10.0);
        
        assertNotNull(color1);
        assertNotNull(color2);
        assertNotNull(width1);
        assertNotNull(width2);
    }

    @Test
    void strokeStyleCanBeReused() {
        Stroke stroke = new Stroke(null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
        StrokeStyle strokeStyle = new StrokeStyle(stroke);
        
        // Multiple calls should work without side effects
        for (int i = 0; i < 5; i++) {
            Color color = strokeStyle.getColor(i);
            Double width = strokeStyle.getStrokeWidth(i);
            
            assertNotNull(color);
            assertNotNull(width);
            assertTrue(width >= 0);
        }
    }

    @Test
    void getColorReturnsBlackWhenColorNull() {
        Stroke stroke = new Stroke(null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
        StrokeStyle strokeStyle = new StrokeStyle(stroke);
        
        Color color = strokeStyle.getColor(0.0);
        
        assertEquals(Color.BLACK, color);
    }
}

