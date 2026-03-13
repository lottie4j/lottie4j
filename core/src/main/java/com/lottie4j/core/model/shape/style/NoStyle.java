package com.lottie4j.core.model.shape.style;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lottie4j.core.definition.BlendMode;
import com.lottie4j.core.definition.ShapeType;
import com.lottie4j.core.info.PropertyListingList;
import com.lottie4j.core.model.shape.BaseShape;

/**
 * Represents a placeholder shape with no visual styling in a Lottie animation.
 * <p>
 * This record implements the BaseShape interface and serves as a marker or placeholder
 * in the shape hierarchy that does not apply any visual effects or styling. It may be
 * used for organizational purposes, grouping, or as a structural element in the
 * animation layer tree without contributing to the rendered output.
 * <p>
 * The NoStyle shape contains common shape properties like name, visibility, blend mode,
 * and indexing information, but does not define any fill, stroke, gradient, or other
 * visual styling properties. It is identified by the "no" type in JSON serialization.
 * <p>
 * JSON properties are configured to ignore unknown fields during deserialization and
 * exclude null values during serialization.
 *
 * @param name      the display name of this shape element
 * @param matchName the match name used for referencing this shape
 * @param hidden    indicates whether this shape is hidden from rendering
 * @param blendMode the blend mode to apply when compositing this shape
 * @param index     the stacking order index of this shape relative to siblings
 * @param clazz     optional CSS class identifier for this shape
 * @param id        optional unique identifier for this shape
 * @param d         undefined property, purpose not specified
 * @param cix       undefined property, purpose not specified
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record NoStyle(
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
        @JsonProperty("cix") Integer cix
) implements BaseShape {
    @Override
    @JsonIgnore
    public PropertyListingList getList() {
        var list = new PropertyListingList("No Style");
        list.add("Match name", matchName);
        list.add("Type", type);
        list.add("Hidden", hidden);
        list.add("Blend mode", blendMode);
        list.add("Index", index);
        list.add("Clazz", clazz);
        list.add("ID", id);
        list.add("d", d);
        list.add("cix", cix);
        return list;
    }

    @Override
    public ShapeType shapeType() {
        return ShapeType.NO_STYLE;
    }
}
