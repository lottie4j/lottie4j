package com.lottie4j.core.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lottie4j.core.definition.StrokeDashType;

/**
 * <a href="https://lottiefiles.github.io/lottie-docs/shapes/#stroke">Lottie Docs: Stroke</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public record StrokeDash(
        @JsonProperty("nm") String name,
        @JsonProperty("n") StrokeDashType type,
        @JsonProperty("v") Animated length
) {
}
