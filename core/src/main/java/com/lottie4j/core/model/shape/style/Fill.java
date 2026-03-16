package com.lottie4j.core.model.shape.style;

import com.fasterxml.jackson.annotation.*;
import com.lottie4j.core.definition.BlendMode;
import com.lottie4j.core.definition.FillRule;
import com.lottie4j.core.definition.ShapeType;
import com.lottie4j.core.info.PropertyListingList;
import com.lottie4j.core.model.animation.Animated;
import com.lottie4j.core.model.shape.BaseShape;

/**
 * Represents a fill shape that defines how an area is painted with a solid color in a Lottie animation.
 * <p>
 * Fill is a shape modifier that applies a solid color fill to the path it modifies. The fill can have
 * animated properties for color and opacity, and supports different fill rules for determining which
 * areas of overlapping paths should be filled. This shape type is commonly used in conjunction with
 * path shapes to create colored regions in vector graphics.
 * <p>
 * The fill behavior can be controlled through several properties including blend mode for compositing,
 * fill rule for handling path overlaps, and animated values for dynamic color and opacity changes
 * throughout the animation timeline.
 * <p>
 * This record implements BaseShape and is deserialized from JSON with the type identifier "fl".
 * It uses Jackson annotations for JSON processing and ignores unknown properties during deserialization.
 *
 * @param name      the display name of the fill shape
 * @param matchName the match name used for identifying the shape in expressions or references
 * @param hidden    whether this fill shape is hidden from rendering
 * @param blendMode the blend mode used for compositing this fill with underlying layers
 * @param index     the index position of this shape in the layer's shape list
 * @param clazz     the CSS class name associated with this shape for web contexts
 * @param id        the unique identifier for this shape
 * @param d         undefined property preserved for compatibility
 * @param cix       undefined property preserved for compatibility, possibly related to color index
 * @param fillRule  the rule determining which areas should be filled when paths overlap
 * @param opacity   the animated opacity value controlling the transparency of the fill (0-100)
 * @param color     the animated color value defining the fill color in RGB format
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"ty", "c", "o", "r", "bm", "nm", "mn", "hd", "ix", "d", "cix", "cl", "ln"})
public record Fill(
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

        // Fill
        @JsonProperty("r") FillRule fillRule,
        @JsonProperty("o") Animated opacity,
        @JsonProperty("c") Animated color
) implements BaseShape {
    @Override
    @JsonIgnore
    public PropertyListingList getList() {
        var list = new PropertyListingList("Fill");
        list.add("Match name", matchName);
        list.add("Type", type);
        list.add("Hidden", hidden);
        list.add("Blend mode", blendMode);
        list.add("Index", index);
        list.add("Clazz", clazz);
        list.add("ID", id);
        list.add("d", d);
        list.add("cix", cix);
        list.add("FillRule", fillRule);
        list.add("Opacity", opacity);
        list.add("Color", color);
        return list;
    }

    @Override
    public ShapeType shapeType() {
        return ShapeType.FILL;
    }
}
