package com.lottie4j.fxplayer.element;

import com.lottie4j.core.definition.GradientType;
import com.lottie4j.core.model.shape.style.GradientStroke;
import com.lottie4j.fxplayer.util.FxTestHelper;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for GradientStrokeStyle.
 * Validates gradient stroke paint generation and coordinate transformations.
 */
public class GradientStrokeStyleBasicTest {

    @BeforeAll
    public static void initToolkit() {
        FxTestHelper.initToolkit();
    }

    @Test
    void nullGradientStrokeReturnsBlack() {
        GradientStrokeStyle gradientStyle = new GradientStrokeStyle(null);
        Paint paint = gradientStyle.getPaint(0.0);
        
        assertEquals(Color.BLACK, paint);
    }

    @Test
    void nullColorsReturnsBlack() {
        GradientStroke gradientStroke = new GradientStroke(
                null, null, null, null, null, null, null,
                null,
                null, null, null, null,
                null, null,
                null, null,
                GradientType.LINEAR,
                null,
                null
        );
        GradientStrokeStyle gradientStyle = new GradientStrokeStyle(gradientStroke);
        Paint paint = gradientStyle.getPaint(0.0);
        
        assertEquals(Color.BLACK, paint);
    }

    @Test
    void multipleFramesReturnValidPaints() {
        GradientStroke gradientStroke = new GradientStroke(
                null, null, null, null, null, null, null,
                null,
                null, null, null, null,
                null, null,
                null, null,
                GradientType.LINEAR,
                null,
                null
        );
        GradientStrokeStyle gradientStyle = new GradientStrokeStyle(gradientStroke);
        
        Paint paint1 = gradientStyle.getPaint(0.0);
        Paint paint2 = gradientStyle.getPaint(10.0);
        Paint paint3 = gradientStyle.getPaint(20.0);
        
        assertNotNull(paint1);
        assertNotNull(paint2);
        assertNotNull(paint3);
    }

    @Test
    void gradientStrokeStyleCanBeReused() {
        GradientStroke gradientStroke = new GradientStroke(
                null, null, null, null, null, null, null,
                null,
                null, null, null, null,
                null, null,
                null, null,
                GradientType.LINEAR,
                null,
                null
        );
        GradientStrokeStyle gradientStyle = new GradientStrokeStyle(gradientStroke);
        
        Paint paint1 = gradientStyle.getPaint(0.0);
        Paint paint2 = gradientStyle.getPaint(0.0, 0.0, 0.0, 100.0, 100.0);
        Paint paint3 = gradientStyle.getPaint(10.0, 10.0, 10.0, 200.0, 200.0);
        
        assertNotNull(paint1);
        assertNotNull(paint2);
        assertNotNull(paint3);
    }

    @Test
    void zeroShapeBoundsReturnValidPaint() {
        GradientStroke gradientStroke = new GradientStroke(
                null, null, null, null, null, null, null,
                null,
                null, null, null, null,
                null, null,
                null, null,
                GradientType.LINEAR,
                null,
                null
        );
        GradientStrokeStyle gradientStyle = new GradientStrokeStyle(gradientStroke);
        
        Paint paint = gradientStyle.getPaint(0.0, 0.0, 0.0, 0.0, 0.0);
        
        assertNotNull(paint);
    }

    @Test
    void largeBoundsReturnValidPaint() {
        GradientStroke gradientStroke = new GradientStroke(
                null, null, null, null, null, null, null,
                null,
                null, null, null, null,
                null, null,
                null, null,
                GradientType.LINEAR,
                null,
                null
        );
        GradientStrokeStyle gradientStyle = new GradientStrokeStyle(gradientStroke);
        
        Paint paint = gradientStyle.getPaint(0.0, 0.0, 0.0, 10000.0, 10000.0);
        
        assertNotNull(paint);
    }

    @Test
    void radialGradientStrokeCanBeUsed() {
        GradientStroke gradientStroke = new GradientStroke(
                null, null, null, null, null, null, null,
                null,
                null, null, null, null,
                null, null,
                null, null,
                GradientType.RADIAL,
                null,
                null
        );
        GradientStrokeStyle gradientStyle = new GradientStrokeStyle(gradientStroke);
        
        Paint paint = gradientStyle.getPaint(0.0);
        
        assertNotNull(paint);
    }
}

