package com.lottie4j.fxplayer.renderer.shape;

import com.lottie4j.core.model.AnimatedValueType;
import com.lottie4j.core.model.shape.grouping.Group;
import com.lottie4j.core.model.shape.shape.Ellipse;
import javafx.scene.canvas.GraphicsContext;

public class EllipseRenderer implements ShapeRenderer {

    public void render(GraphicsContext gc, Ellipse ellipse, Group parentGroup, double frame) {
        if (ellipse.position() == null || ellipse.size() == null) return;

        double x = ellipse.position().getValue(AnimatedValueType.X, (long) frame);
        double y = ellipse.position().getValue(AnimatedValueType.Y, (long) frame);
        double width = ellipse.size().getValue(AnimatedValueType.X, (long) frame);
        double height = ellipse.size().getValue(AnimatedValueType.Y, (long) frame);

        gc.save();

        // Center the ellipse on its position
        double ellipseX = x - width / 2;
        double ellipseY = y - height / 2;

        gc.fillOval(ellipseX, ellipseY, width, height);
        gc.strokeOval(ellipseX, ellipseY, width, height);

        gc.restore();
    }
}
