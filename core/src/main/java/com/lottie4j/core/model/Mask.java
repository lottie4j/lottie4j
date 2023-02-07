package com.lottie4j.core.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * <a href="https://lottiefiles.github.io/lottie-docs/layers/#masks">Lottie Docs: Mask</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Mask(
        @JsonProperty("nm") String name,
        @JsonProperty("mn") String matchName,
        @JsonProperty("inv") Boolean inverted
        // TODO EXTEND FURTHER
) {
}
