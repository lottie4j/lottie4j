package com.lottie4j.core.model.shape;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lottie4j.core.definition.BlendMode;
import com.lottie4j.core.definition.MergeMode;
import com.lottie4j.core.definition.ShapeType;
import com.lottie4j.core.model.PropertyLabelValue;

import java.util.List;

/**
 * <a href="https://lottiefiles.github.io/lottie-docs/shapes/#merge">Lottie Docs: Merge</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record Merge(
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

        // Merge
        @JsonProperty("mm") MergeMode mergeMode
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
                new PropertyLabelValue("Merge mode", mergeMode == null ? "-" : mergeMode.label()));
    }
}
