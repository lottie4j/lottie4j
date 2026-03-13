package com.lottie4j.core.model.shape.shape;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lottie4j.core.definition.BlendMode;
import com.lottie4j.core.definition.ShapeType;
import com.lottie4j.core.info.PropertyListingList;
import com.lottie4j.core.model.animation.Animated;
import com.lottie4j.core.model.shape.BaseShape;

/**
 * Represents a rectangle shape in a Lottie animation.
 * <p>
 * This record defines a rectangular shape with configurable position, size, and corner rounding.
 * As an implementation of BaseShape, it can be used as part of layer compositions and supports
 * standard shape properties including blend modes, visibility, and layer organization.
 * <p>
 * The rectangle's appearance is controlled through animated properties:
 * - Position determines the center point of the rectangle
 * - Size defines the width and height dimensions
 * - Rounded corner radius enables smooth, rounded corners instead of sharp 90-degree angles
 * <p>
 * This shape is identified by the type code "rc" in Lottie JSON files and is automatically
 * deserialized through Jackson annotations. Properties marked with @JsonProperty map to
 * abbreviated field names used in the compact Lottie JSON format.
 * <p>
 * Common shape properties inherited from BaseShape include:
 * - name: Display name for the shape
 * - matchName: Name used for matching and referencing
 * - hidden: Controls visibility in the animation
 * - blendMode: Determines how this shape composites with layers beneath it
 * - index: Ordering index within the parent container
 * - clazz: CSS class identifier
 * - id: Unique identifier
 *
 * @param name                the display name of the rectangle
 * @param matchName           the name used for matching and referencing this shape
 * @param hidden              whether the rectangle is hidden in the animation
 * @param blendMode           the blend mode used for compositing this shape
 * @param index               the ordering index of this shape within its container
 * @param clazz               the CSS class identifier for styling purposes
 * @param id                  the unique identifier for this shape
 * @param d                   undefined property for future or proprietary use
 * @param cix                 undefined property for future or proprietary use
 * @param position            the animated position of the rectangle's center point
 * @param size                the animated size (width and height) of the rectangle
 * @param roundedCornerRadius the animated radius for rounding the rectangle's corners
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record Rectangle(
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

        // Rectangle
        @JsonProperty("p") Animated position,
        @JsonProperty("s") Animated size,
        @JsonProperty("r") Animated roundedCornerRadius
) implements BaseShape {
    @Override
    @JsonIgnore
    public PropertyListingList getList() {
        var list = new PropertyListingList("Rectangle");
        list.add("Match name", matchName);
        list.add("Type", type);
        list.add("Hidden", hidden);
        list.add("Blend mode", blendMode);
        list.add("Index", index);
        list.add("Clazz", clazz);
        list.add("ID", id);
        list.add("d", d);
        list.add("cix", cix);
        list.add("Position", position);
        list.add("Size", size);
        list.add("Rounded corner radius", roundedCornerRadius);
        return list;
    }

    @Override
    public ShapeType shapeType() {
        return ShapeType.RECTANGLE;
    }
}

