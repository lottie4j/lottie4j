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
 * Represents transformation properties for a repeater element in a Lottie animation.
 * <p>
 * This record encapsulates all transformation parameters that can be applied to repeated instances
 * of shapes or elements, including spatial transformations (position, rotation, scale), skew effects,
 * and opacity gradients. The transformation properties are animated values that can change over time.
 * <p>
 * The transformation properties control how each repeated instance is transformed relative to the
 * previous instance. This allows for creating complex patterns and effects by repeating and
 * progressively transforming shapes.
 * <p>
 * Supports both 2D and 3D transformations through separate rotation axes (rx, ry, rz).
 * Opacity can be controlled with start and end values to create fade effects across repeated instances.
 * <p>
 * JSON deserialization ignores unknown properties and excludes null values during serialization.
 *
 * @param anchor       the anchor point for the transformation
 * @param position     the position offset applied to each repeated instance
 * @param scale        the scale factor applied to each repeated instance
 * @param rotation     the rotation angle applied to each repeated instance
 * @param rx           the rotation around the X-axis (3D rotation)
 * @param ry           the rotation around the Y-axis (3D rotation)
 * @param rz           the rotation around the Z-axis (3D rotation)
 * @param skew         the skew angle applied to each repeated instance
 * @param skewAxis     the axis along which skew is applied
 * @param opacityStart the starting opacity value for the first repeated instance
 * @param opacityEnd   the ending opacity value for the last repeated instance
 * @param unknown      an unidentified property that may be present in some Lottie files
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public record RepeaterTransform(
        @JsonProperty("a") Animated anchor,
        @JsonProperty("p") Animated position,
        @JsonProperty("s") Animated scale,
        @JsonProperty("r") Animated rotation,
        @JsonProperty("rx") Animated rx,
        @JsonProperty("ry") Animated ry,
        @JsonProperty("rz") Animated rz,
        @JsonProperty("sk") Animated skew,
        @JsonProperty("sa") Animated skewAxis,
        @JsonProperty("so") Animated opacityStart,
        @JsonProperty("eo") Animated opacityEnd,
        @JsonProperty("or") Animated unknown
) implements PropertyListing {
    @Override
    @JsonIgnore
    public PropertyListingList getList() {
        var list = new PropertyListingList("Repeater Transform");
        list.add("Anchor", anchor);
        list.add("Position", position);
        list.add("Scale", scale);
        list.add("Rotation", rotation);
        list.add("RX", rx);
        list.add("RY", ry);
        list.add("RZ", rz);
        list.add("Skew", skew);
        list.add("Skew axis", skewAxis);
        list.add("Opacity start", opacityStart);
        list.add("Opacity end", opacityEnd);
        list.add("Unknown", unknown);
        return list;
    }
}