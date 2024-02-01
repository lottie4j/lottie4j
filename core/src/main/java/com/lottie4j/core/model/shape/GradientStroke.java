package com.lottie4j.core.model.shape;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lottie4j.core.definition.*;
import com.lottie4j.core.info.PropertyListingList;
import com.lottie4j.core.model.Animated;
import com.lottie4j.core.model.StrokeDash;

import java.util.List;

/**
 * <a href="https://lottiefiles.github.io/lottie-docs/shapes/#gradients">Lottie Docs: Gradient Stroke</a>
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

        // Undefined

        @JsonProperty("d") Integer d,
        @JsonProperty("cix") Integer cix,

        // GradientStroke
        @JsonProperty("lc") LineCap lineCap,
        @JsonProperty("lj") LineJoin lineJoin,
        @JsonProperty("ml") Integer miterLimit,
        @JsonProperty("ml2") Animated miterLimitAlternative,
        @JsonProperty("w") Animated strokeWidth,
        @JsonProperty("o") Animated opacity,
        @JsonProperty("d") Animated startingPoint,
        @JsonProperty("e") Animated endPoint,
        @JsonProperty("t") GradientType gradientType,
        @JsonProperty("g") List<Double> colors,
        @JsonProperty("d") List<StrokeDash> strokeDashes
) implements BaseShape {
    @Override
    public PropertyListingList getList() {
        var list = new PropertyListingList("Gradient Stroke");
        list.add("Match name", matchName);
        list.add("Type", type);
        list.add("Hidden", hidden);
        list.add("Blend mode", blendMode);
        list.add("Index", index);
        list.add("Clazz", clazz);
        list.add("ID", id);
        list.add("d", d);
        list.add("cix", cix);
        list.add("Line cap", lineCap);
        list.add("Line join", lineJoin);
        list.add("Miter limit", miterLimit);
        list.add("Miter limit alternative", miterLimitAlternative);
        list.add("Stroke width", strokeWidth);
        list.add("Opacity", opacity);
        list.add("Starting point", startingPoint);
        list.add("End point", endPoint);
        list.add("Gradient type", gradientType);
        list.addDoubleList("Colors", colors);
        list.addList("Stroke dashes", strokeDashes);
        return list;
    }
}
