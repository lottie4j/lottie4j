package com.lottie4j.fxplayer.element;

import com.lottie4j.core.definition.GradientType;
import com.lottie4j.core.model.animation.Animated;
import com.lottie4j.core.model.keyframe.Keyframe;
import com.lottie4j.core.model.keyframe.NumberKeyframe;
import com.lottie4j.core.model.shape.style.GradientFill;
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

public class GradientFillStyleTest {

    @BeforeAll
    public static void initToolkit() {
        FxTestHelper.initToolkit();
    }

    @Test
    void nullGradientFillReturnsBlack() {
        Paint paint = new GradientFillStyle(null).getPaint(0.0);
        assertEquals(Color.BLACK, paint);
    }

    @Test
    void nullColorsReturnsBlack() {
        GradientFill fill = new GradientFill(null, null, null, null, null, null, null,
                null, null, null, null, null, null, GradientType.LINEAR, null, null, null);
        Paint paint = new GradientFillStyle(fill).getPaint(0.0);
        assertEquals(Color.BLACK, paint);
    }

    @Test
    void linearGradientReturnsLinearPaint() {
        Paint paint = new GradientFillStyle(buildGradient(GradientType.LINEAR)).getPaint(0.0);
        assertInstanceOf(LinearGradient.class, paint);
    }

    @Test
    void radialGradientReturnsRadialPaint() {
        Paint paint = new GradientFillStyle(buildGradient(GradientType.RADIAL)).getPaint(0.0);
        assertInstanceOf(RadialGradient.class, paint);
    }

    @Test
    void proportionalBoundsStillReturnLinearGradient() {
        Paint paint = new GradientFillStyle(buildGradient(GradientType.LINEAR))
                .getPaint(0.0, 10.0, 20.0, 100.0, 100.0);
        assertInstanceOf(LinearGradient.class, paint);
    }

    private static GradientFill buildGradient(GradientType type) {
        // [offset, r, g, b, offset, r, g, b]
        Animated colors = animated(0.0, 1.0, 0.0, 0.0, 1.0, 0.0, 0.0, 1.0);
        GradientFill.GradientColor gradientColor = new GradientFill.GradientColor(2, colors);

        Animated start = animated(0.0, 0.0);
        Animated end = animated(100.0, 100.0);

        return new GradientFill(
                null, null, null, null, null, null, null,
                null, null,
                null, null,
                start,
                end,
                type,
                gradientColor,
                null, null
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

