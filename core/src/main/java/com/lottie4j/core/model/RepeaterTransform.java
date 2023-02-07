package com.lottie4j.core.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

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
) {
}
