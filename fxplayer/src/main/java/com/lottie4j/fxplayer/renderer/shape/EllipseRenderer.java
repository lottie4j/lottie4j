package com.lottie4j.fxplayer.renderer.shape;

import com.lottie4j.core.model.shape.BaseShape;
import com.lottie4j.core.model.shape.grouping.Group;
import com.lottie4j.core.model.shape.shape.Rectangle;
import com.lottie4j.fxplayer.util.LottieCoordinateHelper;
import javafx.scene.canvas.GraphicsContext;

import java.util.logging.Logger;

public class EllipseRenderer implements ShapeRenderer {

    private static final Logger logger = Logger.getLogger(EllipseRenderer.class.getName());

    @Override
    public void render(GraphicsContext gc, BaseShape shape, Group parentGroup, double frame) {
        if (!(shape instanceof Rectangle ellipse)) {
            logger.warning("EllipseRenderer called with non-Ellipse shape: " + shape.getClass().getSimpleName());
            return;
        }

        if (ellipse.position() == null || ellipse.size() == null) {
            return;
        }

        // Use helper to get position data with coordinate conversion
        var position = LottieCoordinateHelper.getRectanglePosition(ellipse, frame);

        logger.info("Rectangle Lottie center coordinates: x=" + position.x() + ", y=" + position.y());
        logger.info("Rectangle dimensions: w=" + position.width() + ", h=" + position.height());
        logger.info("Rectangle JavaFX top-left coordinates: x=" + position.topLeftX() + ", y=" + position.topLeftY());

        // Use the converted top-left coordinates for JavaFX rendering
        double renderX = position.topLeftX();
        double renderY = position.topLeftY();
        double width = position.width();
        double height = position.height();

        gc.save();

        gc.fillOval(renderX, renderY, width, height);
        gc.strokeOval(renderX, renderY, width, height);

        gc.restore();
    }
}
