package be.webtechie.jlottie.core.model.shape;

import be.webtechie.jlottie.core.definition.*;
import be.webtechie.jlottie.core.model.Animated;
import be.webtechie.jlottie.core.model.StrokeDash;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * https://lottiefiles.github.io/lottie-docs/shapes/#gradients
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record GradientStroke(
        // Generic for all Shapes
        @JsonProperty("nm") String name,
        @JsonProperty("mn") String matchName,
        @JsonProperty("ty") ShapeType type,
        @JsonProperty("hd") Boolean hidden,
        @JsonProperty("bm") BlendMode blendMode,
        @JsonProperty("ix") Integer index,
        @JsonProperty("cl") String clazz,
        @JsonProperty("ln") String id,

        // GradientStroke
        @JsonProperty("lc") LineCap lineCap,
        @JsonProperty("lj") LineJoin lineJoin,
        @JsonProperty("ml") Integer miterLimit,
        @JsonProperty("ml2") Animated miterLimitAlternative,
        @JsonProperty("w") Animated strokeWidth,
        @JsonProperty("d") List<StrokeDash> strokeDashes,
        @JsonProperty("o") Animated opacity,
        @JsonProperty("d") Animated startingPoint,
        @JsonProperty("e") Animated endPoint,
        @JsonProperty("t") GradientType gradientType,
        @JsonProperty("g") Object colors // TODO
) {
}
