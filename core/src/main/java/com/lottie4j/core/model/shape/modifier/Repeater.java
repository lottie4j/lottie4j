package com.lottie4j.core.model.shape.modifier;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lottie4j.core.definition.BlendMode;
import com.lottie4j.core.definition.Composite;
import com.lottie4j.core.definition.ShapeType;
import com.lottie4j.core.info.PropertyListingList;
import com.lottie4j.core.model.animation.Animated;
import com.lottie4j.core.model.shape.BaseShape;
import com.lottie4j.core.model.transform.RepeaterTransform;

/**
 * Represents a repeater shape modifier that duplicates and transforms shapes in Lottie animations.
 * <p>
 * A Repeater is a shape modifier that creates multiple copies of the shapes it affects, applying
 * transformations to each successive copy. It can be used to create patterns, arrays, or radial
 * distributions of shapes. The repeater applies its transform cumulatively, so each copy is
 * transformed relative to the previous one.
 * <p>
 * This record implements BaseShape and provides JSON serialization/deserialization support
 * through Jackson annotations. Unknown JSON properties are ignored during deserialization,
 * and null values are excluded from serialization.
 * <p>
 * The repeater operates by:
 * - Creating the specified number of copies of the affected shapes
 * - Applying the repeater transform to each copy cumulatively
 * - Using the offset to skip or delay the start of the repetition
 * - Following the stacking order to determine how copies are layered
 *
 * @param name              the display name of the repeater
 * @param matchName         the name used for matching and identification purposes
 * @param hidden            indicates whether this repeater is hidden and should not be rendered
 * @param blendMode         the blend mode used for compositing this repeater with other layers
 * @param index             the ordering index of this shape within its parent container
 * @param clazz             the CSS class name associated with this shape for styling or identification
 * @param id                the unique identifier for this shape element
 * @param d                 an undefined property whose purpose is not documented
 * @param cix               an undefined property whose purpose is not documented
 * @param copies            the animated number of copies to create, can vary over time
 * @param offset            the animated offset value that shifts where the repetition starts
 * @param stackingOrder     determines how repeated copies are stacked (above or below each other)
 * @param repeaterTransform the transform applied cumulatively to each copy, including position, scale, rotation, anchor, opacity, start opacity, and end opacity
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record Repeater(
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

        // Repeater
        @JsonProperty("c") Animated copies,
        @JsonProperty("o") Animated offset,
        @JsonProperty("m") Composite stackingOrder,

        @JsonProperty("tr") RepeaterTransform repeaterTransform
) implements BaseShape {
    @Override
    @JsonIgnore
    public PropertyListingList getList() {
        var list = new PropertyListingList("Repeater");
        list.add("Match name", matchName);
        list.add("Type", type);
        list.add("Hidden", hidden);
        list.add("Blend mode", blendMode);
        list.add("Index", index);
        list.add("Clazz", clazz);
        list.add("ID", id);
        list.add("d", d);
        list.add("cix", cix);
        list.add("Copies", copies);
        list.add("Offset", offset);
        list.add("Stacking order", stackingOrder);
        list.add("Repeater transform", repeaterTransform);
        return list;
    }

    @Override
    public ShapeType shapeType() {
        return ShapeType.REPEATER;
    }
}
