package com.lottie4j.fxplayer.renderer;

import com.lottie4j.core.model.AnimatedValueType;
import com.lottie4j.core.model.shape.Ellipse;
import com.lottie4j.core.model.shape.Group;
import com.lottie4j.fxplayer.LottieRenderEngine;
import javafx.scene.canvas.GraphicsContext;

public class EllipseRenderer implements ShapeRenderer<Ellipse> {

    @Override
    public void render(LottieRenderEngine engine, GraphicsContext gc, Ellipse shape, Group parentGroup, double frame) {
        if (shape.position() == null || shape.size() == null) return;

        double x = shape.position().getValue(AnimatedValueType.X, (long) frame);
        double y = shape.position().getValue(AnimatedValueType.Y, (long) frame);
        double width = shape.size().getValue(AnimatedValueType.X, (long) frame);
        double height = shape.size().getValue(AnimatedValueType.Y, (long) frame);

        gc.save();

        // Center the ellipse on its position
        double ellipseX = x - width / 2;
        double ellipseY = y - height / 2;

        gc.fillOval(ellipseX, ellipseY, width, height);
        gc.strokeOval(ellipseX, ellipseY, width, height);

        gc.restore();
    }

    @Override
    public Class<Ellipse> getShapeType() {
        return Ellipse.class;
    }
}
