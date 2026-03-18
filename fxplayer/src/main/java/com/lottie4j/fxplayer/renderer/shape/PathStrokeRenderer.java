package com.lottie4j.fxplayer.renderer.shape;

import com.lottie4j.core.definition.LineCap;
import com.lottie4j.core.definition.LineJoin;
import com.lottie4j.core.model.animation.Animated;
import com.lottie4j.core.model.keyframe.TimedKeyframe;
import com.lottie4j.core.model.shape.BaseShape;
import com.lottie4j.core.model.shape.grouping.Group;
import com.lottie4j.core.model.shape.modifier.TrimPath;
import com.lottie4j.core.model.shape.style.Stroke;
import com.lottie4j.core.model.transform.StrokeDash;
import com.lottie4j.fxplayer.element.StrokeStyle;
import com.lottie4j.fxplayer.util.StrokeHelper;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Renders stroke output for path-based shapes, including Trim Path handling.
 * <p>
 * This class owns stroke-specific behavior (line style, dashes, trim-window extraction,
 * and arc-length based Bezier trim mapping) so {@code PathRenderer} can focus on path/fill.
 */
public class PathStrokeRenderer {

    private static final Logger logger = LoggerFactory.getLogger(PathStrokeRenderer.class);

    /**
     * Creates a new PathStrokeRenderer.
     */
    public PathStrokeRenderer() {
        // Constructor for PathStrokeRenderer
    }

    /**
     * Renders stroke output for path-based shapes, including Trim Path handling.
     * <p>
     * This class owns stroke-specific behavior (line style, dashes, trim-window extraction,
     * and arc-length based Bezier trim mapping) so {@code PathRenderer} can focus on path/fill.
     *
     * @param gc          JavaFX graphics context
     * @param parentGroup parent shape group containing stroke/trim style items
     * @param frame       current animation frame
     * @param pathName    debug name of the path currently rendered
     * @param vertices    path vertices
     * @param tangentsIn  incoming Bezier tangents per vertex
     * @param tangentsOut outgoing Bezier tangents per vertex
     * @param closed      whether the path is closed
     */
    public void renderStroke(GraphicsContext gc,
                             Group parentGroup,
                             double frame,
                             String pathName,
                             List<List<Double>> vertices,
                             List<List<Double>> tangentsIn,
                             List<List<Double>> tangentsOut,
                             Boolean closed) {
        var strokeStyle = getStrokeStyle(parentGroup);
        if (strokeStyle.isEmpty()) {
            logger.debug("  No stroke style found");
            return;
        }

        var strokeWidth = strokeStyle.get().getStrokeWidth(frame);
        if (!StrokeHelper.shouldRenderStroke(strokeWidth)) {
            logger.debug("  Skipping stroke with width: {}", strokeWidth);
            return;
        }

        var allTrimPaths = getAllTrimPaths(parentGroup);
        if (allTrimPaths.isEmpty()) {
            logger.debug("  No TrimPath found in parent group, rendering full stroke");
            var strokeColor = strokeStyle.get().getColor(frame);
            double compensatedWidth = StrokeHelper.getCompensatedStrokeWidth(gc, strokeWidth);
            gc.setStroke(strokeColor);
            gc.setLineWidth(compensatedWidth);
            applyStrokeStyle(gc, strokeStyle.get().stroke(), frame);
            gc.stroke();
            return;
        }

        logger.debug("  Found {} TrimPath(s) in parent group", allTrimPaths.size());
        double combinedStart = 0;
        double combinedEnd = 100;
        double combinedOffsetDegrees = 0;
        List<Animated> animatedSegEnds = new ArrayList<>();

        for (TrimPath trimPath : allTrimPaths) {
            Animated segStart = trimPath.segmentStart();
            Animated segEnd = trimPath.segmentEnd();
            Animated offset = trimPath.offset();

            double trimStart = segStart != null ? getTrimSampledValue(segStart, frame) : 0;
            double trimEnd = segEnd != null ? getTrimSampledValue(segEnd, frame) : 100;

            logger.debug("  Frame {}: trimStart={}, trimEnd={}", frame, trimStart, trimEnd);
            double trimOffsetDegrees = offset != null ? offset.getValue(0, frame) : 0;

            if (segEnd != null && segEnd.animated() != null && segEnd.animated() > 0) {
                animatedSegEnds.add(segEnd);
            }

            combinedStart = Math.max(combinedStart, trimStart);
            combinedEnd = Math.min(combinedEnd, trimEnd);
            combinedOffsetDegrees = trimOffsetDegrees;
        }

        double visibleWindow = combinedEnd - combinedStart;
        double offsetPercent = degreesToTrimPercent(combinedOffsetDegrees);
        boolean isClosedPath = Boolean.TRUE.equals(closed);
        double offsetAdjustedStart;
        double offsetAdjustedEnd;

        if (isClosedPath) {
            offsetAdjustedStart = normalizeTrimPercent(combinedStart + offsetPercent);
            offsetAdjustedEnd = offsetAdjustedStart + visibleWindow;
        } else {
            // For open paths, preserve endpoints like 100 exactly; modulo normalization breaks TrimPath.
            offsetAdjustedStart = clampTrimPercent(combinedStart + offsetPercent);
            offsetAdjustedEnd = clampTrimPercent(combinedEnd + offsetPercent);
        }

        if (Double.isNaN(offsetAdjustedStart) || Double.isNaN(offsetAdjustedEnd)) {
            logger.warn("  Combined TrimPath has NaN value at frame {} - skipping rendering", frame);
            return;
        }

        if (combinedEnd == 100 && combinedStart == 0 && !animatedSegEnds.isEmpty() && isBeforeAnyKeyframe(animatedSegEnds, frame)) {
            logger.warn("SKIP_BEFORE_ANIM: FRAME {}: Path '{}' - before first animated keyframe", frame, pathName);
            return;
        }

        if (Math.abs(visibleWindow) < 0.01) {
            logger.debug("EMPTY_PATH FRAME {}: visibleWindow={} - NOT RENDERING", frame, visibleWindow);
            return;
        }

        // When visible window is 100%, we can use the existing path geometry for stroke.
        // However, for open paths, we should use renderTrimmedPath to ensure consistent rendering
        // regardless of trim percentage (avoids visual discontinuity at 100%).
        if (visibleWindow >= 100 && isClosedPath) {
            var strokeColor = strokeStyle.get().getColor(frame);
            double compensatedWidth = StrokeHelper.getCompensatedStrokeWidth(gc, strokeWidth);
            gc.setStroke(strokeColor);
            gc.setLineWidth(compensatedWidth);
            applyStrokeStyle(gc, strokeStyle.get().stroke(), frame);
            gc.stroke();
            return;
        }

        renderTrimmedPath(gc, vertices, tangentsIn, tangentsOut, closed,
                offsetAdjustedStart, offsetAdjustedEnd, strokeStyle.get(), strokeWidth, frame, combinedOffsetDegrees);
    }

