package com.lottie4j.core.model.shape.modifier;

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
 * Represents a twist distortion effect that can be applied to shapes in a Lottie animation.
 * <p>
 * The Twist shape modifier applies a rotational distortion around a specified center point,
 * creating a spiral or twisting effect on the target shape. The intensity and direction of
 * the twist can be controlled through the angle parameter, while the center parameter defines
 * the pivot point of the distortion.
 * <p>
 * This class is a record-based implementation of BaseShape that is designed for JSON
 * serialization/deserialization of Lottie animation files. It contains both common shape
 * properties inherited from the BaseShape interface and twist-specific properties.
 * <p>
 * The twist effect is identified by the type "tw" in Lottie JSON files and provides
 * animated control over both the twist angle and center position, allowing for dynamic
 * distortion effects throughout an animation timeline.
 *
 * @param name      the human-readable name of the twist effect
 * @param matchName the name used for matching and identification purposes
 * @param hidden    indicates whether the twist effect is hidden/disabled
 * @param blendMode the blend mode used for compositing this effect
 * @param index     the ordering index of this shape in the layer
 * @param clazz     the CSS class name for styling purposes
 * @param id        the unique identifier for this shape
 * @param d         an undefined property, purpose currently unknown
 * @param cix       an undefined property, possibly related to composition index
 * @param angle     the animated angle of rotation for the twist effect, measured in degrees
 * @param center    the animated center point around which the twist distortion is applied
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record Twist(
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

        // Twist
        @JsonProperty("a") Animated angle,
        @JsonProperty("c") Animated center
) implements BaseShape {
    @Override
    @JsonIgnore
    public PropertyListingList getList() {
        var list = new PropertyListingList("Twist");
        list.add("Match name", matchName);
        list.add("Type", type);
        list.add("Hidden", hidden);
        list.add("Blend mode", blendMode);
        list.add("Index", index);
        list.add("Clazz", clazz);
        list.add("ID", id);
        list.add("d", d);
        list.add("cix", cix);
        list.add("Angle", angle);
        list.add("Center", center);
        return list;
    }

    @Override
    public ShapeType shapeType() {
        return ShapeType.TWIST;
    }
}
