package com.lottie4j.core.model.transform;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.lottie4j.core.info.PropertyListing;
import com.lottie4j.core.info.PropertyListingList;
import com.lottie4j.core.model.animation.Animated;

/**
 * Represents a transformation applied to a layer or shape in a Lottie animation.
 * <p>
 * This class encapsulates all possible transformation properties including position, scale,
 * rotation, skew, opacity, and anchor point. Each property is animated and can change over
 * time during the animation. Transformations are applied in a specific order to determine
 * the final visual appearance of the element.
 * <p>
 * The transformation properties support both 2D and 3D transformations, with separate
 * rotation axes (rx, ry, rz) for 3D rotations in addition to the standard 2D rotation.
 * <p>
 * All transformation properties are optional and may be null if not specified in the
 * animation data.
 *
 * @param anchor the animated anchor point position
 * @param position the animated position coordinates
 * @param scale the animated scale factors
 * @param rotation the animated 2D rotation angle
 * @param rx the animated x-axis rotation (3D)
 * @param ry the animated y-axis rotation (3D)
 * @param rz the animated z-axis rotation (3D)
 * @param skew the animated skew angle
 * @param skewAxis the animated skew axis angle
 * @param opacity the animated opacity value
 * @param unknown undefined property for future use
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
@JsonPropertyOrder({"a", "p", "s", "r", "rx", "ry", "rz", "sk", "sa", "o", "or"})
public record Transform(
        @JsonProperty("a") Animated anchor,
        @JsonProperty("p") Animated position,
        @JsonProperty("s") Animated scale,
        @JsonProperty("r") Animated rotation,
        @JsonProperty("rx") Animated rx,
        @JsonProperty("ry") Animated ry,
        @JsonProperty("rz") Animated rz,
        @JsonProperty("sk") Animated skew,
        @JsonProperty("sa") Animated skewAxis,
        @JsonProperty("o") Animated opacity,
        @JsonProperty("or") Animated unknown
) implements PropertyListing {
    @Override
    @JsonIgnore
    public PropertyListingList getList() {
        var list = new PropertyListingList("Transform");
        list.add("Anchor", anchor);
        list.add("Position", position);
        list.add("Scale", scale);
        list.add("Rotation", rotation);
        list.add("RX", rx);
        list.add("RY", ry);
        list.add("RZ", rz);
        list.add("Skew", skew);
        list.add("Skew axis", skewAxis);
        list.add("Opacity", opacity);
        list.add("Unknown", unknown);
        return list;
    }
}
