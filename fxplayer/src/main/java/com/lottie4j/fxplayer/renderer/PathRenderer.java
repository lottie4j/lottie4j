
package com.lottie4j.fxplayer.renderer;

import com.lottie4j.core.model.shape.Path;
import com.lottie4j.core.model.bezier.AnimatedBezier;
import com.lottie4j.core.model.bezier.FixedBezier;
import com.lottie4j.core.model.bezier.BezierDefinition;
import javafx.scene.canvas.GraphicsContext;

import java.util.List;

public class PathRenderer implements ShapeRenderer<Path> {

    @Override
    public void render(GraphicsContext gc, Path shape, double frame) {
        if (shape.bezier() == null) return;

        BezierDefinition bezierDef = getBezierDefinition(shape, frame);
        if (bezierDef == null || bezierDef.vertices() == null || bezierDef.vertices().isEmpty()) return;

        gc.save();
        gc.beginPath();

        List<List<Double>> vertices = bezierDef.vertices();
        List<List<Double>> tangentsIn = bezierDef.tangentsIn();
        List<List<Double>> tangentsOut = bezierDef.tangentsOut();

        boolean first = true;
        for (int i = 0; i < vertices.size(); i++) {
            List<Double> vertex = vertices.get(i);
            if (vertex.size() < 2) continue;

            double x = vertex.get(0);
            double y = vertex.get(1);

            if (first) {
                gc.moveTo(x, y);
                first = false;
            } else {
                // Handle bezier curves if tangents are available
                if (tangentsIn != null && tangentsOut != null &&
                        i - 1 < tangentsOut.size() && i < tangentsIn.size()) {

                    List<Double> prevTangentOut = tangentsOut.get(i - 1);
                    List<Double> currentTangentIn = tangentsIn.get(i);

                    if (prevTangentOut.size() >= 2 && currentTangentIn.size() >= 2) {
                        List<Double> prevVertex = vertices.get(i - 1);

                        double cp1x = prevVertex.get(0) + prevTangentOut.get(0);
                        double cp1y = prevVertex.get(1) + prevTangentOut.get(1);
                        double cp2x = x + currentTangentIn.get(0);
                        double cp2y = y + currentTangentIn.get(1);

                        gc.bezierCurveTo(cp1x, cp1y, cp2x, cp2y, x, y);
                    } else {
                        gc.lineTo(x, y);
                    }
                } else {
                    gc.lineTo(x, y);
                }
            }
        }

        if (bezierDef.closed() != null && bezierDef.closed()) {
            gc.closePath();
        }

        gc.fill();
        gc.stroke();

        gc.restore();
    }

    private BezierDefinition getBezierDefinition(Path shape, double frame) {
        if (shape.bezier() instanceof FixedBezier fixedBezier) {
            return fixedBezier.bezier();
        } else if (shape.bezier() instanceof AnimatedBezier animatedBezier) {
            // For now, just use the first keyframe - this could be improved
            // to properly interpolate between keyframes based on the frame
            if (animatedBezier.beziers() != null && !animatedBezier.beziers().isEmpty()) {
                var keyframe = animatedBezier.beziers().get(0);
                if (keyframe.beziers() != null && !keyframe.beziers().isEmpty()) {
                    return keyframe.beziers().get(0);
                }
            }
        }
        return null;
    }


    @Override
    public Class<Path> getShapeType() {
        return Path.class;
    }
}