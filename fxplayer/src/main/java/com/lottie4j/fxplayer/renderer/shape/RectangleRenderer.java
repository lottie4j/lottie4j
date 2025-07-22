package com.lottie4j.fxplayer.renderer.shape;

import com.lottie4j.core.model.shape.BaseShape;
import com.lottie4j.core.model.shape.grouping.Group;
import com.lottie4j.core.model.shape.shape.Rectangle;
import com.lottie4j.core.model.shape.style.Fill;
import com.lottie4j.core.model.shape.style.Stroke;
import com.lottie4j.fxplayer.element.FillStyle;
import com.lottie4j.fxplayer.element.StrokeStyle;
import com.lottie4j.fxplayer.util.LottieCoordinateHelper;
import javafx.scene.canvas.GraphicsContext;

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

        if (rectangle.size() == null || rectangle.position() == null) {
            logger.warning("Rectangle missing size or position data");
            return;
        }

        // Use helper to get position data with coordinate conversion
        var position = LottieCoordinateHelper.getRectanglePosition(rectangle, frame);

        logger.info("Rectangle Lottie center coordinates: x=" + position.x() + ", y=" + position.y());
        logger.info("Rectangle dimensions: w=" + position.width() + ", h=" + position.height());
        logger.info("Rectangle JavaFX top-left coordinates: x=" + position.topLeftX() + ", y=" + position.topLeftY());

        // Use the converted top-left coordinates for JavaFX rendering
        double renderX = position.topLeftX();
        double renderY = position.topLeftY();
        double width = position.width();
        double height = position.height();

        var fillStyle = getFillStyle(parentGroup);
        if (fillStyle.isPresent()) {
            var fillColor = fillStyle.get().getColor(frame);
            logger.info("Drawing rectangle, filled with color: " + fillColor);
            gc.setFill(fillColor);
            gc.fillRect(renderX, renderY, width, height);
            gc.restore();
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