package com.lottie4j.fxplayer.renderer.shape;

import java.util.ArrayList;
import java.util.List;

import com.lottie4j.core.helper.BezierEasing;
import com.lottie4j.core.model.animation.EasingHandle;
import com.lottie4j.core.model.bezier.AnimatedBezier;
import com.lottie4j.core.model.bezier.BezierDefinition;

/**
 * Interpolates animated path Bezier data for a specific frame.
 */
public class PathBezierInterpolator {

    /**
     * Creates a new PathBezierInterpolator.
     */
    public PathBezierInterpolator() {
        // Constructor for PathBezierInterpolator
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
     * Applies cubic Bezier easing from Lottie keyframe handles, delegating to the
     * shared {@link BezierEasing} solver that mirrors lottie-web's
     * {@code BezierEaser.js} byte-for-byte.
     *
     * @param t         input progress in [0, 1]
     * @param easingOut outgoing easing handle
     * @param easingIn  incoming easing handle
     * @return eased progress in [0, 1]
     */
    private double applyBezierEasing(double t, EasingHandle easingOut, EasingHandle easingIn) {
        return BezierEasing.solve(t, easingOut, easingIn);
    }
    }

