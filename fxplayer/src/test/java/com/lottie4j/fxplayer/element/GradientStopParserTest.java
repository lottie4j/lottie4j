package com.lottie4j.fxplayer.element;

import com.lottie4j.core.model.animation.Animated;
import com.lottie4j.core.model.keyframe.Keyframe;
import com.lottie4j.core.model.keyframe.NumberKeyframe;
import javafx.scene.paint.Color;
import javafx.scene.paint.Stop;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link GradientStopParser}.
 *
 * <p>Covers the layouts described in the Lottie spec: {@code n} colour stops
 * encoded as {@code [offset, r, g, b]} optionally followed by {@code m} alpha
 * stops encoded as {@code [offset, alpha]}.</p>
 */
class GradientStopParserTest {

    private static final double EPS = 1e-9;

    private static Animated animatedFlat(double... values) {
        List<Keyframe> frames = Arrays.stream(values)
                .mapToObj(NumberKeyframe::new)
                .map(Keyframe.class::cast)
                .toList();
        return new Animated(0, frames, null, null, null, null, null);
    }

    @Test
    void twoColorStopsNoAlphaTail() {
        Animated data = animatedFlat(
                0.0, 1.0, 0.0, 0.0,
                1.0, 0.0, 0.0, 1.0
        );

        List<Stop> stops = GradientStopParser.parseStops(data, 2);

        assertEquals(2, stops.size());
        assertStop(stops.get(0), 0.0, 1.0, 0.0, 0.0, 1.0);
        assertStop(stops.get(1), 1.0, 0.0, 0.0, 1.0, 1.0);
    }

    @Test
    void threeColorStopsNoAlphaTail() {
        Animated data = animatedFlat(
                0.0, 1.0, 0.945, 0.463,
                0.5, 1.0, 0.857, 0.231,
                1.0, 1.0, 0.769, 0.0
        );

        List<Stop> stops = GradientStopParser.parseStops(data, 3);

        assertEquals(3, stops.size());
        assertStop(stops.get(0), 0.0, 1.0, 0.945, 0.463, 1.0);
        assertStop(stops.get(1), 0.5, 1.0, 0.857, 0.231, 1.0);
        assertStop(stops.get(2), 1.0, 1.0, 0.769, 0.0, 1.0);
    }

    @Test
    void twoColorStopsTwoMatchingAlphaStops() {
        Animated data = animatedFlat(
                0.0, 1.0, 0.0, 0.0,
                1.0, 0.0, 0.0, 1.0,
                // alpha section
                0.0, 1.0,
                1.0, 0.5
        );

        List<Stop> stops = GradientStopParser.parseStops(data, 2);

        assertEquals(2, stops.size());
        assertStop(stops.get(0), 0.0, 1.0, 0.0, 0.0, 1.0);
        assertStop(stops.get(1), 1.0, 0.0, 0.0, 1.0, 0.5);
    }

    @Test
    void threeColorStopsThreeAlphaStops() {
        // Typical isometric_data_analysis layout: alpha tracks colour offsets exactly.
        Animated data = animatedFlat(
                0.0, 0.18, 0.231, 0.733,
                0.5, 0.314, 0.288, 0.79,
                1.0, 0.447, 0.341, 0.851,
                0.0, 1.0,
                0.5, 0.8,
                1.0, 0.4
        );

        List<Stop> stops = GradientStopParser.parseStops(data, 3);

        assertEquals(3, stops.size());
        assertStop(stops.get(0), 0.0, 0.18, 0.231, 0.733, 1.0);
        assertStop(stops.get(1), 0.5, 0.314, 0.288, 0.79, 0.8);
        assertStop(stops.get(2), 1.0, 0.447, 0.341, 0.851, 0.4);
    }

