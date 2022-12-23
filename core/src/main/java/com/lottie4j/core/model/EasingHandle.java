package com.lottie4j.core.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * https://lottiefiles.github.io/lottie-docs/concepts/#animated-property
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public record EasingHandle(
        @JsonProperty("x") List<Double> x,
        @JsonProperty("y") List<Double> y
) {
}
