package com.lottie4j.core.model.keyframe;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.lottie4j.core.info.PropertyListingList;

/**
 * Represents a keyframe in an animation sequence.
 * <p>
 * A keyframe defines a state or value at a specific point in an animation timeline.
 * This interface serves as the base type for different keyframe representations in the
 * animation system, supporting both simple numeric keyframes and time-based keyframes
 * with easing and interpolation controls.
 * <p>
 * The interface uses JSON type information with deduction-based polymorphic deserialization,
 * defaulting to NumberKeyframe when the specific type cannot be determined. Implementations
 * include NumberKeyframe for simple numeric values and TimedKeyframe for complex animations
 * with timing, easing curves, and tangent controls.
 * <p>
 * Implementations of this interface must provide a property listing through the getList()
 * method to support introspection and structured output of the keyframe's configuration.
 *
 * @see NumberKeyframe
 * @see TimedKeyframe
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.DEDUCTION, defaultImpl = NumberKeyframe.class)
@JsonSubTypes({
        @JsonSubTypes.Type(NumberKeyframe.class),
        @JsonSubTypes.Type(TimedKeyframe.class),
})
public interface Keyframe {

    /**
     * Returns a property listing describing the keyframe configuration.
     *
     * @return the property listing for this keyframe
     */
    PropertyListingList getList();
}