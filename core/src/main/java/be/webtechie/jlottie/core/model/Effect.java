package be.webtechie.jlottie.core.model;

import be.webtechie.jlottie.core.definition.EffectType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * https://lottiefiles.github.io/lottie-docs/effects/
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Effect(
        @JsonProperty("nm") String name,
        @JsonProperty("mn") String matchName,
        @JsonProperty("inv") Integer index,
        @JsonProperty("ty") EffectType type,
        @JsonProperty("en") Integer enabled
        // TODO EXTEND FURTHER
) {
}
