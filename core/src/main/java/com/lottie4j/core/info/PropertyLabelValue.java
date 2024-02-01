package com.lottie4j.core.info;

import java.util.Optional;

/**
 * Record used to transform the data within the core model to readable output.
 * This helps to make the structure of a Lottie file easier to understand.
 */
public record PropertyLabelValue(String label, String value, Optional<PropertyListingList> nestedLabelValues) {
    public PropertyLabelValue(String label, String value) {
        this(label, value == null ? "" : value, Optional.empty());
    }

    public PropertyLabelValue(String label, Double value) {
        this(label, value == null ? "" : String.valueOf(value));
    }

    public PropertyLabelValue(String label, Integer value) {
        this(label, value == null ? "" : String.valueOf(value));
    }

    public PropertyLabelValue(String label, Boolean value) {
        this(label, value == null ? "" : String.valueOf(value));
    }
}
