package com.lottie4j.core.model;

/**
 * Enum representing different types of animated values and their corresponding indices.
 * Used to access specific components of multi-dimensional animated properties.
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
