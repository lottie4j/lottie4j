package com.lottie4j.core.model.effect;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lottie4j.core.info.PropertyListing;
import com.lottie4j.core.info.PropertyListingList;
import com.lottie4j.core.model.animation.Animated;

/**
 * Represents a value parameter for an effect in a Lottie animation.
 * <p>
 * Effect values define the parameters that control how effects are applied to layers.
 * Each effect value has a name identifier, a type indicator, and an animated value
 * that can change over time during the animation.
 * <p>
 * This record is used as part of the effect system to configure and control various
 * visual effects that can be applied to animation layers. The value property contains
 * the actual animated data that drives the effect parameter.
 *
 * @param name  the identifier name of the effect value parameter
 * @param type  the numeric type indicator that specifies the kind of effect value
 * @param value the animated value data for this effect parameter
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

