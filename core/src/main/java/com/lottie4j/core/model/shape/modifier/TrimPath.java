package com.lottie4j.core.model.shape.modifier;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lottie4j.core.definition.BlendMode;
import com.lottie4j.core.definition.ShapeType;
import com.lottie4j.core.definition.TrimMultipleShapes;
import com.lottie4j.core.info.PropertyListingList;
import com.lottie4j.core.model.Animated;
import com.lottie4j.core.model.shape.BaseShape;

/**
 * <a href="https://lottiefiles.github.io/lottie-docs/shapes/#trim-path">Lottie Docs: Trim Path</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record TrimPath(
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

        // Trim
        @JsonProperty("s") Animated segmentStart,
        @JsonProperty("e") Animated segmentEnd,
        @JsonProperty("o") Animated offset,
        @JsonProperty("m") TrimMultipleShapes trimMultipleShapes
) implements BaseShape {

    @Override
    public ShapeType type() {
        return ShapeType.TRIM;
    }

    @Override
    public PropertyListingList getList() {
        var list = new PropertyListingList("Trim Path");
        list.add("Match name", matchName);
        list.add("Type", type);
        list.add("Hidden", hidden);
        list.add("Blend mode", blendMode);
        list.add("Index", index);
        list.add("Clazz", clazz);
        list.add("ID", id);
        list.add("d", d);
        list.add("cix", cix);
        list.add("Segment start", segmentStart);
        list.add("Segment end", segmentEnd);
        list.add("Offset", offset);
        list.add("Trim multiple shapes", trimMultipleShapes);
        return list;
    }
}
