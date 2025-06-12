package com.lottie4j.fxplayer.renderer;

import com.lottie4j.core.model.AnimatedValueType;
import com.lottie4j.core.model.shape.*;
import com.lottie4j.fxplayer.LottieRenderEngine;
import com.lottie4j.fxplayer.element.FillStyle;
import com.lottie4j.fxplayer.element.StrokeStyle;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.Optional;
import java.util.logging.Logger;

public class RectangleRenderer implements ShapeRenderer<Rectangle> {

    private static final Logger logger = Logger.getLogger(RectangleRenderer.class.getName());

    @Override
    public void render(LottieRenderEngine engine, GraphicsContext gc, Rectangle rectangle, Group parentGroup, double frame) {
        logger.info("RectangleRenderer.render called for: " + rectangle.name());

        if (rectangle.size() == null || rectangle.position() == null) {
            logger.warning("Rectangle missing size or position data");
            return;
        }

        // Get animated values at current frame
        double x = rectangle.position().getValue(AnimatedValueType.X, (long) frame);
        double y = rectangle.position().getValue(AnimatedValueType.Y, (long) frame);
        double width = rectangle.size().getValue(AnimatedValueType.WIDTH, (long) frame);
        double height = rectangle.size().getValue(AnimatedValueType.HEIGHT, (long) frame);

        logger.info("Rectangle dimensions: x=" + x + ", y=" + y + ", w=" + width + ", h=" + height);

        // For debugging, use bright visible colors
        gc.setFill(Color.CYAN);
        gc.setStroke(Color.DARKBLUE);
        gc.setLineWidth(2);

        // Draw rectangle (center it on the position)
        double rectX = x - width / 2;
        double rectY = y - height / 2;

        logger.info("Drawing rectangle at: " + rectX + ", " + rectY);

        var fillStyle = getFillStyle(parentGroup);
        if (fillStyle.isPresent()) {
            gc.setFill(fillStyle.get().getColor(0));
        }
        var strokeStyle = getStrokeStyle(parentGroup);
        if (strokeStyle.isPresent()) {
            gc.setStroke(strokeStyle.get().getColor(0));
            gc.setLineWidth(strokeStyle.get().getStrokeWidth(0));
        }
        gc.strokeRect(rectangle.position().getValue(AnimatedValueType.X, 0L),
                rectangle.position().getValue(AnimatedValueType.X, 0L),
                rectangle.size().getValue(AnimatedValueType.WIDTH, 0L),
                rectangle.size().getValue(AnimatedValueType.HEIGHT, 0L));
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

    @Override
    public Class<Rectangle> getShapeType() {
        return Rectangle.class;
    }
}