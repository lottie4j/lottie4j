package com.lottie4j.fxplayer.renderer;

import com.lottie4j.core.model.AnimatedValueType;
import com.lottie4j.core.model.shape.BaseShape;
import com.lottie4j.core.model.shape.Group;
import com.lottie4j.core.model.shape.Transform;
import com.lottie4j.fxplayer.LottieRenderEngine;
import javafx.scene.canvas.GraphicsContext;

import java.util.logging.Logger;

public class GroupRenderer implements ShapeRenderer<Group> {

    private static final Logger logger = Logger.getLogger(GroupRenderer.class.getName());

    @Override
    public void render(LottieRenderEngine engine, GraphicsContext gc, Group group, Group parentGroup, double frame) {
        logger.info("GroupRenderer.render called for group with " + group.shapes().size() + " items");

        gc.save();

        // Find transform, fill and stroke styles in the group
        for (BaseShape item : group.shapes()) {
            if (item instanceof Transform transform) {
                //applyTransform(gc, transform, frame);
            }
        }

        // Render shapes with the found styles
        for (BaseShape shape : group.shapes()) {
            engine.renderShape(gc, shape, group, frame);
        }

        gc.restore();
    }

    @Override
    public Class<Group> getShapeType() {
        return Group.class;
    }

    private void applyTransform(GraphicsContext gc, Transform transform, double frame) {
        // Get position
        double translateX = transform.position() != null ?
                transform.position().getValue(AnimatedValueType.X, (long) frame) : 0;
        double translateY = transform.position() != null ?
                transform.position().getValue(AnimatedValueType.Y, (long) frame) : 0;

        // Get anchor point
        /*double anchorX = transform.anchorPoint() != null ?
                transform.anchorPoint().getValue(AnimatedValueType.X, (long) frame) : 0;
        double anchorY = transform.anchorPoint() != null ?
                transform.anchorPoint().getValue(AnimatedValueType.Y, (long) frame) : 0;*/

        // Get scale
        double scaleX = transform.scale() != null ?
                transform.scale().getValue(AnimatedValueType.X, (long) frame) / 100.0 : 1.0;
        double scaleY = transform.scale() != null ?
                transform.scale().getValue(AnimatedValueType.Y, (long) frame) / 100.0 : 1.0;

        // Get rotation
        double rotation = transform.rotation() != null ?
                transform.rotation().getValue(0, (long) frame) : 0;

        // Get opacity - IMPORTANT: Divide by 100 to convert from percentage to decimal
        double opacity = transform.opacity() != null ?
                Math.max(0, Math.min(1, transform.opacity().getValue(0, (long) frame) / 100.0)) : 1.0;
        opacity = 1;

        // Apply transformations in the correct order
        gc.translate(translateX, translateY);
        //gc.translate(-anchorX, -anchorY);
        gc.scale(scaleX, scaleY);
        gc.rotate(rotation);

        // Set opacity
        gc.setGlobalAlpha(gc.getGlobalAlpha() * opacity);

        // Log the transform values for debugging
        logger.info("Group transform position: " + translateX + ", " + translateY);
        //logger.info("Group transform anchor: " + anchorX + ", " + anchorY);
        logger.info("Group transform scale: " + scaleX + ", " + scaleY);
        logger.info("Group transform rotation: " + rotation + " degrees");
        logger.info("Group transform opacity: " + opacity);
    }
}