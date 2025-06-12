package com.lottie4j.fxplayer.renderer;

import com.lottie4j.core.model.AnimatedValueType;
import com.lottie4j.core.model.shape.*;
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
        Transform transform = null;
        Fill fill = null;
        Stroke stroke = null;

        for (BaseShape item : group.shapes()) {
            if (item instanceof Transform) {
                transform = (Transform) item;
            } else if (item instanceof Fill) {
                fill = (Fill) item;
            } else if (item instanceof Stroke) {
                stroke = (Stroke) item;
            }
        }

        // Apply group transform if it exists
        if (transform != null) {
            applyTransform(gc, transform, frame);
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
        // Apply position
        if (transform.position() != null) {
            double x = transform.position().getValue(AnimatedValueType.X, (long) frame);
            double y = transform.position().getValue(AnimatedValueType.Y, (long) frame);
            logger.info("Group transform position: " + x + ", " + y);
            gc.translate(x, y);
        }

        // Apply anchor point offset (note: it's anchor(), not anchorPoint())
        if (transform.anchor() != null) {
            double ax = transform.anchor().getValue(AnimatedValueType.X, (long) frame);
            double ay = transform.anchor().getValue(AnimatedValueType.Y, (long) frame);
            logger.info("Group transform anchor: " + ax + ", " + ay);
            gc.translate(-ax, -ay);
        }

        // Apply scale
        if (transform.scale() != null) {
            double scaleX = transform.scale().getValue(AnimatedValueType.X, (long) frame) / 100.0;
            double scaleY = transform.scale().getValue(AnimatedValueType.Y, (long) frame) / 100.0;
            logger.info("Group transform scale: " + scaleX + ", " + scaleY);
            gc.scale(scaleX, scaleY);
        }

        // Apply rotation
        if (transform.rotation() != null) {
            double rotation = Math.toRadians(transform.rotation().getValue(0, (long) frame));
            logger.info("Group transform rotation: " + Math.toDegrees(rotation) + " degrees");
            gc.rotate(rotation);
        }

        // Apply opacity
        if (transform.opacity() != null) {
            double opacity = transform.opacity().getValue(AnimatedValueType.OPACITY, (long) frame);
            logger.info("Group transform opacity: " + opacity);
            gc.setGlobalAlpha(gc.getGlobalAlpha() * (opacity / 100.0));
        }
    }
}