    @Test
    void alphaOffsetsDifferentFromColorOffsetsAreMerged() {
        // Mirrors face-peeking.json gradient at line 27996:
        //  - 5 colour stops at unrelated offsets
        //  - 3 alpha stops that include an offset (0.378) not present in colours
        Animated data = animatedFlat(
                0.0, 0.957, 0.635, 0.137,
                0.193, 0.963, 0.694, 0.153,
                0.386, 0.969, 0.753, 0.169,
                0.588, 0.98, 0.816, 0.178,
                0.744, 0.992, 0.878, 0.188,
                // alpha section with non-aligned offsets
                0.0, 1.0,
                0.378, 0.5,
                0.756, 0.0
        );

        List<Stop> stops = GradientStopParser.parseStops(data, 5);

        // Union of offsets: 0, 0.193, 0.378, 0.386, 0.588, 0.744, 0.756 => 7 stops
        assertEquals(7, stops.size());

        // Stops must be sorted by offset.
        for (int i = 1; i < stops.size(); i++) {
            assertTrue(stops.get(i).getOffset() >= stops.get(i - 1).getOffset(),
                    "Stops should be sorted by offset");
        }

        // The alpha at offset 0.756 has dropped to zero — the late colour stops should
        // therefore render fully transparent rather than as their opaque RGB value.
        Stop lastStop = stops.get(stops.size() - 1);
        assertEquals(0.756, lastStop.getOffset(), EPS);
        assertEquals(0.0, lastStop.getColor().getOpacity(), EPS);

        // Alpha at offset 0.378 should be exactly 0.5 (matches an alpha keyframe).
        Stop alphaStop = stops.stream()
                .filter(s -> Math.abs(s.getOffset() - 0.378) < EPS)
                .findFirst().orElseThrow();
        assertEquals(0.5, alphaStop.getColor().getOpacity(), EPS);

        // Alpha at colour offset 0.193 must be interpolated between (0, 1.0) and (0.378, 0.5).
        Stop colorStop = stops.stream()
                .filter(s -> Math.abs(s.getOffset() - 0.193) < EPS)
                .findFirst().orElseThrow();
        double expectedAlpha = 1.0 + (0.5 - 1.0) * (0.193 / 0.378);
        assertEquals(expectedAlpha, colorStop.getColor().getOpacity(), 1e-6);
    }

    @Test
    void colorIsInterpolatedAtAlphaOnlyOffset() {
        // Colour stops at 0 and 1, alpha stops introduce extra offset at 0.5.
        Animated data = animatedFlat(
                0.0, 1.0, 0.0, 0.0,
                1.0, 0.0, 0.0, 1.0,
                0.0, 1.0,
                0.5, 0.25,
                1.0, 1.0
        );

        List<Stop> stops = GradientStopParser.parseStops(data, 2);

        assertEquals(3, stops.size());
        Stop mid = stops.get(1);
        assertEquals(0.5, mid.getOffset(), EPS);
        // Colour is the midpoint between red and blue: (0.5, 0, 0.5)
        Color c = mid.getColor();
        assertEquals(0.5, c.getRed(), 1e-6);
        assertEquals(0.0, c.getGreen(), 1e-6);
        assertEquals(0.5, c.getBlue(), 1e-6);
        assertEquals(0.25, c.getOpacity(), 1e-6);
    }

    @Test
    void colorChannelOrderIsRgbNotBgr() {
        // Distinct values per channel to detect any swap.
        Animated data = animatedFlat(
                0.0, 0.1, 0.4, 0.9,
                1.0, 0.2, 0.5, 0.8
        );

        List<Stop> stops = GradientStopParser.parseStops(data, 2);

        Color c0 = stops.get(0).getColor();
        assertEquals(0.1, c0.getRed(), 1e-6);
        assertEquals(0.4, c0.getGreen(), 1e-6);
        assertEquals(0.9, c0.getBlue(), 1e-6);

        Color c1 = stops.get(1).getColor();
        assertEquals(0.2, c1.getRed(), 1e-6);
        assertEquals(0.5, c1.getGreen(), 1e-6);
        assertEquals(0.8, c1.getBlue(), 1e-6);
    }

    @Test
    void offsetsAreInZeroToOneRange() {
        // Make sure positions are not treated as percentages (0..100).
        Animated data = animatedFlat(
                0.0, 1.0, 0.0, 0.0,
                0.5, 0.0, 1.0, 0.0,
                1.0, 0.0, 0.0, 1.0
        );

        List<Stop> stops = GradientStopParser.parseStops(data, 3);

        assertEquals(0.0, stops.get(0).getOffset(), EPS);
        assertEquals(0.5, stops.get(1).getOffset(), EPS);
        assertEquals(1.0, stops.get(2).getOffset(), EPS);
    }

