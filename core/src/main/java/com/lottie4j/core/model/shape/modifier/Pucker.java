package com.lottie4j.core.model.shape.modifier;

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
 * Represents a Pucker shape effect in a Lottie animation.
 * <p>
 * The Pucker effect distorts a shape by pulling its edges inward or outward based on a percentage value.
 * This shape type is identified by the "pb" type identifier in Lottie JSON files and can be applied
 * to modify the appearance of paths and other shapes within a composition.
 * <p>
 * This record implements BaseShape and includes standard shape properties such as name, blend mode,
 * visibility, and indexing, along with the pucker-specific percentage parameter that controls the
 * intensity and direction of the distortion effect.
 * <p>
 * The class is designed for JSON serialization and deserialization, ignoring unknown properties
 * and excluding null values from the output.
 *
 * @param name       the display name of the pucker effect
 * @param matchName  the match name used for identifying the shape in the composition
 * @param hidden     whether the shape is hidden from rendering
 * @param blendMode  the blend mode used for compositing this shape with others
 * @param index      the ordering index of this shape within its parent container
 * @param clazz      the CSS class identifier for the shape
 * @param id         the unique identifier for the shape
 * @param d          an undefined property used in some Lottie files
 * @param cix        an undefined property used in some Lottie files
 * @param percentage the animated percentage value controlling the pucker effect intensity and direction,
 *                   where positive values create an inward pucker and negative values create an outward bloat
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record Pucker(
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

        // Pucker
        @JsonProperty("a") Animated percentage
) implements BaseShape {
    @Override
    @JsonIgnore
    public PropertyListingList getList() {
        var list = new PropertyListingList("Pucker");
        list.add("Match name", matchName);
        list.add("Type", type);
        list.add("Hidden", hidden);
        list.add("Blend mode", blendMode);
        list.add("Index", index);
        list.add("Clazz", clazz);
        list.add("ID", id);
        list.add("d", d);
        list.add("cix", cix);
        list.add("Percentage", percentage);
        return list;
    }

    @Override
    public ShapeType shapeType() {
        return ShapeType.PUCKER;
    }
}
