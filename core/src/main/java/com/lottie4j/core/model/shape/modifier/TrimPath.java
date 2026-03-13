package com.lottie4j.core.model.shape.modifier;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lottie4j.core.definition.BlendMode;
import com.lottie4j.core.definition.ShapeType;
import com.lottie4j.core.definition.TrimMultipleShapes;
import com.lottie4j.core.info.PropertyListingList;
import com.lottie4j.core.model.animation.Animated;
import com.lottie4j.core.model.shape.BaseShape;

/**
 * Represents a trim path shape that modifies the visible portion of a path or multiple paths in a Lottie animation.
 * <p>
 * Trim paths are used to animate the drawing or erasing of vector paths by controlling which segment of the path
 * is visible. This is commonly used for line-drawing animations, progress indicators, and reveal effects.
 * The trim operation can be controlled through three main parameters: segment start, segment end, and offset.
 * <p>
 * This record implements the BaseShape interface and is identified by the shape type TRIM in Lottie JSON files.
 * When applied to multiple shapes, the trimMultipleShapes property determines how the trim operation is distributed
 * across those shapes.
 * <p>
 * As a record class, TrimPath is immutable and provides automatic implementations of equals(), hashCode(), and toString().
 * JSON serialization and deserialization are handled through Jackson annotations, with unknown properties being ignored
 * and null values excluded from the output.
 *
 * @param name               the display name of the trim path shape
 * @param matchName          the match name used for identification and referencing
 * @param hidden             whether this trim path is hidden in the rendering
 * @param blendMode          the blend mode used for compositing this shape with other layers
 * @param index              the index position of this shape in its parent container
 * @param clazz              the CSS class identifier for styling purposes
 * @param id                 the unique identifier for this trim path
 * @param d                  an undefined property, purpose not documented in specification
 * @param cix                an undefined property, possibly related to composition indexing
 * @param segmentStart       the animated property defining where the visible segment starts (0-100%)
 * @param segmentEnd         the animated property defining where the visible segment ends (0-100%)
 * @param offset             the animated property defining the offset rotation of the trim along the path
 * @param trimMultipleShapes defines how trim is applied when multiple shapes are present
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record TrimPath(
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

        // Trim
        @JsonProperty("s") Animated segmentStart,
        @JsonProperty("e") Animated segmentEnd,
        @JsonProperty("o") Animated offset,
        @JsonProperty("m") TrimMultipleShapes trimMultipleShapes
) implements BaseShape {
    @Override
    @JsonIgnore
    public PropertyListingList getList() {
        var list = new PropertyListingList("Trim Path");
        list.add("Match name", matchName);
        list.add("Type", type);
        list.add("Hidden", hidden);
        list.add("Blend mode", blendMode);
        list.add("Index", index);
        list.add("Clazz", clazz);
        list.add("ID", id);
        list.add("d", d);
        list.add("cix", cix);
        list.add("Segment start", segmentStart);
        list.add("Segment end", segmentEnd);
        list.add("Offset", offset);
        list.add("Trim multiple shapes", trimMultipleShapes);
        return list;
    }

    @Override
    public ShapeType shapeType() {
        return ShapeType.TRIM;
    }
}
