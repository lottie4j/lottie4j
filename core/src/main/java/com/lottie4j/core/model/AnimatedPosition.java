package com.lottie4j.core.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lottie4j.core.info.PropertyListing;
import com.lottie4j.core.info.PropertyListingList;

import java.util.List;

/**
 * <a href="https://lottiefiles.github.io/lottie-docs/concepts/#animated-position">Lottie Docs: Animated Position</a>
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

