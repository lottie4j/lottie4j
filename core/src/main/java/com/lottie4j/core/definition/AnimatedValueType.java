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
    X(0),
    Y(1),
    WIDTH(0),
    HEIGHT(1),
    RED(0),
    GREEN(1),
    BLUE(2),
    OPACITY(3),
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
