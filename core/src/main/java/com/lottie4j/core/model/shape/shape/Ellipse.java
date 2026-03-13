package com.lottie4j.core.model.shape.shape;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lottie4j.core.definition.BlendMode;
import com.lottie4j.core.definition.ShapeType;
import com.lottie4j.core.info.PropertyListingList;
import com.lottie4j.core.model.Animated;
import com.lottie4j.core.model.shape.BaseShape;

/**
 * Represents an ellipse shape in a Lottie animation.
 * <p>
 * An ellipse is a geometric shape defined by its position and size properties. The ellipse can be
 * animated through its position and size values, allowing for dynamic scaling and movement within
 * the animation. This shape type is commonly used to create circular or oval forms in vector-based
 * animations.
 * <p>
 * The ellipse inherits common shape properties such as name, visibility, blend mode, and indexing
 * from the BaseShape interface. It is identified by the shape type ELLIPSE and is deserialized
 * from JSON using the type identifier "el".
 * <p>
 * This record implements Jackson JSON annotations for proper serialization and deserialization,
 * ignoring unknown properties and excluding null values from the output. All properties are
 * mapped to their corresponding JSON field names using JsonProperty annotations.
 *
 * @param name      the display name of the ellipse shape
 * @param matchName the match name used for identifying the shape in expressions or references
 * @param hidden    indicates whether the shape is hidden in the composition
 * @param blendMode the blend mode used for compositing this shape with other layers
 * @param index     the index position of this shape in its parent container
 * @param clazz     a class identifier for the shape
 * @param id        a unique identifier for the shape
 * @param d         an undefined property whose purpose is not documented
 * @param cix       an undefined property whose purpose is not documented
 * @param position  the animated position of the ellipse's center point
 * @param size      the animated size of the ellipse, defining its width and height
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record Ellipse(
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

        // Ellipse
        @JsonProperty("p") Animated position,
        @JsonProperty("s") Animated size
) implements BaseShape {

    @Override
    @JsonIgnore
    public PropertyListingList getList() {
        var list = new PropertyListingList("Ellipse");
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
        return list;
    }

    @Override
    public ShapeType shapeType() {
        return ShapeType.ELLIPSE;
    }
}
