package com.lottie4j.core.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
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
    public List<PropertyLabelValue> getLabelValues() {
        return List.of(
                new PropertyLabelValue("Time", time),
                new PropertyLabelValue("Value", value),
                new PropertyLabelValue("Easing in", "", easingIn == null ? new ArrayList<>() : easingIn.getLabelValues()),
                new PropertyLabelValue("Easing out", "", easingOut == null ? new ArrayList<>() : easingOut.getLabelValues()),
                new PropertyLabelValue("Hold frame", holdFrame),
                new PropertyLabelValue("ti", ti == null ? "0" : String.valueOf(ti.size()),
                        ti == null ? new ArrayList<>() : ti.stream().map(v -> new PropertyLabelValue("ti", v.toString())).toList()),
                new PropertyLabelValue("to", to == null ? "0" : String.valueOf(to.size()),
                        to == null ? new ArrayList<>() : to.stream().map(v -> new PropertyLabelValue("to", v.toString())).toList())
        );
    }
}

