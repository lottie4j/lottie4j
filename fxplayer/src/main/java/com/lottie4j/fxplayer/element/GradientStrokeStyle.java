package com.lottie4j.fxplayer.element;

import java.util.List;

import com.lottie4j.core.definition.AnimatedValueType;
import com.lottie4j.core.definition.GradientType;
import com.lottie4j.core.model.shape.style.GradientStroke;

import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Paint;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;

/**
 * Wrapper for Lottie gradient stroke definitions, converting them to JavaFX gradient paints.
 * Supports both linear and radial gradients with alpha channel interpolation.
 */
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
     * Builds the JavaFX paint for the stroke gradient at a specific frame with shape bounds.
     *
     * @param frame       animation frame to sample
     * @param shapeX      shape bounding box x position
     * @param shapeY      shape bounding box y position
     * @param shapeWidth  shape bounding box width
     * @param shapeHeight shape bounding box height
     * @return linear/radial gradient paint using proportional coordinates
     */
    public Paint getPaint(double frame, double shapeX, double shapeY, double shapeWidth, double shapeHeight) {
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

        List<Stop> stops = buildGradientStops(frame);

        if (stops.isEmpty()) {
            return Color.BLACK;
        }

        // Transform to proportional coordinates
        boolean hasShapeBounds = (shapeWidth > 0 && shapeHeight > 0);

        if (gradientStroke.gradientType() == GradientType.RADIAL) {
            if (hasShapeBounds) {
                double centerX = (startX - shapeX) / shapeWidth;
                double centerY = (startY - shapeY) / shapeHeight;
                double radiusX = (endX - startX) / shapeWidth;
                double radiusY = (endY - startY) / shapeHeight;
                double radius = Math.sqrt(radiusX * radiusX + radiusY * radiusY);
                return new RadialGradient(
                        0, 0,
                        centerX, centerY,
                        radius,
                        true, // proportional
                        CycleMethod.NO_CYCLE,
                        stops
                );
            } else {
                double centerX = startX;
                double centerY = startY;
                double radius = Math.sqrt(Math.pow(endX - startX, 2) + Math.pow(endY - startY, 2));
                return new RadialGradient(
                        0, 0,
                        centerX, centerY,
                        radius,
                        false, // absolute
                        CycleMethod.NO_CYCLE,
                        stops
                );
            }
        } else {
            // Linear gradient
            if (hasShapeBounds) {
                double propStartX = (startX - shapeX) / shapeWidth;
                double propStartY = (startY - shapeY) / shapeHeight;
                double propEndX = (endX - shapeX) / shapeWidth;
                double propEndY = (endY - shapeY) / shapeHeight;

                return new LinearGradient(
                        propStartX, propStartY, propEndX, propEndY,
                        true, // proportional
                        CycleMethod.NO_CYCLE,
                        stops
                );
            } else {
                return new LinearGradient(
                        startX, startY, endX, endY,
                        false, // absolute
                        CycleMethod.NO_CYCLE,
                        stops
                );
            }
        }
    }

    /**
     * Builds the JavaFX paint for the stroke gradient at a specific frame.
     *
     * @param frame animation frame to sample
     * @return linear/radial gradient paint, or black when gradient data is unavailable
     */
    public Paint getPaint(double frame) {
        return getPaint(frame, 0, 0, 0, 0);
    }

    /**
     * Builds the list of gradient stops from the stroke color data using the
     * shared {@link GradientStopParser}. Colour and alpha tracks are merged at
     * every unique offset so alpha transitions between colour stops are preserved.
     *
     * <p>The parser also densifies each adjacent stop pair with sub-stops sampled in
     * linear-RGB space so JavaFX's sRGB per-pair interpolation approximates thorvg's
     * linear-RGB interpolation across the gradient.
     *
     * @param frame animation frame to sample
     * @return list of gradient stops
     */
    private List<Stop> buildGradientStops(double frame) {
        if (gradientStroke.colors().colors() == null) {
            return List.of();
        }
        Integer numColorsBoxed = gradientStroke.colors().numberOfColors();
        int numColors = numColorsBoxed != null ? numColorsBoxed : 0;
        return GradientStopParser.parseStopsForLinearRgb(gradientStroke.colors().colors(), numColors);
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
