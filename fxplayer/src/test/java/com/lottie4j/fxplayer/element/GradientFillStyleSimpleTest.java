package com.lottie4j.fxplayer.element;

import com.lottie4j.fxplayer.util.FxTestHelper;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for GradientFillStyle.
 * Validates basic functionality without creating complex test data.
 */
class GradientFillStyleSimpleTest {

    @BeforeAll
    static void initToolkit() {
        FxTestHelper.initToolkit();
    }

    @Test
    void nullGradientFillReturnsBlack() {
        GradientFillStyle gradientStyle = new GradientFillStyle(null);
        Color paint = (Color) gradientStyle.getPaint(0.0);
        
        assertEquals(Color.BLACK, paint);
    }

    @Test
    void getPaintWithFrameReturnsNotNull() {
        GradientFillStyle gradientStyle = new GradientFillStyle(null);
        Object paint = gradientStyle.getPaint(0.0);
        
        assertNotNull(paint);
    }

    @Test
    void getPaintWithShapeBoundsReturnsNotNull() {
        GradientFillStyle gradientStyle = new GradientFillStyle(null);
        Object paint = gradientStyle.getPaint(0.0, 10.0, 20.0, 100.0, 100.0);
        
        assertNotNull(paint);
    }

    @Test
    void multipleFramesReturnPaint() {
        GradientFillStyle gradientStyle = new GradientFillStyle(null);
        
        Object paint1 = gradientStyle.getPaint(0.0);
        Object paint2 = gradientStyle.getPaint(10.0);
        Object paint3 = gradientStyle.getPaint(20.0);
        
        assertNotNull(paint1);
        assertNotNull(paint2);
        assertNotNull(paint3);
    }

    @Test
    void gradientStyleCanBeCreated() {
        GradientFillStyle gradientStyle = new GradientFillStyle(null);
        assertNotNull(gradientStyle);
    }

    @Test
    void zeroShapeBoundsReturnPaint() {
        GradientFillStyle gradientStyle = new GradientFillStyle(null);
        Object paint = gradientStyle.getPaint(0.0, 0.0, 0.0, 0.0, 0.0);
        
        assertNotNull(paint);
    }
}

