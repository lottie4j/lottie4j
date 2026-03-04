package com.lottie4j.fxplayer.renderer.shape;

import com.lottie4j.core.definition.LineCap;
import com.lottie4j.core.model.AnimatedValueType;
import com.lottie4j.core.model.shape.BaseShape;
import com.lottie4j.core.model.shape.grouping.Group;
import com.lottie4j.core.model.shape.modifier.TrimPath;
import com.lottie4j.core.model.shape.shape.Ellipse;
import com.lottie4j.core.model.shape.style.Fill;
import com.lottie4j.core.model.shape.style.GradientFill;
import com.lottie4j.core.model.shape.style.Stroke;
import com.lottie4j.fxplayer.element.FillStyle;
import com.lottie4j.fxplayer.element.GradientFillStyle;
import com.lottie4j.fxplayer.element.StrokeStyle;
import com.lottie4j.fxplayer.util.StrokeHelper;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Paint;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.StrokeLineCap;

import java.util.Optional;
import java.util.logging.Logger;

public class EllipseRenderer implements ShapeRenderer {

    private static final Logger logger = Logger.getLogger(EllipseRenderer.class.getName());

    /**
     * {@inheritDoc}
     * <p>
     * Renders a Lottie ellipse shape with optional fill, stroke, gradient, and trim path support.
     */
    @Override
    public void render(GraphicsContext gc, BaseShape shape, Group parentGroup, double frame) {
        if (!(shape instanceof Ellipse ellipse)) {
            logger.warning("EllipseRenderer called with non-Ellipse shape: " + shape.getClass().getSimpleName());
            return;
        }

        if (ellipse.size() == null) {
            logger.warning("Ellipse missing size data");
            return;
        }

        // Get size from animated property
        double width = ellipse.size().getValue(AnimatedValueType.WIDTH, frame);
        double height = ellipse.size().getValue(AnimatedValueType.HEIGHT, frame);

        // Get position (center point) from animated property, default to 0,0 if null
        double centerX = 0;
        double centerY = 0;
        if (ellipse.position() != null) {
            centerX = ellipse.position().getValue(AnimatedValueType.X, frame);
            centerY = ellipse.position().getValue(AnimatedValueType.Y, frame);
        }

        // Convert from center-based to top-left for JavaFX oval rendering
        double renderX = centerX - (width / 2.0);
        double renderY = centerY - (height / 2.0);

        // Check for trim path
        var trimPath = getTrimPath(parentGroup);

        // Check for stroke style to determine line cap
        var strokeStyle = getStrokeStyle(parentGroup);
        if (strokeStyle.isPresent()) {
            Stroke stroke = strokeStyle.get().stroke();
            // Set line cap: 1=butt, 2=round, 3=square
            if (stroke.lineCap() != null) {
                switch (stroke.lineCap()) {
                    case LineCap.ROUND -> gc.setLineCap(StrokeLineCap.ROUND);
                    case LineCap.SQUARE -> gc.setLineCap(StrokeLineCap.SQUARE);
                    default -> gc.setLineCap(StrokeLineCap.BUTT);
                }
            }
        }

        if (trimPath.isPresent()) {
            // Render with trim path (arc)
            renderWithTrimPath(gc, renderX, renderY, width, height, centerX, centerY,
                    trimPath.get(), frame, parentGroup);
        } else {
            // Render full ellipse
            // Check for gradient fill first, then regular fill
            var gradientFillStyle = getGradientFillStyle(parentGroup);
            if (gradientFillStyle.isPresent()) {
                Paint gradientPaint = gradientFillStyle.get().getPaint(frame);
                gc.setFill(gradientPaint);
                double opacity = gradientFillStyle.get().getOpacity(frame);
                if (opacity < 1.0) {
                    double currentAlpha = gc.getGlobalAlpha();
                    gc.setGlobalAlpha(currentAlpha * opacity);
                }
                gc.fillOval(renderX, renderY, width, height);
                gc.setGlobalAlpha(1.0); // Reset
            } else {
                var fillStyle = getFillStyle(parentGroup);
                if (fillStyle.isPresent()) {
                    var fillColor = fillStyle.get().getColor(frame);
                    logger.info("Ellipse fill found - color: " + fillColor + ", position: (" + renderX + "," + renderY + "), size: " + width + "x" + height);
                    gc.setFill(fillColor);
                    gc.fillOval(renderX, renderY, width, height);
                } else {
                    logger.info("Ellipse NO fill found for parentGroup");
                }
            }

            if (strokeStyle.isPresent()) {
                var strokeWidth = strokeStyle.get().getStrokeWidth(frame);

                if (StrokeHelper.shouldRenderStroke(strokeWidth)) {
                    double compensatedWidth = StrokeHelper.getCompensatedStrokeWidth(gc, strokeWidth);

                    gc.setStroke(strokeStyle.get().getColor(frame));
                    gc.setLineWidth(compensatedWidth);
                    gc.strokeOval(renderX, renderY, width, height);
                }
            }
        }
    }

    /**
     * Extracts fill style from parent group.
     *
     * @param group parent group containing styles
     * @return fill style if present
     */
    private Optional<FillStyle> getFillStyle(Group group) {
        if (group == null) {
            return Optional.empty();
        }
        for (BaseShape baseShape : group.shapes()) {
            if (baseShape instanceof Fill fill) {
                return Optional.of(new FillStyle(fill));
            }
        }
        return Optional.empty();
    }

    /**
     * Extracts gradient fill style from parent group.
     *
     * @param group parent group containing styles
     * @return gradient fill style if present
     */
    private Optional<GradientFillStyle> getGradientFillStyle(Group group) {
        if (group == null) {
            return Optional.empty();
        }
        for (BaseShape baseShape : group.shapes()) {
            if (baseShape instanceof GradientFill gradientFill) {
                return Optional.of(new GradientFillStyle(gradientFill));
            }
        }
        return Optional.empty();
    }

