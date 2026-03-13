package com.lottie4j.core.model.shape.modifier;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lottie4j.core.definition.BlendMode;
import com.lottie4j.core.definition.LineJoin;
import com.lottie4j.core.definition.ShapeType;
import com.lottie4j.core.info.PropertyListingList;
import com.lottie4j.core.model.Animated;
import com.lottie4j.core.model.shape.BaseShape;

/**
 * Represents a Zig Zag shape effect in a Lottie animation.
 * <p>
 * The Zig Zag effect creates a wave-like distortion along the edges of a shape by adding alternating
 * peaks and valleys. This effect is commonly used to create decorative borders, stylized edges, or
 * animated wave patterns. The distortion can be controlled through the amount property and supports
 * different line join styles for controlling how the zig zag corners are rendered.
 * <p>
 * This record implements BaseShape and is deserialized from Lottie JSON files with the type identifier "zz".
 * It supports JSON serialization/deserialization through Jackson annotations, ignoring unknown properties
 * and excluding null values from output.
 * <p>
 * The Zig Zag effect includes standard shape properties inherited from BaseShape such as name, match name,
 * visibility, blend mode, and layer ordering properties. Additionally, it provides specific properties
 * for controlling the zig zag distortion including the line join style, amount of distortion, and miter
 * limit for controlling sharp corners.
 *
 * @param name       the display name of the zig zag effect
 * @param matchName  the match name used for identifying the effect in expressions
 * @param hidden     whether the effect is hidden from rendering
 * @param blendMode  the blend mode used for compositing this effect
 * @param index      the index position of this effect in the layer's effect stack
 * @param clazz      the CSS class name for styling purposes
 * @param id         the unique identifier for this effect
 * @param d          an undefined property for future or internal use
 * @param cix        an undefined composition index property
 * @param lineJoin   the line join style determining how corners are rendered in the zig zag pattern
 * @param amount     the animated property controlling the amplitude or intensity of the zig zag distortion
 * @param miterLimit the animated property controlling the limit at which mitered corners are beveled
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ZigZag(
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

        // ZigZag
        @JsonProperty("lj") LineJoin lineJoin,
        @JsonProperty("a") Animated amount,
        @JsonProperty("ml") Animated miterLimit
) implements BaseShape {
    @Override
    @JsonIgnore
    public PropertyListingList getList() {
        var list = new PropertyListingList("Zig Zag");
        list.add("Match name", matchName);
        list.add("Type", type);
        list.add("Hidden", hidden);
        list.add("Blend mode", blendMode);
        list.add("Index", index);
        list.add("Clazz", clazz);
        list.add("ID", id);
        list.add("d", d);
        list.add("cix", cix);
        list.add("Line join", lineJoin);
        list.add("Amount", amount);
        list.add("Miter limit", miterLimit);
        return list;
    }

    @Override
    public ShapeType shapeType() {
        return ShapeType.ZIG_ZAG;
    }
}
