package com.lottie4j.core.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.lottie4j.core.helper.KeyframeDeserializer;
import com.lottie4j.core.helper.KeyframeSerializer;
import com.lottie4j.core.info.PropertyListing;
import com.lottie4j.core.info.PropertyListingList;
import com.lottie4j.core.model.keyframe.Keyframe;
import com.lottie4j.core.model.keyframe.NumberKeyframe;
import com.lottie4j.core.model.keyframe.TimedKeyframe;

import java.util.List;

/**
 * <a href="https://lottiefiles.github.io/lottie-docs/concepts/#animated-property">Lottie Docs: Animated</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public record Animated(
        @JsonProperty("a")
        Double animated,

        @JsonProperty("k")
        @JsonSerialize(using = KeyframeSerializer.class)
        @JsonDeserialize(using = KeyframeDeserializer.class)
        List<Keyframe> keyframes,

        @JsonProperty("ix") Integer ix,
        @JsonProperty("l") Integer l,
        @JsonProperty("s") Boolean s,
        @JsonProperty("x") Animated x,
        @JsonProperty("y") Animated y
) implements PropertyListing {
    @Override
    public PropertyListingList getList() {
        var list = new PropertyListingList("Animated");
        list.add("Animated", animated);
        list.addKeyframeList("Keyframes", keyframes);
        list.add("ix", ix);
        list.add("l", l);
        list.add("s", s);
        list.add("x", x);
        list.add("y", y);
        return list;
    }

    /**
     * Get value for single-value animated properties (like opacity) at a specific frame
     * @param index the index of the value (typically 0 for single-value properties)
     * @param frame the current animation frame
     * @return the interpolated value at the given frame
     */
    public Double getValue(int index, double frame) {
        if (keyframes == null || keyframes.isEmpty()) {
            return 0D;
        }
        if (animated == null || animated == 0) {
            // not animated, fixed value
            return getValue(index);
        }

        // Find the appropriate keyframe(s) for the current frame
        TimedKeyframe prevKeyframe = null;
        TimedKeyframe nextKeyframe = null;

        for (int i = 0; i < keyframes.size(); i++) {
            if (keyframes.get(i) instanceof TimedKeyframe timedKeyframe) {
                if (timedKeyframe.time() <= frame) {
                    prevKeyframe = timedKeyframe;
                    // Look for the next keyframe
                    if (i + 1 < keyframes.size() && keyframes.get(i + 1) instanceof TimedKeyframe next) {
                        nextKeyframe = next;
                    }
                } else {
                    // We've gone past the current frame
                    if (prevKeyframe == null) {
                        // Frame is before the first keyframe
                        prevKeyframe = timedKeyframe;
                    }
                    break;
                }
            }
        }

        if (prevKeyframe == null) {
            return 0D;
        }

        // If there's no next keyframe, just use the previous one
        if (nextKeyframe == null) {
            return getValueFromKeyframe(prevKeyframe, index);
        }

        // Check if this is a hold keyframe (no interpolation)
        if (prevKeyframe.holdFrame() != null && prevKeyframe.holdFrame() == 1) {
            return getValueFromKeyframe(prevKeyframe, index);
        }

        // Interpolate between keyframes with easing
        double startFrame = prevKeyframe.time();
        double endFrame = nextKeyframe.time();
        double progress = (frame - startFrame) / (endFrame - startFrame);

        // Clamp progress to [0, 1]
        progress = Math.max(0, Math.min(1, progress));

        // Apply Bezier easing if available
        double easedProgress = progress;
        if (prevKeyframe.easingOut() != null && prevKeyframe.easingIn() != null) {
            easedProgress = applyBezierEasing(progress, prevKeyframe.easingOut(), prevKeyframe.easingIn());
        }

        double startValue = getValueFromKeyframe(prevKeyframe, index);
        double endValue = getValueFromKeyframe(nextKeyframe, index);

        // Debug logging for rotation
        if (index == 0 && (Math.abs(endValue - startValue) > 90 || (startValue >= 170 && startValue <= 190))) {
            java.util.logging.Logger.getLogger("Animated").fine(
                String.format("Rotation: frame=%.1f, kf[%.1f→%.1f], val[%.1f→%.1f], prog=%.3f→%.3f = %.1f",
                    frame, startFrame, endFrame, startValue, endValue, progress, easedProgress,
                    startValue + (endValue - startValue) * easedProgress));
        }

        return startValue + (endValue - startValue) * easedProgress;
    }

    public Double getValue(AnimatedValueType valueType, double frame) {
        // Check if this uses separated dimensions (s: true means x and y are separate Animated objects)
        if (s != null && s) {
            if (valueType == AnimatedValueType.X && x != null) {
                return x.getValue(0, frame);
            } else if (valueType == AnimatedValueType.Y && y != null) {
                return y.getValue(0, frame);
            }
        }

        if (keyframes == null || keyframes.isEmpty()) {
            return 0D;
        }
        if (animated == null || animated == 0) {
            // not animated, fixed value
            return getValue(valueType.getIndex());
        }

        // Find the appropriate keyframe(s) for the current frame
        TimedKeyframe prevKeyframe = null;
        TimedKeyframe nextKeyframe = null;

        for (int i = 0; i < keyframes.size(); i++) {
            if (keyframes.get(i) instanceof TimedKeyframe timedKeyframe) {
                if (timedKeyframe.time() <= frame) {
                    prevKeyframe = timedKeyframe;
                    // Look for the next keyframe
                    if (i + 1 < keyframes.size() && keyframes.get(i + 1) instanceof TimedKeyframe next) {
                        nextKeyframe = next;
                    }
                } else {
                    // We've gone past the current frame
                    if (prevKeyframe == null) {
                        // Frame is before the first keyframe
                        prevKeyframe = timedKeyframe;
                    }
                    break;
                }
            }
        }

        // If we only have one keyframe or we're at/after the last keyframe
        if (prevKeyframe == null) {
            return 0D;
        }

        if (nextKeyframe == null) {
            // Use the last keyframe value
            return getValueFromKeyframe(prevKeyframe, valueType.getIndex());
        }

        // Check for hold keyframe (step interpolation)
        if (prevKeyframe.holdFrame() != null && prevKeyframe.holdFrame() == 1) {
            return getValueFromKeyframe(prevKeyframe, valueType.getIndex());
        }

        // Interpolate between keyframes with easing
        double startFrame = prevKeyframe.time();
        double endFrame = nextKeyframe.time();
        double progress = (frame - startFrame) / (endFrame - startFrame);

        // Clamp progress to [0, 1]
        progress = Math.max(0, Math.min(1, progress));

        // Apply Bezier easing if available
        if (prevKeyframe.easingOut() != null && prevKeyframe.easingIn() != null) {
            progress = applyBezierEasing(progress, prevKeyframe.easingOut(), prevKeyframe.easingIn());
        }

        double startValue = getValueFromKeyframe(prevKeyframe, valueType.getIndex());
        double endValue = getValueFromKeyframe(nextKeyframe, valueType.getIndex());

        return startValue + (endValue - startValue) * progress;
    }

    private Double getValueFromKeyframe(TimedKeyframe keyframe, int index) {
        if (keyframe.values() == null || keyframe.values().isEmpty() || index >= keyframe.values().size()) {
            return 0D;
        }
        return keyframe.values().get(index).doubleValue();
    }

    public Double getValue(int idx) {
        if (keyframes == null || keyframes.isEmpty() || keyframes.size() <= idx) {
            return 0D;
        }
        var keyframe = keyframes.get(idx);
        if (keyframe instanceof NumberKeyframe numberKeyframe) {
            return numberKeyframe.doubleValue();
        }
        return 0D;
    }

    /**
     * Apply cubic Bezier easing curve to progress value
     * Based on the Lottie easing specification
     * @param t progress value (0-1)
     * @param easingOut outgoing easing handle from current keyframe
     * @param easingIn incoming easing handle for next keyframe (not used in simplified calculation)
     * @return eased progress value
     */
    private double applyBezierEasing(double t, EasingHandle easingOut, EasingHandle easingIn) {
        // Get control points from easing handles (use first value if multiple)
        // Lottie uses cubic bezier with control points P0(0,0), P1(x1,y1), P2(x2,y2), P3(1,1)
        double x1 = easingOut.x() != null && !easingOut.x().isEmpty() ? easingOut.x().get(0) : 0.0;
        double y1 = easingOut.y() != null && !easingOut.y().isEmpty() ? easingOut.y().get(0) : 0.0;
        double x2 = easingIn.x() != null && !easingIn.x().isEmpty() ? easingIn.x().get(0) : 1.0;
        double y2 = easingIn.y() != null && !easingIn.y().isEmpty() ? easingIn.y().get(0) : 1.0;

        // Use Newton-Raphson iteration to find t value that gives us the correct x
        // This solves the cubic Bezier equation for x to find the corresponding y
        double currentT = t;
        for (int i = 0; i < 8; i++) {
            double currentX = cubicBezier(currentT, 0, x1, x2, 1);
            double dx = currentX - t;
            if (Math.abs(dx) < 0.001) break;

            double derivative = cubicBezierDerivative(currentT, 0, x1, x2, 1);
            if (Math.abs(derivative) < 1e-6) break;

            currentT = currentT - dx / derivative;
        }

        // Calculate y value using the solved t
        return cubicBezier(currentT, 0, y1, y2, 1);
    }

    /**
     * Calculate cubic Bezier value at t for control points p0, p1, p2, p3
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
     * Calculate derivative of cubic Bezier at t for control points p0, p1, p2, p3
     */
    private double cubicBezierDerivative(double t, double p0, double p1, double p2, double p3) {
        double t2 = t * t;
        double mt = 1 - t;
        double mt2 = mt * mt;

        return 3 * mt2 * (p1 - p0) + 6 * mt * t * (p2 - p1) + 3 * t2 * (p3 - p2);
    }
}
