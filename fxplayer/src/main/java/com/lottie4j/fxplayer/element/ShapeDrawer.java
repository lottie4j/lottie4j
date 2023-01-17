package com.lottie4j.fxplayer.element;

import com.lottie4j.core.model.Transform;
import com.lottie4j.core.model.shape.*;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.logging.Logger;

public class ShapeDrawer {

    private static final Logger logger = Logger.getLogger(ShapeDrawer.class.getName());

    private ShapeDrawer() {
        // Hide constructor
    }

    public static void draw(GraphicsContext gc, Object shape) {
        logger.info("Drawing " + shape);
        if (shape instanceof Group group) {
            group.shapes().forEach(s -> draw(gc, s));
        } else if (shape instanceof Ellipse ellipse) {
            drawEllipse(gc, ellipse);
        } else if (shape instanceof Stroke stroke) {
            drawStroke(gc, stroke);
        } else if (shape instanceof Path path) {
            drawPath(gc, path);
        }
    }

    private static void drawPath(GraphicsContext gc, Path path) {
        if (path.animatedBezier() != null
                && path.animatedBezier().bezier() != null
                && path.animatedBezier().bezier().vertices() != null) {
            var vertices = path.animatedBezier().bezier().vertices();
            if (vertices.size() >= 2) {
                gc.moveTo(vertices.get(0).get(0), vertices.get(0).get(1));
                for (int i = 1; i < vertices.size(); i++) {
                    gc.lineTo(vertices.get(i).get(0), vertices.get(i).get(1));
                    gc.stroke();
                }
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
