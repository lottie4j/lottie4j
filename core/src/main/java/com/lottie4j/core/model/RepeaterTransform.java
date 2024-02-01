package com.lottie4j.core.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lottie4j.core.info.PropertyListing;
import com.lottie4j.core.info.PropertyListingList;

/**
 * <a href="https://lottiefiles.github.io/lottie-docs/concepts/#transform">Lottie Docs: Transform</a>
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