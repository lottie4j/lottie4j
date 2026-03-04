package com.lottie4j.fxplayer.element;

import com.lottie4j.core.definition.GradientType;
import com.lottie4j.core.model.AnimatedValueType;
import com.lottie4j.core.model.shape.style.GradientStroke;
import com.lottie4j.fxplayer.util.LottieValueHelper;
import javafx.scene.paint.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class GradientStrokeStyle {

    private final GradientStroke gradientStroke;

    /**
     * Creates a gradient stroke style wrapper for a Lottie gradient stroke definition.
     *
     * @param gradientStroke source gradient stroke definition
     */
    public GradientStrokeStyle(GradientStroke gradientStroke) {
        this.gradientStroke = gradientStroke;
    }

    /**
     * Builds the JavaFX paint for the stroke gradient at a specific frame.
     *
     * @param frame animation frame to sample
     * @return linear/radial gradient paint, or black when gradient data is unavailable
     */
    public Paint getPaint(double frame) {
        if (gradientStroke == null || gradientStroke.colors() == null) {
            return Color.BLACK;
        }

        double startX = 0;
        double startY = 0;
        double endX = 100;
        double endY = 100;
        if (gradientStroke.startingPoint() != null) {
            startX = gradientStroke.startingPoint().getValue(AnimatedValueType.X, frame);
            startY = gradientStroke.startingPoint().getValue(AnimatedValueType.Y, frame);
        }
        if (gradientStroke.endPoint() != null) {
            endX = gradientStroke.endPoint().getValue(AnimatedValueType.X, frame);
            endY = gradientStroke.endPoint().getValue(AnimatedValueType.Y, frame);
        }

        List<Stop> stops = new ArrayList<>();
        if (gradientStroke.colors().colors() != null) {
            int numColors = gradientStroke.colors().numberOfColors();
            var colorAnimated = gradientStroke.colors().colors();
            int totalElements = colorAnimated.keyframes() != null ? colorAnimated.keyframes().size() : 0;

            int colorSectionSize = numColors * 4;
            List<double[]> alphaStops = new ArrayList<>();
            if (totalElements > colorSectionSize) {
                for (int i = colorSectionSize; i + 1 < totalElements; i += 2) {
                    Double alphaOffset = colorAnimated.getValue(i);
                    Double alphaValue = colorAnimated.getValue(i + 1);
                    if (alphaOffset != null && alphaValue != null) {
                        alphaStops.add(new double[]{alphaOffset, LottieValueHelper.clamp(alphaValue)});
                    }
                }
                alphaStops.sort(Comparator.comparingDouble(stop -> stop[0]));
            }

            for (int i = 0; i < numColors; i++) {
                int baseIdx = i * 4;
                double offset = colorAnimated.getValue(baseIdx);
                double r = LottieValueHelper.clamp(colorAnimated.getValue(baseIdx + 1));
                double g = LottieValueHelper.clamp(colorAnimated.getValue(baseIdx + 2));
                double b = LottieValueHelper.clamp(colorAnimated.getValue(baseIdx + 3));
                double alpha = interpolateAlpha(alphaStops, offset);
                stops.add(new Stop(offset, Color.color(r, g, b, alpha)));
            }
        }

        if (stops.isEmpty()) {
            return Color.BLACK;
        }

        if (gradientStroke.gradientType() == GradientType.RADIAL) {
            double centerX = startX;
            double centerY = startY;
            double radius = Math.sqrt(Math.pow(endX - startX, 2) + Math.pow(endY - startY, 2));
            return new RadialGradient(
                    0,
                    0,
                    centerX,
                    centerY,
                    radius,
                    false,
                    CycleMethod.NO_CYCLE,
                    stops
            );
        }

        return new LinearGradient(
                startX,
                startY,
                endX,
                endY,
                false,
                CycleMethod.NO_CYCLE,
                stops
        );
    }

    /**
     * Interpolates alpha for a color stop offset using the parsed alpha stop list.
     *
     * @param alphaStops ordered alpha stop pairs in the form {@code [offset, alpha]}
     * @param offset     stop offset to resolve
     * @return interpolated alpha value in the range {@code [0, 1]}
     */
    private double interpolateAlpha(List<double[]> alphaStops, double offset) {
        if (alphaStops.isEmpty()) {
            return 1.0;
        }
        if (offset <= alphaStops.get(0)[0]) {
            return alphaStops.get(0)[1];
        }
        int last = alphaStops.size() - 1;
        if (offset >= alphaStops.get(last)[0]) {
            return alphaStops.get(last)[1];
        }

        for (int i = 0; i < last; i++) {
            double[] left = alphaStops.get(i);
            double[] right = alphaStops.get(i + 1);
            if (offset >= left[0] && offset <= right[0]) {
                double span = right[0] - left[0];
                if (span <= 1e-9) {
                    return right[1];
                }
                double t = (offset - left[0]) / span;
                return left[1] + ((right[1] - left[1]) * t);
            }
        }

        return 1.0;
    }

    /**
     * Resolves animated stroke opacity at the given frame.
     *
     * @param frame animation frame to sample
     * @return normalized opacity in the range {@code [0, 1]}
     */
    public double getOpacity(double frame) {
        if (gradientStroke.opacity() != null) {
            return gradientStroke.opacity().getValue(0, frame) / 100.0;
        }
        return 1.0;
    }

    /**
     * Resolves animated stroke width at the given frame.
     *
     * @param frame animation frame to sample
     * @return stroke width in layer units, or {@code 0} when not defined
     */
    public double getStrokeWidth(double frame) {
        if (gradientStroke.strokeWidth() != null) {
            return gradientStroke.strokeWidth().getValue(0, frame);
        }
        return 0.0;
    }
}
