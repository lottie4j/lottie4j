package com.lottie4j.fxplayer.renderer.shape;

import com.lottie4j.core.definition.LineCap;
import com.lottie4j.core.definition.LineJoin;
import com.lottie4j.core.model.bezier.AnimatedBezier;
import com.lottie4j.core.model.bezier.BezierDefinition;
import com.lottie4j.core.model.bezier.FixedBezier;
import com.lottie4j.core.model.shape.BaseShape;
import com.lottie4j.core.model.shape.grouping.Group;
import com.lottie4j.core.model.shape.modifier.TrimPath;
import com.lottie4j.core.model.shape.shape.Path;
import com.lottie4j.core.model.shape.style.Fill;
import com.lottie4j.core.model.shape.style.GradientFill;
import com.lottie4j.core.model.shape.style.Stroke;
import com.lottie4j.fxplayer.element.FillStyle;
import com.lottie4j.fxplayer.element.GradientFillStyle;
import com.lottie4j.fxplayer.element.StrokeStyle;
import com.lottie4j.fxplayer.util.StrokeHelper;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Paint;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;

import java.util.ArrayList;
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
                }
            }
            gc.closePath();
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
            var strokeWidth = strokeStyle.get().getStrokeWidth(frame);

            if (StrokeHelper.shouldRenderStroke(strokeWidth)) {
                // Check for TrimPath - if present, we need special handling
                var trimPath = getTrimPath(parentGroup);
                if (trimPath.isPresent()) {
                    double trimStart = trimPath.get().segmentStart() != null ?
                            trimPath.get().segmentStart().getValue(0, frame) : 0;
                    double trimEnd = trimPath.get().segmentEnd() != null ?
                            trimPath.get().segmentEnd().getValue(0, frame) : 100;

                    // Handle NaN values (animation interpolation bug at exact keyframe boundaries)
                    if (Double.isNaN(trimStart)) {
                        logger.warning("  TrimPath start is NaN at frame " + frame + ", defaulting to 0");
                        trimStart = 0;
                    }
                    if (Double.isNaN(trimEnd)) {
                        logger.warning("  TrimPath end is NaN at frame " + frame + ", using start value as fallback");
                        // When end is NaN in a reversed path (start > end), use 0 to show full path
                        // When end is NaN in a normal path, use start value to show nothing
                        trimEnd = (trimStart > 50) ? 0 : trimStart;
                    }

                    // Handle different trim cases
                    logger.fine("  Path TrimPath: start=" + trimStart + ", end=" + trimEnd + " at frame " + frame);
                    if (Math.abs(trimStart - trimEnd) < 0.01) {
                        // Empty path - start and end are essentially the same
                        logger.fine("  Path TrimPath is empty (start=" + trimStart + ", end=" + trimEnd + ") - skipping");
                    } else if (trimStart >= 100 && trimEnd >= 100) {
                        // Full path
                        var strokeColor = strokeStyle.get().getColor(frame);
                        double compensatedWidth = StrokeHelper.getCompensatedStrokeWidth(gc, strokeWidth);
                        logger.fine("  TrimPath full path, rendering stroke: " + strokeColor + ", width: " + strokeWidth);
                        gc.setStroke(strokeColor);
                        gc.setLineWidth(compensatedWidth);
                        applyStrokeStyle(gc, strokeStyle.get().stroke);
                        gc.stroke();
                    } else if (trimStart > trimEnd) {
                        // Reversed trim: render from 0 to trimEnd, then from trimStart to 100
                        // For simplicity, render the visible portion from trimEnd to trimStart
                        logger.fine("  Reversed TrimPath (start=" + trimStart + ", end=" + trimEnd + ") - rendering from end to start");
                        renderTrimmedPath(gc, vertices, tangentsIn, tangentsOut, bezierDef.closed(),
                                trimEnd, trimStart, strokeStyle.get(), strokeWidth, frame);
                    } else {
                        // Normal trim: render from trimStart to trimEnd
                        renderTrimmedPath(gc, vertices, tangentsIn, tangentsOut, bezierDef.closed(),
                                trimStart, trimEnd, strokeStyle.get(), strokeWidth, frame);
                    }
                } else {
                    var strokeColor = strokeStyle.get().getColor(frame);
                    double compensatedWidth = StrokeHelper.getCompensatedStrokeWidth(gc, strokeWidth);

                    logger.fine("  Applying stroke: " + strokeColor + ", width: " + strokeWidth +
                            " (compensated: " + compensatedWidth + ")");
                    gc.setStroke(strokeColor);
                    gc.setLineWidth(compensatedWidth);

                    // Apply line cap and join
                    applyStrokeStyle(gc, strokeStyle.get().stroke);

                    gc.stroke();
                }
            } else {
                logger.fine("  Skipping stroke with width: " + strokeWidth);
            }
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

    private Optional<TrimPath> getTrimPath(Group group) {
        if (group == null) {
            return Optional.empty();
        }
        for (BaseShape baseShape : group.shapes()) {
            if (baseShape instanceof TrimPath trimPath) {
                return Optional.of(trimPath);
            }
        }
        return Optional.empty();
    }

    private void renderTrimmedPath(GraphicsContext gc, List<List<Double>> vertices,
                                   List<List<Double>> tangentsIn, List<List<Double>> tangentsOut,
                                   Boolean closed, double trimStart, double trimEnd,
                                   StrokeStyle strokeStyle, double strokeWidth, double frame) {
        if (vertices.isEmpty()) return;

        // Calculate segment lengths
        List<Double> segmentLengths = new ArrayList<>();
        double totalLength = 0;

        for (int i = 0; i < vertices.size(); i++) {
            int nextIdx = (i + 1) % vertices.size();
            if (!closed && nextIdx == 0) break; // Don't include closing segment for open paths

            double segmentLength = calculateSegmentLength(vertices, tangentsIn, tangentsOut, i, nextIdx);
            segmentLengths.add(segmentLength);
            totalLength += segmentLength;
        }

        if (totalLength == 0) return;

        // Convert trim percentages to lengths
        double startLength = (trimStart / 100.0) * totalLength;
        double endLength = (trimEnd / 100.0) * totalLength;

        logger.fine("  Rendering trimmed path: totalLength=" + totalLength + ", start=" + startLength + ", end=" + endLength);

        // Build new path with only the trimmed portion
        gc.save();
        gc.beginPath();

        double currentLength = 0;
        boolean pathStarted = false;
        int segmentsRendered = 0;

        for (int i = 0; i < segmentLengths.size(); i++) {
            double segmentLength = segmentLengths.get(i);
            double segmentStart = currentLength;
            double segmentEnd = currentLength + segmentLength;

            int nextIdx = (i + 1) % vertices.size();
            List<Double> vertex = vertices.get(i);
            List<Double> nextVertex = vertices.get(nextIdx);

            // Check if this segment overlaps with trim range
            if (segmentEnd > startLength && segmentStart < endLength) {
                double t0 = Math.max(0, (startLength - segmentStart) / segmentLength);
                double t1 = Math.min(1, (endLength - segmentStart) / segmentLength);

                logger.fine("    Segment " + i + ": segmentStart=" + segmentStart + ", segmentEnd=" + segmentEnd +
                        ", t0=" + t0 + ", t1=" + t1);

                // Get points at t0 and t1
                double[] point0 = evaluateBezierSegment(vertices, tangentsIn, tangentsOut, i, nextIdx, t0);
                double[] point1 = evaluateBezierSegment(vertices, tangentsIn, tangentsOut, i, nextIdx, t1);

                logger.fine("    Point0: (" + point0[0] + ", " + point0[1] + "), Point1: (" + point1[0] + ", " + point1[1] + ")");

                if (!pathStarted) {
                    gc.moveTo(point0[0], point0[1]);
                    pathStarted = true;
                    logger.fine("    Path started with moveTo");
                }

                segmentsRendered++;

                // Check if this is a bezier segment
                if (tangentsIn != null && tangentsOut != null &&
                        i < tangentsOut.size() && nextIdx < tangentsIn.size()) {
                    List<Double> prevTangentOut = tangentsOut.get(i);
                    List<Double> currentTangentIn = tangentsIn.get(nextIdx);

                    if (prevTangentOut.size() >= 2 && currentTangentIn.size() >= 2) {
                        // Subdivide the bezier curve for the trimmed portion
                        double[] subdividedCP = subdivideBezierCurve(
                                vertex.get(0), vertex.get(1),
                                vertex.get(0) + prevTangentOut.get(0), vertex.get(1) + prevTangentOut.get(1),
                                nextVertex.get(0) + currentTangentIn.get(0), nextVertex.get(1) + currentTangentIn.get(1),
                                nextVertex.get(0), nextVertex.get(1),
                                t0, t1
                        );
                        gc.bezierCurveTo(subdividedCP[0], subdividedCP[1], subdividedCP[2], subdividedCP[3], point1[0], point1[1]);
                    } else {
                        gc.lineTo(point1[0], point1[1]);
                    }
                } else {
                    gc.lineTo(point1[0], point1[1]);
                }
            }

            currentLength += segmentLength;
        }

        // Apply stroke
        if (!pathStarted) {
            logger.warning("  TrimPath: No path segments found to render (pathStarted=false)");
        } else {
            var strokeColor = strokeStyle.getColor(frame);
            double compensatedWidth = StrokeHelper.getCompensatedStrokeWidth(gc, strokeWidth);

            logger.fine("  Applying stroke: segmentsRendered=" + segmentsRendered +
                    ", color=" + strokeColor + ", width=" + compensatedWidth);

            if (StrokeHelper.shouldRenderStroke(compensatedWidth)) {
                gc.setStroke(strokeColor);
                gc.setLineWidth(compensatedWidth);
                applyStrokeStyle(gc, strokeStyle.stroke);
                gc.stroke();
                logger.fine("  Stroke applied successfully");
            } else {
                logger.warning("  Stroke width too small to render: " + compensatedWidth);
            }
        }

        gc.restore();
    }

    private double calculateSegmentLength(List<List<Double>> vertices,
                                          List<List<Double>> tangentsIn,
                                          List<List<Double>> tangentsOut,
                                          int i, int nextIdx) {
        List<Double> p0 = vertices.get(i);
        List<Double> p3 = vertices.get(nextIdx);

        // Check if we have bezier curves
        if (tangentsIn != null && tangentsOut != null &&
                i < tangentsOut.size() && nextIdx < tangentsIn.size()) {
            List<Double> tangOut = tangentsOut.get(i);
            List<Double> tangIn = tangentsIn.get(nextIdx);

            if (tangOut.size() >= 2 && tangIn.size() >= 2) {
                // Bezier curve - approximate length with sampling
                double x0 = p0.get(0), y0 = p0.get(1);
                double x1 = x0 + tangOut.get(0), y1 = y0 + tangOut.get(1);
                double x2 = p3.get(0) + tangIn.get(0), y2 = p3.get(1) + tangIn.get(1);
                double x3 = p3.get(0), y3 = p3.get(1);

                return approximateBezierLength(x0, y0, x1, y1, x2, y2, x3, y3);
            }
        }

        // Straight line
        double dx = p3.get(0) - p0.get(0);
        double dy = p3.get(1) - p0.get(1);
        return Math.sqrt(dx * dx + dy * dy);
    }

    private double approximateBezierLength(double x0, double y0, double x1, double y1,
                                           double x2, double y2, double x3, double y3) {
        // Sample the curve at multiple points
        int samples = 10;
        double length = 0;
        double prevX = x0, prevY = y0;

        for (int i = 1; i <= samples; i++) {
            double t = i / (double) samples;
            double t2 = t * t;
            double t3 = t2 * t;
            double mt = 1 - t;
            double mt2 = mt * mt;
            double mt3 = mt2 * mt;

            double x = mt3 * x0 + 3 * mt2 * t * x1 + 3 * mt * t2 * x2 + t3 * x3;
            double y = mt3 * y0 + 3 * mt2 * t * y1 + 3 * mt * t2 * y2 + t3 * y3;

            double dx = x - prevX;
            double dy = y - prevY;
            length += Math.sqrt(dx * dx + dy * dy);

            prevX = x;
            prevY = y;
        }

        return length;
    }

    private double[] evaluateBezierSegment(List<List<Double>> vertices,
                                           List<List<Double>> tangentsIn,
                                           List<List<Double>> tangentsOut,
                                           int i, int nextIdx, double t) {
        List<Double> p0 = vertices.get(i);
        List<Double> p3 = vertices.get(nextIdx);

        // Check if we have bezier curves
        if (tangentsIn != null && tangentsOut != null &&
                i < tangentsOut.size() && nextIdx < tangentsIn.size()) {
            List<Double> tangOut = tangentsOut.get(i);
            List<Double> tangIn = tangentsIn.get(nextIdx);

            if (tangOut.size() >= 2 && tangIn.size() >= 2) {
                // Bezier curve
                double x0 = p0.get(0), y0 = p0.get(1);
                double x1 = x0 + tangOut.get(0), y1 = y0 + tangOut.get(1);
                double x2 = p3.get(0) + tangIn.get(0), y2 = p3.get(1) + tangIn.get(1);
                double x3 = p3.get(0), y3 = p3.get(1);

                double t2 = t * t;
                double t3 = t2 * t;
                double mt = 1 - t;
                double mt2 = mt * mt;
                double mt3 = mt2 * mt;

                double x = mt3 * x0 + 3 * mt2 * t * x1 + 3 * mt * t2 * x2 + t3 * x3;
                double y = mt3 * y0 + 3 * mt2 * t * y1 + 3 * mt * t2 * y2 + t3 * y3;

                return new double[]{x, y};
            }
        }

        // Linear interpolation
        double x = p0.get(0) + t * (p3.get(0) - p0.get(0));
        double y = p0.get(1) + t * (p3.get(1) - p0.get(1));
        return new double[]{x, y};
    }

    /**
     * Subdivide a cubic bezier curve from parameter t0 to t1
     * Returns the two new control points [cp1x, cp1y, cp2x, cp2y]
     * Using proper bezier subdivision formula based on derivatives
     */
    private double[] subdivideBezierCurve(double x0, double y0, double x1, double y1,
                                          double x2, double y2, double x3, double y3,
                                          double t0, double t1) {
        // For a cubic bezier B(t) = (1-t)³P0 + 3(1-t)²tP1 + 3(1-t)t²P2 + t³P3
        // Extract the portion from t0 to t1 and reparameterize to [0,1]

        double dt = t1 - t0;

        // Evaluate bezier and its derivative at t0
        double mt0 = 1 - t0;
        double mt0_2 = mt0 * mt0;
        double mt0_3 = mt0_2 * mt0;
        double t0_2 = t0 * t0;
        double t0_3 = t0_2 * t0;

        // Derivative at t0 (tangent direction)
        double dpx = 3 * mt0_2 * (x1 - x0) + 6 * mt0 * t0 * (x2 - x1) + 3 * t0_2 * (x3 - x2);
        double dpy = 3 * mt0_2 * (y1 - y0) + 6 * mt0 * t0 * (y2 - y1) + 3 * t0_2 * (y3 - y2);

        // First control point: start point + (dt/3) * tangent
        double p0x = mt0_3 * x0 + 3 * mt0_2 * t0 * x1 + 3 * mt0 * t0_2 * x2 + t0_3 * x3;
        double p0y = mt0_3 * y0 + 3 * mt0_2 * t0 * y1 + 3 * mt0 * t0_2 * y2 + t0_3 * y3;
        double cp1x = p0x + (dt / 3.0) * dpx;
        double cp1y = p0y + (dt / 3.0) * dpy;

        // Evaluate bezier and derivative at t1
        double mt1 = 1 - t1;
        double mt1_2 = mt1 * mt1;
        double mt1_3 = mt1_2 * mt1;
        double t1_2 = t1 * t1;
        double t1_3 = t1_2 * t1;

        double p3x = mt1_3 * x0 + 3 * mt1_2 * t1 * x1 + 3 * mt1 * t1_2 * x2 + t1_3 * x3;
        double p3y = mt1_3 * y0 + 3 * mt1_2 * t1 * y1 + 3 * mt1 * t1_2 * y2 + t1_3 * y3;

        // Derivative at t1
        double dpx1 = 3 * mt1_2 * (x1 - x0) + 6 * mt1 * t1 * (x2 - x1) + 3 * t1_2 * (x3 - x2);
        double dpy1 = 3 * mt1_2 * (y1 - y0) + 6 * mt1 * t1 * (y2 - y1) + 3 * t1_2 * (y3 - y2);

        // Second control point: end point - (dt/3) * tangent
        double cp2x = p3x - (dt / 3.0) * dpx1;
        double cp2y = p3y - (dt / 3.0) * dpy1;

        return new double[]{cp1x, cp1y, cp2x, cp2y};
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