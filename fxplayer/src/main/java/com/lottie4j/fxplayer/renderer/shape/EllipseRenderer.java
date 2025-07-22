package com.lottie4j.fxplayer.renderer.shape;

import com.lottie4j.core.model.AnimatedValueType;
import com.lottie4j.core.model.shape.BaseShape;
import com.lottie4j.core.model.shape.grouping.Group;
import com.lottie4j.core.model.shape.shape.Rectangle;
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

        if (ellipse.position() == null || ellipse.size() == null) return;

        double x = ellipse.position().getValue(AnimatedValueType.X, frame);
        double y = ellipse.position().getValue(AnimatedValueType.Y, frame);
        double width = ellipse.size().getValue(AnimatedValueType.X, frame);
        double height = ellipse.size().getValue(AnimatedValueType.Y, frame);

        gc.save();

        // Center the ellipse on its position
        double ellipseX = x - width / 2;
        double ellipseY = y - height / 2;

        gc.fillOval(ellipseX, ellipseY, width, height);
        gc.strokeOval(ellipseX, ellipseY, width, height);

        gc.restore();
    }
}
