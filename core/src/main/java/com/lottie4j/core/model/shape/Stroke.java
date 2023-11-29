package com.lottie4j.core.model.shape;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lottie4j.core.definition.BlendMode;
import com.lottie4j.core.definition.LineCap;
import com.lottie4j.core.definition.LineJoin;
import com.lottie4j.core.definition.ShapeType;
import com.lottie4j.core.model.Animated;
import com.lottie4j.core.model.PropertyLabelValue;
import com.lottie4j.core.model.StrokeDash;

import java.util.ArrayList;
import java.util.List;

/**
 * <a href="https://lottiefiles.github.io/lottie-docs/shapes/#stroke">Lottie Docs: Stroke</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record Stroke(
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

        //@JsonProperty("d") Integer d,
        @JsonProperty("cix") Integer cix,

        // Stroke
        @JsonProperty("lc") LineCap lineCap,
        @JsonProperty("lj") LineJoin lineJoin,
        @JsonProperty("ml") Integer miterLimit,
        @JsonProperty("ml2") Animated miterLimitAlternative,
        @JsonProperty("w") Animated strokeWidth,
        @JsonProperty("d") List<StrokeDash> strokeDashes,
        @JsonProperty("o") Animated opacity,
        @JsonProperty("c") Animated color
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
}
