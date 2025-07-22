package com.lottie4j.core.model.shape.shape;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lottie4j.core.definition.BlendMode;
import com.lottie4j.core.definition.ShapeType;
import com.lottie4j.core.definition.StarType;
import com.lottie4j.core.info.PropertyListingList;
import com.lottie4j.core.model.Animated;
import com.lottie4j.core.model.shape.BaseShape;

/**
 * <a href="https://lottiefiles.github.io/lottie-docs/shapes/#polystar">Lottie Docs: Polystar</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record Polystar(
        // Generic for all Shapes
        @JsonProperty("nm") String name,
        @JsonProperty("mn") String matchName,
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
    public ShapeType type() {
        return ShapeType.POLYSTAR;
    }

    @Override
    public PropertyListingList getList() {
        var list = new PropertyListingList("Polystar");
        list.add("Match name", matchName);
        list.add("Type", type);
        list.add("Hidden", hidden);
        list.add("Blend mode", blendMode);
        list.add("Index", index);
        list.add("Clazz", clazz);
        list.add("ID", id);
        list.add("d", d);
        list.add("cix", cix);
        list.add("Star type", starType);
        list.add("Position", position);
        list.add("Outer radius", outerRadius);
        list.add("Outer roundness", outerRoundness);
        list.add("Rotation", rotation);
        list.add("Points", points);
        list.add("Inner radius", innerRadius);
        list.add("Inner roundness", innerRoundness);
        return list;
    }
}
