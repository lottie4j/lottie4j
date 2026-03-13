package com.lottie4j.core.model.shape.style;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lottie4j.core.definition.*;
import com.lottie4j.core.info.PropertyListing;
import com.lottie4j.core.info.PropertyListingList;
import com.lottie4j.core.model.Animated;
import com.lottie4j.core.model.StrokeDash;
import com.lottie4j.core.model.shape.BaseShape;

import java.util.List;

/**
 * Represents a gradient stroke shape in a Lottie animation.
 * <p>
 * A gradient stroke applies a gradient color transition along a stroke path, similar to a regular stroke
 * but with color interpolation between multiple color stops. The gradient can be linear or radial and
 * is defined by starting and ending points that determine the gradient's direction and spread.
 * <p>
 * This record extends the base shape functionality with gradient-specific properties including gradient
 * type, color definitions, stroke appearance settings (line caps, joins, miter limits), and optional
 * dash patterns for creating dashed or dotted gradient strokes.
 * <p>
 * All animated properties (stroke width, opacity, gradient points, colors) can change over time according
 * to their respective animation keyframes, allowing for dynamic gradient stroke effects in animations.
 *
 * @param name                  the display name of the gradient stroke
 * @param matchName             the match name used for identification and referencing
 * @param hidden                whether this gradient stroke is hidden in the output
 * @param blendMode             the blend mode used for compositing this stroke with underlying layers
 * @param index                 the rendering order index of this shape
 * @param clazz                 the CSS class name associated with this shape
 * @param id                    the unique identifier for this shape
 * @param cix                   an undefined property for potential future use
 * @param lineCap               the style of line endings (butt, round, or square)
 * @param lineJoin              the style of line corners (miter, round, or bevel)
 * @param miterLimit            the maximum miter length for miter joins, as an integer
 * @param miterLimitAlternative an alternative animated miter limit value
 * @param strokeWidth           the animated width of the stroke
 * @param opacity               the animated opacity of the gradient stroke (0-100)
 * @param startingPoint         the animated starting position of the gradient in coordinate space
 * @param endPoint              the animated ending position of the gradient in coordinate space
 * @param gradientType          the type of gradient (linear or radial)
 * @param colors                the gradient color configuration including color stops and interpolation
 * @param strokeDashes          optional list of dash patterns to create dashed gradient strokes
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record GradientStroke(
        // Generic for all Shapes
        @JsonProperty("nm") String name,
        @JsonProperty("mn") String matchName,
        @JsonProperty("hd") Boolean hidden,
        @JsonProperty("bm") BlendMode blendMode,
        @JsonProperty("ix") Integer index,
        @JsonProperty("cl") String clazz,
        @JsonProperty("ln") String id,

        // Undefined
        @JsonProperty("cix") Integer cix,

        // GradientStroke
        @JsonProperty("lc") LineCap lineCap,
        @JsonProperty("lj") LineJoin lineJoin,
        @JsonProperty("ml") Integer miterLimit,
        @JsonProperty("ml2") Animated miterLimitAlternative,
        @JsonProperty("w") Animated strokeWidth,
        @JsonProperty("o") Animated opacity,
        @JsonProperty("s") Animated startingPoint,
        @JsonProperty("e") Animated endPoint,
        @JsonProperty("t") GradientType gradientType,
        @JsonProperty("g") GradientColor colors,
        @JsonProperty("d") List<StrokeDash> strokeDashes
) implements BaseShape {
    @Override
    @JsonIgnore
    public PropertyListingList getList() {
        var list = new PropertyListingList("Gradient Stroke");
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
        list.add("Starting point", startingPoint);
        list.add("End point", endPoint);
        list.add("Gradient type", gradientType);
        list.add("Colors", colors);
        list.addList("Stroke dashes", strokeDashes);
        return list;
    }

    @Override
    public ShapeType shapeType() {
        return ShapeType.GRADIENT_STROKE;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record GradientColor(
            @JsonProperty("p") Integer numberOfColors,
            @JsonProperty("k") Animated colors
    ) implements PropertyListing {
        @Override
        public PropertyListingList getList() {
            var list = new PropertyListingList("Gradient Color");
            list.add("Number of colors", numberOfColors);
            list.add("Colors", colors);
            return list;
        }
    }
}
