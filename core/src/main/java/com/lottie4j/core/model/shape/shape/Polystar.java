package com.lottie4j.core.model.shape.shape;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lottie4j.core.definition.BlendMode;
import com.lottie4j.core.definition.ShapeType;
import com.lottie4j.core.definition.StarType;
import com.lottie4j.core.info.PropertyListingList;
import com.lottie4j.core.model.animation.Animated;
import com.lottie4j.core.model.shape.BaseShape;

/**
 * Represents a polystar shape in a Lottie animation, which can render either star or polygon shapes.
 * <p>
 * A polystar is a parametric shape that can create multi-pointed stars or regular polygons based on
 * configuration. The shape is defined by its number of points, radii (inner and outer for stars),
 * position, rotation, and roundness values that control the curvature of the points and edges.
 * <p>
 * This record implements BaseShape and supports JSON serialization/deserialization for Lottie files.
 * The shape type is identified by the "sr" type code in JSON. All properties support animation through
 * the Animated type, allowing the polystar to transform over time.
 * <p>
 * The polystar can operate in two modes determined by the StarType:
 * - Star mode: Uses both inner and outer radius to create pointed star shapes
 * - Polygon mode: Uses only outer radius to create regular polygons
 * <p>
 * Roundness properties control the Bezier curve smoothing applied to the vertices and edges,
 * allowing for everything from sharp geometric shapes to smooth, rounded forms.
 *
 * @param name the display name of the shape
 * @param matchName the match name for referencing
 * @param hidden whether the shape is hidden from rendering
 * @param blendMode the blend mode for compositing
 * @param index the layer ordering index
 * @param clazz the CSS class identifier
 * @param id the unique identifier for the shape
 * @param d undefined property for future use
 * @param cix undefined property for future use
 * @param starType the type of polystar (star or polygon)
 * @param position the animated center position of the shape
 * @param outerRadius the animated outer radius value
 * @param outerRoundness the animated outer vertex roundness value
 * @param rotation the animated rotation angle in degrees
 * @param points the animated number of points or vertices
 * @param innerRadius the animated inner radius for stars
 * @param innerRoundness the animated inner vertex roundness value
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record Polystar(
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

        // Polystar
        @JsonProperty("sy") StarType starType,
        @JsonProperty("p") Animated position,
        @JsonProperty("or") Animated outerRadius,
        @JsonProperty("os") Animated outerRoundness,
        @JsonProperty("r") Animated rotation,
        @JsonProperty("pt") Animated points,
        @JsonProperty("ir") Animated innerRadius,
        @JsonProperty("is") Animated innerRoundness
) implements BaseShape {
    @Override
    @JsonIgnore
    public PropertyListingList getList() {
        var list = new PropertyListingList("Polystar");
        list.add("Match name", matchName);
        list.add("Type", type);
        list.add("Hidden", hidden);
        list.add("Blend mode", blendMode);
        list.add("Index", index);
        list.add("Clazz", clazz);
        list.add("ID", id);
        list.add("d", d);
        list.add("cix", cix);
        list.add("Star type", starType);
        list.add("Position", position);
        list.add("Outer radius", outerRadius);
        list.add("Outer roundness", outerRoundness);
        list.add("Rotation", rotation);
        list.add("Points", points);
        list.add("Inner radius", innerRadius);
        list.add("Inner roundness", innerRoundness);
        return list;
    }

    @Override
    public ShapeType shapeType() {
        return ShapeType.POLYSTAR;
    }
}
