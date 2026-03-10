package com.lottie4j.core.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lottie4j.core.info.PropertyListing;
import com.lottie4j.core.info.PropertyListingList;

/**
 * Represents a value parameter for a Lottie effect.
 * Each effect has multiple values that control its behavior.
 *
 * @param name  the parameter name
 * @param type  the value type identifier
 * @param value the animated value
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record EffectValue(
        @JsonProperty("nm") String name,
        @JsonProperty("ty") Integer type,
        @JsonProperty("v") Animated value
) implements PropertyListing {
    @Override
    @JsonIgnore
    public PropertyListingList getList() {
        var list = new PropertyListingList("EffectValue");
        list.add("Name", name);
        list.add("Type", type);
        list.add("Value", value);
        return list;
    }
}

