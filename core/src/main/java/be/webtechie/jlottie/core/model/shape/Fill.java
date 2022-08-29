package be.webtechie.jlottie.core.model.shape;

import be.webtechie.jlottie.core.definition.BlendMode;
import be.webtechie.jlottie.core.definition.FillRule;
import be.webtechie.jlottie.core.definition.ShapeType;
import be.webtechie.jlottie.core.model.Animated;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * https://lottiefiles.github.io/lottie-docs/shapes/#fill
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record Fill(
        // Generic for all Shapes
        @JsonProperty("nm") String name,
        @JsonProperty("mn") String matchName,
        @JsonProperty("ty") ShapeType type,
        @JsonProperty("hd") Boolean hidden,
        @JsonProperty("bm") BlendMode blendMode,
        @JsonProperty("ix") Integer index,
        @JsonProperty("cl") String clazz,
        @JsonProperty("ln") String id,

        // Fill
        @JsonProperty("r") FillRule fillRule,
        @JsonProperty("o") Animated opacity,
        @JsonProperty("c") Animated color
) {
}
