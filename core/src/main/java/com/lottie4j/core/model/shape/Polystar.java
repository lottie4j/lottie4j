package com.lottie4j.core.model.shape;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lottie4j.core.definition.BlendMode;
import com.lottie4j.core.definition.ShapeType;
import com.lottie4j.core.definition.StarType;
import com.lottie4j.core.model.Animated;
import com.lottie4j.core.model.PropertyLabelValue;

import java.util.ArrayList;
import java.util.List;

/**
 * <a href="https://lottiefiles.github.io/lottie-docs/shapes/#polystar">Lottie Docs: Polystar</a>
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
        @JsonProperty("sy") StarType starType,
        @JsonProperty("p") Animated position,
        @JsonProperty("or") Animated outerRadius,
        @JsonProperty("os") Animated outerRoundness,
        @JsonProperty("r") Animated rotation,
        @JsonProperty("pt") Animated points,
        @JsonProperty("ir") Animated innerRadius,
        @JsonProperty("is") Animated innerRoundness
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
                new PropertyLabelValue("Star type", starType == null ? "-" : starType.label()),
                new PropertyLabelValue("Position", "", position == null ? new ArrayList<>() : position.getLabelValues()),
                new PropertyLabelValue("Outer radius", "", outerRadius == null ? new ArrayList<>() : outerRadius.getLabelValues()),
                new PropertyLabelValue("Outer roundness", "", outerRoundness == null ? new ArrayList<>() : outerRoundness.getLabelValues()),
                new PropertyLabelValue("Rotation", "", rotation == null ? new ArrayList<>() : rotation.getLabelValues()),
                new PropertyLabelValue("Points", "", points == null ? new ArrayList<>() : points.getLabelValues()),
                new PropertyLabelValue("Inner radius", "", innerRadius == null ? new ArrayList<>() : innerRadius.getLabelValues()),
                new PropertyLabelValue("Inner roundness", "", innerRoundness == null ? new ArrayList<>() : innerRoundness.getLabelValues())
        );
    }
}
