package com.lottie4j.core.model.keyframe;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lottie4j.core.info.PropertyListing;
import com.lottie4j.core.info.PropertyListingList;
import com.lottie4j.core.model.EasingHandle;

import java.math.BigDecimal;
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
    public PropertyListingList getList() {
        var list = new PropertyListingList("Timed Keyframe");
        list.add("Time", time);
        list.addBigDecimalList("Values", values);
        list.addBigDecimalList("E", unknown_e);
        list.add("Easing in", easingIn);
        list.add("Easing out", easingOut);
        list.add("Hold frame", holdFrame);

        return list;
    }
}

