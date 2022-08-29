package be.webtechie.jlottie.core.model;

import be.webtechie.jlottie.core.definition.StrokeDashType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * https://lottiefiles.github.io/lottie-docs/shapes/#stroke
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public record StrokeDash(
        @JsonProperty("n") StrokeDashType type,
        @JsonProperty("v") Animated length
) {
}
