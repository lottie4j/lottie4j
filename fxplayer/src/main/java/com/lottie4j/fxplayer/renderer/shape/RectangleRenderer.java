package com.lottie4j.fxplayer.renderer.shape;

import com.lottie4j.core.model.shape.BaseShape;
import com.lottie4j.core.model.shape.grouping.Group;
import com.lottie4j.core.model.shape.shape.Rectangle;
import com.lottie4j.core.model.shape.style.Fill;
import com.lottie4j.core.model.shape.style.GradientFill;
import com.lottie4j.core.model.shape.style.GradientStroke;
import com.lottie4j.core.model.shape.style.Stroke;
import com.lottie4j.fxplayer.element.FillStyle;
import com.lottie4j.fxplayer.element.GradientFillStyle;
import com.lottie4j.fxplayer.element.GradientStrokeStyle;
import com.lottie4j.fxplayer.element.StrokeStyle;
import com.lottie4j.fxplayer.util.StrokeHelper;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Paint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * Renders Lottie rectangle shapes with support for rounded corners, fill, stroke, and gradient styles.
 * Handles center-based positioning from Lottie format and converts to JavaFX top-left coordinate system.
 */
public class RectangleRenderer implements ShapeRenderer {

    private static final Logger logger = LoggerFactory.getLogger(RectangleRenderer.class);

    /**
     * {@inheritDoc}
     * <p>
     * Renders a Lottie rectangle shape with support for rounded corners, gradients, and both fill and stroke styles.
     */
    @Override
    public void render(GraphicsContext gc, BaseShape shape, Group parentGroup, double frame) {
        if (!(shape instanceof Rectangle rectangle)) {
            logger.warn("RectangleRenderer called with non-Rectangle shape: {}", shape.getClass().getSimpleName());
            return;
        }

        logger.debug("RectangleRenderer.render called for: {}", rectangle.name());

        if (rectangle.size() == null) {
            logger.warn("Rectangle missing size data");
            return;
        }

        // Get size from animated property
        double width = rectangle.size().getValue(com.lottie4j.core.model.AnimatedValueType.WIDTH, frame);
        double height = rectangle.size().getValue(com.lottie4j.core.model.AnimatedValueType.HEIGHT, frame);

        // Get position (center point) from animated property, default to 0,0 if null
        double centerX = 0;
        double centerY = 0;
        if (rectangle.position() != null) {
            centerX = rectangle.position().getValue(com.lottie4j.core.model.AnimatedValueType.X, frame);
            centerY = rectangle.position().getValue(com.lottie4j.core.model.AnimatedValueType.Y, frame);
        }

        // Convert from center-based to top-left for JavaFX rectangle rendering
        double renderX = centerX - (width / 2.0);
        double renderY = centerY - (height / 2.0);

        // Lottie rectangle radius is in local shape units and should not exceed half-dimension.
        double radius = 0;
        if (rectangle.roundedCornerRadius() != null) {
            radius = Math.max(0, rectangle.roundedCornerRadius().getValue(0, frame));
            radius = Math.min(radius, Math.min(width, height) / 2.0);
        }
        double arc = radius * 2.0;

        // Check for gradient fill first, then regular fill
        var gradientFillStyle = getGradientFillStyle(parentGroup);
        if (gradientFillStyle.isPresent()) {
            Paint gradientPaint = gradientFillStyle.get().getPaint(frame, renderX, renderY, width, height);
            gc.save();
            gc.setFill(gradientPaint);
            double opacity = gradientFillStyle.get().getOpacity(frame);
            if (opacity < 1.0) {
                gc.setGlobalAlpha(gc.getGlobalAlpha() * opacity);
            }
            if (radius > 0) {
                gc.fillRoundRect(renderX, renderY, width, height, arc, arc);
            } else {
                gc.fillRect(renderX, renderY, width, height);
            }
            gc.restore();
        } else {
            var fillStyle = getFillStyle(parentGroup);
            if (fillStyle.isPresent()) {
                var fillColor = fillStyle.get().getColor(frame);
                gc.setFill(fillColor);
                if (radius > 0) {
                    gc.fillRoundRect(renderX, renderY, width, height, arc, arc);
                } else {
                    gc.fillRect(renderX, renderY, width, height);
                }
            }
        }

        var gradientStrokeStyle = getGradientStrokeStyle(parentGroup);
        if (gradientStrokeStyle.isPresent()) {
            double strokeWidth = gradientStrokeStyle.get().getStrokeWidth(frame);
            if (StrokeHelper.shouldRenderStroke(strokeWidth)) {
                double compensatedWidth = StrokeHelper.getCompensatedStrokeWidth(gc, strokeWidth);
                gc.save();
                gc.setStroke(gradientStrokeStyle.get().getPaint(frame));
                gc.setLineWidth(compensatedWidth);
                double opacity = gradientStrokeStyle.get().getOpacity(frame);
                if (opacity < 1.0) {
                    gc.setGlobalAlpha(gc.getGlobalAlpha() * opacity);
                }
                if (radius > 0) {
                    gc.strokeRoundRect(renderX, renderY, width, height, arc, arc);
                } else {
                    gc.strokeRect(renderX, renderY, width, height);
                }
                gc.restore();
            }
            return;
        }

        var strokeStyle = getStrokeStyle(parentGroup);
        if (strokeStyle.isPresent()) {
            var strokeWidth = strokeStyle.get().getStrokeWidth(frame);

            if (StrokeHelper.shouldRenderStroke(strokeWidth)) {
                double compensatedWidth = StrokeHelper.getCompensatedStrokeWidth(gc, strokeWidth);

                logger.debug("Drawing rectangle stroke with color {} and width: {} (compensated: {})",
                        strokeStyle.get().getColor(frame), strokeWidth, compensatedWidth);
                gc.setStroke(strokeStyle.get().getColor(frame));
                gc.setLineWidth(compensatedWidth);
                if (radius > 0) {
                    gc.strokeRoundRect(renderX, renderY, width, height, arc, arc);
                } else {
                    gc.strokeRect(renderX, renderY, width, height);
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
     * Extracts gradient stroke style from parent group.
     *
     * @param group parent group containing styles
     * @return gradient stroke style if present
     */
    private Optional<GradientStrokeStyle> getGradientStrokeStyle(Group group) {
        if (group == null) {
            return Optional.empty();
        }
        for (BaseShape baseShape : group.shapes()) {
            if (baseShape instanceof GradientStroke gradientStroke) {
                return Optional.of(new GradientStrokeStyle(gradientStroke));
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
}
