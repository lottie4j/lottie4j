package com.lottie4j.core.model.shape.grouping;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lottie4j.core.definition.BlendMode;
import com.lottie4j.core.definition.ShapeType;
import com.lottie4j.core.info.PropertyListingList;
import com.lottie4j.core.model.shape.BaseShape;

import java.util.List;

/**
 * Represents a group shape container in a Lottie animation that can hold multiple child shapes.
 * <p>
 * A Group is a container shape that organizes and groups together multiple BaseShape elements,
 * allowing them to be transformed, styled, and animated as a single unit. Groups can contain
 * any type of shape including other groups, enabling hierarchical composition of vector graphics.
 * Groups are commonly used to apply transformations or effects to multiple shapes simultaneously
 * while maintaining their relative positioning and properties.
 * <p>
 * This record implements BaseShape and provides JSON serialization support through Jackson
 * annotations. Unknown JSON properties are ignored during deserialization, and null values
 * are excluded from serialization.
 * <p>
 * The Group contains common shape properties inherited from BaseShape such as name, match name,
 * visibility, blend mode, and indexing information. Additionally, it maintains a list of child
 * shapes and metadata about the number of properties contained within the group.
 *
 * @param name               the display name of the group
 * @param matchName          the match name used for identification and targeting in expressions
 * @param hidden             whether the group is hidden from rendering
 * @param blendMode          the blend mode used for compositing this group with underlying layers
 * @param index              the ordering index of this shape within its parent container
 * @param clazz              CSS class identifier for the group
 * @param id                 unique identifier for the group
 * @param d                  undefined property preserved for compatibility
 * @param cix                undefined property preserved for compatibility
 * @param numberOfProperties the count of properties contained in this group
 * @param shapes             the list of child BaseShape elements contained within this group
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record Group(
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

        // Group
        @JsonProperty("np") Integer numberOfProperties,
        @JsonProperty("it") List<BaseShape> shapes
) implements BaseShape {
    @Override
    @JsonIgnore
    public PropertyListingList getList() {
        var list = new PropertyListingList("Group");
        list.add("Match name", matchName);
        list.add("Type", type);
        list.add("Hidden", hidden);
        list.add("Blend mode", blendMode);
        list.add("Index", index);
        list.add("Clazz", clazz);
        list.add("ID", id);
        list.add("d", d);
        list.add("cix", cix);
        list.add("Number of properties", numberOfProperties);
        list.addShapeList("Shapes", shapes);
        return list;
    }

    @Override
    public ShapeType shapeType() {
        return ShapeType.GROUP;
    }
}