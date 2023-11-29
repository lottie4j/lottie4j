package com.lottie4j.core.model.shape;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lottie4j.core.definition.BlendMode;
import com.lottie4j.core.definition.FillRule;
import com.lottie4j.core.definition.GradientType;
import com.lottie4j.core.definition.ShapeType;
import com.lottie4j.core.model.Animated;
import com.lottie4j.core.model.PropertyLabelValue;

import java.util.ArrayList;
import java.util.List;

/**
 * <a href="https://lottiefiles.github.io/lottie-docs/shapes/#gradients">Lottie Docs: Gradient Fill</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record GradientFill(
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

        // GradientFill
        @JsonProperty("r") FillRule fillRule,
        @JsonProperty("o") Animated opacity,
        @JsonProperty("s") Animated startingPoint,
        @JsonProperty("e") Animated endPoint,
        @JsonProperty("t") GradientType gradientType,
        @JsonProperty("g") GradientColor colors,
        @JsonProperty("h") Animated highlightLength,
        @JsonProperty("a") Animated highlightAngle
) implements BaseShape {
    @Override
    public List<PropertyLabelValue> getLabelValues() {
        return List.of(new PropertyLabelValue("Match name", matchName),
                new PropertyLabelValue("Type", type == null ? "-" : type.label()),
                new PropertyLabelValue("Hidden", hidden),
                new PropertyLabelValue("Blend mode", blendMode == null ? "-" : blendMode.label()),
                new PropertyLabelValue("Index", index),
                new PropertyLabelValue("Clazz", clazz),
                new PropertyLabelValue("ID", id),
                new PropertyLabelValue("d", d),
                new PropertyLabelValue("cix", cix),
                new PropertyLabelValue("Line cap", lineCap == null ? "-" : lineCap.label()),
                new PropertyLabelValue("Line join", lineJoin == null ? "-" : lineJoin.label()),
                new PropertyLabelValue("Miter limit", miterLimit),
                new PropertyLabelValue("Miter limit alternative", "", miterLimitAlternative == null ? new ArrayList<>() : miterLimitAlternative.getLabelValues()),
                new PropertyLabelValue("Stroke width", "", strokeWidth == null ? new ArrayList<>() : strokeWidth.getLabelValues()),
                new PropertyLabelValue("Opacity", "", opacity == null ? new ArrayList<>() : opacity.getLabelValues()),
                new PropertyLabelValue("Color", "", color == null ? new ArrayList<>() : color.getLabelValues()),
                new PropertyLabelValue("Stroke dashes", strokeDashes == null ? "0" : String.valueOf(strokeDashes.size()),
                        strokeDashes == null ? new ArrayList<>() : strokeDashes.stream().map(sd -> new PropertyLabelValue("Stroke dash", sd.name() == null ? "No name" : sd.name(), sd.getLabelValues())).toList()));
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record GradientColor(
            @JsonProperty("p") Integer numberOfColors,
            @JsonProperty("k") Animated colors
    ) {
    }
}
