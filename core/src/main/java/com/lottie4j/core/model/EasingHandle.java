package com.lottie4j.core.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.lottie4j.core.helper.ListDoubleDeserializer;
import com.lottie4j.core.info.PropertyListing;
import com.lottie4j.core.info.PropertyListingList;
import com.lottie4j.core.model.keyframe.Keyframe;

import java.util.List;

/**
 * <a href="https://lottiefiles.github.io/lottie-docs/concepts/#easing-handles">Lottie Docs: Easing Handle</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public record EasingHandle(
        @JsonProperty("x")
        @JsonDeserialize(using = ListDoubleDeserializer.class)
        List<Double> x,
        @JsonProperty("y")
        @JsonDeserialize(using = ListDoubleDeserializer.class)
        List<Double> y
) implements Keyframe, PropertyListing {
    @Override
    public PropertyListingList getList() {
        var list = new PropertyListingList("Easing Handle");
        list.addDoubleList("X values", x);
        list.addDoubleList("Y values", y);
        return list;
    }
}