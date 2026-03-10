package com.lottie4j.fxplayer.renderer.shape;

import com.lottie4j.core.model.EasingHandle;
import com.lottie4j.core.model.bezier.AnimatedBezier;
import com.lottie4j.core.model.bezier.BezierDefinition;

import java.util.ArrayList;
import java.util.List;

/**
 * Interpolates animated path Bezier data for a specific frame.
 */
public class PathBezierInterpolator {

    /**
     * Creates a new PathBezierInterpolator.
     */
    public PathBezierInterpolator() {
    }

    /**
     * Interpolates the animated Bezier between surrounding keyframes.
     *
     * @param animatedBezier animated path data
     * @param frame          current frame
     * @return interpolated bezier definition, or null when unavailable
     */
    public BezierDefinition getInterpolatedBezier(AnimatedBezier animatedBezier, double frame) {
        if (animatedBezier.beziers() == null || animatedBezier.beziers().isEmpty()) {
            return null;
        }

        var keyframes = animatedBezier.beziers();
        var prevKeyframe = keyframes.get(0);
        var nextKeyframe = keyframes.size() > 1 ? keyframes.get(1) : null;

        for (int i = 0; i < keyframes.size(); i++) {
            var keyframe = keyframes.get(i);
            if (keyframe.time() <= frame) {
                prevKeyframe = keyframe;
                nextKeyframe = (i + 1 < keyframes.size()) ? keyframes.get(i + 1) : null;
            } else {
                break;
            }
        }

        if (nextKeyframe == null || prevKeyframe.beziers() == null || prevKeyframe.beziers().isEmpty()) {
            return (prevKeyframe.beziers() != null && !prevKeyframe.beziers().isEmpty())
                    ? prevKeyframe.beziers().get(0)
                    : null;
        }

        if (nextKeyframe.beziers() == null || nextKeyframe.beziers().isEmpty()) {
            return prevKeyframe.beziers().get(0);
        }

        double startFrame = prevKeyframe.time();
        double endFrame = nextKeyframe.time();
        double progress = (frame - startFrame) / (endFrame - startFrame);
        progress = Math.max(0, Math.min(1, progress));

        if (prevKeyframe.easingOut() != null && prevKeyframe.easingIn() != null) {
            progress = applyBezierEasing(progress, prevKeyframe.easingOut(), prevKeyframe.easingIn());
        }

        BezierDefinition prevBezier = prevKeyframe.beziers().get(0);
        BezierDefinition nextBezier = nextKeyframe.beziers().get(0);

        List<List<Double>> interpolatedVertices = interpolateVertices(prevBezier.vertices(), nextBezier.vertices(), progress);
        List<List<Double>> interpolatedTangentsIn = interpolateVertices(prevBezier.tangentsIn(), nextBezier.tangentsIn(), progress);
        List<List<Double>> interpolatedTangentsOut = interpolateVertices(prevBezier.tangentsOut(), nextBezier.tangentsOut(), progress);

        return new BezierDefinition(
                prevBezier.closed(),
                interpolatedVertices,
                interpolatedTangentsIn,
                interpolatedTangentsOut
        );
    }

    /**
     * Linearly interpolates a list of 2D vertices/tangent vectors.
     *
     * @param start    start vertices
     * @param end      end vertices
     * @param progress interpolation factor in [0, 1]
     * @return interpolated vertices
     */
    private List<List<Double>> interpolateVertices(List<List<Double>> start, List<List<Double>> end, double progress) {
        if (start == null || end == null || start.isEmpty()) {
            return start;
        }

        List<List<Double>> result = new ArrayList<>();
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

    /**
     * Applies cubic Bezier easing from Lottie keyframe handles.
     * Uses Newton-Raphson on the x curve, then samples the y curve.
     *
     * @param t         input progress in [0, 1]
     * @param easingOut outgoing easing handle
     * @param easingIn  incoming easing handle
     * @return eased progress in [0, 1]
     */
    private double applyBezierEasing(double t, EasingHandle easingOut, EasingHandle easingIn) {
        double x1 = easingOut.x() != null && !easingOut.x().isEmpty() ? easingOut.x().get(0) : 0.0;
        double y1 = easingOut.y() != null && !easingOut.y().isEmpty() ? easingOut.y().get(0) : 0.0;
        double x2 = easingIn.x() != null && !easingIn.x().isEmpty() ? easingIn.x().get(0) : 1.0;
        double y2 = easingIn.y() != null && !easingIn.y().isEmpty() ? easingIn.y().get(0) : 1.0;

        double currentT = t;
        for (int i = 0; i < 8; i++) {
            double currentX = cubicBezier(currentT, 0, x1, x2, 1);
            double dx = currentX - t;
            if (Math.abs(dx) < 0.001) {
                break;
            }

            double derivative = cubicBezierDerivative(currentT, 0, x1, x2, 1);
            if (Math.abs(derivative) < 1e-6) {
                break;
            }

            currentT = currentT - dx / derivative;
        }

        return cubicBezier(currentT, 0, y1, y2, 1);
    }

    /**
     * Evaluates a cubic Bezier curve at parameter {@code t}.
     *
     * @param t  parameter value in [0, 1]
     * @param p0 first control point
     * @param p1 second control point
     * @param p2 third control point
     * @param p3 fourth control point
     * @return evaluated curve value at parameter t
     */
    private double cubicBezier(double t, double p0, double p1, double p2, double p3) {
        double t2 = t * t;
        double t3 = t2 * t;
        double mt = 1 - t;
        double mt2 = mt * mt;
        double mt3 = mt2 * mt;
        return mt3 * p0 + 3 * mt2 * t * p1 + 3 * mt * t2 * p2 + t3 * p3;
    }

    /**
     * Evaluates the derivative of a cubic Bezier curve at parameter {@code t}.
     *
     * @param t  parameter value in [0, 1]
     * @param p0 first control point
     * @param p1 second control point
     * @param p2 third control point
     * @param p3 fourth control point
     * @return derivative value at parameter t
     */
    private double cubicBezierDerivative(double t, double p0, double p1, double p2, double p3) {
        double t2 = t * t;
        double mt = 1 - t;
        double mt2 = mt * mt;
        return 3 * mt2 * (p1 - p0) + 6 * mt * t * (p2 - p1) + 3 * t2 * (p3 - p2);
    }
}

