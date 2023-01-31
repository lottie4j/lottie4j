package com.lottie4j.core.model.bezier;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * https://lottiefiles.github.io/lottie-docs/concepts/#bezier
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public record AnimatedBezier(
        @JsonProperty("a") Integer animated,

        @JsonProperty("k")
        List<BezierKeyframe> beziers
) implements Bezier {
}
