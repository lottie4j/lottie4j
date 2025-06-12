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

public class PropertyListingList {

    private final String title;
    private final List<PropertyLabelValue> list;

    public PropertyListingList(String title) {
        this.title = title;
        list = new ArrayList<>();
    }

    public String getTitle() {
        return title;
    }

    public List<PropertyLabelValue> getList() {
        return list;
    }

    public void add(String label, String value) {
        if (value == null) {
            return;
        }
        list.add(new PropertyLabelValue(label, value));
    }

    public void add(String label, Integer value) {
        if (value == null) {
            return;
        }
        list.add(new PropertyLabelValue(label, value));
    }

    public void add(String label, Double value) {
        if (value == null) {
            return;
        }
        list.add(new PropertyLabelValue(label, value));
    }

    public void add(String label, Boolean value) {
        if (value == null) {
            return;
        }
        list.add(new PropertyLabelValue(label, value));
    }

    public void add(String label, Animated value) {
        if (value == null) {
            return;
        }
        list.add(new PropertyLabelValue(label, "Animated", Optional.of(value.getList())));
    }

    public void add(String label, Bezier value) {
        if (value == null) {
            return;
        }
        list.add(new PropertyLabelValue(label, "Bezier", Optional.of(value.getList())));
    }

    public void add(String label, DefinitionWithLabel value) {
        if (value == null) {
            return;
        }
        list.add(new PropertyLabelValue(label, value.label()));
    }

    public void add(String label, PropertyListingList childList) {
        list.add(new PropertyLabelValue(label, "", Optional.of(childList)));
    }

    public void add(String label, String value, Optional<PropertyListingList> childList) {
        list.add(new PropertyLabelValue(label, value, childList));
    }

    public <T extends PropertyListing> void add(String label, T value) {
        if (value == null) {
            return;
        }
        list.add(new PropertyLabelValue(label, value.getClass().getSimpleName(), Optional.ofNullable(value.getList())));
    }

    public <T extends PropertyListing> void addList(String label, List<T> childList) {
        if (childList == null || childList.isEmpty()) {
            return;
        }
        var nestedLabelValues = new PropertyListingList(childList.get(0).getClass().getSimpleName());
        childList.forEach(i -> nestedLabelValues.add(childList.get(0).getClass().getSimpleName(), i.getList()));
        list.add(new PropertyLabelValue(label, String.valueOf(childList.size()), Optional.of(nestedLabelValues)));
    }

    public <T extends Keyframe> void addKeyframeList(String label, List<T> childList) {
        if (childList == null || childList.isEmpty()) {
            return;
        }
        var nestedLabelValues = new PropertyListingList(childList.get(0).getClass().getSimpleName());
        childList.forEach(i -> nestedLabelValues.add(childList.get(0).getClass().getSimpleName(), i.getList()));
        list.add(new PropertyLabelValue(label, String.valueOf(childList.size()), Optional.of(nestedLabelValues)));
    }

    public <T extends BaseShape> void addShapeList(String label, List<T> childList) {
        if (childList == null || childList.isEmpty()) {
            return;
        }
        var nestedLabelValues = new PropertyListingList(childList.get(0).getClass().getSimpleName());
        childList.forEach(i -> nestedLabelValues.add(i.getClass().getSimpleName(), i.getList()));
        list.add(new PropertyLabelValue(label, String.valueOf(childList.size()), Optional.of(nestedLabelValues)));
    }

    public void addDoubleList(String label, List<Double> childList) {
        if (childList == null || childList.isEmpty()) {
            return;
        }
        list.add(new PropertyLabelValue(label, childList.stream().map(String::valueOf).collect(Collectors.joining(", "))));
    }

    public void addBigDecimalList(String label, List<BigDecimal> childList) {
        if (childList == null || childList.isEmpty()) {
            return;
        }
        list.add(new PropertyLabelValue(label, childList.stream().map(String::valueOf).collect(Collectors.joining(", "))));
    }

    public void addDoubleDoubleList(String label, List<List<Double>> childList) {
        if (childList == null || childList.isEmpty()) {
            return;
        }
        var nestedLabelValues = new PropertyListingList(childList.get(0).getClass().getSimpleName());
        childList.forEach(i -> nestedLabelValues.add(i.getClass().getSimpleName(), i.stream().map(String::valueOf).collect(Collectors.joining(", "))));
        list.add(new PropertyLabelValue(label, String.valueOf(childList.size()), Optional.of(nestedLabelValues)));
    }
}