    /**
     * Extracts stroke style from parent group.
     *
     * @param group parent group containing styles
     * @return stroke style if present
     */
    private Optional<StrokeStyle> getStrokeStyle(Group group) {
        if (group == null) {
            return Optional.empty();
        }
        for (BaseShape baseShape : group.shapes()) {
            if (baseShape instanceof Stroke stroke) {
                return Optional.of(new StrokeStyle(stroke));
            }
        }
        return Optional.empty();
    }

    /**
     * Extracts trim path modifier from parent group.
     *
     * @param group parent group containing modifiers
     * @return trim path if present
     */
    private Optional<TrimPath> getTrimPath(Group group) {
        if (group == null) {
            return Optional.empty();
        }
        for (BaseShape baseShape : group.shapes()) {
            if (baseShape instanceof TrimPath trimPath) {
                return Optional.of(trimPath);
            }
        }
        return Optional.empty();
    }

    /**
     * Renders an ellipse with trim path applied, converting it to an arc stroke.
     *
     * @param gc          graphics context
     * @param x           top-left x coordinate
     * @param y           top-left y coordinate
     * @param width       ellipse width
     * @param height      ellipse height
     * @param centerX     center x coordinate
     * @param centerY     center y coordinate
     * @param trimPath    trim path modifier
     * @param frame       animation frame
     * @param parentGroup parent group containing styles
     */
    private void renderWithTrimPath(GraphicsContext gc, double x, double y, double width, double height,
                                    double centerX, double centerY, TrimPath trimPath, double frame, Group parentGroup) {
        // Get trim values (all in 0-100 range in Lottie, or degrees for offset depending on version)
        double start = trimPath.segmentStart() != null ? trimPath.segmentStart().getValue(0, frame) : 0;
        double end = trimPath.segmentEnd() != null ? trimPath.segmentEnd().getValue(0, frame) : 100;
        double offset = trimPath.offset() != null ? trimPath.offset().getValue(0, frame) : 0;

        logger.fine("Trim Path - Start: " + start + ", End: " + end + ", Offset: " + offset + " at frame " + frame);

        // Convert to normalized 0-1 range
        start = start / 100.0;
        end = end / 100.0;

        // Offset in Lottie is in degrees (can be positive or negative)
        // For example: offset goes 0 → -360 → -720 for 2 full rotations
        // Convert degrees to rotations (fraction of a full circle)
        double offsetRotations = offset / 360.0;

        // Apply offset as rotations to both start and end positions
        start = start + offsetRotations;
        end = end + offsetRotations;

        // Handle wrapping for values > 1.0
        while (start > 1.0) start -= 1.0;
        while (end > 1.0) end -= 1.0;

        // Lottie measures from the top (12 o'clock) going clockwise
        // JavaFX measures from the right (3 o'clock) with positive angles going counterclockwise

        // Convert start/end from 0-1 range to degrees (0=top, going clockwise in Lottie space)
        // Then convert to JavaFX coordinate system (0=right, counterclockwise positive)
        // Lottie: 0 = top (12 o'clock) = 90° in JavaFX system
        double startAngle = 90 - (start * 360.0);
        double endAngle = 90 - (end * 360.0);

        // Calculate arc extent (how much of the circle to draw)
        // In JavaFX, negative extent = clockwise
        double arcExtent = startAngle - endAngle;

        // Normalize angles
        while (arcExtent <= 0) {
            arcExtent += 360;
        }
        while (arcExtent > 360) {
            arcExtent -= 360;
        }

        logger.fine("Rendering arc - StartAngle: " + startAngle + "°, Extent: " + arcExtent + "°");

        // Fill the entire ellipse (not affected by trim path)
        var gradientFillStyle = getGradientFillStyle(parentGroup);
        if (gradientFillStyle.isPresent()) {
            Paint gradientPaint = gradientFillStyle.get().getPaint(frame);
            gc.setFill(gradientPaint);
            double opacity = gradientFillStyle.get().getOpacity(frame);
            if (opacity < 1.0) {
                double currentAlpha = gc.getGlobalAlpha();
                gc.setGlobalAlpha(currentAlpha * opacity);
            }
            logger.info("TrimPath ellipse gradient fill - position: (" + x + "," + y + "), size: " + width + "x" + height);
            gc.fillOval(x, y, width, height);
            gc.setGlobalAlpha(1.0); // Reset
        } else {
            var fillStyle = getFillStyle(parentGroup);
            if (fillStyle.isPresent()) {
                var fillColor = fillStyle.get().getColor(frame);
                logger.info("TrimPath ellipse fill found - color: " + fillColor + ", position: (" + x + "," + y + "), size: " + width + "x" + height);
                gc.setFill(fillColor);
                gc.fillOval(x, y, width, height);
            } else {
                logger.info("TrimPath ellipse NO fill found for parentGroup");
            }
        }

        // Only stroke is affected by trim paths
        var strokeStyle = getStrokeStyle(parentGroup);
        if (strokeStyle.isPresent()) {
            var strokeWidth = strokeStyle.get().getStrokeWidth(frame);

            if (StrokeHelper.shouldRenderStroke(strokeWidth)) {
                double compensatedWidth = StrokeHelper.getCompensatedStrokeWidth(gc, strokeWidth);

                gc.setStroke(strokeStyle.get().getColor(frame));
                gc.setLineWidth(compensatedWidth);
                gc.strokeArc(x, y, width, height, startAngle, -arcExtent, ArcType.OPEN);
            }
        }
    }
}
