package com.lottie4j.fxplayer.element;

import com.lottie4j.core.model.Transform;
import com.lottie4j.core.model.bezier.AnimatedBezier;
import com.lottie4j.core.model.bezier.BezierDefinition;
import com.lottie4j.core.model.bezier.FixedBezier;
import com.lottie4j.core.model.keyframe.NumberKeyframe;
import com.lottie4j.core.model.keyframe.TimedKeyframe;
import com.lottie4j.core.model.shape.*;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.logging.Level;
import java.util.logging.Logger;

public class ShapeDrawer {

    private static final Logger logger = Logger.getLogger(ShapeDrawer.class.getName());

    private ShapeDrawer() {
        // Hide constructor
    }

    public static void draw(GraphicsContext gc, BaseShape baseShape) {
        logger.info("Drawing " + baseShape.getClass().getName());
        if (baseShape instanceof Group group) {
            drawGroup(gc, group);
            return;
        }
        logger.log(Level.WARNING, "Unexpected shape");
    }

    /**
     * A Lottie Group consists of a ShapeGroup.SHAPE object
     * with one or more ShapeGroup.STYLE and/or ShapeGroup.TRANSFORM.
     *
     * @param gc
     * @param group
     */
    private static void drawGroup(GraphicsContext gc, Group group) {
        logger.info(" > Group " + group.name());
        for (BaseShape baseShape : group.shapes()) {
            logger.info("   > Drawing " + baseShape.getClass().getName());
            if (baseShape instanceof Group subGroup) {
                drawGroup(gc, subGroup);
            } else if (baseShape instanceof Ellipse ellipse) {
                drawEllipse(gc, ellipse);
            } else if (baseShape instanceof Rectangle rectangle) {
                drawRectangle(gc, rectangle);
            } else if (baseShape instanceof Path path) {
                drawPath(gc, path, group);
            }
        }
    }

    private static void drawPath(GraphicsContext gc, Path path, Group group) {
        if (path.bezier() == null) {
            return;
        }
        if (path.bezier() instanceof FixedBezier fixedBezier) {
            if (fixedBezier.bezier() != null) {
                drawBezier(gc, fixedBezier.bezier());
            }
        } else if (path.bezier() instanceof AnimatedBezier animatedBezier) {
            if (animatedBezier.beziers() != null
                    && !animatedBezier.beziers().isEmpty()) {
                drawBezier(gc, animatedBezier.beziers().get(0).beziers().get(0));
            }
        }
    }

    private static void drawBezier(GraphicsContext gc, BezierDefinition bezierDefinition) {
        var vertices = bezierDefinition.vertices();
        if (vertices.size() >= 2) {
            gc.moveTo(vertices.get(0).get(0), vertices.get(0).get(1));
            for (int i = 1; i < vertices.size(); i++) {
                gc.lineTo(vertices.get(i).get(0), vertices.get(i).get(1));
                gc.stroke();
            }
        }
    }

    private static void setStroke(GraphicsContext gc, Group group, Long time) {
        for (BaseShape shape : group.shapes()) {
            if (shape instanceof Stroke stroke) {
                var strokeWidthKeyframe = stroke.strokeWidth().atTime(time);
                var strokeWidth = 1;
                if (strokeWidthKeyframe instanceof NumberKeyframe numberKeyframe) {
                    strokeWidth = numberKeyframe.intValue();
                } else if (strokeWidthKeyframe instanceof TimedKeyframe timedKeyframe) {
                    strokeWidth = timedKeyframe.values() != null && !timedKeyframe.values().isEmpty() ? timedKeyframe.values().get(0).intValue() : 1;
                }
                gc.setLineWidth(strokeWidth);
                gc.setStroke(Color.DARKVIOLET);
            }
        }


    }

    private static void drawEllipse(GraphicsContext gc, Ellipse ellipse) {
        gc.setFill(Color.DARKKHAKI);
        /*graphicContext.fillOval(ellipse.position().keyframes().get(0).intValue(),
                ellipse.position().keyframes().get(1).intValue(),
                ellipse.size().keyframes().get(0).intValue(),
                ellipse.size().keyframes().get(1).intValue());*/
    }

    private static void drawRectangle(GraphicsContext gc, Rectangle rectangle) {
        gc.setFill(Color.DARKKHAKI);

        //gc.setStroke(colors[colorIdx]);
        /*
        gc.rect((double) rectangle.position().keyframes().get(0),
                (double) rectangle.position().keyframes().get(1),
                (double) rectangle.size().keyframes().get(0),
                (double) rectangle.size().keyframes().get(1));
        */
        //gc.strokeRect(0, 200, 50, 200);
        //gc.fillRect(10, 320, 300, 40);
    }

    private static void drawTransform(GraphicsContext gc, Transform transform) {
    }

    private static void drawFill(GraphicsContext gc, Fill fill) {
        //graphicContext.fillArc(11, 111, 31, 31, 46, 241, ArcType.OPEN);
    }

    private static void drawStroke(GraphicsContext gc, Stroke stroke) {
        gc.setStroke(Color.DARKVIOLET);
        gc.setLineWidth(6);
        /*graphicContext.strokeOval(stroke.position().keyframes().get(0).intValue(),
                stroke.position().keyframes().get(1).intValue(),
                stroke.size().keyframes().get(0).intValue(),
                stroke.size().keyframes().get(1).intValue());*/
    }
}
