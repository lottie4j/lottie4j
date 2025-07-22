package com.lottie4j.fxplayer.renderer.shape;

import com.lottie4j.core.model.shape.BaseShape;
import com.lottie4j.core.model.shape.grouping.Group;
import javafx.scene.canvas.GraphicsContext;

public interface ShapeRenderer {
    void render(GraphicsContext gc, BaseShape shape, Group parentGroup, double frame);
}
