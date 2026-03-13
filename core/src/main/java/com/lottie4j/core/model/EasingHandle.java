package com.lottie4j.core.model;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.lottie4j.core.info.PropertyListing;
import com.lottie4j.core.info.PropertyListingList;
import com.lottie4j.core.model.keyframe.Keyframe;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents an easing handle that defines Bézier curve control points for animation interpolation.
 * <p>
 * An easing handle controls the velocity curve of an animation transition between keyframes by
 * specifying control points for a cubic Bézier curve. The x and y coordinates can be stored as
 * either scalar values or arrays to support multi-dimensional animations.
 * <p>
 * This class preserves the original JSON representation (scalar vs array) to ensure accurate
 * round-trip serialization and deserialization. Raw values are stored as Object types and
 * converted to lists of doubles when accessed through the accessor methods.
 * <p>
 * Implements Keyframe to participate in the keyframe type hierarchy and PropertyListing to
 * provide structured output of its configuration.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class EasingHandle implements Keyframe, PropertyListing {
    // Keep raw values so Jackson can round-trip scalar vs array exactly as provided.
    @JsonProperty("x")
    private final Object rawX;

    @JsonProperty("y")
    private final Object rawY;

    @JsonCreator
    public EasingHandle(
            @JsonProperty("x") Object rawX,
            @JsonProperty("y") Object rawY
    ) {
        this.rawX = rawX;
        this.rawY = rawY;
    }

    private static List<Double> toDoubleList(Object value) {
        if (value == null) {
            return Collections.emptyList();
        }

        if (value instanceof Number number) {
            return List.of(number.doubleValue());
        }

        if (value instanceof List<?> list) {
            List<Double> values = new ArrayList<>(list.size());
            for (Object item : list) {
                if (item instanceof Number number) {
                    values.add(number.doubleValue());
                }
            }
            return values;
        }

        return Collections.emptyList();
    }

    public List<Double> x() {
        return toDoubleList(rawX);
    }

    public List<Double> y() {
        return toDoubleList(rawY);
    }

    @Override
    @JsonIgnore
    public PropertyListingList getList() {
        var list = new PropertyListingList("Easing Handle");
        list.addDoubleList("X values", x());
        list.addDoubleList("Y values", y());
        return list;
    }
}