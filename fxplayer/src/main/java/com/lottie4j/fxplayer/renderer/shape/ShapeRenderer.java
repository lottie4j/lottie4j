package com.lottie4j.fxplayer.renderer.shape;

import com.lottie4j.core.model.shape.BaseShape;
import com.lottie4j.core.model.shape.grouping.Group;
import javafx.scene.canvas.GraphicsContext;

public interface ShapeRenderer {
    /**
     * Renders a single Lottie shape instance.
     *
     * @param gc          destination graphics context
     * @param shape       shape to render
     * @param parentGroup parent group containing styles/modifiers, or null when standalone
     * @param frame       animation frame to sample
     */
    void render(GraphicsContext gc, BaseShape shape, Group parentGroup, double frame);
}
