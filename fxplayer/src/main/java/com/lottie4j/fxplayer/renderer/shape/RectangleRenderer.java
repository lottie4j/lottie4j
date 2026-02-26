package com.lottie4j.fxplayer.renderer.shape;

import com.lottie4j.core.model.shape.BaseShape;
import com.lottie4j.core.model.shape.grouping.Group;
import com.lottie4j.core.model.shape.shape.Rectangle;
import com.lottie4j.core.model.shape.style.Fill;
import com.lottie4j.core.model.shape.style.GradientFill;
import com.lottie4j.core.model.shape.style.Stroke;
import com.lottie4j.fxplayer.element.FillStyle;
import com.lottie4j.fxplayer.element.GradientFillStyle;
import com.lottie4j.fxplayer.element.StrokeStyle;
import com.lottie4j.fxplayer.util.LottieCoordinateHelper;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Paint;

import java.util.Optional;
import java.util.logging.Logger;

public class RectangleRenderer implements ShapeRenderer {

    private static final Logger logger = Logger.getLogger(RectangleRenderer.class.getName());

    @Override
    public void render(GraphicsContext gc, BaseShape shape, Group parentGroup, double frame) {
        if (!(shape instanceof Rectangle rectangle)) {
            logger.warning("RectangleRenderer called with non-Rectangle shape: " + shape.getClass().getSimpleName());
            return;
        }

        logger.info("RectangleRenderer.render called for: " + rectangle.name());

        if (rectangle.size() == null) {
            logger.warning("Rectangle missing size data");
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
            gc.fillRect(renderX, renderY, width, height);
            gc.setGlobalAlpha(1.0); // Reset
        } else {
            var fillStyle = getFillStyle(parentGroup);
            if (fillStyle.isPresent()) {
                var fillColor = fillStyle.get().getColor(frame);
                gc.setFill(fillColor);
                gc.fillRect(renderX, renderY, width, height);
            }
        }

        var strokeStyle = getStrokeStyle(parentGroup);
        if (strokeStyle.isPresent()) {
            logger.info("Drawing rectangle stroke with color and width: "
                    + strokeStyle.get().getColor(frame)
                    + strokeStyle.get().getStrokeWidth(frame));
            gc.setStroke(strokeStyle.get().getColor(frame));
            gc.setLineWidth(strokeStyle.get().getStrokeWidth(frame));
            gc.strokeRect(renderX, renderY, width, height);
        }
    }

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