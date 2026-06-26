package com.lottie4j.core.model.animation;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lottie4j.core.definition.AnimatedValueType;
import com.lottie4j.core.helper.BezierEasing;
import com.lottie4j.core.helper.KeyframeDeserializer;
import com.lottie4j.core.helper.KeyframeSerializer;
import com.lottie4j.core.info.PropertyListing;
import com.lottie4j.core.info.PropertyListingList;
import com.lottie4j.core.model.keyframe.Keyframe;
import com.lottie4j.core.model.keyframe.NumberKeyframe;
import com.lottie4j.core.model.keyframe.TimedKeyframe;

import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.annotation.JsonSerialize;

/**
 * Represents an animated property in a Lottie animation that can contain either static values or keyframe-based animations.
 * This class handles both single-value properties (like opacity) and multi-value properties (like position or color).
 * <p>
 * Animated properties can be either:
 * - Static: defined by a single constant value
 * - Animated: defined by a series of keyframes with interpolation between them
 * <p>
 * The class supports:
 * - Value interpolation between keyframes based on the current animation frame
 * - Cubic Bezier easing curves for smooth transitions
 * - Separation of X and Y components for dimensional properties
 * - Both timed keyframes (with start/end values) and static number keyframes
 * <p>
 * This is a core component of the Lottie animation system, used throughout shape properties,
 * transform properties, and effects to define how values change over time.
 *
 * @param animated  indicates whether this property is animated (1) or static (0)
 * @param keyframes list of keyframes defining the animation, can contain NumberKeyframe or TimedKeyframe instances
 * @param ix        property index for identification purposes
 * @param l         property length or dimension indicator
 * @param s         indicates if this is a split/separated property
 * @param x         the X-axis component for split dimensional properties
 * @param y         the Y-axis component for split dimensional properties
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public record Animated(
        @JsonProperty("a")
        Integer animated,

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
    @JsonIgnore
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
     *
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
        boolean frameBeforeFirstKeyframe = false;

        for (int i = 0; i < keyframes.size(); i++) {
            if (keyframes.get(i) instanceof TimedKeyframe timedKeyframe) {
                if (timedKeyframe.time() <= frame) {
                    prevKeyframe = timedKeyframe;
                    // Look for the next keyframe
                    nextKeyframe = null; // Reset to null before checking for next keyframe
                    if (i + 1 < keyframes.size() && keyframes.get(i + 1) instanceof TimedKeyframe next) {
                        nextKeyframe = next;
                    }
                } else {
                    // We've gone past the current frame
                    if (prevKeyframe == null) {
                        // Frame is before the first keyframe - return 0 instead of using first keyframe
                        frameBeforeFirstKeyframe = true;
                    }
                    break;
                }
            }
        }

        if (prevKeyframe == null) {
            // No keyframes at or before this frame - use the first keyframe's value
            if (!keyframes.isEmpty() && keyframes.get(0) instanceof TimedKeyframe firstKeyframe) {
                return getValueFromKeyframe(firstKeyframe, index);
            }
            return 0D;
        }

        // If frame was before the first keyframe, return the first keyframe's value
        if (frameBeforeFirstKeyframe) {
            if (!keyframes.isEmpty() && keyframes.get(0) instanceof TimedKeyframe firstKeyframe) {
                return getValueFromKeyframe(firstKeyframe, index);
            }
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

        double startValue = getValueFromKeyframe(prevKeyframe, index);
        double endValue = getValueFromKeyframe(nextKeyframe, index);

        // Check for spatial bezier interpolation (for position animations)
        // Spatial bezier uses linear progress for path shape, temporal easing controls speed
        if (hasSpatialBezier(prevKeyframe, nextKeyframe, index)) {
            return applySpatialBezier(prevKeyframe, nextKeyframe, index, progress);
        }

        // Apply Bezier easing if available (for non-spatial interpolation)
        double easedProgress = progress;
        if (prevKeyframe.easingOut() != null && prevKeyframe.easingIn() != null) {
            easedProgress = applyBezierEasing(progress, prevKeyframe.easingOut(), prevKeyframe.easingIn());
        }

        return startValue + (endValue - startValue) * easedProgress;
    }

    /**
     * Get value for multi-value animated properties (like position, color) at a specific frame.
     *
     * @param valueType the type of value to retrieve (X, Y, RED, GREEN, etc.)
     * @param frame     the current animation frame
     * @return the interpolated value at the given frame
     */
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
        boolean frameBeforeFirstKeyframe2 = false;

        for (int i = 0; i < keyframes.size(); i++) {
            if (keyframes.get(i) instanceof TimedKeyframe timedKeyframe) {
                if (timedKeyframe.time() <= frame) {
                    prevKeyframe = timedKeyframe;
                    // Look for the next keyframe
                    nextKeyframe = null; // Reset to null before checking for next keyframe
                    if (i + 1 < keyframes.size() && keyframes.get(i + 1) instanceof TimedKeyframe next) {
                        nextKeyframe = next;
                    }
                } else {
                    // We've gone past the current frame
                    if (prevKeyframe == null) {
                        // Frame is before the first keyframe - return 0 instead of using first keyframe
                        frameBeforeFirstKeyframe2 = true;
                    }
                    break;
                }
            }
        }

        // If we only have one keyframe or we're at/after the last keyframe
        if (prevKeyframe == null) {
            // No keyframes at or before this frame - use the first keyframe's value
            if (!keyframes.isEmpty() && keyframes.get(0) instanceof TimedKeyframe firstKeyframe) {
                return getValueFromKeyframe(firstKeyframe, valueType.getIndex());
            }
            return 0D;
        }

        // If frame was before the first keyframe, return the first keyframe's value
        if (frameBeforeFirstKeyframe2) {
            if (!keyframes.isEmpty() && keyframes.get(0) instanceof TimedKeyframe firstKeyframe) {
                return getValueFromKeyframe(firstKeyframe, valueType.getIndex());
            }
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

        double startValue = getValueFromKeyframe(prevKeyframe, valueType.getIndex());
        double endValue = getValueFromKeyframe(nextKeyframe, valueType.getIndex());

        // Check for spatial bezier interpolation (for position animations)
        // Spatial bezier uses linear progress for path shape, temporal easing controls speed
        if (hasSpatialBezier(prevKeyframe, nextKeyframe, valueType.getIndex())) {
            return applySpatialBezier(prevKeyframe, nextKeyframe, valueType.getIndex(), progress);
        }

        // Apply Bezier easing if available (for non-spatial interpolation)
        if (prevKeyframe.easingOut() != null && prevKeyframe.easingIn() != null) {
            progress = applyBezierEasing(progress, prevKeyframe.easingOut(), prevKeyframe.easingIn());
        }

        return startValue + (endValue - startValue) * progress;
    }

    /**
     * Extracts a specific value from a timed keyframe by index.
     *
     * @param keyframe the keyframe to extract from
     * @param index    the index of the value within the keyframe
     * @return the value at the specified index, or 0 if not available
     */
    private Double getValueFromKeyframe(TimedKeyframe keyframe, int index) {
        if (keyframe.values() == null || keyframe.values().isEmpty() || index >= keyframe.values().size()) {
            return 0D;
        }
        return keyframe.values().get(index).doubleValue();
    }

    /**
     * Gets a static (non-animated) value from keyframe at specified index.
     *
     * @param idx the keyframe index
     * @return the value, or 0 if not available
     */
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
     * Apply cubic Bezier easing curve to progress value, delegating to the
     * shared {@link BezierEasing} solver that mirrors lottie-web's
     * {@code BezierEaser.js} byte-for-byte. Centralising the algorithm here
     * removes solver drift as a source of distributed sub-pixel rendering
     * differences against lottie-web.
     *
     * @param t         progress value (0-1)
     * @param easingOut outgoing easing handle from current keyframe
     * @param easingIn  incoming easing handle for next keyframe
     * @return eased progress value
     */
    private double applyBezierEasing(double t, EasingHandle easingOut, EasingHandle easingIn) {
        return BezierEasing.solve(t, easingOut, easingIn);
    }

    /**
     * Calculate cubic Bezier value at t for control points p0, p1, p2, p3.
     *
     * @param t  the parameter value (0-1)
     * @param p0 first control point
     * @param p1 second control point
     * @param p2 third control point
     * @param p3 fourth control point
     * @return the Bezier curve value at parameter t
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
     * Checks if spatial bezier interpolation should be used for the given keyframes and value index.
     * Spatial bezier is used when tangent values (ti/to) are present and non-zero.
     *
     * @param prevKeyframe the starting keyframe
     * @param nextKeyframe the ending keyframe
     * @param index        the value index to check
     * @return true if spatial bezier should be applied
     */
    private boolean hasSpatialBezier(TimedKeyframe prevKeyframe, TimedKeyframe nextKeyframe, int index) {
        // Spatial bezier requires both tangentOut and tangentIn to be present
        if (prevKeyframe.tangentOut() == null || nextKeyframe.tangentIn() == null) {
            return false;
        }

        // Check if the tangents have values at the specified index
        if (prevKeyframe.tangentOut().isEmpty() || nextKeyframe.tangentIn().isEmpty()) {
            return false;
        }

        // Check if index is within bounds
        if (index >= prevKeyframe.tangentOut().size() || index >= nextKeyframe.tangentIn().size()) {
            return false;
        }

        return true;
    }

    /**
     * Applies spatial bezier interpolation using tangent control points.
     * This creates curved paths for position animations instead of linear interpolation.
     *
     * Formula: P(t) = (1-t)³·P0 + 3(1-t)²t·P1 + 3(1-t)t²·P2 + t³·P3
     * Where:
     * - P0 = start value
     * - P1 = P0 + tangentOut
     * - P2 = P3 + tangentIn
     * - P3 = end value
     *
     * @param prevKeyframe the starting keyframe
     * @param nextKeyframe the ending keyframe
     * @param index        the value index to interpolate
     * @param progress     the interpolation progress (0-1), already eased
     * @return the interpolated value using spatial bezier curve
     */
    private double applySpatialBezier(TimedKeyframe prevKeyframe, TimedKeyframe nextKeyframe, int index, double progress) {
        double startValue = getValueFromKeyframe(prevKeyframe, index);
        double endValue = getValueFromKeyframe(nextKeyframe, index);

        // Get tangent values, defaulting to 0 if not available
        double tangentOut = 0.0;
        double tangentIn = 0.0;

        if (prevKeyframe.tangentOut() != null && index < prevKeyframe.tangentOut().size()) {
            tangentOut = prevKeyframe.tangentOut().get(index).doubleValue();
        }

        if (nextKeyframe.tangentIn() != null && index < nextKeyframe.tangentIn().size()) {
            tangentIn = nextKeyframe.tangentIn().get(index).doubleValue();
        }

        // Calculate control points for cubic bezier
        // P0 = startValue
        // P1 = startValue + tangentOut
        // P2 = endValue + tangentIn
        // P3 = endValue
        double p0 = startValue;
        double p1 = startValue + tangentOut;
        double p2 = endValue + tangentIn;
        double p3 = endValue;

        // Apply cubic bezier formula
        return cubicBezier(progress, p0, p1, p2, p3);
    }
}
