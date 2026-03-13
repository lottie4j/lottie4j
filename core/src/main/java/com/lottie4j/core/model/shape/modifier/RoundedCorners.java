package com.lottie4j.core.model.shape.modifier;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lottie4j.core.definition.BlendMode;
import com.lottie4j.core.definition.ShapeType;
import com.lottie4j.core.info.PropertyListingList;
import com.lottie4j.core.model.Animated;
import com.lottie4j.core.model.shape.BaseShape;

/**
 * Represents a rounded corners modifier shape in Lottie animations.
 * <p>
 * This shape modifier applies rounded corners to paths in a shape layer by rounding the vertices
 * with a specified radius. When applied to a group of shapes, it affects all paths within that group,
 * smoothing sharp corners into curved edges. The radius can be animated over time to create dynamic
 * corner rounding effects.
 * <p>
 * As a BaseShape implementation, RoundedCorners supports standard shape properties including name,
 * visibility, blend mode, and hierarchical indexing. The primary configuration is the radius property,
 * which determines the curvature applied to corners.
 * <p>
 * In the Lottie JSON format, this shape is identified by the type "rd" and is typically placed
 * after path definitions but before fill or stroke operations in the shape layer hierarchy.
 * <p>
 * This record is immutable and uses Jackson annotations for JSON serialization/deserialization,
 * ignoring unknown properties and excluding null values from output.
 *
 * @param name      the display name of the rounded corners modifier
 * @param matchName the match name used for identification and referencing
 * @param hidden    whether this modifier is hidden and should not be rendered
 * @param blendMode the blend mode applied when compositing this shape
 * @param index     the hierarchical index within the shape layer
 * @param clazz     optional CSS class identifier for styling or categorization
 * @param id        unique identifier for this shape element
 * @param d         undefined property, purpose reserved for future use
 * @param cix       undefined property related to composition indexing
 * @param radius    the animated radius value determining the curvature of rounded corners
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record RoundedCorners(
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

        // RoundedCorners
        @JsonProperty("r") Animated radius
) implements BaseShape {
    @Override
    @JsonIgnore
    public PropertyListingList getList() {
        var list = new PropertyListingList("Rounded Corners");
        list.add("Match name", matchName);
        list.add("Type", type);
        list.add("Hidden", hidden);
        list.add("Blend mode", blendMode);
        list.add("Index", index);
        list.add("Clazz", clazz);
        list.add("ID", id);
        list.add("d", d);
        list.add("cix", cix);
        list.add("Radius", radius);
        return list;
    }

    @Override
    public ShapeType shapeType() {
        return ShapeType.ROUNDED_CORNERS;
    }
}
