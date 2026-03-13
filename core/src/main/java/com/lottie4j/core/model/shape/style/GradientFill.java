package com.lottie4j.core.model.shape.style;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lottie4j.core.definition.BlendMode;
import com.lottie4j.core.definition.FillRule;
import com.lottie4j.core.definition.GradientType;
import com.lottie4j.core.definition.ShapeType;
import com.lottie4j.core.info.PropertyListing;
import com.lottie4j.core.info.PropertyListingList;
import com.lottie4j.core.model.Animated;
import com.lottie4j.core.model.shape.BaseShape;

/**
 * Represents a gradient fill shape in a Lottie animation that fills a path with a gradient color transition.
 * <p>
 * A gradient fill defines how colors transition smoothly from one point to another within a shape path.
 * The gradient can be either linear or radial, with multiple color stops defining the color progression.
 * The fill behavior is controlled by fill rules that determine which areas of complex paths are filled.
 * <p>
 * The gradient is defined by a starting point and ending point (for linear gradients), or by radial
 * parameters including highlight position and angle (for radial gradients). The actual colors and their
 * positions along the gradient are specified in the GradientColor object.
 * <p>
 * This class implements the BaseShape interface and is part of a polymorphic shape hierarchy used in
 * Lottie animations. It is deserialized from JSON using the type identifier "gf".
 * <p>
 * All visual properties such as opacity, start/end points, and highlight parameters can be animated
 * over time using the Animated wrapper type. This allows the gradient to change dynamically throughout
 * the animation timeline.
 * <p>
 * The class includes standard shape properties inherited from BaseShape such as name, match name,
 * blend mode, visibility, and layer ordering index. These properties control how the gradient fill
 * interacts with other shapes and layers in the animation.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record GradientFill(
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

        // GradientFill
        @JsonProperty("r") FillRule fillRule,
        @JsonProperty("o") Animated opacity,
        @JsonProperty("s") Animated startingPoint,
        @JsonProperty("e") Animated endPoint,
        @JsonProperty("t") GradientType gradientType,
        @JsonProperty("g") GradientColor colors,
        @JsonProperty("h") Animated highlightLength,
        @JsonProperty("a") Animated highlightAngle
) implements BaseShape {
    @Override
    @JsonIgnore
    public PropertyListingList getList() {
        var list = new PropertyListingList("Gradient Fill");
        list.add("Match name", matchName);
        list.add("Type", type);
        list.add("Hidden", hidden);
        list.add("Blend mode", blendMode);
        list.add("Index", index);
        list.add("Clazz", clazz);
        list.add("ID", id);
        list.add("d", d);
        list.add("cix", cix);
        list.add("Fill rule", fillRule);
        list.add("Opacity", opacity);
        list.add("Starting point", startingPoint);
        list.add("End point", endPoint);
        list.add("GradientType", gradientType);
        list.add("Colors", colors);
        list.add("Highlight length", highlightLength);
        list.add("Highlight angle", highlightAngle);
        return list;
    }

    @Override
    public ShapeType shapeType() {
        return ShapeType.GRADIENT_FILL;
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
