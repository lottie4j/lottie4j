package com.lottie4j.core.info;

import java.util.Optional;

/**
 * Record used to transform the data within the core model to readable output.
 * This helps to make the structure of a Lottie file easier to understand.
 *
 * @param label             display label for the property
 * @param value             string representation of the property value
 * @param nestedLabelValues optional nested properties for hierarchical structures
 */
public record PropertyLabelValue(String label, String value, Optional<PropertyListingList> nestedLabelValues) {
    /**
     * Creates a property with a string value and no nested properties.
     *
     * @param label display label
     * @param value property value (null converted to empty string)
     */
    public PropertyLabelValue(String label, String value) {
        this(label, value == null ? "" : value, Optional.empty());
    }

    /**
     * Creates a property with a double value and no nested properties.
     *
     * @param label display label
     * @param value numeric value (null converted to empty string)
     */
    public PropertyLabelValue(String label, Double value) {
        this(label, value == null ? "" : String.valueOf(value));
    }

    /**
     * Creates a property with an integer value and no nested properties.
     *
     * @param label display label
     * @param value numeric value (null converted to empty string)
     */
    public PropertyLabelValue(String label, Integer value) {
        this(label, value == null ? "" : String.valueOf(value));
    }

    /**
     * Creates a property with a boolean value and no nested properties.
     *
     * @param label display label
     * @param value boolean value (null converted to empty string)
     */
    public PropertyLabelValue(String label, Boolean value) {
        this(label, value == null ? "" : String.valueOf(value));
    }
}
