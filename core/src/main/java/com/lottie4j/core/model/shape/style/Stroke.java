package com.lottie4j.core.model.shape.style;

import com.fasterxml.jackson.annotation.*;
import com.lottie4j.core.definition.BlendMode;
import com.lottie4j.core.definition.LineCap;
import com.lottie4j.core.definition.LineJoin;
import com.lottie4j.core.definition.ShapeType;
import com.lottie4j.core.info.PropertyListingList;
import com.lottie4j.core.model.animation.Animated;
import com.lottie4j.core.model.shape.BaseShape;
import com.lottie4j.core.model.transform.StrokeDash;

import java.util.List;

/**
 * Represents a stroke shape in a Lottie animation that defines the outline style for vector paths.
 * <p>
 * A Stroke defines how the outline of a path is rendered, including properties such as color,
 * width, opacity, line caps, line joins, and miter limits. It can also include animated properties
 * to change stroke characteristics over time. Strokes are typically applied to shape layers and
 * path elements to create visible outlines.
 * <p>
 * This record implements BaseShape and is part of the shape type hierarchy used in Lottie animations.
 * It is deserialized from JSON with the type identifier "st" and supports various stroke styling
 * options compatible with vector graphics standards.
 * <p>
 * The stroke supports both static and animated properties through the Animated type, allowing
 * for dynamic changes to stroke characteristics during animation playback. Stroke dashes can be
 * defined to create dashed or dotted line patterns.
 * <p>
 * JSON properties are mapped using Jackson annotations, with unknown properties being ignored
 * during deserialization and null values excluded from serialization.
 *
 * @param name                  the display name of the stroke
 * @param matchName             the match name used for referencing the stroke
 * @param hidden                whether the stroke is hidden from rendering
 * @param blendMode             the blend mode used for compositing the stroke
 * @param index                 the index position of the stroke in the shape stack
 * @param clazz                 the CSS class identifier for the stroke
 * @param id                    the unique identifier for the stroke
 * @param cix                   an undefined property with unknown purpose
 * @param lineCap               the style of line endings (butt, round, or square)
 * @param lineJoin              the style of line corners (miter, round, or bevel)
 * @param miterLimit            the limit for miter joins before beveling occurs
 * @param miterLimitAlternative an alternative animated miter limit value
 * @param strokeWidth           the width of the stroke line, can be animated
 * @param strokeDashes          the list of dash patterns for creating dashed strokes
 * @param opacity               the opacity of the stroke, can be animated
 * @param color                 the color of the stroke, can be animated
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"ty", "c", "o", "w", "lc", "lj", "ml", "ml2", "bm", "nm", "mn", "hd", "ix", "d", "cix", "cl", "ln"})
public record Stroke(
        // Generic for all Shapes
        @JsonProperty("nm") String name,
        @JsonProperty("mn") String matchName,
        @JsonProperty("hd") Boolean hidden,
        @JsonProperty("bm") BlendMode blendMode,
        @JsonProperty("ix") Integer index,
        @JsonProperty("cl") String clazz,
        @JsonProperty("ln") String id,

        // Undefined

        //@JsonProperty("d") Integer d,
        @JsonProperty("cix") Integer cix,

        // Stroke
        @JsonProperty("lc") LineCap lineCap,
        @JsonProperty("lj") LineJoin lineJoin,
        @JsonProperty("ml") Integer miterLimit,
        @JsonProperty("ml2") Animated miterLimitAlternative,
        @JsonProperty("w") Animated strokeWidth,
        @JsonProperty("d") List<StrokeDash> strokeDashes,
        @JsonProperty("o") Animated opacity,
        @JsonProperty("c") Animated color
) implements BaseShape {
    @Override
    @JsonIgnore
    public PropertyListingList getList() {
        var list = new PropertyListingList("Stroke");
        list.add("Match name", matchName);
        list.add("Type", type);
        list.add("Hidden", hidden);
        list.add("Blend mode", blendMode);
        list.add("Index", index);
        list.add("Clazz", clazz);
        list.add("ID", id);
        list.add("cix", cix);
        list.add("Line cap", lineCap);
        list.add("Line join", lineJoin);
        list.add("Miter limit", miterLimit);
        list.add("Miter limit alternative", miterLimitAlternative);
        list.add("Stroke width", strokeWidth);
        list.add("Opacity", opacity);
        list.add("Color", color);
        list.addList("Stroke dashes", strokeDashes);
        return list;
    }

    @Override
    public ShapeType shapeType() {
        return ShapeType.STROKE;
    }
}
