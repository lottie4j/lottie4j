package com.lottie4j.fxplayer.element;

import com.lottie4j.fxplayer.util.FxTestHelper;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for GradientStrokeStyle.
 * Validates basic functionality without creating complex test data.
 */
class GradientStrokeStyleSimpleTest {

    @BeforeAll
    static void initToolkit() {
        FxTestHelper.initToolkit();
    }

    @Test
    void nullGradientStrokeReturnsBlack() {
        GradientStrokeStyle gradientStyle = new GradientStrokeStyle(null);
        Color paint = (Color) gradientStyle.getPaint(0.0);
        
        assertEquals(Color.BLACK, paint);
    }

    @Test
    void getPaintWithFrameReturnsNotNull() {
        GradientStrokeStyle gradientStyle = new GradientStrokeStyle(null);
        Object paint = gradientStyle.getPaint(0.0);
        
        assertNotNull(paint);
    }

    @Test
    void getPaintWithShapeBoundsReturnsNotNull() {
        GradientStrokeStyle gradientStyle = new GradientStrokeStyle(null);
        Object paint = gradientStyle.getPaint(0.0, 0.0, 0.0, 100.0, 100.0);
        
        assertNotNull(paint);
    }

    @Test
    void multipleFramesReturnPaint() {
        GradientStrokeStyle gradientStyle = new GradientStrokeStyle(null);
        
        Object paint1 = gradientStyle.getPaint(0.0);
        Object paint2 = gradientStyle.getPaint(10.0);
        Object paint3 = gradientStyle.getPaint(20.0);
        
        assertNotNull(paint1);
        assertNotNull(paint2);
        assertNotNull(paint3);
    }

    @Test
    void gradientStrokeStyleCanBeCreated() {
        GradientStrokeStyle gradientStyle = new GradientStrokeStyle(null);
        assertNotNull(gradientStyle);
    }

    @Test
    void largeBoundsReturnPaint() {
        GradientStrokeStyle gradientStyle = new GradientStrokeStyle(null);
        Object paint = gradientStyle.getPaint(0.0, 0.0, 0.0, 10000.0, 10000.0);
        
        assertNotNull(paint);
    }

    @Test
    void negativeBoundsReturnPaint() {
        GradientStrokeStyle gradientStyle = new GradientStrokeStyle(null);
        Object paint = gradientStyle.getPaint(0.0, -10.0, -20.0, -100.0, -100.0);
        
        assertNotNull(paint);
    }
}

