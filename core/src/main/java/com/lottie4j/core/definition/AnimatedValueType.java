package com.lottie4j.core.definition;

/**
 * Enumeration of types representing individual components of animated properties in Lottie animations.
 * Each type corresponds to a specific aspect of an animatable property, such as position coordinates,
 * dimensions, or color channels. The enum associates each type with an index that indicates its position
 * within the property's value array, allowing for efficient access to individual components during animation.
 * <p>
 * The types are organized into logical groups:
 * - Position: X and Y coordinates
 * - Dimensions: WIDTH and HEIGHT
 * - Color channels: RED, GREEN, BLUE, and OPACITY
 * - Composite: COLOR (representing the entire color property)
 * <p>
 * This enumeration is essential for the animation system to correctly interpret and manipulate
 * multi-dimensional property values by accessing their individual components through their respective indices.
 */
public enum AnimatedValueType {
    /** X coordinate position component (index 0) */
    X(0),
    /** Y coordinate position component (index 1) */
    Y(1),
    /** Width dimension component (index 0) */
    WIDTH(0),
    /** Height dimension component (index 1) */
    HEIGHT(1),
    /** Red color channel component (index 0) */
    RED(0),
    /** Green color channel component (index 1) */
    GREEN(1),
    /** Blue color channel component (index 2) */
    BLUE(2),
    /** Opacity/alpha channel component (index 3) */
    OPACITY(3),
    /** Composite color property (index 0) */
    COLOR(0) // TODO
    ;

    final int index;

    /**
     * Constructs an AnimatedValueType with the specified index.
     *
     * @param index the index of this value type within the animated property
     */
    AnimatedValueType(int index) {
        this.index = index;
    }

    /**
     * Returns the index of this value type within the animated property.
     *
     * @return the index
     */
    public int getIndex() {
        return index;
    }
}
