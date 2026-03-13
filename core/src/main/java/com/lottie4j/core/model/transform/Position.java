package com.lottie4j.core.model.transform;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lottie4j.core.info.PropertyListing;
import com.lottie4j.core.info.PropertyListingList;
import com.lottie4j.core.model.animation.Animated;

/**
 * Represents a position property in a Lottie animation with x and y coordinates.
 * <p>
 * This record encapsulates positional data that can be animated over time. The position
 * consists of two animated coordinate values (x and y) that define a point in 2D space.
 * An optional boolean value can be used to enable or disable the position property.
 * <p>
 * The class is designed for JSON serialization/deserialization with Jackson annotations,
 * using shortened property names ("s" for value, "x" for x-coordinate, "y" for y-coordinate)
 * to match the Lottie file format specification. Unknown JSON properties are ignored during
 * deserialization, and null values are excluded from serialization.
 * <p>
 * Implements the PropertyListing interface to provide a human-readable representation
 * of the position data structure for debugging and inspection purposes.
 *
 * @param value flag indicating if position is enabled
 * @param x the animated x coordinate value
 * @param y the animated y coordinate value
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public record Position(
        @JsonProperty("s") Boolean value,
        @JsonProperty("x") Animated x,
        @JsonProperty("y") Animated y
) implements PropertyListing {
    @Override
    @JsonIgnore
    public PropertyListingList getList() {
        var list = new PropertyListingList("Position");
        list.add("Value", value);
        list.add("x", x);
        list.add("y", y);
        return list;
    }
}