    @Test
    void nullAnimatedReturnsEmpty() {
        assertEquals(List.of(), GradientStopParser.parseStops(null, 3));
    }

    @Test
    void zeroNumColorsReturnsEmpty() {
        Animated data = animatedFlat(0.0, 1.0, 1.0, 1.0);
        assertEquals(List.of(), GradientStopParser.parseStops(data, 0));
    }

    @Test
    void tooFewValuesForRequestedColorsReturnsEmpty() {
        // 4 values cannot describe 2 colour stops (would need 8)
        Animated data = animatedFlat(0.0, 1.0, 0.0, 0.0);
        assertEquals(List.of(), GradientStopParser.parseStops(data, 2));
    }

    @Test
    void alphaSectionWithOddTailIsTolerated() {
        // 8 colour values + 3 alpha values: the trailing unpaired value is ignored.
        Animated data = animatedFlat(
                0.0, 1.0, 0.0, 0.0,
                1.0, 0.0, 0.0, 1.0,
                0.0, 1.0,
                0.5   // unpaired tail — must not crash
        );

        List<Stop> stops = GradientStopParser.parseStops(data, 2);
        // Should still produce stops based on the valid alpha pair.
        assertEquals(2, stops.size());
        assertEquals(1.0, stops.get(0).getColor().getOpacity(), EPS);
    }

    // ── Linear-RGB densification ───────────────────────────────────────────

    @Test
    void densifyReturnsCopyForFewerThanTwoStops() {
        // Empty input → empty result (never null).
        assertEquals(0, GradientStopParser.densifyForLinearRgb(List.of(), 4).size());

        // Single-stop input is returned unchanged (defensive copy).
        Stop only = new Stop(0.0, Color.color(0.5, 0.5, 0.5, 1.0));
        List<Stop> singleResult = GradientStopParser.densifyForLinearRgb(List.of(only), 4);
        assertEquals(1, singleResult.size());
        assertEquals(0.0, singleResult.get(0).getOffset(), EPS);
    }

    @Test
    void densifyIsNoOpWhenSubdivisionsZero() {
        List<Stop> src = List.of(
                new Stop(0.0, Color.color(1.0, 0.0, 0.0, 1.0)),
                new Stop(1.0, Color.color(0.0, 0.0, 1.0, 1.0))
        );
        List<Stop> out = GradientStopParser.densifyForLinearRgb(src, 0);
        assertEquals(2, out.size());
        assertEquals(1.0, out.get(0).getColor().getRed(), 1e-9);
        assertEquals(1.0, out.get(1).getColor().getBlue(), 1e-9);
    }

    @Test
    void densifyPreservesDesignerEndpoints() {
        // Endpoints must be pixel-exact even after inserting linear-RGB sub-stops.
        List<Stop> src = List.of(
                new Stop(0.0, Color.color(1.0, 0.0, 0.0, 1.0)),
                new Stop(1.0, Color.color(0.0, 0.0, 1.0, 1.0))
        );
        List<Stop> out = GradientStopParser.densifyForLinearRgb(src, 4);
        Color first = out.get(0).getColor();
        Color last = out.get(out.size() - 1).getColor();
        assertEquals(1.0, first.getRed(), 1e-9);
        assertEquals(0.0, first.getGreen(), 1e-9);
        assertEquals(0.0, first.getBlue(), 1e-9);
        assertEquals(0.0, last.getRed(), 1e-9);
        assertEquals(0.0, last.getGreen(), 1e-9);
        assertEquals(1.0, last.getBlue(), 1e-9);
    }

    @Test
    void densifyIsIdempotentForSolidColourGradient() {
        // Same colour on both sides — every sub-stop must reproduce that colour.
        Color grey = Color.color(0.5, 0.5, 0.5, 1.0);
        List<Stop> src = List.of(new Stop(0.0, grey), new Stop(1.0, grey));
        List<Stop> out = GradientStopParser.densifyForLinearRgb(src, 4);
        for (Stop s : out) {
            Color c = s.getColor();
            assertEquals(0.5, c.getRed(), 1e-6);
            assertEquals(0.5, c.getGreen(), 1e-6);
            assertEquals(0.5, c.getBlue(), 1e-6);
        }
    }

