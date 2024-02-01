package com.lottie4j.core.model.bezier;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lottie4j.core.info.PropertyListingList;

/**
 * https://lottiefiles.github.io/lottie-docs/concepts/#bezier
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public record FixedBezier(
        @JsonProperty("a")
        Integer animated,

        @JsonProperty("k")
        BezierDefinition bezier

) implements Bezier {
    @Override
    public PropertyListingList getList() {
        var list = new PropertyListingList("Fixed Bezier");
        list.add("Animated", animated);
        list.add("Bezier", bezier);
        return list;
    }
}
