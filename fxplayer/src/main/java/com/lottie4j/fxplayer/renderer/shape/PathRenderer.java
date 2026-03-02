package com.lottie4j.fxplayer.renderer.shape;

import com.lottie4j.core.definition.LineCap;
import com.lottie4j.core.definition.LineJoin;
import com.lottie4j.core.model.bezier.AnimatedBezier;
import com.lottie4j.core.model.bezier.BezierDefinition;
import com.lottie4j.core.model.bezier.FixedBezier;
import com.lottie4j.core.model.shape.BaseShape;
import com.lottie4j.core.model.shape.grouping.Group;
import com.lottie4j.core.model.shape.shape.Path;
import com.lottie4j.core.model.shape.style.Fill;
import com.lottie4j.core.model.shape.style.GradientFill;
import com.lottie4j.core.model.shape.style.Stroke;
import com.lottie4j.fxplayer.element.FillStyle;
import com.lottie4j.fxplayer.element.GradientFillStyle;
import com.lottie4j.fxplayer.element.StrokeStyle;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Paint;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;

import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

public class PathRenderer implements ShapeRenderer {

    private static final Logger logger = Logger.getLogger(PathRenderer.class.getName());

    @Override
    public void render(GraphicsContext gc, BaseShape shape, Group parentGroup, double frame) {
        if (!(shape instanceof Path path)) {
            logger.warning("PathRenderer called with non-Path shape: " + shape.getClass().getSimpleName());
            return;
        }

        if (path.bezier() == null) return;

        BezierDefinition bezierDef = getBezierDefinition(path, frame);
        if (bezierDef == null || bezierDef.vertices() == null || bezierDef.vertices().isEmpty()) return;

        gc.save();
        gc.beginPath();

        List<List<Double>> vertices = bezierDef.vertices();
        List<List<Double>> tangentsIn = bezierDef.tangentsIn();
        List<List<Double>> tangentsOut = bezierDef.tangentsOut();

        if (vertices.size() == 8) {
            logger.warning("*** 8-VERTEX F-LOGO PATH: " + path.name() + " ***");
        }
        logger.fine("Path '" + path.name() + "' - vertices: " + vertices.size() +
                ", closed: " + bezierDef.closed());
        if (!vertices.isEmpty()) {
            logger.finer("  First vertex: " + vertices.get(0));
        }

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

        // Handle closing bezier curve from last vertex back to first vertex
        if (bezierDef.closed() != null && bezierDef.closed() && vertices.size() > 1) {
            int lastIdx = vertices.size() - 1;
            if (tangentsIn != null && tangentsOut != null &&
                    lastIdx < tangentsOut.size() && 0 < tangentsIn.size()) {

                List<Double> lastTangentOut = tangentsOut.get(lastIdx);
                List<Double> firstTangentIn = tangentsIn.get(0);

                if (lastTangentOut.size() >= 2 && firstTangentIn.size() >= 2) {
                    List<Double> lastVertex = vertices.get(lastIdx);
                    List<Double> firstVertex = vertices.get(0);

                    double cp1x = lastVertex.get(0) + lastTangentOut.get(0);
                    double cp1y = lastVertex.get(1) + lastTangentOut.get(1);
                    double cp2x = firstVertex.get(0) + firstTangentIn.get(0);
                    double cp2y = firstVertex.get(1) + firstTangentIn.get(1);

                    gc.bezierCurveTo(cp1x, cp1y, cp2x, cp2y, firstVertex.get(0), firstVertex.get(1));
                } else {
                    gc.closePath();
                }
            } else {
                gc.closePath();
            }
        }

        // Apply fill and stroke from parent group
        // Check for gradient fill first, then regular fill
        var gradientFillStyle = getGradientFillStyle(parentGroup);
        if (gradientFillStyle.isPresent()) {
            Paint gradientPaint = gradientFillStyle.get().getPaint(frame);
            gc.setFill(gradientPaint);
            double opacity = gradientFillStyle.get().getOpacity(frame);
            logger.finer("  Applying gradient fill, opacity: " + opacity);
            if (opacity < 1.0) {
                double currentAlpha = gc.getGlobalAlpha();
                gc.setGlobalAlpha(currentAlpha * opacity);
            }
            gc.fill();
            gc.setGlobalAlpha(1.0); // Reset
        } else {
            var fillStyle = getFillStyle(parentGroup);
            if (fillStyle.isPresent()) {
                var fillColor = fillStyle.get().getColor(frame);
                logger.fine("  Applying fill: " + fillColor);
                gc.setFill(fillColor);
                gc.fill();
            } else {
                logger.fine("  No fill style found");
            }
        }

        var strokeStyle = getStrokeStyle(parentGroup);
        if (strokeStyle.isPresent()) {
            var strokeColor = strokeStyle.get().getColor(frame);
            var strokeWidth = strokeStyle.get().getStrokeWidth(frame);
            logger.fine("  Applying stroke: " + strokeColor + ", width: " + strokeWidth);
            gc.setStroke(strokeColor);
            gc.setLineWidth(strokeWidth);

            // Apply line cap and join
            applyStrokeStyle(gc, strokeStyle.get().stroke);

            gc.stroke();
        } else {
            logger.fine("  No stroke style found");
        }

        gc.restore();
    }

