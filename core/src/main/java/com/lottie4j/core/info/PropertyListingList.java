package com.lottie4j.core.info;

import com.lottie4j.core.model.Animated;
import com.lottie4j.core.model.bezier.Bezier;
import com.lottie4j.core.model.keyframe.Keyframe;
import com.lottie4j.core.model.shape.BaseShape;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Builder class for creating hierarchical property listings from Lottie model objects.
 * Provides type-safe methods to add various property types (strings, numbers, lists, nested objects).
 */
public class PropertyListingList {

    private final String title;
    private final List<PropertyLabelValue> list;

    /**
     * Creates a property listing with a title.
     *
     * @param title title for this property group
     */
    public PropertyListingList(String title) {
        this.title = title;
        list = new ArrayList<>();
    }

    /**
     * Gets the title of this property group.
     *
     * @return group title
     */
    public String getTitle() {
        return title;
    }

    /**
     * Gets the list of properties.
     *
     * @return mutable list of property label-value pairs
     */
    public List<PropertyLabelValue> getList() {
        return list;
    }

    /**
     * Adds a string property (null values are skipped).
     *
     * @param label property label
     * @param value property value
     */
    public void add(String label, String value) {
        if (value == null) {
            return;
        }
        list.add(new PropertyLabelValue(label, value));
    }

    /**
     * Adds an integer property (null values are skipped).
     *
     * @param label property label
     * @param value numeric value
     */
    public void add(String label, Integer value) {
        if (value == null) {
            return;
        }
        list.add(new PropertyLabelValue(label, value));
    }

    /**
     * Adds a double property (null values are skipped).
     *
     * @param label property label
     * @param value numeric value
     */
    public void add(String label, Double value) {
        if (value == null) {
            return;
        }
        list.add(new PropertyLabelValue(label, value));
    }

    /**
     * Adds a boolean property (null values are skipped).
     *
     * @param label property label
     * @param value boolean value
     */
    public void add(String label, Boolean value) {
        if (value == null) {
            return;
        }
        list.add(new PropertyLabelValue(label, value));
    }

    /**
     * Adds an animated property with nested structure.
     *
     * @param label property label
     * @param value animated value object
     */
    public void add(String label, Animated value) {
        if (value == null) {
            return;
        }
        list.add(new PropertyLabelValue(label, "Animated", Optional.of(value.getList())));
    }

    /**
     * Adds a Bezier curve property with nested structure.
     *
     * @param label property label
     * @param value bezier object
     */
    public void add(String label, Bezier value) {
        if (value == null) {
            return;
        }
        list.add(new PropertyLabelValue(label, "Bezier", Optional.of(value.getList())));
    }

    /**
     * Adds a definition enum property with human-readable label.
     *
     * @param label property label
     * @param value definition with label
     */
    public void add(String label, DefinitionWithLabel value) {
        if (value == null) {
            return;
        }
        list.add(new PropertyLabelValue(label, value.label()));
    }

    /**
     * Adds a nested property list.
     *
     * @param label     property label
     * @param childList nested property group
     */
    public void add(String label, PropertyListingList childList) {
        list.add(new PropertyLabelValue(label, "", Optional.of(childList)));
    }

    /**
     * Adds a property with explicit value and nested children.
     *
     * @param label     property label
     * @param value     display value
     * @param childList nested property group
     */
    public void add(String label, String value, Optional<PropertyListingList> childList) {
        list.add(new PropertyLabelValue(label, value, childList));
    }

    /**
     * Adds a PropertyListing object with nested structure.
     *
     * @param label property label
     * @param value object implementing PropertyListing
     * @param <T>   type implementing PropertyListing
     */
    public <T extends PropertyListing> void add(String label, T value) {
        if (value == null) {
            return;
        }
        list.add(new PropertyLabelValue(label, value.getClass().getSimpleName(), Optional.ofNullable(value.getList())));
    }

    /**
     * Adds a list of PropertyListing objects with count and nested details.
     *
     * @param label     property label
     * @param childList list of objects implementing PropertyListing
     * @param <T>       type implementing PropertyListing
     */
    public <T extends PropertyListing> void addList(String label, List<T> childList) {
        if (childList == null || childList.isEmpty()) {
            return;
        }
        var nestedLabelValues = new PropertyListingList(childList.get(0).getClass().getSimpleName());
        childList.forEach(i -> nestedLabelValues.add(childList.get(0).getClass().getSimpleName(), i.getList()));
        list.add(new PropertyLabelValue(label, String.valueOf(childList.size()), Optional.of(nestedLabelValues)));
    }

    /**
     * Adds a list of keyframe objects with count and nested details.
     *
     * @param label     property label
     * @param childList list of keyframes
     * @param <T>       keyframe type
     */
    public <T extends Keyframe> void addKeyframeList(String label, List<T> childList) {
        if (childList == null || childList.isEmpty()) {
            return;
        }
        var nestedLabelValues = new PropertyListingList(childList.get(0).getClass().getSimpleName());
        childList.forEach(i -> nestedLabelValues.add(childList.get(0).getClass().getSimpleName(), i.getList()));
        list.add(new PropertyLabelValue(label, String.valueOf(childList.size()), Optional.of(nestedLabelValues)));
    }

    /**
     * Adds a list of shape objects with count and nested details.
     *
     * @param label     property label
     * @param childList list of shapes
     * @param <T>       shape type
     */
    public <T extends BaseShape> void addShapeList(String label, List<T> childList) {
        if (childList == null || childList.isEmpty()) {
            return;
        }
        var nestedLabelValues = new PropertyListingList(childList.get(0).getClass().getSimpleName());
        childList.forEach(i -> nestedLabelValues.add(i.getClass().getSimpleName(), i.getList()));
        list.add(new PropertyLabelValue(label, String.valueOf(childList.size()), Optional.of(nestedLabelValues)));
    }

    /**
     * Adds a list of double values as comma-separated string.
     *
     * @param label     property label
     * @param childList list of numbers
     */
    public void addDoubleList(String label, List<Double> childList) {
        if (childList == null || childList.isEmpty()) {
            return;
        }
        list.add(new PropertyLabelValue(label, childList.stream().map(String::valueOf).collect(Collectors.joining(", "))));
    }

    /**
     * Adds a list of BigDecimal values as comma-separated string.
     *
     * @param label     property label
     * @param childList list of decimal numbers
     */
    public void addBigDecimalList(String label, List<BigDecimal> childList) {
        if (childList == null || childList.isEmpty()) {
            return;
        }
        list.add(new PropertyLabelValue(label, childList.stream().map(String::valueOf).collect(Collectors.joining(", "))));
    }

    /**
     * Adds a 2D list of double values with nested structure.
     *
     * @param label     property label
     * @param childList list of number lists
     */
    public void addDoubleDoubleList(String label, List<List<Double>> childList) {
        if (childList == null || childList.isEmpty()) {
            return;
        }
        var nestedLabelValues = new PropertyListingList(childList.get(0).getClass().getSimpleName());
        childList.forEach(i -> nestedLabelValues.add(i.getClass().getSimpleName(), i.stream().map(String::valueOf).collect(Collectors.joining(", "))));
        list.add(new PropertyLabelValue(label, String.valueOf(childList.size()), Optional.of(nestedLabelValues)));
    }
}
