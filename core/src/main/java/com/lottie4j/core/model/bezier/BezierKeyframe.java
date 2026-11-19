package com.lottie4j.core.model.bezier;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lottie4j.core.helper.DoubleAsIntegerIfWholeSerializer;
import com.lottie4j.core.info.PropertyListing;
import com.lottie4j.core.info.PropertyListingList;
import com.lottie4j.core.model.animation.EasingHandle;

import tools.jackson.databind.annotation.JsonSerialize;

import java.util.List;

/**
 * Represents a keyframe in an animation timeline that uses Bézier curves for interpolation.
 * <p>
 * A BezierKeyframe defines a specific point in time during an animation where property values
 * are set, along with the easing functions that control how the animation transitions to and
 * from this keyframe. The Bézier curves specified in this keyframe determine the shape or path
 * values at this point in the animation timeline.
 * <p>
 * The easing handles control the velocity curve of transitions: easingOut affects the transition
 * from this keyframe to the next, while easingIn affects the transition from the previous keyframe
 * to this one. Multiple Bézier definitions can be specified to support complex multi-path animations.
 * <p>
 * This record implements PropertyListing to provide structured output of its configuration for
 * debugging and inspection purposes.
 *
 * @param time      the time position of this keyframe in the animation timeline, measured in frames
 * @param easingIn  the easing handle that controls the interpolation curve when transitioning
 *                  into this keyframe from the previous keyframe
 * @param easingOut the easing handle that controls the interpolation curve when transitioning
 *                  from this keyframe to the next keyframe
 * @param beziers   list of Bézier curve definitions that specify the shape or path values at
 *                  this keyframe, supporting multi-path animations
 * @param endBeziers the shape the segment that starts here ends on; only in files written in the old
 *                   form, see {@link com.lottie4j.core.helper.OldFormatKeyframes}
 * @param holdFrame  1 if the shape is held until the next keyframe instead of interpolated
 */
public record BezierKeyframe(
        @JsonProperty("t") @JsonSerialize(using = DoubleAsIntegerIfWholeSerializer.class) Double time, // in frames
        @JsonProperty("i") EasingHandle easingIn,
        @JsonProperty("o") EasingHandle easingOut,
        @JsonProperty("s") List<BezierDefinition> beziers,
        @JsonProperty("e") List<BezierDefinition> endBeziers,
        @JsonProperty("h") Integer holdFrame
) implements PropertyListing {

    /**
     * Creates a keyframe in the current form: without an end shape, and not held.
     *
     * @param time      the time of the keyframe in frames
     * @param easingIn  the easing handle towards the next keyframe
     * @param easingOut the easing handle away from this keyframe
     * @param beziers   the shape at this keyframe
     */
    public BezierKeyframe(Double time, EasingHandle easingIn, EasingHandle easingOut,
                          List<BezierDefinition> beziers) {
        this(time, easingIn, easingOut, beziers, null, null);
    }

    /**
     * Whether the shape stays as it is until the next keyframe instead of changing towards it.
     *
     * @return true for a hold keyframe
     */
    @JsonIgnore
    public boolean isHold() {
        return holdFrame != null && holdFrame != 0;
    }

    @Override
    @JsonIgnore
    public PropertyListingList getList() {
        var list = new PropertyListingList("Bezier Keyframe");
        list.add("Time", time);
        list.add("Easing in", easingIn);
        list.add("Easing out", easingOut);
        list.addList("Beziers", beziers);
        list.add("Hold frame", holdFrame);
        return list;
    }
}
