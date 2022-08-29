package be.webtechie.jlottie.core.model.shape;

import be.webtechie.jlottie.core.definition.BlendMode;
import be.webtechie.jlottie.core.definition.ShapeType;
import be.webtechie.jlottie.core.model.Animated;
import be.webtechie.jlottie.core.model.Position;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * https://lottiefiles.github.io/lottie-docs/shapes/#transform-shape
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record Transform(
        // Generic for all Shapes
        @JsonProperty("nm") String name,
        @JsonProperty("mn") String matchName,
        @JsonProperty("ty") ShapeType type,
        @JsonProperty("hd") Boolean hidden,
        @JsonProperty("bm") BlendMode blendMode,
        @JsonProperty("ix") Integer index,
        @JsonProperty("cl") String clazz,
        @JsonProperty("ln") String id,

        // Transform
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
