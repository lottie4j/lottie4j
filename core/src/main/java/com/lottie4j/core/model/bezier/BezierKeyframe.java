package com.lottie4j.core.model.bezier;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lottie4j.core.info.PropertyListing;
import com.lottie4j.core.info.PropertyListingList;
import com.lottie4j.core.model.EasingHandle;

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
 */
public record BezierKeyframe(
        @JsonProperty("t") Integer time, // in frames
        @JsonProperty("i") EasingHandle easingIn,
        @JsonProperty("o") EasingHandle easingOut,
        @JsonProperty("s") List<BezierDefinition> beziers
) implements PropertyListing {
    @Override
    @JsonIgnore
    public PropertyListingList getList() {
        var list = new PropertyListingList("Bezier Keyframe");
        list.add("Time", time);
        list.add("Easing in", easingIn);
        list.add("Easing out", easingOut);
        list.addList("Beziers", beziers);
        return list;
    }
}