    private Optional<FillStyle> getFillStyle(Group group) {
        if (group == null) {
            return Optional.empty();
        }
        for (BaseShape baseShape : group.shapes()) {
            if (baseShape instanceof Fill fill) {
                return Optional.of(new FillStyle(fill));
            }
        }
        return Optional.empty();
    }

    private Optional<GradientFillStyle> getGradientFillStyle(Group group) {
        if (group == null) {
            return Optional.empty();
        }
        for (BaseShape baseShape : group.shapes()) {
            if (baseShape instanceof GradientFill gradientFill) {
                return Optional.of(new GradientFillStyle(gradientFill));
            }
        }
        return Optional.empty();
    }

    private Optional<StrokeStyle> getStrokeStyle(Group group) {
        if (group == null) {
            return Optional.empty();
        }
        for (BaseShape baseShape : group.shapes()) {
            if (baseShape instanceof Stroke stroke) {
                return Optional.of(new StrokeStyle(stroke));
            }
        }
        return Optional.empty();
    }

    private BezierDefinition getBezierDefinition(Path shape, double frame) {
        if (shape.bezier() instanceof FixedBezier fixedBezier) {
            return fixedBezier.bezier();
        } else if (shape.bezier() instanceof AnimatedBezier animatedBezier) {
            return getInterpolatedBezier(animatedBezier, frame);
        }
        return null;
    }

    private BezierDefinition getInterpolatedBezier(AnimatedBezier animatedBezier, double frame) {
        if (animatedBezier.beziers() == null || animatedBezier.beziers().isEmpty()) {
            return null;
        }

        var keyframes = animatedBezier.beziers();

        // Find the appropriate keyframes for interpolation
        var prevKeyframe = keyframes.get(0);
        var nextKeyframe = keyframes.size() > 1 ? keyframes.get(1) : null;

        for (int i = 0; i < keyframes.size(); i++) {
            var keyframe = keyframes.get(i);
            if (keyframe.time() <= frame) {
                prevKeyframe = keyframe;
                if (i + 1 < keyframes.size()) {
                    nextKeyframe = keyframes.get(i + 1);
                } else {
                    nextKeyframe = null;
                }
            } else {
                break;
            }
        }

        // If no next keyframe or we're at/past the last keyframe, use the current one
        if (nextKeyframe == null || prevKeyframe.beziers() == null || prevKeyframe.beziers().isEmpty()) {
            return prevKeyframe.beziers() != null && !prevKeyframe.beziers().isEmpty()
                    ? prevKeyframe.beziers().get(0)
                    : null;
        }

        // If next keyframe has no bezier data, use previous
        if (nextKeyframe.beziers() == null || nextKeyframe.beziers().isEmpty()) {
            return prevKeyframe.beziers().get(0);
        }

        // Interpolate between keyframes
        double startFrame = prevKeyframe.time();
        double endFrame = nextKeyframe.time();
        double progress = (frame - startFrame) / (endFrame - startFrame);
        progress = Math.max(0, Math.min(1, progress));

        // Apply Bezier easing if available
        if (prevKeyframe.easingOut() != null && prevKeyframe.easingIn() != null) {
            progress = applyBezierEasing(progress, prevKeyframe.easingOut(), prevKeyframe.easingIn());
        }

        BezierDefinition prevBezier = prevKeyframe.beziers().get(0);
        BezierDefinition nextBezier = nextKeyframe.beziers().get(0);

        // Interpolate vertices
        List<List<Double>> interpolatedVertices = interpolateVertices(
                prevBezier.vertices(), nextBezier.vertices(), progress);
        List<List<Double>> interpolatedTangentsIn = interpolateVertices(
                prevBezier.tangentsIn(), nextBezier.tangentsIn(), progress);
        List<List<Double>> interpolatedTangentsOut = interpolateVertices(
                prevBezier.tangentsOut(), nextBezier.tangentsOut(), progress);

        return new BezierDefinition(
                prevBezier.closed(),
                interpolatedVertices,
                interpolatedTangentsIn,
                interpolatedTangentsOut
        );
    }

