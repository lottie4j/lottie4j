package com.lottie4j.fxplayer.renderer;

import com.lottie4j.core.model.Animated;
import com.lottie4j.core.model.shape.Rectangle;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.logging.Logger;

public class RectangleRenderer implements ShapeRenderer<Rectangle> {

    private static final Logger logger = Logger.getLogger(RectangleRenderer.class.getName());

    @Override
    public void render(GraphicsContext gc, Rectangle rectangle, double frame) {
        logger.info("RectangleRenderer.render called for: " + rectangle.name());

        if (rectangle.size() == null || rectangle.position() == null) {
            logger.warning("Rectangle missing size or position data");
            return;
        }

        // Get animated values at current frame
        double x = rectangle.position().getValue(Animated.ValueType.X, (long) frame);
        double y = rectangle.position().getValue(Animated.ValueType.Y, (long) frame);
        double width = rectangle.size().getValue(Animated.ValueType.WIDTH, (long) frame);
        double height = rectangle.size().getValue(Animated.ValueType.HEIGHT, (long) frame);

        logger.info("Rectangle dimensions: x=" + x + ", y=" + y + ", w=" + width + ", h=" + height);

        // For debugging, use bright visible colors
        gc.setFill(Color.CYAN);
        gc.setStroke(Color.DARKBLUE);
        gc.setLineWidth(2);

        // Draw rectangle (center it on the position)
        double rectX = x - width / 2;
        double rectY = y - height / 2;

        logger.info("Drawing rectangle at: " + rectX + ", " + rectY);

        gc.fillRect(rectX, rectY, width, height);
        gc.strokeRect(rectX, rectY, width, height);
    }

    @Override
    public Class<Rectangle> getShapeType() {
        return Rectangle.class;
    }
}