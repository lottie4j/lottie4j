package com.lottie4j.core.model.animation;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lottie4j.core.info.PropertyListing;
import com.lottie4j.core.info.PropertyListingList;

import java.util.List;

/**
 * Represents a keyframe for animated position properties in a Lottie animation.
 * <p>
 * AnimatedPosition defines a single keyframe in a position animation sequence, containing
 * timing information, the position value, easing functions for smooth transitions, and
 * optional tangent vectors for spatial path interpolation.
 * <p>
 * The position value is stored as a string representation, with timing specified in frames.
 * Easing handles control the interpolation curve between this keyframe and the next,
 * with separate handles for easing in and easing out to create sophisticated motion curves.
 * <p>
 * For spatial animations (like position along a path), the tangent vectors ti and to
 * define the incoming and outgoing direction of the motion path at this keyframe,
 * enabling smooth curved trajectories rather than linear point-to-point movement.
 * <p>
 * The holdFrame property allows for step interpolation, where the animation holds
 * at this keyframe's value until the specified frame before transitioning to the next keyframe.
 * <p>
 * This record implements PropertyListing to provide structured inspection of its
 * configuration for debugging and analysis purposes.
 *
 * @param time      the frame number at which this keyframe occurs
 * @param value     the position value as a string representation
 * @param easingIn  the easing handle controlling interpolation into this keyframe
 * @param easingOut the easing handle controlling interpolation out of this keyframe
 * @param holdFrame optional frame number to hold this keyframe's value before transitioning
 * @param ti        incoming spatial tangent vector for path interpolation
 * @param to        outgoing spatial tangent vector for path interpolation
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public record AnimatedPosition(
        @JsonProperty("t") Integer time, // in frames
        @JsonProperty("s") String value,
        @JsonProperty("i") EasingHandle easingIn,
        @JsonProperty("o") EasingHandle easingOut,
        @JsonProperty("h") Integer holdFrame,
        @JsonProperty("ti") List<Double> ti,
        @JsonProperty("to") List<Double> to
) implements PropertyListing {
    @Override
    @JsonIgnore
    public PropertyListingList getList() {
        var list = new PropertyListingList("Animated Position");
        list.add("Time", time);
        list.add("Value", value);
        list.add("Easing in", easingIn);
        list.add("Easing out", easingOut);
        list.add("Hold frame", holdFrame);
        list.addDoubleList("ti", ti);
        list.addDoubleList("to", to);
        return list;
    }
}

