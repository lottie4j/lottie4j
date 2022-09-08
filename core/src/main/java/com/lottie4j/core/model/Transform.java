package com.lottie4j.core.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * https://lottiefiles.github.io/lottie-docs/concepts/#transform
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public record Transform(
        @JsonProperty("a") Animated anchor,
        @JsonProperty("p") Position position,
        @JsonProperty("s") Animated scale,
        @JsonProperty("r") Animated rotation,
        @JsonProperty("rx") Animated rx,
        @JsonProperty("ry") Animated ry,
        @JsonProperty("rz") Animated rz,
        @JsonProperty("sk") Integer skew,
        @JsonProperty("sa") Integer skewAxis,
        @JsonProperty("o") Animated opacity,
        @JsonProperty("or") Animated unknown
) {
}