    /**
     * Extracts stroke style from the parent group.
     *
     * @param group the parent group to search for stroke style
     * @return optional containing the stroke style if found, empty otherwise
     */
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

    /**
     * Collects all Trim Path modifiers from the parent group.
     *
     * @param group the parent group to search for trim paths
     * @return list of trim paths found in the group, empty list if none found
     */
    private List<TrimPath> getAllTrimPaths(Group group) {
        List<TrimPath> trimPaths = new ArrayList<>();
        if (group == null) {
            return trimPaths;
        }
        for (BaseShape baseShape : group.shapes()) {
            if (baseShape instanceof TrimPath trimPath) {
                trimPaths.add(trimPath);
            }
        }
        return trimPaths;
    }

    /**
     * Returns true if the current frame is before the first timed keyframe.
     *
     * @param animated the animated property to check
     * @param frame    the current frame number
     * @return true if the frame is before the first keyframe, false otherwise
     */
    private boolean isBeforeFirstKeyframe(Animated animated, double frame) {
        if (animated == null || animated.keyframes() == null || animated.keyframes().isEmpty()) {
            return false;
        }
        for (Object keyframeObj : animated.keyframes()) {
            if (keyframeObj instanceof TimedKeyframe timedKeyframe
                    && timedKeyframe.time() != null
                    && timedKeyframe.time() > frame) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns true if any animated value starts after the current frame.
     *
     * @param animatedList list of animated properties to check
     * @param frame        the current frame number
     * @return true if any animation starts after the frame, false otherwise
     */
    private boolean isBeforeAnyKeyframe(List<Animated> animatedList, double frame) {
        for (Animated animated : animatedList) {
            if (isBeforeFirstKeyframe(animated, frame)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Gets trim value with NaN handling at keyframe boundaries.
     * <p>
     * When getValue returns NaN at exact keyframe time, samples just before the keyframe.
     * If the sampled value is very close to 100 (end of animation), returns 0 instead
     * to create an empty window (shape not visible). This prevents shapes from appearing
     * when they shouldn't at keyframe boundaries.
     *
     * @param animated the animated property to sample
     * @param frame    the current frame number
     * @return the sampled trim value, with NaN handling applied
     */
    private double getTrimSampledValue(Animated animated, double frame) {
        if (animated == null) {
            return 0.0;
        }

        double exact = animated.getValue(0, frame);

        // Handle NaN at exact keyframe boundaries
        // NaN typically indicates a keyframe boundary where the animation transitions
        if (Double.isNaN(exact) && isAtAnimatedKeyframeTime(animated, frame)) {
            double sampledFrame = Math.max(0.0, frame - 1.0e-3);
            double sampled = animated.getValue(0, sampledFrame);

            if (!Double.isNaN(sampled)) {
                // If sampled value is very close to 100 (end of animation),
                // return 0 instead to create an empty window (shape not visible)
                // This prevents shapes from appearing when they shouldn't at keyframe boundaries
                if (sampled >= 99.0) {
                    return 0.0;
                }
                return sampled;
            }
        }

        return exact;
    }

    /**
     * Checks if the current frame is at an animated keyframe time.
     * <p>
     * Uses a tolerance of 0.01 frames to account for floating-point precision issues.
     *
     * @param animated the animated property to check
     * @param frame    the current frame number
     * @return true if the frame is at a keyframe time, false otherwise
     */
    private boolean isAtAnimatedKeyframeTime(Animated animated, double frame) {
        if (animated == null || animated.keyframes() == null || animated.keyframes().isEmpty()) {
            return false;
        }
        for (Object keyframeObj : animated.keyframes()) {
            if (keyframeObj instanceof TimedKeyframe timedKeyframe && timedKeyframe.time() != null) {
                if (Math.abs(frame - timedKeyframe.time()) < 1.0e-2) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Renders only the currently visible trim window for the path stroke.
     * <p>
     * This method calculates arc-length based trimming for precise segment extraction,
     * handles both open and closed paths, and supports wrapped rendering for closed paths.
     *
     * @param gc                    the graphics context to render to
     * @param vertices              path vertices
     * @param tangentsIn            incoming Bezier tangents per vertex
     * @param tangentsOut           outgoing Bezier tangents per vertex
     * @param closed                whether the path is closed
     * @param trimStart             trim start percentage (0-100)
     * @param trimEnd               trim end percentage (0-100)
     * @param strokeStyle           stroke style configuration
     * @param strokeWidth           stroke width
     * @param frame                 current animation frame
     * @param animatedOffsetDegrees trim offset in degrees
     */
    private void renderTrimmedPath(GraphicsContext gc,
                                   List<List<Double>> vertices,
                                   List<List<Double>> tangentsIn,
                                   List<List<Double>> tangentsOut,
                                   Boolean closed,
                                   double trimStart,
                                   double trimEnd,
                                   StrokeStyle strokeStyle,
                                   double strokeWidth,
                                   double frame,
                                   double animatedOffsetDegrees) {
        if (vertices.isEmpty()) return;

        List<Double> segmentLengths = new ArrayList<>();
        double totalLength = 0;

        logger.debug("  Calculating segment lengths for {} vertices, closed={}", vertices.size(), closed);
        for (int i = 0; i < vertices.size(); i++) {
            int nextIdx = (i + 1) % vertices.size();
            if (!Boolean.TRUE.equals(closed) && nextIdx == 0) break;
            double segmentLength = calculateSegmentLength(vertices, tangentsIn, tangentsOut, i, nextIdx);
            segmentLengths.add(segmentLength);
            totalLength += segmentLength;
            List<Double> v1 = vertices.get(i);
            List<Double> v2 = vertices.get(nextIdx);
            logger.debug("    Segment {}->{}: length={}, v[{}]=({},{}), v[{}]=({},{})",
                    i, nextIdx, segmentLength,
                    i, v1.get(0), v1.get(1),
                    nextIdx, v2.get(0), v2.get(1));
        }

        logger.debug("  Total path length: {}", totalLength);
        if (totalLength == 0) return;

        double startLength = (trimStart / 100.0) * totalLength;
        double endLength = (trimEnd / 100.0) * totalLength;

        logger.debug("  Trim: start={}% ({}), end={}% ({})", trimStart, startLength, trimEnd, endLength);

        // Lottie open paths expect a direct segment between start/end values.
        // Normalize reversed ranges (e.g. start=100, end=0) to avoid rendering nothing.
        if (!Boolean.TRUE.equals(closed) && startLength > endLength) {
            logger.debug("  Swapping start/end for open path with reversed range");
            double tmp = startLength;
            startLength = endLength;
            endLength = tmp;
        }

        boolean wrapped = Boolean.TRUE.equals(closed) && endLength > totalLength;

        gc.save();
        gc.beginPath();

        double currentLength = 0;
        boolean pathStarted = false;

        int segmentCount = segmentLengths.size();
        int iterationCount = wrapped ? segmentCount * 2 : segmentCount;

        for (int idx = 0; idx < iterationCount; idx++) {
            int i = idx % segmentCount;
            double segmentLength = segmentLengths.get(i);
            double segmentStart = currentLength;
            double segmentEnd = currentLength + segmentLength;

            if (segmentStart >= endLength) {
                break;
            }

            int nextIdx = (i + 1) % vertices.size();
            List<Double> vertex = vertices.get(i);
            List<Double> nextVertex = vertices.get(nextIdx);

            if (segmentEnd > startLength && segmentStart < endLength) {
                double localStart = Math.max(0, startLength - segmentStart);
                double localEnd = Math.min(segmentLength, endLength - segmentStart);

                double t0 = getSegmentTForArcLength(vertices, tangentsIn, tangentsOut, i, nextIdx, localStart, segmentLength);
                double t1 = getSegmentTForArcLength(vertices, tangentsIn, tangentsOut, i, nextIdx, localEnd, segmentLength);

                double[] point0 = evaluateBezierSegment(vertices, tangentsIn, tangentsOut, i, nextIdx, t0);
                double[] point1 = evaluateBezierSegment(vertices, tangentsIn, tangentsOut, i, nextIdx, t1);

                if (!pathStarted) {
                    gc.moveTo(point0[0], point0[1]);
                    pathStarted = true;
                }

                if (tangentsIn != null && tangentsOut != null
                        && i < tangentsOut.size() && nextIdx < tangentsIn.size()) {
                    List<Double> prevTangentOut = tangentsOut.get(i);
                    List<Double> currentTangentIn = tangentsIn.get(nextIdx);

                    if (prevTangentOut.size() >= 2 && currentTangentIn.size() >= 2) {
                        // Check if this is a straight line (zero tangents)
                        boolean isZeroTangent = (Math.abs(prevTangentOut.get(0)) < 0.001 && Math.abs(prevTangentOut.get(1)) < 0.001
                                && Math.abs(currentTangentIn.get(0)) < 0.001 && Math.abs(currentTangentIn.get(1)) < 0.001);

                        if (isZeroTangent) {
                            // For straight lines, just use lineTo
                            gc.lineTo(point1[0], point1[1]);
                        } else {
                            double[] subdividedCP = subdivideBezierCurve(
                                    vertex.get(0), vertex.get(1),
                                    vertex.get(0) + prevTangentOut.get(0), vertex.get(1) + prevTangentOut.get(1),
                                    nextVertex.get(0) + currentTangentIn.get(0), nextVertex.get(1) + currentTangentIn.get(1),
                                    nextVertex.get(0), nextVertex.get(1),
                                    t0, t1
                            );
                            gc.bezierCurveTo(subdividedCP[0], subdividedCP[1], subdividedCP[2], subdividedCP[3], point1[0], point1[1]);
                        }
                    } else {
                        gc.lineTo(point1[0], point1[1]);
                    }
                } else {
                    gc.lineTo(point1[0], point1[1]);
                }
            }

            currentLength += segmentLength;
        }

        if (pathStarted) {
            var strokeColor = strokeStyle.getColor(frame);
            double compensatedWidth = StrokeHelper.getCompensatedStrokeWidth(gc, strokeWidth);
            if (StrokeHelper.shouldRenderStroke(compensatedWidth)) {
                gc.setStroke(strokeColor);
                gc.setLineWidth(compensatedWidth);
                applyStrokeStyleWithDashOffset(gc, strokeStyle.stroke(), frame, animatedOffsetDegrees);
                gc.stroke();
            }
        }

        gc.restore();
    }

    /**
     * Calculates one segment length using line distance or Bezier length approximation.
     * <p>
     * For segments with Bezier tangents, uses arc-length approximation.
     * For straight segments, uses Euclidean distance.
     *
     * @param vertices    path vertices
     * @param tangentsIn  incoming Bezier tangents per vertex
     * @param tangentsOut outgoing Bezier tangents per vertex
     * @param i           current vertex index
     * @param nextIdx     next vertex index
     * @return the arc length of the segment
     */
    private double calculateSegmentLength(List<List<Double>> vertices,
                                          List<List<Double>> tangentsIn,
                                          List<List<Double>> tangentsOut,
                                          int i,
                                          int nextIdx) {
        List<Double> p0 = vertices.get(i);
        List<Double> p3 = vertices.get(nextIdx);

        if (tangentsIn != null && tangentsOut != null && i < tangentsOut.size() && nextIdx < tangentsIn.size()) {
            List<Double> tangOut = tangentsOut.get(i);
            List<Double> tangIn = tangentsIn.get(nextIdx);
            if (tangOut.size() >= 2 && tangIn.size() >= 2) {
                double x0 = p0.get(0);
                double y0 = p0.get(1);
                double x1 = x0 + tangOut.get(0);
                double y1 = y0 + tangOut.get(1);
                double x2 = p3.get(0) + tangIn.get(0);
                double y2 = p3.get(1) + tangIn.get(1);
                double x3 = p3.get(0);
                double y3 = p3.get(1);
                return approximateBezierLength(x0, y0, x1, y1, x2, y2, x3, y3);
            }
        }

        double dx = p3.get(0) - p0.get(0);
        double dy = p3.get(1) - p0.get(1);
        return Math.sqrt(dx * dx + dy * dy);
    }

    /**
     * Approximates cubic Bezier length by fixed-point sampling.
     * <p>
     * Uses 10 sample points along the curve to calculate the arc length.
     *
     * @param x0 start point x coordinate
     * @param y0 start point y coordinate
     * @param x1 first control point x coordinate
     * @param y1 first control point y coordinate
     * @param x2 second control point x coordinate
     * @param y2 second control point y coordinate
     * @param x3 end point x coordinate
     * @param y3 end point y coordinate
     * @return approximated arc length of the Bezier curve
     */
    private double approximateBezierLength(double x0, double y0, double x1, double y1,
                                           double x2, double y2, double x3, double y3) {
        int samples = 10;
        double length = 0;
        double prevX = x0;
        double prevY = y0;

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

    /**
     * Evaluates point coordinates on the segment at parameter {@code t}.
     * <p>
     * For Bezier segments, uses cubic Bezier evaluation.
     * For straight segments, uses linear interpolation.
     *
     * @param vertices    path vertices
     * @param tangentsIn  incoming Bezier tangents per vertex
     * @param tangentsOut outgoing Bezier tangents per vertex
     * @param i           current vertex index
     * @param nextIdx     next vertex index
     * @param t           parameter value (0-1) along the segment
     * @return array containing [x, y] coordinates at parameter t
     */
    private double[] evaluateBezierSegment(List<List<Double>> vertices,
                                           List<List<Double>> tangentsIn,
                                           List<List<Double>> tangentsOut,
                                           int i,
                                           int nextIdx,
                                           double t) {
        List<Double> p0 = vertices.get(i);
        List<Double> p3 = vertices.get(nextIdx);

        if (tangentsIn != null && tangentsOut != null && i < tangentsOut.size() && nextIdx < tangentsIn.size()) {
            List<Double> tangOut = tangentsOut.get(i);
            List<Double> tangIn = tangentsIn.get(nextIdx);
            if (tangOut.size() >= 2 && tangIn.size() >= 2) {
                double x0 = p0.get(0);
                double y0 = p0.get(1);
                double x1 = x0 + tangOut.get(0);
                double y1 = y0 + tangOut.get(1);
                double x2 = p3.get(0) + tangIn.get(0);
                double y2 = p3.get(1) + tangIn.get(1);
                double x3 = p3.get(0);
                double y3 = p3.get(1);

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

        double x = p0.get(0) + t * (p3.get(0) - p0.get(0));
        double y = p0.get(1) + t * (p3.get(1) - p0.get(1));
        return new double[]{x, y};
    }

    /**
     * Returns control points for the cubic subsection from {@code t0} to {@code t1}.
     * <p>
     * Subdivides a cubic Bezier curve defined by control points (x0, y0) to (x3, y3)
     * with control points (x1, y1) and (x2, y2), extracting the subsection from
     * parameter t0 to t1.
     *
     * @param x0 start point x coordinate
     * @param y0 start point y coordinate
     * @param x1 first control point x coordinate
     * @param y1 first control point y coordinate
     * @param x2 second control point x coordinate
     * @param y2 second control point y coordinate
     * @param x3 end point x coordinate
     * @param y3 end point y coordinate
     * @param t0 start parameter value (0-1)
     * @param t1 end parameter value (0-1), must be greater than t0
     * @return array of 4 elements [cp1x, cp1y, cp2x, cp2y] representing the two control points
     * of the subdivided curve
     */
    private double[] subdivideBezierCurve(double x0, double y0, double x1, double y1,
                                          double x2, double y2, double x3, double y3,
                                          double t0, double t1) {
        double dt = t1 - t0;

        double mt0 = 1 - t0;
        double mt02 = mt0 * mt0;
        double mt03 = mt02 * mt0;
        double t02 = t0 * t0;
        double t03 = t02 * t0;

        double dpx = 3 * mt02 * (x1 - x0) + 6 * mt0 * t0 * (x2 - x1) + 3 * t02 * (x3 - x2);
        double dpy = 3 * mt02 * (y1 - y0) + 6 * mt0 * t0 * (y2 - y1) + 3 * t02 * (y3 - y2);

        double p0x = mt03 * x0 + 3 * mt02 * t0 * x1 + 3 * mt0 * t02 * x2 + t03 * x3;
        double p0y = mt03 * y0 + 3 * mt02 * t0 * y1 + 3 * mt0 * t02 * y2 + t03 * y3;
        double cp1x = p0x + (dt / 3.0) * dpx;
        double cp1y = p0y + (dt / 3.0) * dpy;

        double mt1 = 1 - t1;
        double mt12 = mt1 * mt1;
        double mt13 = mt12 * mt1;
        double t12 = t1 * t1;
        double t13 = t12 * t1;

        double p3x = mt13 * x0 + 3 * mt12 * t1 * x1 + 3 * mt1 * t12 * x2 + t13 * x3;
        double p3y = mt13 * y0 + 3 * mt12 * t1 * y1 + 3 * mt1 * t12 * y2 + t13 * y3;

        double dpx1 = 3 * mt12 * (x1 - x0) + 6 * mt1 * t1 * (x2 - x1) + 3 * t12 * (x3 - x2);
        double dpy1 = 3 * mt12 * (y1 - y0) + 6 * mt1 * t1 * (y2 - y1) + 3 * t12 * (y3 - y2);

        double cp2x = p3x - (dt / 3.0) * dpx1;
        double cp2y = p3y - (dt / 3.0) * dpy1;

        return new double[]{cp1x, cp1y, cp2x, cp2y};
    }

    /**
     * Applies cap/join/miter and dash style to the graphics context.
     * <p>
     * Delegates to applyStrokeStyleWithDashOffset with zero offset.
     *
     * @param gc     the graphics context to apply stroke style to
     * @param stroke the stroke style configuration to apply
     * @param frame  the current animation frame
     */
    private void applyStrokeStyle(GraphicsContext gc, Stroke stroke, double frame) {
        applyStrokeStyleWithDashOffset(gc, stroke, frame, 0.0);
    }

    /**
     * Applies cap/join/miter and dash style with optional trim offset context.
     * <p>
     * Configures the graphics context with line cap, line join, miter limit, and stroke dash pattern.
     * The trim offset is passed to the dash configuration for potential animated dash offset effects.
     *
     * @param gc                the graphics context to apply stroke style to
     * @param stroke            the stroke style configuration to apply
     * @param frame             the current animation frame
     * @param trimOffsetDegrees the trim offset in degrees (used for dash offset calculation)
     */
    private void applyStrokeStyleWithDashOffset(GraphicsContext gc,
                                                Stroke stroke,
                                                double frame,
                                                double trimOffsetDegrees) {
        if (stroke.lineCap() != null) {
            switch (stroke.lineCap()) {
                case LineCap.ROUND -> gc.setLineCap(StrokeLineCap.ROUND);
                case LineCap.SQUARE -> gc.setLineCap(StrokeLineCap.SQUARE);
                default -> gc.setLineCap(StrokeLineCap.BUTT);
            }
        }

        if (stroke.lineJoin() != null) {
            switch (stroke.lineJoin()) {
                case LineJoin.ROUND -> gc.setLineJoin(StrokeLineJoin.ROUND);
                case LineJoin.BEVEL -> gc.setLineJoin(StrokeLineJoin.BEVEL);
                default -> gc.setLineJoin(StrokeLineJoin.MITER);
            }
        }

        if (stroke.miterLimit() != null) {
            gc.setMiterLimit(stroke.miterLimit());
        }

        if (stroke.strokeDashes() != null && !stroke.strokeDashes().isEmpty()) {
            applyStrokeDashesWithAnimatedOffset(gc, stroke.strokeDashes(), frame);
        }
    }

    /**
     * Applies stroke dash pattern and offset from Lottie stroke dash entries.
     * <p>
     * Extracts dash, gap, and offset entries from the stroke dashes list and applies them
     * to the graphics context. The resulting dash pattern is applied to the current stroke.
     *
     * @param gc           the graphics context to apply stroke dashes to
     * @param strokeDashes list of stroke dash definitions (dash, gap, offset entries)
     * @param frame        the current animation frame
     */
    private void applyStrokeDashesWithAnimatedOffset(GraphicsContext gc,
                                                     List<StrokeDash> strokeDashes,
                                                     double frame) {
        List<Double> dashArray = new ArrayList<>();
        double staticOffset = 0;

        for (StrokeDash dash : strokeDashes) {
            if (dash.type() == com.lottie4j.core.definition.StrokeDashType.DASH) {
                double dashLength = dash.length() != null ? dash.length().getValue(0, frame) : 0;
                dashArray.add(dashLength);
            } else if (dash.type() == com.lottie4j.core.definition.StrokeDashType.GAP) {
                double gapLength = dash.length() != null ? dash.length().getValue(0, frame) : 0;
                dashArray.add(gapLength);
            } else if (dash.type() == com.lottie4j.core.definition.StrokeDashType.OFFSET) {
                double dashOffset = dash.length() != null ? dash.length().getValue(0, frame) : 0;
                staticOffset = dashOffset;
            }
        }

        if (!dashArray.isEmpty()) {
            double[] dashes = new double[dashArray.size()];
            for (int i = 0; i < dashArray.size(); i++) {
                dashes[i] = dashArray.get(i);
            }
            gc.setLineDashes(dashes);
            gc.setLineDashOffset(staticOffset);
        }
    }

    /**
     * Normalizes trim percentage into [0, 100).
     * <p>
     * Uses modulo operation to wrap values larger than 100 or smaller than 0
     * into the valid trim percentage range. For negative values, adds 100 to ensure
     * proper wrapping behavior.
     *
     * @param value the trim percentage value to normalize
     * @return the normalized value in the range [0, 100)
     */
    private double normalizeTrimPercent(double value) {
        double normalized = value % 100.0;
        return normalized < 0 ? normalized + 100.0 : normalized;
    }

    /**
     * Clamps trim percentage into [0, 100].
     * <p>
     * Restricts the given value to the valid trim percentage range without wrapping.
     * Values below 0 are clamped to 0, values above 100 are clamped to 100.
     *
     * @param value the trim percentage value to clamp
     * @return the clamped value in the range [0, 100]
     */
    private double clampTrimPercent(double value) {
        if (value < 0.0) {
            return 0.0;
        }
        if (value > 100.0) {
            return 100.0;
        }
        return value;
    }

    /**
     * Converts Lottie trim offset in degrees to percentage units.
     * <p>
     * Translates a degree-based offset (0-360) to a trim percentage (0-100).
     * This is used when applying rotational offsets to trim path animations.
     *
     * @param degrees the trim offset in degrees (0-360)
     * @return the equivalent trim offset as a percentage (0-100)
     */
    private double degreesToTrimPercent(double degrees) {
        return (degrees / 360.0) * 100.0;
    }

    /**
     * Checks whether a segment has valid Bezier tangent data.
     * <p>
     * Verifies that both tangent arrays exist and contain valid tangent vectors
     * for the specified segment indices.
     *
     * @param tangentsIn  incoming Bezier tangents per vertex, may be null
     * @param tangentsOut outgoing Bezier tangents per vertex, may be null
     * @param i           current vertex index
     * @param nextIdx     next vertex index
     * @return true if the segment has valid Bezier tangent data, false otherwise
     */
    private boolean hasBezierSegmentForTrim(List<List<Double>> tangentsIn,
                                            List<List<Double>> tangentsOut,
                                            int i,
                                            int nextIdx) {
        if (tangentsIn == null || tangentsOut == null) {
            return false;
        }
        if (i >= tangentsOut.size() || nextIdx >= tangentsIn.size()) {
            return false;
        }
        List<Double> tangOut = tangentsOut.get(i);
        List<Double> tangIn = tangentsIn.get(nextIdx);
        return tangOut.size() >= 2 && tangIn.size() >= 2;
    }

    /**
     * Maps local arc length on a segment to Bezier parameter t.
     * <p>
     * Converts a distance along the segment arc length to the corresponding parameter
     * value (0-1) for Bezier curve evaluation. Handles both straight line segments
     * and cubic Bezier curves.
     *
     * @param vertices      path vertices
     * @param tangentsIn    incoming Bezier tangents per vertex
     * @param tangentsOut   outgoing Bezier tangents per vertex
     * @param i             current vertex index
     * @param nextIdx       next vertex index
     * @param targetLength  desired arc length along the segment
     * @param segmentLength total arc length of the segment
     * @return parameter value t (0-1) corresponding to the target arc length
     */
    private double getSegmentTForArcLength(List<List<Double>> vertices,
                                           List<List<Double>> tangentsIn,
                                           List<List<Double>> tangentsOut,
                                           int i,
                                           int nextIdx,
                                           double targetLength,
                                           double segmentLength) {
        if (segmentLength <= 0) {
            return 0.0;
        }

        double clampedTarget = Math.max(0, Math.min(segmentLength, targetLength));
        if (!hasBezierSegmentForTrim(tangentsIn, tangentsOut, i, nextIdx)) {
            return clampedTarget / segmentLength;
        }

        List<Double> p0 = vertices.get(i);
        List<Double> p3 = vertices.get(nextIdx);
        List<Double> tangOut = tangentsOut.get(i);
        List<Double> tangIn = tangentsIn.get(nextIdx);

        double x0 = p0.get(0);
        double y0 = p0.get(1);
        double x1 = x0 + tangOut.get(0);
        double y1 = y0 + tangOut.get(1);
        double x2 = p3.get(0) + tangIn.get(0);
        double y2 = p3.get(1) + tangIn.get(1);
        double x3 = p3.get(0);
        double y3 = p3.get(1);

        return mapBezierArcLengthToT(x0, y0, x1, y1, x2, y2, x3, y3, clampedTarget, segmentLength);
    }

    /**
     * Approximates inverse arc-length lookup for a cubic Bezier via sampled cumulative lengths.
     * <p>
     * Uses 96 sample points along the Bezier curve to build a cumulative arc-length table,
     * then performs linear interpolation to find the parameter t corresponding to the target arc length.
     *
     * @param x0           start point x coordinate
     * @param y0           start point y coordinate
     * @param x1           first control point x coordinate
     * @param y1           first control point y coordinate
     * @param x2           second control point x coordinate
     * @param y2           second control point y coordinate
     * @param x3           end point x coordinate
     * @param y3           end point y coordinate
     * @param targetLength desired arc length along the curve
     * @param totalLength  total arc length of the curve
     * @return parameter value t (0-1) corresponding to the target arc length, clamped to [0, 1]
     */
    private double mapBezierArcLengthToT(double x0, double y0, double x1, double y1,
                                         double x2, double y2, double x3, double y3,
                                         double targetLength, double totalLength) {
        if (targetLength <= 0) {
            return 0.0;
        }
        if (targetLength >= totalLength) {
            return 1.0;
        }

        int samples = 96;
        double[] ts = new double[samples + 1];
        double[] lengths = new double[samples + 1];

        ts[0] = 0.0;
        lengths[0] = 0.0;
        double prevX = x0;
        double prevY = y0;
        double cumulative = 0.0;

        for (int s = 1; s <= samples; s++) {
            double t = s / (double) samples;
            ts[s] = t;

            double mt = 1 - t;
            double mt2 = mt * mt;
            double mt3 = mt2 * mt;
            double t2 = t * t;
            double t3 = t2 * t;

            double x = mt3 * x0 + 3 * mt2 * t * x1 + 3 * mt * t2 * x2 + t3 * x3;
            double y = mt3 * y0 + 3 * mt2 * t * y1 + 3 * mt * t2 * y2 + t3 * y3;

            double dx = x - prevX;
            double dy = y - prevY;
            cumulative += Math.sqrt(dx * dx + dy * dy);
            lengths[s] = cumulative;

            prevX = x;
            prevY = y;
        }

        for (int s = 1; s <= samples; s++) {
            if (lengths[s] >= targetLength) {
                double l0 = lengths[s - 1];
                double l1 = lengths[s];
                double t0 = ts[s - 1];
                double t1 = ts[s];
                if (Math.abs(l1 - l0) < 1e-9) {
                    return t1;
                }
                double alpha = (targetLength - l0) / (l1 - l0);
                return t0 + (t1 - t0) * alpha;
            }
        }

        return 1.0;
    }
}

