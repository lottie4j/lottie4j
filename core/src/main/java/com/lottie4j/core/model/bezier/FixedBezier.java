package com.lottie4j.core.model.bezier;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.NumberSerializer;

/**
 * https://lottiefiles.github.io/lottie-docs/concepts/#bezier
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public record FixedBezier(
        @JsonProperty("a")
        @JsonSerialize(using = NumberSerializer.class)
        Integer animated,

        @JsonProperty("k")
        BezierDefinition bezier

) implements Bezier {
}
