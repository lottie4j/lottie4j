package com.lottie4j.fxplayer.element;

import com.lottie4j.core.model.animation.Animated;
import com.lottie4j.fxplayer.util.LottieValueHelper;
import javafx.scene.paint.Color;
import javafx.scene.paint.Stop;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

/**
 * Parses Lottie gradient stop data into JavaFX {@link Stop} instances.
 * <p>
 * The Lottie gradient data is encoded as a flat array of doubles inside
 * {@code g.k.k}. The first {@code 4 * numColors} values describe colour stops as
 * {@code [offset, r, g, b]} tuples. Any additional values describe alpha stops as
 * {@code [offset, alpha]} pairs. Colour stops and alpha stops can have
 * <em>different</em> offsets, in which case the rendered gradient is the per-offset
 * combination of both. This mirrors the behaviour of lottie-web's
 * {@code GradientProperty.js} / {@code GradientFillStyle}.
 */
public final class GradientStopParser {

    private GradientStopParser() {
        // utility class
    }

    /**
     * Builds JavaFX gradient stops from a Lottie gradient definition.
     *
     * @param colorAnimated flat data array as found in {@code g.k}
     * @param numColors     number of colour stops (value of {@code g.p})
     * @return list of stops ready to feed into a JavaFX gradient,
     * empty when the input cannot be interpreted
     */
    public static List<Stop> parseStops(Animated colorAnimated, int numColors) {
        if (colorAnimated == null || numColors <= 0) {
            return List.of();
        }
        int totalElements = colorAnimated.keyframes() != null
                ? colorAnimated.keyframes().size()
                : 0;
        if (totalElements < numColors * 4) {
            return List.of();
        }

        List<ColorStop> colorStops = readColorStops(colorAnimated, numColors);
        List<AlphaStop> alphaStops = readAlphaStops(colorAnimated, numColors, totalElements);

        return mergeToStops(colorStops, alphaStops);
    }

    static List<ColorStop> readColorStops(Animated colorAnimated, int numColors) {
        List<ColorStop> stops = new ArrayList<>(numColors);
        for (int i = 0; i < numColors; i++) {
            int baseIdx = i * 4;
            double offset = clampOffset(colorAnimated.getValue(baseIdx));
            double r = LottieValueHelper.clamp(colorAnimated.getValue(baseIdx + 1));
            double g = LottieValueHelper.clamp(colorAnimated.getValue(baseIdx + 2));
            double b = LottieValueHelper.clamp(colorAnimated.getValue(baseIdx + 3));
            stops.add(new ColorStop(offset, r, g, b));
        }
        return stops;
    }

    static List<AlphaStop> readAlphaStops(Animated colorAnimated, int numColors, int totalElements) {
        int colorSectionSize = numColors * 4;
        List<AlphaStop> stops = new ArrayList<>();
        for (int i = colorSectionSize; i + 1 < totalElements; i += 2) {
            double offset = clampOffset(colorAnimated.getValue(i));
            double alpha = LottieValueHelper.clamp(colorAnimated.getValue(i + 1));
            stops.add(new AlphaStop(offset, alpha));
        }
        return stops;
    }

    static List<Stop> mergeToStops(List<ColorStop> colorStops, List<AlphaStop> alphaStops) {
        if (colorStops.isEmpty()) {
            return List.of();
        }
        if (alphaStops.isEmpty()) {
            List<Stop> result = new ArrayList<>(colorStops.size());
            for (ColorStop cs : colorStops) {
                result.add(new Stop(cs.offset(), Color.color(cs.r(), cs.g(), cs.b(), 1.0)));
            }
            return result;
        }

        TreeSet<Double> offsets = new TreeSet<>();
        for (ColorStop cs : colorStops) {
            offsets.add(cs.offset());
        }
        for (AlphaStop as : alphaStops) {
            offsets.add(as.offset());
        }

        List<Stop> result = new ArrayList<>(offsets.size());
        for (double offset : offsets) {
            double[] rgb = interpolateColor(colorStops, offset);
            double alpha = interpolateAlpha(alphaStops, offset);
            result.add(new Stop(offset, Color.color(rgb[0], rgb[1], rgb[2], alpha)));
        }
        return result;
    }

    static double[] interpolateColor(List<ColorStop> stops, double offset) {
        ColorStop first = stops.get(0);
        if (offset <= first.offset()) {
            return new double[]{first.r(), first.g(), first.b()};
        }
        ColorStop last = stops.get(stops.size() - 1);
        if (offset >= last.offset()) {
            return new double[]{last.r(), last.g(), last.b()};
        }
        for (int i = 1; i < stops.size(); i++) {
            ColorStop prev = stops.get(i - 1);
            ColorStop next = stops.get(i);
            if (offset >= prev.offset() && offset <= next.offset()) {
                double span = next.offset() - prev.offset();
                double t = span <= 0 ? 0 : (offset - prev.offset()) / span;
                return new double[]{
                        prev.r() + t * (next.r() - prev.r()),
                        prev.g() + t * (next.g() - prev.g()),
                        prev.b() + t * (next.b() - prev.b())
                };
            }
        }
        return new double[]{last.r(), last.g(), last.b()};
    }

    static double interpolateAlpha(List<AlphaStop> stops, double offset) {
        AlphaStop first = stops.get(0);
        if (offset <= first.offset()) {
            return first.alpha();
        }
        AlphaStop last = stops.get(stops.size() - 1);
        if (offset >= last.offset()) {
            return last.alpha();
        }
        for (int i = 1; i < stops.size(); i++) {
            AlphaStop prev = stops.get(i - 1);
            AlphaStop next = stops.get(i);
            if (offset >= prev.offset() && offset <= next.offset()) {
                double span = next.offset() - prev.offset();
                double t = span <= 0 ? 0 : (offset - prev.offset()) / span;
                return prev.alpha() + t * (next.alpha() - prev.alpha());
            }
        }
        return last.alpha();
    }

    private static double clampOffset(double v) {
        if (Double.isNaN(v)) {
            return 0.0;
        }
        return Math.max(0.0, Math.min(1.0, v));
    }

    /**
     * Colour stop: offset in {@code [0..1]} and normalised RGB components.
     *
     * @param offset position in {@code [0..1]}
     * @param r      red component in {@code [0..1]}
     * @param g      green component in {@code [0..1]}
     * @param b      blue component in {@code [0..1]}
     */
    public record ColorStop(double offset, double r, double g, double b) {
    }

    /**
     * Alpha stop: offset in {@code [0..1]} and alpha component in {@code [0..1]}.
     *
     * @param offset position in {@code [0..1]}
     * @param alpha  alpha component in {@code [0..1]}
     */
    public record AlphaStop(double offset, double alpha) {
    }
}
