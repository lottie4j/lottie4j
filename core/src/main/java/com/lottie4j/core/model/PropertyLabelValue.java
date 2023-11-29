package com.lottie4j.core.model;

import java.util.ArrayList;
import java.util.List;

public record PropertyLabelValue(String label, String value, List<PropertyLabelValue> nestedLabelValues) {
    public PropertyLabelValue(String label, String value) {
        this(label, value == null ? "" : value, new ArrayList<>());
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
