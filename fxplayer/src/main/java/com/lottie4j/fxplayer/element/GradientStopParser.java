package com.lottie4j.fxplayer.element;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import com.lottie4j.core.model.animation.Animated;
import com.lottie4j.fxplayer.util.LottieValueHelper;

import javafx.scene.paint.Color;
import javafx.scene.paint.Stop;

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
 *
 * <h2>Linear-RGB approximation</h2>
 * <p>JavaFX's {@link javafx.scene.paint.LinearGradient} and {@link javafx.scene.paint.RadialGradient}
 * always interpolate between adjacent stops in sRGB (gamma-encoded) space, whereas
 * thorvg — the engine used by {@code @lottiefiles/dotlottie-wc} and lottie-web's
 * canvas renderer — interpolates in linear-RGB space. sRGB midpoints between two
 * distinct colours are perceptibly darker (and often more saturated) than the
 * equivalent linear-RGB midpoints, producing the "harder" transitions we observe
 * on files such as {@code interactive_mood_selector_ui.json}.
 *
 * <p>{@link #parseStopsForLinearRgb(Animated, int)} approximates linear-RGB
 * interpolation by inserting extra stops between every pair of designer stops,
 * with each sub-stop's RGB values obtained by linearly interpolating in
 * linear-RGB space and converting back to sRGB. JavaFX's per-pair sRGB
 * interpolation between two nearby stops closely tracks the true linear-RGB
 * segment for the far pair.
 */
public final class GradientStopParser {

    /**
     * Number of sub-stops inserted between each pair of designer stops when densifying
     * a gradient for the linear-RGB approximation. A value of 4 keeps the JavaFX
     * gradient below the practical stop-count limit for the largest gradients seen in
     * the fixture suite (mood selector emojis have 9 designer stops → 9 + 8×4 = 41
     * total) while pushing the interpolation error below the SSIM window's sensitivity.
     */
    public static final int LINEAR_RGB_SUBDIVISIONS = 4;

    private GradientStopParser() {
        // utility class
    }

    /**
     * Builds JavaFX gradient stops from a Lottie gradient definition using JavaFX's
     * default sRGB interpolation between adjacent stops.
     *
     * <p>Prefer {@link #parseStopsForLinearRgb(Animated, int)} for renderer paths so
     * the output matches thorvg's linear-RGB gradient interpolation.
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

    /**
     * Builds JavaFX gradient stops that approximate thorvg's linear-RGB gradient
     * interpolation. Between every pair of designer stops (after colour + alpha
     * merging) a fixed number of sub-stops is inserted whose RGB values are the
     * linear-RGB midpoints converted back into sRGB. Alpha remains linearly
     * interpolated between the merged endpoints.
     *
     * <p>Alpha stops are merged with colour stops exactly as in {@link #parseStops};
     * see that method's javadoc for the merge semantics.
     *
     * @param colorAnimated flat data array as found in {@code g.k}
     * @param numColors     number of colour stops (value of {@code g.p})
     * @return densified list of stops, empty when the input cannot be interpreted
     */
    public static List<Stop> parseStopsForLinearRgb(Animated colorAnimated, int numColors) {
        return densifyForLinearRgb(parseStops(colorAnimated, numColors), LINEAR_RGB_SUBDIVISIONS);
    }

    /**
     * Inserts {@code subdivisions} sub-stops between each pair of adjacent stops.
     * Sub-stop RGB values are computed by converting each endpoint into linear-RGB,
     * linearly interpolating, then converting back to sRGB. Alpha is interpolated
     * directly in linear space. The original stops are preserved exactly, so
     * gradient endpoints and existing designer stops are unaffected.
     *
     * <p>When {@code subdivisions <= 0} or the input has fewer than two stops the
     * returned list is a copy of the input.
     *
     * @param stops        source stop list (typically the output of {@link #parseStops})
     * @param subdivisions number of sub-stops to insert between each adjacent pair
     * @return densified stop list
     */
    public static List<Stop> densifyForLinearRgb(List<Stop> stops, int subdivisions) {
        if (stops == null || stops.size() < 2 || subdivisions <= 0) {
            return stops == null ? List.of() : new ArrayList<>(stops);
        }

        List<Stop> out = new ArrayList<>(stops.size() + (stops.size() - 1) * subdivisions);
        for (int i = 0; i < stops.size() - 1; i++) {
            Stop a = stops.get(i);
            Stop b = stops.get(i + 1);
            out.add(a);

            Color ca = a.getColor();
            Color cb = b.getColor();
            double aR = srgbToLinear(ca.getRed());
            double aG = srgbToLinear(ca.getGreen());
            double aB = srgbToLinear(ca.getBlue());
            double bR = srgbToLinear(cb.getRed());
            double bG = srgbToLinear(cb.getGreen());
            double bB = srgbToLinear(cb.getBlue());
            double aA = ca.getOpacity();
            double bA = cb.getOpacity();

            double aOff = a.getOffset();
            double bOff = b.getOffset();
            double span = bOff - aOff;
            if (span <= 1e-9) {
                continue;
            }

            for (int k = 1; k <= subdivisions; k++) {
                double t = k / (double) (subdivisions + 1);
                double offset = aOff + t * span;
                double r = linearToSrgb(aR + t * (bR - aR));
                double g = linearToSrgb(aG + t * (bG - aG));
                double bl = linearToSrgb(aB + t * (bB - aB));
                double alpha = aA + t * (bA - aA);
                out.add(new Stop(offset, Color.color(clamp01(r), clamp01(g), clamp01(bl), clamp01(alpha))));
            }
        }
        out.add(stops.get(stops.size() - 1));
        return out;
    }

    /**
     * Exact sRGB → linear transfer function (IEC 61966-2-1).
     *
     * @param s value in gamma-encoded sRGB, in {@code [0, 1]}
     * @return linear-light value in {@code [0, 1]}
     */
    static double srgbToLinear(double s) {
        double v = clamp01(s);
        return v <= 0.04045 ? v / 12.92 : Math.pow((v + 0.055) / 1.055, 2.4);
    }

    /**
     * Exact linear → sRGB transfer function (IEC 61966-2-1).
     *
     * @param l linear-light value in {@code [0, 1]}
     * @return gamma-encoded sRGB value in {@code [0, 1]}
     */
    static double linearToSrgb(double l) {
        double v = clamp01(l);
        return v <= 0.0031308 ? v * 12.92 : 1.055 * Math.pow(v, 1.0 / 2.4) - 0.055;
    }

    private static double clamp01(double v) {
        if (Double.isNaN(v)) return 0.0;
        if (v < 0.0) return 0.0;
        if (v > 1.0) return 1.0;
        return v;
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
