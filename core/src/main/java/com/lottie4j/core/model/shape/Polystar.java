package com.lottie4j.core.model.shape;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lottie4j.core.definition.BlendMode;
import com.lottie4j.core.definition.ShapeType;
import com.lottie4j.core.definition.StarType;
import com.lottie4j.core.model.Animated;

/**
 * https://lottiefiles.github.io/lottie-docs/shapes/#polystar
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record Polystar(
        // Generic for all Shapes
        @JsonProperty("nm") String name,
        @JsonProperty("mn") String matchName,
        @JsonProperty("ty") ShapeType type,
        @JsonProperty("hd") Boolean hidden,
        @JsonProperty("bm") BlendMode blendMode,
        @JsonProperty("ix") Integer index,
        @JsonProperty("cl") String clazz,
        @JsonProperty("ln") String id,

        // Undefined

        @JsonProperty("d") Integer d,
        @JsonProperty("cix") Integer cix,

        // Polystar
        @JsonProperty("p") Animated position,
        @JsonProperty("or") Animated outerRadius,
        @JsonProperty("os") Animated outerRoundness,
        @JsonProperty("r") Animated rotation,
        @JsonProperty("pt") Animated points,
        @JsonProperty("sy") StarType starType,
        @JsonProperty("ir") Animated innerRadius,
        @JsonProperty("is") Animated innerRoundness
) implements BaseShape {
}
