package com.lottie4j.fxplayer.element;

import com.lottie4j.core.definition.GradientType;
import com.lottie4j.core.model.shape.style.GradientFill;
import com.lottie4j.fxplayer.util.FxTestHelper;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for GradientFillStyle.
 * Validates gradient paint generation and coordinate transformations.
 */
class GradientFillStyleBasicTest {

    @BeforeAll
    static void initToolkit() {
        FxTestHelper.initToolkit();
    }

    @Test
    void nullGradientFillReturnsBlack() {
        GradientFillStyle gradientStyle = new GradientFillStyle(null);
        Paint paint = gradientStyle.getPaint(0.0);
        
        assertEquals(Color.BLACK, paint);
    }

    @Test
    void nullColorsReturnsBlack() {
        GradientFill gradientFill = new GradientFill(
                null, null, null, null, null, null, null,
                null, null, null, null, null, null,
                GradientType.LINEAR,
                null, null, null
        );
        GradientFillStyle gradientStyle = new GradientFillStyle(gradientFill);
        Paint paint = gradientStyle.getPaint(0.0);
        
        assertEquals(Color.BLACK, paint);
    }

    @Test
    void multipleFramesReturnValidPaints() {
        GradientFill gradientFill = new GradientFill(
                null, null, null, null, null, null, null,
                null, null, null, null, null, null,
                GradientType.LINEAR,
                null, null, null
        );
        GradientFillStyle gradientStyle = new GradientFillStyle(gradientFill);
        
        Paint paint1 = gradientStyle.getPaint(0.0);
        Paint paint2 = gradientStyle.getPaint(10.0);
        Paint paint3 = gradientStyle.getPaint(20.0);
        
        assertNotNull(paint1);
        assertNotNull(paint2);
        assertNotNull(paint3);
    }

    @Test
    void gradientStyleCanBeReused() {
        GradientFill gradientFill = new GradientFill(
                null, null, null, null, null, null, null,
                null, null, null, null, null, null,
                GradientType.LINEAR,
                null, null, null
        );
        GradientFillStyle gradientStyle = new GradientFillStyle(gradientFill);
        
        Paint paint1 = gradientStyle.getPaint(0.0);
        Paint paint2 = gradientStyle.getPaint(0.0, 0.0, 0.0, 100.0, 100.0);
        Paint paint3 = gradientStyle.getPaint(10.0, 10.0, 10.0, 200.0, 200.0);
        
        assertNotNull(paint1);
        assertNotNull(paint2);
        assertNotNull(paint3);
    }

    @Test
    void zeroShapeBoundsReturnValidPaint() {
        GradientFill gradientFill = new GradientFill(
                null, null, null, null, null, null, null,
                null, null, null, null, null, null,
                GradientType.LINEAR,
                null, null, null
        );
        GradientFillStyle gradientStyle = new GradientFillStyle(gradientFill);
        
        Paint paint = gradientStyle.getPaint(0.0, 0.0, 0.0, 0.0, 0.0);
        
        assertNotNull(paint);
    }

    @Test
    void gradientFillStyleCanBeInstantiated() {
        GradientFill gradientFill = new GradientFill(
                null, null, null, null, null, null, null,
                null, null, null, null, null, null,
                GradientType.RADIAL,
                null, null, null
        );
        GradientFillStyle gradientStyle = new GradientFillStyle(gradientFill);
        
        assertNotNull(gradientStyle);
    }
}