    @Test
    void densifyMidpointOfRedWhiteIsBrighterThanSrgbMidpoint() {
        // For red→white, the sRGB midpoint (t=0.5) is (1.0, 0.5, 0.5). The linear-RGB
        // midpoint converted back to sRGB should be brighter (both G and B > 0.5).
        List<Stop> src = List.of(
                new Stop(0.0, Color.color(1.0, 0.0, 0.0, 1.0)),
                new Stop(1.0, Color.color(1.0, 1.0, 1.0, 1.0))
        );
        List<Stop> out = GradientStopParser.densifyForLinearRgb(src, 1);
        // With 1 subdivision the middle stop lies at offset 0.5.
        Stop mid = out.get(1);
        assertEquals(0.5, mid.getOffset(), 1e-6);
        Color c = mid.getColor();
        // Linear midpoint: linear(0.5)*(1-t=0.5) is 0.5 in linear; convert 0.5 linear → sRGB ≈ 0.7354
        assertTrue(c.getGreen() > 0.7, "linear-RGB midpoint should exceed sRGB midpoint");
        assertTrue(c.getBlue() > 0.7, "linear-RGB midpoint should exceed sRGB midpoint");
        assertEquals(1.0, c.getRed(), 1e-9);
    }

    @Test
    void densifyProducesExpectedStopCount() {
        // Two designer stops + 4 sub-stops between them = 6 total.
        List<Stop> src = List.of(
                new Stop(0.0, Color.color(0.0, 0.0, 0.0, 1.0)),
                new Stop(1.0, Color.color(1.0, 1.0, 1.0, 1.0))
        );
        List<Stop> out = GradientStopParser.densifyForLinearRgb(src, 4);
        assertEquals(6, out.size());
        for (int i = 1; i < out.size(); i++) {
            assertTrue(out.get(i).getOffset() > out.get(i - 1).getOffset(),
                    "densified stops must be strictly sorted by offset");
        }
    }

    @Test
    void densifyAlphaIsLinearlyInterpolated() {
        // Alpha 1.0 → 0.0 across the gradient. Midpoint alpha should be linear (0.5),
        // not sRGB-adjusted.
        List<Stop> src = List.of(
                new Stop(0.0, Color.color(1.0, 1.0, 1.0, 1.0)),
                new Stop(1.0, Color.color(1.0, 1.0, 1.0, 0.0))
        );
        List<Stop> out = GradientStopParser.densifyForLinearRgb(src, 1);
        Stop mid = out.get(1);
        assertEquals(0.5, mid.getColor().getOpacity(), 1e-9);
    }

    @Test
    void parseStopsForLinearRgbDensifiesTwoStops() {
        // Same input as twoColorStopsNoAlphaTail, but through the linear-RGB path.
        Animated data = animatedFlat(
                0.0, 1.0, 0.0, 0.0,
                1.0, 0.0, 0.0, 1.0
        );
        List<Stop> stops = GradientStopParser.parseStopsForLinearRgb(data, 2);
        // 2 designer stops + 4 sub-stops = 6.
        assertEquals(2 + GradientStopParser.LINEAR_RGB_SUBDIVISIONS, stops.size());
        // Endpoints match designer exactly.
        assertEquals(1.0, stops.get(0).getColor().getRed(), 1e-9);
        assertEquals(1.0, stops.get(stops.size() - 1).getColor().getBlue(), 1e-9);
    }

    private static void assertStop(Stop stop, double offset,
                                   double r, double g, double b, double alpha) {
        assertEquals(offset, stop.getOffset(), EPS);
        Color c = stop.getColor();
        assertEquals(r, c.getRed(), 1e-6);
        assertEquals(g, c.getGreen(), 1e-6);
        assertEquals(b, c.getBlue(), 1e-6);
        assertEquals(alpha, c.getOpacity(), 1e-6);
    }
}
