package com.lottie4j.core.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lottie4j.core.definition.EffectType;
import com.lottie4j.core.info.PropertyListing;
import com.lottie4j.core.info.PropertyListingList;

import java.util.List;

/**
 * Represents an effect applied to a layer in a Lottie animation.
 * <p>
 * Effects are visual transformations or filters that can be applied to modify the appearance
 * of layers. Each effect has a specific type (such as blur, tint, or drop shadow) and contains
 * a collection of configurable values that control its behavior and visual output.
 * <p>
 * This class supports JSON serialization and deserialization for Lottie file format compatibility,
 * using abbreviated property names as defined in the Lottie specification. Unknown properties
 * during deserialization are ignored, and null values are excluded from serialization.
 *
 * @param name      the display name of the effect
 * @param matchName the unique identifier used for matching and referencing this effect
 * @param index     the numeric index position of this effect in the effect stack
 * @param type      the type of effect being applied, determining its visual transformation behavior
 * @param enabled   flag indicating whether this effect is active (1 for enabled, 0 for disabled)
 * @param values    the list of configurable parameters that control the effect's behavior
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record Effect(
        @JsonProperty("nm") String name,
        @JsonProperty("mn") String matchName,
        @JsonProperty("inv") Integer index,
        @JsonProperty("ty") EffectType type,
        @JsonProperty("en") Integer enabled,
        @JsonProperty("ef") List<EffectValue> values
) implements PropertyListing {
    @Override
    @JsonIgnore
    public PropertyListingList getList() {
        var list = new PropertyListingList("Effect");
        list.add("Match name", matchName);
        list.add("Index", index);
        list.add("Effect type", type);
        list.add("Enabled", enabled);
        list.addList("Values", values);
        return list;
    }
}
