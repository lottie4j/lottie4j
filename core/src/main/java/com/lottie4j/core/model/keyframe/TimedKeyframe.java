package com.lottie4j.core.model.keyframe;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lottie4j.core.info.PropertyListing;
import com.lottie4j.core.info.PropertyListingList;
import com.lottie4j.core.model.animation.EasingHandle;

import java.math.BigDecimal;
import java.util.List;

/**
 * Represents a time-based keyframe with easing controls and spatial interpolation for animation.
 * <p>
 * A timed keyframe defines an animation state at a specific time with comprehensive interpolation
 * controls including easing curves and tangent handles for spatial paths. This keyframe type is
 * used for complex animations that require precise control over timing, velocity curves, and
 * spatial motion paths between keyframes.
 * <p>
 * The time value is specified in frames and can be fractional to support sub-frame precision.
 * Values are stored as BigDecimal to maintain numeric precision for both integer and floating-point
 * data during JSON serialization and deserialization.
 * <p>
 * Easing handles control the velocity curve of the transition from this keyframe to the next,
 * with separate handles for easing in and out to support asymmetric interpolation. Tangent handles
 * control the spatial path for position-based animations, defining the direction and magnitude of
 * the curve at this keyframe point.
 * <p>
 * The hold frame flag can be used to create stepped animations where the value remains constant
 * until the next keyframe is reached, effectively disabling interpolation.
 * <p>
 * This record implements both Keyframe and PropertyListing interfaces to participate in the
 * animation keyframe hierarchy and provide structured introspection of its configuration.
 *
 * @param time the time in frames (can be fractional)
 * @param values the keyframe values at this time
 * @param unknown_e undefined property for future use
 * @param easingIn the easing handle for incoming interpolation
 * @param easingOut the easing handle for outgoing interpolation
 * @param tangentOut the outgoing tangent for spatial interpolation
 * @param tangentIn the incoming tangent for spatial interpolation
 * @param holdFrame flag indicating a hold frame (no interpolation)
 * @see Keyframe
 * @see EasingHandle
 * @see PropertyListing
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record TimedKeyframe(
        @JsonProperty("t") Double time, // in frames (can be fractional)
        // Use BigDecimal here to be able to handle both Integer and Double
        // https://stackoverflow.com/questions/40885065/jackson-mapper-integer-from-json-parsed-as-double-with-drong-precision
        @JsonProperty("s") List<BigDecimal> values,
        @JsonProperty("e") List<BigDecimal> unknown_e,
        @JsonProperty("i") EasingHandle easingIn,
        @JsonProperty("o") EasingHandle easingOut,
        @JsonProperty("to") List<BigDecimal> tangentOut,
        @JsonProperty("ti") List<BigDecimal> tangentIn,
        @JsonProperty("h") Integer holdFrame
) implements Keyframe, PropertyListing {
    @Override
    @JsonIgnore
    public PropertyListingList getList() {
        var list = new PropertyListingList("Timed Keyframe");
        list.add("Time", time);
        list.addBigDecimalList("Values", values);
        list.addBigDecimalList("E", unknown_e);
        list.add("Easing in", easingIn);
        list.add("Easing out", easingOut);
        list.addBigDecimalList("Tangent out", tangentOut);
        list.addBigDecimalList("Tangent in", tangentIn);
        list.add("Hold frame", holdFrame);

        return list;
    }
}
