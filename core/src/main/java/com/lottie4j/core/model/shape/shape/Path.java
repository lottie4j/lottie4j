package com.lottie4j.core.model.shape.shape;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.lottie4j.core.definition.BlendMode;
import com.lottie4j.core.definition.ShapeType;
import com.lottie4j.core.helper.BezierDeserializer;
import com.lottie4j.core.info.PropertyListingList;
import com.lottie4j.core.model.bezier.Bezier;
import com.lottie4j.core.model.shape.BaseShape;

/**
 * Represents a path shape in a Lottie animation, defining a custom vector path using Bezier curves.
 * <p>
 * A Path shape is one of the fundamental drawing primitives in Lottie animations, allowing for
 * the creation of arbitrary vector shapes through Bezier curve definitions. The path can be either
 * static or animated, with the shape morphing over time based on keyframe data.
 * <p>
 * This record implements the BaseShape interface and is deserialized from JSON using the "sh" type
 * identifier. It contains both common shape properties (name, blend mode, visibility) and
 * path-specific data including the Bezier curve definition that describes the actual path geometry.
 * <p>
 * The Bezier curve can be either a FixedBezier (for static paths) or an AnimatedBezier (for paths
 * that change over time), with the appropriate type determined during JSON deserialization by the
 * BezierDeserializer.
 * <p>
 * Paths can be combined with other shapes in a Group and modified by path operations like Merge,
 * TrimPath, or RoundedCorners to create complex vector graphics. They form the basis for most
 * custom vector artwork in Lottie animations.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record Path(
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
        @JsonProperty("ind") Integer ind,

        // Path
        @JsonProperty("ks")
        @JsonDeserialize(using = BezierDeserializer.class)
        Bezier bezier
) implements BaseShape {
    @Override
    @JsonIgnore
    public PropertyListingList getList() {
        var list = new PropertyListingList("Path");
        list.add("Match name", matchName);
        list.add("Type", type);
        list.add("Hidden", hidden);
        list.add("Blend mode", blendMode);
        list.add("Index", index);
        list.add("Clazz", clazz);
        list.add("ID", id);
        list.add("d", d);
        list.add("cix", cix);
        list.add("ind", ind);
        list.add("Bezier", bezier);
        return list;
    }

    @Override
    public ShapeType shapeType() {
        return ShapeType.PATH;
    }
}
