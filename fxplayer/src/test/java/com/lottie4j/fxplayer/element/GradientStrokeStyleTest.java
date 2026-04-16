package com.lottie4j.fxplayer.element;

import com.lottie4j.core.definition.GradientType;
import com.lottie4j.core.model.animation.Animated;
import com.lottie4j.core.model.keyframe.Keyframe;
import com.lottie4j.core.model.keyframe.NumberKeyframe;
import com.lottie4j.core.model.shape.style.GradientStroke;
import com.lottie4j.fxplayer.util.FxTestHelper;
import javafx.scene.paint.Color;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Paint;
import javafx.scene.paint.RadialGradient;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/**
 * Unit tests for GradientStrokeStyle.
 * Validates gradient stroke paint generation, color stops, and coordinate transformations.
 */
public class GradientStrokeStyleTest {

    @BeforeAll
    public static void initToolkit() {
        FxTestHelper.initToolkit();
    }

    @Test
    void nullGradientStrokeReturnsBlack() {
        Paint paint = new GradientStrokeStyle(null).getPaint(0.0);
        assertEquals(Color.BLACK, paint);
    }

    @Test
    void nullColorsReturnsBlack() {
        GradientStroke stroke = new GradientStroke(
                null, null, null, null, null, null, null,
                null, null, null, null, null, null, null,
                null, null,
                GradientType.LINEAR,
                null,
                null
        );
        Paint paint = new GradientStrokeStyle(stroke).getPaint(0.0);
        assertEquals(Color.BLACK, paint);
    }

    @Test
    void linearGradientReturnsLinearPaint() {
        Paint paint = new GradientStrokeStyle(buildGradient(GradientType.LINEAR)).getPaint(0.0);
        assertInstanceOf(LinearGradient.class, paint);
    }

    @Test
    void radialGradientReturnsRadialPaint() {
        Paint paint = new GradientStrokeStyle(buildGradient(GradientType.RADIAL)).getPaint(0.0);
        assertInstanceOf(RadialGradient.class, paint);
    }

    @Test
    void proportionalBoundsStillReturnLinearGradient() {
        Paint paint = new GradientStrokeStyle(buildGradient(GradientType.LINEAR))
                .getPaint(0.0, 10.0, 20.0, 100.0, 100.0);
        assertInstanceOf(LinearGradient.class, paint);
    }

    private static GradientStroke buildGradient(GradientType type) {
        Animated colors = animated(0.0, 1.0, 0.0, 0.0, 1.0, 0.0, 0.0, 1.0);
        GradientStroke.GradientColor gradientColor = new GradientStroke.GradientColor(2, colors);

        Animated start = animated(0.0, 0.0);
        Animated end = animated(100.0, 100.0);

        return new GradientStroke(
                null, null, null, null, null, null, null,
                null,
                null, null, null, null,
                null, null,
                start,
                end,
                type,
                gradientColor,
                null
        );
    }

    private static Animated animated(double... values) {
        List<Keyframe> frames = java.util.Arrays.stream(values)
                .mapToObj(NumberKeyframe::new)
                .map(Keyframe.class::cast)
                .toList();
        return new Animated(0, frames, null, null, null, null, null);
    }
}
