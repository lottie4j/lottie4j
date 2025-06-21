package com.lottie4j.fxplayer.renderer.grouping;

import com.lottie4j.core.model.shape.grouping.Group;
import com.lottie4j.core.model.shape.grouping.Transform;
import com.lottie4j.fxplayer.LottieRenderEngine;
import com.lottie4j.fxplayer.renderer.shape.ShapeRenderer;
import javafx.scene.canvas.GraphicsContext;

public class TransformRenderer implements ShapeRenderer<Transform> {
    @Override
    public void render(LottieRenderEngine engine, GraphicsContext gc, Transform transform, Group parentGroup, double frame) {
        // Transform is usually handled by the parent group
        // This can be empty or handle specific transform operations
    }

    @Override
    public Class<Transform> getShapeType() {
        return Transform.class;
    }
}

