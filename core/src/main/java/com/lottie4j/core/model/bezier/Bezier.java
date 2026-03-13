package com.lottie4j.core.model.bezier;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.lottie4j.core.info.PropertyListingList;

/**
 * Represents a Bezier curve that can be either static or animated over time.
 * <p>
 * This interface serves as a base type for different types of Bezier curve implementations,
 * such as fixed Bezier curves with a single definition or animated Bezier curves that
 * change through keyframe interpolation.
 * <p>
 * Implementations of this interface provide access to a property listing that describes
 * the structure and parameters of the specific Bezier curve type, which is useful for
 * inspection, debugging, and serialization purposes.
 * <p>
 * Bezier curves are fundamental geometric primitives used in vector graphics and animation
 * to define smooth, scalable curves through control points.
 */
public interface Bezier {
    @JsonIgnore
    PropertyListingList getList();
}