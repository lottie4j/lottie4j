package com.lottie4j.core.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lottie4j.core.definition.EffectType;

/**
 * <a href="https://lottiefiles.github.io/lottie-docs/effects/">Lottie Docs: Effect</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record Effect(
        @JsonProperty("nm") String name,
        @JsonProperty("mn") String matchName,
        @JsonProperty("inv") Integer index,
        @JsonProperty("ty") EffectType type,
        @JsonProperty("en") Integer enabled
        // TODO EXTEND FURTHER
) {
}
