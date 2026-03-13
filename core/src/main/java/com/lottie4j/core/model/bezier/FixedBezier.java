package com.lottie4j.core.model.bezier;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lottie4j.core.info.PropertyListingList;

/**
 * Represents a fixed (non-animated) Bezier curve with a single, static definition.
 * <p>
 * This implementation of the Bezier interface encapsulates a Bezier curve that does not
 * change over time. It contains a single BezierDefinition that defines the complete shape
 * of the curve including its vertices and control points.
 * <p>
 * The animated field serves as a flag to indicate whether this Bezier curve is animated.
 * For FixedBezier instances, this value typically indicates a static state.
 * <p>
 * This record is commonly used in vector graphics and animation systems where shapes
 * need to be defined but remain constant throughout their lifecycle, as opposed to
 * animated Bezier curves that transition between different states using keyframes.
 * <p>
 * The class supports JSON serialization and deserialization, with unknown properties
 * being ignored during deserialization and null values being excluded from serialization.
 *
 * @param animated flag indicating the animation state of this Bezier curve
 * @param bezier   the static Bezier curve definition containing vertices and control points
 * @param ix       property index for identification purposes
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public record FixedBezier(
        @JsonProperty("a")
        Integer animated,

        @JsonProperty("k")
        BezierDefinition bezier,

        @JsonProperty("ix")
        Integer ix

) implements Bezier {
    @Override
    @JsonIgnore
    public PropertyListingList getList() {
        var list = new PropertyListingList("Fixed Bezier");
        list.add("Animated", animated);
        list.add("Bezier", bezier);
        list.add("ix", ix);
        return list;
    }
}
