package com.lottie4j.core.model.keyframe;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lottie4j.core.model.EasingHandle;
import com.lottie4j.core.model.PropertyLabelValue;
import com.lottie4j.core.model.PropertyListing;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * https://lottiefiles.github.io/lottie-docs/concepts/#keyframe
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record TimedKeyframe(
        @JsonProperty("t") Integer time, // in frames
        // Use BigDecimal here to be able to handle both Integer and Double
        // https://stackoverflow.com/questions/40885065/jackson-mapper-integer-from-json-parsed-as-double-with-drong-precision
        @JsonProperty("s") List<BigDecimal> values,
        @JsonProperty("e") List<BigDecimal> unknown_e,
        @JsonProperty("i") EasingHandle easingIn,
        @JsonProperty("o") EasingHandle easingOut,
        @JsonProperty("h") Integer holdFrame
) implements Keyframe, PropertyListing {
    @Override
    public List<PropertyLabelValue> getLabelValues() {
        return List.of(
                new PropertyLabelValue("Time", time),
                new PropertyLabelValue("Values", (values == null ? "0" : String.valueOf(values.size())),
                        (values == null ? new ArrayList<>() : values.stream().map(v -> new PropertyLabelValue("Value", v.toString())).toList())),
                new PropertyLabelValue("E", (unknown_e == null ? "0" : String.valueOf(unknown_e.size())),
                        (unknown_e == null ? new ArrayList<>() : unknown_e.stream().map(e -> new PropertyLabelValue("E", e.toString())).toList())),
                new PropertyLabelValue("Easing in", "", easingIn == null ? new ArrayList<>() : easingIn.getLabelValues()),
                new PropertyLabelValue("Easing out", "", easingOut == null ? new ArrayList<>() : easingOut.getLabelValues()),
                new PropertyLabelValue("Hold frame", holdFrame)

        );
    }
}

