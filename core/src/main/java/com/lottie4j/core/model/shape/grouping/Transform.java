package com.lottie4j.core.model.shape.grouping;

import com.fasterxml.jackson.annotation.*;
import com.lottie4j.core.definition.BlendMode;
import com.lottie4j.core.definition.ShapeType;
import com.lottie4j.core.info.PropertyListingList;
import com.lottie4j.core.model.animation.Animated;
import com.lottie4j.core.model.shape.BaseShape;

/**
 * Represents a transformation shape in a Lottie animation that defines spatial and visual modifications
 * applied to graphical elements.
 * <p>
 * This record encapsulates all transformation properties including position, scale, rotation, skew,
 * and opacity. Transform objects are fundamental building blocks in Lottie animations, controlling
 * how shapes and groups are positioned, oriented, and rendered in the animation space.
 * <p>
 * The transformation properties can be animated over time through the Animated type, allowing
 * for dynamic changes to an element's appearance throughout the animation timeline. The transform
 * includes both 2D properties (anchor, position, scale, rotation) and 3D rotation properties
 * (rx, ry, rz) for three-dimensional transformations.
 * <p>
 * This class implements BaseShape and is part of the Lottie shape hierarchy, identified by the
 * shape type "tr" in JSON serialization. It includes standard shape properties such as name,
 * blend mode, visibility, and indexing information.
 * <p>
 * The transform uses Jackson annotations for JSON serialization and deserialization, with
 * abbreviated property names matching the Lottie JSON specification. Unknown properties
 * are ignored during deserialization, and null values are excluded from serialization.
 *
 * @param name      the display name of the transform
 * @param matchName the match name used for identification and referencing
 * @param hidden    whether this transform is hidden from rendering
 * @param blendMode the blend mode used for compositing this transform with underlying layers
 * @param index     the ordering index of this transform within its container
 * @param clazz     the CSS class name for styling or identification purposes
 * @param id        the unique identifier for this transform
 * @param d         undefined property with integer value
 * @param cix       undefined property with integer value
 * @param anchor    the anchor point around which transformations are applied
 * @param position  the position of the transform in the coordinate space
 * @param scale     the scale factor applied to the transform
 * @param rotation  the rotation angle applied to the transform
 * @param rx        the rotation around the X-axis for 3D transforms
 * @param ry        the rotation around the Y-axis for 3D transforms
 * @param rz        the rotation around the Z-axis for 3D transforms
 * @param skew      the skew angle applied to the transform
 * @param skewAxis  the axis along which skew is applied
 * @param opacity   the opacity level of the transform
 * @param unknown   undefined property with unknown purpose
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"ty", "p", "a", "s", "r", "o", "sk", "sa", "nm", "mn", "hd", "bm", "ix", "d", "cix", "rx", "ry", "rz", "or", "cl", "ln"})
public record Transform(
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

        // Transform
        @JsonProperty("a") Animated anchor,
        @JsonProperty("p") Animated position,
        @JsonProperty("s") Animated scale,
        @JsonProperty("r") Animated rotation,
        @JsonProperty("rx") Animated rx,
        @JsonProperty("ry") Animated ry,
        @JsonProperty("rz") Animated rz,
        @JsonProperty("sk") Animated skew,
        @JsonProperty("sa") Animated skewAxis,
        @JsonProperty("o") Animated opacity,
        @JsonProperty("or") Animated unknown
) implements BaseShape {
    @Override
    @JsonIgnore
    public PropertyListingList getList() {
        var list = new PropertyListingList("Transform");
        list.add("Match name", matchName);
        list.add("Type", type);
        list.add("Hidden", hidden);
        list.add("Blend mode", blendMode);
        list.add("Index", index);
        list.add("Clazz", clazz);
        list.add("ID", id);
        list.add("d", d);
        list.add("cix", cix);
        list.add("Anchor", anchor);
        list.add("Position", position);
        list.add("Scale", scale);
        list.add("Rotation", rotation);
        list.add("RX", rx);
        list.add("RY", ry);
        list.add("RZ", rz);
        list.add("Skew", skew);
        list.add("Skew axis", skewAxis);
        list.add("Opacity", opacity);
        list.add("Unknown", unknown);
        return list;
    }

    @Override
    public ShapeType shapeType() {
        return ShapeType.TRANSFORM;
    }
}