    private List<List<Double>> interpolateVertices(List<List<Double>> start, List<List<Double>> end, double progress) {
        if (start == null || end == null || start.isEmpty()) {
            return start;
        }

        List<List<Double>> result = new java.util.ArrayList<>();
        int size = Math.min(start.size(), end.size());

        for (int i = 0; i < size; i++) {
            List<Double> startVertex = start.get(i);
            List<Double> endVertex = end.get(i);

            if (startVertex.size() >= 2 && endVertex.size() >= 2) {
                double x = startVertex.get(0) + (endVertex.get(0) - startVertex.get(0)) * progress;
                double y = startVertex.get(1) + (endVertex.get(1) - startVertex.get(1)) * progress;
                result.add(List.of(x, y));
            } else {
                result.add(startVertex);
            }
        }

        return result;
    }

    private double applyBezierEasing(double t, com.lottie4j.core.model.EasingHandle easingOut,
                                     com.lottie4j.core.model.EasingHandle easingIn) {
        double x1 = easingOut.x() != null && !easingOut.x().isEmpty() ? easingOut.x().get(0) : 0.0;
        double y1 = easingOut.y() != null && !easingOut.y().isEmpty() ? easingOut.y().get(0) : 0.0;
        double x2 = easingIn.x() != null && !easingIn.x().isEmpty() ? easingIn.x().get(0) : 1.0;
        double y2 = easingIn.y() != null && !easingIn.y().isEmpty() ? easingIn.y().get(0) : 1.0;

        double currentT = t;
        for (int i = 0; i < 8; i++) {
            double currentX = cubicBezier(currentT, 0, x1, x2, 1);
            double dx = currentX - t;
            if (Math.abs(dx) < 0.001) break;

            double derivative = cubicBezierDerivative(currentT, 0, x1, x2, 1);
            if (Math.abs(derivative) < 1e-6) break;

            currentT = currentT - dx / derivative;
        }

        return cubicBezier(currentT, 0, y1, y2, 1);
    }

    private double cubicBezier(double t, double p0, double p1, double p2, double p3) {
        double t2 = t * t;
        double t3 = t2 * t;
        double mt = 1 - t;
        double mt2 = mt * mt;
        double mt3 = mt2 * mt;
        return mt3 * p0 + 3 * mt2 * t * p1 + 3 * mt * t2 * p2 + t3 * p3;
    }

    private double cubicBezierDerivative(double t, double p0, double p1, double p2, double p3) {
        double t2 = t * t;
        double mt = 1 - t;
        double mt2 = mt * mt;
        return 3 * mt2 * (p1 - p0) + 6 * mt * t * (p2 - p1) + 3 * t2 * (p3 - p2);
    }

    private void applyStrokeStyle(GraphicsContext gc, Stroke stroke) {
        // Set line cap: 1=butt, 2=round, 3=square
        if (stroke.lineCap() != null) {
            switch (stroke.lineCap()) {
                case LineCap.ROUND -> gc.setLineCap(StrokeLineCap.ROUND);
                case LineCap.SQUARE -> gc.setLineCap(StrokeLineCap.SQUARE);
                default -> gc.setLineCap(StrokeLineCap.BUTT);
            }
        }

        // Set line join: 1=miter, 2=round, 3=bevel
        if (stroke.lineJoin() != null) {
            switch (stroke.lineJoin()) {
                case LineJoin.ROUND -> gc.setLineJoin(StrokeLineJoin.ROUND);
                case LineJoin.BEVEL -> gc.setLineJoin(StrokeLineJoin.BEVEL);
                default -> gc.setLineJoin(StrokeLineJoin.MITER);
            }
        }

        // Set miter limit if specified
        if (stroke.miterLimit() != null) {
            gc.setMiterLimit(stroke.miterLimit());
        }
    }
}